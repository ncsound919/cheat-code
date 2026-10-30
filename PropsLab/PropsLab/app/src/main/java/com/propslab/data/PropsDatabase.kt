package com.propslab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GameEntity::class, PlayerEntity::class, OddsSnapshotEntity::class, PredictionEntity::class,
        PaperBetEntity::class, SettlementEntity::class, ModelVersionEntity::class, AuditEventEntity::class,
        FormulaRunEntity::class, SandboxTradeEntity::class, SandboxStateEntity::class,
        PlayerGameStatEntity::class, PlayerResultEntity::class, FeedStateEntity::class,
        AvailabilitySignalEntity::class, HistoryPropEntity::class, ConsoleTurn::class,
        ClaimFormulaEntity::class, GameLineEntity::class,
        LearningBetEntity::class, CalibrationEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class PropsDatabase : RoomDatabase() {
    abstract fun slateDao(): SlateDao
    abstract fun paperDao(): PaperDao
    abstract fun auditDao(): AuditDao
    abstract fun sandboxDao(): SandboxDao
    abstract fun feedDao(): FeedDao
    abstract fun signalDao(): SignalDao
    abstract fun consoleDao(): ConsoleDao
    abstract fun claimDao(): ClaimDao
    abstract fun learningDao(): LearningDao

    companion object {

        /**
         * Adds the formula sandbox. Nothing existing is touched, so a device that already
         * holds predictions and paper bets keeps all of them.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `formula_runs` (
                        `id` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `params` TEXT NOT NULL,
                        `tier` TEXT NOT NULL,
                        `bets` INTEGER NOT NULL,
                        `wins` INTEGER NOT NULL,
                        `losses` INTEGER NOT NULL,
                        `pushes` INTEGER NOT NULL,
                        `staked` REAL NOT NULL,
                        `profit` REAL NOT NULL,
                        `roi` REAL NOT NULL,
                        `tStat` REAL NOT NULL,
                        `maxDrawdown` REAL NOT NULL,
                        `finalBankroll` REAL NOT NULL,
                        `verdict` TEXT NOT NULL,
                        `reasons` TEXT NOT NULL,
                        `round` INTEGER NOT NULL,
                        `updatedUtc` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sandbox_trades` (
                        `id` TEXT NOT NULL,
                        `formulaId` TEXT NOT NULL,
                        `predictionId` TEXT NOT NULL,
                        `playerName` TEXT NOT NULL,
                        `side` TEXT NOT NULL,
                        `line` REAL NOT NULL,
                        `odds` INTEGER NOT NULL,
                        `stake` REAL NOT NULL,
                        `outcome` TEXT,
                        `profit` REAL,
                        `startUtc` INTEGER NOT NULL,
                        `placedUtc` INTEGER NOT NULL,
                        `settledUtc` INTEGER,
                        `synthetic` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sandbox_trades_formulaId` ON `sandbox_trades` (`formulaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sandbox_trades_predictionId` ON `sandbox_trades` (`predictionId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sandbox_state` (
                        `id` TEXT NOT NULL,
                        `round` INTEGER NOT NULL,
                        `seed` INTEGER NOT NULL,
                        `lastRunUtc` INTEGER NOT NULL,
                        `promotedId` TEXT,
                        `promotedLabel` TEXT,
                        `note` TEXT NOT NULL,
                        `bankroll` REAL NOT NULL,
                        `peak` REAL NOT NULL,
                        `bets` INTEGER NOT NULL,
                        `profit` REAL NOT NULL,
                        `autoRun` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Adds the real feed: box scores, the results they settle, and the sync's own state.
         * The three new columns on `games` are nullable or defaulted, so every existing row
         * keeps its old meaning.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `games` ADD COLUMN `externalId` TEXT")
                db.execSQL("ALTER TABLE `games` ADD COLUMN `homeName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `games` ADD COLUMN `awayName` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `player_game_stats` (
                        `id` TEXT NOT NULL,
                        `eventId` TEXT NOT NULL,
                        `playedUtc` INTEGER NOT NULL,
                        `playerKey` TEXT NOT NULL,
                        `playerName` TEXT NOT NULL,
                        `teamAbbr` TEXT NOT NULL,
                        `points` INTEGER NOT NULL,
                        `rebounds` INTEGER NOT NULL,
                        `assists` INTEGER NOT NULL,
                        `threes` INTEGER NOT NULL,
                        `minutes` INTEGER NOT NULL,
                        `fetchedUtc` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_player_game_stats_playerKey` ON `player_game_stats` (`playerKey`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `player_results` (
                        `predictionId` TEXT NOT NULL,
                        `market` TEXT NOT NULL,
                        `actual` REAL NOT NULL,
                        `statId` TEXT NOT NULL,
                        `recordedUtc` INTEGER NOT NULL,
                        PRIMARY KEY(`predictionId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_results_recordedUtc` ON `player_results` (`recordedUtc`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `feed_state` (
                        `id` TEXT NOT NULL,
                        `lastPropsSyncUtc` INTEGER NOT NULL,
                        `lastResultsSyncUtc` INTEGER NOT NULL,
                        `propsNote` TEXT NOT NULL,
                        `resultNote` TEXT NOT NULL,
                        `creditsRemaining` INTEGER,
                        `quotesWritten` INTEGER NOT NULL,
                        `predictionsScored` INTEGER NOT NULL,
                        `resultsRecorded` INTEGER NOT NULL,
                        `fixturesRetired` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Adds late-availability signals and the imported history a theory can be tested on.
         * Neither table touches a row that already exists.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `availability_signals` (
                        `id` TEXT NOT NULL,
                        `playerKey` TEXT NOT NULL,
                        `playerName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `detail` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `headline` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `observedUtc` INTEGER NOT NULL,
                        `expiresUtc` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_availability_signals_playerKey` ON `availability_signals` (`playerKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_availability_signals_expiresUtc` ON `availability_signals` (`expiresUtc`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `history_props` (
                        `id` TEXT NOT NULL,
                        `playedUtc` INTEGER NOT NULL,
                        `playerKey` TEXT NOT NULL,
                        `playerName` TEXT NOT NULL,
                        `market` TEXT NOT NULL,
                        `line` REAL NOT NULL,
                        `overOdds` INTEGER NOT NULL,
                        `underOdds` INTEGER NOT NULL,
                        `pOver` REAL NOT NULL,
                        `pPush` REAL NOT NULL,
                        `actual` REAL NOT NULL,
                        `source` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_props_market` ON `history_props` (`market`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_props_playedUtc` ON `history_props` (`playedUtc`)")
                db.execSQL("ALTER TABLE `feed_state` ADD COLUMN `signalNote` TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Adds the agent console transcript. One new table; nothing existing is touched. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `console_turns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `role` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `toolName` TEXT,
                        `createdUtc` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Adds user-created strategies. One new table; every existing row keeps its meaning. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `claim_formulas` (
                        `id` TEXT NOT NULL,
                        `claim` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `factorTag` TEXT NOT NULL,
                        `headline` TEXT NOT NULL,
                        `lens` TEXT NOT NULL,
                        `formulaId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `bets` INTEGER NOT NULL,
                        `wins` INTEGER NOT NULL,
                        `losses` INTEGER NOT NULL,
                        `profit` REAL NOT NULL,
                        `roi` REAL NOT NULL,
                        `tStat` REAL NOT NULL,
                        `maxDrawdown` REAL NOT NULL,
                        `passed` INTEGER NOT NULL,
                        `plain` TEXT NOT NULL,
                        `boardNote` TEXT NOT NULL,
                        `gameTerms` TEXT NOT NULL,
                        `falsifier` TEXT NOT NULL,
                        `subject` TEXT,
                        `deepVerdict` TEXT NOT NULL,
                        `deepNote` TEXT NOT NULL,
                        `deepAtUtc` INTEGER NOT NULL,
                        `createdUtc` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Adds game lines: the moneyline, spread and total behind each game. One new table. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `game_lines` (
                        `id` TEXT NOT NULL,
                        `gameId` TEXT NOT NULL,
                        `book` TEXT NOT NULL,
                        `market` TEXT NOT NULL,
                        `side` TEXT NOT NULL,
                        `line` REAL,
                        `price` INTEGER NOT NULL,
                        `quoteTimeUtc` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_lines_gameId` ON `game_lines` (`gameId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_lines_market` ON `game_lines` (`market`)")
            }
        }

        /**
         * Adds the learning ledger and the calibration row. Two new tables; every existing
         * row keeps its meaning, so a phone that already has a record keeps it.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `learning_bets` (
                        `id` TEXT NOT NULL,
                        `track` TEXT NOT NULL,
                        `market` TEXT NOT NULL,
                        `gameId` TEXT NOT NULL,
                        `refId` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `homeCode` TEXT NOT NULL,
                        `awayCode` TEXT NOT NULL,
                        `side` TEXT NOT NULL,
                        `line` REAL NOT NULL,
                        `odds` INTEGER NOT NULL,
                        `pModel` REAL NOT NULL,
                        `pPush` REAL NOT NULL,
                        `pMarket` REAL,
                        `edgePts` REAL,
                        `confidence` TEXT NOT NULL,
                        `stake` REAL NOT NULL,
                        `outcome` TEXT,
                        `profit` REAL,
                        `startUtc` INTEGER NOT NULL,
                        `loggedUtc` INTEGER NOT NULL,
                        `settledUtc` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_bets_track` ON `learning_bets` (`track`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_bets_outcome` ON `learning_bets` (`outcome`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_bets_gameId` ON `learning_bets` (`gameId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_calibration` (
                        `track` TEXT NOT NULL,
                        `slope` REAL NOT NULL,
                        `intercept` REAL NOT NULL,
                        `sample` INTEGER NOT NULL,
                        `brierBefore` REAL NOT NULL,
                        `brierAfter` REAL NOT NULL,
                        `generation` INTEGER NOT NULL,
                        `updatedUtc` INTEGER NOT NULL,
                        PRIMARY KEY(`track`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Adds the historical second gate to a formula's stored result. Existing rows are
         * untouched and read as NOT RUN, which is exactly what they were.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `formula_runs` ADD COLUMN `historyVerdict` TEXT NOT NULL DEFAULT 'NOT RUN'")
                db.execSQL("ALTER TABLE `formula_runs` ADD COLUMN `historyBets` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `formula_runs` ADD COLUMN `historyRoi` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `formula_runs` ADD COLUMN `historyNote` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun build(context: Context): PropsDatabase =
            Room.databaseBuilder(context, PropsDatabase::class.java, "propslab.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                )
                .build()
    }
}
