package com.shelfie.app.presentation.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.HouseholdRole
import com.shelfie.app.presentation.common.LocalAppInfo

/** Content of the "Family sharing" settings card. */
@Composable
fun ColumnScope.HouseholdSectionContent(viewModel: HouseholdViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    var code by rememberSaveable(state.linkCode) { mutableStateOf(state.linkCode) }
    val appInfo = LocalAppInfo.current
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.household_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!state.isSignedIn) {
        // Sharing goes through accounts: say what to do instead of showing nothing.
        Text(
            text = if (state.linkCode.isNotEmpty()) {
                stringResource(R.string.household_code_ready, state.linkCode.chunked(4).joinToString(" "))
            } else {
                stringResource(R.string.household_sign_in_first)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        return
    }

    // Someone may have joined while the app was in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    val household = state.household
    if (household == null) {
        if (state.isWorking) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        state.errorRes?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.scanner_action_retry)) }
        }
        return
    }

    if (state.justJoined && household.role == HouseholdRole.MEMBER) {
        Text(
            text = stringResource(R.string.household_joined),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
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
                    text = stringResource(R.string.household_limit, household.maxMembers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            val text = resources.getString(R.string.household_share_message, invite.code, appInfo.inviteUrl(invite.code))
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
