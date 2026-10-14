package com.shelfie.app.presentation.dashboard.components

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.shelfie.app.R
import com.shelfie.app.presentation.dashboard.AddItemState
import com.shelfie.app.presentation.dashboard.DashboardIntent
import com.shelfie.app.presentation.scanner.components.EditItemSheet

/** "Add product": the regular edit form plus a barcode button that pre-fills it. */
@Composable
fun AddItemSheet(state: AddItemState, onIntent: (DashboardIntent) -> Unit) {
    val scanBarcode = rememberBarcodeScanner(
        onScanned = { onIntent(DashboardIntent.BarcodeScanned(it)) },
        onUnavailable = { onIntent(DashboardIntent.BarcodeScannerUnavailable) },
    )

    EditItemSheet(
        item = state.draft,
        title = stringResource(R.string.add_item_title),
        onDismiss = { onIntent(DashboardIntent.DismissAddItem) },
        onSave = { name, quantity, unit, category, expiryDate ->
            onIntent(DashboardIntent.SaveNewItem(name, quantity, unit, category, expiryDate))
        },
        headerContent = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = scanBarcode, enabled = !state.isLookingUp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_barcode),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_item_scan_barcode))
                }
                Spacer(modifier = Modifier.width(12.dp))
                if (state.isLookingUp) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            state.messageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (messageRes == R.string.add_item_found) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
    )
}

/**
 * Google's code scanner: Play services shows its own camera UI, so the app needs no camera
 * permission. Returns a function that starts a scan; a cancelled scan does nothing.
 */
@Composable
private fun rememberBarcodeScanner(onScanned: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val scanner = remember(context) { createScanner(context) }
    return remember(scanner) {
        {
            scanner.startScan()
                .addOnSuccessListener { barcode -> barcode.rawValue?.let(currentOnScanned) }
                .addOnFailureListener { error ->
                    // CANCELLED is the user backing out, not a failure worth reporting.
                    if ((error as? MlKitException)?.errorCode != MlKitException.CANCELLED) currentOnUnavailable()
                }
        }
    }
}

private fun createScanner(context: Context): GmsBarcodeScanner =
    GmsBarcodeScanning.getClient(
        context,
        GmsBarcodeScannerOptions.Builder()
            // Product barcodes only; QR codes on packaging are usually marketing links.
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .enableAutoZoom()
            .build(),
    )
