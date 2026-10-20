package com.shelfie.app.presentation.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.repository.ShoppingListRepository
import com.shelfie.app.domain.usecase.CookRecipeUseCase
import com.shelfie.app.domain.usecase.GetExpiringIngredientsUseCase
import com.shelfie.app.presentation.navigation.RecipeDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class RecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpiringIngredients: GetExpiringIngredientsUseCase,
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val cookRecipe: CookRecipeUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RecipeDestination>()

    private val savedRecipeId: Long? = route.savedRecipeId.takeIf { it != RecipeDestination.NO_SAVED_RECIPE }

    /** Hand-picked pantry items ("cook with what I have"); empty for a rescue recipe. */
    private val pickedIds: List<Long> = route.ingredientIds.split(',').mapNotNull { it.trim().toLongOrNull() }

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Loading())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecipeEffect>(Channel.BUFFERED)
    val effects: Flow<RecipeEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        when {
            savedRecipeId != null -> loadSaved(savedRecipeId)
            pickedIds.isNotEmpty() -> loadPicked()
            else -> load(ingredients = null, forceNew = false)
        }
    }

    fun onIntent(intent: RecipeIntent) {
        when (intent) {
            RecipeIntent.Retry -> retry()
            RecipeIntent.Regenerate -> load(currentIngredients().takeIf { it.isNotEmpty() }, forceNew = true)
            RecipeIntent.StartCooking -> updateReady { ready ->
                ready.copy(cooking = CookingProgress(0, ready.recipe.steps.size))
            }
            RecipeIntent.CompleteStep -> updateCooking { progress ->
                progress.copy(completedSteps = (progress.completedSteps + 1).coerceAtMost(progress.totalSteps))
            }
            RecipeIntent.PreviousStep -> updateCooking { progress ->
                progress.copy(completedSteps = (progress.completedSteps - 1).coerceAtLeast(0))
            }
            RecipeIntent.ExitCooking -> updateReady { it.copy(cooking = null) }
            RecipeIntent.ToggleFavorite -> toggleFavorite()
            RecipeIntent.MarkCooked -> markCooked()
            RecipeIntent.AddMissingToShoppingList -> addMissingToShoppingList()
        }
    }

    private fun retry() {
        if (savedRecipeId != null) {
            loadSaved(savedRecipeId)
        } else {
            load(currentIngredients().takeIf { it.isNotEmpty() }, forceNew = false)
        }
    }

    /** Items may have been eaten since they were picked; whatever is left gets the recipe. */
    private fun loadPicked() {
        loadJob?.cancel()
        _uiState.value = RecipeUiState.Loading()
        loadJob = viewModelScope.launch {
            val items = pickedIds.mapNotNull { pantryRepository.getPantryItem(it) }.filterNot { it.isConsumed }
            load(ingredients = items, forceNew = false)
        }
    }

    private fun loadSaved(id: Long) {
        loadJob?.cancel()
        _uiState.value = RecipeUiState.Loading()
        loadJob = viewModelScope.launch {
            val saved = recipeRepository.getSavedRecipe(id)
            if (saved == null) {
                _uiState.value = RecipeUiState.NotFound
                return@launch
            }
            // Items may have been eaten or deleted since; whatever is left still gives icons.
            val ingredients = saved.pantryItemIds.mapNotNull { pantryRepository.getPantryItem(it) }
            _uiState.value = saved.toReady(ingredients, isFromHistory = true)
        }
    }

    /**
     * Reads the expiring items unless [ingredients] are already known, then shows today's
     * recipe for them (unless [forceNew]) or asks for a new one.
     */
    private fun load(ingredients: List<PantryItem>?, forceNew: Boolean) {
        loadJob?.cancel()
        _uiState.value = RecipeUiState.Loading(ingredients.orEmpty())
        loadJob = viewModelScope.launch {
            val picked = ingredients ?: try {
                getExpiringIngredients()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showFailure(emptyList(), e)
                return@launch
            }
            if (picked.isEmpty()) {
                _uiState.value = RecipeUiState.NoIngredients
                return@launch
            }
            val cached = if (forceNew) null else recipeRepository.getTodaysRecipeFor(picked)
            if (cached != null) {
                _uiState.value = cached.toReady(picked, isFromHistory = false)
                return@launch
            }
            _uiState.value = RecipeUiState.Loading(picked)

            recipeRepository.generateRescueRecipe(picked, MealType.at(LocalTime.now(clock)))
                .onSuccess { _uiState.value = it.toReady(picked, isFromHistory = false) }
                .onFailure { showFailure(picked, it) }
        }
    }

    private fun toggleFavorite() {
        val ready = _uiState.value as? RecipeUiState.Ready ?: return
        val newValue = !ready.isFavorite
        updateReady { it.copy(isFavorite = newValue) }
        launchSafely(onError = { updateReady { it.copy(isFavorite = !newValue) } }) {
            recipeRepository.setFavorite(ready.savedRecipeId, newValue)
        }
    }

    private fun markCooked() {
        val ready = _uiState.value as? RecipeUiState.Ready ?: return
        if (ready.isCooked || ready.isMarkingCooked) return
        updateReady { it.copy(isMarkingCooked = true) }
        launchSafely(onError = { updateReady { it.copy(isMarkingCooked = false) } }) {
            val consumed = cookRecipe(ready.savedRecipeId)
            updateReady { it.copy(isCooked = true, isMarkingCooked = false) }
            _effects.send(RecipeEffect.Cooked(consumed))
        }
    }

    private fun addMissingToShoppingList() {
        val ready = _uiState.value as? RecipeUiState.Ready ?: return
        launchSafely {
            val added = shoppingListRepository.addItems(
                ready.recipe.extraIngredients.map { it.name to it.amount },
            )
            _effects.send(RecipeEffect.AddedToShoppingList(added))
        }
    }

    private fun launchSafely(onError: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError()
                _effects.send(RecipeEffect.ShowMessage(R.string.dashboard_error_update_failed))
            }
        }
    }

    private fun SavedRecipe.toReady(ingredients: List<PantryItem>, isFromHistory: Boolean) =
        RecipeUiState.Ready(
            savedRecipeId = id,
            ingredients = ingredients,
            recipe = recipe,
            isFavorite = isFavorite,
            isCooked = isCooked,
            isFromHistory = isFromHistory,
        )

    private fun showFailure(ingredients: List<PantryItem>, error: Throwable) {
        _uiState.value = RecipeUiState.Error(
            ingredients = ingredients,
            messageRes = error.toRecipeMessageRes(),
            canRetry = error.isRetryableRecipeFailure(),
            canUpgrade = error is PantryScanException.QuotaExceeded && !error.isPremium,
        )
    }

    private fun currentIngredients(): List<PantryItem> = when (val state = _uiState.value) {
        is RecipeUiState.Loading -> state.ingredients
        is RecipeUiState.Error -> state.ingredients
        is RecipeUiState.Ready -> state.ingredients
        RecipeUiState.NoIngredients, RecipeUiState.NotFound -> emptyList()
    }

    private inline fun updateReady(crossinline transform: (RecipeUiState.Ready) -> RecipeUiState.Ready) {
        _uiState.update { if (it is RecipeUiState.Ready) transform(it) else it }
    }

    private inline fun updateCooking(crossinline transform: (CookingProgress) -> CookingProgress) {
        updateReady { ready -> ready.cooking?.let { ready.copy(cooking = transform(it)) } ?: ready }
    }
}
