package com.propslab

import com.propslab.domain.Side
import com.propslab.ocr.SlipParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OCR text is messy. These cases are the shapes the three books actually print, plus the
 * junk a screenshot always carries. The parser is allowed to skip a line; it is never
 * allowed to invent one.
 */
class SlipParserTest {

    @Test
    fun readsAFullDraftKingsStyleLine() {
        val picks = SlipParser.parse("LeBron James Over 24.5 Points  -115")
        assertEquals(1, picks.size)
        val pick = picks.first()
        assertEquals("LeBron James", pick.playerName)
        assertEquals(Side.OVER, pick.side)
        assertEquals(24.5, pick.line, 0.0)
        assertEquals("player_points", pick.market)
        assertEquals(-115, pick.odds)
    }

    @Test
    fun readsASeparateNameAndPrizePicksStylePick() {
        val text = """
            My Slip
            LeBron James
            More 24.5 Points
            Anthony Davis
            Less 12.5 Rebounds
            Payout $180.00
        """.trimIndent()
        val picks = SlipParser.parse(text)
        assertEquals(2, picks.size)

        assertEquals("LeBron James", picks[0].playerName)
        assertEquals(Side.OVER, picks[0].side)
        assertEquals(24.5, picks[0].line, 0.0)

        assertEquals("Anthony Davis", picks[1].playerName)
        assertEquals(Side.UNDER, picks[1].side)
        assertEquals(12.5, picks[1].line, 0.0)
        assertEquals("player_rebounds", picks[1].market)
    }

    @Test
    fun readsShorthandSidesAndMarkets() {
        val picks = SlipParser.parse(
            """
            SHAI GILGEOUS-ALEXANDER O 31.5 PTS -110
            Nikola Jokic U 8.5 AST
            Victor Wembanyama O 2.5 3-Pointers
            """.trimIndent(),
        )
        assertEquals(3, picks.size)
        assertEquals("Shai Gilgeous-Alexander", picks[0].playerName)
        assertEquals(Side.OVER, picks[0].side)
        assertEquals(-110, picks[0].odds)
        assertEquals("player_assists", picks[1].market)
        assertEquals(Side.UNDER, picks[1].side)
        assertEquals("player_threes", picks[2].market)
        assertEquals(31.5, picks[0].line, 0.0)
    }

    @Test
    fun normalisesUnicodeMinusAndOddSpacing() {
        val picks = SlipParser.parse("Jayson Tatum\tOver 27.5 Points \u2212125")
        assertEquals(1, picks.size)
        assertEquals(-125, picks.first().odds)
        assertEquals("Jayson Tatum", picks.first().playerName)
    }

    @Test
    fun dropsHeadersAndJunkInsteadOfMatchingThem() {
        val text = """
            Bet Slip
            Payout
            Round Robin
            Add to slip
            12345
            Save
            Total $50
        """.trimIndent()
        assertTrue(SlipParser.parse(text).isEmpty())
        assertNull(SlipParser.nameCandidate("Bet Slip"))
        assertNull(SlipParser.nameCandidate("12345"))
        assertNull(SlipParser.nameCandidate("LeBron James Over 24.5"))
    }

    @Test
    fun aNameNeverAttachesAcrossAnUnrelatedParagraph() {
        val text = """
            LeBron James
            This slip is powered by a partner
            Over 24.5 Points
        """.trimIndent()
        // The name can only attach to a pick directly after it, so this line is skipped.
        assertTrue(SlipParser.parse(text).isEmpty())
    }

    @Test
    fun ignoresImpossibleLines() {
        assertTrue(SlipParser.parse("Over 999 Points LeBron James").isEmpty())
        assertTrue(SlipParser.parse("Under 0 Points").isEmpty())
        assertTrue(SlipParser.parse("LeBron James").isEmpty())
    }

    @Test
    fun duplicateRowsCollapse() {
        val text = """
            LeBron James Over 24.5 Points -115
            LeBron James Over 24.5 Points -115
        """.trimIndent()
        assertEquals(1, SlipParser.parse(text).size)
    }

    @Test
    fun theSamePlayerOnBothSidesStaysTwoPicks() {
        val text = """
            LeBron James Over 24.5 Points -115
            LeBron James Under 24.5 Points -105
        """.trimIndent()
        assertEquals(2, SlipParser.parse(text).size)
    }

    @Test
    fun keepsNamesWithApostrophesAndHyphens() {
        val text = """
            D'Angelo Russell Over 15.5 Points
            Shai Gilgeous-Alexander Over 30.5 Points
        """.trimIndent()
        val picks = SlipParser.parse(text)
        assertEquals(2, picks.size)
        assertEquals("D'Angelo Russell", picks[0].playerName)
        assertEquals("Shai Gilgeous-Alexander", picks[1].playerName)
    }

    @Test
    fun anUnreadableOddsTokenIsLeftNullRatherThanGuessed() {
        val picks = SlipParser.parse("LeBron James Over 24.5 Points")
        assertEquals(1, picks.size)
        assertNull(picks.first().odds)
    }
}
