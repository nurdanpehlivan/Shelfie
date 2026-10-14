package com.shelfie.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shelfie.app.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules [FreshnessCheckWorker] once a day around the user's reminder time.
 *
 * The first run is delayed until the next reminder time; WorkManager then repeats it every
 * 24 h. Android may defer periodic work (Doze, battery saver), so the time is best-effort.
 */
@Singleton
class FreshnessCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {

    private val workManager get() = WorkManager.getInstance(context)

    /**
     * Brings the scheduled work in line with [settings]. With [timeChanged] = false (app start)
     * an existing schedule keeps its timing; with true it is re-timed to the new hour.
     */
    fun apply(settings: AppSettings, timeChanged: Boolean) {
        if (!settings.notificationsEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val initialDelay = delayUntilNext(LocalDateTime.now(clock), settings.reminderTime)
        val request = PeriodicWorkRequestBuilder<FreshnessCheckWorker>(1, TimeUnit.DAYS)
            // The Duration overloads need API 26; minSdk is 24.
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (timeChanged) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val WORK_NAME = "daily_freshness_check"
        private const val RETRY_BACKOFF_MINUTES = 15L

        /** Time from [now] until the next [time] of day; a full day if it is exactly [time]. */
        internal fun delayUntilNext(now: LocalDateTime, time: LocalTime): Duration {
            var next = now.toLocalDate().atTime(time)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
