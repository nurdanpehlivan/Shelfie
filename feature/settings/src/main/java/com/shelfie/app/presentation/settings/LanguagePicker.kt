package com.shelfie.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.shelfie.app.core.ui.R

/**
 * The languages Shelfie is translated into, each named in itself so anyone can find theirs.
 * Keep in sync with the `values-*` folders of core:ui.
 */
private data class AppLanguage(val tag: String, val flag: String, val name: String)

private val APP_LANGUAGES = listOf(
    AppLanguage("en", "🇬🇧", "English"),
    AppLanguage("tr", "🇹🇷", "Türkçe"),
    AppLanguage("de", "🇩🇪", "Deutsch"),
    AppLanguage("es", "🇪🇸", "Español"),
    AppLanguage("fr", "🇫🇷", "Français"),
)

/**
 * "Device language" by default: Shelfie opens in the phone's language when it has it, and in
 * English otherwise. Picking one overrides that for this app only; the activity restarts in it.
 * One field that opens a list, like any other select, instead of a wall of chips.
 */
@Composable
fun ColumnScope.LanguagePickerContent() {
    val systemLabel = stringResource(R.string.settings_language_system)
    val options = remember(systemLabel) { listOf(AppLanguage("", "📱", systemLabel)) + APP_LANGUAGES }
    var selectedTag by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')) }
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.tag == selectedTag } ?: options.first()

    Text(
        text = stringResource(R.string.settings_language_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Box {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { role = Role.DropdownList },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = selected.flag, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(12.dp))
                Text(text = selected.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(if (expanded) 270f else 90f),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    leadingIcon = { Text(option.flag, style = MaterialTheme.typography.titleMedium) },
                    trailingIcon = if (option.tag == selected.tag) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        if (option.tag == selected.tag) return@DropdownMenuItem
                        selectedTag = option.tag
                        AppCompatDelegate.setApplicationLocales(
                            if (option.tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(option.tag),
                        )
                    },
                )
            }
        }
    }
}
