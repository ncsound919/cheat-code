package com.propslab.claim

import com.propslab.domain.Markets
import com.propslab.sim.SideFilter
import com.propslab.sim.StrategyParams

/**
 * A plain-language idea, turned into something the sandbox can actually run.
 *
 * Nothing in here evaluates a string. A claim is compiled into a small, closed set of
 * knobs — a tilt on the model probability, a side, a market, an edge floor and a price
 * band — and those knobs are the only thing that ever reaches the arithmetic. A model
 * reply can choose among known factor tags; it can never become code.
 */
data class ClaimFactor(
    val tag: String,
    val title: String,
    /** Probability points added to the model's chance of the over, before it is compared to the price. */
    val tiltPts: Double,
    val side: SideFilter,
    val markets: List<String>,
    /** Phrases that mean this factor. Matched on whole words, case-insensitively. */
    val triggers: List<String>,
    /** One sentence a casual reader can follow. */
    val gameTerm: String,
    /** What would prove this lens wrong. */
    val falsifier: String,
)

data class ClaimLens(
    val tiltPts: Double,
    val side: SideFilter,
    val markets: List<String>,
    val minEdgePts: Double,
    val minPrice: Double,
    val maxPrice: Double,
    /** Markets a learned lesson has removed. Empty means nothing is excluded. */
    val excludeMarkets: List<String> = emptyList(),
) {
    fun params(): StrategyParams = StrategyParams(
        minEdgePts = minEdgePts,
        sideFilter = side,
        minPrice = minPrice,
        maxPrice = maxPrice,
        tiltPts = tiltPts,
        markets = markets,
        excludeMarkets = excludeMarkets,
    )

    /** The sentence a non-bettor reads: what this lens actually does to a price. */
    fun plain(): String {
        val sideWord = when (side) {
            SideFilter.OVER_ONLY -> "overs"
            SideFilter.UNDER_ONLY -> "unders"
            SideFilter.BOTH -> "whichever side the claim favours"
        }
        val market = if (markets.isEmpty()) "every market" else markets.joinToString(" and ") { Markets.label(it) }
        val tilt = when {
            tiltPts >= 0.05 -> "adds ${"%.1f".format(tiltPts)} points to the app's chance of the over"
            tiltPts <= -0.05 -> "takes ${"%.1f".format(-tiltPts)} points off the app's chance of the over"
            else -> "takes the app's probability as it stands"
        }
        return "It $tilt, looks only at $market, and only bets $sideWord when the price is at least " +
            "${"%.1f".format(minEdgePts)} points away from the app's number, between " +
            "${"%.2f".format(minPrice)} and ${"%.2f".format(maxPrice)} decimal."
    }
}

data class ClaimFormula(
    val id: String,
    val claim: String,
    val name: String,
    val factorTag: String,
    val factorTitle: String,
    val lens: ClaimLens,
    /** One line: the bet in plain words. */
    val headline: String,
    /** Why this lens is supposed to work, in game terms. */
    val inGameTerms: List<String>,
    val falsifier: String,
    /** A player named in the claim who is on today's board, if any. */
    val subject: String? = null,
) {
    val matches: String get() = if (lens.markets.isEmpty()) "All markets" else lens.markets.joinToString(", ") { Markets.label(it) }
}

/**
 * Everything the compiler knows. Kept as data so a test can pin the vocabulary and a
 * reviewer can read every phrase the app will act on.
 */
object ClaimLexicon {

    val factors: List<ClaimFactor> = listOf(
        ClaimFactor(
            tag = "defense",
            title = "Defense first",
            tiltPts = -3.0,
            side = SideFilter.UNDER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "defense", "defence", "defensive", "rim protection", "lockdown", "stopper",
                "guarded", "guarding", "defensive line", "d line", "d-line", "defense wins",
            ),
            gameTerm = "A defense that takes away clean looks lowers the whole scoring environment, " +
                "so the same line is harder to clear than the raw average suggests.",
            falsifier = "If the player's actual numbers land above the app's estimate as often as below it, " +
                "the defense is already in the price and this lens is adding nothing.",
        ),
        ClaimFactor(
            tag = "blowout",
            title = "Blowout minutes",
            tiltPts = -2.5,
            side = SideFilter.UNDER_ONLY,
            markets = listOf(Markets.POINTS),
            triggers = listOf(
                "blowout", "blow out", "blown out", "win by a lot", "win big", "wins by a lot",
                "win comfortably", "garbage time", "run away with", "runaway", "not close",
                "double digits", "blow them out",
            ),
            gameTerm = "A lopsided game shortens the night for the players who made it lopsided. " +
                "The last quarter becomes bench minutes, and a points line set for 34 minutes is " +
                "being asked to clear on 26.",
            falsifier = "If the player's minutes do not fall in the games that turn into blowouts, " +
                "the extra rest never arrives and the unders are just cheaper overs.",
        ),
        ClaimFactor(
            tag = "absence",
            title = "Next man up",
            tiltPts = 2.5,
            side = SideFilter.OVER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "isn't playing", "is not playing", "not playing", "sitting out", "sits out", "sitting",
                "out tonight", "ruled out", "injured", "injury", "suspended", "without", "missing",
                "no one else", "no x", "absent", "out for",
            ),
            gameTerm = "When a teammate is out, his touches and shot attempts do not disappear — " +
                "they get shared among the players still on the floor. The line was set for the " +
                "old pecking order.",
            falsifier = "If the remaining players' usage does not rise in the games the starter misses, " +
                "the market has already priced the replacement and the overs are trapped.",
        ),
        ClaimFactor(
            tag = "pace",
            title = "Pace and possessions",
            tiltPts = 2.5,
            side = SideFilter.OVER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "pace", "fast game", "shootout", "high scoring", "track meet", "run and gun",
                "up and down", "transition", "quick tempo", "shoot early", "lots of possessions",
            ),
            gameTerm = "More possessions means more chances for the same player to add to the same stat. " +
                "A pace-up game is a volume bet, not a skill bet.",
            falsifier = "If the player's per-minute rate is flat and only his minutes move, pace is not " +
                "the mechanism — the line is just moving with the game total.",
        ),
        ClaimFactor(
            tag = "fatigue",
            title = "Tired legs",
            tiltPts = -2.5,
            side = SideFilter.UNDER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "back to back", "back-to-back", "b2b", "tired", "fatigue", "fatigued", "four in five",
                "road trip", "travel", "jet lag", "heavy legs", "third game in", "minutes load",
                "played last night", "short turnaround",
            ),
            gameTerm = "Fatigue shows up first in the legs: jump shots come up short and minutes get " +
                "managed. The same line is being asked to clear on a worse version of the player.",
            falsifier = "If his shooting and minutes are unchanged on the second night of a back-to-back, " +
                "the fatigue story is folklore and the unders are the wrong side.",
        ),
        ClaimFactor(
            tag = "rest",
            title = "Fresh legs",
            tiltPts = 1.5,
            side = SideFilter.OVER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "rested", "rest", "fresh legs", "days off", "well rested", "coming off a break",
                "had a week off", "no travel",
            ),
            gameTerm = "A rested player keeps his usual minutes instead of losing them to fatigue, " +
                "and a line built on a tired sample is a little too low.",
            falsifier = "If rest does not shift his minutes or efficiency, the line already reflects it.",
        ),
        ClaimFactor(
            tag = "minutes",
            title = "The minutes bet",
            tiltPts = 2.0,
            side = SideFilter.OVER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "minutes", "usage", "plays a lot", "big minutes", "more run", "should play more",
                "role", "starters play", "heavy minutes", "on the floor", "closing lineup",
            ),
            gameTerm = "Most player props are a minutes bet wearing a stats costume. If the player is " +
                "on the floor for 36 minutes instead of 30, every counting stat gets a sixth more chances.",
            falsifier = "If his per-minute rate is exactly what the line assumes, an over only wins when " +
                "the minutes actually arrive — the coach is the risk, not the market.",
        ),
        ClaimFactor(
            tag = "rebounding",
            title = "Own the glass",
            tiltPts = 2.0,
            side = SideFilter.OVER_ONLY,
            markets = listOf(Markets.REBOUNDS),
            triggers = listOf(
                "rebound", "rebounds", "glass", "boards", "crash the glass", "second chance",
                "box out", "outrebound", "on the boards",
            ),
            gameTerm = "Rebounds follow positioning and minutes more than shooting luck. A matchup " +
                "against a team that misses a lot creates the chances the line needs.",
            falsifier = "If his rebound rate is normal against this opponent, the misses are going to " +
                "somebody else's hands.",
        ),
        ClaimFactor(
            tag = "playmaking",
            title = "The passing game",
            tiltPts = 2.0,
            side = SideFilter.OVER_ONLY,
            markets = listOf(Markets.ASSISTS),
            triggers = listOf(
                "assist", "assists", "passing", "playmaker", "facilitator", "dimes", "creates",
                "distributor", "playmaking", "ball movement",
            ),
            gameTerm = "Assists depend on teammates making shots. A passing lane that produces open " +
                "looks is worth more than the same pass into a contested mid-range.",
            falsifier = "If teammates shoot their normal percentage and the assists do not move, the " +
                "playmaking edge was already in the line.",
        ),
        ClaimFactor(
            tag = "shooting",
            title = "From deep",
            tiltPts = 2.0,
            side = SideFilter.OVER_ONLY,
            markets = listOf(Markets.THREES),
            triggers = listOf(
                "three", "threes", "3pt", "3-point", "shooting", "shooter", "from deep", "hot hand",
                "catch and shoot", "volume from three", "trey",
            ),
            gameTerm = "Three-point volume is the most repeatable part of a shooting line: a player " +
                "who takes eight a night needs a normal night, not a hot one, to clear a modest number.",
            falsifier = "If his attempts fall against this defense, the makes have to be unusually " +
                "efficient to bail the over out.",
        ),
        ClaimFactor(
            tag = "home",
            title = "Home floor",
            tiltPts = 1.0,
            side = SideFilter.OVER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "at home", "home crowd", "home game", "home court", "home floor", "in front of",
                "home fans", "home stand",
            ),
            gameTerm = "Home players keep their minutes, shoot slightly better and get the friendlier " +
                "whistle. It is a small edge, and the lens treats it as one.",
            falsifier = "If the home split is already in the line — and it usually is — the lean adds " +
                "nothing but variance.",
        ),
        ClaimFactor(
            tag = "mismatch",
            title = "Physical mismatch",
            tiltPts = 2.0,
            side = SideFilter.OVER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "mismatch", "too big", "too quick", "too strong", "can't guard", "cant guard",
                "cannot guard", "size advantage", "athletic advantage", "physically", "bully",
                "overpower", "too fast",
            ),
            gameTerm = "A matchup the defender cannot hold without help creates clean touches for the " +
                "offense — and help defense gives up the pass that beats it.",
            falsifier = "If the defense doubles him and forces the ball out of his hands, the mismatch " +
                "shows up as assists for somebody else, not stats for him.",
        ),
        ClaimFactor(
            tag = "foul_trouble",
            title = "Whistle risk",
            tiltPts = -2.0,
            side = SideFilter.UNDER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "foul trouble", "fouls", "refs", "referee", "whistles", "ejected", "technical",
                "in foul trouble", "pick up fouls",
            ),
            gameTerm = "Foul trouble is a minutes risk with no upside: two early fouls can turn a " +
                "34-minute night into 24, and the line never gets there.",
            falsifier = "If he stays on the floor and the whistles are normal, this lens has quietly " +
                "bet against a player for no reason.",
        ),
        ClaimFactor(
            tag = "clutch",
            title = "Close-game minutes",
            tiltPts = 1.5,
            side = SideFilter.OVER_ONLY,
            markets = listOf(Markets.POINTS),
            triggers = listOf(
                "clutch", "close game", "down to the wire", "overtime", "ot", "close one",
                "neck and neck", "shootout finish", "game comes down",
            ),
            gameTerm = "A close game keeps the best players on the floor and can add five extra minutes. " +
                "The line assumes a normal game, not a nail-biter.",
            falsifier = "If the game is decided early, the extra minutes never arrive and the over " +
                "needs a hot shooting night instead.",
        ),
        ClaimFactor(
            tag = "veteran_rest",
            title = "Load management",
            tiltPts = -3.0,
            side = SideFilter.UNDER_ONLY,
            markets = emptyList(),
            triggers = listOf(
                "load management", "resting", "scheduled rest", "managing his minutes", "preserve",
                "veteran rest", "not 100 percent", "not fully healthy", "nursing",
            ),
            gameTerm = "A team with nothing to gain from a big minute night will take one. That is a " +
                "direct cut to every counting stat, not a subtle efficiency effect.",
            falsifier = "If he plays his normal rotation, the load-management read was speculation.",
        ),
    )

    private val generic = ClaimFactor(
        tag = "generic",
        title = "Your read",
        tiltPts = 0.0,
        side = SideFilter.BOTH,
        markets = emptyList(),
        triggers = emptyList(),
        gameTerm = "No specific angle was recognised in the wording, so this lens runs the board at " +
            "the app's own probabilities and only filters by price and disagreement.",
        falsifier = "With no directional edge the lens is only a price filter; it should not be " +
            "expected to beat the market on its own.",
    )

    fun genericFactor(): ClaimFactor = generic

    fun byTag(tag: String): ClaimFactor = factors.firstOrNull { it.tag == tag } ?: generic
}
