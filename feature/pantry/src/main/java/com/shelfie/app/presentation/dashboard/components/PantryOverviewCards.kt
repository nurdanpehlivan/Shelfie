package com.shelfie.app.presentation.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.glow
import com.shelfie.app.presentation.dashboard.PantryFilter
import com.shelfie.app.presentation.theme.extendedColors
import com.shelfie.app.presentation.theme.freshnessPalette

/**
 * The two overview widgets at the top of the pantry: what's safe, and what needs eating soon.
 * Each one filters the list below; tapping the selected card again shows everything.
 */
@Composable
fun PantryOverviewCards(
    safeCount: Int,
    criticalCount: Int,
    warningCount: Int,
    selectedFilter: PantryFilter?,
    onFilterSelected: (PantryFilter?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.freshnessPalette
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OverviewCard(
            label = stringResource(R.string.freshness_fresh),
            count = safeCount,
            accent = palette.fresh.accent,
            selected = selectedFilter == PantryFilter.SAFE,
            onClick = { onFilterSelected(if (selectedFilter == PantryFilter.SAFE) null else PantryFilter.SAFE) },
            modifier = Modifier.weight(1f),
        )
        OverviewCard(
            label = stringResource(R.string.dashboard_use_soon),
            count = criticalCount + warningCount,
            accent = palette.warning.accent,
            selected = selectedFilter == PantryFilter.USE_SOON,
            onClick = {
                onFilterSelected(if (selectedFilter == PantryFilter.USE_SOON) null else PantryFilter.USE_SOON)
            },
            // The critical share in red, so "use soon" never hides what's due today.
            footnote = if (criticalCount > 0) {
                "$criticalCount ${stringResource(R.string.freshness_critical)}" to palette.critical.accent
            } else {
                null
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OverviewCard(
    label: String,
    count: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: Pair<String, Color>? = null,
) {
    val shape = MaterialTheme.shapes.large
    val borderColor by animateColorAsState(
        if (selected) accent else MaterialTheme.extendedColors.glassBorder,
        label = "overviewBorder",
    )
    val animatedCount by animateIntAsState(count, label = "overviewCount")

    ShelfieCard(
        onClick = onClick,
        shape = shape,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .then(if (selected) Modifier.glow(accent.copy(alpha = 0.5f), shape, radius = 14.dp) else Modifier)
            .semantics(mergeDescendants = true) { role = Role.Tab; this.selected = selected },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .glow(accent, CircleShape, radius = 6.dp)
                        .background(accent, CircleShape),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = animatedCount.toString(), style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pluralStringResource(R.plurals.dashboard_product_count, count),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = footnote?.first ?: " ",
                style = MaterialTheme.typography.labelMedium,
                color = footnote?.second ?: Color.Transparent,
                maxLines = 1,
            )
        }
    }
}

/**
 * The whole pantry in one line: a bar split green / amber / red by how many items are safe,
 * heads-up and critical, with the counts underneath. Segments animate as items move.
 */
@Composable
fun FreshnessBar(
    safeCount: Int,
    warningCount: Int,
    criticalCount: Int,
    modifier: Modifier = Modifier,
) {
    val total = safeCount + warningCount + criticalCount
    if (total == 0) return
    val palette = MaterialTheme.freshnessPalette
    val segments = listOf(
        Triple(safeCount, palette.fresh.accent, R.string.freshness_fresh),
        Triple(warningCount, palette.warning.accent, R.string.freshness_warning),
        Triple(criticalCount, palette.critical.accent, R.string.freshness_critical),
    )
    val description = stringResource(R.string.dashboard_freshness_bar, safeCount, warningCount, criticalCount)

    ShelfieCard(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.dashboard_freshness_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pluralStringResource(R.plurals.dashboard_product_count_total, total, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                segments.filter { it.first > 0 }.forEach { (count, color, _) ->
                    val weight by animateFloatAsState(count.toFloat(), label = "freshnessSegment")
                    Box(
                        modifier = Modifier
                            .weight(weight.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(color),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                segments.forEach { (count, color, labelRes) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$count ${stringResource(labelRes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
