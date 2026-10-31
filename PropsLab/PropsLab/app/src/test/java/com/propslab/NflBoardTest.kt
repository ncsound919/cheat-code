package com.propslab

import com.propslab.domain.Markets
import com.propslab.domain.Sports
import com.propslab.feed.EspnApi
import com.propslab.feed.FeedBoard
import com.propslab.feed.GameLogModel
import com.propslab.feed.Names
import com.propslab.feed.OddsApi
import com.propslab.feed.RawQuote
import com.propslab.feed.Teams
import com.propslab.ocr.SlipParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The football half of the board.
 *
 * The app was built around one league, so every assumption that used to be safe — a line
 * under 80, a slip number under 200, a club code from the NBA table — is checked here
 * against the second league rather than being left to a live sync to discover.
 */
class NflBoardTest {

    @Test
    fun `football markets are validated and labelled`() {
        for (market in Markets.FOOTBALL) {
            assertTrue("$market should be priceable", market in Markets.VALIDATED)
            assertTrue("$market should read as football", Markets.isFootball(market))
            assertNotNull("$market needs a label", Markets.label(market))
        }
        assertEquals("Passing yards", Markets.label(Markets.PASS_YARDS))
        assertEquals("Rushing yards", Markets.label(Markets.RUSH_YARDS))
        assertEquals("Receiving yards", Markets.label(Markets.RECEIVING_YARDS))
        assertEquals("Receptions", Markets.label(Markets.RECEPTIONS))
        assertFalse(Markets.isFootball(Markets.POINTS))
        assertEquals(Sports.FOOTBALL, Sports.ofMarket(Markets.RECEPTIONS))
        assertEquals(Sports.BASKETBALL, Sports.ofMarket(Markets.POINTS))
    }

    @Test
    fun `a football line reads the right column and nothing else`() {
        assertEquals(279.0, GameLogModel.footballValue(279, 24, 0, 0, Markets.PASS_YARDS)!!, 0.0)
        assertEquals(24.0, GameLogModel.footballValue(279, 24, 0, 0, Markets.RUSH_YARDS)!!, 0.0)
        assertEquals(
            88.0,
            GameLogModel.footballValue(0, 0, 88, 7, Markets.RECEIVING_YARDS)!!,
            0.0,
        )
        assertEquals(7.0, GameLogModel.footballValue(0, 0, 88, 7, Markets.RECEPTIONS)!!, 0.0)
        assertNull(GameLogModel.footballValue(279, 24, 88, 7, Markets.POINTS))

        // The dispatcher is what settlement uses: the same row must answer for both sports.
        assertEquals(
            279.0,
            GameLogModel.anyStatValue(31, 12, 9, 2, 279, 24, 88, 7, Markets.PASS_YARDS)!!,
            0.0,
        )
        assertEquals(
            52.0,
            GameLogModel.anyStatValue(31, 12, 9, 2, 279, 24, 88, 7, Markets.PRA)!!,
            0.0,
        )
    }

    @Test
    fun `line bands follow the market instead of one basketball band`() {
        val passing = quote(Markets.PASS_YARDS, 279.5)
        assertTrue("a 279.5 passing line is ordinary", FeedBoard.acceptable(passing))
        assertFalse("a 600-yard passing line is an artefact", FeedBoard.acceptable(passing.copy(line = 600.0)))
        assertTrue("a 12.5 reception line is ordinary", FeedBoard.acceptable(quote(Markets.RECEPTIONS, 12.5)))
        assertFalse("a 30-reception line is an artefact", FeedBoard.acceptable(quote(Markets.RECEPTIONS, 30.0)))
        assertTrue("the basketball band is unchanged", FeedBoard.acceptable(quote(Markets.POINTS, 26.5)))
        assertFalse(FeedBoard.acceptable(quote(Markets.POINTS, 120.0)))
    }

    @Test
    fun `NFL club names resolve to the codes the marks use`() {
        assertEquals("KC", Teams.abbreviation("Kansas City Chiefs"))
        assertEquals("SF", Teams.abbreviation("San Francisco 49ers"))
        assertEquals("BUF", Teams.abbreviation("Buffalo Bills"))
        assertEquals("LAC", Teams.abbreviation("Los Angeles Chargers"))
        assertTrue(Teams.sameTeam("Kansas City Chiefs", "KC"))
        assertFalse(Teams.sameTeam("Kansas City Chiefs", "Buffalo Bills"))
        // The two leagues share city names. Los Angeles resolves to two different codes,
        // while Chicago genuinely shares CHI; the sport on the game row is what separates
        // those two, which is why every result join compares it.
        assertFalse(Teams.sameTeam("Los Angeles Lakers", "Los Angeles Chargers"))
        assertEquals("CHI", Teams.abbreviation("Chicago Bulls"))
        assertEquals("CHI", Teams.abbreviation("Chicago Bears"))
    }

    @Test
    fun `the NFL box score keeps passing rushing and receiving apart`() {
        val body = """
            {
              "header": {"competitions": [{"date": "2026-10-29T00:15:00Z"}]},
              "boxscore": {"players": [
                {"team": {"abbreviation": "KC"}, "statistics": [
                  {"name": "passing", "athletes": [
                    {"athlete": {"displayName": "Patrick Mahomes", "id": "3139477"},
                     "stats": ["22/31", "279", "9.0", "2", "1", "118.5"]}
                  ]},
                  {"name": "rushing", "athletes": [
                    {"athlete": {"displayName": "Patrick Mahomes", "id": "3139477"},
                     "stats": ["3", "24", "8.0", "0"]}
                  ]},
                  {"name": "receiving", "athletes": [
                    {"athlete": {"displayName": "Travis Kelce", "id": "15847"},
                     "stats": ["7", "88", "12.6", "1"]}
                  ]}
                ]},
                {"team": {"abbreviation": "BUF"}, "statistics": [
                  {"name": "passing", "athletes": [
                    {"athlete": {"displayName": "Josh Allen", "id": "3918298"},
                     "stats": ["19/29", "256", "8.8", "2", "0", "112.0"]}
                  ]}
                ]}
              ]}
            }
        """.trimIndent()

        val lines = EspnApi.parseFootballBoxScore(body, "401772111", 1_000L)
        assertEquals(3, lines.size)
        val mahomes = lines.single { it.playerName == "Patrick Mahomes" }
        assertEquals(279, mahomes.passYards)
        assertEquals(24, mahomes.rushYards)
        assertEquals(0, mahomes.receivingYards)
        assertEquals(0, mahomes.receptions)
        assertEquals(Sports.FOOTBALL, mahomes.sport)

        val kelce = lines.single { it.playerName == "Travis Kelce" }
        assertEquals(7, kelce.receptions)
        assertEquals(88, kelce.receivingYards)
        assertEquals(0, kelce.passYards)

        // The same parser must not read a football row as a basketball one.
        assertEquals(0, kelce.points)
        assertEquals(0, kelce.minutes)
    }

    @Test
    fun `a football slip line is read, including a passing line above 200`() {
        val picks = SlipParser.parse(
            "Patrick Mahomes\nOver 279.5 Passing Yards -115\nCeeDee Lamb\nOver 76.5 Receiving Yards -110",
        )
        assertEquals(2, picks.size)
        val mahomes = picks.first()
        assertEquals("Patrick Mahomes", mahomes.playerName)
        assertEquals(Markets.PASS_YARDS, mahomes.market)
        assertEquals(279.5, mahomes.line, 0.0)
        assertEquals(-115, mahomes.odds)
        assertEquals(Markets.RECEIVING_YARDS, picks[1].market)
    }

    @Test
    fun `the provider is asked for the league it is reading`() {
        val nfl = OddsApi.eventsUrl("key", OddsApi.SPORT_NFL)
        assertTrue(nfl.contains("americanfootball_nfl"))
        assertTrue(OddsApi.eventsUrl("key").contains("basketball_nba"))
        val odds = OddsApi.eventOddsUrl("evt", "key", listOf(Markets.PASS_YARDS), listOf("draftkings"), OddsApi.SPORT_NFL)
        assertTrue(odds.contains("americanfootball_nfl"))
        assertTrue(odds.contains("player_pass_yds"))
        assertEquals(Markets.PASS_YARDS, OddsApi.appMarket("player_pass_yds"))
        assertEquals(Markets.RECEIVING_YARDS, OddsApi.appMarket("player_reception_yds"))
    }

    private fun quote(market: String, line: Double) = RawQuote(
        eventId = "evt",
        playerName = "Player",
        playerKey = Names.key("Player"),
        market = market,
        line = line,
        overPrice = -110,
        underPrice = -110,
        bookKey = "draftkings",
        bookTitle = "DraftKings",
        updatedUtc = 0L,
    )
}
