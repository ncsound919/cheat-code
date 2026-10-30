package com.propslab.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/** One settled bet as the calibration needs it: what the model said, and what happened. */
data class CalibrationPoint(val probability: Double, val won: Boolean)

/**
 * A fitted Platt correction on the logit of the model's probability.
 *
 * `slope` measures whether the model is too confident (below 1) or not confident enough
 * (above 1); `intercept` is a level shift. Applied to the over/under chance of a settled
 * bet, it is the difference between a model that is merely directional and one that can
 * be priced.
 */
data class PlattFit(
    val slope: Double,
    val intercept: Double,
    val sample: Int,
    val brierBefore: Double,
    val brierAfter: Double,
    val generation: Int,
) {
    val identity: Boolean get() = sample == 0 && slope == 1.0 && intercept == 0.0
    val improvement: Double get() = brierBefore - brierAfter

    companion object {
        val IDENTITY = PlattFit(1.0, 0.0, 0, 0.0, 0.0, 0)
    }
}

/**
 * The self-learning half of the model: a two-parameter correction fitted from the bets
 * the system has actually logged and settled.
 *
 * It is intentionally small. A phone does not have the data to relearn a gradient-boosted
 * model, but it does have enough settled bets to answer one question honestly: when the
 * model said 60%, did 60% happen? The fit is ridge-regularised toward the identity, so a
 * short or noisy record moves the correction a little rather than rewriting the model,
 * and a fit that wanders outside a sane range is refused rather than shipped.
 *
 * No holdout is claimed. The Brier figures are in-sample and the UI says so; the only
 * defence against overfitting is the regularization and the minimum sample.
 */
object Calibration {

    /** Below this many settled decisions, identity is the honest answer. */
    const val MIN_SAMPLE = 200

    /** Ridge weight toward the identity fit. */
    const val STRENGTH = 30.0

    const val MIN_SLOPE = 0.25
    const val MAX_SLOPE = 3.0
    const val MAX_INTERCEPT = 1.5

    fun fit(
        points: List<CalibrationPoint>,
        prior: PlattFit = PlattFit.IDENTITY,
        minSample: Int = MIN_SAMPLE,
    ): PlattFit? {
        val usable = points.filter { it.probability.isFinite() && it.probability > 0.001 && it.probability < 0.999 }
        if (usable.size < minSample) return null
        val xs = DoubleArray(usable.size)
        val ys = DoubleArray(usable.size)
        for (i in usable.indices) {
            val p = usable[i].probability
            xs[i] = ln(p / (1.0 - p))
            ys[i] = if (usable[i].won) 1.0 else 0.0
        }

        var a = prior.slope.coerceIn(MIN_SLOPE, MAX_SLOPE)
        var b = prior.intercept.coerceIn(-MAX_INTERCEPT, MAX_INTERCEPT)
        repeat(40) {
            var g0 = 0.0
            var g1 = 0.0
            var h00 = 0.0
            var h01 = 0.0
            var h11 = 0.0
            for (i in xs.indices) {
                val z = a * xs[i] + b
                val q = sigmoid(z)
                val w = (q * (1.0 - q)).coerceAtLeast(1e-9)
                g0 += (q - ys[i]) * xs[i]
                g1 += q - ys[i]
                h00 += w * xs[i] * xs[i]
                h01 += w * xs[i]
                h11 += w
            }
            g0 += STRENGTH * (a - 1.0)
            g1 += STRENGTH * b
            h00 += STRENGTH
            h11 += STRENGTH
            val det = h00 * h11 - h01 * h01
            if (det <= 1e-12) return@repeat
            val da = (h11 * g0 - h01 * g1) / det
            val db = (h00 * g1 - h01 * g0) / det
            a -= da
            b -= db
            if (abs(da) < 1e-10 && abs(db) < 1e-10) return@repeat
        }
        if (!a.isFinite() || !b.isFinite()) return null
        if (a < MIN_SLOPE || a > MAX_SLOPE) return null
        if (b < -MAX_INTERCEPT || b > MAX_INTERCEPT) return null

        val before = brier(usable) { it.probability }
        val after = brier(usable) { apply(a, b, it.probability) }
        return PlattFit(
            slope = a,
            intercept = b,
            sample = usable.size,
            brierBefore = before,
            brierAfter = after,
            generation = prior.generation + 1,
        )
    }

    /** Applies a fit to a probability. Identity is a no-op, so an unfitted model is unchanged. */
    fun apply(fit: PlattFit, probability: Double): Double = apply(fit.slope, fit.intercept, probability)

    private fun apply(slope: Double, intercept: Double, probability: Double): Double {
        if (!probability.isFinite()) return probability
        val p = probability.coerceIn(1e-6, 1.0 - 1e-6)
        return sigmoid(slope * ln(p / (1.0 - p)) + intercept).coerceIn(1e-4, 1.0 - 1e-4)
    }

    /**
     * Applies a correction to a win/push/under distribution. The push mass is the line's
     * property, not the model's opinion, so it is held fixed and the two win probabilities
     * are renormalised into what remains.
     */
    fun apply(fit: PlattFit, probabilities: OutcomeProbabilities): OutcomeProbabilities {
        if (fit.identity) return probabilities
        val win = probabilities.over / (1.0 - probabilities.push).coerceAtLeast(1e-9)
        val corrected = apply(fit, win)
        val room = 1.0 - probabilities.push
        return OutcomeProbabilities(
            over = (corrected * room).coerceIn(0.0, room),
            push = probabilities.push,
            under = ((1.0 - corrected) * room).coerceIn(0.0, room),
        )
    }

    private fun brier(points: List<CalibrationPoint>, probability: (CalibrationPoint) -> Double): Double =
        points.sumOf { point ->
            val p = probability(point).coerceIn(0.0, 1.0)
            val y = if (point.won) 1.0 else 0.0
            (p - y) * (p - y)
        } / points.size

    private fun sigmoid(z: Double): Double {
        if (z >= 0.0) {
            val e = exp(-z)
            return 1.0 / (1.0 + e)
        }
        val e = exp(z)
        return e / (1.0 + e)
    }
}
