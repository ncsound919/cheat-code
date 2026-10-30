package com.propslab.account

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional email verification, using Supabase's open-source GoTrue server.
 *
 * An account is not required and no screen asks for one until the user opens it. The
 * current release does not upload strategies or ledger rows: sign-in proves the address
 * and keeps the verified email on this device. Session tokens are deliberately not
 * retained, because nothing in the app uses them and a long-lived refresh token would be
 * a liability with no matching feature.
 *
 * The protocol is a passwordless email one-time code, so no password is ever collected,
 * stored or reset. The project URL and anon key are public by design.
 */
object Account {

    private val email = mutableStateOf<String?>(null)
    private val busy = mutableStateOf(false)
    private val message = mutableStateOf<String?>(null)
    private var prefs: SharedPreferences? = null

    val signedInEmail: String? get() = email.value
    val isSignedIn: Boolean get() = email.value != null
    val isBusy: Boolean get() = busy.value
    val status: String? get() = message.value

    /**
     * The shipped project, unless this install has been pointed somewhere else. Both values
     * are public: the URL is the auth server and the publishable key is the one Supabase
     * hands to browsers. The secret key never leaves the server.
     */
    val projectUrl: String
        get() = prefs?.getString(KEY_URL, "").orEmpty().ifBlank { com.propslab.brand.Brand.AUTH_URL }

    val anonKey: String
        get() = prefs?.getString(KEY_ANON, "").orEmpty().ifBlank { com.propslab.brand.Brand.AUTH_ANON_KEY }

    /** True when the build ships a project or the user has entered one. */
    val isConfigured: Boolean get() = projectUrl.isNotBlank() && anonKey.isNotBlank()

    val isCustomProject: Boolean
        get() = prefs?.getString(KEY_URL, "").orEmpty().isNotBlank()

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("propslab_account", Context.MODE_PRIVATE)
        prefs = p
        email.value = p.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }
        message.value = null
    }

    fun configure(url: String, anonKey: String) {
        val cleanUrl = url.trim().trimEnd('/')
        if (!cleanUrl.startsWith("https://")) {
            message.value = "Use the project's HTTPS URL; cleartext auth is refused."
            return
        }
        prefs?.edit()
            ?.putString(KEY_URL, cleanUrl)
            ?.putString(KEY_ANON, anonKey.trim())
            ?.apply()
        message.value = "Saved. Request a code to finish signing in."
    }

    fun signOut() {
        email.value = null
        message.value = "Signed out. Local data stays on this phone."
        val editor = prefs?.edit()
        if (editor != null) {
            editor.remove(KEY_EMAIL)
            editor.apply()
        }
    }

    /** Sends a six-digit code to [address]. Returns null on success, or a sentence on failure. */
    suspend fun requestCode(address: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext "Add your Supabase project URL and anon key first."
        if (!address.contains('@')) return@withContext "That does not look like an email address."
        busy.value = true
        try {
            val body = JSONObject().put("email", address.trim()).put("create_user", true).toString()
            val response = post("$projectUrl/auth/v1/otp", body)
            if (response.code in 200..299) {
                message.value = "Code sent to ${address.trim()}. It expires in an hour."
                null
            } else {
                message.value = "The server said ${response.message}"
                response.message
            }
        } catch (t: Throwable) {
            message.value = t.message ?: "Could not reach the auth server."
            t.message
        } finally {
            busy.value = false
        }
    }

    /** Exchanges the emailed code for a session. Returns null on success. */
    suspend fun verify(address: String, code: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext "Add your Supabase project URL and anon key first."
        val token = code.trim()
        if (token.length < 4) return@withContext "Enter the code from the email."
        busy.value = true
        try {
            val body = JSONObject()
                .put("type", "email")
                .put("email", address.trim())
                .put("token", token)
                .toString()
            val response = post("$projectUrl/auth/v1/verify", body)
            if (response.code !in 200..299) {
                message.value = "The server said ${response.message}"
                return@withContext response.message
            }
            val json = runCatching { JSONObject(response.body) }.getOrNull()
                ?: return@withContext "The server's answer could not be read."
            val user = json.optJSONObject("user")
            val signed = user?.optString("email")?.takeIf { it.isNotBlank() } ?: address.trim()
            email.value = signed
            prefs?.edit()?.putString(KEY_EMAIL, signed)?.apply()
            message.value = "Signed in as $signed."
            null
        } catch (t: Throwable) {
            message.value = t.message ?: "Could not reach the auth server."
            t.message
        } finally {
            busy.value = false
        }
    }

    private data class Response(val code: Int, val body: String, val message: String)

    private fun post(url: String, json: String): Response {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer $anonKey")
        }
        try {
            connection.outputStream.use { it.write(json.toByteArray()) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val serverMessage = runCatching {
                JSONObject(text).optString("msg")
                    .ifBlank { JSONObject(text).optString("error_description") }
                    .ifBlank { JSONObject(text).optString("error") }
            }.getOrNull().orEmpty().ifBlank { "HTTP $code" }
            return Response(code, text, serverMessage)
        } finally {
            connection.disconnect()
        }
    }

    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON = "supabase_anon"
    private const val KEY_EMAIL = "signed_email"
}
