package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.Recipe
import com.shelfie.app.domain.model.RecipeIngredient
import com.shelfie.app.domain.model.RecipeStep
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.RecipeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CookRecipeUseCaseTest {

    private val recipes = mockk<RecipeRepository>(relaxUnitFun = true)
    private val pantry = mockk<PantryRepository>()
    private val useCase = CookRecipeUseCase(recipes, pantry)

    @Test
    fun `uses up the recipe's pantry items and marks it cooked`() = runTest {
        coEvery { recipes.getSavedRecipe(7) } returns saved(pantryItemIds = listOf(1, 2))
        coEvery { pantry.markAllAsConsumed(listOf(1L, 2L)) } returns 2

        assertEquals(2, useCase(7))

        coVerify { pantry.markAllAsConsumed(listOf(1L, 2L)) }
        coVerify { recipes.markCooked(7) }
    }

    @Test
    fun `items the chef left out stay in the pantry`() = runTest {
        coEvery { recipes.getSavedRecipe(7) } returns saved(
            pantryItemIds = listOf(1, 2),
            ingredients = listOf(RecipeIngredient("Pirinç pilavı", "2 kase", null, isFromPantry = true)),
        )
        coEvery { pantry.getPantryItem(1) } returns pantryItem(1, "Kola")
        coEvery { pantry.getPantryItem(2) } returns pantryItem(2, "Pirinç pilavı")
        coEvery { pantry.markAllAsConsumed(listOf(2L)) } returns 1

        assertEquals(1, useCase(7))

        coVerify { pantry.markAllAsConsumed(listOf(2L)) }
    }

    @Test
    fun `a deleted recipe changes nothing`() = runTest {
        coEvery { recipes.getSavedRecipe(7) } returns null

        assertEquals(0, useCase(7))

        coVerify(exactly = 0) { pantry.markAllAsConsumed(any()) }
        coVerify(exactly = 0) { recipes.markCooked(any()) }
    }

    private fun saved(pantryItemIds: List<Long>, ingredients: List<RecipeIngredient> = emptyList()) = SavedRecipe(
        id = 7,
        recipe = Recipe("Soup", 10, 2, ingredients, listOf(RecipeStep("Cook.", 0))),
        pantryItemIds = pantryItemIds,
        isFavorite = false,
        createdAt = Instant.EPOCH,
        cookedAt = null,
    )

    private fun pantryItem(id: Long, name: String) = PantryItem(
        id = id,
        name = name,
        category = Category.OTHER,
        purchaseDate = LocalDate.EPOCH,
        expiryDate = LocalDate.EPOCH,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
