package com.shelfie.app.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.shelfie.app.presentation.recipe.RecipeRoute
import com.shelfie.app.presentation.scanner.ScannerRoute
import com.shelfie.app.presentation.settings.SettingsRoute
import com.shelfie.app.presentation.shopping.ShoppingListRoute
import com.shelfie.app.presentation.statistics.StatisticsRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object DashboardDestination

@Serializable
data object ScannerDestination

/**
 * Without [savedRecipeId] the screen picks today's critical items and gets a recipe for them;
 * with one it shows that recipe from the history.
 */
@Serializable
data class RecipeDestination(val savedRecipeId: Long = NO_SAVED_RECIPE) {
    companion object {
        const val NO_SAVED_RECIPE = -1L
    }
}

@Serializable
data object RecipeHistoryDestination

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

private enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
) {
    PANTRY(DashboardDestination, DashboardDestination::class, R.drawable.ic_home, R.string.tab_pantry),
    RECIPES(RecipeHistoryDestination, RecipeHistoryDestination::class, R.drawable.ic_restaurant, R.string.tab_recipes),
    SHOPPING(ShoppingListDestination, ShoppingListDestination::class, R.drawable.ic_shopping_cart, R.string.tab_shopping),
    STATISTICS(StatisticsDestination, StatisticsDestination::class, R.drawable.ic_bar_chart, R.string.tab_statistics),
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
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination.isOn(tab),
                            onClick = {
                                navController.navigate(tab.route) {
                                    // Standard tab behaviour: one copy of each tab, state kept.
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
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
                RecipeRoute(onNavigateUp = { navController.navigateUp() })
            }
            composable<RecipeHistoryDestination> {
                RecipeHistoryRoute(
                    onOpenRecipe = { id -> navigateToRecipe(id) },
                    onNewRecipe = { navigateToRecipe() },
                )
            }
            composable<ShoppingListDestination> {
                ShoppingListRoute()
            }
            composable<StatisticsDestination> {
                StatisticsRoute()
            }
            composable<SettingsDestination> {
                SettingsRoute(onNavigateUp = { navController.navigateUp() })
            }
            composable<ScannerDestination> {
                ScannerRoute(
                    onNavigateUp = { navController.navigateUp() },
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

private fun NavDestination?.isOn(tab: TopLevelTab): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
