package com.shelfie.app.data.remote.gemini.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The JSON the model is instructed to return (see `GeminiPrompts.responseSchema`).
 *
 * Every field is nullable on purpose: the schema is enforced server-side, but the parser still
 * treats model output as untrusted and validates each field itself.
 */
@Serializable
data class ScanResultDto(
    val items: List<ScannedItemDto>? = null,
)

@Serializable
data class ScannedItemDto(
    val name: String? = null,
    val category: String? = null,
    val emoji: String? = null,
    val storage: String? = null,
    @SerialName("expiry_source")
    val expirySource: String? = null,
    @SerialName("estimated_shelf_life_days")
    val estimatedShelfLifeDays: Int? = null,
    /** "ok", "spoiled" or "expired"; missing in answers to older prompts, read as "ok". */
    val condition: String? = null,
    @SerialName("condition_note")
    val conditionNote: String? = null,
    val confidence: Double? = null,
)

/**
 * The coarse categories older prompts asked for; still understood so a cached or older answer
 * parses. The model now picks from the domain `Category` itself.
 */
enum class ScanCategory {
    DAIRY,
    PRODUCE,
    MEAT,
    BAKERY,
    PANTRY,
}
