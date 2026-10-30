package com.propslab

import com.propslab.data.ParlayCandidate
import com.propslab.data.ParlayTickets
import com.propslab.domain.ParlayBook
import com.propslab.domain.ParlayLeg
import org.junit.Assert.*
import org.junit.Test

class ParlayTest {

    @Test fun chanceAndPriceMultiply() {
        val ticket = ParlayBook.build(
            listOf(
                ParlayLeg(odds = 100, pWin = 0.5),
                ParlayLeg(odds = 100, pWin = 0.5),
                ParlayLeg(odds = 100, pWin = 0.4),
            ),
        )!!
        assertEquals(0.1, ticket.probability, 1e-9)
        assertEquals(8.0, ticket.decimal, 1e-9)
        assertEquals(8.0, ticket.payout, 1e-9)
        assertEquals(7.0, ticket.profit, 1e-9)
        // 0.1 * 7 - 0.9 = -0.2 per unit: a fair-priced parlay with no edge
        assertEquals(-0.2, ticket.ev, 1e-9)
    }

    @Test fun aPushIsConditionedOutOfTheLeg() {
        val leg = ParlayLeg(odds = -110, pWin = 0.36, pPush = 0.1)
        assertEquals(0.4, leg.winNoPush, 1e-9)
        val ticket = ParlayBook.build(listOf(leg))!!
        assertEquals(0.4, ticket.probability, 1e-9)
    }

    @Test fun anotherLegAlwaysLowersTheChance() {
        val one = ParlayBook.build(listOf(ParlayLeg(-110, 0.6)))!!
        val two = ParlayBook.build(listOf(ParlayLeg(-110, 0.6), ParlayLeg(-110, 0.55)))!!
        assertTrue(two.probability < one.probability)
        assertTrue(two.decimal > one.decimal)
    }

    @Test fun aGameLegIsReservedOnEveryTicket() {
        val game = ParlayCandidate(
            key = "g:atl", gameId = "g-atl", subject = null, label = "ATL moneyline",
            detail = "CLE at ATL", odds = -118, pWin = 0.59, pPush = 0.0, edgePts = 1.0,
        )
        val second = game.copy(key = "g:bos", gameId = "g-bos", label = "BOS spread", edgePts = 0.9)
        val props = (1..12).map { i ->
            ParlayCandidate(
                key = "p$i", gameId = "pg$i", subject = "Player $i", label = "Player $i over 20.5 Points",
                detail = "game $i", odds = -110, pWin = 0.62, pPush = 0.0, edgePts = 8.0 - i * 0.2,
            )
        }
        val short = ParlayTickets.build(listOf(game, second) + props, 3)!!
        assertEquals(3, short.legs.size)
        assertTrue(short.legs.any { it.candidate.subject == null })
        assertEquals(1, short.gameLegs)
        val long = ParlayTickets.build(listOf(game, second) + props, 10)!!
        assertEquals(2, long.gameLegs)
        assertTrue(long.probability < 0.1)
    }

    @Test fun emptyOrImpossibleTicketsAreRefused() {
        assertNull(ParlayBook.build(emptyList()))
        assertNull(ParlayBook.build(listOf(ParlayLeg(-110, 0.0))))
        assertNull(ParlayBook.build(listOf(ParlayLeg(-110, 0.5)), stake = 0.0))
        assertNull(ParlayBook.build(listOf(ParlayLeg(-110, 0.5), ParlayLeg(-110, 0.0))))
    }
}
