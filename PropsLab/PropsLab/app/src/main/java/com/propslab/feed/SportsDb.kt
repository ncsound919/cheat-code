package com.propslab.feed

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** One game, as TheSportsDB reports it. */
data class SportsDbEvent(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeId: String,
    val awayId: String,
    /** Epoch millis in UTC; 0 means the row carried no readable time. */
    val utcMillis: Long,
    val league: String,
    val sport: String,
    /** "NS" before the start, "FT" once finished; anything else is kept exactly as sent. */
    val status: String,
    val venue: String,
) {
    val finished: Boolean
        get() = status.equals("FT", ignoreCase = true) || status.equals("AET", ignoreCase = true)
}

/** One roster row. The team comes with it so the player can be filed under a code. */
data class SportsDbPlayer(
    val id: String,
    val teamId: String,
    val name: String,
    val teamName: String,
    val position: String,
)

/**
 * TheSportsDB v1 — schedule and roster, and deliberately nothing else.
 *
 * This is the substrate the other two feeds join onto: a prediction needs a game row and a
 * player row before a price can be attached to either, and until now those rows only
 * existed as invented fixtures or as by-products of an odds sync. The endpoints below were
 * read against the live API on 2026-10-29, and the tests feed the parsers the payloads
 * that reading produced.
 *
 * Three things about the free tier are load-bearing and are handled rather than hoped away:
 *
 *  - The key is public (`123`), so it is a constant and not a setting. Nothing here is a
 *    credential, and no URL this class builds should ever reach a log line anyway.
 *  - The free tier answers a *fraction* of what it holds: the day query returned a handful
 *    of events and none of them NBA, while the season page carried the NBA games. So the
 *    caller merges reads and stops at the first that finds the window (see FeedSync).
 *  - `strTimestamp` is UTC with no marker on it: a 7:30pm tip in Los Angeles arrives as the
 *    following day's 02:30. It is read as UTC here; the Eastern-date conversion stays with
 *    [NbaCalendar], which is the only place that policy lives.
 */
class SportsDb(
    private val fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
    private val key: String = FREE_KEY,
) {

    fun dayUrl(date: LocalDate): String = url("eventsday.php?d=$date&s=$SPORT_BASKETBALL")

    fun seasonUrl(leagueId: String, season: String): String = url("eventsseason.php?id=$leagueId&s=$season")

    fun nextUrl(teamId: String): String = url("eventsnext.php?id=$teamId")

    fun playersUrl(teamId: String): String = url("lookup_all_players.php?id=$teamId")

    fun teamUrl(name: String): String = url("searchteams.php?t=${encode(name)}")

    /**
     * Reads the day's events. Both envelope keys are accepted: `eventsday` and
     * `eventsnext` answer under `events`, `eventslast` under `results`, and anything else
     * — including the provider's `"events": null` for an empty day — yields no games
     * rather than an exception, because an unreadable response must not take the board down.
     */
    fun parseEvents(body: String): List<SportsDbEvent> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val array = sequenceOf(root.optJSONArray("events"), root.optJSONArray("results"))
            .firstOrNull { it != null } ?: return emptyList()
        val out = ArrayList<SportsDbEvent>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("idEvent", "").trim()
            val home = o.optString("strHomeTeam", "").trim()
            val away = o.optString("strAwayTeam", "").trim()
            if (id.isEmpty() || home.isEmpty() || away.isEmpty()) continue
            out += SportsDbEvent(
                id = id,
                homeTeam = home,
                awayTeam = away,
                homeId = o.optString("idHomeTeam", "").trim(),
                awayId = o.optString("idAwayTeam", "").trim(),
                utcMillis = utcMillis(o),
                league = o.optString("strLeague", "").trim(),
                sport = o.optString("strSport", "").trim(),
                status = o.optString("strStatus", "").trim(),
                venue = o.optString("strVenue", "").trim(),
            )
        }
        return out
    }

    /** The roster envelope is `player`. Rows without a name or an id are dropped. */
    fun parsePlayers(body: String): List<SportsDbPlayer> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val array = root.optJSONArray("player") ?: return emptyList()
        val out = ArrayList<SportsDbPlayer>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("idPlayer", "").trim()
            val name = o.optString("strPlayer", "").trim()
            if (id.isEmpty() || name.isEmpty()) continue
            out += SportsDbPlayer(
                id = id,
                teamId = o.optString("idTeam", "").trim(),
                name = name,
                teamName = o.optString("strTeam", "").trim(),
                position = o.optString("strPosition", "").trim(),
            )
        }
        return out
    }

    suspend fun day(date: LocalDate): Result<List<SportsDbEvent>> = events(dayUrl(date))

    suspend fun season(leagueId: String, season: String): Result<List<SportsDbEvent>> =
        events(seasonUrl(leagueId, season))

    suspend fun next(teamId: String): Result<List<SportsDbEvent>> = events(nextUrl(teamId))

    suspend fun players(teamId: String): Result<List<SportsDbPlayer>> {
        val result = fetcher.safeGet(playersUrl(teamId)).getOrElse {
            return Result.failure(IllegalStateException("TheSportsDB could not be reached."))
        }
        if (!result.ok) return Result.failure(IllegalStateException(FeedFailure.of(result, "TheSportsDB").message))
        return Result.success(parsePlayers(result.body))
    }

    private suspend fun events(url: String): Result<List<SportsDbEvent>> {
        val result = fetcher.safeGet(url).getOrElse {
            // A raw socket exception can carry the URL, and this URL carries the key, so the
            // caller gets a sentence instead — the same rule FeedFailure follows.
            return Result.failure(IllegalStateException("TheSportsDB could not be reached."))
        }
        if (!result.ok) return Result.failure(IllegalStateException(FeedFailure.of(result, "TheSportsDB").message))
        return Result.success(parseEvents(result.body))
    }

    /**
     * The provider stamps events in UTC without saying so: a 7:30pm tip in Los Angeles
     * arrives as the next day's 02:30, so reading it as local time would place every game
     * hours off. `dateEvent` + `strTime` is the fallback when the stamp is missing, and a
     * row with neither returns 0 — which drops it from every window rather than landing it
     * at the epoch.
     */
    private fun utcMillis(o: JSONObject): Long {
        val stamp = o.optString("strTimestamp", "").trim()
        if (stamp.isNotBlank()) {
            runCatching { LocalDateTime.parse(stamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli() }
                .getOrNull()?.let { return it }
        }
        val date = o.optString("dateEvent", "").trim()
        if (date.isEmpty()) return 0L
        val time = o.optString("strTime", "").trim().ifBlank { "00:00:00" }
        return runCatching {
            LocalDateTime.parse("$date $time", DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrDefault(0L)
    }

    private fun url(path: String): String = "$BASE/$key/$path"

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        /** The published free key. It is not a secret and is not stored as a preference. */
        const val FREE_KEY = "123"
        const val BASE = "https://www.thesportsdb.com/api/v1/json"
        const val NBA = "NBA"
        const val NBA_LEAGUE_ID = "4387"

        private const val SPORT_BASKETBALL = "Basketball"
        private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /** Only the league this board plays in; the day query returns every basketball league. */
        fun nba(events: List<SportsDbEvent>): List<SportsDbEvent> = events.filter { it.league == NBA }

        /**
         * Inside the window, oldest first. A row whose time could not be read (utc = 0)
         * falls outside every real window, which is the point: an unschedulable game is
         * not written.
         */
        fun between(events: List<SportsDbEvent>, fromUtc: Long, toUtc: Long): List<SportsDbEvent> =
            events.filter { it.utcMillis in fromUtc..toUtc }.sortedBy { it.utcMillis }

        /**
         * The label the season page wants: the NBA's year crosses the calendar, so a game
         * in November belongs to "2026-2027" and one in June to "2025-2026".
         */
        fun seasonFor(date: LocalDate): String =
            if (date.monthValue >= 9) "${date.year}-${date.year + 1}" else "${date.year - 1}-${date.year}"
    }
}
