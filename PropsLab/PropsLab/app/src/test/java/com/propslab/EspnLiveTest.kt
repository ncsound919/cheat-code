package com.propslab

import com.propslab.fantasy.FantasyFeedApi
import com.propslab.fantasy.FantasyGameLogApi
import com.propslab.fantasy.FantasySport
import com.propslab.fantasy.SportsNews
import com.propslab.feed.EspnApi
import com.propslab.feed.HttpUrlConnectionFetcher
import com.propslab.feed.safeGet
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The smoke test that talks to ESPN.
 *
 * It is opt-in because a unit suite must not fail when the network is down or a public
 * provider is having a bad morning. Run it deliberately:
 *
 *     PROPSLAB_LIVE_ESPN=1 gradle :app:testDebugUnitTest --tests com.propslab.EspnLiveTest
 *
 * The event ids are real finals from 2026-10-26, kept because a finished game stays readable
 * after the schedule has moved on. Every assertion exercises the parser the app ships, not a
 * mock: this is how the documented payload shapes are checked against today's service.
 */
class EspnLiveTest {

    private fun live() = assumeTrue(
        "Set PROPSLAB_LIVE_ESPN=1 to run the live ESPN smoke test.",
        System.getenv("PROPSLAB_LIVE_ESPN") == "1",
    )

    @Test fun theDocumentedNewsEndpointParses() = runBlocking {
        live()
        val fetcher = HttpUrlConnectionFetcher()
        val feed = SportsNews.fetch(
            fetcher,
            leagues = listOf(SportsNews.NFL, SportsNews.NBA, SportsNews.MLB, SportsNews.NHL),
        )
        assertTrue("at least one league should answer", feed.items.isNotEmpty())
        assertTrue(feed.items.any { it.headline.isNotBlank() })
        assertTrue(feed.items.any { it.url.startsWith("https://") })
    }

    @Test fun theDocumentedNflScoreboardAndSummaryParse() = runBlocking {
        live()
        val fetcher = HttpUrlConnectionFetcher()
        val board = fetcher.safeGet(FantasyFeedApi.nflScoreboardUrl("2026-10-26")).getOrThrow()
        assertTrue("NFL scoreboard HTTP ${board.status}", board.ok)
        val games = EspnApi.parseGames(board.body, "2026-10-26")
        assertTrue("NFL scoreboard should carry events", games.isNotEmpty())

        val summary = fetcher.safeGet(FantasyFeedApi.nflSummaryUrl("401873009")).getOrThrow()
        assertTrue("NFL summary HTTP ${summary.status}", summary.ok)
        val lines = FantasyFeedApi.parseNflBoxScore(summary.body, "401873009", System.currentTimeMillis())
        assertTrue("NFL summary should carry player lines", lines.isNotEmpty())
        val injuries = FantasyFeedApi.parseInjuries(summary.body)
        assertTrue("NFL summary should carry designations", injuries.isNotEmpty())
    }

    @Test fun theDocumentedAthleteGameLogParses() = runBlocking {
        live()
        val fetcher = HttpUrlConnectionFetcher()

        val nba = fetcher.safeGet(FantasyGameLogApi.url(FantasySport.BASKETBALL, "4066328")).getOrThrow()
        assertTrue("NBA gamelog HTTP ${nba.status}", nba.ok)
        val nbaLog = FantasyGameLogApi.parse(nba.body, FantasySport.BASKETBALL, "4066328", "Jarrett Allen")
        assertTrue("NBA gamelog should carry games", nbaLog.entries.isNotEmpty())
        assertNotNull("NBA gamelog should be scorable", nbaLog.entries.first().fantasyPoints)
        assertTrue("NBA gamelog should expose season splits", nbaLog.seasons.isNotEmpty())
        assertTrue("every NBA split should have a label", nbaLog.seasons.all { it.label.isNotBlank() })

        val nfl = fetcher.safeGet(FantasyGameLogApi.url(FantasySport.FOOTBALL, "2577417")).getOrThrow()
        assertTrue("NFL gamelog HTTP ${nfl.status}", nfl.ok)
        val nflLog = FantasyGameLogApi.parse(nfl.body, FantasySport.FOOTBALL, "2577417", "Dak Prescott")
        assertTrue("NFL gamelog should carry games", nflLog.entries.isNotEmpty())
        assertNotNull("NFL gamelog should be scorable", nflLog.entries.first().fantasyPoints)
        assertTrue(nflLog.entries.first().statLine.contains("pass"))
        assertTrue("NFL gamelog should expose season splits", nflLog.seasons.isNotEmpty())
        assertTrue(
            "the NFL default should be a regular-season split",
            nflLog.seasons.none { it.entries.isNotEmpty() } ||
                nflLog.season.contains("Regular Season", ignoreCase = true) ||
                nflLog.seasons.firstOrNull { it.name == nflLog.season }?.label == "Regular season",
        )
    }

    @Test fun theDocumentedNbaScoreboardAndSummaryParse() = runBlocking {
        live()
        val fetcher = HttpUrlConnectionFetcher()
        val board = fetcher.safeGet(EspnApi.scoreboardUrl("2026-10-26")).getOrThrow()
        assertTrue("NBA scoreboard HTTP ${board.status}", board.ok)
        val games = EspnApi.parseGames(board.body, "2026-10-26")
        assertTrue("NBA scoreboard should carry events", games.isNotEmpty())

        val summary = fetcher.safeGet(EspnApi.summaryUrl("401909893")).getOrThrow()
        assertTrue("NBA summary HTTP ${summary.status}", summary.ok)
        val lines = FantasyFeedApi.parseBasketballBoxScore(
            summary.body, "401909893", System.currentTimeMillis(),
        )
        assertTrue("NBA summary should carry player lines", lines.isNotEmpty())
        assertTrue(FantasyFeedApi.parseInjuries(summary.body).isNotEmpty())
    }
}
