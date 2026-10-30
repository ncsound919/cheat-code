package com.propslab.fantasy

import com.propslab.feed.HttpUrlConnectionFetcher
import com.propslab.feed.JsonFetcher
import com.propslab.feed.Names
import com.propslab.feed.safeGet
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/** A club's public logo and the aliases a screenshot might use for it. */
data class FantasyTeamAsset(
    val code: String,
    val name: String,
    val logoUrl: String,
    val aliases: Set<String>,
)

/** The public athlete identity behind a name on a screenshot. */
data class FantasyPlayerAsset(
    val athleteId: String,
    val name: String,
    val teamName: String,
    val teamCode: String,
    val headshotUrl: String,
    val teamLogoUrl: String,
)

data class FantasyAssetReport(
    val tried: Int = 0,
    val found: Int = 0,
    val teams: Int = 0,
)

/**
 * ESPN's keyless public directory.
 *
 * The screenshot is read on the device. The only thing that leaves it is a player's name,
 * sent to ESPN's public search to find the headshot and club that already belong to him.
 * No image, league name, team name or projection is uploaded.
 */
object FantasyAssets {

    private const val SEARCH = "https://site.web.api.espn.com/apis/search/v2"
    private const val TEAMS = "https://site.api.espn.com/apis/site/v2/sports"

    fun teamsUrl(sport: FantasySport): String = "$TEAMS/${sport.path}/teams"

    fun searchUrl(name: String): String =
        "$SEARCH?query=${URLEncoder.encode(name, Charsets.UTF_8.name())}&limit=5&type=player"

    fun headshotUrl(sport: FantasySport, athleteId: String): String {
        if (athleteId.isBlank()) return ""
        val league = if (sport == FantasySport.FOOTBALL) "nfl" else "nba"
        return "https://a.espncdn.com/i/headshots/$league/players/full/$athleteId.png"
    }

    fun parseTeams(body: String, sport: FantasySport): List<FantasyTeamAsset> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val sports = root.optJSONArray("sports") ?: return emptyList()
        val leagues = sports.optJSONObject(0)?.optJSONArray("leagues") ?: return emptyList()
        val teams = leagues.optJSONObject(0)?.optJSONArray("teams") ?: return emptyList()
        val out = ArrayList<FantasyTeamAsset>(teams.length())
        for (i in 0 until teams.length()) {
            val wrapper = teams.optJSONObject(i) ?: continue
            val team = wrapper.optJSONObject("team") ?: continue
            val name = team.optString("displayName", "").trim()
            if (name.isEmpty()) continue
            val rawCode = team.optString("abbreviation", "").trim()
            val code = ProTeams.code(rawCode, sport) ?: rawCode
            val logo = team.optJSONArray("logos")?.optJSONObject(0)
                ?.optString("href", "").orEmpty().trim()
            val aliases = buildSet {
                if (rawCode.isNotBlank()) add(Names.key(rawCode))
                if (code.isNotBlank()) add(Names.key(code))
                team.optString("shortDisplayName", "").takeIf { it.isNotBlank() }?.let { add(Names.key(it)) }
                team.optString("name", "").takeIf { it.isNotBlank() }?.let { add(Names.key(it)) }
                team.optString("location", "").takeIf { it.isNotBlank() }?.let { add(Names.key(it)) }
                ProTeams.nameFor(code, sport)?.let { add(Names.key(it)) }
            }
            out += FantasyTeamAsset(
                code = code,
                name = name,
                // The directory usually carries a mark, but the bundled one means a club
                // is never blank just because ESPN omitted its logo array.
                logoUrl = logo.ifBlank { ProTeams.logoUrl(code, sport) },
                aliases = aliases,
            )
        }
        return out
    }

    /**
     * An exact-name match in the right league. The first result is not used blindly: a
     * search for "Josh Allen" returns more than one athlete, and a wrong headshot is worse
     * than an initials placeholder.
     */
    fun parsePlayer(
        body: String,
        name: String,
        sport: FantasySport,
        teams: List<FantasyTeamAsset> = emptyList(),
        preferredTeam: String = "",
    ): FantasyPlayerAsset? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val results = root.optJSONArray("results") ?: return null
        val wanted = Names.key(name)
        if (wanted.isEmpty()) return null
        val league = if (sport == FantasySport.FOOTBALL) "NFL" else "NBA"
        val candidates = ArrayList<JSONObject>()
        for (i in 0 until results.length()) {
            val block = results.optJSONObject(i) ?: continue
            if (!block.optString("type", "").equals("player", ignoreCase = true)) continue
            val contents = block.optJSONArray("contents") ?: continue
            for (j in 0 until contents.length()) {
                val row = contents.optJSONObject(j) ?: continue
                if (!row.optString("description", "").equals(league, ignoreCase = true)) continue
                if (Names.key(row.optString("displayName", "")) != wanted) continue
                candidates += row
            }
        }
        if (candidates.isEmpty()) return null
        val preferred = ProTeams.code(preferredTeam, sport)
        val chosen = candidates.firstOrNull { row ->
            val teamKey = Names.key(row.optString("subtitle", ""))
            preferred != null && (teamKey == Names.key(preferred) ||
                teams.firstOrNull { it.code == preferred }?.aliases?.contains(teamKey) == true)
        } ?: candidates.first()

        val uid = chosen.optString("uid", "")
        val athleteId = uid.substringAfter("~a:", "").trim()
        val image = chosen.optJSONObject("image")?.optString("default", "").orEmpty().trim()
        val teamName = chosen.optString("subtitle", "").trim()
        val team = teams.firstOrNull { asset ->
            val key = Names.key(teamName)
            key.isNotBlank() && (key in asset.aliases || Names.key(asset.name) == key ||
                key.contains(Names.key(asset.name)) || Names.key(asset.name).contains(key))
        } ?: teams.firstOrNull { it.code == preferred }
        return FantasyPlayerAsset(
            athleteId = athleteId,
            name = chosen.optString("displayName", name).trim(),
            teamName = team?.name ?: teamName,
            teamCode = team?.code ?: preferred.orEmpty(),
            headshotUrl = image.ifBlank { headshotUrl(sport, athleteId) },
            teamLogoUrl = team?.logoUrl?.ifBlank { ProTeams.logoUrl(team.code, sport) }
                ?: ProTeams.logoUrl(preferred.orEmpty(), sport),
        )
    }

    suspend fun fetchTeams(
        sport: FantasySport,
        fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
    ): List<FantasyTeamAsset> {
        val response = fetcher.safeGet(teamsUrl(sport)).getOrNull() ?: return emptyList()
        if (!response.ok) return emptyList()
        return parseTeams(response.body, sport)
    }

    suspend fun findPlayer(
        name: String,
        sport: FantasySport,
        teams: List<FantasyTeamAsset>,
        preferredTeam: String = "",
        fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
    ): FantasyPlayerAsset? {
        if (name.isBlank()) return null
        val response = fetcher.safeGet(searchUrl(name)).getOrNull() ?: return null
        if (!response.ok) return null
        return parsePlayer(response.body, name, sport, teams, preferredTeam)
    }

    fun scoreboardLabel(sport: FantasySport): String = sport.label.lowercase(Locale.ROOT)
}
