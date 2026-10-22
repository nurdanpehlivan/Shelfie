package com.shelfie.app.presentation.statistics

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryStatistics
import com.shelfie.app.domain.model.StatisticsPeriod
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.ScreenHeader
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.common.shelfieSegmentedColors
import com.shelfie.app.presentation.common.shortDateFormatter
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.chartPalette
import com.shelfie.app.presentation.theme.extendedColors
import java.text.NumberFormat

@Composable
fun StatisticsRoute(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(state = state, onPeriodSelected = viewModel::selectPeriod)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(state: StatisticsUiState, onPeriodSelected: (StatisticsPeriod) -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(title = stringResource(R.string.stats_title))
            // Filters sit in one row above everything they affect.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                StatisticsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { onPeriodSelected(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, StatisticsPeriod.entries.size),
                        colors = shelfieSegmentedColors(),
                        icon = {},
                    ) { Text(stringResource(period.labelRes)) }
                }
            }

            val stats = state.statistics
            if (stats == null) {
                Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            HeroCard(stats)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("♻", stats.rescuedCount.toString(), stringResource(R.string.stats_tile_rescued), Modifier.weight(1f))
                StatTile("🗑", stats.wastedCount.toString(), stringResource(R.string.stats_tile_wasted), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val currency = remember { NumberFormat.getCurrencyInstance().apply { maximumFractionDigits = 0 } }
                StatTile("💰", currency.format(stats.estimatedSavings), stringResource(R.string.stats_tile_savings), Modifier.weight(1f))
                StatTile("👩‍🍳", stats.cookedRecipeCount.toString(), stringResource(R.string.stats_tile_cooked), Modifier.weight(1f))
            }

            ChartCard(stats)

            if (stats.topWastedCategories.isNotEmpty()) TopWastedCard(stats)

            Text(
                text = stringResource(R.string.stats_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** The headline, on the evergreen hero: what share of the food that left the pantry was eaten. */
@Composable
private fun HeroCard(stats: PantryStatistics) {
    val extended = MaterialTheme.extendedColors
    val rate = stats.rescueRate
    HighlightCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                // "%42" in Turkish, "42%" in English.
                val percent = remember { NumberFormat.getPercentInstance() }
                // With no data, sleepy Frizzy and the hint say it all; a lone dash would add nothing.
                if (rate != null) {
                    Text(text = percent.format(rate), style = MaterialTheme.typography.displayMedium)
                }
                Text(
                    text = stringResource(if (rate == null) R.string.stats_hero_empty else R.string.stats_hero_caption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.onHeroVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Frizzy(
                mood = when {
                    rate == null -> FrizzyMood.SLEEPY
                    rate >= GOOD_RESCUE_RATE -> FrizzyMood.CELEBRATING
                    rate >= OK_RESCUE_RATE -> FrizzyMood.HAPPY
                    else -> FrizzyMood.WORRIED
                },
                size = 96.dp,
            )
        }
    }
}

@Composable
private fun StatTile(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    ShelfieCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = icon, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Weekly chart with a table view of the same numbers for anyone who'd rather read them. */
@Composable
private fun ChartCard(stats: PantryStatistics) {
    var showTable by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { shortDateFormatter() }
    ShelfieCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.stats_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showTable = !showTable }) {
                    Text(stringResource(if (showTable) R.string.stats_show_chart else R.string.stats_show_table))
                }
            }
            Column(modifier = Modifier.padding(end = 8.dp)) {
                if (showTable) {
                    Row {
                        TableCell(stringResource(R.string.stats_table_week), Modifier.weight(1.4f), header = true)
                        TableCell(stringResource(R.string.stats_legend_rescued), Modifier.weight(1f), header = true)
                        TableCell(stringResource(R.string.stats_legend_wasted), Modifier.weight(1f), header = true)
                    }
                    stats.weekly.asReversed().forEach { week ->
                        Row {
                            TableCell(week.weekStart.format(formatter), Modifier.weight(1.4f))
                            TableCell(week.rescued.toString(), Modifier.weight(1f))
                            TableCell(week.wasted.toString(), Modifier.weight(1f))
                        }
                    }
                } else {
                    WeeklyChart(weeks = stats.weekly)
                }
            }
        }
    }
}

/** Most-wasted categories as bars relative to the worst one, so the ranking reads at a glance. */
@Composable
private fun TopWastedCard(stats: PantryStatistics) {
    val barColor = MaterialTheme.chartPalette.wasted
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val max = stats.topWastedCategories.maxOf { it.second }.coerceAtLeast(1)
    ShelfieCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.stats_top_wasted), style = MaterialTheme.typography.titleMedium)
            stats.topWastedCategories.forEach { (category, count) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${category.emoji}  ${stringResource(category.labelRes)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = pluralStringResource(R.plurals.stats_items, count, count),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(trackColor, CircleShape),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(count.toFloat() / max)
                                .height(6.dp)
                                .background(barColor, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, modifier: Modifier, header: Boolean = false) {
    Text(
        text = text,
        style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
        color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(vertical = 6.dp),
    )
}

/** Frizzy celebrates at 80 % eaten and worries below 50 %. */
private const val GOOD_RESCUE_RATE = 0.8f
private const val OK_RESCUE_RATE = 0.5f

private val StatisticsPeriod.labelRes: Int
    get() = when (this) {
        StatisticsPeriod.WEEK -> R.string.stats_period_week
        StatisticsPeriod.MONTH -> R.string.stats_period_month
        StatisticsPeriod.ALL_TIME -> R.string.stats_period_all
    }
