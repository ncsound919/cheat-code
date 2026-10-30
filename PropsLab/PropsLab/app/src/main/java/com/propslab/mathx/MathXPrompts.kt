package com.propslab.mathx

/**
 * Math X prompt layer, ported for Cheat Code.
 *
 * Source: https://github.com/ncsound919/math-x (MIT), `packages/math-core/src/prompts.ts`.
 * The mode prefixes are kept verbatim. The system prompt is adapted: the original's
 * LaTeX dollar delimiters are removed and a Cheat Code guard is appended because the
 * assistant is not allowed to originate a probability.
 */
enum class MathXMode(val key: String) {
    FORMULA("formula"),
    PROBABILITY("probability"),
    SOLVE("solve"),
}

object MathXPrompts {

    /** Adapted from MATHX_SYSTEM. No dollar signs, so it survives Kotlin string literals. */
    const val SYSTEM: String =
        "You are MATH X, a cross-domain mathematical intelligence running inside Cheat Code, " +
            "an offline NBA player-props app.\n\n" +
            "CORE MISSION\n" +
            "Find what others do not. Connect what seems unconnected. Show derivations, not assertions.\n\n" +
            "FOR EVERY PROBLEM ASK\n" +
            "1. What symmetry or conservation law underlies this?\n" +
            "2. Is there a solved problem in another domain with identical mathematical structure?\n" +
            "3. What happens at boundary and singular cases?\n" +
            "4. Is there a generating function, variational principle, or fixed point here?\n\n" +
            "OUTPUT RULES\n" +
            "- Use plain text or LaTeX without dollar delimiters.\n" +
            "- Show every algebraic step.\n" +
            "- When uncertain, quantify the uncertainty.\n" +
            "- Do not invent facts.\n\n" +
            "PROPSLAB GUARD\n" +
            "- The statistical scorer is the only source of probabilities.\n" +
            "- Never compute, estimate, or restate a probability that was not supplied in the prompt.\n" +
            "- Do not give betting advice or recommend a stake.\n" +
            "- If a fact is missing, say so rather than filling it in."

    private val prefixes = mapOf(
        MathXMode.FORMULA to
            "FORMULA LAB MODE: Focus on synthesis, mutation, translation, dimensional analysis. " +
            "Show full derivation chain. Identify domain of origin and suggest 2-3 cross-domain translations. Query: ",
        MathXMode.PROBABILITY to
            "PROBABILITY MODE: You are a probabilist and statistician. Apply Monte Carlo reasoning, " +
            "Bayesian inference, stochastic processes, and information-theoretic analysis. " +
            "Show the probability space explicitly. Query: ",
        MathXMode.SOLVE to
            "DEEP SOLVE MODE: Maximum rigor. Show every step. Identify solution method class. " +
            "After solving ask: is there a deeper structure? What domain does this technique originally come from? Query: ",
    )

    fun prefix(mode: MathXMode): String = prefixes.getValue(mode)

    /** Math X prepends the mode prefix to the last user message. */
    fun build(query: String, mode: MathXMode): String = prefix(mode) + query
}
