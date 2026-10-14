package com.shelfie.app.presentation.scanner.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.formatQuantity
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.common.parseQuantity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditItemSheet(
    item: PantryItem,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.scanner_edit_title),
    /** Shown under the title, e.g. the barcode button of the "add product" sheet. */
    headerContent: @Composable () -> Unit = {},
    onSave: (
        name: String,
        quantity: Double,
        unit: MeasurementUnit,
        category: Category,
        expiryDate: LocalDate,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    // Keyed by [item] so a new prefill (e.g. a scanned barcode) replaces the fields in place.
    var name by rememberSaveable(item) { mutableStateOf(item.name) }
    var quantityText by rememberSaveable(item) { mutableStateOf(formatQuantity(item.quantity)) }
    var unit by rememberSaveable(item) { mutableStateOf(item.unit) }
    var category by rememberSaveable(item) { mutableStateOf(item.category) }
    var expiryEpochDay by rememberSaveable(item) { mutableLongStateOf(item.expiryDate.toEpochDay()) }

    val quantity = parseQuantity(quantityText)
    val isQuantityValid = quantity != null && quantity > 0.0
    val isValid = name.isNotBlank() && isQuantityValid

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            headerContent()

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.scanner_edit_name)) },
                isError = name.isBlank(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text(stringResource(R.string.scanner_edit_quantity)) },
                isError = !isQuantityValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(R.string.scanner_edit_unit)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurementUnit.entries.forEach { option ->
                    FilterChip(
                        selected = option == unit,
                        onClick = { unit = option },
                        label = { Text(stringResource(option.labelRes)) },
                    )
                }
            }

            SectionLabel(R.string.scanner_edit_category)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { option ->
                    FilterChip(
                        selected = option == category,
                        onClick = { category = option },
                        label = { Text("${option.emoji} ${stringResource(option.labelRes)}") },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.scanner_edit_expiry),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = { expiryEpochDay-- }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.scanner_edit_expiry_minus),
                    )
                }
                Text(
                    text = LocalDate.ofEpochDay(expiryEpochDay).format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = { expiryEpochDay++ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.scanner_edit_expiry_plus),
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = ::closeSheet) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = isValid,
                    onClick = {
                        onSave(
                            name.trim(),
                            requireNotNull(quantity),
                            unit,
                            category,
                            LocalDate.ofEpochDay(expiryEpochDay),
                        )
                        closeSheet()
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(textRes: Int) {
    Text(text = stringResource(textRes), style = MaterialTheme.typography.titleSmall)
}
