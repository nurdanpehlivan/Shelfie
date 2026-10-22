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

/** Warm cream page, white cards, leaf-green actions, carrot for cooking. */
private val LightColorScheme = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF5E3),
    onPrimaryContainer = Color(0xFF0B3D1E),
    inversePrimary = LeafLight,
    secondary = Color(0xFF6B5E52),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4EBE1),
    onSecondaryContainer = Color(0xFF2E241B),
    tertiary = Carrot,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6D6),
    onTertiaryContainer = Color(0xFF5A2408),
    error = Color(0xFFD92D20),
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A1712),
    background = Cream,
    onBackground = WarmInk,
    surface = Cream,
    onSurface = WarmInk,
    surfaceVariant = Color(0xFFF2E8DC),
    onSurfaceVariant = Color(0xFF7A6F66),
    outline = Color(0xFFB8AA9C),
    outlineVariant = Color(0xFFEFE5D8),
    inverseSurface = WarmInk,
    inverseOnSurface = Color(0xFFFFF4E8),
    surfaceTint = Leaf,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF4E8),
    surfaceContainer = Color(0xFFFBEFE2),
    surfaceContainerHigh = Color(0xFFF5E8DA),
    surfaceContainerHighest = Color(0xFFEFE1D1),
)

/** Warm espresso rather than blue-black, so the dark theme still feels like a kitchen. */
private val DarkColorScheme = darkColorScheme(
    primary = LeafLight,
    onPrimary = Color(0xFF053019),
    primaryContainer = Color(0xFF12452A),
    onPrimaryContainer = Color(0xFFC7F5D6),
    inversePrimary = Leaf,
    secondary = Color(0xFFD6C6B6),
    onSecondary = Color(0xFF392D22),
    secondaryContainer = Color(0xFF2E2721),
    onSecondaryContainer = Color(0xFFF1E3D4),
    tertiary = Color(0xFFFF9A62),
    onTertiary = Color(0xFF4A1E06),
    tertiaryContainer = Color(0xFF4A2513),
    onTertiaryContainer = Color(0xFFFFDCC8),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7A1712),
    onErrorContainer = Color(0xFFFEE4E2),
    background = Espresso,
    onBackground = Color(0xFFF6EEE6),
    surface = Espresso,
    onSurface = Color(0xFFF6EEE6),
    surfaceVariant = Color(0xFF3A332C),
    onSurfaceVariant = Color(0xFFB5A99C),
    outline = Color(0xFF8A7E72),
    outlineVariant = Color(0xFF3A332C),
    inverseSurface = Color(0xFFF6EEE6),
    inverseOnSurface = Color(0xFF2A2420),
    surfaceTint = LeafLight,
    surfaceContainerLowest = Color(0xFF191511),
    surfaceContainerLow = Color(0xFF1B1713),
    surfaceContainer = Color(0xFF1E1A16),
    surfaceContainerHigh = Color(0xFF29241F),
    surfaceContainerHighest = Color(0xFF332D27),
)

/**
 * One radius scale for the whole app: chips and fields 12, cards 16–24, sheets and heroes 28+.
 * Buttons stay fully rounded (Material's default) so they never look like cards.
 */
private val ShelfieShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Brand colours always, not the wallpaper's dynamic colours: the app should look like Shelfie
 * on every phone, and the freshness red/honey/green must never clash with a random palette.
 * [darkTheme] comes from the user's theme setting (light by default), see MainActivity.
 */
@Composable
fun ShelfieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFreshnessPalette provides if (darkTheme) DarkFreshnessPalette else LightFreshnessPalette,
        LocalChartPalette provides if (darkTheme) DarkChartPalette else LightChartPalette,
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
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

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

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

/** Brand colours outside Material's scheme, such as the evergreen hero surface. */
val MaterialTheme.extendedColors: ShelfieExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
