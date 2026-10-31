package com.propslab.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** The odds provider's rows. Quotes carrying this source are real market prices. */
const val ODDS_FEED_SOURCE = "odds_api"

/** Box scores come from the results provider, never from [FIXTURE_SOURCE]. */
const val BOX_SCORE_SOURCE = "balldontlie"

/** The game-log model. Every prediction carrying this version is priced from real box scores. */
const val GAMELOG_MODEL = "gamelog-normal-v1"

/** The Kaggle empirical table, used for points where the artifact covers the line. */
const val KAGGLE_TABLE_MODEL = "kaggle-emp-v1"

/**
 * One player's box score in one finished game, flattened to the columns the model needs.
 *
 * Points, rebounds, assists and made threes are stored rather than derived, because a
 * market is nothing but one of those columns or a sum of them. Nothing else from the box
 * score is kept: possessions and shooting splits would only invite a more confident model
 * than the data supports.
 */
@Entity(tableName = "player_game_stats", indices = [Index("playerKey")])
data class PlayerGameStatEntity(
    /** `<provider>:<row id>`, so a re-fetch can never duplicate a row. */
    @PrimaryKey val id: String,
    /** The results provider's game id, used to join a finished game to a prediction. */
    val eventId: String,
    val playedUtc: Long,
    /** Normalised player name; the only join key between the odds feed and the box score. */
    val playerKey: String,
    val playerName: String,
    val teamAbbr: String,
    val points: Int,
    val rebounds: Int,
    val assists: Int,
    val threes: Int,
    val minutes: Int,
    val fetchedUtc: Long,
    /** The four football columns; zero on a basketball row and on rows written before 1.1.3. */
    @ColumnInfo(defaultValue = "0") val passYards: Int = 0,
    @ColumnInfo(defaultValue = "0") val rushYards: Int = 0,
    @ColumnInfo(defaultValue = "0") val receivingYards: Int = 0,
    @ColumnInfo(defaultValue = "0") val receptions: Int = 0,
    @ColumnInfo(defaultValue = "basketball") val sport: String = "basketball",
)

/**
 * A finished result written against one prediction. Append-only in practice: a prediction
 * settles once, and the row records which box score line it settled from.
 */
@Entity(tableName = "player_results", indices = [Index("recordedUtc")])
data class PlayerResultEntity(
    @PrimaryKey val predictionId: String,
    val market: String,
    val actual: Double,
    val statId: String,
    val recordedUtc: Long,
)

/**
 * What the last sync did, so the screen can be specific instead of saying "sync failed".
 * One row, rewritten in place.
 */
@Entity(tableName = "feed_state")
data class FeedStateEntity(
    @PrimaryKey val id: String,
    val lastPropsSyncUtc: Long,
    val lastResultsSyncUtc: Long,
    val propsNote: String,
    val resultNote: String,
    @ColumnInfo(defaultValue = "") val signalNote: String,
    /** Credits the odds provider reported remaining, when it reported. */
    val creditsRemaining: Int?,
    val quotesWritten: Int,
    val predictionsScored: Int,
    val resultsRecorded: Int,
    val fixturesRetired: Boolean,
) {
    companion object {
        const val ID = "feed"

        val EMPTY = FeedStateEntity(
            id = ID,
            lastPropsSyncUtc = 0L,
            lastResultsSyncUtc = 0L,
            propsNote = "The feed has not run yet.",
            resultNote = "No box scores have been read yet.",
            signalNote = "No availability news has been checked yet.",
            creditsRemaining = null,
            quotesWritten = 0,
            predictionsScored = 0,
            resultsRecorded = 0,
            fixturesRetired = false,
        )
    }
}
