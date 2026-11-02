package com.propslab.data

import com.propslab.sim.InsightKind
import com.propslab.sim.StrategyParams

/** The correction currently applied to one model. */
data class CalibrationUi(
    val track: String,
    val slope: Double,
    val intercept: Double,
    val sample: Int,
    val brierBefore: Double,
    val brierAfter: Double,
    val generation: Int,
    val updatedUtc: Long,
) {
    val label: String get() = if (track == LearningTrack.GAME) "Game model" else "Prop model"
    val fitted: Boolean get() = sample > 0
    val summary: String
        get() = if (!fitted) {
            "$label: no correction yet. The model's own probabilities are used unchanged."
        } else {
            "$label: slope ${"%.2f".format(slope)}, shift ${"%+.2f".format(intercept)} on $sample settled " +
                "decisions. Brier ${"%.4f".format(brierBefore)} → ${"%.4f".format(brierAfter)} in sample."
        }
}

/** One row of the ledger, as the Lab shows it. */
data class LearningBetUi(
    val id: String,
    val label: String,
    val detail: String,
    val odds: Int,
    val pModel: Double,
    val edgePts: Double?,
    val confidence: String,
    val outcome: String?,
    val profit: Double?,
)

/** One segment finding from the losing record, and what happened to the rule it implies. */
data class LearningInsightUi(
    val id: String,
    val kind: InsightKind,
    val title: String,
    val detail: String,
    val advice: String,
    val proposalLabel: String?,
    /** What the sandbox did with the candidate, or null when the finding implies none. */
    val status: String?,
)

/** What the learning loop has recorded, and what it has learned from it. */
data class LearningUi(
    val props: Int = 0,
    val games: Int = 0,
    val settled: Int = 0,
    val open: Int = 0,
    val staked: Double = 0.0,
    val profit: Double = 0.0,
    val calibration: List<CalibrationUi> = emptyList(),
    val recent: List<LearningBetUi> = emptyList(),
    val note: String = "",
    /** Segments where the settled record is leaning, newest analysis first. */
    val insights: List<LearningInsightUi> = emptyList(),
    val insightNote: String = "",
) {
    val total: Int get() = props + games
    val roi: Double? get() = if (staked > 0.0) profit / staked else null

    /** The model is learning from every settled bet, whatever its confidence was. */
    val summary: String
        get() = when {
            total == 0 -> "No bets logged yet. The next sync writes every priced prop and game market to the ledger."
            settled == 0 -> "$total bets logged, none settled yet. The ledger starts learning as soon as results arrive."
            else -> "$total bets logged ($props props, $games games), $settled settled, " +
                "${"%.2f".format(profit)} units ${if (profit >= 0) "up" else "down"} on ${"%.0f".format(staked)} staked."
        }
}

/** What one call to [PropsRepository.learn] did. */
data class LearningReport(
    val logged: Int = 0,
    val settled: Int = 0,
    val refit: Int = 0,
    val note: String = "",
)

/**
 * What the user's own settled paper bets teach, separate from the system's losing
 * record. The same descriptive analysis runs over the user's picks only, so a finding
 * here is about the bettor's own habits — which side, market or price band they keep
 * taking — and each rule it implies can be filed as a testable strategy.
 */
data class UserFindingUi(
    val id: String,
    val kind: InsightKind,
    val title: String,
    val detail: String,
    val advice: String,
    val proposal: StrategyParams?,
    val proposalLabel: String?,
    /** What the sandbox did with the candidate, or null when the finding implies none. */
    val status: String?,
)

/** The user's own record through the same lens the system uses on itself. */
data class UserInsightsUi(
    val findings: List<UserFindingUi> = emptyList(),
    val considered: Int = 0,
    val note: String = "",
)
