package com.propslab.domain

import kotlin.math.abs

/** American-odds math. Pure Kotlin so it can be unit-tested on the JVM. */
object Odds {
    fun americanToDecimal(american: Int): Double {
        require(american >= 100 || american <= -100) { "Invalid American odds: $american" }
        return if (american > 0) 1.0 + american / 100.0 else 1.0 + 100.0 / -american
    }

    fun impliedProbability(american: Int): Double = 1.0 / americanToDecimal(american)

    /** Two-way no-vig probability of the over, from both quoted prices. */
    fun noVigOver(overAmerican: Int, underAmerican: Int): Double {
        val o = impliedProbability(overAmerican)
        val u = impliedProbability(underAmerican)
        return o / (o + u)
    }

    fun profitOnWin(stake: Double, american: Int): Double = stake * (americanToDecimal(american) - 1.0)
}

enum class Side {
    OVER, UNDER;

    val label: String get() = if (this == OVER) "Over" else "Under"
}

enum class Outcome { WIN, LOSS, PUSH, VOID }

/** P(over), P(push), P(under). Construction fails unless all three are finite, in [0,1] and sum to 1. */
data class OutcomeProbabilities(val over: Double, val push: Double, val under: Double) {
    init {
        require(listOf(over, push, under).all { it.isFinite() && it in 0.0..1.0 }) { "Probabilities must be finite and within [0,1]" }
        require(abs(over + push + under - 1.0) < 1e-6) { "Probabilities must sum to 1" }
    }

    /** P(over) conditional on the bet not pushing; comparable to a no-vig market price. */
    val overGivenNoPush: Double get() = if (over + under == 0.0) 0.5 else over / (over + under)

    fun forSide(side: Side): Double = if (side == Side.OVER) over else under
}

object Settlement {
    const val RULE_VERSION = "points_v2"

    fun outcome(side: Side, line: Double, actual: Double, voided: Boolean = false): Outcome = when {
        voided -> Outcome.VOID
        actual == line -> Outcome.PUSH
        (actual > line) == (side == Side.OVER) -> Outcome.WIN
        else -> Outcome.LOSS
    }

    fun profit(outcome: Outcome, stake: Double, american: Int): Double = when (outcome) {
        Outcome.WIN -> Odds.profitOnWin(stake, american)
        Outcome.LOSS -> -stake
        Outcome.PUSH -> 0.0
        Outcome.VOID -> 0.0
    }
}

/** Why a prediction cannot be shown as a usable estimate. */
enum class RejectReason(val message: String) {
    STALE_QUOTE("The quote is too old to trust"),
    MISSING_LINE("No line was quoted for this prop"),
    STALE_FEATURES("Injury and minutes data is stale"),
    PLAYER_UNMATCHED("The player could not be matched reliably"),
    OUT_OF_SCOPE("This market is outside the validated scope"),
    SUPERSEDED("Real quotes have replaced this synthetic fixture"),
    INSUFFICIENT_SAMPLE("There are not enough logged games to price this player"),
    OUT_SIGNAL("A late report says this player will not play"),
}

/**
 * The markets this build can price.
 *
 * Every one of them is a plain sum of box-score columns, which is what lets the game-log
 * model price them: points, rebounds, assists, made threes, and points+rebounds+assists.
 * It is a list of what the arithmetic covers, not a claim that any of them has been
 * backtested against a real book.
 */
object Markets {
    const val POINTS = "player_points"
    const val REBOUNDS = "player_rebounds"
    const val ASSISTS = "player_assists"
    const val THREES = "player_threes"
    const val PRA = "player_pra"

    val VALIDATED: Set<String> = setOf(POINTS, REBOUNDS, ASSISTS, THREES, PRA)

    /** How the market is written on screen. Unknown keys are shown as they arrived. */
    fun label(market: String): String = when (market) {
        POINTS -> "Points"
        REBOUNDS -> "Rebounds"
        ASSISTS -> "Assists"
        THREES -> "Threes"
        PRA -> "PRA"
        else -> market
    }
}

object PredictionGate {
    const val HOUR_MS = 3_600_000L

    fun evaluate(
        line: Double?,
        quoteTimeUtc: Long,
        featureCutoffUtc: Long,
        nowUtc: Long,
        playerMatched: Boolean,
        market: String,
        maxQuoteAgeMs: Long = 6 * HOUR_MS,
        maxFeatureAgeMs: Long = 24 * HOUR_MS,
        validatedMarkets: Set<String> = Markets.VALIDATED,
    ): List<RejectReason> = buildList {
        if (line == null) add(RejectReason.MISSING_LINE)
        if (nowUtc - quoteTimeUtc > maxQuoteAgeMs) add(RejectReason.STALE_QUOTE)
        if (nowUtc - featureCutoffUtc > maxFeatureAgeMs) add(RejectReason.STALE_FEATURES)
        if (!playerMatched) add(RejectReason.PLAYER_UNMATCHED)
        if (market !in validatedMarkets) add(RejectReason.OUT_OF_SCOPE)
    }
}
