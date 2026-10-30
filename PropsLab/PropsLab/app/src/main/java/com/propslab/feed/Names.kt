package com.propslab.feed

import java.text.Normalizer
import java.util.Locale

/**
 * The join key between two providers that were never meant to be joined.
 *
 * The odds feed names a player in a free-text `description`; the box score carries
 * `first_name` and `last_name`. Neither publishes an id the other understands, so the only
 * honest join is a normalised name: accents folded, punctuation dropped, suffixes removed,
 * case and spacing flattened. A name that survives that is very likely the same person;
 * anything ambiguous stays unmatched rather than being guessed at.
 */
object Names {

    private val SUFFIXES = setOf("jr", "sr", "ii", "iii", "iv", "v")
    private val MARKS = Regex("\\p{Mn}+")
    private val NOT_PARTS = Regex("[^a-z0-9 ]")

    fun key(raw: String): String {
        val folded = Normalizer.normalize(raw, Normalizer.Form.NFD).replace(MARKS, "")
        val parts = folded.lowercase(Locale.ROOT).replace(NOT_PARTS, " ")
            .split(' ').filter { it.isNotBlank() }
        val trimmed = if (parts.size > 2 && parts.last() in SUFFIXES) parts.dropLast(1) else parts
        return trimmed.joinToString(" ")
    }

    /** Player ids are derived from the key so the same person is one row across syncs. */
    fun playerId(raw: String): String = "fp-" + Integer.toHexString(key(raw).hashCode())
}

/**
 * Team abbreviations.
 *
 * The odds feed only gives full team names. The results feed gives both, but a game has to
 * be drawn on the board before anyone has played it. A fixed table is smaller and more
 * truthful than a rule that turns "Los Angeles Lakers" into "LOS".
 */
object Teams {

    private val ABBREVIATIONS = mapOf(
        "atlanta hawks" to "ATL", "boston celtics" to "BOS", "brooklyn nets" to "BKN",
        "charlotte hornets" to "CHA", "chicago bulls" to "CHI", "cleveland cavaliers" to "CLE",
        "dallas mavericks" to "DAL", "denver nuggets" to "DEN", "detroit pistons" to "DET",
        "golden state warriors" to "GSW", "houston rockets" to "HOU", "indiana pacers" to "IND",
        "la clippers" to "LAC", "los angeles clippers" to "LAC", "los angeles lakers" to "LAL",
        "memphis grizzlies" to "MEM", "miami heat" to "MIA", "milwaukee bucks" to "MIL",
        "minnesota timberwolves" to "MIN", "new orleans pelicans" to "NOP", "new york knicks" to "NYK",
        "oklahoma city thunder" to "OKC", "orlando magic" to "ORL", "philadelphia 76ers" to "PHI",
        "phoenix suns" to "PHX", "portland trail blazers" to "POR", "sacramento kings" to "SAC",
        "san antonio spurs" to "SAS", "toronto raptors" to "TOR", "utah jazz" to "UTA",
        "washington wizards" to "WAS",
    )

    fun abbreviation(fullName: String): String =
        ABBREVIATIONS[Names.key(fullName)]
            ?: fullName.split(' ').filter { it.isNotBlank() }
                .joinToString("") { it.first().uppercase() }.take(4).ifEmpty { "NBA" }

    /** True when the two names are the same club, however each provider spells it. */
    fun sameTeam(a: String, b: String): Boolean {
        val ka = Names.key(a)
        val kb = Names.key(b)
        if (ka.isEmpty() || kb.isEmpty()) return false
        return ka == kb || abbreviation(a) == abbreviation(b)
    }
}
