package com.shelfie.app.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.shelfie.app.R
import com.shelfie.app.presentation.dashboard.DashboardRoute
import com.shelfie.app.presentation.history.RecipeHistoryRoute
import com.shelfie.app.presentation.mealplan.MealPlanRoute
import com.shelfie.app.presentation.premium.PremiumRoute
import com.shelfie.app.presentation.recipe.RecipeRoute
import com.shelfie.app.presentation.recipe.picker.IngredientPickerRoute
import com.shelfie.app.presentation.scanner.ScannerRoute
import com.shelfie.app.presentation.settings.SettingsRoute
import com.shelfie.app.presentation.shopping.ShoppingListRoute
import com.shelfie.app.presentation.statistics.StatisticsRoute
import com.shelfie.app.presentation.theme.extendedColors
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object DashboardDestination

@Serializable
data object ScannerDestination

/**
 * Without [savedRecipeId] the screen picks today's critical items and gets a recipe for them;
 * with one it shows that recipe from the history. [ingredientIds] (comma-separated pantry ids)
 * asks for a recipe with exactly those items instead.
 */
@Serializable
data class RecipeDestination(
    val savedRecipeId: Long = NO_SAVED_RECIPE,
    val ingredientIds: String = "",
) {
    companion object {
        const val NO_SAVED_RECIPE = -1L
    }
}

@Serializable
data object RecipeHistoryDestination

@Serializable
data object IngredientPickerDestination

@Serializable
data object MealPlanDestination

@Serializable
data object PremiumDestination

@Serializable
data object ShoppingListDestination

@Serializable
data object StatisticsDestination

@Serializable
data object SettingsDestination

/** Opens the rescue recipe, e.g. from the daily freshness notification. */
const val RECIPE_DEEP_LINK = "shelfie://recipe"

/** Deep link to one recipe of the history. */
fun savedRecipeDeepLink(savedRecipeId: Long): String = "$RECIPE_DEEP_LINK?savedRecipeId=$savedRecipeId"

/** Result key the scanner uses to tell the dashboard how many items it saved. */
private const val KEY_SAVED_ITEMS_COUNT = "saved_items_count"

/** [iconRes] is the outlined icon of an idle tab, [selectedIconRes] the filled one. */
private enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
    @param:StringRes val labelRes: Int,
) {
    PANTRY(DashboardDestination, DashboardDestination::class, R.drawable.ic_home_outlined, R.drawable.ic_home, R.string.tab_pantry),
    RECIPES(RecipeHistoryDestination, RecipeHistoryDestination::class, R.drawable.ic_restaurant, R.drawable.ic_restaurant, R.string.tab_recipes),
    SHOPPING(ShoppingListDestination, ShoppingListDestination::class, R.drawable.ic_shopping_cart_outlined, R.drawable.ic_shopping_cart, R.string.tab_shopping),
    STATISTICS(StatisticsDestination, StatisticsDestination::class, R.drawable.ic_bar_chart_outlined, R.drawable.ic_bar_chart, R.string.tab_statistics),
}

@Composable
fun ShelfieNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelTab.entries.any { currentDestination.isOn(it) }

    fun navigateToRecipe(savedRecipeId: Long = RecipeDestination.NO_SAVED_RECIPE) {
        navController.navigate(RecipeDestination(savedRecipeId)) { launchSingleTop = true }
    }

    Scaffold(
        // Each screen has its own Scaffold that handles the status bar; this one only adds
        // the bottom bar, and the padding below tells the inner ones that it took the bottom.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                ShelfieNavigationBar(
                    isSelected = { tab -> currentDestination.isOn(tab) },
                    onTabClick = { tab ->
                        navController.navigate(tab.route) {
                            // Standard tab behaviour: one copy of each tab, state kept.
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<DashboardDestination> { entry ->
                val savedItemsCount by entry.savedStateHandle
                    .getStateFlow<Int?>(KEY_SAVED_ITEMS_COUNT, null)
                    .collectAsStateWithLifecycle()

                DashboardRoute(
                    onNavigateToScanner = {
                        navController.navigate(ScannerDestination) { launchSingleTop = true }
                    },
                    onNavigateToRecipe = { navigateToRecipe() },
                    onNavigateToSettings = {
                        navController.navigate(SettingsDestination) { launchSingleTop = true }
                    },
                    savedItemsCount = savedItemsCount,
                    onSavedItemsMessageShown = {
                        entry.savedStateHandle[KEY_SAVED_ITEMS_COUNT] = null
                    },
                )
            }
            composable<RecipeDestination>(
                deepLinks = listOf(navDeepLink<RecipeDestination>(basePath = RECIPE_DEEP_LINK)),
            ) {
                RecipeRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenPremium = { navController.navigate(PremiumDestination) { launchSingleTop = true } },
                )
            }
            composable<RecipeHistoryDestination> {
                RecipeHistoryRoute(
                    onOpenRecipe = { id -> navigateToRecipe(id) },
                    onNewRecipe = { navigateToRecipe() },
                    onCookWithWhatIHave = {
                        navController.navigate(IngredientPickerDestination) { launchSingleTop = true }
                    },
                    onOpenMealPlan = {
                        navController.navigate(MealPlanDestination) { launchSingleTop = true }
                    },
                )
            }
            composable<MealPlanDestination> {
                MealPlanRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenRecipe = { id -> navigateToRecipe(id) },
                )
            }
            composable<IngredientPickerDestination> {
                IngredientPickerRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onCreateRecipe = { ids ->
                        // The picker has done its job; back from the recipe returns to the history.
                        navController.navigate(RecipeDestination(ingredientIds = ids.joinToString(","))) {
                            popUpTo(IngredientPickerDestination) { inclusive = true }
                        }
                    },
                )
            }
            composable<ShoppingListDestination> {
                ShoppingListRoute()
            }
            composable<StatisticsDestination> {
                StatisticsRoute()
            }
            composable<SettingsDestination> {
                SettingsRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenPremium = { navController.navigate(PremiumDestination) { launchSingleTop = true } },
                )
            }
            composable<PremiumDestination> {
                PremiumRoute(
                    onNavigateUp = { navController.navigateUp() },
                    // Premium needs an account; settings is where one is made.
                    onOpenSettings = {
                        navController.navigate(SettingsDestination) {
                            popUpTo(PremiumDestination) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<ScannerDestination> {
                ScannerRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenPremium = { navController.navigate(PremiumDestination) { launchSingleTop = true } },
                    onItemsSaved = { count ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(KEY_SAVED_ITEMS_COUNT, count)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

/**
 * Floating glass dock: inset from the screen edges, rounded, with a soft shadow. The active
 * tab gets a filled icon in emerald on a faint emerald pill; idle tabs are outlined and muted.
 */
@Composable
private fun ShelfieNavigationBar(isSelected: (TopLevelTab) -> Boolean, onTabClick: (TopLevelTab) -> Unit) {
    val extended = MaterialTheme.extendedColors
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)
            .shadow(elevation = 20.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(extended.navBar)
            .border(1.dp, extended.glassBorder, shape)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .selectableGroup(),
    ) {
        TopLevelTab.entries.forEach { tab ->
            NavItem(tab = tab, selected = isSelected(tab), onClick = { onTabClick(tab) })
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: TopLevelTab, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val iconColor by animateColorAsState(if (selected) scheme.primary else scheme.onSurfaceVariant, label = "navIcon")
    val pillColor by animateColorAsState(
        if (selected) scheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        label = "navPill",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .background(pillColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (selected) tab.selectedIconRes else tab.iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun NavDestination?.isOn(tab: TopLevelTab): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
