package com.shelfie.app.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.HouseholdAction
import com.shelfie.app.domain.model.HouseholdActivity
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.shelfieFilterChipColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The household's activity, newest first and grouped by day, with chips to show one kind of
 * event at a time: the bell at the top of the pantry opens it. Loading the feed marks it seen,
 * which clears the badge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsRoute(
    onNavigateUp: () -> Unit,
    onOpenFamily: () -> Unit,
    viewModel: HouseholdViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Someone may have done something while the screen was in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = { ShelfieTopBar(title = stringResource(R.string.notifications_title), onNavigateUp = onNavigateUp) },
    ) { padding ->
        val events = state.activity
        when {
            // Signed out, or not in a household yet: nothing will ever arrive here.
            !state.isSignedIn || (state.household != null && !state.isShared) -> EmptyState(
                modifier = Modifier.padding(padding),
                onOpenFamily = onOpenFamily,
            )

            events == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            events.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.activity_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> ActivityList(
                events = events,
                hasMore = state.hasMoreActivity,
                isLoadingMore = state.isLoadingMoreActivity,
                onLoadMore = viewModel::loadMoreActivity,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/** Kinds of event, each a filter chip. */
private enum class ActivityKind(val emoji: String, @param:StringRes val labelRes: Int, val actions: Set<HouseholdAction>) {
    ADDED("➕", R.string.activity_kind_added, setOf(HouseholdAction.ADDED)),
    USED("🍽", R.string.activity_kind_used, setOf(HouseholdAction.USED, HouseholdAction.USED_SOME)),
    WASTED("🗑", R.string.activity_kind_wasted, setOf(HouseholdAction.WASTED, HouseholdAction.REMOVED)),
    FREEZER("❄", R.string.activity_kind_freezer, setOf(HouseholdAction.FROZE, HouseholdAction.THAWED)),
    SHOPPING("🛒", R.string.activity_kind_shopping, setOf(HouseholdAction.LIST_ADDED, HouseholdAction.LIST_BOUGHT)),
    RECIPES("👩‍🍳", R.string.activity_kind_recipes, setOf(HouseholdAction.RECIPE_SAVED)),
}

@Composable
private fun ActivityList(
    events: List<HouseholdActivity>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Near the end of what is loaded: fetch the next older page.
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, hasMore, events.size) {
        if (nearEnd && hasMore) onLoadMore()
    }
    var kindName by rememberSaveable { mutableStateOf<String?>(null) }
    val kind = ActivityKind.entries.firstOrNull { it.name == kindName }
    val counts = remember(events) { ActivityKind.entries.associateWith { k -> events.count { it.action in k.actions } } }
    val shown = remember(events, kind) { if (kind == null) events else events.filter { it.action in kind.actions } }

    val zone = remember { ZoneId.systemDefault() }
    val days = remember(shown) { shown.groupBy { it.at.atZone(zone).toLocalDate() } }
    val today = remember { LocalDate.now(zone) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "filters") {
            // Only kinds that happened, so no chip leads to an empty list.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = kind == null,
                    onClick = { kindName = null },
                    label = { Text(stringResource(R.string.activity_kind_all) + " · ${events.size}") },
                    shape = CircleShape,
                    colors = shelfieFilterChipColors(),
                )
                ActivityKind.entries.filter { counts.getValue(it) > 0 }.forEach { k ->
                    FilterChip(
                        selected = kind == k,
                        onClick = { kindName = if (kind == k) null else k.name },
                        label = { Text("${k.emoji} ${stringResource(k.labelRes)} · ${counts.getValue(k)}") },
                        shape = CircleShape,
                        colors = shelfieFilterChipColors(),
                    )
                }
            }
        }
        days.forEach { (day, dayEvents) ->
            item(key = "day_$day") {
                Text(
                    text = when (day) {
                        today -> stringResource(R.string.activity_today)
                        today.minusDays(1) -> stringResource(R.string.activity_yesterday)
                        else -> day.format(dateFormatter)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp).animateItem(),
                )
            }
            items(items = dayEvents, key = { it.id }) { event ->
                ShelfieCard(modifier = Modifier.fillMaxWidth().animateItem()) {
                    ActivityRow(event = event, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }
        if (hasMore || isLoadingMore) {
            item(key = "loadingMore") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

/** How close to the end (in list rows) the next page starts loading. */
private const val LOAD_MORE_THRESHOLD = 5

@Composable
private fun EmptyState(onOpenFamily: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🔔", style = MaterialTheme.typography.displayMedium)
        Text(
            text = stringResource(R.string.notifications_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpenFamily) { Text(stringResource(R.string.notifications_open_family)) }
    }
}
