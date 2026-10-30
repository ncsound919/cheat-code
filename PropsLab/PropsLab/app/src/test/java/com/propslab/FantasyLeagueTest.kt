package com.propslab

import com.propslab.data.AvailabilityStatus
import com.propslab.fantasy.CaptureKind
import com.propslab.fantasy.FantasyAssets
import com.propslab.fantasy.FantasyCall
import com.propslab.fantasy.FantasyCaptureParser
import com.propslab.fantasy.FantasyInjury
import com.propslab.fantasy.FantasyLeagueDesk
import com.propslab.fantasy.FantasyLine
import com.propslab.fantasy.FantasySport
import com.propslab.fantasy.LeaguePlayer
import com.propslab.fantasy.ProTeams
import com.propslab.feed.Names
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FantasyLeagueTest {

    private val basketball = """
        ESPN | My Team | Week 8
        PG Stephen Curry GSW vs BOS 42.5
        SG Anthony Edwards MIN @ DEN 38.2
        G Mikal Bridges BKN 28.1
        F LeBron James LAL 41.0
        C Nikola Jokic DEN 55.4
        UTIL Jalen Brunson NYK 35.0
        Bench
        PG Derrick White BOS 24.5
        IR Lonzo Ball CHI
        Total 264.7
    """.trimIndent()

    private fun player(
        name: String,
        slot: String,
        projected: Double? = null,
        injury: FantasyInjury? = null,
        recent: com.propslab.fantasy.FantasyPlayer? = null,
        opponent: String = "",
    ) = LeaguePlayer(
        key = Names.key(name),
        name = name,
        slot = slot,
        proTeam = "BOS",
        opponent = opponent,
        projected = projected,
        injury = injury,
        recent = recent,
    )

    private fun recent(name: String, vararg points: Double): com.propslab.fantasy.FantasyPlayer =
        com.propslab.fantasy.FantasyDesk.rank(
            FantasySport.BASKETBALL,
            points.mapIndexed { index, value ->
                FantasyLine(
                    playerKey = Names.key(name),
                    playerName = name,
                    team = "BOS",
                    playedUtc = 10_000L - index * 1_000L,
                    fantasyPoints = value,
                    statLine = "line",
                    role = "G",
                )
            },
        ).single()

    @Test fun aBasketballLineupReadsSlotsNamesTeamsAndProjections() {
        val page = FantasyCaptureParser.parse(
            basketball, CaptureKind.MY_TEAM, FantasySport.BASKETBALL, "My Team",
        )
        assertEquals(8, page.players.size)
        assertEquals(8, page.week)
        assertEquals("ESPN", page.platform)
        val curry = page.players.first { it.name == "Stephen Curry" }
        assertEquals("PG", curry.slot)
        assertEquals("GSW", curry.proTeam)
        assertEquals("BOS", curry.opponent)
        assertEquals(42.5, curry.projected!!, 1e-9)
        val lonzo = page.players.first { it.name == "Lonzo Ball" }
        assertEquals("IR", lonzo.slot)
        assertNull(lonzo.projected)
        assertTrue("the bench row is kept separate", page.players.none { it.name == "Bench" })
        assertTrue("the total is not a player", page.players.none { it.name.startsWith("Total") })
    }

    @Test fun theProjectionAndSeasonAverageCanArriveInEitherOrder() {
        val page = FantasyCaptureParser.parse(
            "Stephen Curry GSW 30.2 42.5",
            CaptureKind.MY_TEAM,
            FantasySport.BASKETBALL,
            "My Team",
        )
        val curry = page.players.single()
        assertEquals(42.5, curry.projected!!, 1e-9)
        assertEquals(30.2, curry.seasonAverage!!, 1e-9)
    }

    @Test fun aLeagueTableKeepsTheRecordAndPointsAsPrinted() {
        val page = FantasyCaptureParser.parse(
            """
            1. Sunday Scaries 8-2 2,345.6 2,100.2
            2. Gridiron Gang 7-3 2,250.1 2,150.0
            """.trimIndent(),
            CaptureKind.LEAGUE_TABLE,
            FantasySport.FOOTBALL,
            "",
        )
        assertEquals(2, page.teams.size)
        val first = page.teams.first()
        assertEquals("Sunday Scaries", first.name)
        assertEquals(1, first.rank)
        assertEquals("8-2", first.record)
        assertEquals(2345.6, first.pointsFor!!, 1e-9)
        assertEquals(2100.2, first.pointsAgainst!!, 1e-9)
    }

    @Test fun theDefaultWeekFollowsTheSportCalendar() {
        val date = LocalDate.of(2026, 10, 30)
        assertEquals(9, FantasyCaptureParser.defaultWeek(FantasySport.FOOTBALL, date))
        assertEquals(2, FantasyCaptureParser.defaultWeek(FantasySport.BASKETBALL, date))
    }

    @Test fun aStartCallNamesTheProjectionAndTheReason() {
        val roster = listOf(
            player("Stephen Curry", "PG", 42.5),
            player("Mikal Bridges", "G", 28.1),
            player("Derrick White", "BENCH", 24.5),
        )
        val brief = FantasyLeagueDesk.build(8, roster)
        val curry = brief.writeUps.first { it.player.name == "Stephen Curry" }
        assertEquals(FantasyCall.START, curry.call)
        assertTrue(curry.body.contains("42.5"))
        assertTrue(curry.body.contains("Slot: PG"))
        val bench = brief.writeUps.first { it.player.name == "Derrick White" }
        assertEquals(FantasyCall.SIT, bench.call)
        assertTrue(bench.body.contains("bench"))
    }

    @Test fun aBenchPlayerWhoOutProjectsAStarterIsCalledUp() {
        val roster = listOf(
            player("Starter One", "G", 10.0),
            player("Starter Two", "F", 20.0),
            player("Bench Star", "BENCH", 31.0),
        )
        val brief = FantasyLeagueDesk.build(8, roster)
        val bench = brief.writeUps.first { it.player.name == "Bench Star" }
        assertEquals(FantasyCall.START, bench.call)
        assertTrue(bench.headline.contains("Worth a start"))
        val weakest = brief.writeUps.first { it.player.name == "Starter One" }
        assertEquals(FantasyCall.SIT, weakest.call)
    }

    @Test fun anOutDesignationOverridesEveryProjection() {
        val roster = listOf(
            player(
                "Stephen Curry", "PG", 60.0,
                injury = FantasyInjury(
                    "Stephen Curry", Names.key("Stephen Curry"), "GSW",
                    AvailabilityStatus.OUT, "ankle", 1L,
                ),
            ),
        )
        val call = FantasyLeagueDesk.build(8, roster).writeUps.single()
        assertEquals(FantasyCall.OUT, call.call)
        assertTrue(call.body.contains("ruled him out"))
    }

    @Test fun aPlayerWithNoProjectionIsAWatchNotAnInvention() {
        val roster = listOf(player("Mystery Rookie", "G", null))
        val call = FantasyLeagueDesk.build(8, roster).writeUps.single()
        assertEquals(FantasyCall.WATCH, call.call)
        assertTrue(call.body.contains("No recent ESPN game"))
    }

    @Test fun aRecentEspnLineIsUsedBeforeTheScreenshotProjection() {
        val roster = listOf(
            player("Hot Guard", "G", 99.0, recent = recent("Hot Guard", 30.0, 28.0, 26.0)),
        )
        val call = FantasyLeagueDesk.build(8, roster).writeUps.single()
        assertEquals(FantasyCall.START, call.call)
        assertTrue(call.body.contains("28.0 points a game"))
        assertFalse(call.body.contains("99.0"))
    }

    @Test fun aTwoTeamMatchupShowsTheProjectedEdge() {
        val mine = listOf(
            player("My One", "G", 30.0),
            player("My Two", "F", 20.0),
        )
        val theirs = listOf(
            player("Their One", "G", 25.0),
            player("Their Two", "F", 15.0),
        )
        val brief = FantasyLeagueDesk.build(8, mine, theirs, "Rival")
        assertEquals(50.0, brief.startersProjected!!, 1e-9)
        assertEquals(40.0, brief.opponentProjected!!, 1e-9)
        assertEquals(10.0, brief.edge!!, 1e-9)
        assertTrue(brief.insights.any { it.contains("Rival") && it.contains("+10.0") })
    }

    @Test fun proTeamAliasesResolveWithoutHittingTheNetwork() {
        assertEquals("GSW", ProTeams.code("GS", FantasySport.BASKETBALL))
        assertEquals("NOP", ProTeams.code("NO", FantasySport.BASKETBALL))
        assertEquals("NO", ProTeams.code("NO", FantasySport.FOOTBALL))
        assertEquals("WAS", ProTeams.code("WSH", FantasySport.FOOTBALL))
        assertEquals("Golden State Warriors", ProTeams.fullNameMatch("GSW at Golden State Warriors"))
    }

    @Test fun theTeamAssetParserKeepsTheLogoAndAliases() {
        val body = """
            {"sports":[{"leagues":[{"teams":[
              {"team":{"id":"9","abbreviation":"GS","displayName":"Golden State Warriors",
                "shortDisplayName":"Warriors","name":"Warriors","location":"Golden State",
                "logos":[{"href":"https://a.espncdn.com/i/teamlogos/nba/500/gs.png"}]}}
            ]}]}]}
        """.trimIndent()
        val teams = FantasyAssets.parseTeams(body, FantasySport.BASKETBALL)
        assertEquals(1, teams.size)
        assertEquals("GSW", teams.single().code)
        assertEquals("https://a.espncdn.com/i/teamlogos/nba/500/gs.png", teams.single().logoUrl)
        assertTrue(teams.single().aliases.contains("golden state warriors"))
    }

    @Test fun thePlayerSearchOnlyAcceptsAnExactNameInTheRightLeague() {
        val teams = listOf(
            com.propslab.fantasy.FantasyTeamAsset(
                "GSW", "Golden State Warriors",
                "https://a.espncdn.com/i/teamlogos/nba/500/gs.png",
                setOf("golden state warriors", "warriors", "gs", "gsw"),
            ),
        )
        val body = """
            {"results":[{"type":"player","contents":[
              {"uid":"s:40~l:46~a:3975","displayName":"Stephen Curry","description":"NBA",
               "subtitle":"Golden State Warriors",
               "image":{"default":"https://a.espncdn.com/i/headshots/nba/players/full/3975.png"}},
              {"uid":"s:20~l:23~a:1","displayName":"Stephen Curry","description":"NCAAF",
               "subtitle":"Wittenberg Tigers","image":{"default":"https://bad.example/curry.png"}}
            ]}]}
        """.trimIndent()
        val asset = FantasyAssets.parsePlayer(body, "Stephen Curry", FantasySport.BASKETBALL, teams, "GSW")
        assertNotNull(asset)
        assertEquals("3975", asset!!.athleteId)
        assertEquals("GSW", asset.teamCode)
        assertEquals("https://a.espncdn.com/i/headshots/nba/players/full/3975.png", asset.headshotUrl)
    }

    @Test fun aWrongNameGetsNoHeadshotRatherThanTheFirstSearchResult() {
        val body = """
            {"results":[{"type":"player","contents":[
              {"uid":"s:40~l:46~a:1","displayName":"Josh Allen","description":"NBA",
               "subtitle":"Some Team","image":{"default":"https://bad.example/josh.png"}}
            ]}]}
        """.trimIndent()
        assertNull(FantasyAssets.parsePlayer(body, "Josh Allen", FantasySport.FOOTBALL))
    }
}
