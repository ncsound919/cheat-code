package com.propslab.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object Status {
    const val AVAILABLE = "AVAILABLE"
    const val UNAVAILABLE = "UNAVAILABLE"
}

const val FIXTURE_SOURCE = "synthetic_fixture"

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val startUtc: Long,
    val homeCode: String,
    val awayCode: String,
    val status: String,
    /** The results provider's id for this game, once the two feeds have been joined. */
    val externalId: String? = null,
    /** Full team names, kept so a fixture-free game can be matched to a box score by name. */
    @ColumnInfo(defaultValue = "") val homeName: String = "",
    @ColumnInfo(defaultValue = "") val awayName: String = "",
)

@Entity(tableName = "players")
data class PlayerEntity(@PrimaryKey val id: String, val name: String, val teamCode: String)

@Entity(tableName = "odds_snapshots", indices = [Index("gameId"), Index("playerId")])
data class OddsSnapshotEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val playerId: String,
    val market: String,
    val book: String,
    val line: Double?,
    val overOdds: Int?,
    val underOdds: Int?,
    val quoteTimeUtc: Long,
    val source: String,
)

@Entity(tableName = "predictions", indices = [Index("quoteId"), Index("gameId"), Index("playerId")])
data class PredictionEntity(
    @PrimaryKey val id: String,
    val quoteId: String,
    val gameId: String,
    val playerId: String,
    val market: String,
    val modelVersion: String,
    val featureSchemaVersion: Int,
    val featureCutoffUtc: Long,
    val pOver: Double?,
    val pPush: Double?,
    val pUnder: Double?,
    val status: String,
    /** Comma-separated RejectReason names. */
    val unavailableReasons: String,
    /** Pipe-separated readable data-quality flags. */
    val qualityFlags: String,
    /** Semicolon-separated "label=value" feature influences, in probability points. */
    val drivers: String,
    val explanation: String,
    val createdUtc: Long,
)

@Entity(tableName = "paper_bets", indices = [Index("predictionId")])
data class PaperBetEntity(
    @PrimaryKey val id: String,
    val predictionId: String,
    val side: String,
    val quotedOdds: Int,
    /** The line at decision time, so later line moves cannot change settlement. */
    val line: Double,
    val stakeUnits: Double,
    val decidedUtc: Long,
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val betId: String,
    val actual: Double,
    val outcome: String,
    val profitUnits: Double,
    val ruleVersion: String,
    val settledUtc: Long,
    val reconciliation: String,
)

/**
 * One book's price on one side of a game market: the moneyline, a spread or a total.
 *
 * These are the book's own numbers, not a model estimate, so they are kept apart from
 * `odds_snapshots` and never grow a prediction. The row is replaced on every refresh
 * because a line is a snapshot, not a history.
 */
@Entity(tableName = "game_lines", indices = [Index("gameId"), Index("market")])
data class GameLineEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val book: String,
    /** `h2h`, `spreads` or `totals`, the provider's own names. */
    val market: String,
    /** The team's full name, or `Over` / `Under` for a total. */
    val side: String,
    /** The handicap or the total; null on a moneyline. */
    val line: Double?,
    val price: Int,
    val quoteTimeUtc: Long,
)

@Entity(tableName = "model_versions")
data class ModelVersionEntity(
    @PrimaryKey val version: String,
    val artifactHash: String,
    val schemaVersion: Int,
    val evalStatus: String,
    val importedUtc: Long,
    val active: Boolean,
)

/**
 * One line of the agent console. Append-only, like the audit log, because a transcript a
 * turn can be silently rewritten is not a record of what the system said.
 */
@Entity(tableName = "console_turns")
data class ConsoleTurn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val kind: String,
    val text: String,
    val toolName: String? = null,
    val createdUtc: Long,
)

/** Append-only: the DAO exposes insert and read, never update or delete. */
@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val kind: String,
    val subjectId: String,
    val detail: String,
)
