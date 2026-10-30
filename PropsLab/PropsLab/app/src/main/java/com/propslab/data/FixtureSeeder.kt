package com.propslab.data

import androidx.room.withTransaction
import com.propslab.domain.Odds
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.PredictionGate
import com.propslab.domain.Settlement
import com.propslab.domain.Side
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
    )

    suspend fun seed() {
        val games = listOf(
            GameEntity("g-past", now - 20 * hour, "HRB", "BLT", "FINAL"),
            GameEntity("g-hrb-lsv", now + 150 * min, "HRB", "LSV", "SCHEDULED"),
            GameEntity("g-ngw-dsf", now + 240 * min, "NGW", "DSF", "SCHEDULED"),
            GameEntity("g-smt-blt", now + 330 * min, "SMT", "BLT", "SCHEDULED"),
        )
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
        val past = listOf(
            Fx("past-aldana", "Dev Aldana", "HRB", "g-past", 24.5, -110, -110, 0.540, quoteAgeMin = 22 * 60, cutoffAgeMin = 22 * 60 + 8),
            Fx("past-duvall", "Ansel Duvall", "BLT", "g-past", 26.5, -110, -110, 0.455, quoteAgeMin = 22 * 60, cutoffAgeMin = 22 * 60 + 8),
            Fx("past-lindqvist", "Pia Lindqvist", "HRB", "g-past", 17.0, -115, -105, 0.490, pPush = 0.05, quoteAgeMin = 22 * 60, cutoffAgeMin = 22 * 60 + 8),
        )

        val all = tonight + past
        val players = all.map { PlayerEntity("p-${it.name.lowercase().replace(' ', '-')}", it.name, it.team) }.distinctBy { it.id }
        val events = mutableListOf<AuditEventEntity>()
        val quotes = mutableListOf<OddsSnapshotEntity>()
        val preds = mutableListOf<PredictionEntity>()

        events += AuditEventEntity(ts = now - 25 * hour, kind = "fixtures_loaded", subjectId = "fixtures", detail = "Synthetic fixtures loaded for interface development")
        events += AuditEventEntity(ts = now - 25 * hour + 1, kind = "model_registered", subjectId = model, detail = "Fixture scorer registered, schema 1, not validated")

        for (f in all) {
            val quoteTs = now - f.quoteAgeMin * min
            val cutoffTs = now - f.cutoffAgeMin * min
            val pid = "p-${f.name.lowercase().replace(' ', '-')}"
            val qid = "q-${f.key}"
            quotes += OddsSnapshotEntity(qid, f.game, pid, "player_points", "Fixture book", f.line, f.over, f.under, quoteTs, FIXTURE_SOURCE)

            // Past-game fixtures were fresh when they were made, so judge them at their own creation time.
            val evalNow = if (f.game == "g-past") quoteTs + 2 * min else now
            val reasons = PredictionGate.evaluate(f.line, quoteTs, cutoffTs, evalNow, true, "player_points")
            val probs = if (reasons.isEmpty()) OutcomeProbabilities(f.pOver, f.pPush, 1.0 - f.pOver - f.pPush) else null
            val explanation = if (probs != null && f.over != null && f.under != null) {
                assistant.explain(ExplainInput(f.name, f.line!!, probs, Odds.noVigOver(f.over, f.under), f.flags))
            } else ""
            preds += PredictionEntity(
                id = "pred-${f.key}", quoteId = qid, gameId = f.game, playerId = pid, market = "player_points",
                modelVersion = model, featureSchemaVersion = 1, featureCutoffUtc = cutoffTs,
                pOver = probs?.over, pPush = probs?.push, pUnder = probs?.under,
                status = if (probs != null) Status.AVAILABLE else Status.UNAVAILABLE,
                unavailableReasons = reasons.joinToString(",") { it.name },
                qualityFlags = f.flags.joinToString("|"), drivers = f.drivers, explanation = explanation, createdUtc = quoteTs + min,
            )
            events += if (probs != null) {
                AuditEventEntity(ts = quoteTs + min, kind = "prediction_created", subjectId = "pred-${f.key}",
                    detail = "${f.name}, points ${f.line}, over ${"%.1f".format(probs.over * 100)}%, cutoff recorded")
            } else {
                AuditEventEntity(ts = quoteTs + min, kind = "prediction_rejected", subjectId = "pred-${f.key}",
                    detail = "${f.name}: " + reasons.joinToString("; ") { it.message })
            }
        }

        // Three settled paper bets from the past game and one open bet tonight.
        val bets = listOf(
            Triple(PaperBetEntity("bet-pred-past-aldana-over", "pred-past-aldana", Side.OVER.name, -110, 24.5, 2.0, now - 21 * hour), 28.0, "Dev Aldana"),
            Triple(PaperBetEntity("bet-pred-past-duvall-under", "pred-past-duvall", Side.UNDER.name, -110, 26.5, 1.0, now - 21 * hour + 5 * min), 29.0, "Ansel Duvall"),
            Triple(PaperBetEntity("bet-pred-past-lindqvist-over", "pred-past-lindqvist", Side.OVER.name, -115, 17.0, 1.0, now - 21 * hour + 9 * min), 17.0, "Pia Lindqvist"),
        )
        val settlements = bets.mapIndexed { i, (b, actual, _) ->
            val outcome = Settlement.outcome(Side.valueOf(b.side), b.line, actual)
            SettlementEntity(b.id, actual, outcome.name, Settlement.profit(outcome, b.stakeUnits, b.quotedOdds),
                Settlement.RULE_VERSION, now - 17 * hour + i * min, "fixture")
        }
        val openBet = PaperBetEntity("bet-pred-okafor-over", "pred-okafor", Side.OVER.name, -120, 18.5, 1.0, now - 9 * min)
        for ((b, _, name) in bets) {
            events += AuditEventEntity(ts = b.decidedUtc, kind = "paper_bet_placed", subjectId = b.id, detail = "$name ${Side.valueOf(b.side).label} ${b.line}, ${"%.1f".format(b.stakeUnits)} units")
        }
        for ((s, b) in settlements.zip(bets.map { it.first })) {
            events += AuditEventEntity(ts = s.settledUtc, kind = "result_recorded", subjectId = b.id,
                detail = "Actual ${s.actual} against ${b.line}: ${s.outcome.lowercase()}, ${"%+.2f".format(s.profitUnits)} units")
        }
        events += AuditEventEntity(ts = openBet.decidedUtc, kind = "paper_bet_placed", subjectId = openBet.id, detail = "Remy Okafor Over 18.5, 1.0 units")

        db.withTransaction {
            db.slateDao().insertGames(games)
            db.slateDao().insertPlayers(players)
            db.slateDao().insertModels(listOf(ModelVersionEntity(model, "none", 1, "Fixture. Not validated.", now - 25 * hour, true)))
            db.slateDao().insertQuotes(quotes)
            db.slateDao().insertPredictions(preds)
            db.paperDao().insertBets(bets.map { it.first } + openBet)
            db.paperDao().insertSettlements(settlements)
            db.auditDao().insertAll(events)
        }
    }
}
