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
    @SerialName("estimated_shelf_life_days")
    val estimatedShelfLifeDays: Int? = null,
    val confidence: Double? = null,
)

/** Coarse categories the model chooses from; mapped onto the richer domain `Category`. */
enum class ScanCategory {
    DAIRY,
    PRODUCE,
    MEAT,
    BAKERY,
    PANTRY,
}
