package com.shelfie.app.data.remote.gemini

import com.shelfie.app.data.remote.gemini.dto.Content
import com.shelfie.app.data.remote.gemini.dto.GenerateContentRequest
import com.shelfie.app.data.remote.gemini.dto.GenerationConfig
import com.shelfie.app.data.remote.gemini.dto.InlineData
import com.shelfie.app.data.remote.gemini.dto.Part
import com.shelfie.app.data.remote.gemini.dto.ScanCategory
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Locale

/**
 * Prompt + structured-output configuration for pantry photo analysis.
 *
 * Structured output (`responseMimeType` + `responseSchema`) makes Gemini emit only JSON matching
 * the schema; the system prompt explains the semantics of each field.
 */
internal object GeminiPrompts {

    const val MAX_SHELF_LIFE_DAYS = 3650

    private const val TEMPERATURE = 0.1

    val SYSTEM_PROMPT = """
        You are Shelfie's food inventory assistant. You receive ONE photo of a fridge, a pantry
        shelf, or a grocery receipt and list the food items in it.

        Rules:
        - List only food and drink items. Ignore containers, appliances, people and non-food goods.
        - Receipts: decode abbreviated product lines into a short generic product name
          ("SUT 1L YRM YGL" -> "Semi-skimmed milk"). Skip prices, totals, discounts and store info.
        - One entry per visible item; if the same product appears several times, repeat the entry.
        - name: short, generic, singular, no brand unless it identifies the product type, max 60 chars.
        - category, one of:
          DAIRY (milk, cheese, yogurt, eggs, butter),
          PRODUCE (fresh fruit, vegetables, herbs),
          MEAT (meat, poultry, fish, seafood, deli meat),
          BAKERY (bread, pastries, cakes),
          PANTRY (everything else: dry goods, canned food, sauces, snacks, drinks, frozen food).
        - estimated_shelf_life_days: whole days from today until the item should be used, assuming
          proper storage (refrigerated if seen in a fridge, unopened if it looks sealed). If an
          expiry date is legible on the package, use it. Be conservative when unsure.
        - confidence: 0.0-1.0, how sure you are that the item exists and is identified correctly.
        - If no food is visible, or the photo is unrelated, return {"items": []}.
        - Never invent items you cannot see.
    """.trimIndent()

    /** Mirrors [com.shelfie.app.data.remote.gemini.dto.ScanResultDto]; all fields required. */
    val responseSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("items") {
                put("type", "ARRAY")
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") {
                            put("type", "STRING")
                            put("description", "Short generic product name.")
                        }
                        putJsonObject("category") {
                            put("type", "STRING")
                            put("format", "enum")
                            putJsonArray("enum") { ScanCategory.entries.forEach { add(it.name) } }
                        }
                        putJsonObject("estimated_shelf_life_days") {
                            put("type", "INTEGER")
                            put("minimum", 0)
                            put("maximum", MAX_SHELF_LIFE_DAYS)
                        }
                        putJsonObject("confidence") {
                            put("type", "NUMBER")
                            put("minimum", 0)
                            put("maximum", 1)
                        }
                    }
                    val fields = listOf("name", "category", "estimated_shelf_life_days", "confidence")
                    putJsonArray("required") { fields.forEach { add(it) } }
                    putJsonArray("propertyOrdering") { fields.forEach { add(it) } }
                }
            }
        }
        putJsonArray("required") { add("items") }
    }

    fun buildRequest(
        imageBase64: String,
        imageMimeType: String,
        locale: Locale,
    ): GenerateContentRequest = GenerateContentRequest(
        systemInstruction = Content(parts = listOf(Part(text = SYSTEM_PROMPT))),
        contents = listOf(
            Content(
                role = "user",
                parts = listOf(
                    Part(inlineData = InlineData(mimeType = imageMimeType, data = imageBase64)),
                    Part(
                        text = "List the food items in this photo. Write item names in " +
                            "${locale.getDisplayLanguage(Locale.ENGLISH)} " +
                            "(${locale.toLanguageTag()}).",
                    ),
                ),
            ),
        ),
        generationConfig = GenerationConfig(
            responseMimeType = "application/json",
            responseSchema = responseSchema,
            temperature = TEMPERATURE,
        ),
    )
}
