package com.propslab.agent

import java.util.Locale

/**
 * The strategy notes the console consults.
 *
 * Every answer is written down here rather than generated, and every number in them is one
 * the app actually uses. That matters more than it sounds: a betting app that explains
 * itself with invented statistics is worse than one that says nothing, so the model is only
 * ever allowed to rephrase these notes, never to add a figure to them.
 */
object StrategyNotes {

    private val TOPICS: List<Pair<List<String>, String>> = listOf(
        listOf("kelly", "stake", "sizing", "size") to
            "Kelly turns an edge and a price into a fraction of bankroll: f = (p·b − q)/b, where b is " +
                "the decimal odds minus 1. It is the growth-optimal stake *if* the probability is right, " +
                "and this app's probabilities are a baseline, not a proven edge — so the screen shows full " +
                "Kelly and a quarter of it, and the sandbox stakes a flat 1 unit precisely so two formulas " +
                "are compared on turnover rather than on a lucky early compounding.",

        listOf("t-stat", "t stat", "significance", "luck", "noise") to
            "The t-statistic is mean return per bet divided by its standard error. At roughly one unit of " +
                "variance per bet, a formula needs about 400 bets before t = 2.0 means anything, which is why " +
                "the gate demands both. A positive return with t below 2 is a story about variance; the gate " +
                "says so in those words rather than calling it an edge.",

        listOf("roi", "return", "yield") to
            "Return on stake is profit divided by amount staked. It is the number most easily gamed by " +
                "volume, so the gate pairs it with a bet floor (400), a drawdown ceiling (45%) and a hit-rate " +
                "check against the break-even the prices actually demanded.",

        listOf("drawdown", "risk", "ruin") to
            "Drawdown is measured from the running peak of the fake bankroll, and the gate refuses anything " +
                "worse than 45%. A run also stops at the 25-unit ruin floor instead of betting a bankroll that " +
                "is effectively gone. Both numbers are deliberately pessimistic: they are meant to make a " +
                "formula fail, not to make it look brave.",

        listOf("edge", "no-vig", "novig", "hold", "vig", "juice") to
            "Edge is the model's over probability minus the market's no-vig over probability, in points. The " +
                "no-vig price removes the book's margin from both sides so a 50/50 market reads as 50%, and " +
                "the hold is what was removed — the app prints it so a wide margin is visible instead of " +
                "being quietly priced in.",

        listOf("gate", "promote", "promotion", "retire", "sandbox", "ladder") to
            "A formula moves to the live book only after clearing five checks on a simulated season it has " +
                "not seen: at least 400 bets, ROI above 1.5%, t above 2.0, drawdown under 45%, and a hit rate " +
                "above the break-even its own prices demanded. Promotion never happens on real results, and " +
                "retirement does: after 150 settled real picks a losing record sends the formula back.",

        listOf("sample", "history", "games", "evidence", "baseline") to
            "A real estimate is published from at least 8 logged games and up to 20, as a normal " +
                "approximation with the spread floored at 30% of the mean. That is a baseline, not a model " +
                "with league-wide information: it knows the player, not the matchup, the pace or the scheme.",

        listOf("fixture", "fixtures", "synthetic", "simulation", "season") to
            "Fixtures are invented rows that exist so the interface has something to render before any key " +
                "is saved, and their results come from the sandbox oracle. The simulated season is rigged on " +
                "purpose (truth is 70% model, 30% market) so the formula *search* can be tested. Neither is " +
                "evidence about a real market, and the first successful sync retires the fixtures.",

        listOf("odds", "american", "decimal", "convert", "price") to
            "American odds convert to decimal as 1 + 100/|a| for negatives and 1 + a/100 for positives, and " +
                "implied probability is 1/decimal. Break-even at −110 is 52.4%; at +120 it is 45.5%. The " +
                "spread the app trades is the tiny gap between the model's number and the no-vig number.",

        listOf("news", "injury", "injuries", "signal", "late", "questionable", "doubtful", "out") to
            "A late report is a multiplier on the expected stat, never on the price: out withholds the " +
                "estimate, doubtful cuts it to 65%, questionable to 90%, a game-time decision to 95%. A signal " +
                "older than 18 hours is recorded but not applied. A report entered by hand always beats " +
                "anything a headline search found, and both are named on the row they changed.",

        listOf("kaggle", "compute", "train", "notebook", "experiment") to
            "Kaggle is the high-compute lane: the app can push a notebook to your account and report the " +
                "run's status, which is where a real model gets trained. Nothing is downloaded automatically. " +
                "Export predictions as CSV, import them, and the grid can be backtested on real history " +
                "instead of a simulated season.",

        listOf("backtest", "dataset", "theories", "csv", "history") to
            "A backtest needs three things per row: the two prices, the probability the model gave at the " +
                "time, and what actually happened. With those, the same grid and the same gate run over " +
                "history instead of a simulation — which is the only way to tell whether a formula's " +
                "promotion was about the market or about the simulator.",
    )

    /** The best-matching note, or null when the question matches no topic. */
    fun answer(question: String): String? {
        val lower = question.lowercase(Locale.ROOT)
        val scored = TOPICS.map { (keys, text) -> keys.count { lower.contains(it) } to text }
        val best = scored.maxByOrNull { it.first } ?: return null
        return if (best.first == 0) null else best.second
    }

    fun topic(question: String): String? {
        val lower = question.lowercase(Locale.ROOT)
        return TOPICS.firstOrNull { (keys, _) -> keys.any { lower.contains(it) } }?.first?.first()
    }

    /** Every percentage a note mentions, so a model rephrasing it may not invent another. */
    fun percents(note: String): List<Double> =
        Regex("""(\d+(?:\.\d+)?)\s*%""").findAll(note)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .toList()

    fun unmatched(): String =
        "I did not match that to anything I can do.\n\n" + ConsoleCatalogue.help()
}
