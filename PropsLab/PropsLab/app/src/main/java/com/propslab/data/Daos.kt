package com.propslab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class SlateRow(
    val predictionId: String,
    val playerId: String,
    val playerName: String,
    val teamCode: String,
    val gameId: String,
    val homeCode: String,
    val awayCode: String,
    val startUtc: Long,
    val market: String,
    val book: String,
    val line: Double?,
    val overOdds: Int?,
    val underOdds: Int?,
    val quoteTimeUtc: Long,
    val source: String,
    val modelVersion: String,
    val schemaVersion: Int,
    val featureCutoffUtc: Long,
    val pOver: Double?,
    val pPush: Double?,
    val pUnder: Double?,
    val status: String,
    val unavailableReasons: String,
    val qualityFlags: String,
    val drivers: String,
    val explanation: String,
)

data class FreshnessRow(
    val predictionId: String,
    val market: String,
    val line: Double?,
    val quoteTimeUtc: Long,
    val featureCutoffUtc: Long,
    val source: String,
)

data class RecordedResult(val predictionId: String, val actual: Double)

data class BetRow(
    val betId: String,
    val predictionId: String,
    val playerName: String,
    val teamCode: String,
    val side: String,
    val line: Double,
    val quotedOdds: Int,
    val stakeUnits: Double,
    val decidedUtc: Long,
    /** Joined from the prediction so the ledger can print the slip the way a book shows it. */
    val market: String,
    val book: String,
    val actual: Double?,
    val outcome: String?,
    val profitUnits: Double?,
    val settledUtc: Long?,
    val formulaId: String,
    val formulaLabel: String,
)

private const val SLATE_SELECT = """
SELECT p.id AS predictionId, pl.id AS playerId, pl.name AS playerName, pl.teamCode AS teamCode,
  g.id AS gameId, g.homeCode AS homeCode, g.awayCode AS awayCode, g.startUtc AS startUtc,
  p.market AS market, o.book AS book, o.line AS line, o.overOdds AS overOdds, o.underOdds AS underOdds,
  o.quoteTimeUtc AS quoteTimeUtc, o.source AS source,
  p.modelVersion AS modelVersion, p.featureSchemaVersion AS schemaVersion, p.featureCutoffUtc AS featureCutoffUtc,
  p.pOver AS pOver, p.pPush AS pPush, p.pUnder AS pUnder, p.status AS status, p.unavailableReasons AS unavailableReasons,
  p.qualityFlags AS qualityFlags, p.drivers AS drivers, p.explanation AS explanation
FROM predictions p
JOIN odds_snapshots o ON o.id = p.quoteId
JOIN players pl ON pl.id = p.playerId
JOIN games g ON g.id = p.gameId
"""

@Dao
interface SlateDao {
    @Query("$SLATE_SELECT WHERE g.startUtc >= :sinceUtc ORDER BY g.startUtc, pl.name")
    fun observeSlate(sinceUtc: Long): Flow<List<SlateRow>>

    @Query("$SLATE_SELECT WHERE p.id = :id")
    fun observeRow(id: String): Flow<SlateRow?>

    @Query("$SLATE_SELECT WHERE p.id = :id")
    suspend fun row(id: String): SlateRow?

    @Query("SELECT COUNT(*) FROM predictions")
    suspend fun predictionCount(): Int

    @Query("SELECT COUNT(*) FROM predictions WHERE id = :id")
    suspend fun predictionExists(id: String): Int

    @Query(
        "SELECT p.id AS predictionId, p.market AS market, o.line AS line, o.quoteTimeUtc AS quoteTimeUtc, " +
            "p.featureCutoffUtc AS featureCutoffUtc, o.source AS source " +
            "FROM predictions p JOIN odds_snapshots o ON o.id = p.quoteId WHERE p.status = 'AVAILABLE'"
    )
    suspend fun freshnessCandidates(): List<FreshnessRow>

    @Query("UPDATE predictions SET status = 'UNAVAILABLE', unavailableReasons = :reasons WHERE id = :id")
    suspend fun markUnavailable(id: String, reasons: String)

    /** Actual results already recorded against a paper bet, keyed by prediction. */
    @Query(
        "SELECT b.predictionId AS predictionId, s.actual AS actual FROM settlements s " +
            "JOIN paper_bets b ON b.id = s.betId"
    )
    suspend fun recordedResults(): List<RecordedResult>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertGames(items: List<GameEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPlayers(items: List<PlayerEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertQuotes(items: List<OddsSnapshotEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPredictions(items: List<PredictionEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertModels(items: List<ModelVersionEntity>)
}

@Dao
interface PaperDao {
    @Query(
        // Left joins on purpose: a settled bet must survive the retirement of the prop it was
        // priced from, otherwise the ledger silently loses the bets it was built to remember.
        "SELECT b.id AS betId, b.predictionId AS predictionId, " +
            "COALESCE(pl.name, 'Removed prop') AS playerName, COALESCE(pl.teamCode, '') AS teamCode, " +
            "b.side AS side, b.line AS line, b.quotedOdds AS quotedOdds, b.stakeUnits AS stakeUnits, b.decidedUtc AS decidedUtc, " +
            "COALESCE(p.market, '') AS market, COALESCE(o.book, '') AS book, " +
            "s.actual AS actual, s.outcome AS outcome, s.profitUnits AS profitUnits, s.settledUtc AS settledUtc, " +
            "b.formulaId AS formulaId, b.formulaLabel AS formulaLabel " +
            "FROM paper_bets b LEFT JOIN predictions p ON p.id = b.predictionId " +
            "LEFT JOIN players pl ON pl.id = p.playerId " +
            "LEFT JOIN odds_snapshots o ON o.id = p.quoteId " +
            "LEFT JOIN settlements s ON s.betId = b.id ORDER BY b.decidedUtc DESC"
    )
    fun observeBets(): Flow<List<BetRow>>

    @Query("SELECT * FROM paper_bets WHERE id = :id")
    suspend fun bet(id: String): PaperBetEntity?

    /** Stake already committed to bets whose result is not recorded yet. */
    @Query(
        "SELECT IFNULL(SUM(b.stakeUnits), 0) FROM paper_bets b " +
            "LEFT JOIN settlements s ON s.betId = b.id WHERE s.betId IS NULL"
    )
    suspend fun openStakes(): Double

    /** Money won or lost across every settled practice bet. */
    @Query("SELECT IFNULL(SUM(profitUnits), 0) FROM settlements")
    suspend fun settledProfit(): Double

    @Query("DELETE FROM settlements") suspend fun clearSettlements()

    @Query("DELETE FROM paper_bets") suspend fun clearBets()

    /** Returns -1 when a bet for the same prediction and side already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBet(bet: PaperBetEntity): Long

    /** Returns -1 when the bet is already settled. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSettlement(s: SettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBets(items: List<PaperBetEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSettlements(items: List<SettlementEntity>)
}

@Dao
interface ConsoleDao {
    @Insert suspend fun append(turn: ConsoleTurn): Long

    @Query("SELECT * FROM console_turns ORDER BY createdUtc DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ConsoleTurn>

    @Query("SELECT COUNT(*) FROM console_turns")
    suspend fun count(): Int

    /** The one destructive call, and only when the user asks for it. */
    @Query("DELETE FROM console_turns") suspend fun clear()
}

@Dao
interface AuditDao {
    @Insert suspend fun insert(event: AuditEventEntity)
    @Insert suspend fun insertAll(events: List<AuditEventEntity>)

    @Query("SELECT * FROM audit_events ORDER BY ts DESC, id DESC LIMIT 300")
    fun observe(): Flow<List<AuditEventEntity>>
}
