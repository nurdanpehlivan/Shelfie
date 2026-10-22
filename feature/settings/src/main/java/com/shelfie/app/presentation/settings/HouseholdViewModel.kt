package com.shelfie.app.presentation.settings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Household
import com.shelfie.app.domain.model.HouseholdException
import com.shelfie.app.domain.repository.AccountRepository
import com.shelfie.app.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HouseholdUiState(
    val isSignedIn: Boolean = false,
    val household: Household? = null,
    val isWorking: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    /** Set right after joining: the shared data is on its way to this phone. */
    val justJoined: Boolean = false,
    /** A code from an invite link, to fill in; it waits through signing in. */
    val linkCode: String = "",
)

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The settings route's `joinCode` argument, set when an invite link opened the app. */
    private val linkCode: String = savedStateHandle.get<String>(JOIN_CODE_ARG).orEmpty()

    private val _uiState = MutableStateFlow(HouseholdUiState(linkCode = linkCode))
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.account.map { it != null }.distinctUntilChanged().collect { signedIn ->
                _uiState.value = HouseholdUiState(isSignedIn = signedIn, linkCode = linkCode)
                if (signedIn) refresh()
            }
        }
    }

    /** Reloads members and the invite, e.g. when the screen comes back after someone joined. */
    fun refresh() {
        if (!_uiState.value.isSignedIn || _uiState.value.isWorking) return
        run { householdRepository.household() }
    }

    fun createInvite() = run {
        householdRepository.createInvite().map { invite ->
            checkNotNull(_uiState.value.household).copy(invite = invite)
        }
    }

    fun join(code: String) = run(joining = true) { householdRepository.join(code) }

    fun leave() = run { householdRepository.leave() }

    fun remove(email: String) = run { householdRepository.removeMember(email) }

    private fun run(joining: Boolean = false, action: suspend () -> Result<Household>) {
        _uiState.update { it.copy(isWorking = true, errorRes = null) }
        viewModelScope.launch {
            action()
                .onSuccess { household ->
                    _uiState.update { it.copy(household = household, isWorking = false, justJoined = joining || it.justJoined) }
                }
                .onFailure { error -> _uiState.update { it.copy(isWorking = false, errorRes = error.toMessageRes()) } }
        }
    }

    private companion object {
        const val JOIN_CODE_ARG = "joinCode"
    }

    private fun Throwable.toMessageRes(): Int = when (this) {
        is HouseholdException.InvalidCode -> R.string.household_error_code
        is HouseholdException.Conflict -> when {
            serverMessage?.contains("Premium", ignoreCase = true) == true -> R.string.household_error_full_free
            serverMessage?.contains("full", ignoreCase = true) == true -> R.string.household_error_full
            serverMessage?.contains("own household", ignoreCase = true) == true -> R.string.household_error_own
            else -> R.string.household_error_has_members
        }
        is HouseholdException.Network -> R.string.account_error_network
        else -> R.string.account_error_server
    }
}
