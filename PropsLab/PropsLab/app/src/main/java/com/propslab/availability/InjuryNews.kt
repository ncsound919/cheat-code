package com.propslab.availability

import com.propslab.feed.HttpUrlConnectionFetcher
import com.propslab.feed.JsonFetcher
import com.propslab.feed.OddsApi
import com.propslab.feed.safeGet
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Headline search for late availability news.
 *
 * This is a *hint channel*, and it is built to fail quietly: a news index can be minutes
 * behind a team's own injury report, so a headline never becomes a signal unless it
 * contains an explicit status phrase and names the player. Anything weaker is discarded
 * rather than softened into a guess.
 *
 * The transport is a plain GET with the token in the query string — the provider documents
 * every parameter as query-string or form-body — which keeps the app to one HTTP verb.
 */
class InjuryNews(
    private val fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
) {

    data class Hit(
        val title: String,
        val text: String,
        val url: String,
        val publishedUtc: Long,
    ) {
        val readable: String get() = if (text.isNotBlank()) text else title
    }

    data class SignalRead(val mention: InjuryParser.Mention, val hit: Hit)

    fun url(token: String, playerName: String, size: Int = 5): String {
        val query = "\"$playerName\" AND (injury OR injured OR questionable OR doubtful OR \"ruled out\") language:english"
        return "$BASE?token=${encode(token)}&format=json&size=${size.coerceIn(1, 25)}" +
            "&sort=crawled&q=${encode(query)}"
    }

    /** Null means "the provider answered but nothing usable was in it" — not an error. */
    suspend fun search(token: String, playerName: String, size: Int = 5): Result<List<Hit>> {
        val response = fetcher.safeGet(url(token, playerName, size))
            .getOrElse { return Result.failure(it) }
        if (!response.ok) {
            return Result.failure(
                IllegalStateException(
                    when (response.status) {
                        401, 403 -> "The news provider rejected the key"
                        429 -> "The news provider rate-limited the request"
                        else -> "The news provider returned HTTP ${response.status}"
                    },
                ),
            )
        }
        return Result.success(parse(response.body))
    }

    /**
     * The provider has shipped more than one envelope for this endpoint. Both are read, and
     * anything else yields no hits rather than an exception: an unreadable response must not
     * take the board down.
     */
    fun parse(body: String): List<Hit> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val array = sequenceOf(
            root.optJSONObject("posts")?.optJSONArray("results"),
            root.optJSONArray("results"),
            root.optJSONObject("data")?.optJSONArray("results"),
        ).firstOrNull { it != null } ?: return emptyList()
        return read(array)
    }

    private fun read(array: JSONArray): List<Hit> {
        val out = ArrayList<Hit>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val title = o.optString("title", "").trim()
            val text = o.optString("text", "").trim()
            if (title.isEmpty() && text.isEmpty()) continue
            out += Hit(
                title = title,
                text = text,
                url = o.optString("url", ""),
                publishedUtc = OddsApi.isoUtc(o.optString("published", o.optString("crawled", ""))),
            )
        }
        return out
    }

    /**
     * The strongest readable status among the hits, newest first. A mention tied to a hit
     * older than [maxAgeMs] is ignored: a report from yesterday is not a game-time report.
     */
    fun latestMention(hits: List<Hit>, playerName: String, nowUtc: Long, maxAgeMs: Long): SignalRead? {
        val fresh = hits.filter { it.publishedUtc <= 0L || nowUtc - it.publishedUtc <= maxAgeMs }
        for (hit in fresh.sortedByDescending { it.publishedUtc }) {
            val mention = InjuryParser.parse("${hit.title} ${hit.text}", playerName) ?: continue
            if (mention.confidence < 2) continue
            return SignalRead(mention, hit)
        }
        return null
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        const val BASE = "https://api.webz.io/filterWebContent"
    }
}
