package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Recipe
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
    fun `a deleted recipe changes nothing`() = runTest {
        coEvery { recipes.getSavedRecipe(7) } returns null

        assertEquals(0, useCase(7))

        coVerify(exactly = 0) { pantry.markAllAsConsumed(any()) }
        coVerify(exactly = 0) { recipes.markCooked(any()) }
    }

    private fun saved(pantryItemIds: List<Long>) = SavedRecipe(
        id = 7,
        recipe = Recipe("Soup", 10, 2, emptyList(), listOf(RecipeStep("Cook.", 0))),
        pantryItemIds = pantryItemIds,
        isFavorite = false,
        createdAt = Instant.EPOCH,
        cookedAt = null,
    )
}
