package com.propslab.sim

/**
 * The autonomous loop, with every decision expressed as data.
 *
 * One round does three things and nothing else:
 *  1. Run the whole candidate grid over a fresh simulated season.
 *  2. Gate every candidate on [PromotionGate].
 *  3. Keep the best passing candidate as the promoted formula, holding on to the
 *     incumbent unless it has genuinely stopped passing.
 *
 * "Best" is the candidate with the largest profit, among those that clear every check.
 * Profit is only ever compared between candidates that already proved an edge, so a
 * high-volume formula cannot win by volume alone.
 *
 * Nothing in here touches storage, a clock or the UI. The repository runs it and writes
 * the results down; that split is what makes the loop testable.
 */
object Autopilot {

    /** The search space. Coarse on purpose: a finer grid would only overfit the season. */
    val grid: List<StrategyParams> = buildList {
        val edges = listOf(0.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)
        for (edge in edges) {
            for (filter in SideFilter.entries) {
                add(StrategyParams(minEdgePts = edge, sideFilter = filter))
            }
        }
    }

    data class Candidate(
        val result: SimResult,
        val gate: GateResult,
    ) {
        val id: String get() = result.params.id
        val params: StrategyParams get() = result.params
        val stats: SimStats get() = result.stats
    }

    /**
     * @param incumbent id of the formula currently on the live book, if any.
     */
    data class Plan(
        val candidates: List<Candidate>,
        val promoted: Candidate?,
        val incumbentHeld: Boolean,
        val note: String,
        /** Candidates vetoed by the historical second gate; they cannot hold the live book. */
        val vetoed: Set<String> = emptySet(),
    ) {
        /** Candidates that cleared the gate and the historical check, best first. */
        val ready: List<Candidate>
            get() = candidates.filter { it.gate.allPassed && it.id !in vetoed }
                .sortedByDescending { it.stats.profit }

        /** The formula the UI calls "the lab's current answer", whether promoted or not. */
        val leader: Candidate?
            get() = promoted
                ?: ready.firstOrNull()
                ?: candidates.filter { it.id !in vetoed }.maxByOrNull { it.stats.profit }
    }

    fun plan(
        season: List<SimProp>,
        incumbent: String? = null,
        thresholds: GateThresholds = GateThresholds.STANDARD,
        grid: List<StrategyParams> = this.grid,
        /**
         * Candidates derived from the losing record. They enter the same gate as every
         * other formula, but may also challenge an incumbent that still passes when the
         * fresh season says they earn materially more per unit staked.
         */
        challengers: Set<String> = emptySet(),
        /**
         * Candidates the imported historical dataset refused. They still appear in the
         * result so the screen can show why, but they cannot be promoted or defend a book.
         */
        vetoed: Set<String> = emptySet(),
    ): Plan {
        val candidates = grid
            .map { params ->
                val result = SandboxEngine.run(season, params)
                Candidate(result, PromotionGate.evaluate(result.stats, thresholds))
            }
            .sortedByDescending { it.stats.profit }

        val ready = candidates.filter { it.gate.allPassed && it.id !in vetoed }
            .sortedByDescending { it.stats.profit }
        val incumbentCandidate = candidates.firstOrNull { it.id == incumbent }

        // An incumbent that still passes keeps its place: churn between near-identical
        // formulas is a cost, not a discovery. A loss-informed challenger is the one
        // exception, and only when it clears the same gate and earns a full point more
        // return on stake on a fresh season.
        if (incumbentCandidate != null && incumbentCandidate.gate.allPassed && incumbentCandidate.id !in vetoed) {
            val challenger = ready
                .filter { it.id in challengers && it.id != incumbentCandidate.id }
                .filter {
                    it.stats.roi >= incumbentCandidate.stats.roi + CHALLENGER_ROI_MARGIN &&
                        it.stats.profit > incumbentCandidate.stats.profit
                }
                .maxByOrNull { it.stats.profit }
            if (challenger != null) {
                return Plan(
                    candidates,
                    challenger,
                    incumbentHeld = false,
                    note = "${challenger.id} replaces ${incumbentCandidate.id}: a candidate learned from " +
                        "the losing record cleared every check and earned " +
                        "${pct(challenger.stats.roi)} per unit against the incumbent's " +
                        "${pct(incumbentCandidate.stats.roi)} on a fresh season.",
                    vetoed = vetoed,
                )
            }
            return Plan(
                candidates,
                incumbentCandidate,
                true,
                note = "Incumbent ${incumbentCandidate.id} still clears every check and keeps the live book.",
                vetoed = vetoed,
            )
        }

        val promoted = ready.firstOrNull()
        val note = when {
            promoted != null && incumbentCandidate != null ->
                "${promoted.id} replaces ${incumbentCandidate.id}: the incumbent no longer clears the gate."
            promoted != null ->
                "${promoted.id} cleared every check on ${promoted.stats.bets} bets and moves to the live book."
            else -> {
                val best = candidates.firstOrNull()
                "No formula cleared the gate on this season. " +
                    (best?.gate?.summary?.let { "Best attempt ${best.id}: $it." } ?: "The grid is empty.")
            }
        }
        return Plan(candidates, promoted, incumbentHeld = false, note = note, vetoed = vetoed)
    }

    /**
     * The neighbourhood of the incumbent formula: one step in tilt, in edge threshold and
     * in side filter. These are the candidates a new round adds when a formula already holds
     * the live book, so the search refines what is working instead of only re-testing the
     * same fixed grid. Every mutant still has to clear the same gate on a fresh season; a
     * mutation is a question, never an automatic promotion.
     */
    /** A challenger must beat the incumbent by this much return on stake, not by noise. */
    private const val CHALLENGER_ROI_MARGIN = 0.01

    fun mutations(seed: StrategyParams, limit: Int = 8): List<StrategyParams> {
        val out = buildList {
            for (tilt in listOf(-1.5, 1.5, -3.0, 3.0)) {
                add(seed.copy(tiltPts = (seed.tiltPts + tilt).coerceIn(-12.0, 12.0)))
            }
            add(seed.copy(minEdgePts = (seed.minEdgePts + 1.0).coerceAtMost(12.0)))
            if (seed.minEdgePts > 0.0) {
                add(seed.copy(minEdgePts = (seed.minEdgePts - 1.0).coerceAtLeast(0.0)))
            }
            add(
                seed.copy(
                    sideFilter = when (seed.sideFilter) {
                        SideFilter.BOTH -> SideFilter.OVER_ONLY
                        SideFilter.OVER_ONLY -> SideFilter.UNDER_ONLY
                        SideFilter.UNDER_ONLY -> SideFilter.BOTH
                    },
                ),
            )
        }
        return out.filter { it.id != seed.id }.distinctBy { it.id }.take(limit)
    }

    /**
     * Which of a promoted formula's picks on today's board are worth showing as the live
     * book. This is the bridge from the sandbox to the real slate: same selection rule,
     * real quotes.
     */
    fun livePicks(props: List<SimProp>, params: StrategyParams, limit: Int = 8): List<Pair<SimProp, Decision>> =
        props.mapNotNull { prop -> SandboxEngine.decide(prop, params)?.let { prop to it } }
            .sortedByDescending { it.second.edgePts }
            .take(limit)

    private fun pct(value: Double): String = "%.2f%%".format(value * 100)
}
