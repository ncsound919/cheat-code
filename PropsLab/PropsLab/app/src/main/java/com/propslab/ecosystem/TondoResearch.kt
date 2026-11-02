package com.propslab.ecosystem

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Tondo Research Assist — CheatCode's copy of the shared contract.
 *
 * This app is still on Kotlin 2.0, so it cannot consume the shared kotlin-core
 * artifact compiled with 2.2. The wire format is identical; keep this in step
 * with shared/kotlin-core/src/main/java/com/overlay/tondo/TondoResearch.kt.
 */

data class TondoResearchConfig(
    val baseUrl: String,
    val appId: String,
    val token: String,
    val timeoutMs: Int = 60_000,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && appId.isNotBlank() && token.isNotBlank()
}

data class TondoFinding(
    val claim: String,
    val support: String,
    val confidence: Double,
    val kind: String,
)

data class TondoAction(
    val step: String,
    val why: String,
    val risk: String,
)

data class TondoResearchResult(
    val status: String,
    val summary: String,
    val findings: List<TondoFinding>,
    val actions: List<TondoAction>,
    val unresolved: List<String>,
    val escalate: Boolean,
) {
    fun toPlainText(): String {
        val out = StringBuilder()
        out.append("Tondo ").append(status).append(": ").append(summary)
        for (finding in findings) {
            out.append("\n• ").append(finding.claim)
            out.append(" [").append(finding.support)
            out.append(", ").append((finding.confidence * 100).roundToInt()).append("%]")
        }
        for (action in actions) {
            out.append("\n→ ").append(action.step).append(" (").append(action.risk).append(" risk)")
        }
        if (unresolved.isNotEmpty()) {
            out.append("\nStill open: ").append(unresolved.joinToString("; "))
        }
        return out.toString()
    }
}

class TondoResearchException(
    message: String,
    val status: Int = 0,
    val code: String = "tondo_error",
) : Exception(message)

class TondoResearchClient(private val config: TondoResearchConfig) {

    val isConfigured: Boolean get() = config.isConfigured

    fun assist(
        operation: String,
        question: String,
        symptom: String = "",
        context: String = "",
        dataClass: String = "internal",
    ): TondoResearchResult {
        if (!config.isConfigured) {
            throw TondoResearchException("Tondo research is not configured", 0, "not_configured")
        }
        if (question.isBlank()) {
            throw TondoResearchException("question is required", 0, "question_required")
        }

        val body = JSONObject().apply {
            put("app", config.appId)
            put("operation", operation)
            put("question", question)
            if (symptom.isNotBlank()) put("symptom", symptom)
            if (context.isNotBlank()) put("context", context)
            put("data_class", dataClass)
            put("max_findings", 4)
        }
        return parse(request(body))
    }

    private fun parse(data: JSONObject): TondoResearchResult {
        val findings = mutableListOf<TondoFinding>()
        data.optJSONArray("findings")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                findings.add(
                    TondoFinding(
                        claim = item.optString("claim"),
                        support = item.optString("support", "model_prior"),
                        confidence = item.optDouble("confidence", 0.0),
                        kind = item.optString("kind", "fact"),
                    )
                )
            }
        }
        val actions = mutableListOf<TondoAction>()
        data.optJSONArray("actions")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                actions.add(
                    TondoAction(
                        step = item.optString("step"),
                        why = item.optString("why"),
                        risk = item.optString("risk", "medium"),
                    )
                )
            }
        }
        val unresolved = mutableListOf<String>()
        data.optJSONArray("unresolved")?.let { array ->
            for (i in 0 until array.length()) unresolved.add(array.optString(i))
        }
        return TondoResearchResult(
            status = data.optString("status", "insufficient"),
            summary = data.optString("summary"),
            findings = findings,
            actions = actions,
            unresolved = unresolved,
            escalate = data.optBoolean("escalate", false),
        )
    }

    private fun request(body: JSONObject): JSONObject {
        val connection = (URL(config.baseUrl.trimEnd('/') + "/v1/research/assist").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = minOf(config.timeoutMs, 10_000)
            readTimeout = config.timeoutMs
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-tondo-app", config.appId)
            setRequestProperty("Authorization", "Bearer ${config.token}")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input ->
                BufferedReader(InputStreamReader(input)).use { reader -> reader.readText() }
            } ?: ""
            val parsed = if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse {
                throw TondoResearchException("Tondo returned non-JSON", status, "invalid_json")
            }
            if (status !in 200..299) {
                val code = parsed.optString("error", "tondo_error")
                val detail = parsed.optString("detail")
                throw TondoResearchException(
                    if (detail.isNotBlank()) "$code: $detail" else code,
                    status,
                    code,
                )
            }
            return parsed
        } finally {
            connection.disconnect()
        }
    }
}
