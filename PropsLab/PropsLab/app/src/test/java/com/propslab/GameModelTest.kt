package com.propslab

import com.propslab.data.LearningSettlement
import com.propslab.data.LearningSide
import com.propslab.domain.Calibration
import com.propslab.domain.CalibrationPoint
import com.propslab.domain.Outcome
import com.propslab.domain.GameModel
import com.propslab.domain.TeamGameRow
import com.propslab.inference.ChampionTable
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

class GameModelTest {

    private fun game(
        id: String,
        team: String,
        opponent: String,
        scored: Int,
        allowed: Int,
        at: Long = 0L,
    ) = TeamGameRow(id, team, opponent, scored, allowed, at)

    /** Ten games for each of two teams, alternating 110-100 and 100-110. */
    private fun balancedRows(): List<TeamGameRow> = (0 until 10).flatMap { i ->
        val a = if (i % 2 == 0) 110 else 104
        val b = if (i % 2 == 0) 104 else 110
        listOf(
            game("g$i", "AAA", "BBB", a, b, i * 1000L),
            game("g$i", "BBB", "AAA", b, a, i * 1000L),
        )
    }

    @Test fun normalCdfMatchesTheTable() {
        assertEquals(0.5, GameModel.normalCdf(0.0), 1e-9)
        assertEquals(0.975, GameModel.normalCdf(1.959964), 1e-5)
        assertEquals(0.025, GameModel.normalCdf(-1.959964), 1e-5)
        assertEquals(1.0, GameModel.normalCdf(9.0), 1e-6)
        assertEquals(0.0, GameModel.normalCdf(-9.0), 1e-6)
    }

    @Test fun ratingsAreShrunkTowardTheLeague() {
        val book = GameModel.build(balancedRows())
        assertEquals(107.0, book.leagueAverage, 1e-9)
        assertEquals(10, book.teams.getValue("AAA").games)
        // Exactly balanced, so the shrunk rating is the league average.
        assertEquals(107.0, book.teams.getValue("AAA").scored, 1e-9)
        assertEquals(107.0, book.teams.getValue("AAA").allowed, 1e-9)
    }

    @Test fun aThinTeamIsPulledTowardAverage() {
        val rows = listOf(game("g1", "AAA", "BBB", 130, 100, 1_000L)) +
            balancedRows()
        val book = GameModel.build(rows)
        val aaa = book.teams.getValue("AAA")
        // One blowout in eleven games must not make AAA a 130-point offense.
        assertTrue(aaa.scored < 112.0)
        assertTrue(aaa.scored > 107.0)
    }

    @Test fun forecastRefusesAShortSample() {
        val book = GameModel.build(
            listOf(
                game("g1", "AAA", "BBB", 110, 100, 1_000L),
                game("g1", "BBB", "AAA", 100, 110, 1_000L),
            ),
        )
        assertNull(GameModel.forecast(book, "AAA", "BBB"))
    }

    @Test fun aForecastPutsTheHomeTeamAhead() {
        val book = GameModel.build(balancedRows())
        val f = GameModel.forecast(book, "AAA", "BBB")!!
        assertEquals(2.6, f.expectedMargin, 1e-9)
        assertTrue(GameModel.moneyline(f).leftNoPush > 0.55)
        assertEquals(214.0, f.expectedTotal, 1e-9)
    }

    @Test fun spreadAndTotalAreValidDistributions() {
        val book = GameModel.build(balancedRows())
        val f = GameModel.forecast(book, "AAA", "BBB")!!
        for (line in listOf(-7.5, -2.5, 0.5, 3.5, 2.0, -3.0, 0.0)) {
            val c = GameModel.spread(f, line)
            assertEquals(1.0, c.left + c.push + c.right, 1e-9)
            assertTrue(c.left >= 0.0 && c.right >= 0.0 && c.push >= 0.0)
            if (abs(line - Math.rint(line)) < 1e-9) assertTrue("line $line should push", c.push > 0.0)
            else assertEquals(0.0, c.push, 1e-12)
        }
        for (line in listOf(207.5, 214.0, 220.5, 230.0)) {
            val c = GameModel.total(f, line)
            assertEquals(1.0, c.left + c.push + c.right, 1e-9)
            assertTrue(c.left >= 0.0 && c.right >= 0.0 && c.push >= 0.0)
        }
    }

    @Test fun aLineBelowTheProjectionFavoursTheOver() {
        val book = GameModel.build(balancedRows())
        val f = GameModel.forecast(book, "AAA", "BBB")!!
        assertTrue(GameModel.total(f, 200.5).left > 0.5)
        assertTrue(GameModel.total(f, 220.5).left < 0.5)
        assertTrue(GameModel.total(f, 207.0).push > 0.0)
    }

    @Test fun theEmpiricalTablePricesHalfAndIntegerLines() {
        val json = """
            {"kind":"propslab_empirical_distribution","version":1,"bucket_width":0.5,"kmax":5,
             "fit_rows":123,"fit_max_season":"2022-23","source_dataset":"tap919/test",
             "source_sha256":"abc123",
             "cdf":[[0.1,0.2,0.4,0.7,0.9,1.0],[0.05,0.15,0.35,0.65,0.9,1.0]]}
        """.trimIndent()
        val table = ChampionTable.fromJson(json)!!
        assertEquals(123, table.fitRows)
        assertEquals(0.075, table.cdfAt(0, 0.25), 1e-9)
        val half = table.probabilities(0.0, 2.5)!!
        assertEquals(0.6, half.over, 1e-9)
        assertEquals(0.4, half.under, 1e-9)
        assertEquals(0.0, half.push, 1e-12)
        val whole = table.probabilities(0.0, 2.0)!!
        assertEquals(0.2, whole.push, 1e-9)
        assertEquals(0.6, whole.over, 1e-9)
        assertEquals(0.2, whole.under, 1e-9)
        assertEquals(1.0, half.over + half.push + half.under, 1e-9)
        // Outside the table's range the loader refuses rather than extrapolating.
        assertNull(table.probabilities(0.0, 0.0))
        assertNull(table.probabilities(0.0, 7.0))
        assertNull(ChampionTable.fromJson("{}"))
    }

    @Test fun gameMarketsSettleLikeABook() {
        // Moneyline has no push.
        assertEquals(Outcome.WIN, LearningSettlement.game("h2h", LearningSide.HOME, 0.0, 112, 104))
        assertEquals(Outcome.LOSS, LearningSettlement.game("h2h", LearningSide.AWAY, 0.0, 112, 104))
        assertNull(LearningSettlement.game("h2h", LearningSide.HOME, 0.0, 100, 100))
        // Home −3.5 covers at 4, loses at 3.
        assertEquals(Outcome.WIN, LearningSettlement.game("spreads", LearningSide.HOME, -3.5, 104, 100))
        assertEquals(Outcome.LOSS, LearningSettlement.game("spreads", LearningSide.HOME, -3.5, 103, 100))
        // The away side takes the mirror, and an integer line pushes on the number.
        assertEquals(Outcome.WIN, LearningSettlement.game("spreads", LearningSide.AWAY, 3.5, 103, 100))
        assertEquals(Outcome.PUSH, LearningSettlement.game("spreads", LearningSide.HOME, -3.0, 103, 100))
        assertEquals(Outcome.PUSH, LearningSettlement.game("spreads", LearningSide.AWAY, 3.0, 103, 100))
        // Totals push on the exact number.
        assertEquals(Outcome.WIN, LearningSettlement.game("totals", LearningSide.OVER, 210.5, 108, 103))
        assertEquals(Outcome.LOSS, LearningSettlement.game("totals", LearningSide.UNDER, 210.5, 108, 103))
        assertEquals(Outcome.PUSH, LearningSettlement.game("totals", LearningSide.OVER, 211.0, 108, 103))
        assertNull(LearningSettlement.game("player_points", LearningSide.OVER, 20.5, 108, 103))
    }

    @Test fun calibrationRecoversAKnownCorrection() {
        val points = (0 until 4000).map { i ->
            val p = 0.30 + (i % 41) / 100.0
            val x = ln(p / (1.0 - p))
            val truth = sigmoid(1.2 * x + 0.3)
            val draw = ((i * 7919) % 1000) / 1000.0
            CalibrationPoint(p, draw < truth)
        }
        val fit = Calibration.fit(points)!!
        assertEquals(1.2, fit.slope, 0.18)
        assertEquals(0.3, fit.intercept, 0.18)
        assertTrue(fit.brierAfter < fit.brierBefore)
        assertEquals(0, Calibration.fit(points.take(50))?.sample ?: 0)
    }

    @Test fun aSettledPushIsNotALearningPoint() {
        val points = (0 until 500).map { CalibrationPoint(0.55, it % 2 == 0) }
        val fit = Calibration.fit(points)!!
        assertEquals(500, fit.sample)
        // 55% called on an even split must be pulled down, not up.
        assertTrue(fit.slope < 1.0)
        assertTrue(Calibration.apply(fit, 0.55) < 0.55)
    }

    private fun sigmoid(z: Double) = 1.0 / (1.0 + exp(-z))
}
