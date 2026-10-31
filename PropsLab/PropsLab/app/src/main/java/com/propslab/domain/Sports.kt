package com.propslab.domain

/**
 * The two leagues this build prices, and the words it prints for them.
 *
 * A sport is stored on the game row, not derived from the market at read time: a game is
 * basketball or football before any prop exists on it, and a board that guesses from a
 * market string would relabel a game the moment an unsupported market arrived.
 */
object Sports {
    const val BASKETBALL = "basketball"
    const val FOOTBALL = "football"

    val ALL = listOf(BASKETBALL, FOOTBALL)

    /** The league a fan reads on the card. */
    fun label(sport: String): String = when (sport) {
        BASKETBALL -> "NBA"
        FOOTBALL -> "NFL"
        else -> sport.uppercase()
    }

    /** The league that owns a market, for rows that predate the sport column. */
    fun ofMarket(market: String): String =
        if (market in Markets.FOOTBALL) FOOTBALL else BASKETBALL

    /** The filter value a chip carries; null is the unfiltered board. */
    fun parse(text: String): String? = ALL.firstOrNull { it.equals(text, ignoreCase = true) }

    /** True when this sport's props come from an ESPN football payload. */
    fun isFootball(sport: String): Boolean = sport == FOOTBALL
}
