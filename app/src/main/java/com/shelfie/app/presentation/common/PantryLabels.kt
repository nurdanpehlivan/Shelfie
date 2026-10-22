package com.shelfie.app.presentation.common

import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.shelfie.app.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.StorageLocation
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.DAIRY -> R.string.category_dairy
        Category.MEAT -> R.string.category_meat
        Category.SEAFOOD -> R.string.category_seafood
        Category.FRUITS -> R.string.category_fruits
        Category.VEGETABLES -> R.string.category_vegetables
        Category.BAKERY -> R.string.category_bakery
        Category.GRAINS -> R.string.category_grains
        Category.CANNED -> R.string.category_canned
        Category.FROZEN -> R.string.category_frozen
        Category.BEVERAGES -> R.string.category_beverages
        Category.CONDIMENTS -> R.string.category_condiments
        Category.SNACKS -> R.string.category_snacks
        Category.OTHER -> R.string.category_other
    }

@get:StringRes
val StorageLocation.labelRes: Int
    get() = when (this) {
        StorageLocation.FRIDGE -> R.string.location_fridge
        StorageLocation.FREEZER -> R.string.location_freezer
        StorageLocation.PANTRY -> R.string.location_pantry
    }

val StorageLocation.emoji: String
    get() = when (this) {
        StorageLocation.FRIDGE -> "🧊"
        StorageLocation.FREEZER -> "❄️"
        StorageLocation.PANTRY -> "🗄️"
    }

val Category.emoji: String
    get() = when (this) {
        Category.DAIRY -> "🥛"
        Category.MEAT -> "🥩"
        Category.SEAFOOD -> "🐟"
        Category.FRUITS -> "🍎"
        Category.VEGETABLES -> "🥕"
        Category.BAKERY -> "🍞"
        Category.GRAINS -> "🌾"
        Category.CANNED -> "🥫"
        Category.FROZEN -> "🧊"
        Category.BEVERAGES -> "🧃"
        Category.CONDIMENTS -> "🧂"
        Category.SNACKS -> "🍪"
        Category.OTHER -> "📦"
    }

@get:StringRes
val MeasurementUnit.labelRes: Int
    get() = when (this) {
        MeasurementUnit.PIECE -> R.string.unit_piece
        MeasurementUnit.PACKAGE -> R.string.unit_package
        MeasurementUnit.GRAM -> R.string.unit_gram
        MeasurementUnit.KILOGRAM -> R.string.unit_kilogram
        MeasurementUnit.MILLILITER -> R.string.unit_milliliter
        MeasurementUnit.LITER -> R.string.unit_liter
    }

/** "2" rather than "2.0", "1.5" stays as is (with the locale's decimal separator). */
fun formatQuantity(quantity: Double): String = DecimalFormat("0.##").format(quantity)

/** Accepts both "1.5" and "1,5"; returns `null` for anything that isn't a number. */
fun parseQuantity(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/**
 * Day + abbreviated month in the user's own order and style: "Oct 5", "5 Eki", "5. Okt.".
 * A fixed "d MMM" pattern reads wrong in half the languages.
 */
fun shortDateFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
