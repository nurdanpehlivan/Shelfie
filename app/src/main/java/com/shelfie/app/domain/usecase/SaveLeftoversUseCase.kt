package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.domain.repository.PantryRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Cooked too much: the leftovers of [recipe] go into the fridge as a pantry item, so they show
 * up in the "use soon" list instead of being forgotten at the back of the fridge.
 */
class SaveLeftoversUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(recipe: SavedRecipe): Long {
        val today = LocalDate.now(clock)
        val now = clock.instant()
        return pantryRepository.addPantryItem(
            PantryItem(
                name = recipe.recipe.title.take(MAX_NAME_LENGTH),
                category = Category.OTHER,
                location = StorageLocation.FRIDGE,
                emoji = LEFTOVERS_EMOJI,
                isExpiryEstimated = true,
                purchaseDate = today,
                expiryDate = today.plusDays(LEFTOVER_DAYS),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private companion object {
        /** Cooked food keeps 3–4 days in the fridge; the shorter end, to be safe. */
        const val LEFTOVER_DAYS = 3L
        const val LEFTOVERS_EMOJI = "🍲"
        const val MAX_NAME_LENGTH = 60
    }
}
