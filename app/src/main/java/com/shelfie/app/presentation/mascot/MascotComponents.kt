package com.shelfie.app.presentation.mascot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Speech bubble with a small tail on the left, pointing back at Frizzy.
 * Uses `surfaceContainerHigh` so it reads as Frizzy talking, not as another card.
 */
@Composable
fun SpeechBubble(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit,
) {
    val tail = 10.dp
    val density = LocalDensity.current
    val tailPx = with(density) { tail.toPx() }
    val radius = with(density) { 18.dp.toPx() }
    Surface(
        modifier = modifier,
        color = color,
        shape = GenericShape { size, _ ->
            addRoundRect(
                RoundRect(tailPx, 0f, size.width, size.height, CornerRadius(radius)),
            )
            // Tail near the bottom-left corner.
            moveTo(tailPx, size.height - radius - tailPx)
            lineTo(0f, size.height - radius * 0.6f)
            lineTo(tailPx + 2f, size.height - radius * 0.9f)
            close()
        },
    ) {
        Column(modifier = Modifier.padding(start = tail + 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            content()
        }
    }
}

/** Frizzy next to a speech bubble: the dashboard greeting. */
@Composable
fun FrizzySays(
    mood: FrizzyMood,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 88.dp,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Frizzy(mood = mood, size = mascotSize)
        Spacer(modifier = Modifier.width(4.dp))
        SpeechBubble(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Full-screen empty / error state: big Frizzy, a headline, a sentence and up to two actions.
 * One component for every empty state keeps them consistent across the app.
 */
@Composable
fun MascotMessage(
    mood: FrizzyMood,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Frizzy(mood = mood, size = 168.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (primaryAction != null) {
            Spacer(modifier = Modifier.height(28.dp))
            Button(onClick = primaryAction.second, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(primaryAction.first)
            }
        }
        if (secondaryAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = secondaryAction.second, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(secondaryAction.first)
            }
        }
    }
}
