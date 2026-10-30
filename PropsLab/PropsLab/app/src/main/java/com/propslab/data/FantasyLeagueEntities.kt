package com.propslab.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * A fantasy league the user captured from screenshots.
 *
 * The league is the anchor; every roster row belongs to one week, so a player added in
 * September is not overwritten by the same player's October row. That is what makes the
 * season track possible without a second, disconnected history table.
 */
@Entity(tableName = "fantasy_leagues", indices = [Index("sport")])
data class FantasyLeagueEntity(
    @PrimaryKey val id: String,
    val sport: String,
    val platform: String,
    val name: String,
    val scoring: String,
    val myTeam: String,
    val week: Int,
    val importedUtc: Long,
    val updatedUtc: Long,
    val note: String,
)

/** One fantasy team, as a standings screenshot or a lineup screenshot described it. */
@Entity(tableName = "fantasy_teams", primaryKeys = ["leagueId", "name"], indices = [Index("leagueId")])
data class FantasyTeamEntity(
    val leagueId: String,
    val name: String,
    val owner: String,
    val rank: Int,
    val wins: Int,
    val losses: Int,
    val ties: Int,
    val pointsFor: Double?,
    val pointsAgainst: Double?,
    val isMine: Boolean,
    val updatedUtc: Long,
) {
    val record: String get() = if (ties > 0) "$wins-$losses-$ties" else "$wins-$losses"
}

/** One player on one team in one week. The row is replaced on a re-import of that same week. */
@Entity(
    tableName = "fantasy_roster",
    primaryKeys = ["id"],
    indices = [Index("leagueId"), Index(value = ["leagueId", "week"]), Index("playerKey")],
)
data class FantasyRosterEntity(
    val id: String,
    val leagueId: String,
    val week: Int,
    val teamName: String,
    val playerKey: String,
    val playerName: String,
    val slot: String,
    val position: String,
    val proTeam: String,
    val opponent: String,
    val projected: Double?,
    val seasonPoints: Double?,
    val athleteId: String,
    val headshotUrl: String,
    val teamLogoUrl: String,
    val source: String,
    val updatedUtc: Long,
)

/** The weekly matchup snapshot. The roster rows are the detail; this is the line score. */
@Entity(tableName = "fantasy_weeks", primaryKeys = ["id"], indices = [Index("leagueId")])
data class FantasyWeekEntity(
    val id: String,
    val leagueId: String,
    val week: Int,
    val myTeam: String,
    val opponent: String,
    val myProjected: Double?,
    val oppProjected: Double?,
    val myScore: Double?,
    val oppScore: Double?,
    val note: String,
    val updatedUtc: Long,
)

/** The league plus every row that belongs to it, in one Room transaction. */
data class FantasyLeagueWithData(
    @Embedded val league: FantasyLeagueEntity,
    @Relation(parentColumn = "id", entityColumn = "leagueId")
    val teams: List<FantasyTeamEntity>,
    @Relation(parentColumn = "id", entityColumn = "leagueId")
    val roster: List<FantasyRosterEntity>,
    @Relation(parentColumn = "id", entityColumn = "leagueId")
    val weeks: List<FantasyWeekEntity>,
) {
    fun rosterFor(week: Int, teamName: String): List<FantasyRosterEntity> =
        roster.filter { it.week == week && it.teamName.equals(teamName, ignoreCase = true) }
}

@Dao
interface FantasyLeagueDao {

    @Transaction
    @Query("SELECT * FROM fantasy_leagues WHERE sport = :sport ORDER BY updatedUtc DESC LIMIT 1")
    fun observeLatest(sport: String): Flow<FantasyLeagueWithData?>

    @Transaction
    @Query("SELECT * FROM fantasy_leagues WHERE sport = :sport ORDER BY updatedUtc DESC LIMIT 1")
    suspend fun latest(sport: String): FantasyLeagueWithData?

    @Transaction
    @Query("SELECT * FROM fantasy_leagues WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): FantasyLeagueWithData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeague(league: FantasyLeagueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeams(teams: List<FantasyTeamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoster(roster: List<FantasyRosterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeek(week: FantasyWeekEntity)

    @Query("DELETE FROM fantasy_teams WHERE leagueId = :leagueId")
    suspend fun deleteTeams(leagueId: String)

    @Query("DELETE FROM fantasy_roster WHERE leagueId = :leagueId AND week = :week AND teamName = :teamName")
    suspend fun clearRoster(leagueId: String, week: Int, teamName: String)

    @Query("UPDATE fantasy_teams SET isMine = 0 WHERE leagueId = :leagueId")
    suspend fun clearMine(leagueId: String)

    @Query("SELECT * FROM fantasy_teams WHERE leagueId = :leagueId AND name = :name LIMIT 1")
    suspend fun team(leagueId: String, name: String): FantasyTeamEntity?

    @Query("SELECT * FROM fantasy_weeks WHERE id = :id LIMIT 1")
    suspend fun week(id: String): FantasyWeekEntity?

    @Query("SELECT * FROM fantasy_roster WHERE leagueId = :leagueId")
    suspend fun roster(leagueId: String): List<FantasyRosterEntity>

    @Query(
        "UPDATE fantasy_roster SET athleteId = :athleteId, headshotUrl = :headshotUrl, " +
            "teamLogoUrl = :teamLogoUrl, proTeam = :proTeam, position = :position " +
            "WHERE leagueId = :leagueId AND playerKey = :playerKey",
    )
    suspend fun setPlayerAssets(
        leagueId: String,
        playerKey: String,
        athleteId: String,
        headshotUrl: String,
        teamLogoUrl: String,
        proTeam: String,
        position: String,
    )
}
