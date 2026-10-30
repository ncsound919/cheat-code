package com.propslab.data

import com.propslab.domain.Circadian
import com.propslab.domain.LinePush
import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.ParlayBook
import com.propslab.domain.ParlayLeg
import com.propslab.domain.ParlayStaking
import com.propslab.domain.SameGame
import com.propslab.domain.Side
import com.propslab.domain.StakeAllocation
import com.propslab.domain.StakeOffer
import com.propslab.sim.StrategyParams
import kotlin.math.abs

/**
 * One thing a parlay can be built from. A prop and a game market differ in where the
 * number comes from, not in the arithmetic: both have a price and a chance, and a ticket
 * multiplies them.
 *
 * The extra fields are what let a leg be repriced after the line is pushed. `mean` and
 * `spread` come from the stored game log when the feed kept one; `overProb` is the
 * distribution at the quoted line either way. A game market carries no player game log, so
 * [pushable] is false and its line stays where the book put it.
 */
data class ParlayCandidate(
    val key: String,
    val gameId: String,
    /** The player for a prop, null for a game market. */
    val subject: String?,
    val label: String,
    val detail: String,
    val odds: Int,
    val pWin: Double,
    val pPush: Double,
    val edgePts: Double,
    val line: Double? = null,
    val side: Side = Side.OVER,
    /** The model's P(over) at the quoted line, used to reprice a push. */
    val overProb: Double = 0.0,
    val mean: Double? = null,
    val spread: Double? = null,
    val games: Int = 0,
    val teamCode: String? = null,
    val venueCode: String? = null,
    val startUtc: Long = 0L,
) {
    val pushable: Boolean get() = subject != null && line != null && line > 0.0
}

/** One leg as the screen shows it: the pick, and the chance and price after any adjustment. */
data class ParlayLegUi(
    val candidate: ParlayCandidate,
    val winChance: Double,
    val odds: Int = candidate.odds,
    val line: Double? = candidate.line,
    val pWin: Double = candidate.pWin,
    val pPush: Double = candidate.pPush,
    val shiftPts: Double = 0.0,
    val circadianPct: Double = 0.0,
    val inferred: Boolean = false,
    val note: String = "",
)

enum class ParlayKind { STRAIGHT, SAME_GAME }

/**
 * A ready-to-read ticket: which picks, in what order, the chance they all land, and what
 * the book would pay. Nothing here is stored — a parlay is a view of today's board, and a
 * board that changes should change the ticket with it.
 */
data class ParlayTicketUi(
    val size: Int,
    val legs: List<ParlayLegUi>,
    val probability: Double,
    val decimal: Double,
    val games: Int,
    val sameGame: Int,
    val anchor: ParlayCandidate,
    val propLegs: Int,
    val gameLegs: Int,
    val explanation: String,
    val kind: ParlayKind = ParlayKind.STRAIGHT,
    val strategyId: String? = null,
    val strategyLabel: String? = null,
    val pushPoints: Double = 0.0,
    val inferredLegs: Int = 0,
    /** For a same-game ticket, the product before the correlation adjustment. */
    val naiveProbability: Double? = null,
    val correlationShrink: Double = 0.0,
    val allocatedStake: Double = 0.0,
) {
    /** Stable identity for allocation and for per-ticket formula selection. */
    val key: String get() = if (kind == ParlayKind.SAME_GAME) "sgp" else size.toString()

    val returns: Double get() = allocatedStake * decimal
    val profit: Double get() = returns - allocatedStake
    val ev: Double get() = probability * (decimal - 1.0) - (1.0 - probability)
}

/** The knobs the builder applies to every leg. Defaults reproduce the old, unmodified ticket. */
data class ParlayOptions(
    val pushPoints: Double = 0.0,
    val circadian: Boolean = false,
    val strategyId: String? = null,
    val strategyLabel: String? = null,
)

/** One formula the user can build a ticket from, with its measured scope. */
data class ParlayStrategyUi(
    val id: String,
    val label: String,
    val tier: String,
    val params: StrategyParams,
    val picks: List<BestPickUi>,
    val scope: String,
    val profit: Double = 0.0,
) {
    val live: Boolean get() = tier == Tier.LIVE
}

/** The board the parlay lab reads: one ranked list per formula, plus the game markets. */
data class ParlayBoardUi(
    val strategies: List<ParlayStrategyUi> = emptyList(),
    val gamePicks: List<GamePickUi> = emptyList(),
    val live: Boolean = false,
    val note: String = "",
    val round: Int = 0,
    val priced: Int = 0,
) {
    val defaultStrategyId: String?
        get() = strategies.firstOrNull { it.live }?.id ?: strategies.firstOrNull()?.id
}

/** What the user has set on the screen. Kept as data so the computation is testable. */
data class ParlayControls(
    val pushPoints: Double = 0.0,
    val budget: String = "",
    val circadian: Boolean = true,
    /** Ticket key ("3", "5", "10", "sgp") to strategy id. Missing means the default. */
    val byTicket: Map<String, String> = emptyMap(),
)

data class ParlayUi(
    val strategies: List<ParlayStrategyUi> = emptyList(),
    val tickets: List<ParlayTicketUi> = emptyList(),
    /** The field's text as typed, so the input is never reformatted mid-keystroke. */
    val budgetText: String = "",
    val budget: Double = 0.0,
    val staked: Double = 0.0,
    val unallocated: Double = 0.0,
    val stakes: List<StakeAllocation> = emptyList(),
    val priced: Int = 0,
    val live: Boolean = false,
    val strategy: String? = null,
    val note: String = "",
    val loaded: Boolean = false,
    val pushPoints: Double = 0.0,
    val circadian: Boolean = true,
    val warnings: List<String> = emptyList(),
    /**
     * Ticket key ("3", "5", "10", "sgp") to the formulas that can actually fill it from
     * today's board. A formula listed nowhere else is shown, darkened and untappable, rather
     * than silently disappearing from the picker.
     */
    val eligible: Map<String, Set<String>> = emptyMap(),
    /** The priced candidates behind the cards, used by the slip's leg swap. */
    val candidates: List<ParlayCandidate> = emptyList(),
)

/**
 * Builds the day's tickets out of the same ranked board the Best screen shows.
 *
 * Both kinds of pick compete on the same edge ordering. The first pass takes one leg per
 * game, because legs from the same game move together and a ticket that looks like five
 * independent chances is not. Only when the board cannot supply that many different games
 * does the second pass add a leg from a game already used, and the explanation says so.
 */
object ParlayTickets {

    val SIZES = listOf(3, 5, 10)

    /** Turns the two ranked boards into one candidate list, sorted by model-versus-book gap. */
    fun candidates(picks: List<BestPickUi>, games: List<GamePickUi>): List<ParlayCandidate> {
        val props = picks.mapNotNull { pick ->
            val probs = pick.prop.probabilities ?: return@mapNotNull null
            if (!oddsSane(pick.odds)) return@mapNotNull null
            val pWin = probs.forSide(pick.side)
            if (pWin <= 0.0 || pWin + probs.push > 1.0) return@mapNotNull null
            val p = pick.prop
            ParlayCandidate(
                key = "p:${p.id}:${pick.side.name}",
                gameId = p.gameId,
                subject = p.playerName,
                label = "${p.playerName} ${pick.side.label} ${point(p.line)} " + Markets.label(p.market),
                detail = "${p.matchup} · ${p.book.ifBlank { "best price" }}",
                odds = pick.odds,
                pWin = pWin,
                pPush = probs.push,
                edgePts = pick.edgePts,
                line = p.line,
                side = pick.side,
                overProb = probs.over,
                mean = driver(p, "mean"),
                spread = driver(p, "spread"),
                games = (driver(p, "games") ?: 0.0).toInt(),
                teamCode = p.teamCode,
                venueCode = p.matchup.substringAfterLast(" at ", "").trim().takeIf { it.isNotBlank() },
                startUtc = p.startUtc,
            )
        }
        val markets = games.mapNotNull { pick ->
            if (!oddsSane(pick.odds)) return@mapNotNull null
            if (pick.chance <= 0.0 || pick.chance + pick.push > 1.0) return@mapNotNull null
            val over = pick.sideKey == "OVER" || pick.sideKey == "HOME"
            ParlayCandidate(
                key = "g:${pick.key}",
                gameId = pick.gameId,
                subject = null,
                label = pick.label,
                detail = "${pick.matchup} · ${pick.book.ifBlank { "best price" }}",
                odds = pick.odds,
                pWin = pick.chance,
                pPush = pick.push,
                edgePts = pick.edgePts ?: 0.0,
                line = pick.line,
                side = if (over) Side.OVER else Side.UNDER,
                overProb = if (over) pick.chance else (1.0 - pick.chance - pick.push).coerceAtLeast(0.0),
                venueCode = pick.homeCode,
                startUtc = pick.startUtc,
            )
        }
        return (props + markets)
            .filter { it.edgePts > 0.0 }
            .sortedByDescending { it.edgePts }
    }

    /**
     * The candidates a ticket may actually use. A quote whose two sides do not form a
     * distribution is dropped, as is a duplicate key, and nothing here is repriced.
     */
    private fun usable(candidates: List<ParlayCandidate>): List<ParlayCandidate> = candidates
        .filter { oddsSane(it.odds) && it.pWin > 0.0 && it.pWin + it.pPush <= 1.0 }
        .distinctBy { it.key }

    /**
     * Whether a formula can supply [size] legs at all. This is the check behind the darkened
     * chip: the builder's last pass will take any remaining usable candidate, so enough
     * usable legs is both necessary and sufficient.
     */
    fun canBuild(candidates: List<ParlayCandidate>, size: Int): Boolean =
        usable(candidates).size >= size

    /** Whether enough legs exist in one game for a same-game ticket of [size] legs. */
    fun canBuildSameGame(candidates: List<ParlayCandidate>, size: Int): Boolean {
        if (size < 2) return false
        return usable(candidates).groupBy { it.gameId }.values.any { oneEach(it).size >= size }
    }

    fun build(
        candidates: List<ParlayCandidate>,
        size: Int,
        options: ParlayOptions = ParlayOptions(),
    ): ParlayTicketUi? {
        val usable = usable(candidates)
        if (usable.size < size) return null

        val chosen = ArrayList<ParlayCandidate>(size)
        val games = HashSet<String>()
        // Reserve the game-market share first. Without this the board's largest prop edges fill
        // every slot and the game model never reaches a ticket, which would make the whole
        // game-scoring layer inert. One game leg in the short tickets, two in the long one.
        val gameSlots = if (size >= 10) 2 else 1
        for (candidate in usable.filter { it.subject == null }.take(gameSlots)) {
            if (games.add(candidate.gameId)) chosen += candidate
        }
        for (candidate in usable) {
            if (chosen.size >= size) break
            if (candidate.subject == null) continue
            if (chosen.any { it.key == candidate.key }) continue
            if (games.add(candidate.gameId)) chosen += candidate
        }
        if (chosen.size < size) {
            // Second pass: a different player, even if that means a second leg in a game
            // already used. Two markets on one player move together far more than two
            // players in one game do.
            for (candidate in usable) {
                if (chosen.size >= size) break
                if (chosen.any { it.key == candidate.key }) continue
                if (candidate.subject != null && chosen.any { it.subject == candidate.subject }) continue
                chosen += candidate
            }
        }
        if (chosen.size < size) {
            for (candidate in usable) {
                if (chosen.size >= size) break
                if (chosen.none { it.key == candidate.key }) chosen += candidate
            }
        }
        if (chosen.size < size) return null

        val legs = chosen.map { candidate -> priceLeg(candidate, options) }
        val built = ParlayBook.build(legs.map { leg -> ParlayLeg(odds = leg.odds, pWin = leg.winChance) })
            ?: return null

        val distinctGames = legs.map { it.candidate.gameId }.distinct().size
        val propLegs = legs.count { it.candidate.subject != null }
        val gameLegs = legs.size - propLegs
        val anchor = legs.maxByOrNull { it.candidate.edgePts }?.candidate ?: legs.first().candidate
        return ParlayTicketUi(
            size = size,
            legs = legs,
            probability = built.probability,
            decimal = built.decimal,
            games = distinctGames,
            sameGame = legs.size - distinctGames,
            anchor = anchor,
            propLegs = propLegs,
            gameLegs = gameLegs,
            explanation = explain(
                size = size,
                legs = legs,
                games = distinctGames,
                probability = built.probability,
                decimal = built.decimal,
                anchor = anchor,
                propLegs = propLegs,
                gameLegs = gameLegs,
                options = options,
            ),
            strategyId = options.strategyId,
            strategyLabel = options.strategyLabel,
            pushPoints = options.pushPoints,
            inferredLegs = legs.count { it.inferred },
        )
    }

    /**
 * A ticket made entirely of legs from one game.
 *
 * The system looks for the game that supplies the strongest [size] legs, one per player,
 * and prices it separately because a same-game ticket is not the product of its parts.
 * See [SameGame] for the direction and size of the correction.
     */
    fun buildSameGame(
        candidates: List<ParlayCandidate>,
        options: ParlayOptions = ParlayOptions(),
        size: Int = 3,
    ): ParlayTicketUi? {
        val usable = usable(candidates)
        if (usable.size < size) return null
        val groups = usable.groupBy { it.gameId }
            .mapValues { (_, legs) -> oneEach(legs).take(size) }
            .filterValues { it.size == size }
        if (groups.isEmpty()) return null
        val chosen = groups.maxByOrNull { (_, legs) -> legs.sumOf { it.edgePts } }?.value ?: return null

        val legs = chosen.map { candidate -> priceLeg(candidate, options) }
        val built = ParlayBook.build(legs.map { leg -> ParlayLeg(odds = leg.odds, pWin = leg.winChance) })
            ?: return null
        val subjects = chosen.mapNotNull { it.subject }
        val together = subjects.size != subjects.distinct().size
        val adjusted = SameGame.adjust(built.probability, built.decimal, together) ?: return null

        val propLegs = legs.count { it.candidate.subject != null }
        val gameLegs = legs.size - propLegs
        val anchor = legs.maxByOrNull { it.candidate.edgePts }?.candidate ?: legs.first().candidate
        val head = "${size} legs, one game, ${legs.map { it.candidate.gameId }.distinct().size} game. " +
            "Multiplied as if independent they would be ${pct(adjusted.naiveProbability)} and " +
            "${"%.2f".format(adjusted.naiveDecimal)}x."
        val correction = " Same-game legs move together, so the app lifts that chance to " +
            "${pct(adjusted.probability)} and cuts the price to ${"%.2f".format(adjusted.decimal)}x — " +
            "a stated ${(adjusted.shrink * 100).toInt()}% log-odds pull, not a measured covariance. " +
            "Books cut same-game payouts for the same reason."
        val mix = when {
            propLegs > 0 && gameLegs > 0 ->
                " The ticket mixes $propLegs prop${if (propLegs == 1) "" else "s"} with $gameLegs game " +
                    "market${if (gameLegs == 1) "" else "s"} from that game."
            gameLegs > 0 -> " Every leg is a game market from that game."
            else -> " Every leg is a player prop from that game."
        }
        return ParlayTicketUi(
            size = size,
            legs = legs,
            probability = adjusted.probability,
            decimal = adjusted.decimal,
            games = 1,
            sameGame = legs.size - 1,
            anchor = anchor,
            propLegs = propLegs,
            gameLegs = gameLegs,
            explanation = "$head$correction$mix The biggest gap in the game is ${anchor.label}.",
            kind = ParlayKind.SAME_GAME,
            strategyId = options.strategyId,
            strategyLabel = options.strategyLabel,
            pushPoints = options.pushPoints,
            inferredLegs = legs.count { it.inferred },
            naiveProbability = adjusted.naiveProbability,
            correlationShrink = adjusted.shrink,
        )
    }

    /**
     * Reprices one leg for a custom line move. Exposed so the slip can move a single leg
     * without changing the ticket the parlay screen generated.
     */
    fun reprice(
        candidate: ParlayCandidate,
        points: Double,
        circadian: Boolean = true,
    ): ParlayLegUi = priceLeg(candidate, ParlayOptions(pushPoints = points, circadian = circadian))

    /** Reprices one leg for the current push and circadian settings. */
    private fun priceLeg(candidate: ParlayCandidate, options: ParlayOptions): ParlayLegUi {
        val circadian = if (options.circadian && candidate.pushable) {
            Circadian.impact(candidate.startUtc, candidate.teamCode, candidate.venueCode)
        } else null
        val multiplier = circadian?.meanMultiplier ?: 1.0
        val points = if (candidate.pushable) options.pushPoints else 0.0
        val line = candidate.line
        val pushed = if (line != null && line > 0.0 && (points != 0.0 || abs(multiplier - 1.0) > 1e-12)) {
            LinePush.reprice(
                quotedOdds = candidate.odds,
                line = line,
                side = candidate.side,
                points = points,
                overProb = candidate.overProb,
                pushProb = candidate.pPush,
                mean = candidate.mean,
                spread = candidate.spread,
                meanMultiplier = multiplier,
            )
        } else null

        val notes = buildList {
            if (options.pushPoints != 0.0 && !candidate.pushable) {
                add("No player game log, so this leg keeps its quoted line.")
            }
            if ((options.pushPoints != 0.0 || abs(multiplier - 1.0) > 1e-12) && pushed == null) {
                add("This leg could not be repriced, so it keeps its quote.")
            }
            if (pushed?.inferredCurve == true && points != 0.0) {
                add("The pushed price is inferred from this row's own probability, not a stored game log.")
            }
            if (circadian != null && circadian.offProjectionPct >= 0.05) add(circadian.summary)
        }
        return ParlayLegUi(
            candidate = candidate,
            winChance = pushed?.winNoPush ?: candidate.winNoPush(),
            odds = pushed?.odds ?: candidate.odds,
            line = pushed?.line ?: line,
            pWin = pushed?.probability ?: candidate.pWin,
            pPush = pushed?.push ?: candidate.pPush,
            shiftPts = points,
            circadianPct = circadian?.offProjectionPct ?: 0.0,
            inferred = pushed?.inferredCurve == true,
            note = notes.joinToString(" "),
        )
    }

    private fun explain(
        size: Int,
        legs: List<ParlayLegUi>,
        games: Int,
        probability: Double,
        decimal: Double,
        anchor: ParlayCandidate,
        propLegs: Int,
        gameLegs: Int,
        options: ParlayOptions,
    ): String {
        val head = "$size legs from $games games. Multiply every leg's chance and you get ${pct(probability)}; " +
            "one unit returns ${"%.2f".format(decimal)} units only if all $size land."
        val mix = when {
            propLegs > 0 && gameLegs > 0 ->
                " The ticket mixes $propLegs prop${if (propLegs == 1) "" else "s"} with $gameLegs game " +
                    "market${if (gameLegs == 1) "" else "s"}; a game result feeds every market in that game."
            gameLegs > 0 -> " Every leg is a game market: moneyline, spread or total, priced from team form."
            else -> " Every leg is a player prop."
        }
        val shared = legs.size - games
        val correlation = if (shared > 0) {
            val word = if (shared == 1) "leg sits" else "legs sit"
            " $shared $word in a game already used, so those legs are correlated and the real chance is not exactly this product."
        } else {
            " Every leg is in a different game, so the legs are close to independent."
        }
        val anchorText = " The biggest gap on the ticket is ${anchor.label}."
        val pushable = legs.count { it.candidate.pushable }
        val push = if (options.pushPoints != 0.0) {
            " Every pushable leg was moved ${"%.1f".format(options.pushPoints)} points against you, out for a " +
                "longer price; $pushable of $size legs could be moved and the rest kept their quote. " +
                "The bigger price is bought with a lower chance, not given away."
        } else ""
        val circadian = if (options.circadian && legs.any { it.circadianPct > 0.0 }) {
            " A circadian correction (up to 1.5% of the projection) is applied to each player leg from tip-off hour and travel."
        } else ""
        val formula = options.strategyLabel?.takeIf { it.isNotBlank() }?.let {
            " Built from the \"$it\" formula; the formula chooses legs, it never changes a price."
        } ?: ""
        val lottery = if (probability < 0.05) {
            " At this chance it is a lottery ticket: the price is large because the ticket is unlikely."
        } else ""
        return "$head$mix$anchorText$correlation$push$circadian$formula$lottery"
    }

    private fun oneEach(legs: List<ParlayCandidate>): List<ParlayCandidate> {
        val seen = HashSet<String>()
        return legs.filter { seen.add(it.subject ?: "game:${it.key}") }
    }

    private fun driver(prop: PropUi, label: String): Double? =
        prop.drivers.firstOrNull { it.label == label }?.value

    private fun ParlayCandidate.winNoPush(): Double =
        if (pPush >= 1.0) 0.0 else (pWin / (1.0 - pPush)).coerceIn(0.0, 1.0)

    private fun point(line: Double?): String {
        if (line == null) return ""
        return if (abs(line - Math.rint(line)) < 1e-9) "%.0f".format(line) else "%.1f".format(line)
    }

    private fun oddsSane(odds: Int): Boolean = odds >= 100 || odds <= -100

    private fun pct(p: Double): String = when {
        p >= 0.1 -> "%.0f%%".format(p * 100)
        p >= 0.01 -> "%.1f%%".format(p * 100)
        else -> "%.2f%%".format(p * 100)
    }
}

/**
 * What a formula is allowed to do, in words, beside what the other formulas do.
 *
 * This is the part that stops a formula picker from being a mystery toggle. Each line names
 * the four numbers the formula actually owns — the edge it demands, the sides it may take,
 * the price band it accepts, and any tilt — plus how many props on today's board clear it.
 */
object FormulaScope {

    fun describe(
        params: StrategyParams,
        qualified: Int,
        considered: Int,
        widest: Pair<StrategyParams, Int>? = null,
    ): String {
        val edge = if (params.minEdgePts <= 0.0) "any gap over the no-vig price"
        else "at least ${one(params.minEdgePts)} points of gap over the no-vig price"
        val sides = when (params.sideFilter) {
            com.propslab.sim.SideFilter.BOTH -> "both sides"
            com.propslab.sim.SideFilter.OVER_ONLY -> "overs only"
            com.propslab.sim.SideFilter.UNDER_ONLY -> "unders only"
        }
        val price = "prices from ${two(params.minPrice)}x to ${two(params.maxPrice)}x"
        val tilt = if (abs(params.tiltPts) >= 0.05) {
            ", and shifts the model ${signed(params.tiltPts)} points for its own claim"
        } else ""
        val markets = when {
            params.markets.isNotEmpty() && params.excludeMarkets.isNotEmpty() ->
                params.markets.joinToString(", ") { Markets.label(it) } +
                    " except " + params.excludeMarkets.joinToString(", ") { Markets.label(it) }
            params.markets.isNotEmpty() -> params.markets.joinToString(", ") { Markets.label(it) }
            params.excludeMarkets.isNotEmpty() ->
                "every market except " + params.excludeMarkets.joinToString(", ") { Markets.label(it) }
            else -> "every market"
        }
        val head = "Scope: takes $sides $edge, $price$tilt, on $markets. " +
            "Today it qualifies $qualified of $considered priced props."
        val compare = if (widest != null && widest.first.id != params.id) {
            val wider = widest.first.minEdgePts < params.minEdgePts
            val direction = if (wider) "stricter" else "looser"
            val legs = if (wider) "fewer, stronger legs" else "more, thinner legs"
            " Its neighbour \"${widest.first.label}\" takes ${widest.second} props: this one is $direction, " +
                "so tickets built from it arrive with $legs."
        } else {
            " It is the broadest formula on the board, so it supplies the longest tickets."
        }
        return "$head$compare It chooses legs only; a price or a line is never its to change."
    }

    private fun one(v: Double): String = "%.1f".format(v)
    private fun two(v: Double): String = "%.2f".format(v)
    private fun signed(v: Double): String = "%+.1f".format(v)
}

/**
 * Ties the board and the controls into what the screen renders.
 *
 * Every ticket is built from the formula the user assigned to it, or the default one. The
 * budget is then split across those tickets at the settings screen's Kelly fraction, capped
 * per ticket, and whatever the edges do not justify stays as unallocated cash.
 */
object ParlayLab {

    fun compose(
        board: ParlayBoardUi,
        controls: ParlayControls,
        sizes: List<Int> = ParlayTickets.SIZES,
        stakeFraction: Double = 0.25,
    ): ParlayUi {
        val fallback = board.defaultStrategyId
        // Built once per formula: the picker, the eligibility map and the tickets all read
        // the same candidate list, so a chip can never disagree with the card it builds.
        val candidatesByStrategy = board.strategies.associate { strategy ->
            strategy.id to ParlayTickets.candidates(strategy.picks, board.gamePicks)
        }
        fun candidatesOf(strategy: ParlayStrategyUi): List<ParlayCandidate> =
            candidatesByStrategy[strategy.id].orEmpty()
        fun canFill(strategy: ParlayStrategyUi, size: Int, sameGame: Boolean): Boolean {
            val candidates = candidatesOf(strategy)
            return if (sameGame) {
                ParlayTickets.canBuildSameGame(candidates, size) ||
                    (size == 3 && ParlayTickets.canBuildSameGame(candidates, 2))
            } else {
                ParlayTickets.canBuild(candidates, size)
            }
        }
        val fallbacks = ArrayList<String>()
        fun strategyFor(key: String, size: Int, sameGame: Boolean = false): ParlayStrategyUi? {
            val requestedId = controls.byTicket[key] ?: fallback
            val requested = board.strategies.firstOrNull { it.id == requestedId }
            if (requested != null && canFill(requested, size, sameGame)) return requested
            val chosen = board.strategies.firstOrNull { canFill(it, size, sameGame) }
            if (requested != null && chosen != null && requested.id != chosen.id) {
                fallbacks += "\"${requested.label}\" cannot fill the ${if (sameGame) "same-game" else "$size-leg"} " +
                    "ticket from today's board, so that card was built from \"${chosen.label}\" instead."
            }
            return chosen
        }

        val eligible = LinkedHashMap<String, Set<String>>()

        val tickets = buildList {
            for (size in sizes) {
                val key = size.toString()
                eligible[key] = board.strategies.filter { canFill(it, size, false) }.map { it.id }.toSet()
                val strategy = strategyFor(key, size) ?: continue
                val options = ParlayOptions(
                    pushPoints = controls.pushPoints,
                    circadian = controls.circadian,
                    strategyId = strategy.id,
                    strategyLabel = strategy.label,
                )
                ParlayTickets.build(candidatesOf(strategy), size, options)?.let { add(it) }
            }
            // The same-game ticket is a three-leg ticket by definition; it is offered only
            // when the free build already includes the three-leg size.
            if (sizes.contains(3)) {
                eligible["sgp"] = board.strategies.filter { canFill(it, 3, true) }.map { it.id }.toSet()
                val strategy = strategyFor("sgp", 3, sameGame = true) ?: return@buildList
                val options = ParlayOptions(
                    pushPoints = controls.pushPoints,
                    circadian = controls.circadian,
                    strategyId = strategy.id,
                    strategyLabel = strategy.label,
                )
                // Three legs is the usual same-game ticket; a thin board that supplies only
                // two props in one game can still show the correlation, and the card says
                // what it priced.
                (ParlayTickets.buildSameGame(candidatesOf(strategy), options, size = 3)
                    ?: ParlayTickets.buildSameGame(candidatesOf(strategy), options, size = 2))
                    ?.let { add(it) }
            }
        }

        val budget = controls.budget.toDoubleOrNull()?.coerceIn(0.0, 1_000_000.0) ?: 0.0
        val stakes = ParlayStaking.allocate(
            budget = budget,
            offers = tickets.map { StakeOffer(it.key, it.probability, it.decimal) },
            fraction = stakeFraction,
        )
        val byKey = stakes.associate { it.key to it.stake }
        val staked = stakes.sumOf { it.stake }
        val withStakes = tickets.map { it.copy(allocatedStake = byKey[it.key] ?: 0.0) }

        val warnings = buildList {
            if (board.strategies.isEmpty()) {
                add("The sandbox has not produced a formula yet, so there is nothing to build a ticket from.")
            }
            if (controls.pushPoints > 0.0 && withStakes.any { it.inferredLegs > 0 }) {
                add("Some pushed prices are inferred from the row's probability because no game log is stored for it. They are estimates, not quotes.")
            }
            if (budget > 0.0 && withStakes.isNotEmpty() && staked <= 0.0) {
                add("No ticket has positive expected value at these prices, so the budget is not staked.")
            }
            addAll(fallbacks)
        }

        return ParlayUi(
            strategies = board.strategies,
            tickets = withStakes,
            budgetText = controls.budget,
            budget = budget,
            staked = staked,
            unallocated = (budget - staked).coerceAtLeast(0.0),
            stakes = stakes,
            priced = board.priced,
            live = board.live,
            strategy = board.strategies.firstOrNull { it.id == fallback }?.label,
            note = board.note,
            loaded = true,
            pushPoints = controls.pushPoints,
            circadian = controls.circadian,
            warnings = warnings,
            eligible = eligible,
            candidates = candidatesByStrategy.values.flatten().distinctBy { it.key },
        )
    }

}
