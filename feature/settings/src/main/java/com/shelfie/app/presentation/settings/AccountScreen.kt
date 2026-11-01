package com.shelfie.app.presentation.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.HouseholdActivity
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.activityLabelRes
import com.shelfie.app.presentation.common.displayName
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood

/**
 * Account, family sharing and the family's recent activity, one tap from the pantry. Signed
 * out, it leads with what an account is for, so the sign-in button isn't a mystery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountRoute(
    onNavigateUp: () -> Unit,
    onOpenPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    householdViewModel: HouseholdViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val accountUi by viewModel.accountUi.collectAsStateWithLifecycle()
    val household by householdViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(accountUi.message) {
        val message = accountUi.message ?: return@LaunchedEffect
        viewModel.messageShown()
        snackbarHostState.showSnackbar(resources.getString(message))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ShelfieTopBar(title = stringResource(R.string.account_screen_title), onNavigateUp = onNavigateUp) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (account == null) {
                SignedOutHero(onSignInClick = viewModel::openSignIn)
            } else {
                Section(title = stringResource(R.string.settings_section_account)) {
                    AccountSectionContent(
                        account = account,
                        isWorking = accountUi.isWorking,
                        onSignInClick = viewModel::openSignIn,
                        onSyncNow = viewModel::syncNow,
                        onSignOut = viewModel::signOut,
                        onDeleteAccount = viewModel::deleteAccount,
                    )
                }
            }

            Section(title = stringResource(R.string.household_title)) {
                HouseholdSectionContent(onOpenPremium = onOpenPremium, viewModel = householdViewModel)
            }

            if (household.isShared) {
                Section(title = stringResource(R.string.activity_title)) {
                    ActivityFeed(events = household.activity)
                }
            }
        }
    }

    if (accountUi.isSheetOpen) {
        AccountSheet(
            isWorking = accountUi.isWorking,
            errorRes = accountUi.formError,
            infoRes = accountUi.formInfo,
            onSubmit = viewModel::submitCredentials,
            onRequestReset = viewModel::requestPasswordReset,
            onModeChange = viewModel::clearFormMessages,
            onDismiss = viewModel::closeSignIn,
        )
    }
}

@Composable
private fun SignedOutHero(onSignInClick: () -> Unit) {
    ShelfieCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Frizzy(mood = FrizzyMood.HAPPY, size = 88.dp)
            Text(
                text = stringResource(R.string.account_hero_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.account_hero_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(4.dp))
            Button(onClick = onSignInClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_sign_in_or_register))
            }
        }
    }
}

/** Newest first: "🥛 Milk · ayse · Added · 5 min ago". */
@Composable
private fun ColumnScope.ActivityFeed(events: List<HouseholdActivity>?) {
    if (events.isNullOrEmpty()) {
        Text(
            text = stringResource(if (events == null) R.string.activity_loading else R.string.activity_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val you = stringResource(R.string.activity_you)
    events.forEach { event ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = event.emoji ?: "•", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(
                        if (event.isMe) you else displayName(event.actor),
                        stringResource(event.action.activityLabelRes),
                        DateUtils.getRelativeTimeSpanString(event.at.toEpochMilli()).toString(),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (event.isMe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A titled card, like the groups on the settings screen. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )
        ShelfieCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}
