package com.shelfie.app.data.mapper

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.ItemCondition
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GeminiScanResultParserTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 7)
    private val parser = GeminiScanResultParser(clock)

    @Test
    fun `maps valid items to unsaved pantry items`() {
        val json = """
            {"items":[
              {"name":"Milk","category":"DAIRY","estimated_shelf_life_days":5,"confidence":0.95},
              {"name":"Sourdough","category":"BAKERY","estimated_shelf_life_days":3,"confidence":0.8}
            ]}
        """.trimIndent()

        val items = parser.parseItems(json, imageUri = "content://photo/1")

        assertEquals(2, items.size)
        with(items[0]) {
            assertEquals("Milk", name)
            assertEquals(Category.DAIRY, category)
            assertEquals(today, purchaseDate)
            assertEquals(today.plusDays(5), expiryDate)
            assertEquals("content://photo/1", imageUri)
            assertEquals(0L, id)
            assertEquals(clock.instant(), createdAt)
        }
    }

    @Test
    fun `tolerates code fences, unknown keys and quoted numbers`() {
        val raw = """
            ```json
            {"items":[{"name":"  Apple ","category":"produce","estimated_shelf_life_days":"10",
              "confidence":"0.9","brand":"x"}],"extra":true}
            ```
        """.trimIndent()

        val item = parser.parseItems(raw, imageUri = null).single()

        assertEquals("Apple", item.name)
        assertEquals(Category.VEGETABLES, item.category)
        assertEquals(today.plusDays(10), item.expiryDate)
    }

    @Test
    fun `drops blank names and low or missing confidence`() {
        val json = """
            {"items":[
              {"name":"","category":"DAIRY","estimated_shelf_life_days":5,"confidence":0.9},
              {"name":"Ghost","category":"DAIRY","estimated_shelf_life_days":5,"confidence":0.2},
              {"name":"NoScore","category":"DAIRY","estimated_shelf_life_days":5},
              {"name":"Rice","category":"PANTRY","estimated_shelf_life_days":300,"confidence":0.7}
            ]}
        """.trimIndent()

        val item = parser.parseItems(json, imageUri = null).single()

        assertEquals("Rice", item.name)
        assertEquals(Category.GRAINS, item.category)
    }

    @Test
    fun `falls back to category default and clamps shelf life`() {
        val json = """
            {"items":[
              {"name":"Steak","category":"MEAT","confidence":0.9},
              {"name":"Honey","category":"PANTRY","estimated_shelf_life_days":999999,"confidence":0.9},
              {"name":"Mystery","category":"SPACE_FOOD","estimated_shelf_life_days":-4,"confidence":0.9}
            ]}
        """.trimIndent()

        val items = parser.parseItems(json, imageUri = null).associateBy { it.name }

        assertEquals(today.plusDays(Category.MEAT.defaultShelfLifeDays.toLong()), items.getValue("Steak").expiryDate)
        assertEquals(today.plusDays(3650), items.getValue("Honey").expiryDate)
        assertEquals(Category.OTHER, items.getValue("Mystery").category)
        assertEquals(today.plusDays(Category.OTHER.defaultShelfLifeDays.toLong()), items.getValue("Mystery").expiryDate)
    }

    @Test
    fun `merges duplicates into quantity using the soonest expiry`() {
        val json = """
            {"items":[
              {"name":"Egg","category":"DAIRY","estimated_shelf_life_days":14,"confidence":0.9},
              {"name":"egg","category":"DAIRY","estimated_shelf_life_days":10,"confidence":0.9}
            ]}
        """.trimIndent()

        val item = parser.parseItems(json, imageUri = null).single()

        assertEquals(2.0, item.quantity, 0.0)
        assertEquals(today.plusDays(10), item.expiryDate)
    }

    @Test
    fun `keeps the AI's emoji, place and a printed date`() {
        val json = """
            {"items":[{"name":"Reçel","category":"CONDIMENTS","emoji":"🫙","storage":"PANTRY",
              "expiry_source":"label","estimated_shelf_life_days":200,"confidence":0.9}]}
        """.trimIndent()

        val item = parser.parseItems(json, imageUri = null).single()

        assertEquals("🫙", item.emoji)
        assertEquals(StorageLocation.PANTRY, item.location)
        assertEquals(today.plusDays(200), item.expiryDate)
        assertFalse(item.isExpiryEstimated)
    }

    @Test
    fun `an estimated date is capped by the shelf-life table`() {
        val json = """
            {"items":[{"name":"Marul","category":"VEGETABLES","emoji":"🥬","storage":"FRIDGE",
              "expiry_source":"estimate","estimated_shelf_life_days":20,"confidence":0.9}]}
        """.trimIndent()

        val item = parser.parseItems(json, imageUri = null).single()

        assertEquals(today.plusDays(5), item.expiryDate)
        assertTrue(item.isExpiryEstimated)
    }

    @Test
    fun `words instead of an emoji are dropped`() {
        val json = """
            {"items":[{"name":"Milk","category":"DAIRY","emoji":"milk","confidence":0.9}]}
        """.trimIndent()

        assertNull(parser.parseItems(json, imageUri = null).single().emoji)
    }

    @Test
    fun `empty or missing items yields empty list`() {
        assertTrue(parser.parseItems("""{"items":[]}""", imageUri = null).isEmpty())
        assertTrue(parser.parseItems("{}", imageUri = null).isEmpty())
    }

    @Test(expected = PantryScanException.MalformedResponse::class)
    fun `non-json output throws malformed response`() {
        parser.parse("Sorry, I can't help with that.", imageUri = null)
    }

    @Test(expected = PantryScanException.MalformedResponse::class)
    fun `wrong root shape throws malformed response`() {
        parser.parse("""{"items":"milk"}""", imageUri = null)
    }

    @Test
    fun `spoiled items are flagged with a note, kept apart from fresh ones and given no days`() {
        val json = """
            {"items":[
              {"name":"Domates","category":"VEGETABLES","emoji":"🍅","storage":"FRIDGE","expiry_source":"estimate",
               "estimated_shelf_life_days":5,"condition":"spoiled","condition_note":"üstünde küf","confidence":0.9},
              {"name":"Domates","category":"VEGETABLES","emoji":"🍅","storage":"FRIDGE","expiry_source":"estimate",
               "estimated_shelf_life_days":5,"condition":"ok","condition_note":"","confidence":0.9}
            ]}
        """.trimIndent()

        val (spoiled, fresh) = parser.parse(json, imageUri = null).partition { it.isFlagged }

        assertEquals(ItemCondition.SPOILED, spoiled.single().condition)
        assertEquals("üstünde küf", spoiled.single().conditionNote)
        assertEquals(LocalDate.now(clock), spoiled.single().item.expiryDate)
        assertNull(fresh.single().conditionNote)
    }

    @Test
    fun `an answer without condition reads as ok`() {
        val json = """{"items":[{"name":"Milk","category":"DAIRY","confidence":0.9}]}"""

        assertEquals(ItemCondition.OK, parser.parse(json, imageUri = null).single().condition)
    }

    private fun GeminiScanResultParser.parseItems(raw: String, imageUri: String?) = parse(raw, imageUri).map { it.item }
}
