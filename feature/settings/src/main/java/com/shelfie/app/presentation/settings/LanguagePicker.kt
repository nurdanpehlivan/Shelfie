package com.shelfie.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.shelfie.app.core.ui.R
import com.shelfie.app.presentation.common.shelfieFilterChipColors

/**
 * The languages Shelfie is translated into, each named in itself so anyone can find theirs.
 * Keep in sync with the `values-*` folders of core:ui.
 */
private val APP_LANGUAGES = listOf(
    "en" to "English",
    "tr" to "Türkçe",
    "de" to "Deutsch",
    "es" to "Español",
    "fr" to "Français",
)

/**
 * "Device language" by default: Shelfie opens in the phone's language when it has it, and in
 * English otherwise. Picking one overrides that for this app only; the activity restarts in it.
 */
@Composable
fun ColumnScope.LanguagePickerContent() {
    var selected by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')) }

    Text(
        text = stringResource(R.string.settings_language_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf("" to stringResource(R.string.settings_language_system)) + APP_LANGUAGES).forEach { (tag, name) ->
            FilterChip(
                selected = selected == tag,
                onClick = {
                    selected = tag
                    AppCompatDelegate.setApplicationLocales(
                        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                    )
                },
                label = { Text(name) },
                shape = CircleShape,
                colors = shelfieFilterChipColors(),
            )
        }
    }
}
