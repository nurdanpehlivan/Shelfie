package com.shelfie.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shelfie.app.R

/**
 * Nunito (SIL Open Font License, see assets/licenses): rounded, friendly, and with full Latin
 * Extended coverage for Turkish, German, Spanish and French. One variable font file serves
 * every weight; Android 7 (API 24–25) can't apply variation axes and shows the default weight.
 */
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: FontWeight) = Font(
    resId = R.font.nunito,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Nunito = FontFamily(
    nunito(FontWeight.Normal),
    nunito(FontWeight.Medium),
    nunito(FontWeight.SemiBold),
    nunito(FontWeight.Bold),
    nunito(FontWeight.ExtraBold),
)

private val Default = Typography()

private fun TextStyle.nunito(weight: FontWeight? = null) =
    copy(fontFamily = Nunito, fontWeight = weight ?: fontWeight)

/** Material 3 sizes, in Nunito; headings a notch bolder for a warmer, more playful voice. */
val Typography = Typography(
    displayLarge = Default.displayLarge.nunito(FontWeight.ExtraBold),
    displayMedium = Default.displayMedium.nunito(FontWeight.ExtraBold),
    displaySmall = Default.displaySmall.nunito(FontWeight.Bold),
    headlineLarge = Default.headlineLarge.nunito(FontWeight.ExtraBold),
    headlineMedium = Default.headlineMedium.nunito(FontWeight.ExtraBold),
    headlineSmall = Default.headlineSmall.nunito(FontWeight.Bold),
    titleLarge = Default.titleLarge.nunito(FontWeight.Bold),
    titleMedium = Default.titleMedium.nunito(FontWeight.Bold),
    titleSmall = Default.titleSmall.nunito(FontWeight.Bold),
    bodyLarge = Default.bodyLarge.nunito(),
    bodyMedium = Default.bodyMedium.nunito(),
    bodySmall = Default.bodySmall.nunito(),
    labelLarge = Default.labelLarge.nunito(FontWeight.Bold),
    labelMedium = Default.labelMedium.nunito(FontWeight.SemiBold),
    labelSmall = Default.labelSmall.nunito(FontWeight.SemiBold),
)
