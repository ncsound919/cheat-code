package com.propslab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<AvailabilitySignalEntity>): List<Long>

    /** Signals that have not expired. An expired signal is not a signal. */
    @Query("SELECT * FROM availability_signals WHERE expiresUtc > :nowUtc ORDER BY observedUtc DESC")
    suspend fun active(nowUtc: Long): List<AvailabilitySignalEntity>

    @Query("SELECT * FROM availability_signals ORDER BY observedUtc DESC LIMIT 60")
    fun observe(): Flow<List<AvailabilitySignalEntity>>

    @Query("DELETE FROM availability_signals WHERE expiresUtc <= :nowUtc")
    suspend fun purge(nowUtc: Long): Int

    @Query("SELECT COUNT(*) FROM availability_signals")
    suspend fun count(): Int

    // ------------------------------------------------------------------ the theory lab

    /** Returns -1 for a row that is already in the dataset, so a re-import is idempotent. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(items: List<HistoryPropEntity>): List<Long>

    @Query("SELECT * FROM history_props ORDER BY playedUtc")
    suspend fun history(): List<HistoryPropEntity>

    @Query("SELECT COUNT(*) FROM history_props")
    suspend fun historyCount(): Int

    @Query("SELECT COUNT(*) FROM history_props")
    fun observeHistoryCount(): Flow<Int>

    @Query("DELETE FROM history_props")
    suspend fun clearHistory(): Int

    @Query("SELECT COUNT(DISTINCT market) FROM history_props")
    suspend fun historyMarkets(): Int
}
