package com.propslab.availability

import com.propslab.data.AvailabilityStatus
import java.util.Locale

/**
 * Reads one headline into at most one status.
 *
 * This is a rule set, not a model, and it is deliberately narrow. It only answers when the
 * text actually contains a status phrase, it requires the player's surname to appear, and
 * it scores its own confidence so the app can ignore a weak read. A headline it cannot
 * classify produces nothing — which is the correct output far more often than not.
 */
object InjuryParser {

    data class Mention(
        val status: String,
        val area: String?,
        val confidence: Int,
        val phrase: String,
    )

    /** Strongest first: "ruled out" must win over a stray "will play" elsewhere in the text. */
    private val RULES: List<Triple<String, String, Int>> = listOf(
        Triple("ruled out", AvailabilityStatus.OUT, 3),
        Triple("will not play", AvailabilityStatus.OUT, 3),
        Triple("won't play", AvailabilityStatus.OUT, 3),
        Triple("not expected to play", AvailabilityStatus.OUT, 3),
        Triple("out for", AvailabilityStatus.OUT, 3),
        Triple("out tonight", AvailabilityStatus.OUT, 3),
        Triple("suspended", AvailabilityStatus.OUT, 3),
        Triple("doubtful", AvailabilityStatus.DOUBTFUL, 3),
        Triple("game-time decision", AvailabilityStatus.QUESTIONABLE, 3),
        Triple("game time decision", AvailabilityStatus.QUESTIONABLE, 3),
        Triple("questionable", AvailabilityStatus.QUESTIONABLE, 3),
        Triple("50/50", AvailabilityStatus.QUESTIONABLE, 2),
        Triple("toss-up", AvailabilityStatus.QUESTIONABLE, 2),
        Triple("probable", AvailabilityStatus.PROBABLE, 3),
        Triple("expected to play", AvailabilityStatus.PROBABLE, 2),
        Triple("cleared to play", AvailabilityStatus.PROBABLE, 2),
        Triple("minutes restriction", AvailabilityStatus.GAME_TIME, 2),
        Triple("limited minutes", AvailabilityStatus.GAME_TIME, 2),
        Triple("will be limited", AvailabilityStatus.GAME_TIME, 2),
        Triple("coming off the bench", AvailabilityStatus.GAME_TIME, 1),
    )

    private val INJURY_WORDS = listOf(
        "injur", "sore", "strain", "sprain", "contusion", "illness", "tightness",
        "surgery", "concussion", "bruise", "swelling", "rest",
    )

    private val AREAS = listOf(
        "ankle", "knee", "hamstring", "calf", "groin", "back", "wrist", "thumb", "foot",
        "toe", "hip", "illness", "concussion", "shoulder", "elbow", "quad", "achilles",
        "hand", "ribs", "neck", "leg", "shin",
    )

    /**
     * @param playerName when given, the surname must appear in the text. A headline about a
     *   different player is worse than no signal at all.
     */
    fun parse(text: String, playerName: String? = null): Mention? {
        val lower = text.lowercase(Locale.ROOT)
        if (lower.isBlank()) return null
        if (playerName != null && !mentionsPlayer(lower, playerName)) return null

        val rule = RULES.firstOrNull { lower.contains(it.first) } ?: return null
        val injuryWord = INJURY_WORDS.firstOrNull { lower.contains(it) }
        // A status phrase alone is enough for the strong ones; the weaker ones ("coming off
        // the bench") need an injury word beside them or they are just tactics.
        if (rule.third < 3 && injuryWord == null) return null
        return Mention(
            status = rule.second,
            area = AREAS.firstOrNull { lower.contains(it) }?.let { titleCase(it) },
            confidence = rule.third,
            phrase = rule.first,
        )
    }

    fun mentionsPlayer(lowerText: String, playerName: String): Boolean {
        val parts = playerName.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z ]"), " ")
            .split(' ')
            .filter { it.isNotBlank() }
        if (parts.isEmpty()) return false
        val surname = parts.last()
        if (surname.length < 3) return lowerText.contains(parts.first())
        return lowerText.contains(surname)
    }

    private fun titleCase(value: String): String =
        value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}
