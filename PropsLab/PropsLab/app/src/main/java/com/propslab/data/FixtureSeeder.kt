package com.propslab.data

import androidx.room.withTransaction
import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.PredictionGate
import com.propslab.domain.Settlement
import com.propslab.domain.Side
import com.propslab.domain.Sports
import com.propslab.inference.ExplainInput
import com.propslab.inference.LocalAssistant

/**
 * Synthetic data so the UI has something to render before real quotes and a trained bundle exist.
 * Names, teams, lines and probabilities are invented. Every row is tagged FIXTURE_SOURCE and the UI says so.
 */
internal class FixtureSeeder(
    private val db: PropsDatabase,
    private val assistant: LocalAssistant,
    private val now: Long,
) {
    private val min = 60_000L
    private val hour = PredictionGate.HOUR_MS
    private val model = "fixture-points-v0"

    private data class Fx(
        val key: String, val name: String, val team: String, val game: String,
        val line: Double?, val over: Int?, val under: Int?,
        val pOver: Double, val pPush: Double = 0.0,
        val flags: List<String> = emptyList(), val drivers: String = "",
        val quoteAgeMin: Long = 12, val cutoffAgeMin: Long = 20,
        /** The market this fixture is written on; football rows name one of the NFL markets. */
        val market: String = Markets.POINTS,
    )

    suspend fun seed() {
        val games = listOf(
            GameEntity("g-past", now - 20 * hour, "HRB", "BLT", "FINAL"),
            GameEntity("g-hrb-lsv", now + 150 * min, "HRB", "LSV", "SCHEDULED"),
            GameEntity("g-ngw-dsf", now + 240 * min, "NGW", "DSF", "SCHEDULED"),
            GameEntity("g-smt-blt", now + 330 * min, "SMT", "BLT", "SCHEDULED"),
        ) + footballGames
        val tonight = listOf(
            Fx("aldana", "Dev Aldana", "HRB", "g-hrb-lsv", 26.5, -115, -105, 0.552,
                flags = listOf("Minutes projected from a rolling average"),
                drivers = "Recent minutes=1.9;Usage rate=1.1;Opponent defense=-0.6;Rest days=0.4"),
            Fx("brandt", "Kofi Brandt", "LSV", "g-hrb-lsv", 22.5, -110, -110, 0.468,
                drivers = "Opponent defense=-1.8;Recent minutes=-0.7;Pace=0.9;Rest days=-0.3"),
            Fx("tarrant", "Nico Tarrant", "DSF", "g-ngw-dsf", 29.5, -105, -125, 0.431,
                flags = listOf("Teammate availability changed 40 minutes before cutoff"),
                drivers = "Teammate availability=-2.4;Back-to-back=-1.5;Usage rate=0.8;Pace=0.3"),
            Fx("okafor", "Remy Okafor", "NGW", "g-ngw-dsf", 18.5, -120, 100, 0.571,
                drivers = "Recent minutes=2.2;Starting role=1.3;Opponent defense=0.9;Home court=0.2"),
            Fx("duvall", "Ansel Duvall", "BLT", "g-smt-blt", 25.0, -110, -110, 0.472, pPush = 0.046,
                flags = listOf("Integer line, so a push is possible"),
                drivers = "Recent minutes=-0.4;Usage rate=0.3;Pace=0.2;Rest days=-0.2"),
            Fx("moriyama", "Cade Moriyama", "SMT", "g-smt-blt", 21.5, -110, -110, 0.50, cutoffAgeMin = 31 * 60),
            Fx("abara", "Soren Abara", "SMT", "g-smt-blt", null, null, null, 0.50),
        )
        val nfl = nflFixtures
        val past = listOf(
            Fx("past-aldana", "Dev Aldana", "HRB", "g-past", 24.5, -110, -110, 0.540, quoteAgeMin = 22 * 60, cutoffAgeMin = 22 * 60 + 8),
            Fx("past-duvall", "Ansel Duvall", "BLT", "g-past", 26.5, -110, -110, 0.455, quoteAgeMin = 22 * 60, cutoffAgeMin = 22 * 60 + 8),
            Fx("past-lindqvist", "Pia Lindqvist", "HRB", "g-past", 17.0, -115, -105, 0.490, pPush = 0.05, quoteAgeMin = 22 * 60, cutoffAgeMin = 22 * 60 + 8),
        )

        write(games, tonight + past + nfl, withBets = true, banner = true)
    }

    /**
     * The football half on its own, for an install that was seeded before football existed.
     * A device with basketball fixtures already stored would otherwise never see an NFL
     * prop: the first seed only runs when the prediction table is completely empty.
     */
    suspend fun seedFootball() {
        write(footballGames, nflFixtures, withBets = false, banner = false)
    }

    private val footballGames: List<GameEntity>
        get() = listOf(
            // Real clubs and the players a fan would look for, on invented lines: the same
            // bargain every fixture here makes, and the board labels it as one. The codes
            // match the bundled NFL club marks.
            GameEntity("g-nfl-kc-buf", now + 200 * min, "BUF", "KC", "SCHEDULED", sport = Sports.FOOTBALL),
            GameEntity("g-nfl-sf-dal", now + 380 * min, "DAL", "SF", "SCHEDULED", sport = Sports.FOOTBALL),
        )

    private val nflFixtures: List<Fx> = listOf(
        Fx("mahomes-yds", "Patrick Mahomes", "KC", "g-nfl-kc-buf", 279.5, -115, -105, 0.545,
            market = Markets.PASS_YARDS,
            flags = listOf("Synthetic fixture: the line and the chance are invented"),
            drivers = "Pass attempts=1.8;Opponent pressure=-1.1;Home field=0.4;Rest days=0.2"),
        Fx("allen-yds", "Josh Allen", "BUF", "g-nfl-kc-buf", 256.5, -110, -110, 0.512,
            market = Markets.PASS_YARDS,
            drivers = "Pass attempts=1.2;Rush threat=0.9;Opponent coverage=0.7;Weather=-0.2"),
        Fx("allen-rush", "Josh Allen", "BUF", "g-nfl-kc-buf", 47.5, -120, 100, 0.470,
            market = Markets.RUSH_YARDS,
            flags = listOf("Same player, a second market: each row is its own bet"),
            drivers = "Designed runs=-0.8;Scramble rate=0.6;Game script=0.5;Opponent front=0.1"),
        Fx("kelce-rec", "Travis Kelce", "KC", "g-nfl-kc-buf", 6.5, -130, 110, 0.538,
            market = Markets.RECEPTIONS,
            drivers = "Target share=1.6;Slot snaps=0.8;Opponent linebackers=0.5;Score script=-0.3"),
        Fx("mccaffrey-rush", "Christian McCaffrey", "SF", "g-nfl-sf-dal", 72.5, -110, -110, 0.558,
            market = Markets.RUSH_YARDS,
            drivers = "Carries=1.9;Offensive line=1.2;Opponent front=-0.6;Rest days=0.3"),
        Fx("lamb-rec", "CeeDee Lamb", "DAL", "g-nfl-sf-dal", 76.5, -105, -115, 0.462,
            market = Markets.RECEIVING_YARDS,
            drivers = "Target share=-0.9;Deep routes=0.4;Opponent corner=-1.2;Home field=0.3"),
        Fx("kittle-rec", "George Kittle", "SF", "g-nfl-sf-dal", 58.5, -115, -105, 0.505,
            market = Markets.RECEIVING_YARDS,
            flags = listOf("Synthetic fixture: the line and the chance are invented"),
            drivers = "Target share=1.4;Slot snaps=1.0;Opponent corner=0.2;Game script=-0.4"),
    )

    private suspend fun write(
        games: List<GameEntity>,
        all: List<Fx>,
        withBets: Boolean,
        banner: Boolean,
    ) {
        val players = all.map { PlayerEntity("p-${it.name.lowercase().replace(' ', '-')}", it.name, it.team) }.distinctBy { it.id }
        val events = mutableListOf<AuditEventEntity>()
        val quotes = mutableListOf<OddsSnapshotEntity>()
        val preds = mutableListOf<PredictionEntity>()

        if (banner) {
            events += AuditEventEntity(ts = now - 25 * hour, kind = "fixtures_loaded", subjectId = "fixtures", detail = "Synthetic fixtures loaded for interface development")
            events += AuditEventEntity(ts = now - 25 * hour + 1, kind = "model_registered", subjectId = model, detail = "Fixture scorer registered, schema 1, not validated")
        }

        for (f in all) {
            val quoteTs = now - f.quoteAgeMin * min
            val cutoffTs = now - f.cutoffAgeMin * min
            val pid = "p-${f.name.lowercase().replace(' ', '-')}"
            val qid = "q-${f.key}"
            quotes += OddsSnapshotEntity(qid, f.game, pid, f.market, "Fixture book", f.line, f.over, f.under, quoteTs, FIXTURE_SOURCE)

            // Past-game fixtures were fresh when they were made, so judge them at their own creation time.
            val evalNow = if (f.game == "g-past") quoteTs + 2 * min else now
            val reasons = PredictionGate.evaluate(f.line, quoteTs, cutoffTs, evalNow, true, f.market)
            val probs = if (reasons.isEmpty()) OutcomeProbabilities(f.pOver, f.pPush, 1.0 - f.pOver - f.pPush) else null
            val line = f.line
            val explanation = if (probs != null && line != null && f.over != null && f.under != null) {
                assistant.explain(ExplainInput(f.name, line, probs, Odds.noVigOver(f.over, f.under), f.flags))
            } else ""
            preds += PredictionEntity(
                id = "pred-${f.key}", quoteId = qid, gameId = f.game, playerId = pid, market = f.market,
                modelVersion = model, featureSchemaVersion = 1, featureCutoffUtc = cutoffTs,
                pOver = probs?.over, pPush = probs?.push, pUnder = probs?.under,
                status = if (probs != null) Status.AVAILABLE else Status.UNAVAILABLE,
                unavailableReasons = reasons.joinToString(",") { it.name },
                qualityFlags = f.flags.joinToString("|"), drivers = f.drivers, explanation = explanation, createdUtc = quoteTs + min,
            )
            events += if (probs != null) {
                AuditEventEntity(ts = quoteTs + min, kind = "prediction_created", subjectId = "pred-${f.key}",
                    detail = "${f.name}, ${f.line} ${Markets.short(f.market)}, over ${"%.1f".format(probs.over * 100)}%, cutoff recorded")
            } else {
                AuditEventEntity(ts = quoteTs + min, kind = "prediction_rejected", subjectId = "pred-${f.key}",
                    detail = "${f.name}: " + reasons.joinToString("; ") { it.message })
            }
        }

        // Three settled paper bets from the past game and one open bet tonight. A football
        // top-up writes none of these: the ledger is the basketball demo's history, and
        // inventing a second history for the new league would double-count it.
        val bets = if (withBets) listOf(
            Triple(PaperBetEntity("bet-pred-past-aldana-over", "pred-past-aldana", Side.OVER.name, -110, 24.5, 2.0, now - 21 * hour), 28.0, "Dev Aldana"),
            Triple(PaperBetEntity("bet-pred-past-duvall-under", "pred-past-duvall", Side.UNDER.name, -110, 26.5, 1.0, now - 21 * hour + 5 * min), 29.0, "Ansel Duvall"),
            Triple(PaperBetEntity("bet-pred-past-lindqvist-over", "pred-past-lindqvist", Side.OVER.name, -115, 17.0, 1.0, now - 21 * hour + 9 * min), 17.0, "Pia Lindqvist"),
        ) else emptyList()
        val settlements = bets.mapIndexed { i, (b, actual, _) ->
            val outcome = Settlement.outcome(Side.valueOf(b.side), b.line, actual)
            SettlementEntity(b.id, actual, outcome.name, Settlement.profit(outcome, b.stakeUnits, b.quotedOdds),
                Settlement.RULE_VERSION, now - 17 * hour + i * min, "fixture")
        }
        val openBet = PaperBetEntity("bet-pred-okafor-over", "pred-okafor", Side.OVER.name, -120, 18.5, 1.0, now - 9 * min)
        if (withBets) {
            for ((b, _, name) in bets) {
                events += AuditEventEntity(ts = b.decidedUtc, kind = "paper_bet_placed", subjectId = b.id, detail = "$name ${Side.valueOf(b.side).label} ${b.line}, ${"%.1f".format(b.stakeUnits)} units")
            }
            for ((s, b) in settlements.zip(bets.map { it.first })) {
                events += AuditEventEntity(ts = s.settledUtc, kind = "result_recorded", subjectId = b.id,
                    detail = "Actual ${s.actual} against ${b.line}: ${s.outcome.lowercase()}, ${"%+.2f".format(s.profitUnits)} units")
            }
            events += AuditEventEntity(ts = openBet.decidedUtc, kind = "paper_bet_placed", subjectId = openBet.id, detail = "Remy Okafor Over 18.5, 1.0 units")
        }

        db.withTransaction {
            db.slateDao().insertGames(games)
            db.slateDao().insertPlayers(players)
            if (banner) {
                db.slateDao().insertModels(listOf(ModelVersionEntity(model, "none", 1, "Fixture. Not validated.", now - 25 * hour, true)))
            }
            db.slateDao().insertQuotes(quotes)
            db.slateDao().insertPredictions(preds)
            if (withBets) {
                db.paperDao().insertBets(bets.map { it.first } + openBet)
                db.paperDao().insertSettlements(settlements)
            }
            db.auditDao().insertAll(events)
        }
    }
}
