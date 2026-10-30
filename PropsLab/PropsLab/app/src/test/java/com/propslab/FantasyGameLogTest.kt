package com.propslab

import com.propslab.fantasy.FantasyGameLogApi
import com.propslab.fantasy.FantasySport
import org.junit.Assert.*
import org.junit.Test

class FantasyGameLogTest {

    private val basketballBody = """
        {
          "labels":["MIN","PTS","REB","AST","3PT"],
          "names":["minutes","points","totalRebounds","assists","threePointFieldGoalsMade-threePointFieldGoalsAttempted"],
          "displayNames":["Minutes","Points","Rebounds","Assists","3PT"],
          "events":{
            "1":{"gameDate":"2026-10-28T23:30:00.000+00:00","opponent":{"abbreviation":"ATL"},
                 "atVs":"vs","gameResult":"L","score":"114-111"},
            "2":{"gameDate":"2026-10-26T00:30:00.000+00:00","opponent":{"abbreviation":"BOS"},
                 "atVs":"@","gameResult":"W","score":"112-108"}
          },
          "seasonTypes":[
            {"displayName":"2026-27 Regular Season","categories":[
              {"type":"event","events":[
                {"eventId":"1","stats":["30","24","8","6","3-7"]},
                {"eventId":"2","stats":["28","10","12","2","0-2"]}
              ]}
            ]}
          ]
        }
    """.trimIndent()

    @Test fun basketballRowsAreScoredJoinedAndSortedNewestFirst() {
        val log = FantasyGameLogApi.parse(
            basketballBody, FantasySport.BASKETBALL, "4066328", "Jarrett Allen",
        )
        assertEquals("2026-27 Regular Season", log.season)
        assertEquals(2, log.entries.size)
        val first = log.entries.first()
        assertEquals("1", first.eventId)
        assertEquals(51.6, first.fantasyPoints!!, 1e-9)
        assertEquals("24 pts · 8 reb · 6 ast", first.statLine)
        assertEquals("vs ATL", "${first.homeAway} ${first.opponent}")
        assertEquals("L", first.result)
        assertEquals("114-111", first.score)
        assertEquals(27.4, log.entries[1].fantasyPoints!!, 1e-9)
        assertTrue(log.note.contains("last 2 games"))
        val regular = log.seasons.single()
        assertEquals(2, regular.scoredGames)
        assertEquals(39.5, regular.averagePoints!!, 1e-9)
        assertEquals(51.6, regular.bestPoints!!, 1e-9)
        val hit = regular.hitRate(30.0)
        assertEquals(1, hit.over)
        assertEquals(1, hit.under)
        assertEquals(0, hit.push)
        assertEquals(2, hit.decided)
        assertEquals(0.5, hit.overShare!!, 1e-9)
        assertEquals(0.5, hit.underShare!!, 1e-9)
    }

    @Test fun footballRowsUseTheCanonicalNamesNotTheColumnOrder() {
        val body = """
            {
              "labels":["CMP","ATT","YDS","CMP%","AVG","TD","INT","LNG","SACK","RTG","QBR","CAR","YDS","AVG","TD","LNG"],
              "names":["completions","passingAttempts","passingYards","completionPct",
                       "yardsPerPassAttempt","passingTouchdowns","interceptions","longPassing",
                       "sacks","QBRating","adjQBR","rushingAttempts","rushingYards",
                       "yardsPerRushAttempt","rushingTouchdowns","longRushing"],
              "displayNames":["Completions","Attempts","Yards","Completion %","Average","Touchdowns",
                              "Interceptions","Long","Sacks","Rating","QBR","Carries","Yards",
                              "Average","Touchdowns","Long"],
              "events":{"401873009":{"gameDate":"2026-10-27T00:15:00.000+00:00",
                        "opponent":{"abbreviation":"PHI"},"atVs":"@","gameResult":"W","score":"38-21"}},
              "seasonTypes":[
                {"displayName":"2026 Regular Season","categories":[
                  {"type":"event","events":[
                    {"eventId":"401873009","stats":["15","27","212","55.6","7.9","3","0","35","0","118.1","88.3","2","8","4.0","0","6"]}
                  ]}
                ]}
              ]
            }
        """.trimIndent()
        val log = FantasyGameLogApi.parse(body, FantasySport.FOOTBALL, "2577417", "Dak Prescott")
        val row = log.entries.single()
        assertEquals(21.28, row.fantasyPoints!!, 1e-9)
        assertTrue(row.statLine.contains("212 pass yds"))
        assertTrue(row.statLine.contains("3 pass TD"))
        assertTrue(row.statLine.contains("8 rush yds"))
        assertEquals("@ PHI", "${row.homeAway} ${row.opponent}")
    }

    @Test fun theRegularSeasonIsPreferredOverThePreseason() {
        val body = """
            {
              "names":["points","totalRebounds","assists"],
              "displayNames":["Points","Rebounds","Assists"],
              "events":{
                "pre":{"gameDate":"2026-10-01T00:00:00.000+00:00","opponent":{"abbreviation":"NYK"}},
                "reg":{"gameDate":"2026-10-28T00:00:00.000+00:00","opponent":{"abbreviation":"BOS"}}
              },
              "seasonTypes":[
                {"displayName":"2026-27 Preseason","categories":[
                  {"type":"event","events":[{"eventId":"pre","stats":["40","10","5"]}]}
                ]},
                {"displayName":"2026-27 Regular Season","categories":[
                  {"type":"event","events":[{"eventId":"reg","stats":["20","8","4"]}]}
                ]}
              ]
            }
        """.trimIndent()
        val log = FantasyGameLogApi.parse(body, FantasySport.BASKETBALL, "1", "Regular Guy")
        assertEquals(1, log.entries.size)
        assertEquals("reg", log.entries.single().eventId)
        assertEquals("2026-27 Regular Season", log.season)
        assertEquals(
            listOf("2026-27 Preseason", "2026-27 Regular Season"),
            log.seasons.map { it.name },
        )
        assertEquals("pre", log.entriesFor("2026-27 Preseason").single().eventId)
    }

    @Test fun postseasonIsItsOwnSplitAndDoesNotBleedIntoTheDefault() {
        val body = """
            {
              "names":["points"],
              "displayNames":["Points"],
              "events":{
                "reg":{"gameDate":"2026-10-28T00:00:00.000+00:00"},
                "post":{"gameDate":"2026-06-10T00:00:00.000+00:00"}
              },
              "seasonTypes":[
                {"displayName":"2025-26 Regular Season","categories":[
                  {"type":"event","events":[{"eventId":"reg","stats":["22"]}]}
                ]},
                {"displayName":"2025-26 Postseason","categories":[
                  {"type":"event","events":[{"eventId":"post","stats":["31"]}]}
                ]}
              ]
            }
        """.trimIndent()
        val log = FantasyGameLogApi.parse(body, FantasySport.BASKETBALL, "1", "Playoff Guy")
        assertEquals("reg", log.entries.single().eventId)
        assertEquals("2025-26 Regular Season", log.season)
        val postseason = log.entriesFor("2025-26 Postseason").single()
        assertEquals("post", postseason.eventId)
        assertEquals(listOf("Regular season", "Postseason"), log.seasons.map { it.label })
        assertTrue(log.entriesFor("2025-26 Preseason").isEmpty())
        val push = log.seasons.first { it.label == "Postseason" }.hitRate(31.0)
        assertEquals(1, push.push)
        assertEquals(0, push.decided)
        assertNull(push.overShare)
        assertNull(push.underShare)
    }

    @Test fun aKickerHasALogButNoInventedFantasyTotal() {
        val body = """
            {
              "names":["fieldGoalsMade-fieldGoalsAttempted","points"],
              "displayNames":["Field Goals Made-Attempted","Points"],
              "events":{"1":{"gameDate":"2026-10-28T00:00:00.000+00:00"}},
              "seasonTypes":[
                {"displayName":"2026 Regular Season","categories":[
                  {"type":"event","events":[{"eventId":"1","stats":["3-4","9"]}]}
                ]}
              ]
            }
        """.trimIndent()
        val log = FantasyGameLogApi.parse(body, FantasySport.FOOTBALL, "2", "Kicker")
        val row = log.entries.single()
        assertNull(row.fantasyPoints)
        assertTrue(row.statLine.contains("3-4"))
        assertEquals(0, log.seasons.single().scoredGames)
        assertNull(log.seasons.single().averagePoints)
        assertNull(log.seasons.single().bestPoints)
        val hit = log.seasons.single().hitRate(10.0)
        assertEquals(0, hit.scored)
        assertEquals(0, hit.decided)
        assertNull(hit.overShare)
    }

    @Test fun malformedLogsYieldAnEmptyLogRatherThanAnException() {
        val empty = FantasyGameLogApi.parse("not json", FantasySport.FOOTBALL, "x", "Nobody")
        assertTrue(empty.entries.isEmpty())
        assertTrue(
            FantasyGameLogApi.parse(
                """{"names":[],"seasonTypes":[]}""", FantasySport.BASKETBALL, "x", "Nobody",
            ).entries.isEmpty(),
        )
    }
}
