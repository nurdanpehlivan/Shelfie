package com.shelfie.app.domain.model

/** What a photo scan says about the state of a product, beyond what it is. */
enum class ItemCondition {
    OK,

    /** Visible signs it went bad: mould, rot, slime, a bulging can or a torn seal. */
    SPOILED,

    /** A printed date on it has already passed. */
    EXPIRED,
}

/**
 * One product found in a photo: the unsaved pantry item, and its [condition] with a short
 * reason in the user's language ([conditionNote]) when it isn't [ItemCondition.OK].
 */
data class ScannedItem(
    val item: PantryItem,
    val condition: ItemCondition = ItemCondition.OK,
    val conditionNote: String? = null,
) {
    val isFlagged: Boolean get() = condition != ItemCondition.OK
}
