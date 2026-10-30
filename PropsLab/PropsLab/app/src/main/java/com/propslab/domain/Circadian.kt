package com.propslab.domain

import kotlin.math.abs
import kotlin.math.min

/**
 * A small, capped correction for body-clock misalignment.
 *
 * The evidence people point at is real but modest: players shoot worse in games that start
 * away from their usual evening slot, and travel across time zones adds to it. What the app
 * cannot do with the data it has is estimate a per-player effect. So this is a stated
 * heuristic, capped at 1.5% of the projection, applied symmetrically to every prop whose
 * game time is known:
 *
 *  - **Start hour.** The tip-off hour converted to the player's own team's zone, measured
 *    from a 19:30 norm. Six or more hours away costs the full 1.0%.
 *  - **Travel.** Every zone crossed between the player's team and the venue costs 0.17%,
 *    up to 0.5% for three or more zones.
 *
 * Zones are fixed standard-time offsets. Daylight saving is ignored, so the hour can be off
 * by one; the effect is capped well below that error's size. No game time, no adjustment.
 */
data class CircadianImpact(
    /** Multiplier on the player's projected mean. Always in [0.985, 1]. */
    val meanMultiplier: Double,
    /** Tip-off hour on the player's own clock, 0..24. */
    val localHour: Double,
    /** Circular distance from the 19:30 norm, in hours. */
    val hoursFromIdeal: Double,
    /** Zones crossed between the team's home and the venue. */
    val zoneShift: Int,
    /** How much of the projection was taken off, in percent. Never negative. */
    val offProjectionPct: Double,
    val summary: String,
)

object Circadian {

    /** The cap the whole correction lives under, in percent of the projection. */
    const val MAX_OFF_PCT = 1.5

    private const val IDEAL_HOUR = 19.5
    private const val START_PENALTY_PCT = 1.0
    private const val TRAVEL_PENALTY_PCT = 0.5
    private const val MAX_ZONES = 3.0

    /** Standard-time UTC offsets. Daylight saving is deliberately not modelled. */
    private val ZONES: Map<String, Int> = mapOf(
        "ATL" to -5, "BOS" to -5, "BKN" to -5, "CHA" to -5, "CLE" to -5, "DET" to -5,
        "IND" to -5, "MIA" to -5, "NYK" to -5, "ORL" to -5, "PHI" to -5, "TOR" to -5,
        "WAS" to -5,
        "CHI" to -6, "DAL" to -6, "HOU" to -6, "MEM" to -6, "MIL" to -6, "MIN" to -6,
        "NOP" to -6, "OKC" to -6, "SAS" to -6,
        "DEN" to -7, "PHX" to -7, "UTA" to -7,
        "GSW" to -8, "LAC" to -8, "LAL" to -8, "POR" to -8, "SAC" to -8,
    )

    fun zone(teamCode: String?): Int? =
        teamCode?.trim()?.uppercase()?.let { ZONES[it] }

    /** Tip-off on a team's own clock, 0..24. */
    fun localHour(startUtc: Long, zoneOffset: Int): Double {
        val hours = (startUtc / 3_600_000.0 + zoneOffset) % 24.0
        return if (hours < 0.0) hours + 24.0 else hours
    }

    /**
     * @param teamCode the player's own team, whose body clock is the one that travels.
     * @param venueCode the home team, which sets the clock the game is played on.
     */
    fun impact(startUtc: Long, teamCode: String?, venueCode: String?): CircadianImpact? {
        if (startUtc <= 0L) return null
        val playerZone = zone(teamCode) ?: zone(venueCode) ?: return null
        val venueZone = zone(venueCode) ?: playerZone
        val hour = localHour(startUtc, playerZone)
        val rawDelta = abs(hour - IDEAL_HOUR)
        val hoursFromIdeal = min(rawDelta, 24.0 - rawDelta)
        val startPenalty = min(hoursFromIdeal, 6.0) / 6.0 * START_PENALTY_PCT
        val zoneShift = playerZone - venueZone
        val travelPenalty = min(abs(zoneShift).toDouble(), MAX_ZONES) / MAX_ZONES * TRAVEL_PENALTY_PCT
        val off = (startPenalty + travelPenalty).coerceIn(0.0, MAX_OFF_PCT)
        val multiplier = 1.0 - off / 100.0
        return CircadianImpact(
            meanMultiplier = multiplier,
            localHour = hour,
            hoursFromIdeal = hoursFromIdeal,
            zoneShift = zoneShift,
            offProjectionPct = off,
            summary = summary(hour, hoursFromIdeal, zoneShift, off),
        )
    }

    private fun summary(hour: Double, fromIdeal: Double, zoneShift: Int, off: Double): String {
        val clock = "%02d:%02d".format(hour.toInt(), ((hour - hour.toInt()) * 60.0).toInt().coerceIn(0, 59))
        val travel = when {
            zoneShift == 0 -> "no travel across zones"
            abs(zoneShift) == 1 -> "one zone crossed"
            else -> "${abs(zoneShift)} zones crossed"
        }
        val start = if (fromIdeal < 0.25) "on the usual evening clock" else "%.1f h from a 19:30 start".format(fromIdeal)
        return "Tips at $clock on the player's clock, $start, $travel: ${"%.1f".format(off)}% off the projection."
    }
}
