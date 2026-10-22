package com.shelfie.app.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.domain.model.canFreeze
import com.shelfie.app.presentation.dashboard.DashboardIntent
import com.shelfie.app.presentation.scanner.components.EditItemSheet

/**
 * Edit sheet of a pantry item, with one-tap actions on top for the most common changes:
 * using part of it, freezing it before it spoils (when it freezes well), or thawing it.
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onIntent(DashboardIntent.UseHalf(item.id)) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_action_use_half), maxLines = 1)
                }
                when {
                    item.location == StorageLocation.FREEZER ->
                        FilledTonalButton(onClick = { onIntent(DashboardIntent.Thaw(item.id)) }, modifier = Modifier.weight(1f)) {
                            Text("💧 " + stringResource(R.string.dashboard_action_thaw), maxLines = 1)
                        }
                    item.canFreeze ->
                        FilledTonalButton(onClick = { onIntent(DashboardIntent.Freeze(item.id)) }, modifier = Modifier.weight(1f)) {
                            Text("❄ " + stringResource(R.string.dashboard_action_freeze), maxLines = 1)
                        }
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
