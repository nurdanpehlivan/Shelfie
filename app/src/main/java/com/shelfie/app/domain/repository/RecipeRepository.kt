package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.SavedRecipe
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Generates rescue recipes and keeps every generated one in the recipe history. */
interface RecipeRepository {

    /**
     * Asks the AI chef for a practical [mealType] recipe of about 15 minutes that uses up
     * [ingredients], and saves it to the history.
     *
     * A failure always carries a [com.shelfie.app.domain.model.PantryScanException], the same
     * typed AI-service errors as a photo scan.
     */
    suspend fun generateRescueRecipe(ingredients: List<PantryItem>, mealType: MealType): Result<SavedRecipe>

    /**
     * The recipe generated today for exactly these [ingredients], e.g. by the morning freshness
     * check, so opening its notification doesn't wait for (or pay for) a second generation.
     */
    suspend fun getTodaysRecipeFor(ingredients: List<PantryItem>): SavedRecipe?

    /** Newest first. */
    fun observeSavedRecipes(): Flow<List<SavedRecipe>>

    suspend fun getSavedRecipe(id: Long): SavedRecipe?

    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    suspend fun markCooked(id: Long)

    suspend fun deleteSavedRecipe(id: Long)

    fun observeCookedCountSince(since: Instant): Flow<Int>
}
