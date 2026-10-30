package com.propslab.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.propslab.brand.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** A published build, as the site's release feed describes it. */
data class ReleaseInfo(
    val tag: String,
    val name: String,
    val notes: String,
    val pageUrl: String,
    val published: String,
    val assetName: String?,
    val assetUrl: String?,
    val assetBytes: Long,
    /** SHA-256 of the APK, when the feed publishes one. */
    val sha256: String = "",
) {
    val hasApk: Boolean get() = assetUrl != null
}

/** The result of a check: either a release, or a plain sentence explaining why not. */
sealed interface UpdateCheck {
    data class Available(val release: ReleaseInfo, val currentVersion: String) : UpdateCheck
    data class Current(val release: ReleaseInfo?, val currentVersion: String) : UpdateCheck
    data class Failed(val reason: String) : UpdateCheck
}

/**
 * One-button updates, straight from the site that sells the app.
 *
 * The build is distributed from its own site rather than a store, so the release feed lives
 * there too: `latest.json` names the version, the APK, its size and its SHA-256. The app
 * compares that version with its own, and — only when the user taps install — downloads the
 * APK, checks the hash, and hands it to Android's package installer. Android still shows its
 * own confirmation; an app can never install silently, and this one does not try.
 *
 * A GitHub release feed is still understood, so a fork that publishes there only has to
 * point [Brand.UPDATE_FEED] at its API URL.
 */
object Updater {

    suspend fun check(context: Context): UpdateCheck = withContext(Dispatchers.IO) {
        val current = versionName(context)
        val body = runCatching { read(Brand.UPDATE_FEED) }.getOrNull()
            ?: return@withContext UpdateCheck.Failed(
                "Could not reach ${Brand.SITE}. Check the connection, or open the download page in a browser.",
            )
        val json = runCatching { JSONObject(body) }.getOrNull()
            ?: return@withContext UpdateCheck.Failed("The release feed did not answer with a build.")

        val release = parseFeed(json) ?: return@withContext UpdateCheck.Failed(
            if (json.has("message")) {
                "No release is published yet (${json.optString("message")})."
            } else {
                "The release feed has no build in it."
            },
        )
        if (isNewer(release.tag, current)) UpdateCheck.Available(release, current)
        else UpdateCheck.Current(release, current)
    }

    /** Reads either our own `latest.json` or a GitHub release object. */
    private fun parseFeed(json: JSONObject): ReleaseInfo? {
        val version = json.optString("version")
        if (version.isNotBlank()) {
            val apk = json.optString("apk").ifBlank { "cheat-code.apk" }
            val url = if (apk.startsWith("http")) apk else Brand.SITE + "/" + apk.trimStart('/')
            return ReleaseInfo(
                tag = version,
                name = json.optString("name").ifBlank { "${Brand.APP} $version" },
                notes = json.optString("notes").take(4_000),
                pageUrl = json.optString("page").ifBlank { Brand.DOWNLOAD },
                published = json.optString("published").take(10),
                assetName = url.substringAfterLast('/'),
                assetUrl = url,
                assetBytes = json.optLong("size", 0L),
                sha256 = json.optString("sha256").lowercase(),
            )
        }

        val tag = json.optString("tag_name")
        if (tag.isBlank()) return null
        var name: String? = null
        var url: String? = null
        var bytes = 0L
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val fileName = asset.optString("name")
                if (!fileName.endsWith(".apk", ignoreCase = true)) continue
                name = fileName
                bytes = asset.optLong("size", 0L)
                url = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                break
            }
        }
        return ReleaseInfo(
            tag = tag,
            name = json.optString("name").ifBlank { tag },
            notes = json.optString("body").take(4_000),
            pageUrl = json.optString("html_url").ifBlank { Brand.DOWNLOAD },
            published = json.optString("published_at").take(10),
            assetName = name,
            assetUrl = url,
            assetBytes = bytes,
        )
    }

    /**
     * Downloads the release APK into the cache, reporting bytes as they arrive.
     *
     * A `.part` file is kept between attempts and the next attempt asks for the rest with a
     * Range request, so a metered connection that drops half way does not start again. When
     * the feed publishes a SHA-256 the finished file is checked against it, and a mismatch is
     * deleted rather than handed to the installer.
     */
    suspend fun download(
        context: Context,
        release: ReleaseInfo,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val url = release.assetUrl
            ?: return@withContext Result.failure(IllegalStateException("This release has no APK attached."))
        if (!url.startsWith("https://")) {
            return@withContext Result.failure(IllegalStateException("The update link is not HTTPS, so it was refused."))
        }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val name = release.assetName ?: "cheat-code-${release.tag}.apk"
        val target = File(dir, name)
        if (target.isFile && target.length() > 0L) return@withContext Result.success(target)
        val part = File(dir, "$name.part")
        var start = if (part.isFile) part.length() else 0L
        if (release.assetBytes > 0L && start > release.assetBytes) {
            part.delete()
            start = 0L
        }

        val connection = runCatching {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "CheatCode-Android")
                if (start > 0L) setRequestProperty("Range", "bytes=$start-")
            }
        }.getOrElse { return@withContext Result.failure(it) }

        try {
            val code = connection.responseCode
            if (code !in 200..299 && code != 416) {
                return@withContext Result.failure(IllegalStateException("${Brand.SITE} answered HTTP $code"))
            }
            val appending = code == 206 && start > 0L
            if (!appending) {
                start = 0L
                part.delete()
            }
            var copied = start
            val total = connection.contentLengthLong.takeIf { it > 0L }?.plus(start)
                ?: release.assetBytes
            if (code != 416) {
                connection.inputStream.use { input ->
                    FileOutputStream(part, appending).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress(copied, total)
                        }
                    }
                }
            }
            if (!part.isFile || part.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("The download was empty"))
            }
            if (release.assetBytes > 0L && part.length() != release.assetBytes) {
                return@withContext Result.failure(
                    IllegalStateException("The download stopped at ${part.length()} of ${release.assetBytes} bytes. Tap install again to resume."),
                )
            }
            if (release.sha256.isNotBlank()) {
                val actual = runCatching { com.propslab.hub.ModelHub.sha256(part) }.getOrNull()
                if (actual == null || !actual.equals(release.sha256, ignoreCase = true)) {
                    part.delete()
                    return@withContext Result.failure(
                        IllegalStateException("The update did not match its published checksum, so it was discarded."),
                    )
                }
            }
            if (target.exists()) target.delete()
            if (!part.renameTo(target)) {
                return@withContext Result.failure(IllegalStateException("Could not store the update"))
            }
            Result.success(target)
        } catch (t: Throwable) {
            if (!part.isFile || part.length() == 0L) part.delete()
            Result.failure(t)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    /** Hands the APK to Android's installer. The system confirms; this app cannot silent-install. */
    fun install(context: Context, file: File): Boolean {
        val uri: Uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }.getOrElse { return false }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun versionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "0"

    /**
     * Compares a release tag with a version name. `v1.2.0`, `1.2.0` and `release-1.2` all
     * work; a tag with no digits is not treated as newer, because the numbers are the only
     * thing this can honestly compare.
     */
    fun isNewer(tag: String, current: String): Boolean {
        val a = numbers(tag)
        val b = numbers(current)
        if (a.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun numbers(text: String): List<Int> =
        Regex("\\d+").findAll(text).mapNotNull { it.value.toIntOrNull() }.take(4).toList()

    private fun read(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CheatCode-Android")
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
