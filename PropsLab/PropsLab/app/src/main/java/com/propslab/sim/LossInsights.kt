package com.propslab.sim

import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.Outcome
import com.propslab.domain.Settlement
import com.propslab.domain.Side
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * One settled prop, reduced to the fields a formula can actually react to.
 *
 * The ledger is the right input because it records every priced lean, not only the picks a
 * formula was willing to take. That matters for learning: a losing side or price band is
 * visible even when the live formula avoided it, and a winning pattern is visible before a
 * formula exists to exploit it.
 */
data class LossObservation(
    val market: String,
    val side: Side,
    val line: Double,
    val odds: Int,
    val edgePts: Double?,
    val outcome: Outcome,
    val startUtc: Long,
)

enum class InsightKind(val label: String) {
    LEAK("Leak"),
    EDGE("Edge"),
    PATTERN("Pattern"),
    TREND("Trend"),
}

/** One named observation about the settled record, with the candidate it implies if any. */
data class LossFinding(
    val id: String,
    val dimension: String,
    val kind: InsightKind,
    val title: String,
    val detail: String,
    val advice: String,
    val sample: Int,
    val wins: Int,
    val losses: Int,
    val roi: Double,
    val hitRate: Double,
    val breakeven: Double,
    val proposal: StrategyParams? = null,
) {
    val proposalLabel: String? get() = proposal?.label
}

data class LossInsightReport(
    val findings: List<LossFinding> = emptyList(),
    val proposals: List<StrategyParams> = emptyList(),
    val considered: Int = 0,
    val note: String = "",
) {
    val weaknesses: List<LossFinding> get() = findings.filter { it.kind == InsightKind.LEAK || it.kind == InsightKind.TREND }
    val patterns: List<LossFinding> get() = findings.filter { it.kind == InsightKind.EDGE || it.kind == InsightKind.PATTERN }
}

/**
 * Finds where the settled record is leaning in one direction, then turns the strongest
 * findings into candidate rules for the next sandbox round.
 *
 * This is deliberately descriptive, not predictive. Every finding is a segment with a
 * minimum sample, its wins, losses, return on one-unit stakes and hit rate against the
 * break-even its own prices demanded. Nothing here promotes a formula or changes a live
 * probability: a proposal is only ever a new candidate, and the promotion gate has to
 * clear it on a season the candidate has never seen. That is the difference between
 * noticing a losing pattern and fitting it.
 */
object LossInsights {

    /** Below this, a segment is an anecdote rather than a trend worth testing. */
    const val MIN_SAMPLE = 12

    private const val MAX_FINDINGS = 6
    private const val MAX_PROPOSALS = 4

    private data class PriceBand(val key: String, val label: String, val min: Double, val max: Double) {
        fun contains(decimal: Double): Boolean = decimal >= min && decimal < max
    }

    private val priceBands = listOf(
        PriceBand("short", "Short prices", 1.0, 1.80),
        PriceBand("near", "Near-even prices", 1.80, 2.20),
        PriceBand("long", "Long prices", 2.20, 100.0),
    )

    private data class EdgeBand(val key: String, val label: String, val min: Double, val max: Double) {
        fun contains(edge: Double): Boolean = edge >= min && edge < max
    }

    private val edgeBands = listOf(
        EdgeBand("low", "Low-edge leans", 0.0, 1.0),
        EdgeBand("mid", "Mid-edge leans", 1.0, 3.0),
        EdgeBand("high", "High-edge leans", 3.0, 100.0),
    )

    private enum class Verdict { LEAK, EDGE }

    private class Stats(rows: List<LossObservation>) {
        val sample: Int = rows.size
        val wins: Int = rows.count { it.outcome == Outcome.WIN }
        val losses: Int = rows.count { it.outcome == Outcome.LOSS }
        val profit: Double = rows.sumOf { Settlement.profit(it.outcome, 1.0, it.odds) }
        val roi: Double = if (sample == 0) 0.0 else profit / sample
        val hitRate: Double = if (sample == 0) 0.0 else wins.toDouble() / sample
        val breakeven: Double = if (sample == 0) {
            0.0
        } else {
            rows.sumOf { 1.0 / Odds.americanToDecimal(it.odds) } / sample
        }
    }

    private class Draft(val finding: LossFinding, val score: Double)

    fun analyze(
        observations: List<LossObservation>,
        seed: StrategyParams = StrategyParams(minEdgePts = 2.0),
        minSample: Int = MIN_SAMPLE,
    ): LossInsightReport {
        val usable = observations.filter {
            (it.outcome == Outcome.WIN || it.outcome == Outcome.LOSS) &&
                runCatching { Odds.americanToDecimal(it.odds) }.isSuccess
        }
        if (usable.size < minSample) {
            val needed = minSample - usable.size
            return LossInsightReport(
                considered = usable.size,
                note = "Only ${usable.size} settled prop${plural(usable.size)}. " +
                    "$needed more ${if (needed == 1) "is" else "are"} needed before a segment can be called a trend.",
            )
        }

        val overall = Stats(usable)
        val drafts = ArrayList<Draft>()
        val seen = HashSet<String>()

        fun add(finding: LossFinding, score: Double) {
            if (seen.add(finding.id)) drafts += Draft(finding, score)
        }

        // Side. A weak side and its strong mirror describe the same fact, so only one is
        // kept, and the leak wins: the pass exists to learn from what went wrong.
        val sideDrafts = ArrayList<Draft>()
        for (side in listOf(Side.OVER, Side.UNDER)) {
            val stats = Stats(usable.filter { it.side == side })
            val verdict = verdict(stats, minSample) ?: continue
            val label = if (side == Side.OVER) "Overs" else "Unders"
            val proposal = sideProposal(seed, side, verdict)
            val draft = Draft(
                finding = finding(
                    id = "side-${side.name.lowercase()}",
                    dimension = "side",
                    kind = if (verdict == Verdict.LEAK) InsightKind.LEAK else InsightKind.EDGE,
                    title = if (verdict == Verdict.LEAK) "$label are losing" else "$label are paying",
                    stats = stats,
                    advice = advice(proposal, verdict),
                    proposal = proposal,
                ),
                score = score(stats, overall),
            )
            sideDrafts += draft
        }
        val bestSide = sideDrafts.filter { it.finding.kind == InsightKind.LEAK }.maxByOrNull { it.score }
            ?: sideDrafts.maxByOrNull { it.score }
        bestSide?.let { add(it.finding, it.score) }

        // Price band.
        for (band in priceBands) {
            val stats = Stats(usable.filter { band.contains(decimalOf(it)) })
            val verdict = verdict(stats, minSample) ?: continue
            val proposal = priceProposal(seed, band.key, verdict)
            add(
                finding = finding(
                    id = "price-${band.key}",
                    dimension = "price",
                    kind = if (verdict == Verdict.LEAK) InsightKind.LEAK else InsightKind.EDGE,
                    title = if (verdict == Verdict.LEAK) "${band.label} are leaking" else "${band.label} are paying",
                    stats = stats,
                    advice = advice(proposal, verdict),
                    proposal = proposal,
                ),
                score = score(stats, overall),
            )
        }

        // Edge size. Only rows whose ledger recorded an edge are eligible.
        for (band in edgeBands) {
            val stats = Stats(
                usable.filter { row -> row.edgePts?.let { band.contains(abs(it)) } == true },
            )
            val verdict = verdict(stats, minSample) ?: continue
            val proposal = edgeProposal(seed, band.key, verdict)
            add(
                finding = finding(
                    id = "edge-${band.key}",
                    dimension = "edge",
                    kind = if (verdict == Verdict.LEAK) InsightKind.LEAK else InsightKind.EDGE,
                    title = if (verdict == Verdict.LEAK) "${band.label} are losing" else "${band.label} are paying",
                    stats = stats,
                    advice = advice(proposal, verdict),
                    proposal = proposal,
                ),
                score = score(stats, overall),
            )
        }

        // Market. A losing market can be excluded; a winning one can become its own
        // single-market formula rather than lifting the whole board.
        for ((market, rows) in usable.filter { it.market.isNotBlank() }.groupBy { it.market }) {
            val stats = Stats(rows)
            val verdict = verdict(stats, minSample) ?: continue
            val label = marketLabel(market)
            val proposal = marketProposal(seed, market, verdict)
            add(
                finding = finding(
                    id = "market-$market",
                    dimension = "market",
                    kind = if (verdict == Verdict.LEAK) InsightKind.LEAK else InsightKind.EDGE,
                    title = if (verdict == Verdict.LEAK) "$label are losing" else "$label are paying",
                    stats = stats,
                    advice = advice(proposal, verdict),
                    proposal = proposal,
                ),
                score = score(stats, overall),
            )
        }

        // Line shape. No parameter targets it yet, so this is recorded rather than turned
        // into a candidate; the advice says exactly that.
        for ((whole, rows) in usable.groupBy { it.line % 1.0 == 0.0 }) {
            val stats = Stats(rows)
            val verdict = verdict(stats, minSample) ?: continue
            val label = if (whole) "Whole-number lines" else "Half-point lines"
            add(
                finding = finding(
                    id = "line-${if (whole) "whole" else "half"}",
                    dimension = "line",
                    kind = if (verdict == Verdict.LEAK) InsightKind.LEAK else InsightKind.EDGE,
                    title = if (verdict == Verdict.LEAK) "$label are losing" else "$label are paying",
                    stats = stats,
                    advice = "No parameter targets a line shape yet; this is a record, not a tested change.",
                    proposal = null,
                ),
                score = score(stats, overall),
            )
        }

        // Recent form versus the window before it. A trend is a warning about the current
        // run, not a new rule on its own.
        val ordered = usable.sortedWith(compareBy({ it.startUtc }, { it.market }))
        val window = maxOf(minSample, ordered.size / 4)
        if (ordered.size >= window * 2) {
            val recent = Stats(ordered.takeLast(window))
            val prior = Stats(ordered.dropLast(window).takeLast(window))
            val difference = recent.roi - prior.roi
            if (recent.sample >= minSample && prior.sample >= minSample && abs(difference) >= 0.10) {
                val isLeak = difference < 0.0
                val draft = Draft(
                    finding = LossFinding(
                        id = "trend-recent",
                        dimension = "trend",
                        kind = if (isLeak) InsightKind.TREND else InsightKind.EDGE,
                        title = if (isLeak) "Recent bets are worse than earlier ones"
                        else "Recent bets are improving",
                        detail = "Last ${recent.sample}: ${evidence(recent)} " +
                            "The previous ${prior.sample}: ${evidence(prior)}",
                        advice = if (isLeak) {
                            "The live formula keeps its place until a candidate clears the gate; this is the " +
                                "signal that a tighter edge bar is worth testing."
                        } else {
                            "Form is improving, but that is not yet a reason to loosen a rule."
                        },
                        sample = recent.sample,
                        wins = recent.wins,
                        losses = recent.losses,
                        roi = recent.roi,
                        hitRate = recent.hitRate,
                        breakeven = recent.breakeven,
                        proposal = if (isLeak) {
                            seed.copy(minEdgePts = (seed.minEdgePts + 1.0).coerceAtMost(12.0))
                                .takeIf { it != seed }
                        } else {
                            null
                        },
                    ),
                    score = abs(difference) * sqrt(recent.sample.toDouble()),
                )
                add(draft.finding, draft.score)
            }
        }

        // A side and a price band that are both paying are a new formula, not just two
        // observations. The bar is double the base sample because the cell is smaller.
        for (side in listOf(Side.OVER, Side.UNDER)) {
            for (band in priceBands) {
                val stats = Stats(
                    usable.filter { it.side == side && band.contains(decimalOf(it)) },
                )
                if (stats.sample < minSample * 2 || verdict(stats, minSample * 2) != Verdict.EDGE) continue
                val proposal = priceProposal(seed, band.key, Verdict.EDGE)?.copy(
                    sideFilter = if (side == Side.OVER) SideFilter.OVER_ONLY else SideFilter.UNDER_ONLY,
                )?.takeIf { it != seed }
                if (proposal == null) continue
                val sideLabel = if (side == Side.OVER) "Overs" else "Unders"
                add(
                    finding = finding(
                        id = "pattern-${side.name.lowercase()}-${band.key}",
                        dimension = "pattern",
                        kind = InsightKind.PATTERN,
                        title = "$sideLabel at ${band.label.lowercase()} keep paying",
                        stats = stats,
                        advice = "A narrower candidate: ${proposal.label}.",
                        proposal = proposal,
                    ),
                    score = score(stats, overall) + 5.0,
                )
            }
        }

        // Keep both sides of the story: the three strongest leaks and the three strongest
        // patterns, not six variations of the same one.
        val leaks = drafts.filter { it.finding.kind == InsightKind.LEAK || it.finding.kind == InsightKind.TREND }
            .sortedByDescending { it.score }
            .take(3)
        val patterns = drafts.filter { it.finding.kind == InsightKind.EDGE || it.finding.kind == InsightKind.PATTERN }
            .sortedByDescending { it.score }
            .take(3)
        val chosen = (leaks + patterns).distinctBy { it.finding.id }.take(MAX_FINDINGS)
        val proposals = chosen.mapNotNull { it.finding.proposal }
            .distinctBy { it.id }
            .filter { it.id != seed.id }
            .take(MAX_PROPOSALS)

        val findings = chosen.map { it.finding }
        return LossInsightReport(
            findings = findings,
            proposals = proposals,
            considered = usable.size,
            note = if (findings.isEmpty()) {
                "No segment has separated from the rest by enough to act on. " +
                    "${usable.size} settled props were checked."
            } else {
                "${usable.size} settled props: ${leaks.size} leak${plural(leaks.size)}, " +
                    "${patterns.size} pattern${plural(patterns.size)}, ${proposals.size} candidate" +
                    "${plural(proposals.size)} for the next gate run."
            },
        )
    }

    private fun decimalOf(observation: LossObservation): Double =
        runCatching { Odds.americanToDecimal(observation.odds) }.getOrDefault(1.0)

    private fun verdict(stats: Stats, minSample: Int): Verdict? {
        if (stats.sample < minSample) return null
        if (stats.roi <= -0.05 && stats.hitRate <= stats.breakeven - 0.02) return Verdict.LEAK
        if (stats.roi >= 0.05 && stats.hitRate >= stats.breakeven + 0.02) return Verdict.EDGE
        return null
    }

    private fun score(stats: Stats, overall: Stats): Double =
        abs(stats.roi - overall.roi) * sqrt(stats.sample.toDouble())

    private fun sideProposal(seed: StrategyParams, side: Side, verdict: Verdict): StrategyParams? {
        val target = when {
            verdict == Verdict.LEAK && side == Side.UNDER -> SideFilter.OVER_ONLY
            verdict == Verdict.LEAK && side == Side.OVER -> SideFilter.UNDER_ONLY
            side == Side.OVER -> SideFilter.OVER_ONLY
            else -> SideFilter.UNDER_ONLY
        }
        return if (seed.sideFilter == target) null else seed.copy(sideFilter = target)
    }

    private fun priceProposal(seed: StrategyParams, band: String, verdict: Verdict): StrategyParams? {
        val next = when (verdict) {
            Verdict.LEAK -> when (band) {
                "short" -> seed.copy(minPrice = maxOf(seed.minPrice, 1.80))
                "long" -> seed.copy(maxPrice = minOf(seed.maxPrice, 2.20))
                else -> null
            }
            Verdict.EDGE -> when (band) {
                "short" -> seed.copy(maxPrice = minOf(seed.maxPrice, 1.80))
                "long" -> seed.copy(minPrice = maxOf(seed.minPrice, 2.20))
                else -> seed.copy(
                    minPrice = maxOf(seed.minPrice, 1.80),
                    maxPrice = minOf(seed.maxPrice, 2.20),
                )
            }
        } ?: return null
        return next.takeIf { it != seed && it.minPrice < it.maxPrice - 1e-9 }
    }

    private fun edgeProposal(seed: StrategyParams, band: String, verdict: Verdict): StrategyParams? {
        val next = if (verdict == Verdict.LEAK) {
            val floor = when (band) {
                "low" -> 2.0
                "mid" -> 3.0
                else -> 4.0
            }
            seed.copy(minEdgePts = maxOf(seed.minEdgePts, floor))
        } else {
            seed.copy(
                minEdgePts = when (band) {
                    "low" -> 0.0
                    "mid" -> 1.0
                    else -> 3.0
                },
            )
        }
        return next.takeIf { it != seed }
    }

    private fun marketProposal(seed: StrategyParams, market: String, verdict: Verdict): StrategyParams? {
        if (verdict == Verdict.LEAK) {
            // The rule already refuses this market, whether by exclusion or by an
            // inclusion list that does not name it. There is nothing new to test.
            if (market in seed.excludeMarkets) return null
            if (seed.markets.isNotEmpty() && market !in seed.markets) return null
            if (seed.markets == listOf(market)) return null
            return seed.copy(excludeMarkets = (seed.excludeMarkets + market).distinct())
                .takeIf { it != seed }
        }
        // Focusing on a paying market must also lift an old exclusion, or inclusion and
        // exclusion would cancel each other and the candidate would never place a bet.
        if (seed.markets == listOf(market) && market !in seed.excludeMarkets) return null
        return seed.copy(
            markets = listOf(market),
            excludeMarkets = seed.excludeMarkets - market,
        ).takeIf { it != seed }
    }

    private fun finding(
        id: String,
        dimension: String,
        kind: InsightKind,
        title: String,
        stats: Stats,
        advice: String,
        proposal: StrategyParams?,
    ) = LossFinding(
        id = id,
        dimension = dimension,
        kind = kind,
        title = title,
        detail = evidence(stats),
        advice = advice,
        sample = stats.sample,
        wins = stats.wins,
        losses = stats.losses,
        roi = stats.roi,
        hitRate = stats.hitRate,
        breakeven = stats.breakeven,
        proposal = proposal,
    )

    private fun evidence(stats: Stats): String =
        "${stats.wins}-${stats.losses} for ${signed(stats.profit)} units, " +
            "ROI ${pct(stats.roi)}, hit ${pct(stats.hitRate)} against a ${pct(stats.breakeven)} break-even."

    private fun advice(proposal: StrategyParams?, verdict: Verdict): String = when {
        proposal != null -> "Candidate: ${proposal.label}. It is a hypothesis until the gate tests it on a fresh season."
        verdict == Verdict.LEAK -> "No parameter change follows from this segment; it is already outside the live rule."
        else -> "The live rule already covers this pattern; no new candidate follows."
    }

    private fun marketLabel(market: String): String {
        val known = Markets.label(market)
        val name = if (known == market) {
            market.removePrefix("player_").replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
        } else {
            known
        }
        return "$name props"
    }

    private fun pct(value: Double): String = "%.1f%%".format(value * 100)
    private fun signed(value: Double): String = "%+.1f".format(value)
    private fun plural(count: Int): String = if (count == 1) "" else "s"
}
