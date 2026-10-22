package com.shelfie.app.presentation.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.daysLeftText
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.foodEmoji
import com.shelfie.app.presentation.common.formatQuantity
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.dashboard.DashboardItem
import com.shelfie.app.presentation.theme.freshnessPalette

private val CardShape = RoundedCornerShape(20.dp)

private val EmojiShadow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 6f), blurRadius = 12f)

/** Swipe right → consumed, swipe left → wasted. */
@Composable
fun SwipeablePantryItemCard(
    entry: DashboardItem,
    onConsumed: () -> Unit,
    onWasted: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val currentOnConsumed by rememberUpdatedState(onConsumed)
    val currentOnWasted by rememberUpdatedState(onWasted)
    val dismissState = rememberSwipeToDismissBoxState(
        // Require a deliberate swipe; the 56dp default is easy to trigger while scrolling.
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
    )
    val consumedLabel = stringResource(R.string.dashboard_action_consumed)
    val wastedLabel = stringResource(R.string.dashboard_action_wasted)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.semantics {
            // Swipes aren't discoverable by TalkBack; expose the same actions explicitly.
            customActions = listOf(
                CustomAccessibilityAction(consumedLabel) { currentOnConsumed(); true },
                CustomAccessibilityAction(wastedLabel) { currentOnWasted(); true },
            )
        },
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> currentOnConsumed()
                SwipeToDismissBoxValue.EndToStart -> currentOnWasted()
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = {
            SwipeBackground(
                direction = dismissState.dismissDirection,
                progress = dismissState.progress,
                consumedLabel = consumedLabel,
                wastedLabel = wastedLabel,
            )
        },
    ) {
        PantryItemCard(entry = entry, onClick = onClick)
    }
}

@Composable
private fun SwipeBackground(
    direction: SwipeToDismissBoxValue,
    progress: Float,
    consumedLabel: String,
    wastedLabel: String,
) {
    val palette = MaterialTheme.freshnessPalette
    val scheme = MaterialTheme.colorScheme
    val (color, iconRes, label, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            SwipeVisuals(palette.fresh.accent, R.drawable.ic_check, consumedLabel, Alignment.CenterStart)
        SwipeToDismissBoxValue.EndToStart ->
            SwipeVisuals(scheme.error, R.drawable.ic_delete, wastedLabel, Alignment.CenterEnd)
        SwipeToDismissBoxValue.Settled ->
            SwipeVisuals(Color.Transparent, null, null, Alignment.Center)
    }
    // Light accents in dark mode need dark content; the card colour is the inverse in both modes.
    val contentColor = if (direction == SwipeToDismissBoxValue.EndToStart) scheme.onError else scheme.surfaceContainerLowest
    val animatedColor by animateColorAsState(color, label = "swipeBackground")
    val iconScale by animateFloatAsState(if (progress > 0.4f) 1.15f else 0.85f, label = "swipeIcon")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(animatedColor)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        if (iconRes != null && label != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.scale(iconScale),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, color = contentColor, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private data class SwipeVisuals(
    val color: Color,
    val iconRes: Int?,
    val label: String?,
    val alignment: Alignment,
)

/**
 * One pantry item: a calm, flat white card for every group, so the list doesn't shout. Urgency
 * is carried by the coloured "days left" pill and the tinted emoji tile, plus the group header.
 */
@Composable
fun PantryItemCard(entry: DashboardItem, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.freshnessPalette.forStatus(entry.status)
    val item = entry.item
    val isCritical = entry.status == FreshnessStatus.CRITICAL

    // An opaque base under the glass, so the swipe colours don't shine through the card.
    ShelfieCard(
        onClick = onClick,
        shape = CardShape,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, CardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(colors.container, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // A soft drop shadow lifts the emoji off its tile, like a product photo.
                Text(
                    text = item.displayEmoji,
                    style = MaterialTheme.typography.headlineSmall.copy(shadow = EmojiShadow),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // Where it is kept, so a frozen or moved item visibly changes.
                    text = stringResource(
                        R.string.dashboard_item_summary,
                        formatQuantity(item.quantity),
                        stringResource(item.unit.labelRes),
                        stringResource(item.category.labelRes),
                    ) + " · " + item.location.emoji + " " + stringResource(item.location.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = CircleShape,
                // Critical gets the solid accent so it stands out in a long list.
                color = if (isCritical) colors.accent else colors.container,
                contentColor = if (isCritical) MaterialTheme.colorScheme.surfaceContainerLowest else colors.onContainer,
            ) {
                Text(
                    // "≈": no printed date, the expiry is an estimate from the product type.
                    text = (if (item.isExpiryEstimated) "≈ " else "") + daysLeftText(entry.daysLeft),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
