package com.shelfie.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.shelfie.app.domain.model.FreshnessStatus

// region Brand: "fresh market". Leaf green on warm cream, with a carrot accent (tertiary).
//
// The app is about food that's still good: the palette is light, warm and appetising, like a
// market stall in the morning, not a dashboard. Green means fresh and "go"; carrot is the
// cooking accent (recipes, the rescue banner). Status colours (red / honey / green) are only
// ever paired with a label, so the carrot accent never has to carry meaning on its own.

val Leaf = Color(0xFF23974F)
val LeafDeep = Color(0xFF1B7A40)
val LeafLight = Color(0xFF5BD48A)
val Cream = Color(0xFFFFF9F2)
val WarmInk = Color(0xFF2A2420)
val Carrot = Color(0xFFE8692A)
val Espresso = Color(0xFF15120F)

// endregion

/** Colours of one freshness group: [accent] for dots/bars, [container]/[onContainer] for tags. */
@Immutable
data class StatusColors(val accent: Color, val container: Color, val onContainer: Color)

/**
 * Fixed red / honey / green palette for freshness groups. It deliberately ignores dynamic colour
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
    critical = StatusColors(Color(0xFFDC2626), Color(0xFFFEE4E2), Color(0xFF7A1712)),
    warning = StatusColors(Color(0xFFCA8A04), Color(0xFFFEF3C7), Color(0xFF713F12)),
    fresh = StatusColors(Color(0xFF16A34A), Color(0xFFDCFCE7), Color(0xFF14532D)),
)

val DarkFreshnessPalette = FreshnessPalette(
    critical = StatusColors(Color(0xFFF87171), Color(0xFF3B1A18), Color(0xFFFCA5A5)),
    warning = StatusColors(Color(0xFFFACC15), Color(0xFF3A3010), Color(0xFFFDE68A)),
    fresh = StatusColors(Color(0xFF4ADE80), Color(0xFF14321F), Color(0xFFBBF7D0)),
)

/**
 * Two-series chart colours (rescued vs thrown away). Teal/orange instead of the freshness
 * green/red because red-green pairs are indistinguishable for deutan/protan colour vision.
 */
@Immutable
data class ChartPalette(val rescued: Color, val wasted: Color)

val LightChartPalette = ChartPalette(rescued = Color(0xFF0F9D7A), wasted = Color(0xFFEA580C))

val DarkChartPalette = ChartPalette(rescued = Color(0xFF2DD4BF), wasted = Color(0xFFFB923C))

/**
 * Colours Material's scheme has no slot for.
 *
 * - [hero]/[onHero]: the soft leaf-tinted highlight, used once per screen at most (recipe
 *   header, statistics headline, Premium) through [HighlightCard]; [heroTile] sits on it and
 *   [heroBorder] outlines it. Light on purpose: the app has no solid dark-green blocks.
 * - [glassTop]/[glassBottom]/[glassBorder]: the card surface. In light mode a white card with a
 *   warm hairline on the cream page; in dark mode a faint sheen over the espresso background.
 * - [glow]: coloured shadow of primary elements (the scan button, the active tab).
 */
@Immutable
data class ShelfieExtendedColors(
    val hero: Color,
    val onHero: Color,
    val onHeroVariant: Color,
    val heroTile: Color,
    val heroBorder: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorder: Color,
    val glow: Color,
    val navBar: Color,
)

val LightExtendedColors = ShelfieExtendedColors(
    hero = Color(0xFFE6F5EB),
    onHero = Color(0xFF154D2B),
    onHeroVariant = Color(0xFF4A6E57),
    heroTile = Color.White.copy(alpha = 0.8f),
    heroBorder = Leaf.copy(alpha = 0.22f),
    glassTop = Color.White,
    glassBottom = Color(0xFFFFFDF9),
    glassBorder = Color(0xFFF0E6D8),
    glow = Leaf.copy(alpha = 0.35f),
    navBar = Color.White.copy(alpha = 0.97f),
)

val DarkExtendedColors = ShelfieExtendedColors(
    hero = Color(0xFF18271D),
    onHero = Color(0xFFD6F3E0),
    onHeroVariant = Color(0xFF9FC8AE),
    heroTile = Color.White.copy(alpha = 0.08f),
    heroBorder = LeafLight.copy(alpha = 0.18f),
    glassTop = Color.White.copy(alpha = 0.06f),
    glassBottom = Color.White.copy(alpha = 0.03f),
    glassBorder = Color.White.copy(alpha = 0.09f),
    glow = LeafLight.copy(alpha = 0.45f),
    navBar = Color(0xFF1E1A16).copy(alpha = 0.96f),
)
