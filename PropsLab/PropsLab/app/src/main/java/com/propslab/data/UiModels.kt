package com.propslab.data

import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.Outcome
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.Side

data class Driver(val label: String, val value: Double)

data class PropUi(
    val id: String,
    val playerName: String,
    val teamCode: String,
    val gameId: String,
    val matchup: String,
    val startUtc: Long,
    val market: String,
    val book: String,
    val line: Double?,
    val overOdds: Int?,
    val underOdds: Int?,
    val quoteTimeUtc: Long,
    val probabilities: OutcomeProbabilities?,
    /** No-vig market probability of the over. */
    val marketOver: Double?,
    /** Model minus market for the over, in percentage points. */
    val edgePts: Double?,
    val reasons: List<String>,
    val flags: List<String>,
    val drivers: List<Driver>,
    val explanation: String,
    val modelVersion: String,
    val schemaVersion: Int,
    val featureCutoffUtc: Long,
    val isFixture: Boolean,
    /** The league this prop belongs to: `basketball` or `football`. */
    val sport: String = com.propslab.domain.Sports.BASKETBALL,
) {
    val available: Boolean get() = probabilities != null && line != null && overOdds != null && underOdds != null
    val lean: Side? get() = edgePts?.let { if (it >= 0) Side.OVER else Side.UNDER }
}

data class GameGroup(
    val id: String,
    val matchup: String,
    val startUtc: Long,
    val props: List<PropUi>,
    val sport: String = com.propslab.domain.Sports.BASKETBALL,
) {
    /** The league label the card prints above the matchup. */
    val league: String get() = com.propslab.domain.Sports.label(sport)
}

data class BetUi(
    val id: String,
    val playerName: String,
    val teamCode: String,
    val side: Side,
    val line: Double,
    val odds: Int,
    val stake: Double,
    val decidedUtc: Long,
    val actual: Double?,
    val outcome: Outcome?,
    val profit: Double?,
    val settledUtc: Long?,
    val market: String = "player_points",
    val book: String = "",
    /** The formula this bet was tagged to; empty means the user's own pick. */
    val formulaId: String = "",
    val formulaLabel: String = "",
) {
    val isOpen: Boolean get() = outcome == null

    /** Stable key used by the history filter and the formula chart. */
    val formulaKey: String get() = formulaId.ifBlank { PracticeLedger.MANUAL_FORMULA_ID }

    /** What the ledger prints for a bet with no formula tag. */
    val formulaName: String get() = formulaLabel.ifBlank { "My pick" }

    /** The market in the words a book uses: "Points", "Rebounds", and so on. */
    val marketLabel: String get() = Markets.label(market)

    /** Decimal price of the bet, so stake and payout can be shown the way a slip does. */
    val decimal: Double get() = Odds.americanToDecimal(odds)

    /** Profit if the bet wins, in the same practice units as the stake. */
    val toWin: Double get() = stake * (decimal - 1.0)

    /** What the ticket returns if it wins, stake included. */
    val toReturn: Double get() = stake * decimal
}

sealed interface PlaceResult {
    data object Placed : PlaceResult
    data object Duplicate : PlaceResult
    data object Unavailable : PlaceResult
    data class NotEnoughBankroll(val available: Double) : PlaceResult
}
