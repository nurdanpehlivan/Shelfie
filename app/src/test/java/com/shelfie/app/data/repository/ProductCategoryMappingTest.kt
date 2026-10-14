package com.shelfie.app.data.repository

import com.shelfie.app.data.repository.ProductLookupRepositoryImpl.Companion.categoryFromTags
import com.shelfie.app.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductCategoryMappingTest {

    @Test
    fun `uses the most specific tag`() {
        val milk = listOf("en:dairies", "en:milks", "en:semi-skimmed-milks")
        assertEquals(Category.DAIRY, categoryFromTags(milk))
    }

    @Test
    fun `broad umbrella tags don't win over specific ones`() {
        // Every plant-based product carries "...-and-beverages"; tomatoes are still vegetables.
        val tomatoes = listOf("en:plant-based-foods-and-beverages", "en:plant-based-foods", "en:vegetables", "en:tomatoes")
        assertEquals(Category.VEGETABLES, categoryFromTags(tomatoes))
    }

    @Test
    fun `storage beats food type for shelf life`() {
        val frozenFish = listOf("en:seafood", "en:fishes", "en:frozen-foods")
        assertEquals(Category.FROZEN, categoryFromTags(frozenFish))
    }

    @Test
    fun `falls back to OTHER`() {
        assertEquals(Category.OTHER, categoryFromTags(emptyList()))
        assertEquals(Category.OTHER, categoryFromTags(listOf("en:something-unusual")))
    }
}
