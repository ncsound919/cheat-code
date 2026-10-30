package com.propslab.agent

import com.propslab.data.AvailabilityStatus

/**
 * The system tools: the ones that let a conversation actually change something.
 *
 * They are split from the ticket tools on purpose. A ticket tool moves a *bet*, and needs a
 * human to confirm it. A system tool moves the *machine* — running the search, refreshing
 * the feed, recording a report — and every one of them writes an audit event, so a week
 * later the trail shows which sentence caused which round.
 */
object SystemTools {

    fun all(systems: SystemPort): List<AgentTool> = listOf(
        Status(systems), RunRound(systems), SyncFeed(systems), SetFormula(systems),
        NoteInjury(systems), Backtest(systems), Kaggle(systems),
    )
}

private class Status(private val systems: SystemPort) : AgentTool {
    override val spec = ToolSpec(
        name = "system_status",
        capability = ToolCapability.READ,
        summary = "Bankroll, the formula on the live book, what the board holds and what the feed last did.",
        params = emptyList(),
    )

    override suspend fun run(args: Map<String, String>) = ToolResult.Ok(systems.statusLine())
}

private class RunRound(private val systems: SystemPort) : AgentTool {
    override val spec = ToolSpec(
        name = "run_round",
        capability = ToolCapability.ACT,
        summary = "Run one sandbox round: gate every formula on a fresh simulated season and trade the live board.",
        params = emptyList(),
    )

    override suspend fun run(args: Map<String, String>) = ToolResult.Ok(systems.runRound())
}

private class SyncFeed(private val systems: SystemPort) : AgentTool {
    override val spec = ToolSpec(
        name = "sync_feed",
        capability = ToolCapability.ACT,
        summary = "Refresh prop prices, box scores and late news, settle the live book, and take any new picks.",
        params = emptyList(),
    )

    override suspend fun run(args: Map<String, String>) = ToolResult.Ok(systems.syncFeed())
}

private class SetFormula(private val systems: SystemPort) : AgentTool {
    override val spec = ToolSpec(
        name = "set_formula",
        capability = ToolCapability.ACT,
        summary = "Choose which formula holds the live book. Use `best` for the leading passing candidate or `none` to empty it.",
        params = listOf(
            ParamSpec(
                name = "formulaId",
                type = "string",
                description = "A formula id such as e3.0-both, or best, or none. Omit for best.",
                required = false,
            ),
            ParamSpec(
                name = "reason",
                type = "string",
                description = "Why the change was made. Recorded in the audit trail.",
                required = false,
            ),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        val target = args.arg("formulaId") ?: return ToolResult.Rejected("formulaId is required")
        return ToolResult.Ok(systems.setFormula(target.trim(), args.arg("reason").orEmpty()))
    }
}

private class NoteInjury(private val systems: SystemPort) : AgentTool {
    override val spec = ToolSpec(
        name = "note_injury",
        capability = ToolCapability.ACT,
        summary = "Record a late availability report for one player, which shifts the projection on the next sync.",
        params = listOf(
            ParamSpec("player", "string", "The player's name as it appears on the board."),
            ParamSpec(
                "status",
                "string",
                "What the report says.",
                allowed = AvailabilityStatus.ALL,
            ),
            ParamSpec("detail", "string", "The body part or the source, if known.", required = false),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        val player = args.arg("player") ?: return ToolResult.Rejected("player is required")
        val status = args.arg("status") ?: return ToolResult.Rejected("status is required")
        if (AvailabilityStatus.parse(status) == null) {
            return ToolResult.Rejected("status must be one of ${AvailabilityStatus.ALL.joinToString()}")
        }
        return ToolResult.Ok(systems.noteSignal(player, status, args.arg("detail").orEmpty()))
    }
}

private class Backtest(private val systems: SystemPort) : AgentTool {
    override val spec = ToolSpec(
        name = "backtest",
        capability = ToolCapability.READ,
        summary = "Run the whole formula grid against the imported dataset and report which formulas cleared the gate on real history.",
        params = emptyList(),
    )

    override suspend fun run(args: Map<String, String>) = ToolResult.Ok(systems.backtest())
}

private class Kaggle(private val systems: SystemPort) : AgentTool {
    override val spec = ToolSpec(
        name = "kaggle_experiment",
        capability = ToolCapability.ACT,
        summary = "Queue a training run on the wired Kaggle account and report its status. Needs an account and key saved first.",
        params = listOf(
            ParamSpec("note", "string", "What the experiment is for. Recorded with the run.", required = false),
        ),
    )

    override suspend fun run(args: Map<String, String>) =
        ToolResult.Ok(systems.kaggleExperiment(args.arg("note").orEmpty()))
}
