package com.propslab.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.propslab.domain.PlattFit

/** The two models the ledger can teach: the prop model and the game model. */
object LearningTrack {
    const val PROP = "PROP"
    const val GAME = "GAME"
}

object LearningSide {
    const val OVER = "OVER"
    const val UNDER = "UNDER"
    const val HOME = "HOME"
    const val AWAY = "AWAY"
}

/**
 * One hypothetical bet on the live board, logged whether or not the strategy would have
 * taken it.
 *
 * This is the difference between a record and a classroom. The sandbox only writes the
 * bets its promoted formula chose; those results can retire a formula but they cannot
 * answer "was the model calibrated?" because they are a selected sample. The ledger logs
 * the model's lean on every priced row, at a flat unit, and settles what happens. Every
 * settled row is a calibration observation, so the correction is learned from the whole
 * board rather than from the winners.
 */
@Entity(tableName = "learning_bets", indices = [Index("track"), Index("outcome"), Index("gameId")])
data class LearningBetEntity(
    @PrimaryKey val id: String,
    /** PROP or GAME. */
    val track: String,
    /** The market key: player_points, h2h, spreads, totals. */
    val market: String,
    val gameId: String,
    /** The prediction id for a prop, or the game id for a game market. */
    val refId: String,
    /** The player name, or the matchup for a game. */
    val subject: String,
    val homeCode: String,
    val awayCode: String,
    /** OVER / UNDER for a prop or total, HOME / AWAY for a moneyline or spread. */
    val side: String,
    val line: Double,
    val odds: Int,
    /** The model's chance of this side winning. */
    val pModel: Double,
    val pPush: Double,
    /** The book's no-vig chance of the same side, when it could be paired. */
    val pMarket: Double?,
    val edgePts: Double?,
    /** HIGH, MEDIUM or LOW, from the size of the gap. */
    val confidence: String,
    val stake: Double,
    val outcome: String?,
    val profit: Double?,
    val startUtc: Long,
    val loggedUtc: Long,
    val settledUtc: Long?,
)

/**
 * The current correction for one model, rewritten whenever enough new results arrive.
 * The identity row (slope 1, intercept 0) is what an unfitted model applies, so the
 * absence of a row means "unchanged".
 */
@Entity(tableName = "model_calibration")
data class CalibrationEntity(
    @PrimaryKey val track: String,
    val slope: Double,
    val intercept: Double,
    val sample: Int,
    val brierBefore: Double,
    val brierAfter: Double,
    val generation: Int,
    val updatedUtc: Long,
)

fun CalibrationEntity.toFit(): PlattFit = PlattFit(
    slope = slope,
    intercept = intercept,
    sample = sample,
    brierBefore = brierBefore,
    brierAfter = brierAfter,
    generation = generation,
)

fun PlattFit.toEntity(track: String, now: Long): CalibrationEntity = CalibrationEntity(
    track = track,
    slope = slope,
    intercept = intercept,
    sample = sample,
    brierBefore = brierBefore,
    brierAfter = brierAfter,
    generation = generation,
    updatedUtc = now,
)
