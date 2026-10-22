package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.ShoppingItem
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.ShoppingListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StockUpFromShoppingUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 20)
    private val pantry = mockk<PantryRepository>()
    private val shopping = mockk<ShoppingListRepository>(relaxUnitFun = true)
    private val useCase = StockUpFromShoppingUseCase(pantry, shopping, clock)

    @Test
    fun `bought items become estimated pantry items and leave the list`() = runTest {
        val added = slot<List<PantryItem>>()
        coEvery { pantry.addPantryItems(capture(added)) } returns listOf(10L, 11L)

        val count = useCase(
            listOf(
                ShoppingItem(id = 1, name = "Süt", createdAt = Instant.EPOCH),
                ShoppingItem(id = 2, name = "Pirinç", createdAt = Instant.EPOCH),
            ),
        )

        assertEquals(2, count)
        val (milk, rice) = added.captured
        assertEquals(Category.DAIRY, milk.category)
        assertEquals(StorageLocation.FRIDGE, milk.location)
        assertEquals(today.plusDays(5), milk.expiryDate)
        assertTrue(milk.isExpiryEstimated)
        assertEquals(StorageLocation.PANTRY, rice.location)
        coVerify { shopping.delete(1) }
        coVerify { shopping.delete(2) }
    }
}
