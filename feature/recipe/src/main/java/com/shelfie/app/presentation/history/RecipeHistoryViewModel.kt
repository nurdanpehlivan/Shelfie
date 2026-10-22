package com.shelfie.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryFilter { ALL, FAVORITES, COOKED }

data class RecipeHistoryUiState(
    val isLoading: Boolean = true,
    val recipes: List<SavedRecipe> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    /** Whether anything is saved at all, so an empty filter isn't shown as an empty history. */
    val hasAnyRecipe: Boolean = false,
)

@HiltViewModel
class RecipeHistoryViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<RecipeHistoryUiState> = combine(
        recipeRepository.observeSavedRecipes(),
        filter,
    ) { recipes, filter ->
        RecipeHistoryUiState(
            isLoading = false,
            recipes = when (filter) {
                HistoryFilter.ALL -> recipes
                HistoryFilter.FAVORITES -> recipes.filter { it.isFavorite }
                HistoryFilter.COOKED -> recipes.filter { it.isCooked }
            },
            filter = filter,
            hasAnyRecipe = recipes.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeHistoryUiState())

    fun selectFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun toggleFavorite(recipe: SavedRecipe) {
        viewModelScope.launch { recipeRepository.setFavorite(recipe.id, !recipe.isFavorite) }
    }

    fun delete(recipe: SavedRecipe) {
        viewModelScope.launch { recipeRepository.deleteSavedRecipe(recipe.id) }
    }
}
