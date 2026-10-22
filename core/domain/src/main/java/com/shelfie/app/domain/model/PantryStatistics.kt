package com.shelfie.app.domain.model

import java.time.LocalDate

/** Eaten vs thrown-away numbers for one period, see `ObserveStatisticsUseCase`. */
data class PantryStatistics(
    val rescuedCount: Int,
    val wastedCount: Int,
    val cookedRecipeCount: Int,
    /** [rescuedCount] × the user's average item value. */
    val estimatedSavings: Double,
    /** Oldest week first; always [WEEKS_IN_CHART] entries. */
    val weekly: List<WeekStatistics>,
    /** Categories thrown away most often in the period, most first; at most 3. */
    val topWastedCategories: List<Pair<Category, Int>>,
) {
    val totalCount: Int get() = rescuedCount + wastedCount

    /** Share of removed items that were eaten, 0–1; `null` while nothing has been removed. */
    val rescueRate: Float? get() = if (totalCount == 0) null else rescuedCount.toFloat() / totalCount

    companion object {
        const val WEEKS_IN_CHART = 8
    }
}

data class WeekStatistics(
    /** Monday of the week. */
    val weekStart: LocalDate,
    val rescued: Int,
    val wasted: Int,
)

enum class StatisticsPeriod { WEEK, MONTH, ALL_TIME }
