package com.shelfie.app.presentation.shopping

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.Frizzy
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.R
import com.shelfie.app.domain.model.ShoppingItem

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_title)) },
                actions = {
                    if (state.bought.isNotEmpty()) {
                        TextButton(onClick = onClearBought) { Text(stringResource(R.string.shopping_clear_bought)) }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "input") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
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
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(onClick = ::submit, enabled = newItem.isNotBlank()) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.shopping_add))
                    }
                }
            }

            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestionsTitle") { SectionHeader(stringResource(R.string.shopping_suggestions)) }
                item(key = "suggestions") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.suggestions.forEach { name ->
                            SuggestionChip(onClick = { onAdd(name) }, label = { Text("+ $name") })
                        }
                    }
                }
            }

            item(key = "toBuyTitle") {
                SectionHeader(pluralStringResource(R.plurals.shopping_to_buy, state.toBuy.size, state.toBuy.size))
            }
            if (state.toBuy.isEmpty()) {
                item(key = "empty") {
                    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Frizzy(mood = FrizzyMood.SLEEPY, size = 72.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.shopping_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
    Surface(
        onClick = { onCheckedChange(item, !item.isChecked) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (item.isChecked) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = if (item.isChecked) 0.dp else 1.dp,
    ) {
    Row(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = { onCheckedChange(item, it) })
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}
