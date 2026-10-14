package com.shelfie.app.presentation.common

import com.shelfie.app.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodEmojiTest {

    @Test
    fun `recognises common foods in every app language`() {
        assertEquals("🍅", foodEmoji("Tomatoes", Category.VEGETABLES))
        assertEquals("🍅", foodEmoji("Domates", Category.VEGETABLES))
        assertEquals("🥛", foodEmoji("Milch", Category.DAIRY))
        assertEquals("🍗", foodEmoji("Pechuga de pollo", Category.MEAT))
        assertEquals("🍞", foodEmoji("Pain de mie", Category.BAKERY))
    }

    @Test
    fun `specific rules win over look-alike prefixes`() {
        assertEquals("🧃", foodEmoji("Orange juice", Category.BEVERAGES))
        assertEquals("🥔", foodEmoji("Pomme de terre", Category.VEGETABLES))
        assertEquals("🥒", foodEmoji("Salatalık", Category.VEGETABLES))
        assertEquals("🥬", foodEmoji("Laitue", Category.VEGETABLES))
        assertEquals("🍆", foodEmoji("Eggplant", Category.VEGETABLES))
    }

    @Test
    fun `short keywords only match whole words`() {
        // "pan" must not catch "pancake", "tea" must not catch "steak".
        assertEquals("🥩", foodEmoji("Steak", Category.MEAT))
        assertEquals(Category.SNACKS.emoji, foodEmoji("Pancakes", Category.SNACKS))
    }

    @Test
    fun `unknown names fall back to the category`() {
        assertEquals(Category.CONDIMENTS.emoji, foodEmoji("Mystery sauce", Category.CONDIMENTS))
    }
}
