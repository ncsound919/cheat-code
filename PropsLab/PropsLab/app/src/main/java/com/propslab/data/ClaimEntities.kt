package com.propslab.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Where a user-created strategy stands. A claim that failed the bench is kept as a note. */
object ClaimStatus {
    /** Passed the synthetic bench and is now in the sandbox grid. */
    const val CANDIDATE = "CANDIDATE"

    /** Did not clear the gate. Kept so the failure is visible, never traded. */
    const val REJECTED = "REJECTED"
}

/**
 * One plain-language idea and the strategy it compiled to.
 *
 * The lens is stored as the same `edge=…;side=…;price=…;tilt=…` text the sandbox uses, so a
 * claim can be reconstructed exactly and re-entered into the autopilot grid on every round.
 * Nothing about the sentence becomes code.
 */
@Entity(tableName = "claim_formulas")
data class ClaimFormulaEntity(
    @PrimaryKey val id: String,
    val claim: String,
    val name: String,
    val factorTag: String,
    val headline: String,
    /** The compiled lens, in StrategyParams.paramsText form. */
    val lens: String,
    /** The sandbox id this lens runs under. */
    val formulaId: String,
    val status: String,
    val bets: Int,
    val wins: Int,
    val losses: Int,
    val profit: Double,
    val roi: Double,
    val tStat: Double,
    val maxDrawdown: Double,
    val passed: Boolean,
    val plain: String,
    val boardNote: String,
    /** Newline-separated, in the order they should be read. */
    val gameTerms: String,
    val falsifier: String,
    val subject: String?,
    val deepVerdict: String,
    val deepNote: String,
    val deepAtUtc: Long,
    val createdUtc: Long,
) {
    val inGameTerms: List<String> get() = gameTerms.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
}

@Dao
interface ClaimDao {

    @Query("SELECT * FROM claim_formulas ORDER BY createdUtc DESC")
    fun observe(): Flow<List<ClaimFormulaEntity>>

    @Query("SELECT * FROM claim_formulas ORDER BY createdUtc DESC")
    suspend fun all(): List<ClaimFormulaEntity>

    @Query("SELECT * FROM claim_formulas WHERE id = :id")
    suspend fun byId(id: String): ClaimFormulaEntity?

    @Query("SELECT * FROM claim_formulas WHERE status = 'CANDIDATE'")
    suspend fun candidates(): List<ClaimFormulaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(claim: ClaimFormulaEntity)

    @Query("UPDATE claim_formulas SET deepVerdict = :verdict, deepNote = :note, deepAtUtc = :ts WHERE id = :id")
    suspend fun recordDeepTest(id: String, verdict: String, note: String, ts: Long)

    @Query("SELECT COUNT(*) FROM claim_formulas")
    suspend fun count(): Int
}
