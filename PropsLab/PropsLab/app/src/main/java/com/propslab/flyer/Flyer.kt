package com.propslab.flyer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.propslab.R
import java.io.File
import java.io.FileOutputStream

/** The four cards a user actually wants to post. */
enum class FlyerTemplate(
    val kicker: String,
    val headline: String,
    val accent: Int,
    val metricLabel: String,
) {
    BET_OF_DAY("BET OF THE DAY", "Today's board,\nread properly.", 0xFF58C7E6.toInt(), "MODEL CHANCE"),
    MY_BET("MY BET", "This is the\nposition.", 0xFF58C7E6.toInt(), "TO WIN"),
    WIN("SETTLED", "Ticket\ncashed.", 0xFF6FD3A0.toInt(), "PROFIT"),
    STRATEGY("NEW STRATEGY", "The model\nfound an edge.", 0xFFF0A54B.toInt(), "PROVEN ON"),
}

data class FlyerRow(val primary: String, val secondary: String, val price: String)

data class FlyerContent(
    val template: FlyerTemplate,
    val rows: List<FlyerRow>,
    val metricValue: String,
    val metricCaption: String,
    val footer: String,
    val dateLine: String,
    val watermark: Boolean,
)

/**
 * Draws the share card with Android's own canvas.
 *
 * Compose could render this too, but a bitmap needs no composition, no window and no
 * screenshot: the same pixels come out on any screen size, which is what makes the card
 * look deliberate rather than like a phone screenshot. The palette is the app's: court at
 * night, aqua for the model's side, amber for the counter-case.
 */
object FlyerRenderer {

    const val WIDTH = 1080
    const val HEIGHT = 1350
    private const val PAD = 72f

    fun render(context: Context, content: FlyerContent): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val display = runCatching { ResourcesCompat.getFont(context, R.font.bricolage) }.getOrNull()
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val bodyFont = runCatching { ResourcesCompat.getFont(context, R.font.manrope) }.getOrNull()
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // Background: court at night, with one warm light off the top-right corner.
        canvas.drawRect(
            0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, HEIGHT.toFloat(),
                    intArrayOf(0xFF121A27.toInt(), 0xFF0B1018.toInt(), 0xFF080D14.toInt()),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawCircle(
            WIDTH * 0.88f, -40f, 620f,
            Paint().apply {
                shader = RadialGradient(
                    WIDTH * 0.88f, -40f, 620f,
                    intArrayOf(withAlpha(content.template.accent, 0.22f), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawCircle(
            -120f, HEIGHT * 0.92f, 520f,
            Paint().apply {
                shader = RadialGradient(
                    -120f, HEIGHT * 0.92f, 520f,
                    intArrayOf(0x14F0A54B, Color.TRANSPARENT), null, Shader.TileMode.CLAMP,
                )
            },
        )

        // Brand row.
        mark(canvas, PAD + 26f, 108f, 26f)
        text(canvas, "PROPSLAB", PAD + 74f, 118f, 30f, 0xFFEDEAE2.toInt(), display, letterSpacing = 0.18f)
        text(canvas, content.dateLine.uppercase(), WIDTH - PAD, 116f, 22f, 0xFF98A3B5.toInt(), bodyFont, align = Paint.Align.RIGHT)

        var y = 196f
        text(canvas, content.template.kicker, PAD, y, 24f, content.template.accent, bodyFont, letterSpacing = 0.34f)
        y += 18f
        y += drawWrapped(canvas, content.template.headline, PAD, y + 46f, WIDTH - PAD * 2, 76f, 82f, 0xFFEDEAE2.toInt(), display, bold = true)

        // The ticket.
        val cardTop = y + 36f
        val cardBottom = cardTop + 88f + content.rows.size * 96f + 26f
        val card = RectF(PAD, cardTop, WIDTH - PAD, cardBottom)
        canvas.drawRoundRect(card, 30f, 30f, Paint().apply {
            color = 0xFF151F2E.toInt()
            style = Paint.Style.FILL
        })
        canvas.drawRoundRect(card, 30f, 30f, Paint().apply {
            color = 0xFF263247.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        })
        var rowY = cardTop + 60f
        content.rows.take(4).forEach { row ->
            text(canvas, row.primary, PAD + 34f, rowY, 39f, 0xFFEDEAE2.toInt(), display, bold = true)
            text(canvas, row.secondary, PAD + 34f, rowY + 34f, 23f, 0xFF98A3B5.toInt(), bodyFont)
            text(canvas, row.price, WIDTH - PAD - 34f, rowY + 4f, 38f, content.template.accent, display, align = Paint.Align.RIGHT, bold = true)
            rowY += 96f
        }

        // The number the whole card exists for.
        val metricTop = cardBottom + 74f
        text(canvas, content.template.metricLabel, PAD, metricTop, 22f, 0xFF5E6B80.toInt(), bodyFont, letterSpacing = 0.28f)
        text(canvas, content.metricValue, PAD, metricTop + 92f, 96f, 0xFFEDEAE2.toInt(), display, bold = true)
        text(canvas, content.metricCaption, PAD, metricTop + 142f, 24f, 0xFF98A3B5.toInt(), bodyFont)

        // Footer.
        val lineY = HEIGHT - 132f
        canvas.drawRect(PAD, lineY, WIDTH - PAD, lineY + 1.5f, Paint().apply { color = 0xFF263247.toInt() })
        text(canvas, content.footer, PAD, lineY + 50f, 23f, 0xFF98A3B5.toInt(), bodyFont)
        // The watermark and the address share the bottom-right slot, so they never overlap.
        if (content.watermark) {
            val pill = RectF(WIDTH - PAD - 268f, lineY + 30f, WIDTH - PAD, lineY + 70f)
            canvas.drawRoundRect(pill, 20f, 20f, Paint().apply { color = 0x1A58C7E6 })
            text(
                canvas, "MADE WITH PROPSLAB", WIDTH - PAD - 18f, lineY + 58f, 16f,
                0xFF58C7E6.toInt(), bodyFont, align = Paint.Align.RIGHT, letterSpacing = 0.18f,
            )
        } else {
            text(canvas, "propslab.app", WIDTH - PAD, lineY + 50f, 23f, 0xFFEDEAE2.toInt(), bodyFont, align = Paint.Align.RIGHT)
        }
        return bitmap
    }

    /** The same split ring the app uses, at poster scale. */
    private fun mark(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val arc = RectF(cx - r, cy - r, cx + r, cy + r)
        val stroke = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        stroke.color = 0xFF58C7E6.toInt()
        canvas.drawArc(arc, -90f, 206f, false, stroke)
        stroke.color = 0xFFF0A54B.toInt()
        canvas.drawArc(arc, 121f, 100f, false, stroke)
        stroke.color = 0xFF98A3B5.toInt()
        canvas.drawArc(arc, 226f, 44f, false, stroke)
    }

    private fun drawWrapped(
        canvas: Canvas, value: String, x: Float, top: Float, width: Float,
        size: Float, lineHeight: Float, color: Int, face: Typeface, bold: Boolean = false,
    ): Float {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = face
            textSize = size
            this.color = color
            letterSpacing = -0.01f
            if (bold) typeface = Typeface.create(face, Typeface.BOLD)
        }
        var y = top
        value.split('\n').forEach { paragraph ->
            var line = StringBuilder()
            for (word in paragraph.split(' ')) {
                val attempt = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(attempt) > width && line.isNotEmpty()) {
                    canvas.drawText(line.toString(), x, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(attempt)
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
            }
        }
        return y - top
    }

    private fun text(
        canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int,
        face: Typeface, align: Paint.Align = Paint.Align.LEFT, letterSpacing: Float = 0f,
        bold: Boolean = false,
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = if (bold) Typeface.create(face, Typeface.BOLD) else face
            textSize = size
            this.color = color
            textAlign = align
            this.letterSpacing = letterSpacing
        }
        canvas.drawText(value, x, baseline, paint)
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((255 * alpha).toInt(), Color.red(color), Color.green(color), Color.blue(color))
}

/** Shares the card, and can save it to the phone's pictures. */
object FlyerShare {

    fun share(context: Context, bitmap: Bitmap, name: String): Boolean {
        val file = write(context, bitmap, name) ?: return false
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }.getOrNull() ?: return false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Cheat Code · propslab.app")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share this card").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(chooser) }.isSuccess
    }

    fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Cheat Code")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val stream = resolver.openOutputStream(uri)
        if (stream == null) {
            resolver.delete(uri, null, null)
            return false
        }
        val written = stream.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!written) {
            resolver.delete(uri, null, null)
            return false
        }
        true
    }.getOrDefault(false)

    private fun write(context: Context, bitmap: Bitmap, name: String): File? = runCatching {
        val dir = File(context.cacheDir, "flyers").apply { mkdirs() }
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }.getOrNull()
}
