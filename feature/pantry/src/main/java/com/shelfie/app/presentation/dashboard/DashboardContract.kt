package com.shelfie.app.presentation.dashboard

import androidx.annotation.StringRes
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.StorageLocation
import java.time.LocalDate

/** A pantry item with its freshness already worked out for "today". */
data class DashboardItem(
    val item: PantryItem,
    /** Negative once expired, 0 on the expiry day. */
    val daysLeft: Long,
    val status: FreshnessStatus,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val critical: List<DashboardItem> = emptyList(),
    val warning: List<DashboardItem> = emptyList(),
    val fresh: List<DashboardItem> = emptyList(),
    /** `null` shows every group. */
    val selectedFilter: PantryFilter? = null,
    val thresholds: FreshnessThresholds = FreshnessThresholds.DEFAULT,
    /** Name search; blank shows everything. */
    val query: String = "",
    /** `null` shows every storage place. */
    val locationFilter: StorageLocation? = null,
    /** Active items per place, ignoring the place filter and search, for the chip counts. */
    val locationCounts: Map<StorageLocation, Int> = emptyMap(),
) {
    /** Items shown after the place filter and search. */
    val totalCount: Int get() = critical.size + warning.size + fresh.size

    /** Every active item, whatever the filters. */
    val allCount: Int get() = locationCounts.values.sum()

    val isFiltered: Boolean get() = query.isNotBlank() || locationFilter != null

    /** Headline number of the summary widget: items that need to be used within days. */
    val expiringSoonCount: Int get() = critical.size

    /** The pantry itself is empty (not just the current search or place filter). */
    val isEmpty: Boolean get() = !isLoading && allCount == 0

    /** Critical items that are not expired yet, i.e. what a rescue recipe can still use. */
    val hasRescuableItems: Boolean get() = critical.any { it.daysLeft >= 0 }

    fun itemsFor(status: FreshnessStatus): List<DashboardItem> = when (status) {
        FreshnessStatus.CRITICAL -> critical
        FreshnessStatus.WARNING -> warning
        FreshnessStatus.FRESH -> fresh
    }

    /** Items to use up soon: critical plus warning, the second overview card. */
    val useSoonCount: Int get() = critical.size + warning.size

    /** Groups to render in order, honouring [selectedFilter]. */
    val visibleGroups: List<FreshnessStatus>
        get() = selectedFilter?.groups ?: FreshnessStatus.entries
}

/** The two overview cards double as list filters; each shows one or more freshness groups. */
enum class PantryFilter(val groups: List<FreshnessStatus>) {
    SAFE(listOf(FreshnessStatus.FRESH)),
    USE_SOON(listOf(FreshnessStatus.CRITICAL, FreshnessStatus.WARNING)),
}

enum class RemovalReason { CONSUMED, WASTED }

sealed interface DashboardIntent {

    data class SelectFilter(val filter: PantryFilter?) : DashboardIntent

    data class Search(val query: String) : DashboardIntent

    data class SelectLocation(val location: StorageLocation?) : DashboardIntent

    /** Tapped a card: opens its edit sheet. */
    data class OpenItem(val itemId: Long) : DashboardIntent

    data object DismissItem : DashboardIntent

    data class SaveItemEdits(
        val itemId: Long,
        val name: String,
        val quantity: Double,
        val unit: MeasurementUnit,
        val category: Category,
        val expiryDate: LocalDate,
        val location: StorageLocation,
    ) : DashboardIntent

    /** Part of the item was used; halves the quantity left. */
    data class UseHalf(val itemId: Long) : DashboardIntent

    /** Moves the item to the freezer and extends its expiry to the frozen shelf life. */
    data class Freeze(val itemId: Long) : DashboardIntent

    /** Takes the item out of the freezer to thaw in the fridge, with a short date. */
    data class Thaw(val itemId: Long) : DashboardIntent

    /** Puts an item back the way it was before a one-tap change. */
    data class UndoChange(val previous: PantryItem) : DashboardIntent

    /** Swiped right. */
    data class MarkConsumed(val itemId: Long) : DashboardIntent

    /** Swiped left. */
    data class MarkWasted(val itemId: Long) : DashboardIntent

    data class UndoRemoval(val itemId: Long) : DashboardIntent

    data object ScanNewPhoto : DashboardIntent

    data object OpenRescueRecipe : DashboardIntent

    data object OpenSettings : DashboardIntent

    data object OpenAddItem : DashboardIntent

    data object DismissAddItem : DashboardIntent

    data class BarcodeScanned(val barcode: String) : DashboardIntent

    /** The barcode scanner itself failed, e.g. its module is still downloading. */
    data object BarcodeScannerUnavailable : DashboardIntent

    data class SaveNewItem(
        val name: String,
        val quantity: Double,
        val unit: MeasurementUnit,
        val category: Category,
        val expiryDate: LocalDate,
        val location: StorageLocation,
    ) : DashboardIntent
}

sealed interface DashboardEffect {

    data object NavigateToScanner : DashboardEffect

    data object NavigateToRecipe : DashboardEffect

    data object NavigateToSettings : DashboardEffect

    data class ItemAdded(val itemName: String) : DashboardEffect

    /** Item left the pantry; the UI offers an undo. */
    data class ShowRemovalUndo(
        val itemId: Long,
        val itemName: String,
        val reason: RemovalReason,
    ) : DashboardEffect

    data class ShowMessage(@param:StringRes val messageRes: Int) : DashboardEffect

    /**
     * A message about one item; [messageRes] takes the item name (and [extra]) as arguments.
     * With [undo], the UI offers to put the item back to that state.
     */
    data class ShowItemMessage(
        @param:StringRes val messageRes: Int,
        val itemName: String,
        val extra: String? = null,
        val undo: PantryItem? = null,
    ) : DashboardEffect
}

/**
 * The "add product" sheet. [draft] pre-fills the form and is replaced when a barcode lookup
 * succeeds; [messageRes] explains a lookup that found nothing or failed.
 */
data class AddItemState(
    val draft: PantryItem,
    val isLookingUp: Boolean = false,
    @param:StringRes val messageRes: Int? = null,
)
