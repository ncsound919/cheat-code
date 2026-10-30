package com.propslab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SandboxDao {

    @Query("SELECT * FROM formula_runs ORDER BY profit DESC")
    fun observeFormulas(): Flow<List<FormulaRunEntity>>

    @Query("SELECT * FROM formula_runs WHERE tier = 'LIVE' LIMIT 1")
    suspend fun liveFormula(): FormulaRunEntity?

    @Query("SELECT * FROM formula_runs WHERE id = :id")
    suspend fun formula(id: String): FormulaRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFormulas(items: List<FormulaRunEntity>)

    @Query("UPDATE formula_runs SET tier = :tier WHERE id = :id")
    suspend fun setTier(id: String, tier: String)

    @Query("UPDATE formula_runs SET tier = :tier")
    suspend fun setAllTiers(tier: String)

    @Query("SELECT * FROM sandbox_state WHERE id = :id")
    fun observeState(id: String): Flow<SandboxStateEntity?>

    @Query("SELECT * FROM sandbox_state WHERE id = :id")
    suspend fun state(id: String): SandboxStateEntity?

    @Query("SELECT * FROM sandbox_trades")
    suspend fun allTrades(): List<SandboxTradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: SandboxStateEntity)

    @Query("SELECT * FROM sandbox_trades ORDER BY startUtc DESC, placedUtc DESC LIMIT 200")
    fun observeTrades(): Flow<List<SandboxTradeEntity>>

    @Query("SELECT * FROM sandbox_trades WHERE outcome IS NULL ORDER BY startUtc")
    suspend fun openTrades(): List<SandboxTradeEntity>

    @Query("SELECT COUNT(*) FROM sandbox_trades")
    suspend fun tradeCount(): Int

    /** Returns -1 for a trade that already exists, so a round can be re-run safely. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrades(items: List<SandboxTradeEntity>): List<Long>

    @Query("UPDATE sandbox_trades SET outcome = :outcome, profit = :profit, settledUtc = :ts WHERE id = :id")
    suspend fun settle(id: String, outcome: String, profit: Double, ts: Long)
}
