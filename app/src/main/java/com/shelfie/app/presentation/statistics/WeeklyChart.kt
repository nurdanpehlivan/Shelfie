package com.shelfie.app.presentation.statistics

import com.shelfie.app.presentation.common.shortDateFormatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.domain.model.WeekStatistics
import com.shelfie.app.presentation.theme.chartPalette
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

/**
 * Grouped bars per week: rescued and thrown away side by side on one shared y-axis. Tapping a
 * week shows its exact numbers in the readout above; the latest week is selected at first.
 * Identity never rests on colour alone: a legend names both series and the readout spells
 * the numbers out.
 */
@Composable
fun WeeklyChart(weeks: List<WeekStatistics>, modifier: Modifier = Modifier) {
    if (weeks.isEmpty()) return
    val palette = MaterialTheme.chartPalette
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectionColor = MaterialTheme.colorScheme.surfaceVariant
    val labelFormatter = remember { shortDateFormatter() }
    var selected by rememberSaveable(weeks.size) { mutableIntStateOf(weeks.lastIndex) }
    val selectedWeek = weeks[selected.coerceIn(weeks.indices)]

    val maxValue = weeks.maxOf { max(it.rescued, it.wasted) }
    // Round the axis up to a tidy number so the gridlines land on whole items.
    val axisMax = niceCeiling(maxValue)

    val rescuedLabel = stringResource(R.string.stats_legend_rescued)
    val wastedLabel = stringResource(R.string.stats_legend_wasted)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendEntry(color = palette.rescued, label = rescuedLabel)
            LegendEntry(color = palette.wasted, label = wastedLabel)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Readout of the selected week, in text colours (series colours only on the swatches).
        Text(
            text = stringResource(
                R.string.stats_week_readout,
                selectedWeek.weekStart.format(labelFormatter),
                selectedWeek.rescued,
                selectedWeek.wasted,
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            // Y-axis labels: 0, half and max.
            Column(
                modifier = Modifier.width(24.dp).height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf(axisMax, axisMax / 2, 0).forEach { value ->
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp)
                    .semantics {
                        contentDescription = weeks.joinToString("; ") {
                            "${it.weekStart.format(labelFormatter)}: $rescuedLabel ${it.rescued}, $wastedLabel ${it.wasted}"
                        }
                    }
                    .pointerInput(weeks.size) {
                        detectTapGestures { offset ->
                            // The whole week column is the hit target, much wider than a bar.
                            val slot = size.width / weeks.size
                            selected = (offset.x / slot).toInt().coerceIn(weeks.indices)
                        }
                    },
            ) {
                val slot = size.width / weeks.size
                val barWidth = minOf(12.dp.toPx(), slot * 0.3f)
                val gap = 2.dp.toPx()
                val radius = 4.dp.toPx()

                // Selected week band behind the bars.
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(slot * selected, 0f),
                    size = Size(slot, size.height),
                )
                // Recessive gridlines at 0, half and max.
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = size.height * (1 - fraction)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                weeks.forEachIndexed { index, week ->
                    val centre = slot * index + slot / 2
                    drawBar(week.rescued, axisMax, centre - gap / 2 - barWidth, barWidth, radius, palette.rescued)
                    drawBar(week.wasted, axisMax, centre + gap / 2, barWidth, radius, palette.wasted)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 30.dp)) {
            weeks.forEachIndexed { index, week ->
                // Every other label keeps eight weeks readable on a phone.
                Text(
                    text = if (index % 2 == weeks.lastIndex % 2) week.weekStart.format(labelFormatter) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A bar with 4dp rounded top corners, square at the baseline; zero draws nothing. */
private fun DrawScope.drawBar(value: Int, axisMax: Int, left: Float, width: Float, radius: Float, color: Color) {
    if (value <= 0 || axisMax <= 0) return
    val height = size.height * value / axisMax
    val top = size.height - height
    val r = minOf(radius, height, width / 2)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = size.height,
                topLeftCornerRadius = CornerRadius(r, r),
                topRightCornerRadius = CornerRadius(r, r),
                bottomLeftCornerRadius = CornerRadius.Zero,
                bottomRightCornerRadius = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}

@Composable
private fun LegendEntry(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Smallest even number ≥ [value] (at least 2), so the half gridline is a whole number. */
internal fun niceCeiling(value: Int): Int {
    if (value <= 2) return 2
    return (ceil(value / 2.0) * 2).toInt()
}
