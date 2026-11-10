package com.shelfie.app.presentation.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.ItemValue
import com.shelfie.app.domain.model.PantryStatistics
import com.shelfie.app.domain.model.ValuedItem
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.Pill
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.currencySymbol
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.formatQuantity
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.common.parseQuantity
import com.shelfie.app.presentation.common.rememberMoneyFormat
import com.shelfie.app.presentation.theme.extendedColors

/** Which total the sheet explains. */
enum class ValueBreakdown { SAVED, WASTED }

/**
 * Where a money figure comes from: every item behind it with its own amount, how that amount is
 * worked out, and the one number the user can correct (what an average product costs them).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValueBreakdownSheet(
    kind: ValueBreakdown,
    stats: PantryStatistics,
    onAverageValueChange: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val money = rememberMoneyFormat(stats.currencyCode)
    val valuedItems = if (kind == ValueBreakdown.SAVED) stats.savedItems else stats.wastedItems
    val total = if (kind == ValueBreakdown.SAVED) stats.estimatedSavings else stats.wastedValue
    // Only what the user opened or closed; the rest follows the default.
    val expandedOverride = remember(kind) { mutableStateMapOf<Category, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "total") {
                HighlightCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(if (kind == ValueBreakdown.SAVED) R.string.stats_tile_savings else R.string.stats_tile_wasted_value),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.extendedColors.onHeroVariant,
                        )
                        Text(text = money.format(total), style = MaterialTheme.typography.displaySmall)
                        Text(
                            text = stringResource(
                                if (kind == ValueBreakdown.SAVED) R.string.stats_breakdown_saved_hint else R.string.stats_breakdown_wasted_hint,
                                valuedItems.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.extendedColors.onHeroVariant,
                        )
                    }
                }
            }
            item(key = "formula") {
                Text(
                    text = stringResource(R.string.stats_breakdown_formula, money.format(stats.averageItemValue)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item(key = "average") {
                AverageValueEditor(
                    current = stats.averageItemValue,
                    currencyCode = stats.currencyCode,
                    onSave = onAverageValueChange,
                )
            }
            if (valuedItems.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.stats_breakdown_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
                    )
                }
            } else {
                // One collapsible group per category, biggest amount first: a few rows to scan
                // instead of one long list; a short list starts open.
                val groups = valuedItems.groupBy { it.item.category }.entries.sortedByDescending { (_, list) -> list.sumOf { it.value } }
                items(items = groups, key = { "group_${it.key}" }) { (category, list) ->
                    val expanded = expandedOverride[category] ?: (valuedItems.size <= OPEN_ALL_UP_TO)
                    CategoryGroup(
                        category = category,
                        items = list,
                        expanded = expanded,
                        onToggle = { expandedOverride[category] = !expanded },
                        format = money::format,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** A category's line ("🥬 Sebze · 4 ürün · ₺86") that opens to its items. */
@Composable
private fun CategoryGroup(
    category: Category,
    items: List<ValuedItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    ShelfieCard(modifier = modifier.fillMaxWidth(), onClick = onToggle) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = category.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(category.labelRes), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = pluralStringResource(R.plurals.stats_items, items.size, items.size) +
                            " · ×" + formatQuantity(ItemValue.weight(category)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = format(items.sumOf { it.value }), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(if (expanded) 90f else 0f),
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    items.forEach { valued ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        BreakdownRow(valued, format)
                    }
                }
            }
        }
    }
}

private const val OPEN_ALL_UP_TO = 5

/** "🥬 Marul / 1 adet · ×0.6 ............ ₺30", with the item's own emoji, not its category's. */
@Composable
private fun BreakdownRow(valued: ValuedItem, format: (Double) -> String) {
    val item = valued.item
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = item.displayEmoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = formatQuantity(item.quantity) + " " + stringResource(item.unit.labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Pill(text = "×" + formatQuantity(ItemValue.weight(item.category) * ItemValue.purchases(item)))
        Spacer(Modifier.width(10.dp))
        Text(text = format(valued.value), style = MaterialTheme.typography.bodyLarge)
    }
}

/** The average product value, editable in place: every amount on the sheet follows it. */
@Composable
private fun AverageValueEditor(current: Double, currencyCode: String?, onSave: (Double) -> Unit) {
    val focusManager = LocalFocusManager.current
    var text by rememberSaveable(current) { mutableStateOf(formatQuantity(current)) }
    val parsed = parseQuantity(text)
    val valid = parsed != null && parsed > 0.0
    fun save() {
        if (valid && parsed != current) onSave(requireNotNull(parsed))
        focusManager.clearFocus()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.stats_breakdown_average)) },
            suffix = { Text(currencySymbol(currencyCode)) },
            isError = !valid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { save() }),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(onClick = ::save, enabled = valid && parsed != current, modifier = Modifier.height(56.dp)) {
            Text(stringResource(R.string.action_save))
        }
    }
}
