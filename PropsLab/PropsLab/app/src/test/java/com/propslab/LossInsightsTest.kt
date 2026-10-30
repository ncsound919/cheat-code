package com.propslab

import com.propslab.domain.Markets
import com.propslab.domain.Outcome
import com.propslab.domain.Side
import com.propslab.sim.InsightKind
import com.propslab.sim.LossInsights
import com.propslab.sim.LossObservation
import com.propslab.sim.SandboxEngine
import com.propslab.sim.Season
import com.propslab.sim.SideFilter
import com.propslab.sim.SimProp
import com.propslab.sim.StrategyParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The loss analysis is allowed to be wrong about the cause; it is not allowed to be
 * mysterious. These tests pin the evidence, the minimum sample, and the fact that every
 * proposal is a rule the existing gate can test rather than an automatic promotion.
 */
class LossInsightsTest {

    private var sequence = 0L

    private fun observation(
        side: Side,
        outcome: Outcome,
        market: String = Markets.POINTS,
        odds: Int = -110,
        edge: Double? = 2.0,
        line: Double = 20.5,
    ) = LossObservation(
        market = market,
        side = side,
        line = line,
        odds = odds,
        edgePts = edge,
        outcome = outcome,
        startUtc = sequence++,
    )

    @Test
    fun aLosingSideBecomesAProposalForTheOtherSide() {
        val rows = buildList {
            repeat(16) { add(observation(Side.UNDER, Outcome.LOSS)) }
            repeat(4) { add(observation(Side.UNDER, Outcome.WIN)) }
            repeat(20) { add(observation(Side.OVER, Outcome.WIN)) }
        }
        val report = LossInsights.analyze(rows)
        assertEquals(40, report.considered)
        assertTrue("a losing side should be named", report.findings.any { it.title.contains("Unders") })
        assertTrue(
            "the losing side should propose the opposite filter",
            report.proposals.any { it.sideFilter == SideFilter.OVER_ONLY },
        )
        assertTrue(report.findings.all { it.sample >= LossInsights.MIN_SAMPLE })
    }

    @Test
    fun lowEdgeLeansRaiseTheEdgeFloor() {
        val markets = listOf(Markets.POINTS, Markets.REBOUNDS, Markets.ASSISTS, Markets.THREES, Markets.PRA)
        val rows = buildList {
            repeat(20) { i ->
                add(
                    observation(
                        side = if (i % 2 == 0) Side.OVER else Side.UNDER,
                        outcome = Outcome.LOSS,
                        market = markets[i % markets.size],
                        line = if (i % 2 == 0) 20.5 else 21.0,
                        edge = 0.4,
                    ),
                )
            }
        }
        val report = LossInsights.analyze(rows, StrategyParams(minEdgePts = 1.0))
        assertTrue(
            "a low-edge bucket that loses should test a higher floor",
            report.proposals.any { it.minEdgePts >= 2.0 },
        )
        assertTrue(
            "every segment needs the minimum sample",
            report.findings.all { it.sample >= LossInsights.MIN_SAMPLE },
        )
    }

    @Test
    fun longPricesThatLoseCapThePriceBand() {
        val rows = buildList {
            repeat(20) { i ->
                add(
                    observation(
                        side = if (i % 2 == 0) Side.OVER else Side.UNDER,
                        outcome = if (i < 15) Outcome.LOSS else Outcome.WIN,
                        market = if (i % 2 == 0) Markets.POINTS else Markets.REBOUNDS,
                        line = if (i % 2 == 0) 20.5 else 21.0,
                        edge = 3.5,
                        odds = 150,
                    ),
                )
            }
        }
        val report = LossInsights.analyze(rows)
        assertTrue(
            "long-price losses should cut the top of the price band",
            report.proposals.any { it.maxPrice <= 2.20 },
        )
    }

    @Test
    fun aWinningMarketBecomesItsOwnNarrowFormula() {
        val rows = buildList {
            repeat(21) { i ->
                add(
                    observation(
                        side = if (i % 2 == 0) Side.OVER else Side.UNDER,
                        outcome = Outcome.WIN,
                        line = if (i % 2 == 0) 20.5 else 21.0,
                    ),
                )
            }
        }
        val report = LossInsights.analyze(rows)
        assertTrue(
            "a paying market should be proposed as a market-specific rule",
            report.proposals.any { it.markets == listOf(Markets.POINTS) },
        )
        assertTrue(report.patterns.isNotEmpty())
        assertFalse(report.findings.any { it.kind == InsightKind.LEAK })
    }

    @Test
    fun aSideAndPricePatternIsProposedAsANewFormula() {
        val rows = buildList {
            repeat(30) {
                add(observation(Side.OVER, Outcome.WIN, line = 20.5, odds = -130))
            }
        }
        val report = LossInsights.analyze(rows)
        assertTrue("the combination should be named", report.findings.any { it.kind == InsightKind.PATTERN })
        assertTrue(
            "the pattern should narrow both the side and the price band",
            report.proposals.any {
                it.sideFilter == SideFilter.OVER_ONLY && it.maxPrice <= 1.80
            },
        )
    }

    @Test
    fun aSmallSampleMakesNoClaim() {
        val rows = (1..5).map { observation(Side.OVER, Outcome.LOSS) }
        val report = LossInsights.analyze(rows)
        assertEquals(5, report.considered)
        assertTrue(report.findings.isEmpty())
        assertTrue(report.proposals.isEmpty())
        assertTrue(report.note.contains("5"))
    }

    @Test
    fun pushesAndVoidsAreNotLosses() {
        val rows = buildList {
            repeat(5) { add(observation(Side.OVER, Outcome.WIN)) }
            repeat(5) { add(observation(Side.OVER, Outcome.LOSS)) }
            repeat(10) { add(observation(Side.OVER, Outcome.PUSH)) }
            repeat(10) { add(observation(Side.OVER, Outcome.VOID)) }
        }
        val report = LossInsights.analyze(rows)
        assertEquals(10, report.considered)
        assertTrue(report.findings.isEmpty())
    }

    @Test
    fun priceAndMarketChangesHaveTheirOwnIdsAndRoundTrip() {
        val params = StrategyParams(
            minEdgePts = 3.0,
            sideFilter = SideFilter.UNDER_ONLY,
            minPrice = 1.80,
            maxPrice = 2.20,
            excludeMarkets = listOf(Markets.POINTS),
        )
        val restored = StrategyParams.parse(params.paramsText)
        assertEquals(params, restored)
        assertTrue(params.id.contains("-p180-220"))
        assertTrue(params.id.contains("-xpo"))
        assertNotEquals(StrategyParams(minEdgePts = 3.0).id, params.id)
        assertEquals("e3.0-both", StrategyParams(minEdgePts = 3.0).id)
    }

    @Test
    fun theSimulatedBenchCarriesMarketsSoAnExclusionCanBeTested() {
        val season = Season.generate(seed = Season.BASE_SEED).take(3_000)
        assertTrue("every simulated prop needs a market", season.all { it.market.isNotBlank() })
        val broad = SandboxEngine.run(season, StrategyParams(minEdgePts = 0.0))
        val excluded = SandboxEngine.run(
            season,
            StrategyParams(minEdgePts = 0.0, excludeMarkets = listOf(Markets.POINTS)),
        )
        assertTrue("the exclusion must change the bench", excluded.stats.bets < broad.stats.bets)
    }

    @Test
    fun anExcludedMarketIsNotBetButTheSamePropIsOtherwiseEligible() {
        val prop = SimProp(
            id = "p",
            playerName = "Player",
            line = 20.5,
            overOdds = -110,
            underOdds = -110,
            pOver = 0.60,
            pPush = 0.0,
            pUnder = 0.40,
            startUtc = 0L,
            actual = 25.0,
            synthetic = true,
            market = Markets.POINTS,
        )
        val open = StrategyParams(minEdgePts = 0.0)
        assertNotNull(SandboxEngine.decide(prop, open))
        val excluded = open.copy(excludeMarkets = listOf(Markets.POINTS))
        assertNull(SandboxEngine.decide(prop, excluded))
        // A different market is untouched by the exclusion.
        assertEquals(
            SandboxEngine.decide(prop, open)?.side,
            SandboxEngine.decide(prop, open.copy(excludeMarkets = listOf(Markets.REBOUNDS)))?.side,
        )
    }
}
