package com.shelfie.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.shelfie.app.domain.model.FreshnessStatus

// region Brand: fresh mint green on warm cream, with an apricot accent (tertiary).

val BrandGreen = Color(0xFF2E7D5B)
val BrandGreenLight = Color(0xFF8FD8B0)
val BrandCream = Color(0xFFFBF8F1)
val BrandApricot = Color(0xFFF2A65A)

// endregion

/** Colours of one freshness group: [accent] for dots/bars, [container]/[onContainer] for cards. */
@Immutable
data class StatusColors(val accent: Color, val container: Color, val onContainer: Color)

/**
 * Fixed red / amber / green palette for freshness groups. It deliberately ignores dynamic colour
 * so "critical" always reads as red, whatever the wallpaper.
 */
@Immutable
data class FreshnessPalette(
    val critical: StatusColors,
    val warning: StatusColors,
    val fresh: StatusColors,
) {
    fun forStatus(status: FreshnessStatus): StatusColors = when (status) {
        FreshnessStatus.CRITICAL -> critical
        FreshnessStatus.WARNING -> warning
        FreshnessStatus.FRESH -> fresh
    }
}

val LightFreshnessPalette = FreshnessPalette(
    critical = StatusColors(Color(0xFFD32F2F), Color(0xFFFFDAD6), Color(0xFF410002)),
    warning = StatusColors(Color(0xFFE6A100), Color(0xFFFFF0C2), Color(0xFF3D2E00)),
    fresh = StatusColors(Color(0xFF2E7D32), Color(0xFFDCF3DD), Color(0xFF0B3D0F)),
)

val DarkFreshnessPalette = FreshnessPalette(
    critical = StatusColors(Color(0xFFFF8A80), Color(0xFF5C1714), Color(0xFFFFDAD6)),
    warning = StatusColors(Color(0xFFFFD54F), Color(0xFF4A3B00), Color(0xFFFFF0C2)),
    fresh = StatusColors(Color(0xFF81C784), Color(0xFF1B3D1E), Color(0xFFDCF3DD)),
)

/**
 * Two-series chart colours (rescued vs thrown away). Teal/orange instead of the freshness
 * green/red because red-green pairs are indistinguishable for deutan/protan colour vision;
 * these pass a CVD separation check (ΔE ≥ 13) against each mode's surface.
 */
@Immutable
data class ChartPalette(val rescued: Color, val wasted: Color)

val LightChartPalette = ChartPalette(rescued = Color(0xFF00897B), wasted = Color(0xFFD84315))

val DarkChartPalette = ChartPalette(rescued = Color(0xFF26A69A), wasted = Color(0xFFF4511E))
