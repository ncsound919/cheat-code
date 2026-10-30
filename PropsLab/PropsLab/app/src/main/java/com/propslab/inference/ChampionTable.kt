package com.propslab.inference

import com.propslab.domain.OutcomeProbabilities
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.floor

/**
 * The Kaggle empirical distribution, exported from the champion run and shipped as an
 * asset.
 *
 * The Kaggle comparison promoted a constrained GBM, but its twelve features include
 * minutes, usage, rest, home/away and a prior-season count — columns the phone's odds and
 * box-score feed does not carry. Running that bundle here would mean filling five features
 * with invented numbers, so the app runs the artifact that actually fits its data instead:
 * the champion table, `P(points <= k | trailing mean bucket)` measured over 594k real
 * player-games. It needs only the trailing mean and the line, and it gives an integer line
 * a real push mass rather than a normal-approximation guess.
 *
 * It is points-only on purpose. The training dataset was points-only, so every other
 * market stays on the game-log normal model and the screen says which one priced the row.
 */
class ChampionTable(
    val bucketWidth: Double,
    val kmax: Int,
    private val cdf: List<DoubleArray>,
    val fitRows: Int,
    val fitThrough: String,
    val sourceDataset: String,
    val sha256: String,
) {

    val minMu: Double get() = 0.0
    val maxMu: Double get() = (cdf.size - 1) * bucketWidth

    /** A short provenance line for the Lab. */
    val provenance: String
        get() = "$sourceDataset · %,d player-games through %s · %s".format(fitRows, fitThrough, sha256.take(12))

    /**
     * P(over), P(push), P(under) for a points line. Null when the line falls outside what
     * the table can speak to, so the caller can fall back rather than publish a 0% or 100%.
     */
    fun probabilities(mu: Double, line: Double): OutcomeProbabilities? {
        if (!mu.isFinite() || !line.isFinite()) return null
        if (line < 0.5 || line > kmax + 0.5) return null
        val integer = abs(line - Math.rint(line)) < 1e-9
        val over: Double
        val push: Double
        val under: Double
        if (integer) {
            val at = line.toInt()
            val le = cdfAt(at, mu)
            val lt = cdfAt(at - 1, mu)
            push = (le - lt).coerceAtLeast(0.0)
            over = (1.0 - le).coerceAtLeast(0.0)
            under = lt.coerceAtLeast(0.0)
        } else {
            val at = floor(line).toInt()
            val le = cdfAt(at, mu)
            push = 0.0
            over = (1.0 - le).coerceAtLeast(0.0)
            under = le.coerceAtLeast(0.0)
        }
        val total = over + push + under
        if (total <= 0.0) return null
        val o = over / total
        val t = push / total
        val u = under / total
        // A row the table says is essentially decided tells the strategy nothing.
        if (o < 0.001 || u < 0.001) return null
        return OutcomeProbabilities(o, t, u)
    }

    /** P(points <= k | mu), interpolated linearly between the two nearest mean buckets. */
    fun cdfAt(k: Int, mu: Double): Double {
        if (k < 0) return 0.0
        if (k >= kmax) return 1.0
        val raw = mu.coerceIn(minMu, maxMu) / bucketWidth
        val low = floor(raw).toInt().coerceIn(0, cdf.size - 1)
        val high = (low + 1).coerceAtMost(cdf.size - 1)
        val fraction = (raw - low).coerceIn(0.0, 1.0)
        val a = cdf[low][k]
        val b = cdf[high][k]
        return (a + (b - a) * fraction).coerceIn(0.0, 1.0)
    }

    companion object {

        /** Parses the exported JSON. Null means the artifact is unreadable, never a guess. */
        fun fromJson(text: String): ChampionTable? {
            val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
            if (root.optString("kind") != "propslab_empirical_distribution") return null
            val width = root.optDouble("bucket_width", 0.0)
            val kmax = root.optInt("kmax", 0)
            if (width <= 0.0 || kmax <= 0) return null
            val array = root.optJSONArray("cdf") ?: return null
            if (array.length() < 2) return null
            val rows = ArrayList<DoubleArray>(array.length())
            for (i in 0 until array.length()) {
                val row = array.optJSONArray(i) ?: return null
                if (row.length() != kmax + 1) return null
                val values = DoubleArray(row.length()) { index ->
                    val v = row.optDouble(index, Double.NaN)
                    if (!v.isFinite()) return null
                    v
                }
                for (k in 1 until values.size) {
                    if (values[k] + 1e-9 < values[k - 1]) return null
                }
                rows += values
            }
            return ChampionTable(
                bucketWidth = width,
                kmax = kmax,
                cdf = rows,
                fitRows = root.optInt("fit_rows", 0),
                fitThrough = root.optString("fit_max_season", ""),
                sourceDataset = root.optString("source_dataset", ""),
                sha256 = root.optString("source_sha256", ""),
            )
        }
    }
}
