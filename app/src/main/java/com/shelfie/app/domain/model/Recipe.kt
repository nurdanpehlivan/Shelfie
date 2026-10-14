package com.shelfie.app.domain.model

/** A quick recipe generated to use up pantry items that are about to spoil. */
data class Recipe(
    val title: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    /** Everything the recipe needs; see [RecipeIngredient.isFromPantry]. */
    val ingredients: List<RecipeIngredient>,
    /** Never empty. */
    val steps: List<RecipeStep>,
) {
    /** The expiring pantry items this recipe rescues. */
    val rescuedIngredients: List<RecipeIngredient> get() = ingredients.filter { it.isFromPantry }

    /** Basics that are not among the expiring items (salt, oil, an egg…) and may need buying. */
    val extraIngredients: List<RecipeIngredient> get() = ingredients.filterNot { it.isFromPantry }
}

data class RecipeIngredient(
    val name: String,
    /** Free-form amount as the model wrote it, e.g. "200 g" or "1 tbsp"; may be empty. */
    val amount: String,
    /** A single food emoji, or `null` if the model didn't give a usable one. */
    val emoji: String?,
    /** `true` for one of the expiring items sent in the prompt. */
    val isFromPantry: Boolean,
)

data class RecipeStep(
    val instruction: String,
    /** Hands-off time such as simmering or baking; 0 when the step has none. */
    val durationMinutes: Int,
)
