package com.shelfie.app.presentation.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.presentation.common.GlassIconButton
import com.shelfie.app.presentation.common.Pill
import com.shelfie.app.presentation.common.ScreenHeader
import com.shelfie.app.presentation.common.rangeText
import com.shelfie.app.presentation.common.titleRes
import com.shelfie.app.presentation.dashboard.components.AddItemSheet
import com.shelfie.app.presentation.dashboard.components.FreshnessBar
import com.shelfie.app.presentation.dashboard.components.ItemEditSheet
import com.shelfie.app.presentation.dashboard.components.LocationChips
import com.shelfie.app.presentation.dashboard.components.PantryOverviewCards
import com.shelfie.app.presentation.dashboard.components.PantrySearchField
import com.shelfie.app.presentation.dashboard.components.RescueRecipeBanner
import com.shelfie.app.presentation.dashboard.components.ScanActions
import com.shelfie.app.presentation.dashboard.components.SwipeablePantryItemCard
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.MascotMessage
import com.shelfie.app.presentation.theme.ShelfieTheme
import com.shelfie.app.presentation.theme.freshnessPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

/**
 * @param savedItemsCount number of items the scanner just saved, shown once as a snackbar;
 *   [onSavedItemsMessageShown] clears it so it isn't shown again.
 */
@Composable
fun DashboardRoute(
    onNavigateToScanner: () -> Unit,
    onNavigateToRecipe: () -> Unit,
    onNavigateToSettings: () -> Unit,
    savedItemsCount: Int? = null,
    onSavedItemsMessageShown: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentOnNavigateToScanner by rememberUpdatedState(onNavigateToScanner)
    val currentOnNavigateToRecipe by rememberUpdatedState(onNavigateToRecipe)
    val currentOnNavigateToSettings by rememberUpdatedState(onNavigateToSettings)
    val addItem by viewModel.addItem.collectAsStateWithLifecycle()
    val editItem by viewModel.editItem.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        DashboardEffect.NavigateToScanner -> currentOnNavigateToScanner()
                        DashboardEffect.NavigateToRecipe -> currentOnNavigateToRecipe()
                        DashboardEffect.NavigateToSettings -> currentOnNavigateToSettings()

                        is DashboardEffect.ItemAdded -> snackbarScope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.add_item_saved, effect.itemName),
                            )
                        }

                        is DashboardEffect.ShowRemovalUndo -> snackbarScope.launch {
                            // Replace, don't queue: after several swipes only the last undo matters.
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val message = context.getString(
                                when (effect.reason) {
                                    RemovalReason.CONSUMED -> R.string.dashboard_removed_consumed
                                    RemovalReason.WASTED -> R.string.dashboard_removed_wasted
                                },
                                effect.itemName,
                            )
                            val result = snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = context.getString(R.string.action_undo),
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.onIntent(DashboardIntent.UndoRemoval(effect.itemId))
                            }
                        }

                        is DashboardEffect.ShowItemMessage -> snackbarScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(effect.messageRes, effect.itemName, effect.extra),
                                actionLabel = effect.undo?.let { context.getString(R.string.action_undo) },
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed && effect.undo != null) {
                                viewModel.onIntent(DashboardIntent.UndoChange(effect.undo))
                            }
                        }

                        is DashboardEffect.ShowMessage -> snackbarScope.launch {
                            snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(savedItemsCount) {
        val count = savedItemsCount ?: return@LaunchedEffect
        // Clearing the result changes this effect's key, so show the snackbar in a scope that
        // outlives it.
        onSavedItemsMessageShown()
        snackbarScope.launch {
            snackbarHostState.showSnackbar(
                context.resources.getQuantityString(R.plurals.scanner_items_saved, count, count),
            )
        }
    }

    DashboardScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )

    editItem?.let { item ->
        ItemEditSheet(item = item, onIntent = viewModel::onIntent)
    }

    addItem?.let { addState ->
        AddItemSheet(state = addState, onIntent = viewModel::onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onIntent: (DashboardIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    // Open while there is a query, so coming back to the tab keeps the search visible.
    var isSearching by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // The empty state has its own big buttons for the same actions.
            if (!state.isEmpty) {
                ScanActions(
                    expanded = fabExpanded,
                    onScan = { onIntent(DashboardIntent.ScanNewPhoto) },
                    onQuickAdd = { onIntent(DashboardIntent.OpenAddItem) },
                )
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.isEmpty -> MascotMessage(
                mood = FrizzyMood.SLEEPY,
                title = stringResource(R.string.dashboard_empty_title),
                message = stringResource(R.string.dashboard_empty_subtitle),
                primaryAction = stringResource(R.string.dashboard_action_scan) to { onIntent(DashboardIntent.ScanNewPhoto) },
                secondaryAction = stringResource(R.string.add_item_title) to { onIntent(DashboardIntent.OpenAddItem) },
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                // Bottom padding keeps the last card clear of the scan actions.
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 168.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") {
                    ScreenHeader(
                        title = stringResource(R.string.dashboard_title),
                        subtitle = stringResource(R.string.dashboard_subtitle),
                        actions = {
                            GlassIconButton(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.dashboard_search),
                                onClick = { isSearching = true },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            GlassIconButton(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.settings_title),
                                onClick = { onIntent(DashboardIntent.OpenSettings) },
                            )
                        },
                    )
                }
                if (isSearching) {
                    item(key = "search") {
                        PantrySearchField(
                            query = state.query,
                            onQueryChange = { onIntent(DashboardIntent.Search(it)) },
                            onClose = {
                                onIntent(DashboardIntent.Search(""))
                                isSearching = false
                            },
                        )
                    }
                }
                item(key = "overview") {
                    PantryOverviewCards(
                        safeCount = state.fresh.size,
                        criticalCount = state.critical.size,
                        warningCount = state.warning.size,
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = { onIntent(DashboardIntent.SelectFilter(it)) },
                    )
                }
                item(key = "freshnessBar") {
                    FreshnessBar(
                        safeCount = state.fresh.size,
                        warningCount = state.warning.size,
                        criticalCount = state.critical.size,
                    )
                }
                if (state.hasRescuableItems) {
                    item(key = "rescueRecipe") {
                        RescueRecipeBanner(onClick = { onIntent(DashboardIntent.OpenRescueRecipe) })
                    }
                }
                item(key = "locations") {
                    LocationChips(
                        selected = state.locationFilter,
                        counts = state.locationCounts,
                        onSelect = { onIntent(DashboardIntent.SelectLocation(it)) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (state.isFiltered && state.totalCount == 0) {
                    item(key = "noResults") {
                        Text(
                            text = if (state.query.isNotBlank()) {
                                stringResource(R.string.dashboard_search_empty, state.query.trim())
                            } else {
                                stringResource(R.string.dashboard_group_empty)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        )
                    }
                    return@LazyColumn
                }
                item(key = "swipeHint") {
                    Text(
                        text = stringResource(R.string.dashboard_swipe_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }

                state.visibleGroups.forEach { status ->
                    val items = state.itemsFor(status)
                    stickyHeader(key = "header_$status") {
                        GroupHeader(status = status, count = items.size, thresholds = state.thresholds)
                    }
                    if (items.isEmpty()) {
                        item(key = "empty_$status") {
                            Text(
                                text = stringResource(R.string.dashboard_group_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            )
                        }
                    }
                    items(items = items, key = { it.item.id }) { entry ->
                        SwipeablePantryItemCard(
                            entry = entry,
                            onConsumed = { onIntent(DashboardIntent.MarkConsumed(entry.item.id)) },
                            onWasted = { onIntent(DashboardIntent.MarkWasted(entry.item.id)) },
                            onClick = { onIntent(DashboardIntent.OpenItem(entry.item.id)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(status: FreshnessStatus, count: Int, thresholds: FreshnessThresholds) {
    val colors = MaterialTheme.freshnessPalette.forStatus(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Opaque so cards scrolling under the sticky header don't show through.
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(colors.accent, CircleShape),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(status.titleRes),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status.rangeText(thresholds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Pill(text = count.toString(), containerColor = colors.container, contentColor = colors.onContainer)
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    val today = LocalDate.of(2026, 10, 8)
    fun entry(id: Long, name: String, category: Category, days: Long) = DashboardItem(
        item = PantryItem(
            id = id,
            name = name,
            category = category,
            unit = MeasurementUnit.PIECE,
            purchaseDate = today,
            expiryDate = today.plusDays(days),
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        ),
        daysLeft = days,
        status = FreshnessStatus.from(today.plusDays(days), today),
    )
    ShelfieTheme {
        DashboardScreen(
            state = DashboardUiState(
                isLoading = false,
                critical = listOf(entry(1, "Süt", Category.DAIRY, 1), entry(2, "Tavuk", Category.MEAT, 0)),
                warning = listOf(entry(3, "Domates", Category.VEGETABLES, 4)),
                fresh = listOf(entry(4, "Pirinç", Category.GRAINS, 120)),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardEmptyPreview() {
    ShelfieTheme {
        DashboardScreen(state = DashboardUiState(isLoading = false), onIntent = {})
    }
}
