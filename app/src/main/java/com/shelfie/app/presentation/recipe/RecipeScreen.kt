package com.shelfie.app.presentation.recipe

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shelfie.app.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.Recipe
import com.shelfie.app.domain.model.RecipeIngredient
import com.shelfie.app.domain.model.RecipeStep
import com.shelfie.app.presentation.common.BottomActionBar
import com.shelfie.app.presentation.common.Pill
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.foodEmoji
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.MascotMessage
import com.shelfie.app.presentation.recipe.components.CookingMode
import com.shelfie.app.presentation.recipe.components.IngredientGrid
import com.shelfie.app.presentation.recipe.components.IngredientTile
import com.shelfie.app.presentation.recipe.components.RecipeHeroCard
import com.shelfie.app.presentation.recipe.components.SectionTitle
import com.shelfie.app.presentation.recipe.components.StepOverviewCard
import com.shelfie.app.presentation.recipe.components.ingredientEmoji
import com.shelfie.app.presentation.scanner.components.ShimmerBlock
import com.shelfie.app.presentation.theme.ShelfieTheme
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun RecipeRoute(
    onNavigateUp: () -> Unit,
    onOpenPremium: () -> Unit = {},
    viewModel: RecipeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                val message = when (effect) {
                    is RecipeEffect.Cooked -> context.resources.getQuantityString(
                        R.plurals.recipe_cooked_message,
                        effect.consumedCount,
                        effect.consumedCount,
                    )
                    is RecipeEffect.AddedToShoppingList -> if (effect.addedCount == 0) {
                        context.getString(R.string.recipe_shopping_already_listed)
                    } else {
                        context.resources.getQuantityString(
                            R.plurals.recipe_shopping_added,
                            effect.addedCount,
                            effect.addedCount,
                        )
                    }
                    is RecipeEffect.ShowMessage -> context.getString(effect.messageRes)
                }
                launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }

    RecipeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbarHostState,
        onOpenPremium = onOpenPremium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    state: RecipeUiState,
    onIntent: (RecipeIntent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenPremium: () -> Unit = {},
) {
    val ready = state as? RecipeUiState.Ready
    val isCooking = ready?.cooking != null
    // In cooking mode, back returns to the overview instead of leaving the recipe.
    BackHandler(enabled = isCooking) { onIntent(RecipeIntent.ExitCooking) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShelfieTopBar(
                title = stringResource(if (isCooking) R.string.recipe_cooking_title else R.string.recipe_title),
                onNavigateUp = if (isCooking) {
                    { onIntent(RecipeIntent.ExitCooking) }
                } else {
                    onNavigateUp
                },
                navigationContentDescription = stringResource(
                    if (isCooking) R.string.recipe_action_exit_cooking else R.string.action_navigate_up,
                ),
                actions = {
                    if (ready != null && !isCooking) {
                        IconButton(onClick = { onIntent(RecipeIntent.ToggleFavorite) }) {
                            Icon(
                                painter = painterResource(
                                    if (ready.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                                ),
                                contentDescription = stringResource(
                                    if (ready.isFavorite) R.string.recipe_action_unfavorite else R.string.recipe_action_favorite,
                                ),
                                tint = if (ready.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
                            )
                        }
                        if (!ready.isFromHistory) {
                            IconButton(onClick = { onIntent(RecipeIntent.Regenerate) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_refresh),
                                    contentDescription = stringResource(R.string.recipe_action_regenerate),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        // Keyed by state type and mode, so steps changing within cooking mode don't cross-fade
        // the whole screen (CookingMode animates those itself).
        AnimatedContent(
            targetState = state,
            contentKey = { it::class to ((it as? RecipeUiState.Ready)?.cooking != null) },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "recipeState",
        ) { current ->
            when (current) {
                is RecipeUiState.Loading -> LoadingContent(current.ingredients, contentModifier)
                RecipeUiState.NoIngredients -> MascotMessage(
                    mood = FrizzyMood.CELEBRATING,
                    title = stringResource(R.string.recipe_empty_title),
                    message = stringResource(R.string.recipe_empty_subtitle),
                    primaryAction = stringResource(R.string.recipe_action_done) to onNavigateUp,
                    modifier = contentModifier,
                )
                RecipeUiState.NotFound -> MascotMessage(
                    mood = FrizzyMood.SLEEPY,
                    title = stringResource(R.string.recipe_not_found_title),
                    message = stringResource(R.string.recipe_not_found_subtitle),
                    primaryAction = stringResource(R.string.action_navigate_up) to onNavigateUp,
                    modifier = contentModifier,
                )
                is RecipeUiState.Error -> MascotMessage(
                    mood = FrizzyMood.WORRIED,
                    title = stringResource(R.string.scanner_error_title),
                    message = stringResource(current.messageRes),
                    primaryAction = when {
                        current.canUpgrade -> stringResource(R.string.premium_upgrade) to onOpenPremium
                        current.canRetry -> stringResource(R.string.scanner_action_retry) to { onIntent(RecipeIntent.Retry) }
                        else -> stringResource(R.string.recipe_action_done) to onNavigateUp
                    },
                    secondaryAction = if (current.canUpgrade) stringResource(R.string.recipe_action_done) to onNavigateUp else null,
                    modifier = contentModifier,
                )
                is RecipeUiState.Ready -> {
                    val cooking = current.cooking
                    if (cooking == null) {
                        RecipeOverview(
                            state = current,
                            onStartCooking = { onIntent(RecipeIntent.StartCooking) },
                            onMarkCooked = { onIntent(RecipeIntent.MarkCooked) },
                            onAddMissing = { onIntent(RecipeIntent.AddMissingToShoppingList) },
                            modifier = contentModifier,
                        )
                    } else {
                        CookingMode(
                            recipe = current.recipe,
                            progress = cooking,
                            isCooked = current.isCooked,
                            isMarkingCooked = current.isMarkingCooked,
                            onMarkCooked = { onIntent(RecipeIntent.MarkCooked) },
                            onStepDone = { onIntent(RecipeIntent.CompleteStep) },
                            onPreviousStep = { onIntent(RecipeIntent.PreviousStep) },
                            onBackToRecipe = { onIntent(RecipeIntent.ExitCooking) },
                            onFinish = onNavigateUp,
                            modifier = contentModifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeOverview(
    state: RecipeUiState.Ready,
    onStartCooking: () -> Unit,
    onMarkCooked: () -> Unit,
    onAddMissing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = state.recipe
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                RecipeHeroCard(
                    recipe = recipe,
                    rescuedEmojis = recipe.rescuedIngredients.map { ingredientEmoji(it, state.ingredients) },
                )
            }
            if (state.isCooked) {
                item(key = "cooked") {
                    Row {
                        Pill(
                            text = "✓ " + stringResource(R.string.recipe_badge_cooked),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            if (recipe.rescuedIngredients.isNotEmpty()) {
                item(key = "rescuedTitle") { SectionTitle(stringResource(R.string.recipe_section_rescued)) }
                item(key = "rescued") {
                    IngredientGrid(ingredients = recipe.rescuedIngredients, pantryItems = state.ingredients)
                }
            }
            if (recipe.extraIngredients.isNotEmpty()) {
                item(key = "extrasTitle") { SectionTitle(stringResource(R.string.recipe_section_extras)) }
                item(key = "extras") {
                    IngredientGrid(ingredients = recipe.extraIngredients, pantryItems = state.ingredients)
                }
                item(key = "addMissing") {
                    FilledTonalButton(
                        onClick = onAddMissing,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_shopping),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.recipe_action_add_missing))
                    }
                }
            }
            item(key = "stepsTitle") { SectionTitle(stringResource(R.string.recipe_section_steps)) }
            itemsIndexed(recipe.steps, key = { index, _ -> "step_$index" }) { index, step ->
                StepOverviewCard(number = index + 1, step = step)
            }
        }
        BottomActionBar {
                OutlinedButton(
                    onClick = onMarkCooked,
                    enabled = !state.isCooked && !state.isMarkingCooked,
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(
                        stringResource(
                            if (state.isCooked) R.string.recipe_badge_cooked else R.string.recipe_action_cooked,
                        ),
                    )
                }
                Button(
                    onClick = onStartCooking,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Text("👩‍🍳  " + stringResource(R.string.recipe_action_start))
                }
        }
    }
}

@Composable
private fun LoadingContent(ingredients: List<PantryItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Frizzy(mood = FrizzyMood.CHEF, size = 150.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.recipe_loading),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.recipe_loading_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        // The ingredients being rescued, so the wait already shows something concrete.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ingredients.forEach { item ->
                IngredientTile(
                    emoji = item.displayEmoji,
                    name = item.name,
                    amount = "",
                    isRescued = true,
                    modifier = Modifier.width(76.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        ShimmerBlock(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(28.dp),
        )
        repeat(3) {
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth().height(64.dp), shape = MaterialTheme.shapes.large)
        }
    }
}

private val previewToday = LocalDate.of(2026, 10, 8)

private val previewIngredients = listOf(
    PantryItem(
        id = 1, name = "Süt", category = Category.DAIRY, purchaseDate = previewToday,
        expiryDate = previewToday.plusDays(1), createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    ),
    PantryItem(
        id = 2, name = "Domates", category = Category.VEGETABLES, purchaseDate = previewToday,
        expiryDate = previewToday, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    ),
)

private val previewRecipe = Recipe(
    title = "Sütlü Domatesli Menemen",
    cookingTimeMinutes = 15,
    servings = 2,
    ingredients = listOf(
        RecipeIngredient("Domates", "3 adet", "🍅", isFromPantry = true),
        RecipeIngredient("Süt", "50 ml", "🥛", isFromPantry = true),
        RecipeIngredient("Yumurta", "3 adet", "🥚", isFromPantry = false),
        RecipeIngredient("Tereyağı", "1 yk", "🧈", isFromPantry = false),
        RecipeIngredient("Tuz", "1 tutam", "🧂", isFromPantry = false),
    ),
    steps = listOf(
        RecipeStep("Domatesleri küp küp doğra.", 0),
        RecipeStep("Tereyağını tavada erit, domatesleri ekleyip suyunu çekene kadar pişir.", 5),
        RecipeStep("Yumurtaları sütle çırp, tavaya ekle ve kısık ateşte karıştırarak pişir.", 3),
    ),
)

@Preview(showBackground = true)
@Composable
private fun RecipeOverviewPreview() {
    ShelfieTheme {
        RecipeScreen(
            state = RecipeUiState.Ready(savedRecipeId = 1, ingredients = previewIngredients, recipe = previewRecipe),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeCookingPreview() {
    ShelfieTheme {
        RecipeScreen(
            state = RecipeUiState.Ready(
                savedRecipeId = 1,
                ingredients = previewIngredients,
                recipe = previewRecipe,
                cooking = CookingProgress(completedSteps = 1, totalSteps = previewRecipe.steps.size),
            ),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeLoadingPreview() {
    ShelfieTheme {
        RecipeScreen(state = RecipeUiState.Loading(previewIngredients), onIntent = {}, onNavigateUp = {})
    }
}
