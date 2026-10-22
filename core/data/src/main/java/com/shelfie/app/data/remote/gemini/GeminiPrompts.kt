package com.shelfie.app.data.remote.gemini

import com.shelfie.app.data.remote.gemini.dto.Content
import com.shelfie.app.data.remote.gemini.dto.GenerateContentRequest
import com.shelfie.app.data.remote.gemini.dto.GenerationConfig
import com.shelfie.app.data.remote.gemini.dto.InlineData
import com.shelfie.app.data.remote.gemini.dto.Part
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.StorageLocation
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

    const val EXPIRY_FROM_LABEL = "label"
    const val EXPIRY_ESTIMATED = "estimate"

    const val CONDITION_OK = "ok"
    const val CONDITION_SPOILED = "spoiled"
    const val CONDITION_EXPIRED = "expired"

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
          DAIRY (milk, cheese, yogurt, eggs, butter, cream),
          MEAT (meat, poultry, deli meat, sausages),
          SEAFOOD (fish, shrimp, shellfish),
          FRUITS (fresh fruit),
          VEGETABLES (fresh vegetables, herbs, potatoes, onions),
          BAKERY (bread, pastries, cakes),
          GRAINS (rice, pasta, flour, cereal, oats, legumes),
          CANNED (tins and jars of cooked food, tomato paste),
          FROZEN (frozen ready meals and frozen vegetables),
          BEVERAGES (water, juice, soda, coffee, tea),
          CONDIMENTS (sauces, ketchup, mayonnaise, jam, honey, oil, spices),
          SNACKS (chips, biscuits, chocolate, nuts),
          OTHER (anything else).
        - emoji: exactly ONE emoji that best shows the product itself, e.g. 🍅 tomato, 🫙 jam,
          🍯 honey, 🥫 tomato paste or ketchup, 🥣 cereal, 🥤 cola, 🧃 juice. Never a face or a flag.
        - storage: where the item is kept: FRIDGE (seen in a fridge), FREEZER (freezer drawer or
          frozen), PANTRY (shelf, cupboard, counter). For receipts, where it normally belongs.
        - expiry_source: "label" ONLY if you can actually read an expiry or best-before date
          printed on this very item in the photo; otherwise "estimate". Never guess a printed date.
        - estimated_shelf_life_days: whole days from today until the item should be used.
          With a label, count to the printed date. Without one, use the typical home storage time
          for that product kept in that storage, unopened if it looks sealed: fresh produce
          usually lasts 2-7 days in the fridge (berries 2-3, leafy greens 3-5, apples and carrots
          several weeks); fresh meat and fish 1-3 days; opened milk about 5. For produce, look at
          ripeness and shorten the estimate if it has spots, bruises or wilting. Be conservative.
        - condition: "spoiled" ONLY with clear visible signs the food went bad: mould, rot, slime,
          dark mushy spots, a bulging or rusted can, a torn or swollen package. "expired" ONLY if a
          printed date you can read on the item has already passed. Otherwise "ok". Natural
          ripeness (brown banana spots, a soft avocado) is "ok". When unsure, "ok".
        - condition_note: for "spoiled" or "expired", a few words on what you see, in the same
          language as the item names ("mould on top", "date passed: 12.10"); "" for "ok".
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
                            putJsonArray("enum") { Category.entries.forEach { add(it.name) } }
                        }
                        putJsonObject("emoji") { put("type", "STRING") }
                        putJsonObject("storage") {
                            put("type", "STRING")
                            put("format", "enum")
                            putJsonArray("enum") { StorageLocation.entries.forEach { add(it.name) } }
                        }
                        putJsonObject("expiry_source") {
                            put("type", "STRING")
                            put("format", "enum")
                            putJsonArray("enum") {
                                add(EXPIRY_FROM_LABEL)
                                add(EXPIRY_ESTIMATED)
                            }
                        }
                        putJsonObject("estimated_shelf_life_days") {
                            put("type", "INTEGER")
                            put("minimum", 0)
                            put("maximum", MAX_SHELF_LIFE_DAYS)
                        }
                        putJsonObject("condition") {
                            put("type", "STRING")
                            put("format", "enum")
                            putJsonArray("enum") {
                                add(CONDITION_OK)
                                add(CONDITION_SPOILED)
                                add(CONDITION_EXPIRED)
                            }
                        }
                        putJsonObject("condition_note") { put("type", "STRING") }
                        putJsonObject("confidence") {
                            put("type", "NUMBER")
                            put("minimum", 0)
                            put("maximum", 1)
                        }
                    }
                    val fields = listOf(
                        "name",
                        "category",
                        "emoji",
                        "storage",
                        "expiry_source",
                        "estimated_shelf_life_days",
                        "condition",
                        "condition_note",
                        "confidence",
                    )
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
