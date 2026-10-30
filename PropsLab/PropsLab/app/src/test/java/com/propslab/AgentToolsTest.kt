package com.propslab

import com.propslab.agent.AgentContext
import com.propslab.agent.AgentData
import com.propslab.agent.AgentProp
import com.propslab.agent.AgentTools
import com.propslab.agent.PlaceOutcome
import com.propslab.agent.Sportsbook
import com.propslab.agent.ToolCapability
import com.propslab.agent.ToolRegistry
import com.propslab.agent.ToolResult
import com.propslab.domain.Side
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The policy that makes a local model safe to hand tools to. The model may propose
 * anything; these tests pin down what the tools actually allow to happen.
 */
class AgentToolsTest {

    private class FakeData(
        private val props: List<AgentProp>,
        private val defaults: Map<String, Map<String, String>> = emptyMap(),
    ) : AgentData {
        val audits = mutableListOf<String>()
        val placed = mutableListOf<Triple<String, Side, Double>>()
        var nextOutcome = PlaceOutcome.PLACED

        override suspend fun props(): List<AgentProp> = props

        override suspend fun toolContext(): AgentContext =
            AgentContext("the top pick is pred-aldana", defaults)

        override suspend fun placePaperBet(predictionId: String, side: Side, stakeUnits: Double): PlaceOutcome {
            placed += Triple(predictionId, side, stakeUnits)
            return nextOutcome
        }

        override suspend fun audit(kind: String, subjectId: String, detail: String) {
            audits += "$kind|$subjectId|$detail"
        }

        fun kinds(): List<String> = audits.map { it.substringBefore("|") }
    }

    private fun prop(
        id: String = "pred-aldana",
        playerName: String = "Dev Aldana",
        over: Int? = -115,
        under: Int? = -105,
        pOver: Double? = 0.552,
        pPush: Double? = 0.0,
        pUnder: Double? = 0.448,
        line: Double? = 26.5,
        fixture: Boolean = true,
    ) = AgentProp(
        id = id,
        playerName = playerName,
        teamCode = "HRB",
        matchup = "HRB at LSV",
        startUtc = 1_000L,
        market = "Points",
        book = "Fixture book",
        line = line,
        overOdds = over,
        underOdds = under,
        pOver = pOver,
        pPush = pPush,
        pUnder = pUnder,
        isFixture = fixture,
    )

    /** Mirrors the `abara` fixture: no line, no price, so the gate rejects it. */
    private fun ungatedProp() = prop(id = "pred-abara", playerName = "Soren Abara", line = null, over = null, under = null)

    private fun registry(vararg props: AgentProp): Pair<ToolRegistry, FakeData> {
        val data = FakeData(props.toList())
        return AgentTools.create(data) { 1_700_000_000_000L } to data
    }

    @Test
    fun everyToolIsDescribedAndTyped() {
        val (tools, _) = registry(prop())
        assertEquals(
            listOf("list_props", "get_edge", "stage_ticket", "confirm_ticket", "open_book", "submit_live_wager"),
            tools.specs.map { it.name },
        )
        for (spec in tools.specs) {
            assertTrue("${spec.name} needs a summary", spec.summary.isNotBlank())
            for (param in spec.params) {
                assertTrue(param.name.isNotBlank())
                assertTrue(param.description.isNotBlank())
            }
            val json = spec.schemaJson()
            assertTrue(json.startsWith("{") && json.endsWith("}"))
            assertTrue(json.contains("\"name\":\"${spec.name}\""))
        }
        val submit = tools.specs.first { it.name == "submit_live_wager" }
        assertEquals(ToolCapability.SUBMIT_LIVE, submit.capability)
        val schemas = tools.schemasJson()
        assertTrue(schemas.startsWith("[") && schemas.endsWith("]"))
        assertTrue(schemas.contains("\"capability\":\"stage\""))
    }

    @Test
    fun liveWagerIsAlwaysBlockedWhateverTheArguments() = runBlocking {
        val (tools, data) = registry(prop())

        val full = tools.run(
            "submit_live_wager",
            mapOf("book" to "draftkings", "side" to "OVER", "stakeUnits" to "500"),
        )
        assertTrue("a full argument set must still be refused", full is ToolResult.Blocked)
        assertTrue(full.text.contains("no public"))
        assertTrue((full as ToolResult.Blocked).reason.contains("automated"))

        val empty = tools.run("submit_live_wager")
        assertTrue(empty is ToolResult.Blocked)

        assertTrue(data.placed.isEmpty())
        assertEquals(listOf("live_wager_blocked", "live_wager_blocked"), data.kinds())
    }

    @Test
    fun handoffOnlyEverReturnsAllowlistedUrls() = runBlocking {
        val (tools, data) = registry(prop())

        for (book in Sportsbook.entries) {
            val result = tools.run("open_book", mapOf("book" to book.slug))
            assertTrue(result is ToolResult.Ok)
            assertTrue(result.text.contains(book.homeUrl))
        }
        assertEquals(Sportsbook.entries.size, data.kinds().count { it == "book_handoff" })

        // The tool takes no url parameter at all, so a prompt-injected link cannot travel through it.
        val spec = tools.specs.first { it.name == "open_book" }
        assertEquals(listOf("book"), spec.params.map { it.name })

        val injected = tools.run("open_book", mapOf("book" to "https://evil.example/steal"))
        assertTrue(injected is ToolResult.Rejected)
        assertFalse(injected.text.contains("evil.example"))
    }

    @Test
    fun missingArgumentsComeFromTheLiveAppState() = runBlocking {
        val defaults = mapOf(
            "get_edge" to mapOf("predictionId" to "pred-aldana"),
            "stage_ticket" to mapOf(
                "predictionId" to "pred-aldana",
                "side" to "OVER",
                "book" to "draftkings",
                "stakeUnits" to "1.0",
            ),
        )
        val data = FakeData(listOf(prop()), defaults)
        val registry = AgentTools.create(data)

        val edge = registry.run("get_edge")
        assertTrue("expected the default prop, got $edge", edge is ToolResult.Ok)
        assertTrue(edge.text.contains("Dev Aldana"))
        assertTrue(registry.briefing().contains("pred-aldana"))

        val staged = registry.run("stage_ticket")
        assertTrue("expected a staged default ticket, got $staged", staged is ToolResult.Ok)
        assertTrue(staged.text.contains("Dev Aldana Over"))
        assertTrue(staged.text.contains("1.0 units"))

        // "confirm" with no id confirms the ticket that was just staged.
        val confirmed = registry.run("confirm_ticket")
        assertTrue("expected the staged ticket to confirm, got $confirmed", confirmed is ToolResult.Ok)
        assertEquals(listOf(Triple("pred-aldana", Side.OVER, 1.0)), data.placed)
    }

    @Test
    fun anExplicitArgumentBeatsTheDefault() = runBlocking {
        val other = prop(id = "pred-other", playerName = "Nico Brandt")
        val data = FakeData(
            listOf(prop(), other),
            mapOf("get_edge" to mapOf("predictionId" to "pred-aldana")),
        )
        val registry = AgentTools.create(data)

        val result = registry.run("get_edge", mapOf("predictionId" to "pred-other"))
        assertTrue(result is ToolResult.Ok)
        assertTrue(result.text.contains("Nico Brandt"))
    }

    @Test
    fun edgeMatchesTheHandCalculation() = runBlocking {
        val (tools, _) = registry(prop())
        val result = tools.run("get_edge", mapOf("predictionId" to "pred-aldana"))
        assertTrue(result is ToolResult.Ok)
        val text = result.text
        // no-vig over from -115 / -105 is 51.0834%; the model says 55.2%.
        assertTrue("market: $text", text.contains("51.1%"))
        assertTrue("hold: $text", text.contains("4.71%"))
        assertTrue("edge: $text", text.contains("+4.1 points on Over"))
        assertTrue("ev: $text", text.contains("EV at -115 for 1.0 unit: +0.032"))
        assertTrue("kelly: $text", text.contains("Kelly fraction: 0.037"))
        assertTrue(text.contains("synthetic fixture"))
    }

    @Test
    fun edgeIsRejectedForUnknownOrUnusableProps() = runBlocking {
        val (tools, _) = registry(prop(), ungatedProp())

        assertTrue(tools.run("get_edge") is ToolResult.Rejected)
        assertTrue(tools.run("get_edge", mapOf("predictionId" to "pred-nope")) is ToolResult.Rejected)
        assertTrue(tools.run("get_edge", mapOf("predictionId" to "pred-abara")) is ToolResult.Rejected)
    }

    @Test
    fun stagingRefusesBadArgumentsWithoutSideEffects() = runBlocking {
        val (tools, data) = registry(prop(), ungatedProp())

        val cases = listOf(
            emptyMap(),
            mapOf("predictionId" to "pred-nope", "side" to "OVER", "book" to "draftkings", "stakeUnits" to "1"),
            mapOf("predictionId" to "pred-abara", "side" to "OVER", "book" to "draftkings", "stakeUnits" to "1"),
            mapOf("predictionId" to "pred-aldana", "side" to "MIDDLE", "book" to "draftkings", "stakeUnits" to "1"),
            mapOf("predictionId" to "pred-aldana", "side" to "OVER", "book" to "https://evil.example", "stakeUnits" to "1"),
            mapOf("predictionId" to "pred-aldana", "side" to "OVER", "book" to "draftkings", "stakeUnits" to "abc"),
            mapOf("predictionId" to "pred-aldana", "side" to "OVER", "book" to "draftkings", "stakeUnits" to "0"),
            mapOf("predictionId" to "pred-aldana", "side" to "OVER", "book" to "draftkings", "stakeUnits" to "11"),
        )
        for (args in cases) {
            val result = tools.run("stage_ticket", args)
            assertTrue("expected rejection for $args, got $result", result is ToolResult.Rejected)
        }
        assertTrue(data.audits.isEmpty())
        assertTrue(data.placed.isEmpty())
    }

    @Test
    fun stagingThenConfirmationRecordsExactlyOnePaperBet() = runBlocking {
        val (tools, data) = registry(prop())

        val staged = tools.run(
            "stage_ticket",
            mapOf("predictionId" to "pred-aldana", "side" to "OVER", "book" to "prizepicks", "stakeUnits" to "1.5"),
        )
        assertTrue(staged is ToolResult.Ok)
        assertTrue(staged.text.contains("Nothing has been placed"))
        assertEquals(listOf("ticket_staged"), data.kinds())

        val confirmed = tools.run("confirm_ticket", mapOf("ticketId" to "tkt-0001"))
        assertTrue("expected confirmation, got $confirmed", confirmed is ToolResult.Ok)
        assertTrue(confirmed.text.contains("No real money moved"))
        assertEquals(listOf(Triple("pred-aldana", Side.OVER, 1.5)), data.placed)

        // The ticket is consumed: it cannot be confirmed twice.
        val again = tools.run("confirm_ticket", mapOf("ticketId" to "tkt-0001"))
        assertTrue(again is ToolResult.Rejected)
        assertEquals(1, data.placed.size)
        assertEquals(listOf("ticket_staged", "ticket_confirmed"), data.kinds())
    }

    @Test
    fun confirmationPropagatesARefusalFromTheLedger() = runBlocking {
        val (tools, data) = registry(prop())
        tools.run("stage_ticket", mapOf("predictionId" to "pred-aldana", "side" to "UNDER", "book" to "fanatics", "stakeUnits" to "2"))
        data.nextOutcome = PlaceOutcome.DUPLICATE

        val result = tools.run("confirm_ticket", mapOf("ticketId" to "tkt-0001"))
        assertTrue(result is ToolResult.Rejected)
        assertEquals(1, data.placed.size)
        assertFalse(data.kinds().contains("ticket_confirmed"))
    }

    @Test
    fun listPropsFiltersOutWhatTheGateRejected() = runBlocking {
        val (tools, _) = registry(prop(), ungatedProp())

        val available = tools.run("list_props")
        assertTrue(available.text.contains("pred-aldana"))
        assertFalse(available.text.contains("pred-abara"))

        val all = tools.run("list_props", mapOf("status" to "all"))
        assertTrue(all.text.contains("pred-abara"))
        assertTrue(all.text.contains("unavailable"))

        assertTrue(tools.run("list_props", mapOf("status" to "everything")) is ToolResult.Rejected)
    }

    @Test
    fun unknownToolsAreRejectedAndAudited() = runBlocking {
        val (tools, data) = registry(prop())
        val result = tools.run("place_real_money", mapOf("book" to "fanatics"))
        assertTrue(result is ToolResult.Rejected)
        assertEquals(listOf("tool_unknown"), data.kinds())
        assertTrue(data.placed.isEmpty())
    }
}
