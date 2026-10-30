package com.propslab.data

import com.propslab.domain.Outcome
import com.propslab.sim.SandboxEngine

/**
 * The arithmetic behind the user's own practice account.
 *
 * The account is deliberately separate from the sandbox's fake bankroll. The sandbox is a
 * formula competition with its own 250 units; this account is the one the user stakes from.
 * Both start at the same figure so the two screens speak the same language, but a manual
 * bet can never spend the autopilot's money or the other way around.
 *
 * A stake is reserved while the bet is open. The available balance is therefore the
 * starting bankroll plus everything already won or lost, minus the stakes still at risk.
 * There is no separate balance row to drift out of sync: the ledger is the balance.
 */
object PracticeLedger {

    const val MANUAL_FORMULA_ID = "manual"
    const val MANUAL_LABEL = "My pick"

    /** Same starting point as [SandboxEngine], so the two practice surfaces agree. */
    const val STARTING_BANKROLL = SandboxEngine.STARTING_BANKROLL

    /** Below this many decisions the app refuses to call a trend a finding. */
    const val MIN_ADVICE_DECISIONS = 8

    /** A formula needs this many settled bets before its record can drive advice. */
    const val MIN_FORMULA_DECISIONS = 4

    /** 250 plus every settled profit. Open stakes are not deducted here. */
    fun balance(settledProfit: Double): Double =
        (STARTING_BANKROLL + settledProfit.coerceFinite()).coerceAtLeast(0.0)

    /** What can still be staked after reserving every open bet. */
    fun available(settledProfit: Double, openStakes: Double): Double =
        (balance(settledProfit) - openStakes.coerceFinite()).coerceAtLeast(0.0)

    /**
     * All practice bets, grouped by their formula tag.
     *
     * The two sources are the user's paper ledger and the formula live book. They are
     * different accounts but the same question — "is this tag making money?" — so the chart
     * reads them together and labels the total as practice results.
     */
    fun records(
        bets: List<BetUi>,
        trades: List<SandboxTradeUi>,
        formulas: List<FormulaUi>,
    ): List<FormulaRecord> {
        val acc = LinkedHashMap<String, Accumulator>()
        bets.forEach { bet ->
            val row = acc.getOrPut(bet.formulaKey) { Accumulator(bet.formulaName) }
            row.add(bet.stake, bet.outcome, bet.profit, open = bet.isOpen)
        }
        val labels = formulas.associate { it.id to it.label }
        val live = formulas.filter { it.isLive }.map { it.id }.toSet()
        trades.forEach { trade ->
            val id = trade.formulaId.ifBlank { MANUAL_FORMULA_ID }
            val row = acc.getOrPut(id) { Accumulator(labels[id] ?: if (id == MANUAL_FORMULA_ID) MANUAL_LABEL else id) }
            row.add(trade.stake, trade.outcome, trade.profit, open = trade.isOpen)
            if (id in live) row.live = true
        }
        return acc.map { (id, row) ->
            FormulaRecord(
                id = id,
                label = row.label.ifBlank { if (id == MANUAL_FORMULA_ID) MANUAL_LABEL else id },
                bets = row.bets,
                open = row.open,
                wins = row.wins,
                losses = row.losses,
                pushes = row.pushes,
                staked = row.staked,
                profit = row.profit,
                live = row.live,
            )
        }.sortedWith(compareByDescending<FormulaRecord> { it.settled }.thenByDescending { it.profit })
    }

    /**
     * One short paragraph for the card under the chart.
     *
     * It only says things the stored record supports: sample size, the weakest tagged
     * formula with enough decisions, the fitted model's direction, and whether a formula
     * currently holds the live book. No result here changes a formula automatically.
     */
    fun advice(
        bets: List<BetUi>,
        records: List<FormulaRecord>,
        calibration: List<CalibrationUi>,
        promotedLabel: String?,
    ): String {
        val settled = bets.filter { !it.isOpen }
        val decisions = settled.count { it.outcome == Outcome.WIN || it.outcome == Outcome.LOSS }
        val open = bets.count { it.isOpen }
        val formulaSettled = records.sumOf { it.settled }
        if (settled.isEmpty() && formulaSettled == 0) {
            return if (open == 0) {
                "No results yet. Place a few paper bets and record each result; this paragraph only " +
                    "starts giving advice after ${MIN_ADVICE_DECISIONS} decided bets."
            } else {
                "You have $open paper bet${if (open == 1) "" else "s"} at risk and nothing settled yet. " +
                    "Record the results before changing a formula — the app will not treat a sample of zero as evidence."
            }
        }

        val profit = settled.sumOf { it.profit ?: 0.0 }
        val staked = settled.sumOf { it.stake }
        val roi = if (staked > 0.0) profit / staked else 0.0
        val prop = calibration.firstOrNull { it.track == LearningTrack.PROP && it.fitted }
        val worst = records
            .filter { it.settled >= MIN_FORMULA_DECISIONS && it.roi != null }
            .minByOrNull { it.roi ?: 0.0 }
        val worstRoi = worst?.roi

        // A fitted model or a live formula can have enough evidence even when the user's own
        // account is still tiny, so those findings come before the small-sample gate.
        if (prop != null && prop.slope < 0.85) {
            return "The prop model is overconfident: its fitted slope is ${"%.2f".format(prop.slope)} on ${prop.sample} " +
                "settled decisions. The live correction is already shrinking those probabilities, so raise the " +
                "minimum edge or narrow the side filter instead of adding more stake."
        }
        if (prop != null && prop.slope > 1.15) {
            return "The prop model is underconfident: its fitted slope is ${"%.2f".format(prop.slope)} on ${prop.sample} " +
                "settled decisions. The correction is expanding its probabilities; keep the current filters and let " +
                "the record grow rather than loosening the edge threshold."
        }
        if (worst != null && worstRoi != null && worstRoi < -0.02) {
            return "${worst.label} is the drag: ${"%.1f".format(worstRoi * 100)}% on ${worst.settled} settled " +
                "practice bet${if (worst.settled == 1) "" else "s"}. Raise its minimum edge or stop tagging bets to it; " +
                "keep the stake flat until the remaining record turns."
        }
        if (decisions < MIN_ADVICE_DECISIONS) {
            return "Only $decisions of your own decided bet${if (decisions == 1) "" else "s"} so far. Keep the " +
                "stakes flat and record ${MIN_ADVICE_DECISIONS - decisions} more before changing a formula; a handful " +
                "of results cannot separate a real edge from a good week."
        }
        if (profit < 0.0) {
            return "The practice account is ${"%.2f".format(profit)} units down (${"%.1f".format(roi * 100)}% on " +
                "$staked staked) across $decisions decided bets. Keep stakes flat and record a larger sample before " +
                "changing the model; if one market or side keeps losing, remove that segment first."
        }
        if (promotedLabel == null) {
            return "The practice account is ${"%.2f".format(profit)} units up, but no formula holds the live book yet. " +
                "Keep taking manual paper bets and let the sandbox find a formula that clears the gate before " +
                "raising stakes."
        }
        return "The practice account is ${"%.2f".format(profit)} units up (${"%.1f".format(roi * 100)}% on $staked " +
            "staked) across $decisions decided bets. Keep the stake flat and let the sample grow; one profitable " +
            "stretch is not enough evidence to increase it."
    }

    private fun Double.coerceFinite(): Double = if (isFinite()) this else 0.0

    private class Accumulator(var label: String) {
        var bets = 0
        var open = 0
        var wins = 0
        var losses = 0
        var pushes = 0
        var staked = 0.0
        var profit = 0.0
        var live = false

        fun add(stake: Double, outcome: Outcome?, pnl: Double?, open: Boolean) {
            bets += 1
            val safeStake = if (stake.isFinite() && stake > 0.0) stake else 0.0
            val safePnl = if (pnl?.isFinite() == true) pnl else 0.0
            if (open || outcome == null) {
                this.open += 1
                return
            }
            staked += safeStake
            profit += safePnl
            when (outcome) {
                Outcome.WIN -> wins += 1
                Outcome.LOSS -> losses += 1
                Outcome.PUSH -> pushes += 1
                Outcome.VOID -> Unit
            }
        }
    }
}

/** One formula tag's practice record, or the user's untagged picks under [MANUAL_FORMULA_ID]. */
data class FormulaRecord(
    val id: String,
    val label: String,
    val bets: Int,
    val open: Int,
    val wins: Int,
    val losses: Int,
    val pushes: Int,
    val staked: Double,
    val profit: Double,
    val live: Boolean,
) {
    val settled: Int get() = wins + losses + pushes
    val decisions: Int get() = wins + losses
    val roi: Double? get() = if (staked > 0.0) profit / staked else null
    val winRate: Double? get() = if (decisions > 0) wins.toDouble() / decisions else null
}
