package com.propslab

import com.propslab.feed.Names
import com.propslab.feed.SportsDb
import com.propslab.feed.Teams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The schedule feed, without a socket.
 *
 * Every payload here was read from the live API on 2026-10-29 with the published free
 * key, then trimmed to the fields the parser consumes. That is the point: the shapes these
 * tests pin are what the provider actually sent, including the two habits that would
 * otherwise cost hours — an empty day answers `"events": null`, and the event stamp is UTC
 * with nothing on it to say so.
 */
class SportsDbTest {

    // Live payload: eventsday.php?d=2026-10-29&s=Basketball, free key, 2026-10-29.
    private val dayPayload = """
    {
      "events": [
        {
          "idEvent": "2487926",
          "strTimestamp": "2026-10-29T09:00:00",
          "strEvent": "Brisbane Bullets vs Cairns Taipans",
          "strSport": "Basketball",
          "strLeague": "Australian NBL",
          "strHomeTeam": "Brisbane Bullets",
          "strAwayTeam": "Cairns Taipans",
          "idHomeTeam": "136287",
          "idAwayTeam": "135395",
          "dateEvent": "2026-10-29",
          "strTime": "09:00:00",
          "strStatus": "FT",
          "strVenue": "Brisbane Entertainment Centre"
        },
        {
          "idEvent": "2551437",
          "strTimestamp": "2026-10-29T18:15:00",
          "strEvent": "Panathlitikos Sykeon vs ŽKK Brod na Savi",
          "strSport": "Basketball",
          "strLeague": "EuroCup Women",
          "strHomeTeam": "Panathlitikos Sykeon",
          "strAwayTeam": "ŽKK Brod na Savi",
          "idHomeTeam": "153192",
          "idAwayTeam": "156354",
          "dateEvent": "2026-10-29",
          "strTime": "18:15:00",
          "strStatus": "NS",
          "strVenue": ""
        },
        {
          "idEvent": "2551441",
          "strTimestamp": "2026-10-29T19:30:00",
          "strEvent": "BC Namur-Capitale vs CB Avenida Women",
          "strSport": "Basketball",
          "strLeague": "EuroCup Women",
          "strHomeTeam": "BC Namur-Capitale",
          "strAwayTeam": "CB Avenida Women",
          "idHomeTeam": "151542",
          "idAwayTeam": "144027",
          "dateEvent": "2026-10-29",
          "strTime": "19:30:00",
          "strStatus": "NS",
          "strVenue": ""
        }
      ]
    }
    """

    // Live payload: eventsnext.php?id=134867 (Los Angeles Lakers), same day.
    private val nbaPayload = """
    {
      "events": [
        {
          "idEvent": "2601732",
          "strTimestamp": "2026-10-29T02:30:00",
          "strEvent": "Los Angeles Clippers vs Los Angeles Lakers",
          "strSport": "Basketball",
          "strLeague": "NBA",
          "strHomeTeam": "Los Angeles Clippers",
          "strAwayTeam": "Los Angeles Lakers",
          "idHomeTeam": "134866",
          "idAwayTeam": "134867",
          "dateEvent": "2026-10-29",
          "strTime": "02:30:00",
          "strStatus": "FT",
          "strVenue": "Intuit Dome"
        }
      ]
    }
    """

    // Live payload: lookup_all_players.php?id=134867, trimmed to the parsed fields.
    private val rosterPayload = """
    {
      "player": [
        { "idPlayer": "34348470", "idTeam": "134867", "strPlayer": "Adou Thiero",
          "strTeam": "Los Angeles Lakers", "strPosition": "Small Forward", "strNumber": "1" },
        { "idPlayer": "34396518", "idTeam": "134867", "strPlayer": "Arthur Kaluma",
          "strTeam": "Los Angeles Lakers", "strPosition": "Forward", "strNumber": "47" },
        { "idPlayer": "34196189", "idTeam": "134867", "strPlayer": "Austin Reaves",
          "strTeam": "Los Angeles Lakers", "strPosition": "Shooting Guard", "strNumber": "15" },
        { "idPlayer": "34250402", "idTeam": "134867", "strPlayer": "Bronny James",
          "strTeam": "Los Angeles Lakers", "strPosition": "Point Guard", "strNumber": "9" },
        { "idPlayer": "34165366", "idTeam": "134867", "strPlayer": "Collin Sexton",
          "strTeam": "Los Angeles Lakers", "strPosition": "Point Guard", "strNumber": "10" }
      ]
    }
    """

    private val db = SportsDb()

    // ------------------------------------------------------------------ the schedule

    @Test
    fun dayQueryParsesEveryEventAndNoneOfThemAreNba() {
        val events = db.parseEvents(dayPayload)
        assertEquals(3, events.size)
        assertEquals("2487926", events[0].id)
        assertEquals("Brisbane Bullets", events[0].homeTeam)
        assertEquals("Cairns Taipans", events[0].awayTeam)
        assertEquals("Australian NBL", events[0].league)
        assertEquals("Brisbane Entertainment Centre", events[0].venue)
        assertTrue(events[0].finished)
        assertEquals("NS", events[1].status)
        assertTrue(!events[1].finished)

        // The free tier's day query answered with no NBA game at all, which is exactly why
        // the sync falls back to the season page instead of trusting one read.
        assertTrue(SportsDb.nba(events).isEmpty())
    }

    @Test
    fun nbaGameReadsItsStampAsUtcAndKeepsItsNames() {
        val events = SportsDb.nba(db.parseEvents(nbaPayload))
        assertEquals(1, events.size)
        val game = events[0]
        assertEquals("Los Angeles Clippers vs Los Angeles Lakers", "${game.homeTeam} vs ${game.awayTeam}")
        // 2026-10-29 02:30 UTC is 7:30pm the previous evening in Los Angeles: read as a
        // local stamp it would land nine hours late on the board.
        assertEquals(1793241000000L, game.utcMillis)
        assertEquals("NBA", game.league)
        assertEquals("Basketball", game.sport)
        assertEquals("Intuit Dome", game.venue)
        assertTrue(game.finished)
        assertEquals("LAC", Teams.abbreviation(game.homeTeam))
        assertEquals("LAL", Teams.abbreviation(game.awayTeam))
        assertTrue(Teams.sameTeam(game.awayTeam, "Los Angeles Lakers"))
    }

    @Test
    fun anEventWithOnlyADateAndTimeIsStillScheduled() {
        val body = """
        { "events": [ {
            "idEvent": "9", "strHomeTeam": "Denver Nuggets", "strAwayTeam": "Utah Jazz",
            "idHomeTeam": "1", "idAwayTeam": "2", "strLeague": "NBA", "strSport": "Basketball",
            "dateEvent": "2026-11-01", "strTime": "00:00:00", "strStatus": "NS"
        } ] }
        """
        val events = db.parseEvents(body)
        assertEquals(1, events.size)
        assertEquals(1793491200000L, events[0].utcMillis)
        assertTrue(!events[0].finished)
    }

    @Test
    fun anEventWithNoReadableTimeFallsOutsideEveryWindow() {
        val body = """
        { "events": [ {
            "idEvent": "9", "strHomeTeam": "Denver Nuggets", "strAwayTeam": "Utah Jazz",
            "strLeague": "NBA", "strStatus": "NS"
        } ] }
        """
        val events = db.parseEvents(body)
        assertEquals(1, events.size)
        assertEquals(0L, events[0].utcMillis)
        // A game that cannot be placed in time is not written, rather than landing in 1970.
        assertTrue(SportsDb.between(events, 1_793_000_000_000L, 1_794_000_000_000L).isEmpty())
    }

    @Test
    fun windowKeepsOrderAndNothingElse() {
        val events = db.parseEvents(dayPayload)
        val window = SportsDb.between(events, 1_793_200_000_000L, 1_793_310_000_000L)
        assertEquals(listOf("2487926", "2551437", "2551441"), window.map { it.id })
        // One call narrower keeps only the middle game, and the ends come back off-screen.
        val narrow = SportsDb.between(events, 1_793_290_000_000L, 1_793_300_000_000L)
        assertEquals(listOf("2551437"), narrow.map { it.id })
    }

    @Test
    fun emptyAndUnreadableBodiesYieldNoGamesRatherThanAnException() {
        assertTrue(db.parseEvents("""{ "events": null }""").isEmpty())
        assertTrue(db.parseEvents("{}").isEmpty())
        assertTrue(db.parseEvents("this is not json").isEmpty())
        assertTrue(db.parsePlayers("""{ "player": null }""").isEmpty())
        assertTrue(db.parsePlayers("not json either").isEmpty())
    }

    @Test
    fun eventslastAnswersUnderItsResultsKey() {
        val body = """
        { "results": [ {
            "idEvent": "2601720", "strTimestamp": "2026-10-28T03:00:00",
            "strHomeTeam": "Los Angeles Lakers", "strAwayTeam": "Portland Trail Blazers",
            "strLeague": "NBA", "strStatus": "FT"
        } ] }
        """
        val events = db.parseEvents(body)
        assertEquals(1, events.size)
        assertEquals("2601720", events[0].id)
        assertTrue(events[0].finished)
    }

    @Test
    fun aRowMissingItsTeamsIsDroppedInsteadOfHalfWritten() {
        val body = """
        { "events": [
            { "idEvent": "1", "strHomeTeam": "Boston Celtics", "strAwayTeam": "Miami Heat",
              "strLeague": "NBA", "strTimestamp": "2026-10-29T23:30:00" },
            { "idEvent": "2", "strHomeTeam": "", "strAwayTeam": "Miami Heat",
              "strLeague": "NBA", "strTimestamp": "2026-10-29T23:30:00" },
            { "idEvent": "", "strHomeTeam": "Denver Nuggets", "strAwayTeam": "Utah Jazz",
              "strLeague": "NBA", "strTimestamp": "2026-10-29T23:30:00" }
        ] }
        """
        assertEquals(listOf("1"), db.parseEvents(body).map { it.id })
    }

    // ---------------------------------------------------------------------- the roster

    @Test
    fun rosterParsesRealPlayersUnderStableIds() {
        val players = db.parsePlayers(rosterPayload)
        assertEquals(5, players.size)
        val reaves = players.first { it.name == "Austin Reaves" }
        assertEquals("34196189", reaves.id)
        assertEquals("134867", reaves.teamId)
        assertEquals("Los Angeles Lakers", reaves.teamName)
        assertEquals("Shooting Guard", reaves.position)

        // The player row the sync writes is keyed the same way the odds feed keys one, so
        // a roster import and a later price attach to the same person.
        assertEquals(Names.playerId("Austin Reaves"), Names.playerId("austin  reaves"))
        assertTrue(Names.playerId("Austin Reaves") != Names.playerId("Bronny James"))
        assertEquals("LAL", Teams.abbreviation(reaves.teamName))
    }

    @Test
    fun aPlayerWithoutANameIsDropped() {
        val body = """{ "player": [ { "idPlayer": "1", "strPlayer": "" }, { "idPlayer": "", "strPlayer": "Real Player" } ] }"""
        assertTrue(db.parsePlayers(body).isEmpty())
    }

    // ------------------------------------------------------------------- the URLs

    @Test
    fun urlBuildersMatchTheDocumentedShape() {
        val named = SportsDb(key = "test-key")
        assertEquals(
            "https://www.thesportsdb.com/api/v1/json/test-key/eventsday.php?d=2026-10-29&s=Basketball",
            named.dayUrl(LocalDate.parse("2026-10-29")),
        )
        assertEquals(
            "https://www.thesportsdb.com/api/v1/json/test-key/eventsseason.php?id=4387&s=2026-2027",
            named.seasonUrl(SportsDb.NBA_LEAGUE_ID, "2026-2027"),
        )
        assertEquals(
            "https://www.thesportsdb.com/api/v1/json/test-key/lookup_all_players.php?id=134867",
            named.playersUrl("134867"),
        )
        assertTrue(named.teamUrl("Los Angeles Lakers").endsWith("searchteams.php?t=Los+Angeles+Lakers"))
        // The free key is a constant, not a credential: it is in the code, not a setting.
        assertEquals("123", SportsDb.FREE_KEY)
        assertTrue(db.dayUrl(LocalDate.parse("2026-10-29")).contains("/123/"))
    }

    @Test
    fun seasonLabelCrossesTheCalendarYear() {
        assertEquals("2026-2027", SportsDb.seasonFor(LocalDate.parse("2026-10-29")))
        assertEquals("2025-2026", SportsDb.seasonFor(LocalDate.parse("2026-01-10")))
        assertEquals("2025-2026", SportsDb.seasonFor(LocalDate.parse("2026-06-15")))
    }
}
