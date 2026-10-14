package com.shelfie.app.presentation.dashboard

import androidx.annotation.StringRes
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
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
    val selectedFilter: FreshnessStatus? = null,
    val thresholds: FreshnessThresholds = FreshnessThresholds.DEFAULT,
) {
    val totalCount: Int get() = critical.size + warning.size + fresh.size

    /** Headline number of the summary widget: items that need to be used within days. */
    val expiringSoonCount: Int get() = critical.size

    val isEmpty: Boolean get() = !isLoading && totalCount == 0

    /** Critical items that are not expired yet, i.e. what a rescue recipe can still use. */
    val hasRescuableItems: Boolean get() = critical.any { it.daysLeft >= 0 }

    fun itemsFor(status: FreshnessStatus): List<DashboardItem> = when (status) {
        FreshnessStatus.CRITICAL -> critical
        FreshnessStatus.WARNING -> warning
        FreshnessStatus.FRESH -> fresh
    }

    /** Groups to render in order, honouring [selectedFilter]. */
    val visibleGroups: List<FreshnessStatus>
        get() = selectedFilter?.let(::listOf) ?: FreshnessStatus.entries
}

enum class RemovalReason { CONSUMED, WASTED }

sealed interface DashboardIntent {

    data class SelectFilter(val status: FreshnessStatus?) : DashboardIntent

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
