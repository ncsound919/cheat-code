package com.propslab

import com.propslab.data.BestPickUi
import com.propslab.data.ParlayBoardUi
import com.propslab.data.ParlayCandidate
import com.propslab.data.ParlayControls
import com.propslab.data.ParlayKind
import com.propslab.data.ParlayLab
import com.propslab.data.ParlayOptions
import com.propslab.data.ParlayStrategyUi
import com.propslab.data.ParlayTickets
import com.propslab.data.FormulaScope
import com.propslab.data.PropUi
import com.propslab.data.Tier
import com.propslab.domain.Circadian
import com.propslab.domain.LinePush
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.ParlayStaking
import com.propslab.domain.SameGame
import com.propslab.domain.Side
import com.propslab.domain.StakeOffer
import com.propslab.feed.GameLogModel
import com.propslab.inference.ScoreResult
import com.propslab.sim.SideFilter
import com.propslab.sim.StrategyParams
import org.junit.Assert.*
import org.junit.Test

class ParlayLabTest {

    private fun utc(hours: Double): Long = (hours * 3_600_000.0).toLong()

    // ------------------------------------------------------------------ line push

    @Test fun noPushReturnsTheQuotedPriceAndChance() {
        val leg = LinePush.reprice(
            quotedOdds = -110, line = 24.5, side = Side.OVER, points = 0.0,
            overProb = 0.55, pushProb = 0.0, mean = 25.0, spread = 5.0,
        )!!
        assertEquals(-110, leg.odds)
        assertEquals(24.5, leg.line, 1e-9)
        assertEquals(0.55, leg.probability, 1e-9)
        assertEquals(1.0, leg.fairFactor, 1e-9)
        assertFalse(leg.inferredCurve)
    }

    @Test fun aPushBuysPriceWithChance() {
        val base = LinePush.reprice(-110, 24.5, Side.OVER, 0.0, 0.55, 0.0, 25.0, 5.0)!!
        val pushed = LinePush.reprice(-110, 24.5, Side.OVER, 2.0, 0.55, 0.0, 25.0, 5.0)!!
        assertEquals(26.5, pushed.line, 1e-9)
        assertTrue("a longer price", pushed.odds > base.odds)
        assertTrue("a lower chance", pushed.probability < base.probability)
        assertTrue(pushed.fairFactor > 1.0)
    }

    @Test fun anUnderPushFallsToo() {
        val base = LinePush.reprice(-110, 24.5, Side.UNDER, 0.0, 0.55, 0.0, 25.0, 5.0)!!
        val pushed = LinePush.reprice(-110, 24.5, Side.UNDER, 1.0, 0.55, 0.0, 25.0, 5.0)!!
        // Under 23.5 needs fewer points than Under 24.5, so it is harder and pays longer.
        assertEquals(23.5, pushed.line, 1e-9)
        assertTrue("a longer price", pushed.odds > base.odds)
        assertTrue("a lower chance", pushed.probability < base.probability)
    }

    @Test fun pushingThatManyPointsIsRefusedAboveTheCap() {
        val leg = LinePush.reprice(-110, 24.5, Side.OVER, 40.0, 0.55, 0.0, 25.0, 5.0)!!
        assertEquals(24.5 + LinePush.MAX_POINTS, leg.line, 1e-9)
    }

    @Test fun aMissingGameLogStillGivesACurve() {
        val curve = LinePush.curve(0.552, 0.0, 26.5)!!
        assertTrue(curve.inferred)
        val (over, push, under) = LinePush.probabilities(curve.mean, curve.spread, 26.5)!!
        assertEquals(0.552, over, 0.002)
        assertEquals(0.0, push, 1e-9)
        assertEquals(0.448, under, 0.002)
    }

    @Test fun anInferredIntegerCurveKeepsItsPushMass() {
        val curve = LinePush.curve(0.472, 0.046, 25.0)!!
        assertTrue(curve.inferred)
        val (over, push, under) = LinePush.probabilities(curve.mean, curve.spread, 25.0)!!
        assertEquals(0.472, over, 0.01)
        assertEquals(0.046, push, 0.01)
        assertEquals(0.482, under, 0.01)
    }

    /**
     * The repricer must agree with the model that published the quote. If it did not, the
     * first half-point of slider movement would be a jump rather than a reprice.
     */
    @Test fun thePushCurveAgreesWithTheGameLogModel() {
        val values = listOf(31.0, 22.0, 26.0, 18.0, 29.0, 24.0, 27.0, 33.0, 21.0, 25.0, 30.0, 23.0)
        val (mean, spread) = GameLogModel.meanAndSpread(values)
        for (line in listOf(24.5, 25.0)) {
            val scored = GameLogModel.score(values, line) as ScoreResult.Scored
            val (over, push, under) = LinePush.probabilities(mean, spread, line)!!
            assertEquals(scored.probabilities.over, over, 1e-9)
            assertEquals(scored.probabilities.push, push, 1e-9)
            assertEquals(scored.probabilities.under, under, 1e-9)
        }
    }

    @Test fun aBookPriceIsNeverBetweenMinus99And100() {
        listOf(1.02, 1.5, 1.95, 2.4, 4.0, 25.0).forEach { decimal ->
            val american = LinePush.bookAmerican(decimal)
            assertTrue("$american for $decimal", american >= 100 || american <= -100)
        }
    }

    // ------------------------------------------------------------------ circadian

    @Test fun anEveningGameOnTheTeamsOwnClockIsUntouched() {
        val impact = Circadian.impact(utc(24.5), "BOS", "BOS")!!
        assertEquals(19.5, impact.localHour, 1e-9)
        assertEquals(0.0, impact.offProjectionPct, 1e-9)
        assertEquals(1.0, impact.meanMultiplier, 1e-9)
    }

    @Test fun aWestCoastTeamPlayingEarlyInNewYorkIsPenalised() {
        val impact = Circadian.impact(utc(24.5), "LAL", "NYK")!!
        assertEquals(16.5, impact.localHour, 1e-9)
        assertEquals(-3, impact.zoneShift)
        assertEquals(1.0, impact.offProjectionPct, 1e-6)
        assertTrue(impact.meanMultiplier < 1.0)
    }

    @Test fun theCorrectionIsCappedAtOneAndAHalfPercent() {
        val impact = Circadian.impact(utc(17.0), "LAL", "NYK")!!
        assertEquals(Circadian.MAX_OFF_PCT, impact.offProjectionPct, 1e-9)
        assertEquals(0.985, impact.meanMultiplier, 1e-9)
    }

    @Test fun unknownTeamsOrTimesMakeNoClaim() {
        assertNull(Circadian.impact(0L, "BOS", "BOS"))
        assertNull(Circadian.impact(utc(24.5), "XXX", "YYY"))
    }

    // ------------------------------------------------------------------ same game

    @Test fun aSameGameAdjustmentRaisesChanceAndCutsPrice() {
        val adjusted = SameGame.adjust(0.15, 6.0, together = false)!!
        assertEquals(0.15, adjusted.naiveProbability, 1e-9)
        assertEquals(6.0, adjusted.naiveDecimal, 1e-9)
        assertTrue("correlation lifts the joint chance", adjusted.probability > 0.15)
        assertTrue("and cuts the price", adjusted.decimal < 6.0)
        assertEquals(SameGame.SAME_GAME_SHRINK, adjusted.shrink, 1e-9)
    }

    @Test fun twoLegsOnOnePlayerAreTreatedAsMoreCorrelated() {
        val apart = SameGame.adjust(0.15, 6.0, together = false)!!
        val same = SameGame.adjust(0.15, 6.0, together = true)!!
        assertTrue(same.probability > apart.probability)
        assertTrue(same.decimal < apart.decimal)
    }

    @Test fun theBuilderSpotsAThreeLegSameGameTicket() {
        val legs = listOf("A", "B", "C").map { name ->
            candidate(key = "p$name", game = "g1", subject = "Player $name", odds = -110, pWin = 0.6, edge = 3.0)
        }
        val ticket = ParlayTickets.buildSameGame(legs)!!
        assertEquals(ParlayKind.SAME_GAME, ticket.kind)
        assertEquals(1, ticket.games)
        assertEquals(3, ticket.legs.size)
        assertEquals(3, ticket.legs.map { it.candidate.subject }.distinct().size)
        assertNotNull(ticket.naiveProbability)
        assertTrue(ticket.probability > ticket.naiveProbability!!)
        // Three -110 legs multiply to 1.909^3 = 6.96x; the correlation cut brings it down.
        assertTrue(ticket.decimal < 6.958)
        assertTrue(ticket.decimal > 1.0)
    }

    @Test fun aThinBoardCanStillShowATwoLegSameGameTicket() {
        val legs = listOf(
            candidate("p1", "g1", "A", -110, 0.6, 3.0),
            candidate("p2", "g1", "B", -110, 0.6, 2.0),
        )
        val ticket = ParlayTickets.buildSameGame(legs, size = 2)!!
        assertEquals(2, ticket.legs.size)
        assertTrue(ticket.probability > ticket.naiveProbability!!)
    }

    @Test fun twoLegsInOneGameAreNotAThreeLegSameGameTicket() {
        val legs = listOf(
            candidate("p1", "g1", "A", -110, 0.6, 3.0),
            candidate("p2", "g1", "B", -110, 0.6, 3.0),
            candidate("p3", "g2", "C", -110, 0.6, 3.0),
        )
        assertNull(ParlayTickets.buildSameGame(legs))
    }

    @Test fun aSameGameTicketUsesOneLegPerPlayer() {
        val legs = listOf(
            candidate("p1", "g1", "A", -110, 0.6, 4.0),
            candidate("p2", "g1", "A", -105, 0.6, 3.0),
            candidate("p3", "g1", "B", -110, 0.6, 2.0),
            candidate("p4", "g1", "C", -110, 0.6, 1.0),
        )
        val ticket = ParlayTickets.buildSameGame(legs)!!
        assertEquals(3, ticket.legs.size)
        assertEquals(3, ticket.legs.map { it.candidate.subject }.distinct().size)
    }

    // ------------------------------------------------------------------ budget

    @Test fun allocationNeverSpendsMoreThanTheBudget() {
        val offers = listOf(
            StakeOffer("3", 0.6, 3.0),
            StakeOffer("5", 0.5, 2.5),
            StakeOffer("10", 0.3, 4.0),
            StakeOffer("sgp", 0.35, 3.5),
        )
        val stakes = ParlayStaking.allocate(100.0, offers, fraction = 0.25, capShare = 0.25)
        assertEquals(4, stakes.size)
        assertTrue(stakes.sumOf { it.stake } <= 100.0 + 1e-9)
        stakes.forEach { stake ->
            assertTrue(stake.stake >= 0.0)
            assertTrue("cap", stake.stake <= 25.0 + 1e-9)
        }
    }

    @Test fun aTicketWithNoEdgeGetsNoStake() {
        val stakes = ParlayStaking.allocate(100.0, listOf(StakeOffer("3", 0.2, 3.0)))
        assertEquals(0.0, stakes.single().stake, 1e-9)
    }

    @Test fun aFractionOfKellySetsTheStake() {
        // p=0.6 at 3.0x: Kelly = (1.2 - 0.4) / 2 = 0.4, a quarter of that is 10%.
        val stakes = ParlayStaking.allocate(100.0, listOf(StakeOffer("3", 0.6, 3.0)))
        assertEquals(10.0, stakes.single().stake, 0.011)
        assertEquals(0.4, stakes.single().kelly, 1e-9)
    }

    @Test fun thePerTicketCapBindsBeforeFullKellyDoes() {
        // Full Kelly on this price is 87.5% of the budget; the 25% cap holds it down.
        val stakes = ParlayStaking.allocate(100.0, listOf(StakeOffer("3", 0.9, 5.0)), fraction = 1.0)
        assertEquals(25.0, stakes.single().stake, 1e-9)
        assertTrue(stakes.single().capped)
    }

    @Test fun aZeroBudgetStakesNothing() {
        val stakes = ParlayStaking.allocate(0.0, listOf(StakeOffer("3", 0.6, 3.0)))
        assertEquals(0.0, stakes.single().stake, 1e-9)
    }

    // ------------------------------------------------------------------ push in a ticket

    @Test fun aPushedTicketIsLongerAndLessLikely() {
        val legs = (1..3).map { i ->
            candidate(key = "p$i", game = "g$i", subject = "Player $i", odds = -110, pWin = 0.6, edge = 3.0)
                .copy(line = 24.5, mean = 25.0, spread = 5.0, overProb = 0.6, side = Side.OVER)
        }
        val plain = ParlayTickets.build(legs, 3)!!
        val pushed = ParlayTickets.build(legs, 3, ParlayOptions(pushPoints = 2.0))!!
        assertEquals(3, pushed.legs.count { it.shiftPts == 2.0 })
        assertEquals(26.5, pushed.legs.first().line!!, 1e-9)
        assertTrue(pushed.decimal > plain.decimal)
        assertTrue(pushed.probability < plain.probability)
        assertTrue(pushed.explanation.contains("moved 2.0 points"))
    }

    @Test fun aGameMarketKeepsItsLineWhenPushIsOn() {
        val game = ParlayCandidate(
            key = "g:atl", gameId = "g-atl", subject = null, label = "ATL moneyline",
            detail = "CLE at ATL", odds = -118, pWin = 0.59, pPush = 0.0, edgePts = 2.0,
            line = null, venueCode = "ATL", startUtc = utc(24.5),
        )
        val props = (1..2).map { i ->
            candidate("p$i", "pg$i", "Player $i", -110, 0.6, 3.0)
                .copy(line = 20.5, mean = 21.0, spread = 4.0, overProb = 0.6)
        }
        val ticket = ParlayTickets.build(listOf(game) + props, 3, ParlayOptions(pushPoints = 1.0))!!
        val gameLeg = ticket.legs.first { it.candidate.subject == null }
        assertEquals(0.0, gameLeg.shiftPts, 1e-9)
        assertEquals(-118, gameLeg.odds)
        assertTrue(gameLeg.note.contains("quoted line"))
    }

    // ------------------------------------------------------------------ formula scopes

    @Test fun scopesNameWhatTheFormulaOwns() {
        val broad = StrategyParams(minEdgePts = 0.0)
        val strict = StrategyParams(minEdgePts = 4.0, sideFilter = SideFilter.OVER_ONLY)
        val text = FormulaScope.describe(strict, qualified = 4, considered = 12, widest = broad to 12)
        assertTrue(text.contains("overs only"))
        assertTrue(text.contains("4.0"))
        assertTrue(text.contains("4 of 12"))
        assertTrue(text.contains("stricter"))
        assertTrue(text.contains("never its to change"))
    }

    @Test fun aBroadFormulaIsComparedAsTheWidest() {
        val text = FormulaScope.describe(StrategyParams(minEdgePts = 0.0), 12, 12, widest = null)
        assertTrue(text.contains("broadest formula"))
    }

    // ------------------------------------------------------------------ composition

    @Test fun composeSplitsTheBudgetAcrossTheTickets() {
        val board = boardOf(
            strategy("live", "Live formula", picks = listOf(
                pick("p1", "g1", 0.6, 3.0),
                pick("p2", "g2", 0.6, 2.5),
                pick("p3", "g3", 0.6, 2.0),
            )),
        )
        val ui = ParlayLab.compose(board, ParlayControls(budget = "100", circadian = false), sizes = listOf(3))
        assertEquals(1, ui.tickets.size)
        val ticket = ui.tickets.single()
        assertTrue(ui.staked > 0.0)
        assertTrue(ui.staked <= ui.budget + 1e-9)
        assertEquals(ui.budget - ui.staked, ui.unallocated, 1e-9)
        assertTrue(ticket.allocatedStake <= 25.0 + 1e-9)
    }

    @Test fun composeLetsEachTicketUseADifferentFormula() {
        val live = strategy("live", "Live", picks = listOf(
            pick("p1", "g1", 0.6, 3.0),
            pick("p2", "g2", 0.6, 2.5),
            pick("p3", "g3", 0.6, 2.0),
        ))
        val strict = strategy("strict", "Strict", picks = listOf(
            pick("s1", "h1", 0.58, 6.0),
            pick("s2", "h2", 0.58, 5.5),
            pick("s3", "h3", 0.58, 5.0),
        ))
        val board = boardOf(live, strict)
        val ui = ParlayLab.compose(
            board,
            ParlayControls(budget = "50", circadian = false, byTicket = mapOf("3" to "strict")),
            sizes = listOf(3),
        )
        val ticket = ui.tickets.singleOrNull { it.key == "3" }!!
        assertEquals("strict", ticket.strategyId)
        assertEquals("Strict", ticket.strategyLabel)
        assertTrue(ticket.legs.any { it.candidate.key == "p:s1:OVER" })
    }

    @Test fun anEmptyBudgetStakesNothingButStillBuildsTickets() {
        val board = boardOf(strategy("live", "Live", picks = listOf(
            pick("p1", "g1", 0.6, 3.0),
            pick("p2", "g2", 0.6, 2.5),
            pick("p3", "g3", 0.6, 2.0),
        )))
        val ui = ParlayLab.compose(board, ParlayControls(budget = "", circadian = false), listOf(3))
        assertEquals(1, ui.tickets.size)
        assertEquals(0.0, ui.staked, 1e-9)
        assertEquals(0.0, ui.tickets.single().allocatedStake, 1e-9)
    }

    // ------------------------------------------------------------------ formula capacity

    @Test fun aFormulaThatCannotFillTheTicketIsIneligible() {
        val full = strategy("live", "Live", picks = listOf(
            pick("p1", "g1", 0.6, 3.0),
            pick("p2", "g2", 0.6, 2.5),
            pick("p3", "g3", 0.6, 2.0),
        ))
        val thin = strategy("thin", "Thin", picks = listOf(pick("s1", "h1", 0.58, 6.0)))
        val ui = ParlayLab.compose(
            boardOf(full, thin),
            ParlayControls(circadian = false),
            sizes = listOf(3),
        )
        assertEquals(setOf("live"), ui.eligible["3"])
        assertEquals("live", ui.tickets.single().strategyId)
    }

    @Test fun choosingAnIneligibleFormulaFallsBackAndSaysWhy() {
        val full = strategy("live", "Live", picks = listOf(
            pick("p1", "g1", 0.6, 3.0),
            pick("p2", "g2", 0.6, 2.5),
            pick("p3", "g3", 0.6, 2.0),
        ))
        val thin = strategy("thin", "Thin", picks = listOf(pick("s1", "h1", 0.58, 6.0)))
        val ui = ParlayLab.compose(
            boardOf(full, thin),
            ParlayControls(circadian = false, byTicket = mapOf("3" to "thin")),
            sizes = listOf(3),
        )
        assertEquals(1, ui.tickets.size)
        assertEquals("live", ui.tickets.single().strategyId)
        assertTrue(ui.warnings.any { it.contains("Thin") && it.contains("cannot fill") })
    }

    @Test fun sameGameEligibilityNeedsEnoughPlayersInOneGame() {
        val oneGame = strategy("live", "Live", picks = listOf(
            pick("p1", "g1", 0.6, 3.0),
            pick("p2", "g1", 0.6, 2.5),
            pick("p3", "g1", 0.6, 2.0),
        ))
        val spread = strategy("spread", "Spread", picks = listOf(
            pick("s1", "h1", 0.6, 3.0),
            pick("s2", "h2", 0.6, 2.5),
            pick("s3", "h3", 0.6, 2.0),
        ))
        val ui = ParlayLab.compose(
            boardOf(oneGame, spread),
            ParlayControls(circadian = false),
            sizes = listOf(3),
        )
        assertEquals(setOf("live"), ui.eligible["sgp"])
        assertEquals(ParlayKind.SAME_GAME, ui.tickets.first { it.key == "sgp" }.kind)
    }

    @Test fun aTwoLegSameGameOptionCountsWhenOnlyTwoPlayersAreAvailable() {
        val two = strategy("live", "Live", picks = listOf(
            pick("p1", "g1", 0.6, 3.0),
            pick("p2", "g1", 0.6, 2.5),
            pick("p3", "g2", 0.6, 2.0),
        ))
        val ui = ParlayLab.compose(
            boardOf(two),
            ParlayControls(circadian = false),
            sizes = listOf(3),
        )
        assertTrue(ui.eligible["sgp"]!!.contains("live"))
        assertEquals(2, ui.tickets.first { it.key == "sgp" }.legs.size)
    }

    // ------------------------------------------------------------------ helpers

    private fun candidate(
        key: String,
        game: String,
        subject: String,
        odds: Int,
        pWin: Double,
        edge: Double,
    ) = ParlayCandidate(
        key = key, gameId = game, subject = subject, label = "$subject Over 20.5 Points",
        detail = "$game · book", odds = odds, pWin = pWin, pPush = 0.0, edgePts = edge,
    )

    private fun strategy(id: String, label: String, picks: List<BestPickUi>) = ParlayStrategyUi(
        id = id,
        label = label,
        tier = if (id == "live") Tier.LIVE else Tier.SANDBOX,
        params = StrategyParams(minEdgePts = 2.0),
        picks = picks,
        scope = "scope",
    )

    private fun boardOf(vararg strategies: ParlayStrategyUi) = ParlayBoardUi(
        strategies = strategies.toList(),
        gamePicks = emptyList(),
        live = strategies.any { it.tier == Tier.LIVE },
        priced = strategies.sumOf { it.picks.size },
    )

    private fun pick(id: String, game: String, pWin: Double, edge: Double): BestPickUi {
        val probs = OutcomeProbabilities(pWin, 0.0, 1.0 - pWin)
        val prop = PropUi(
            id = id,
            playerName = "Player $id",
            teamCode = "BOS",
            gameId = game,
            matchup = "NYK at BOS",
            startUtc = utc(24.5),
            market = "Points",
            book = "book",
            line = 20.5,
            overOdds = -110,
            underOdds = -110,
            quoteTimeUtc = utc(23.0),
            probabilities = probs,
            marketOver = 1.0 - pWin,
            edgePts = edge,
            reasons = emptyList(),
            flags = emptyList(),
            drivers = emptyList(),
            explanation = "",
            modelVersion = "test",
            schemaVersion = 1,
            featureCutoffUtc = utc(23.0),
            isFixture = false,
        )
        return BestPickUi(
            prop = prop,
            side = Side.OVER,
            odds = -110,
            edgePts = edge,
            modelProb = pWin,
            marketProb = 1.0 - pWin,
            ev = 0.05,
            breakeven = 0.5,
            kelly = 0.1,
            live = true,
            shadow = false,
        )
    }
}
