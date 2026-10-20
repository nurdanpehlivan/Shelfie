package com.shelfie.app.presentation.settings

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfie.app.R
import com.shelfie.app.domain.model.Household
import com.shelfie.app.domain.model.HouseholdException
import com.shelfie.app.domain.model.HouseholdRole
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
)

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.account.map { it != null }.distinctUntilChanged().collect { signedIn ->
                _uiState.value = HouseholdUiState(isSignedIn = signedIn)
                if (signedIn) run { householdRepository.household() }
            }
        }
    }

    fun createInvite() = run {
        householdRepository.createInvite().map { invite ->
            checkNotNull(_uiState.value.household).copy(invite = invite)
        }
    }

    fun join(code: String) = run { householdRepository.join(code) }

    fun leave() = run { householdRepository.leave() }

    fun remove(email: String) = run { householdRepository.removeMember(email) }

    private fun run(action: suspend () -> Result<Household>) {
        _uiState.update { it.copy(isWorking = true, errorRes = null) }
        viewModelScope.launch {
            action()
                .onSuccess { household -> _uiState.update { it.copy(household = household, isWorking = false) } }
                .onFailure { error -> _uiState.update { it.copy(isWorking = false, errorRes = error.toMessageRes()) } }
        }
    }

    private fun Throwable.toMessageRes(): Int = when (this) {
        is HouseholdException.InvalidCode -> R.string.household_error_code
        is HouseholdException.Conflict -> when {
            serverMessage?.contains("full", ignoreCase = true) == true -> R.string.household_error_full
            serverMessage?.contains("own household", ignoreCase = true) == true -> R.string.household_error_own
            else -> R.string.household_error_has_members
        }
        is HouseholdException.Network -> R.string.account_error_network
        else -> R.string.account_error_server
    }
}

/** Content of the "Family sharing" settings card. */
@Composable
fun ColumnScope.HouseholdSectionContent(viewModel: HouseholdViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var code by rememberSaveable { mutableStateOf("") }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.household_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!state.isSignedIn) return

    val household = state.household
    if (household == null) {
        if (state.isWorking) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        return
    }

    if (household.role != HouseholdRole.NONE) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            household.members.forEach { member ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (member.isOwner) "👑 " else "👤 ") + member.email,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (household.role == HouseholdRole.OWNER && !member.isOwner) {
                        IconButton(onClick = { viewModel.remove(member.email) }, enabled = !state.isWorking) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.household_remove, member.email),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }

    when (household.role) {
        HouseholdRole.MEMBER -> TextButton(onClick = { confirmLeave = true }, enabled = !state.isWorking) {
            Text(stringResource(R.string.household_leave), color = MaterialTheme.colorScheme.error)
        }
        else -> {
            val invite = household.invite
            if (invite != null) {
                Text(
                    text = invite.code.chunked(4).joinToString(" "),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.household_invite_valid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = viewModel::createInvite, enabled = !state.isWorking, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (invite == null) R.string.household_invite else R.string.household_invite_new), maxLines = 1)
                }
                if (invite != null) {
                    Button(
                        onClick = {
                            val text = context.getString(R.string.household_share_message, invite.code)
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                            context.startActivity(Intent.createChooser(send, null))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.household_share), maxLines = 1) }
                }
            }
            // An owner with members can't join elsewhere; the server would refuse anyway.
            if (household.role == HouseholdRole.NONE) {
                Spacer(Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().take(INVITE_INPUT_LENGTH) },
                        label = { Text(stringResource(R.string.household_code)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.join(code) }, enabled = !state.isWorking && code.isNotBlank()) {
                        Text(stringResource(R.string.household_join))
                    }
                }
            }
        }
    }

    state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.household_leave_title)) },
            text = { Text(stringResource(R.string.household_leave_message)) },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; viewModel.leave() }) {
                    Text(stringResource(R.string.household_leave), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** 8 characters, plus room for a space or dash pasted from the shared message. */
private const val INVITE_INPUT_LENGTH = 9
