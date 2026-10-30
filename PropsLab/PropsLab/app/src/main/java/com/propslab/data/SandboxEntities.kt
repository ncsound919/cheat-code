package com.propslab.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where a formula sits in the ladder. Only a formula on the live book trades today's board. */
object Tier {
    const val SANDBOX = "SANDBOX"
    const val LIVE = "LIVE"
    const val RETIRED = "RETIRED"
}

/**
 * One row per formula in the search grid, holding the last measured result of running it
 * over a simulated season. The season itself is regenerated from its seed and never stored.
 */
@Entity(tableName = "formula_runs")
data class FormulaRunEntity(
    @PrimaryKey val id: String,
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
    /** PASS or FAIL, straight from the promotion gate. */
    val verdict: String,
    /** Semicolon-separated failed checks; empty when the formula passed. */
    val reasons: String,
    val round: Int,
    val updatedUtc: Long,
    /** NOT RUN, PASS or FAIL over the imported historical dataset. */
    val historyVerdict: String = "NOT RUN",
    val historyBets: Int = 0,
    val historyRoi: Double = 0.0,
    val historyNote: String = "",
)

/**
 * A bet the live book took on the real board. `synthetic` is true when the result comes
 * from the sandbox oracle rather than a recorded game.
 */
@Entity(tableName = "sandbox_trades", indices = [Index("formulaId"), Index("predictionId")])
data class SandboxTradeEntity(
    @PrimaryKey val id: String,
    val formulaId: String,
    val predictionId: String,
    val playerName: String,
    val side: String,
    val line: Double,
    val odds: Int,
    val stake: Double,
    val outcome: String?,
    val profit: Double?,
    val startUtc: Long,
    val placedUtc: Long,
    val settledUtc: Long?,
    val synthetic: Boolean,
)

/** Single row, id = "autopilot". Everything the loop needs to continue where it stopped. */
@Entity(tableName = "sandbox_state")
data class SandboxStateEntity(
    @PrimaryKey val id: String = AUTOPILOT_ID,
    val round: Int = 0,
    val seed: Long = 0L,
    val lastRunUtc: Long = 0L,
    val promotedId: String? = null,
    val promotedLabel: String? = null,
    val note: String = "The loop has not run yet.",
    val bankroll: Double = 0.0,
    val peak: Double = 0.0,
    val bets: Int = 0,
    val profit: Double = 0.0,
    val autoRun: Boolean = true,
) {
    companion object {
        const val AUTOPILOT_ID = "autopilot"
    }
}
