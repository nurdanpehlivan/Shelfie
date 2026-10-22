package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.MealPlanEntry
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.Recipe
import com.shelfie.app.domain.model.RecipeIngredient
import com.shelfie.app.domain.model.RecipeStep
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.repository.ShoppingListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AddMealPlanShoppingUseCaseTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val shopping = mockk<ShoppingListRepository>()
    private val useCase = AddMealPlanShoppingUseCase(shopping)

    @Test
    fun `adds the missing ingredients of today's and later recipes, once each`() = runTest {
        val menemen = saved(1, RecipeIngredient("Yumurta", "3", null, isFromPantry = false), RecipeIngredient("Domates", "2", null, isFromPantry = true))
        val soup = saved(2, RecipeIngredient("Mercimek", "1 su bardağı", null, isFromPantry = false))
        coEvery { shopping.addItems(any()) } returns 1

        useCase(
            listOf(
                MealPlanEntry(today.minusDays(1), MealType.DINNER, soup, ""),
                MealPlanEntry(today, MealType.BREAKFAST, menemen, ""),
                MealPlanEntry(today.plusDays(1), MealType.BREAKFAST, menemen, ""),
            ),
            today,
        )

        coVerify { shopping.addItems(listOf("Yumurta" to "3")) }
    }

    @Test
    fun `nothing to add makes no call`() = runTest {
        assertEquals(0, useCase(listOf(MealPlanEntry(today, MealType.LUNCH, null, "Dışarıda")), today))
        coVerify(exactly = 0) { shopping.addItems(any()) }
    }

    private fun saved(id: Long, vararg ingredients: RecipeIngredient) = SavedRecipe(
        id = id,
        recipe = Recipe("Yemek $id", 15, 2, ingredients.toList(), listOf(RecipeStep("Pişir.", 0))),
        pantryItemIds = emptyList(),
        isFavorite = false,
        createdAt = Instant.EPOCH,
        cookedAt = null,
    )
}
