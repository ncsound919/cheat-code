package com.propslab.inference

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The optional cloud model.
 *
 * Some phones cannot hold useful weights, and some people would rather borrow a large
 * model than download a small one. This is a plain OpenAI-compatible chat-completions
 * call: any endpoint that speaks that dialect works, including a self-hosted one. The key
 * is supplied by the user, kept in private preferences, and never logged.
 *
 * The local model stays the default. Nothing here runs until a key is saved and the user
 * asks a question in the console — the app does not quietly send the board anywhere.
 */
object CloudModel {

    private val saved = mutableStateOf(false)
    private var prefs: SharedPreferences? = null

    val hasKey: Boolean get() = saved.value

    val baseUrl: String get() = prefs?.getString(KEY_BASE, DEFAULT_BASE).orEmpty().ifBlank { DEFAULT_BASE }
    val model: String get() = prefs?.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
    val keyHint: String
        get() {
            val key = prefs?.getString(KEY_KEY, "").orEmpty()
            return if (key.length <= 6) "" else "…" + key.takeLast(4)
        }

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("propslab_cloud", Context.MODE_PRIVATE)
        saved.value = !prefs?.getString(KEY_KEY, "").isNullOrBlank()
    }

    fun save(base: String, key: String, model: String) {
        prefs?.edit()
            ?.putString(KEY_BASE, base.trim().trimEnd('/').ifBlank { DEFAULT_BASE })
            ?.putString(KEY_KEY, key.trim())
            ?.putString(KEY_MODEL, model.trim().ifBlank { DEFAULT_MODEL })
            ?.apply()
        saved.value = key.trim().isNotBlank()
    }

    fun forget() {
        prefs?.edit()?.remove(KEY_KEY)?.apply()
        saved.value = false
    }

    /** One round trip, used by the settings card to prove the wiring works. */
    suspend fun ping(): Result<String> = complete(
        system = "You are a terse assistant. Answer in one short sentence.",
        prompt = "Say that the cloud connection works.",
        maxTokens = 40,
    )

    suspend fun complete(system: String, prompt: String, maxTokens: Int = 400): Result<String> =
        withContext(Dispatchers.IO) {
            val key = prefs?.getString(KEY_KEY, "").orEmpty()
            if (key.isBlank()) return@withContext Result.failure(IllegalStateException("No cloud key is saved."))
            val payload = JSONObject()
                .put("model", model)
                .put("max_tokens", maxTokens)
                .put("temperature", 0.4)
                .put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().put("role", "system").put("content", system))
                        .put(JSONObject().put("role", "user").put("content", prompt)),
                )
                .toString()
            val connection = runCatching {
                (URL("$baseUrl/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $key")
                }
            }.getOrElse { return@withContext Result.failure(it) }
            try {
                connection.outputStream.use { it.write(payload.toByteArray()) }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val reason = runCatching {
                        JSONObject(text).optJSONObject("error")?.optString("message")
                    }.getOrNull().orEmpty().ifBlank { "HTTP $code" }
                    return@withContext Result.failure(IllegalStateException(reason))
                }
                val content = runCatching {
                    JSONObject(text).getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").optString("content")
                }.getOrNull().orEmpty().trim()
                if (content.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("The endpoint returned an empty answer."))
                }
                Result.success(content)
            } catch (t: Throwable) {
                Result.failure(t)
            } finally {
                connection.disconnect()
            }
        }

    const val DEFAULT_BASE = "https://api.openai.com/v1"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    private const val KEY_BASE = "cloud_base"
    private const val KEY_MODEL = "cloud_model"
    private const val KEY_KEY = "cloud_key"
}
