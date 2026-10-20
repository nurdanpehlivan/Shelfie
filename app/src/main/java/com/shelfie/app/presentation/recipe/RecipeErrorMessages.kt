package com.shelfie.app.presentation.recipe

import androidx.annotation.StringRes
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryScanException

@StringRes
internal fun Throwable.toRecipeMessageRes(): Int = when (this) {
    is PantryScanException.NotConfigured -> R.string.recipe_error_not_configured
    is PantryScanException.QuotaExceeded ->
        if (isPremium) R.string.error_quota_premium else R.string.error_quota_free
    is PantryScanException.Network -> R.string.scanner_error_network
    is PantryScanException.Api ->
        when {
            isDailyQuotaExhausted -> R.string.error_daily_quota
            isRateLimited -> R.string.scanner_error_rate_limited
            else -> R.string.scanner_error_server
        }
    is PantryScanException.Blocked,
    is PantryScanException.MalformedResponse -> R.string.recipe_error_malformed
    else -> R.string.scanner_error_unknown
}

/** Recipes are generated with some randomness, so even a bad answer is worth another try. */
internal fun Throwable.isRetryableRecipeFailure(): Boolean =
    this !is PantryScanException.NotConfigured && this !is PantryScanException.QuotaExceeded
