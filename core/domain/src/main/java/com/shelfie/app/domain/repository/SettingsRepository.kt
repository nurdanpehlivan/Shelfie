package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    /** Emits the current settings right away, then on every change. */
    val settings: Flow<AppSettings>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    /** Clamped to [AppSettings.MIN_REMINDER_HOUR]..[AppSettings.MAX_REMINDER_HOUR]. */
    suspend fun setReminderHour(hour: Int)

    /** Clamped to the allowed range of [com.shelfie.app.domain.model.FreshnessThresholds]. */
    suspend fun setCriticalDays(days: Long)

    suspend fun setAverageItemValue(value: Double)

    suspend fun setOnboardingCompleted()

    suspend fun setThemeMode(mode: ThemeMode)

    /** `null` goes back to the phone's region. */
    suspend fun setCurrencyCode(code: String?)

    /** One-time data repairs that already ran on this install, by key. */
    suspend fun isMaintenanceDone(key: String): Boolean

    suspend fun markMaintenanceDone(key: String)
}
