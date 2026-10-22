package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RepairCategoriesUseCaseTest {

    private val pantry = mockk<PantryRepository>(relaxUnitFun = true)
    private val settings = mockk<SettingsRepository>(relaxUnitFun = true)
    private val useCase = RepairCategoriesUseCase(pantry, settings)

    @Test
    fun `moves mis-filed shelf products out of grains, once`() = runTest {
        coEvery { settings.isMaintenanceDone(any()) } returns false
        every { pantry.observePantryItems() } returns flowOf(
            listOf(item(1, "Ketçap", Category.GRAINS), item(2, "Pirinç", Category.GRAINS), item(3, "Kahve", Category.GRAINS)),
        )

        assertEquals(2, useCase())

        coVerify { pantry.updatePantryItem(match { it.id == 1L && it.category == Category.CONDIMENTS }) }
        coVerify { pantry.updatePantryItem(match { it.id == 3L && it.category == Category.BEVERAGES }) }
        coVerify(exactly = 0) { pantry.updatePantryItem(match { it.id == 2L }) }
        coVerify { settings.markMaintenanceDone(any()) }
    }

    @Test
    fun `does nothing after it ran`() = runTest {
        coEvery { settings.isMaintenanceDone(any()) } returns true

        assertEquals(0, useCase())

        coVerify(exactly = 0) { pantry.updatePantryItem(any()) }
    }

    private fun item(id: Long, name: String, category: Category) = PantryItem(
        id = id,
        name = name,
        category = category,
        purchaseDate = LocalDate.EPOCH,
        expiryDate = LocalDate.EPOCH,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
