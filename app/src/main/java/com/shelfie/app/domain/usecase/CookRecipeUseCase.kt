package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.model.SavedRecipe
import java.util.Locale
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
        val consumed = pantryRepository.markAllAsConsumed(usedItemIds(saved))
        recipeRepository.markCooked(savedRecipeId)
        return consumed
    }

    /**
     * The chef may leave out expiring items that don't fit the dish; those stay in the pantry.
     * Items are matched to the recipe's pantry ingredients by name. If none matches (an old
     * recipe, or the model renamed everything), every item the recipe was made for counts.
     */
    private suspend fun usedItemIds(saved: SavedRecipe): List<Long> {
        val usedNames = saved.recipe.rescuedIngredients.map { it.name.normalized() }
        if (usedNames.isEmpty()) return saved.pantryItemIds
        val matched = saved.pantryItemIds.filter { id ->
            val name = pantryRepository.getPantryItem(id)?.name?.normalized() ?: return@filter false
            usedNames.any { used -> used == name || used.contains(name) || name.contains(used) }
        }
        return matched.ifEmpty { saved.pantryItemIds }
    }

    private fun String.normalized(): String = trim().lowercase(Locale.getDefault())
}
