package com.shelfie.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F0D2),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4E6356),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E8D8),
    onSecondaryContainer = Color(0xFF0B1F15),
    tertiary = Color(0xFF8B4F1E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2F1500),
    background = BrandCream,
    onBackground = Color(0xFF1A1C19),
    surface = BrandCream,
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDDE5DC),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F3EC),
    surfaceContainer = Color(0xFFF0EDE6),
    surfaceContainerHigh = Color(0xFFEAE8E0),
    surfaceContainerHighest = Color(0xFFE4E2DB),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF0F5236),
    onPrimaryContainer = Color(0xFFABF5CB),
    secondary = Color(0xFFB5CCBC),
    onSecondary = Color(0xFF203529),
    secondaryContainer = Color(0xFF374B3F),
    onSecondaryContainer = Color(0xFFD1E8D8),
    tertiary = Color(0xFFFFB77C),
    onTertiary = Color(0xFF4D2600),
    tertiaryContainer = Color(0xFF6E3908),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF111411),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF111411),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFF0C0F0C),
    surfaceContainerLow = Color(0xFF1A1C19),
    surfaceContainer = Color(0xFF1E201D),
    surfaceContainerHigh = Color(0xFF282B27),
    surfaceContainerHighest = Color(0xFF333532),
)

/** Softer, rounder than the Material defaults, to match the mascot and the Nunito type. */
private val ShelfieShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Brand colours always, not the wallpaper's dynamic colours: the app should look like Shelfie
 * on every phone, and the freshness red/amber/green must never clash with a random palette.
 */
@Composable
fun ShelfieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFreshnessPalette provides if (darkTheme) DarkFreshnessPalette else LightFreshnessPalette,
        LocalChartPalette provides if (darkTheme) DarkChartPalette else LightChartPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = ShelfieShapes,
            content = content,
        )
    }
}

private val LocalFreshnessPalette = staticCompositionLocalOf { LightFreshnessPalette }

private val LocalChartPalette = staticCompositionLocalOf { LightChartPalette }

/** Colours for the rescued / thrown-away statistics chart, matching light/dark mode. */
val MaterialTheme.chartPalette: ChartPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalChartPalette.current

/** Red / amber / green colours for freshness groups, matching the current light/dark mode. */
val MaterialTheme.freshnessPalette: FreshnessPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalFreshnessPalette.current
