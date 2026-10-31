package com.propslab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One prediction on a game that has started and has no result recorded against it yet. */
/** One team's total points in one finished game, summed from the box score it stored. */
data class TeamTotalRow(
    val eventId: String,
    val teamAbbr: String,
    val points: Int,
    val playedUtc: Long,
)

data class UnsettledRow(
    val predictionId: String,
    val market: String,
    val gameId: String,
    /** The league of the game, so a final is never matched across leagues that share a city. */
    val sport: String,
    val playerId: String,
    val playerName: String,
    val line: Double,
    val startUtc: Long,
    val homeName: String,
    val awayName: String,
    val externalId: String?,
)

@Dao
interface FeedDao {

    @Query(
        "SELECT p.id AS predictionId, p.market AS market, p.gameId AS gameId, p.playerId AS playerId, " +
            "pl.name AS playerName, o.line AS line, g.startUtc AS startUtc, g.sport AS sport, " +
            "g.homeName AS homeName, g.awayName AS awayName, g.externalId AS externalId " +
            "FROM predictions p " +
            "JOIN odds_snapshots o ON o.id = p.quoteId " +
            "JOIN players pl ON pl.id = p.playerId " +
            "JOIN games g ON g.id = p.gameId " +
            "LEFT JOIN player_results r ON r.predictionId = p.id " +
            "WHERE r.predictionId IS NULL AND o.source = '$ODDS_FEED_SOURCE' " +
            "AND g.startUtc <= :nowUtc AND g.startUtc >= :sinceUtc " +
            "ORDER BY g.startUtc DESC LIMIT 200",
    )
    suspend fun unsettled(nowUtc: Long, sinceUtc: Long): List<UnsettledRow>

    @Query("UPDATE games SET externalId = :externalId, status = :status WHERE id = :id")
    suspend fun attachResultGame(id: String, externalId: String, status: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStats(items: List<PlayerGameStatEntity>): List<Long>

    /**
     * Prices move, so a re-sync replaces a quote rather than keeping a stale one. A
     * prediction always answers to the newest quote behind it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotes(items: List<OddsSnapshotEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPredictions(items: List<PredictionEntity>): List<Long>

    /** Most recent box scores for one player, newest first. The model's whole input. */
    @Query("SELECT * FROM player_game_stats WHERE playerKey = :playerKey ORDER BY playedUtc DESC LIMIT :limit")
    suspend fun recentStats(playerKey: String, limit: Int): List<PlayerGameStatEntity>

    @Query("SELECT COUNT(DISTINCT playerKey) FROM player_game_stats")
    suspend fun loggedPlayerCount(): Int

    @Query("SELECT COUNT(*) FROM player_game_stats")
    suspend fun statCount(): Int

    @Query("SELECT * FROM player_game_stats WHERE playerKey = :playerKey AND eventId = :eventId LIMIT 1")
    suspend fun statFor(playerKey: String, eventId: String): PlayerGameStatEntity?

    /** How many lines are already stored for a game, so a re-sync does not re-read it. */
    @Query("SELECT COUNT(*) FROM player_game_stats WHERE eventId = :eventId")
    suspend fun statsForGame(eventId: String): Int

    /**
     * Team totals per finished game. The box score stores every player who played, so a
     * team's total is the sum of its players' points; a game with only one side stored
     * produces no usable pair and is dropped by the game model.
     */
    @Query(
        "SELECT eventId AS eventId, teamAbbr AS teamAbbr, SUM(points) AS points, " +
            "MAX(playedUtc) AS playedUtc FROM player_game_stats GROUP BY eventId, teamAbbr",
    )
    fun observeTeamTotals(): Flow<List<TeamTotalRow>>

    @Query(
        "SELECT eventId AS eventId, teamAbbr AS teamAbbr, SUM(points) AS points, " +
            "MAX(playedUtc) AS playedUtc FROM player_game_stats WHERE eventId = :eventId " +
            "GROUP BY eventId, teamAbbr",
    )
    suspend fun teamTotals(eventId: String): List<TeamTotalRow>

    @Query("SELECT externalId FROM games WHERE id = :id")
    suspend fun externalId(id: String): String?

    @Query("SELECT * FROM games WHERE startUtc BETWEEN :fromUtc AND :toUtc")
    suspend fun gamesBetween(fromUtc: Long, toUtc: Long): List<GameEntity>

    /** Returns -1 for a prediction that already has a result, so settlement is idempotent. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertResults(items: List<PlayerResultEntity>): List<Long>

    @Query("SELECT * FROM player_results")
    suspend fun results(): List<PlayerResultEntity>

    @Query("SELECT * FROM feed_state WHERE id = :id")
    fun observeState(id: String): Flow<FeedStateEntity?>

    @Query("SELECT * FROM feed_state WHERE id = :id")
    suspend fun state(id: String): FeedStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: FeedStateEntity)

    /**
     * Retires the synthetic board the first time real quotes arrive.
     *
     * The fixture slate is removed rather than greyed out, because a retired fixture still
     * renders: it shows invented names on the live board and turns the whole screen into a
     * warning banner. Paper bets and settlements keep their own snapshot of line and price,
     * so the ledger survives the delete and the live book can never trade invented prices.
     */
    @Query(
        "DELETE FROM settlements WHERE betId IN (SELECT b.id FROM paper_bets b " +
            "JOIN predictions p ON p.id = b.predictionId JOIN odds_snapshots o ON o.id = p.quoteId " +
            "WHERE o.source = :source)",
    )
    suspend fun deleteSettlementsForSource(source: String): Int

    @Query(
        "DELETE FROM paper_bets WHERE id IN (SELECT b.id FROM paper_bets b " +
            "JOIN predictions p ON p.id = b.predictionId JOIN odds_snapshots o ON o.id = p.quoteId " +
            "WHERE o.source = :source)",
    )
    suspend fun deleteBetsForSource(source: String): Int

    /** A bet whose prop row is gone can no longer be read or settled, so it is cleaned up. */
    @Query("SELECT COUNT(*) FROM paper_bets WHERE predictionId NOT IN (SELECT id FROM predictions)")
    suspend fun countOrphanBets(): Int

    @Query("DELETE FROM settlements WHERE betId IN (SELECT id FROM paper_bets WHERE predictionId NOT IN (SELECT id FROM predictions))")
    suspend fun deleteOrphanSettlements(): Int

    @Query("DELETE FROM paper_bets WHERE predictionId NOT IN (SELECT id FROM predictions)")
    suspend fun deleteOrphanBets(): Int

    @Query("DELETE FROM predictions WHERE quoteId IN (SELECT id FROM odds_snapshots WHERE source = :source)")
    suspend fun deletePredictionsForSource(source: String): Int

    @Query("DELETE FROM odds_snapshots WHERE source = :source")
    suspend fun deleteQuotesForSource(source: String): Int

    @Query("SELECT COUNT(*) FROM odds_snapshots WHERE source = :source")
    suspend fun countQuotesForSource(source: String): Int

    /**
     * The same cleanup, limited to one league.
     *
     * A real NBA price must retire the invented basketball board without touching the
     * football slate, and a real NFL price must do the reverse. Deleting every fixture at
     * once would make one league's key erase the other league's demo rows.
     */
    @Query(
        "SELECT COUNT(*) FROM odds_snapshots o JOIN games g ON g.id = o.gameId " +
            "WHERE o.source = :source AND g.sport = :sport",
    )
    suspend fun countQuotesForSourceInSport(source: String, sport: String): Int

    @Query(
        "DELETE FROM settlements WHERE betId IN (SELECT b.id FROM paper_bets b " +
            "JOIN predictions p ON p.id = b.predictionId " +
            "JOIN odds_snapshots o ON o.id = p.quoteId " +
            "JOIN games g ON g.id = p.gameId " +
            "WHERE o.source = :source AND g.sport = :sport)",
    )
    suspend fun deleteSettlementsForSourceInSport(source: String, sport: String): Int

    @Query(
        "DELETE FROM paper_bets WHERE id IN (SELECT b.id FROM paper_bets b " +
            "JOIN predictions p ON p.id = b.predictionId " +
            "JOIN odds_snapshots o ON o.id = p.quoteId " +
            "JOIN games g ON g.id = p.gameId " +
            "WHERE o.source = :source AND g.sport = :sport)",
    )
    suspend fun deleteBetsForSourceInSport(source: String, sport: String): Int

    @Query(
        "DELETE FROM predictions WHERE quoteId IN (SELECT o.id FROM odds_snapshots o " +
            "JOIN games g ON g.id = o.gameId WHERE o.source = :source AND g.sport = :sport)",
    )
    suspend fun deletePredictionsForSourceInSport(source: String, sport: String): Int

    @Query(
        "DELETE FROM odds_snapshots WHERE id IN (SELECT o.id FROM odds_snapshots o " +
            "JOIN games g ON g.id = o.gameId WHERE o.source = :source AND g.sport = :sport)",
    )
    suspend fun deleteQuotesForSourceInSport(source: String, sport: String): Int

    /** Fixture players are seeded as `p-…`; the odds feed mints `fp-…` ids. */
    @Query("DELETE FROM players WHERE id LIKE 'p-%' AND id NOT IN (SELECT DISTINCT playerId FROM odds_snapshots)")
    suspend fun deleteOrphanFixturePlayers(): Int

    @Query(
        "DELETE FROM games WHERE id IN ('g-past', 'g-hrb-lsv', 'g-ngw-dsf', 'g-smt-blt', " +
            "'g-nfl-kc-buf', 'g-nfl-sf-dal') " +
            "AND sport = :sport AND id NOT IN (SELECT DISTINCT gameId FROM odds_snapshots)",
    )
    suspend fun deleteFixtureGamesForSport(sport: String): Int

    /**
     * Game lines joined onto their game, so the board can name the teams the way the
     * schedule does rather than the way each book happens to spell them.
     */
    @Query(
        "SELECT gl.gameId AS gameId, g.startUtc AS startUtc, g.homeCode AS homeCode, " +
            "g.awayCode AS awayCode, g.homeName AS homeName, g.awayName AS awayName, " +
            "gl.book AS book, gl.market AS market, gl.side AS side, gl.line AS line, gl.price AS price " +
            "FROM game_lines gl JOIN games g ON g.id = gl.gameId " +
            "WHERE g.startUtc >= :sinceUtc ORDER BY g.startUtc, gl.market",
    )
    fun observeGameLines(sinceUtc: Long): Flow<List<GameLineRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameLines(items: List<GameLineEntity>): List<Long>

    /** A line is a snapshot: a refresh replaces the whole board rather than growing it. */
    @Query("DELETE FROM game_lines") suspend fun clearGameLines()
}

data class GameLineRow(
    val gameId: String,
    val startUtc: Long,
    val homeCode: String,
    val awayCode: String,
    val homeName: String,
    val awayName: String,
    val book: String,
    val market: String,
    val side: String,
    val line: Double?,
    val price: Int,
)
