package com.shelfie.app.presentation.scanner

import androidx.annotation.StringRes
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.domain.model.PantryItem
import java.time.LocalDate

/**
 * An unsaved item detected in the photo. Scanned items all share [PantryItem.NEW_ITEM_ID], so
 * [key] identifies them in the review list until they are saved.
 */
data class DetectedItem(val key: Long, val item: PantryItem)

sealed interface ScannerUiState {

    /** Waiting for the user to take or pick a photo. */
    data object Idle : ScannerUiState

    data class Analyzing(val imageUri: String) : ScannerUiState

    /** Detection finished; the user reviews [items] before saving. [items] may be empty. */
    data class Success(
        val imageUri: String,
        val items: List<DetectedItem>,
        val isSaving: Boolean = false,
    ) : ScannerUiState

    data class Error(
        val imageUri: String?,
        @param:StringRes val messageRes: Int,
        val canRetry: Boolean,
        /** The free daily allowance is used up; offer Premium. */
        val canUpgrade: Boolean = false,
    ) : ScannerUiState
}

sealed interface ScannerIntent {

    data object OpenCamera : ScannerIntent

    data object OpenPhotoPicker : ScannerIntent

    /** A photo was taken or picked; starts the analysis. */
    data class CaptureImage(val imageUri: String) : ScannerIntent

    /** The camera app returned without a photo; [imageUri] is the unused capture target. */
    data class CameraCaptureCancelled(val imageUri: String) : ScannerIntent

    data class EditItem(
        val key: Long,
        val name: String,
        val quantity: Double,
        val unit: MeasurementUnit,
        val category: Category,
        val expiryDate: LocalDate,
        val location: StorageLocation,
    ) : ScannerIntent

    data class DeleteItem(val key: Long) : ScannerIntent

    /** Saves every remaining detected item in one go. */
    data object ConfirmItems : ScannerIntent

    data object RetryAnalysis : ScannerIntent

    /** Discards the current scan and returns to [ScannerUiState.Idle]. */
    data object Reset : ScannerIntent
}

sealed interface ScannerEffect {

    data class LaunchCamera(val outputUri: String) : ScannerEffect

    data object LaunchPhotoPicker : ScannerEffect

    data class ShowMessage(@param:StringRes val messageRes: Int) : ScannerEffect

    data class ItemsSaved(val count: Int) : ScannerEffect
}
