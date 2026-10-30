package com.propslab.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where a signal came from, so the screen can say whether to trust it. */
object SignalSource {
    const val MANUAL = "manual"
    const val NEWS = "news"

    /** The league's own injury report: free, keyless, and the first to be published. */
    const val LEAGUE = "league"

    fun label(source: String): String = when (source) {
        MANUAL -> "entered by hand"
        NEWS -> "news report"
        LEAGUE -> "league injury report"
        else -> "report"
    }
}

/**
 * The statuses a late report can put a player in.
 *
 * There is no "healthy" status: a player with no signal simply has no row. Absence is the
 * default and the app never invents a signal to fill it.
 */
object AvailabilityStatus {
    const val GAME_TIME = "GAME_TIME"
    const val PROBABLE = "PROBABLE"
    const val QUESTIONABLE = "QUESTIONABLE"
    const val DOUBTFUL = "DOUBTFUL"
    const val OUT = "OUT"

    val ALL = listOf(GAME_TIME, PROBABLE, QUESTIONABLE, DOUBTFUL, OUT)

    fun parse(raw: String): String? =
        ALL.firstOrNull { it.equals(raw.trim(), ignoreCase = true) || it.replace('_', ' ').equals(raw.trim().replace('_', ' '), ignoreCase = true) }

    fun label(status: String): String = when (status) {
        GAME_TIME -> "Game-time decision"
        PROBABLE -> "Probable"
        QUESTIONABLE -> "Questionable"
        DOUBTFUL -> "Doubtful"
        OUT -> "Out"
        else -> status
    }
}

/**
 * One late report about one player.
 *
 * A signal is a claim with a clock on it. It expires, it is never edited, and a newer
 * signal supersedes an older one by being more recent — there is no delete, because a
 * report that turned out to be wrong is itself information.
 */
@Entity(tableName = "availability_signals", indices = [Index("playerKey"), Index("expiresUtc")])
data class AvailabilitySignalEntity(
    /** Stable per player and status-per-hour, so a repeated search does not stack rows. */
    @PrimaryKey val id: String,
    val playerKey: String,
    val playerName: String,
    val status: String,
    val detail: String,
    val source: String,
    val headline: String,
    val url: String,
    val observedUtc: Long,
    val expiresUtc: Long,
)

/**
 * One historical prop with the model's own probability and the result.
 *
 * This is what makes a theory testable: a dataset of prices, probabilities and outcomes
 * can run the same formula grid the sandbox runs, but on the world instead of a simulation.
 * Imported, never invented — a row with no `actual` cannot be tested and is rejected.
 */
@Entity(tableName = "history_props", indices = [Index("market"), Index("playedUtc")])
data class HistoryPropEntity(
    @PrimaryKey val id: String,
    val playedUtc: Long,
    val playerKey: String,
    val playerName: String,
    val market: String,
    val line: Double,
    val overOdds: Int,
    val underOdds: Int,
    val pOver: Double,
    val pPush: Double,
    val actual: Double,
    val source: String,
)
