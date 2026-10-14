package com.shelfie.app.presentation.history

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import com.shelfie.app.presentation.mascot.MascotMessage
import com.shelfie.app.presentation.mascot.FrizzyMood
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.R
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.presentation.recipe.components.FALLBACK_INGREDIENT_EMOJI
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RecipeHistoryRoute(
    onOpenRecipe: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    viewModel: RecipeHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeHistoryScreen(
        state = state,
        onFilterSelected = viewModel::selectFilter,
        onOpenRecipe = { onOpenRecipe(it.id) },
        onToggleFavorite = viewModel::toggleFavorite,
        onDelete = viewModel::delete,
        onNewRecipe = onNewRecipe,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeHistoryScreen(
    state: RecipeHistoryUiState,
    onFilterSelected: (HistoryFilter) -> Unit,
    onOpenRecipe: (SavedRecipe) -> Unit,
    onToggleFavorite: (SavedRecipe) -> Unit,
    onDelete: (SavedRecipe) -> Unit,
    onNewRecipe: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<SavedRecipe?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            !state.hasAnyRecipe -> EmptyHistory(onNewRecipe, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "filters") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.filter == filter,
                                onClick = { onFilterSelected(filter) },
                                label = { Text(stringResource(filter.labelRes)) },
                            )
                        }
                    }
                }
                if (state.recipes.isEmpty()) {
                    item(key = "emptyFilter") {
                        Text(
                            text = stringResource(R.string.history_filter_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(state.recipes, key = { it.id }) { saved ->
                    HistoryCard(
                        saved = saved,
                        onClick = { onOpenRecipe(saved) },
                        onToggleFavorite = { onToggleFavorite(saved) },
                        onDelete = { pendingDelete = saved },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    pendingDelete?.let { saved ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message, saved.recipe.title)) },
            confirmButton = {
                TextButton(onClick = { onDelete(saved); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryCard(
    saved: SavedRecipe,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = saved.recipe
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val date = remember(saved.createdAt) { saved.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter) }
    val emojis = recipe.rescuedIngredients.mapNotNull { it.emoji }.ifEmpty { listOf(FALLBACK_INGREDIENT_EMOJI) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Up to two ingredient emojis on a mint tile: the recipe's "cover".
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emojis.take(2).joinToString(""), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    MetaChip(
                        "⏱ " + pluralStringResource(
                            R.plurals.recipe_time_minutes,
                            recipe.cookingTimeMinutes,
                            recipe.cookingTimeMinutes,
                        ),
                    )
                    if (saved.isCooked) {
                        MetaChip(
                            "✓ " + stringResource(R.string.recipe_badge_cooked),
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        painter = painterResource(if (saved.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                        contentDescription = stringResource(
                            if (saved.isFavorite) R.string.recipe_action_unfavorite else R.string.recipe_action_favorite,
                        ),
                        tint = if (saved.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(onClick = onDelete) {
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
}

@Composable
private fun MetaChip(
    text: String,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyHistory(onNewRecipe: () -> Unit, modifier: Modifier = Modifier) {
    MascotMessage(
        mood = FrizzyMood.CHEF,
        title = stringResource(R.string.history_empty_title),
        message = stringResource(R.string.history_empty_subtitle),
        primaryAction = stringResource(R.string.dashboard_rescue_action) to onNewRecipe,
        modifier = modifier,
    )
}

private val HistoryFilter.labelRes: Int
    get() = when (this) {
        HistoryFilter.ALL -> R.string.history_filter_all
        HistoryFilter.FAVORITES -> R.string.history_filter_favorites
        HistoryFilter.COOKED -> R.string.history_filter_cooked
    }
