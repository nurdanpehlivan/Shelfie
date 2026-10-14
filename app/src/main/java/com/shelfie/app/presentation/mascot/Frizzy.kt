package com.shelfie.app.presentation.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Frizzy's moods; each screen picks the one that matches its message. */
enum class FrizzyMood {
    /** Default, all is well. */
    HAPPY,

    /** Something is about to spoil, or an error. */
    WORRIED,

    /** Nothing to show yet: an empty pantry, list or history. */
    SLEEPY,

    /** Cooking: recipe loading, cooking mode. */
    CHEF,

    /** A dish finished, a product saved. */
    CELEBRATING,
}

/** Fixed mascot colours: Frizzy looks the same in light and dark mode, like a logo. */
private object FrizzyColors {
    val body = Color(0xFFEAF7EF)
    val bodyShade = Color(0xFFD3EEDD)
    val outline = Color(0xFF1F5C43)
    val handle = Color(0xFF8FD8B0)
    val eye = Color(0xFF1C2B22)
    val cheek = Color(0xFFFF9E9E)
    val mouth = Color(0xFF1C2B22)
    val tongue = Color(0xFFFF7A7A)
    val leaf = Color(0xFF43A36F)
    val hat = Color.White
    val sparkle = Color(0xFFF2A65A)
}

/**
 * Shelfie's mascot, a small friendly fridge, drawn in code so it scales crisply and needs no
 * image assets. Idles with a gentle float and blinks; [animated] = false gives a still pose
 * (previews, notifications-style surfaces).
 */
@Composable
fun Frizzy(
    mood: FrizzyMood,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    animated: Boolean = true,
    contentDescription: String? = null,
) {
    val float = if (animated) rememberLoop(durationMillis = 2600, label = "float") else remember { mutableFloatStateOf(0f) }
    val blink = if (animated) rememberBlink() else remember { mutableFloatStateOf(1f) }

    Canvas(
        modifier = modifier
            .size(size)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        val u = this.size.minDimension / 100f
        val phase = float.value * 2 * PI.toFloat()
        val (dx, dy) = when (mood) {
            // A nervous little shiver.
            FrizzyMood.WORRIED -> sin(phase * 6) * 0.8f * u to sin(phase) * 1.5f * u
            // Happy hops.
            FrizzyMood.CELEBRATING -> 0f to -kotlin.math.abs(sin(phase * 2)) * 5f * u
            // Slow breathing.
            FrizzyMood.SLEEPY -> 0f to sin(phase) * 1f * u
            else -> 0f to sin(phase) * 2.5f * u
        }
        val offsetX = (this.size.width - 100f * u) / 2
        translate(left = offsetX + dx, top = dy) {
            drawFrizzy(u, mood, eyeOpen = if (mood == FrizzyMood.SLEEPY) 0f else blink.value, phase = phase)
        }
    }
}

private fun DrawScope.drawFrizzy(u: Float, mood: FrizzyMood, eyeOpen: Float, phase: Float) {
    val stroke = Stroke(width = 3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Feet.
    listOf(30f, 60f).forEach { x ->
        drawRoundRect(FrizzyColors.outline, Offset(x * u, 86f * u), Size(10f * u, 7f * u), CornerRadius(3f * u))
    }

    // Body with a soft side shade, then the outline.
    val body = Rect(18f * u, 14f * u, 82f * u, 90f * u)
    val radius = CornerRadius(16f * u)
    drawRoundRect(FrizzyColors.body, body.topLeft, body.size, radius)
    drawRoundRect(
        FrizzyColors.bodyShade,
        Offset(body.right - 12f * u, body.top + 4f * u),
        Size(8f * u, body.height - 8f * u),
        CornerRadius(4f * u),
    )
    drawRoundRect(FrizzyColors.outline, body.topLeft, body.size, radius, style = stroke)

    // Freezer door split and handles.
    drawLine(FrizzyColors.outline, Offset(body.left, 40f * u), Offset(body.right, 40f * u), 3f * u, StrokeCap.Round)
    drawRoundRect(FrizzyColors.handle, Offset(70f * u, 22f * u), Size(4.5f * u, 12f * u), CornerRadius(2.5f * u))
    drawRoundRect(FrizzyColors.handle, Offset(70f * u, 47f * u), Size(4.5f * u, 16f * u), CornerRadius(2.5f * u))

    // Leaf magnet on the freezer door: the "fresh" in Shelfie.
    drawLeaf(center = Offset(32f * u, 27f * u), u = u)

    drawFace(u, mood, eyeOpen)

    when (mood) {
        FrizzyMood.CHEF -> drawChefHat(u)
        FrizzyMood.SLEEPY -> drawZzz(u, phase)
        FrizzyMood.CELEBRATING -> drawSparkles(u, phase)
        FrizzyMood.WORRIED -> drawSweatDrop(u)
        FrizzyMood.HAPPY -> Unit
    }
}

private fun DrawScope.drawFace(u: Float, mood: FrizzyMood, eyeOpen: Float) {
    val leftEye = Offset(40f * u, 58f * u)
    val rightEye = Offset(58f * u, 58f * u)

    if (mood == FrizzyMood.SLEEPY || eyeOpen < 0.15f) {
        // Closed eyes: little downward arcs.
        listOf(leftEye, rightEye).forEach { c ->
            val path = Path().apply {
                moveTo(c.x - 4f * u, c.y)
                quadraticTo(c.x, c.y + 3.5f * u, c.x + 4f * u, c.y)
            }
            drawPath(path, FrizzyColors.eye, style = Stroke(2.6f * u, cap = StrokeCap.Round))
        }
    } else {
        listOf(leftEye, rightEye).forEach { c ->
            val h = 9.5f * u * eyeOpen
            drawOval(FrizzyColors.eye, Offset(c.x - 4.2f * u, c.y - h / 2), Size(8.4f * u, h))
            // Sparkle highlight.
            drawCircle(Color.White, radius = 1.5f * u, center = Offset(c.x + 1.4f * u, c.y - h / 4))
        }
    }

    if (mood == FrizzyMood.WORRIED) {
        // Eyebrows tilted up towards the middle.
        drawLine(FrizzyColors.eye, Offset(35f * u, 50f * u), Offset(43f * u, 48f * u), 2.4f * u, StrokeCap.Round)
        drawLine(FrizzyColors.eye, Offset(55f * u, 48f * u), Offset(63f * u, 50f * u), 2.4f * u, StrokeCap.Round)
    }

    // Cheeks.
    drawOval(FrizzyColors.cheek.copy(alpha = 0.55f), Offset(29f * u, 63f * u), Size(7f * u, 4.5f * u))
    drawOval(FrizzyColors.cheek.copy(alpha = 0.55f), Offset(62f * u, 63f * u), Size(7f * u, 4.5f * u))

    val mouthStroke = Stroke(2.6f * u, cap = StrokeCap.Round)
    when (mood) {
        FrizzyMood.HAPPY, FrizzyMood.CHEF -> drawPath(
            Path().apply {
                moveTo(43f * u, 67f * u)
                quadraticTo(49f * u, 73f * u, 55f * u, 67f * u)
            },
            FrizzyColors.mouth,
            style = mouthStroke,
        )
        FrizzyMood.CELEBRATING -> {
            // Big open "D" smile with a tongue.
            val mouth = Path().apply {
                moveTo(41f * u, 66f * u)
                lineTo(57f * u, 66f * u)
                quadraticTo(57f * u, 76f * u, 49f * u, 76f * u)
                quadraticTo(41f * u, 76f * u, 41f * u, 66f * u)
                close()
            }
            drawPath(mouth, FrizzyColors.mouth)
            drawOval(FrizzyColors.tongue, Offset(45f * u, 71f * u), Size(8f * u, 4.5f * u))
        }
        FrizzyMood.WORRIED -> drawPath(
            Path().apply {
                moveTo(43f * u, 71f * u)
                quadraticTo(46f * u, 68f * u, 49f * u, 70.5f * u)
                quadraticTo(52f * u, 73f * u, 55f * u, 70f * u)
            },
            FrizzyColors.mouth,
            style = mouthStroke,
        )
        FrizzyMood.SLEEPY -> drawCircle(
            FrizzyColors.mouth,
            radius = 2.2f * u,
            center = Offset(49f * u, 70f * u),
            style = Stroke(2.2f * u),
        )
    }
}

private fun DrawScope.drawLeaf(center: Offset, u: Float) {
    val leaf = Path().apply {
        moveTo(center.x - 6f * u, center.y + 4f * u)
        cubicTo(center.x - 6f * u, center.y - 6f * u, center.x + 4f * u, center.y - 8f * u, center.x + 7f * u, center.y - 6f * u)
        cubicTo(center.x + 7f * u, center.y + 2f * u, center.x + 1f * u, center.y + 7f * u, center.x - 6f * u, center.y + 4f * u)
        close()
    }
    drawPath(leaf, FrizzyColors.leaf)
    drawLine(
        Color.White.copy(alpha = 0.7f),
        Offset(center.x - 4f * u, center.y + 2.5f * u),
        Offset(center.x + 4f * u, center.y - 4f * u),
        1.2f * u,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawChefHat(u: Float) {
    val outline = Stroke(2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val puffs = listOf(Offset(38f * u, 6f * u) to 8f * u, Offset(50f * u, 2f * u) to 9.5f * u, Offset(62f * u, 6f * u) to 8f * u)
    puffs.forEach { (c, r) -> drawCircle(FrizzyColors.outline, r + 1.2f * u, c) }
    puffs.forEach { (c, r) -> drawCircle(FrizzyColors.hat, r, c) }
    drawRoundRect(FrizzyColors.hat, Offset(34f * u, 6f * u), Size(32f * u, 10f * u), CornerRadius(2f * u))
    drawRoundRect(FrizzyColors.outline, Offset(34f * u, 6f * u), Size(32f * u, 10f * u), CornerRadius(2f * u), style = outline)
}

private fun DrawScope.drawZzz(u: Float, phase: Float) {
    val rise = (phase / (2 * PI.toFloat())) * 6f * u
    listOf(Triple(84f, 26f, 6f), Triple(90f, 16f, 4.5f)).forEach { (x, y, s) ->
        val top = y * u - rise
        val path = Path().apply {
            moveTo(x * u, top)
            lineTo(x * u + s * u, top)
            lineTo(x * u, top + s * u)
            lineTo(x * u + s * u, top + s * u)
        }
        drawPath(path, FrizzyColors.outline.copy(alpha = 0.8f), style = Stroke(1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawSparkles(u: Float, phase: Float) {
    val twinkle = 0.75f + 0.25f * sin(phase * 3)
    listOf(Offset(10f * u, 22f * u) to 5f, Offset(90f * u, 18f * u) to 4f, Offset(92f * u, 56f * u) to 3.5f, Offset(8f * u, 60f * u) to 3f)
        .forEach { (c, s) -> drawStar(c, s * u * twinkle, FrizzyColors.sparkle) }
}

/** Four-pointed sparkle. */
private fun DrawScope.drawStar(center: Offset, r: Float, color: Color) {
    val inner = r * 0.3f
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + r, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + r)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - r, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSweatDrop(u: Float) {
    val drop = Path().apply {
        moveTo(76f * u, 44f * u)
        quadraticTo(80f * u, 50f * u, 78f * u, 53f * u)
        quadraticTo(74f * u, 55f * u, 73f * u, 51f * u)
        quadraticTo(73f * u, 48f * u, 76f * u, 44f * u)
        close()
    }
    drawPath(drop, Color(0xFF8EC9F5))
}

@Composable
private fun rememberLoop(durationMillis: Int, label: String): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = label,
    )

/** 1 = eyes open; dips to 0 for a quick blink about every four seconds. */
@Composable
private fun rememberBlink(): State<Float> =
    rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                1f at 0
                1f at 3800
                0.05f at 3900 using FastOutSlowInEasing
                1f at 4050
            },
        ),
        label = "blink",
    )
