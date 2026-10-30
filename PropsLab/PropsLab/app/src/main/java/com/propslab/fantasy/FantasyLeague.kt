package com.propslab.fantasy

import com.propslab.data.AvailabilityStatus
import com.propslab.feed.Names
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

/** What the user is importing. The mode is explicit so a standings page is never read as a roster. */
enum class CaptureKind(val label: String, val detail: String) {
    MY_TEAM("My lineup", "My starters, bench and this week's opponent"),
    OPPONENT("Opponent", "The other team's lineup for the same week"),
    LEAGUE_TABLE("League table", "Every team's record and points"),
}

/** One player row read from a screenshot. Every field is what the image said, not a guess. */
data class CapturedPlayer(
    val name: String,
    val slot: String = "",
    val position: String = "",
    val proTeam: String = "",
    val opponent: String = "",
    val projected: Double? = null,
    val seasonAverage: Double? = null,
    val raw: String = "",
) {
    val key: String get() = Names.key(name)
}

/** One fantasy team row read from a standings screenshot. */
data class CapturedTeam(
    val name: String,
    val owner: String = "",
    val rank: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    val pointsFor: Double? = null,
    val pointsAgainst: Double? = null,
) {
    val key: String get() = Names.key(name)
    val record: String
        get() = if (ties > 0) "$wins-$losses-$ties" else "$wins-$losses"
}

/** Everything one screenshot (or a batch of screenshots) put on the table. */
data class CapturedPage(
    val kind: CaptureKind,
    val sport: FantasySport,
    val teamName: String = "",
    val players: List<CapturedPlayer> = emptyList(),
    val teams: List<CapturedTeam> = emptyList(),
    val week: Int? = null,
    val leagueName: String = "",
    val platform: String = "Screenshot",
    val warnings: List<String> = emptyList(),
    val rawText: String = "",
) {
    val isEmpty: Boolean get() = players.isEmpty() && teams.isEmpty()
}

/** What the capture screen hands to the database. */
data class FantasyCaptureSave(
    val sport: FantasySport,
    val kind: CaptureKind,
    val leagueName: String,
    val teamName: String,
    val opponentName: String,
    val week: Int,
    val players: List<CapturedPlayer>,
    val teams: List<CapturedTeam>,
    val platform: String = "Screenshot",
    val note: String = "",
)

data class FantasySaveResult(
    val leagueId: String,
    val players: Int,
    val teams: Int,
)

/** The slots a screenshot can print, and the one rule every screen uses to call a player a starter. */
object FantasySlots {

    private val BENCH = setOf(
        "BENCH", "BN", "BE", "BEN", "RES", "RESERVE", "RESERVES",
        "IR", "IR+", "IL", "IL+", "INJURED RESERVE", "TAXI", "SUSP",
    )

    private val STARTER = setOf(
        "PG", "SG", "SF", "PF", "C", "G", "F", "UTIL", "UT", "FLEX",
        "W/R", "W/T", "Q/W/R/T", "QB", "RB", "WR", "TE", "K", "DST",
        "D/ST", "DEF", "IDP", "SUPERFLEX", "SFLEX",
    )

    private val ALL = BENCH + STARTER

    fun normalize(raw: String): String =
        raw.uppercase(Locale.ROOT).replace(".", "").replace(" ", "").trim()

    fun isBench(slot: String): Boolean {
        val value = normalize(slot)
        if (value.isEmpty()) return false
        return value in BENCH || value.startsWith("BN") || value.startsWith("IR") ||
            value.startsWith("IL") || value.startsWith("TAXI")
    }

    /** A blank slot is treated as a starter: a lineup screenshot with no slot column is a lineup. */
    fun isStarter(slot: String): Boolean = !isBench(slot)

    fun isKnown(slot: String): Boolean = normalize(slot) in ALL

    fun tokenIn(raw: String, sport: FantasySport): String? {
        val token = normalize(raw)
        // SF is a basketball slot and the 49ers' club code. In football the club wins,
        // because a screenshot without a slot column still needs the team logo.
        if (sport == FantasySport.FOOTBALL && token == "SF") return null
        if (token in ALL) return token
        val football = sport == FantasySport.FOOTBALL
        return when (token) {
            "DEF" -> if (football) "DST" else null
            "D" -> if (football) "DST" else null
            "BN" -> "BENCH"
            else -> null
        }
    }
}

/**
 * The OCR-to-roster parser.
 *
 * It reads the shapes fantasy apps actually print: a slot, a player, a pro team, an opponent
 * and sometimes two numbers. It deliberately does not guess a player from a line that has no
 * plausible name. A row that cannot be read is left out, and the review screen shows the raw
 * OCR so the omission is visible.
 */
object FantasyCaptureParser {

    private val WEEK = Regex("""(?i)\b(?:week|wk)\s*#?\s*(\d{1,2})\b""")
    private val LEAGUE = Regex("""(?i)\b(?:league|league name)\s*[:\-]\s*(.{2,60})""")
    private val RANKED_RECORD = Regex(
        """^(?:#\s*)?(\d{1,2})[.)]?\s+(.+?)\s+(\d{1,2})-(\d{1,2})(?:-(\d{1,2}))?""" +
            """(?:\s+([\d,]+(?:\.\d+)?))?(?:\s+([\d,]+(?:\.\d+)?))?$""",
    )
    private val RECORD = Regex(
        """^(.+?)\s+(\d{1,2})-(\d{1,2})(?:-(\d{1,2}))?""" +
            """(?:\s+([\d,]+(?:\.\d+)?))?(?:\s+([\d,]+(?:\.\d+)?))?$""",
    )
    private val NUMBER = Regex("""(?<![\d.])(\d{1,3}(?:\.\d{1,2})?)(?![\d.])(?!\s*%)""")
    private val AFTER_LABEL = Regex(
        """(?i)\b(?:proj(?:ected)?|fpts|fantasy points|points)\s*[:=]?\s*(\d{1,3}(?:\.\d{1,2})?)""",
    )
    private val AVG_LABEL = Regex(
        """(?i)\b(?:avg|average|season avg|season average)\s*[:=]?\s*(\d{1,3}(?:\.\d{1,2})?)""",
    )
    private val OPPONENT = Regex("""(?i)(?:\bvs\.?|\bv\.?|@|\bat\b)\s*([A-Za-z]{2,4})\b""")

    private val BENCH_WORDS = setOf(
        "bench", "bench players", "reserves", "reserve", "be", "bn", "ir", "ir+", "il", "il+",
        "injured reserve", "taxi squad", "taxi",
    )
    private val STARTER_WORDS = setOf("starters", "starting lineup", "lineup", "active roster")

    private val NOISE = setOf(
        "lineup", "starting lineup", "roster", "matchup", "projected", "projection",
        "projections", "total", "totals", "average", "avg", "points", "fantasy", "week",
        "league", "settings", "waivers", "free agents", "free agent", "trade", "scoreboard",
        "standings", "record", "team", "teams", "opponent", "position", "pos", "player",
        "players", "game", "games", "lock", "locked", "starts", "start", "sit", "bench",
        "today", "tomorrow", "live", "final", "score", "scores", "pro team", "injuries",
        "injury report", "my team", "edit", "save", "cancel",
    )

    private val STOPWORDS = setOf(
        "the", "and", "with", "from", "this", "that", "your", "you", "for", "via",
        "out", "questionable", "doubtful", "probable", "gtd", "sus", "susp", "note",
        "proj", "projected", "projection", "fpts", "pts", "avg", "average", "season",
        "total", "start", "sit", "bench", "ir", "taxi",
    )

    /**
     * Parses the text of one screenshot. [teamHint] wins over any guessed header: the review
     * screen shows it and lets the user correct it, so a guessed header can never be silently
     * filed as the wrong team.
     */
    fun parse(
        text: String,
        kind: CaptureKind,
        sport: FantasySport,
        teamHint: String = "",
        today: LocalDate = LocalDate.now(),
    ): CapturedPage {
        val lines = text.lineSequence()
            .map { clean(it) }
            .filter { it.isNotEmpty() }
            .toList()
        val week = WEEK.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?.takeIf { it in 1..30 }
        val leagueName = LEAGUE.find(text)?.groupValues?.getOrNull(1)
            ?.trim()?.trimEnd('.', ',')?.take(60).orEmpty()
        val platform = when {
            text.contains("ESPN", ignoreCase = true) -> "ESPN"
            text.contains("Yahoo", ignoreCase = true) -> "Yahoo"
            text.contains("Sleeper", ignoreCase = true) -> "Sleeper"
            else -> "Screenshot"
        }

        if (kind == CaptureKind.LEAGUE_TABLE) {
            val teams = parseTeams(lines)
            return CapturedPage(
                kind = kind,
                sport = sport,
                teamName = teamHint,
                teams = teams,
                week = week,
                leagueName = leagueName,
                platform = platform,
                warnings = if (teams.isEmpty()) {
                    listOf("No team records were found. A standings screenshot needs a row such as \"Team name 8-2 2,345.6\".")
                } else {
                    emptyList()
                },
                rawText = text,
            )
        }

        val players = ArrayList<CapturedPlayer>()
        val warnings = ArrayList<String>()
        var pendingSlot = ""
        var skipped = 0
        lines.forEach { line ->
            val lower = line.lowercase(Locale.ROOT).trimEnd(':', '.', ' ')
            when {
                lower in BENCH_WORDS || lower == "bench" -> {
                    pendingSlot = "BENCH"
                    return@forEach
                }
                lower in STARTER_WORDS -> {
                    pendingSlot = ""
                    return@forEach
                }
                isNoise(lower) -> return@forEach
            }
            if (looksLikeTeamRecord(line)) return@forEach
            // A table can put the slot on its own line. The next name line inherits it.
            if (line.split(' ').size == 1 && FantasySlots.tokenIn(line, sport) != null) {
                pendingSlot = FantasySlots.tokenIn(line, sport) ?: pendingSlot
                return@forEach
            }
            val player = parsePlayer(line, sport, pendingSlot)
            if (player == null) {
                if (line.count { it.isLetter() } >= 3 && !isNoise(lower)) skipped++
            } else {
                players += player
            }
        }

        val unique = players
            .groupBy { it.key }
            .map { (_, rows) ->
                rows.maxWithOrNull(compareBy<CapturedPlayer> { it.projected != null }.thenBy { it.projected ?: 0.0 })
                    ?: rows.first()
            }
            .sortedWith(compareBy<CapturedPlayer> { FantasySlots.isBench(it.slot) }.thenByDescending { it.projected ?: 0.0 })

        if (unique.isEmpty()) {
            warnings += "No player rows were found. Try a screenshot where each player and his slot are visible."
        }
        if (skipped > 0 && unique.isNotEmpty()) {
            warnings += "$skipped other line${if (skipped == 1) "" else "s"} looked like text but not a player row."
        }

        return CapturedPage(
            kind = kind,
            sport = sport,
            teamName = teamHint.ifBlank { if (kind == CaptureKind.MY_TEAM) "My team" else "Opponent" },
            players = unique,
            week = week,
            leagueName = leagueName,
            platform = platform,
            warnings = warnings,
            rawText = text,
        )
    }

    fun parseTeams(lines: List<String>): List<CapturedTeam> {
        val teams = LinkedHashMap<String, CapturedTeam>()
        lines.forEach { line ->
            val parsed = teamLine(line) ?: return@forEach
            if (parsed.name.length < 2) return@forEach
            val key = parsed.key
            val old = teams[key]
            if (old == null || parsed.rank > 0 || old.rank == 0) teams[key] = parsed
        }
        return teams.values.sortedWith(
            compareBy<CapturedTeam> { if (it.rank > 0) it.rank else 99 }
                .thenByDescending { it.wins }
                .thenBy { it.name },
        )
    }

    /** The week a screenshot should default to when it does not print one. */
    fun defaultWeek(sport: FantasySport, today: LocalDate = LocalDate.now()): Int {
        val start = when (sport) {
            FantasySport.FOOTBALL -> firstTuesday(today.withMonth(9).withDayOfMonth(1))
            FantasySport.BASKETBALL -> firstTuesday(today.withMonth(10).withDayOfMonth(1)).plusWeeks(2)
        }
        if (today.isBefore(start)) return 1
        val weeks = ChronoUnit.DAYS.between(start, today) / 7L + 1L
        return weeks.toInt().coerceIn(1, if (sport == FantasySport.FOOTBALL) 18 else 24)
    }

    private fun firstTuesday(day: LocalDate): LocalDate =
        day.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))

    private fun parsePlayer(line: String, sport: FantasySport, pendingSlot: String): CapturedPlayer? {
        var work = line
            .replace(Regex("""^\s*(?:#\d{1,2}\s+|\d{1,2}[.)]\s*|[-•·–—]\s*)"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (work.length < 3) return null
        if (work.none { it.isLetter() }) return null

        val afterLabel = AFTER_LABEL.find(work)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val seasonAverage = AVG_LABEL.find(work)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val numbers = NUMBER.findAll(work)
            .mapNotNull { it.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .filter { it in 0.0..250.0 }
            .toList()
        val projected = afterLabel ?: numbers.lastOrNull()
        val average = seasonAverage ?: numbers.dropLast(1).firstOrNull()

        var slot = pendingSlot
        var opponent = ""
        OPPONENT.findAll(work).toList().forEach { match ->
            val code = ProTeams.code(match.groupValues.getOrNull(1).orEmpty(), sport)
            if (code != null) {
                opponent = code
                work = work.replace(match.value, " ")
            }
        }

        val tokens = work.split(' ').filter { it.isNotBlank() }.toMutableList()
        val slotIndex = tokens.indexOfFirst { FantasySlots.tokenIn(it.trim(','), sport) != null }
        if (slotIndex >= 0) {
            slot = FantasySlots.tokenIn(tokens[slotIndex].trim(','), sport) ?: slot
            tokens.removeAt(slotIndex)
        }
        val slotOnly = slot

        // Full team names first, because "Golden State Warriors" would otherwise leave
        // "Golden" behind as if it were a first name.
        var proTeam = ""
        ProTeams.fullNameMatch(work)?.let { full ->
            proTeam = ProTeams.code(full, sport).orEmpty()
            work = work.replace(Regex(Regex.escape(full), RegexOption.IGNORE_CASE), " ")
        }
        val afterFullTeam = work.split(' ').filter { it.isNotBlank() }.toMutableList()
        val teamIndex = afterFullTeam.indexOfFirst {
            ProTeams.code(it.trim(',', '.', ':', ';'), sport) != null
        }
        if (teamIndex >= 0) {
            val raw = afterFullTeam[teamIndex].trim(',', '.', ':', ';')
            proTeam = ProTeams.code(raw, sport) ?: proTeam
            afterFullTeam.removeAt(teamIndex)
        }

        val nameTokens = afterFullTeam
            .map { it.trim(',', '.', ':', ';', '(', ')') }
            .filter { token ->
                token.length >= 2 &&
                    token.any { it.isLetter() } &&
                    token.none { it.isDigit() } &&
                    token.lowercase(Locale.ROOT) !in STOPWORDS &&
                    token.lowercase(Locale.ROOT) !in NOISE &&
                    FantasySlots.tokenIn(token, sport) == null &&
                    ProTeams.code(token, sport) == null
            }
            .take(4)
        if (nameTokens.size < 2) return null
        val name = titleCase(nameTokens.joinToString(" "))
        if (name.length < 3) return null

        val rawSlot = slotOnly
        val position = if (FantasySlots.isKnown(rawSlot) && !FantasySlots.isBench(rawSlot)) rawSlot else ""

        return CapturedPlayer(
            name = name,
            slot = rawSlot,
            position = position,
            proTeam = proTeam,
            opponent = opponent,
            projected = projected?.takeIf { it > 0.0 },
            seasonAverage = average?.takeIf { it > 0.0 },
            raw = line,
        )
    }

    private fun teamLine(line: String): CapturedTeam? {
        val cleaned = line.trim()
        val ranked = RANKED_RECORD.matchEntire(cleaned)
        if (ranked != null) {
            return teamFrom(ranked, ranked = true)
        }
        val plain = RECORD.matchEntire(cleaned)
        return if (plain != null) teamFrom(plain, ranked = false) else null
    }

    private fun teamFrom(match: MatchResult, ranked: Boolean): CapturedTeam? {
        val offset = if (ranked) 1 else 0
        val rank = if (ranked) match.groupValues[1].toIntOrNull() ?: 0 else 0
        val name = match.groupValues[1 + offset].trim()
        if (name.lowercase(Locale.ROOT) in setOf("team", "total", "average")) return null
        if (name.none { it.isLetter() }) return null
        return CapturedTeam(
            name = name,
            rank = rank,
            wins = match.groupValues[2 + offset].toIntOrNull() ?: 0,
            losses = match.groupValues[3 + offset].toIntOrNull() ?: 0,
            ties = match.groupValues[4 + offset].toIntOrNull() ?: 0,
            pointsFor = match.groupValues[5 + offset].replace(",", "").toDoubleOrNull(),
            pointsAgainst = match.groupValues[6 + offset].replace(",", "").toDoubleOrNull(),
        )
    }

    private fun looksLikeTeamRecord(line: String): Boolean =
        line.contains(Regex("""\b\d{1,2}-\d{1,2}(?:-\d{1,2})?\b""")) &&
            line.split(' ').size in 3..8 &&
            line.count { it.isLetter() } > 4

    private fun isNoise(lower: String): Boolean {
        if (lower.isEmpty()) return true
        if (lower.startsWith("espn") || lower.startsWith("yahoo") || lower.startsWith("sleeper")) return true
        if (lower in NOISE) return true
        return NOISE.any { lower.startsWith("$it ") || lower.endsWith(" $it") }
    }

    private fun clean(line: String): String =
        line.replace('\u00a0', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('|')

    private fun titleCase(raw: String): String {
        if (raw.isEmpty()) return raw
        if (raw != raw.uppercase(Locale.ROOT)) return raw
        return raw.split(' ').joinToString(" ") { part ->
            // Keep initials punctuated as initials (A.J., P.J.) instead of turning them into
            // sentence case, and capitalise after apostrophes and hyphens.
            if (part.matches(Regex("[A-Za-z](?:\\.[A-Za-z])+\\.?"))) {
                part.uppercase(Locale.ROOT)
            } else {
                part.lowercase(Locale.ROOT).replace(Regex("(^|[-'’])([a-z])")) { match ->
                    match.groupValues[1] + match.groupValues[2].uppercase(Locale.ROOT)
                }
            }
        }
    }
}

/** NBA/NFL club codes, so a pro team can be read without asking a server what "GSW" means. */
object ProTeams {

    private val NBA = listOf(
        "ATL" to "Atlanta Hawks", "BOS" to "Boston Celtics", "BKN" to "Brooklyn Nets",
        "CHA" to "Charlotte Hornets", "CHI" to "Chicago Bulls", "CLE" to "Cleveland Cavaliers",
        "DAL" to "Dallas Mavericks", "DEN" to "Denver Nuggets", "DET" to "Detroit Pistons",
        "GSW" to "Golden State Warriors", "HOU" to "Houston Rockets", "IND" to "Indiana Pacers",
        "LAC" to "LA Clippers", "LAL" to "Los Angeles Lakers", "MEM" to "Memphis Grizzlies",
        "MIA" to "Miami Heat", "MIL" to "Milwaukee Bucks", "MIN" to "Minnesota Timberwolves",
        "NOP" to "New Orleans Pelicans", "NYK" to "New York Knicks", "OKC" to "Oklahoma City Thunder",
        "ORL" to "Orlando Magic", "PHI" to "Philadelphia 76ers", "PHX" to "Phoenix Suns",
        "POR" to "Portland Trail Blazers", "SAC" to "Sacramento Kings", "SAS" to "San Antonio Spurs",
        "TOR" to "Toronto Raptors", "UTA" to "Utah Jazz", "WAS" to "Washington Wizards",
    )
    private val NFL = listOf(
        "ARI" to "Arizona Cardinals", "ATL" to "Atlanta Falcons", "BAL" to "Baltimore Ravens",
        "BUF" to "Buffalo Bills", "CAR" to "Carolina Panthers", "CHI" to "Chicago Bears",
        "CIN" to "Cincinnati Bengals", "CLE" to "Cleveland Browns", "DAL" to "Dallas Cowboys",
        "DEN" to "Denver Broncos", "DET" to "Detroit Lions", "GB" to "Green Bay Packers",
        "HOU" to "Houston Texans", "IND" to "Indianapolis Colts", "JAX" to "Jacksonville Jaguars",
        "KC" to "Kansas City Chiefs", "LV" to "Las Vegas Raiders", "LAC" to "Los Angeles Chargers",
        "LAR" to "Los Angeles Rams", "MIA" to "Miami Dolphins", "MIN" to "Minnesota Vikings",
        "NE" to "New England Patriots", "NO" to "New Orleans Saints", "NYG" to "New York Giants",
        "NYJ" to "New York Jets", "PHI" to "Philadelphia Eagles", "PIT" to "Pittsburgh Steelers",
        "SEA" to "Seattle Seahawks", "SF" to "San Francisco 49ers", "TB" to "Tampa Bay Buccaneers",
        "TEN" to "Tennessee Titans", "WAS" to "Washington Commanders",
    )
    private val ALL = NBA + NFL
    private val NBA_BY_CODE = NBA.toMap()
    private val NFL_BY_CODE = NFL.toMap()
    private val BY_CODE = ALL.groupBy({ it.first }) { it.second }
    private val BY_NAME = ALL.associate { (code, name) -> Names.key(name) to code }
    private val ALIASES = mapOf(
        "GS" to "GSW", "NY" to "NYK", "NO" to "NOP", "NOLA" to "NOP", "WSH" to "WAS",
        "PHO" to "PHX", "SA" to "SAS", "UTAH" to "UTA", "JAC" to "JAX", "LVR" to "LV",
        "LA" to "LAR", "OAK" to "LV", "SD" to "LAC", "STL" to "LAR", "D" to "DST",
    )

    /**
     * ESPN's CDN slug is the club code in lower case for most clubs, but not all of them.
     * The exceptions are written down rather than guessed: a wrong mark is worse than no mark.
     */
    private val NBA_MARK = mapOf(
        "ATL" to "atl", "BOS" to "bos", "BKN" to "bkn", "CHA" to "cha", "CHI" to "chi",
        "CLE" to "cle", "DAL" to "dal", "DEN" to "den", "DET" to "det", "GSW" to "gs",
        "HOU" to "hou", "IND" to "ind", "LAC" to "lac", "LAL" to "lal", "MEM" to "mem",
        "MIA" to "mia", "MIL" to "mil", "MIN" to "min", "NOP" to "no", "NYK" to "ny",
        "OKC" to "okc", "ORL" to "orl", "PHI" to "phi", "PHX" to "phx", "POR" to "por",
        "SAC" to "sac", "SAS" to "sa", "TOR" to "tor", "UTA" to "utah", "WAS" to "wsh",
    )
    private val NFL_MARK = NFL.associate { (code, _) -> code to code.lowercase(Locale.ROOT) }
        .toMutableMap()
        .apply { put("WAS", "wsh") }

    /** Every club this build can draw, so a test can prove the marks are complete. */
    fun codes(sport: FantasySport): List<String> = when (sport) {
        FantasySport.BASKETBALL -> NBA.map { it.first }
        FantasySport.FOOTBALL -> NFL.map { it.first }
    }

    /**
     * The bundled club mark. No lookup, no API key and no network discovery: the URL is
     * known from the code alone. An unknown code returns "" rather than a broken image.
     */
    fun logoUrl(code: String, sport: FantasySport? = null): String {
        val canonical = code(code, sport) ?: return ""
        val basketball = when (sport) {
            FantasySport.FOOTBALL -> false
            FantasySport.BASKETBALL -> true
            null -> canonical in NBA_BY_CODE
        }
        val league = if (basketball) "nba" else "nfl"
        val slug = (if (basketball) NBA_MARK else NFL_MARK)[canonical] ?: return ""
        return "https://a.espncdn.com/i/teamlogos/$league/500/$slug.png"
    }

    /**
     * The canonical code for a code, full club name or common alias. Null means "not a team".
     * The sport resolves the two shared marks: NO is the Pelicans' NOP in basketball and the
     * Saints' NO in football.
     */
    fun code(raw: String, sport: FantasySport? = null): String? {
        val trimmed = raw.trim().trim(',', '.', ':', ';', '(', ')')
        if (trimmed.isEmpty()) return null
        val upper = trimmed.uppercase(Locale.ROOT)
        when (sport) {
            FantasySport.BASKETBALL -> {
                if (upper == "NO") return "NOP"
                if (upper in NBA_BY_CODE) return upper
            }
            FantasySport.FOOTBALL -> {
                if (upper == "NOP") return "NO"
                if (upper in NFL_BY_CODE) return upper
            }
            else -> Unit
        }
        if (upper in BY_CODE) return upper
        ALIASES[upper]?.let { return it }
        return BY_NAME[Names.key(trimmed)]
    }

    fun fullNameMatch(line: String): String? =
        BY_NAME.keys
            .sortedByDescending { it.length }
            .firstOrNull { line.contains(it, ignoreCase = true) }
            ?.let { key -> ALL.firstOrNull { Names.key(it.second) == key }?.second }

    fun nameFor(code: String, sport: FantasySport? = null): String? {
        val upper = code.uppercase(Locale.ROOT)
        return when (sport) {
            FantasySport.BASKETBALL -> NBA_BY_CODE[upper]
            FantasySport.FOOTBALL -> NFL_BY_CODE[upper]
            else -> BY_CODE[upper]?.firstOrNull()
        }
    }
}

/** The UI-facing player used by the weekly advisor. */
data class LeaguePlayer(
    val key: String,
    val name: String,
    val slot: String,
    val position: String = "",
    val proTeam: String = "",
    val opponent: String = "",
    val projected: Double? = null,
    val seasonAverage: Double? = null,
    val recent: FantasyPlayer? = null,
    val injury: FantasyInjury? = null,
    val headshotUrl: String = "",
    val teamLogoUrl: String = "",
) {
    /** ESPN's form projection, then the screenshot's projection. Never a made-up number. */
    val projection: Double?
        get() = recent?.takeIf { it.games > 0 }?.projection ?: projected

    val isStarter: Boolean get() = FantasySlots.isStarter(slot)
}

data class LeaguePlayerWriteUp(
    val player: LeaguePlayer,
    val call: FantasyCall,
    val headline: String,
    val body: String,
    val projected: Double?,
    val starter: Boolean,
)

data class LeagueWeeklyBrief(
    val week: Int,
    val writeUps: List<LeaguePlayerWriteUp> = emptyList(),
    val insights: List<String> = emptyList(),
    val startersProjected: Double? = null,
    val opponentProjected: Double? = null,
    val opponentName: String = "",
    val startersMissingProjection: Int = 0,
) {
    val edge: Double? get() = if (startersProjected != null && opponentProjected != null) {
        startersProjected - opponentProjected
    } else {
        null
    }
}

/**
 * The weekly call, with its reasoning in the same object.
 *
 * This is still a form call, not a guarantee: it knows the slot, the imported projection and
 * the public box-score form. It does not know the league's exact scoring rules or the
 * opponent's defensive matchup, and the body says so when the data runs out.
 */
object FantasyLeagueDesk {

    const val SWAP_MARGIN = 3.0

    fun build(
        week: Int,
        roster: List<LeaguePlayer>,
        opponent: List<LeaguePlayer> = emptyList(),
        opponentName: String = "",
    ): LeagueWeeklyBrief {
        if (roster.isEmpty()) {
            return LeagueWeeklyBrief(
                week = week,
                insights = listOf(
                    "Capture your lineup to get a start/sit call and a short reason for every player.",
                ),
                opponentName = opponentName,
            )
        }
        val starters = roster.filter { it.isStarter }
        val bench = roster.filterNot { it.isStarter }
        // A ruled-out player's old projection is not a live projection. Leave it out of the
        // total rather than making the lineup look stronger than the news allows.
        val availableStarters = starters.filter {
            it.injury?.status != AvailabilityStatus.OUT && it.injury?.status != AvailabilityStatus.DOUBTFUL
        }
        val starterProjections = availableStarters.mapNotNull { it.projection }
        val weakest = starterProjections.minOrNull()
        val benchBest = bench.filter {
            it.projection != null &&
                it.injury?.status != AvailabilityStatus.OUT &&
                it.injury?.status != AvailabilityStatus.DOUBTFUL
        }.maxByOrNull { it.projection ?: 0.0 }
        val injuryCount = roster.count {
            it.injury?.status in setOf(
                AvailabilityStatus.OUT,
                AvailabilityStatus.DOUBTFUL,
                AvailabilityStatus.QUESTIONABLE,
                AvailabilityStatus.GAME_TIME,
            )
        }
        val missing = starters.count { it.projection == null }
        val writeUps = roster.map { player ->
            writeUp(player, starters, benchBest, weakest)
        }.sortedWith(
            compareBy<LeaguePlayerWriteUp> { !it.starter }
                .thenBy { callRank(it.call) }
                .thenByDescending { it.projected ?: -1.0 },
        )

        val opponentStarters = opponent.filter { it.isStarter }
            .filter {
                it.injury?.status != AvailabilityStatus.OUT && it.injury?.status != AvailabilityStatus.DOUBTFUL
            }
        val opponentProjections = opponentStarters.mapNotNull { it.projection }
        val myTotal = starterProjections.takeIf { it.isNotEmpty() }?.sum()
        val oppTotal = opponentProjections.takeIf { it.isNotEmpty() }?.sum()

        val insights = buildList {
            if (myTotal != null) {
                add(
                    "Your starting slots project %.1f across %d available player%s.".format(
                        myTotal, availableStarters.size, if (availableStarters.size == 1) "" else "s",
                    ),
                )
                if (availableStarters.size < starters.size) {
                    add(
                        "${starters.size - availableStarters.size} starter" +
                            "${if (starters.size - availableStarters.size == 1) " is" else "s are"} " +
                            "ruled out or doubtful and left out of that total.",
                    )
                }
            }
            if (myTotal != null && oppTotal != null) {
                val edge = myTotal - oppTotal
                add(
                    "%s projects %.1f, so the current edge is %s%.1f.".format(
                        opponentName.ifBlank { "The opponent" }, oppTotal,
                        if (edge >= 0) "+" else "", edge,
                    ),
                )
            } else if (opponent.isEmpty()) {
                add("No opponent lineup is stored for this week. Import it to compare the projected totals.")
            }
            if (benchBest?.projection != null && weakest != null) {
                val boost = (benchBest.projection ?: 0.0) - weakest
                if (boost >= SWAP_MARGIN) {
                    add(
                        "%s is the best bench option at %.1f, %.1f above your weakest starting slot. Check eligibility before swapping.".format(
                            benchBest.name, benchBest.projection ?: 0.0, boost,
                        ),
                    )
                }
            }
            if (missing > 0) {
                add(
                    "$missing starting player${if (missing == 1) " has" else "s have"} no projection on file. " +
                        "A screenshot with the projected column, or a fresh ESPN read, will fill that in.",
                )
            }
            if (injuryCount > 0) {
                add(
                    "$injuryCount player${if (injuryCount == 1) " carries" else "s carry"} an injury designation. " +
                        "The designation overrides form in every call below.",
                )
            }
            val thin = roster.count { (it.recent?.games ?: 0) in 1..2 }
            if (thin > 0) {
                add("$thin player${if (thin == 1) " has" else "s have"} fewer than three games on file; those projections move easily.")
            }
            add(
                "The totals add the players already in starting slots. This is form advice, not a promise " +
                    "about a game, and it places nothing.",
            )
        }

        return LeagueWeeklyBrief(
            week = week,
            writeUps = writeUps,
            insights = insights,
            startersProjected = myTotal,
            opponentProjected = oppTotal,
            opponentName = opponentName,
            startersMissingProjection = missing,
        )
    }

    private fun writeUp(
        player: LeaguePlayer,
        starters: List<LeaguePlayer>,
        benchBest: LeaguePlayer?,
        weakest: Double?,
    ): LeaguePlayerWriteUp {
        val projection = player.projection
        val status = player.injury?.status
        val designation = player.injury?.let {
            "${AvailabilityStatus.label(it.status)} on the injury report" +
                it.note.takeIf { note -> note.isNotBlank() }?.let { note -> " — $note" }.orEmpty()
        }
        val data = dataSentence(player, projection)
        val matchup = buildString {
            if (player.opponent.isNotBlank()) append("Matchup: ${player.opponent}. ")
            if (player.slot.isNotBlank()) append("Slot: ${player.slot}.")
            else append("The slot was not readable in the screenshot.")
        }

        fun advice(call: FantasyCall, headline: String, extra: String = ""): LeaguePlayerWriteUp =
            LeaguePlayerWriteUp(
                player = player,
                call = call,
                headline = headline,
                body = listOf(designation, data, extra, matchup)
                    .filter { !it.isNullOrBlank() }
                    .joinToString(" "),
                projected = projection,
                starter = player.isStarter,
            )

        when (status) {
            AvailabilityStatus.OUT -> return advice(
                FantasyCall.OUT, "Ruled out — do not start",
                "The club has ruled him out, so the app will not rank him on form.",
            )
            AvailabilityStatus.DOUBTFUL -> return advice(
                FantasyCall.SIT, "Doubtful — plan without him",
                "A doubtful designation is not a coin flip worth starting.",
            )
            AvailabilityStatus.QUESTIONABLE, AvailabilityStatus.GAME_TIME -> return advice(
                FantasyCall.WATCH, "Check before lock",
                "The club has not said he plays. The app will not guess.",
            )
            else -> Unit
        }

        if (projection == null) {
            return advice(
                FantasyCall.WATCH, "No projection to defend",
                "No recent ESPN game and no projection in the screenshot, so this is a wait-for-news call.",
            )
        }

        if (player.isStarter) {
            val benchProjection = benchBest?.projection
            if (weakest != null && benchProjection != null &&
                projection <= weakest + 0.001 && benchProjection >= projection + SWAP_MARGIN
            ) {
                return advice(
                    FantasyCall.SIT, "Move to the bench",
                    "He is at the bottom of your starting projections, and ${benchBest.name} " +
                        "projects %.1f from the bench.".format(benchProjection),
                )
            }
            return advice(
                FantasyCall.START, "Start him",
                when {
                    starters.size == 1 -> "He is the only player in a starting slot on this roster."
                    benchBest?.projection == null ->
                        "He is in a starting slot; no bench projection is available to challenge it."
                    else -> "He is in a starting slot and the best bench option is %.1f, below him.".format(
                        benchBest.projection,
                    )
                },
            )
        }

        if (weakest != null && projection >= weakest + SWAP_MARGIN) {
            return advice(
                FantasyCall.START, "Worth a start",
                "He projects %.1f, %.1f above your weakest starting slot. Check eligibility, then start him.".format(
                    projection, projection - weakest,
                ),
            )
        }
        return advice(
            FantasyCall.SIT, "Leave on the bench",
            if (weakest != null) {
                "He projects %.1f; your weakest starting slot is %.1f. The bench is the call this week.".format(
                    projection, weakest,
                )
            } else {
                "No starting projection is available to compare, so the bench is the conservative call."
            },
        )
    }

    private fun dataSentence(player: LeaguePlayer, projection: Double?): String {
        val recent = player.recent
        val parts = ArrayList<String>(2)
        if (recent != null && recent.games > 0) {
            parts += "ESPN has %.1f points a game over %d game%s; the latest was %.1f.".format(
                recent.average, recent.games, if (recent.games == 1) "" else "s",
                recent.lines.firstOrNull()?.fantasyPoints ?: recent.average,
            )
            if (abs(recent.trend) >= 1.0) {
                parts += "The last three are %.1f %s the three before.".format(
                    abs(recent.trend), if (recent.trend > 0) "above" else "below",
                )
            }
            if (recent.games < 3) parts += "That is a thin sample."
        } else if (projection != null) {
            parts += "The screenshot lists %.1f projected.".format(projection)
        }
        if (player.seasonAverage != null && projection != null &&
            abs(player.seasonAverage - projection) >= 0.5
        ) {
            parts += "The screenshot's season average is %.1f.".format(player.seasonAverage)
        }
        return parts.joinToString(" ")
    }

    private fun callRank(call: FantasyCall): Int = when (call) {
        FantasyCall.START -> 0
        FantasyCall.WATCH -> 1
        FantasyCall.SIT -> 2
        FantasyCall.OUT -> 3
    }
}
