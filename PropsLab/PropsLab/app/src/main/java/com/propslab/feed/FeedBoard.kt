package com.propslab.feed

import com.propslab.domain.Odds

/**
 * Turning a pile of prices into a board.
 *
 * Both feeds are noisy in ways that are easy to miss: a book that quotes only one side,
 * a line of 0, a price of 0, two books offering the same player at the same line. The rule
 * here is one price pair per game, player, market and line — the pair with the lowest hold,
 * which is the pair a bettor could actually have taken — and nothing else.
 */
object FeedBoard {

    /** A plausible line for an NBA player. Anything outside this is a parse artefact. */
    const val MIN_LINE = 0.5
    const val MAX_LINE = 80.0

    /** American prices are rejected outside this band; the format has no meaning there. */
    val PRICE_RANGE = -2000..2000

    /** Past this the book is not quoting a two-way market, it is quoting a mistake. */
    const val MAX_HOLD = 0.12

    fun acceptable(quote: RawQuote): Boolean {
        if (!quote.line.isFinite()) return false
        if (quote.line < MIN_LINE || quote.line > MAX_LINE) return false
        if (quote.overPrice !in PRICE_RANGE || quote.underPrice !in PRICE_RANGE) return false
        val hold = hold(quote.overPrice, quote.underPrice) ?: return false
        return hold <= MAX_HOLD
    }

    /** The book's margin on the pair, or null when either price is not a real price. */
    fun hold(over: Int, under: Int): Double? = runCatching {
        Odds.impliedProbability(over) + Odds.impliedProbability(under) - 1.0
    }.getOrNull()

    /**
     * One two-way pair per game, player, market and line, cheapest hold first.
     *
     * Sorting by hold rather than by price keeps the choice independent of which side the
     * formula eventually takes. Ties break on the book key so the same board always comes
     * out of the same input.
     */
    fun bestPrices(quotes: List<RawQuote>): List<RawQuote> {
        val bySlot = LinkedHashMap<String, MutableList<RawQuote>>()
        for (quote in quotes) {
            if (!acceptable(quote)) continue
            val slot = "${quote.eventId}|${quote.playerKey}|${quote.market}|${quote.line}"
            bySlot.getOrPut(slot) { mutableListOf() }.add(quote)
        }
        return bySlot.values.mapNotNull { options ->
            options.sortedWith(
                compareBy<RawQuote> { hold(it.overPrice, it.underPrice) ?: Double.MAX_VALUE }
                    .thenBy { it.bookKey },
            ).firstOrNull()
        }.sortedWith(compareBy({ it.eventId }, { it.playerKey }, { it.market }, { it.line }))
    }
}
