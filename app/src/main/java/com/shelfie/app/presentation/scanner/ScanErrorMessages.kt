package com.shelfie.app.presentation.scanner

import androidx.annotation.StringRes
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryScanException

@StringRes
internal fun Throwable.toScanMessageRes(): Int = when (this) {
    is PantryScanException.NotConfigured -> R.string.scanner_error_not_configured
    is PantryScanException.ImageUnreadable -> R.string.scanner_error_image_unreadable
    is PantryScanException.Network -> R.string.scanner_error_network
    is PantryScanException.Api ->
        when {
            isDailyQuotaExhausted -> R.string.error_daily_quota
            isRateLimited -> R.string.scanner_error_rate_limited
            else -> R.string.scanner_error_server
        }
    is PantryScanException.Blocked -> R.string.scanner_error_blocked
    is PantryScanException.MalformedResponse -> R.string.scanner_error_malformed
    else -> R.string.scanner_error_unknown
}

/** Retrying only makes sense when the same photo could succeed on a second attempt. */
internal fun Throwable.isRetryableScanFailure(): Boolean = when (this) {
    is PantryScanException.NotConfigured,
    is PantryScanException.ImageUnreadable,
    is PantryScanException.Blocked -> false
    else -> true
}
