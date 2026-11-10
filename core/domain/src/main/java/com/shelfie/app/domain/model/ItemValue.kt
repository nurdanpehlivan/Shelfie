package com.shelfie.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What a pantry item is roughly worth, for the savings statistic. The app has no prices, so it
 * starts from the user's "average product" value (Settings) and scales it by what was bought:
 * a kilo of meat is worth more than a bunch of parsley, two packs more than one.
 */
object ItemValue {

    /** How a typical purchase in each category compares with an average product. */
    private val CATEGORY_WEIGHT = mapOf(
        Category.MEAT to 2.5,
        Category.SEAFOOD to 3.0,
        Category.DAIRY to 1.0,
        Category.FROZEN to 1.3,
        Category.FRUITS to 0.7,
        Category.VEGETABLES to 0.6,
        Category.BAKERY to 0.5,
        Category.GRAINS to 0.8,
        Category.CANNED to 0.7,
        Category.BEVERAGES to 0.7,
        Category.CONDIMENTS to 0.8,
        Category.SNACKS to 0.6,
        Category.OTHER to 1.0,
    )

    /** No single entry counts as more than this many typical purchases, so a typo can't skew the total. */
    private const val MAX_PURCHASES = 5.0

    fun of(item: PantryItem, averageItemValue: Double): Double =
        averageItemValue * weight(item.category) * purchases(item)

    /** The category's multiplier, shown next to each item in the breakdown. */
    fun weight(category: Category): Double = CATEGORY_WEIGHT[category] ?: 1.0

    /**
     * The amount as a number of typical purchases. Pieces count less after the first (six eggs
     * are one purchase, not six); kilos and litres count one each; grams and millilitres per 1000.
     */
    fun purchases(item: PantryItem): Double {
        val q = item.quantity.coerceAtLeast(0.0)
        val amount = when (item.unit) {
            MeasurementUnit.PIECE -> if (q <= 1.0) q else 1.0 + (q - 1.0) * 0.25
            MeasurementUnit.PACKAGE -> q
            MeasurementUnit.KILOGRAM, MeasurementUnit.LITER -> q
            MeasurementUnit.GRAM, MeasurementUnit.MILLILITER -> (q / 1000.0).coerceAtLeast(0.1)
        }
        return amount.coerceIn(0.0, MAX_PURCHASES)
    }

    /**
     * Whether eating [item] on [eatenOn] saved it: it was within [thresholds]' warning window
     * (or past its date) by then. Food eaten long before its date was never at risk, so it
     * isn't counted as saved.
     */
    fun wasAtRisk(item: PantryItem, eatenOn: LocalDate, thresholds: FreshnessThresholds): Boolean =
        ChronoUnit.DAYS.between(eatenOn, item.expiryDate) <= thresholds.warningDays
}
