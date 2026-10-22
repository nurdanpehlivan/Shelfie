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

    @Test
    fun `freezing is offered only for products that freeze well and aren't frozen`() {
        val chicken = jam.copy(name = "Tavuk", category = Category.MEAT, location = StorageLocation.FRIDGE)

        assertTrue(chicken.canFreeze)
        assertFalse(chicken.frozen(today).canFreeze)
        assertFalse(jam.canFreeze)
    }

    @Test
    fun `thawing moves the item to the fridge with a short date`() {
        val frozenChicken = jam.copy(
            name = "Tavuk",
            category = Category.MEAT,
            location = StorageLocation.FREEZER,
            isExpiryEstimated = false,
            expiryDate = today.plusDays(100),
        )

        val thawed = frozenChicken.thawed(today)

        assertEquals(StorageLocation.FRIDGE, thawed.location)
        assertEquals(today.plusDays(1), thawed.expiryDate)
        assertTrue(thawed.isExpiryEstimated)
    }

    @Test
    fun `thawing never extends a date that is already sooner`() {
        val bread = jam.copy(category = Category.BAKERY, location = StorageLocation.FREEZER, expiryDate = today)

        assertEquals(today, bread.thawed(today).expiryDate)
    }

    @Test
    fun `opening shelf-stable milk moves it to the fridge with a short date`() {
        val milk = jam.copy(name = "Süt", category = Category.DAIRY, expiryDate = today.plusDays(120))

        assertTrue(milk.canMarkOpened(today))
        val opened = milk.opened(today)

        assertEquals(StorageLocation.FRIDGE, opened.location)
        assertEquals(today.plusDays(4), opened.expiryDate)
        assertFalse(opened.canMarkOpened(today))
    }

    @Test
    fun `opening a jam keeps it where it is and is offered for no produce`() {
        val opened = jam.opened(today)
        val apple = jam.copy(name = "Elma", category = Category.FRUITS, expiryDate = today.plusDays(20))

        assertEquals(StorageLocation.PANTRY, opened.location)
        assertEquals(today.plusDays(30), opened.expiryDate)
        assertFalse(apple.canMarkOpened(today))
    }
}
