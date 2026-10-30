package com.propslab.sim

import com.propslab.domain.Odds
import com.propslab.domain.Outcome
import com.propslab.domain.Pricing
import com.propslab.domain.Settlement
import com.propslab.domain.Side
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The formula sandbox.
 *
 * A formula is run forward over a season in time order. Every accepted prop costs one
 * flat unit of a fake bankroll, and the run records the bankroll path so the drawdown is
 * measured the way an account would feel it, not just as a final profit number.
 *
 * Determinism: given the same props and the same params, [run] returns the same result,
 * down to the cent. Nothing here reads a clock or a random source.
 */
object SandboxEngine {

    /** The fake bankroll every formula starts with, in units. */
    const val STARTING_BANKROLL = 250.0

    /** A run stops here rather than continuing to bet a bankroll that is effectively gone. */
    const val RUIN_FLOOR = 25.0

    const val UNIT_STAKE = 1.0

    /** Which side this formula would take, and at what price. Null means it passes. */
    fun decide(prop: SimProp, params: StrategyParams): Decision? {
        val probs = prop.probabilities ?: return null
        val market = prop.marketOver ?: return null
        // A lens that names a market is not allowed to bet a different one. The simulated
        // season carries no market, so this restricts real rows and leaves the bench alone.
        if (params.markets.isNotEmpty() && prop.market.isNotEmpty() && prop.market !in params.markets) return null
        // A lesson can remove a market outright. Exclusion runs after inclusion, so a row
        // that somehow matched both is still refused rather than silently bet.
        if (prop.market.isNotEmpty() && prop.market in params.excludeMarkets) return null

        // The tilt is applied to the no-push over probability, which is the quantity that is
        // compared to the de-vigged price. Push mass is untouched: it describes the line, not
        // the opinion, and shifting it would silently change the break-even arithmetic.
        val overNoPush = (probs.overGivenNoPush + params.tiltPts / 100.0).coerceIn(0.005, 0.995)
        val edge = runCatching { Pricing.edgePoints(overNoPush, market) }.getOrNull() ?: return null

        val side = when {
            edge >= params.minEdgePts && params.sideFilter.allowsOver -> Side.OVER
            -edge >= params.minEdgePts && params.sideFilter.allowsUnder -> Side.UNDER
            else -> return null
        }
        val price = prop.oddsFor(side)
        val decimal = runCatching { Odds.americanToDecimal(price) }.getOrNull() ?: return null
        if (decimal < params.minPrice || decimal > params.maxPrice) return null

        return Decision(side = side, odds = price, edgePts = abs(edge), implied = 1.0 / decimal)
    }

    fun run(
        props: List<SimProp>,
        params: StrategyParams,
        startingBankroll: Double = STARTING_BANKROLL,
    ): SimResult {
        var bankroll = startingBankroll
        var peak = startingBankroll
        var lowest = startingBankroll
        var maxDrawdown = 0.0
        var bets = 0
        var wins = 0
        var losses = 0
        var pushes = 0
        var staked = 0.0
        var profit = 0.0
        var impliedSum = 0.0
        var ruined = false
        val perBet = ArrayList<Double>(props.size)
        val trades = ArrayList<SimTrade>()

        for (prop in props) {
            if (bankroll <= RUIN_FLOOR) {
                ruined = true
                break
            }
            val decision = decide(prop, params) ?: continue
            val actual = prop.actual ?: continue
            val stake = min(UNIT_STAKE, bankroll)
            if (stake <= 0.0) break

            val outcome = Settlement.outcome(decision.side, prop.line, actual)
            val pnl = Settlement.profit(outcome, stake, decision.odds)
            bankroll += pnl
            staked += stake
            profit += pnl
            impliedSum += decision.implied
            bets++
            when (outcome) {
                Outcome.WIN -> wins++
                Outcome.LOSS -> losses++
                Outcome.PUSH -> pushes++
                Outcome.VOID -> Unit
            }
            perBet += pnl / stake
            peak = max(peak, bankroll)
            lowest = min(lowest, bankroll)
            if (peak > 0.0) maxDrawdown = max(maxDrawdown, (peak - bankroll) / peak)
            trades += SimTrade(
                formulaId = params.id,
                propId = prop.id,
                side = decision.side,
                line = prop.line,
                odds = decision.odds,
                stake = stake,
                outcome = outcome,
                profit = pnl,
                startUtc = prop.startUtc,
                bankrollAfter = bankroll,
            )
        }

        val (mean, sd) = unitStats(perBet)
        val tStat = if (perBet.size >= 2 && sd > 0.0) mean / (sd / sqrt(perBet.size.toDouble())) else 0.0

        val stats = SimStats(
            bets = bets,
            wins = wins,
            losses = losses,
            pushes = pushes,
            staked = staked,
            profit = profit,
            tStat = tStat,
            maxDrawdown = maxDrawdown,
            finalBankroll = bankroll,
            peak = peak,
            lowest = lowest,
            avgImplied = if (bets > 0) impliedSum / bets else 0.0,
            ruined = ruined,
        )
        return SimResult(params = params, stats = stats, trades = trades)
    }

    /** Run several formulas over the same props, each starting from a full bankroll. */
    fun runAll(props: List<SimProp>, grid: List<StrategyParams>): List<SimResult> =
        grid.map { run(props, it) }
}
