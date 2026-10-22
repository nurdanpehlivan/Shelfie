package com.shelfie.app.domain.model

import java.time.LocalDate

/** One planned meal. [recipe] is `null` for a note-only slot, or if the recipe was deleted. */
data class MealPlanEntry(
    val date: LocalDate,
    val mealType: MealType,
    val recipe: SavedRecipe?,
    val note: String,
)
