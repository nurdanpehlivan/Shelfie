package com.shelfie.app.presentation.scanner.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.shelfie.app.domain.model.ShelfLife
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.formatQuantity
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.common.parseQuantity
import com.shelfie.app.presentation.common.shelfieFilterChipColors
import com.shelfie.app.presentation.dashboard.components.daysLeftText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

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
        location: StorageLocation,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Keyed by [item] so a new prefill (e.g. a scanned barcode) replaces the fields in place.
    var name by rememberSaveable(item) { mutableStateOf(item.name) }
    var quantityText by rememberSaveable(item) { mutableStateOf(formatQuantity(item.quantity)) }
    var unit by rememberSaveable(item) { mutableStateOf(item.unit) }
    var category by rememberSaveable(item) { mutableStateOf(item.category) }
    var location by rememberSaveable(item) { mutableStateOf(item.location) }
    // Until the user picks a place, it follows the category (milk → fridge, rice → pantry).
    var locationTouched by rememberSaveable(item) { mutableStateOf(item.id != PantryItem.NEW_ITEM_ID) }
    var expiryEpochDay by rememberSaveable(item) { mutableLongStateOf(item.expiryDate.toEpochDay()) }
    // A date read from a label or chosen by the user is kept; an estimate follows the product.
    var expiryTouched by rememberSaveable(item) { mutableStateOf(!item.isExpiryEstimated && item.name.isNotBlank()) }

    fun reestimate() {
        if (expiryTouched || name.isBlank()) return
        val days = ShelfLife.estimateDays(name.trim(), category, location)
        expiryEpochDay = item.purchaseDate.plusDays(days.toLong()).toEpochDay()
    }

    val quantity = parseQuantity(quantityText)
    val isQuantityValid = quantity != null && quantity > 0.0
    val isValid = name.isNotBlank() && isQuantityValid

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
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
                style = MaterialTheme.typography.headlineSmall,
            )
            headerContent()

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    reestimate()
                },
                label = { Text(stringResource(R.string.scanner_edit_name)) },
                isError = name.isBlank(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                shape = MaterialTheme.shapes.small,
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
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(R.string.scanner_edit_unit)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurementUnit.entries.forEach { option ->
                    FilterChip(
                        selected = option == unit,
                        onClick = { unit = option },
                        label = { Text(stringResource(option.labelRes)) },
                        shape = CircleShape,
                        colors = shelfieFilterChipColors(),
                    )
                }
            }

            SectionLabel(R.string.scanner_edit_category)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { option ->
                    FilterChip(
                        selected = option == category,
                        onClick = {
                            category = option
                            if (!locationTouched) location = option.defaultLocation
                            reestimate()
                        },
                        label = { Text("${option.emoji} ${stringResource(option.labelRes)}") },
                        shape = CircleShape,
                        colors = shelfieFilterChipColors(),
                    )
                }
            }

            SectionLabel(R.string.edit_location)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StorageLocation.entries.forEach { option ->
                    FilterChip(
                        selected = option == location,
                        onClick = {
                            location = option
                            locationTouched = true
                            reestimate()
                        },
                        label = { Text(stringResource(option.labelRes)) },
                        shape = CircleShape,
                        colors = shelfieFilterChipColors(),
                    )
                }
            }

            ExpiryPicker(
                expiry = LocalDate.ofEpochDay(expiryEpochDay),
                onExpiryChange = {
                    expiryEpochDay = it.toEpochDay()
                    expiryTouched = true
                },
                isEstimate = !expiryTouched,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = ::closeSheet, modifier = Modifier.height(52.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = isValid,
                    modifier = Modifier.weight(1f).height(52.dp),
                    onClick = {
                        onSave(
                            name.trim(),
                            requireNotNull(quantity),
                            unit,
                            category,
                            LocalDate.ofEpochDay(expiryEpochDay),
                            location,
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

/**
 * Expiry date: tap the date for a calendar, nudge it a day with − / +, or pick a common shelf
 * life in one tap. Shows how many days that leaves, so the date never has to be worked out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryPicker(expiry: LocalDate, onExpiryChange: (LocalDate) -> Unit, isEstimate: Boolean) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val today = remember { LocalDate.now() }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.scanner_edit_expiry),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = daysLeftText(ChronoUnit.DAYS.between(today, expiry)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { onExpiryChange(expiry.minusDays(1)) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.scanner_edit_expiry_minus),
                )
            }
            Surface(
                onClick = { showCalendar = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = expiry.format(dateFormatter), style = MaterialTheme.typography.titleMedium)
                }
            }
            FilledTonalIconButton(onClick = { onExpiryChange(expiry.plusDays(1)) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.scanner_edit_expiry_plus),
                )
            }
        }
        if (isEstimate) {
            Text(
                text = stringResource(R.string.edit_expiry_estimate_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                3L to R.string.edit_expiry_in_3_days,
                7L to R.string.edit_expiry_in_week,
                30L to R.string.edit_expiry_in_month,
                90L to R.string.edit_expiry_in_3_months,
            ).forEach { (days, labelRes) ->
                val date = today.plusDays(days)
                FilterChip(
                    selected = expiry == date,
                    onClick = { onExpiryChange(date) },
                    label = { Text(stringResource(labelRes)) },
                    shape = CircleShape,
                    colors = shelfieFilterChipColors(),
                )
            }
        }
    }

    if (showCalendar) {
        // DatePicker works in UTC millis; converting at UTC keeps the chosen day exact.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiry.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onExpiryChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showCalendar = false
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
