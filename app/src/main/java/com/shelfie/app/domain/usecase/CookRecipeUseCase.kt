package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.RecipeRepository
import javax.inject.Inject

/**
 * "I cooked it": the recipe's pantry items leave the pantry as eaten (rescued) and the recipe
 * is marked as cooked in the history.
 */
class CookRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository,
) {

    /** Returns how many pantry items were marked as eaten (already removed ones don't count). */
    suspend operator fun invoke(savedRecipeId: Long): Int {
        val saved = recipeRepository.getSavedRecipe(savedRecipeId) ?: return 0
        val consumed = pantryRepository.markAllAsConsumed(saved.pantryItemIds)
        recipeRepository.markCooked(savedRecipeId)
        return consumed
    }
}
