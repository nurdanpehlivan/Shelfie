package com.shelfie.app.data.mapper

import com.shelfie.app.data.remote.gemini.GeminiPrompts
import com.shelfie.app.data.remote.gemini.dto.ScanCategory
import com.shelfie.app.data.remote.gemini.dto.ScanResultDto
import com.shelfie.app.data.remote.gemini.dto.ScannedItemDto
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.model.ShelfLife
import com.shelfie.app.domain.model.StorageLocation
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
 * - clamps shelf life to a sane range; an estimated one (no legible date) is also capped by
 *   [ShelfLife], so a lettuce never gets three weeks, and falls back to it when missing;
 * - keeps the model's emoji only if it is one (no letters or digits);
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
                // Be conservative: the soonest date among duplicates wins.
                val soonest = duplicates.minBy { it.shelfLifeDays }
                PantryItem(
                    name = first.name,
                    category = first.category,
                    quantity = duplicates.size.toDouble(),
                    unit = MeasurementUnit.PIECE,
                    location = first.location,
                    emoji = duplicates.firstNotNullOfOrNull { it.emoji },
                    isExpiryEstimated = soonest.isEstimated,
                    imageUri = imageUri,
                    purchaseDate = today,
                    expiryDate = today.plusDays(soonest.shelfLifeDays.toLong()),
                    createdAt = now,
                    updatedAt = now,
                )
            }
    }

    private class ValidItem(
        val name: String,
        val category: Category,
        val location: StorageLocation,
        val emoji: String?,
        val shelfLifeDays: Int,
        val isEstimated: Boolean,
    )

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
        val location = StorageLocation.fromNameOrNull(storage?.trim()) ?: domainCategory.defaultLocation
        val isEstimated = !expirySource.equals(GeminiPrompts.EXPIRY_FROM_LABEL, ignoreCase = true)
        val modelDays = estimatedShelfLifeDays
            ?.takeIf { it >= 0 }
            ?.coerceAtMost(GeminiPrompts.MAX_SHELF_LIFE_DAYS)
        val shelfLife = when {
            !isEstimated && modelDays != null -> modelDays
            else -> listOfNotNull(modelDays, ShelfLife.knownDays(cleanName, location)).minOrNull()
                ?: ShelfLife.estimateDays(cleanName, domainCategory, location)
        }

        return ValidItem(cleanName, domainCategory, location, emoji.validEmoji(), shelfLife, isEstimated)
    }

    /** A short run of symbols; a word or a number means the model ignored the instruction. */
    private fun String?.validEmoji(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_EMOJI_LENGTH && !it.any(Char::isLetterOrDigit) }

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

        /** One emoji, including skin-tone or ZWJ sequences. */
        const val MAX_EMOJI_LENGTH = 16
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
