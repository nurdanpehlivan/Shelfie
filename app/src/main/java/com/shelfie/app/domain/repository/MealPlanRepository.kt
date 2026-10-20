package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.MealPlanEntry
import com.shelfie.app.domain.model.MealType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The weekly meal plan; synced with the account (and household) like the pantry. */
interface MealPlanRepository {

    /** Planned meals from [start] for seven days. */
    fun observeWeek(start: LocalDate): Flow<List<MealPlanEntry>>

    /** Plans the saved recipe [recipeId] for that meal, keeping any note. */
    suspend fun setRecipe(date: LocalDate, mealType: MealType, recipeId: Long)

    /** Sets the note of that meal, keeping any recipe; a blank note on an empty slot clears it. */
    suspend fun setNote(date: LocalDate, mealType: MealType, note: String)

    suspend fun clear(date: LocalDate, mealType: MealType)
}
