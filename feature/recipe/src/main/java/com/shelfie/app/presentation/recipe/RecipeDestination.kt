package com.shelfie.app.presentation.recipe

import kotlinx.serialization.Serializable

/**
 * Without [savedRecipeId] the screen picks today's critical items and gets a recipe for them;
 * with one it shows that recipe from the history. [ingredientIds] (comma-separated pantry ids)
 * asks for a recipe with exactly those items instead.
 */
@Serializable
data class RecipeDestination(
    val savedRecipeId: Long = NO_SAVED_RECIPE,
    val ingredientIds: String = "",
) {
    companion object {
        const val NO_SAVED_RECIPE = -1L
    }
}
