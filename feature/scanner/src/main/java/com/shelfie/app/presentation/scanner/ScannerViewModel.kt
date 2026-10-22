package com.shelfie.app.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.model.edited
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.PhotoStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ScannerEffect>(Channel.BUFFERED)
    val effects: Flow<ScannerEffect> = _effects.receiveAsFlow()

    /** Import + analysis of the current photo; replaced when a new photo arrives. */
    private var scanJob: Job? = null

    /** Stored photo of the current scan that is not referenced by any saved item yet. */
    private var unsavedPhotoUri: String? = null

    private var nextItemKey = 0L

    fun onIntent(intent: ScannerIntent) {
        when (intent) {
            ScannerIntent.OpenCamera -> openCamera()
            ScannerIntent.OpenPhotoPicker -> sendEffect(ScannerEffect.LaunchPhotoPicker)
            is ScannerIntent.CaptureImage -> captureImage(intent.imageUri)
            is ScannerIntent.CameraCaptureCancelled -> deletePhoto(intent.imageUri)
            is ScannerIntent.EditItem -> editItem(intent)
            is ScannerIntent.DeleteItem -> deleteItem(intent.key)
            ScannerIntent.ConfirmItems -> confirmItems()
            ScannerIntent.RetryAnalysis -> retryAnalysis()
            ScannerIntent.Reset -> reset()
        }
    }

    private fun openCamera() {
        viewModelScope.launch {
            val target = try {
                photoStorage.createCaptureTarget()
            } catch (e: IOException) {
                _effects.send(ScannerEffect.ShowMessage(R.string.scanner_error_storage))
                return@launch
            }
            _effects.send(ScannerEffect.LaunchCamera(target))
        }
    }

    private fun captureImage(sourceUri: String) {
        if (isSaving()) return
        scanJob?.cancel()
        discardUnsavedPhoto()
        // Show the photo right away; importing a large picker image can take a moment.
        _uiState.value = ScannerUiState.Analyzing(sourceUri)
        scanJob = viewModelScope.launch {
            val storedUri = try {
                photoStorage.importPhoto(sourceUri)
            } catch (e: IOException) {
                showImportFailure()
                return@launch
            } catch (e: SecurityException) {
                showImportFailure()
                return@launch
            }
            unsavedPhotoUri = storedUri
            analyze(storedUri)
        }
    }

    private fun retryAnalysis() {
        val state = _uiState.value as? ScannerUiState.Error ?: return
        val imageUri = state.imageUri ?: return
        if (!state.canRetry) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch { analyze(imageUri) }
    }

    private suspend fun analyze(imageUri: String) {
        _uiState.value = ScannerUiState.Analyzing(imageUri)
        pantryRepository.scanPantryImage(imageUri)
            .onSuccess { items ->
                _uiState.value = ScannerUiState.Success(
                    imageUri = imageUri,
                    items = items.map { DetectedItem(key = nextItemKey++, item = it) },
                )
            }
            .onFailure { error ->
                _uiState.value = ScannerUiState.Error(
                    imageUri = imageUri,
                    messageRes = error.toScanMessageRes(),
                    canRetry = error.isRetryableScanFailure(),
                    canUpgrade = error is PantryScanException.QuotaExceeded && !error.isPremium,
                )
            }
    }

    private fun editItem(intent: ScannerIntent.EditItem) {
        val name = intent.name.trim()
        if (name.isEmpty() || intent.quantity <= 0.0) return
        updateEditableItems { items ->
            items.map { detected ->
                if (detected.key != intent.key) return@map detected
                detected.copy(
                    item = detected.item.edited(name, intent.quantity, intent.unit, intent.category, intent.expiryDate, intent.location),
                )
            }
        }
    }

    private fun deleteItem(key: Long) {
        updateEditableItems { items -> items.filterNot { it.key == key } }
    }

    private fun confirmItems() {
        val state = _uiState.value as? ScannerUiState.Success ?: return
        if (state.isSaving || state.items.isEmpty()) return
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            try {
                pantryRepository.addPantryItems(state.items.map { it.item })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { current ->
                    (current as? ScannerUiState.Success)?.copy(isSaving = false) ?: current
                }
                _effects.send(ScannerEffect.ShowMessage(R.string.scanner_error_save_failed))
                return@launch
            }
            // The photo is now referenced by saved items, so it must outlive this scan.
            unsavedPhotoUri = null
            _uiState.value = ScannerUiState.Idle
            _effects.send(ScannerEffect.ItemsSaved(state.items.size))
        }
    }

    private fun reset() {
        if (isSaving()) return
        scanJob?.cancel()
        discardUnsavedPhoto()
        _uiState.value = ScannerUiState.Idle
    }

    private fun updateEditableItems(transform: (List<DetectedItem>) -> List<DetectedItem>) {
        _uiState.update { state ->
            if (state is ScannerUiState.Success && !state.isSaving) {
                state.copy(items = transform(state.items))
            } else {
                state
            }
        }
    }

    private fun showImportFailure() {
        _uiState.value = ScannerUiState.Error(
            imageUri = null,
            messageRes = R.string.scanner_error_image_unreadable,
            canRetry = false,
        )
    }

    private fun discardUnsavedPhoto() {
        unsavedPhotoUri?.let(::deletePhoto)
        unsavedPhotoUri = null
    }

    private fun deletePhoto(uri: String) {
        viewModelScope.launch { photoStorage.delete(uri) }
    }

    private fun isSaving(): Boolean = (_uiState.value as? ScannerUiState.Success)?.isSaving == true

    private fun sendEffect(effect: ScannerEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
