package com.propslab

import com.propslab.domain.Odds
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.PredictionGate
import com.propslab.domain.Pricing
import com.propslab.domain.RejectReason
import com.propslab.domain.Staking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

/**
 * Table-driven checks for the formulas that create prices, edges and stakes.
 *
 * These are invariants, not memorised numbers where possible: any change that
 * breaks an identity below is a real defect, not a fixture that needs updating.
 */
class FormulaTest {

    private val validPrices = listOf(-1000, -500, -250, -200, -150, -125, -110, -105, 100, 105, 110, 120, 150, 200, 300, 500, 1000)

    // ---------------------------------------------------------------- pricing

    @Test fun americanDecimalAndImpliedRoundTrip() {
        for (price in validPrices) {
            val decimal = Odds.americanToDecimal(price)
            assertTrue("decimal > 1 for $price", decimal > 1.0)
            val implied = Odds.impliedProbability(price)
            assertTrue("implied in (0,1) for $price", implied > 0.0 && implied < 1.0)
            assertEquals("1/decimal for $price", implied, 1.0 / decimal, 1e-15)
            assertEquals("fair price round trip for $price", price.toDouble(), Pricing.fairAmerican(implied).toDouble(), 1.0)
        }
    }

    @Test fun noVigIsAProbabilityAndSymmetric() {
        for (over in validPrices) {
            for (under in validPrices) {
                val pOver = Odds.noVigOver(over, under)
                val pUnder = Odds.noVigOver(under, over)
                assertTrue("pOver in (0,1): $over/$under", pOver > 0.0 && pOver < 1.0)
                assertEquals("no-vig complements: $over/$under", 1.0, pOver + pUnder, 1e-12)
                assertEquals("no-vig symmetry: $over/$under", Pricing.edgePoints(pOver, pUnder), -Pricing.edgePoints(pUnder, pOver), 1e-12)
            }
        }
    }

    @Test fun holdIdentityAndBalancedBook() {
        assertEquals("hold(-110,-110)", 1.0 - 2.0 * (100.0 / 210.0), Pricing.hold(-110, -110), 1e-12)
        for (over in validPrices) {
            for (under in validPrices) {
                val expected = Odds.impliedProbability(over) + Odds.impliedProbability(under) - 1.0
                assertEquals("hold $over/$under", expected, Pricing.hold(over, under), 1e-12)
            }
        }
        // A normal two-way book charges something. An actual arbitrage would be negative.
        assertTrue("normal book hold is positive", Pricing.hold(-110, -110) > 0.0)
        assertTrue("arbitrage is representable", Pricing.hold(150, 200) < 0.0)
    }

    @Test fun edgeIsSignedModelMinusMarket() {
        val cases = listOf(
            Triple(0.55, 0.50, 5.0),
            Triple(0.50, 0.55, -5.0),
            Triple(0.50, 0.50, 0.0),
            Triple(0.61, 0.5238, 8.62),
        )
        for ((model, market, expected) in cases) {
            assertEquals("edge $model vs $market", expected, Pricing.edgePoints(model, market), 1e-9)
        }
        for (model in 1..99) {
            for (market in 1..99) {
                val m = model / 100.0
                val q = market / 100.0
                assertEquals("antisymmetric $m/$q", -Pricing.edgePoints(m, q), Pricing.edgePoints(q, m), 1e-12)
            }
        }
    }

    @Test fun fairPriceMakesEvZero() {
        val probabilities = (5..95 step 5).map { it / 100.0 }
        val pushes = listOf(0.0, 0.05, 0.12)
        var cases = 0
        for (p in probabilities) {
            for (push in pushes) {
                if (p + push >= 0.999) continue
                val fair = Pricing.fairAmerican(p, push)
                assertTrue("fair price is valid American: $p/$push -> $fair", fair >= 100 || fair <= -100)
                val ev = Staking.expectedValue(p, push, fair)
                assertTrue("EV at fair is zero: p=$p push=$push fair=$fair ev=$ev", abs(ev) <= 0.02)
                cases++
            }
        }
        assertTrue("expected coverage", cases >= 40)
    }

    @Test fun fairPriceIncludesPushMass() {
        val noPush = Pricing.fairAmerican(0.5, 0.0)
        val withPush = Pricing.fairAmerican(0.5, 0.10)
        assertEquals("no-push fair is even money", 100, noPush)
        assertTrue("push mass raises the break-even price", withPush < noPush)
        // With 10% push the break-even decimal is 1.8, i.e. -125.
        assertEquals(-125, withPush)
    }

    @Test fun fairPriceRejectsImpossibleDistributions() {
        assertThrows<IllegalArgumentException> { Pricing.fairAmerican(0.0, 0.0) }
        assertThrows<IllegalArgumentException> { Pricing.fairAmerican(0.99, 0.01) }
        assertThrows<IllegalArgumentException> { Pricing.fairAmerican(-0.1, 0.0) }
    }

    @Test fun edgeValidatesProbabilityInputs() {
        assertThrows<IllegalArgumentException> { Pricing.edgePoints(1.1, 0.5) }
        assertThrows<IllegalArgumentException> { Pricing.edgePoints(Double.NaN, 0.5) }
        assertThrows<IllegalArgumentException> { Pricing.edgePoints(0.5, -0.01) }
    }

    // ---------------------------------------------------------------- staking

    @Test fun expectedValueMatchesOutcomeSum() {
        val p = 0.56
        val push = 0.04
        val pLoss = 1.0 - p - push
        for (price in validPrices) {
            val expected = p * (Odds.americanToDecimal(price) - 1.0) - pLoss
            assertEquals("EV $price", expected, Staking.expectedValue(p, push, price), 1e-12)
        }
        assertEquals("stake scales EV", Staking.expectedValue(p, push, -110, 2.0), 2.0 * Staking.expectedValue(p, push, -110), 1e-12)
    }

    @Test fun expectedValueSignTracksThePrice() {
        for (p in listOf(0.45, 0.55, 0.6, 0.7)) {
            val fair = Pricing.fairAmerican(p)
            // +10 improves both a positive and a negative American price; -10 worsens it.
            val above = Staking.expectedValue(p, 0.0, fair + 10)
            val below = Staking.expectedValue(p, 0.0, fair - 10)
            assertTrue("price better than fair is +EV at p=$p fair=$fair", above > 0.0)
            assertTrue("price worse than fair is -EV at p=$p fair=$fair", below < 0.0)
        }
    }

    @Test fun kellyIsZeroAtFairAndNegativeBelow() {
        for (p in (20..80 step 4).map { it / 100.0 }) {
            for (push in listOf(0.0, 0.05, 0.12)) {
                if (p + push >= 0.99) continue
                val fair = Pricing.fairAmerican(p, push)
                val atFair = Staking.kellyFraction(p, push, fair)
                assertTrue("Kelly near zero at fair: p=$p push=$push fair=$fair f=$atFair", abs(atFair) < 0.02)
                // Stay outside the invalid (-100, 100) American band when moving away from fair.
                // From a short positive price the next worse valid quote is just past -100.
                val worsePrice = if (fair > 0) {
                    val candidate = fair - 20
                    if (candidate >= 100) candidate else -101
                } else {
                    fair - 20
                }
                val worse = Staking.kellyFraction(p, push, worsePrice)
                assertTrue("Kelly negative below fair: p=$p push=$push price=$worsePrice f=$worse", worse < 0.0)
            }
        }
    }

    @Test fun kellyMatchesNumericGrowthMaximum() {
        val grid = (0..995).map { it / 1000.0 } // f in [0, 0.995]
        val cases = listOf(
            Triple(0.55, 0.0, -110),
            Triple(0.60, 0.0, -110),
            Triple(0.70, 0.0, 120),
            Triple(0.55, 0.08, -110),
            Triple(0.62, 0.10, -125),
            Triple(0.75, 0.05, 150),
        )
        for ((p, push, price) in cases) {
            val fStar = Staking.kellyFraction(p, push, price)
            assertTrue("case should have a positive stake: p=$p push=$push price=$price f=$fStar", fStar > 0.0)
            val decimal = Odds.americanToDecimal(price)
            val b = decimal - 1.0
            val q = 1.0 - p - push
            fun growth(f: Double): Double =
                if (1.0 - f <= 0.0) Double.NEGATIVE_INFINITY
                else p * kotlin.math.ln(1.0 + b * f) + q * kotlin.math.ln(1.0 - f)
            val numeric = grid.maxByOrNull { growth(it) }!!
            assertTrue(
                "closed form vs numeric at p=$p push=$push price=$price: $fStar vs $numeric",
                abs(numeric - fStar) <= 0.002,
            )
            assertTrue("growth improves over not betting", growth(fStar) > growth(0.0))
        }
    }

    @Test fun kellyHandlesPushAndBoundaries() {
        // No loss state means no price is bad enough to avoid; the all-in fraction is 1.
        assertEquals(1.0, Staking.kellyFraction(0.9, 0.1, -110), 1e-12)
        assertEquals(1.0, Staking.kellyFraction(0.5, 0.5, -110), 1e-12)
        assertEquals(Staking.kellyFraction(0.5, 0.0, -110), Staking.kellyFraction(0.5, -110), 1e-12)
        assertTrue("no edge at a vigged even-money price", Staking.kellyFraction(0.5, -110) < 0.0)
        assertThrows<IllegalArgumentException> { Staking.kellyFraction(0.7, 0.4, -110) }
        assertThrows<IllegalArgumentException> { Staking.kellyFraction(0.5, 0.0, 50) }
    }

    @Test fun probabilityObjectRejectsInvalidTriples() {
        assertThrows<IllegalArgumentException> { OutcomeProbabilities(0.6, 0.0, 0.5) }
        assertThrows<IllegalArgumentException> { OutcomeProbabilities(-0.01, 0.01, 1.0) }
        assertThrows<IllegalArgumentException> { OutcomeProbabilities(1.1, 0.0, 0.0) }
        assertThrows<IllegalArgumentException> { OutcomeProbabilities(Double.NaN, 0.0, 1.0) }
        assertThrows<IllegalArgumentException> { OutcomeProbabilities(Double.POSITIVE_INFINITY, 0.0, 1.0) }
        val near = OutcomeProbabilities(0.5, 1e-7, 0.5 - 1e-7)
        assertEquals(0.5 / (1.0 - 1e-7), near.overGivenNoPush, 1e-12)
    }

    @Test fun gateUsesStrictAgeBoundaries() {
        val now = 1_000 * PredictionGate.HOUR_MS
        val atLimit = PredictionGate.evaluate(24.5, now - 6 * PredictionGate.HOUR_MS, now - 24 * PredictionGate.HOUR_MS, now, true, "player_points")
        assertTrue("exactly at both limits is still usable", atLimit.isEmpty())

        val quoteOld = PredictionGate.evaluate(24.5, now - 6 * PredictionGate.HOUR_MS - 1, now - 1, now, true, "player_points")
        assertEquals(listOf(RejectReason.STALE_QUOTE), quoteOld)

        val featuresOld = PredictionGate.evaluate(24.5, now - 1, now - 24 * PredictionGate.HOUR_MS - 1, now, true, "player_points")
        assertEquals(listOf(RejectReason.STALE_FEATURES), featuresOld)

        assertNotNull(PredictionGate.evaluate(null, now - 7 * PredictionGate.HOUR_MS, now - 30 * PredictionGate.HOUR_MS, now, false, "player_assists"))
    }

    private inline fun <reified T : Throwable> assertThrows(noinline block: () -> Unit) {
        try {
            block()
            fail("Expected ${T::class.java.simpleName}")
        } catch (t: Throwable) {
            if (t !is T) throw t
        }
    }
}
