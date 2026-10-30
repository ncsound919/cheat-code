package com.propslab.domain

import kotlin.math.roundToInt

/**
 * Market-side arithmetic shared by the repository, the detail screen and the tests.
 *
 * Every function is pure. Probabilities are decimals in [0,1]; "American" always
 * means a valid American price (>= 100 or <= -100).
 */
object Pricing {

    /** Book margin: the sum of both implied probabilities minus one. */
    fun hold(overAmerican: Int, underAmerican: Int): Double =
        Odds.impliedProbability(overAmerican) + Odds.impliedProbability(underAmerican) - 1.0

    /**
     * Model edge in percentage points of probability. Positive means the model
     * rates the over higher than the no-vig market does.
     */
    fun edgePoints(modelOverNoPush: Double, marketOverNoVig: Double): Double {
        require(modelOverNoPush.isFinite() && marketOverNoVig.isFinite()) {
            "Edge inputs must be finite"
        }
        require(modelOverNoPush in 0.0..1.0 && marketOverNoVig in 0.0..1.0) {
            "Edge inputs must be probabilities"
        }
        return (modelOverNoPush - marketOverNoVig) * 100.0
    }

    /** Decimal price at which the given outcome distribution has exactly zero EV. */
    fun fairDecimal(pWin: Double, pPush: Double = 0.0): Double {
        require(pWin.isFinite() && pPush.isFinite() && pWin > 0.0 && pPush >= 0.0) {
            "Win and push probabilities must be finite, with a positive win probability"
        }
        val pLoss = 1.0 - pWin - pPush
        require(pLoss > 0.0 && pLoss.isFinite()) {
            "A distribution with no losing outcome has no finite fair price"
        }
        return 1.0 + pLoss / pWin
    }

    /**
     * American price at which EV is zero. Push mass is included: a push returns the
     * stake, so it raises the break-even price for the same win probability.
     */
    fun fairAmerican(pWin: Double, pPush: Double = 0.0): Int = decimalToAmerican(fairDecimal(pWin, pPush))

    fun decimalToAmerican(decimal: Double): Int {
        require(decimal > 1.0 && decimal.isFinite()) { "Decimal odds must exceed 1.0" }
        val american = if (decimal >= 2.0) (decimal - 1.0) * 100.0 else -100.0 / (decimal - 1.0)
        return american.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).roundToInt()
    }
}

/** Staking formulas. Values are in units of stake. */
object Staking {

    /**
     * Expected profit for a bet of [stake], given P(win), P(push) and the quoted
     * American price. A push contributes zero profit and returns the stake.
     */
    fun expectedValue(pWin: Double, pPush: Double, american: Int, stake: Double = 1.0): Double {
        require(stake.isFinite() && stake >= 0.0) { "Stake must be finite and non-negative" }
        require(pWin.isFinite() && pPush.isFinite() && pWin >= 0.0 && pPush >= 0.0) {
            "Probabilities must be finite and non-negative"
        }
        require(pWin + pPush <= 1.0 + 1e-9) { "Win and push probabilities cannot exceed 1" }
        val pLoss = (1.0 - pWin - pPush).coerceAtLeast(0.0)
        return stake * (pWin * (Odds.americanToDecimal(american) - 1.0) - pLoss)
    }

    /**
     * Kelly fraction f* = (b·p − q) / (b·(1 − t)), where b = decimal − 1,
     * t = P(push) and q = 1 − p − t. Negative means the edge does not justify a bet.
     */
    fun kellyFraction(pWin: Double, pPush: Double, american: Int): Double {
        require(pWin.isFinite() && pPush.isFinite() && pWin >= 0.0 && pPush >= 0.0) {
            "Probabilities must be finite and non-negative"
        }
        require(pWin + pPush <= 1.0 + 1e-9) { "Win and push probabilities cannot exceed 1" }
        val b = Odds.americanToDecimal(american) - 1.0
        if (b <= 0.0) return 0.0
        val denominator = b * (1.0 - pPush)
        if (denominator <= 0.0) return 0.0
        val pLoss = (1.0 - pWin - pPush).coerceAtLeast(0.0)
        return (pWin * b - pLoss) / denominator
    }

    fun kellyFraction(pWin: Double, american: Int): Double = kellyFraction(pWin, 0.0, american)
}
