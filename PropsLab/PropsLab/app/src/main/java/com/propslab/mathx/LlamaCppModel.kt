package com.propslab.mathx

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The local model, actually running.
 *
 * Weights live in the app's own `files/models` directory and are read by a native library
 * built from llama.cpp at compile time. Nothing here reaches the network: the library has no
 * HTTP code compiled in, and the runtime opens exactly one file, the one the user put there.
 *
 * Failure is reported rather than papered over. A missing runtime, missing weights or a
 * refusal to load each produces a distinct message, because "the model is unavailable" is
 * useless and "no .gguf in /data/user/0/com.propslab/files/models" is not.
 */
class LlamaCppModel(
    private val modelsDir: File,
    private val adapterName: String? = DEFAULT_ADAPTER,
) : LocalMathModel {

    private val lock = Any()
    private var handle: Long = 0L

    /**
     * A base model is any GGUF that is not an adapter. Adapters cannot run on their own.
     *
     * The model shop writes `active.txt`; when it names an installed file, that is the one
     * loaded. The read is not cached, so switching models in the shop and calling [unload]
     * changes what the next question runs without restarting the app.
     */
    private val baseWeights: File?
        get() {
            val candidates = modelsDir.listFiles { file ->
                file.isFile && file.extension.equals("gguf", ignoreCase = true) &&
                    !file.name.contains("lora", ignoreCase = true)
            }.orEmpty().sortedBy { it.name }
            val active = runCatching {
                File(modelsDir, com.propslab.hub.ModelHub.ACTIVE_POINTER).readText().trim()
            }.getOrNull()
            return active?.let { name -> candidates.firstOrNull { it.name == name } }
                ?: candidates.firstOrNull { it.name == PREFERRED_MODEL }
                ?: candidates.firstOrNull()
        }

    private val adapter: File? by lazy {
        val wanted = adapterName ?: return@lazy null
        modelsDir.listFiles { file -> file.isFile && file.name == wanted }
            ?.firstOrNull()
    }

    override val displayName: String
        get() {
            val base = baseWeights?.nameWithoutExtension ?: return "Qwen3.5 4B (no weights yet)"
            val lora = adapter?.nameWithoutExtension ?: return base
            return "$base + $lora"
        }

    override fun status(): LocalModelStatus {
        if (!LlamaNative.ready) {
            return LocalModelStatus.NotInstalled(
                expectedPath = modelsDir.absolutePath,
                note = "The native runtime did not load on this device, so no model can run. " +
                    "The template explanations stay active.",
            )
        }
        val weights = baseWeights ?: return LocalModelStatus.NotInstalled(
            expectedPath = File(modelsDir, PREFERRED_MODEL).absolutePath,
            note = "No base .gguf was found in ${modelsDir.absolutePath}. Put a GGUF there and " +
                "the model loads on the next question. Adapters alone cannot run.",
        )
        if (!weights.canRead()) {
            return LocalModelStatus.NotInstalled(
                expectedPath = weights.absolutePath,
                note = "The weights are there but this app cannot read ${weights.absolutePath}.",
            )
        }
        return LocalModelStatus.Ready
    }

    override suspend fun complete(prompt: String, system: String, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val model = ensureLoaded()
            val chat = LlamaNative.prompt(model, system, prompt)
            // The watcher does one thing: when this caller is abandoned — a timeout, a screen
            // that went away — its finally line switches the native loop off. Without it a
            // discarded generation would run to the last token while every later question
            // queued behind it.
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    LlamaNative.cancel(model)
                }
            }
            try {
                // Sampling comes from the settings screen rather than literals: temperature,
                // top-k, top-p and the answer cap are the knobs, and this is the only place
                // they take effect. maxTokens is a cap on whatever the caller asked for, so a
                // short formula answer stays short while the console can still be tuned down.
                val s = com.propslab.settings.AppSettings.current
                LlamaNative.generate(
                    handle = model,
                    prompt = chat,
                    maxTokens = maxTokens.coerceIn(1, 4096).coerceAtMost(s.maxTokens),
                    temperature = s.temperature,
                    topK = s.topK,
                    topP = s.topP,
                    seed = -1,
                )
            } finally {
                watcher.cancel()
            }
        }

    /** Loads on the first question, off the main thread, and never twice. */
    private fun ensureLoaded(): Long = synchronized(lock) {
        if (handle != 0L) return handle
        val weights = baseWeights
            ?: error("no base .gguf in ${modelsDir.absolutePath}")
        val loaded = LlamaNative.load(
            modelPath = weights.absolutePath,
            loraPath = adapter?.absolutePath.orEmpty(),
            nCtx = com.propslab.settings.AppSettings.current.contextSize,
        )
        if (loaded == 0L) {
            val why = LlamaNative.lastError().ifBlank { "the runtime refused the file" }
            error(why)
        }
        handle = loaded
        handle
    }

    /** Frees the context and the mapped weights. Called when the process is done with them. */
    fun unload() {
        synchronized(lock) {
            if (handle != 0L) {
                LlamaNative.free(handle)
                handle = 0L
            }
        }
    }

    /** A non-fatal problem, e.g. an adapter that would not attach. Shown as-is. */
    fun warning(): String = if (LlamaNative.ready) LlamaNative.lastWarning() else ""

    companion object {
        const val PREFERRED_MODEL = "Qwen3.5-4B-Q4_K_M.gguf"

        /**
         * The adapter attached when one is present.
         *
         * The general-purpose adapter rather than the domain ones: this app writes prose
         * about odds, not code or lab notes, and an adapter chosen silently would be a
         * claim about what the weights do.
         */
        const val DEFAULT_ADAPTER = "qwen-mix-v2.lora.f16.gguf"
    }
}

/**
 * The JNI surface. Every call is guarded by [ready]: calling into a library that never
 * loaded is the one way this file can crash the app instead of reporting a problem.
 */
internal object LlamaNative {
    val ready: Boolean = runCatching { System.loadLibrary("propslab_llama") }.isSuccess

    external fun load(modelPath: String, loraPath: String, nCtx: Int): Long
    external fun prompt(handle: Long, system: String, user: String): String

    external fun generate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        seed: Int,
    ): String

    external fun free(handle: Long)
    external fun cancel(handle: Long)
    external fun lastError(): String
    external fun lastWarning(): String
    external fun version(): String
}
