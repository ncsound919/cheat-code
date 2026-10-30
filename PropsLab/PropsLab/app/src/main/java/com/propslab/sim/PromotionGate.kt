package com.propslab.sim

/**
 * The line between a formula that has demonstrated an edge and one that has merely been
 * lucky. Every rule is a claim that can fail, and each failure is reported with its own
 * sentence so the UI never has to guess why a candidate is still in the sandbox.
 *
 * The thresholds are picked to be reachable only with a real edge: at ~1 unit of
 * variance per bet, clearing t = 2.0 on a season means the profit is unlikely to be
 * noise, and 400 bets is the smallest sample where that test has any power at all.
 */
data class GateThresholds(
    val minBets: Int = 400,
    val minRoi: Double = 0.015,
    val minTStat: Double = 2.0,
    val maxDrawdown: Double = 0.45,
    /** Required hit rate above the break-even implied by the prices actually taken. */
    val minWinRateMargin: Double = 0.0,
) {
    companion object {
        val STANDARD = GateThresholds()
    }
}

enum class Verdict {
    PROMOTE,
    HOLD,
    RETIRE;

    val label: String
        get() = when (this) {
            PROMOTE -> "Ready"
            HOLD -> "Sandbox"
            RETIRE -> "Retired"
        }
}

data class GateResult(
    val verdict: Verdict,
    val passed: List<String>,
    val failed: List<String>,
) {
    val allPassed: Boolean get() = failed.isEmpty()
    val summary: String get() = if (allPassed) "clears every check" else failed.first()
}

object PromotionGate {

    fun evaluate(stats: SimStats, thresholds: GateThresholds = GateThresholds.STANDARD): GateResult {
        val passed = mutableListOf<String>()
        val failed = mutableListOf<String>()

        fun check(ok: Boolean, pass: String, fail: String) {
            if (ok) passed += pass else failed += fail
        }

        check(
            !stats.ruined,
            "the fake bankroll survives the season",
            "the fake bankroll was wiped out part-way through the season",
        )
        check(
            stats.bets >= thresholds.minBets,
            "${stats.bets} bets is enough to judge",
            "only ${stats.bets} bets, and ${thresholds.minBets} are needed before a result means anything",
        )
        check(
            stats.roi >= thresholds.minRoi,
            "return on stake ${pct(stats.roi)} clears ${pct(thresholds.minRoi)}",
            "return on stake ${pct(stats.roi)} is below the ${pct(thresholds.minRoi)} floor",
        )
        check(
            stats.tStat >= thresholds.minTStat,
            "t-statistic ${two(stats.tStat)} is above ${two(thresholds.minTStat)}",
            "t-statistic ${two(stats.tStat)} is below ${two(thresholds.minTStat)}, so the profit is not distinguishable from luck",
        )
        check(
            stats.maxDrawdown <= thresholds.maxDrawdown,
            "worst drawdown ${pct(stats.maxDrawdown)} stays under the limit",
            "worst drawdown ${pct(stats.maxDrawdown)} exceeds ${pct(thresholds.maxDrawdown)}",
        )
        val margin = stats.winRateOverBreakeven
        check(
            margin != null && margin >= thresholds.minWinRateMargin,
            "hit rate beats break-even by ${pct(margin ?: 0.0)}",
            "hit rate does not beat the ${pct(stats.breakeven)} break-even the prices demanded",
        )

        return GateResult(Verdict.PROMOTE.takeIf { failed.isEmpty() } ?: Verdict.HOLD, passed, failed)
    }

    /**
     * A formula already on the live book is re-checked on the same rules. One bad season
     * does not retire it — the live book is scored on a much smaller sample — but a
     * formula that is losing after a real sample goes back to the sandbox.
     */
    fun review(stats: SimStats, thresholds: GateThresholds = GateThresholds.STANDARD): GateResult {
        val live = thresholds.copy(
            minBets = (thresholds.minBets / 4).coerceAtLeast(150),
            minTStat = -1.5,
            minRoi = -0.02,
        )
        val base = evaluate(stats, live)
        return if (base.verdict == Verdict.PROMOTE) base else base.copy(verdict = Verdict.RETIRE)
    }

    private fun pct(v: Double): String = "%.2f%%".format(v * 100)
    private fun two(v: Double): String = "%.2f".format(v)
}
