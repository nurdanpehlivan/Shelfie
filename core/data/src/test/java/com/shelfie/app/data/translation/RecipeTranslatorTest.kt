package com.shelfie.app.data.translation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeTranslatorTest {

    private val original = """
        {"title":"Ispanaklı menemen","cooking_time_minutes":10,"servings":2,
         "ingredients":[{"name":"Domates","amount":"2 adet","emoji":"🍅","from_pantry":true}],
         "steps":[{"instruction":"Domatesleri doğra.","duration_minutes":3}]}
    """.trimIndent()

    @Test
    fun `a translation keeps the original emojis, times and servings`() {
        val translated = """
            {"title":"Spinach menemen","cooking_time_minutes":99,"servings":9,
             "ingredients":[{"name":"Tomatoes","amount":"2","emoji":"|","from_pantry":false}],
             "steps":[{"instruction":"Dice the tomatoes.","duration_minutes":0}]}
        """.trimIndent()

        val merged = Json.parseToJsonElement(RecipeTranslator.withOriginalDetails(original, translated)).jsonObject
        val ingredient = merged["ingredients"]!!.jsonArray[0].jsonObject
        val step = merged["steps"]!!.jsonArray[0].jsonObject

        assertEquals("Spinach menemen", merged["title"]!!.jsonPrimitive.content)
        assertEquals(10, merged["cooking_time_minutes"]!!.jsonPrimitive.int)
        assertEquals(2, merged["servings"]!!.jsonPrimitive.int)
        assertEquals("Tomatoes", ingredient["name"]!!.jsonPrimitive.content)
        assertEquals("🍅", ingredient["emoji"]!!.jsonPrimitive.content)
        assertEquals("true", ingredient["from_pantry"]!!.jsonPrimitive.content)
        assertEquals("Dice the tomatoes.", step["instruction"]!!.jsonPrimitive.content)
        assertEquals(3, step["duration_minutes"]!!.jsonPrimitive.int)
    }
}
