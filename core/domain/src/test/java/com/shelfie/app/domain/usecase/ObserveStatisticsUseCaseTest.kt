package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.StatisticsPeriod
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ObserveStatisticsUseCaseTest {

    // Thursday 8 Oct 2026; the week starts on Monday 5 Oct.
    private val clock = Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneOffset.UTC)
    private val pantry = mockk<PantryRepository>()
    private val recipes = mockk<RecipeRepository> { every { observeCookedCountSince(any()) } returns flowOf(2) }
    private val settings = mockk<SettingsRepository> {
        every { settings } returns flowOf(AppSettings(averageItemValue = 40.0))
    }
    private val useCase = ObserveStatisticsUseCase(pantry, recipes, settings, clock)

    @Test
    fun `counts the period and charts eight weeks`() = runTest {
        every { pantry.observeRemovedItemsSince(any()) } returns flowOf(
            listOf(
                removed(1, Category.DAIRY, wasted = false, on = LocalDate.of(2026, 10, 7)),
                removed(2, Category.VEGETABLES, wasted = true, on = LocalDate.of(2026, 10, 6)),
                removed(3, Category.VEGETABLES, wasted = true, on = LocalDate.of(2026, 10, 5)),
                // Last week: outside "this week", still in the chart.
                removed(4, Category.MEAT, wasted = false, on = LocalDate.of(2026, 10, 1)),
            ),
        )

        val stats = useCase(StatisticsPeriod.WEEK).first()

        assertEquals(1, stats.rescuedCount)
        assertEquals(2, stats.wastedCount)
        assertEquals(40.0, stats.estimatedSavings, 0.0)
        assertEquals(2, stats.cookedRecipeCount)
        assertEquals(1f / 3, stats.rescueRate!!, 0.001f)
        assertEquals(listOf(Category.VEGETABLES to 2), stats.topWastedCategories)

        assertEquals(8, stats.weekly.size)
        with(stats.weekly.last()) {
            assertEquals(LocalDate.of(2026, 10, 5), weekStart)
            assertEquals(1, rescued)
            assertEquals(2, wasted)
        }
        assertEquals(1, stats.weekly[6].rescued)
    }

    @Test
    fun `no removals means no rescue rate`() = runTest {
        every { pantry.observeRemovedItemsSince(any()) } returns flowOf(emptyList())

        val stats = useCase(StatisticsPeriod.ALL_TIME).first()

        assertEquals(null, stats.rescueRate)
        assertEquals(0, stats.totalCount)
    }

    private fun removed(id: Long, category: Category, wasted: Boolean, on: LocalDate) = PantryItem(
        id = id,
        name = "item$id",
        category = category,
        purchaseDate = on.minusDays(3),
        expiryDate = on,
        isConsumed = true,
        isWasted = wasted,
        createdAt = Instant.EPOCH,
        updatedAt = on.atTime(12, 0).toInstant(ZoneOffset.UTC),
    )
}
