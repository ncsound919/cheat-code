package com.propslab.fantasy

import com.propslab.availability.LeagueInjuries
import com.propslab.feed.EspnApi
import com.propslab.feed.HttpUrlConnectionFetcher
import com.propslab.feed.JsonFetcher
import com.propslab.feed.Names
import com.propslab.feed.ResultGame
import com.propslab.feed.safeGet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** One headline from a public news feed. Text only: no remote images, no article scraping. */
data class NewsItem(
    val id: String,
    val league: String,
    val headline: String,
    val summary: String,
    val url: String,
    val publishedUtc: Long,
    val source: String = "ESPN",
)

data class NewsFeed(
    val items: List<NewsItem> = emptyList(),
    val note: String = "",
    val fetchedUtc: Long = 0L,
)

/** One refresh of the desk: the player lines, the league report, and what was read. */
data class FantasyFeedResult(
    val lines: List<FantasyLine> = emptyList(),
    val injuries: List<FantasyInjury> = emptyList(),
    val note: String = "",
    val fetchedUtc: Long = 0L,
    val ok: Boolean = false,
)

/**
 * General sports news from ESPN's keyless public endpoints.
 *
 * One league is one request, all of them are read in parallel, and a league that fails does
 * not take the others down. The feed is text and a link out: the app never mirrors an
 * article, and the card says where the story came from.
 */
object SportsNews {

    data class League(val label: String, val path: String)

    val NFL = League("NFL", "football/nfl")
    val NBA = League("NBA", "basketball/nba")
    val MLB = League("MLB", "baseball/mlb")
    val NHL = League("NHL", "hockey/nhl")

    val GENERAL = listOf(NFL, NBA, MLB, NHL)

    fun url(path: String, limit: Int = 8): String =
        "https://site.api.espn.com/apis/site/v2/sports/$path/news?limit=${limit.coerceIn(1, 25)}"

    fun parse(body: String, league: String): List<NewsItem> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val articles = root.optJSONArray("articles") ?: return emptyList()
        val out = ArrayList<NewsItem>(articles.length())
        for (i in 0 until articles.length()) {
            val article = articles.optJSONObject(i) ?: continue
            val headline = article.optString("headline", "").trim()
            if (headline.isEmpty()) continue
            val id = article.optString("id", "").trim()
                .ifBlank { article.optString("nowId", "").trim() }
                .ifBlank { headline }
            val links = article.optJSONObject("links")
            val url = links?.optJSONObject("web")?.optString("href", "").orEmpty().trim()
                .ifBlank { links?.optJSONObject("mobile")?.optString("href", "").orEmpty().trim() }
            out += NewsItem(
                id = id,
                league = league,
                headline = headline,
                summary = article.optString("description", "").trim()
                    .replace(Regex("\\s+"), " "),
                url = url,
                publishedUtc = parseIso(article.optString("published", ""))
                    .takeIf { it > 0L }
                    ?: parseIso(article.optString("lastModified", "")),
            )
        }
        return out
    }

    suspend fun fetch(
        fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
        leagues: List<League> = GENERAL,
        nowUtc: Long = System.currentTimeMillis(),
    ): NewsFeed {
        val reads = coroutineScope {
            leagues.map { league ->
                async {
                    val response = fetcher.safeGet(url(league.path)).getOrNull()
                    when {
                        response == null -> league to null
                        !response.ok -> league to null
                        else -> league to parse(response.body, league.label)
                    }
                }
            }.awaitAll()
        }
        val items = reads.flatMap { it.second.orEmpty() }
            .distinctBy { it.headline.lowercase() }
            .sortedByDescending { it.publishedUtc }
            .take(40)
        val failed = reads.filter { it.second == null }.map { it.first.label }
        val note = buildString {
            append("${leagues.size - failed.size} of ${leagues.size} leagues read")
            append(", ${items.size} headline${if (items.size == 1) "" else "s"}")
            if (failed.isNotEmpty()) append(". No answer from ${failed.joinToString(", ")}")
            append('.')
        }
        return NewsFeed(items = items, note = note, fetchedUtc = nowUtc)
    }

    /**
     * ISO-8601 from ESPN. The injury wire omits seconds (`01:23Z`) while the news wire does
     * not, so both forms are read. A missing or unreadable stamp is zero, never now.
     */
    fun parseIso(raw: String): Long {
        val text = raw.trim()
        if (text.isEmpty()) return 0L
        return runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(text).toEpochMilli() }
            .getOrDefault(0L)
    }
}

/**
 * The desk's data source: ESPN's public box scores and the league's own injury report.
 *
 * Nothing here needs a key or an account, which is why the fantasy desk can exist as a paid
 * convenience without a per-user feed bill. It reads only finished games, claims only the
 * stat columns it actually parses, and reports every partial failure rather than showing an
 * empty desk as if nothing had happened.
 */
object FantasyFeedApi {

    const val MAX_GAMES = 8
    private const val NFL_BASE = "https://site.api.espn.com/apis/site/v2/sports/football/nfl"

    fun nflScoreboardUrl(isoDate: String): String =
        "$NFL_BASE/scoreboard?dates=${isoDate.replace("-", "")}"

    fun nflSummaryUrl(eventId: String): String = "$NFL_BASE/summary?event=$eventId"

    suspend fun fetch(
        sport: FantasySport,
        fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
        nowUtc: Long = System.currentTimeMillis(),
    ): FantasyFeedResult = coroutineScope {
        val games = recentFinalGames(sport, fetcher, nowUtc)
        val summaries = games.map { game ->
            async {
                val url = if (sport == FantasySport.FOOTBALL) {
                    nflSummaryUrl(game.id)
                } else {
                    EspnApi.summaryUrl(game.id)
                }
                val response = fetcher.safeGet(url).getOrNull()
                if (response == null || !response.ok) null else response.body
            }
        }.awaitAll()

        val lines = ArrayList<FantasyLine>()
        var unread = 0
        summaries.forEachIndexed { index, body ->
            if (body == null) {
                unread++
                return@forEachIndexed
            }
            lines += if (sport == FantasySport.FOOTBALL) {
                parseNflBoxScore(body, games[index].id, nowUtc)
            } else {
                parseBasketballBoxScore(body, games[index].id, nowUtc)
            }
        }
        // The injury designations ride inside the summary payload for both clubs. Reading
        // them here avoids the league-wide endpoint, which is several megabytes for the NFL.
        val injuries = summaries.filterNotNull()
            .flatMap { parseInjuries(it, nowUtc) }
            .groupBy { it.playerKey }
            .mapNotNull { (_, rows) -> rows.maxByOrNull { it.observedUtc } }
            .sortedByDescending { it.observedUtc }

        val note = buildString {
            if (games.isEmpty()) {
                append(
                    if (sport == FantasySport.FOOTBALL) {
                        "No final NFL games were found in the last seven days."
                    } else {
                        "No final NBA games were found for today or yesterday."
                    },
                )
            } else {
                append("Read ${games.size} final game${if (games.size == 1) "" else "s"}")
                append(" and ${lines.size} player line${if (lines.size == 1) "" else "s"}.")
            }
            if (unread > 0) append(" $unread game summar${if (unread == 1) "y" else "ies"} could not be read.")
            append(
                if (injuries.isEmpty()) {
                    " The game summaries carried no injury designations."
                } else {
                    " ${injuries.size} designation${if (injuries.size == 1) "" else "s"} in the game summaries."
                },
            )
        }
        FantasyFeedResult(
            lines = lines,
            injuries = injuries,
            note = note,
            fetchedUtc = nowUtc,
            ok = games.isNotEmpty(),
        )
    }

    /**
     * The most recent finished slate. Basketball looks at today and yesterday; football walks
     * back a week because a Tuesday or Wednesday has no games and the desk should still show
     * the weekend's lines.
     */
    private suspend fun recentFinalGames(
        sport: FantasySport,
        fetcher: JsonFetcher,
        nowUtc: Long,
    ): List<ResultGame> {
        val zone = ZoneId.of("America/New_York")
        val today = Instant.ofEpochMilli(nowUtc).atZone(zone).toLocalDate()
        if (sport == FantasySport.BASKETBALL) {
            val dates = listOf(today, today.minusDays(1))
            return dates.flatMap { date ->
                scoreboard(sport, fetcher, date).filter { it.isFinal }
            }.distinctBy { it.id }.take(MAX_GAMES)
        }
        for (offset in 0L..7L) {
            val date = today.minusDays(offset)
            val finals = scoreboard(sport, fetcher, date).filter { it.isFinal }
            if (finals.isNotEmpty()) return finals.take(MAX_GAMES)
        }
        return emptyList()
    }

    private suspend fun scoreboard(
        sport: FantasySport,
        fetcher: JsonFetcher,
        date: LocalDate,
    ): List<ResultGame> {
        val url = if (sport == FantasySport.FOOTBALL) {
            nflScoreboardUrl(date.toString())
        } else {
            EspnApi.scoreboardUrl(date.toString())
        }
        val response = fetcher.safeGet(url).getOrNull() ?: return emptyList()
        if (!response.ok) return emptyList()
        return EspnApi.parseGames(response.body, date.toString())
    }

    /**
     * Reads the injury blocks carried by a game summary. The shape is the same for both
     * sports: a team wrapper, then one row per player with a status and a short comment.
     */
    fun parseInjuries(body: String, nowUtc: Long = System.currentTimeMillis()): List<FantasyInjury> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val teams = root.optJSONArray("injuries") ?: return emptyList()
        val out = ArrayList<FantasyInjury>()
        for (t in 0 until teams.length()) {
            val block = teams.optJSONObject(t) ?: continue
            val team = block.optJSONObject("team")?.optString("abbreviation", "").orEmpty()
            val rows = block.optJSONArray("injuries") ?: continue
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val name = row.optJSONObject("athlete")?.optString("displayName", "").orEmpty().trim()
                if (name.isEmpty()) continue
                val status = LeagueInjuries.readStatus(row.optString("status", "")) ?: continue
                out += FantasyInjury(
                    playerName = name,
                    playerKey = Names.key(name),
                    team = team,
                    status = status,
                    note = row.optString("shortComment", "").trim()
                        .replace(Regex("\\s+"), " "),
                    observedUtc = SportsNews.parseIso(row.optString("date", ""))
                        .takeIf { it > 0L } ?: nowUtc,
                )
            }
        }
        return out
    }

    /** Reuses the app's existing NBA box-score parser, so there is one reading of that payload. */
    fun parseBasketballBoxScore(body: String, gameId: String, fetchedUtc: Long): List<FantasyLine> =
        EspnApi.parseBoxScore(body, gameId, fetchedUtc).map { row ->
            FantasyLine(
                playerKey = row.playerKey,
                playerName = row.playerName,
                team = row.teamAbbr,
                playedUtc = row.playedUtc,
                fantasyPoints = FantasyDesk.basketballScore(
                    row.points, row.rebounds, row.assists, row.threes,
                ),
                statLine = "${row.points} pts · ${row.rebounds} reb · ${row.assists} ast · " +
                    "${row.threes} 3PM",
                role = "",
                minutes = row.minutes,
                athleteId = row.athleteId,
            )
        }

    /**
     * NFL box scores do not ship the stat-column names that the NBA payload carries, so the
     * columns are read by the order ESPN has used for these categories: passing (C/ATT, YDS,
     * AVG, TD, INT, …), rushing (CAR, YDS, AVG, TD, …), receiving (REC, YDS, AVG, TD, …).
     * A row shorter than the columns being read is skipped rather than guessed at.
     */
    fun parseNflBoxScore(body: String, gameId: String, fetchedUtc: Long): List<FantasyLine> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val played = headerDate(root).takeIf { it > 0L } ?: fetchedUtc
        val teams = root.optJSONObject("boxscore")?.optJSONArray("players") ?: return emptyList()
        val acc = LinkedHashMap<String, LineAcc>()
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
                    val athlete = row.optJSONObject("athlete") ?: continue
                    val playerName = athlete.optString("displayName", "").trim()
                    if (playerName.isEmpty()) continue
                    val key = Names.key(playerName)
                    val stats = row.optJSONArray("stats") ?: continue
                    val athleteId = athlete.optString("id", "").trim()
                    val line = acc.getOrPut(key) { LineAcc(key, playerName, abbr, athleteId) }
                    when {
                        name.startsWith("passing") && stats.length() >= 5 -> {
                            line.passYards = intAt(stats, 1)
                            line.passTouchdowns = intAt(stats, 3)
                            line.interceptions = intAt(stats, 4)
                            line.role = "QB"
                        }
                        name.startsWith("rushing") && stats.length() >= 4 -> {
                            line.rushYards = intAt(stats, 1)
                            line.rushTouchdowns = intAt(stats, 3)
                            if (line.role.isEmpty()) line.role = "RB"
                        }
                        name.startsWith("receiving") && stats.length() >= 4 -> {
                            line.receptions = intAt(stats, 0)
                            line.receivingYards = intAt(stats, 1)
                            line.receivingTouchdowns = intAt(stats, 3)
                            if (line.role.isEmpty()) line.role = "Receiver"
                        }
                    }
                }
            }
        }
        return acc.values
            .filter { it.hasLine() }
            .map { line ->
                val score = FantasyDesk.footballScore(
                    passYards = line.passYards,
                    passTouchdowns = line.passTouchdowns,
                    interceptions = line.interceptions,
                    rushYards = line.rushYards,
                    rushTouchdowns = line.rushTouchdowns,
                    receptions = line.receptions,
                    receivingYards = line.receivingYards,
                    receivingTouchdowns = line.receivingTouchdowns,
                )
                FantasyLine(
                    playerKey = line.key,
                    playerName = line.name,
                    team = line.team,
                    playedUtc = played,
                    fantasyPoints = score,
                    statLine = line.statLine(),
                    role = line.role,
                    touchdownPoints = FantasyDesk.footballTouchdownPoints(
                        line.passTouchdowns, line.rushTouchdowns, line.receivingTouchdowns,
                    ),
                    athleteId = line.athleteId,
                )
            }
            .filter { it.fantasyPoints > 0.0 }
    }

    private data class LineAcc(
        val key: String,
        val name: String,
        val team: String,
        val athleteId: String,
        var passYards: Int = 0,
        var passTouchdowns: Int = 0,
        var interceptions: Int = 0,
        var rushYards: Int = 0,
        var rushTouchdowns: Int = 0,
        var receptions: Int = 0,
        var receivingYards: Int = 0,
        var receivingTouchdowns: Int = 0,
        var role: String = "",
    ) {
        fun hasLine(): Boolean =
            passYards > 0 || passTouchdowns > 0 || rushYards > 0 || rushTouchdowns > 0 ||
                receptions > 0 || receivingYards > 0 || receivingTouchdowns > 0

        /** The most informative line, never every column: the card is a summary, not a box score. */
        fun statLine(): String = buildList {
            if (passYards > 0 || passTouchdowns > 0) {
                add("$passYards pass yds")
                if (passTouchdowns > 0) add("$passTouchdowns pass TD")
            }
            if (rushYards > 0 || rushTouchdowns > 0) {
                add("$rushYards rush yds")
                if (rushTouchdowns > 0) add("$rushTouchdowns rush TD")
            }
            if (receptions > 0 || receivingYards > 0 || receivingTouchdowns > 0) {
                add("$receptions rec")
                add("$receivingYards rec yds")
                if (receivingTouchdowns > 0) add("$receivingTouchdowns rec TD")
            }
        }.take(4).joinToString(" · ").ifEmpty { "no counting stats" }
    }

    private fun intAt(values: JSONArray, index: Int): Int {
        if (index >= values.length()) return 0
        return values.optString(index, "").trim().substringBefore(':').toIntOrNull()
            ?: values.optString(index, "").trim().substringBefore('-').toIntOrNull()
            ?: 0
    }

    private fun headerDate(root: JSONObject): Long {
        val date = root.optJSONObject("header")?.optJSONArray("competitions")
            ?.optJSONObject(0)?.optString("date", "").orEmpty()
        return SportsNews.parseIso(date)
    }
}
