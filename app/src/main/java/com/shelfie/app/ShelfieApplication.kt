package com.shelfie.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shelfie.app.data.local.dao.SyncDao
import com.shelfie.app.data.sync.AccountStore
import com.shelfie.app.di.ApplicationScope
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.SettingsRepository
import com.shelfie.app.notification.FreshnessNotifier
import com.shelfie.app.presentation.widget.PantryWidget
import com.shelfie.app.work.FreshnessCheckScheduler
import com.shelfie.app.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    @Inject lateinit var accountStore: AccountStore

    @Inject lateinit var syncDao: SyncDao

    @Inject lateinit var syncScheduler: SyncScheduler

    @Inject lateinit var pantryRepository: PantryRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        freshnessNotifier.createChannel()
        keepDailyCheckInSyncWithSettings()
        syncLocalChanges()
        keepWidgetInSyncWithPantry()
    }

    /** Redraws the home-screen widget whenever the pantry changes (a no-op without widgets). */
    private fun keepWidgetInSyncWithPantry() {
        applicationScope.launch {
            pantryRepository.observePantryItems()
                .distinctUntilChanged()
                .collect { runCatching { PantryWidget().updateAll(this@ShelfieApplication) } }
        }
    }

    /**
     * While signed in: syncs once at start-up (to pick up changes made elsewhere), then shortly
     * after every local change, which the database's triggers put in the outbox.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun syncLocalChanges() {
        applicationScope.launch {
            accountStore.session
                .map { it != null }
                .distinctUntilChanged()
                .flatMapLatest { signedIn ->
                    if (!signedIn) return@flatMapLatest emptyFlow()
                    syncScheduler.requestSync(delaySeconds = 0)
                    syncDao.observeOutboxCount().filter { it > 0 }
                }
                .collect { syncScheduler.requestSync() }
        }
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
