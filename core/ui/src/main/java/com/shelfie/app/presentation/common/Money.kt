package com.shelfie.app.presentation.common

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Offered in settings next to "automatic"; any ISO 4217 code works if stored. */
val COMMON_CURRENCIES = listOf("TRY", "EUR", "USD", "GBP")

/**
 * The currency to show money in: the one picked in settings ([code]), else the device's
 * region. The region comes from the phone, not the app language: "Türkçe" picked in the app
 * has no country, while the phone still knows it is in Turkey.
 */
fun resolveCurrency(code: String?): Currency {
    code?.let { picked -> runCatching { Currency.getInstance(picked) }.getOrNull()?.let { return it } }
    val region = sequenceOf(Locale.getDefault().country, Resources.getSystem().configuration.locales[0].country)
        .firstOrNull { it.isNotBlank() }
    return region
        ?.let { runCatching { Currency.getInstance(Locale.Builder().setRegion(it).build()) }.getOrNull() }
        ?: Currency.getInstance("USD")
}

/** Whole-number money in the app language's style, e.g. "₺450" or "450 €". */
@Composable
fun rememberMoneyFormat(currencyCode: String?): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(currencyCode, locale) {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = resolveCurrency(currencyCode)
            maximumFractionDigits = 0
        }
    }
}

/** The currency's sign as the app language writes it ("₺", "€"), for an amount field. */
@Composable
fun currencySymbol(currencyCode: String?): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(currencyCode, locale) { resolveCurrency(currencyCode).getSymbol(locale) }
}
