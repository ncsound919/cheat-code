package com.propslab.slip

import com.propslab.data.ParlayKind
import com.propslab.data.ParlayTicketUi
import com.propslab.domain.Side
import com.propslab.fantasy.FantasySport
import com.propslab.fantasy.ProTeams
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The books a slip can be labelled for.
 *
 * This is a label and an address, not an integration. None of these books publishes a
 * public way to fill a slip from another app, so opening one is the last manual step and
 * the app never pretends otherwise.
 */
enum class SlipBook(
    val displayName: String,
    val siteUrl: String,
) {
    DRAFTKINGS("DraftKings", "https://sportsbook.draftkings.com/"),
    FANDUEL("FanDuel", "https://sportsbook.fanduel.com/"),
    BETMGM("BetMGM", "https://sports.betmgm.com/"),
    CAESARS("Caesars", "https://sportsbook.caesars.com/"),
}

/** Where a finished slip can be handed off, and what that app actually accepts. */
enum class SlipTracker(
    val displayName: String,
    val packageId: String,
    val storeUrl: String,
    val importUrl: String,
    val importLabel: String,
    val accept: SlipAccept,
) {
    GAMBLY(
        displayName = "Gambly",
        packageId = "com.gambly.app",
        storeUrl = "https://play.google.com/store/apps/details?id=com.gambly.app",
        importUrl = "https://gambly.com/image-to-betslip",
        importLabel = "Share the slip image to Gambly",
        accept = SlipAccept.IMAGE,
    ),
    PIKKIT(
        displayName = "Pikkit",
        packageId = "com.pikkit.android",
        storeUrl = "https://play.google.com/store/apps/details?id=com.pikkit.android",
        importUrl = "https://pikkit.com/quickpickbot-web",
        importLabel = "Paste the slip text into QuickPickBot",
        accept = SlipAccept.TEXT,
    ),
}

enum class SlipAccept { IMAGE, TEXT }

/** One leg as it should read outside the app, with the club mark already attached. */
data class SlipLeg(
    val player: String,
    val pick: String,
    val market: String,
    val side: Side,
    val line: Double?,
    val odds: Int,
    val modelChance: Double,
    val teamCode: String,
    val teamLogoUrl: String,
)

/**
 * A finished slip, ready to share.
 *
 * The numbers are the ticket's own: the stake is the only thing the user adds. The text is
 * deliberately one pick per line in the shape a tracker's parser expects; the image adds
 * the layout a person expects.
 */
data class BetSlip(
    val label: String,
    val book: SlipBook,
    val legs: List<SlipLeg>,
    val decimal: Double,
    val probability: Double,
    val stake: Double,
    val dateLine: String = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
        .format(LocalDate.now()),
) {
    val potentialReturn: Double get() = stake * decimal
    val profit: Double get() = potentialReturn - stake

    /** No header noise on the pick lines: that is the part a tracker can actually read. */
    fun plainText(): String = buildString {
        appendLine("CHEAT CODE · BET SLIP")
        appendLine("${book.displayName} · $label · $dateLine")
        appendLine()
        legs.forEach { leg ->
            appendLine(leg.pickWithPrice())
        }
        appendLine()
        appendLine(
            "Stake ${"%.2f".format(stake)} u · ${"%.2f".format(decimal)}x · " +
                "returns ${"%.2f".format(potentialReturn)} u",
        )
        appendLine(
            "Model chance ${"%.1f".format(probability * 100)}% · paper analysis, not a placed bet.",
        )
        append("propslab.app")
    }

    fun fileName(): String = "bet-slip-${label.replace(' ', '-')}-${dateLine.replace(' ', '-')}"

    /** The pick line a tracker or a person reads: name, side, line, market, price. */
    fun SlipLeg.pickWithPrice(): String = "$pick (${priceLabel(odds)})"

    private fun priceLabel(odds: Int): String = if (odds > 0) "+$odds" else "$odds"
}

/**
 * Turns a generated ticket into a slip.
 *
 * The legs come from the board the ticket was built on, so the players are already the
 * ones the model ranked highest and the club marks need no import step. A game market has
 * no player, so its matchup is used as the subject and its venue supplies the mark.
 */
object BetSlipBuilder {

    fun from(
        ticket: ParlayTicketUi,
        book: SlipBook = SlipBook.DRAFTKINGS,
        stake: Double = 0.0,
        sport: FantasySport = FantasySport.BASKETBALL,
    ): BetSlip {
        val legs = ticket.legs.map { leg ->
            val candidate = leg.candidate
            val teamCode = candidate.teamCode ?: candidate.venueCode.orEmpty()
            SlipLeg(
                player = candidate.subject ?: candidate.detail.substringBefore(" · ").ifBlank { candidate.label },
                pick = candidate.label,
                market = candidate.label.substringAfterLast(' '),
                side = candidate.side,
                line = leg.line,
                odds = leg.odds,
                modelChance = leg.winChance,
                teamCode = teamCode,
                teamLogoUrl = ProTeams.logoUrl(teamCode, sport),
            )
        }
        val label = if (ticket.kind == ParlayKind.SAME_GAME) {
            "same-game parlay"
        } else {
            "${ticket.size}-leg parlay"
        }
        return BetSlip(
            label = label,
            book = book,
            legs = legs,
            decimal = ticket.decimal.coerceAtLeast(1.0),
            probability = ticket.probability,
            stake = stake.coerceAtLeast(0.0),
        )
    }
}
