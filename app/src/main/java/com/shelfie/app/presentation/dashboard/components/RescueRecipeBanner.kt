package com.shelfie.app.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shelfie.app.R
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood

/**
 * Call to action under the summary: chef Frizzy offers to cook the critical items. It uses the
 * aubergine accent, which no freshness status shares, so it reads as an invitation rather than
 * another alarm.
 */
@Composable
fun RescueRecipeBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    ShelfieCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        containerColor = scheme.tertiaryContainer,
        border = BorderStroke(1.dp, scheme.tertiary.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    color = scheme.onTertiaryContainer.copy(alpha = 0.8f),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.tertiary,
                        contentColor = scheme.onTertiary,
                    ),
                ) {
                    Text(stringResource(R.string.dashboard_rescue_action))
                }
            }
        }
    }
}
