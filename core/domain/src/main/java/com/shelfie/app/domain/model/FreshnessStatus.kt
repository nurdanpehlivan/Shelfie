package com.shelfie.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How urgently an item needs to be used, relative to the user's [FreshnessThresholds].
 *
 * - [CRITICAL]: expired, or expires within [FreshnessThresholds.criticalDays] days.
 * - [WARNING]: expires within [FreshnessThresholds.warningDays] days.
 * - [FRESH]: everything else.
 */
enum class FreshnessStatus {
    CRITICAL,
    WARNING,
    FRESH;

    companion object {
        fun from(
            expiryDate: LocalDate,
            today: LocalDate,
            thresholds: FreshnessThresholds = FreshnessThresholds.DEFAULT,
        ): FreshnessStatus {
            val daysLeft = ChronoUnit.DAYS.between(today, expiryDate)
            return when {
                daysLeft <= thresholds.criticalDays -> CRITICAL
                daysLeft <= thresholds.warningDays -> WARNING
                else -> FRESH
            }
        }

        /**
         * Inclusive expiry-date window for [status] relative to [today].
         * A `null` bound means the window is open on that side.
         */
        fun expiryWindow(
            status: FreshnessStatus,
            today: LocalDate,
            thresholds: FreshnessThresholds = FreshnessThresholds.DEFAULT,
        ): ExpiryWindow =
            when (status) {
                CRITICAL -> ExpiryWindow(
                    start = null,
                    end = today.plusDays(thresholds.criticalDays),
                )
                WARNING -> ExpiryWindow(
                    start = today.plusDays(thresholds.criticalDays + 1),
                    end = today.plusDays(thresholds.warningDays),
                )
                FRESH -> ExpiryWindow(
                    start = today.plusDays(thresholds.warningDays + 1),
                    end = null,
                )
            }
    }
}

/** Day limits of the freshness groups; the warning band is always [WARNING_BAND_DAYS] wide. */
data class FreshnessThresholds(val criticalDays: Long) {

    val warningDays: Long get() = criticalDays + WARNING_BAND_DAYS

    companion object {
        const val WARNING_BAND_DAYS = 3L
        const val MIN_CRITICAL_DAYS = 1L
        const val MAX_CRITICAL_DAYS = 4L

        /** "Critical" means 0–2 days left, "heads up" 3–5 days. */
        val DEFAULT = FreshnessThresholds(criticalDays = 2)
    }
}

data class ExpiryWindow(val start: LocalDate?, val end: LocalDate?)
