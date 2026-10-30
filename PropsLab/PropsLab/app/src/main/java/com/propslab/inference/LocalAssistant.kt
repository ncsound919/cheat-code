package com.propslab.inference

import com.propslab.domain.OutcomeProbabilities
import kotlin.math.abs

data class ExplainInput(
    val player: String,
    val line: Double,
    val probabilities: OutcomeProbabilities,
    val marketOver: Double,
    val flags: List<String>,
)

/**
 * Receives a structured prediction record and returns prose. It never returns the authoritative probability.
 * [com.propslab.mathx.MathXAssistant] wraps a [com.propslab.mathx.LocalMathModel] and falls back to
 * [TemplateAssistant] when no weights are installed; its guard rejects any percentage that was not supplied.
 */
interface LocalAssistant {
    suspend fun explain(input: ExplainInput): String
}

class TemplateAssistant : LocalAssistant {
    override suspend fun explain(input: ExplainInput): String {
        val edge = (input.probabilities.overGivenNoPush - input.marketOver) * 100
        val lean = if (edge >= 0) "Over" else "Under"
        val size = when {
            abs(edge) < 2 -> "barely differs from"
            abs(edge) < 5 -> "sits modestly away from"
            else -> "sits well away from"
        }
        val flags = if (input.flags.isEmpty()) "No data-quality flags were raised."
        else "Worth knowing: " + input.flags.joinToString("; ") { it.replaceFirstChar(Char::lowercase) } + "."
        return "The estimate for ${input.player} on ${input.line} points leans $lean. " +
            "It $size the market's no-vig price, by ${"%.1f".format(abs(edge))} points. $flags"
    }
}
