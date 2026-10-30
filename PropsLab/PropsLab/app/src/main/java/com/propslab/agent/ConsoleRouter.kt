package com.propslab.agent

import com.propslab.data.AvailabilityStatus
import java.util.Locale

/**
 * Turning a sentence into an operation.
 *
 * This is a router, not a mind. It reads a sentence for a verb it knows and the argument
 * that verb needs; when it is unsure it says so instead of guessing, because a misread
 * sentence that promotes the wrong formula is worse than a sentence the app admits it did
 * not understand. A local model, when one is installed, is allowed to *explain* the answer,
 * never to choose the operation.
 */
object ConsoleRouter {

    sealed interface Intent {
        /** Run one tool with these arguments. */
        data class Call(val tool: String, val args: Map<String, String>) : Intent

        /** Answer from the strategy notes. */
        data class Consult(val question: String) : Intent

        data object Help : Intent

        data object Unknown : Intent
    }

    private val FORMULA_ID = Regex("""\be\d+(?:\.\d+)?-(?:both|over|under)\b""", RegexOption.IGNORE_CASE)
    private val FORMULA_VERB = Regex(
        """(?:promote|use|set|switch to|lock in|put)\s+(?:the\s+)?(?:formula\s+)?(best|none|e\d+(?:\.\d+)?-(?:both|over|under))""",
        RegexOption.IGNORE_CASE,
    )
    private val INJURY_LINE = Regex(
        """(?:injury|injured|report|note)\s+(?:for\s+)?([a-z][a-z .'\-]{2,40}?)\s+(?:is\s+|was\s+|listed\s+as\s+)?(out|doubtful|questionable|probable|game[- ]time(?: decision)?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val STATUS_SENTENCE = Regex(
        """^([a-z][a-z .'\-]{2,40}?)\s+(?:is|was|listed\s+as|ruled|now)\s+(out|doubtful|questionable|probable)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val DETAIL = Regex("""\(([^)]{2,60})\)""")

    fun route(raw: String): Intent {
        val text = raw.trim()
        if (text.isEmpty()) return Intent.Help
        val lower = text.lowercase(Locale.ROOT)

        if (lower == "?" || lower in setOf("help", "commands", "what can you do", "what can i do")) {
            return Intent.Help
        }

        injury(text)?.let { return it }

        // These use the registry's live defaults: the top pick, its side and the 1-unit
        // paper stake. A model or a novice should not have to recite a prediction id.
        if (listOf("stage it", "stage that", "stage the best", "stage my best", "stage the top pick", "paper bet the best")
                .any { lower.contains(it) }
        ) {
            return Intent.Call("stage_ticket", emptyMap())
        }
        if (lower == "confirm" || lower == "confirm it" || lower == "confirm that" ||
            lower == "confirm the ticket" || lower == "confirm that ticket" ||
            lower == "confirm the bet" || lower == "confirm the pick" || lower == "confirm ticket"
        ) {
            return Intent.Call("confirm_ticket", emptyMap())
        }
        listOf("draftkings", "prizepicks", "fanatics").firstOrNull { lower.contains(it) }?.let { book ->
            if (lower.contains("open") || lower.contains("take me") || lower.contains("go to")) {
                return Intent.Call("open_book", mapOf("book" to book))
            }
        }

        // The best pick is a lookup, not a question. The tools already know today's top
        // pick, so "the best pick" should answer with it instead of asking for an id.
        if (listOf("best pick", "top pick", "best bet", "what's the best", "what is the best", "show me the best")
                .any { lower.contains(it) }
        ) {
            return Intent.Call("get_edge", emptyMap())
        }

        if (listOf("status", "bankroll", "how am i doing", "how's the book", "where do i stand", "report", "what is the app doing", "what's the app doing").any { lower.contains(it) }) {
            return Intent.Call("system_status", emptyMap())
        }

        FORMULA_VERB.find(text)?.let { match ->
            val target = match.groupValues[1]
            val detail = DETAIL.find(text)?.groupValues?.get(1).orEmpty()
            return Intent.Call("set_formula", buildMap {
                put("formulaId", target.lowercase(Locale.ROOT))
                if (detail.isNotBlank()) put("reason", detail)
            })
        }
        FORMULA_ID.find(text)?.let { match ->
            if (lower.contains("formula") || lower.contains("hold") || lower.contains("book")) {
                return Intent.Call("set_formula", mapOf("formulaId" to match.value.lowercase(Locale.ROOT)))
            }
        }

        if (lower.contains("sync") || lower.contains("refresh") ||
            listOf("update odds", "update prices", "pull odds", "fetch odds", "check news", "check the news").any { lower.contains(it) }
        ) {
            return Intent.Call("sync_feed", emptyMap())
        }

        if (lower.contains("backtest") || lower.contains("test the theor") || lower.contains("test my dataset") ||
            lower.contains("run the dataset") || lower.contains("test the grid")
        ) {
            return Intent.Call("backtest", emptyMap())
        }

        if (lower.contains("kaggle") || lower.contains("high compute") || lower.contains("train a model") ||
            lower.contains("run a notebook") || lower.contains("experiment")
        ) {
            return Intent.Call("kaggle_experiment", mapOf("note" to text.take(160)))
        }

        if (listOf("round", "search", "grid", "sandbox").any { lower.contains(it) } &&
            listOf("run", "new", "next", "start", "again", "search").any { lower.contains(it) }
        ) {
            return Intent.Call("run_round", emptyMap())
        }

        if (lower.contains("sync")) return Intent.Call("sync_feed", emptyMap())

        return if (StrategyNotes.answer(text) != null) Intent.Consult(text) else Intent.Unknown
    }

    /** "Jokic is doubtful (ankle)" and "injury for Jokic questionable" both land here. */
    private fun injury(text: String): Intent.Call? {
        val match = INJURY_LINE.find(text) ?: STATUS_SENTENCE.find(text) ?: return null
        val player = match.groupValues[1].trim().trimEnd('.', ',').trim()
        val statusRaw = match.groupValues[2].lowercase(Locale.ROOT)
        val status = when {
            statusRaw.startsWith("game") -> AvailabilityStatus.GAME_TIME
            else -> AvailabilityStatus.parse(statusRaw) ?: return null
        }
        if (player.isEmpty() || player.length < 3) return null
        // "the model is out of date" is not an injury report.
        if (player.split(' ').all { it in STOP_WORDS }) return null
        val detail = DETAIL.find(text)?.groupValues?.get(1).orEmpty()
        return Intent.Call(
            "note_injury",
            buildMap {
                put("player", player)
                put("status", status)
                if (detail.isNotBlank()) put("detail", detail)
            },
        )
    }

    private val STOP_WORDS = setOf(
        "it", "this", "that", "the", "he", "she", "they", "model", "formula", "board", "feed",
        "data", "price", "round", "sandbox", "bankroll", "system", "app",
    )

    /** How a routed call is shown in the transcript. */
    fun describe(intent: Intent): String = when (intent) {
        is Intent.Call -> intent.tool + "(" + intent.args.entries.joinToString { "${it.key}=${it.value}" } + ")"
        is Intent.Consult -> "notes on ${StrategyNotes.topic(intent.question) ?: "strategy"}"
        Intent.Help -> "help"
        Intent.Unknown -> "no match"
    }
}
