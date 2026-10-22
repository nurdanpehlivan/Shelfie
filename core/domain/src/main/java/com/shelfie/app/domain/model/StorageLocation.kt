package com.shelfie.app.domain.model

/** Where an item is kept. Freezing an item also pushes its expiry date out, see [Category]. */
enum class StorageLocation {
    FRIDGE,
    FREEZER,
    PANTRY;

    companion object {
        /** Lenient lookup for persisted or remote values; `null` for unknown names. */
        fun fromNameOrNull(name: String?): StorageLocation? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }

        fun fromName(name: String?): StorageLocation = fromNameOrNull(name) ?: FRIDGE
    }
}
