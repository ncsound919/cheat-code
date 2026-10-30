package com.propslab.compute

import android.content.Context
import com.propslab.feed.HttpUrlConnectionFetcher
import com.propslab.feed.JsonFetcher
import com.propslab.feed.safeGet
import com.propslab.feed.safePost
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64

/**
 * The high-compute lane.
 *
 * Training a real model does not happen on a phone, so the app does not pretend to try.
 * What it does do is run the slower half of the formula pipeline there: a generated kernel
 * reads the real 665k-row player-game dataset, tests the claim's basketball idea against
 * the column that idea names, replays the lens on a held-out tail, and writes a small JSON
 * result the phone downloads and reads.
 *
 * Two credential shapes are accepted. A modern `KGAT_…` token is a bearer token and needs
 * only the username for the ref; the legacy username/key pair uses HTTP basic auth. The
 * value is never echoed back, logged, or written to the database.
 */
class KaggleClient(
    private val fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
) {

    data class Credentials(val username: String, val key: String) {
        val ready: Boolean get() = username.isNotBlank() && key.isNotBlank()
        val token: Boolean get() = key.startsWith("KGAT_")
    }

    data class Kernel(val ref: String, val title: String, val lastRunIso: String) {
        val url: String get() = "https://www.kaggle.com/code/$ref"
    }

    enum class State { QUEUED, RUNNING, COMPLETE, ERROR, CANCEL_REQUESTED, UNKNOWN }

    data class Status(val state: State, val failure: String = "") {
        val done: Boolean get() = state == State.COMPLETE || state == State.ERROR || state == State.CANCEL_REQUESTED
        val label: String
            get() = when (state) {
                State.QUEUED -> "queued"
                State.RUNNING -> "running"
                State.COMPLETE -> "complete"
                State.ERROR -> "failed"
                State.CANCEL_REQUESTED -> "cancelled"
                State.UNKNOWN -> "unknown"
            }
    }

    data class OutputFile(val name: String, val url: String)

    fun listUrl(username: String, pageSize: Int = 12): String =
        "$BASE/kernels/list?user=${encode(username)}&pageSize=${pageSize.coerceIn(1, 50)}&sortBy=dateRun"

    fun pushUrl(): String = "$BASE/kernels/push"

    fun statusUrl(ref: String): String {
        val (owner, slug) = split(ref)
        return "$BASE/kernels/status?user_name=${encode(owner)}&kernel_slug=${encode(slug)}"
    }

    fun outputUrl(ref: String): String {
        val (owner, slug) = split(ref)
        return "$BASE/kernels/output?user_name=${encode(owner)}&kernel_slug=${encode(slug)}"
    }

    /** The Kaggle API accepts either a bearer token or basic auth over `username:key`. */
    fun authHeader(credentials: Credentials): String =
        if (credentials.token) "Bearer ${credentials.key}"
        else "Basic " + Base64.getEncoder().encodeToString("${credentials.username}:${credentials.key}".toByteArray())

    suspend fun recent(credentials: Credentials): Result<List<Kernel>> {
        val response = fetcher.safeGet(listUrl(credentials.username), mapOf("Authorization" to authHeader(credentials)))
            .getOrElse { return Result.failure(it) }
        if (!response.ok) return Result.failure(IllegalStateException(failure(response.status)))
        return Result.success(parseKernels(response.body))
    }

    suspend fun status(credentials: Credentials, ref: String): Result<String> =
        sessionStatus(credentials, ref).map { it.label + if (it.failure.isBlank()) "" else " — ${it.failure}" }

    suspend fun sessionStatus(credentials: Credentials, ref: String): Result<Status> {
        val response = fetcher.safeGet(statusUrl(ref), mapOf("Authorization" to authHeader(credentials)))
            .getOrElse { return Result.failure(it) }
        if (!response.ok) return Result.failure(IllegalStateException(failure(response.status)))
        return Result.success(parseStatus(response.body))
    }

    /**
     * Saves a new version of a script kernel and runs it. Everything variable is in the
     * script text, which the caller generated; the dataset is pinned by ref so a later
     * version of the data cannot silently change what the result means.
     */
    suspend fun push(
        credentials: Credentials,
        slug: String,
        title: String,
        script: String,
        datasetRef: String,
    ): Result<Kernel> {
        if (!credentials.ready) return Result.failure(IllegalStateException("No Kaggle account is saved."))
        val ref = "${credentials.username}/$slug"
        val body = JSONObject().apply {
            put("slug", ref)
            put("newTitle", title)
            put("text", script)
            put("language", "python")
            put("kernelType", "script")
            put("isPrivate", true)
            put("enableGpu", false)
            put("enableTpu", false)
            put("enableInternet", false)
            put("datasetDataSources", JSONArray(listOf(datasetRef)))
            put("competitionDataSources", JSONArray())
            put("kernelDataSources", JSONArray())
            put("modelDataSources", JSONArray())
            put("categoryIds", JSONArray())
        }.toString()
        val response = fetcher.safePost(
            pushUrl(),
            body,
            mapOf("Authorization" to authHeader(credentials)),
        ).getOrElse { return Result.failure(it) }
        if (!response.ok) return Result.failure(IllegalStateException(failure(response.status)))
        val parsed = runCatching { JSONObject(response.body) }.getOrNull()
            ?: return Result.failure(IllegalStateException("Kaggle returned an unreadable push receipt."))
        val error = parsed.optString("error", "").trim()
        val badSources = parsed.optJSONArray("invalidDatasetSources")
        val bad = badSources?.length() ?: 0
        if (error.isNotEmpty()) return Result.failure(IllegalStateException("Kaggle refused the run: $error"))
        if (bad > 0) return Result.failure(IllegalStateException("Kaggle could not attach the dataset; check the account still has access."))
        val savedRef = parsed.optString("ref", "").removePrefix("/code/").ifBlank { ref }
        return Result.success(Kernel(savedRef, title, ""))
    }

    suspend fun output(credentials: Credentials, ref: String): Result<List<OutputFile>> {
        val response = fetcher.safeGet(outputUrl(ref), mapOf("Authorization" to authHeader(credentials)))
            .getOrElse { return Result.failure(it) }
        if (!response.ok) return Result.failure(IllegalStateException(failure(response.status)))
        return Result.success(parseOutput(response.body))
    }

    /** The file URLs are pre-signed by Kaggle; they need no credential and expire on their own. */
    suspend fun download(url: String): Result<String> {
        val response = fetcher.safeGet(url).getOrElse { return Result.failure(it) }
        if (!response.ok) return Result.failure(IllegalStateException(failure(response.status)))
        return Result.success(response.body)
    }

    fun parseKernels(body: String): List<Kernel> {
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Kernel>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val ref = o.optString("ref", "").trim()
            if (ref.isEmpty()) continue
            out += Kernel(
                ref = ref,
                title = o.optString("title", ref),
                lastRunIso = o.optString("lastRunTime", o.optString("lastRun", "")),
            )
        }
        return out
    }

    fun parseStatus(body: String): Status {
        val o = runCatching { JSONObject(body) }.getOrNull()
            ?: return Status(State.UNKNOWN, "unreadable status")
        val raw = o.optString("status", "").trim().lowercase()
        val state = when {
            raw.startsWith("complete") -> State.COMPLETE
            raw.startsWith("error") || raw.startsWith("fail") -> State.ERROR
            raw.startsWith("running") -> State.RUNNING
            raw.startsWith("queued") || raw.startsWith("pending") -> State.QUEUED
            raw.startsWith("cancel") -> State.CANCEL_REQUESTED
            else -> State.UNKNOWN
        }
        return Status(state, o.optString("failureMessage", "").trim())
    }

    fun parseOutput(body: String): List<OutputFile> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val files = root.optJSONArray("files") ?: return emptyList()
        val out = ArrayList<OutputFile>(files.length())
        for (i in 0 until files.length()) {
            val f = files.optJSONObject(i) ?: continue
            val name = f.optString("fileName", "").trim()
            val url = f.optString("url", "").trim()
            if (name.isEmpty() || url.isEmpty()) continue
            out += OutputFile(name, url)
        }
        return out
    }

    private fun split(ref: String): Pair<String, String> {
        val clean = ref.removePrefix("/code/").trim().trim('/')
        val owner = clean.substringBefore('/', "")
        val slug = clean.substringAfter('/', "")
        return owner to slug
    }

    private fun failure(status: Int): String = when (status) {
        401, 403 -> "Kaggle rejected the username and key"
        404 -> "Kaggle found no such kernel"
        429 -> "Kaggle rate-limited the request"
        else -> "Kaggle returned HTTP $status"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        const val BASE = "https://www.kaggle.com/api/v1"
    }
}

/**
 * Compute credentials on the device, in the same private preferences as the feed keys.
 * A Kaggle key is a bearer credential: it is never shown back on screen and never logged.
 */
class ComputeSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("compute", Context.MODE_PRIVATE)

    fun credentials(): KaggleClient.Credentials = KaggleClient.Credentials(
        username = prefs.getString(KEY_USER, "").orEmpty(),
        key = prefs.getString(KEY_KEY, "").orEmpty(),
    )

    fun save(credentials: KaggleClient.Credentials) {
        prefs.edit()
            .putString(KEY_USER, credentials.username.trim())
            .putString(KEY_KEY, credentials.key.trim())
            .apply()
    }

    private companion object {
        const val KEY_USER = "kaggle_user"
        const val KEY_KEY = "kaggle_key"
    }
}
