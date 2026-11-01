package com.shelfie.app.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.ShelfLife
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.domain.model.edited
import com.shelfie.app.domain.model.frozen
import com.shelfie.app.domain.model.halved
import com.shelfie.app.domain.model.hasEstimatedExpiry
import com.shelfie.app.domain.model.opened
import com.shelfie.app.domain.model.thawed
import com.shelfie.app.domain.repository.HouseholdRepository
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.ProductLookupRepository
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val productLookupRepository: ProductLookupRepository,
    private val shoppingListRepository: ShoppingListRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val householdRepository: HouseholdRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<PantryFilter?>(null)
    private val query = MutableStateFlow("")
    private val locationFilter = MutableStateFlow<StorageLocation?>(null)

    /** The three list filters in one flow, since [combine] takes five typed flows at most. */
    private val listFilters = combine(selectedFilter, query, locationFilter, ::ListFilters)

    /**
     * Ids swiped away but possibly not yet gone from the Room stream. Hiding them here keeps a
     * dismissed card from flashing back, and lets it reappear if the write fails.
     */
    private val pendingRemovals = MutableStateFlow<Set<Long>>(emptySet())

    /**
     * Re-checked periodically rather than scheduled for midnight: `delay` doesn't advance while
     * the device sleeps, so a single long delay could leave the groups a day behind.
     */
    private val today: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(TODAY_REFRESH_INTERVAL)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<DashboardUiState> = combine(
        pantryRepository.observePantryItems(),
        today,
        listFilters,
        pendingRemovals,
        settingsRepository.settings.map { it.freshnessThresholds }.distinctUntilChanged(),
    ) { items, today, filters, pending, thresholds ->
        val active = items.filterNot { it.id in pending }
        val needle = filters.query.trim().lowercase(Locale.getDefault())
        val grouped = active
            .asSequence()
            .filter { filters.location == null || it.location == filters.location }
            .filter { needle.isEmpty() || it.name.lowercase(Locale.getDefault()).contains(needle) }
            .map { it.toDashboardItem(today, thresholds) }
            .groupBy { it.status }
        DashboardUiState(
            isLoading = false,
            critical = grouped[FreshnessStatus.CRITICAL].orEmpty(),
            warning = grouped[FreshnessStatus.WARNING].orEmpty(),
            fresh = grouped[FreshnessStatus.FRESH].orEmpty(),
            selectedFilter = filters.freshness,
            thresholds = thresholds,
            query = filters.query,
            locationFilter = filters.location,
            locationCounts = StorageLocation.entries.associateWith { place -> active.count { it.location == place } },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardUiState(),
    )

    /** What the rest of the household did since the user last looked; a badge on the family button. */
    val unseenActivity: StateFlow<Int> = householdRepository.unseenActivityCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Checks the household feed; does nothing useful (and fails quietly) when signed out. */
    fun refreshActivity() {
        viewModelScope.launch { householdRepository.activity() }
    }

    private val _editItem = MutableStateFlow<PantryItem?>(null)

    /** The item whose edit sheet is open, `null` while it is closed. */
    val editItem: StateFlow<PantryItem?> = _editItem.asStateFlow()

    private val _addItem = MutableStateFlow<AddItemState?>(null)

    /** `null` while the "add product" sheet is closed. */
    val addItem: StateFlow<AddItemState?> = _addItem.asStateFlow()

    private var lookupJob: Job? = null

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.SelectFilter -> selectedFilter.value = intent.filter
            is DashboardIntent.Search -> query.value = intent.query
            is DashboardIntent.SelectLocation -> locationFilter.value = intent.location
            is DashboardIntent.OpenItem -> _editItem.value = uiState.value.findItem(intent.itemId)
            DashboardIntent.DismissItem -> _editItem.value = null
            is DashboardIntent.SaveItemEdits -> saveItemEdits(intent)
            is DashboardIntent.UseHalf -> useHalf(intent.itemId)
            is DashboardIntent.Freeze -> freeze(intent.itemId)
            is DashboardIntent.Thaw -> thaw(intent.itemId)
            is DashboardIntent.MarkOpened -> markOpened(intent.itemId)
            is DashboardIntent.AddToShoppingList -> addToShoppingList(intent.itemId)
            is DashboardIntent.UndoChange -> updateItem(intent.previous, onSuccess = null)
            is DashboardIntent.MarkConsumed -> removeItem(intent.itemId, RemovalReason.CONSUMED)
            is DashboardIntent.MarkWasted -> removeItem(intent.itemId, RemovalReason.WASTED)
            is DashboardIntent.UndoRemoval -> undoRemoval(intent.itemId)
            DashboardIntent.ScanNewPhoto -> sendEffect(DashboardEffect.NavigateToScanner)
            DashboardIntent.OpenRescueRecipe -> sendEffect(DashboardEffect.NavigateToRecipe)
            DashboardIntent.OpenSettings -> sendEffect(DashboardEffect.NavigateToSettings)
            DashboardIntent.OpenAccount -> sendEffect(DashboardEffect.NavigateToAccount)
            DashboardIntent.OpenAddItem -> _addItem.value = AddItemState(draft = emptyDraft())
            DashboardIntent.DismissAddItem -> dismissAddItem()
            is DashboardIntent.BarcodeScanned -> lookUpBarcode(intent.barcode)
            DashboardIntent.BarcodeScannerUnavailable ->
                _addItem.update { it?.copy(messageRes = R.string.add_item_scanner_unavailable) }
            is DashboardIntent.SaveNewItem -> saveNewItem(intent)
        }
    }

    private fun removeItem(itemId: Long, reason: RemovalReason) {
        // SwipeToDismissBox may report the same dismissal more than once.
        if (itemId in pendingRemovals.value) return
        val name = uiState.value.findItem(itemId)?.name ?: return
        pendingRemovals.update { it + itemId }

        viewModelScope.launch {
            try {
                when (reason) {
                    RemovalReason.CONSUMED -> pantryRepository.markAsConsumed(itemId)
                    RemovalReason.WASTED -> pantryRepository.markAsWasted(itemId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pendingRemovals.update { it - itemId }
                _effects.send(DashboardEffect.ShowMessage(R.string.dashboard_error_update_failed))
                return@launch
            }
            _effects.send(DashboardEffect.ShowRemovalUndo(itemId, name, reason))
        }
    }

    private fun undoRemoval(itemId: Long) {
        viewModelScope.launch {
            try {
                pantryRepository.restoreToPantry(itemId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effects.send(DashboardEffect.ShowMessage(R.string.dashboard_error_update_failed))
                return@launch
            }
            pendingRemovals.update { it - itemId }
        }
    }

    private fun emptyDraft(): PantryItem {
        val today = LocalDate.now(clock)
        val now = clock.instant()
        return PantryItem(
            name = "",
            category = Category.OTHER,
            purchaseDate = today,
            expiryDate = today.plusDays(Category.OTHER.defaultShelfLifeDays.toLong()),
            isExpiryEstimated = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun dismissAddItem() {
        lookupJob?.cancel()
        _addItem.value = null
    }

    /** Open Food Facts lookup; a hit pre-fills name, category and a category-based expiry. */
    private fun lookUpBarcode(barcode: String) {
        val current = _addItem.value ?: return
        lookupJob?.cancel()
        _addItem.value = current.copy(isLookingUp = true, messageRes = null)
        lookupJob = viewModelScope.launch {
            val result = productLookupRepository.lookup(barcode)
            _addItem.update { state ->
                state ?: return@update null
                val product = result.getOrNull()
                when {
                    result.isFailure -> state.copy(isLookingUp = false, messageRes = R.string.add_item_lookup_failed)
                    product == null -> state.copy(isLookingUp = false, messageRes = R.string.add_item_not_found)
                    else -> state.copy(
                        isLookingUp = false,
                        messageRes = R.string.add_item_found,
                        draft = state.draft.copy(
                            name = product.name,
                            category = product.category,
                            location = product.category.defaultLocation,
                            expiryDate = state.draft.purchaseDate.plusDays(
                                ShelfLife.estimateDays(product.name, product.category, product.category.defaultLocation).toLong(),
                            ),
                            isExpiryEstimated = true,
                        ),
                    )
                }
            }
        }
    }

    private fun saveNewItem(intent: DashboardIntent.SaveNewItem) {
        val draft = _addItem.value?.draft ?: emptyDraft()
        dismissAddItem()
        viewModelScope.launch {
            try {
                val item = draft.copy(
                    name = intent.name,
                    quantity = intent.quantity,
                    unit = intent.unit,
                    category = intent.category,
                    expiryDate = intent.expiryDate,
                    location = intent.location,
                )
                // Kept the date the sheet worked out for the product: still an estimate.
                pantryRepository.addPantryItem(item.copy(isExpiryEstimated = item.hasEstimatedExpiry()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effects.send(DashboardEffect.ShowMessage(R.string.dashboard_error_update_failed))
                return@launch
            }
            _effects.send(DashboardEffect.ItemAdded(intent.name))
        }
    }

    private fun saveItemEdits(intent: DashboardIntent.SaveItemEdits) {
        val item = uiState.value.findItem(intent.itemId) ?: return
        _editItem.value = null
        updateItem(
            item.edited(intent.name, intent.quantity, intent.unit, intent.category, intent.expiryDate, intent.location),
            DashboardEffect.ShowItemMessage(R.string.dashboard_item_updated, intent.name, undo = item),
        )
    }

    private fun useHalf(itemId: Long) {
        val item = uiState.value.findItem(itemId) ?: return
        _editItem.value = null
        updateItem(
            item.halved(),
            DashboardEffect.ShowItemMessage(R.string.dashboard_item_half_used, item.name, undo = item),
        )
    }

    private fun freeze(itemId: Long) {
        val item = uiState.value.findItem(itemId) ?: return
        _editItem.value = null
        val frozen = item.frozen(LocalDate.now(clock))
        updateItem(
            frozen,
            DashboardEffect.ShowItemMessage(
                R.string.dashboard_item_frozen,
                item.name,
                frozen.expiryDate.format(DATE_FORMAT),
                undo = item,
            ),
        )
    }

    private fun thaw(itemId: Long) {
        val item = uiState.value.findItem(itemId) ?: return
        _editItem.value = null
        val thawed = item.thawed(LocalDate.now(clock))
        updateItem(
            thawed,
            DashboardEffect.ShowItemMessage(
                R.string.dashboard_item_thawed,
                item.name,
                thawed.expiryDate.format(DATE_FORMAT),
                undo = item,
            ),
        )
    }

    private fun markOpened(itemId: Long) {
        val item = uiState.value.findItem(itemId) ?: return
        _editItem.value = null
        val opened = item.opened(LocalDate.now(clock))
        updateItem(
            opened,
            DashboardEffect.ShowItemMessage(
                R.string.dashboard_item_opened,
                item.name,
                opened.expiryDate.format(DATE_FORMAT),
                undo = item,
            ),
        )
    }

    private fun addToShoppingList(itemId: Long) {
        val item = uiState.value.findItem(itemId) ?: return
        _editItem.value = null
        viewModelScope.launch {
            val added = try {
                shoppingListRepository.addItems(listOf(item.name to ""))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Adding ${item.id} to the shopping list failed", e)
                _effects.send(DashboardEffect.ShowMessage(R.string.dashboard_error_update_failed))
                return@launch
            }
            _effects.send(
                DashboardEffect.ShowItemMessage(
                    if (added > 0) R.string.dashboard_item_added_to_list else R.string.dashboard_item_already_on_list,
                    item.name,
                ),
            )
        }
    }

    private fun updateItem(item: PantryItem, onSuccess: DashboardEffect?) {
        viewModelScope.launch {
            try {
                pantryRepository.updatePantryItem(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Updating item ${item.id} failed", e)
                _effects.send(DashboardEffect.ShowMessage(R.string.dashboard_error_update_failed))
                return@launch
            }
            onSuccess?.let { _effects.send(it) }
        }
    }

    private fun sendEffect(effect: DashboardEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun DashboardUiState.findItem(itemId: Long): PantryItem? =
        sequenceOf(critical, warning, fresh).flatten().firstOrNull { it.item.id == itemId }?.item

    private fun PantryItem.toDashboardItem(today: LocalDate, thresholds: FreshnessThresholds) = DashboardItem(
        item = this,
        daysLeft = ChronoUnit.DAYS.between(today, expiryDate),
        status = freshnessStatus(today, thresholds),
    )

    private data class ListFilters(
        val freshness: PantryFilter?,
        val query: String,
        val location: StorageLocation?,
    )

    private companion object {
        const val TAG = "DashboardViewModel"
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val TODAY_REFRESH_INTERVAL = 1.minutes
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}
