package com.propslab.sim

import com.propslab.domain.Markets
import com.propslab.domain.Odds

/**
 * The sandbox's playing field: a simulated season of prop-days.
 *
 * It is generated, not stored. A season is a pure function of its seed, so every round
 * can be re-derived exactly, nothing has to be migrated, and the database only ever
 * holds conclusions rather than twelve thousand rows of invented history.
 *
 * Each round of the autopilot uses a new seed, which means a promoted formula has to
 * keep working on data it has not seen before. That is the only defence against
 * promoting a formula that merely fits one season.
 */
object Season {

    const val DEFAULT_DAYS = 150
    const val DEFAULT_PROPS_PER_DAY = 80
    const val BASE_SEED = 20261029L

    /**
     * The simulated bench carries real market labels. Without them a formula that names
     * or excludes a market would look identical to the broad rule in every gate run, and
     * a lesson learned about a losing market could never be tested.
     */
    private val marketPool = listOf(
        Markets.POINTS,
        Markets.REBOUNDS,
        Markets.ASSISTS,
        Markets.THREES,
        Markets.PRA,
    )

    private val pricePairs = listOf(
        -110 to -110,
        -115 to -105,
        -105 to -115,
        -120 to 100,
        100 to -120,
        -125 to 105,
        105 to -125,
        -130 to 110,
        110 to -130,
    )

    private val firstNames = listOf(
        "Dev", "Kofi", "Nico", "Remy", "Ansel", "Cade", "Soren", "Pia", "Mikal", "Jonas",
        "Terrell", "Elias", "Bruno", "Owen", "Rasheed", "Dario", "Kenji", "Luca", "Amari", "Yusuf",
    )
    private val lastNames = listOf(
        "Aldana", "Brandt", "Tarrant", "Okafor", "Duvall", "Moriyama", "Abara", "Lindqvist",
        "Petrov", "Haddad", "Nakamura", "Silva", "Osei", "Kovac", "Reyes", "Bakker",
    )

    /** Player ids are stable across the season so a formula can be judged per player. */
    private const val PLAYER_POOL = 120

    fun playerName(index: Int): String =
        "${firstNames[index % firstNames.size]} ${lastNames[(index * 7 + index / lastNames.size) % lastNames.size]}"

    fun playerId(index: Int): String = "sim-p%03d".format(index)

    /**
     * @param seed season identity; the autopilot advances it every round.
     * @param endUtc the moment the season finishes, so prop times line up with "now".
     */
    fun generate(
        seed: Long = BASE_SEED,
        days: Int = DEFAULT_DAYS,
        propsPerDay: Int = DEFAULT_PROPS_PER_DAY,
        endUtc: Long = 1_790_000_000_000L,
    ): List<SimProp> {
        val dayMs = 86_400_000L
        val out = ArrayList<SimProp>(days * propsPerDay)
        for (day in 0 until days) {
            val dayStart = endUtc - (days - day) * dayMs
            for (slot in 0 until propsPerDay) {
                val key = "s${seed}d$day-p$slot"
                val player = (slot * 7 + day * 13 + (seed % 97).toInt()).mod(PLAYER_POOL)
                val pair = pricePairs[SandboxHash.pick("$key:price", 0, pricePairs.size)]
                val whole = SandboxHash.unit("$key:half") > 0.78
                val base = 12 + SandboxHash.pick("$key:line", 0, 44)
                val line = if (whole) base.toDouble() else base + 0.5

                // The model is anchored to the price it is looking at, then shifted by its own
                // disagreement with that price. Anchoring it to the quote is what makes the
                // disagreement — and nothing else — the signal in this world.
                val market = Odds.noVigOver(pair.first, pair.second)
                val gap = SandboxHash.signed("$key:gap", 0.085)
                val modelOver = (market + gap).coerceIn(0.05, 0.95)
                val push = if (whole) 0.015 + SandboxHash.unit("$key:push") * 0.030 else 0.0

                val startsAt = dayStart + 17 * 3_600_000L + slot * 6L * 60_000L
                val marketKey = marketPool[SandboxHash.pick("$key:market", 0, marketPool.size)]
                val prop = SimProp(
                    id = "sim-${seed}-$day-$slot",
                    playerName = playerName(player),
                    line = line,
                    overOdds = pair.first,
                    underOdds = pair.second,
                    pOver = modelOver * (1.0 - push),
                    pPush = push,
                    pUnder = (1.0 - modelOver) * (1.0 - push),
                    startUtc = startsAt,
                    actual = null,
                    synthetic = true,
                    market = marketKey,
                )
                out += prop.copy(actual = SandboxTruth.actualFor(prop))
            }
        }
        return out.sortedWith(compareBy({ it.startUtc }, { it.id }))
    }
}
