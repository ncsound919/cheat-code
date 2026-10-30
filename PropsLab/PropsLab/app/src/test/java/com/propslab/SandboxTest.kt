package com.propslab

import com.propslab.domain.Odds
import com.propslab.domain.Outcome
import com.propslab.domain.Side
import com.propslab.sim.Autopilot
import com.propslab.sim.PromotionGate
import com.propslab.sim.SandboxEngine
import com.propslab.sim.SandboxTruth
import com.propslab.sim.Season
import com.propslab.sim.SideFilter
import com.propslab.sim.SimProp
import com.propslab.sim.StrategyParams
import com.propslab.sim.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The sandbox exists to answer one question honestly: has a formula shown an edge, or
 * has it just been lucky? These tests pin the world down, then pin down what the gate
 * does to formulas that are guessing.
 */
class SandboxTest {

    private val season by lazy { Season.generate(seed = Season.BASE_SEED) }

    @Test
    fun seasonIsReproducibleAndSeeded() {
        val again = Season.generate(seed = Season.BASE_SEED)
        assertEquals(season.size, again.size)
        assertEquals(season.take(200).map { it.id to it.actual }, again.take(200).map { it.id to it.actual })

        val other = Season.generate(seed = Season.BASE_SEED + 1)
        assertNotEquals(season.first().id, other.first().id)
        assertNotEquals(season.take(50).map { it.actual }, other.take(50).map { it.actual })
    }

    @Test
    fun everySeasonPropIsAWellFormedDistribution() {
        assertEquals(Season.DEFAULT_DAYS * Season.DEFAULT_PROPS_PER_DAY, season.size)
        for (prop in season.take(500)) {
            val probs = prop.probabilities
            assertNotNull("${prop.id} must be a distribution", probs)
            assertEquals(1.0, probs!!.over + probs.push + probs.under, 1e-9)
            assertTrue(prop.overOdds >= 100 || prop.overOdds <= -100)
            assertTrue(prop.underOdds >= 100 || prop.underOdds <= -100)
            val actual = prop.actual
            assertNotNull(actual, )
            val delta = actual!! - prop.line
            assertTrue("${prop.id} settled off the grid: $delta", abs(delta) <= 1.0)
        }
    }

    @Test
    fun theTruthIsOnlyBeatableWhereTheModelDisagrees() {
        // Half-point lines can never push, so the comparison is clean.
        val halfPoint = season.filter { it.line % 1.0 != 0.0 }
        val wideModelEdges = halfPoint.filter { (SandboxTruth.trueEdgePts(it, Side.OVER)) > 3.0 }
        assertTrue(wideModelEdges.isNotEmpty())

        var agree = 0
        for (prop in wideModelEdges.take(400)) {
            val modelEdge = com.propslab.domain.Pricing.edgePoints(
                prop.probabilities!!.overGivenNoPush,
                prop.marketOver!!,
            )
            if (modelEdge > 0) agree++
            // Truth leans toward the model, so a positive model edge must mean a positive true edge.
            assertTrue("${prop.id}: model $modelEdge", SandboxTruth.trueEdgePts(prop, Side.OVER) > 0)
        }
        assertEquals("the model and the truth must agree on direction", wideModelEdges.take(400).size, agree)
    }

    /**
     * A season where the model simply repeats the price. There is no disagreement to find,
     * so nothing here can be promoted, however long it runs.
     */
    private fun noSignalSeason(count: Int = 8_000): List<SimProp> {
        val pairs = listOf(-110 to -110, -115 to -105, -120 to 100, -105 to -115, 100 to -120)
        return (0 until count).map { i ->
            val pair = pairs[i % pairs.size]
            val market = Odds.noVigOver(pair.first, pair.second)
            val push = if (i % 9 == 0) 0.03 else 0.0
            val line = 20.0 + (i % 20) + if (i % 9 == 0) 0.0 else 0.5
            val over = market * (1.0 - push)
            val prop = SimProp(
                id = "flat-$i",
                playerName = "Flat $i",
                line = line,
                overOdds = pair.first,
                underOdds = pair.second,
                pOver = over,
                pPush = push,
                pUnder = 1.0 - over - push,
                startUtc = i.toLong(),
                actual = null,
                synthetic = true,
            )
            prop.copy(actual = SandboxTruth.actualFor(prop))
        }
    }

    @Test
    fun aSeasonWithoutSignalPromotesNothing() {
        val plan = Autopilot.plan(noSignalSeason())
        assertEquals(null, plan.promoted)
        assertTrue(plan.candidates.isNotEmpty())
        assertTrue(
            "every candidate must stay in the sandbox: ${plan.candidates.map { it.id + "=" + "%.3f".format(it.stats.roi) }}",
            plan.candidates.none { it.gate.allPassed },
        )
        // Repeating the no-vig price is worth exactly zero, and the gate must see that.
        val leader = plan.leader!!
        assertTrue("roi on a flat world: ${leader.stats.roi}", abs(leader.stats.roi) < 0.01)
        assertTrue("t on a flat world: ${leader.stats.tStat}", abs(leader.stats.tStat) < 2.0)
    }

    @Test
    fun engineIsReproducibleToTheCent() {
        val params = StrategyParams(minEdgePts = 4.0)
        val a = SandboxEngine.run(season.take(3_000), params)
        val b = SandboxEngine.run(season.take(3_000), params)
        assertEquals(a.stats.bets, b.stats.bets)
        assertEquals(a.stats.profit, b.stats.profit, 1e-12)
        assertEquals(a.stats.tStat, b.stats.tStat, 1e-12)
        assertEquals(a.trades.size, b.trades.size)
    }

    @Test
    fun aWideEdgeFormulaCanActuallyPassTheGate() {
        val plan = Autopilot.plan(season)
        val promoted = plan.promoted
        assertNotNull("some candidate in the grid must demonstrate an edge", promoted)

        val stats = promoted!!.stats
        assertTrue("bets: ${stats.bets}", stats.bets >= 400)
        assertTrue("roi: ${stats.roi}", stats.roi > 0.015)
        assertTrue("t: ${stats.tStat}", stats.tStat > 2.0)
        assertTrue("drawdown: ${stats.maxDrawdown}", stats.maxDrawdown <= 0.45)
        assertEquals(Verdict.PROMOTE, promoted.gate.verdict)
        assertTrue(promoted.gate.failed.isEmpty())
    }

    @Test
    fun thePromotedFormulaAsksForARealDisagreement() {
        val plan = Autopilot.plan(season)
        val promoted = plan.promoted!!
        assertTrue(
            "the winner must demand more than a rounding-error edge, not ${promoted.params.minEdgePts}",
            promoted.params.minEdgePts >= 2.0,
        )
        assertEquals(SideFilter.BOTH, promoted.params.sideFilter)
        // The ladder has to be monotone where it matters: demanding more must not pay worse.
        val byEdge = plan.candidates
            .filter { it.params.sideFilter == SideFilter.BOTH }
            .sortedBy { it.params.minEdgePts }
        assertTrue(byEdge.size >= 4)
        val loose = byEdge.first()
        val tight = byEdge.first { it.params.minEdgePts >= 4.0 }
        assertTrue(
            "a wider disagreement should pay a better return (${loose.stats.roi} vs ${tight.stats.roi})",
            tight.stats.roi > loose.stats.roi,
        )
    }

    @Test
    fun theGateReportsEveryReasonACandidateIsStillInTheSandbox() {
        val weak = SandboxEngine.run(season.take(40), StrategyParams(minEdgePts = 5.0))
        val gate = PromotionGate.evaluate(weak.stats)
        assertEquals(Verdict.HOLD, gate.verdict)
        assertFalse(gate.failed.isEmpty())
        assertTrue(gate.failed.any { it.contains("bets") })
        assertTrue(gate.summary.isNotBlank())
    }

    @Test
    fun anIncumbentThatStillPassesKeepsTheLiveBook() {
        val first = Autopilot.plan(season)
        val held = Autopilot.plan(season, incumbent = first.promoted!!.id)
        assertTrue(held.incumbentHeld)
        assertEquals(first.promoted.id, held.promoted!!.id)

        // A retired formula is not an incumbent; the best passing candidate takes over.
        val replaced = Autopilot.plan(season, incumbent = "e0.0-both")
        assertFalse(replaced.incumbentHeld)
        assertNotEquals("e0.0-both", replaced.promoted!!.id)
    }

    @Test
    fun aLossInformedChallengerCanTakeTheBookFromAPassingIncumbent() {
        val plan = Autopilot.plan(season)
        val ready = plan.ready
        assertTrue("the season must have more than one passing formula", ready.size >= 2)
        val pair = ready.flatMap { incumbent ->
            ready.mapNotNull { challenger ->
                if (incumbent.id == challenger.id) {
                    null
                } else if (
                    challenger.stats.roi >= incumbent.stats.roi + 0.01 &&
                    challenger.stats.profit > incumbent.stats.profit
                ) {
                    incumbent to challenger
                } else {
                    null
                }
            }
        }.maxByOrNull { (incumbent, challenger) -> challenger.stats.profit - incumbent.stats.profit }
        assertNotNull("a challenger that beats a passing incumbent must exist", pair)
        val (incumbent, challenger) = pair!!

        val held = Autopilot.plan(
            season,
            incumbent = incumbent.id,
            challengers = setOf(challenger.id),
        )
        assertFalse("a loss-informed challenger should take the book when it clears the gate", held.incumbentHeld)
        assertEquals(challenger.id, held.promoted!!.id)
        assertTrue(held.note.contains("learned from"))
    }

    @Test
    fun anIncumbentThatStopsPassingLosesTheBook() {
        val plan = Autopilot.plan(noSignalSeason(), incumbent = "e4.0-both")
        assertFalse(plan.incumbentHeld)
        assertEquals(null, plan.promoted)
        assertTrue(plan.note.contains("No formula cleared the gate"))
    }

    @Test
    fun stakesAreFlatSoFormulasAreComparable() {
        val result = SandboxEngine.run(season.take(2_000), StrategyParams(minEdgePts = 3.0))
        assertTrue(result.stats.bets > 0)
        assertEquals(result.stats.bets.toDouble(), result.stats.staked, 1e-9)
        assertTrue(result.trades.all { it.stake == SandboxEngine.UNIT_STAKE })
    }

    @Test
    fun livePicksUseRealQuotesAndRespectTheSideFilter() {
        val board = season.filter { it.startUtc > 0L }.take(50)
        val both = Autopilot.livePicks(board, StrategyParams(minEdgePts = 1.0, sideFilter = SideFilter.BOTH), limit = 5)
        assertTrue(both.size <= 5)
        assertTrue(both.all { (prop, decision) -> decision.odds == prop.oddsFor(decision.side) })

        val overs = Autopilot.livePicks(board, StrategyParams(minEdgePts = 1.0, sideFilter = SideFilter.OVER_ONLY), 50)
        assertTrue(overs.all { it.second.side == Side.OVER })

        val unders = Autopilot.livePicks(board, StrategyParams(minEdgePts = 1.0, sideFilter = SideFilter.UNDER_ONLY), 50)
        assertTrue(unders.all { it.second.side == Side.UNDER })
    }

    @Test
    fun aPropWithNoUsablePriceIsNeverBet() {
        val prop = SimProp(
            id = "broken",
            playerName = "Nobody",
            line = 20.0,
            overOdds = -50,
            underOdds = -50,
            pOver = 0.55,
            pPush = 0.0,
            pUnder = 0.45,
            startUtc = 0L,
            actual = 21.0,
            synthetic = true,
        )
        val result = SandboxEngine.run(listOf(prop), StrategyParams(minEdgePts = 0.0))
        assertEquals(0, result.stats.bets)
        assertEquals(0.0, result.stats.profit, 0.0)
    }

    @Test
    fun aOneSidedFormulaFindsTheSameEdgeOnHalfTheVolume() {
        val both = SandboxEngine.run(season, StrategyParams(minEdgePts = 3.0, sideFilter = SideFilter.BOTH))
        val over = SandboxEngine.run(season, StrategyParams(minEdgePts = 3.0, sideFilter = SideFilter.OVER_ONLY))
        val under = SandboxEngine.run(season, StrategyParams(minEdgePts = 3.0, sideFilter = SideFilter.UNDER_ONLY))

        assertTrue("over: ${over.stats.bets} of ${both.stats.bets}", over.stats.bets in 1 until both.stats.bets)
        assertTrue("under: ${under.stats.bets} of ${both.stats.bets}", under.stats.bets in 1 until both.stats.bets)
        assertTrue(
            "one side should be roughly half the board (${over.stats.bets} + ${under.stats.bets} vs ${both.stats.bets})",
            abs((over.stats.bets + under.stats.bets) - both.stats.bets) < both.stats.bets * 0.05,
        )
        assertTrue(
            "both sides should find the same edge per bet (${both.stats.roi} vs ${over.stats.roi})",
            abs(both.stats.roi - over.stats.roi) < 0.05,
        )
    }

    @Test
    fun outcomeAccountingMatchesSettlement() {
        val result = SandboxEngine.run(season.take(1_000), StrategyParams(minEdgePts = 3.0))
        val wins = result.trades.count { it.outcome == Outcome.WIN }
        val losses = result.trades.count { it.outcome == Outcome.LOSS }
        val pushes = result.trades.count { it.outcome == Outcome.PUSH }
        assertEquals(wins, result.stats.wins)
        assertEquals(losses, result.stats.losses)
        assertEquals(pushes, result.stats.pushes)
        assertEquals(result.trades.size, wins + losses + pushes)
        for (trade in result.trades) {
            val expected = when (trade.outcome) {
                Outcome.WIN -> trade.stake * (Odds.americanToDecimal(trade.odds) - 1.0)
                Outcome.LOSS -> -trade.stake
                else -> 0.0
            }
            assertEquals(expected, trade.profit, 1e-12)
        }
    }
}
