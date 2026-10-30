package com.propslab

import com.propslab.domain.OutcomeProbabilities
import com.propslab.inference.ExplainInput
import com.propslab.inference.TemplateAssistant
import com.propslab.mathx.AssistantGuard
import com.propslab.mathx.FormulaLab
import com.propslab.mathx.LocalMathModel
import com.propslab.mathx.LocalModelStatus
import com.propslab.mathx.MathXAssistant
import com.propslab.mathx.MathXMode
import com.propslab.mathx.MathXPrompts
import com.propslab.mathx.UninstalledLocalModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MathXTest {

    private val input = ExplainInput(
        player = "Test Player",
        line = 24.5,
        probabilities = OutcomeProbabilities(over = 0.58, push = 0.02, under = 0.40),
        marketOver = 0.55,
        flags = listOf("Minutes are stale"),
    )

    private class FakeModel(
        private val reply: String,
        private val state: LocalModelStatus = LocalModelStatus.Ready,
    ) : LocalMathModel {
        var lastPrompt: String? = null
        override val displayName = "Fake local model"
        override fun status(): LocalModelStatus = state
        override suspend fun complete(prompt: String, system: String, maxTokens: Int): String {
            lastPrompt = prompt
            return reply
        }
    }

    @Test fun everyFormulaCheckPasses() {
        val reports = FormulaLab.report()
        assertEquals(4, reports.size)
        for (report in reports) {
            val failed = report.results.filterNot { it.passed }.map { it.name }
            assertTrue("${report.spec.id} failed: $failed", report.allPassed)
        }
        assertTrue("the lab should run a meaningful number of checks", reports.sumOf { it.total } >= 12)
    }

    @Test fun modePrefixesAreThePortedMathXOnes() {
        assertTrue(MathXPrompts.prefix(MathXMode.FORMULA).startsWith("FORMULA LAB MODE"))
        assertTrue(MathXPrompts.prefix(MathXMode.PROBABILITY).startsWith("PROBABILITY MODE"))
        assertTrue(MathXPrompts.build("derive it", MathXMode.FORMULA).startsWith(MathXPrompts.prefix(MathXMode.FORMULA)))
        assertTrue(MathXPrompts.SYSTEM.contains("PROPSLAB GUARD"))
        assertFalse("the local layer must not mention Anthropic", MathXPrompts.SYSTEM.contains("Anthropic", ignoreCase = true))
    }

    @Test fun promptCarriesOnlyStoredFactsAndTheGuard() = runBlocking {
        val model = FakeModel("The model is 58.0% over against a 55.0% market, a 4.2 point edge.")
        MathXAssistant(model).explain(input)
        val prompt = model.lastPrompt.orEmpty()
        assertTrue(prompt.contains("FORMULA") || prompt.contains("PROBABILITY MODE"))
        assertTrue(prompt.contains("58.0%"))
        assertTrue(prompt.contains("55.0%"))
        assertTrue(prompt.contains("Do not calculate a new probability"))
        assertTrue(prompt.contains("Minutes are stale"))
    }

    @Test fun assistantUsesASafeLocalReply() = runBlocking {
        val reply = "The model is 58.0% over against a 55.0% market, a 4.2 point edge."
        val out = MathXAssistant(FakeModel(reply)).explain(input)
        assertEquals(reply, out)
    }

    @Test fun assistantRejectsAnInventedProbability() = runBlocking {
        val out = MathXAssistant(FakeModel("There is a 63% chance the over hits.")).explain(input)
        val fallback = TemplateAssistant().explain(input)
        assertEquals(fallback, out)
        assertFalse(out.contains("63%"))
    }

    @Test fun assistantFallsBackWithNoWeights() = runBlocking {
        val model = FakeModel(
            "This should never be used.",
            LocalModelStatus.NotInstalled("/data/models/qwen.gguf", "No weights"),
        )
        val out = MathXAssistant(model).explain(input)
        assertEquals(TemplateAssistant().explain(input), out)
        assertNull(model.lastPrompt)
    }

    @Test fun guardUsesAToleranceAndRejectsEmptyText() {
        assertTrue(AssistantGuard.isSafe("58.2% over", listOf(58.2)))
        assertTrue(AssistantGuard.isSafe("58.3% over", listOf(58.2), tolerance = 0.15))
        assertFalse(AssistantGuard.isSafe("58.4% over", listOf(58.2), tolerance = 0.15))
        assertTrue(AssistantGuard.isSafe("No percentages here.", listOf(58.2)))
        assertFalse(AssistantGuard.isSafe("   ", listOf(58.2)))
    }

    @Test fun uninstalledModelIsHonestAboutItsPath() {
        val model = UninstalledLocalModel("/data/user/0/com.propslab/files/models/qwen.gguf")
        val status = model.status() as LocalModelStatus.NotInstalled
        assertEquals("/data/user/0/com.propslab/files/models/qwen.gguf", status.expectedPath)
        assertTrue(status.note.contains("Template explanations"))
        assertThrows(IllegalStateException::class.java) {
            runBlocking { model.complete("anything") }
            Unit
        }
    }
}
