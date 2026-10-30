package com.propslab.feed

import org.json.JSONArray
import org.json.JSONObject

/**
 * Box scores from ESPN's public scoreboard and summary feeds.
 *
 * BALLDONTLIE is still supported, but its per-player stats need a paid entitlement and the
 * free key in hand is rejected. ESPN's public JSON needs no key, carries every player's
 * line, and is the source that makes the real board priceable on day one. It is read-only
 * and unauthenticated; the app never sends it anything.
 *
 * The parser only claims what the payload states: a player with `didNotPlay` is skipped, a
 * missing stat column is skipped, and a game that is not final is not returned as one.
 */
object EspnApi {

    const val BASE = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba"

    fun scoreboardUrl(isoDate: String): String = "$BASE/scoreboard?dates=${isoDate.replace("-", "")}"

    fun summaryUrl(eventId: String): String = "$BASE/summary?event=$eventId"

    fun parseGames(body: String, dateIso: String): List<ResultGame> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val events = root.optJSONArray("events") ?: return emptyList()
        val out = ArrayList<ResultGame>(events.length())
        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: continue
            val id = event.optString("id", "").trim()
            if (id.isEmpty()) continue
            val competition = event.optJSONArray("competitions")?.optJSONObject(0)
            val competitors = competition?.optJSONArray("competitors") ?: continue
            var home: JSONObject? = null
            var away: JSONObject? = null
            for (c in 0 until competitors.length()) {
                val team = competitors.optJSONObject(c) ?: continue
                when (team.optString("homeAway", "")) {
                    "home" -> home = team
                    "away" -> away = team
                }
            }
            val homeTeam = home?.optJSONObject("team")
            val awayTeam = away?.optJSONObject("team")
            if (homeTeam == null || awayTeam == null) continue
            val completed = competition.optJSONObject("status")
                ?.optJSONObject("type")
                ?.optBoolean("completed", false)
                ?: false
            out += ResultGame(
                id = id,
                dateIso = dateIso,
                state = if (completed) "final" else "scheduled",
                homeName = homeTeam.optString("displayName", homeTeam.optString("name", "")),
                awayName = awayTeam.optString("displayName", awayTeam.optString("name", "")),
                homeAbbr = homeTeam.optString("abbreviation", ""),
                awayAbbr = awayTeam.optString("abbreviation", ""),
            )
        }
        return out
    }

    /**
     * Every player's line in one game, reduced to the four counting stats the app prices.
     * `playedUtc` comes from the game's own header, so a late tip-off is settled on the day
     * it was played rather than the day it was read.
     */
    fun parseBoxScore(body: String, gameId: String, fetchedUtc: Long): List<BoxScore> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val played = headerDate(root)?.let { OddsApi.isoUtc(it) }?.takeIf { it > 0L } ?: fetchedUtc
        val players = root.optJSONObject("boxscore")?.optJSONArray("players") ?: return emptyList()
        val out = ArrayList<BoxScore>()
        for (p in 0 until players.length()) {
            val team = players.optJSONObject(p) ?: continue
            val abbr = team.optJSONObject("team")?.optString("abbreviation", "").orEmpty()
            val stats = team.optJSONArray("statistics") ?: continue
            for (s in 0 until stats.length()) {
                val block = stats.optJSONObject(s) ?: continue
                val names = block.optJSONArray("names") ?: continue
                val index = HashMap<String, Int>(names.length())
                for (n in 0 until names.length()) index[names.optString(n, "")] = n
                val athletes = block.optJSONArray("athletes") ?: continue
                for (a in 0 until athletes.length()) {
                    val row = athletes.optJSONObject(a) ?: continue
                    if (row.optBoolean("didNotPlay", false)) continue
                    val athlete = row.optJSONObject("athlete")
                    val name = athlete?.optString("displayName", "")?.trim().orEmpty()
                    if (name.isEmpty()) continue
                    val athleteId = athlete?.optString("id", "").orEmpty().trim()
                    val values = row.optJSONArray("stats") ?: continue
                    fun stat(key: String): Int? {
                        val at = index[key] ?: return null
                        val raw = values.optString(at, "").trim()
                        return raw.substringBefore(':').toIntOrNull()
                    }
                    val points = stat("PTS") ?: continue
                    val rebounds = stat("REB") ?: 0
                    val assists = stat("AST") ?: 0
                    // The three-point column is "made-attempted"; only the makes are a prop.
                    val threes = index["3PT"]?.let { at ->
                        values.optString(at, "").substringBefore('-').toIntOrNull()
                    } ?: 0
                    val minutes = stat("MIN") ?: 0
                    if (minutes <= 0) continue
                    out += BoxScore(
                        statId = "espn-$gameId-${Names.key(name)}",
                        gameId = gameId,
                        playedUtc = played,
                        playerName = name,
                        playerKey = Names.key(name),
                        teamAbbr = abbr,
                        points = points,
                        rebounds = rebounds,
                        assists = assists,
                        threes = threes,
                        minutes = minutes,
                        athleteId = athleteId,
                    )
                }
            }
        }
        return out
    }

    private fun headerDate(root: JSONObject): String? {
        val competitions = root.optJSONObject("header")?.optJSONArray("competitions") ?: return null
        val first = competitions.optJSONObject(0) ?: return null
        val direct = first.optString("date", "").trim()
        if (direct.isNotEmpty()) return direct
        val competitors = first.optJSONArray("competitors") ?: return null
        for (i in 0 until competitors.length()) {
            val date = competitors.optJSONObject(i)?.optString("date", "").orEmpty()
            if (date.isNotBlank()) return date
        }
        return null
    }

    /** The player names in one summary payload, for tests and diagnostics. */
    fun namesIn(body: String): List<String> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val players: JSONArray = root.optJSONObject("boxscore")?.optJSONArray("players") ?: return emptyList()
        val out = ArrayList<String>()
        for (i in 0 until players.length()) {
            val stats = players.optJSONObject(i)?.optJSONArray("statistics") ?: continue
            for (s in 0 until stats.length()) {
                val athletes = stats.optJSONObject(s)?.optJSONArray("athletes") ?: continue
                for (a in 0 until athletes.length()) {
                    athletes.optJSONObject(a)?.optJSONObject("athlete")
                        ?.optString("displayName", "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { out += it }
                }
            }
        }
        return out
    }
}
