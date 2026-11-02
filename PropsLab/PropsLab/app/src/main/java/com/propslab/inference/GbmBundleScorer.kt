package com.propslab.inference

import com.propslab.domain.OutcomeProbabilities
import kotlin.math.exp

/**
 * Parsed representation of a `propslab_gbm_bundle` JSON artifact (see
 * `research/kaggle/export_gbm.py`). The Python reference is `predict_bundle`;
 * this file must stay bit-for-bit aligned with it.
 *
 * The bundle is 4 cross-fitted HistGradientBoostingClassifier ensembles plus 4
 * isotonic step functions. Learning rate and baseline are baked into the leaf
 * values and baseline respectively, so the walker does NOT multiply by a learning
 * rate.
 */
data class GbmNode(
    val leaf: Boolean,
    val value: Double = 0.0,            // leaf contribution (already scaled)
    val feature: Int = -1,
    val threshold: Double = 0.0,        // num_threshold; split goes left when x <= threshold
    val missingGoLeft: Boolean = false,
    val left: Int = -1,
    val right: Int = -1,
)

data class GbmTree(val nodes: List<GbmNode>)

data class GbmIsotonic(val xs: DoubleArray, val ys: DoubleArray)

data class GbmFold(val baseline: Double, val trees: List<GbmTree>, val isotonic: GbmIsotonic)

data class GbmBundle(
    val featureSchemaVersion: Int,
    val features: List<String>,
    val folds: List<GbmFold>,
)

/**
 * Scores P(over) using the promoted constrained GBM. Mirrors `predict_bundle`
 * in `research/kaggle/export_gbm.py` exactly:
 *
 *   for each fold:
 *     raw = fold.baseline
 *     for each tree: raw += walk(tree, x)        # leaf value, already scaled
 *     prob = sigmoid(raw)
 *     calibrated = interp(prob, fold.isotonic)    # linear, clipped to range
 *   p = mean(calibrated over folds)
 *   p = clip(p, 1e-6, 1 - 1e-6)
 *
 * Push mass is not modelled (the grid uses half-point lines only), so integer
 * lines are rejected, exactly like [LogisticBundleScorer].
 */
class GbmBundleScorer(private val bundle: GbmBundle) : StatisticalScorer {

    override fun score(request: ScoreRequest): ScoreResult {
        if (request.featureSchemaVersion != bundle.featureSchemaVersion) {
            return ScoreResult.Unavailable(
                "Feature schema ${request.featureSchemaVersion} does not match bundle schema ${bundle.featureSchemaVersion}")
        }
        if (request.line % 1.0 != 0.5) {
            return ScoreResult.Unavailable("Integer lines need a push model")
        }
        // Validate the whole frozen schema, not only the features this tree walk
        // happens to touch. A missing input must never silently score a row.
        for (name in bundle.features) {
            val x = request.features[name] ?: return ScoreResult.Unavailable("Missing feature: $name")
            if (x.isInfinite()) return ScoreResult.Unavailable("Non-finite feature: $name")
        }

        val calibrated = DoubleArray(bundle.folds.size)
        for (fi in bundle.folds.indices) {
            val fold = bundle.folds[fi]
            var raw = fold.baseline
            for (tree in fold.trees) {
                raw += walk(tree.nodes, request.features, bundle.features)
            }
            // No epsilon clamp here: `predict_bundle` feeds sigmoid(raw) straight
            // into np.interp, which already clips to the isotonic endpoints. A
            // clamp would diverge from the reference wherever the map does not
            // start at 0 / end at 1.
            val prob = sigmoid(raw)
            calibrated[fi] = interp(prob, fold.isotonic.xs, fold.isotonic.ys)
        }
        val meanCalibrated = calibrated.average()
        if (!meanCalibrated.isFinite()) {
            return ScoreResult.Unavailable("Scorer produced a non-finite value")
        }
        val p = meanCalibrated.coerceIn(1e-6, 1.0 - 1e-6)
        return ScoreResult.Scored(OutcomeProbabilities(p, 0.0, 1.0 - p))
    }

    private fun walk(nodes: List<GbmNode>, features: Map<String, Double>, featureOrder: List<String>): Double {
        var i = 0
        var guard = 0
        while (true) {
            val node = nodes[i]
            if (node.leaf) return node.value
            val name = featureOrder[node.feature]
            val x = features[name] ?: return Double.NaN
            if (!x.isFinite() && !x.isNaN()) return Double.NaN  // reject Inf, allow NaN (handled as missing)
            val goLeft = if (x.isNaN()) node.missingGoLeft else x <= node.threshold
            i = if (goLeft) node.left else node.right
            if (i < 0 || i >= nodes.size) return Double.NaN
            if (++guard > nodes.size) return Double.NaN
        }
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))

    private fun interp(x: Double, xs: DoubleArray, ys: DoubleArray): Double {
        // np.interp equivalent: linear, clipped to endpoints.
        if (xs.isEmpty()) return x
        if (x <= xs.first()) return ys.first()
        if (x >= xs.last()) return ys.last()
        // binary search
        var lo = 0
        var hi = xs.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (x < xs[mid]) hi = mid else lo = mid
        }
        val dx = xs[hi] - xs[lo]
        if (dx == 0.0) return ys[lo]
        val t = (x - xs[lo]) / dx
        return ys[lo] + t * (ys[hi] - ys[lo])
    }

    companion object {
        /**
         * Parse a `propslab_gbm_bundle` JSON document. Uses org.json, which is
         * provided by the Android runtime (API 29+) and by the test classpath.
         */
        fun fromJson(json: String): GbmBundle {
            val root = org.json.JSONObject(json)
            val features = mutableListOf<String>()
            val fArr = root.getJSONArray("features")
            for (i in 0 until fArr.length()) features.add(fArr.getString(i))
            val folds = mutableListOf<GbmFold>()
            val foldsArr = root.getJSONArray("folds")
            for (fi in 0 until foldsArr.length()) {
                val f = foldsArr.getJSONObject(fi)
                val trees = mutableListOf<GbmTree>()
                val treesArr = f.getJSONArray("trees")
                for (ti in 0 until treesArr.length()) {
                    val t = treesArr.getJSONObject(ti)
                    val nodesArr = t.getJSONArray("nodes")
                    val nodes = mutableListOf<GbmNode>()
                    for (ni in 0 until nodesArr.length()) {
                        val n = nodesArr.getJSONObject(ni)
                        if (n.getBoolean("leaf")) {
                            nodes.add(GbmNode(leaf = true, value = n.getDouble("value")))
                        } else {
                            nodes.add(GbmNode(
                                leaf = false,
                                feature = n.getInt("feature"),
                                threshold = n.getDouble("threshold"),
                                missingGoLeft = n.getBoolean("missing_go_left"),
                                left = n.getInt("left"),
                                right = n.getInt("right"),
                            ))
                        }
                    }
                    trees.add(GbmTree(nodes))
                }
                val iso = f.getJSONObject("isotonic")
                val xsArr = iso.getJSONArray("x")
                val ysArr = iso.getJSONArray("y")
                val xs = DoubleArray(xsArr.length()) { xsArr.getDouble(it) }
                val ys = DoubleArray(ysArr.length()) { ysArr.getDouble(it) }
                folds.add(GbmFold(
                    baseline = f.getDouble("baseline"),
                    trees = trees,
                    isotonic = GbmIsotonic(xs, ys),
                ))
            }
            return GbmBundle(
                featureSchemaVersion = root.getInt("feature_schema_version"),
                features = features,
                folds = folds,
            )
        }
    }
}
