package com.shelfie.app.data.mapper

import com.shelfie.app.domain.model.PantryScanException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRecipeParserTest {

    private val parser = GeminiRecipeParser()

    @Test
    fun `maps a valid recipe and splits rescued from extra ingredients`() {
        val json = """
            {"title":"Tomato scramble","cooking_time_minutes":12,"servings":2,
             "ingredients":[
               {"name":"Tomato","amount":"3 pcs","emoji":"🍅","from_pantry":true},
               {"name":"Egg","amount":"3 pcs","emoji":"🥚","from_pantry":false}
             ],
             "steps":[
               {"instruction":"Dice the tomatoes.","duration_minutes":0},
               {"instruction":"Cook with the eggs.","duration_minutes":4}
             ]}
        """.trimIndent()

        val recipe = parser.parse(json)

        assertEquals("Tomato scramble", recipe.title)
        assertEquals(12, recipe.cookingTimeMinutes)
        assertEquals(2, recipe.servings)
        assertEquals(listOf("Tomato"), recipe.rescuedIngredients.map { it.name })
        assertEquals(listOf("Egg"), recipe.extraIngredients.map { it.name })
        assertEquals("🍅", recipe.ingredients[0].emoji)
        assertEquals(4, recipe.steps[1].durationMinutes)
    }

    @Test
    fun `tolerates fences, drops blank entries and clamps numbers`() {
        val raw = """
            ```json
            {"title":"  Quick   soup ","cooking_time_minutes":"-5","servings":99,
             "ingredients":[{"name":" "},{"name":"Milk","emoji":"milk"},{"name":"milk"}],
             "steps":[{"instruction":""},{"instruction":"Heat it.","duration_minutes":-1}]}
            ```
        """.trimIndent()

        val recipe = parser.parse(raw)

        assertEquals("Quick soup", recipe.title)
        assertEquals(1, recipe.cookingTimeMinutes)
        assertEquals(GeminiRecipeParser.MAX_SERVINGS, recipe.servings)
        val milk = recipe.ingredients.single()
        assertNull(milk.emoji)
        assertEquals("", milk.amount)
        assertEquals(false, milk.isFromPantry)
        assertEquals(0, recipe.steps.single().durationMinutes)
    }

    @Test
    fun `fails without a title or without steps`() {
        assertMalformed("""{"title":"","steps":[{"instruction":"Cook."}]}""")
        assertMalformed("""{"title":"Soup","steps":[]}""")
        assertMalformed("not json at all")
    }

    private fun assertMalformed(raw: String) {
        val error = runCatching { parser.parse(raw) }.exceptionOrNull()
        assertTrue("Expected MalformedResponse for $raw", error is PantryScanException.MalformedResponse)
    }
}
