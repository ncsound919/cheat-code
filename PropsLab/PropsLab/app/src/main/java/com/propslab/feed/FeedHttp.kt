package com.propslab.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** A raw HTTP response, headers lowercased so callers can read the quota headers by name. */
data class HttpResult(val status: Int, val body: String, val headers: Map<String, String>) {
    val ok: Boolean get() = status in 200..299

    fun header(name: String): String? = headers[name.lowercase()]
}

/**
 * The one way this app touches the network.
 *
 * It is an interface so the whole sync can be exercised on the JVM against recorded
 * payloads: nothing above this line knows what a socket is.
 */
interface JsonFetcher {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResult

    /** Only the Kaggle lane needs a body; a test fake that never pushes can ignore it. */
    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): HttpResult =
        throw UnsupportedOperationException("This fetcher cannot POST")
}

/** Plain `HttpURLConnection`: one GET, JSON, a hard timeout, no redirects to chase. */
class HttpUrlConnectionFetcher(
    private val timeoutMs: Int = 15_000,
    private val userAgent: String = "Cheat Code/0.1 (Android)",
) : JsonFetcher {

    override suspend fun get(url: String, headers: Map<String, String>): HttpResult =
        call(url, null, headers)

    override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResult =
        call(url, body, headers)

    private suspend fun call(url: String, body: String?, headers: Map<String, String>): HttpResult = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", userAgent)
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val flat = HashMap<String, String>()
            for ((name, values) in connection.headerFields) {
                if (name == null) continue
                flat[name.lowercase()] = values.joinToString(",")
            }
            HttpResult(status, text, flat)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * A failure the screen can repeat without leaking anything. The message never contains a
 * request URL, because every URL carries an API key.
 */
data class FeedFailure(val status: Int, val message: String) {
    companion object {
        fun of(result: HttpResult, provider: String): FeedFailure = when (result.status) {
            401, 403 -> FeedFailure(result.status, "$provider rejected the key")
            404 -> FeedFailure(result.status, "$provider found no such route")
            429 -> FeedFailure(result.status, "$provider rate-limited the request; try again later")
            else -> FeedFailure(result.status, "$provider returned HTTP ${result.status}")
        }
    }
}

/** Wraps a fetch so a network exception becomes a failure line instead of a crash. */
suspend fun JsonFetcher.safeGet(url: String, headers: Map<String, String> = emptyMap()): Result<HttpResult> =
    runCatching { get(url, headers) }

suspend fun JsonFetcher.safePost(
    url: String,
    body: String,
    headers: Map<String, String> = emptyMap(),
): Result<HttpResult> = runCatching { post(url, body, headers) }
