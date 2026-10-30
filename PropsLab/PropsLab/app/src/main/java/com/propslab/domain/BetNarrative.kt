package com.propslab.domain

import com.propslab.settings.stakeWord
import java.util.Locale

/**
 * The paragraph behind a pick.
 *
 * A number without a sentence is a number nobody can argue with, and a bet nobody can argue
 * with is a bet nobody can learn from. So every pick carries prose that states what was
 * taken, at what price, what the price implies, what the model says instead, how much
 * evidence stands behind it, and what would make it wrong.
 *
 * It is generated from the same values the screen shows, so it cannot drift from them, and
 * it never asserts anything the data does not: no certainty, no "lock", no edge without the
 * numbers that produced it.
 */
object BetNarrative {

    data class Input(
        val player: String,
        val market: String,
        val matchup: String,
        val book: String,
        val line: Double,
        val side: Side,
        val odds: Int,
        val modelProb: Double,
        val marketProb: Double,
        val edgePts: Double,
        val ev: Double,
        val breakeven: Double,
        val kelly: Double,
        /** Fraction of full Kelly the board actually stakes; 0.25 = quarter Kelly. */
        val stakeFraction: Double = 0.25,
        val gamesLogged: Int?,
        val flags: List<String>,
        val live: Boolean,
        val isFixture: Boolean,
    )

    fun paragraph(input: Input): String = buildString {
        val side = input.side.label
        val line = if (input.line % 1.0 == 0.0) "%.0f".format(input.line) else "%.1f".format(input.line)
        append(input.player).append(' ').append(side).append(' ').append(line).append(' ')
        append(input.market.lowercase(Locale.ROOT)).append(" at ").append(odds(input.odds))
        append(" on ").append(input.book).append(". ")

        val edge = kotlin.math.abs(input.edgePts)
        append("The price implies ").append(pct(input.marketProb)).append("; the model puts it at ")
        append(pct(input.modelProb)).append(", a ").append("%.1f".format(edge)).append("-point disagreement. ")
        append("At ").append(odds(input.odds)).append(" the bet needs ")
        append(pct(input.breakeven)).append(" to break even")
        append(", so a unit staked is worth ").append("%+.3f".format(input.ev)).append(" in expectation")
        if (input.kelly > 0.0) {
            append(", and a ").append(stakeWord(input.stakeFraction)).append("-Kelly stake would be ")
                .append(pct(input.kelly * input.stakeFraction)).append(" of the bankroll")
        }
        append(". ")

        val games = input.gamesLogged
        if (games != null && games > 0) {
            append("The estimate comes from ").append(games).append(" logged games")
            append(if (games < 12) ", which is a thin sample" else "")
            append(". ")
        }
        val signal = input.flags.firstOrNull { it.startsWith("Late report:") }
        if (signal != null) {
            append(signal.removePrefix("Late report: ").replaceFirstChar { it.uppercase() })
                .append(". ")
        }
        val other = input.flags.filterNot { it.startsWith("Late report:") }
        if (other.isNotEmpty()) {
            append("Worth knowing: ").append(other.first().replaceFirstChar { it.lowercase() })
            if (other.size > 1) append(", and ").append(other.size - 1).append(" more flag(s) on the detail screen")
            append(". ")
        }

        when {
            input.isFixture -> append("This row is invented fixture data, so the whole paragraph describes a test board.")
            input.live -> append("This is the live book's pick: the formula behind it cleared every check on a season it had not seen.")
            else -> append("This is a shadow pick: the formula behind it has not earned the live book yet.")
        }
    }

    private fun pct(value: Double): String = "%.1f%%".format(value * 100)

    private fun odds(value: Int): String = if (value > 0) "+$value" else "\u2212${-value}"
}
