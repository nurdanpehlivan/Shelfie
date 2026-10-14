package com.shelfie.app.presentation.statistics

import com.shelfie.app.presentation.common.shortDateFormatter
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.Frizzy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryStatistics
import com.shelfie.app.domain.model.StatisticsPeriod
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.labelRes
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

@Composable
fun StatisticsRoute(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(state = state, onPeriodSelected = viewModel::selectPeriod)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(state: StatisticsUiState, onPeriodSelected: (StatisticsPeriod) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Filters sit in one row above everything they affect.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                StatisticsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { onPeriodSelected(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, StatisticsPeriod.entries.size),
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

            if (stats.topWastedCategories.isNotEmpty()) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.stats_top_wasted),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        stats.topWastedCategories.forEach { (category, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${category.emoji}  ${stringResource(category.labelRes)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = pluralStringResource(R.plurals.stats_items, count, count),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.stats_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The headline: what share of the food that left the pantry was eaten. */
@Composable
private fun HeroCard(stats: PantryStatistics) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rate = stats.rescueRate
            Frizzy(
                mood = when {
                    rate == null -> FrizzyMood.SLEEPY
                    rate >= GOOD_RESCUE_RATE -> FrizzyMood.CELEBRATING
                    rate >= OK_RESCUE_RATE -> FrizzyMood.HAPPY
                    else -> FrizzyMood.WORRIED
                },
                size = 96.dp,
            )
            // "%42" in Turkish, "42%" in English.
            val percent = remember { NumberFormat.getPercentInstance() }
            // With no data, sleepy Frizzy and the hint say it all; a lone dash would add nothing.
            if (rate != null) {
                Text(
                    text = percent.format(rate),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = stringResource(if (rate == null) R.string.stats_hero_empty else R.string.stats_hero_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatTile(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = icon, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Weekly chart with a table view of the same numbers for anyone who'd rather read them. */
@Composable
private fun ChartCard(stats: PantryStatistics) {
    var showTable by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { shortDateFormatter() }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.stats_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showTable = !showTable }) {
                    Text(stringResource(if (showTable) R.string.stats_show_chart else R.string.stats_show_table))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun TableCell(text: String, modifier: Modifier, header: Boolean = false) {
    Text(
        text = text,
        style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
        fontWeight = if (header) FontWeight.Bold else null,
        modifier = modifier.padding(vertical = 4.dp),
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
