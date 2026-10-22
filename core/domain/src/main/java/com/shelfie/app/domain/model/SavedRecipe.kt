package com.shelfie.app.domain.model

import java.time.Instant

/** A generated rescue recipe as kept in the recipe history. */
data class SavedRecipe(
    val id: Long,
    val recipe: Recipe,
    /** The expiring pantry items the recipe was generated for; "I cooked it" uses these up. */
    val pantryItemIds: List<Long>,
    val isFavorite: Boolean,
    val createdAt: Instant,
    /** When the user said they cooked it, or `null`. */
    val cookedAt: Instant?,
) {
    val isCooked: Boolean get() = cookedAt != null
}
