package com.shelfie.app.data.remote.gemini

import com.shelfie.app.data.remote.gemini.dto.Content
import com.shelfie.app.data.remote.gemini.dto.GenerateContentRequest
import com.shelfie.app.data.remote.gemini.dto.GenerationConfig
import com.shelfie.app.data.remote.gemini.dto.Part
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Locale

/** Prompt + structured-output configuration for translating item names, see [GeminiPrompts]. */
internal object TranslationPrompts {

    private const val TEMPERATURE = 0.0

    val SYSTEM_PROMPT = """
        You translate the names of food and grocery items in a home pantry app.
        - Translate each name into the requested language as a native speaker would write it on a
          shopping list: short, generic, same meaning ("Çürük armut" -> "Rotten pear").
        - Keep brand names as they are. Keep the capitalisation style of the input.
        - A name already in the requested language is returned unchanged.
        - Return exactly one translation per input name, in the same order.
    """.trimIndent()

    val responseSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("translations") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
            }
        }
        putJsonArray("required") { add("translations") }
    }

    fun buildRequest(names: List<String>, locale: Locale): GenerateContentRequest = GenerateContentRequest(
        systemInstruction = Content(parts = listOf(Part(text = SYSTEM_PROMPT))),
        contents = listOf(
            Content(
                role = "user",
                parts = listOf(
                    Part(
                        text = "Translate into ${locale.getDisplayLanguage(Locale.ENGLISH)} " +
                            "(${locale.toLanguageTag()}):\n" +
                            buildJsonArray { names.forEach { add(it) } }.toString(),
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
