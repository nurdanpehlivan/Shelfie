package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GetExpiringIngredientsUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 8)
    private val repository = mockk<PantryRepository>()
    private val settings = mockk<SettingsRepository> { every { settings } returns flowOf(AppSettings()) }
    private val useCase = GetExpiringIngredientsUseCase(repository, settings, clock)

    @Test
    fun `returns at most three unexpired critical items, skipping duplicate names`() = runTest {
        every { repository.observeItemsByFreshness(FreshnessStatus.CRITICAL, today, FreshnessThresholds.DEFAULT) } returns flowOf(
            listOf(
                item(1, "Yogurt", daysLeft = -1),
                item(2, "Milk", daysLeft = 0),
                item(3, "milk", daysLeft = 1),
                item(4, "Tomato", daysLeft = 1),
                item(5, "Chicken", daysLeft = 2),
                item(6, "Spinach", daysLeft = 2),
            ),
        )

        val result = useCase()

        assertEquals(listOf(2L, 4L, 5L), result.map { it.id })
    }

    @Test
    fun `returns an empty list when nothing is critical`() = runTest {
        every { repository.observeItemsByFreshness(FreshnessStatus.CRITICAL, today, FreshnessThresholds.DEFAULT) } returns flowOf(emptyList())

        assertEquals(emptyList<PantryItem>(), useCase())
    }

    private fun item(id: Long, name: String, daysLeft: Long) = PantryItem(
        id = id,
        name = name,
        category = Category.OTHER,
        purchaseDate = today,
        expiryDate = today.plusDays(daysLeft),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
