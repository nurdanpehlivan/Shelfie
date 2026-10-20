package com.shelfie.app.presentation.scanner

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.shelfie.app.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.presentation.common.BottomActionBar
import com.shelfie.app.presentation.common.Pill
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.scanner.components.DetectedItemCard
import com.shelfie.app.presentation.scanner.components.DetectedItemPlaceholder
import com.shelfie.app.presentation.scanner.components.EditItemSheet
import com.shelfie.app.presentation.scanner.components.scanLineOverlay
import com.shelfie.app.presentation.theme.ShelfieTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

/**
 * Stateful entry point: wires the [ScannerViewModel] to the camera / Photo Picker launchers and
 * turns one-off [ScannerEffect]s into launches, snackbars and navigation.
 */
@Composable
fun ScannerRoute(
    onItemsSaved: (count: Int) -> Unit = {},
    onNavigateUp: (() -> Unit)? = null,
    onOpenPremium: () -> Unit = {},
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentOnItemsSaved by rememberUpdatedState(onItemsSaved)

    // Survives process death: the camera app may push us out of memory while it's open.
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = pendingCameraUri ?: return@rememberLauncherForActivityResult
        pendingCameraUri = null
        viewModel.onIntent(
            if (saved) ScannerIntent.CaptureImage(uri) else ScannerIntent.CameraCaptureCancelled(uri),
        )
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onIntent(ScannerIntent.CaptureImage(uri.toString()))
    }

    fun showSnackbar(message: String) {
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Main.immediate so no effect is lost between emission and collection.
            withContext(Dispatchers.Main.immediate) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is ScannerEffect.LaunchCamera -> {
                            pendingCameraUri = effect.outputUri
                            try {
                                cameraLauncher.launch(Uri.parse(effect.outputUri))
                            } catch (e: ActivityNotFoundException) {
                                pendingCameraUri = null
                                viewModel.onIntent(
                                    ScannerIntent.CameraCaptureCancelled(effect.outputUri),
                                )
                                showSnackbar(context.getString(R.string.scanner_error_no_camera))
                            }
                        }

                        ScannerEffect.LaunchPhotoPicker -> photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )

                        is ScannerEffect.ShowMessage ->
                            showSnackbar(context.getString(effect.messageRes))

                        // The caller reports the result, since it usually navigates away.
                        is ScannerEffect.ItemsSaved -> currentOnItemsSaved(effect.count)
                    }
                }
            }
        }
    }

    ScannerScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onOpenPremium = onOpenPremium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    state: ScannerUiState,
    onIntent: (ScannerIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateUp: (() -> Unit)? = null,
    onOpenPremium: () -> Unit = {},
) {
    var editingKey by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ShelfieTopBar(title = stringResource(R.string.scanner_title), onNavigateUp = onNavigateUp)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state is ScannerUiState.Success) ConfirmBar(state = state, onIntent = onIntent)
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "scannerState",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { target ->
            when (target) {
                ScannerUiState.Idle -> IdleContent(onIntent = onIntent)
                is ScannerUiState.Analyzing -> AnalyzingContent(imageUri = target.imageUri)
                is ScannerUiState.Success -> ReviewContent(
                    state = target,
                    onEditItem = { editingKey = it },
                    onIntent = onIntent,
                )
                is ScannerUiState.Error -> ErrorContent(state = target, onIntent = onIntent, onOpenPremium = onOpenPremium)
            }
        }
    }

    val editing = (state as? ScannerUiState.Success)?.items?.firstOrNull { it.key == editingKey }
    if (editing != null) {
        EditItemSheet(
            item = editing.item,
            onDismiss = { editingKey = null },
            onSave = { name, quantity, unit, category, expiryDate, location ->
                onIntent(
                    ScannerIntent.EditItem(
                        key = editing.key,
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        category = category,
                        expiryDate = expiryDate,
                        location = location,
                    ),
                )
            },
        )
    }
}

@Composable
private fun IdleContent(onIntent: (ScannerIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Frizzy(mood = FrizzyMood.HAPPY, size = 160.dp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.scanner_idle_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scanner_idle_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        // What a photo can show: receipts are easy to miss otherwise.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "🧊 " + stringResource(R.string.location_fridge),
                "🗄 " + stringResource(R.string.location_pantry),
                "🧾 " + stringResource(R.string.scanner_source_receipt),
            ).forEach { Pill(text = it) }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = { onIntent(ScannerIntent.OpenCamera) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            ButtonIconLabel(R.drawable.ic_photo_camera, R.string.scanner_action_take_photo)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onIntent(ScannerIntent.OpenPhotoPicker) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            ButtonIconLabel(R.drawable.ic_photo_library, R.string.scanner_action_pick_photo)
        }
    }
}

@Composable
private fun AnalyzingContent(imageUri: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShelfiePhoto(
            imageUri = imageUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .scanLineOverlay(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.scanner_analyzing),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        repeat(PLACEHOLDER_COUNT) { DetectedItemPlaceholder() }
    }
}

@Composable
private fun ReviewContent(
    state: ScannerUiState.Success,
    onEditItem: (key: Long) -> Unit,
    onIntent: (ScannerIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShelfiePhoto(imageUri = state.imageUri, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = pluralStringResource(
                            R.plurals.scanner_items_found,
                            state.items.size,
                            state.items.size,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.scanner_review_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.items.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.scanner_review_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                )
            }
        }

        items(items = state.items, key = { it.key }) { detected ->
            DetectedItemCard(
                item = detected.item,
                enabled = !state.isSaving,
                onEdit = { onEditItem(detected.key) },
                onDelete = { onIntent(ScannerIntent.DeleteItem(detected.key)) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun ConfirmBar(state: ScannerUiState.Success, onIntent: (ScannerIntent) -> Unit) {
    BottomActionBar {
            OutlinedButton(
                onClick = { onIntent(ScannerIntent.Reset) },
                enabled = !state.isSaving,
                modifier = Modifier.height(56.dp),
            ) {
                Text(stringResource(R.string.scanner_action_rescan))
            }
            Button(
                onClick = { onIntent(ScannerIntent.ConfirmItems) },
                enabled = !state.isSaving && state.items.isNotEmpty(),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    ButtonIconLabel(
                        iconRes = R.drawable.ic_check,
                        text = pluralStringResource(
                            R.plurals.scanner_action_confirm,
                            state.items.size,
                            state.items.size,
                        ),
                    )
                }
            }
    }
}

@Composable
private fun ErrorContent(state: ScannerUiState.Error, onIntent: (ScannerIntent) -> Unit, onOpenPremium: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.imageUri != null) {
            ShelfiePhoto(imageUri = state.imageUri, modifier = Modifier.size(120.dp))
            Spacer(modifier = Modifier.height(24.dp))
        }
        Text(
            text = stringResource(R.string.scanner_error_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(state.messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (state.canUpgrade) {
            Button(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("✨ " + stringResource(R.string.premium_upgrade))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (state.canRetry && state.imageUri != null) {
            Button(
                onClick = { onIntent(ScannerIntent.RetryAnalysis) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                ButtonIconLabel(R.drawable.ic_refresh, R.string.scanner_action_retry)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        FilledTonalButton(
            onClick = { onIntent(ScannerIntent.Reset) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.scanner_action_other_photo))
        }
    }
}

@Composable
private fun ShelfiePhoto(imageUri: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageUri,
        contentDescription = stringResource(R.string.scanner_photo_description),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun ButtonIconLabel(iconRes: Int, textRes: Int) {
    ButtonIconLabel(iconRes = iconRes, text = stringResource(textRes))
}

@Composable
private fun ButtonIconLabel(iconRes: Int, text: String) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(text)
}

private const val PLACEHOLDER_COUNT = 3

@Preview(showBackground = true)
@Composable
private fun ScannerIdlePreview() {
    ShelfieTheme {
        ScannerScreen(state = ScannerUiState.Idle, onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerAnalyzingPreview() {
    ShelfieTheme {
        ScannerScreen(state = ScannerUiState.Analyzing(imageUri = ""), onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerReviewPreview() {
    val today = LocalDate.of(2026, 10, 8)
    fun preview(key: Long, name: String, category: Category, unit: MeasurementUnit, days: Long) =
        DetectedItem(
            key = key,
            item = PantryItem(
                name = name,
                category = category,
                unit = unit,
                purchaseDate = today,
                expiryDate = today.plusDays(days),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        )
    ShelfieTheme {
        ScannerScreen(
            state = ScannerUiState.Success(
                imageUri = "",
                items = listOf(
                    preview(0, "Süt", Category.DAIRY, MeasurementUnit.LITER, 5),
                    preview(1, "Domates", Category.VEGETABLES, MeasurementUnit.KILOGRAM, 6),
                    preview(2, "Tavuk göğsü", Category.MEAT, MeasurementUnit.PACKAGE, 2),
                ),
            ),
            onIntent = {},
        )
    }
}
