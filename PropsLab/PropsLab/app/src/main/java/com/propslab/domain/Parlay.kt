package com.propslab.domain

import kotlin.math.exp
import kotlin.math.ln

/**
 * One leg of a parlay: the price the book pays and the model's chance of the leg winning.
 *
 * A push does not lose the ticket the way a loss does — the book simply drops that leg and
 * recalculates — so the chance used here is conditioned on the leg not pushing. On a
 * half-point line the push mass is zero and this is just the win chance.
 */
data class ParlayLeg(val odds: Int, val pWin: Double, val pPush: Double = 0.0) {
    val decimal: Double get() = Odds.americanToDecimal(odds)

    val winNoPush: Double
        get() = if (pPush >= 1.0) 0.0 else (pWin / (1.0 - pPush)).coerceIn(0.0, 1.0)
}

/** A whole ticket: legs, the chance all of them land, and what one unit returns. */
data class Parlay(
    val legs: List<ParlayLeg>,
    val probability: Double,
    val decimal: Double,
    val stake: Double = 1.0,
) {
    val payout: Double get() = stake * decimal
    val profit: Double get() = payout - stake

    /** Average profit per unit, using the model's chance and ignoring the push rebate. */
    val ev: Double get() = probability * (decimal - 1.0) - (1.0 - probability)
}

/**
 * Parlay arithmetic, kept in the domain so it is testable without a screen.
 *
 * The probability is the product of the legs' chances. That assumes the legs are
 * independent, which is true across different games and only approximately true inside
 * one — the screen says so rather than pretending the number is exact.
 */
object ParlayBook {

    fun build(legs: List<ParlayLeg>, stake: Double = 1.0): Parlay? {
        if (legs.isEmpty()) return null
        if (stake <= 0.0 || !stake.isFinite()) return null
        val probability = legs.fold(1.0) { acc, leg -> acc * leg.winNoPush }
        val decimal = legs.fold(1.0) { acc, leg -> acc * leg.decimal }
        if (!probability.isFinite() || !decimal.isFinite()) return null
        if (probability <= 0.0 || decimal <= 1.0) return null
        return Parlay(legs, probability.coerceIn(0.0, 1.0), decimal, stake)
    }
}

/**
 * A same-game ticket: the product of the legs, pulled toward even odds to stand in for the
 * correlation the app cannot measure.
 *
 * The direction matters and is easy to get backwards. Legs in one game move together — a
 * high-usage night helps a player's points and his assists — so positive correlation makes
 * the true joint chance **higher** than the product, and a book's fair price **lower** than
 * the multiplied price. That is why books cap same-game payouts. The app therefore lifts
 * the probability and cuts the decimal by the same ratio, and says which assumption it used.
 *
 * The strength is stated, not fitted: 10% of the log-odds for different players in one game,
 * 18% when two legs are on the same player. No stored data can fit a covariance matrix yet.
 */
object SameGame {

    const val SAME_GAME_SHRINK = 0.10
    const val SAME_PLAYER_SHRINK = 0.18

    data class Adjusted(
        val probability: Double,
        val decimal: Double,
        val naiveProbability: Double,
        val naiveDecimal: Double,
        val shrink: Double,
    )

    /**
     * @param together true when two or more legs share a player, which correlates them more.
     */
    fun adjust(probability: Double, decimal: Double, together: Boolean): Adjusted? {
        if (!probability.isFinite() || probability <= 1e-9 || probability >= 1.0 - 1e-9) return null
        if (!decimal.isFinite() || decimal <= 1.0) return null
        val shrink = if (together) SAME_PLAYER_SHRINK else SAME_GAME_SHRINK
        val pulled = 1.0 / (1.0 + exp(-ln(probability / (1.0 - probability)) * (1.0 - shrink)))
        if (!pulled.isFinite() || pulled <= probability) return null
        return Adjusted(
            probability = pulled.coerceAtMost(0.999),
            decimal = decimal * (probability / pulled),
            naiveProbability = probability,
            naiveDecimal = decimal,
            shrink = shrink,
        )
    }
}

/** One ticket competing for a share of a stated budget. */
data class StakeOffer(val key: String, val probability: Double, val decimal: Double)

/** What one ticket was given, and why. `capped` means the cap, not the edge, set the stake. */
data class StakeAllocation(
    val key: String,
    val stake: Double,
    val kelly: Double,
    val capped: Boolean,
    val share: Double,
)

/**
 * Budget allocation across the day's tickets.
 *
 * Each ticket is staked at the same fraction of Kelly the settings screen uses, capped so
 * one long shot cannot take the whole budget. The stakes never add up to more than the
 * budget, and they may add up to less: a ticket whose edge does not justify a stake gets
 * nothing, and the remainder is reported as unallocated rather than forced onto a bet.
 */
object ParlayStaking {

    /** No single ticket may take more than this share of the budget. */
    const val CAP_SHARE = 0.25

    fun kelly(probability: Double, decimal: Double): Double {
        if (!probability.isFinite() || !decimal.isFinite() || decimal <= 1.0) return 0.0
        val b = decimal - 1.0
        if (b <= 0.0) return 0.0
        return (probability * b - (1.0 - probability)) / b
    }

    fun allocate(
        budget: Double,
        offers: List<StakeOffer>,
        fraction: Double = 0.25,
        capShare: Double = CAP_SHARE,
    ): List<StakeAllocation> {
        if (offers.isEmpty()) return emptyList()
        if (!budget.isFinite() || budget <= 0.0) {
            return offers.map { StakeAllocation(it.key, 0.0, 0.0, false, 0.0) }
        }
        val cap = capShare.coerceIn(0.01, 1.0)
        val scale = fraction.coerceIn(0.0, 1.0)
        val kelly = offers.map { kelly(it.probability, it.decimal) }
        val wants = kelly.map { k -> (k * scale).coerceIn(0.0, cap) }
        val totalWant = wants.sum()
        val total = if (totalWant > 1.0) budget / totalWant else 1.0

        // Floor to cents, then hand the leftover cents to the largest stakes while they are
        // still under their own target. That keeps the printed stakes tidy without ever
        // pushing the total past the budget.
        val cents = wants.map { ((it * total * budget) * 100.0).toInt() }.toMutableList()
        var left = ((budget * 100.0).toInt() - cents.sum()).coerceAtLeast(0)
        val order = cents.indices.sortedByDescending { cents[it] }
        while (left > 0) {
            var moved = false
            for (i in order) {
                if (left == 0) break
                val target = (wants[i] * total * budget * 100.0).toInt()
                if (cents[i] < target) {
                    cents[i] += 1
                    left -= 1
                    moved = true
                }
            }
            if (!moved) break
        }
        return offers.mapIndexed { i, offer ->
            val stake = cents[i] / 100.0
            StakeAllocation(
                key = offer.key,
                stake = stake,
                kelly = kelly[i],
                capped = kelly[i] > 0.0 && wants[i] >= cap - 1e-12,
                share = if (budget > 0.0) stake / budget else 0.0,
            )
        }
    }
}
