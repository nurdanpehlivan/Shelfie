package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Picks the [FreshnessStatus.CRITICAL] items (by the user's own threshold) a rescue recipe
 * should use, soonest expiry first.
 *
 * Items already past their expiry date are left out: the app shouldn't suggest cooking food
 * that may no longer be safe. Products with the same name count once.
 */
class GetExpiringIngredientsUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(limit: Int = MAX_INGREDIENTS): List<PantryItem> {
        val today = LocalDate.now(clock)
        val thresholds = settingsRepository.settings.first().freshnessThresholds
        return pantryRepository.observeItemsByFreshness(FreshnessStatus.CRITICAL, today, thresholds)
            .first()
            .asSequence()
            .filterNot { it.isExpired(today) }
            .distinctBy { it.name.trim().lowercase() }
            .take(limit)
            .toList()
    }

    companion object {
        /** More than three and a 15-minute recipe stops being realistic. */
        const val MAX_INGREDIENTS = 3
    }
}
