package com.propslab.inference

import com.propslab.domain.OutcomeProbabilities
import kotlin.math.exp
import kotlin.math.ln

data class ScoreRequest(val line: Double, val features: Map<String, Double>, val featureSchemaVersion: Int)

sealed interface ScoreResult {
    data class Scored(val probabilities: OutcomeProbabilities) : ScoreResult
    data class Unavailable(val reason: String) : ScoreResult
}

/** The statistical scorer is the only source of probabilities. The LLM never overwrites them. */
interface StatisticalScorer {
    fun score(request: ScoreRequest): ScoreResult
}

/** Exported from Kaggle: standardised logistic regression plus Platt calibration on the logit. */
class LogisticBundle(
    val schemaVersion: Int,
    val features: List<String>,
    val mean: DoubleArray,
    val scale: DoubleArray,
    val coef: DoubleArray,
    val intercept: Double,
    val plattA: Double,
    val plattB: Double,
)

class LogisticBundleScorer(private val bundle: LogisticBundle) : StatisticalScorer {
    override fun score(request: ScoreRequest): ScoreResult {
        if (request.featureSchemaVersion != bundle.schemaVersion) {
            return ScoreResult.Unavailable("Feature schema ${request.featureSchemaVersion} does not match bundle schema ${bundle.schemaVersion}")
        }
        // A classifier has no push mass, so only half-point lines are safe.
        if (request.line % 1.0 != 0.5) return ScoreResult.Unavailable("Integer lines need a push model")

        var z = bundle.intercept
        for (i in bundle.features.indices) {
            val name = bundle.features[i]
            val x = request.features[name] ?: return ScoreResult.Unavailable("Missing feature: $name")
            if (!x.isFinite()) return ScoreResult.Unavailable("Non-finite feature: $name")
            z += bundle.coef[i] * (x - bundle.mean[i]) / bundle.scale[i]
        }
        val raw = sigmoid(z).coerceIn(1e-6, 1 - 1e-6)
        val calibrated = sigmoid(bundle.plattA * ln(raw / (1 - raw)) + bundle.plattB)
        if (!calibrated.isFinite()) return ScoreResult.Unavailable("Scorer produced a non-finite value")
        return ScoreResult.Scored(OutcomeProbabilities(calibrated, 0.0, 1.0 - calibrated))
    }

    private fun sigmoid(z: Double) = 1.0 / (1.0 + exp(-z))
}
