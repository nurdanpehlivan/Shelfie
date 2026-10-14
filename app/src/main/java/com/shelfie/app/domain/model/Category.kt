package com.shelfie.app.domain.model

/**
 * Food categories recognised from a shelfie photo.
 *
 * [defaultShelfLifeDays] is a conservative estimate used to pre-fill the expiry date when
 * the label cannot be read from the photo, keeping the flow free of manual text input.
 */
enum class Category(val defaultShelfLifeDays: Int) {
    DAIRY(defaultShelfLifeDays = 7),
    MEAT(defaultShelfLifeDays = 3),
    SEAFOOD(defaultShelfLifeDays = 2),
    FRUITS(defaultShelfLifeDays = 7),
    VEGETABLES(defaultShelfLifeDays = 7),
    BAKERY(defaultShelfLifeDays = 4),
    GRAINS(defaultShelfLifeDays = 180),
    CANNED(defaultShelfLifeDays = 365),
    FROZEN(defaultShelfLifeDays = 90),
    BEVERAGES(defaultShelfLifeDays = 30),
    CONDIMENTS(defaultShelfLifeDays = 90),
    SNACKS(defaultShelfLifeDays = 60),
    OTHER(defaultShelfLifeDays = 14);

    companion object {
        /** Lenient lookup for persisted or remote values; unknown names fall back to [OTHER]. */
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
    }
}
