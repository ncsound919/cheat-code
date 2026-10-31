package com.propslab.feed

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** One NBA game from the results provider. */
data class ResultGame(
    val id: String,
    val dateIso: String,
    val state: String,
    val homeName: String,
    val awayName: String,
    val homeAbbr: String,
    val awayAbbr: String,
    /** The league that published this game, so a basketball final can never attach to a football row. */
    val sport: String = com.propslab.domain.Sports.BASKETBALL,
) {
    val isFinal: Boolean get() = state == "final"
}

/** One player's box score line, reduced to the columns a prop can be written on. */
data class BoxScore(
    val statId: String,
    val gameId: String,
    val playedUtc: Long,
    val playerName: String,
    val playerKey: String,
    val teamAbbr: String,
    val points: Int,
    val rebounds: Int,
    val assists: Int,
    val threes: Int,
    val minutes: Int,
    /** ESPN's athlete id when the row came from ESPN; empty for other providers. */
    val athleteId: String = "",
    /** Football columns. A basketball row leaves them at zero. */
    val passYards: Int = 0,
    val rushYards: Int = 0,
    val receivingYards: Int = 0,
    val receptions: Int = 0,
    /** Which league the line came from, so a settlement never reads the wrong columns. */
    val sport: String = com.propslab.domain.Sports.BASKETBALL,
)

/**
 * Box scores, as documented 2026-10-29.
 *
 * The provider's own docs disagree about the path prefix (`/v1` in the rendered docs,
 * `/nba/v1` in the OpenAPI spec), so both are tried and the one that answers is remembered.
 * Player stat rows need a paid plan on their side; a key without that entitlement answers
 * 401/403 and the screen says so instead of pretending the model has no data.
 */
object ResultsApi {

    const val BASE = "https://api.balldontlie.io"
    const val PREFIX = "/v1"
    val PREFIXES = listOf("/v1", "/nba/v1")

    fun gamesUrl(prefix: String, isoDate: String): String =
        "$BASE$prefix/games?dates[]=$isoDate&per_page=100"

    fun statsUrl(prefix: String, gameId: String): String =
        "$BASE$prefix/stats?game_ids[]=$gameId&per_page=100"

    fun parseGames(body: String): List<ResultGame> {
        val data = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return emptyList()
        val out = ArrayList<ResultGame>(data.length())
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            val id = o.opt("id")?.toString().orEmpty()
            if (id.isEmpty() || id == "null") continue
            val home = o.optJSONObject("home_team")
            val away = o.optJSONObject("visitor_team")
            out += ResultGame(
                id = id,
                dateIso = o.optString("date", "").take(10),
                state = o.optString("status_state", "").lowercase(),
                homeName = home?.optString("full_name", "").orEmpty(),
                awayName = away?.optString("full_name", "").orEmpty(),
                homeAbbr = home?.optString("abbreviation", "").orEmpty(),
                awayAbbr = away?.optString("abbreviation", "").orEmpty(),
            )
        }
        return out
    }

    fun parseStats(body: String, fetchedUtc: Long): List<BoxScore> {
        val data = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return emptyList()
        val out = ArrayList<BoxScore>(data.length())
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            val player = o.optJSONObject("player") ?: continue
            val game = o.optJSONObject("game") ?: continue
            val first = player.optString("first_name", "").trim()
            val last = player.optString("last_name", "").trim()
            val name = "$first $last".trim()
            if (name.isEmpty()) continue
            val statId = o.opt("id")?.toString().orEmpty()
            val gameId = game.opt("id")?.toString().orEmpty()
            if (statId.isEmpty() || statId == "null" || gameId.isEmpty() || gameId == "null") continue

            val dateIso = game.optString("date", "").take(10)
            out += BoxScore(
                statId = "bdl:$statId",
                gameId = gameId,
                playedUtc = playedUtc(dateIso),
                playerName = name,
                playerKey = Names.key(name),
                teamAbbr = o.optJSONObject("team")?.optString("abbreviation", "").orEmpty(),
                points = intOf(o, "pts"),
                rebounds = intOf(o, "reb"),
                assists = intOf(o, "ast"),
                threes = intOf(o, "fg3m"),
                minutes = minutesOf(o),
            )
        }
        return out
    }

    private fun playedUtc(isoDate: String): Long =
        runCatching { LocalDate.parse(isoDate).toEpochDay() * 86_400_000L }.getOrDefault(0L)

    private fun intOf(o: JSONObject, name: String): Int = when (val value = o.opt(name)) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }

    /** The provider sends minutes as a string, and omits them for a player who did not play. */
    private fun minutesOf(o: JSONObject): Int = intOf(o, "min")
}
