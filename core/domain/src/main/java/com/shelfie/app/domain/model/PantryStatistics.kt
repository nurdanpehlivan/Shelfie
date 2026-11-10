package com.shelfie.app.domain.model

import java.time.LocalDate

/** Eaten vs thrown-away numbers for one period, see `ObserveStatisticsUseCase`. */
data class PantryStatistics(
    val rescuedCount: Int,
    val wastedCount: Int,
    val cookedRecipeCount: Int,
    /**
     * Rough worth of the food eaten just in time ([savedCount] items: within the warning window
     * or past the date), see `ItemValue`.
     */
    val estimatedSavings: Double,
    /** Oldest week first; always [WEEKS_IN_CHART] entries. */
    val weekly: List<WeekStatistics>,
    /** Categories thrown away most often in the period, most first; at most 3. */
    val topWastedCategories: List<Pair<Category, Int>>,
    /** Currency of [estimatedSavings], see [AppSettings.currencyCode]. */
    val currencyCode: String? = null,
    /** Eaten items that were close to spoiling: what [estimatedSavings] adds up. */
    val savedCount: Int = 0,
    /** Rough worth of what was thrown away, same scale as [estimatedSavings]. */
    val wastedValue: Double = 0.0,
    /** What [estimatedSavings] is made of, most valuable first. */
    val savedItems: List<ValuedItem> = emptyList(),
    /** What [wastedValue] is made of, most valuable first. */
    val wastedItems: List<ValuedItem> = emptyList(),
    /** The user's "average product" value the amounts were worked out from. */
    val averageItemValue: Double = AppSettings.DEFAULT_AVERAGE_ITEM_VALUE,
) {
    val totalCount: Int get() = rescuedCount + wastedCount

    /** Share of removed items that were eaten, 0–1; `null` while nothing has been removed. */
    val rescueRate: Float? get() = if (totalCount == 0) null else rescuedCount.toFloat() / totalCount

    companion object {
        const val WEEKS_IN_CHART = 8
    }
}

/** A removed item and what it was estimated to be worth. */
data class ValuedItem(val item: PantryItem, val value: Double)

data class WeekStatistics(
    /** Monday of the week. */
    val weekStart: LocalDate,
    val rescued: Int,
    val wasted: Int,
)

enum class StatisticsPeriod { WEEK, MONTH, ALL_TIME }
