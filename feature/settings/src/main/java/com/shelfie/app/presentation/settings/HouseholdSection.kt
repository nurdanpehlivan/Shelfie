package com.shelfie.app.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Household
import com.shelfie.app.domain.model.HouseholdMember
import com.shelfie.app.domain.model.HouseholdRole
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.LocalAppInfo
import com.shelfie.app.presentation.common.Pill
import com.shelfie.app.presentation.common.PremiumNudge
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.displayName
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.extendedColors

/*
 * The family tab, as separate cards: a hero with who is in the household, the members, the
 * invite code, joining someone else's household. Each card shows only when it applies.
 */

/** The top of the tab: the household at a glance, or what sharing is for when there is none yet. */
@Composable
internal fun HouseholdHero(state: HouseholdUiState, myEmail: String?) {
    val extended = MaterialTheme.extendedColors
    val household = state.household
    HighlightCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                household != null && household.role != HouseholdRole.NONE -> {
                    AvatarStack(emails = household.members.map { it.email })
                    Text(text = stringResource(R.string.household_hero_title), style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(
                            text = stringResource(R.string.household_members_count, household.members.size, household.maxMembers),
                            containerColor = extended.heroTile,
                            contentColor = extended.onHero,
                        )
                        Pill(
                            text = stringResource(
                                if (household.role == HouseholdRole.OWNER) R.string.household_role_owner else R.string.household_role_member,
                            ),
                            containerColor = extended.heroTile,
                            contentColor = extended.onHero,
                        )
                    }
                    if (state.justJoined && household.role == HouseholdRole.MEMBER) {
                        Text(stringResource(R.string.household_joined), style = MaterialTheme.typography.bodyMedium, color = extended.onHeroVariant)
                    }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(if (myEmail == null) R.string.household_title else R.string.household_alone_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.household_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.onHeroVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Frizzy(mood = FrizzyMood.HAPPY, size = 72.dp)
                }
            }
        }
    }
}

/** Members with their roles; the owner can remove others, a member can leave. */
@Composable
internal fun MembersCard(
    household: Household,
    myEmail: String?,
    isWorking: Boolean,
    onRemove: (String) -> Unit,
    onLeave: () -> Unit,
) {
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf<String?>(null) }
    FamilyCard(title = stringResource(R.string.household_members_title)) {
        household.members.forEachIndexed { index, member ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MemberRow(
                member = member,
                isMe = member.email.equals(myEmail, ignoreCase = true),
                canRemove = household.role == HouseholdRole.OWNER && !member.isOwner,
                enabled = !isWorking,
                onRemove = { confirmRemove = member.email },
            )
        }
        if (household.role == HouseholdRole.MEMBER) {
            OutlinedButton(
                onClick = { confirmLeave = true },
                enabled = !isWorking,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) { Text(stringResource(R.string.household_leave), color = MaterialTheme.colorScheme.error) }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.household_leave_title)) },
            text = { Text(stringResource(R.string.household_leave_message)) },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onLeave() }) {
                    Text(stringResource(R.string.household_leave), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    confirmRemove?.let { email ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.household_remove, displayName(email))) },
            text = { Text(stringResource(R.string.household_remove_message)) },
            confirmButton = {
                TextButton(onClick = { confirmRemove = null; onRemove(email) }) {
                    Text(stringResource(R.string.household_remove_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun MemberRow(member: HouseholdMember, isMe: Boolean, canRemove: Boolean, enabled: Boolean, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(email = member.email, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName(member.email) + if (member.isOwner) " 👑" else "",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isMe) {
            Pill(
                text = stringResource(R.string.household_you),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (canRemove) {
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.household_remove, member.email),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** The invite code, big and easy to read out, with copy and share. */
@Composable
internal fun InviteCard(household: Household, isWorking: Boolean, onCreateInvite: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val appInfo = LocalAppInfo.current
    val invite = household.invite
    FamilyCard(title = stringResource(R.string.household_invite_title)) {
        if (invite != null) {
            val spaced = invite.code.chunked(4).joinToString(" ")
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = spaced,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { copyToClipboard(context, invite.code) }) {
                        Text(stringResource(R.string.household_copy))
                    }
                }
            }
            Text(
                text = stringResource(R.string.household_invite_valid) + " " + stringResource(R.string.household_limit, household.maxMembers),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.household_invite_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onCreateInvite, enabled = !isWorking, modifier = Modifier.weight(1f)) {
                Text(stringResource(if (invite == null) R.string.household_invite else R.string.household_invite_new), maxLines = 1)
            }
            if (invite != null) {
                Button(
                    onClick = {
                        val text = resources.getString(
                            R.string.household_share_message,
                            invite.code.chunked(4).joinToString(" "),
                            appInfo.playStoreInviteUrl(invite.code),
                        )
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                        context.startActivity(Intent.createChooser(send, null))
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.household_share), maxLines = 1) }
            }
        }
    }
}

/** Joining someone else's household with their code; only while not in one. */
@Composable
internal fun JoinCard(initialCode: String, isWorking: Boolean, onJoin: (String) -> Unit) {
    var code by rememberSaveable(initialCode) { mutableStateOf(initialCode) }
    FamilyCard(title = stringResource(R.string.household_join_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(INVITE_INPUT_LENGTH) },
                label = { Text(stringResource(R.string.household_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onJoin(code) }, enabled = !isWorking && code.isNotBlank(), modifier = Modifier.height(56.dp)) {
                Text(stringResource(R.string.household_join))
            }
        }
    }
}

/** Signed out, a code from an invite link waits here; otherwise, what to do first. */
@Composable
internal fun HouseholdSignInHint(linkCode: String) {
    Text(
        text = if (linkCode.isNotEmpty()) {
            stringResource(R.string.household_code_ready, linkCode.chunked(4).joinToString(" "))
        } else {
            stringResource(R.string.household_sign_in_first)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** Loading spinner, or the error with a retry, while there is no household to show yet. */
@Composable
internal fun HouseholdStatus(state: HouseholdUiState, onRetry: () -> Unit, onOpenPremium: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        if (state.household == null && state.isWorking) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        }
        state.errorRes?.let { error ->
            Text(stringResource(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (state.household == null) TextButton(onClick = onRetry) { Text(stringResource(R.string.scanner_action_retry)) }
        }
        if (state.errorRes == R.string.household_error_full_free) {
            PremiumNudge(message = stringResource(R.string.premium_nudge_family), onOpenPremium = onOpenPremium)
        }
    }
}

/** A titled glass card, the building block of the family tab. */
@Composable
internal fun FamilyCard(title: String, content: @Composable () -> Unit) {
    ShelfieCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** A round initial in a colour of its own, stable per person. */
@Composable
internal fun Avatar(email: String, size: Int) {
    val colors = avatarColors()
    val (background, foreground) = colors[Math.floorMod(email.lowercase().hashCode(), colors.size)]
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName(email).take(1).uppercase(),
            color = foreground,
            style = if (size >= 40) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The members' avatars overlapping, as in a group chat. */
@Composable
private fun AvatarStack(emails: List<String>) {
    val ring = MaterialTheme.extendedColors.hero
    Row {
        emails.take(MAX_STACKED).forEachIndexed { index, email ->
            Box(
                modifier = Modifier
                    .offset(x = (-10 * index).dp)
                    .border(3.dp, ring, CircleShape),
            ) { Avatar(email = email, size = 44) }
        }
    }
}

/**
 * Saturated colours with white initials: they stand out on the pale leaf hero and the cream
 * cards in light mode as well as on the dark background. (The theme's container tones were
 * almost the hero's own colour in light mode.)
 */
private val AVATAR_COLORS = listOf(
    Color(0xFF2E7D57), // emerald
    Color(0xFFD9822B), // amber
    Color(0xFFD9544A), // coral
    Color(0xFF4F63C4), // indigo
    Color(0xFF8E4FB5), // plum
    Color(0xFF1C8A8A), // teal
    Color(0xFFB5487A), // berry
)

@Composable
private fun avatarColors(): List<Pair<Color, Color>> = AVATAR_COLORS.map { it to Color.White }

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Frizzy", code))
}

private const val MAX_STACKED = 6

/** 8 characters, plus room for a space or dash pasted from the shared message. */
private const val INVITE_INPUT_LENGTH = 9
