package com.propslab.slip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.propslab.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * The slip as a shareable image.
 *
 * It is drawn with Android's canvas rather than captured from the screen, so every phone
 * produces the same 1080-pixel-wide ticket. Club marks are fetched once when the sheet
 * prepares the image; if one cannot be read, the club code in a coloured disc is drawn
 * instead of an empty hole.
 */
object BetSlipImage {

    const val WIDTH = 1080
    const val HEIGHT = 1350
    private const val PAD = 72f
    private const val ROW = 76f

    private const val INK = 0xFF0B1018.toInt()
    private const val CARD = 0xFF151F2E.toInt()
    private const val HAIRLINE = 0xFF263247.toInt()
    private const val CHALK = 0xFFEDEAE2.toInt()
    private const val DUST = 0xFF98A3B5.toInt()
    private const val FAINT = 0xFF5E6B80.toInt()
    private const val AQUA = 0xFF58C7E6.toInt()
    private const val AMBER = 0xFFF0A54B.toInt()

    fun render(context: Context, slip: BetSlip, logos: Map<String, Bitmap> = emptyMap()): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val display = runCatching { ResourcesCompat.getFont(context, R.font.bricolage) }.getOrNull()
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val body = runCatching { ResourcesCompat.getFont(context, R.font.manrope) }.getOrNull()
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        drawBackground(canvas)

        // Brand and date.
        mark(canvas, PAD + 24f, 94f, 24f)
        text(canvas, "CHEAT CODE", PAD + 68f, 104f, 28f, CHALK, display, letterSpacing = 0.16f, bold = true)
        text(canvas, slip.dateLine.uppercase(), WIDTH - PAD, 102f, 20f, DUST, body, align = Paint.Align.RIGHT)

        text(canvas, "BET SLIP", PAD, 180f, 22f, AQUA, body, letterSpacing = 0.34f)
        val headline = if (slip.label.contains("same-game")) "SAME-GAME PARLAY" else "${slip.legs.size}-LEG PARLAY"
        text(canvas, headline, PAD, 250f, 58f, CHALK, display, bold = true, letterSpacing = -0.01f)

        val bookLineY = 290f
        text(canvas, "${slip.book.displayName}  ·  ${slip.legs.size} legs  ·  ${"%.2f".format(slip.decimal)}x", PAD, bookLineY, 24f, DUST, body)

        // The ticket itself.
        val cardTop = bookLineY + 28f
        val cardBottom = cardTop + 24f + slip.legs.size * ROW
        val card = RectF(PAD, cardTop, WIDTH - PAD, cardBottom)
        canvas.drawRoundRect(card, 28f, 28f, Paint().apply { color = CARD; style = Paint.Style.FILL })
        canvas.drawRoundRect(card, 28f, 28f, Paint().apply {
            color = HAIRLINE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        })

        slip.legs.forEachIndexed { index, leg ->
            val center = cardTop + 24f + index * ROW + ROW / 2f
            val logo = logos[leg.teamCode] ?: logos[leg.teamLogoUrl]
            if (logo != null) {
                drawCircleBitmap(canvas, logo, 112f, center - 30f, 60f)
            } else {
                drawCodeDisc(canvas, leg.teamCode, 112f, center - 30f, 60f, body)
            }
            text(
                canvas,
                fit(leg.player, display, 32f, 540f),
                PAD + 112f, center - 3f, 32f, CHALK, display, bold = true,
            )
            text(
                canvas,
                fit(leg.pick, body, 23f, 570f),
                PAD + 112f, center + 27f, 23f, DUST, body,
            )
            text(canvas, price(leg.odds), WIDTH - PAD - 20f, center - 2f, 30f, AQUA, display,
                align = Paint.Align.RIGHT, bold = true)
            text(
                canvas,
                "${"%.0f".format(leg.modelChance * 100)}% model",
                WIDTH - PAD - 20f, center + 28f, 20f, FAINT, body,
                align = Paint.Align.RIGHT,
            )
            if (index < slip.legs.lastIndex) {
                canvas.drawRect(
                    PAD + 20f, center + ROW / 2f, WIDTH - PAD - 20f, center + ROW / 2f + 1f,
                    Paint().apply { color = HAIRLINE },
                )
            }
        }

        // The money, in three columns.
        val metricsTop = cardBottom + 58f
        metric(canvas, "STAKE", "${"%.2f".format(slip.stake)} u", PAD, metricsTop, display, body)
        metric(canvas, "RETURNS", "${"%.2f".format(slip.potentialReturn)} u", PAD + 250f, metricsTop, display, body)
        metric(canvas, "MODEL CHANCE", "${"%.0f".format(slip.probability * 100)}%", PAD + 520f, metricsTop, display, body)

        // Footer.
        canvas.drawRect(PAD, HEIGHT - 122f, WIDTH - PAD, HEIGHT - 120f, Paint().apply { color = HAIRLINE })
        text(
            canvas,
            "Paper analysis, not a placed bet. Prices move. 18+ · 1-800-522-4700",
            PAD, HEIGHT - 78f, 20f, DUST, body,
        )
        text(canvas, "propslab.app", WIDTH - PAD, HEIGHT - 78f, 20f, CHALK, body, align = Paint.Align.RIGHT)
        return bitmap
    }

    /** Fetches the club marks once, with a hard timeout. A failure is simply no mark. */
    suspend fun loadLogos(legs: List<SlipLeg>): Map<String, Bitmap> = withContext(Dispatchers.IO) {
        legs.filter { it.teamLogoUrl.isNotBlank() }
            .distinctBy { it.teamLogoUrl }
            .mapNotNull { leg -> fetch(leg.teamLogoUrl)?.let { leg.teamCode to it } }
            .toMap()
    }

    private fun fetch(url: String): Bitmap? = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Cheat Code/1.1 (Android)")
            setRequestProperty("Accept", "image/*")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(
            0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, HEIGHT.toFloat(),
                    intArrayOf(0xFF121A27.toInt(), INK, 0xFF080D14.toInt()),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawCircle(
            WIDTH * 0.9f, -60f, 560f,
            Paint().apply {
                shader = RadialGradient(
                    WIDTH * 0.9f, -60f, 560f,
                    intArrayOf(withAlpha(AQUA, 0.16f), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawRect(0f, 0f, 700f, 8f, Paint().apply { color = AQUA })
        canvas.drawRect(700f, 0f, WIDTH.toFloat(), 8f, Paint().apply { color = AMBER })
    }

    private fun metric(
        canvas: Canvas, label: String, value: String, x: Float, top: Float,
        display: Typeface, body: Typeface,
    ) {
        text(canvas, label, x, top, 19f, FAINT, body, letterSpacing = 0.22f)
        text(canvas, value, x, top + 48f, 38f, CHALK, display, bold = true)
    }

    private fun drawCodeDisc(canvas: Canvas, code: String, x: Float, y: Float, size: Float, body: Typeface) {
        val hue = (abs(code.hashCode()) % 360).toFloat()
        val color = Color.HSVToColor(floatArrayOf(hue, 0.42f, 0.34f))
        canvas.drawCircle(x + size / 2f, y + size / 2f, size / 2f, Paint().apply { this.color = color })
        text(
            canvas, code.take(3).uppercase(), x + size / 2f, y + size / 2f + 7f, 18f,
            CHALK, body, align = Paint.Align.CENTER, bold = true,
        )
    }

    private fun drawCircleBitmap(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, size: Float) {
        canvas.save()
        canvas.clipPath(Path().apply { addCircle(x + size / 2f, y + size / 2f, size / 2f, Path.Direction.CW) })
        val side = minOf(bitmap.width, bitmap.height)
        val src = Rect(
            (bitmap.width - side) / 2, (bitmap.height - side) / 2,
            (bitmap.width + side) / 2, (bitmap.height + side) / 2,
        )
        canvas.drawBitmap(bitmap, src, RectF(x, y, x + size, y + size), Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        })
        canvas.restore()
    }

    private fun fit(value: String, face: Typeface, size: Float, maxWidth: Float): String {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(face, Typeface.BOLD)
            textSize = size
        }
        if (paint.measureText(value) <= maxWidth) return value
        val ellipsis = "…"
        val budget = maxWidth - paint.measureText(ellipsis)
        val count = paint.breakText(value, true, budget, null)
        return value.take(count).trimEnd() + ellipsis
    }

    private fun price(odds: Int): String = if (odds > 0) "+$odds" else "$odds"

    private fun text(
        canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int,
        face: Typeface, align: Paint.Align = Paint.Align.LEFT, letterSpacing: Float = 0f,
        bold: Boolean = false,
    ) {
        canvas.drawText(value, x, baseline, Paint().apply {
            isAntiAlias = true
            typeface = if (bold) Typeface.create(face, Typeface.BOLD) else face
            textSize = size
            this.color = color
            textAlign = align
            this.letterSpacing = letterSpacing
        })
    }

    private fun mark(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val arc = RectF(cx - r, cy - r, cx + r, cy + r)
        val stroke = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        stroke.color = AQUA
        canvas.drawArc(arc, -90f, 206f, false, stroke)
        stroke.color = AMBER
        canvas.drawArc(arc, 121f, 100f, false, stroke)
        stroke.color = DUST
        canvas.drawArc(arc, 226f, 44f, false, stroke)
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((255 * alpha).toInt(), Color.red(color), Color.green(color), Color.blue(color))
}
