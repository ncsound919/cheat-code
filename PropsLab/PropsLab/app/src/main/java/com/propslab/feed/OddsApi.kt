package com.propslab.feed

import com.propslab.domain.Markets
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

/** One NBA game, as the odds provider sees it. */
data class OddsEvent(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val commenceUtc: Long,
)

/**
 * One book's two-way price on one player, market and line.
 *
 * Both prices come from the same bookmaker on purpose: mixing a DraftKings over with a
 * PrizePicks under would produce a no-vig number that nobody could actually bet.
 */
data class RawQuote(
    val eventId: String,
    val playerName: String,
    val playerKey: String,
    val market: String,
    val line: Double,
    val overPrice: Int,
    val underPrice: Int,
    val bookKey: String,
    val bookTitle: String,
    val updatedUtc: Long,
)

/** The game markets, in the provider's own keys. Props are priced separately from these. */
object GameMarkets {
    const val MONEYLINE = "h2h"
    const val SPREAD = "spreads"
    const val TOTAL = "totals"

    val ALL = listOf(MONEYLINE, SPREAD, TOTAL)

    fun label(market: String): String = when (market) {
        MONEYLINE -> "Moneyline"
        SPREAD -> "Spread"
        TOTAL -> "Total"
        else -> market
    }
}

/** One book's price on one side of a game: a team's moneyline, a team's spread, or a total. */
data class RawGameLine(
    val eventId: String,
    val market: String,
    val side: String,
    val line: Double?,
    val price: Int,
    val bookKey: String,
    val bookTitle: String,
    val updatedUtc: Long,
)

/**
 * The odds provider, as documented 2026-10-29.
 *
 * Player props are only served by the single-event endpoint, which costs one credit per
 * market returned per region; the free plan allows 500 credits a month, so a sync is
 * deliberately capped at a handful of games. Everything the app asks for is a read: this
 * provider publishes prices, never wagers.
 */
object OddsApi {

    const val BASE = "https://api.the-odds-api.com/v4"
    const val SPORT = "basketball_nba"

    fun eventsUrl(apiKey: String): String =
        "$BASE/sports/$SPORT/events?apiKey=$apiKey&dateFormat=iso"

    fun eventOddsUrl(
        eventId: String,
        apiKey: String,
        markets: List<String>,
        bookmakers: List<String>,
    ): String = buildString {
        append("$BASE/sports/$SPORT/events/$eventId/odds?apiKey=$apiKey")
        append("&regions=us&oddsFormat=american&dateFormat=iso")
        append("&markets=").append(markets.joinToString(",") { apiMarket(it) })
        // Bookmakers take priority over regions when both are present, which is exactly
        // what is wanted: the three books the app knows about, and nothing else.
        if (bookmakers.isNotEmpty()) append("&bookmakers=").append(bookmakers.joinToString(","))
    }

    /** The provider's market key for one of this app's markets. */
    fun apiMarket(market: String): String = when (market) {
        Markets.PRA -> "player_points_rebounds_assists"
        else -> market
    }

    /** This app's market for one of the provider's keys. Unknown keys return null. */
    fun appMarket(apiKey: String): String? = when (apiKey) {
        "player_points_rebounds_assists" -> Markets.PRA
        Markets.POINTS, Markets.REBOUNDS, Markets.ASSISTS, Markets.THREES, Markets.PRA -> apiKey
        else -> null
    }

    fun parseEvents(body: String): List<OddsEvent> {
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return emptyList()
        val out = ArrayList<OddsEvent>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id", "")
            val home = o.optString("home_team", "")
            val away = o.optString("away_team", "")
            val commence = isoUtc(o.optString("commence_time", ""))
            if (id.isEmpty() || home.isEmpty() || away.isEmpty() || commence <= 0L) continue
            out += OddsEvent(id, home, away, commence)
        }
        return out
    }

    /**
     * Reads the straight game markets out of the same payload the props come in.
     *
     * These are single prices, not two-way pairs: a moneyline, a spread and a total each
     * stand on their own and are compared across books on the screen. A spread without a
     * handicap, or a total without a number, is dropped rather than shown as a blank.
     */
    fun parseGameLines(body: String, fallbackUtc: Long): List<RawGameLine> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val eventId = root.optString("id", "")
        if (eventId.isEmpty()) return emptyList()
        val books = root.optJSONArray("bookmakers") ?: return emptyList()
        val out = ArrayList<RawGameLine>()
        for (b in 0 until books.length()) {
            val book = books.optJSONObject(b) ?: continue
            val bookKey = book.optString("key", "")
            val bookTitle = book.optString("title", bookKey)
            val markets = book.optJSONArray("markets") ?: continue
            for (m in 0 until markets.length()) {
                val market = markets.optJSONObject(m) ?: continue
                val key = market.optString("key", "")
                if (key !in GameMarkets.ALL) continue
                val updated = isoUtc(market.optString("last_update", "")).takeIf { it > 0L } ?: fallbackUtc
                val outcomes = market.optJSONArray("outcomes") ?: continue
                for (i in 0 until outcomes.length()) {
                    val o = outcomes.optJSONObject(i) ?: continue
                    val side = o.optString("name", "").trim()
                    if (side.isEmpty()) continue
                    val price = priceOf(o) ?: continue
                    val line = if (o.has("point") && !o.isNull("point")) {
                        o.optDouble("point", Double.NaN).takeIf { it.isFinite() }
                    } else null
                    if (key != GameMarkets.MONEYLINE && line == null) continue
                    out += RawGameLine(eventId, key, side, line, price, bookKey, bookTitle, updated)
                }
            }
        }
        return out.distinctBy { "${it.market}|${it.side}|${it.line}|${it.bookKey}" }
    }

    /**
     * Reads one event's bookmakers into prices. Only complete two-way pairs survive: a book
     * that quotes one side only is a book the app cannot price.
     */
    fun parseEventOdds(body: String, fallbackUtc: Long): List<RawQuote> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val eventId = root.optString("id", "")
        val books = root.optJSONArray("bookmakers") ?: return emptyList()
        val out = ArrayList<RawQuote>()
        for (b in 0 until books.length()) {
            val book = books.optJSONObject(b) ?: continue
            val bookKey = book.optString("key", "")
            val bookTitle = book.optString("title", bookKey)
            val markets = book.optJSONArray("markets") ?: continue
            for (m in 0 until markets.length()) {
                val market = markets.optJSONObject(m) ?: continue
                val appMarket = appMarket(market.optString("key", "")) ?: continue
                val marketUpdated = isoUtc(market.optString("last_update", ""))
                val updated = if (marketUpdated > 0L) marketUpdated else fallbackUtc
                val outcomes = market.optJSONArray("outcomes") ?: continue
                out += pairOutcomes(eventId, appMarket, bookKey, bookTitle, updated, outcomes)
            }
        }
        return out
    }

    private fun pairOutcomes(
        eventId: String,
        market: String,
        bookKey: String,
        bookTitle: String,
        updatedUtc: Long,
        outcomes: JSONArray,
    ): List<RawQuote> {
        // player -> line -> the two prices seen so far
        val byPlayer = LinkedHashMap<String, LinkedHashMap<Double, Pair<Int?, Int?>>>()
        for (i in 0 until outcomes.length()) {
            val o = outcomes.optJSONObject(i) ?: continue
            val player = o.optString("description", "").trim()
            if (player.isEmpty()) continue
            val side = o.optString("name", "").trim().lowercase()
            if (side != "over" && side != "under") continue
            if (!o.has("point") || o.isNull("point")) continue
            val line = o.optDouble("point", Double.NaN)
            if (!line.isFinite()) continue
            val price = priceOf(o)
            if (price == null) continue
            val lines = byPlayer.getOrPut(player) { LinkedHashMap() }
            val current = lines[line]
            lines[line] = if (side == "over") price to (current?.second) else (current?.first) to price
        }

        val out = ArrayList<RawQuote>()
        for ((player, lines) in byPlayer) {
            for ((line, pair) in lines) {
                val over = pair.first ?: continue
                val under = pair.second ?: continue
                out += RawQuote(
                    eventId = eventId,
                    playerName = player,
                    playerKey = Names.key(player),
                    market = market,
                    line = line,
                    overPrice = over,
                    underPrice = under,
                    bookKey = bookKey,
                    bookTitle = bookTitle,
                    updatedUtc = updatedUtc,
                )
            }
        }
        return out
    }

    /** American prices are integers; anything else is rounded and range-checked. */
    fun priceOf(outcome: JSONObject): Int? {
        val raw = when (val value = outcome.opt("price")) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        } ?: return null
        val price = Math.round(raw).toInt()
        if (price > -100 && price < 100) return null
        return price
    }

    fun isoUtc(text: String): Long {
        if (text.isBlank()) return 0L
        runCatching { return OffsetDateTime.parse(text).toInstant().toEpochMilli() }
        runCatching { return Instant.parse(text).toEpochMilli() }
        return 0L
    }
}
