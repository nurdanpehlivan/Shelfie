package com.shelfie.app.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.domain.model.canFreeze
import com.shelfie.app.domain.model.canMarkOpened
import com.shelfie.app.presentation.common.item.EditItemSheet
import com.shelfie.app.presentation.dashboard.DashboardIntent
import java.time.LocalDate

/**
 * Edit sheet of a pantry item, with one-tap actions on top for the most common changes:
 * using part of it, freezing it before it spoils (when it freezes well) or thawing it,
 * marking it opened, and putting it on the shopping list.
 */
@Composable
fun ItemEditSheet(item: PantryItem, onIntent: (DashboardIntent) -> Unit) {
    EditItemSheet(
        item = item,
        onDismiss = { onIntent(DashboardIntent.DismissItem) },
        onSave = { name, quantity, unit, category, expiryDate, location ->
            onIntent(DashboardIntent.SaveItemEdits(item.id, name, quantity, unit, category, expiryDate, location))
        },
        headerContent = {
            val today = remember { LocalDate.now() }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAction("½ " + stringResource(R.string.dashboard_action_use_half)) { onIntent(DashboardIntent.UseHalf(item.id)) }
                when {
                    item.location == StorageLocation.FREEZER ->
                        QuickAction("💧 " + stringResource(R.string.dashboard_action_thaw)) { onIntent(DashboardIntent.Thaw(item.id)) }
                    item.canFreeze ->
                        QuickAction("❄ " + stringResource(R.string.dashboard_action_freeze)) { onIntent(DashboardIntent.Freeze(item.id)) }
                }
                if (item.canMarkOpened(today)) {
                    QuickAction("📦 " + stringResource(R.string.dashboard_action_opened)) { onIntent(DashboardIntent.MarkOpened(item.id)) }
                }
                QuickAction("🛒 " + stringResource(R.string.dashboard_action_add_to_list)) {
                    onIntent(DashboardIntent.AddToShoppingList(item.id))
                }
            }
            Text(
                text = stringResource(R.string.dashboard_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Text(label, maxLines = 1)
    }
}
