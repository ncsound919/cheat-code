package com.propslab.mathx

import com.propslab.billing.Feature
import com.propslab.billing.TierStore
import com.propslab.inference.CloudModel

/**
 * Local weights first, an optional cloud endpoint second.
 *
 * The order is not a preference, it is the product's privacy promise: a phone that has
 * weights installed never sends a prompt anywhere, even when a cloud key is saved. The
 * cloud is only reached when the local runtime has nothing to run, and only while Premium
 * is active — the fallback is a paid convenience, not a silent default.
 *
 * A failure in the cloud is not thrown: [MathXAssistant] already catches a failed
 * completion and falls back to the deterministic template, so the screen always says
 * something true.
 */
class HybridMathModel(
    private val local: LocalMathModel,
    private val cloudEnabled: () -> Boolean = { TierStore.allows(Feature.CLOUD_MODEL) && CloudModel.hasKey },
) : LocalMathModel {

    private val localReady: Boolean
        get() = local.status() is LocalModelStatus.Ready

    override val displayName: String
        get() = when {
            localReady -> local.displayName
            cloudEnabled() -> "${CloudModel.model} (cloud)"
            else -> local.displayName
        }

    override fun status(): LocalModelStatus {
        val localStatus = local.status()
        if (localStatus is LocalModelStatus.Ready) return localStatus
        return if (cloudEnabled()) {
            LocalModelStatus.Ready
        } else {
            localStatus
        }
    }

    override suspend fun complete(prompt: String, system: String, maxTokens: Int): String {
        if (localReady) return local.complete(prompt, system, maxTokens)
        val result = CloudModel.complete(system = system, prompt = prompt, maxTokens = maxTokens)
        return result.getOrThrow()
    }
}
