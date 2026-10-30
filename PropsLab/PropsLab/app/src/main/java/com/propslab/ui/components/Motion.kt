package com.propslab.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Paint
import com.propslab.ui.theme.Palette

/**
 * The app's motion vocabulary, small on purpose.
 *
 * A glow marks the one thing the screen is about, a pulse means "this is live right now",
 * and a number that counts up is telling the eye that it changed. None of it is decoration
 * for its own sake: there is no animation on a row that carries no state.
 */
fun Modifier.glow(color: Color, radius: Dp = 22.dp, alpha: Float = 0.28f): Modifier = drawBehind {
    if (size.minDimension <= 0f) return@drawBehind
    val brush = Brush.radialGradient(
        0.0f to color.copy(alpha = alpha),
        0.55f to color.copy(alpha = alpha * 0.35f),
        1.0f to Color.Transparent,
        center = center,
        radius = radius.toPx().coerceAtLeast(size.minDimension * 0.72f),
    )
    drawRect(brush)
}

/** A slow breath of light behind a card that is live. */
@Composable
fun Modifier.liveGlow(color: Color): Modifier {
    val transition = rememberInfiniteTransition(label = "live")
    val strength by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    return this.drawBehind {
        val brush = Brush.radialGradient(
            0.0f to color.copy(alpha = strength * 0.5f),
            1.0f to Color.Transparent,
            center = center,
            radius = size.minDimension,
        )
        drawRect(brush)
    }
}

/** A dot that breathes, for "the feed is live" and "this number just moved". */
@Composable
fun PulseDot(color: Color, size: Dp = 7.dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier.size(size).alpha(alpha).background(color, CircleShape),
    )
}

@Composable
fun LiveBadge(
    label: String,
    color: Color = Palette.Gain,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.padding(end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseDot(color, 6.dp)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/**
 * A percentage that counts to its value. The animation is short enough not to lie about
 * the number: it resolves in the time it takes to read the label above it.
 */
@Composable
fun AnimatedPercent(
    value: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(850, easing = FastOutSlowInEasing))
    }
    Text(
        text = percent(animated.value.toDouble()),
        style = style,
        color = color,
        modifier = modifier,
    )
}

/** A currency-ish figure that counts, used for bankroll and profit. */
@Composable
fun AnimatedAmount(
    value: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(850, easing = FastOutSlowInEasing))
    }
    Text(
        text = "%.2f".format(animated.value),
        style = style,
        color = color,
        modifier = modifier,
    )
}

private fun percent(p: Double): String = when {
    p >= 0.1 -> "%.0f%%".format(p * 100)
    p >= 0.01 -> "%.1f%%".format(p * 100)
    else -> "%.2f%%".format(p * 100)
}

/**
 * A soft drop shadow, drawn by hand.
 *
 * Compose's shadow only accepts an elevation and one ambient colour; a coloured halo on a
 * dark surface reads better as a blurred paint pass. Used sparingly, behind the score card.
 */
fun Modifier.halo(color: Color, radius: Dp = 26.dp): Modifier = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color
            asFrameworkPaint().apply {
                isAntiAlias = true
                setShadowLayer(radius.toPx(), 0f, 0f, color.toArgb())
            }
        }
        canvas.nativeCanvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            radius.toPx(),
            radius.toPx(),
            paint.asFrameworkPaint(),
        )
    }
}
