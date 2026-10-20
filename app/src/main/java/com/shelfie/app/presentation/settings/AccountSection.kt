package com.shelfie.app.presentation.settings

import android.text.format.DateUtils
import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shelfie.app.BuildConfig
import com.shelfie.app.R
import com.shelfie.app.domain.model.Account

/** Minimum enforced by the server too (backend/src/auth.ts). */
private const val MIN_PASSWORD_LENGTH = 8

private val WEB_PANEL_URL = BuildConfig.SHELFIE_PROXY_URL.trimEnd('/') + "/panel"

/** Content of the "Account & sync" settings card. */
@Composable
fun ColumnScope.AccountSectionContent(
    account: Account?,
    isWorking: Boolean,
    onSignInClick: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    if (account == null) {
        Text(
            text = stringResource(R.string.account_signed_out_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onSignInClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_sign_in_or_register))
        }
        return
    }

    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(text = account.email, style = MaterialTheme.typography.titleSmall)
        Text(
            text = account.lastSyncAt?.let {
                stringResource(
                    R.string.account_last_sync,
                    DateUtils.getRelativeTimeSpanString(it.toEpochMilli()).toString(),
                )
            } ?: stringResource(R.string.account_never_synced),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onSyncNow, enabled = !isWorking, modifier = Modifier.weight(1f)) {
            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.account_sync_now))
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { uriHandler.openUri(WEB_PANEL_URL) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.account_open_web_panel))
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onSignOut, enabled = !isWorking) { Text(stringResource(R.string.account_sign_out)) }
        TextButton(onClick = { confirmDelete = true }, enabled = !isWorking) {
            Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteAccount()
                }) { Text(stringResource(R.string.account_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

/** Sign in or create an account, in one sheet with a mode switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    isWorking: Boolean,
    errorRes: Int?,
    onSubmit: (email: String, password: String, isRegistration: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var isRegistration by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    // Not saveable on purpose: a password shouldn't end up in the saved instance state.
    var password by remember { mutableStateOf("") }
    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordValid = password.length >= MIN_PASSWORD_LENGTH
    val canSubmit = emailValid && passwordValid && !isWorking
    val submit = { if (canSubmit) onSubmit(email, password, isRegistration) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (isRegistration) R.string.account_register_title else R.string.account_sign_in_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.account_sheet_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.account_email)) },
                singleLine = true,
                isError = email.isNotBlank() && !emailValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.account_password)) },
                supportingText = { Text(stringResource(R.string.account_password_hint, MIN_PASSWORD_LENGTH)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorRes != null) {
                Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(if (isRegistration) R.string.account_register else R.string.account_sign_in))
            }
            TextButton(onClick = { isRegistration = !isRegistration }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(if (isRegistration) R.string.account_switch_to_sign_in else R.string.account_switch_to_register))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
