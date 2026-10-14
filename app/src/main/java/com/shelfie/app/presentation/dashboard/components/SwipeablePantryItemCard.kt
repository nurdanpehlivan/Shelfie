package com.shelfie.app.presentation.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.presentation.common.foodEmoji
import com.shelfie.app.presentation.common.formatQuantity
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.dashboard.DashboardItem
import com.shelfie.app.presentation.theme.freshnessPalette

private val CardShape = RoundedCornerShape(20.dp)

/** Swipe right → consumed, swipe left → wasted. */
@Composable
fun SwipeablePantryItemCard(
    entry: DashboardItem,
    onConsumed: () -> Unit,
    onWasted: () -> Unit,
    modifier: Modifier = Modifier,
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
        PantryItemCard(entry = entry)
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
    val (color, iconRes, label, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            SwipeVisuals(palette.fresh.accent, R.drawable.ic_check, consumedLabel, Alignment.CenterStart)
        SwipeToDismissBoxValue.EndToStart ->
            SwipeVisuals(MaterialTheme.colorScheme.error, R.drawable.ic_delete, wastedLabel, Alignment.CenterEnd)
        SwipeToDismissBoxValue.Settled ->
            SwipeVisuals(Color.Transparent, null, null, Alignment.Center)
    }
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
                    tint = Color.White,
                    modifier = Modifier.scale(iconScale),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, color = Color.White, fontWeight = FontWeight.SemiBold)
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
 * One pantry item: a calm white card for every group, so the list doesn't shout. Urgency is
 * carried by the coloured "days left" pill and the tinted emoji tile, plus the group header.
 */
@Composable
fun PantryItemCard(entry: DashboardItem, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.freshnessPalette.forStatus(entry.status)
    val item = entry.item
    val isCritical = entry.status == FreshnessStatus.CRITICAL

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(colors.container, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = foodEmoji(item.name, item.category), style = MaterialTheme.typography.headlineSmall)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.dashboard_item_summary,
                        formatQuantity(item.quantity),
                        stringResource(item.unit.labelRes),
                        stringResource(item.category.labelRes),
                    ),
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
                contentColor = if (isCritical) Color.White else colors.onContainer,
            ) {
                Text(
                    text = daysLeftText(entry.daysLeft),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
