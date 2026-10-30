package com.propslab.sim

import com.propslab.domain.Side
import kotlin.math.roundToInt

/**
 * The hidden truth of the simulated world.
 *
 * This is the one place where the sandbox is deliberately allowed to be a rigged world,
 * so it is spelled out rather than buried:
 *
 *  - The market's no-vig price is not the truth. The truth is a blend of the market and
 *    the model: `truth = 0.70 · model + 0.30 · market`, plus a small per-prop wobble.
 *  - That blend is what makes an edge *findable*: where the model disagrees with the
 *    market by a wide margin, the truth leans toward the model by 70% of that gap. A
 *    formula that demands a wide disagreement therefore bets props whose real
 *    over-probability beats the price it paid.
 *  - Where the model and the market agree, there is no edge, and the book's margin eats
 *    the bet. So "bet everything" loses, and the search has something real to separate.
 *
 * A 70/30 blend is a strong assumption — it describes a model with most of the signal and
 * a soft market, which is not what real NBA props look like. It is a simulator, and the
 * sandbox answers exactly one question: can the formula search recover an edge that is
 * known to be there? It does not claim the recovered edge exists in the real market.
 *
 * Every draw is keyed by the prop id, so the same season always produces the same
 * results, and two formulas betting the same prop always see the same outcome.
 */
object SandboxTruth {

    /** How much of the model the truth agrees with. The rest follows the market. */
    const val MODEL_WEIGHT = 0.70

    /** Size of the per-prop wobble, in probability units (±). */
    const val WOBBLE = 0.03

    /** The realised over-probability the world actually uses for this prop. */
    fun truthOver(prop: SimProp): Double {
        val market = prop.marketOver ?: return 0.5
        val model = prop.probabilities?.overGivenNoPush ?: return market
        val ripe = MODEL_WEIGHT * model + (1.0 - MODEL_WEIGHT) * market
        val wobble = SandboxHash.signed("${prop.id}:wobble", WOBBLE)
        return (ripe + wobble).coerceIn(0.03, 0.97)
    }

    /**
     * The realised result of the prop, expressed as a number so it settles through the
     * same [com.propslab.domain.Settlement] rules the rest of the app uses.
     *
     * Over becomes `line + 1`, under becomes `line - 1`, and a push lands exactly on the
     * line. A half-point line can never push, and the draw range excludes the push mass
     * for it.
     */
    fun actualFor(prop: SimProp): Double {
        val truthOver = truthOver(prop)
        val push = if (prop.line % 1.0 == 0.0) prop.pPush.coerceIn(0.0, 0.20) else 0.0
        val over = (truthOver * (1.0 - push)).coerceIn(0.0, 1.0 - push)
        val draw = SandboxHash.unit("${prop.id}:result")
        return when {
            draw < over -> prop.line + 1.0
            draw < over + push -> prop.line
            else -> prop.line - 1.0
        }
    }

    /** What the truth says this prop's edge on the given side is, in probability points. */
    fun trueEdgePts(prop: SimProp, side: Side): Double {
        val market = prop.marketOver ?: return 0.0
        val truth = truthOver(prop)
        return if (side == Side.OVER) (truth - market) * 100.0 else (market - truth) * 100.0
    }

    /** Rounded truth, for display. */
    fun truthPct(prop: SimProp): Int = (truthOver(prop) * 100).roundToInt()
}
