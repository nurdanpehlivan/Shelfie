package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FoodCategories
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.ShelfLife
import com.shelfie.app.domain.model.ShoppingItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.ShoppingListRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * "Bought it, now it's in the pantry": bought shopping items become pantry items with a
 * category from their name and an estimated expiry, then leave the list. Typing everything in
 * again after shopping is where pantry apps lose people; this makes it one tap.
 */
class StockUpFromShoppingUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val clock: Clock,
) {

    /** Returns how many items went into the pantry. */
    suspend operator fun invoke(items: List<ShoppingItem>): Int {
        if (items.isEmpty()) return 0
        val today = LocalDate.now(clock)
        val now = clock.instant()
        val pantryItems = items.map { bought ->
            val name = bought.name.trim()
            val category = FoodCategories.guess(name) ?: Category.OTHER
            val location = category.defaultLocation
            PantryItem(
                name = name,
                category = category,
                location = location,
                isExpiryEstimated = true,
                purchaseDate = today,
                expiryDate = today.plusDays(ShelfLife.estimateDays(name, category, location).toLong()),
                createdAt = now,
                updatedAt = now,
            )
        }
        pantryRepository.addPantryItems(pantryItems)
        items.forEach { shoppingListRepository.delete(it.id) }
        return pantryItems.size
    }
}
