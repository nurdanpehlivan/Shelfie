package com.shelfie.app.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.presentation.theme.extendedColors

/*
 * Shelfie's small component kit. Cards are frosted glass: a faint vertical sheen and a 1px
 * hairline over the slate, no drop shadow. Glow is reserved for primary elements (the scan
 * button, the active tab) so the eye always lands on the next action.
 */

/**
 * The standard content card. By default it is glass; pass a [containerColor] for a solid,
 * tinted card (e.g. the recipe banner), or [Color.Unspecified] to keep the glass.
 */
@Composable
fun ShelfieCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = Color.Unspecified,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.extendedColors.glassBorder),
    content: @Composable ColumnScope.() -> Unit,
) {
    val extended = MaterialTheme.extendedColors
    val isGlass = containerColor == Color.Unspecified
    val cardModifier = if (isGlass) {
        modifier.background(Brush.verticalGradient(listOf(extended.glassTop, extended.glassBottom)), shape)
    } else {
        modifier
    }
    val colors = CardDefaults.cardColors(
        containerColor = if (isGlass) Color.Transparent else containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    }
}

/**
 * A soft coloured halo behind a primary element. Coloured shadows need API 28; older phones
 * get a neutral shadow, which still lifts the element.
 */
fun Modifier.glow(color: Color, shape: Shape, radius: Dp = 18.dp): Modifier =
    shadow(elevation = radius, shape = shape, ambientColor = color, spotColor = color)

/**
 * Screen title of the main tabs: a bold, large heading with an optional muted subtitle and
 * actions on the right. It scrolls with the content, so the list gets the whole screen.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        actions()
    }
}

/** Round glass icon button for header actions such as settings. */
@Composable
fun GlassIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.extendedColors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extended.glassBorder),
        modifier = modifier
            .size(44.dp)
            .background(Brush.verticalGradient(listOf(extended.glassTop, extended.glassBottom)), CircleShape),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painter, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/** Heading above a group of content, with an optional trailing element (count, action). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * Segmented buttons and filter chips share one look: the selected option in solid ink, the
 * rest white with a hairline. High contrast, and it never competes with the brand green.
 */
@Composable
fun shelfieSegmentedColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.inverseSurface,
    activeContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    activeBorderColor = MaterialTheme.colorScheme.inverseSurface,
    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

/** See [shelfieSegmentedColors]. */
@Composable
fun shelfieFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    selectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
    selectedLabelColor = MaterialTheme.colorScheme.inverseOnSurface,
)

/** Thin rule plus a flat bar for actions pinned to the bottom of a screen. */
@Composable
fun BottomActionBar(content: @Composable RowScope.() -> Unit) {
    Column(modifier = Modifier.background(MaterialTheme.extendedColors.navBar)) {
        HorizontalDivider(color = MaterialTheme.extendedColors.glassBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Small rounded label: counts, cooking time, "cooked" badges. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(modifier = modifier, shape = CircleShape, color = containerColor, contentColor = contentColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Top bar of pushed screens (recipe, scanner, settings): a back arrow and a compact title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfieTopBar(
    title: String,
    onNavigateUp: (() -> Unit)?,
    navigationContentDescription: String = stringResource(R.string.action_navigate_up),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = navigationContentDescription,
                    )
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
