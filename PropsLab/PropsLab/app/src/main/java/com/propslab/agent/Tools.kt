package com.propslab.agent

import com.propslab.domain.Side

/**
 * The tool surface a local model may propose.
 *
 * Cheat Code's rules are enforced by the tools themselves, not by the prompt: a model
 * reply can only change the world through [ToolRegistry.run], and every capability
 * other than READ goes through an explicit gate. The three sportsbooks publish no
 * public wager API, so SUBMIT_LIVE exists only to be refused honestly and loudly.
 *
 * Nothing here touches the network. The app's only network path is the read-only feed.
 */

/** What a tool is allowed to do to the world. */
enum class ToolCapability(val label: String) {
    /** Reads local state. No writes, no side effects. */
    READ("read"),

    /** Proposes a ticket that a human must still confirm. */
    STAGE("stage"),

    /**
     * Changes local state: runs a round, syncs the read-only feed, records a signal.
     * An ACT tool can cost credits or move the bankroll, but it can never reach a book.
     */
    ACT("act"),

    /** Hands the user to a book's official site so they can act there themselves. */
    HANDOFF("handoff"),

    /** Would place a real-money wager. Always refused. */
    SUBMIT_LIVE("submit-live"),
}

data class ParamSpec(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true,
    /** Closed set of accepted values. Empty means any value of [type] is accepted. */
    val allowed: List<String> = emptyList(),
) {
    fun accepts(value: String): Boolean = allowed.isEmpty() || allowed.any { it.equals(value, ignoreCase = true) }
}

data class ToolSpec(
    val name: String,
    val capability: ToolCapability,
    val summary: String,
    val params: List<ParamSpec>,
) {
    /**
     * Serialised for the model's prompt. Built by hand so no JSON library is needed,
     * and so a value in [summary] or [params] can never break out of its string.
     */
    fun schemaJson(): String = buildString {
        append("{\"name\":\"").append(escape(name)).append('"')
        append(",\"capability\":\"").append(capability.label).append('"')
        append(",\"description\":\"").append(escape(summary)).append('"')
        append(",\"parameters\":{")
        params.forEachIndexed { index, param ->
            if (index > 0) append(',')
            append('"').append(escape(param.name)).append("\":{\"type\":\"").append(escape(param.type)).append('"')
            if (param.required) append(",\"required\":true")
            if (param.allowed.isNotEmpty()) {
                append(",\"enum\":[")
                param.allowed.forEachIndexed { i, value -> if (i > 0) append(','); append('"').append(escape(value)).append('"') }
                append(']')
            }
            append(",\"description\":\"").append(escape(param.description)).append('"')
            append('}')
        }
        append("}}")
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}

sealed interface ToolResult {
    val text: String

    data class Ok(override val text: String) : ToolResult

    /** The arguments were wrong, or the state they name does not exist. Safe to retry. */
    data class Rejected(val reason: String) : ToolResult {
        override val text: String get() = "Rejected: $reason"
    }

    /** Policy refused the call. Retrying with different arguments will not help. */
    data class Blocked(val reason: String) : ToolResult {
        override val text: String get() = "Blocked: $reason"
    }
}

/** Minimal projection of a prop the tools are allowed to see. */
data class AgentProp(
    val id: String,
    val playerName: String,
    val teamCode: String,
    val matchup: String,
    val startUtc: Long,
    val market: String,
    val book: String,
    val line: Double?,
    val overOdds: Int?,
    val underOdds: Int?,
    val pOver: Double?,
    val pPush: Double?,
    val pUnder: Double?,
    val isFixture: Boolean,
) {
    val available: Boolean
        get() = line != null && overOdds != null && underOdds != null &&
            pOver != null && pPush != null && pUnder != null

    fun oddsFor(side: Side): Int? = if (side == Side.OVER) overOdds else underOdds
}

enum class PlaceOutcome { PLACED, DUPLICATE, UNAVAILABLE }

/**
 * The live state a tool may use to fill arguments the model left out.
 *
 * The model still decides whether to call a tool; this only supplies the facts the app
 * already knows — today's top pick, its side, the supported book and the paper stake.
 * An argument the caller actually supplied always wins.
 */
data class AgentContext(
    val brief: String = "",
    val defaults: Map<String, Map<String, String>> = emptyMap(),
)

/**
 * The only port the tools use to reach the app. Implemented by PropsRepository in
 * production and by a fake in tests, so the policy can be exercised without Room.
 */
interface AgentData {
    suspend fun props(): List<AgentProp>

    /** Context and auto-filled arguments. A fake that does not need either keeps the default. */
    suspend fun toolContext(): AgentContext = AgentContext()

    /** Records a paper bet. This is the settlement path; it is never a real wager. */
    suspend fun placePaperBet(predictionId: String, side: Side, stakeUnits: Double): PlaceOutcome

    suspend fun audit(kind: String, subjectId: String, detail: String)
}

data class StagedTicket(
    val id: String,
    val predictionId: String,
    val playerName: String,
    val book: Sportsbook,
    val side: Side,
    val line: Double,
    val odds: Int,
    val stakeUnits: Double,
    val edgePts: Double?,
    val createdUtc: Long,
)

/**
 * Holds tickets between staging and human confirmation. Deliberately in-memory:
 * a ticket is a proposal, not a record. The durable record is the paper bet that
 * confirmation writes and the audit event that accompanies it.
 */
class TicketStager(private val clock: () -> Long) {
    private val tickets = LinkedHashMap<String, StagedTicket>()
    private var sequence = 0

    val pendingCount: Int get() = tickets.size

    fun stage(
        predictionId: String,
        playerName: String,
        book: Sportsbook,
        side: Side,
        line: Double,
        odds: Int,
        stakeUnits: Double,
        edgePts: Double?,
    ): StagedTicket {
        sequence += 1
        val ticket = StagedTicket(
            id = "tkt-%04d".format(sequence),
            predictionId = predictionId,
            playerName = playerName,
            book = book,
            side = side,
            line = line,
            odds = odds,
            stakeUnits = stakeUnits,
            edgePts = edgePts,
            createdUtc = clock(),
        )
        tickets[ticket.id] = ticket
        return ticket
    }

    fun pending(): List<StagedTicket> = tickets.values.toList()

    /** Removes and returns the ticket, or null when it never existed or was already used. */
    fun consume(id: String): StagedTicket? = tickets.remove(id)
}

interface AgentTool {
    val spec: ToolSpec
    suspend fun run(args: Map<String, String>): ToolResult
}

class ToolRegistry(
    private val tools: List<AgentTool>,
    private val data: AgentData,
) {
    val specs: List<ToolSpec> get() = tools.map { it.spec }

    /** Every schema, for a prompt that wants to show the model what it may call. */
    fun schemasJson(): String = specs.joinToString(",", prefix = "[", postfix = "]") { it.schemaJson() }

    /** The current board, formula and bankroll in one paragraph, for the model's prompt. */
    suspend fun briefing(): String = runCatching { data.toolContext().brief }.getOrDefault("")

    /**
     * Defaults are looked up only for the tool being run. A supplied value always wins,
     * so a learner can override the auto-pick by naming a different prop, side or stake.
     */
    suspend fun run(name: String, args: Map<String, String> = emptyMap()): ToolResult {
        val tool = tools.firstOrNull { it.spec.name == name }
        if (tool == null) {
            data.audit("tool_unknown", name, "The model called a tool that does not exist")
            return ToolResult.Rejected("unknown tool '$name'")
        }
        val supplied = args.filterValues { it.isNotBlank() }
        val defaults = runCatching { data.toolContext().defaults[name].orEmpty() }.getOrDefault(emptyMap())
        return tool.run(defaults + supplied)
    }
}

internal fun Map<String, String>.arg(name: String): String? = this[name]?.trim()?.takeIf { it.isNotEmpty() }
