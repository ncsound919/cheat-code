package com.propslab.availability

import com.propslab.data.AvailabilityStatus
import com.propslab.feed.HttpUrlConnectionFetcher
import com.propslab.feed.JsonFetcher
import com.propslab.feed.OddsApi
import com.propslab.feed.safeGet
import org.json.JSONObject
import java.util.Locale

/**
 * The league's own injury report.
 *
 * This is the free, keyless half of the late-availability story, and it is the half that
 * matters most before tip-off: a club's injury designation is the report everyone else is
 * quoting, including the beat writers a headline search finds an hour later. It is read
 * from the same public endpoint the league's own site uses, so no key can be revoked and
 * nothing is scraped out of a rendered page.
 *
 * Only a designation that actually changes a projection becomes a signal. A generic
 * "day-to-day" is read as questionable, a status the model already knows how to weigh; an
 * outlet's speculation never is, because it is not on this feed.
 */
class LeagueInjuries(
    private val fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
    /** The league whose report to read. The payload shape is the same for both sports. */
    private val url: String = URL,
) {

    /** One player's designation, reduced to what a projection can use. */
    data class Row(
        val playerName: String,
        val team: String,
        val status: String,
        val note: String,
        val observedUtc: Long,
    )

    /**
     * Reads the report.
     *
     * A failure is returned rather than swallowed, because "the injury wire is down" and
     * "nobody is hurt" must never read the same on screen.
     */
    suspend fun fetch(nowUtc: Long): Result<List<Row>> {
        val response = fetcher.safeGet(url).getOrElse { return Result.failure(it) }
        if (!response.ok) {
            return Result.failure(
                IllegalStateException(
                    when (response.status) {
                        429 -> "The league injury report rate-limited the request"
                        in 500..599 -> "The league injury report is unavailable (${response.status})"
                        else -> "The league injury report returned HTTP ${response.status}"
                    },
                ),
            )
        }
        return Result.success(parse(response.body, nowUtc))
    }

    fun parse(body: String, nowUtc: Long = System.currentTimeMillis()): List<Row> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val teams = root.optJSONArray("injuries") ?: return emptyList()
        val out = ArrayList<Row>()
        for (t in 0 until teams.length()) {
            val team = teams.optJSONObject(t) ?: continue
            val tag = team.optString("abbreviation", team.optString("displayName", "")).trim()
            val list = team.optJSONArray("injuries") ?: continue
            for (i in 0 until list.length()) {
                val entry = list.optJSONObject(i) ?: continue
                val athlete = entry.optJSONObject("athlete")
                val name = athlete?.optString("displayName", "").orEmpty().trim()
                if (name.isEmpty()) continue
                val status = readStatus(entry.optString("status", "")) ?: continue
                val observed = OddsApi.isoUtc(entry.optString("date", "")).takeIf { it > 0L } ?: nowUtc
                out += Row(
                    playerName = name,
                    team = tag,
                    status = status,
                    note = entry.optString("shortComment", "").trim().replace(Regex("\\s+"), " "),
                    observedUtc = observed,
                )
            }
        }
        return out
    }

    companion object {
        const val NBA_URL = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/injuries"
        const val NFL_URL = "https://site.api.espn.com/apis/site/v2/sports/football/nfl/injuries"
        const val URL = NBA_URL

        /**
         * The designation vocabulary, mapped onto the statuses the model already weighs.
         * Unrecognised wording yields null: a phrase nobody on this side understands must
         * not be guessed into a multiplier.
         */
        fun readStatus(raw: String): String? = when (raw.trim().lowercase(Locale.ROOT)) {
            "out", "suspended", "inactive" -> AvailabilityStatus.OUT
            "doubtful" -> AvailabilityStatus.DOUBTFUL
            "questionable", "day-to-day", "day to day" -> AvailabilityStatus.QUESTIONABLE
            "game-time decision", "game time decision", "gtd" -> AvailabilityStatus.GAME_TIME
            "probable" -> AvailabilityStatus.PROBABLE
            else -> null
        }
    }
}
