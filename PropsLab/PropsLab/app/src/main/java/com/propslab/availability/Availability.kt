package com.propslab.availability

import com.propslab.data.AvailabilitySignalEntity
import com.propslab.data.AvailabilityStatus
import com.propslab.data.SignalSource

/**
 * A late report, as the model uses it.
 *
 * The point of a signal is not to be right; it is to be *explicit*. A projection built
 * from twenty games knows nothing about the ankle that was rolled in warmups, so the only
 * honest way to use that knowledge is to state it separately, shift the projection by a
 * named amount, and let the audit trail show both.
 */
data class AvailabilityNote(
    val playerKey: String,
    val playerName: String,
    val status: String,
    val detail: String,
    val source: String,
    val observedUtc: Long,
    val expiresUtc: Long,
) {
    val label: String get() = AvailabilityStatus.label(status)

    val isOut: Boolean get() = status == AvailabilityStatus.OUT

    val fromNews: Boolean get() = source == SignalSource.NEWS

    /** A signal older than the window is history, not news. */
    fun fresh(nowUtc: Long, maxAgeMs: Long = AvailabilityModel.MAX_AGE_MS): Boolean =
        observedUtc > 0L && nowUtc - observedUtc <= maxAgeMs && nowUtc < expiresUtc

    fun summary(): String = buildString {
        append(label)
        if (detail.isNotBlank()) append(" · ").append(detail)
        append(" · ").append(SignalSource.label(source))
    }

    companion object {
        fun from(entity: AvailabilitySignalEntity) = AvailabilityNote(
            playerKey = entity.playerKey,
            playerName = entity.playerName,
            status = entity.status,
            detail = entity.detail,
            source = entity.source,
            observedUtc = entity.observedUtc,
            expiresUtc = entity.expiresUtc,
        )
    }
}

/**
 * How a status moves the projection.
 *
 * These are multipliers on the expected stat, not on the probability. That distinction
 * matters: cutting the mean is a claim about minutes and usage, which is a thing a report
 * can actually tell you, while nudging a probability is a claim about the book being wrong.
 * Everything here is bounded, so no single headline can turn a projection into a different
 * player.
 */
object AvailabilityModel {

    /** A signal older than this cannot move a number. */
    const val MAX_AGE_MS = 18 * 3_600_000L

    /** How long a recorded signal stays live. Tip-off plus a warmup is the real bound. */
    const val TTL_MS = 14 * 3_600_000L

    /** No report cuts a projection by more than this. */
    const val MIN_FACTOR = 0.55

    fun factor(status: String): Double = when (status) {
        AvailabilityStatus.OUT -> 0.0
        AvailabilityStatus.DOUBTFUL -> 0.65
        AvailabilityStatus.QUESTIONABLE -> 0.90
        AvailabilityStatus.GAME_TIME -> 0.95
        AvailabilityStatus.PROBABLE -> 1.0
        else -> 1.0
    }.coerceIn(0.0, 1.0)

    /** The factor actually applied, after the floor. Zero stays zero: out is out. */
    fun applied(status: String): Double {
        val raw = factor(status)
        return if (raw <= 0.0) 0.0 else raw.coerceAtLeast(MIN_FACTOR)
    }

    fun shiftPct(status: String): Int = ((1.0 - applied(status)) * 100).toInt()

    /**
     * The one line a prediction carries about a signal, written to be read by a person
     * deciding whether they trust the number.
     */
    fun flag(note: AvailabilityNote, stale: Boolean = false): String = buildString {
        append("Late report: ").append(note.label)
        if (note.detail.isNotBlank()) append(" (").append(note.detail).append(')')
        when {
            stale -> append(" — older than 18 hours, so it is recorded but not applied")
            note.isOut -> append(" — the estimate is withheld and the prop is not tradable")
            else -> {
                val cut = shiftPct(note.status)
                if (cut > 0) append(" — the projection is cut by ").append(cut).append('%')
            }
        }
    }
}
