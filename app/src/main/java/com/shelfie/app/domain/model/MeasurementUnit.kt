package com.shelfie.app.domain.model

enum class MeasurementUnit {
    PIECE,
    PACKAGE,
    GRAM,
    KILOGRAM,
    MILLILITER,
    LITER;

    companion object {
        /** Lenient lookup for persisted or remote values; unknown names fall back to [PIECE]. */
        fun fromName(name: String?): MeasurementUnit =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PIECE
    }
}
