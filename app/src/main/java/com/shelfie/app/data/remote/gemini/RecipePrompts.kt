package com.shelfie.app.data.remote.gemini

import com.shelfie.app.data.remote.gemini.dto.Content
import com.shelfie.app.data.remote.gemini.dto.GenerateContentRequest
import com.shelfie.app.data.remote.gemini.dto.GenerationConfig
import com.shelfie.app.data.remote.gemini.dto.Part
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.PantryItem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Prompt + structured-output configuration for "rescue" recipes, see [GeminiPrompts]. */
internal object RecipePrompts {

    const val TARGET_COOKING_TIME_MINUTES = 15
    const val MAX_STEPS = 10

    /**
     * Low: the dish should be a known one, not an invention. Variety comes from the ingredients;
     * at 0.8 the model happily put cola into rice.
     */
    private const val TEMPERATURE = 0.4

    val SYSTEM_PROMPT = """
        You are Shelfie's home chef. The user has a few ingredients that will spoil soon and
        wants ONE quick, practical recipe that saves as many of them as sensibly possible.

        Choosing the dish:
        - Cook a real, well-known home-style dish that a home cook would recognise by name,
          preferably from the user's own cuisine (the recipe language tells you which, e.g.
          Turkish home cooking for Turkish). Never invent an odd combination.
        - Use the expiring ingredients that genuinely belong together in that dish. Leave out any
          that would not fit; a good dish with two of them beats a strange dish with all of them.
          Use at least one.
        - Never put drinks (soda, juice, beer), sweets or desserts into a savoury dish, and never
          pair ingredients that clash (e.g. fish with chocolate, cola with rice).
        - Before answering, check: would a home cook in the user's country actually make and enjoy
          this? If not, pick a different dish.

        Rules:
        - Make a dish that suits the requested meal (breakfast, lunch or dinner).
        - Total time (prep + cooking) must be about $TARGET_COOKING_TIME_MINUTES minutes or less.
        - Keep extra ingredients to a minimum: prefer pantry basics most homes have (salt, pepper,
          oil, butter, flour, sugar, eggs, garlic, onion, common spices). Never add more than five.
        - Only use simple equipment: stove, pan, pot, oven, knife, bowl.
        - title: short and appetising, max 60 chars.
        - cooking_time_minutes: realistic total time in whole minutes.
        - servings: how many people the recipe feeds.
        - ingredients: every ingredient the recipe needs, the expiring ones first.
          - name: short generic name. For expiring ingredients, reuse the name you were given.
          - amount: quantity with unit, e.g. "200 g", "1 tbsp", "a pinch".
          - emoji: exactly ONE emoji that best depicts the ingredient (e.g. 🥛 🍅 🧄 🧂).
          - from_pantry: true only for expiring ingredients you were given AND actually used;
            name them exactly as given. Ingredients you left out are not listed at all.
        - steps: 3 to $MAX_STEPS short, clear, imperative steps, one action each.
          - duration_minutes: hands-off waiting time of the step (simmering, baking), else 0.
        - Food safety: cook meat, poultry, fish and eggs through.
    """.trimIndent()

    /** Mirrors [com.shelfie.app.data.remote.gemini.dto.RecipeDto]; all fields required. */
    val responseSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("title") { put("type", "STRING") }
            putJsonObject("cooking_time_minutes") {
                put("type", "INTEGER")
                put("minimum", 1)
            }
            putJsonObject("servings") {
                put("type", "INTEGER")
                put("minimum", 1)
            }
            putJsonObject("ingredients") {
                put("type", "ARRAY")
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") { put("type", "STRING") }
                        putJsonObject("amount") { put("type", "STRING") }
                        putJsonObject("emoji") { put("type", "STRING") }
                        putJsonObject("from_pantry") { put("type", "BOOLEAN") }
                    }
                    val fields = listOf("name", "amount", "emoji", "from_pantry")
                    putJsonArray("required") { fields.forEach { add(it) } }
                    putJsonArray("propertyOrdering") { fields.forEach { add(it) } }
                }
            }
            putJsonObject("steps") {
                put("type", "ARRAY")
                put("maxItems", MAX_STEPS)
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("instruction") { put("type", "STRING") }
                        putJsonObject("duration_minutes") {
                            put("type", "INTEGER")
                            put("minimum", 0)
                        }
                    }
                    val fields = listOf("instruction", "duration_minutes")
                    putJsonArray("required") { fields.forEach { add(it) } }
                    putJsonArray("propertyOrdering") { fields.forEach { add(it) } }
                }
            }
        }
        val fields = listOf("title", "cooking_time_minutes", "servings", "ingredients", "steps")
        putJsonArray("required") { fields.forEach { add(it) } }
        putJsonArray("propertyOrdering") { fields.forEach { add(it) } }
    }

    fun buildRequest(
        ingredients: List<PantryItem>,
        mealType: MealType,
        today: LocalDate,
        locale: Locale,
    ): GenerateContentRequest = GenerateContentRequest(
        systemInstruction = Content(parts = listOf(Part(text = SYSTEM_PROMPT))),
        contents = listOf(
            Content(
                role = "user",
                parts = listOf(Part(text = buildUserPrompt(ingredients, mealType, today, locale))),
            ),
        ),
        generationConfig = GenerationConfig(
            responseMimeType = "application/json",
            responseSchema = responseSchema,
            temperature = TEMPERATURE,
        ),
    )

    internal fun buildUserPrompt(
        ingredients: List<PantryItem>,
        mealType: MealType,
        today: LocalDate,
        locale: Locale,
    ): String = buildString {
        appendLine("Ingredients to use soon (use the ones that fit one good dish):")
        ingredients.forEach { item ->
            val quantity = DecimalFormat("0.##").format(item.quantity)
            val unit = item.unit.name.lowercase(Locale.ROOT)
            appendLine("- ${item.name} ($quantity $unit, ${item.expiryHint(today)})")
        }
        appendLine("Meal: ${mealType.name.lowercase(Locale.ROOT)}.")
        append(
            "Write the whole recipe in ${locale.getDisplayLanguage(Locale.ENGLISH)} " +
                "(${locale.toLanguageTag()}).",
        )
    }

    private fun PantryItem.expiryHint(today: LocalDate): String =
        when (val days = ChronoUnit.DAYS.between(today, expiryDate)) {
            0L -> "expires today"
            1L -> "expires tomorrow"
            else -> "expires in $days days"
        }
}
