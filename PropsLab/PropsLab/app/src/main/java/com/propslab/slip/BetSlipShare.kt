package com.propslab.slip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * The handoff, limited to what Android and the destinations actually support.
 *
 * Gambly documents receiving a screenshot through the Android share sheet. Pikkit's
 * QuickPickBot documents pasted slip text or an image on the web. Neither offers a public
 * API to create a slip in its app, and no sportsbook offers an outside app a prefill, so
 * the book button opens the book and no more. Copy and share are the integration.
 */
object BetSlipShare {

    /** Pikkit's documented paste point. */
    const val QUICK_PICK_URL = "https://pikkit.com/quickpickbot-web"

    fun share(context: Context, slip: BetSlip, bitmap: Bitmap): Boolean {
        val intent = imageIntent(context, slip, bitmap) ?: return false
        return launchChooser(context, intent, "Share bet slip")
    }

    /** Sends directly to an installed tracker; otherwise lets the user choose. */
    fun sendToTracker(context: Context, slip: BetSlip, bitmap: Bitmap, tracker: SlipTracker): Boolean {
        val intent = when (tracker.accept) {
            SlipAccept.IMAGE -> imageIntent(context, slip, bitmap) ?: return false
            SlipAccept.TEXT -> Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, slip.plainText())
                putExtra(Intent.EXTRA_SUBJECT, "Cheat Code bet slip")
            }
        }
        if (isInstalled(context, tracker.packageId)) intent.setPackage(tracker.packageId)
        return launchChooser(context, intent, "Send slip to ${tracker.displayName}")
    }

    fun copy(context: Context, slip: BetSlip): Boolean = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Cheat Code bet slip", slip.plainText()))
        true
    }.getOrDefault(false)

    fun openBook(context: Context, book: SlipBook): Boolean = openUrl(context, book.siteUrl)

    fun openTracker(context: Context, tracker: SlipTracker): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(tracker.packageId)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { context.startActivity(launch) }.isSuccess
        }
        return openUrl(context, tracker.storeUrl)
    }

    fun openUrl(context: Context, url: String): Boolean = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)

    fun isInstalled(context: Context, packageId: String): Boolean =
        runCatching { context.packageManager.getLaunchIntentForPackage(packageId) != null }
            .getOrDefault(false)

    private fun imageIntent(context: Context, slip: BetSlip, bitmap: Bitmap): Intent? {
        val file = write(context, slip, bitmap) ?: return null
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }.getOrNull() ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, slip.plainText())
            putExtra(Intent.EXTRA_SUBJECT, "Cheat Code bet slip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun launchChooser(context: Context, intent: Intent, title: String): Boolean = runCatching {
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    }.getOrDefault(false)

    private fun write(context: Context, slip: BetSlip, bitmap: Bitmap): File? = runCatching {
        val dir = File(context.cacheDir, "slips").apply { mkdirs() }
        val file = File(dir, "${slip.fileName()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }.getOrNull()
}
