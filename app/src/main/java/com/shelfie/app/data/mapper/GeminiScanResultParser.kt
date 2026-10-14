package com.shelfie.app.data.mapper

import com.shelfie.app.data.remote.gemini.GeminiPrompts
import com.shelfie.app.data.remote.gemini.dto.ScanCategory
import com.shelfie.app.data.remote.gemini.dto.ScanResultDto
import com.shelfie.app.data.remote.gemini.dto.ScannedItemDto
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.PantryScanException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Turns the model's JSON answer into unsaved [PantryItem]s.
 *
 * Model output is treated as untrusted input:
 * - tolerates markdown code fences, surrounding prose, unknown keys and quoted numbers;
 * - drops entries with a blank name or a confidence below [MIN_CONFIDENCE];
 * - clamps shelf life to a sane range, falling back to the category default when missing;
 * - merges duplicates of the same product into one item with a higher quantity.
 *
 * Only a structurally unusable answer fails (with [PantryScanException.MalformedResponse]);
 * individual bad entries are skipped.
 */
class GeminiScanResultParser @Inject constructor(
    private val clock: Clock,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(rawJson: String, imageUri: String?): List<PantryItem> {
        val dto = try {
            json.decodeFromString<ScanResultDto>(rawJson.extractJsonObject())
        } catch (e: SerializationException) {
            throw PantryScanException.MalformedResponse("Model output is not valid scan JSON", e)
        } catch (e: IllegalArgumentException) {
            throw PantryScanException.MalformedResponse("Model output is not valid scan JSON", e)
        }

        val today = LocalDate.now(clock)
        val now = clock.instant()

        return dto.items.orEmpty()
            .asSequence()
            .take(MAX_ITEMS)
            .mapNotNull { it.validate() }
            .groupBy { it.name.lowercase() to it.category }
            .map { (_, duplicates) ->
                val first = duplicates.first()
                PantryItem(
                    name = first.name,
                    category = first.category,
                    quantity = duplicates.size.toDouble(),
                    unit = MeasurementUnit.PIECE,
                    imageUri = imageUri,
                    purchaseDate = today,
                    // Be conservative: the soonest estimate among duplicates wins.
                    expiryDate = today.plusDays(duplicates.minOf { it.shelfLifeDays }.toLong()),
                    createdAt = now,
                    updatedAt = now,
                )
            }
    }

    private class ValidItem(val name: String, val category: Category, val shelfLifeDays: Int)

    private fun ScannedItemDto.validate(): ValidItem? {
        val cleanName = name
            ?.replace(WHITESPACE, " ")
            ?.trim()
            ?.take(MAX_NAME_LENGTH)
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val score = confidence?.takeIf { it.isFinite() } ?: return null
        if (score < MIN_CONFIDENCE) return null

        val domainCategory = category.toDomainCategory()
        val shelfLife = estimatedShelfLifeDays
            ?.takeIf { it >= 0 }
            ?.coerceAtMost(GeminiPrompts.MAX_SHELF_LIFE_DAYS)
            ?: domainCategory.defaultShelfLifeDays

        return ValidItem(cleanName, domainCategory, shelfLife)
    }

    /** Strips code fences or chatter around the JSON object, if the model added any. */
    private fun String.extractJsonObject(): String {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        if (start == -1 || end <= start) {
            throw PantryScanException.MalformedResponse("Model output contains no JSON object")
        }
        return substring(start, end + 1)
    }

    companion object {
        const val MIN_CONFIDENCE = 0.5
        const val MAX_ITEMS = 100
        const val MAX_NAME_LENGTH = 60
        private val WHITESPACE = Regex("\\s+")

        /**
         * The scan schema is coarser than the domain model. PRODUCE has no single domain
         * counterpart, so it maps to VEGETABLES; PANTRY (dry/shelf-stable goods) maps to GRAINS.
         * Unknown values fall back to [Category.OTHER].
         */
        internal fun String?.toDomainCategory(): Category =
            when (ScanCategory.entries.firstOrNull { it.name.equals(this?.trim(), ignoreCase = true) }) {
                ScanCategory.DAIRY -> Category.DAIRY
                ScanCategory.PRODUCE -> Category.VEGETABLES
                ScanCategory.MEAT -> Category.MEAT
                ScanCategory.BAKERY -> Category.BAKERY
                ScanCategory.PANTRY -> Category.GRAINS
                null -> Category.fromName(this?.trim())
            }
    }
}
