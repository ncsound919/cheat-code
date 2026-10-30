package com.propslab.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * One team's side of one finished game, reduced to the two numbers a rating needs.
 *
 * Both rows of a game are produced from the box score by summing each team's player
 * points, so this is the real score of a real game, not an estimate. A game whose box
 * score is missing one side produces no rows at all rather than half a result.
 */
data class TeamGameRow(
    val gameId: String,
    val team: String,
    val opponent: String,
    val scored: Int,
    val allowed: Int,
    val playedUtc: Long,
)

/** A team's scoring form after shrinkage toward the league average. */
data class TeamForm(
    val team: String,
    val games: Int,
    val scored: Double,
    val allowed: Double,
)

/**
 * The whole league as the game model sees it: one shrunk rating per team, the league
 * scoring average, and the two spreads that turn a forecast into a probability.
 *
 * The spreads are measured from the games themselves when there are enough of them and
 * fall back to the long-run NBA figures otherwise. Neither number is tuned to make the
 * model look confident: a margin is a noisy quantity and pretending otherwise is how a
 * game model lies.
 */
data class GameBook(
    val teams: Map<String, TeamForm>,
    val leagueAverage: Double,
    val marginSd: Double,
    val totalSd: Double,
    val homeEdge: Double,
    val gamesObserved: Int,
    val measuredSpreads: Boolean,
)

/** What the model expects one game to do. */
data class GameForecast(
    val home: String,
    val away: String,
    val expectedHome: Double,
    val expectedAway: Double,
    val marginSd: Double,
    val totalSd: Double,
    val homeGames: Int,
    val awayGames: Int,
) {
    val expectedMargin: Double get() = expectedHome - expectedAway
    val expectedTotal: Double get() = expectedHome + expectedAway

    /** The thinner of the two samples: the forecast is only as good as its weakest input. */
    val sample: Int get() = minOf(homeGames, awayGames)

    val confidence: String
        get() = when {
            sample >= 12 -> "HIGH"
            sample >= 8 -> "MEDIUM"
            else -> "LOW"
        }
}

/**
 * A three-way chance on a game market: the left side (home or over), the push, and the
 * right side. A moneyline has no push and its push is exactly zero.
 */
data class GameChance(val left: Double, val push: Double, val right: Double) {
    init {
        require(listOf(left, push, right).all { it.isFinite() && it >= 0.0 }) {
            "Chances must be finite and non-negative"
        }
        require(abs(left + push + right - 1.0) < 1e-6) { "Chances must sum to 1" }
    }

    /** P(left) once a push is conditioned away, comparable to a no-vig price. */
    val leftNoPush: Double
        get() = if (left + right <= 0.0) 0.5 else left / (left + right)

    /** P(right) once a push is conditioned away. */
    val rightNoPush: Double get() = 1.0 - leftNoPush
}

/**
 * The game-level half of the scoring system.
 *
 * It is deliberately smaller than the prop model: four numbers per team (points for,
 * points against), a league average, a fixed home edge and two measured spreads. That is
 * all a phone can honestly know about a basketball game from box scores. It does not try
 * to model injuries, rest or matchups — the app has no feed for them at game level, and
 * inventing them would make the number look better than the data.
 *
 * The ratings are shrunk toward the league average by [SHRINK_GAMES], so a team four games
 * into a season is treated as mostly average rather than as its hot start.
 */
object GameModel {

    /** A team needs this many logged games before its rating is published. */
    const val MIN_TEAM_GAMES = 4

    /** Shrinkage weight, in games, toward the league average. */
    const val SHRINK_GAMES = 6.0

    /** Long-run NBA totals and margins, used until enough games are stored to measure them. */
    const val DEFAULT_MARGIN_SD = 12.0
    const val DEFAULT_TOTAL_SD = 17.0

    /** How many games back a rating looks. */
    const val WINDOW = 24

    /** Home court, in points of expected margin. */
    const val HOME_EDGE = 2.6

    /** Used when no box score has been ingested at all. */
    const val DEFAULT_LEAGUE_AVERAGE = 112.0

    /** A floor that keeps a log-linear projection from producing an absurd number. */
    private const val MIN_EXPECTED = 80.0
    private const val MAX_EXPECTED = 150.0

    /**
     * Builds the book from finished team games. Rows are the two sides of each game, so
     * one game contributes two rows with mirrored scored/allowed values.
     */
    fun build(rows: List<TeamGameRow>, window: Int = WINDOW): GameBook {
        val usable = rows.filter { it.team.isNotBlank() && it.scored >= 0 && it.allowed >= 0 }
        val league = if (usable.isEmpty()) DEFAULT_LEAGUE_AVERAGE else usable.sumOf { it.scored }.toDouble() / usable.size
        val teams = usable.groupBy { it.team }.mapValues { (team, all) ->
            val recent = all.sortedByDescending { it.playedUtc }.take(window.coerceAtLeast(1))
            val scored = (recent.sumOf { it.scored.toDouble() } + SHRINK_GAMES * league) / (recent.size + SHRINK_GAMES)
            val allowed = (recent.sumOf { it.allowed.toDouble() } + SHRINK_GAMES * league) / (recent.size + SHRINK_GAMES)
            TeamForm(team = team, games = recent.size, scored = scored, allowed = allowed)
        }

        val paired = usable.groupBy { it.gameId }.values.mapNotNull { group ->
            val first = group.firstOrNull() ?: return@mapNotNull null
            val opponent = group.firstOrNull { it.team != first.team } ?: return@mapNotNull null
            first to opponent
        }
        val margins = paired.map { (a, b) -> (a.scored - b.scored).toDouble() }
        val totals = paired.map { (a, b) -> (a.scored + b.scored).toDouble() }
        val measured = paired.size >= 40
        val marginSd = if (measured) sd(margins).coerceIn(8.0, 20.0) else DEFAULT_MARGIN_SD
        val totalSd = if (measured) sd(totals).coerceIn(12.0, 28.0) else DEFAULT_TOTAL_SD
        return GameBook(
            teams = teams,
            leagueAverage = league,
            marginSd = marginSd,
            totalSd = totalSd,
            homeEdge = HOME_EDGE,
            gamesObserved = paired.size,
            measuredSpreads = measured,
        )
    }

    /**
     * Projects one game. Both teams need [MIN_TEAM_GAMES] logged games, otherwise the
     * model declines rather than extrapolating a rating from a two-game sample.
     */
    fun forecast(book: GameBook, home: String, away: String): GameForecast? {
        val h = book.teams[home] ?: return null
        val a = book.teams[away] ?: return null
        if (h.games < MIN_TEAM_GAMES || a.games < MIN_TEAM_GAMES) return null
        val league = book.leagueAverage.coerceAtLeast(1.0)
        // Log-linear: each side's expected score is the league average scaled by its own
        // offense and the opponent's defense. Home court is split evenly so it moves the
        // margin without inventing points in the total.
        val expectedHome = league * (h.scored / league) * (a.allowed / league) + book.homeEdge / 2.0
        val expectedAway = league * (a.scored / league) * (h.allowed / league) - book.homeEdge / 2.0
        if (!expectedHome.isFinite() || !expectedAway.isFinite()) return null
        return GameForecast(
            home = home,
            away = away,
            expectedHome = expectedHome.coerceIn(MIN_EXPECTED, MAX_EXPECTED),
            expectedAway = expectedAway.coerceIn(MIN_EXPECTED, MAX_EXPECTED),
            marginSd = book.marginSd,
            totalSd = book.totalSd,
            homeGames = h.games,
            awayGames = a.games,
        )
    }

    /** The chance the home team wins outright. A moneyline cannot push. */
    fun moneyline(forecast: GameForecast): GameChance {
        val p = normalCdf(forecast.expectedMargin / forecast.marginSd).coerceIn(0.01, 0.99)
        return chance(p, 0.0)
    }

    /** The chance the home side covers [homeHandicap] (a −3.5 home line is −3.5). */
    fun spread(forecast: GameForecast, homeHandicap: Double): GameChance {
        if (!homeHandicap.isFinite()) return chance(0.5, 0.0)
        val threshold = -homeHandicap
        val sd = forecast.marginSd
        val integer = isInteger(homeHandicap)
        return if (integer) {
            val hi = normalCdf((threshold + 0.5 - forecast.expectedMargin) / sd)
            val lo = normalCdf((threshold - 0.5 - forecast.expectedMargin) / sd)
            chance(left = 1.0 - hi, push = (hi - lo).coerceAtLeast(0.0))
        } else {
            val p = normalCdf((threshold - forecast.expectedMargin) / sd)
            chance(left = 1.0 - p, push = 0.0)
        }
    }

    /** The chance the game goes over [line]. The left side is the over. */
    fun total(forecast: GameForecast, line: Double): GameChance {
        if (!line.isFinite()) return chance(0.5, 0.0)
        val sd = forecast.totalSd
        return if (isInteger(line)) {
            val hi = normalCdf((line + 0.5 - forecast.expectedTotal) / sd)
            val lo = normalCdf((line - 0.5 - forecast.expectedTotal) / sd)
            chance(left = 1.0 - hi, push = (hi - lo).coerceAtLeast(0.0))
        } else {
            val p = normalCdf((line - forecast.expectedTotal) / sd)
            chance(left = 1.0 - p, push = 0.0)
        }
    }

    /** Builds a validated chance from a raw left probability and a push mass. */
    private fun chance(left: Double, push: Double): GameChance {
        val p = left.coerceIn(0.0, 1.0)
        val t = push.coerceIn(0.0, 1.0 - p)
        val right = (1.0 - p - t).coerceAtLeast(0.0)
        val sum = p + t + right
        val scale = if (sum <= 0.0) 1.0 else 1.0 / sum
        return GameChance(
            left = (p * scale).coerceIn(0.0, 1.0),
            push = (t * scale).coerceIn(0.0, 1.0),
            right = (right * scale).coerceIn(0.0, 1.0),
        )
    }

    private fun isInteger(value: Double): Boolean = abs(value - Math.rint(value)) < 1e-9

    /** Standard normal CDF, accurate to about 1.5e-7 — far more than a game model needs. */
    fun normalCdf(z: Double): Double {
        if (!z.isFinite()) return if (z > 0) 1.0 else 0.0
        return 0.5 * (1.0 + erf(z / sqrt(2.0)))
    }

    private fun erf(x: Double): Double {
        val sign = if (x < 0.0) -1.0 else 1.0
        val ax = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * ax)
        val poly = (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t
        return sign * (1.0 - poly * exp(-ax * ax))
    }

    private fun sd(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }
}
