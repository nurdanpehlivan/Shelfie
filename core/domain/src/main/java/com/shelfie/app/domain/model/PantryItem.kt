package com.shelfie.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PantryItem(
    val id: Long = NEW_ITEM_ID,
    val name: String,
    val category: Category,
    val quantity: Double = 1.0,
    val unit: MeasurementUnit = MeasurementUnit.PIECE,
    val location: StorageLocation = category.defaultLocation,
    /** The emoji the AI chose when scanning; `null` for manual items (the icon comes from the name). */
    val emoji: String? = null,
    /** The expiry was estimated (fresh produce, no legible date) rather than read or chosen. */
    val isExpiryEstimated: Boolean = false,
    /** Local content URI of the shelfie photo this item was detected in. */
    val imageUri: String? = null,
    val purchaseDate: LocalDate,
    val expiryDate: LocalDate,
    /** `true` once the item has left the pantry, whether it was eaten or thrown away. */
    val isConsumed: Boolean = false,
    /** `true` if the item was thrown away instead of eaten; implies [isConsumed]. */
    val isWasted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun daysUntilExpiry(today: LocalDate): Long = ChronoUnit.DAYS.between(today, expiryDate)

    fun isExpired(today: LocalDate): Boolean = expiryDate.isBefore(today)

    fun freshnessStatus(
        today: LocalDate,
        thresholds: FreshnessThresholds = FreshnessThresholds.DEFAULT,
    ): FreshnessStatus = FreshnessStatus.from(expiryDate, today, thresholds)

    companion object {
        /** Room treats 0 as "not set" for auto-generated primary keys. */
        const val NEW_ITEM_ID = 0L
    }
}
