package com.shelfie.app.presentation.scanner.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.shelfie.app.presentation.common.ShelfieCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Grey placeholder with a highlight sweeping across it. */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val progress = rememberLoopingProgress(durationMillis = 1300, label = "shimmer")

    Box(
        modifier = modifier
            .clip(shape)
            // The animated value is read in the draw phase only, so it never recomposes.
            .drawBehind {
                val bandWidth = size.width.coerceAtLeast(size.height)
                val x = -bandWidth + progress.value * (size.width + bandWidth * 2)
                drawRect(base)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(x, 0f),
                        end = Offset(x + bandWidth, size.height),
                    ),
                )
            },
    )
}

/** Placeholder shaped like [DetectedItemCard], shown while the photo is analysed. */
@Composable
fun DetectedItemPlaceholder(modifier: Modifier = Modifier) {
    ShelfieCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShimmerBlock(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShimmerBlock(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
            }
        }
    }
}

/** Draws a glowing line that sweeps up and down over the content, like a scanner. */
@Composable
fun Modifier.scanLineOverlay(color: Color = MaterialTheme.colorScheme.primary): Modifier {
    val progress = rememberLoopingProgress(
        durationMillis = 1800,
        repeatMode = RepeatMode.Reverse,
        label = "scanLine",
    )
    return drawWithContent {
        drawContent()
        drawRect(color.copy(alpha = 0.12f))

        val y = progress.value * size.height
        val glowHeight = size.height * 0.18f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = 0.45f)),
                startY = y - glowHeight,
                endY = y,
            ),
            topLeft = Offset(0f, y - glowHeight),
            size = Size(size.width, glowHeight),
        )
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 3.dp.toPx(),
        )
    }
}

@Composable
private fun rememberLoopingProgress(
    durationMillis: Int,
    label: String,
    repeatMode: RepeatMode = RepeatMode.Restart,
): State<Float> = rememberInfiniteTransition(label = label).animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = durationMillis, easing = LinearEasing),
        repeatMode = repeatMode,
    ),
    label = label,
)
