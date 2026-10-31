package com.shelfie.app.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.extendedColors

/**
 * Premium, offered where it helps: when a free limit was just reached. Never on launch or as
 * a pop-up; [message] says which limit, the card adds the trial and a way to look.
 */
@Composable
fun PremiumNudge(message: String, onOpenPremium: () -> Unit, modifier: Modifier = Modifier) {
    HighlightCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Frizzy(mood = FrizzyMood.CELEBRATING, size = 64.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.premium_nudge_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extendedColors.onHeroVariant,
                    )
                    Text(
                        text = stringResource(R.string.premium_nudge_trial),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Button(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth()) {
                Text("✨ " + stringResource(R.string.premium_nudge_action))
            }
        }
    }
}
