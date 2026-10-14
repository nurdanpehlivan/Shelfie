package com.shelfie.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shelfie.app.di.ApplicationScope
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.notification.FreshnessNotifier
import com.shelfie.app.work.FreshnessCheckScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WorkManager is initialised on demand with [HiltWorkerFactory] (the default startup
 * initializer is removed in the manifest), so workers can use constructor injection.
 */
@HiltAndroidApp
class ShelfieApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var freshnessNotifier: FreshnessNotifier

    @Inject lateinit var freshnessCheckScheduler: FreshnessCheckScheduler

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        freshnessNotifier.createChannel()
        keepDailyCheckInSyncWithSettings()
    }

    /**
     * Schedules the daily check from the stored settings at start-up (keeping an existing
     * schedule), then re-times or cancels it whenever the user changes the reminder settings.
     */
    private fun keepDailyCheckInSyncWithSettings() {
        applicationScope.launch {
            var isFirst = true
            settingsRepository.settings
                .distinctUntilChangedBy { it.notificationsEnabled to it.reminderHour }
                .collect { settings ->
                    freshnessCheckScheduler.apply(settings, timeChanged = !isFirst)
                    isFirst = false
                }
        }
    }
}
