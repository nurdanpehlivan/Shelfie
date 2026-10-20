package com.shelfie.app.presentation.recipe.picker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.R
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.presentation.common.BottomActionBar
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.foodEmoji
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.dashboard.components.daysLeftText
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.MascotMessage
import com.shelfie.app.presentation.theme.extendedColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun IngredientPickerRoute(
    onNavigateUp: () -> Unit,
    onCreateRecipe: (ingredientIds: List<Long>) -> Unit,
    viewModel: IngredientPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IngredientPickerScreen(
        state = state,
        onToggle = viewModel::toggle,
        onNavigateUp = onNavigateUp,
        // Soonest expiry first, so the recipe leads with what needs using most.
        onCreateRecipe = { onCreateRecipe(state.items.map { it.id }.filter { it in state.selectedIds }) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerScreen(
    state: IngredientPickerUiState,
    onToggle: (Long) -> Unit,
    onNavigateUp: () -> Unit,
    onCreateRecipe: () -> Unit,
) {
    Scaffold(
        topBar = { ShelfieTopBar(title = stringResource(R.string.picker_title), onNavigateUp = onNavigateUp) },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                BottomActionBar {
                    Button(
                        onClick = onCreateRecipe,
                        enabled = state.selectedIds.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text(
                            if (state.selectedIds.isEmpty()) {
                                stringResource(R.string.picker_select_prompt)
                            } else {
                                pluralStringResource(R.plurals.picker_create, state.selectedIds.size, state.selectedIds.size)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.items.isEmpty() -> MascotMessage(
                mood = FrizzyMood.SLEEPY,
                title = stringResource(R.string.dashboard_empty_title),
                message = stringResource(R.string.picker_empty),
                primaryAction = stringResource(R.string.action_navigate_up) to onNavigateUp,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.picker_hint, MAX_PICKED_INGREDIENTS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(state.items, key = { it.id }) { item ->
                    val selected = item.id in state.selectedIds
                    PickerRow(
                        item = item,
                        selected = selected,
                        enabled = selected || state.canPickMore,
                        onToggle = { onToggle(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(item: PantryItem, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val today = LocalDate.now()
    ShelfieCard(
        onClick = onToggle.takeIf { enabled },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) scheme.primary else MaterialTheme.extendedColors.glassBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(scheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item.displayEmoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${daysLeftText(ChronoUnit.DAYS.between(today, item.expiryDate))} · ${stringResource(item.location.labelRes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = enabled,
                colors = CheckboxDefaults.colors(checkedColor = scheme.primary, uncheckedColor = scheme.outline),
            )
        }
    }
}
