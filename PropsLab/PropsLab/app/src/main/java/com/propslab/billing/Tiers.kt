package com.propslab.billing

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import com.propslab.brand.Brand
import java.util.Locale

/**
 * What the free build can do, and what five dollars a month adds.
 *
 * Every entry here is enforced somewhere in the app. A feature list that promises a gate
 * the code does not have is a lie told on the pricing screen, so the list stays short.
 */
enum class Feature(val label: String, val freeVersion: String) {
    PARLAY_10("10-leg parlays", "3- and 5-leg parlays"),
    FANTASY_DESK("Fantasy desk", "A locked preview card"),
    SPORTS_WIRE("Sports wire", "A locked preview card"),
    FLYER_CLEAN("Flyers without a watermark", "Flyers carry a small Cheat Code mark"),
    CLOUD_MODEL("Cloud model fallback", "On-device model only"),
}

enum class Tier(val label: String) {
    FREE(Brand.FREE_NAME),
    TRIAL("Trial"),
    PREMIUM(Brand.PREMIUM_NAME),
}

/**
 * The premium unlock.
 *
 * The build is local and private by design, so the entitlement is stored on the device and
 * checked offline. A code is a checksummed token: `PL5-XXXX-XXXX-XXXX`, where the last four
 * characters are derived from the first twelve. The check is deliberately honest about what
 * it is — it proves a code was issued under this scheme, not that a payment happened. The
 * site decides who gets a code (it reads the Stripe subscription), the app only decides
 * whether the code it was given is well formed, which is what lets Premium work offline.
 */
object TierStore {

    /**
     * The site endpoint that creates a Stripe Checkout Session and redirects to it. It is a
     * redirect rather than a payment link so the price and the trial live in one place, on
     * the server, and a stale URL can never charge an old price.
     */
    const val CHECKOUT = Brand.CHECKOUT

    const val TRIAL_DAYS = 7

    private val premium = mutableStateOf(false)
    private val trialUntil = mutableStateOf(0L)
    private val checkout = mutableStateOf(CHECKOUT)
    private val licensedAs = mutableStateOf<String?>(null)

    private var prefs: SharedPreferences? = null

    val isPremium: Boolean
        get() = premium.value || trialUntil.value > System.currentTimeMillis()

    val onTrial: Boolean
        get() = !premium.value && trialUntil.value > System.currentTimeMillis()

    val tier: Tier
        get() = when {
            premium.value -> Tier.PREMIUM
            trialUntil.value > System.currentTimeMillis() -> Tier.TRIAL
            else -> Tier.FREE
        }

    val tierLabel: String get() = tier.label

    val trialDaysLeft: Int
        get() {
            val ms = trialUntil.value - System.currentTimeMillis()
            return if (ms <= 0L) 0 else Math.ceil(ms / 86_400_000.0).toInt()
        }

    val checkoutUrl: String get() = checkout.value

    /** The email a code was issued to, shown so a buyer can tell which account is unlocked. */
    val licensedEmail: String? get() = licensedAs.value

    /** One switch today, but asked per feature so a future free allowance cannot leak by accident. */
    fun allows(feature: Feature): Boolean = when (feature) {
        Feature.PARLAY_10,
        Feature.FANTASY_DESK,
        Feature.SPORTS_WIRE,
        Feature.FLYER_CLEAN,
        Feature.CLOUD_MODEL,
        -> isPremium
    }

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("propslab_tier", Context.MODE_PRIVATE)
        prefs = p
        premium.value = p.getBoolean(KEY_PREMIUM, false)
        trialUntil.value = p.getLong(KEY_TRIAL, 0L)
        checkout.value = p.getString(KEY_CHECKOUT, CHECKOUT).orEmpty().ifBlank { CHECKOUT }
        licensedAs.value = p.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }
    }

    /** Starts the trial once. A second tap cannot extend it. */
    fun startTrial(): Boolean {
        if (trialUntil.value > 0L || premium.value) return false
        val until = System.currentTimeMillis() + TRIAL_DAYS * 86_400_000L
        trialUntil.value = until
        prefs?.edit()?.putLong(KEY_TRIAL, until)?.apply()
        return true
    }

    fun unlock(code: String): LicenseResult {
        val normalised = Licensing.normalise(code)
        if (normalised.isEmpty()) return LicenseResult.Invalid("Enter the code from your receipt.")
        if (!Licensing.verify(normalised)) {
            return LicenseResult.Invalid("That code does not check out. Copy it exactly as it was sent.")
        }
        premium.value = true
        prefs?.edit()?.putBoolean(KEY_PREMIUM, true)?.apply()
        return LicenseResult.Unlocked
    }

    /** Used by the owner's issue script and by tests. */
    fun issue(email: String): String = Licensing.issue(Licensing.payloadFor(email))

    fun rememberEmail(email: String) {
        licensedAs.value = email.trim().lowercase(Locale.ROOT)
        prefs?.edit()?.putString(KEY_EMAIL, licensedAs.value)?.apply()
    }

   fun setCheckoutUrl(url: String) {
        checkout.value = url.trim().ifBlank { CHECKOUT }
        prefs?.edit()?.putString(KEY_CHECKOUT, checkout.value)?.apply()
    }

    fun revoke() {
        premium.value = false
        licensedAs.value = null
        prefs?.edit()?.putBoolean(KEY_PREMIUM, false)?.remove(KEY_EMAIL)?.apply()
    }

    sealed interface LicenseResult {
        data object Unlocked : LicenseResult
        data class Invalid(val reason: String) : LicenseResult
    }

    private const val KEY_PREMIUM = "premium"
    private const val KEY_TRIAL = "trial_until"
    private const val KEY_CHECKOUT = "checkout_url"
    private const val KEY_EMAIL = "licensed_email"
}

/**
 * Pure, testable licence token arithmetic.
 *
 * A token is `PL5` followed by twelve payload characters and a four-character checksum, all
 * drawn from a 32-letter alphabet that omits I, O, 0 and 1 so a code can be read off a
 * receipt without guessing. The checksum is FNV-1a over the payload and a fixed salt,
 * written in the same alphabet: short enough to type, long enough that a typo is caught.
 *
 * This is an offline entitlement check, not a cryptographic signature. Anyone with the APK
 * can read the salt, so the check cannot stop a determined forger; what it does stop is a
 * mistyped or invented code, which is the only failure mode a paying customer meets.
 */
object Licensing {

    private const val SALT = "propslab-premium-2026-v1"
    private const val PREFIX = "PL5"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val PAYLOAD = 12
    private const val CHECKSUM = 4

    fun normalise(raw: String): String =
        raw.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    fun verify(code: String): Boolean {
        val clean = normalise(code)
        if (clean.length != PREFIX.length + PAYLOAD + CHECKSUM) return false
        if (!clean.startsWith(PREFIX)) return false
        val body = clean.substring(PREFIX.length, PREFIX.length + PAYLOAD)
        if (body.any { it !in ALPHABET }) return false
        val given = clean.substring(PREFIX.length + PAYLOAD)
        return given == checksum(body)
    }

    /** A deterministic payload from an email, so one purchase maps to one code. */
    fun payloadFor(email: String): String {
        val key = email.trim().lowercase(Locale.ROOT)
        val hash = fnv(key)
        val out = StringBuilder(PAYLOAD)
        var value = hash
        repeat(PAYLOAD) {
            out.append(ALPHABET[(value and 0x1FUL).toInt()])
            value = (value shr 5) xor (value * 0x100000001B3UL)
        }
        return out.toString()
    }

    fun issue(payload: String): String {
        val body = payload.uppercase(Locale.ROOT).filter { it in ALPHABET }.take(PAYLOAD)
            .padEnd(PAYLOAD, 'A')
        val grouped = body.chunked(4).joinToString("-")
        return "$PREFIX-$grouped-${checksum(body)}"
    }

    private fun checksum(body: String): String {
        val hash = fnv(body + SALT)
        val out = StringBuilder(CHECKSUM)
        var value = hash
        repeat(CHECKSUM) {
            out.append(ALPHABET[(value and 0x1FUL).toInt()])
            value = (value shr 5) or (hash shl 3)
        }
        return out.toString()
    }

    private fun fnv(text: String): ULong {
        var h = 0xcbf29ce484222325UL
        for (ch in text) {
            h = (h xor ch.code.toULong()) * 0x100000001b3UL
        }
        return h
    }
}
