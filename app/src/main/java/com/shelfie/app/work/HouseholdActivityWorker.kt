package com.shelfie.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shelfie.app.domain.repository.HouseholdRepository
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.notification.HouseholdActivityNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Tells the user what the rest of the household did (added, used up, threw away...) and pulls
 * those changes in. Runs right away when a push arrives (FrizzyMessagingService), and every
 * [PERIOD_MINUTES] while signed in in case a push was missed; opening the app checks too.
 */
@HiltWorker
class HouseholdActivityWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val householdRepository: HouseholdRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: HouseholdActivityNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val events = householdRepository.takeActivityToNotify()
        if (settingsRepository.settings.first().notificationsEnabled) notifier.show(events)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "household_activity"
        private const val WORK_NOW = "household_activity_now"
        private const val PERIOD_MINUTES = 60L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HouseholdActivityWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** After a push; a burst of pushes is one run. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<HouseholdActivityWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NOW)
        }
    }
}
