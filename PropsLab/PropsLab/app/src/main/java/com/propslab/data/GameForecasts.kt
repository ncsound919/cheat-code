package com.propslab.data

import com.propslab.domain.Calibration
import com.propslab.domain.GameBook
import com.propslab.domain.GameChance
import com.propslab.domain.GameForecast
import com.propslab.domain.GameModel
import com.propslab.domain.PlattFit
import com.propslab.domain.Staking
import com.propslab.domain.TeamGameRow
import com.propslab.feed.GameMarkets
import kotlin.math.abs
import kotlin.math.roundToInt

/** One side of a game market, priced by the book and scored by the model. */
data class GamePickUi(
    val key: String,
    val gameId: String,
    val homeCode: String,
    val awayCode: String,
    val matchup: String,
    val startUtc: Long,
    val market: String,
    val marketLabel: String,
    /** HOME, AWAY, OVER or UNDER. */
    val sideKey: String,
    /** The selection as a book writes it: "ATL −3.5", "Over 231.5", "CLE ml". */
    val selection: String,
    val line: Double?,
    val odds: Int,
    val book: String,
    /** The model's chance of this side winning, after the learned correction. */
    val chance: Double,
    /** The same chance before the correction; the learning ledger fits on this one. */
    val rawChance: Double,
    val push: Double,
    /** The book's no-vig chance of the same side. */
    val marketChance: Double?,
    val edgePts: Double?,
    val ev: Double,
    val confidence: String,
    val sample: Int,
    val expectedMargin: Double,
    val expectedTotal: Double,
    val note: String,
) {
    val isMoneyline: Boolean get() = market == GameMarkets.MONEYLINE
    val label: String get() = "$selection ${marketLabel.lowercase()}"
}

/** The game half of the board: the books' lines, enriched with the model's forecast. */
data class GameBoardUi(
    val lines: List<GameLinesUi> = emptyList(),
    val picks: List<GamePickUi> = emptyList(),
    val book: GameBook? = null,
    val note: String = "No team ratings yet — the game model needs box scores from both sides of at least four games.",
)

/**
 * Turns quoted game lines into a scored board.
 *
 * The book's numbers are never overwritten: [GameLinesBoard] owns the prices and the
 * no-vig chances, and this layer adds what the model thinks beside them. A game is only
 * scored when both teams have [GameModel.MIN_TEAM_GAMES] logged games, so a thin October
 * board shows prices with no forecast rather than a confident number from two games.
 *
 * For each market the pick is the side with the larger model-versus-market gap, and only
 * a side the model makes positive expected value on is published. The other side is not
 * lost: the learning ledger logs both the pick and its mirror when it records the board.
 */
object GameForecastBoard {

    const val MODEL_VERSION = "game-form-v1"

    fun build(
        rows: List<GameLineRow>,
        totals: List<TeamTotalRow>,
        fit: PlattFit = PlattFit.IDENTITY,
    ): GameBoardUi {
        val book = GameModel.build(teamGames(totals))
        val plain = GameLinesBoard.build(rows)
        if (plain.isEmpty()) {
            return GameBoardUi(book = book, note = "No moneyline, spread or total is on the board yet.")
        }
        val byGame = rows.groupBy { it.gameId }
        val lines = ArrayList<GameLinesUi>(plain.size)
        val picks = ArrayList<GamePickUi>()

        for (line in plain) {
            val source = byGame[line.gameId].orEmpty()
            val first = source.firstOrNull()
            val homeCode = first?.homeCode.orEmpty()
            val awayCode = first?.awayCode.orEmpty()
            val forecast = if (homeCode.isNotBlank() && awayCode.isNotBlank()) {
                GameModel.forecast(book, homeCode, awayCode)
            } else null
            if (forecast == null) {
                lines += line
                continue
            }
            val enriched = line.copy(
                homeCode = homeCode,
                awayCode = awayCode,
                markets = line.markets.map { market ->
                    when (market.market) {
                        GameMarkets.MONEYLINE -> enrich(market, GameModel.moneyline(forecast), fit)
                        GameMarkets.SPREAD -> market.line?.let {
                            enrich(market, GameModel.spread(forecast, it), fit)
                        } ?: market
                        GameMarkets.TOTAL -> market.line?.let {
                            enrich(market, GameModel.total(forecast, it), fit)
                        } ?: market
                        else -> market
                    }
                },
            )
            lines += enriched
            picks += enriched.markets.mapNotNull { market -> pickOf(enriched, market, forecast) }
        }

        val note = when {
            book.teams.isEmpty() -> GameBoardUi(book = book, lines = lines).note
            !book.measuredSpreads -> "Game ratings from ${book.teams.size} teams, ${book.gamesObserved} finished games. " +
                "Margin and total spreads are the league defaults until 40 games are stored."
            else -> "Game ratings from ${book.teams.size} teams, ${book.gamesObserved} finished games. " +
                "Margin spread ${"%.1f".format(book.marginSd)} and total spread ${"%.1f".format(book.totalSd)} measured from those games."
        }
        return GameBoardUi(lines = lines, picks = picks.sortedByDescending { it.ev }, book = book, note = note)
    }

    private fun enrich(
        market: GameMarketUi,
        chance: GameChance,
        fit: PlattFit,
    ): GameMarketUi {
        val corrected = Calibration.apply(fit, chance.leftNoPush)
        val room = 1.0 - chance.push
        val rawLeftChance = (chance.leftNoPush * room).coerceIn(0.0, room)
        val rawRightChance = ((1.0 - chance.leftNoPush) * room).coerceIn(0.0, room)
        val leftChance = (corrected * room).coerceIn(0.0, room)
        val rightChance = ((1.0 - corrected) * room).coerceIn(0.0, room)
        val leftEdge = market.left.noVig?.let { (corrected - it) * 100.0 }
        val rightEdge = market.right.noVig?.let { ((1.0 - corrected) - it) * 100.0 }
        return market.copy(
            left = market.left.copy(modelChance = leftChance, rawModelChance = rawLeftChance, edgePts = leftEdge),
            right = market.right.copy(modelChance = rightChance, rawModelChance = rawRightChance, edgePts = rightEdge),
        )
    }

    private fun pickOf(
        source: GameLinesUi,
        market: GameMarketUi,
        forecast: GameForecast,
    ): GamePickUi? {
        val leftEdge = market.left.edgePts
        val rightEdge = market.right.edgePts
        if (leftEdge == null && rightEdge == null) return null
        val home = (leftEdge ?: -999.0) >= (rightEdge ?: -999.0)
        val side = if (home) market.left else market.right
        val edge = if (home) leftEdge else rightEdge
        if (edge == null || edge <= 0.0) return null
        val sideKey = when {
            market.market == GameMarkets.TOTAL -> if (home) LearningSide.OVER else LearningSide.UNDER
            market.market == GameMarkets.SPREAD || market.market == GameMarkets.MONEYLINE ->
                if (home) LearningSide.HOME else LearningSide.AWAY
            else -> return null
        }
        val modelChance = side.modelChance ?: return null
        val otherChance = if (home) market.right.modelChance else market.left.modelChance
        val push = if (market.market == GameMarkets.MONEYLINE) {
            0.0
        } else {
            (1.0 - modelChance - (otherChance ?: 0.0)).coerceAtLeast(0.0)
        }
        val ev = runCatching { Staking.expectedValue(modelChance, push, side.price) }.getOrNull() ?: return null
        if (ev <= 0.0) return null
        val selection = selection(market, side, home)
        return GamePickUi(
            key = "${source.gameId}|${market.market}|$sideKey|${market.line ?: 0.0}",
            gameId = source.gameId,
            homeCode = source.homeCode,
            awayCode = source.awayCode,
            matchup = source.matchup,
            startUtc = source.startUtc,
            market = market.market,
            marketLabel = market.label,
            sideKey = sideKey,
            selection = selection,
            // The ledger settles the side that was actually taken, so a spread stores the
            // handicap as that side sees it: the away side of a home −3.5 is +3.5.
            line = sideLine(market, home),
            odds = side.price,
            book = side.book,
            chance = modelChance,
            rawChance = side.rawModelChance ?: modelChance,
            push = push,
            marketChance = side.noVig,
            edgePts = edge,
            ev = ev,
            confidence = forecast.confidence,
            sample = forecast.sample,
            expectedMargin = forecast.expectedMargin,
            expectedTotal = forecast.expectedTotal,
            note = explain(forecast, market, home),
        )
    }

    private fun explain(forecast: GameForecast, market: GameMarketUi, home: Boolean): String {
        val projection = "The model projects ${forecast.home} ${one(forecast.expectedHome)} – " +
            "${forecast.away} ${one(forecast.expectedAway)} on ${forecast.homeGames} and ${forecast.awayGames} games."
        val lean = when (market.market) {
            GameMarkets.MONEYLINE ->
                "It makes the ${if (forecast.expectedMargin >= 0) forecast.home else forecast.away} side the better team tonight."
            GameMarkets.SPREAD ->
                "That is a ${one(abs(forecast.expectedMargin))}-point edge for ${if (forecast.expectedMargin >= 0) forecast.home else forecast.away}, " +
                    "which is what the spread is being judged against."
            else ->
                "The total lands ${if (forecast.expectedTotal > (market.line ?: forecast.expectedTotal)) "above" else "below"} " +
                    "the quoted ${one(market.line ?: forecast.expectedTotal)}."
        }
        return "$projection $lean"
    }

    private fun sideLine(market: GameMarketUi, home: Boolean): Double? = when (market.market) {
        GameMarkets.TOTAL -> market.line
        GameMarkets.SPREAD -> market.line?.let { if (home) it else -it }
        else -> null
    }

    private fun selection(market: GameMarketUi, side: GameSideUi, home: Boolean): String = when (market.market) {
        GameMarkets.TOTAL -> "${if (home) "Over" else "Under"} ${point(market.line ?: 0.0)}"
        GameMarkets.SPREAD -> "${side.label} ${signed(if (home) market.line ?: 0.0 else -(market.line ?: 0.0))}"
        else -> side.label
    }

    private fun point(value: Double): String =
        if (abs(value - Math.rint(value)) < 1e-9) "%.0f".format(value) else "%.1f".format(value)

    private fun signed(value: Double): String {
        val sign = if (value >= 0) "+" else "−"
        return sign + point(abs(value))
    }

    private fun one(value: Double): String = "%.1f".format(value)

    private fun teamGames(totals: List<TeamTotalRow>): List<TeamGameRow> =
        totals.groupBy { it.eventId }.flatMap { (eventId, group) ->
            val clean = group.filter { it.teamAbbr.isNotBlank() && it.points >= 0 }
            if (clean.size < 2) emptyList() else {
                val a = clean[0]
                val b = clean[1]
                val at = maxOf(a.playedUtc, b.playedUtc)
                listOf(
                    TeamGameRow(eventId, a.teamAbbr, b.teamAbbr, a.points, b.points, at),
                    TeamGameRow(eventId, b.teamAbbr, a.teamAbbr, b.points, a.points, at),
                )
            }
        }
}
