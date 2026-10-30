package com.propslab.data

import com.propslab.domain.Outcome
import com.propslab.feed.GameMarkets

/**
 * How a finished game settles one ledger row.
 *
 * The rules are the book's, not the model's: a moneyline has no push, a spread pushes on
 * the exact number, and a total pushes on the exact total. Keeping this out of the
 * repository makes it testable on the JVM, which matters because a settlement bug would
 * quietly poison every calibration that follows.
 */
object LearningSettlement {

    /**
     * @param line the handicap applied to the chosen side for a spread (the home side is
     *   usually the negative number), the total for a total, ignored for a moneyline.
     * @param home the home team's final score.
     * @param away the away team's final score.
     */
    fun game(market: String, side: String, line: Double, home: Int, away: Int): Outcome? {
        return when (market) {
            GameMarkets.MONEYLINE -> {
                if (home == away) return null
                val homeWon = home > away
                if ((side == LearningSide.HOME) == homeWon) Outcome.WIN else Outcome.LOSS
            }
            GameMarkets.SPREAD -> {
                if (side != LearningSide.HOME && side != LearningSide.AWAY) return null
                // The line is the handicap on the side being settled, so it is added to that
                // side's margin: away +3.5 covers a three-point loss.
                val margin = if (side == LearningSide.HOME) {
                    (home - away) + line
                } else {
                    (away - home) + line
                }
                when {
                    margin > 0.0 -> Outcome.WIN
                    margin < 0.0 -> Outcome.LOSS
                    else -> Outcome.PUSH
                }
            }
            GameMarkets.TOTAL -> {
                if (side != LearningSide.OVER && side != LearningSide.UNDER) return null
                val total = home + away
                when {
                    total.toDouble() == line -> Outcome.PUSH
                    (total > line) == (side == LearningSide.OVER) -> Outcome.WIN
                    else -> Outcome.LOSS
                }
            }
            else -> null
        }
    }
}
