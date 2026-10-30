package com.propslab

import com.propslab.data.AvailabilityStatus
import com.propslab.fantasy.FantasyFeedApi
import com.propslab.fantasy.SportsNews
import com.propslab.feed.HttpResult
import com.propslab.feed.JsonFetcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FantasyFeedTest {

    private class FakeFetcher(private val responses: Map<String, HttpResult>) : JsonFetcher {
        override suspend fun get(url: String, headers: Map<String, String>): HttpResult =
            responses[url] ?: HttpResult(404, "", emptyMap())
    }

    // ------------------------------------------------------------------ news

    @Test fun newsIsReadWithItsLeagueSummaryAndLink() {
        val body = """
            {"header":"NFL News","articles":[
              {"id":50324081,"headline":"A quarterback returns",
               "description":"He was cleared   after a precautionary trip.",
               "published":"2026-10-30T02:39:58Z",
               "links":{"web":{"href":"https://www.espn.com/nfl/story/_/id/1"}}},
              {"id":2,"headline":"No link here","description":"","published":"not-a-date",
               "links":{}},
              {"id":3,"headline":"   ","description":"dropped"}
            ]}
        """.trimIndent()
        val items = SportsNews.parse(body, "NFL")
        assertEquals(2, items.size)
        val first = items.first()
        assertEquals("NFL", first.league)
        assertEquals("A quarterback returns", first.headline)
        assertEquals("He was cleared after a precautionary trip.", first.summary)
        assertEquals("https://www.espn.com/nfl/story/_/id/1", first.url)
        assertTrue(first.publishedUtc > 0L)
        assertTrue(items[1].url.isEmpty())
        assertEquals(0L, items[1].publishedUtc)
    }

    @Test fun newsParsingRefusesJunkInsteadOfCrashing() {
        assertTrue(SportsNews.parse("not json", "NFL").isEmpty())
        assertTrue(SportsNews.parse("""{"articles":[]}""", "NFL").isEmpty())
        assertEquals(0L, SportsNews.parseIso("yesterday"))
    }

    @Test fun aLeagueThatFailsDoesNotTakeTheOthersDown() = runBlocking {
        val nfl = SportsNews.url(SportsNews.NFL.path)
        val nba = SportsNews.url(SportsNews.NBA.path)
        val fetcher = FakeFetcher(
            mapOf(
                nfl to HttpResult(
                    200,
                    """{"articles":[{"id":1,"headline":"NFL story","description":"","published":"2026-10-30T02:00:00Z","links":{"web":{"href":"https://espn.com/1"}}}]}""",
                    emptyMap(),
                ),
                nba to HttpResult(500, "down", emptyMap()),
            ),
        )
        val feed = SportsNews.fetch(fetcher, leagues = listOf(SportsNews.NFL, SportsNews.NBA))
        assertEquals(1, feed.items.size)
        assertEquals("NFL", feed.items.single().league)
        assertTrue(feed.note.contains("No answer from NBA"))
        assertTrue(feed.note.contains("1 of 2 leagues read"))
    }

    // ------------------------------------------------------------------ football

    private val nflBody = """
        {"header":{"competitions":[{"date":"2026-10-26T20:00:00Z"}]},
         "boxscore":{"players":[
           {"team":{"abbreviation":"DAL"},"statistics":[
             {"name":"passing","athletes":[
               {"athlete":{"id":"2577417","displayName":"Dak Prescott"},"stats":["15/27","212","7.9","3","0","0-0","88.3","118.1"]},
               {"athlete":{"displayName":"Short Row"},"stats":["1","2"]}
             ]},
             {"name":"rushing","athletes":[
               {"athlete":{"id":"2577417","displayName":"Dak Prescott"},"stats":["3","12","4.0","1","10"]},
               {"athlete":{"id":"4361579","displayName":"Javonte Williams"},"stats":["25","155","6.2","1","34"]}
             ]},
             {"name":"receiving","athletes":[
               {"athlete":{"id":"4241372","displayName":"CeeDee Lamb"},"stats":["5","74","14.8","2","30","12"]}
             ]},
             {"name":"defensive","athletes":[
               {"athlete":{"displayName":"Micah Parsons"},"stats":["6","5","0","1"]}
             ]}
           ]}
         ]}}
    """.trimIndent()

    @Test fun nflLinesAreMergedByPlayerAndScored() {
        val lines = FantasyFeedApi.parseNflBoxScore(nflBody, "401873009", 777L)
        assertEquals(3, lines.size)
        val dak = lines.first { it.playerName == "Dak Prescott" }
        assertEquals("DAL", dak.team)
        assertEquals("2577417", dak.athleteId)
        assertEquals("QB", dak.role)
        assertEquals(27.68, dak.fantasyPoints, 1e-9)
        assertEquals(18.0, dak.touchdownPoints, 1e-9)
        assertTrue(dak.statLine.contains("3 pass TD"))
        assertTrue(dak.statLine.contains("1 rush TD"))
        assertEquals(SportsNews.parseIso("2026-10-26T20:00:00Z"), dak.playedUtc)

        val receiver = lines.first { it.playerName == "CeeDee Lamb" }
        assertEquals("Receiver", receiver.role)
        assertEquals(21.9, receiver.fantasyPoints, 1e-9)

        val runner = lines.first { it.playerName == "Javonte Williams" }
        assertEquals("RB", runner.role)
        assertEquals(21.5, runner.fantasyPoints, 1e-9)

        assertTrue(lines.none { it.playerName == "Short Row" })
        assertTrue(lines.none { it.playerName == "Micah Parsons" })
    }

    @Test fun nflParsingRefusesMalformedPayloads() {
        assertTrue(FantasyFeedApi.parseNflBoxScore("not json", "x", 1L).isEmpty())
        assertTrue(FantasyFeedApi.parseNflBoxScore("""{"boxscore":{"players":[]}}""", "x", 1L).isEmpty())
    }

    // ------------------------------------------------------------------ basketball

    @Test fun theBasketballParserFeedsTheSameFantasyScoring() {
        val body = """
            {"header":{"competitions":[{"date":"2026-10-26T23:00:00Z"}]},
             "boxscore":{"players":[
               {"team":{"abbreviation":"BOS"},"statistics":[
                 {"names":["MIN","PTS","REB","AST","3PT"],"athletes":[
                   {"athlete":{"id":"4065648","displayName":"Jayson Tatum"},
                    "stats":["36","24","8","6","3-7"]}
                 ]}
               ]}
             ]}}
        """.trimIndent()
        val lines = FantasyFeedApi.parseBasketballBoxScore(body, "g1", 123L)
        assertEquals(1, lines.size)
        val tatum = lines.single()
        assertEquals("Jayson Tatum", tatum.playerName)
        assertEquals(51.6, tatum.fantasyPoints, 1e-9)
        assertEquals("4065648", tatum.athleteId)
        assertEquals(36, tatum.minutes)
        assertTrue(tatum.statLine.contains("24 pts"))
    }

    // ------------------------------------------------------------------ injuries

    @Test fun theInjuryBlocksInAGameSummaryAreRead() {
        val body = """
            {"injuries":[
              {"team":{"abbreviation":"DAL"},"injuries":[
                {"status":"Questionable","date":"2026-10-30T01:23Z",
                 "shortComment":"Limited in Thursday's practice.",
                 "athlete":{"displayName":"Mack Wilson Sr."}},
                {"status":"Day-to-day","date":"2026-10-30T01:00Z",
                 "shortComment":"Day to day is read as questionable.",
                 "athlete":{"displayName":"Day To Day"}},
                {"status":"Limited","date":"2026-10-30T00:30Z",
                 "shortComment":"Not a designation this side understands.",
                 "athlete":{"displayName":"Unknown Status"}}
              ]},
              {"team":{"abbreviation":"PHI"},"injuries":[
                {"status":"Out","date":"2026-10-29T21:55Z",
                 "athlete":{"displayName":"Ruled Out"}}
              ]}
            ]}
        """.trimIndent()
        val rows = FantasyFeedApi.parseInjuries(body, nowUtc = 5_000L)
        assertEquals(3, rows.size)
        val questionable = rows.first { it.playerName == "Mack Wilson Sr." }
        assertEquals(AvailabilityStatus.QUESTIONABLE, questionable.status)
        assertEquals("DAL", questionable.team)
        assertEquals(SportsNews.parseIso("2026-10-30T01:23Z"), questionable.observedUtc)
        assertEquals(AvailabilityStatus.OUT, rows.first { it.playerName == "Ruled Out" }.status)
        assertEquals(AvailabilityStatus.QUESTIONABLE, rows.first { it.playerName == "Day To Day" }.status)
        assertTrue(rows.none { it.playerName == "Unknown Status" })
    }

    @Test fun malformedInjuryBlocksYieldNothing() {
        assertTrue(FantasyFeedApi.parseInjuries("not json", 1L).isEmpty())
        assertTrue(FantasyFeedApi.parseInjuries("""{"injuries":[]}""", 1L).isEmpty())
    }
}
