package com.shelfie.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.ProductLookupRepository
import com.shelfie.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val productLookupRepository: ProductLookupRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<FreshnessStatus?>(null)

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
        selectedFilter,
        pendingRemovals,
        settingsRepository.settings.map { it.freshnessThresholds }.distinctUntilChanged(),
    ) { items, today, filter, pending, thresholds ->
        val grouped = items
            .asSequence()
            .filterNot { it.id in pending }
            .map { it.toDashboardItem(today, thresholds) }
            .groupBy { it.status }
        DashboardUiState(
            isLoading = false,
            critical = grouped[FreshnessStatus.CRITICAL].orEmpty(),
            warning = grouped[FreshnessStatus.WARNING].orEmpty(),
            fresh = grouped[FreshnessStatus.FRESH].orEmpty(),
            selectedFilter = filter,
            thresholds = thresholds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardUiState(),
    )

    private val _addItem = MutableStateFlow<AddItemState?>(null)

    /** `null` while the "add product" sheet is closed. */
    val addItem: StateFlow<AddItemState?> = _addItem.asStateFlow()

    private var lookupJob: Job? = null

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.SelectFilter -> selectedFilter.value = intent.status
            is DashboardIntent.MarkConsumed -> removeItem(intent.itemId, RemovalReason.CONSUMED)
            is DashboardIntent.MarkWasted -> removeItem(intent.itemId, RemovalReason.WASTED)
            is DashboardIntent.UndoRemoval -> undoRemoval(intent.itemId)
            DashboardIntent.ScanNewPhoto -> sendEffect(DashboardEffect.NavigateToScanner)
            DashboardIntent.OpenRescueRecipe -> sendEffect(DashboardEffect.NavigateToRecipe)
            DashboardIntent.OpenSettings -> sendEffect(DashboardEffect.NavigateToSettings)
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
                            expiryDate = state.draft.purchaseDate
                                .plusDays(product.category.defaultShelfLifeDays.toLong()),
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
                pantryRepository.addPantryItem(
                    draft.copy(
                        name = intent.name,
                        quantity = intent.quantity,
                        unit = intent.unit,
                        category = intent.category,
                        expiryDate = intent.expiryDate,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effects.send(DashboardEffect.ShowMessage(R.string.dashboard_error_update_failed))
                return@launch
            }
            _effects.send(DashboardEffect.ItemAdded(intent.name))
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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val TODAY_REFRESH_INTERVAL = 1.minutes
    }
}
