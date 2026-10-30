package com.propslab.mathx

/**
 * The only boundary a language model is allowed to cross in Cheat Code.
 *
 * Implementations must be on-device and must not use the network. The interface is
 * deliberately small so a llama.cpp or LiteRT-LM binding can be dropped in without
 * changing the assistant, the lab or the screens.
 */
interface LocalMathModel {
    val displayName: String

    fun status(): LocalModelStatus

    suspend fun complete(
        prompt: String,
        system: String = MathXPrompts.SYSTEM,
        maxTokens: Int = 512,
    ): String
}

sealed interface LocalModelStatus {
    /** Weights and runtime are loaded. */
    data object Ready : LocalModelStatus

    /** The app knows where weights belong, but none are installed. */
    data class NotInstalled(val expectedPath: String, val note: String) : LocalModelStatus

    /** A runtime exists but could not start. */
    data class Failed(val reason: String) : LocalModelStatus
}

/**
 * Honest default: Cheat Code ships without model weights. Explanations fall back to
 * [com.propslab.inference.TemplateAssistant], which is deterministic and offline.
 *
 * To go live, put an Apache-2.0 GGUF at [expectedPath] and provide a LocalMathModel
 * implementation backed by llama.cpp or LiteRT-LM. Inference stays on the device: the
 * app's only network permission exists for the read-only odds and box-score feed.
 */
class UninstalledLocalModel(
    private val expectedPath: String,
    override val displayName: String = "Qwen3 1.7B (not installed)",
) : LocalMathModel {

    override fun status(): LocalModelStatus = LocalModelStatus.NotInstalled(
        expectedPath = expectedPath,
        note = "No weights are bundled. Template explanations are active until a local runtime is installed.",
    )

    override suspend fun complete(prompt: String, system: String, maxTokens: Int): String =
        error("No local model is installed at $expectedPath")
}
