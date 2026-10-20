package com.shelfie.app.presentation.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.R
import com.shelfie.app.domain.model.ShoppingItem
import com.shelfie.app.presentation.common.ScreenHeader
import com.shelfie.app.presentation.common.SectionHeader
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.extendedColors

@Composable
fun ShoppingListRoute(viewModel: ShoppingListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingListScreen(
        state = state,
        onAdd = { viewModel.add(it) },
        onCheckedChange = viewModel::setChecked,
        onDelete = viewModel::delete,
        onClearBought = viewModel::clearBought,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingListScreen(
    state: ShoppingListUiState,
    onAdd: (String) -> Unit,
    onCheckedChange: (ShoppingItem, Boolean) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onClearBought: () -> Unit,
) {
    var newItem by rememberSaveable { mutableStateOf("") }
    fun submit() {
        onAdd(newItem)
        newItem = ""
    }

    Scaffold { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.shopping_title),
                    actions = {
                        if (state.bought.isNotEmpty()) {
                            TextButton(onClick = onClearBought) { Text(stringResource(R.string.shopping_clear_bought)) }
                        }
                    },
                )
            }
            item(key = "input") {
                // A pill-shaped composer, like a chat input: type, then tap + or Done.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newItem,
                        onValueChange = { newItem = it },
                        placeholder = { Text(stringResource(R.string.shopping_add_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.extendedColors.glassBorder, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = ::submit,
                        enabled = newItem.isNotBlank(),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.shopping_add))
                    }
                }
            }

            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestionsTitle") { SectionHeader(stringResource(R.string.shopping_suggestions)) }
                item(key = "suggestions") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.suggestions.forEach { name ->
                            SuggestionChip(
                                onClick = { onAdd(name) },
                                label = { Text("+ $name") },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                                border = null,
                            )
                        }
                    }
                }
            }

            item(key = "toBuyTitle") {
                SectionHeader(pluralStringResource(R.plurals.shopping_to_buy, state.toBuy.size, state.toBuy.size))
            }
            if (state.toBuy.isEmpty()) {
                item(key = "empty") {
                    ShelfieCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = null,
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Frizzy(mood = FrizzyMood.SLEEPY, size = 64.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.shopping_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(state.toBuy, key = { it.id }) { item ->
                ShoppingRow(item, onCheckedChange, onDelete, Modifier.animateItem())
            }

            if (state.bought.isNotEmpty()) {
                item(key = "boughtTitle") { SectionHeader(stringResource(R.string.shopping_bought)) }
                items(state.bought, key = { it.id }) { item ->
                    ShoppingRow(item, onCheckedChange, onDelete, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onCheckedChange: (ShoppingItem, Boolean) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bought items sink into the page: no border, page-tinted, struck through.
    ShelfieCard(
        onClick = { onCheckedChange(item, !item.isChecked) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = if (item.isChecked) MaterialTheme.colorScheme.surfaceContainerLow else Color.Unspecified,
        border = if (item.isChecked) null else BorderStroke(1.dp, MaterialTheme.extendedColors.glassBorder),
    ) {
    Row(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onCheckedChange(item, it) },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (item.amount.isNotEmpty()) {
                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = { onDelete(item) }) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    }
}

