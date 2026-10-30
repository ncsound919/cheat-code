package com.propslab.feed

import android.content.Context
import com.propslab.domain.Markets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One bookmaker the odds provider can quote props from. */
data class Bookmaker(val key: String, val title: String, val note: String)

object Bookmakers {
    const val DRAFTKINGS = "draftkings"
    const val PRIZEPICKS = "prizepicks"
    const val FANATICS = "fanatics"

    /**
     * Verified 2026-10-29 against the provider's bookmaker list. PrizePicks is quoted under
     * the DFS region rather than `us`, and Fanatics is only returned on a paid plan, so the
     * screen says both out loud instead of failing silently.
     */
    val ALL = listOf(
        Bookmaker(DRAFTKINGS, "DraftKings", "US region, included on the free plan"),
        Bookmaker(PRIZEPICKS, "PrizePicks", "Quoted under the DFS region"),
        Bookmaker(FANATICS, "Fanatics", "Paid plans only"),
    )

    fun titles(keys: List<String>): String =
        keys.mapNotNull { key -> ALL.firstOrNull { it.key == key }?.title }.joinToString(", ")
            .ifEmpty { "no bookmaker selected" }
}

/**
 * Where per-player box scores come from.
 *
 * ESPN needs no account and carries every player's line, which is what makes a real board
 * priceable on the first sync. BALLDONTLIE is kept because its key is already supported, but
 * its per-player endpoint needs a paid plan, so it is not the default.
 */
object ResultsProvider {
    const val ESPN = "espn"
    const val BALLDONTLIE = "balldontlie"

    fun label(key: String): String = when (key) {
        BALLDONTLIE -> "BALLDONTLIE (needs a key)"
        else -> "ESPN (no key)"
    }

    fun note(key: String): String = when (key) {
        BALLDONTLIE -> "The player-stat endpoint needs a paid plan; a free key answers 401."
        else -> "Public box scores, no account, read-only."
    }
}

/**
 * What the device is allowed to ask for. Keys live in this app's private preferences and
 * are never written to the database, the audit trail, an audit note or a log line.
 */
data class FeedConfig(
    val oddsKey: String = "",
    val resultsKey: String = "",
    /** Which provider supplies per-player box scores. */
    val resultsProvider: String = ResultsProvider.ESPN,
    /** Optional: headline search for late availability news. */
    val newsKey: String = "",
    /** How many players on the board are checked for news per sync. */
    val newsMaxPlayers: Int = 6,
    /** A headline older than this is not a game-time report. */
    val newsLookbackHours: Int = 24,
    val bookmakers: List<String> = listOf(Bookmakers.DRAFTKINGS),
    val markets: List<String> = listOf(Markets.POINTS, Markets.REBOUNDS, Markets.ASSISTS),
    /**
     * Read the moneyline, spread and total as well as the props. Three extra markets per
     * game costs three extra credits, so they ride a slower clock than the props do.
     */
    val gameLines: Boolean = true,
    /** How far ahead the board looks for tip-off. */
    val lookaheadHours: Int = 36,
    /** Caps the odds requests one sync may spend. */
    val maxEvents: Int = 8,
    /** How many days of finished games are re-read to build the player log. */
    val resultsLookbackDays: Int = 2,
    /** Caps the box-score requests one sync may spend. */
    val maxResultGames: Int = 10,
) {
    val oddsReady: Boolean get() = oddsKey.isNotBlank()
    val resultsReady: Boolean get() = resultsProvider == ResultsProvider.ESPN || resultsKey.isNotBlank()
    val newsReady: Boolean get() = newsKey.isNotBlank()
    val anyReady: Boolean get() = oddsReady || resultsReady || newsReady

    /** Stored with the markets the model can actually price, whatever the row said. */
    fun pricedMarkets(): List<String> = markets.filter { it in Markets.VALIDATED }.ifEmpty { listOf(Markets.POINTS) }

    fun quotedBookmakers(): List<String> = bookmakers.ifEmpty { listOf(Bookmakers.DRAFTKINGS) }
}

/**
 * Keys and preferences on the device. Plain private preferences, deliberately: the value
 * is a token the user typed on their own phone, and the app has nothing stronger to bind
 * it to. It is never echoed back to a screen, so the field only ever shows whether a key
 * is present.
 */
class FeedSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("feed", Context.MODE_PRIVATE)

    // The numeric knobs ride along with the keys: they used to be accepted by save() and
    // silently dropped, so a tuned board reverted to defaults on the next load. The defaults
    // read from FeedConfig() itself, so a changed default cannot drift from the stored one.
    fun load(): FeedConfig = FeedConfig(
        oddsKey = prefs.getString(KEY_ODDS, "").orEmpty(),
        resultsKey = prefs.getString(KEY_RESULTS, "").orEmpty(),
        resultsProvider = prefs.getString(KEY_RESULTS_PROVIDER, ResultsProvider.ESPN)
            ?: ResultsProvider.ESPN,
        newsKey = prefs.getString(KEY_NEWS, "").orEmpty(),
        newsMaxPlayers = prefs.getInt(KEY_NEWS_MAX, FeedConfig().newsMaxPlayers),
        newsLookbackHours = prefs.getInt(KEY_NEWS_LOOKBACK, FeedConfig().newsLookbackHours),
        bookmakers = split(prefs.getString(KEY_BOOKMAKERS, Bookmakers.DRAFTKINGS)),
        markets = split(prefs.getString(KEY_MARKETS, "${Markets.POINTS},${Markets.REBOUNDS},${Markets.ASSISTS}")),
        gameLines = prefs.getBoolean(KEY_GAME_LINES, FeedConfig().gameLines),
        lookaheadHours = prefs.getInt(KEY_LOOKAHEAD, FeedConfig().lookaheadHours),
        maxEvents = prefs.getInt(KEY_MAX_EVENTS, FeedConfig().maxEvents),
        resultsLookbackDays = prefs.getInt(KEY_RESULTS_LOOKBACK, FeedConfig().resultsLookbackDays),
        maxResultGames = prefs.getInt(KEY_MAX_RESULTS, FeedConfig().maxResultGames),
    )

    fun save(config: FeedConfig) {
        prefs.edit()
            .putString(KEY_ODDS, config.oddsKey.trim())
            .putString(KEY_RESULTS, config.resultsKey.trim())
            .putString(KEY_RESULTS_PROVIDER, config.resultsProvider)
            .putString(KEY_NEWS, config.newsKey.trim())
            .putInt(KEY_NEWS_MAX, config.newsMaxPlayers.coerceIn(0, 24))
            .putInt(KEY_NEWS_LOOKBACK, config.newsLookbackHours.coerceIn(6, 72))
            .putString(KEY_BOOKMAKERS, config.quotedBookmakers().joinToString(","))
            .putString(KEY_MARKETS, config.pricedMarkets().joinToString(","))
            .putBoolean(KEY_GAME_LINES, config.gameLines)
            .putInt(KEY_LOOKAHEAD, config.lookaheadHours.coerceIn(6, 96))
            .putInt(KEY_MAX_EVENTS, config.maxEvents.coerceIn(1, 24))
            .putInt(KEY_RESULTS_LOOKBACK, config.resultsLookbackDays.coerceIn(1, 10))
            .putInt(KEY_MAX_RESULTS, config.maxResultGames.coerceIn(1, 120))
            .apply()
    }

    /**
     * When the straight game markets were last read. They move on a slower clock than the
     * props: a moneyline does not need refreshing every half hour, and each read costs credits.
     */
    fun gameLinesUtc(): Long = prefs.getLong(KEY_GAME_LINES_UTC, 0L)

    fun rememberGameLines(utc: Long) {
        prefs.edit().putLong(KEY_GAME_LINES_UTC, utc).apply()
    }

    /** Which path prefix the results provider accepted last, so a 404 is only paid once. */
    fun resultsPrefix(): String = prefs.getString(KEY_PREFIX, ResultsApi.PREFIX) ?: ResultsApi.PREFIX

    fun rememberResultsPrefix(prefix: String) {
        prefs.edit().putString(KEY_PREFIX, prefix).apply()
    }

    private fun split(raw: String?): List<String> =
        raw.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }

    private companion object {
        const val KEY_ODDS = "odds_key"
        const val KEY_RESULTS = "results_key"
        const val KEY_RESULTS_PROVIDER = "results_provider"
        const val KEY_NEWS = "news_key"
        const val KEY_NEWS_MAX = "news_max_players"
        const val KEY_NEWS_LOOKBACK = "news_lookback_hours"
        const val KEY_BOOKMAKERS = "bookmakers"
        const val KEY_MARKETS = "markets"
        const val KEY_GAME_LINES = "game_lines"
        const val KEY_GAME_LINES_UTC = "game_lines_utc"
        const val KEY_LOOKAHEAD = "lookahead_hours"
        const val KEY_MAX_EVENTS = "max_events"
        const val KEY_RESULTS_LOOKBACK = "results_lookback_days"
        const val KEY_MAX_RESULTS = "max_result_games"
        const val KEY_PREFIX = "results_prefix"
    }
}

/**
 * The results provider dates its games in US Eastern, and a 10pm tip-off is already the
 * next day in UTC. Matching a tip-off to the right box score therefore has to use the
 * Eastern date, not the UTC one.
 */
object NbaCalendar {
    private val EASTERN: ZoneId = ZoneId.of("America/New_York")

    fun gameDate(utc: Long): LocalDate = Instant.ofEpochMilli(utc).atZone(EASTERN).toLocalDate()
}
