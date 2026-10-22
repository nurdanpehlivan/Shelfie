package com.shelfie.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.shelfie.app.core.ui.R

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
    nunito(FontWeight.Black),
)

private val Default = Typography()

/** Tabular figures, so counts and percentages don't jitter while they animate. */
private const val TABULAR_FIGURES = "tnum"

private fun TextStyle.nunito(
    weight: FontWeight? = null,
    letterSpacing: TextUnit = this.letterSpacing,
    tabular: Boolean = false,
) = copy(
    fontFamily = Nunito,
    fontWeight = weight ?: fontWeight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabular) TABULAR_FIGURES else fontFeatureSettings,
)

/**
 * Material 3 sizes in Nunito. Display and headline sizes are heavy and slightly tightened, as
 * large rounded type looks loose at Material's default tracking. Labels and counts are set
 * tight, like a modern UI face; body text keeps its spacing for readability.
 */
val Typography = Typography(
    displayLarge = Default.displayLarge.nunito(FontWeight.Black, (-0.02).em, tabular = true),
    displayMedium = Default.displayMedium.nunito(FontWeight.Black, (-0.02).em, tabular = true),
    displaySmall = Default.displaySmall.nunito(FontWeight.ExtraBold, (-0.01).em, tabular = true),
    headlineLarge = Default.headlineLarge.nunito(FontWeight.ExtraBold, (-0.01).em, tabular = true),
    headlineMedium = Default.headlineMedium.nunito(FontWeight.ExtraBold, (-0.01).em),
    headlineSmall = Default.headlineSmall.nunito(FontWeight.ExtraBold, (-0.005).em),
    titleLarge = Default.titleLarge.nunito(FontWeight.ExtraBold, (-0.01).em),
    titleMedium = Default.titleMedium.nunito(FontWeight.Bold, (-0.005).em),
    titleSmall = Default.titleSmall.nunito(FontWeight.Bold, 0.sp),
    bodyLarge = Default.bodyLarge.nunito(FontWeight.Medium, 0.sp),
    bodyMedium = Default.bodyMedium.nunito(FontWeight.Medium, 0.sp),
    bodySmall = Default.bodySmall.nunito(FontWeight.Medium, 0.1.sp),
    labelLarge = Default.labelLarge.nunito(FontWeight.Bold, 0.sp, tabular = true),
    labelMedium = Default.labelMedium.nunito(FontWeight.Bold, 0.sp, tabular = true),
    labelSmall = Default.labelSmall.nunito(FontWeight.Bold, 0.1.sp),
)
