package com.propslab.fantasy

import com.propslab.feed.HttpUrlConnectionFetcher
import com.propslab.feed.JsonFetcher
import com.propslab.feed.safeGet
import org.json.JSONArray
import org.json.JSONObject

/**
 * The deeper, per-player log, fetched only when a card is opened.
 *
 * The documented endpoint is
 * `site.web.api.espn.com/apis/common/v3/sports/{sport}/{league}/athletes/{id}/gamelog`.
 * It returns every stat column under a canonical `names` array, with the game metadata in a
 * separate `events` map, so the parser joins by name and by event id rather than by column
 * position. That is the same reason the row is readable for a quarterback and a receiver:
 * the labels differ, the canonical names do not.
 *
 * One request carries every season split — regular season, postseason and preseason — so the
 * panel can switch between them without another call. Regular season is the default, and an
 * empty split is returned as empty rather than borrowing another split's games.
 */
object FantasyGameLogApi {

    const val BASE = "https://site.web.api.espn.com/apis/common/v3/sports"
    const val MAX_GAMES = 12

    fun url(sport: FantasySport, athleteId: String): String =
        "$BASE/${sport.path}/athletes/${athleteId.trim()}/gamelog"

    suspend fun fetch(
        sport: FantasySport,
        athleteId: String,
        playerName: String,
        fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
    ): Result<FantasyGameLog> {
        if (athleteId.isBlank()) {
            return Result.failure(IllegalArgumentException("ESPN did not publish an athlete id"))
        }
        val response = fetcher.safeGet(url(sport, athleteId)).getOrElse { return Result.failure(it) }
        if (!response.ok) {
            return Result.failure(
                IllegalStateException("The ESPN game log returned HTTP ${response.status}"),
            )
        }
        return Result.success(parse(response.body, sport, athleteId, playerName))
    }

    fun parse(
        body: String,
        sport: FantasySport,
        athleteId: String,
        playerName: String,
    ): FantasyGameLog {
        val root = runCatching { JSONObject(body) }.getOrNull()
            ?: return FantasyGameLog(athleteId, playerName)
        val names = strings(root.optJSONArray("names"))
        val display = strings(root.optJSONArray("displayNames"))
        val meta = root.optJSONObject("events")
        val seasonTypes = root.optJSONArray("seasonTypes")
            ?: return FantasyGameLog(athleteId, playerName)

        val seasons = ArrayList<GameLogSeason>(seasonTypes.length())
        for (i in 0 until seasonTypes.length()) {
            val season = seasonTypes.optJSONObject(i) ?: continue
            val name = season.optString("displayName", "").trim()
            seasons += GameLogSeason(
                name = name,
                label = splitLabel(name),
                entries = readSeason(season, sport, names, display, meta),
            )
        }

        val regular = seasons.firstOrNull { it.label == "Regular season" && it.entries.isNotEmpty() }
        val fallback = seasons.firstOrNull { it.entries.isNotEmpty() } ?: seasons.firstOrNull()
        val chosen = regular ?: fallback
        val entries = chosen?.entries.orEmpty()
        val seasonName = chosen?.name.orEmpty()

        return FantasyGameLog(
            athleteId = athleteId,
            playerName = playerName,
            entries = entries,
            season = seasonName,
            note = if (entries.isEmpty()) {
                "ESPN returned no ${sport.label.lowercase()} games" +
                    if (seasonName.isBlank()) "." else " for $seasonName."
            } else {
                "ESPN game log · $seasonName · last ${entries.size} " +
                    "game${if (entries.size == 1) "" else "s"}."
            },
            seasons = seasons,
        )
    }

    private fun readSeason(
        season: JSONObject,
        sport: FantasySport,
        names: List<String>,
        display: List<String>,
        meta: JSONObject?,
    ): List<GameLogEntry> {
        val rows = LinkedHashMap<String, JSONArray>()
        val categories = season.optJSONArray("categories")
        if (categories != null) {
            for (c in 0 until categories.length()) {
                val events = categories.optJSONObject(c)?.optJSONArray("events") ?: continue
                collect(events, rows)
            }
        } else {
            season.optJSONArray("events")?.let { collect(it, rows) }
        }

        return rows.mapNotNull { (eventId, stats) ->
            val event = meta?.optJSONObject(eventId)
            val playedUtc = SportsNews.parseIso(event?.optString("gameDate", "").orEmpty())
            val opponent = event?.optJSONObject("opponent")?.let { team ->
                team.optString("abbreviation", "").trim()
                    .ifBlank { team.optString("displayName", "").trim() }
            }.orEmpty()
            fun value(name: String): String {
                val index = names.indexOf(name)
                return if (index < 0) "" else stats.optString(index, "").trim()
            }
            fun has(name: String): Boolean = names.contains(name)
            fun int(name: String): Int = value(name).toIntOrNull() ?: 0

            val points = when (sport) {
                FantasySport.BASKETBALL -> {
                    if (!has("points")) {
                        null
                    } else {
                        FantasyDesk.basketballScore(
                            points = int("points"),
                            rebounds = int("totalRebounds"),
                            assists = int("assists"),
                            threes = value("threePointFieldGoalsMade-threePointFieldGoalsAttempted")
                                .substringBefore('-').toIntOrNull() ?: 0,
                        )
                    }
                }
                FantasySport.FOOTBALL -> {
                    val relevant = listOf(
                        "passingYards", "passingTouchdowns", "rushingYards",
                        "rushingTouchdowns", "receptions", "receivingYards",
                        "receivingTouchdowns",
                    ).any { has(it) }
                    if (!relevant) {
                        null
                    } else {
                        FantasyDesk.footballScore(
                            passYards = int("passingYards"),
                            passTouchdowns = int("passingTouchdowns"),
                            interceptions = int("interceptions"),
                            rushYards = int("rushingYards"),
                            rushTouchdowns = int("rushingTouchdowns"),
                            receptions = int("receptions"),
                            receivingYards = int("receivingYards"),
                            receivingTouchdowns = int("receivingTouchdowns"),
                            fumblesLost = int("fumblesLost"),
                        )
                    }
                }
            }

            GameLogEntry(
                eventId = eventId,
                playedUtc = playedUtc,
                opponent = opponent,
                homeAway = event?.optString("atVs", "").orEmpty(),
                result = event?.optString("gameResult", "").orEmpty(),
                score = event?.optString("score", "").orEmpty(),
                fantasyPoints = points,
                statLine = statLine(sport, ::value, ::has, names, display, stats),
            )
        }
            .sortedByDescending { it.playedUtc }
            .take(MAX_GAMES)
    }

    private fun splitLabel(name: String): String = when {
        name.contains("post", ignoreCase = true) -> "Postseason"
        name.contains("regular", ignoreCase = true) -> "Regular season"
        name.contains("preseason", ignoreCase = true) ||
            name.contains("pre-season", ignoreCase = true) -> "Preseason"
        else -> name
    }

    private fun collect(events: JSONArray, into: MutableMap<String, JSONArray>) {
        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: continue
            val id = event.optString("eventId", "").trim()
            val stats = event.optJSONArray("stats") ?: continue
            if (id.isEmpty()) continue
            into.putIfAbsent(id, stats)
        }
    }

    /**
     * A tight line, not the whole box score: three or four numbers the card can print above
     * the fold. Football columns are grouped by their canonical names so a duplicated "YDS"
     * label can never be mistaken for the wrong phase of play.
     */
    private fun statLine(
        sport: FantasySport,
        value: (String) -> String,
        has: (String) -> Boolean,
        names: List<String>,
        display: List<String>,
        stats: JSONArray,
    ): String {
        val parts = ArrayList<String>(4)
        if (sport == FantasySport.BASKETBALL) {
            if (has("points")) parts += "${value("points")} pts"
            if (has("totalRebounds")) parts += "${value("totalRebounds")} reb"
            if (has("assists")) parts += "${value("assists")} ast"
        } else {
            val passYards = value("passingYards")
            val passTd = value("passingTouchdowns")
            if (passYards.isNotBlank() || passTd.isNotBlank()) {
                parts += "${passYards.ifBlank { "0" }} pass yds"
                if (passTd.isNotBlank()) parts += "$passTd pass TD"
            }
            val rushYards = value("rushingYards")
            val rushTd = value("rushingTouchdowns")
            if (rushYards.isNotBlank() || rushTd.isNotBlank()) {
                parts += "${rushYards.ifBlank { "0" }} rush yds"
                if (rushTd.isNotBlank()) parts += "$rushTd rush TD"
            }
            val rec = value("receptions")
            val recYards = value("receivingYards")
            val recTd = value("receivingTouchdowns")
            if (rec.isNotBlank() || recYards.isNotBlank() || recTd.isNotBlank()) {
                if (rec.isNotBlank()) parts += "$rec rec"
                if (recYards.isNotBlank()) parts += "$recYards rec yds"
                if (recTd.isNotBlank()) parts += "$recTd rec TD"
            }
        }
        if (parts.isNotEmpty()) return parts.take(4).joinToString(" · ")
        // A kicker or a defender has a log but none of the desk's scoring columns. Show the
        // first few columns ESPN published rather than inventing a fantasy total.
        return names.indices.take(4).mapNotNull { index ->
            val raw = stats.optString(index, "").trim()
            if (raw.isBlank()) null else {
                val label = display.getOrNull(index)?.takeIf { it.isNotBlank() } ?: names[index]
                "$raw $label"
            }
        }.joinToString(" · ").ifBlank { "No counting stats" }
    }

    private fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.optString(it, "").trim() }
    }
}
