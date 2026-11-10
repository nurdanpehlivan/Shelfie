package com.shelfie.app.presentation.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.HouseholdActivity
import com.shelfie.app.domain.model.HouseholdRole
import com.shelfie.app.presentation.common.ScreenHeader
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.activityLabelRes
import com.shelfie.app.presentation.common.displayName

/**
 * Account, family sharing and the family's recent activity, one tap from the pantry. Signed
 * out, it leads with what an account is for, so the sign-in button isn't a mystery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountRoute(
    /** `null` when shown as a tab, which has no back arrow. */
    onNavigateUp: (() -> Unit)?,
    onOpenPremium: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    householdViewModel: HouseholdViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val accountUi by viewModel.accountUi.collectAsStateWithLifecycle()
    val householdState by householdViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(accountUi.message) {
        val message = accountUi.message ?: return@LaunchedEffect
        viewModel.messageShown()
        snackbarHostState.showSnackbar(resources.getString(message))
    }

    // Someone may have joined, left or changed something while the app was in the background.
    LifecycleResumeEffect(householdViewModel) {
        householdViewModel.refresh()
        onPauseOrDispose {}
    }
    val household = householdState.household
    val myEmail = account?.email

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // As a tab it has a big header like the other tabs; pushed from settings, a back arrow.
        topBar = {
            if (onNavigateUp != null) ShelfieTopBar(title = stringResource(R.string.account_screen_title), onNavigateUp = onNavigateUp)
        },
    ) { padding ->
        // Lazy, so only what is on screen is composed; the full feed lives on its own screen.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onNavigateUp == null) {
                item(key = "header") {
                    ScreenHeader(
                        title = stringResource(R.string.tab_family),
                        subtitle = stringResource(R.string.family_subtitle),
                    )
                }
            }
            item(key = "hero") { HouseholdHero(state = householdState, myEmail = myEmail) }

            if (account == null) {
                item(key = "signIn") { SignInCard(onSignInClick = viewModel::openSignIn) }
                item(key = "signInHint") { HouseholdSignInHint(linkCode = householdState.linkCode) }
            } else {
                item(key = "status") {
                    HouseholdStatus(state = householdState, onRetry = householdViewModel::refresh, onOpenPremium = onOpenPremium)
                }
                if (household != null) {
                    if (householdState.isShared) {
                        item(key = "members") {
                            MembersCard(
                                household = household,
                                myEmail = myEmail,
                                isWorking = householdState.isWorking,
                                onRemove = householdViewModel::remove,
                                onLeave = householdViewModel::leave,
                            )
                        }
                        item(key = "activity") {
                            FamilyCard(title = stringResource(R.string.activity_title)) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ActivityPreview(events = householdState.activity, onSeeAll = onOpenNotifications)
                                }
                            }
                        }
                    }
                    // An owner with members can still invite; a member can't.
                    if (household.role != HouseholdRole.MEMBER) {
                        item(key = "invite") {
                            InviteCard(household = household, isWorking = householdState.isWorking, onCreateInvite = householdViewModel::createInvite)
                        }
                    }
                    // An owner with members can't join elsewhere; the server would refuse anyway.
                    if (household.role == HouseholdRole.NONE) {
                        item(key = "join") {
                            JoinCard(initialCode = householdState.linkCode, isWorking = householdState.isWorking, onJoin = householdViewModel::join)
                        }
                    }
                }
                item(key = "account") {
                    FamilyCard(title = stringResource(R.string.settings_section_account)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

/** Signed out: what an account is for, and the button to make one. */
@Composable
private fun SignInCard(onSignInClick: () -> Unit) {
    FamilyCard(title = stringResource(R.string.account_hero_title)) {
        Text(
            text = stringResource(R.string.account_hero_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onSignInClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_sign_in_or_register))
        }
    }
}

/** The newest few events and a way to the full list on the notifications screen. */
@Composable
private fun ColumnScope.ActivityPreview(events: List<HouseholdActivity>?, onSeeAll: () -> Unit) {
    if (events.isNullOrEmpty()) {
        Text(
            text = stringResource(if (events == null) R.string.activity_loading else R.string.activity_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    events.take(ACTIVITY_PREVIEW_COUNT).forEach { ActivityRow(it) }
    if (events.size > ACTIVITY_PREVIEW_COUNT) {
        TextButton(onClick = onSeeAll, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.activity_see_all) + " (${events.size})")
        }
    }
}

private const val ACTIVITY_PREVIEW_COUNT = 3

/** One event: "🥛 Milk / ayse · Added · 5 min ago". */
@Composable
internal fun ActivityRow(event: HouseholdActivity, modifier: Modifier = Modifier) {
    val you = stringResource(R.string.activity_you)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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
                    // Server time; a phone clock running behind would otherwise say "in 1 minute".
                    DateUtils.getRelativeTimeSpanString(minOf(event.at.toEpochMilli(), System.currentTimeMillis())).toString(),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (event.isMe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
