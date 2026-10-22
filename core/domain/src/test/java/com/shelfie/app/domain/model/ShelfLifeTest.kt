package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShelfLifeTest {

    @Test
    fun `fresh produce keeps for days, not the category's week`() {
        assertEquals(3, ShelfLife.estimateDays("Çilek", Category.FRUITS, StorageLocation.FRIDGE))
        assertEquals(5, ShelfLife.estimateDays("Marul", Category.VEGETABLES, StorageLocation.FRIDGE))
        assertEquals(30, ShelfLife.estimateDays("Elma", Category.FRUITS, StorageLocation.FRIDGE))
    }

    @Test
    fun `where it is kept changes the estimate`() {
        assertEquals(30, ShelfLife.estimateDays("Patates", Category.VEGETABLES, StorageLocation.PANTRY))
        assertEquals(14, ShelfLife.estimateDays("Patates", Category.VEGETABLES, StorageLocation.FRIDGE))
        assertEquals(Category.MEAT.freezerShelfLifeDays, ShelfLife.estimateDays("Tavuk", Category.MEAT, StorageLocation.FREEZER))
    }

    @Test
    fun `dried spices are not fresh peppers`() {
        assertEquals(365, ShelfLife.estimateDays("Karabiber", Category.CONDIMENTS, StorageLocation.PANTRY))
        assertEquals(7, ShelfLife.estimateDays("Kırmızı biber", Category.VEGETABLES, StorageLocation.FRIDGE))
    }

    @Test
    fun `unknown products fall back to the category`() {
        assertNull(ShelfLife.knownDays("Mystery spread", StorageLocation.FRIDGE))
        assertEquals(
            Category.CONDIMENTS.defaultShelfLifeDays,
            ShelfLife.estimateDays("Mystery spread", Category.CONDIMENTS, StorageLocation.PANTRY),
        )
    }
}
