package com.shelfie.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.ThemeMode
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.LocalAppInfo
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.formatQuantity
import com.shelfie.app.presentation.common.parseQuantity
import com.shelfie.app.presentation.common.shelfieSegmentedColors
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.extendedColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

@Composable
fun SettingsRoute(
    onNavigateUp: () -> Unit,
    onOpenPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val accountUi by viewModel.accountUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(accountUi.message) {
        val message = accountUi.message ?: return@LaunchedEffect
        viewModel.messageShown()
        snackbarHostState.showSnackbar(context.getString(message))
    }

    SettingsScreen(
        settings = settings,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onNotificationsChange = viewModel::setNotificationsEnabled,
        onReminderHourChange = viewModel::setReminderHour,
        onCriticalDaysChange = viewModel::setCriticalDays,
        onAverageValueChange = viewModel::setAverageItemValue,
        onThemeModeChange = viewModel::setThemeMode,
        onOpenPremium = onOpenPremium,
        accountSection = {
            AccountSectionContent(
                account = account,
                isWorking = accountUi.isWorking,
                onSignInClick = viewModel::openSignIn,
                onSyncNow = viewModel::syncNow,
                onSignOut = viewModel::signOut,
                onDeleteAccount = viewModel::deleteAccount,
            )
        },
    )

    if (accountUi.isSheetOpen) {
        AccountSheet(
            isWorking = accountUi.isWorking,
            errorRes = accountUi.formError,
            onSubmit = viewModel::submitCredentials,
            onDismiss = viewModel::closeSignIn,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings?,
    onNavigateUp: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onReminderHourChange: (Int) -> Unit,
    onCriticalDaysChange: (Long) -> Unit,
    onAverageValueChange: (Double) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    accountSection: @Composable ColumnScope.() -> Unit = {},
    onOpenPremium: () -> Unit = {},
    householdSection: @Composable ColumnScope.() -> Unit = { HouseholdSectionContent() },
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShelfieTopBar(title = stringResource(R.string.settings_title), onNavigateUp = onNavigateUp)
        },
    ) { padding ->
        if (settings == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumCard(onClick = onOpenPremium)

            SettingsCard(title = stringResource(R.string.settings_section_appearance)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            colors = shelfieSegmentedColors(),
                            icon = {},
                        ) { Text(stringResource(mode.labelRes)) }
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.settings_language_title)) {
                LanguagePickerContent()
            }

            SettingsCard(title = stringResource(R.string.settings_section_notifications)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = stringResource(R.string.settings_notifications_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = settings.notificationsEnabled, onCheckedChange = onNotificationsChange)
                }
                AnimatedVisibility(visible = settings.notificationsEnabled) {
                    ReminderTimePicker(hour = settings.reminderHour, onHourChange = onReminderHourChange)
                }
            }

            SettingsCard(title = stringResource(R.string.settings_section_freshness)) {
                Text(stringResource(R.string.settings_critical_days), style = MaterialTheme.typography.bodyLarge)
                val dayOptions = (FreshnessThresholds.MIN_CRITICAL_DAYS..FreshnessThresholds.MAX_CRITICAL_DAYS).toList()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    dayOptions.forEachIndexed { index, days ->
                        SegmentedButton(
                            selected = settings.freshnessThresholds.criticalDays == days,
                            onClick = { onCriticalDaysChange(days) },
                            shape = SegmentedButtonDefaults.itemShape(index, dayOptions.size),
                            colors = shelfieSegmentedColors(),
                            icon = {},
                        ) { Text(pluralStringResource(R.plurals.settings_days, days.toInt(), days.toInt())) }
                    }
                }
                Text(
                    text = stringResource(
                        R.string.settings_critical_hint,
                        settings.freshnessThresholds.criticalDays,
                        settings.freshnessThresholds.warningDays,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsCard(title = stringResource(R.string.settings_section_statistics)) {
                AverageValueField(settings.averageItemValue, onAverageValueChange)
            }

            SettingsCard(title = stringResource(R.string.settings_section_account), content = accountSection)

            SettingsCard(title = stringResource(R.string.household_title), content = householdSection)

            AboutCard()
        }
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
        ThemeMode.SYSTEM -> R.string.settings_theme_system
    }

/** Entry to Premium at the top of the settings, on the evergreen hero surface. */
@Composable
private fun PremiumCard(onClick: () -> Unit) {
    val extended = MaterialTheme.extendedColors
    HighlightCard(onClick = onClick, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "✨", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.premium_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.premium_settings_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.onHeroVariant,
                )
            }
        }
    }
}

/** A titled group of settings: the title sits above a flat white card, like a list section. */
@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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

/**
 * Big, readable time with −/+ buttons: one tap per hour beats dragging a 16-stop slider.
 * The time is shown in the user's own format ("9:00 AM" or "09:00").
 */
@Composable
private fun ReminderTimePicker(hour: Int, onHourChange: (Int) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_reminder_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = LocalTime.of(hour, 0).format(formatter), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.settings_reminder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(
            onClick = { onHourChange(hour - 1) },
            enabled = hour > AppSettings.MIN_REMINDER_HOUR,
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
        }
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = { onHourChange(hour + 1) },
            enabled = hour < AppSettings.MAX_REMINDER_HOUR,
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        }
    }
}

@Composable
private fun AverageValueField(value: Double, onValueChange: (Double) -> Unit) {
    // Seeded once: re-seeding from [value] on every keystroke would eat a trailing "1,".
    var text by rememberSaveable { mutableStateOf(formatQuantity(value)) }
    val parsed = parseQuantity(text)
    val symbol = remember { runCatching { Currency.getInstance(Locale.getDefault()).symbol }.getOrDefault("") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            parseQuantity(it)?.takeIf { number -> number >= 0 }?.let(onValueChange)
        },
        label = { Text(stringResource(R.string.settings_average_value)) },
        suffix = { Text(symbol) },
        supportingText = { Text(stringResource(R.string.settings_average_value_hint)) },
        isError = parsed == null || parsed < 0,
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Frizzy waving goodbye at the bottom, with the version for support requests. */
@Composable
private fun AboutCard() {
    val uriHandler = LocalUriHandler.current
    val privacyUrl = LocalAppInfo.current.privacyPolicyUrl
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Frizzy(mood = FrizzyMood.HAPPY, size = 56.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_about_version, LocalAppInfo.current.versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { uriHandler.openUri(privacyUrl) },
                contentPadding = PaddingValues(0.dp),
            ) { Text(stringResource(R.string.settings_privacy)) }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
