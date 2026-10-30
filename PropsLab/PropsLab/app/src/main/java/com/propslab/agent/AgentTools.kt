package com.propslab.agent

import com.propslab.domain.Odds
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.Pricing
import com.propslab.domain.Side
import com.propslab.domain.Staking

/**
 * The six tools the local model may call.
 *
 * Two of them can change state: `stage_ticket` (creates a proposal) and
 * `confirm_ticket` (turns a confirmed proposal into a paper bet). Both write audit
 * events. `submit_live_wager` exists so the model has a truthful answer when asked
 * to place a real bet: it is always [ToolResult.Blocked], whatever the arguments.
 *
 * Reads are not audited; the audit trail records decisions, not lookups.
 */
object AgentTools {

    /** One paper bet larger than this is refused at the tool boundary. */
    const val MAX_STAKE_UNITS = 10.0

    internal const val LIVE_WAGER_REFUSAL =
        "Real wagers are not placed from Cheat Code. DraftKings, PrizePicks and Fanatics publish no public " +
            "wager API, their terms prohibit automated or bot wagering, and this app only reads prices " +
            "and box scores. Call stage_ticket instead and let the user confirm it as a paper bet."

    fun create(
        data: AgentData,
        systems: SystemPort? = null,
        clock: () -> Long = System::currentTimeMillis,
    ): ToolRegistry {
        val stager = TicketStager(clock)
        val tools = buildList {
            add(ListProps(data))
            add(GetEdge(data))
            add(StageTicket(data, stager))
            add(ConfirmTicket(data, stager))
            add(OpenBook(data))
            add(SubmitLiveWager(data))
            // The system tools only exist when the app can actually perform the operation,
            // so a caller that hands in nothing cannot be told a round was run.
            systems?.let { addAll(SystemTools.all(it)) }
        }
        return ToolRegistry(tools, data)
    }
}

/** The three stored chances, or null when any is missing or they do not form a distribution. */
private fun AgentProp.probabilitiesOrNull(): OutcomeProbabilities? {
    val over = pOver ?: return null
    val push = pPush ?: return null
    val under = pUnder ?: return null
    return runCatching { OutcomeProbabilities(over, push, under) }.getOrNull()
}

private class ListProps(private val data: AgentData) : AgentTool {
    override val spec = ToolSpec(
        name = "list_props",
        capability = ToolCapability.READ,
        summary = "List the props Cheat Code currently holds, with their lines and quoted prices.",
        params = listOf(
            ParamSpec(
                name = "status",
                type = "string",
                description = "available (default) lists only usable props; all also lists props whose gate rejected them.",
                required = false,
                allowed = listOf("available", "all"),
            ),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        val status = args["status"]?.trim()?.lowercase() ?: "available"
        if (status != "available" && status != "all") {
            return ToolResult.Rejected("status must be 'available' or 'all'")
        }
        val props = data.props()
            .filter { status == "all" || it.available }
            .sortedBy { it.startUtc }
            .take(25)
        if (props.isEmpty()) return ToolResult.Ok("No props match.")
        val lines = props.joinToString("\n") { p ->
            val line = p.line?.let { "%.1f".format(it) } ?: "no line"
            val prices = if (p.overOdds != null && p.underOdds != null) {
                "${plain(p.overOdds)} / ${plain(p.underOdds)}"
            } else "no price"
            val model = if (!p.available) "unavailable"
                else p.pOver?.let { "%.1f%% over".format(it * 100.0) } ?: "probability missing"
            val source = if (p.isFixture) " fixture" else ""
            "${p.id} | ${p.playerName} (${p.teamCode}) | ${p.matchup} | line $line | $prices | $model$source"
        }
        return ToolResult.Ok(lines)
    }

    private fun plain(odds: Int) = if (odds > 0) "+$odds" else "$odds"
}

private class GetEdge(private val data: AgentData) : AgentTool {
    override val spec = ToolSpec(
        name = "get_edge",
        capability = ToolCapability.READ,
        summary = "Show the no-vig market price, the model's edge, the expected value and the Kelly fraction for one prop.",
        params = listOf(
            ParamSpec(
                "predictionId",
                "string",
                "The prop id, e.g. pred-aldana. Omit to use today's top pick.",
                required = false,
            ),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        val id = args.arg("predictionId") ?: return ToolResult.Rejected("predictionId is required")
        val prop = data.props().firstOrNull { it.id == id }
            ?: return ToolResult.Rejected("no prop with id '$id'")
        if (!prop.available) return ToolResult.Rejected("'$id' is unavailable: it has no usable line, price or probability")
        val over = prop.overOdds ?: return ToolResult.Rejected("no over price is quoted for '$id'")
        val under = prop.underOdds ?: return ToolResult.Rejected("no under price is quoted for '$id'")
        val probs = prop.probabilitiesOrNull()
            ?: return ToolResult.Rejected("stored probabilities for '$id' are not a distribution")
        val line = prop.line ?: return ToolResult.Rejected("no line is stored for '$id'")

        val marketOver = Odds.noVigOver(over, under)
        val hold = Pricing.hold(over, under)
        val edge = Pricing.edgePoints(probs.overGivenNoPush, marketOver)
        val side = if (edge >= 0) Side.OVER else Side.UNDER
        val price = prop.oddsFor(side)
            ?: return ToolResult.Rejected("no price is quoted for the ${side.label}")
        val pWin = probs.forSide(side)
        val ev = Staking.expectedValue(pWin, probs.push, price, 1.0)
        val kelly = Staking.kellyFraction(pWin, probs.push, price)

        return ToolResult.Ok(
            buildString {
                append(prop.playerName).append(" · ").append(prop.matchup).append(" · line ")
                append("%.1f".format(line)).append('\n')
                append("model: over ").append("%.1f%%".format(probs.over * 100))
                append(", push ").append("%.1f%%".format(probs.push * 100))
                append(", under ").append("%.1f%%".format(probs.under * 100))
                append(" (no-push over ").append("%.1f%%".format(probs.overGivenNoPush * 100)).append(")\n")
                append("market: no-vig over ").append("%.1f%%".format(marketOver * 100))
                append(", hold ").append("%.2f%%".format(hold * 100)).append('\n')
                append("edge: ").append("%+.1f".format(edge)).append(" points on ").append(side.label).append('\n')
                append("EV at ").append(if (price > 0) "+$price" else "$price")
                append(" for 1.0 unit: ").append("%+.3f".format(ev)).append('\n')
                append("Kelly fraction: ").append("%.3f".format(kelly))
                append(" (").append("%.1f%%".format(kelly * 100)).append(" of bankroll)")
                if (prop.isFixture) append("\nsource: synthetic fixture")
            },
        )
    }
}

private class StageTicket(
    private val data: AgentData,
    private val stager: TicketStager,
) : AgentTool {
    override val spec = ToolSpec(
        name = "stage_ticket",
        capability = ToolCapability.STAGE,
        summary = "Propose a paper ticket for the user to review. Staging places nothing; the user must confirm.",
        params = listOf(
            ParamSpec("predictionId", "string", "The prop id to stage. Omit for today's top pick.", required = false),
            ParamSpec("side", "string", "Which side to take. Omit for that pick's side.", required = false, allowed = listOf("OVER", "UNDER")),
            ParamSpec(
                "book",
                "string",
                "Which book the user should be sent to. Omit for the top pick's book.",
                required = false,
                allowed = Sportsbook.entries.map { it.slug },
            ),
            ParamSpec(
                "stakeUnits",
                "number",
                "Stake in paper units, up to ${AgentTools.MAX_STAKE_UNITS}. Omit for 1.0.",
                required = false,
            ),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        val id = args.arg("predictionId") ?: return ToolResult.Rejected("predictionId is required")
        val prop = data.props().firstOrNull { it.id == id }
            ?: return ToolResult.Rejected("no prop with id '$id'")
        if (!prop.available) return ToolResult.Rejected("'$id' is unavailable, so it cannot be staged")

        val sideRaw = args.arg("side")?.uppercase() ?: return ToolResult.Rejected("side is required")
        val side = Side.entries.firstOrNull { it.name == sideRaw }
            ?: return ToolResult.Rejected("side must be OVER or UNDER")
        val bookRaw = args.arg("book") ?: return ToolResult.Rejected("book is required")
        val book = Sportsbook.parse(bookRaw)
            ?: return ToolResult.Rejected("book must be one of ${Sportsbook.entries.joinToString { it.slug }}")

        val stake = args.arg("stakeUnits")?.toDoubleOrNull()
            ?: return ToolResult.Rejected("stakeUnits must be a number")
        if (!stake.isFinite() || stake <= 0.0) return ToolResult.Rejected("stakeUnits must be greater than zero")
        if (stake > AgentTools.MAX_STAKE_UNITS) {
            return ToolResult.Rejected("stakeUnits may not exceed ${AgentTools.MAX_STAKE_UNITS} units")
        }

        val odds = prop.oddsFor(side) ?: return ToolResult.Rejected("no price is quoted for the ${side.label}")
        val edge = edgeOf(prop)

        val ticket = stager.stage(
            predictionId = prop.id,
            playerName = prop.playerName,
            book = book,
            side = side,
            line = prop.line ?: return ToolResult.Rejected("no line is stored for '$id'"),
            odds = odds,
            stakeUnits = stake,
            edgePts = edge,
        )
        data.audit(
            "ticket_staged", ticket.id,
            "${ticket.playerName} ${side.label} ${ticket.line} at ${ticket.odds}, " +
                "%.1f".format(stake) + " units, ${book.displayName}, edge " +
                (edge?.let { "%+.1f pts".format(it) } ?: "unknown") + " — awaiting confirmation",
        )
        return ToolResult.Ok(
            "Staged ${ticket.id}: ${ticket.playerName} ${side.label} ${ticket.line} at ${ticket.odds}, " +
                "%.1f".format(stake) + " units on ${book.displayName}. Nothing has been placed; " +
                "the user must call confirm_ticket.",
        )
    }

    private fun edgeOf(prop: AgentProp): Double? {
        val over = prop.overOdds ?: return null
        val under = prop.underOdds ?: return null
        val probs = prop.probabilitiesOrNull() ?: return null
        return runCatching { Pricing.edgePoints(probs.overGivenNoPush, Odds.noVigOver(over, under)) }.getOrNull()
    }
}

private class ConfirmTicket(
    private val data: AgentData,
    private val stager: TicketStager,
) : AgentTool {
    override val spec = ToolSpec(
        name = "confirm_ticket",
        capability = ToolCapability.STAGE,
        summary = "Turn a staged ticket into a paper bet, after the user has said yes.",
        params = listOf(
            ParamSpec(
                "ticketId",
                "string",
                "The id returned by stage_ticket, e.g. tkt-0001. Omit to confirm the most recent ticket.",
                required = false,
            ),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        // "confirm" without an id means the ticket the app just staged. It is still one
        // ticket, still consumed once, and still only a paper bet.
        val id = args.arg("ticketId")
            ?: stager.pending().lastOrNull()?.id
            ?: return ToolResult.Rejected("there is no staged ticket to confirm")
        val ticket = stager.consume(id)
            ?: return ToolResult.Rejected("no staged ticket '$id' — stage it first")
        val outcome = data.placePaperBet(ticket.predictionId, ticket.side, ticket.stakeUnits)
        return when (outcome) {
            PlaceOutcome.PLACED -> {
                data.audit(
                    "ticket_confirmed", id,
                    "${ticket.playerName} ${ticket.side.label} ${ticket.line} at ${ticket.odds}, " +
                        "%.1f".format(ticket.stakeUnits) + " units recorded as a paper bet",
                )
                ToolResult.Ok(
                    "Confirmed ${id}. ${ticket.playerName} ${ticket.side.label} ${ticket.line} is now a paper " +
                        "bet of %.1f".format(ticket.stakeUnits) + " units. No real money moved.",
                )
            }
            PlaceOutcome.DUPLICATE -> ToolResult.Rejected(
                "a paper bet on ${ticket.playerName} ${ticket.side.label} already exists",
            )
            PlaceOutcome.UNAVAILABLE -> ToolResult.Rejected(
                "the prop behind ${id} is no longer available, so the ticket was not recorded",
            )
            PlaceOutcome.INSUFFICIENT_BANKROLL -> ToolResult.Rejected(
                "the practice bankroll is too low for this ticket; record a result or reset the practice account first",
            )
        }
    }
}

private class OpenBook(private val data: AgentData) : AgentTool {
    override val spec = ToolSpec(
        name = "open_book",
        capability = ToolCapability.HANDOFF,
        summary = "Send the user to a book's official site so they can review and place the pick themselves.",
        params = listOf(
            ParamSpec(
                "book",
                "string",
                "Which book to open. Omit for the top pick's book.",
                required = false,
                allowed = Sportsbook.entries.map { it.slug },
            ),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        val raw = args.arg("book") ?: return ToolResult.Rejected("book is required")
        val book = Sportsbook.parse(raw)
            ?: return ToolResult.Rejected("book must be one of ${Sportsbook.entries.joinToString { it.slug }}")
        data.audit("book_handoff", book.slug, "User handed off to ${book.displayName} for manual review")
        return ToolResult.Ok(
            "Open ${book.displayName} for the user: ${book.homeUrl}\n" +
                "There is no documented deep link that prefills a bet slip, so the pick is reviewed by hand.",
        )
    }
}

private class SubmitLiveWager(private val data: AgentData) : AgentTool {
    override val spec = ToolSpec(
        name = "submit_live_wager",
        capability = ToolCapability.SUBMIT_LIVE,
        summary = "Place a real-money wager at a sportsbook. Declined in Cheat Code: use stage_ticket.",
        params = listOf(
            ParamSpec("book", "string", "Target book.", allowed = Sportsbook.entries.map { it.slug }),
            ParamSpec("side", "string", "Which side to take.", allowed = listOf("OVER", "UNDER")),
            ParamSpec("stakeUnits", "number", "Stake in real currency units."),
        ),
    )

    override suspend fun run(args: Map<String, String>): ToolResult {
        data.audit(
            "live_wager_blocked", args["book"]?.trim() ?: "unknown",
            "A real-money wager was attempted and refused by policy",
        )
        return ToolResult.Blocked(AgentTools.LIVE_WAGER_REFUSAL)
    }
}
