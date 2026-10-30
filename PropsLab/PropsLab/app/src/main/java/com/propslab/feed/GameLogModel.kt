package com.propslab.feed

import com.propslab.data.GAMELOG_MODEL
import com.propslab.domain.Calibration
import com.propslab.domain.Markets
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.PlattFit
import com.propslab.inference.ChampionTable
import com.propslab.inference.ScoreResult
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The model behind every real prediction: a player's own recent games, read as a
 * distribution.
 *
 * It is deliberately simple, and deliberately not a classifier. Given the last games a
 * player actually played, it takes the mean and the spread of the stat the market is
 * written on and asks how often that distribution lands past the line. A normal
 * approximation is honest about what it does not know: the mean is real, the spread is
 * real, and the sample is finite, so the spread is floored rather than allowed to collapse
 * to zero after a few quiet games.
 *
 * What it does not do is claim an edge the market has not offered. The estimate is compared
 * to the book's own no-vig price like any other prediction, and the sandbox gate — not this
 * file — decides whether the disagreement is worth money.
 *
 * Integer lines get a real push mass, computed from the same distribution, because a push
 * is a third outcome and not a rounding detail.
 */
object GameLogModel {

    const val MODEL_VERSION = GAMELOG_MODEL

    /** At this many games the estimate is published at full strength. */
    const val MIN_GAMES = 8

    /**
     * Early in a season nobody has eight games. Between this floor and [MIN_GAMES] the
     * estimate is published but pulled toward 50% by the fraction of a full sample the
     * player actually has, so a four-game heater cannot look like a twenty-game edge.
     */
    const val THIN_MIN_GAMES = 4

    /** How many of the most recent games feed the estimate. */
    const val WINDOW = 20

    /** The spread never falls below this fraction of the mean… */
    const val SD_FLOOR_FRACTION = 0.30

    /** …nor below this absolute floor, which keeps low-count markets like threes sane. */
    const val SD_FLOOR_ABS = 1.0

    /** A push cannot be more likely than this; the clamp keeps a coarse line from dominating. */
    const val MAX_PUSH = 0.20

    /** The stat this market is written on, or null for a market the model cannot price. */
    fun statValue(points: Int, rebounds: Int, assists: Int, threes: Int, market: String): Double? =
        when (market) {
            Markets.POINTS -> points.toDouble()
            Markets.REBOUNDS -> rebounds.toDouble()
            Markets.ASSISTS -> assists.toDouble()
            Markets.THREES -> threes.toDouble()
            Markets.PRA -> (points + rebounds + assists).toDouble()
            else -> null
        }

    /**
     * @param values the player's stats, newest first. Order does not change the answer; it
     *   is only used to cut the list to [WINDOW].
     * @param availability a multiplier on the *expectation*, from a late availability
     *   report. It scales the mean and the spread together, because a player who plays less
     *   also varies less, and a zero means the player is not playing at all.
     * @param market the market being priced. Points can use the Kaggle empirical table;
     *   every other market uses the normal approximation on the same game log.
     * @param table the exported Kaggle artifact, when the build ships one.
     * @param calibration the correction learned from settled bets. Identity is a no-op.
     */
    fun score(
        values: List<Double>,
        line: Double,
        availability: Double = 1.0,
        market: String = "",
        table: ChampionTable? = null,
        calibration: PlattFit = PlattFit.IDENTITY,
    ): ScoreResult {
        if (availability <= 0.0) {
            return ScoreResult.Unavailable("a late report says this player will not play")
        }
        val sample = values.filter { it.isFinite() }.take(WINDOW)
        if (sample.size < THIN_MIN_GAMES) {
            return ScoreResult.Unavailable("only ${sample.size} games logged, and $THIN_MIN_GAMES are needed")
        }
        if (!line.isFinite() || line <= 0.0) {
            return ScoreResult.Unavailable("no usable line")
        }
        val factor = availability.coerceIn(0.0, 1.0)
        val mean = sample.average() * factor
        val empirical = if (market == Markets.POINTS && table != null) {
            table.probabilities(mean, line)
        } else null
        val base = empirical ?: normal(sample, mean, line, factor)
            ?: return ScoreResult.Unavailable("the game log has no spread")
        val pulled = thinSample(base, sample.size)
        return ScoreResult.Scored(Calibration.apply(calibration, pulled))
    }

    private fun normal(sample: List<Double>, mean: Double, line: Double, factor: Double): OutcomeProbabilities? {
        val variance = sample.sumOf { (it - sample.average()) * (it - sample.average()) } / (sample.size - 1)
        val sd = max(sqrt(variance) * factor, max(mean * SD_FLOOR_FRACTION, SD_FLOOR_ABS))
        if (!sd.isFinite() || sd <= 0.0) return null
        return if (line % 1.0 == 0.0) {
            // X >= line + 1 is an over, X == line is a push, X <= line - 1 is an under.
            val hi = cdf((line + 0.5 - mean) / sd)
            val lo = cdf((line - 0.5 - mean) / sd)
            combine(over = 1.0 - hi, push = hi - lo, under = lo)
        } else {
            // A half-point line cannot push.
            val p = cdf((line - mean) / sd)
            combine(over = 1.0 - p, push = 0.0, under = p)
        }
    }

    /**
     * Pulls a short sample's probabilities toward an even split of the non-push mass.
     * Eight games or more is returned untouched; four games halves the excess.
     */
    private fun thinSample(probs: OutcomeProbabilities, games: Int): OutcomeProbabilities {
        if (games >= MIN_GAMES) return probs
        val factor = (games.toDouble() / MIN_GAMES).coerceIn(0.0, 1.0)
        val rest = 1.0 - probs.push
        val mid = rest / 2.0
        val over = mid + (probs.over - mid) * factor
        val under = (rest - over).coerceAtLeast(0.0)
        return OutcomeProbabilities(over.coerceIn(0.0, rest), probs.push, under)
    }

    fun meanAndSpread(values: List<Double>): Pair<Double, Double> {
        if (values.isEmpty()) return 0.0 to 0.0
        val mean = values.average()
        val variance = if (values.size > 1) {
            values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        } else 0.0
        return mean to sqrt(variance)
    }

    /** Keeps every probability finite, inside [0,1] and summing to one. */
    private fun combine(over: Double, push: Double, under: Double): OutcomeProbabilities {
        val p = push.coerceIn(0.0, MAX_PUSH)
        val rest = 1.0 - p
        val total = over + under
        if (!total.isFinite() || total <= 0.0) return OutcomeProbabilities(rest / 2.0, p, rest / 2.0)
        val o = (rest * over / total).coerceIn(0.0, rest)
        val u = (rest - o).coerceIn(0.0, rest)
        return OutcomeProbabilities(o, p, u)
    }

    /** Standard normal CDF, via Abramowitz & Stegun 7.1.26 (absolute error under 1.5e-7). */
    fun cdf(z: Double): Double = 0.5 * (1.0 + erf(z / sqrt(2.0)))

    private fun erf(x: Double): Double {
        val sign = if (x < 0.0) -1.0 else 1.0
        val ax = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * ax)
        val poly = ((((1.061405429 * t - 1.453152027) * t + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t
        return sign * (1.0 - poly * exp(-ax * ax))
    }
}
