package com.propslab.mathx

import com.propslab.inference.ExplainInput
import com.propslab.inference.LocalAssistant
import com.propslab.inference.TemplateAssistant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * Runs Math X's probability mode on a local model, with two hard guards:
 *
 * 1. No model, no generation. The deterministic template is used instead.
 * 2. A reply may only mention percentages that were supplied to it. Any invented
 *    probability rejects the whole reply, because the assistant is never the source
 *    of a probability.
 */
class MathXAssistant(
    private val model: LocalMathModel,
    private val fallback: LocalAssistant = TemplateAssistant(),
    private val clock: () -> Long = System::currentTimeMillis,
) : LocalAssistant {

    /**
     * When the phone could not finish a reply in time, the template is the answer anyway.
     *
     * A sync asks for a sentence on every priced prop, and on a device where one prompt pass
     * already takes longer than the timeout each of those is a full CPU pass that is thrown
     * away. After one miss the model is left alone for a while instead of being asked the same
     * question sixteen hundred times.
     */
    @Volatile
    private var quietUntilUtc = 0L

    override suspend fun explain(input: ExplainInput): String {
        if (model.status() !is LocalModelStatus.Ready) return fallback.explain(input)
        if (clock() < quietUntilUtc) return fallback.explain(input)

        val prompt = MathXPrompts.build(query(input), MathXMode.PROBABILITY)
        // A local model must never hold up Room seeding or a screen. Timeout is a
        // ceiling, not an expectation: three short sentences should be far quicker.
        val raw = withTimeoutOrNull(REPLY_TIMEOUT_MS) {
            try {
                model.complete(prompt, MathXPrompts.SYSTEM, maxTokens = MAX_TOKENS)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }?.trim()
        if (raw.isNullOrEmpty() || raw.length > MAX_REPLY_CHARS) {
            // The turn did not arrive: too slow or refused. Stop asking for a while.
            quietUntilUtc = clock() + QUIET_MS
            return fallback.explain(input)
        }

        return if (AssistantGuard.isSafe(raw, allowedPercents(input))) raw else fallback.explain(input)
    }

    private fun query(input: ExplainInput): String {
        val edge = (input.probabilities.overGivenNoPush - input.marketOver) * 100.0
        val lean = if (edge >= 0.0) "Over" else "Under"
        val flags = if (input.flags.isEmpty()) "none" else input.flags.joinToString("; ")
        return buildString {
            append("Stored prediction for ${input.player} on ${input.line} points.\n")
            append(
                "Supplied numbers: model over ${pct(input.probabilities.over)}, " +
                    "push ${pct(input.probabilities.push)}, under ${pct(input.probabilities.under)}; " +
                    "over given no push ${pct(input.probabilities.overGivenNoPush)}; " +
                    "no-vig market over ${pct(input.marketOver)}; " +
                    "edge ${"%.1f".format(abs(edge))} points to the $lean.\n",
            )
            append("Data-quality flags: $flags.\n")
            append(
                "Explain why the model differs from the market in at most three sentences. " +
                    "Use only the supplied numbers. Do not calculate a new probability.",
            )
        }
    }

    /** Every percentage the model is allowed to repeat, in percent units. */
    private fun allowedPercents(input: ExplainInput): List<Double> {
        val edge = (input.probabilities.overGivenNoPush - input.marketOver) * 100.0
        return listOf(
            input.probabilities.over * 100.0,
            input.probabilities.push * 100.0,
            input.probabilities.under * 100.0,
            input.probabilities.overGivenNoPush * 100.0,
            input.marketOver * 100.0,
            abs(edge),
        )
    }

    private fun pct(value: Double): String = "${"%.1f".format(value * 100.0)}%"

    private companion object {
        const val MAX_REPLY_CHARS = 1200
        const val MAX_TOKENS = 192
        const val REPLY_TIMEOUT_MS = 6_000L

        /** How long the model is left alone after it misses a reply. */
        const val QUIET_MS = 10 * 60 * 1000L
    }
}

/**
 * The guard is intentionally a whitelist, not a numerical fact-checker. It cannot
 * tell whether a sentence is true, only that every percentage in it came from the
 * prediction record.
 */
object AssistantGuard {
    private val percent = Regex("""(\d+(?:\.\d+)?)\s*%""")

    fun isSafe(text: String, allowedPercents: List<Double>, tolerance: Double = 0.06): Boolean {
        if (text.isBlank()) return false
        return percent.findAll(text).all { match ->
            val value = match.groupValues[1].toDoubleOrNull() ?: return@all false
            allowedPercents.any { abs(it - value) <= tolerance }
        }
    }
}
