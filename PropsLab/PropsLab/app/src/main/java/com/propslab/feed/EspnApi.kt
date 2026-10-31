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

    const val BASKETBALL = "basketball/nba"
    const val FOOTBALL = "football/nfl"
    const val BASE = "https://site.api.espn.com/apis/site/v2/sports/$BASKETBALL"

    /** The sport path for a stored sport, so one parser reads both leagues' payloads. */
    fun pathFor(sport: String): String =
        if (sport == com.propslab.domain.Sports.FOOTBALL) FOOTBALL else BASKETBALL

    fun baseFor(sport: String): String =
        "https://site.api.espn.com/apis/site/v2/sports/${pathFor(sport)}"

    fun scoreboardUrl(isoDate: String, sport: String = com.propslab.domain.Sports.BASKETBALL): String =
        "${baseFor(sport)}/scoreboard?dates=${isoDate.replace("-", "")}"

    fun summaryUrl(eventId: String, sport: String = com.propslab.domain.Sports.BASKETBALL): String =
        "${baseFor(sport)}/summary?event=$eventId"

    fun parseGames(
        body: String,
        dateIso: String,
        sport: String = com.propslab.domain.Sports.BASKETBALL,
    ): List<ResultGame> {
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
                sport = sport,
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

    /**
     * The NFL summary, reduced to the four columns a football prop can be written on.
     *
     * ESPN's football box score has no `names` array, so the columns are read by the order
     * the payload has used for each category: passing (C/ATT, YDS, AVG, TD, INT, …), rushing
     * (CAR, YDS, AVG, TD, …) and receiving (REC, YDS, AVG, TD, …). A row that does not carry
     * the columns being read is skipped; a category row shorter than the columns is not
     * turned into a zero, because a missing column is not a zero stat.
     */
    fun parseFootballBoxScore(body: String, gameId: String, fetchedUtc: Long): List<BoxScore> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val played = headerDate(root)?.let { OddsApi.isoUtc(it) }?.takeIf { it > 0L } ?: fetchedUtc
        val teams = root.optJSONObject("boxscore")?.optJSONArray("players") ?: return emptyList()
        val acc = LinkedHashMap<String, FootballAcc>()
        for (t in 0 until teams.length()) {
            val team = teams.optJSONObject(t) ?: continue
            val abbr = team.optJSONObject("team")?.optString("abbreviation", "").orEmpty()
            val categories = team.optJSONArray("statistics") ?: continue
            for (c in 0 until categories.length()) {
                val category = categories.optJSONObject(c) ?: continue
                val name = category.optString("name", "").lowercase()
                val athletes = category.optJSONArray("athletes") ?: continue
                for (a in 0 until athletes.length()) {
                    val row = athletes.optJSONObject(a) ?: continue
                    if (row.optBoolean("didNotPlay", false)) continue
                    val athlete = row.optJSONObject("athlete") ?: continue
                    val playerName = athlete.optString("displayName", "").trim()
                    if (playerName.isEmpty()) continue
                    val stats = row.optJSONArray("stats") ?: continue
                    val line = acc.getOrPut(Names.key(playerName)) {
                        FootballAcc(playerName, abbr, athlete.optString("id", "").trim())
                    }
                    when {
                        name.startsWith("passing") && stats.length() >= 5 ->
                            line.passYards = intAt(stats, 1)
                        name.startsWith("rushing") && stats.length() >= 4 ->
                            line.rushYards = intAt(stats, 1)
                        name.startsWith("receiving") && stats.length() >= 4 -> {
                            line.receptions = intAt(stats, 0)
                            line.receivingYards = intAt(stats, 1)
                        }
                    }
                }
            }
        }
        return acc.values
            .filter { it.hasLine() }
            .map { line ->
                BoxScore(
                    statId = "espn-nfl-$gameId-${Names.key(line.name)}",
                    gameId = gameId,
                    playedUtc = played,
                    playerName = line.name,
                    playerKey = Names.key(line.name),
                    teamAbbr = line.team,
                    points = 0,
                    rebounds = 0,
                    assists = 0,
                    threes = 0,
                    minutes = 0,
                    athleteId = line.athleteId,
                    passYards = line.passYards,
                    rushYards = line.rushYards,
                    receivingYards = line.receivingYards,
                    receptions = line.receptions,
                    sport = com.propslab.domain.Sports.FOOTBALL,
                )
            }
    }

    private data class FootballAcc(
        val name: String,
        val team: String,
        val athleteId: String,
        var passYards: Int = 0,
        var rushYards: Int = 0,
        var receivingYards: Int = 0,
        var receptions: Int = 0,
    ) {
        fun hasLine(): Boolean =
            passYards > 0 || rushYards > 0 || receivingYards > 0 || receptions > 0
    }

    /** ESPN writes football stats as plain strings, and marks "--" for a category a player did not record. */
    private fun intAt(values: JSONArray, index: Int): Int {
        if (index >= values.length()) return 0
        return values.optString(index, "").trim().substringBefore(':').toIntOrNull()
            ?: values.optString(index, "").trim().substringBefore('-').toIntOrNull()
            ?: 0
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
