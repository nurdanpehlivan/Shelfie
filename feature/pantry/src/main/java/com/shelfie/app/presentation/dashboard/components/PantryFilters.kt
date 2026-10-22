package com.shelfie.app.presentation.dashboard.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.common.shelfieFilterChipColors
import com.shelfie.app.presentation.theme.extendedColors

/** Glass pill search field; it takes focus when it appears, and × clears and closes it. */
@Composable
fun PantrySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val container = MaterialTheme.colorScheme.surfaceContainerLowest
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.dashboard_search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.dashboard_search_close))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.extendedColors.glassBorder, CircleShape)
            .focusRequester(focusRequester),
    )
}

/** "All · Fridge · Freezer · Pantry" with item counts; scrolls sideways on narrow screens. */
@Composable
fun LocationChips(
    selected: StorageLocation?,
    counts: Map<StorageLocation, Int>,
    onSelect: (StorageLocation?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.dashboard_location_all, counts.values.sum())) },
            shape = CircleShape,
            colors = shelfieFilterChipColors(),
        )
        StorageLocation.entries.forEach { place ->
            FilterChip(
                selected = selected == place,
                // Tapping the selected place again goes back to everything.
                onClick = { onSelect(if (selected == place) null else place) },
                label = { Text("${stringResource(place.labelRes)} ${counts[place] ?: 0}") },
                shape = CircleShape,
                colors = shelfieFilterChipColors(),
            )
        }
    }
}
