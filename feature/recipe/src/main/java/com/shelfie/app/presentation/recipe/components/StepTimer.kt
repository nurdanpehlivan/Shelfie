package com.shelfie.app.presentation.recipe.components

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import kotlinx.coroutines.delay

/**
 * The step's waiting time as a timer: tap to start, tap again to pause, and it beeps and
 * buzzes when the time is up. It runs while the cooking screen is open (the screen stays on),
 * which is where the cook is anyway.
 */
@Composable
fun StepTimer(minutes: Int, stepIndex: Int, modifier: Modifier = Modifier) {
    val total = minutes * SECONDS_PER_MINUTE
    var remaining by rememberSaveable(stepIndex) { mutableIntStateOf(total) }
    var running by rememberSaveable(stepIndex) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(running, stepIndex) {
        while (running && remaining > 0) {
            delay(1_000)
            remaining--
        }
        if (running && remaining == 0) {
            running = false
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            beep()
        }
    }

    val finished = remaining == 0
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = {
            if (finished) {
                remaining = total
            } else {
                running = !running
            }
        },
        shape = CircleShape,
        color = if (running || finished) scheme.tertiary else scheme.tertiaryContainer,
        contentColor = if (running || finished) scheme.onTertiary else scheme.onTertiaryContainer,
        modifier = modifier,
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (running) "⏸" else "⏲", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Text(
                text = when {
                    finished -> stringResource(R.string.recipe_timer_done)
                    running || remaining < total -> "%d:%02d".format(remaining / SECONDS_PER_MINUTE, remaining % SECONDS_PER_MINUTE)
                    else -> pluralStringResource(R.plurals.recipe_timer_start, minutes, minutes)
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** A short alarm on the alarm stream, so it is heard even with media muted. */
private fun beep() {
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_MILLIS)
        // Released after it played; a ToneGenerator holds a native audio track.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, BEEP_MILLIS + 200L)
    }
}

private const val SECONDS_PER_MINUTE = 60
private const val BEEP_MILLIS = 1_500
