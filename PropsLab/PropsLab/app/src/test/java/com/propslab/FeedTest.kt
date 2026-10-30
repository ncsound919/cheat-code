package com.propslab

import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.Side
import com.propslab.domain.Settlement
import com.propslab.feed.FeedBoard
import com.propslab.feed.GameLogModel
import com.propslab.feed.Names
import com.propslab.feed.OddsApi
import com.propslab.feed.RawQuote
import com.propslab.feed.ResultsApi
import com.propslab.feed.Teams
import com.propslab.inference.ScoreResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The feed, without a socket.
 *
 * Every parser here is fed the shape the provider documents, and every number the model
 * produces is checked against arithmetic that can be done by hand. The one thing these
 * tests cannot prove is that the endpoints answer, which is why the sync reports what it
 * actually got instead of assuming.
 */
class FeedTest {

    // ---------------------------------------------------------------------- joining

    @Test
    fun playerNamesJoinAcrossTwoProvidersThatShareNoId() {
        assertEquals("lebron james", Names.key("LeBron James"))
        assertEquals("nikola jokic", Names.key("Nikola Jokić"))
        assertEquals("luka doncic", Names.key("Luka Dončić"))
        assertEquals("marcus smart", Names.key("Marcus Smart Jr."))
        assertEquals("jaren jackson", Names.key("Jaren Jackson III"))
        assertEquals("og anunoby", Names.key("OG Anunoby"))
        assertEquals(Names.key("Stephen Curry "), Names.key("Stephen  Curry"))
    }

    @Test
    fun teamNamesMatchHoweverEachProviderSpellsThem() {
        assertTrue(Teams.sameTeam("Denver Nuggets", "Denver Nuggets"))
        assertTrue(Teams.sameTeam("LA Clippers", "Los Angeles Clippers"))
        assertTrue(Teams.sameTeam("Oklahoma City Thunder", "oklahoma city thunder"))
        assertTrue(!Teams.sameTeam("Denver Nuggets", "Boston Celtics"))
        assertEquals("LAL", Teams.abbreviation("Los Angeles Lakers"))
        assertEquals("NOP", Teams.abbreviation("New Orleans Pelicans"))
    }

    @Test
    fun everyPlayerOfTheSameNameMapsToOneId() {
        assertEquals(Names.playerId("Jayson Tatum"), Names.playerId("jayson  tatum"))
        assertTrue(Names.playerId("Jayson Tatum") != Names.playerId("Jaylen Brown"))
    }

    // ------------------------------------------------------------------- the prices

    private val eventOdds = """
    {
      "id": "evt-1",
      "sport_key": "basketball_nba",
      "commence_time": "2026-10-30T23:30:00Z",
      "home_team": "Denver Nuggets",
      "away_team": "Boston Celtics",
      "bookmakers": [
        {
          "key": "draftkings",
          "title": "DraftKings",
          "markets": [
            {
              "key": "player_points",
              "last_update": "2026-10-30T18:00:00Z",
              "outcomes": [
                { "name": "Over", "description": "Nikola Jokic", "price": -115, "point": 26.5 },
                { "name": "Under", "description": "Nikola Jokic", "price": -105, "point": 26.5 },
                { "name": "Over", "description": "Jayson Tatum", "price": -110, "point": 28.5 }
              ]
            },
            {
              "key": "player_points_rebounds_assists",
              "last_update": "2026-10-30T18:00:00Z",
              "outcomes": [
                { "name": "Over", "description": "Nikola Jokic", "price": -120, "point": 45.5 },
                { "name": "Under", "description": "Nikola Jokic", "price": 100, "point": 45.5 }
              ]
            },
            {
              "key": "player_steals",
              "last_update": "2026-10-30T18:00:00Z",
              "outcomes": [
                { "name": "Over", "description": "Nikola Jokic", "price": 120, "point": 1.5 },
                { "name": "Under", "description": "Nikola Jokic", "price": -140, "point": 1.5 }
              ]
            }
          ]
        },
        {
          "key": "prizepicks",
          "title": "PrizePicks",
          "markets": [
            {
              "key": "player_points",
              "last_update": "2026-10-30T18:05:00Z",
              "outcomes": [
                { "name": "Over", "description": "Nikola Jokic", "price": -105, "point": 26.5 },
                { "name": "Under", "description": "Nikola Jokic", "price": -115, "point": 26.5 }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()

    @Test
    fun oneSidedPropsAndUnpricedMarketsAreDropped() {
        val quotes = OddsApi.parseEventOdds(eventOdds, fallbackUtc = 1_900_000_000_000L)
        // Jokic points (two books), Jokic PRA (one). Tatum has no under, steals is not a market.
        assertEquals(3, quotes.size)
        assertTrue(quotes.none { it.playerKey == "jayson tatum" })
        assertTrue(quotes.none { it.market == "player_steals" })
    }

    @Test
    fun theProvidersPraKeyBecomesTheAppsMarket() {
        val quotes = OddsApi.parseEventOdds(eventOdds, fallbackUtc = 1_900_000_000_000L)
        val pra = quotes.single { it.market == Markets.PRA }
        assertEquals(45.5, pra.line, 0.0)
        assertEquals(-120, pra.overPrice)
        assertEquals("player_points_rebounds_assists", OddsApi.apiMarket(Markets.PRA))
    }

    @Test
    fun theQuoteTimeComesFromTheMarketNotTheClock() {
        val quotes = OddsApi.parseEventOdds(eventOdds, fallbackUtc = 1_900_000_000_000L)
        val draftkings = quotes.first { it.bookKey == "draftkings" }
        assertEquals(OddsApi.isoUtc("2026-10-30T18:00:00Z"), draftkings.updatedUtc)
        val prizepicks = quotes.first { it.bookKey == "prizepicks" }
        assertEquals(OddsApi.isoUtc("2026-10-30T18:05:00Z"), prizepicks.updatedUtc)
    }

    @Test
    fun eventsAreReadWithTheirTipOff() {
        val body = """
        [
          { "id": "evt-1", "home_team": "Denver Nuggets", "away_team": "Boston Celtics",
            "commence_time": "2026-10-30T23:30:00Z" },
          { "id": "", "home_team": "No Id", "away_team": "Nothing", "commence_time": "2026-10-30T23:30:00Z" }
        ]
        """.trimIndent()
        val events = OddsApi.parseEvents(body)
        assertEquals(1, events.size)
        assertEquals(OddsApi.isoUtc("2026-10-30T23:30:00Z"), events[0].commenceUtc)
    }

    private fun quote(book: String, over: Int, under: Int, line: Double = 26.5) = RawQuote(
        eventId = "evt-1",
        playerName = "Nikola Jokic",
        playerKey = "nikola jokic",
        market = Markets.POINTS,
        line = line,
        overPrice = over,
        underPrice = under,
        bookKey = book,
        bookTitle = book,
        updatedUtc = 1_900_000_000_000L,
    )

    @Test
    fun theCheapestHoldIsThePriceThatSurvives() {
        val board = FeedBoard.bestPrices(
            listOf(
                quote("draftkings", -110, -110),
                quote("prizepicks", -105, -115),
                quote("fanatics", -115, -115),
            ),
        )
        assertEquals(1, board.size)
        assertEquals("prizepicks", board.single().bookKey)
    }

    @Test
    fun aLineThatCannotBeTakenIsNotOnTheBoard() {
        val board = FeedBoard.bestPrices(
            listOf(
                quote("draftkings", -110, -110, line = 0.0),
                quote("draftkings", -110, -110, line = 1000.0),
                quote("draftkings", 50, -110),
                quote("draftkings", -110, -110, line = 26.5),
            ),
        )
        assertEquals(1, board.size)
        assertEquals(26.5, board.single().line, 0.0)
    }

    @Test
    fun theSamePlayerAtTwoLinesKeepsBothLines() {
        val board = FeedBoard.bestPrices(
            listOf(quote("draftkings", -110, -110, line = 26.5), quote("draftkings", -110, -110, line = 27.5)),
        )
        assertEquals(2, board.size)
    }

    // ------------------------------------------------------------------ box scores

    private val gamesPayload = """
    {
      "data": [
        {
          "id": 15907925, "date": "2026-10-28", "season": 2026,
          "status": "Final", "status_state": "final", "period": 4,
          "home_team": { "id": 6, "full_name": "Cleveland Cavaliers", "abbreviation": "CLE" },
          "visitor_team": { "id": 4, "full_name": "Charlotte Hornets", "abbreviation": "CHA" }
        },
        {
          "id": 15907926, "date": "2026-10-28", "season": 2026,
          "status": "7:30 pm", "status_state": "scheduled",
          "home_team": { "full_name": "Denver Nuggets", "abbreviation": "DEN" },
          "visitor_team": { "full_name": "Boston Celtics", "abbreviation": "BOS" }
        }
      ],
      "meta": { "next_cursor": null, "per_page": 100 }
    }
    """.trimIndent()

    private val statsPayload = """
    {
      "data": [
        {
          "id": 15531179, "min": "34", "pts": 31, "reb": 12, "ast": 9, "fg3m": 2,
          "player": { "id": 70, "first_name": "Nikola", "last_name": "Jokić" },
          "team": { "id": 8, "abbreviation": "DEN", "full_name": "Denver Nuggets" },
          "game": { "id": 15907927, "date": "2026-10-28", "status": "Final" }
        },
        {
          "id": 15531180, "min": "0", "pts": 0, "reb": 0, "ast": 0, "fg3m": 0,
          "player": { "id": 71, "first_name": "Bench", "last_name": "Rider" },
          "team": { "id": 8, "abbreviation": "DEN", "full_name": "Denver Nuggets" },
          "game": { "id": 15907927, "date": "2026-10-28", "status": "Final" }
        }
      ]
    }
    """.trimIndent()

    @Test
    fun gamesAreReadWithTheirStateAndTeams() {
        val games = ResultsApi.parseGames(gamesPayload)
        assertEquals(2, games.size)
        assertTrue(games[0].isFinal)
        assertTrue(!games[1].isFinal)
        assertEquals("Cleveland Cavaliers", games[0].homeName)
        assertEquals("Charlotte Hornets", games[0].awayName)
        assertEquals("15907925", games[0].id)
    }

    @Test
    fun aBoxScoreLineBecomesTheColumnsAPropIsWrittenOn() {
        val rows = ResultsApi.parseStats(statsPayload, fetchedUtc = 1_900_000_000_000L)
        assertEquals(2, rows.size)
        val jokic = rows.first()
        assertEquals("bdl:15531179", jokic.statId)
        assertEquals("nikola jokic", jokic.playerKey)
        assertEquals("DEN", jokic.teamAbbr)
        assertEquals(34, jokic.minutes)
        assertEquals(31.0, GameLogModel.statValue(31, 12, 9, 2, Markets.POINTS)!!, 0.0)
        assertEquals(12.0, GameLogModel.statValue(31, 12, 9, 2, Markets.REBOUNDS)!!, 0.0)
        assertEquals(9.0, GameLogModel.statValue(31, 12, 9, 2, Markets.ASSISTS)!!, 0.0)
        assertEquals(2.0, GameLogModel.statValue(31, 12, 9, 2, Markets.THREES)!!, 0.0)
        assertEquals(52.0, GameLogModel.statValue(31, 12, 9, 2, Markets.PRA)!!, 0.0)
        assertNull(GameLogModel.statValue(31, 12, 9, 2, "player_steals"))
    }

    // ----------------------------------------------------------------- the estimate

    @Test
    fun aVeryThinGameLogProducesNoEstimateAtAll() {
        // Below the hard floor there is nothing to estimate from, not even a shrunken one.
        val tooThin = GameLogModel.score(List(3) { 22.0 }, 21.5)
        assertTrue(tooThin is ScoreResult.Unavailable)
    }

    @Test
    fun aThinGameLogIsPulledTowardEvenOddsRatherThanUsedAtFullStrength() {
        // Early in a season eight games do not exist yet. Five is published, but the excess
        // over a coin flip is cut by 5/8, so a short heater cannot look like a long edge.
        val five = (GameLogModel.score(List(5) { 30.0 }, 24.5) as ScoreResult.Scored).probabilities
        val eight = (GameLogModel.score(List(8) { 30.0 }, 24.5) as ScoreResult.Scored).probabilities
        assertTrue(five.over > 0.5)
        assertTrue(five.over < eight.over)
        assertEquals(1.0, five.over + five.push + five.under, 1e-9)
    }

    @Test
    fun aHalfPointLineHasNoPushAndLeansWithTheMean() {
        val steady = List(12) { 30.0 }
        val scored = GameLogModel.score(steady, 24.5)
        assertTrue(scored is ScoreResult.Scored)
        val probs = (scored as ScoreResult.Scored).probabilities
        assertEquals(0.0, probs.push, 1e-9)
        assertEquals(1.0, probs.over + probs.push + probs.under, 1e-9)
        // Mean 30, spread floored at 0.30 × 30 = 9, so P(X > 24.5) ≈ Φ(0.611) ≈ 0.729.
        assertEquals(0.7294, probs.over, 0.004)
        assertTrue(probs.over > probs.under)
    }

    @Test
    fun anIntegerLineCarriesARealPushChance() {
        val steady = List(12) { 30.0 }
        val scored = GameLogModel.score(steady, 30.0)
        assertTrue(scored is ScoreResult.Scored)
        val probs = (scored as ScoreResult.Scored).probabilities
        assertEquals(1.0, probs.over + probs.push + probs.under, 1e-6)
        // P(29.5 < X < 30.5) at σ = 9 is about 4.4%.
        assertEquals(0.0443, probs.push, 0.004)
        assertEquals(probs.over, probs.under, 1e-6)
    }

    @Test
    fun theSpreadNeverCollapsesBelowTheFloor() {
        // Eleven identical games and one quiet one: variance is tiny, and the model still
        // refuses to become certain. Without the floor this would price at 99%+.
        val values = List(11) { 30.0 } + listOf(26.0)
        val probs = (GameLogModel.score(values, 20.5) as ScoreResult.Scored).probabilities
        assertTrue("a near-certain estimate means the spread collapsed: $probs", probs.over < 0.90)
    }

    @Test
    fun aLineAtZeroIsNotAProp() {
        assertTrue(GameLogModel.score(List(12) { 30.0 }, 0.0) is ScoreResult.Unavailable)
    }

    @Test
    fun theEstimateAndTheSettlementUseTheSameArithmetic() {
        // The model says over 24.5 is likely; a 31-point game settles it as a win, and a
        // 22-point game as a loss. Nothing between the two steps re-derives the line.
        val scored = GameLogModel.score(List(12) { 30.0 }, 24.5) as ScoreResult.Scored
        assertTrue(scored.probabilities.over > 0.5)
        assertEquals(Settlement.outcome(Side.OVER, 24.5, 31.0).name, "WIN")
        assertEquals(Settlement.outcome(Side.OVER, 24.5, 22.0).name, "LOSS")
        assertEquals(Settlement.outcome(Side.UNDER, 24.5, 22.0).name, "WIN")
    }

    // ------------------------------------------------------------------ market math

    @Test
    fun aNoVigPriceStillMatchesTheDomainLayer() {
        val quotes = OddsApi.parseEventOdds(eventOdds, fallbackUtc = 0L)
        val dk = quotes.first { it.bookKey == "draftkings" && it.market == Markets.POINTS }
        val noVig = Odds.noVigOver(dk.overPrice, dk.underPrice)
        assertEquals(0.5108, noVig, 0.001)
        val hold = FeedBoard.hold(dk.overPrice, dk.underPrice)
        assertNotNull(hold)
        assertTrue(abs(hold!!) < 0.05)
    }
}
