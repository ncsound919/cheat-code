package com.propslab.agent

import com.propslab.data.ConsoleDao
import com.propslab.data.ConsoleTurn
import com.propslab.ecosystem.TondoResearchClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The agent console.
 *
 * Two things happen here and they stay apart on purpose:
 *
 *  1. **Routing.** A typed line goes through [ConsoleRouter], which reads it for a verb it
 *     knows and calls the matching tool on [ToolRegistry]. The console therefore holds the
 *     same powers as the model and no more: nothing here can reach a book, because no tool
 *     can.
 *
 *  2. **Talking.** A line the router does not claim is handed to [AssistantPort]. In the
 *     shipped app that port is the on-device model, so a conversation never leaves the phone;
 *     when no weights are installed it says so instead of inventing an answer.
 *
 * Every exchange is written to the transcript, and a routed call is written as the tool's
 * own report — not a paraphrase — so the record shows what actually ran.
 */

/**
 * Where the transcript lives. A port rather than the DAO itself, so the engine's routing and
 * refusal rules can be tested without a database.
 */
interface ConsoleTranscript {
    suspend fun append(turn: ConsoleTurn): Long
    suspend fun recent(limit: Int): List<ConsoleTurn>
    suspend fun clear()
}

/** The model, seen from the console's side. Implemented over the on-device runtime. */
interface AssistantPort {
    /** Shown on the console header, so the user knows who is answering. */
    val label: String

    suspend fun ask(system: String, prompt: String): String
}

object Role {
    const val USER = "user"
    const val ASSISTANT = "assistant"
}

/** How one line was answered. */
enum class TurnKind(val label: String) {
    CALLED("ran"),
    CONSULTED("notes"),
    SPOKEN("said"),
    REFUSED("refused"),
}

data class ConsoleTurnUi(
    val id: Long,
    val role: String,
    val kind: String,
    val text: String,
    val toolName: String?,
    val createdUtc: Long,
) {
    val fromUser: Boolean get() = role == Role.USER
    val refused: Boolean get() = kind == TurnKind.REFUSED.name
}

class ConsoleEngine(
    private val registry: ToolRegistry,
    private val transcript: ConsoleTranscript,
    private val assistant: AssistantPort,
    private val clock: () -> Long = System::currentTimeMillis,
    /** The bounded second opinion when the on-device model cannot answer. */
    private val research: TondoResearchClient? = null,
) {

    val modelLabel: String get() = assistant.label

    /** What a line would do, for the "will run" hint under the input. */
    fun preview(raw: String): String = ConsoleRouter.describe(ConsoleRouter.route(raw))

    suspend fun submit(raw: String): ConsoleTurnUi {
        val line = raw.trim()
        val now = clock()
        transcript.append(ConsoleTurn(role = Role.USER, kind = TurnKind.SPOKEN.name, text = line, createdUtc = now))

        val turn = when (val intent = ConsoleRouter.route(line)) {
            is ConsoleRouter.Intent.Call -> fromTool(intent)
            is ConsoleRouter.Intent.Consult -> consult(intent.question)
            ConsoleRouter.Intent.Help -> reply(TurnKind.SPOKEN, ConsoleCatalogue.help(), null)
            ConsoleRouter.Intent.Unknown -> spoken(line)
        }
        return persist(turn)
    }

    private suspend fun fromTool(intent: ConsoleRouter.Intent.Call): ConsoleTurn {
        val result = registry.run(intent.tool, intent.args)
        val kind = if (result is ToolResult.Blocked || result is ToolResult.Rejected) {
            TurnKind.REFUSED
        } else {
            TurnKind.CALLED
        }
        return reply(kind, result.text, intent.tool)
    }

    /**
     * The notes are returned as written.
     *
     * Every figure in them is one the app actually uses, so rephrasing them through a small
     * local model could only make the numbers less true. The model is offered for prose, not
     * for arithmetic.
     */
    private fun consult(question: String): ConsoleTurn {
        val note = StrategyNotes.answer(question)
            ?: return reply(
                TurnKind.SPOKEN,
                "I don't have a note on that. Ask about kelly, t-stat, ROI, drawdown, edge, the gate, " +
                    "the sample, fixtures, odds conversion, news, kaggle, or the backtest.",
                null,
            )
        return reply(TurnKind.CONSULTED, note, StrategyNotes.topic(question))
    }

    private suspend fun spoken(line: String): ConsoleTurn {
        val brief = runCatching { registry.briefing() }.getOrDefault("")
        val prompt = buildString {
            append("You are the Cheat Code console, running on the user's phone.\n")
            if (brief.isNotBlank()) {
                append("What the app knows right now: ").append(brief).append('\n')
                append("Use those values when the user says 'the best pick' or 'it'; never ask for them again.\n")
            }
            append("Operations the console can run, and the words that trigger them:\n")
            ConsoleCatalogue.COMMANDS.forEach { (command, meaning) ->
                append("- ").append(command).append(": ").append(meaning).append('\n')
            }
            append("Rules: never invent odds, lines, edges, injuries or news. If a number was not ")
            append("produced by a tool, say plainly that you do not have it. If the user asks for an ")
            append("operation, name the command that does it. Three sentences at most.\n")
            append("User: ").append(line).append('\n')
            append("Console:")
        }
        val local = runCatching { assistant.ask(ASSISTANT_RULES, prompt) }
        val localText = local.getOrNull()?.trim().orEmpty()
        if (localText.isNotEmpty()) return reply(TurnKind.SPOKEN, localText, null)

        // Only the question and the failure travel. No props, bets, bankroll or
        // transcript is sent; the gateway's "internal" class says as much.
        val assisted = research
            ?.takeIf { it.isConfigured }
            ?.let { client ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        client.assist(
                            operation = "propslab console",
                            question = line,
                            symptom = local.exceptionOrNull()?.let { failure ->
                                "the on-device model failed: ${failure.message ?: failure::class.simpleName}"
                            } ?: "the on-device model returned no text",
                            dataClass = "internal",
                        )
                    }
                }.getOrNull()
            }
        if (assisted != null) return reply(TurnKind.SPOKEN, assisted.toPlainText(), null)

        val answer = local.exceptionOrNull()?.let { failure ->
            "The on-device model is not answering (${failure.message ?: failure::class.simpleName}). " +
                "Operations still run: type `?` for the list."
        } ?: "The on-device model returned nothing. Type `?` for the operations I can run."
        return reply(TurnKind.SPOKEN, answer, null)
    }

    private fun reply(kind: TurnKind, text: String, tool: String?): ConsoleTurn =
        ConsoleTurn(
            role = Role.ASSISTANT,
            kind = kind.name,
            text = text,
            toolName = tool,
            createdUtc = clock(),
        )

    private suspend fun persist(turn: ConsoleTurn): ConsoleTurnUi {
        val id = transcript.append(turn)
        return ConsoleTurnUi(
            id = id,
            role = turn.role,
            kind = turn.kind,
            text = turn.text,
            toolName = turn.toolName,
            createdUtc = turn.createdUtc,
        )
    }

    suspend fun history(limit: Int = 80): List<ConsoleTurnUi> =
        transcript.recent(limit).reversed().map {
            ConsoleTurnUi(
                id = it.id,
                role = it.role,
                kind = it.kind,
                text = it.text,
                toolName = it.toolName,
                createdUtc = it.createdUtc,
            )
        }

    /** Destructive, so the screen asks first. */
    suspend fun clear() = transcript.clear()

    companion object {
        /**
         * The system prompt. It forbids invented figures because that is the one failure a
         * betting console must not have: a plausible number the system never computed.
         */
        const val ASSISTANT_RULES: String =
            "You are Cheat Code's on-device console. You never invent odds, lines, edges, injuries, " +
                "news or statistics. You only restate figures that appear in the user's app or in " +
                "the notes you are given. If you do not know, you say so in one sentence and name " +
                "the operation that would find out."
    }
}
