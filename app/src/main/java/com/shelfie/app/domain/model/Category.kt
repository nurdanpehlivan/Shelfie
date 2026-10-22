package com.shelfie.app.domain.model

/**
 * Food categories recognised from a shelfie photo.
 *
 * [defaultShelfLifeDays] is a conservative estimate used to pre-fill the expiry date when
 * the label cannot be read from the photo, keeping the flow free of manual text input.
 * [defaultLocation] is where such an item usually lives, and [freezerShelfLifeDays] how long
 * it keeps once frozen (conservative home-freezer figures at -18 °C).
 */
enum class Category(
    val defaultShelfLifeDays: Int,
    val defaultLocation: StorageLocation,
    val freezerShelfLifeDays: Int,
) {
    DAIRY(defaultShelfLifeDays = 7, StorageLocation.FRIDGE, freezerShelfLifeDays = 60),
    MEAT(defaultShelfLifeDays = 3, StorageLocation.FRIDGE, freezerShelfLifeDays = 120),
    SEAFOOD(defaultShelfLifeDays = 2, StorageLocation.FRIDGE, freezerShelfLifeDays = 90),
    FRUITS(defaultShelfLifeDays = 7, StorageLocation.FRIDGE, freezerShelfLifeDays = 240),
    VEGETABLES(defaultShelfLifeDays = 7, StorageLocation.FRIDGE, freezerShelfLifeDays = 240),
    BAKERY(defaultShelfLifeDays = 4, StorageLocation.PANTRY, freezerShelfLifeDays = 90),
    GRAINS(defaultShelfLifeDays = 180, StorageLocation.PANTRY, freezerShelfLifeDays = 180),
    CANNED(defaultShelfLifeDays = 365, StorageLocation.PANTRY, freezerShelfLifeDays = 365),
    FROZEN(defaultShelfLifeDays = 90, StorageLocation.FREEZER, freezerShelfLifeDays = 90),
    BEVERAGES(defaultShelfLifeDays = 30, StorageLocation.PANTRY, freezerShelfLifeDays = 90),
    CONDIMENTS(defaultShelfLifeDays = 90, StorageLocation.PANTRY, freezerShelfLifeDays = 90),
    SNACKS(defaultShelfLifeDays = 60, StorageLocation.PANTRY, freezerShelfLifeDays = 90),
    OTHER(defaultShelfLifeDays = 14, StorageLocation.FRIDGE, freezerShelfLifeDays = 60);

    /**
     * Whether freezing is worth offering. Shelf-stable goods gain nothing from it, and
     * drinks, sauces and snacks change texture or burst their packaging.
     */
    val isFreezable: Boolean
        get() = this !in NOT_FREEZABLE

    /** Days a thawed item keeps in the fridge; thawed meat and fish go first. */
    val thawedShelfLifeDays: Int
        get() = when (this) {
            MEAT, SEAFOOD -> 1
            DAIRY, OTHER, FROZEN -> 2
            else -> 3
        }

    companion object {
        private val NOT_FREEZABLE = setOf(GRAINS, CANNED, BEVERAGES, CONDIMENTS, SNACKS)

        /** Lenient lookup for persisted or remote values; unknown names fall back to [OTHER]. */
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
    }
}
