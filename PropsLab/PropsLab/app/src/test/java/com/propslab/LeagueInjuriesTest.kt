package com.propslab

import com.propslab.availability.LeagueInjuries
import com.propslab.data.AvailabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The league report is the one availability source that needs no key, so its parser is
 * pinned to a real payload: the shape is public and stable, but the wording of a
 * designation is not, and a phrase this side does not understand must be dropped rather
 * than guessed into a multiplier.
 */
class LeagueInjuriesTest {

    private val payload = """
        {"timestamp":"2026-10-29T17:03:42Z","status":"success",
         "injuries":[
           {"id":"1","displayName":"Atlanta Hawks","abbreviation":"ATL","injuries":[
             {"id":"-1","status":"Out","date":"2026-10-19T19:38Z",
              "shortComment":"Gueye underwent surgery Tuesday to repair a fractured left foot.",
              "athlete":{"displayName":"Mouhamed Gueye"}},
             {"id":"-2","status":"Day-To-Day","date":"2026-10-28T21:05Z",
              "shortComment":"  Listed as   questionable   after a limited practice.  ",
              "athlete":{"displayName":"Onyeka Okongwu"}},
             {"id":"-3","status":"Fined","date":"2026-10-28T21:05Z",
              "shortComment":"Not an availability change.",
              "athlete":{"displayName":"Someone Else"}},
             {"id":"-4","status":"Out","date":"2026-10-28T21:05Z",
              "shortComment":"No athlete object at all."}
           ]},
           {"id":"2","displayName":"Boston Celtics","abbreviation":"BOS","injuries":[
             {"id":"-5","status":"Probable","date":"2026-10-28T18:00Z",
              "shortComment":"Expected to play.",
              "athlete":{"displayName":"Derrick White"}}
           ]}
         ]}
    """.trimIndent()

    @Test
    fun onlyDesignationsWithAKnownStatusBecomeRows() {
        val rows = LeagueInjuries().parse(payload, nowUtc = 1_700_000_000_000L)

        assertEquals(listOf("Mouhamed Gueye", "Onyeka Okongwu", "Derrick White"), rows.map { it.playerName })
        assertEquals(
            listOf(AvailabilityStatus.OUT, AvailabilityStatus.QUESTIONABLE, AvailabilityStatus.PROBABLE),
            rows.map { it.status },
        )
    }

    @Test
    fun theReportDateIsTheObservationTime() {
        val rows = LeagueInjuries().parse(payload, nowUtc = 1_700_000_000_000L)
        assertEquals(Instant.parse("2026-10-19T19:38:00Z").toEpochMilli(), rows.first().observedUtc)
    }

    @Test
    fun aReportWithoutADateFallsBackToNowAndTheNoteIsFlattened() {
        val rows = LeagueInjuries().parse(payload, nowUtc = 1_700_000_000_000L)
        val okongwu = rows.first { it.playerName == "Onyeka Okongwu" }
        assertEquals(
            "Listed as questionable after a limited practice.",
            okongwu.note,
        )
        assertTrue(okongwu.note.none { it == '\n' })

        val undated = LeagueInjuries().parse(
            """{"injuries":[{"abbreviation":"LAL","injuries":[{"status":"Out","athlete":{"displayName":"A Player"}}]}]}""",
            nowUtc = 1_700_000_000_000L,
        )
        assertEquals(1_700_000_000_000L, undated.single().observedUtc)
    }

    @Test
    fun anUnreadablePayloadYieldsNothingRatherThanAnException() {
        assertTrue(LeagueInjuries().parse("not json").isEmpty())
        assertTrue(LeagueInjuries().parse("""{"status":"success"}""").isEmpty())
        assertTrue(LeagueInjuries().parse("""{"injuries":[]}""").isEmpty())
    }
}
