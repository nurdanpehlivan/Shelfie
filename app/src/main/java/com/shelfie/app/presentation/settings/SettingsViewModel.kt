package com.shelfie.app.presentation.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.R
import com.shelfie.app.domain.model.Account
import com.shelfie.app.domain.model.AccountException
import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.ThemeMode
import com.shelfie.app.domain.model.SyncException
import com.shelfie.app.domain.repository.AccountRepository
import com.shelfie.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings are written straight to DataStore; ShelfieApplication watches them and re-times the
 * daily check, so this screen doesn't need to know about WorkManager.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    /** `null` until the stored settings are read, so the controls don't flash defaults. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val account: StateFlow<Account?> = accountRepository.account
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _accountUi = MutableStateFlow(AccountUiState())
    val accountUi: StateFlow<AccountUiState> = _accountUi.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) = launch { setNotificationsEnabled(enabled) }

    fun setReminderHour(hour: Int) = launch { setReminderHour(hour) }

    fun setCriticalDays(days: Long) = launch { setCriticalDays(days) }

    fun setAverageItemValue(value: Double) = launch { setAverageItemValue(value) }

    fun setThemeMode(mode: ThemeMode) = launch { setThemeMode(mode) }

    fun openSignIn() = _accountUi.update { it.copy(isSheetOpen = true, formError = null) }

    fun closeSignIn() = _accountUi.update { it.copy(isSheetOpen = false, formError = null) }

    fun submitCredentials(email: String, password: String, isRegistration: Boolean) {
        if (_accountUi.value.isWorking) return
        _accountUi.update { it.copy(isWorking = true, formError = null) }
        viewModelScope.launch {
            val result = if (isRegistration) {
                accountRepository.register(email, password)
            } else {
                accountRepository.signIn(email, password)
            }
            _accountUi.update { state ->
                result.fold(
                    onSuccess = { state.copy(isWorking = false, isSheetOpen = false, message = R.string.account_signed_in) },
                    onFailure = { state.copy(isWorking = false, formError = it.toMessage()) },
                )
            }
        }
    }

    fun syncNow() {
        if (_accountUi.value.isWorking) return
        _accountUi.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val result = accountRepository.syncNow()
            _accountUi.update { state ->
                state.copy(
                    isWorking = false,
                    message = result.fold(onSuccess = { R.string.account_sync_done }, onFailure = { it.toMessage() }),
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            accountRepository.signOut()
            _accountUi.update { it.copy(message = R.string.account_signed_out) }
        }
    }

    fun deleteAccount() {
        if (_accountUi.value.isWorking) return
        _accountUi.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val result = accountRepository.deleteAccount()
            _accountUi.update { state ->
                state.copy(
                    isWorking = false,
                    message = result.fold(onSuccess = { R.string.account_deleted }, onFailure = { it.toMessage() }),
                )
            }
        }
    }

    fun messageShown() = _accountUi.update { it.copy(message = null) }

    private fun launch(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }

    @StringRes
    private fun Throwable.toMessage(): Int = when (this) {
        is AccountException.InvalidCredentials -> R.string.account_error_credentials
        is AccountException.EmailTaken -> R.string.account_error_email_taken
        is AccountException.InvalidInput -> R.string.account_error_input
        is AccountException.TooManyAttempts -> R.string.account_error_too_many
        is AccountException.Network, is SyncException.Network -> R.string.account_error_network
        is SyncException.SessionExpired -> R.string.account_error_session_expired
        else -> R.string.account_error_server
    }
}

data class AccountUiState(
    val isSheetOpen: Boolean = false,
    /** A request (sign-in, sync, deletion) is running. */
    val isWorking: Boolean = false,
    /** Shown inside the sign-in sheet. */
    @StringRes val formError: Int? = null,
    /** One-off result shown as a snackbar, then cleared with [SettingsViewModel.messageShown]. */
    @StringRes val message: Int? = null,
)
