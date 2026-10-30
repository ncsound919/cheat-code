package com.propslab.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/** How odds are printed. American is the sportsbook's own voice; decimal is the raw price. */
enum class OddsFormat { AMERICAN, DECIMAL }

/**
 * Everything the user can tune, in one place.
 *
 * The values live in [AppSettings] as snapshot state rather than behind a view model,
 * because they are read from pure formatting code ([com.propslab.ui.fmtOdds]), from the
 * narrative builder and from the native sampling loop — places a Compose owner cannot
 * reach. A read during composition subscribes the caller, so a change here repaints every
 * screen that shows the number.
 *
 * Defaults are the app's shipped behaviour: quarter-Kelly (what StrategyNotes recommends),
 * American odds, and the llama.cpp sampling values the runtime used before this file
 * existed. Changing a default would silently change every pick's stated stake, so the
 * defaults are deliberately the old constants.
 */
data class Settings(
    // ---- display
    /**
     * The default reading of the app.
     *
     * On, a pick is a sentence and a verdict; the edge, EV and Kelly arithmetic is one tap
     * away under "Show the numbers". Off, the full ledger is on screen at once. The maths is
     * identical either way — this changes what is explained, never what is calculated.
     */
    val easyMode: Boolean = true,
    val oddsFormat: OddsFormat = OddsFormat.AMERICAN,
    // ---- staking
    /** Fraction of full Kelly the board stakes. 0.25 = quarter Kelly. */
    val stakeFraction: Double = 0.25,
    // ---- on-device model sampling (llama.cpp)
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    /** Prompt context in tokens; applies at the next model load. */
    val contextSize: Int = 4096,
)

object AppSettings {

    private val state = mutableStateOf(Settings())

    /** Current values. Safe to read before [init] — tests and early startup get defaults. */
    val current: Settings get() = state.value

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs = p
        state.value = Settings(
            easyMode = p.getBoolean(KEY_EASY, Settings().easyMode),
            oddsFormat = runCatching { OddsFormat.valueOf(p.getString(KEY_ODDS_FORMAT, "").orEmpty()) }
                .getOrDefault(Settings().oddsFormat),
            stakeFraction = p.getFloat(KEY_STAKE, Settings().stakeFraction.toFloat()).toDouble()
                .coerceIn(0.05, 1.0),
            temperature = p.getFloat(KEY_TEMP, Settings().temperature).coerceIn(0.0f, 1.5f),
            topK = p.getInt(KEY_TOP_K, Settings().topK).coerceIn(0, 100),
            topP = p.getFloat(KEY_TOP_P, Settings().topP).coerceIn(0.5f, 1.0f),
            maxTokens = p.getInt(KEY_MAX_TOKENS, Settings().maxTokens).coerceIn(64, 2048),
            contextSize = p.getInt(KEY_CTX, Settings().contextSize).coerceIn(2048, 8192),
        )
    }

    /** Persist and publish one change. Every setter in the settings screen goes through here. */
    fun update(transform: (Settings) -> Settings) {
        val next = transform(state.value)
        state.value = next
        prefs?.edit()
            ?.putBoolean(KEY_EASY, next.easyMode)
            ?.putString(KEY_ODDS_FORMAT, next.oddsFormat.name)
            ?.putFloat(KEY_STAKE, next.stakeFraction.toFloat())
            ?.putFloat(KEY_TEMP, next.temperature)
            ?.putInt(KEY_TOP_K, next.topK)
            ?.putFloat(KEY_TOP_P, next.topP)
            ?.putInt(KEY_MAX_TOKENS, next.maxTokens)
            ?.putInt(KEY_CTX, next.contextSize)
            ?.apply()
    }

    fun reset() = update { Settings() }

    private const val KEY_EASY = "easy_mode"
    private const val KEY_ODDS_FORMAT = "odds_format"
    private const val KEY_STAKE = "stake_fraction"
    private const val KEY_TEMP = "temperature"
    private const val KEY_TOP_K = "top_k"
    private const val KEY_TOP_P = "top_p"
    private const val KEY_MAX_TOKENS = "max_tokens"
    private const val KEY_CTX = "context_size"
}

/**
 * The stake fraction in words, so a sentence can say "a quarter-Kelly stake" rather than
 * "a 0.25-Kelly stake". Unusual fractions fall back to their percentage.
 */
fun stakeWord(fraction: Double): String = when {
    fraction >= 0.999 -> "full"
    fraction >= 0.4999 -> "half"
    fraction >= 0.2499 && fraction <= 0.2501 -> "quarter"
    else -> "%.0f%%".format(fraction * 100)
}
