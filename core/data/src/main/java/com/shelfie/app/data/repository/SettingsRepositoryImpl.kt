package com.shelfie.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.ThemeMode
import com.shelfie.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        // A corrupt or unreadable file falls back to defaults instead of crashing every screen.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                notificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
                reminderHour = (prefs[REMINDER_HOUR] ?: AppSettings.DEFAULT_REMINDER_HOUR).clampHour(),
                freshnessThresholds = FreshnessThresholds(
                    criticalDays = (prefs[CRITICAL_DAYS] ?: FreshnessThresholds.DEFAULT.criticalDays).clampDays(),
                ),
                averageItemValue = prefs[AVERAGE_ITEM_VALUE] ?: AppSettings.DEFAULT_AVERAGE_ITEM_VALUE,
                currencyCode = prefs[CURRENCY_CODE],
                onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false,
                themeMode = ThemeMode.fromName(prefs[THEME_MODE]),
            )
        }
        .distinctUntilChanged()

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setReminderHour(hour: Int) {
        dataStore.edit { it[REMINDER_HOUR] = hour.clampHour() }
    }

    override suspend fun setCriticalDays(days: Long) {
        dataStore.edit { it[CRITICAL_DAYS] = days.clampDays() }
    }

    override suspend fun setAverageItemValue(value: Double) {
        dataStore.edit { it[AVERAGE_ITEM_VALUE] = value.coerceIn(0.0, MAX_AVERAGE_ITEM_VALUE) }
    }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setCurrencyCode(code: String?) {
        dataStore.edit { prefs -> if (code == null) prefs.remove(CURRENCY_CODE) else prefs[CURRENCY_CODE] = code }
    }

    override suspend fun isMaintenanceDone(key: String): Boolean =
        dataStore.data.first()[MAINTENANCE_DONE].orEmpty().contains(key)

    override suspend fun markMaintenanceDone(key: String) {
        dataStore.edit { it[MAINTENANCE_DONE] = it[MAINTENANCE_DONE].orEmpty() + key }
    }

    private fun Int.clampHour() = coerceIn(AppSettings.MIN_REMINDER_HOUR, AppSettings.MAX_REMINDER_HOUR)

    private fun Long.clampDays() =
        coerceIn(FreshnessThresholds.MIN_CRITICAL_DAYS, FreshnessThresholds.MAX_CRITICAL_DAYS)

    private companion object {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val CRITICAL_DAYS = longPreferencesKey("critical_days")
        val AVERAGE_ITEM_VALUE = doublePreferencesKey("average_item_value")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val MAINTENANCE_DONE = stringSetPreferencesKey("maintenance_done")
        const val MAX_AVERAGE_ITEM_VALUE = 1_000_000.0
    }
}
