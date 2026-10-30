package com.propslab.data

/**
 * What the app is doing on its own, in one place.
 *
 * This is the easy-mode answer to "do I need to do anything?". Each step is a component
 * that runs without the user: prices, the formula search, the learning ledger, and the
 * optional historical gate. A step is never described as working when it has not run —
 * the state comes from stored facts, not from a promise.
 */
enum class AutoStepState { DONE, WORKING, WAITING, NEEDS_YOU }

data class AutoStep(
    val title: String,
    val detail: String,
    val state: AutoStepState,
)

data class AutopilotUi(
    val headline: String = "Checking the app…",
    val detail: String = "Reading what is already on this phone.",
    val steps: List<AutoStep> = emptyList(),
    /** True when the board has never received a real price sync. */
    val needsSetup: Boolean = false,
    val live: Boolean = false,
    val topPickId: String? = null,
    val topPickLabel: String? = null,
    val lastRunUtc: Long = 0L,
)

/**
 * Turns the stored state into the sentence a novice reads first.
 *
 * Everything here is derived from flows the rest of the app already observes. It never
 * starts work, and it never upgrades an unfinished component to "done" just because the
 * screen is open.
 */
object AutopilotStatus {

    fun build(
        bets: BestBetsUi,
        box: SandboxUi,
        totals: LearningTotals,
        feed: FeedStateEntity?,
        historyRows: Int,
    ): AutopilotUi {
        val realPriced = bets.picks.count { !it.prop.isFixture }
        val anyPriced = bets.picks.isNotEmpty()
        val syncedUtc = feed?.lastPropsSyncUtc ?: 0L
        val hasRealSync = syncedUtc > 0L
        val needsSetup = !hasRealSync && (bets.anyFixture || !anyPriced)

        val board = when {
            realPriced > 0 -> AutoStep(
                "Board",
                "$realPriced priced props from the live feed.",
                AutoStepState.DONE,
            )
            hasRealSync -> AutoStep(
                "Board",
                "Prices were synced, but nothing on today's board is usable yet.",
                AutoStepState.WORKING,
            )
            anyPriced -> AutoStep(
                "Board",
                "The board is synthetic test data. Add a read-only price key once and it will fill itself.",
                AutoStepState.NEEDS_YOU,
            )
            else -> AutoStep(
                "Board",
                "No prices yet. Add a read-only price key once; after that the app syncs on its own.",
                AutoStepState.NEEDS_YOU,
            )
        }

        val liveFormula = box.live
        val formula = when {
            liveFormula != null -> AutoStep(
                "Formula",
                "${liveFormula.label} holds the live book after passing every check.",
                AutoStepState.DONE,
            )
            box.ready.isNotEmpty() -> AutoStep(
                "Formula",
                "${box.ready.size} candidate${if (box.ready.size == 1) "" else "s"} passed the gate; the live book is being chosen.",
                AutoStepState.WORKING,
            )
            box.considered > 0 -> AutoStep(
                "Formula",
                "Round ${box.round} ran, but nothing has passed every check yet. The search keeps going.",
                AutoStepState.WORKING,
            )
            else -> AutoStep(
                "Formula",
                "The first search starts on its own. It can also be started now.",
                AutoStepState.WAITING,
            )
        }

        val learning = when {
            totals.settled > 0 -> AutoStep(
                "Learning",
                "${totals.settled} finished result${if (totals.settled == 1) "" else "s"} have updated the model's correction.",
                AutoStepState.DONE,
            )
            totals.open > 0 -> AutoStep(
                "Learning",
                "${totals.open} bet${if (totals.open == 1) "" else "s"} are waiting for a result. Learning starts when they finish.",
                AutoStepState.WORKING,
            )
            else -> AutoStep(
                "Learning",
                "The next sync writes every priced prop to the learning ledger. No setup is needed.",
                AutoStepState.WORKING,
            )
        }

        val history = if (historyRows >= 400) {
            AutoStep(
                "Second test",
                "$historyRows imported results are checking learned rules before they reach the live book.",
                AutoStepState.DONE,
            )
        } else {
            AutoStep(
                "Second test",
                "Optional: import a past-results CSV to test learned rules against real history. " +
                    "Everything else works without it.",
                AutoStepState.WAITING,
            )
        }

        val top = bets.picks.firstOrNull()
        val livePicks = bets.picks.count { it.live }
        val (headline, detail) = when {
            needsSetup -> "One key and it runs itself" to
                "The app already schedules the rounds, learning and price refreshes. Add the price key once, " +
                "then leave it alone."
            bets.live && livePicks > 0 -> "Today's card is ready" to
                "$livePicks pick${if (livePicks == 1) "" else "s"} from ${bets.strategyLabel ?: "the live formula"}. " +
                "Prices refresh themselves every 30 minutes."
            bets.live -> "The formula is live, with no pick yet" to
                "It is checking today's prices and will take a pick only when one clears its bar. No action needed."
            bets.hasFormula && bets.picks.isNotEmpty() -> "Still practising — and watching this board" to
                "These are try-out picks. The best candidate keeps testing until it earns the live book."
            else -> "The search is running" to
                "No formula has earned the live book yet. The app keeps searching and learning in the background."
        }

        return AutopilotUi(
            headline = headline,
            detail = detail,
            steps = if (historyRows >= 400) listOf(board, formula, learning, history) else listOf(board, formula, learning),
            needsSetup = needsSetup,
            live = bets.live,
            topPickId = top?.prop?.id,
            topPickLabel = top?.let { "${it.prop.playerName} ${it.side.label} ${it.prop.market}" },
            lastRunUtc = maxOf(box.lastRunUtc, syncedUtc),
        )
    }
}
