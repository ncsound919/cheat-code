package com.propslab.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The price of pushing a line out for a longer price.
 *
 * Pushing the number away from the bettor — Over 24.5 becomes Over 26.5, Under 24.5 becomes
 * Under 22.5 — makes the bet less likely to win, so the book pays more for it. That is the
 * whole trade: a longer price is not free, it is bought with probability.
 *
 * The number the app prints here is an **estimate**, never a quote. A book prices the
 * pushed line against its own distribution, which the app cannot see. What it can do is
 * hold the book's markup fixed and move the price by exactly as much as the model's own
 * fair value moved between the two lines. At zero push this returns the quoted price
 * untouched; above zero it is a model-relative reprice, and the screen says so.
 *
 * The distribution has two sources:
 *  - the player's stored game log (`mean` and `spread` drivers), which is the same normal
 *    approximation `GameLogModel` used to publish the quote; or
 *  - when no game log is stored (synthetic fixtures), a curve inferred from the row's own
 *    probability and line, marked [PushedLeg.inferredCurve]. That fallback exists so the
 *    control still works on invented data; it is not evidence about a real player.
 */
data class PushedLeg(
    val odds: Int,
    val line: Double,
    /** Probability of the taken side at the pushed line, push mass included. */
    val probability: Double,
    val push: Double,
    /** Fair value at the pushed line divided by fair value at the quoted line. */
    val fairFactor: Double,
    val inferredCurve: Boolean,
) {
    /** Probability conditioned on the leg not pushing; what a parlay multiplies. */
    val winNoPush: Double
        get() = if (push >= 1.0) 0.0 else (probability / (1.0 - push)).coerceIn(0.0, 1.0)
}

object LinePush {

    /** The most any single leg can be pushed. Books rarely go further, and the curve gets thin. */
    const val MAX_POINTS = 4.0

    /** The slider's granularity. Half points are how a book moves a line. */
    const val STEP_POINTS = 0.5

    /** Mirrors GameLogModel: the spread never collapses below these floors. */
    private const val SD_FLOOR_FRACTION = 0.30
    private const val SD_FLOOR_ABS = 1.0

    /** Mirrors GameLogModel: a push cannot be more likely than this. */
    private const val MAX_PUSH = 0.20

    private data class Dist(val over: Double, val push: Double, val under: Double) {
        fun side(side: Side): Double = if (side == Side.OVER) over else under
    }

    /**
     * The line after [points] of push: an Over needs more, an Under needs fewer. Both make
     * the bet harder to win, which is what moves the price out.
     */
    fun shiftedLine(line: Double, side: Side, points: Double): Double =
        if (side == Side.OVER) line + points else line - points

    /**
     * Reprices one leg.
     *
     * @param quotedOdds the price actually quoted at [line].
     * @param overProb the model's P(over) at [line], as stored on the prop.
     * @param pushProb the model's P(push) at [line].
     * @param mean the player's stored trailing mean, when the game log was kept.
     * @param spread the player's stored trailing spread, when the game log was kept.
     * @param meanMultiplier a small correction to the projection (circadian alignment).
     *
     * A positive [points] always makes the taken side less likely: an Over line rises and an
     * Under line falls. That is the only way the returned price can be longer than the quote.
     */
    fun reprice(
        quotedOdds: Int,
        line: Double,
        side: Side,
        points: Double,
        overProb: Double,
        pushProb: Double,
        mean: Double? = null,
        spread: Double? = null,
        meanMultiplier: Double = 1.0,
    ): PushedLeg? {
        if (!line.isFinite() || line <= 0.0) return null
        if (!overProb.isFinite() || overProb <= 0.0 || overProb >= 1.0) return null
        if (!pushProb.isFinite() || pushProb < 0.0) return null
        if (!points.isFinite() || !meanMultiplier.isFinite() || meanMultiplier <= 0.0) return null

        val curve = curveOf(overProb, pushProb, line, mean, spread) ?: return null
        val quoted = runCatching { Odds.americanToDecimal(quotedOdds) }.getOrNull() ?: return null
        val shifted = shiftedLine(line, side, points.coerceIn(-MAX_POINTS, MAX_POINTS))

        if (points == 0.0 && abs(meanMultiplier - 1.0) < 1e-12) {
            return PushedLeg(
                odds = quotedOdds,
                line = line,
                probability = if (side == Side.OVER) overProb else (1.0 - overProb - pushProb).coerceAtLeast(0.0),
                push = pushProb,
                fairFactor = 1.0,
                inferredCurve = curve.inferred,
            )
        }

        val base = probabilities(curve.mean, curve.spread, line) ?: return null
        val moved = probabilities(curve.mean * meanMultiplier, curve.spread, shifted) ?: return null
        // probabilities() returns (over, push, under); the taken side is picked here so a
        // push is still carried separately into the price.
        val baseProb = if (side == Side.OVER) base.first else base.third
        val movedProb = if (side == Side.OVER) moved.first else moved.third
        if (baseProb <= 1e-9 || movedProb <= 1e-9) return null

        val fairBase = runCatching { Pricing.fairDecimal(baseProb, base.second) }.getOrNull() ?: return null
        val fairMoved = runCatching { Pricing.fairDecimal(movedProb, moved.second) }.getOrNull() ?: return null
        val factor = fairMoved / fairBase
        if (!factor.isFinite() || factor <= 0.0) return null

        return PushedLeg(
            odds = bookAmerican(quoted * factor),
            line = shifted,
            probability = movedProb,
            push = moved.second,
            fairFactor = factor,
            inferredCurve = curve.inferred,
        )
    }

    /**
     * The distribution behind a leg. A stored game log wins; otherwise a curve is solved
     * from the one probability the row carries.
     */
    fun curve(overProb: Double, pushProb: Double, line: Double): Curve? =
        curveOf(overProb, pushProb, line, null, null)

    data class Curve(val mean: Double, val spread: Double, val inferred: Boolean)

    /** P(over), P(push), P(under) under a normal approximation with the model's floors. */
    fun probabilities(mean: Double, spread: Double, line: Double): Triple<Double, Double, Double>? {
        if (!mean.isFinite() || !spread.isFinite() || !line.isFinite()) return null
        val sd = flooredSpread(mean, spread)
        if (!sd.isFinite() || sd <= 0.0) return null
        val raw = if (abs(line - Math.rint(line)) < 1e-9) {
            val hi = normalCdf((line + 0.5 - mean) / sd)
            val lo = normalCdf((line - 0.5 - mean) / sd)
            Dist(over = 1.0 - hi, push = hi - lo, under = lo)
        } else {
            val p = normalCdf((line - mean) / sd)
            Dist(over = 1.0 - p, push = 0.0, under = p)
        }
        // The same clamp GameLogModel applies, so a pushed line and the published estimate
        // cannot disagree about how much push mass a line carries.
        val push = raw.push.coerceIn(0.0, MAX_PUSH)
        val rest = 1.0 - push
        val total = raw.over + raw.under
        if (!total.isFinite() || total <= 0.0) return Triple(rest / 2.0, push, rest / 2.0)
        val over = (rest * raw.over / total).coerceIn(0.0, rest)
        return Triple(over, push, rest - over)
    }

    fun flooredSpread(mean: Double, spread: Double): Double =
        max(spread, max(abs(mean) * SD_FLOOR_FRACTION, SD_FLOOR_ABS))

    /** American price rounded the way a book writes it: to the nearest five. */
    fun bookAmerican(decimal: Double): Int {
        if (!decimal.isFinite() || decimal <= 1.0) return 100
        val american = runCatching { Pricing.decimalToAmerican(decimal) }.getOrDefault(100).toDouble()
        val rounded = (american / 5.0).roundToInt() * 5
        return when {
            rounded > -100 && rounded < 100 -> if (american >= 0) 100 else -100
            rounded > 5_000 -> 5_000
            rounded < -20_000 -> -20_000
            else -> rounded
        }
    }

    private fun curveOf(
        overProb: Double,
        pushProb: Double,
        line: Double,
        mean: Double?,
        spread: Double?,
    ): Curve? {
        if (mean != null && spread != null && mean.isFinite() && spread.isFinite() && mean > 0.0 && spread > 0.0) {
            return Curve(mean, spread, inferred = false)
        }
        val p = overProb.coerceIn(0.01, 0.99)
        val push = pushProb.coerceIn(0.0, 0.45)
        if (push > 1e-6 && abs(line - Math.rint(line)) < 1e-9) {
            // An integer line fixes the spread through its push mass:
            // push = 2·Φ(0.5 / sd) − 1, so sd = 0.5 / Φ⁻¹((1 + push) / 2).
            val z = normalQuantile((1.0 + push) / 2.0)
            if (z > 1e-9) {
                val sd = 0.5 / z
                val m = line + 0.5 - sd * normalQuantile(1.0 - p)
                if (m.isFinite() && m > 0.0) return Curve(m, sd, inferred = true)
            }
        }
        // A half-point line leaves one equation for two unknowns, so hold the coefficient
        // of variation at a plausible 60% and solve for the mean by bisection.
        var lo = line * 0.02
        var hi = line * 6.0
        repeat(90) {
            val mid = (lo + hi) / 2.0
            val sd = flooredSpread(mid, mid * 0.60)
            val overMid = 1.0 - normalCdf((line - mid) / sd)
            if (overMid < p) lo = mid else hi = mid
        }
        val solved = (lo + hi) / 2.0
        if (!solved.isFinite() || solved <= 0.0) return null
        return Curve(solved, flooredSpread(solved, solved * 0.60), inferred = true)
    }

    /** Standard normal CDF, via Abramowitz & Stegun 7.1.26 (absolute error under 1.5e-7). */
    fun normalCdf(z: Double): Double = 0.5 * (1.0 + erf(z / sqrt(2.0)))

    /** Inverse standard normal, by bisection on [normalCdf]. Deterministic and monotone. */
    fun normalQuantile(p: Double): Double {
        val target = p.coerceIn(1e-9, 1.0 - 1e-9)
        var lo = -9.0
        var hi = 9.0
        repeat(90) {
            val mid = (lo + hi) / 2.0
            if (normalCdf(mid) < target) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    private fun erf(x: Double): Double {
        val sign = if (x < 0.0) -1.0 else 1.0
        val ax = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * ax)
        val poly = ((((1.061405429 * t - 1.453152027) * t + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t
        return sign * (1.0 - poly * exp(-ax * ax))
    }
}
