package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.MealPlanEntry
import com.shelfie.app.domain.repository.ShoppingListRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Puts the missing (non-pantry) ingredients of the planned recipes from [today] on onto the
 * shopping list. Past days are already cooked or skipped; each recipe counts once.
 */
class AddMealPlanShoppingUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
) {

    /** Returns how many items were actually added (names already on the list are skipped). */
    suspend operator fun invoke(entries: List<MealPlanEntry>, today: LocalDate): Int {
        val items = entries
            .filter { !it.date.isBefore(today) }
            .mapNotNull { it.recipe }
            .distinctBy { it.id }
            .flatMap { it.recipe.extraIngredients }
            .map { it.name to it.amount }
        return if (items.isEmpty()) 0 else shoppingListRepository.addItems(items)
    }
}
