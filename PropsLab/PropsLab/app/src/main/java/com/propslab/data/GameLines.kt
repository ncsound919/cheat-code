package com.propslab.data

import com.propslab.domain.Odds
import com.propslab.domain.Pricing
import com.propslab.feed.GameMarkets

/** One side of a game market, at the best price any followed book is offering. */
data class GameSideUi(
    val label: String,
    val price: Int,
    val book: String,
    /** The book's own no-vig chance of this side, not the model's. Null if it cannot be paired. */
    val noVig: Double?,
    /** The game model's chance of this side, once both teams have enough games. */
    val modelChance: Double? = null,
    /** The same chance before the learned correction, which is what the ledger fits on. */
    val rawModelChance: Double? = null,
    /** Model minus the book, in probability points. Positive means the model likes this side. */
    val edgePts: Double? = null,
)

/** Moneyline, spread or total for one game, both sides side by side. */
data class GameMarketUi(
    val market: String,
    val label: String,
    val left: GameSideUi,
    val right: GameSideUi,
    /** The handicap or the total. Null on a moneyline. */
    val line: Double?,
    /** The book's cut on the pair, when both sides came from the same book. */
    val hold: Double?,
)

data class GameLinesUi(
    val gameId: String,
    val matchup: String,
    val startUtc: Long,
    val markets: List<GameMarketUi>,
    val homeCode: String = "",
    val awayCode: String = "",
) {
    val books: String get() = markets.flatMap { listOf(it.left.book, it.right.book) }.distinct().joinToString(", ")
}

/**
 * Turns stored game lines into the three rows a book shows above its props.
 *
 * Two rules keep the numbers honest. A price is only "best" within the same line, so a
 * spread or total compares books quoting the same number rather than shopping a −3.5
 * against a −4.5. And a no-vig chance is only computed inside one book's pair: mixing a
 * best home price with a best away price from a different book would describe a bet
 * nobody can actually make.
 */
object GameLinesBoard {

    fun build(rows: List<GameLineRow>): List<GameLinesUi> =
        rows.groupBy { it.gameId }
            .map { (gameId, all) ->
                val first = all.first()
                GameLinesUi(
                    gameId = gameId,
                    matchup = "${first.awayCode} at ${first.homeCode}",
                    startUtc = first.startUtc,
                    markets = listOfNotNull(moneyline(all), spread(all), total(all)),
                    homeCode = first.homeCode,
                    awayCode = first.awayCode,
                )
            }
            .sortedBy { it.startUtc }

    private data class Pair(val book: String, val left: Int, val right: Int)

    private fun moneyline(rows: List<GameLineRow>): GameMarketUi? {
        val r = rows.filter { it.market == GameMarkets.MONEYLINE }
        if (r.isEmpty()) return null
        val home = r.first().homeName
        val pairs = r.groupBy { it.book }.mapNotNull { (book, lines) ->
            val h = lines.firstOrNull { it.side == home }?.price ?: return@mapNotNull null
            val a = lines.firstOrNull { it.side != home }?.price ?: return@mapNotNull null
            Pair(book, h, a)
        }
        return market(GameMarkets.MONEYLINE, pairs, rows.first().homeCode, rows.first().awayCode, null)
    }

    private fun spread(rows: List<GameLineRow>): GameMarketUi? {
        val r = rows.filter { it.market == GameMarkets.SPREAD }
        if (r.isEmpty()) return null
        val home = r.first().homeName
        // The consensus handicap is the number most books agree on, not the widest one.
        val line = r.filter { it.side == home }.mapNotNull { it.line }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return null
        val pairs = r.groupBy { it.book }.mapNotNull { (book, l) ->
            val h = l.firstOrNull { it.side == home && it.line == line }?.price ?: return@mapNotNull null
            val a = l.firstOrNull { it.side != home && it.line == -line }?.price ?: return@mapNotNull null
            Pair(book, h, a)
        }
        return market(GameMarkets.SPREAD, pairs, rows.first().homeCode, rows.first().awayCode, line)
    }

    private fun total(rows: List<GameLineRow>): GameMarketUi? {
        val r = rows.filter { it.market == GameMarkets.TOTAL }
        if (r.isEmpty()) return null
        val line = r.filter { it.side.equals("Over", true) }.mapNotNull { it.line }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return null
        val pairs = r.groupBy { it.book }.mapNotNull { (book, l) ->
            val over = l.firstOrNull { it.side.equals("Over", true) && it.line == line }?.price ?: return@mapNotNull null
            val under = l.firstOrNull { it.side.equals("Under", true) && it.line == line }?.price ?: return@mapNotNull null
            Pair(book, over, under)
        }
        return market(GameMarkets.TOTAL, pairs, "Over", "Under", line)
    }

    private fun market(
        market: String,
        pairs: List<Pair>,
        leftLabel: String,
        rightLabel: String,
        line: Double?,
    ): GameMarketUi? {
        if (pairs.isEmpty()) return null
        val bestLeft = pairs.maxByOrNull { it.left } ?: return null
        val bestRight = pairs.maxByOrNull { it.right } ?: return null
        val leftNoVig = Odds.noVigOver(bestLeft.left, bestLeft.right)
        val rightNoVig = 1.0 - Odds.noVigOver(bestRight.left, bestRight.right)
        return GameMarketUi(
            market = market,
            label = GameMarkets.label(market),
            left = GameSideUi(leftLabel, bestLeft.left, bestLeft.book, leftNoVig),
            right = GameSideUi(rightLabel, bestRight.right, bestRight.book, rightNoVig),
            line = line,
            hold = if (bestLeft.book == bestRight.book) {
                runCatching { Pricing.hold(bestLeft.left, bestLeft.right) }.getOrNull()
            } else null,
        )
    }
}
