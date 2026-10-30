package com.propslab

import com.propslab.agent.AgentContext
import com.propslab.agent.AgentData
import com.propslab.agent.AgentProp
import com.propslab.agent.AgentTool
import com.propslab.agent.AssistantPort
import com.propslab.agent.ConsoleEngine
import com.propslab.agent.ConsoleRouter
import com.propslab.agent.ConsoleTranscript
import com.propslab.agent.PlaceOutcome
import com.propslab.agent.Role
import com.propslab.agent.StrategyNotes
import com.propslab.agent.SystemPort
import com.propslab.agent.ToolCapability
import com.propslab.agent.ToolRegistry
import com.propslab.agent.ToolResult
import com.propslab.agent.ToolSpec
import com.propslab.agent.ParamSpec
import com.propslab.agent.SystemTools
import com.propslab.data.ConsoleTurn
import com.propslab.domain.Side
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The console's rules: an operation runs a tool, a question consults the written notes, and
 * anything else goes to the model and is recorded as something said rather than something
 * done. A test that only checked the happy path would miss the distinction, which is the
 * whole point of the screen.
 */
class ConsoleTest {

    private class FakeTranscript : ConsoleTranscript {
        val turns = mutableListOf<ConsoleTurn>()
        private var next = 1L

        override suspend fun append(turn: ConsoleTurn): Long {
            turns += turn
            return next++
        }

        override suspend fun recent(limit: Int): List<ConsoleTurn> = turns.takeLast(limit)

        override suspend fun clear() = turns.clear()
    }

    private class FakeAssistant(private val answer: String = "I don't have that figure.") : AssistantPort {
        var calls = 0
        var lastPrompt: String? = null
        override val label = "fake model"
        override suspend fun ask(system: String, prompt: String): String {
            calls++
            lastPrompt = prompt
            return answer
        }
    }

    private class FakeData(private val brief: String = "") : AgentData {
        override suspend fun props(): List<AgentProp> = emptyList()
        override suspend fun placePaperBet(predictionId: String, side: Side, stakeUnits: Double): PlaceOutcome =
            PlaceOutcome.PLACED
        override suspend fun audit(kind: String, subjectId: String, detail: String) = Unit
        override suspend fun toolContext(): AgentContext = AgentContext(brief)
    }

    private class FakeRun(private val text: String) : AgentTool {
        override val spec = ToolSpec("run_round", ToolCapability.ACT, "Runs a round.", emptyList())
        override suspend fun run(args: Map<String, String>): ToolResult = ToolResult.Ok(text)
    }

    /** A routed tool whose policy says no, which is what a refusal looks like in practice. */
    private class FakeBlocked : AgentTool {
        override val spec = ToolSpec(
            name = "run_round",
            capability = ToolCapability.ACT,
            summary = "Runs a round.",
            params = listOf(ParamSpec("rounds", "int", "How many rounds.", required = false)),
        )

        override suspend fun run(args: Map<String, String>): ToolResult =
            ToolResult.Blocked("the sandbox is paused by the user")
    }

    private fun engine(
        tools: List<AgentTool> = listOf(FakeRun("Round 7 ran.")),
        assistant: AssistantPort = FakeAssistant(),
        data: AgentData = FakeData(),
    ): Triple<ConsoleEngine, FakeTranscript, AssistantPort> {
        val transcript = FakeTranscript()
        val registry = ToolRegistry(tools, data)
        return Triple(ConsoleEngine(registry, transcript, assistant) { 1_700_000_000_000L }, transcript, assistant)
    }

    @Test
    fun anOperationRunsTheToolAndRecordsItsOwnReport() = runBlocking {
        val (engine, transcript, assistant) = engine()
        val turn = engine.submit("run a round")

        assertEquals("Round 7 ran.", turn.text)
        assertEquals("CALLED", turn.kind)
        assertEquals("run_round", turn.toolName)
        assertEquals("the model must not be consulted for an operation", 0, (assistant as FakeAssistant).calls)
        assertEquals(Role.USER, transcript.turns.first().role)
    }

    @Test
    fun aBlockedToolIsRecordedAsRefused() = runBlocking {
        val (engine, _, _) = engine(tools = listOf(FakeBlocked()))
        val turn = engine.submit("run a round")

        assertEquals("REFUSED", turn.kind)
        assertEquals("run_round", turn.toolName)
        assertTrue(turn.text.startsWith("Blocked:"))
    }

    @Test
    fun aStrategyQuestionReturnsTheWrittenNoteVerbatim() = runBlocking {
        val (engine, _, assistant) = engine()
        val turn = engine.submit("why kelly")
        val note = StrategyNotes.answer("why kelly")

        assertEquals("CONSULTED", turn.kind)
        assertEquals(note, turn.text)
        assertEquals("the note is not paraphrased by the model", 0, (assistant as FakeAssistant).calls)
    }

    @Test
    fun aNovicePhraseRoutesToTheToolThatAlreadyKnowsThePick() {
        val best = ConsoleRouter.route("show me the best pick") as ConsoleRouter.Intent.Call
        assertEquals("get_edge", best.tool)
        assertTrue(best.args.isEmpty())

        val stage = ConsoleRouter.route("stage the best pick") as ConsoleRouter.Intent.Call
        assertEquals("stage_ticket", stage.tool)
        assertTrue(stage.args.isEmpty())

        val confirm = ConsoleRouter.route("confirm") as ConsoleRouter.Intent.Call
        assertEquals("confirm_ticket", confirm.tool)
        assertTrue(confirm.args.isEmpty())
    }

    @Test
    fun helpListsTheOperationsWithoutCallingAnything() = runBlocking {
        val (engine, _, assistant) = engine()
        val turn = engine.submit("?")

        assertTrue(turn.text.contains("I can do these"))
        assertEquals(0, (assistant as FakeAssistant).calls)
    }

    @Test
    fun anUnknownLineGoesToTheModelAndIsMarkedAsSaid() = runBlocking {
        val (engine, _, assistant) = engine(assistant = FakeAssistant("Ayton has been fine at the line."))
        val turn = engine.submit("what do you make of Ayton's free throws")

        assertEquals("SPOKEN", turn.kind)
        assertEquals("Ayton has been fine at the line.", turn.text)
        assertEquals(1, (assistant as FakeAssistant).calls)
    }

    @Test
    fun theModelPromptCarriesTheLiveAppState() = runBlocking {
        val assistant = FakeAssistant("Use the top pick.")
        val (engine, _, _) = engine(
            assistant = assistant,
            data = FakeData("Top pick: Dev Aldana over 26.5 points at -115, id pred-aldana."),
        )
        engine.submit("what do you make of Ayton tonight")

        assertTrue(assistant.lastPrompt.orEmpty().contains("Top pick: Dev Aldana"))
        assertTrue(assistant.lastPrompt.orEmpty().contains("Use those values"))
    }

    @Test
    fun aFailingModelIsReportedRatherThanSwallowed() = runBlocking {
        val broken = object : AssistantPort {
            override val label = "broken model"
            override suspend fun ask(system: String, prompt: String): String = error("no weights")
        }
        val (engine, _, _) = engine(assistant = broken)
        val turn = engine.submit("tell me something")

        assertEquals("SPOKEN", turn.kind)
        assertTrue(turn.text.contains("not answering"))
        assertTrue(turn.text.contains("no weights"))
    }

    @Test
    fun theSystemToolsAreRegisteredWhenTheAppSuppliesThem() {
        val systems = object : SystemPort {
            override suspend fun statusLine() = "ok"
            override suspend fun runRound() = "ok"
            override suspend fun syncFeed() = "ok"
            override suspend fun setFormula(target: String, reason: String) = "ok"
            override suspend fun noteSignal(player: String, status: String, detail: String) = "ok"
            override suspend fun backtest() = "ok"
            override suspend fun kaggleExperiment(note: String) = "ok"
        }
        val registry = ToolRegistry(SystemTools.all(systems), FakeData())
        assertEquals(
            listOf(
                "system_status", "run_round", "sync_feed", "set_formula",
                "note_injury", "backtest", "kaggle_experiment",
            ),
            registry.specs.map { it.name },
        )
    }
}
