package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PantryItemChangesTest {

    private val today = LocalDate.of(2026, 10, 20)

    private val jam = PantryItem(
        name = "Reçel",
        category = Category.CONDIMENTS,
        location = StorageLocation.PANTRY,
        emoji = "🫙",
        isExpiryEstimated = true,
        purchaseDate = today,
        expiryDate = today.plusDays(90),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `renaming drops the scanned emoji, keeping the name keeps it`() {
        val renamed = jam.edited("Bal", 1.0, MeasurementUnit.PIECE, Category.CONDIMENTS, jam.expiryDate, jam.location)
        val sameName = jam.edited("reçel", 2.0, MeasurementUnit.PIECE, Category.CONDIMENTS, jam.expiryDate, jam.location)

        assertNull(renamed.emoji)
        assertEquals("🫙", sameName.emoji)
    }

    @Test
    fun `a date the user chose is no longer an estimate`() {
        val edited = jam.edited(jam.name, 1.0, MeasurementUnit.PIECE, jam.category, today.plusDays(200), jam.location)

        assertFalse(edited.isExpiryEstimated)
        assertTrue(jam.edited(jam.name, 3.0, MeasurementUnit.PIECE, jam.category, jam.expiryDate, jam.location).isExpiryEstimated)
    }

    @Test
    fun `freezing extends a short date but never shortens a long one`() {
        val chicken = jam.copy(name = "Tavuk", category = Category.MEAT, expiryDate = today.plusDays(1), isExpiryEstimated = false)

        val frozen = chicken.frozen(today)
        assertEquals(StorageLocation.FREEZER, frozen.location)
        assertEquals(today.plusDays(Category.MEAT.freezerShelfLifeDays.toLong()), frozen.expiryDate)
        assertTrue(frozen.isExpiryEstimated)

        val honey = jam.copy(expiryDate = today.plusDays(700), isExpiryEstimated = false)
        assertEquals(honey.expiryDate, honey.frozen(today).expiryDate)
        assertFalse(honey.frozen(today).isExpiryEstimated)
    }

    @Test
    fun `halving keeps half of what is left`() {
        assertEquals(1.5, jam.copy(quantity = 3.0).halved().quantity, 0.0)
    }
}
