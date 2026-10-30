package com.propslab

import com.propslab.domain.*
import com.propslab.inference.*
import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    @Test fun americanOdds() {
        assertEquals(1.909090, Odds.americanToDecimal(-110), 1e-5)
        assertEquals(2.5, Odds.americanToDecimal(150), 1e-9)
        assertEquals(0.5, Odds.noVigOver(-110, -110), 1e-9)
        assertThrows(IllegalArgumentException::class.java) { Odds.americanToDecimal(50) }
    }

    @Test fun settlementAndPush() {
        assertEquals(Outcome.PUSH, Settlement.outcome(Side.OVER, 25.0, 25.0))
        assertEquals(Outcome.WIN, Settlement.outcome(Side.OVER, 24.5, 25.0))
        assertEquals(Outcome.LOSS, Settlement.outcome(Side.OVER, 24.5, 24.0))
        assertEquals(Outcome.WIN, Settlement.outcome(Side.UNDER, 24.5, 24.0))
        assertEquals(0.909090, Settlement.profit(Outcome.WIN, 1.0, -110), 1e-5)
        assertEquals(-2.0, Settlement.profit(Outcome.LOSS, 2.0, -110), 1e-9)
        assertEquals(0.0, Settlement.profit(Outcome.PUSH, 2.0, -110), 1e-9)
    }

    @Test fun probabilitiesAreValidated() {
        assertThrows(IllegalArgumentException::class.java) { OutcomeProbabilities(0.6, 0.0, 0.5) }
        assertThrows(IllegalArgumentException::class.java) { OutcomeProbabilities(Double.NaN, 0.0, 1.0) }
        assertEquals(0.5, OutcomeProbabilities(0.45, 0.1, 0.45).overGivenNoPush, 1e-9)
    }

    @Test fun gateRejectsBadInputs() {
        val now = 100 * PredictionGate.HOUR_MS
        val ok = PredictionGate.evaluate(24.5, now - 1, now - 1, now, true, "player_points")
        assertTrue(ok.isEmpty())
        // An unvalidated market is the only way to reach every rejection at once. The gate
        // produces five reasons; the two feed-only reasons are never produced here.
        val bad = PredictionGate.evaluate(null, now - 7 * PredictionGate.HOUR_MS, now - 30 * PredictionGate.HOUR_MS, now, false, "player_steals")
        assertEquals(
            setOf(
                RejectReason.MISSING_LINE, RejectReason.STALE_QUOTE, RejectReason.STALE_FEATURES,
                RejectReason.PLAYER_UNMATCHED, RejectReason.OUT_OF_SCOPE,
            ),
            bad.toSet(),
        )
    }

    @Test fun theGatePricedMarketsMatchTheModel() {
        val now = 100 * PredictionGate.HOUR_MS
        for (market in Markets.VALIDATED) {
            assertTrue(market, PredictionGate.evaluate(20.5, now - 1, now - 1, now, true, market).isEmpty())
        }
        assertEquals(
            listOf(RejectReason.OUT_OF_SCOPE),
            PredictionGate.evaluate(20.5, now - 1, now - 1, now, true, "player_steals"),
        )
    }

    @Test fun voidSettlement() {
        assertEquals(Outcome.VOID, Settlement.outcome(Side.OVER, 24.5, 25.0, voided = true))
        assertEquals(Outcome.VOID, Settlement.outcome(Side.UNDER, 24.5, 24.0, voided = true))
        assertEquals(0.0, Settlement.profit(Outcome.VOID, 2.0, -110), 1e-9)
        // voided takes precedence over a push
        assertEquals(Outcome.VOID, Settlement.outcome(Side.OVER, 25.0, 25.0, voided = true))
    }

    @Test fun logisticScorerParityAndGuards() {
        val b = LogisticBundle(1, listOf("a", "b"), doubleArrayOf(10.0, 0.0), doubleArrayOf(2.0, 1.0), doubleArrayOf(0.5, -0.25), 0.1, 1.0, 0.0)
        val s = LogisticBundleScorer(b)
        // z = 0.1 + 0.5*(12-10)/2 - 0.25*(2-0)/1 = 0.1 ; p = sigmoid(0.1)
        val r = s.score(ScoreRequest(24.5, mapOf("a" to 12.0, "b" to 2.0), 1)) as ScoreResult.Scored
        assertEquals(0.52497918747894, r.probabilities.over, 1e-9)
        assertTrue(s.score(ScoreRequest(25.0, mapOf("a" to 1.0, "b" to 1.0), 1)) is ScoreResult.Unavailable)
        assertTrue(s.score(ScoreRequest(24.5, mapOf("a" to 1.0), 1)) is ScoreResult.Unavailable)
        assertTrue(s.score(ScoreRequest(24.5, mapOf("a" to 1.0, "b" to 1.0), 2)) is ScoreResult.Unavailable)
    }

    @Test fun gbmScorerParityAndGuards() {
        val nodes = listOf(
            GbmNode(leaf = false, feature = 0, threshold = 0.0, missingGoLeft = true, left = 1, right = 2),
            GbmNode(leaf = true, value = -0.5),
            GbmNode(leaf = true, value = 0.5),
        )
        val iso = GbmIsotonic(doubleArrayOf(0.2, 0.8), doubleArrayOf(0.25, 0.75))
        val bundle = GbmBundle(
            featureSchemaVersion = 1,
            features = listOf("a", "b"),
            folds = listOf(GbmFold(baseline = 0.0, trees = listOf(GbmTree(nodes)), isotonic = iso)),
        )
        val s = GbmBundleScorer(bundle)
        val r = s.score(ScoreRequest(24.5, mapOf("a" to 0.5, "b" to 0.0), 1)) as ScoreResult.Scored
        assertTrue(r.probabilities.over > 0.6 && r.probabilities.over < 0.7)
        assertTrue(s.score(ScoreRequest(25.0, mapOf("a" to 0.5, "b" to 0.0), 1)) is ScoreResult.Unavailable)
        assertTrue(s.score(ScoreRequest(24.5, mapOf("a" to 0.5, "b" to 0.0), 2)) is ScoreResult.Unavailable)
        assertTrue(s.score(ScoreRequest(24.5, mapOf("a" to 0.5), 1)) is ScoreResult.Unavailable)
    }
}
