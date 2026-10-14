package com.shelfie.app.data.mapper

import com.shelfie.app.data.remote.gemini.RecipePrompts
import com.shelfie.app.data.remote.gemini.dto.RecipeDto
import com.shelfie.app.data.remote.gemini.dto.RecipeIngredientDto
import com.shelfie.app.data.remote.gemini.dto.RecipeStepDto
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.model.Recipe
import com.shelfie.app.domain.model.RecipeIngredient
import com.shelfie.app.domain.model.RecipeStep
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Turns the model's JSON answer into a [Recipe], treating it as untrusted input like
 * [GeminiScanResultParser]: blank entries are dropped, numbers are clamped and an unusable emoji
 * becomes `null`. Only a recipe without a title or without any step fails, with
 * [PantryScanException.MalformedResponse].
 */
class GeminiRecipeParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(rawJson: String): Recipe {
        val dto = try {
            json.decodeFromString<RecipeDto>(rawJson.extractJsonObject())
        } catch (e: SerializationException) {
            throw PantryScanException.MalformedResponse("Model output is not valid recipe JSON", e)
        } catch (e: IllegalArgumentException) {
            throw PantryScanException.MalformedResponse("Model output is not valid recipe JSON", e)
        }

        val title = dto.title.clean(MAX_TITLE_LENGTH)
            ?: throw PantryScanException.MalformedResponse("Recipe has no title")
        val steps = dto.steps.orEmpty()
            .asSequence()
            .mapNotNull { it.validate() }
            .take(RecipePrompts.MAX_STEPS)
            .toList()
        if (steps.isEmpty()) throw PantryScanException.MalformedResponse("Recipe has no steps")

        return Recipe(
            title = title,
            cookingTimeMinutes = dto.cookingTimeMinutes
                ?.coerceIn(1, MAX_COOKING_TIME_MINUTES)
                ?: RecipePrompts.TARGET_COOKING_TIME_MINUTES,
            servings = dto.servings?.coerceIn(1, MAX_SERVINGS) ?: 1,
            ingredients = dto.ingredients.orEmpty()
                .asSequence()
                .mapNotNull { it.validate() }
                .distinctBy { it.name.lowercase() }
                .take(MAX_INGREDIENTS)
                .toList(),
            steps = steps,
        )
    }

    private fun RecipeIngredientDto.validate(): RecipeIngredient? {
        val cleanName = name.clean(MAX_NAME_LENGTH) ?: return null
        return RecipeIngredient(
            name = cleanName,
            amount = amount.clean(MAX_AMOUNT_LENGTH).orEmpty(),
            emoji = emoji?.trim()?.takeIf { it.looksLikeEmoji() },
            isFromPantry = fromPantry == true,
        )
    }

    private fun RecipeStepDto.validate(): RecipeStep? {
        val text = instruction.clean(MAX_INSTRUCTION_LENGTH) ?: return null
        return RecipeStep(
            instruction = text,
            durationMinutes = durationMinutes?.coerceIn(0, MAX_COOKING_TIME_MINUTES) ?: 0,
        )
    }

    private fun String?.clean(maxLength: Int): String? =
        this?.replace(WHITESPACE, " ")?.trim()?.take(maxLength)?.takeIf { it.isNotEmpty() }

    /**
     * A single emoji can be several code points (skin tones, ZWJ sequences) but contains no
     * letters or digits; anything longer or wordy is the model ignoring the instruction.
     */
    private fun String.looksLikeEmoji(): Boolean =
        isNotEmpty() && length <= MAX_EMOJI_LENGTH && none { it.isLetterOrDigit() || it.isWhitespace() }

    private fun String.extractJsonObject(): String {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        if (start == -1 || end <= start) {
            throw PantryScanException.MalformedResponse("Model output contains no JSON object")
        }
        return substring(start, end + 1)
    }

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val MAX_NAME_LENGTH = 40
        const val MAX_AMOUNT_LENGTH = 30
        const val MAX_INSTRUCTION_LENGTH = 400
        const val MAX_INGREDIENTS = 12
        const val MAX_COOKING_TIME_MINUTES = 180
        const val MAX_SERVINGS = 12
        private const val MAX_EMOJI_LENGTH = 16
        private val WHITESPACE = Regex("\\s+")
    }
}
