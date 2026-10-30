package com.propslab.claim

import com.propslab.sim.GateResult
import com.propslab.sim.PromotionGate
import com.propslab.sim.SandboxEngine
import com.propslab.sim.Season
import com.propslab.sim.SimProp
import com.propslab.sim.SimStats
import com.propslab.sim.StrategyParams
import kotlin.math.abs

/**
 * Tests a compiled claim before it is allowed to become a strategy.
 *
 * The bench is deliberately the same engine the autopilot uses: a fresh simulated season,
 * a flat unit per bet, the same promotion gate. A claim therefore has to earn its place on
 * exactly the evidence a grid formula does, and it cannot pass by being explained well.
 *
 * The search is small on purpose. Three tilts around the claim's own hypothesis and three
 * edge floors is enough to tell whether the direction is worth anything; a finer grid would
 * only fit the season it was run on.
 *
 * The production side of the same idea — 665,203 real player-games on Kaggle — is built by
 * [DeepTest] and is a separate, slower check.
 */
object ClaimBench {

    /** A different season from the autopilot's, so a claim cannot be tuned to the live one. */
    const val SEED_OFFSET = 991L

    data class Trial(
        val params: StrategyParams,
        val stats: SimStats,
        val gate: GateResult,
    ) {
        val label: String get() = params.label
        val passed: Boolean get() = gate.allPassed
    }

    data class Result(
        val formula: ClaimFormula,
        val seasonProps: Int,
        val trials: List<Trial>,
        val chosen: Trial,
        val ready: Boolean,
        /** What the user reads. No derivation, no jargon unless the number is the point. */
        val plain: String,
        /** The sentence shown on the lens board. */
        val boardNote: String,
    )

    fun run(
        formula: ClaimFormula,
        season: List<SimProp> = Season.generate(
            seed = Season.BASE_SEED + SEED_OFFSET,
            endUtc = 1_790_000_000_000L,
        ),
    ): Result {
        val base = formula.lens
        val tilts = listOf(base.tiltPts - 2.0, base.tiltPts, base.tiltPts + 2.0)
            .map { it.coerceIn(-8.0, 8.0) }
            .distinct()
        val edges = listOf(base.minEdgePts - 0.5, base.minEdgePts, base.minEdgePts + 1.0)
            .map { it.coerceAtLeast(1.0) }
            .distinct()

        val trials = buildList {
            for (tilt in tilts) {
                for (edge in edges) {
                    val params = StrategyParams(
                        minEdgePts = edge,
                        sideFilter = base.side,
                        minPrice = base.minPrice,
                        maxPrice = base.maxPrice,
                        tiltPts = tilt,
                        markets = base.markets,
                    )
                    val result = SandboxEngine.run(season, params)
                    add(Trial(params, result.stats, PromotionGate.evaluate(result.stats)))
                }
            }
        }

        val chosen = trials.filter { it.passed }.maxByOrNull { it.stats.profit }
            ?: trials.maxByOrNull { it.stats.profit }
            ?: error("The claim bench produced no trials")

        val ready = chosen.passed
        return Result(
            formula = formula,
            seasonProps = season.size,
            trials = trials,
            chosen = chosen,
            ready = ready,
            plain = plain(chosen, ready, season.size),
            boardNote = boardNote(formula, chosen, ready),
        )
    }

    private fun plain(chosen: Trial, ready: Boolean, seasonProps: Int): String {
        val stats = chosen.stats
        val header = "Tested on ${"%,d".format(seasonProps)} invented props from a season this lens has never seen."
        return if (ready) {
            "$header It took ${stats.bets} bets, returned ${pct(stats.roi)} on stake, and cleared every check. " +
                "It is allowed to compete for the live board."
        } else {
            "$header It took ${stats.bets} bets and returned ${pct(stats.roi)} on stake. " +
                "That failed the bar — ${chosen.gate.summary} — so it stays a research note, not a strategy."
        }
    }

    private fun boardNote(formula: ClaimFormula, chosen: Trial, ready: Boolean): String {
        val direction = when (formula.lens.side) {
            com.propslab.sim.SideFilter.OVER_ONLY -> "leans overs"
            com.propslab.sim.SideFilter.UNDER_ONLY -> "leans unders"
            com.propslab.sim.SideFilter.BOTH -> "takes both sides"
        }
        val tilt = if (abs(chosen.params.tiltPts) < 0.05) "no tilt" else
            "${"%+.1f".format(chosen.params.tiltPts)} pts on the over"
        val verdict = if (ready) "cleared the bar" else "did not clear the bar"
        return "$direction · $tilt · needs a ${"%.1f".format(chosen.params.minEdgePts)}-point gap · $verdict"
    }

    private fun pct(v: Double): String = "%+.1f%%".format(v * 100)
}
