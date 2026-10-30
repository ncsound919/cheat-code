package com.propslab

import com.propslab.domain.Markets
import com.propslab.sim.Autopilot
import com.propslab.sim.Season
import com.propslab.sim.SideFilter
import com.propslab.sim.StrategyParams
import com.propslab.theory.HistoryCsv
import com.propslab.theory.TheoryBench
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A simulated promotion is not enough for a learned rule. These tests pin the second gate:
 * the same formula has to clear the imported history, and a veto has to keep it off the
 * live book even when the simulation liked it.
 */
class HistoryGateTest {

    private fun rows(count: Int = 500): List<HistoryCsv.Row> = (1..count).map { i ->
        HistoryCsv.Row(
            id = "hist-$i",
            playedUtc = i.toLong() * 86_400_000L,
            playerName = "Player $i",
            market = Markets.POINTS,
            line = 20.5,
            overOdds = -110,
            underOdds = -110,
            pOver = 0.60,
            pPush = 0.0,
            actual = if (i <= 300) 25.0 else 15.0,
        )
    }

    @Test
    fun historyKeepsTheMarketOnEachRowAndCanPassACandidate() {
        val props = TheoryBench.toProps(rows(6))
        assertTrue(props.all { it.market == Markets.POINTS })

        val candidate = StrategyParams(minEdgePts = 2.0, sideFilter = SideFilter.OVER_ONLY)
        val result = TheoryBench.run(rows(), grid = listOf(candidate))
        assertEquals(1, result.candidates.size)
        val checked = result.candidates.single()
        assertEquals(500, checked.stats.bets)
        assertTrue("ROI ${checked.stats.roi}", checked.stats.roi > 0.10)
        assertTrue(checked.gate.allPassed)
        assertEquals(listOf(candidate.id), result.passed.map { it.id })
    }

    @Test
    fun aMarketExclusionIsNotSilentlyBetOnHistory() {
        val excluded = StrategyParams(
            minEdgePts = 2.0,
            sideFilter = SideFilter.OVER_ONLY,
            excludeMarkets = listOf(Markets.POINTS),
        )
        val result = TheoryBench.run(rows(), grid = listOf(excluded))
        assertEquals(1, result.candidates.size)
        assertEquals(0, result.candidates.single().stats.bets)
        assertTrue(result.passed.isEmpty())
    }

    @Test
    fun aVetoedCandidateCannotHoldTheBookAndCannotDefendIt() {
        val season = Season.generate(seed = Season.BASE_SEED)
        val champion = requireNotNull(Autopilot.plan(season).promoted) {
            "the base season must promote some formula for the veto to mean anything"
        }

        val withoutChampion = Autopilot.plan(season, vetoed = setOf(champion.id))
        assertFalse(withoutChampion.ready.any { it.id == champion.id })
        assertNotEquals(champion.id, withoutChampion.promoted?.id)

        val defended = Autopilot.plan(
            season,
            incumbent = champion.id,
            vetoed = setOf(champion.id),
        )
        assertFalse("a history veto must remove the incumbent", defended.incumbentHeld)
        assertNotEquals(champion.id, defended.promoted?.id)
    }
}
