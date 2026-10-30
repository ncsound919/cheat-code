package com.propslab

import com.propslab.data.AvailabilityStatus
import com.propslab.fantasy.FantasyCall
import com.propslab.fantasy.FantasyDesk
import com.propslab.fantasy.FantasyInjury
import com.propslab.fantasy.FantasyLine
import com.propslab.fantasy.FantasySport
import com.propslab.fantasy.FantasyWatchlist
import com.propslab.feed.Names
import org.junit.Assert.*
import org.junit.Test

class FantasyDeskTest {

    private fun line(points: Double, playedUtc: Long = 0L, name: String = "Player One") = FantasyLine(
        playerKey = Names.key(name),
        playerName = name,
        team = "BOS",
        playedUtc = playedUtc,
        fantasyPoints = points,
        statLine = "line",
        role = "",
    )

    private fun injury(status: String, name: String = "Player One") = FantasyInjury(
        playerName = name,
        playerKey = Names.key(name),
        team = "BOS",
        status = status,
        note = "left knee",
        observedUtc = 1_000L,
    )

    @Test fun basketballScoringUsesTheStatedWeights() {
        assertEquals(51.6, FantasyDesk.basketballScore(24, 8, 6, 3), 1e-9)
        assertEquals(0.0, FantasyDesk.basketballScore(0, 0, 0, 0), 1e-9)
    }

    @Test fun footballScoringIsHalfPprAndPenalisesTurnovers() {
        val score = FantasyDesk.footballScore(
            passYards = 250,
            passTouchdowns = 2,
            interceptions = 1,
            rushYards = 40,
            rushTouchdowns = 1,
            receptions = 5,
            receivingYards = 60,
            receivingTouchdowns = 1,
        )
        assertEquals(40.5, score, 1e-9)
        assertEquals(20.0, FantasyDesk.footballTouchdownPoints(2, 1, 1), 1e-9)
    }

    @Test fun aNegativeFootballLineIsFlooredAtZero() {
        assertEquals(0.0, FantasyDesk.footballScore(0, 0, 3, 0, 0, 0, 0, 0), 1e-9)
    }

    @Test fun theProjectionWeightsTheNewestGameMost() {
        val player = FantasyDesk.rank(
            FantasySport.BASKETBALL,
            listOf(
                line(40.0, 5_000L),
                line(20.0, 4_000L),
                line(10.0, 3_000L),
                line(10.0, 2_000L),
            ),
        ).single()
        assertEquals(20.0, player.average, 1e-9)
        assertEquals(69.5 / 3.1, player.projection, 1e-9)
        assertTrue("newest game pulls the projection up", player.projection > player.average)
        assertEquals(13.333333333, player.trend, 1e-6)
    }

    @Test fun aHotPlayerIsAStartCall() {
        val player = FantasyDesk.rank(
            FantasySport.BASKETBALL,
            listOf(
                line(40.0, 4_000L),
                line(30.0, 3_000L),
                line(20.0, 2_000L),
                line(10.0, 1_000L),
            ),
        ).single()
        assertEquals(FantasyCall.START, FantasyDesk.advise(player).call)
        assertTrue(FantasyDesk.advise(player).headline.contains("Trending up"))
    }

    @Test fun aColdPlayerIsABenchCall() {
        val player = FantasyDesk.rank(
            FantasySport.BASKETBALL,
            listOf(
                line(8.0, 4_000L),
                line(9.0, 3_000L),
                line(30.0, 2_000L),
                line(32.0, 1_000L),
            ),
        ).single()
        assertEquals(FantasyCall.SIT, FantasyDesk.advise(player).call)
    }

    @Test fun anOutDesignationBeatsAnyForm() {
        val player = FantasyDesk.rank(
            FantasySport.BASKETBALL,
            listOf(line(40.0, 4_000L), line(38.0, 3_000L), line(36.0, 2_000L)),
            listOf(injury(AvailabilityStatus.OUT)),
        ).single()
        val advice = FantasyDesk.advise(player)
        assertEquals(FantasyCall.OUT, advice.call)
        assertTrue(advice.headline.contains("Ruled out"))
        assertTrue(advice.tips.any { it.contains("ruled out") })
    }

    @Test fun aQuestionableDesignationIsAWatchNotAGuess() {
        val player = FantasyDesk.rank(
            FantasySport.BASKETBALL,
            listOf(line(40.0, 4_000L), line(38.0, 3_000L), line(36.0, 2_000L)),
            listOf(injury(AvailabilityStatus.QUESTIONABLE)),
        ).single()
        val advice = FantasyDesk.advise(player)
        assertEquals(FantasyCall.WATCH, advice.call)
        assertTrue(advice.headline.contains("check before lock"))
        assertTrue(advice.tips.any { it.contains("will not guess") })
    }

    @Test fun aThinSampleSaysItIsThin() {
        val player = FantasyDesk.rank(FantasySport.BASKETBALL, listOf(line(30.0))).single()
        val advice = FantasyDesk.advise(player)
        assertEquals(FantasyCall.WATCH, advice.call)
        assertTrue(advice.tips.any { it.contains("One game is a sample") })
    }

    @Test fun rankGroupsLinesAndAttachesTheLatestDesignation() {
        val players = FantasyDesk.rank(
            FantasySport.FOOTBALL,
            listOf(
                line(10.0, 1_000L, "Same Player"),
                line(20.0, 2_000L, "Same Player"),
                line(30.0, 3_000L, "Other Player"),
            ),
            listOf(
                FantasyInjury("Same Player", Names.key("Same Player"), "DAL", AvailabilityStatus.OUT, "old", 1L),
                FantasyInjury("Same Player", Names.key("Same Player"), "DAL", AvailabilityStatus.PROBABLE, "new", 9L),
            ),
        )
        val same = players.first { it.name == "Same Player" }
        assertEquals(2, same.games)
        assertEquals(AvailabilityStatus.PROBABLE, same.injury?.status)
        assertEquals(30.0, players.first().projection, 1e-9)
    }

    @Test fun theDeskSplitsTrackedFromTrendingAndKeepsTheWire() {
        val watch = FantasyWatchlist.key(FantasySport.BASKETBALL, Names.key("Tracked Guy"))
        val ui = FantasyDesk.build(
            sport = FantasySport.BASKETBALL,
            lines = listOf(
                line(40.0, 3_000L, "Tracked Guy"),
                line(20.0, 3_000L, "Other Guy"),
            ),
            injuries = listOf(injury(AvailabilityStatus.OUT, "Tracked Guy")),
            watchlist = setOf(watch),
            fetchedUtc = 5_000L,
        )
        assertEquals(1, ui.tracked.size)
        assertEquals("Tracked Guy", ui.tracked.single().name)
        assertEquals(1, ui.trending.size)
        assertEquals(1, ui.injuries.size)
        assertTrue(ui.lineupTips.any { it.contains("form ranking, not your league") })
    }

    @Test fun anEmptyDeskExplainsTheReadRatherThanInventingPlayers() {
        val ui = FantasyDesk.build(
            FantasySport.FOOTBALL, emptyList(), emptyList(), emptySet(), 0L,
        )
        assertFalse(ui.hasData)
        assertTrue(ui.lineupTips.single().contains("No finished games"))
    }

    @Test fun theWatchlistRoundTripsAndRejectsJunk() {
        val a = FantasyWatchlist.key(FantasySport.BASKETBALL, "jaylen brown")
        val b = FantasyWatchlist.key(FantasySport.FOOTBALL, "ceedee lamb")
        val encoded = FantasyWatchlist.encode(setOf(b, a))
        assertEquals(setOf(a, b), FantasyWatchlist.decode(encoded))
        assertEquals(setOf(a, b), FantasyWatchlist.decode("junk|$a|$b|"))
        assertTrue(FantasyWatchlist.decode(null).isEmpty())
        assertTrue(FantasyWatchlist.toggle(setOf(a), a).isEmpty())
        assertEquals(setOf(a, b), FantasyWatchlist.toggle(setOf(a), b))
        assertEquals(FantasySport.FOOTBALL, FantasyWatchlist.sportOf(b))
    }
}
