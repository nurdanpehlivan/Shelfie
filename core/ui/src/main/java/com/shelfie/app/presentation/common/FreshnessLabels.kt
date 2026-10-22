package com.shelfie.app.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds

@get:StringRes
val FreshnessStatus.titleRes: Int
    get() = when (this) {
        FreshnessStatus.CRITICAL -> R.string.freshness_critical
        FreshnessStatus.WARNING -> R.string.freshness_warning
        FreshnessStatus.FRESH -> R.string.freshness_fresh
    }

/** Day range the group covers with the user's [thresholds], e.g. "0–2 days". */
@Composable
fun FreshnessStatus.rangeText(thresholds: FreshnessThresholds): String = when (this) {
    FreshnessStatus.CRITICAL -> stringResource(R.string.freshness_critical_range, thresholds.criticalDays)
    FreshnessStatus.WARNING -> stringResource(
        R.string.freshness_warning_range,
        thresholds.criticalDays + 1,
        thresholds.warningDays,
    )
    FreshnessStatus.FRESH -> stringResource(R.string.freshness_fresh_range, thresholds.warningDays + 1)
}

val FreshnessStatus.emoji: String
    get() = when (this) {
        FreshnessStatus.CRITICAL -> "🔴"
        FreshnessStatus.WARNING -> "🟡"
        FreshnessStatus.FRESH -> "🟢"
    }

@Composable
fun daysLeftText(daysLeft: Long): String = when {
    daysLeft < 0 -> pluralStringResource(
        R.plurals.dashboard_expired_days_ago,
        (-daysLeft).toInt(),
        (-daysLeft).toInt(),
    )
    daysLeft == 0L -> stringResource(R.string.dashboard_expires_today)
    daysLeft == 1L -> stringResource(R.string.dashboard_expires_tomorrow)
    else -> pluralStringResource(R.plurals.dashboard_days_left, daysLeft.toInt(), daysLeft.toInt())
}
