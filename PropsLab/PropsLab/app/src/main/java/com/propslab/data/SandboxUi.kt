package com.propslab.data

import com.propslab.domain.Outcome
import com.propslab.domain.Side

data class FormulaUi(
    val id: String,
    val label: String,
    val params: String,
    val tier: String,
    val bets: Int,
    val wins: Int,
    val losses: Int,
    val pushes: Int,
    val staked: Double,
    val profit: Double,
    val roi: Double,
    val tStat: Double,
    val maxDrawdown: Double,
    val finalBankroll: Double,
    val passed: Boolean,
    val reasons: List<String>,
    val round: Int,
    val historyVerdict: String = "NOT RUN",
    val historyBets: Int = 0,
    val historyRoi: Double = 0.0,
    val historyNote: String = "",
) {
    val decisions: Int get() = wins + losses
    val winRate: Double? get() = if (decisions > 0) wins.toDouble() / decisions else null
    val isLive: Boolean get() = tier == Tier.LIVE
    val isRetired: Boolean get() = tier == Tier.RETIRED
    val historyPassed: Boolean? get() = when (historyVerdict) {
        "PASS" -> true
        "FAIL" -> false
        else -> null
    }
    val edgeTenths: Int get() = id.removePrefix("e").substringBefore('-').replace(".", "").toIntOrNull() ?: 0
}

data class SandboxTradeUi(
    val id: String,
    val formulaId: String,
    val playerName: String,
    val side: Side,
    val line: Double,
    val odds: Int,
    val stake: Double,
    val outcome: Outcome?,
    val profit: Double?,
    val startUtc: Long,
    val settledUtc: Long?,
    val synthetic: Boolean,
) {
    val isOpen: Boolean get() = outcome == null
}

data class SandboxUi(
    val round: Int = 0,
    val promotedId: String? = null,
    val promotedLabel: String? = null,
    val note: String = "The loop has not run yet.",
    val lastRunUtc: Long = 0L,
    val autoRun: Boolean = true,
    val bankroll: Double = 0.0,
    val peak: Double = 0.0,
    val formulas: List<FormulaUi> = emptyList(),
    val trades: List<SandboxTradeUi> = emptyList(),
) {
    val live: FormulaUi? get() = formulas.firstOrNull { it.isLive }
    val ready: List<FormulaUi> get() = formulas.filter { it.passed }.sortedByDescending { it.profit }
    val ranked: List<FormulaUi> get() = formulas.sortedByDescending { it.profit }
    val leader: FormulaUi? get() = live ?: ready.firstOrNull() ?: formulas.maxByOrNull { it.profit }
    val considered: Int get() = formulas.size
    val openTrades: List<SandboxTradeUi> get() = trades.filter { it.isOpen }
    val settledTrades: List<SandboxTradeUi> get() = trades.filter { !it.isOpen }
    val liveProfit: Double get() = trades.sumOf { it.profit ?: 0.0 }
    val liveBets: Int get() = trades.size
    val liveStaked: Double get() = trades.sumOf { it.stake }
    val liveRoi: Double? get() = if (liveStaked > 0.0) liveProfit / liveStaked else null
    val liveWins: Int get() = trades.count { it.outcome == Outcome.WIN }
    val liveLosses: Int get() = trades.count { it.outcome == Outcome.LOSS }

    /** Drawdown of the live book from its peak, as a fraction of the starting bankroll. */
    val liveDrawdown: Double
        get() {
            if (trades.isEmpty()) return 0.0
            val ordered = trades.sortedBy { it.settledUtc ?: it.startUtc }
            var running = 0.0
            var peak2 = 0.0
            var worst = 0.0
            for (t in ordered) {
                running += t.profit ?: 0.0
                peak2 = maxOf(peak2, running)
                worst = maxOf(worst, (peak2 - running) / 250.0)
            }
            return worst
        }
}

/** One row on the Today's best bets screen. */
data class BestPickUi(
    val prop: PropUi,
    val side: Side,
    val odds: Int,
    val edgePts: Double,
    val modelProb: Double,
    val marketProb: Double,
    val ev: Double,
    val breakeven: Double,
    val kelly: Double,
    val live: Boolean,
    /** True when the pick came from a formula that is still in the sandbox. */
    val shadow: Boolean,
    /** The prose shown under the pick: what was taken, why, and what would make it wrong. */
    val explanation: String = "",
)

data class BestBetsUi(
    val picks: List<BestPickUi> = emptyList(),
    /** Scored moneyline, spread and total picks. They compete in the same parlay. */
    val gamePicks: List<GamePickUi> = emptyList(),
    val strategyLabel: String? = null,
    val strategyId: String? = null,
    val live: Boolean = false,
    val round: Int = 0,
    val note: String = "",
    val ready: Int = 0,
    val considered: Int = 0,
) {
    val hasFormula: Boolean get() = strategyId != null

    /** True when the board still carries invented rows, which changes what the footer may claim. */
    val anyFixture: Boolean get() = picks.any { it.prop.isFixture }

    /** Open picks at a flat unit, for the card's summary line. */
    val flatUnits: Double get() = (picks.size + gamePicks.size).toDouble()

    /**
     * Picks whose estimate rests on fewer than eight games. Early in a season that is all of
     * them, and the screen has to say so rather than presenting a short sample as a long one.
     */
    val thinPicks: Int get() = picks.count { pick ->
        (pick.prop.drivers.firstOrNull { it.label == "games" }?.value ?: 99.0) < 8.0
    }
}
