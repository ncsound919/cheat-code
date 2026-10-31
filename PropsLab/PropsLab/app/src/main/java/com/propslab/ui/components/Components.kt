package com.propslab.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.propslab.fantasy.FantasySport
import com.propslab.fantasy.ProTeams
import com.propslab.ui.initials
import com.propslab.ui.theme.Palette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The signature element. Over in aqua, push in a quiet grey, under in amber, and a chalk tick where the
 * no-vig market sits. The distance between the tick and the end of the aqua arc is the edge.
 */
@Composable
fun SplitRing(
    over: Float,
    push: Float,
    market: Float?,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(over) { progress.animateTo(over, tween(900, easing = FastOutSlowInEasing)) }
    val underPct = ((1f - over - push).coerceAtLeast(0f) * 100).roundToInt()
    val description = buildString {
        append("Model: ${(over * 100).roundToInt()}% over, ${(push * 100).roundToInt()}% push, $underPct% under")
        if (market != null) append(". Market no-vig over ${(market * 100).roundToInt()}%")
    }
    Box(modifier.aspectRatio(1f).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = strokeWidth.toPx()
            val d = size.minDimension - s * 2f
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arc = Size(d, d)
            val gap = 4f
            val stroke = Stroke(width = s, cap = StrokeCap.Round)
            val overSweep = 360f * progress.value
            val pushSweep = 360f * push * (progress.value / over.coerceAtLeast(0.001f)).coerceAtMost(1f)
            // Under first so the rounded caps of the other arcs sit on top.
            drawArc(Palette.Under, -90f + overSweep + pushSweep + gap / 2f, (360f - overSweep - pushSweep - gap).coerceAtLeast(0.1f), false, topLeft, arc, style = stroke)
            if (pushSweep > 1f) {
                drawArc(Palette.Faint, -90f + overSweep + gap / 2f, (pushSweep - gap / 2f).coerceAtLeast(0.1f), false, topLeft, arc, style = stroke)
            }
            drawArc(Palette.Over, -90f + gap / 2f, (overSweep - gap).coerceAtLeast(0.1f), false, topLeft, arc, style = stroke)
            if (market != null) {
                val angle = Math.toRadians(-90.0 + 360.0 * market.toDouble())
                val r = d / 2f
                val inner = Offset(center.x + (r - s * 0.9f) * cos(angle).toFloat(), center.y + (r - s * 0.9f) * sin(angle).toFloat())
                val outer = Offset(center.x + (r + s * 0.9f) * cos(angle).toFloat(), center.y + (r + s * 0.9f) * sin(angle).toFloat())
                drawLine(Palette.Chalk, inner, outer, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        content()
    }
}

/** Slate-row version of the ring: the same three segments and market tick, laid flat. */
@Composable
fun SplitBar(over: Float, push: Float, market: Float?, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val description = buildString {
        append("Model: ${(over * 100).roundToInt()}% over, ${(push * 100).roundToInt()}% push")
        if (market != null) append(". Market no-vig over ${(market * 100).roundToInt()}%")
    }
    // The bar fills from the left the first time a row appears, so a scan down the board
    // reads as a position rather than five static stripes.
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(over, push) { sweep.animateTo(over, tween(650, easing = FastOutSlowInEasing)) }
    val shown = sweep.value
    Canvas(modifier.fillMaxWidth().height(height + 10.dp).semantics { contentDescription = description }) {
        val h = height.toPx()
        val y = (size.height - h) / 2f
        val gap = 2.dp.toPx()
        val w = size.width
        val overW = (w * shown).coerceIn(0f, w)
        val pushW = (w * push).coerceIn(0f, w - overW)
        val r = CornerRadius(h / 2f, h / 2f)
        drawRoundRect(Palette.Under, Offset(overW + pushW + gap / 2f, y), Size((w - overW - pushW - gap / 2f).coerceAtLeast(1f), h), r)
        if (pushW > 0f) drawRoundRect(Palette.Faint, Offset(overW + gap / 2f, y), Size((pushW - gap / 2f).coerceAtLeast(1f), h), r)
        drawRoundRect(Palette.Over, Offset(0f, y), Size((overW - gap / 2f).coerceAtLeast(1f), h), r)
        if (market != null) {
            val x = (w * market).coerceIn(1.dp.toPx(), w - 1.dp.toPx())
            drawLine(Palette.Chalk, Offset(x, y - 3.dp.toPx()), Offset(x, y + h + 3.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/**
 * The club's own mark, from the directory bundled with the build. A code that is not a
 * known club falls back to the initials tile, so a row never shows a broken image.
 */
@Composable
fun TeamMark(
    teamCode: String,
    sport: FantasySport = FantasySport.BASKETBALL,
    size: Dp = 30.dp,
) {
    val code = ProTeams.code(teamCode, sport) ?: teamCode.trim().uppercase()
    val url = ProTeams.logoUrl(code, sport)
    if (url.isBlank()) {
        TeamAvatar(code.ifBlank { "?" }, code, size)
        return
    }
    val shape = CircleShape
    RemoteImage(
        url = url,
        contentDescription = "$code club mark",
        modifier = Modifier.size(size).clip(shape).background(Palette.Raised)
            .border(1.dp, Palette.Hairline, shape),
        contentScale = ContentScale.Fit,
        fallback = {
            Text(code.take(3), style = MaterialTheme.typography.labelSmall, color = Palette.Dust)
        },
    )
}

@Composable
fun TeamAvatar(name: String, teamCode: String, size: Dp = 42.dp) {
    val hue = (abs(teamCode.hashCode()) % 360).toFloat()
    val top = Color.hsl(hue, 0.42f, 0.32f)
    val bottom = Color.hsl((hue + 28f) % 360f, 0.5f, 0.17f)
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(top, bottom)))
            .border(1.dp, Color.hsl(hue, 0.5f, 0.48f).copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), style = MaterialTheme.typography.labelLarge, color = Palette.Chalk)
    }
}

/** Honest labelling for invented data. Shown wherever fixtures are on screen. */
@Composable
fun FixtureNotice(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(Palette.Caution, CircleShape))
        Column(Modifier.padding(start = 12.dp)) {
            Text("Synthetic fixtures", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
            Text(
                "Names, lines, odds and probabilities here are invented test data. They are not real quotes or predictions. " +
                    "The football slate uses real clubs and real players so the marks and markets can be checked; its lines and chances are invented too.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        }
    }
}

/**
 * A compact, selectable control. The visual height is 40dp but the touch target
 * is 48dp, matching the platform minimum.
 *
 * A disabled chip is visibly darker and cannot be tapped. That is used where a choice
 * exists on the board but cannot satisfy this particular card — a formula that cannot
 * fill a 5-leg ticket, for example — so the option is shown, not hidden, and its refusal
 * is part of the screen rather than a mystery.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(
                when {
                    !enabled -> Palette.Ink
                    selected -> Palette.Raised
                    else -> Color.Transparent
                },
                shape,
            )
            .border(
                1.dp,
                when {
                    !enabled -> Palette.Hairline.copy(alpha = 0.6f)
                    selected -> Palette.Chalk.copy(alpha = 0.55f)
                    else -> Palette.Hairline
                },
                shape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                if (!enabled) disabled()
            }
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                !enabled -> Palette.Faint
                selected -> Palette.Chalk
                else -> Palette.Dust
            },
        )
    }
}

/** A bordered surface. Every card on the new screens is one of these, so the rhythm matches. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    tone: Color = Palette.Court,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(tone).border(1.dp, Palette.Hairline, shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.clip(shape)
            .background(if (filled) color.copy(alpha = 0.16f) else Color.Transparent, shape)
            .border(1.dp, color.copy(alpha = if (filled) 0.35f else 0.55f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/** Label, large figure, optional caption. Used for the numbers a screen is about. */
@Composable
fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    tone: Color = Palette.Chalk,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.Faint, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = tone, maxLines = 1)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = Palette.Dust, maxLines = 2)
        }
    }
}

/** One button style, so every action on the new screens reads the same. */
@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: Color = Palette.Chalk,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.clip(shape).background(tone.copy(alpha = if (enabled) 0.14f else 0.05f), shape)
            .border(1.dp, tone.copy(alpha = if (enabled) 0.5f else 0.2f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) tone else Palette.Faint,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
}

/**
 * The way into the glossary. It is a word rather than a question-mark icon because the
 * word is what a first-time reader is looking for, and it sits in each header so the
 * answer is one tap away at the exact moment a term stops making sense.
 */
@Composable
fun PlainLink(onOpen: () -> Unit, modifier: Modifier = Modifier, label: String = "Plain English") {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, Palette.Hairline, shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Palette.Dust,
            textAlign = TextAlign.Center,
        )
    }
}

fun bookLabel(book: String): String = if (book.isBlank()) "Best price" else book

/**
 * The one control that reveals the arithmetic in easy mode. The maths is always there;
 * this only decides whether it is the first thing a new reader meets.
 */
@Composable
fun WorkToggle(
    showing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Show the work",
) {
    Text(
        if (showing) "Hide the work" else label,
        modifier.clickable(role = Role.Button, onClick = onToggle).padding(vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Palette.Over,
    )
}

/**
 * Sportsbooks are recognisable before you read them, so each one gets its own mark.
 * Shared so a slip on the ledger and a slip to place carry the same mark.
 */
@Composable
fun BookMark(book: String, size: Dp = 26.dp) {
    val name = bookLabel(book)
    val hue = (abs(name.hashCode()) % 360).toFloat()
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Color.hsl(hue, 0.45f, 0.26f))
            .border(1.dp, Color.hsl(hue, 0.55f, 0.45f).copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(),
            style = MaterialTheme.typography.labelSmall, color = Palette.Chalk,
        )
    }
}

@Composable
fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(0.4f), style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
        Text(value, Modifier.weight(0.6f), style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
    }
}
