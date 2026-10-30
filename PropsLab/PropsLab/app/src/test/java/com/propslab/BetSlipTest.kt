package com.propslab

import com.propslab.data.ParlayCandidate
import com.propslab.data.ParlayKind
import com.propslab.data.ParlayLegUi
import com.propslab.data.ParlayTicketUi
import com.propslab.data.ParlayTickets
import com.propslab.domain.Odds
import com.propslab.domain.Side
import com.propslab.fantasy.FantasySport
import com.propslab.fantasy.ProTeams
import com.propslab.slip.BetSlipBuilder
import com.propslab.slip.SlipBook
import com.propslab.slip.SlipTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BetSlipTest {

    private fun candidate(
        name: String,
        team: String,
        label: String,
        odds: Int,
        chance: Double,
        line: Double = 24.5,
    ) = ParlayCandidate(
        key = "p:$name",
        gameId = "g-$team",
        subject = name,
        label = label,
        detail = "$team vs OPP · book",
        odds = odds,
        pWin = chance,
        pPush = 0.0,
        edgePts = 2.0,
        line = line,
        side = Side.OVER,
        overProb = chance,
        teamCode = team,
    )

    private fun ticket(vararg legs: Pair<ParlayCandidate, Double>): ParlayTicketUi {
        val rows = legs.map { (pick, chance) -> ParlayLegUi(candidate = pick, winChance = chance) }
        return ParlayTicketUi(
            size = rows.size,
            legs = rows,
            probability = 0.41,
            decimal = 6.42,
            games = rows.size,
            sameGame = 0,
            anchor = rows.first().candidate,
            propLegs = rows.size,
            gameLegs = 0,
            explanation = "Three different games.",
            kind = ParlayKind.STRAIGHT,
        )
    }

    private fun sample() = ticket(
        candidate("LeBron James", "LAL", "LeBron James Over 24.5 Points", -115, 0.56, 24.5) to 0.56,
        candidate("Jayson Tatum", "BOS", "Jayson Tatum Over 28.5 Points", -105, 0.52, 28.5) to 0.52,
        candidate("Nikola Jokic", "DEN", "Nikola Jokic Over 9.5 Assists", 110, 0.48, 9.5) to 0.48,
    )

    @Test
    fun `a slip keeps the board's picks in tracker-readable lines`() {
        val slip = BetSlipBuilder.from(sample(), SlipBook.DRAFTKINGS, stake = 2.0)
        val text = slip.plainText()

        assertEquals(3, slip.legs.size)
        assertEquals("3-leg parlay", slip.label)
        assertEquals("LeBron James Over 24.5 Points", slip.legs[0].pick)
        assertEquals("Jayson Tatum Over 28.5 Points", slip.legs[1].pick)
        assertEquals("Nikola Jokic Over 9.5 Assists", slip.legs[2].pick)
        assertTrue(text, text.contains("LeBron James Over 24.5 Points (-115)"))
        assertTrue(text, text.contains("Jayson Tatum Over 28.5 Points (-105)"))
        assertTrue(text, text.contains("Nikola Jokic Over 9.5 Assists (+110)"))
        assertTrue(text.contains("Stake 2.00 u"))
        assertTrue(text.contains("not a placed bet", ignoreCase = true))
    }

    @Test
    fun `moving one leg's line changes its price and chance`() {
        val candidate = candidate(
            "LeBron James", "LAL", "LeBron James Over 24.5 Points", -115, 0.56, 24.5,
        ).copy(mean = 24.5, spread = 6.0)

        val quoted = ParlayTickets.reprice(candidate, 0.0, circadian = false)
        val moved = ParlayTickets.reprice(candidate, 2.0, circadian = false)

        assertEquals(24.5, quoted.line!!, 1e-9)
        assertEquals(26.5, moved.line!!, 1e-9)
        assertEquals(2.0, moved.shiftPts, 1e-9)
        assertTrue(
            "a longer line must pay a longer price",
            Odds.americanToDecimal(moved.odds) > Odds.americanToDecimal(quoted.odds),
        )
        assertTrue(
            "a longer line must be less likely",
            moved.winChance < quoted.winChance,
        )
    }

    @Test
    fun `moving a line toward you shortens the price and raises the chance`() {
        val candidate = candidate(
            "LeBron James", "LAL", "LeBron James Over 24.5 Points", -115, 0.56, 24.5,
        ).copy(mean = 24.5, spread = 6.0)

        val quoted = ParlayTickets.reprice(candidate, 0.0, circadian = false)
        val eased = ParlayTickets.reprice(candidate, -2.0, circadian = false)

        assertEquals(22.5, eased.line!!, 1e-9)
        assertEquals(-2.0, eased.shiftPts, 1e-9)
        assertTrue(
            "a shorter line must pay a shorter price",
            Odds.americanToDecimal(eased.odds) < Odds.americanToDecimal(quoted.odds),
        )
        assertTrue(
            "a shorter line must be more likely",
            eased.winChance > quoted.winChance,
        )
    }

    @Test
    fun `an edited leg writes the moved line into the tracker text`() {
        val candidate = candidate(
            "LeBron James", "LAL", "LeBron James Over 24.5 Points", -115, 0.56, 24.5,
        )
        val edited = ParlayLegUi(
            candidate = candidate,
            winChance = 0.51,
            odds = 105,
            line = 26.5,
        )
        val slip = BetSlipBuilder.from(
            label = "3-leg parlay",
            legs = listOf(edited),
            decimal = 4.2,
            probability = 0.30,
            book = SlipBook.DRAFTKINGS,
            stake = 1.0,
        )
        assertEquals("LeBron James Over 26.5 Points", slip.legs.single().pick)
        assertTrue(slip.plainText().contains("LeBron James Over 26.5 Points (+105)"))
        assertEquals(0.51, slip.legs.single().modelChance, 1e-9)
    }

    @Test
    fun `the stake and the ticket price produce the return`() {
        val slip = BetSlipBuilder.from(sample(), SlipBook.FANDUEL, stake = 2.0)
        assertEquals(12.84, slip.potentialReturn, 0.0001)
        assertEquals(10.84, slip.profit, 0.0001)
        assertEquals(0.41, slip.probability, 0.0001)
        assertEquals(6.42, slip.decimal, 0.0001)
    }

    @Test
    fun `each leg carries its club's bundled mark`() {
        val slip = BetSlipBuilder.from(sample(), SlipBook.BETMGM, stake = 1.0)
        assertEquals("https://a.espncdn.com/i/teamlogos/nba/500/lal.png", slip.legs[0].teamLogoUrl)
        assertEquals("https://a.espncdn.com/i/teamlogos/nba/500/bos.png", slip.legs[1].teamLogoUrl)
        assertEquals("https://a.espncdn.com/i/teamlogos/nba/500/den.png", slip.legs[2].teamLogoUrl)
    }

    @Test
    fun `every NBA and NFL club has a mark without a lookup`() {
        FantasySport.entries.forEach { sport ->
            val codes = ProTeams.codes(sport)
            assertTrue("$sport should ship clubs", codes.isNotEmpty())
            codes.forEach { code ->
                val url = ProTeams.logoUrl(code, sport)
                assertTrue("$sport $code has no bundled mark", url.startsWith("https://a.espncdn.com/i/teamlogos/"))
                assertTrue(url.endsWith(".png"))
            }
        }
        assertTrue(ProTeams.logoUrl("GSW", FantasySport.BASKETBALL).endsWith("/gs.png"))
        assertTrue(ProTeams.logoUrl("NOP", FantasySport.BASKETBALL).endsWith("/no.png"))
        assertTrue(ProTeams.logoUrl("UTA", FantasySport.BASKETBALL).endsWith("/utah.png"))
        assertTrue(ProTeams.logoUrl("WAS", FantasySport.FOOTBALL).endsWith("/wsh.png"))
    }

    @Test
    fun `the books and trackers are public addresses`() {
        SlipBook.entries.forEach { book ->
            assertTrue(book.siteUrl.startsWith("https://"))
        }
        SlipTracker.entries.forEach { tracker ->
            assertTrue(tracker.storeUrl.startsWith("https://play.google.com/store/apps/details?id="))
            assertTrue(tracker.importUrl.startsWith("https://"))
            assertTrue(tracker.packageId.contains('.'))
        }
        assertEquals("com.gambly.app", SlipTracker.GAMBLY.packageId)
        assertEquals("com.pikkit.android", SlipTracker.PIKKIT.packageId)
    }

    @Test
    fun `a file name is safe for the share cache`() {
        val name = BetSlipBuilder.from(sample(), SlipBook.CAESARS, stake = 1.0).fileName()
        assertTrue(name.startsWith("bet-slip-3-leg-parlay-"))
        assertFalse(name.contains(' '))
        assertFalse(name.contains('/'))
    }
}
