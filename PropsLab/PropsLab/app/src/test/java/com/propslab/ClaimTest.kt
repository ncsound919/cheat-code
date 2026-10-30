package com.propslab

import com.propslab.claim.ClaimBench
import com.propslab.claim.ClaimCompiler
import com.propslab.claim.ClaimLexicon
import com.propslab.claim.DeepTest
import com.propslab.compute.KaggleClient
import com.propslab.domain.Markets
import com.propslab.feed.EspnApi
import com.propslab.sim.SandboxEngine
import com.propslab.sim.Season
import com.propslab.sim.SideFilter
import com.propslab.sim.SimProp
import com.propslab.sim.StrategyParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The plain-language lane, pinned at every boundary that could turn a sentence into
 * something the app did not intend to run.
 */
class ClaimTest {

    private val board = ClaimCompiler.Board(
        playerNames = listOf("LeBron James", "Jayson Tatum", "Nikola Jokic"),
        marketByPlayer = mapOf("LeBron James" to listOf(Markets.POINTS)),
    )

    // ------------------------------------------------------------------ the compiler

    @Test
    fun `a defensive claim becomes an unders lens`() {
        val formula = ClaimCompiler.compile("I think the defensive line will make the difference this game", board)
        assertEquals("defense", formula.factorTag)
        assertEquals(SideFilter.UNDER_ONLY, formula.lens.side)
        assertTrue(formula.lens.tiltPts < 0.0)
        assertEquals(3.0, formula.lens.minEdgePts, 1e-9)
        assertTrue(formula.inGameTerms.isNotEmpty())
        assertTrue(formula.falsifier.contains("already in the price"))
        assertTrue(formula.id.startsWith("claim-"))
    }

    @Test
    fun `a blowout sentence leads with the blowout, not the absence offered as its reason`() {
        val formula = ClaimCompiler.compile(
            "lebrons team will win by alot bc x isnt playing",
            board,
        )
        assertEquals("blowout", formula.factorTag)
        assertEquals(SideFilter.UNDER_ONLY, formula.lens.side)
        assertEquals(listOf(Markets.POINTS), formula.lens.markets)
        assertEquals("LeBron James", formula.subject)
    }

    @Test
    fun `an absence claim leans overs`() {
        val formula = ClaimCompiler.compile("he feasts because their big man is out tonight", board)
        assertEquals("absence", formula.factorTag)
        assertEquals(SideFilter.OVER_ONLY, formula.lens.side)
        assertTrue(formula.lens.tiltPts > 0.0)
    }

    @Test
    fun `an explicit market overrides the factor's open market`() {
        val formula = ClaimCompiler.compile("their defense is too good, take the rebounds under", board)
        assertEquals("defense", formula.factorTag)
        assertEquals(listOf(Markets.REBOUNDS), formula.lens.markets)
        assertEquals(SideFilter.UNDER_ONLY, formula.lens.side)
    }

    @Test
    fun `an explicit side wins over the factor default`() {
        val formula = ClaimCompiler.compile("their defense is good but I still like the over", board)
        assertEquals("defense", formula.factorTag)
        assertEquals(SideFilter.OVER_ONLY, formula.lens.side)
    }

    @Test
    fun `an unrecognised sentence compiles to the generic price filter`() {
        val formula = ClaimCompiler.compile("the vibes are off in this one", board)
        assertEquals("generic", formula.factorTag)
        assertEquals(0.0, formula.lens.tiltPts, 1e-9)
        assertEquals(SideFilter.BOTH, formula.lens.side)
        assertTrue(ClaimCompiler.plainSummary(formula).contains("price filter"))
    }

    @Test
    fun `every factor in the lexicon is reachable by at least one of its own triggers`() {
        for (factor in ClaimLexicon.factors) {
            val compiled = ClaimCompiler.compile(factor.triggers.first(), board)
            assertEquals("trigger '${factor.triggers.first()}' did not select ${factor.tag}", factor.tag, compiled.factorTag)
        }
    }

    @Test
    fun `the same sentence compiles to the same id`() {
        val a = ClaimCompiler.compile("their defense decides it", board)
        val b = ClaimCompiler.compile("their defense decides it", board)
        assertEquals(a.id, b.id)
        assertEquals(a.lens, b.lens)
    }

    // ------------------------------------------------------------ the params contract

    @Test
    fun `a plain grid id is unchanged by the tilt feature`() {
        val plain = StrategyParams(3.0, SideFilter.BOTH)
        assertEquals("e3.0-both", plain.id)
        assertTrue(plain.paramsText.contains("tilt=0.0"))
    }

    @Test
    fun `a tilted lens has its own id and round-trips through paramsText`() {
        val lens = StrategyParams(3.0, SideFilter.UNDER_ONLY, 1.6, 2.4, -3.0, listOf(Markets.POINTS))
        val parsed = StrategyParams.parse(lens.paramsText)
        assertNotNull(parsed)
        assertEquals(lens, parsed)
        assertTrue(lens.id.contains("-t-3.0"))
        assertTrue(lens.id.contains("-mpo"))
    }

    @Test
    fun `a row written before tilts existed reads back as the plain grid`() {
        val old = "edge=4.0;side=over;price=1.50..2.60"
        val parsed = StrategyParams.parse(old)
        assertNotNull(parsed)
        assertEquals(0.0, parsed!!.tiltPts, 1e-9)
        assertEquals(SideFilter.OVER_ONLY, parsed.sideFilter)
        assertEquals(emptyList<String>(), parsed.markets)
    }

    @Test
    fun `a malformed params string is rejected rather than guessed`() {
        assertNull(StrategyParams.parse("side=over"))
        assertNull(StrategyParams.parse(""))
    }

    // -------------------------------------------------------------- the tilt itself

    private fun flatProp(market: String = Markets.POINTS) = SimProp(
        id = "p1",
        playerName = "Test Player",
        line = 20.5,
        overOdds = -110,
        underOdds = -110,
        pOver = 0.5,
        pPush = 0.0,
        pUnder = 0.5,
        startUtc = 0L,
        actual = 25.0,
        synthetic = false,
        market = market,
    )

    @Test
    fun `a neutral lens does not bet a prop the book and the app agree on`() {
        assertNull(SandboxEngine.decide(flatProp(), StrategyParams(3.0)))
    }

    @Test
    fun `a positive tilt opens the over, a negative tilt opens the under`() {
        val over = SandboxEngine.decide(flatProp(), StrategyParams(3.0, SideFilter.OVER_ONLY, tiltPts = 4.0))
        val under = SandboxEngine.decide(flatProp(), StrategyParams(3.0, SideFilter.UNDER_ONLY, tiltPts = -4.0))
        assertNotNull(over)
        assertNotNull(under)
        assertEquals(com.propslab.domain.Side.OVER, over!!.side)
        assertEquals(com.propslab.domain.Side.UNDER, under!!.side)
        assertEquals(4.0, over.edgePts, 1e-9)
        assertEquals(4.0, under.edgePts, 1e-9)
    }

    @Test
    fun `a lens that names a market refuses a different one`() {
        val lens = StrategyParams(3.0, SideFilter.BOTH, tiltPts = 4.0, markets = listOf(Markets.REBOUNDS))
        assertNull(SandboxEngine.decide(flatProp(Markets.POINTS), lens))
        assertNotNull(SandboxEngine.decide(flatProp(Markets.REBOUNDS), lens))
    }

    @Test
    fun `the simulated season has no market, so a market lens still runs on it`() {
        val lens = StrategyParams(3.0, SideFilter.BOTH, tiltPts = 4.0, markets = listOf(Markets.REBOUNDS))
        assertNotNull(SandboxEngine.decide(flatProp(market = ""), lens))
    }

    // ------------------------------------------------------------------- the bench

    @Test
    fun `the bench runs every variant and reports a verdict in words`() {
        val formula = ClaimCompiler.compile("their defense decides this", board)
        val season = Season.generate(seed = 7L, days = 12, propsPerDay = 20, endUtc = 1_790_000_000_000L)
        val result = ClaimBench.run(formula, season)
        assertEquals(9, result.trials.size)
        assertTrue(result.seasonProps == season.size)
        assertTrue(result.plain.contains("invented props"))
        assertTrue(result.boardNote.isNotBlank())
        assertTrue(result.chosen.stats.bets >= 0)
        assertTrue(result.trials.any { it.params.id == result.chosen.params.id })
        if (result.chosen.passed) {
            val bestPassing = result.trials.filter { it.passed }.maxByOrNull { it.stats.profit }
            assertEquals(bestPassing?.params?.id, result.chosen.params.id)
        }
    }

    @Test
    fun `a passing claim is only allowed when its own gate passed`() {
        val formula = ClaimCompiler.compile("he feasts because their big man is out", board)
        val season = Season.generate(seed = 11L, days = 20, propsPerDay = 30, endUtc = 1_790_000_000_000L)
        val result = ClaimBench.run(formula, season)
        assertEquals(result.chosen.gate.allPassed, result.ready)
    }

    // ------------------------------------------------------------------ deep test

    @Test
    fun `the generated kernel embeds the lens and cannot be broken out of its JSON literal`() {
        val hostile = ClaimCompiler.compile("their defense decides it'); import os #", board)
        val script = DeepTest.script(hostile)
        assertFalse(script.contains("__LENS_JSON__"))
        assertTrue(script.contains("propslab_training.parquet"))
        assertTrue(script.contains("claim_result.json"))
        // The sentence lives inside one JSON literal; its quote is escaped, so the Python
        // string is never closed early and the injected statement stays inert text.
        assertTrue(script.contains("decides it\\'"))
        assertFalse(script.contains("decides it'); import os"))
    }

    @Test
    fun `a deep-test result is read into words, including its failures`() {
        val pass = DeepTest.parse(
            """{"claim":"x","factor":"defense","rows":665203,
               "factor_test":{"name":"opp_def100_trail","supported":true,"slope":0.1,"t":8.2},
               "replay":{"bets":10234,"roi":0.031,"t":2.9,"holdout_bets":2012,"holdout_roi":0.012,"brier":0.24},
               "verdict":"PASS","note":"ok"}""",
        )
        assertEquals("PASS", pass.verdict)
        assertEquals(665203, pass.rows)
        assertEquals(10234, pass.bets)
        assertTrue(pass.plain.contains("real player-games"))
        assertTrue(pass.plain.contains("held-out"))

        val fail = DeepTest.parse(
            """{"rows":665203,"factor":"absence",
               "factor_test":{"name":null,"supported":false,"t":null},
               "replay":{"bets":0,"roi":0.0,"t":0.0},
               "verdict":"UNTESTED","note":"The training dataset is points-only."}""",
        )
        assertEquals("UNTESTED", fail.verdict)
        // An untested claim now says why in the kernel's own words, so the reader sees the
        // reason (points-only dataset, contradicted direction) instead of a generic prefix.
        assertTrue(fail.plain.contains("points-only"))
    }

    @Test
    fun `an unreadable deep-test body does not throw`() {
        val outcome = DeepTest.parse("<html>not json</html>")
        assertEquals("UNTESTED", outcome.verdict)
        assertTrue(outcome.note.isNotBlank())
    }

    // ---------------------------------------------------------------- Kaggle plumbing

    @Test
    fun `a KGAT token is a bearer credential and a legacy key is basic auth`() {
        val client = KaggleClient()
        val token = client.authHeader(KaggleClient.Credentials("tap919", "KGAT_abc"))
        assertEquals("Bearer KGAT_abc", token)
        val basic = client.authHeader(KaggleClient.Credentials("tap919", "secret"))
        assertTrue(basic.startsWith("Basic "))
        assertFalse(basic.contains("KGAT"))
    }

    @Test
    fun `kernel status and output are parsed from the shapes the API returns`() {
        val client = KaggleClient()
        assertEquals(KaggleClient.State.COMPLETE, client.parseStatus("""{"status":"complete","failureMessage":""}""").state)
        assertEquals(KaggleClient.State.RUNNING, client.parseStatus("""{"status":"running"}""").state)
        assertEquals(KaggleClient.State.ERROR, client.parseStatus("""{"status":"error","failureMessage":"boom"}""").state)
        val files = client.parseOutput(
            """{"files":[{"fileName":"claim_result.json","url":"https://example.test/a.json"}],"log":"[]"}""",
        )
        assertEquals(1, files.size)
        assertEquals("claim_result.json", files[0].name)
    }

    @Test
    fun `the status and output urls put the ref on the query string`() {
        val client = KaggleClient()
        assertTrue(client.statusUrl("tap919/propslab-claim-lens").contains("user_name=tap919"))
        assertTrue(client.statusUrl("tap919/propslab-claim-lens").contains("kernel_slug=propslab-claim-lens"))
        assertTrue(client.outputUrl("/code/tap919/propslab-claim-lens").contains("kernel_slug=propslab-claim-lens"))
    }

    // ------------------------------------------------------------------ ESPN box scores

    private val scoreboard = """
        {"events":[{"id":"401909881","date":"2026-10-28T23:00Z","competitions":[{
          "status":{"type":{"completed":true}},
          "competitors":[
            {"homeAway":"home","team":{"abbreviation":"ATL","displayName":"Atlanta Hawks"}},
            {"homeAway":"away","team":{"abbreviation":"MIA","displayName":"Miami Heat"}}
          ]}]}]}
    """.trimIndent()

    private val summary = """
        {"header":{"competitions":[{"date":"2026-10-28T23:00Z"}]},
         "boxscore":{"players":[{"team":{"abbreviation":"MIA"},"statistics":[{
           "names":["MIN","PTS","FG","3PT","FT","REB","AST","TO","STL","BLK","OREB","DREB","PF","+/-"],
           "athletes":[
             {"athlete":{"displayName":"Giannis Antetokounmpo"},"didNotPlay":false,
              "stats":["31","24","11-18","0-2","2-5","18","3","2","1","1","5","13","2","-6"]},
             {"athlete":{"displayName":"Bench Guy"},"didNotPlay":true,
              "stats":["0","0","0-0","0-0","0-0","0","0","0","0","0","0","0","0","0"]}
           ]}]}]}}
    """.trimIndent()

    @Test
    fun `espn games and box scores are read from the real payload shape`() {
        val games = EspnApi.parseGames(scoreboard, "2026-10-28")
        assertEquals(1, games.size)
        assertEquals("401909881", games[0].id)
        assertTrue(games[0].isFinal)
        assertEquals("Atlanta Hawks", games[0].homeName)
        assertEquals("MIA", games[0].awayAbbr)

        val lines = EspnApi.parseBoxScore(summary, "401909881", 1L)
        assertEquals(1, lines.size)
        val line = lines[0]
        assertEquals("Giannis Antetokounmpo", line.playerName)
        assertEquals(24, line.points)
        assertEquals(18, line.rebounds)
        assertEquals(3, line.assists)
        assertEquals(0, line.threes)
        assertEquals(31, line.minutes)
        assertEquals("espn-401909881-${com.propslab.feed.Names.key("Giannis Antetokounmpo")}", line.statId)
        assertEquals(com.propslab.feed.OddsApi.isoUtc("2026-10-28T23:00Z"), line.playedUtc)
    }

    @Test
    fun `a scheduled game is not treated as final`() {
        val scheduled = scoreboard.replace("\"completed\":true", "\"completed\":false")
        assertFalse(EspnApi.parseGames(scheduled, "2026-10-29")[0].isFinal)
    }

    @Test
    fun `a box score with no stats block returns nothing rather than inventing rows`() {
        assertTrue(EspnApi.parseBoxScore("""{"boxscore":{}}""", "x", 1L).isEmpty())
        assertTrue(EspnApi.parseGames("not json", "2026-10-28").isEmpty())
    }
}
