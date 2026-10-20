package com.shelfie.app.presentation.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.freshnessPalette

/**
 * The top of the pantry: Frizzy's line for the day, a ring split by freshness group, and the
 * group rows next to it, which double as the list filter (tap again to show everything).
 */
@Composable
fun PantryHeroCard(
    greeting: String,
    criticalCount: Int,
    warningCount: Int,
    freshCount: Int,
    selectedFilter: FreshnessStatus?,
    onFilterSelected: (FreshnessStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = if (criticalCount > 0) {
        pluralStringResource(R.plurals.frizzy_critical, criticalCount, criticalCount)
    } else {
        stringResource(R.string.frizzy_all_fresh)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Frizzy(mood = if (criticalCount > 0) FrizzyMood.WORRIED else FrizzyMood.HAPPY, size = 72.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = greeting, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FreshnessRing(criticalCount, warningCount, freshCount)
                Spacer(modifier = Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        FreshnessStatus.CRITICAL to criticalCount,
                        FreshnessStatus.WARNING to warningCount,
                        FreshnessStatus.FRESH to freshCount,
                    ).forEach { (status, count) ->
                        FilterRow(
                            status = status,
                            count = count,
                            selected = selectedFilter == status,
                            onClick = { onFilterSelected(if (selectedFilter == status) null else status) },
                        )
                    }
                }
            }
            AnimatedVisibility(visible = selectedFilter != null) {
                TextButton(onClick = { onFilterSelected(null) }, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.dashboard_filter_all, criticalCount + warningCount + freshCount))
                }
            }
        }
    }
}

@Composable
private fun FreshnessRing(criticalCount: Int, warningCount: Int, freshCount: Int) {
    val palette = MaterialTheme.freshnessPalette
    val total = criticalCount + warningCount + freshCount
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val animatedCritical by animateIntAsState(criticalCount, label = "criticalCount")
    val summary = pluralStringResource(R.plurals.dashboard_summary_title, criticalCount, criticalCount)

    // Sweeps the ring in once when the card first appears.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .size(112.dp)
            .clearAndSetSemantics { contentDescription = summary },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = stroke)
            if (total == 0) return@Canvas

            // A small gap between segments keeps the colours distinct.
            val gapDegrees = if (listOf(criticalCount, warningCount, freshCount).count { it > 0 } > 1) 14f else 0f
            var startAngle = -90f
            listOf(
                criticalCount to palette.critical.accent,
                warningCount to palette.warning.accent,
                freshCount to palette.fresh.accent,
            ).forEach { (count, color) ->
                if (count == 0) return@forEach
                val sweep = 360f * count / total
                drawArc(
                    color = color,
                    startAngle = startAngle + gapDegrees / 2,
                    sweepAngle = ((sweep - gapDegrees) * reveal.value).coerceAtLeast(0.1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = animatedCritical.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = if (criticalCount > 0) palette.critical.accent else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.dashboard_summary_ring_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One group row; selected rows get the group's tint, like a chip. */
@Composable
private fun FilterRow(status: FreshnessStatus, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.freshnessPalette.forStatus(status)
    val background by animateColorAsState(if (selected) colors.container else Color.Transparent, label = "filterRow")
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        modifier = Modifier.semantics(mergeDescendants = true) { role = Role.Tab; this.selected = selected },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(10.dp).background(colors.accent, CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(status.titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) colors.onContainer else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) colors.onContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
