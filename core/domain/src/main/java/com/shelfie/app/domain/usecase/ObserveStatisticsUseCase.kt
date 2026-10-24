package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.PantryStatistics
import com.shelfie.app.domain.model.StatisticsPeriod
import com.shelfie.app.domain.model.WeekStatistics
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Eaten ("rescued") vs thrown-away items for [StatisticsPeriod], plus a weekly chart of the
 * last [PantryStatistics.WEEKS_IN_CHART] weeks whatever the period.
 */
class ObserveStatisticsUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {

    operator fun invoke(period: StatisticsPeriod): Flow<PantryStatistics> {
        val today = LocalDate.now(clock)
        val chartStart = today.weekStart().minusWeeks(PantryStatistics.WEEKS_IN_CHART - 1L)
        val periodStart: LocalDate? = when (period) {
            StatisticsPeriod.WEEK -> today.weekStart()
            StatisticsPeriod.MONTH -> today.withDayOfMonth(1)
            StatisticsPeriod.ALL_TIME -> null
        }
        val queryStart = periodStart?.let { minOf(it, chartStart) }

        return combine(
            pantryRepository.observeRemovedItemsSince(queryStart?.toInstant() ?: Instant.EPOCH),
            recipeRepository.observeCookedCountSince(periodStart?.toInstant() ?: Instant.EPOCH),
            settingsRepository.settings,
        ) { removed, cookedCount, settings ->
            val inPeriod = removed.filter { periodStart == null || !it.removedOn().isBefore(periodStart) }
            val (wasted, rescued) = inPeriod.partition { it.isWasted }
            PantryStatistics(
                rescuedCount = rescued.size,
                wastedCount = wasted.size,
                cookedRecipeCount = cookedCount,
                estimatedSavings = rescued.size * settings.averageItemValue,
                currencyCode = settings.currencyCode,
                weekly = weeklyChart(removed, chartStart),
                topWastedCategories = wasted
                    .groupingBy { it.category }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(TOP_CATEGORIES)
                    .map { it.key to it.value },
            )
        }
    }

    private fun weeklyChart(removed: List<PantryItem>, chartStart: LocalDate): List<WeekStatistics> {
        val byWeek = removed.groupBy { it.removedOn().weekStart() }
        return (0 until PantryStatistics.WEEKS_IN_CHART).map { offset ->
            val week = chartStart.plusWeeks(offset.toLong())
            val items = byWeek[week].orEmpty()
            WeekStatistics(
                weekStart = week,
                rescued = items.count { !it.isWasted },
                wasted = items.count { it.isWasted },
            )
        }
    }

    private fun PantryItem.removedOn(): LocalDate = updatedAt.atZone(clock.zone).toLocalDate()

    private fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun LocalDate.toInstant(): Instant = atStartOfDay(clock.zone).toInstant()

    private companion object {
        const val TOP_CATEGORIES = 3
    }
}
