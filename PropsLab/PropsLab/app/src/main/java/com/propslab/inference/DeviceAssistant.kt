package com.propslab.inference

import com.propslab.agent.AssistantPort
import com.propslab.mathx.LocalMathModel
import com.propslab.mathx.LocalModelStatus

/**
 * The console's model, wired to whatever runtime the app has.
 *
 * It reports its own state instead of hiding it: with no weights installed there is no
 * conversation, only the honest note that the on-device model is missing. Operations are
 * unaffected, because they never go through the model at all.
 */
class DeviceAssistant(private val model: LocalMathModel) : AssistantPort {

    override val label: String
        get() = when (val status = model.status()) {
            LocalModelStatus.Ready -> "${model.displayName} · on device"
            is LocalModelStatus.NotInstalled -> "no weights · ${model.displayName}"
            is LocalModelStatus.Failed -> "failed · ${status.reason.take(60)}"
            else -> model.displayName
        }

    override suspend fun ask(system: String, prompt: String): String = model.complete(prompt, system)
}
