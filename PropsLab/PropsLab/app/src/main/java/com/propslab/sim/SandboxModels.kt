package com.propslab.sim

import com.propslab.domain.Odds
import com.propslab.domain.Outcome
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.Pricing
import com.propslab.domain.Side
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A prop as the sandbox sees it: two prices, the model distribution, and — for props
 * whose result is known — the realised number.
 *
 * `synthetic` marks a prop whose result was produced by [SandboxTruth] rather than by a
 * recorded game. Nothing in the sandbox treats the two differently; the flag exists so
 * the UI can say which one a row is.
 */
data class SimProp(
    val id: String,
    val playerName: String,
    val line: Double,
    val overOdds: Int,
    val underOdds: Int,
    val pOver: Double,
    val pPush: Double,
    val pUnder: Double,
    val startUtc: Long,
    val actual: Double?,
    val synthetic: Boolean,
    /**
     * The market this prop is written on, when the caller knows it. Empty means the prop
     * belongs to no particular market, which is how the simulated season is built: a lens
     * that names a market only restricts real rows, never the synthetic bench.
     */
    val market: String = "",
) {
    /** Never throws: an unusable distribution simply cannot be bet. */
    val probabilities: OutcomeProbabilities?
        get() = runCatching { OutcomeProbabilities(pOver, pPush, pUnder) }.getOrNull()

    val marketOver: Double?
        get() = runCatching { Odds.noVigOver(overOdds, underOdds) }.getOrNull()

    fun oddsFor(side: Side): Int = if (side == Side.OVER) overOdds else underOdds
}

enum class SideFilter(val label: String, val tag: String) {
    BOTH("Both sides", "both"),
    OVER_ONLY("Over only", "over"),
    UNDER_ONLY("Under only", "under");

    val allowsOver: Boolean get() = this != UNDER_ONLY
    val allowsUnder: Boolean get() = this != OVER_ONLY
}

/**
 * A formula in the sandbox is a selection rule, not an executable expression: how much
 * model-versus-market disagreement it demands, which side it is allowed to take, and
 * the price band it will accept. That is a small enough space to search exhaustively
 * and to mutate later without ever evaluating a string as code.
 *
 * Stakes are flat (one unit) so that two formulas are compared on the same turnover;
 * compounding would make a lucky early run look like an edge.
 */
data class StrategyParams(
    val minEdgePts: Double,
    val sideFilter: SideFilter = SideFilter.BOTH,
    val minPrice: Double = 1.5,
    val maxPrice: Double = 2.6,
    /**
     * How far this formula shifts the model's over probability, in probability points,
     * before it is compared to the price. A claim such as "their defense decides this"
     * compiles to a negative tilt; the plain grid leaves it at zero and is unchanged.
     */
    val tiltPts: Double = 0.0,
    /** Markets this formula is allowed to touch. Empty means every market. */
    val markets: List<String> = emptyList(),
    /** Markets a learned lesson has removed. Inclusion wins, then exclusion wins over that. */
    val excludeMarkets: List<String> = emptyList(),
) {
    private val customPriceBand: Boolean
        get() = kotlin.math.abs(minPrice - 1.5) > 1e-9 || kotlin.math.abs(maxPrice - 2.6) > 1e-9
    /**
     * Stable, locale-free id. It is a primary key, so it must not change with the device
     * locale: a comma decimal separator would fork every formula row. The suffix is only
     * added when a lens actually differs from the plain edge grid, so existing ids stay put.
     */
    val id: String
        get() = buildString {
            append("e${one(minEdgePts)}-${sideFilter.tag}")
            if (kotlin.math.abs(tiltPts) >= 0.05) append("-t${one(tiltPts)}")
            if (markets.isNotEmpty()) append("-m").append(marketTag)
            if (excludeMarkets.isNotEmpty()) append("-x").append(excludeTag)
            if (customPriceBand) append("-p").append(priceTag)
        }

    val label: String
        get() = buildString {
            append(if (minEdgePts <= 0.0) "Any disagreement" else "Edge \u2265 ${one(minEdgePts)} pts")
            append(", ")
            append(sideFilter.label.lowercase())
            if (kotlin.math.abs(tiltPts) >= 0.05) append(", tilted ").append(signedOne(tiltPts)).append(" pts")
            if (markets.isNotEmpty()) append(", ").append(markets.joinToString("/") { it.removePrefix("player_") })
            if (excludeMarkets.isNotEmpty()) append(", without ").append(excludeMarkets.joinToString("/") { it.removePrefix("player_") })
            if (customPriceBand) append(", price ").append(two(minPrice)).append("x-").append(two(maxPrice)).append("x")
        }

    val paramsText: String
        get() = buildString {
            append("edge=${one(minEdgePts)};side=${sideFilter.tag};price=${two(minPrice)}..${two(maxPrice)}")
            append(";tilt=${one(tiltPts)}")
            if (markets.isNotEmpty()) append(";markets=").append(markets.joinToString(","))
            if (excludeMarkets.isNotEmpty()) append(";exclude=").append(excludeMarkets.joinToString(","))
        }

    private val marketTag: String
        get() = markets.joinToString("") { it.removePrefix("player_").take(2) }

    private val excludeTag: String
        get() = excludeMarkets.joinToString("") { it.removePrefix("player_").take(2) }

    /** Hundredths, so the id is locale-free and stable no matter how the price was typed. */
    private val priceTag: String
        get() = "${Math.round(minPrice * 100)}-${Math.round(maxPrice * 100)}"

    private fun one(v: Double): String = String.format(java.util.Locale.ROOT, "%.1f", v)
    private fun two(v: Double): String = String.format(java.util.Locale.ROOT, "%.2f", v)
    private fun signedOne(v: Double): String = String.format(java.util.Locale.ROOT, "%+.1f", v)

    companion object {
        /**
         * Reads back what [paramsText] wrote. A row written before tilts existed has no
         * `tilt` field and reads back as the plain grid, which is exactly what it was.
         */
        fun parse(text: String): StrategyParams? {
            val fields = text.split(';')
                .mapNotNull { part ->
                    val at = part.indexOf('=')
                    if (at <= 0) null else part.substring(0, at).trim() to part.substring(at + 1).trim()
                }
                .toMap()
            val edge = fields["edge"]?.toDoubleOrNull() ?: return null
            val side = SideFilter.entries.firstOrNull { it.tag == fields["side"] } ?: SideFilter.BOTH
            val price = fields["price"]?.split("..")?.mapNotNull { it.toDoubleOrNull() }
            val minPrice = price?.getOrNull(0) ?: 1.5
            val maxPrice = price?.getOrNull(1) ?: 2.6
            val tilt = fields["tilt"]?.toDoubleOrNull() ?: 0.0
            val markets = fields["markets"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            val excluded = fields["exclude"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            return StrategyParams(
                minEdgePts = edge,
                sideFilter = side,
                minPrice = minPrice,
                maxPrice = maxPrice,
                tiltPts = tilt,
                markets = markets,
                excludeMarkets = excluded,
            )
        }
    }
}

data class Decision(val side: Side, val odds: Int, val edgePts: Double, val implied: Double)

data class SimTrade(
    val formulaId: String,
    val propId: String,
    val side: Side,
    val line: Double,
    val odds: Int,
    val stake: Double,
    val outcome: Outcome,
    val profit: Double,
    val startUtc: Long,
    val bankrollAfter: Double,
)

/**
 * What a formula did over a set of props.
 *
 * `tStat` is the whole point: profit alone cannot distinguish an edge from a lucky
 * streak, so every formula is reported with the t-statistic of its per-bet return on
 * stake. A candidate that cannot clear 2.0 is not evidence of anything.
 */
data class SimStats(
    val bets: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val pushes: Int = 0,
    val staked: Double = 0.0,
    val profit: Double = 0.0,
    val tStat: Double = 0.0,
    val maxDrawdown: Double = 0.0,
    val finalBankroll: Double = 0.0,
    val peak: Double = 0.0,
    val lowest: Double = 0.0,
    val avgImplied: Double = 0.0,
    val ruined: Boolean = false,
) {
    val roi: Double get() = if (staked > 0.0) profit / staked else 0.0
    val decisions: Int get() = wins + losses
    val winRate: Double? get() = if (decisions > 0) wins.toDouble() / decisions else null

    /** The hit rate a bettor needs at the prices this formula actually took. */
    val breakeven: Double get() = avgImplied

    val winRateOverBreakeven: Double? get() = winRate?.let { it - breakeven }
}

data class SimResult(
    val params: StrategyParams,
    val stats: SimStats,
    val trades: List<SimTrade>,
)

/** Deterministic 64-bit hash. Every draw in the sandbox comes from here, so runs repeat exactly. */
internal object SandboxHash {

    private val OFFSET = 0xcbf29ce484222325UL
    private val PRIME = 0x100000001b3UL
    private val GAMMA = 0x9e3779b97f4a7c15UL
    private val MIX1 = 0xbf58476d1ce4e5b9UL
    private val MIX2 = 0x94d049bb133111ebUL

    fun bits(key: String): ULong {
        var h = OFFSET
        for (ch in key) {
            h = (h xor ch.code.toULong()) * PRIME
        }
        var z = h + GAMMA
        z = (z xor (z shr 30)) * MIX1
        z = (z xor (z shr 27)) * MIX2
        return z xor (z shr 31)
    }

    /** Uniform in [0, 1). 53 bits, so it spans doubles without clustering. */
    fun unit(key: String): Double = (bits(key) shr 11).toDouble() / (1UL shl 53).toDouble()

    fun pick(key: String, from: Int, until: Int): Int =
        if (until <= from) from else from + (bits(key) % (until - from).toULong()).toInt()

    fun signed(key: String, magnitude: Double): Double = (unit(key) - 0.5) * 2.0 * magnitude
}

/** Shared arithmetic so the engine and the gate never disagree about what a bet returned. */
internal fun unitStats(perBet: List<Double>): Pair<Double, Double> {
    val n = perBet.size
    if (n < 2) return 0.0 to 0.0
    val mean = perBet.average()
    val variance = perBet.sumOf { (it - mean) * (it - mean) } / (n - 1)
    val sd = sqrt(variance)
    return mean to sd
}

internal fun edgeOf(prop: SimProp): Double? {
    val probs = prop.probabilities ?: return null
    val market = prop.marketOver ?: return null
    return runCatching { Pricing.edgePoints(probs.overGivenNoPush, market) }.getOrNull()
}

internal fun absEdgeOf(prop: SimProp): Double = abs(edgeOf(prop) ?: 0.0)
