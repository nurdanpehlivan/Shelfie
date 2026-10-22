package com.shelfie.app.presentation.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.MealPlanEntry
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.repository.MealPlanRepository
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.usecase.AddMealPlanShoppingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class DayPlan(val date: LocalDate, val meals: Map<MealType, MealPlanEntry>)

data class MealPlanUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.MIN,
    val weekStart: LocalDate = LocalDate.MIN,
    val days: List<DayPlan> = emptyList(),
    /** For the slot picker: favourites first, then newest. */
    val recipes: List<SavedRecipe> = emptyList(),
) {
    val plannedCount: Int get() = days.sumOf { it.meals.size }
}

sealed interface MealPlanEffect {
    /** Missing ingredients of the week's recipes went on the shopping list. */
    data class AddedToShoppingList(val addedCount: Int) : MealPlanEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealPlanViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    recipeRepository: RecipeRepository,
    private val addMealPlanShopping: AddMealPlanShoppingUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)

    /** Weeks start on Monday, as in Turkey and most of Europe. */
    private val weekStart = MutableStateFlow(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))

    private val week: Flow<List<MealPlanEntry>> = weekStart.flatMapLatest(mealPlanRepository::observeWeek)

    val uiState: StateFlow<MealPlanUiState> = combine(
        weekStart,
        week,
        recipeRepository.observeSavedRecipes(),
    ) { start, entries, recipes ->
        MealPlanUiState(
            isLoading = false,
            today = today,
            weekStart = start,
            days = (0L until DAYS_IN_WEEK).map { offset ->
                val date = start.plusDays(offset)
                DayPlan(date, entries.filter { it.date == date }.associateBy { it.mealType })
            },
            recipes = recipes.sortedWith(compareByDescending<SavedRecipe> { it.isFavorite }.thenByDescending { it.createdAt }),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealPlanUiState())

    private val _effects = Channel<MealPlanEffect>(Channel.BUFFERED)
    val effects: Flow<MealPlanEffect> = _effects.receiveAsFlow()

    fun shiftWeek(weeks: Long) = weekStart.update { it.plusWeeks(weeks) }

    fun setRecipe(date: LocalDate, mealType: MealType, recipeId: Long) {
        viewModelScope.launch { mealPlanRepository.setRecipe(date, mealType, recipeId) }
    }

    fun setNote(date: LocalDate, mealType: MealType, note: String) {
        viewModelScope.launch { mealPlanRepository.setNote(date, mealType, note) }
    }

    fun clear(date: LocalDate, mealType: MealType) {
        viewModelScope.launch { mealPlanRepository.clear(date, mealType) }
    }

    fun addMissingToShoppingList() {
        val entries = uiState.value.days.flatMap { it.meals.values }
        viewModelScope.launch {
            _effects.send(MealPlanEffect.AddedToShoppingList(addMealPlanShopping(entries, today)))
        }
    }

    private companion object {
        const val DAYS_IN_WEEK = 7L
    }
}
