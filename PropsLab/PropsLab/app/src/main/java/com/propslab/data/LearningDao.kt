package com.propslab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class LearningTotals(
    val props: Int = 0,
    val games: Int = 0,
    val settled: Int = 0,
    val open: Int = 0,
    val staked: Double = 0.0,
    val profit: Double = 0.0,
)

@Dao
interface LearningDao {

    /** Returns -1 for a bet already logged at this exact line and side. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBets(items: List<LearningBetEntity>): List<Long>

    @Query("SELECT * FROM learning_bets WHERE outcome IS NULL ORDER BY startUtc")
    suspend fun open(): List<LearningBetEntity>

    @Query("SELECT * FROM learning_bets WHERE track = :track AND outcome IN ('WIN','LOSS') ORDER BY settledUtc")
    suspend fun settled(track: String): List<LearningBetEntity>

    /** The same settled rows as a stream, so insights refresh when a result arrives. */
    @Query("SELECT * FROM learning_bets WHERE track = :track AND outcome IN ('WIN','LOSS') ORDER BY startUtc")
    fun observeSettled(track: String): Flow<List<LearningBetEntity>>

    @Query("UPDATE learning_bets SET outcome = :outcome, profit = :profit, settledUtc = :ts WHERE id = :id")
    suspend fun settle(id: String, outcome: String, profit: Double, ts: Long)

    @Query("SELECT * FROM learning_bets ORDER BY loggedUtc DESC, startUtc DESC LIMIT 120")
    fun observeRecent(): Flow<List<LearningBetEntity>>

    @Query(
        "SELECT IFNULL(SUM(CASE WHEN track = 'PROP' THEN 1 ELSE 0 END), 0) AS props, " +
            "IFNULL(SUM(CASE WHEN track = 'GAME' THEN 1 ELSE 0 END), 0) AS games, " +
            "IFNULL(SUM(CASE WHEN outcome IS NOT NULL THEN 1 ELSE 0 END), 0) AS settled, " +
            "IFNULL(SUM(CASE WHEN outcome IS NULL THEN 1 ELSE 0 END), 0) AS open, " +
            "IFNULL(SUM(stake), 0) AS staked, IFNULL(SUM(profit), 0) AS profit FROM learning_bets",
    )
    fun observeTotals(): Flow<LearningTotals>

    @Query("SELECT COUNT(*) FROM learning_bets WHERE track = :track")
    suspend fun countTrack(track: String): Int

    @Query("SELECT COUNT(*) FROM learning_bets WHERE outcome IS NOT NULL")
    suspend fun settledCount(): Int

    @Query("SELECT * FROM model_calibration WHERE track = :track")
    suspend fun calibration(track: String): CalibrationEntity?

    @Query("SELECT * FROM model_calibration")
    fun observeCalibration(): Flow<List<CalibrationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalibration(items: List<CalibrationEntity>)
}
