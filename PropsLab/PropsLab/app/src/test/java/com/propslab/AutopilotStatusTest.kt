package com.propslab

import com.propslab.data.AutoStepState
import com.propslab.data.AutopilotStatus
import com.propslab.data.BestBetsUi
import com.propslab.data.FeedStateEntity
import com.propslab.data.FormulaUi
import com.propslab.data.LearningTotals
import com.propslab.data.SandboxUi
import com.propslab.data.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Easy mode is only honest if its status card reflects stored state. These tests pin the
 * two cases a novice actually meets: nothing is set up yet, and a live formula has no pick.
 */
class AutopilotStatusTest {

    private fun liveFormula() = FormulaUi(
        id = "e2.0-both",
        label = "Edge ≥ 2.0 pts, both sides",
        params = "edge=2.0;side=both",
        tier = Tier.LIVE,
        bets = 8_000,
        wins = 4_200,
        losses = 3_700,
        pushes = 100,
        staked = 8_000.0,
        profit = 240.0,
        roi = 0.03,
        tStat = 2.8,
        maxDrawdown = 0.18,
        finalBankroll = 490.0,
        passed = true,
        reasons = emptyList(),
        round = 4,
    )

    @Test
    fun aFreshPhoneSaysOneKeyIsMissingAndLearningIsReady() {
        val ui = AutopilotStatus.build(
            bets = BestBetsUi(),
            box = SandboxUi(),
            totals = LearningTotals(),
            feed = FeedStateEntity.EMPTY,
            historyRows = 0,
        )
        assertTrue(ui.needsSetup)
        assertTrue(ui.headline.contains("One key"))
        assertEquals(AutoStepState.NEEDS_YOU, ui.steps.first { it.title == "Board" }.state)
        assertEquals(AutoStepState.WORKING, ui.steps.first { it.title == "Learning" }.state)
        assertFalse(ui.steps.any { it.title == "Second test" })
    }

    @Test
    fun aLiveFormulaWithNoPickDoesNotInventOne() {
        val box = SandboxUi(
            promotedId = "e2.0-both",
            promotedLabel = "Edge ≥ 2.0 pts, both sides",
            formulas = listOf(liveFormula()),
        )
        val ui = AutopilotStatus.build(
            bets = BestBetsUi(
                live = true,
                strategyId = "e2.0-both",
                strategyLabel = "Edge ≥ 2.0 pts, both sides",
            ),
            box = box,
            totals = LearningTotals(settled = 37),
            feed = FeedStateEntity.EMPTY.copy(lastPropsSyncUtc = 1_700_000_000_000L),
            historyRows = 0,
        )
        assertFalse(ui.needsSetup)
        assertTrue(ui.headline.contains("live"))
        assertEquals(AutoStepState.DONE, ui.steps.first { it.title == "Formula" }.state)
        assertEquals(AutoStepState.DONE, ui.steps.first { it.title == "Learning" }.state)
        assertEquals(null, ui.topPickId)
    }

    @Test
    fun importedHistoryBecomesTheFourthAutomaticStep() {
        val ui = AutopilotStatus.build(
            bets = BestBetsUi(),
            box = SandboxUi(),
            totals = LearningTotals(),
            feed = FeedStateEntity.EMPTY,
            historyRows = 512,
        )
        val second = ui.steps.first { it.title == "Second test" }
        assertEquals(AutoStepState.DONE, second.state)
        assertTrue(second.detail.contains("512"))
    }
}
