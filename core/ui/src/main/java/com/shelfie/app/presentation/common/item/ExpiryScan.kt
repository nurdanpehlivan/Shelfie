package com.shelfie.app.presentation.common.item

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.repository.ExpiryDateReader
import com.shelfie.app.domain.repository.PhotoStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Photo of the label → date, then the photo is thrown away: it was only for reading. */
@HiltViewModel
class ExpiryScanViewModel @Inject constructor(
    private val photoStorage: PhotoStorage,
    private val reader: ExpiryDateReader,
    private val clock: Clock,
) : ViewModel() {

    suspend fun captureTarget(): String? = try {
        photoStorage.createCaptureTarget()
    } catch (e: IOException) {
        null
    }

    fun read(imageUri: String, onResult: (LocalDate?) -> Unit) {
        viewModelScope.launch {
            val date = reader.read(imageUri, LocalDate.now(clock))
            photoStorage.delete(imageUri)
            onResult(date)
        }
    }

    fun discard(imageUri: String) {
        viewModelScope.launch { photoStorage.delete(imageUri) }
    }
}

/**
 * "Read the date": takes a photo of the label with the camera app and fills in the printed
 * expiry. Works offline and costs no AI allowance. [onFailed] when no date could be read.
 */
@Composable
fun ExpiryScanButton(
    onDate: (LocalDate) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpiryScanViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    // Survives process death: the camera app may push us out of memory while it's open.
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var reading by rememberSaveable { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingUri ?: return@rememberLauncherForActivityResult
        pendingUri = null
        if (!saved) {
            viewModel.discard(uri)
            return@rememberLauncherForActivityResult
        }
        reading = true
        viewModel.read(uri) { date ->
            reading = false
            if (date != null) onDate(date) else onFailed()
        }
    }

    OutlinedButton(
        onClick = {
            scope.launch {
                val target = viewModel.captureTarget() ?: return@launch onFailed()
                pendingUri = target
                try {
                    camera.launch(Uri.parse(target))
                } catch (e: ActivityNotFoundException) {
                    pendingUri = null
                    viewModel.discard(target)
                    onFailed()
                }
            }
        },
        enabled = !reading,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (reading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(if (reading) R.string.edit_expiry_reading else R.string.edit_expiry_scan),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
