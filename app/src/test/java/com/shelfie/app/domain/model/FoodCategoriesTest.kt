package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodCategoriesTest {

    @Test
    fun `sauces, spreads and drinks are not grains`() {
        assertEquals(Category.CONDIMENTS, FoodCategories.guess("Ketçap"))
        assertEquals(Category.CONDIMENTS, FoodCategories.guess("Reçel"))
        assertEquals(Category.CONDIMENTS, FoodCategories.guess("Pesto sos"))
        assertEquals(Category.CONDIMENTS, FoodCategories.guess("Fıstık ezmesi"))
        assertEquals(Category.BEVERAGES, FoodCategories.guess("Kahve"))
        assertEquals(Category.BEVERAGES, FoodCategories.guess("Elma suyu"))
        assertEquals(Category.CANNED, FoodCategories.guess("Domates salçası"))
    }

    @Test
    fun `real grains stay grains`() {
        assertEquals(Category.GRAINS, FoodCategories.guess("Mısır gevreği"))
        assertEquals(Category.GRAINS, FoodCategories.guess("Pirinç"))
        assertEquals(Category.GRAINS, FoodCategories.guess("Un"))
    }

    @Test
    fun `look-alike words don't mislead`() {
        assertEquals(Category.DAIRY, FoodCategories.guess("Tereyağı"))
        assertEquals(Category.FRUITS, FoodCategories.guess("Hindistan cevizi"))
        assertEquals(Category.SEAFOOD, FoodCategories.guess("Balık"))
        assertEquals(Category.CONDIMENTS, FoodCategories.guess("Bal"))
        assertNull(FoodCategories.guess("Mystery"))
    }
}
