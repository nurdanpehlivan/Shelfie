package com.shelfie.app.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood

/**
 * Call to action under the summary: chef Frizzy offers to cook the critical items. Warm apricot
 * fading into mint, so it reads as an invitation rather than another alarm.
 */
@Composable
fun RescueRecipeBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(scheme.tertiaryContainer, scheme.primaryContainer)))
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Frizzy(mood = FrizzyMood.CHEF, size = 76.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_rescue_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.dashboard_rescue_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onClick) {
                    Text(stringResource(R.string.dashboard_rescue_action))
                }
            }
        }
    }
}
