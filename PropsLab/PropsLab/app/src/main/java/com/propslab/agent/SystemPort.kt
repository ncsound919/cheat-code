package com.propslab.agent

/**
 * The system functions the console may call.
 *
 * Every method returns the sentence a person reads, because the reply *is* the record: the
 * console shows exactly what the operation reported, with no second layer of paraphrase
 * that could drift from what actually happened. Implemented by the repository in production
 * and by a fake in tests.
 */
interface SystemPort {

    suspend fun statusLine(): String

    /** Runs one sandbox round: gate the grid on a fresh season, then trade the live board. */
    suspend fun runRound(): String

    /** Refreshes prices, box scores and late news, then settles the live book. */
    suspend fun syncFeed(): String

    /**
     * @param target a formula id (`e3.0-both`), `best` for the leading passing candidate,
     *   or `none` to empty the live book.
     */
    suspend fun setFormula(target: String, reason: String = ""): String

    suspend fun noteSignal(player: String, status: String, detail: String): String

    /** Tests the formula grid against the imported dataset instead of a simulated season. */
    suspend fun backtest(): String

    suspend fun kaggleExperiment(note: String): String
}

/** What the console offers, written once so the help text and the model prompt agree. */
object ConsoleCatalogue {
    val COMMANDS: List<Pair<String, String>> = listOf(
        "status" to "bankroll, the live formula, and what the board holds",
        "show the best pick" to "read the top pick's edge, price and paper stake",
        "stage the best pick" to "write a paper ticket for the top pick, ready to confirm",
        "confirm" to "record the staged paper ticket",
        "open draftkings" to "open the book's own page for a manual wager",
        "run a round" to "search the whole formula grid on a fresh simulated season",
        "sync" to "pull prices, box scores and late news, then settle the live book",
        "promote e3.0-both" to "put a named formula on the live book (or `promote none`)",
        "injury Jokic doubtful" to "record a late availability report by hand",
        "backtest" to "run the grid against the dataset you imported",
        "kaggle train" to "queue a high-compute experiment on the wired account",
        "what is the app doing" to "see the automatic price, formula and learning steps",
        "why kelly" to "consult the strategy notes",
    )

    fun help(): String = buildString {
        append("I can do these, and nothing else:\n")
        COMMANDS.forEach { (command, meaning) -> append("· ").append(command).append(" — ").append(meaning).append('\n') }
        append("\nEvery reply you see is the operation's own report, not a retelling.")
    }
}
