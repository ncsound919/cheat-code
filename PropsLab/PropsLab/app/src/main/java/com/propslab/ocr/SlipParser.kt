package com.propslab.ocr

import com.propslab.domain.Side

/** One prop read off a screenshot, before it becomes a quote. */
data class SlipPick(
    val playerName: String,
    val market: String,
    val marketLabel: String,
    val side: Side,
    val line: Double,
    val odds: Int?,
    val raw: String,
)

/**
 * Turns the text of a bet-slip screenshot into picks.
 *
 * This is deliberately a parser and not a model: it only claims what the text says.
 * Anything it cannot read with confidence is dropped rather than guessed, and the screen
 * shows the raw text it worked from so the user can see exactly why a line was skipped.
 *
 * Shapes it handles, in the words the three books actually use:
 *
 *   "LeBron James Over 24.5 Points  -115"
 *   "Nikola Jokic Over 28.5 PTS"
 *   "LeBron James"
 *   "More 24.5 Points"
 *   "Anthony Davis  Less 12.5 Rebounds"
 *   "SHAI GILGEOUS-ALEXANDER O 31.5 PTS -110"
 */
object SlipParser {

    private val SIDE = Regex(
        """(?i)\b(over|more|under|less)\b\s*(\d{1,3}(?:\.\d)?)""",
    )

    /** A bare "O 24.5" / "U 8.5" line, the shorthand some books print next to the name. */
    private val SHORT_SIDE = Regex("""(?i)(?:^|\s)([OU])\s+(\d{1,3}(?:\.\d)?)\b""")

    private val ODDS = Regex("""(?<![0-9.])([+-]\d{2,4})(?![0-9])""")

    private val HEADER_NOISE = listOf(
        "bet slip", "my slip", "slip", "picks", "payout", "potential", "wager", "stake",
        "entry", "total", "buy in", "add to", "remove", "balance", "deposit", "withdraw",
        "promo", "odds boost", "parlay", "single", "round robin", "more ways to win",
        "screenshot", "share", "save",
    )

    private val MARKETS = listOf(
        Triple("pra", "player_pra", "PRA"),
        Triple("points+rebounds+assists", "player_pra", "PRA"),
        Triple("pts+reb+ast", "player_pra", "PRA"),
        Triple("threes", "player_threes", "Threes"),
        Triple("3-pointers", "player_threes", "Threes"),
        Triple("3 pointers", "player_threes", "Threes"),
        Triple("3pm", "player_threes", "Threes"),
        Triple("three-pointers", "player_threes", "Threes"),
        Triple("rebounds", "player_rebounds", "Rebounds"),
        Triple("rebs", "player_rebounds", "Rebounds"),
        Triple("reb", "player_rebounds", "Rebounds"),
        Triple("assists", "player_assists", "Assists"),
        Triple("asts", "player_assists", "Assists"),
        Triple("ast", "player_assists", "Assists"),
        Triple("points", "player_points", "Points"),
        Triple("pts", "player_points", "Points"),
        Triple("pt", "player_points", "Points"),
    )

    private const val DEFAULT_MARKET = "player_points"
    private const val DEFAULT_MARKET_LABEL = "Points"

    fun normalise(text: String): String = text
        .replace('\u2212', '-')
        .replace('\u2013', '-')
        .replace('\u2014', '-')
        .replace('\u00a0', ' ')
        .replace('\u00b7', ' ')

    fun parse(text: String): List<SlipPick> {
        val lines = normalise(text).lines().map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableListOf<SlipPick>()
        var pendingName: String? = null

        for (line in lines) {
            val pick = parseLine(line, pendingName)
            if (pick != null) {
                out += pick
                pendingName = null
                continue
            }
            // A line that is neither a pick nor a name clears the pending name, so a stale
            // name cannot attach itself to a pick three lines later.
            pendingName = nameCandidate(line)
        }
        return out.distinctBy {
            listOf(it.playerName.lowercase(), it.market, it.line.toString(), it.side.name).joinToString("|")
        }
    }

    internal fun parseLine(line: String, pendingName: String?): SlipPick? {
        val side: Side
        val number: Double
        val start: Int
        val end: Int

        val long = SIDE.find(line)
        if (long != null) {
            side = when (long.groupValues[1].lowercase()) {
                "under", "less" -> Side.UNDER
                else -> Side.OVER
            }
            number = long.groupValues[2].toDoubleOrNull() ?: return null
            start = long.range.first
            end = long.range.last + 1
        } else {
            val short = SHORT_SIDE.find(line) ?: return null
            side = if (short.groupValues[1].equals("O", true)) Side.OVER else Side.UNDER
            number = short.groupValues[2].toDoubleOrNull() ?: return null
            start = short.range.first
            end = short.range.last + 1
        }
        if (number <= 0.0 || number >= 200.0) return null

        // The price can sit before or after the pick. Take the last plausible one on the line.
        val odds = ODDS.findAll(line).mapNotNull { it.groupValues[1].toIntOrNull() }
            .lastOrNull { it >= 100 || it <= -100 }

        val beforeName = cleanName(line.substring(0, start))
        val name = beforeName ?: pendingName ?: return null
        if (!looksLikeName(name)) return null

        val market = marketOf(line.substring(end))
        return SlipPick(
            playerName = name,
            market = market.first,
            marketLabel = market.second,
            side = side,
            line = number,
            odds = odds,
            raw = line,
        )
    }

    /** A line that could be a player name and nothing else. */
    internal fun nameCandidate(line: String): String? {
        if (SIDE.containsMatchIn(line) || SHORT_SIDE.containsMatchIn(line)) return null
        val cleaned = cleanName(line) ?: return null
        if (ODDS.containsMatchIn(cleaned)) return null
        if (cleaned.any { it.isDigit() }) return null
        return cleaned.takeIf { looksLikeName(it) }
    }

    private fun looksLikeName(value: String): Boolean {
        val lower = value.lowercase()
        if (lower.length < 3 || lower.length > 40) return false
        if (HEADER_NOISE.any { lower == it || lower.startsWith("$it ") }) return false
        if (value.none { it.isLetter() }) return false
        // A name is words, not sentences.
        if (value.count { it.isWhitespace() } > 4) return false
        return value.any { it.isUpperCase() } || value.split(" ").all { it.length > 1 }
    }

    private fun cleanName(raw: String): String? {
        val trimmed = raw
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .trim('.', ',', ':', ';', '|', '-', '\u2022', '\u00b7', '*', '+', '(', ')', '"', '\'')
            .trim()
        if (trimmed.isEmpty()) return null
        return titleCase(trimmed)
    }

    private fun titleCase(value: String): String {
        if (value != value.uppercase()) return value
        fun capitalise(word: String) = word.lowercase().replaceFirstChar { it.uppercase() }
        return value.split(" ").joinToString(" ") { word ->
            word.split("-").joinToString("-") { capitalise(it) }
        }
    }

    private fun marketOf(tail: String): Pair<String, String> {
        val lower = tail.lowercase()
        for ((needle, id, label) in MARKETS) {
            if (lower.contains(needle)) return id to label
        }
        return DEFAULT_MARKET to DEFAULT_MARKET_LABEL
    }
}
