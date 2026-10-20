package com.shelfie.app.presentation.recipe

import androidx.annotation.StringRes
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.Recipe

sealed interface RecipeUiState {

    /** [ingredients] is empty while they are still being read from the pantry. */
    data class Loading(val ingredients: List<PantryItem> = emptyList()) : RecipeUiState

    /** Nothing is about to spoil, so there is nothing to rescue. */
    data object NoIngredients : RecipeUiState

    /** A recipe opened from the history (or a notification) no longer exists. */
    data object NotFound : RecipeUiState

    data class Error(
        val ingredients: List<PantryItem>,
        @param:StringRes val messageRes: Int,
        val canRetry: Boolean,
        /** The free daily allowance is used up; offer Premium. */
        val canUpgrade: Boolean = false,
    ) : RecipeUiState

    data class Ready(
        val savedRecipeId: Long,
        /** The pantry items the recipe was made for; used for icons when the model gave none. */
        val ingredients: List<PantryItem>,
        val recipe: Recipe,
        val isFavorite: Boolean = false,
        val isCooked: Boolean = false,
        /** Opened from the history: "another recipe" makes no sense there. */
        val isFromHistory: Boolean = false,
        val isMarkingCooked: Boolean = false,
        /** `null` shows the recipe overview; otherwise the step-by-step cooking mode. */
        val cooking: CookingProgress? = null,
    ) : RecipeUiState
}

/** Number of steps done so far; equal to [totalSteps] once the dish is finished. */
data class CookingProgress(val completedSteps: Int, val totalSteps: Int) {
    val currentStepIndex: Int get() = completedSteps
    val isFinished: Boolean get() = completedSteps >= totalSteps
    val fraction: Float get() = if (totalSteps == 0) 1f else completedSteps.toFloat() / totalSteps
}

sealed interface RecipeIntent {

    data object Retry : RecipeIntent

    /** Asks for a different recipe with the same ingredients. */
    data object Regenerate : RecipeIntent

    data object StartCooking : RecipeIntent

    /** "Step N done": moves on to the next step. */
    data object CompleteStep : RecipeIntent

    data object PreviousStep : RecipeIntent

    /** Leaves cooking mode, back to the overview. */
    data object ExitCooking : RecipeIntent

    data object ToggleFavorite : RecipeIntent

    /** "I cooked it": uses up the pantry items and marks the recipe as cooked. */
    data object MarkCooked : RecipeIntent

    /** Puts the extra (non-pantry) ingredients on the shopping list. */
    data object AddMissingToShoppingList : RecipeIntent
}

sealed interface RecipeEffect {

    data class Cooked(val consumedCount: Int) : RecipeEffect

    data class AddedToShoppingList(val addedCount: Int) : RecipeEffect

    data class ShowMessage(@param:StringRes val messageRes: Int) : RecipeEffect
}
