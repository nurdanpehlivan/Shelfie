package com.shelfie.app.data.remote.gemini.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The JSON the model is instructed to return (see `RecipePrompts.responseSchema`).
 *
 * Like [ScanResultDto], every field is nullable so the parser can validate model output itself.
 */
@Serializable
data class RecipeDto(
    val title: String? = null,
    @SerialName("cooking_time_minutes")
    val cookingTimeMinutes: Int? = null,
    val servings: Int? = null,
    val ingredients: List<RecipeIngredientDto>? = null,
    val steps: List<RecipeStepDto>? = null,
)

@Serializable
data class RecipeIngredientDto(
    val name: String? = null,
    val amount: String? = null,
    val emoji: String? = null,
    @SerialName("from_pantry")
    val fromPantry: Boolean? = null,
)

@Serializable
data class RecipeStepDto(
    val instruction: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null,
)
