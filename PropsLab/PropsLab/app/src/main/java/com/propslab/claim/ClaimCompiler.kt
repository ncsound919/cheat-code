package com.propslab.claim

import com.propslab.domain.Markets
import com.propslab.feed.Names
import com.propslab.sim.SideFilter
import java.util.Locale

/**
 * Turns a sentence into a strategy.
 *
 * The compiler is deterministic and offline. It matches a closed vocabulary of basketball
 * ideas, picks the factor the sentence leads with, and derives a lens from it. The wording
 * of the claim never becomes an expression and never reaches the arithmetic: only the
 * factor's four numbers do.
 *
 * A claim that matches nothing still compiles. It becomes the generic lens, which is the
 * app's own probabilities with a price filter — and the result screen says so, rather than
 * pretending a sentence was understood.
 */
object ClaimCompiler {

    /** The board, used only to recognise a player the user named and the market he is on. */
    data class Board(
        val playerNames: List<String> = emptyList(),
        val marketByPlayer: Map<String, List<String>> = emptyMap(),
    )

    private val sideWords = mapOf(
        "under" to SideFilter.UNDER_ONLY,
        "unders" to SideFilter.UNDER_ONLY,
        "less than" to SideFilter.UNDER_ONLY,
        "below" to SideFilter.UNDER_ONLY,
        "won't hit" to SideFilter.UNDER_ONLY,
        "wont hit" to SideFilter.UNDER_ONLY,
        "will not hit" to SideFilter.UNDER_ONLY,
        "fade" to SideFilter.UNDER_ONLY,
        "over" to SideFilter.OVER_ONLY,
        "overs" to SideFilter.OVER_ONLY,
        "more than" to SideFilter.OVER_ONLY,
        "at least" to SideFilter.OVER_ONLY,
        "above" to SideFilter.OVER_ONLY,
        "will hit" to SideFilter.OVER_ONLY,
    )

    private val marketWords = listOf(
        Markets.REBOUNDS to listOf("rebound", "rebounds", "boards", "glass"),
        Markets.ASSISTS to listOf("assist", "assists", "dimes", "passing"),
        Markets.THREES to listOf("three", "threes", "3pt", "3-point", "from deep"),
        Markets.POINTS to listOf("points", "scoring", "scorer", "pts"),
    )

    /**
     * @param board used to notice a player the claim names. Empty is valid and simply
     *   means the compiled lens will not name anybody.
     */
    fun compile(claim: String, board: Board = Board()): ClaimFormula {
        val raw = claim.trim()
        val text = normalise(raw)
        val match = bestMatch(text)
        val factor = match?.factor ?: ClaimLexicon.genericFactor()

        val explicitSide = sideWords.entries
            .filter { word(it.key, text) }
            .maxByOrNull { it.key.length }
            ?.value
        val side = explicitSide ?: factor.side

        val mentioned = marketWords.firstOrNull { (_, words) -> words.any { word(it, text) } }?.first
        val markets = when {
            mentioned != null && factor.markets.isEmpty() -> listOf(mentioned)
            mentioned != null && mentioned in factor.markets -> listOf(mentioned)
            else -> factor.markets
        }

        val subject = findPlayer(raw, board)
        val lens = ClaimLens(
            tiltPts = factor.tiltPts,
            side = side,
            markets = markets,
            minEdgePts = when {
                factor.tag == "generic" -> 3.0
                kotlin.math.abs(factor.tiltPts) >= 2.5 -> 3.0
                else -> 2.5
            },
            minPrice = 1.50,
            maxPrice = 2.60,
        )

        val sideWord = when (side) {
            SideFilter.OVER_ONLY -> "overs"
            SideFilter.UNDER_ONLY -> "unders"
            SideFilter.BOTH -> "both sides"
        }
        val marketWord = if (markets.isEmpty()) "the board" else markets.joinToString(" and ") { Markets.label(it).lowercase(Locale.ROOT) }
        val name = when {
            factor.tag == "generic" && explicitSide != null -> "Your read, $sideWord"
            factor.tag == "generic" -> "Your read, unfiltered"
            else -> "${factor.title}, $sideWord"
        }

        val headline = when (factor.tag) {
            "generic" -> "Take the app's number at face value and only bet where the price is far enough away."
            "blowout" -> "Fade $marketWord when a game is expected to get out of hand."
            "absence" -> "Back $marketWord when somebody else's absence opens up the rotation."
            "defense" -> "Fade $marketWord when the defense is the reason the game is being played."
            "fatigue" -> "Fade $marketWord when the schedule has already taken something out of him."
            "veteran_rest" -> "Fade $marketWord when the team has a reason to keep him fresh."
            else -> "Back $marketWord when ${factor.title.lowercase(Locale.ROOT)} is the story of the game."
        }

        val terms = buildList {
            add(factor.gameTerm)
            add(lens.plain())
            if (subject != null) add("On today's board it starts with $subject — that is the player your sentence named.")
            add(
                "It will not bet a price outside ${"%.2f".format(lens.minPrice)}–${"%.2f".format(lens.maxPrice)} decimal, " +
                    "and it will not bet when the app and the book agree.",
            )
        }

        val id = "claim-" + Integer.toHexString(
            "$raw|${factor.tag}|${side.name}|${markets.joinToString(",")}|${lens.tiltPts}".hashCode(),
        )

        return ClaimFormula(
            id = id,
            claim = raw,
            name = name,
            factorTag = factor.tag,
            factorTitle = factor.title,
            lens = lens,
            headline = headline,
            inGameTerms = terms,
            falsifier = factor.falsifier,
            subject = subject,
        )
    }

    /** Build from a factor tag the local model may have chosen, rather than from text. */
    fun compileFromFactorTag(tag: String, claim: String, board: Board = Board()): ClaimFormula {
        val factor = ClaimLexicon.byTag(tag)
        if (factor.tag == "generic") return compile(claim, board)
        // The model only ever chooses a tag. Everything else is derived here.
        val forced = factor.title
        return compile(forced, board).let { base ->
            val lens = ClaimLens(
                tiltPts = factor.tiltPts,
                side = factor.side,
                markets = factor.markets,
                minEdgePts = 3.0,
                minPrice = 1.50,
                maxPrice = 2.60,
            )
            base.copy(
                id = "claim-" + Integer.toHexString("${claim.trim()}|$tag".hashCode()),
                claim = claim.trim(),
                name = "${factor.title}, ${if (factor.side == SideFilter.UNDER_ONLY) "unders" else "overs"}",
                factorTag = factor.tag,
                factorTitle = factor.title,
                lens = lens,
                inGameTerms = listOf(factor.gameTerm, lens.plain(), factor.falsifier),
                falsifier = factor.falsifier,
            )
        }
    }

    /**
     * People type the way they talk. The matcher runs on a normalised copy so "alot", "bc"
     * and a missing apostrophe do not make a clear idea look unrecognised.
     */
    private fun normalise(raw: String): String {
        var text = raw.lowercase(Locale.ROOT)
        text = Regex("\\balot\\b").replace(text, "a lot")
        text = Regex("\\bbc\\b").replace(text, "because")
        text = Regex("\\bcuz\\b").replace(text, "because")
        text = text.replace("b/c", "because")
        text = Regex("\\bisnt\\b").replace(text, "isn't")
        text = Regex("\\bwont\\b").replace(text, "won't")
        text = Regex("\\bcant\\b").replace(text, "can't")
        text = Regex("\\bdont\\b").replace(text, "don't")
        return text
    }

    /** Every player name the compiler will act on, longest first so a full name beats a surname. */
    private fun findPlayer(claim: String, board: Board): String? {
        if (board.playerNames.isEmpty()) return null
        val key = Names.key(normalise(claim))
        return board.playerNames
            .distinct()
            .sortedByDescending { it.length }
            .firstOrNull { name ->
                val full = Names.key(name)
                if (full.isNotEmpty() && key.contains(full)) return@firstOrNull true
                val surname = Names.key(name.substringAfterLast(' '))
                if (surname.length >= 4 && key.contains(surname)) return@firstOrNull true
                // A first name is enough when it is distinctive ("LeBron's team ...").
                val first = Names.key(name.substringBefore(' '))
                first.length >= 4 && key.contains(first)
            }
    }

    private data class Match(val factor: ClaimFactor, val index: Int, val length: Int)

    /**
     * The leading clause wins. "They win by a lot because X is out" is a blowout claim that
     * offers an absence as the reason, not a Next-man-up claim about a player who is not
     * even playing. Ties go to the longer phrase, so "defensive line" beats "defense".
     */
    private fun bestMatch(text: String): Match? {
        var best: Match? = null
        for (factor in ClaimLexicon.factors) {
            for (trigger in factor.triggers) {
                val at = indexOfPhrase(text, trigger) ?: continue
                val candidate = Match(factor, at, trigger.length)
                val current = best
                if (current == null || candidate.index < current.index ||
                    (candidate.index == current.index && candidate.length > current.length)
                ) {
                    best = candidate
                }
            }
        }
        return best
    }

    private fun indexOfPhrase(text: String, phrase: String): Int? {
        var from = 0
        while (true) {
            val at = text.indexOf(phrase, from)
            if (at < 0) return null
            val before = if (at == 0) ' ' else text[at - 1]
            val afterAt = at + phrase.length
            val after = if (afterAt >= text.length) ' ' else text[afterAt]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return at
            from = at + 1
        }
    }

    private fun word(phrase: String, text: String): Boolean = indexOfPhrase(text, phrase) != null

    /** The sentence a result card leads with, in the smallest number of words that is still true. */
    fun plainSummary(formula: ClaimFormula): String = when (formula.factorTag) {
        "generic" -> "This is a price filter, not a prediction: no specific basketball angle was recognised."
        else -> "It reads your sentence as a ${formula.factorTitle.lowercase(Locale.ROOT)} idea and tests that " +
            "on the app's own season before it is allowed anywhere near the live board."
    }
}
