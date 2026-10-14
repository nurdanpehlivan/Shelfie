package com.shelfie.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings are written straight to DataStore; ShelfieApplication watches them and re-times the
 * daily check, so this screen doesn't need to know about WorkManager.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** `null` until the stored settings are read, so the controls don't flash defaults. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setNotificationsEnabled(enabled: Boolean) = launch { setNotificationsEnabled(enabled) }

    fun setReminderHour(hour: Int) = launch { setReminderHour(hour) }

    fun setCriticalDays(days: Long) = launch { setCriticalDays(days) }

    fun setAverageItemValue(value: Double) = launch { setAverageItemValue(value) }

    private fun launch(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }
}
