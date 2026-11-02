package com.propslab.fantasy

import com.propslab.data.AvailabilityStatus
import com.propslab.feed.Names
import kotlin.math.max
import kotlin.math.sqrt

/** The two sports the desk tracks. Both read ESPN's keyless public feeds. */
enum class FantasySport(val label: String, val path: String, val slug: String) {
    BASKETBALL("Basketball", "basketball/nba", "nba"),
    FOOTBALL("Football", "football/nfl", "nfl");

    companion object {
        fun fromSlug(slug: String): FantasySport? = entries.firstOrNull { it.slug == slug }
    }
}

/**
 * One player's line in one finished game, already reduced to fantasy points.
 *
 * The desk stores nothing: a line exists only for the refresh that read it. That keeps the
 * fantasy surface from becoming a second, quietly diverging copy of the box-score feed.
 */
data class FantasyLine(
    val playerKey: String,
    val playerName: String,
    val team: String,
    val playedUtc: Long,
    val fantasyPoints: Double,
    val statLine: String,
    val role: String,
    /** Points that came from touchdowns, so the desk can warn about touchdown dependence. */
    val touchdownPoints: Double = 0.0,
    val minutes: Int? = null,
    /** ESPN's athlete id, used only when the user asks for the deeper game log. */
    val athleteId: String = "",
)

/** A designation from the league's own injury report. */
data class FantasyInjury(
    val playerName: String,
    val playerKey: String,
    val team: String,
    val status: String,
    val note: String,
    val observedUtc: Long,
)

/**
 * A player as the desk ranks him. The projection is a weighted recent average rather than a
 * fitted model: four weighted games is what a keyless box-score feed can honestly support,
 * and the card says how many games it used.
 */
data class FantasyPlayer(
    val key: String,
    val name: String,
    val team: String,
    val sport: FantasySport,
    /** Newest first. */
    val lines: List<FantasyLine>,
    val injury: FantasyInjury? = null,
    val role: String = "",
) {
    val games: Int = lines.size

    private val recent: List<Double> = lines.take(8).map { it.fantasyPoints }

    val average: Double = if (recent.isEmpty()) 0.0 else recent.sum() / recent.size

    /** Newest games weigh most: 1.0, 0.85, 0.7, 0.55, then the rest at 0.4. */
    val projection: Double = if (recent.isEmpty()) {
        0.0
    } else {
        var weighted = 0.0
        var weight = 0.0
        recent.forEachIndexed { index, value ->
            val w = when (index) {
                0 -> 1.0
                1 -> 0.85
                2 -> 0.7
                3 -> 0.55
                else -> 0.4
            }
            weighted += value * w
            weight += w
        }
        weighted / weight
    }

    /** The last three games against the three before them; zero when that comparison is thin. */
    val trend: Double = run {
        val last = lines.take(3).map { it.fantasyPoints }
        val before = lines.drop(3).take(3).map { it.fantasyPoints }
        if (last.isEmpty() || before.isEmpty()) 0.0 else last.average() - before.average()
    }

    val floor: Double = recent.minOrNull() ?: 0.0
    val ceiling: Double = recent.maxOrNull() ?: 0.0

    /** Standard deviation as a share of the average: a boom-bust flag, not a probability. */
    val volatility: Double = run {
        if (recent.size < 2 || average <= 0.0) return@run 0.0
        val variance = recent.sumOf { (it - average) * (it - average) } / recent.size
        sqrt(variance) / average
    }

    /** The share of points that arrived via touchdowns, for football only. */
    val touchdownShare: Double = run {
        if (sport != FantasySport.FOOTBALL || projection <= 0.0) return@run 0.0
        val td = lines.take(4).sumOf { it.touchdownPoints }
        val total = lines.take(4).sumOf { it.fantasyPoints }
        if (total <= 0.0) 0.0 else (td / total).coerceIn(0.0, 1.0)
    }
}

enum class FantasyCall(val label: String) {
    START("Start"),
    WATCH("Watch"),
    SIT("Sit"),
    OUT("Out"),
}

data class FantasyAdvice(
    val call: FantasyCall,
    val headline: String,
    val tips: List<String>,
)

/** One row of ESPN's athlete game log, reduced to what the desk shows. */
data class GameLogEntry(
    val eventId: String,
    val playedUtc: Long,
    val opponent: String,
    /** "vs" or "@", as ESPN prints it. */
    val homeAway: String,
    val result: String,
    val score: String,
    val fantasyPoints: Double?,
    val statLine: String,
)

/** One season split in ESPN's gamelog: regular season, postseason, or preseason. */
data class GameLogSeason(
    val name: String,
    val label: String,
    val entries: List<GameLogEntry> = emptyList(),
) {
    /** Rows with a recognised fantasy score; a kicker's log can have games but no score. */
    val scoredGames: Int get() = entries.count { it.fantasyPoints != null }

    /** Mean of the scorable rows only, so an unscored row cannot drag the average down. */
    val averagePoints: Double?
        get() = entries.mapNotNull { it.fantasyPoints }.takeIf { it.isNotEmpty() }?.average()

    val bestPoints: Double?
        get() = entries.mapNotNull { it.fantasyPoints }.maxOrNull()

    /** How this split's scorable games fell against one line. */
    fun hitRate(line: Double): SplitHitRate = lineHitRate(entries, line)
}

/**
 * How a set of games fell against a line. A push is kept separate rather than counted as a
 * loss, and the shares use decided games as the denominator, so a push cannot quietly lower
 * a hit rate.
 */
data class SplitHitRate(
    val over: Int,
    val under: Int,
    val push: Int,
    val scored: Int,
) {
    /** Games that finished strictly above or below the line. */
    val decided: Int get() = over + under

    val overShare: Double? get() = if (decided == 0) null else over.toDouble() / decided
    val underShare: Double? get() = if (decided == 0) null else under.toDouble() / decided
}

fun lineHitRate(entries: List<GameLogEntry>, line: Double): SplitHitRate {
    var over = 0
    var under = 0
    var push = 0
    var scored = 0
    entries.forEach { entry ->
        val points = entry.fantasyPoints ?: return@forEach
        scored++
        when {
            points > line -> over++
            points < line -> under++
            else -> push++
        }
    }
    return SplitHitRate(over = over, under = under, push = push, scored = scored)
}

/** The deeper log a card fetches only when it is opened. */
data class FantasyGameLog(
    val athleteId: String,
    val playerName: String,
    val entries: List<GameLogEntry> = emptyList(),
    val season: String = "",
    val note: String = "",
    /** Every split ESPN returned, so the panel can switch without another request. */
    val seasons: List<GameLogSeason> = emptyList(),
) {
    /** The games in one split; an unknown split yields an empty list, never another season's. */
    fun entriesFor(seasonName: String?): List<GameLogEntry> =
        if (seasonName == null) entries
        else seasons.firstOrNull { it.name == seasonName }?.entries.orEmpty()
}

/** Everything one refresh of the desk produced. */
data class FantasyDeskUi(
    val sport: FantasySport = FantasySport.BASKETBALL,
    val tracked: List<FantasyPlayer> = emptyList(),
    /** Top form that is not already on the watchlist. */
    val trending: List<FantasyPlayer> = emptyList(),
    val injuries: List<FantasyInjury> = emptyList(),
    val lineupTips: List<String> = emptyList(),
    val fetchedUtc: Long = 0L,
    val note: String = "",
) {
    val players: List<FantasyPlayer> get() = tracked + trending
    val hasData: Boolean get() = tracked.isNotEmpty() || trending.isNotEmpty()
}

/**
 * The arithmetic and the prose, kept separate from the feed and the screen.
 *
 * Two scoring systems are used, both stated in the UI rather than hidden:
 *  - Basketball is a standard points league: PTS + 1.2 REB + 1.5 AST + 3 3PM.
 *  - Football is half-PPR: 1/25 pass yard, 4 pass TD, −2 INT, 1/10 rush yard, 6 rush TD,
 *    0.5 reception, 1/10 receiving yard, 6 receiving TD.
 *
 * The start/sit call is a *form* call. The app has no league roster, no opponent context and
 * no ownership data, so it compares a player with his own recent baseline instead of
 * pretending to know whether he belongs in someone's lineup.
 */
object FantasyDesk {

    const val MIN_GAMES = 3

    fun basketballScore(points: Int, rebounds: Int, assists: Int, threes: Int): Double =
        points + 1.2 * rebounds + 1.5 * assists + 3.0 * threes

    fun footballScore(
        passYards: Int,
        passTouchdowns: Int,
        interceptions: Int,
        rushYards: Int,
        rushTouchdowns: Int,
        receptions: Int,
        receivingYards: Int,
        receivingTouchdowns: Int,
        fumblesLost: Int = 0,
    ): Double {
        val score = passYards / 25.0 +
            4.0 * passTouchdowns -
            2.0 * interceptions +
            rushYards / 10.0 +
            6.0 * rushTouchdowns +
            0.5 * receptions +
            receivingYards / 10.0 +
            6.0 * receivingTouchdowns -
            2.0 * fumblesLost
        return max(0.0, score)
    }

    fun footballTouchdownPoints(
        passTouchdowns: Int,
        rushTouchdowns: Int,
        receivingTouchdowns: Int,
    ): Double = 4.0 * passTouchdowns + 6.0 * rushTouchdowns + 6.0 * receivingTouchdowns

    /** Groups lines by player, attaches the latest designation and ranks by projection. */
    fun rank(
        sport: FantasySport,
        lines: List<FantasyLine>,
        injuries: List<FantasyInjury> = emptyList(),
    ): List<FantasyPlayer> {
        val injuryByKey = injuries
            .groupBy { it.playerKey.ifBlank { Names.key(it.playerName) } }
            // groupBy never yields an empty list, so this fallback is the
            // first row only if the list invariant is ever broken.
            .mapValues { (_, rows) -> rows.maxByOrNull { it.observedUtc } ?: rows.first() }
        return lines
            .filter { it.playerKey.isNotBlank() && it.fantasyPoints.isFinite() }
            .groupBy { it.playerKey }
            .mapNotNull { (key, rows) ->
                val sorted = rows.sortedByDescending { it.playedUtc }
                val first = sorted.firstOrNull() ?: return@mapNotNull null
                FantasyPlayer(
                    key = key,
                    name = first.playerName,
                    team = first.team,
                    sport = sport,
                    lines = sorted.take(10),
                    injury = injuryByKey[key],
                    role = sorted.groupingBy { it.role }.eachCount()
                        .maxByOrNull { it.value }?.key.orEmpty(),
                )
            }
            .sortedWith(compareByDescending<FantasyPlayer> { it.projection }.thenBy { it.name })
    }

    fun build(
        sport: FantasySport,
        lines: List<FantasyLine>,
        injuries: List<FantasyInjury>,
        watchlist: Set<String>,
        fetchedUtc: Long,
        note: String = "",
    ): FantasyDeskUi {
        val players = rank(sport, lines, injuries)
        val tracked = players
            .filter { FantasyWatchlist.key(sport, it.key) in watchlist }
            .sortedByDescending { it.projection }
        val trending = players
            .filter { FantasyWatchlist.key(sport, it.key) !in watchlist }
            .take(12)
        val wire = injuries
            .sortedWith(compareByDescending<FantasyInjury> { it.observedUtc })
            .take(12)
        return FantasyDeskUi(
            sport = sport,
            tracked = tracked,
            trending = trending,
            injuries = wire,
            lineupTips = lineupTips(players),
            fetchedUtc = fetchedUtc,
            note = note,
        )
    }

    fun advise(player: FantasyPlayer): FantasyAdvice {
        val tips = ArrayList<String>(3)
        val injury = player.injury
        val status = injury?.status
        val designation = injury?.let {
            "${AvailabilityStatus.label(it.status)} on the league report" +
                it.note.takeIf { note -> note.isNotBlank() }?.let { note -> " — $note" }.orEmpty()
        }

        if (injury != null) {
            tips += "$designation."
        }

        when (status) {
            AvailabilityStatus.OUT -> return FantasyAdvice(
                call = FantasyCall.OUT,
                headline = "Ruled out — do not start",
                tips = tips + "The desk does not project a player the club has ruled out.",
            )
            AvailabilityStatus.DOUBTFUL -> return FantasyAdvice(
                call = FantasyCall.SIT,
                headline = "Doubtful — plan without him",
                tips = tips + "A doubtful designation is not a coin flip worth starting.",
            )
            AvailabilityStatus.QUESTIONABLE, AvailabilityStatus.GAME_TIME -> {
                if (player.games < MIN_GAMES) {
                    tips += onlyAGuess(player.games)
                }
                return FantasyAdvice(
                    call = FantasyCall.WATCH,
                    headline = "Designation pending — check before lock",
                    tips = tips + "The app will not guess whether he plays; the club has not said.",
                )
            }
        }

        if (player.games < 2) {
            return FantasyAdvice(
                call = FantasyCall.WATCH,
                headline = "One game on file — no form to read",
                tips = tips + onlyAGuess(player.games) +
                    "Wait for a second game before treating the projection as a trend.",
            )
        }

        val delta = player.projection - player.average
        val hot = delta >= 1.5 && player.trend > 0.0
        val cold = delta <= -1.5 || player.trend <= -2.0
        val call = when {
            hot -> FantasyCall.START
            cold -> FantasyCall.SIT
            else -> FantasyCall.WATCH
        }
        val headline = when (call) {
            FantasyCall.START -> "Trending up — start"
            FantasyCall.SIT -> "Cooling off — bench"
            else -> "Steady — watch"
        }

        tips += "Averaging %.1f over %d games at %.1f in the latest.".format(
            player.average, player.games, player.projection,
        )
        if (player.trend != 0.0) {
            val direction = if (player.trend > 0) "above" else "below"
            tips += "The last three games are %.1f points %s the three before them.".format(
                kotlin.math.abs(player.trend), direction,
            )
        }
        if (player.volatility >= 0.35 && player.games >= 3) {
            tips += "Boom-bust: scores run from %.0f to %.0f, so the floor is real.".format(
                player.floor, player.ceiling,
            )
        }
        if (player.sport == FantasySport.FOOTBALL && player.touchdownShare >= 0.5 && player.games >= 2) {
            tips += "Touchdown-dependent: ${(player.touchdownShare * 100).toInt()}% of his recent points " +
                "came from scores, which are the least repeatable part of a line."
        }
        if (player.games < MIN_GAMES) tips += onlyAGuess(player.games)

        return FantasyAdvice(call = call, headline = headline, tips = tips)
    }

    fun lineupTips(players: List<FantasyPlayer>): List<String> {
        if (players.isEmpty()) {
            return listOf(
                "No finished games are on file for this sport yet. The desk reads ESPN's public box " +
                    "scores, so refresh after a slate has gone final.",
            )
        }
        val healthy = players.filter {
            it.injury?.status != AvailabilityStatus.OUT && it.injury?.status != AvailabilityStatus.DOUBTFUL
        }
        val top = healthy.sortedByDescending { it.projection }.take(3)
        val flagged = players.filter {
            it.injury?.status in setOf(
                AvailabilityStatus.OUT,
                AvailabilityStatus.DOUBTFUL,
                AvailabilityStatus.QUESTIONABLE,
                AvailabilityStatus.GAME_TIME,
            )
        }.take(3)
        val thin = players.count { it.games < MIN_GAMES }
        return buildList {
            if (top.isNotEmpty()) {
                add(
                    "First look: " + top.joinToString(", ") {
                        "%s (%.1f proj.)".format(it.name, it.projection)
                    } + ".",
                )
            }
            if (flagged.isNotEmpty()) {
                add(
                    "Check before lock: " + flagged.joinToString(", ") {
                        "${it.name} (${AvailabilityStatus.label(it.injury?.status.orEmpty()).lowercase()})"
                    } + ".",
                )
            }
            if (thin > 0) {
                add(
                    "$thin ${if (thin == 1) "player has" else "players have"} fewer than $MIN_GAMES " +
                        "games on file, so those projections are the least settled on the desk.",
                )
            }
            add(
                "This is a form ranking, not your league: no roster, scoring settings or opponent " +
                    "are connected, and nothing here is a promise about a game.",
            )
        }
    }

    private fun onlyAGuess(games: Int): String = when (games) {
        0 -> "No game log is available, so there is no projection to defend."
        1 -> "One game is a sample, not a trend."
        else -> "$games games is a thin sample; the projection moves easily."
    }

    private fun List<Double>.average(): Double = if (isEmpty()) 0.0 else sum() / size
}

/**
 * The watchlist is a set of `sport:player-key` strings in private preferences.
 *
 * Pure encode/decode/toggle so the storage format is testable on the JVM, and so a malformed
 * preference can only ever produce a shorter list rather than an exception on screen.
 */
object FantasyWatchlist {

    const val MAX = 40

    fun key(sport: FantasySport, playerKey: String): String = "${sport.slug}:$playerKey"

    fun decode(raw: String?): Set<String> =
        raw.orEmpty()
            .split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() && ':' in it }
            .take(MAX)
            .toSet()

    fun encode(keys: Set<String>): String =
        keys.filter { ':' in it }.take(MAX).sorted().joinToString("|")

    fun toggle(keys: Set<String>, key: String): Set<String> =
        if (key in keys) keys - key else (keys + key).take(MAX).toSet()

    fun playerKeyOf(key: String): String = key.substringAfter(':', key)

    fun sportOf(key: String): FantasySport? = FantasySport.fromSlug(key.substringBefore(':', ""))
}
