package com.shelfie.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.PantryScanException
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.domain.usecase.GetExpiringIngredientsUseCase
import com.shelfie.app.notification.FreshnessNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalTime

/**
 * Daily pantry check (see [FreshnessCheckScheduler]). If critical items exist it prepares a
 * rescue recipe for them up front, so the notification can say the recipe is ready and opening
 * it shows that saved recipe instantly.
 */
@HiltWorker
class FreshnessCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val getExpiringIngredients: GetExpiringIngredientsUseCase,
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: FreshnessNotifier,
    private val clock: Clock,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // The schedule is cancelled when notifications are turned off; this covers a run that
        // was already pending at that moment.
        if (!settingsRepository.settings.first().notificationsEnabled) return Result.success()

        val ingredients = try {
            getExpiringIngredients()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.retry()
        }
        if (ingredients.isEmpty()) return Result.success()

        val mealType = MealType.at(LocalTime.now(clock))
        val saved = recipeRepository.getTodaysRecipeFor(ingredients)
            ?: recipeRepository.generateRescueRecipe(ingredients, mealType, isBackground = true).getOrElse { error ->
                // A flaky connection at reminder time shouldn't cost the user the recipe; try
                // again a bit later, then fall back to a plain reminder.
                if (error is PantryScanException.Network && runAttemptCount < MAX_NETWORK_RETRIES) {
                    return Result.retry()
                }
                null
            }

        notifier.showRescueRecipe(ingredients, saved, mealType)
        return Result.success()
    }

    private companion object {
        const val MAX_NETWORK_RETRIES = 2
    }
}
