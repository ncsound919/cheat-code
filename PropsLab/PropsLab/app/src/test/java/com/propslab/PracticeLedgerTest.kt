package com.propslab

import com.propslab.data.BetUi
import com.propslab.data.CalibrationUi
import com.propslab.data.FormulaRecord
import com.propslab.data.FormulaUi
import com.propslab.data.LearningTrack
import com.propslab.data.PracticeLedger
import com.propslab.data.SandboxTradeUi
import com.propslab.domain.Outcome
import com.propslab.domain.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeLedgerTest {

    private fun bet(
        id: String,
        outcome: Outcome? = Outcome.WIN,
        profit: Double? = 0.9,
        stake: Double = 1.0,
        formulaId: String = "",
        formulaLabel: String = "",
    ) = BetUi(
        id = id,
        playerName = "Player $id",
        teamCode = "BOS",
        side = Side.OVER,
        line = 24.5,
        odds = -110,
        stake = stake,
        decidedUtc = 100L,
        actual = if (outcome == null) null else 24.0,
        outcome = outcome,
        profit = profit,
        settledUtc = if (outcome == null) null else 200L,
        market = "player_points",
        book = "book",
        formulaId = formulaId,
        formulaLabel = formulaLabel,
    )

    private fun trade(
        id: String,
        formulaId: String,
        outcome: Outcome?,
        profit: Double?,
        stake: Double = 1.0,
    ) = SandboxTradeUi(
        id = id,
        formulaId = formulaId,
        playerName = "Trade $id",
        side = Side.OVER,
        line = 20.5,
        odds = 100,
        stake = stake,
        outcome = outcome,
        profit = profit,
        startUtc = 100L,
        settledUtc = if (outcome == null) null else 200L,
        synthetic = false,
    )

    private fun formula(
        id: String,
        label: String,
        tier: String = "SANDBOX",
    ) = FormulaUi(
        id = id,
        label = label,
        params = "",
        tier = tier,
        bets = 10,
        wins = 6,
        losses = 4,
        pushes = 0,
        staked = 10.0,
        profit = 1.0,
        roi = 0.1,
        tStat = 2.1,
        maxDrawdown = 0.1,
        finalBankroll = 251.0,
        passed = true,
        reasons = emptyList(),
        round = 1,
    )

    private fun fit(slope: Double, sample: Int = 250) = CalibrationUi(
        track = LearningTrack.PROP,
        slope = slope,
        intercept = 0.0,
        sample = sample,
        brierBefore = 0.24,
        brierAfter = 0.22,
        generation = 1,
        updatedUtc = 100L,
    )

    @Test fun aStakeIsReservedWhileTheBetIsOpen() {
        assertEquals(250.0, PracticeLedger.balance(0.0), 1e-9)
        assertEquals(242.0, PracticeLedger.available(0.0, 8.0), 1e-9)
        assertEquals(258.0, PracticeLedger.available(10.0, 2.0), 1e-9)
        assertEquals(0.0, PracticeLedger.available(-300.0, 0.0), 1e-9)
    }

    @Test fun recordsKeepManualAndFormulaBetsApartAndCombineTheLiveBook() {
        val records = PracticeLedger.records(
            bets = listOf(
                bet("a"),
                bet("b", formulaId = "e2", formulaLabel = "Edge 2"),
                bet("c", formulaId = "e2", formulaLabel = "Edge 2", outcome = Outcome.LOSS, profit = -1.0),
            ),
            trades = listOf(trade("t1", "e2", Outcome.WIN, 1.0)),
            formulas = listOf(formula("e2", "Edge 2", tier = "LIVE")),
        ).associateBy { it.id }

        assertEquals(1, records.getValue(PracticeLedger.MANUAL_FORMULA_ID).settled)
        assertEquals(3, records.getValue("e2").settled)
        assertEquals(2, records.getValue("e2").wins)
        assertEquals(1, records.getValue("e2").losses)
        assertEquals(0.9, records.getValue("e2").profit, 1e-9)
        assertTrue(records.getValue("e2").live)
    }

    @Test fun aThinRecordRefusesToGiveFormulaAdvice() {
        val advice = PracticeLedger.advice(
            bets = (1..3).map { bet("b$it") },
            records = emptyList(),
            calibration = emptyList(),
            promotedLabel = null,
        )
        assertTrue(advice, advice.contains("Only 3 of your own decided"))
        assertTrue(advice.contains("record 5 more"))
    }

    @Test fun theWeakestTaggedFormulaGetsNamed() {
        val losses = (1..5).map { bet("l$it", Outcome.LOSS, -1.0, 1.0, "e2", "Edge 2") }
        val wins = (1..3).map { bet("w$it", Outcome.WIN, 0.9, 1.0, "e2", "Edge 2") }
        val advice = PracticeLedger.advice(
            bets = losses + wins,
            records = PracticeLedger.records(losses + wins, emptyList(), listOf(formula("e2", "Edge 2"))),
            calibration = emptyList(),
            promotedLabel = "Edge 2",
        )
        assertTrue(advice, advice.contains("Edge 2 is the drag"))
        assertTrue(advice.contains("stop tagging"))
    }

    @Test fun aLosingLiveFormulaGetsAdviceEvenWithNoManualResults() {
        val advice = PracticeLedger.advice(
            bets = emptyList(),
            records = listOf(
                FormulaRecord(
                    id = "e2", label = "Edge 2", bets = 5, open = 0, wins = 1, losses = 4, pushes = 0,
                    staked = 5.0, profit = -3.0, live = true,
                ),
            ),
            calibration = emptyList(),
            promotedLabel = "Edge 2",
        )
        assertTrue(advice, advice.contains("Edge 2 is the drag"))
    }

    @Test fun anOverconfidentModelOutranksTheFormulaRecord() {
        val bets = (1..9).map { bet("b$it") }
        val advice = PracticeLedger.advice(
            bets = bets,
            records = emptyList(),
            calibration = listOf(fit(slope = 0.60)),
            promotedLabel = "Edge 2",
        )
        assertTrue(advice, advice.contains("overconfident"))
        assertTrue(advice.contains("0.60"))
        assertTrue(advice.contains("raise the minimum edge"))
    }

    @Test fun aProfitableRecordStillSaysToKeepStakesFlat() {
        val advice = PracticeLedger.advice(
            bets = (1..9).map { bet("b$it", Outcome.WIN, 0.9) },
            records = emptyList(),
            calibration = emptyList(),
            promotedLabel = "Edge 2",
        )
        assertTrue(advice, advice.contains("units up"))
        assertTrue(advice.contains("Keep the stake flat"))
    }

    @Test fun anEmptyLedgerExplainsWhatStartsTheAdvice() {
        val advice = PracticeLedger.advice(emptyList(), emptyList(), emptyList(), null)
        assertTrue(advice.contains("No results yet"))
        assertTrue(advice.contains("8 decided bets"))
    }

    private fun settledBet(id: String, outcome: Outcome, ts: Long, profit: Double = 0.9) = BetUi(
        id = id,
        playerName = "Player $id",
        teamCode = "BOS",
        side = Side.OVER,
        line = 24.5,
        odds = -110,
        stake = 1.0,
        decidedUtc = ts,
        actual = 24.0,
        outcome = outcome,
        profit = profit,
        settledUtc = ts,
        market = "player_points",
        book = "book",
    )

    @Test fun streakCountsConsecutiveWinsFromTheLatest() {
        val bets = listOf(
            settledBet("a", Outcome.WIN, 1L),
            settledBet("b", Outcome.LOSS, 2L),
            settledBet("c", Outcome.WIN, 3L),
            settledBet("d", Outcome.WIN, 4L),
        )
        assertEquals(2, PracticeLedger.streak(bets))
    }

    @Test fun streakGoesNegativeOnALosingRun() {
        val bets = listOf(
            settledBet("a", Outcome.WIN, 1L),
            settledBet("b", Outcome.LOSS, 2L),
            settledBet("c", Outcome.LOSS, 3L),
            settledBet("d", Outcome.LOSS, 4L),
        )
        assertEquals(-3, PracticeLedger.streak(bets))
    }

    @Test fun streakSkipsPushesInsteadOfEndingTheRun() {
        val bets = listOf(
            settledBet("a", Outcome.WIN, 1L),
            settledBet("b", Outcome.PUSH, 2L, profit = 0.0),
            settledBet("c", Outcome.WIN, 3L),
        )
        assertEquals(2, PracticeLedger.streak(bets))
    }

    @Test fun streakIsZeroWithNothingDecided() {
        assertEquals(0, PracticeLedger.streak(emptyList()))
        assertEquals(0, PracticeLedger.streak(listOf(bet("o", null, null))))
    }

    @Test fun bestStreakFindsTheLongestWinningRun() {
        val bets = listOf(
            settledBet("a", Outcome.WIN, 1L),
            settledBet("b", Outcome.WIN, 2L),
            settledBet("c", Outcome.WIN, 3L),
            settledBet("d", Outcome.LOSS, 4L),
            settledBet("e", Outcome.WIN, 5L),
            settledBet("f", Outcome.WIN, 6L),
        )
        assertEquals(3, PracticeLedger.bestStreak(bets))
        assertEquals(2, PracticeLedger.streak(bets))
    }

    @Test fun formReturnsTheNewestDecidedResultsOldestFirst() {
        val bets = listOf(
            settledBet("a", Outcome.WIN, 1L),
            settledBet("b", Outcome.PUSH, 2L, profit = 0.0),
            settledBet("c", Outcome.LOSS, 3L),
            settledBet("d", Outcome.WIN, 4L),
        )
        assertEquals(listOf(Outcome.WIN, Outcome.LOSS, Outcome.WIN), PracticeLedger.form(bets))
        assertEquals(listOf(Outcome.LOSS, Outcome.WIN), PracticeLedger.form(bets, n = 2))
    }

    @Test fun badgesTrackWhatTheRecordActuallyEarned() {
        val settled = (1..10).map { settledBet("s$it", Outcome.WIN, it.toLong()) }
        val badges = PracticeLedger.badges(emptyList(), settled).associateBy { it.id }
        assertEquals(true, badges.getValue("first-bet").earned)
        assertEquals(true, badges.getValue("first-win").earned)
        assertEquals(true, badges.getValue("ten-settled").earned)
        assertEquals(false, badges.getValue("twenty-five").earned)
        assertEquals(true, badges.getValue("streak-5").earned)
        assertEquals(true, badges.getValue("in-profit").earned)
        assertEquals(false, badges.getValue("ten-units").earned)
    }

    @Test fun badgesStartUnearnedOnAnEmptyLedger() {
        val badges = PracticeLedger.badges(emptyList(), emptyList())
        assertTrue(badges.isNotEmpty())
        assertTrue(badges.none { it.earned })
    }
}
