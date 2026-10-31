package com.propslab.feed

import androidx.room.withTransaction
import com.propslab.availability.AvailabilityModel
import com.propslab.availability.AvailabilityNote
import com.propslab.availability.InjuryNews
import com.propslab.availability.LeagueInjuries
import com.propslab.data.AuditEventEntity
import com.propslab.data.AvailabilitySignalEntity
import com.propslab.data.FIXTURE_SOURCE
import com.propslab.data.FeedStateEntity
import com.propslab.data.GameLineEntity
import com.propslab.data.SignalSource
import com.propslab.data.SlateRow
import com.propslab.data.GameEntity
import com.propslab.data.GAMELOG_MODEL
import com.propslab.data.KAGGLE_TABLE_MODEL
import com.propslab.data.LearningTrack
import com.propslab.data.ODDS_FEED_SOURCE
import com.propslab.data.OddsSnapshotEntity
import com.propslab.data.PlayerEntity
import com.propslab.data.PlayerGameStatEntity
import com.propslab.data.PlayerResultEntity
import com.propslab.data.PredictionEntity
import com.propslab.data.PropsDatabase
import com.propslab.data.Status
import com.propslab.data.UnsettledRow
import com.propslab.data.toFit
import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.PlattFit
import com.propslab.domain.PredictionGate
import com.propslab.domain.RejectReason
import com.propslab.domain.Sports
import com.propslab.inference.ChampionTable
import com.propslab.inference.ExplainInput
import com.propslab.inference.LocalAssistant
import com.propslab.inference.ScoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * A moneyline does not need refreshing every half hour, and each market read costs a
 * credit. Three hours keeps the straight markets current without spending the month's
 * allowance on them.
 */
private const val GAME_LINES_REFRESH_MS = 3L * 60L * 60L * 1000L

/**
 * The bridge between two providers that know nothing about each other.
 *
 * Results are read first, because a price cannot be priced without a game log: the box
 * scores rebuild the model's input, then the odds are read and scored against it. Every
 * row that reaches the board carries which book quoted it, when, and how many games the
 * estimate was built from.
 *
 * Nothing here places a wager. It reads prices and it reads box scores, and the only thing
 * it writes to the ledger is a result for a prop that has already finished.
 */
class FeedSync(
    private val db: PropsDatabase,
    private val settings: FeedSettings,
    private val assistant: LocalAssistant,
    private val fetcher: JsonFetcher = HttpUrlConnectionFetcher(),
    private val sportsDb: SportsDb = SportsDb(fetcher),
    private val news: InjuryNews = InjuryNews(fetcher),
    private val league: LeagueInjuries = LeagueInjuries(fetcher),
    private val champion: ChampionTable? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    data class Step(val ok: Boolean, val note: String, val count: Int) {
        companion object {
            fun skipped(note: String) = Step(false, note, 0)
        }
    }

    data class FeedReport(
        val schedule: Step,
        val props: Step,
        val results: Step,
        val signals: Step,
        val creditsRemaining: Int?,
        val quotes: Int,
        val scored: Int,
        val settled: Int,
    )

    private sealed interface DayResult {
        data class Ok(val prefix: String, val games: List<ResultGame>) : DayResult
        data class Failed(val failure: FeedFailure) : DayResult
    }

    /**
     * One turn of the feed: box scores, then prices, then whatever the two together can
     * settle. Safe to call from a worker, a button or the launch path: every write is
     * idempotent and the audit trail records what each half did.
     */
    suspend fun run(): FeedReport {
        val config = settings.load()
        val now = clock()
        // The schedule needs no key, so it runs first and runs either way: the games and
        // players are the tables every other feed joins a price onto.
        val schedule = syncSchedule(config, now)
        if (!config.anyReady) {
            val note = "No feed keys are saved. The board stays on whatever it already holds."
            val props = Step.skipped(note)
            val results = Step.skipped(note)
            writeState(props, results, Step.skipped(note), now, null, 0, 0, 0, null)
            return FeedReport(
                schedule = schedule,
                props = props,
                results = results,
                signals = Step.skipped(note),
                creditsRemaining = null,
                quotes = 0,
                scored = 0,
                settled = 0,
            )
        }

        val results = syncResults(config, now)
        // Signals sit between the two: they need the board to know which players to check,
        // and the prices want them before anything is scored.
        val signalScan = syncSignals(config, now)
        // Freshness is measured on when the box scores were last read, not on when this
        // player last played: the risk being guarded against is a game that happened and
        // was never ingested.
        val prior = db.feedDao().state(FeedStateEntity.ID)
        val featureCutoff = if (results.step.ok) now else (prior?.lastResultsSyncUtc ?: 0L)
        val props = syncProps(config, now, featureCutoff, signalScan.notes)
        writeState(
            props = props.step,
            results = results.step,
            signals = signalScan.step,
            now = now,
            credits = props.credits,
            quotes = props.quotes,
            scored = props.scored,
            settled = results.settled,
            fixturesRetired = props.retired,
        )
        return FeedReport(
            schedule = schedule,
            props = props.step,
            results = results.step,
            signals = signalScan.step,
            creditsRemaining = props.credits ?: db.feedDao().state(FeedStateEntity.ID)?.creditsRemaining,
            quotes = props.quotes,
            scored = props.scored,
            settled = results.settled,
        )
    }

    // --------------------------------------------------------------- the schedule

    /**
     * Games and rosters: the two tables every other feed joins onto.
     *
     * This step takes no key — TheSportsDB's free tier is open — so it runs before the
     * keyed steps and even when there are none, which is the difference between a board
     * that is waiting for a key and a board with nothing to attach a price to.
     *
     * The free tier answers a fraction of what it holds, and each read is a counted call,
     * so the reads are staged and stop as soon as the window has games in it:
     *
     *  1. today and tomorrow, which is the board's own window;
     *  2. the league's season page, because on 2026-10-29 both day queries came back with
     *     no NBA game at all while that page had them;
     *  3. the per-team schedule, because that same day the season page held only early-
     *     October games while four `eventsnext` probes found the games actually tonight.
     *
     * Step 3 needs team ids, so every NBA event read on the way — even one outside the
     * window — seeds the candidate pool. Writes are insert-if-absent: a game the odds feed
     * already created keeps its own id, and a roster row never overwrites a player the box
     * scores filed.
     */
    private suspend fun syncSchedule(config: FeedConfig, now: Long): Step {
        val today = NbaCalendar.gameDate(now)
        val from = now - 24 * PredictionGate.HOUR_MS
        val to = now + config.lookaheadHours * PredictionGate.HOUR_MS
        val found = LinkedHashMap<String, SportsDbEvent>()
        val candidates = LinkedHashSet<String>()
        var failure: String? = null
        val pauseMs = 1_000L
        val nextTeams = 4
        val rosterTeams = 4

        fun take(result: Result<List<SportsDbEvent>>, seedTeams: Boolean) = result.fold(
            onSuccess = { list ->
                val nba = SportsDb.nba(list)
                if (seedTeams) nba.forEach { event ->
                    if (event.homeId.isNotBlank()) candidates += event.homeId
                    if (event.awayId.isNotBlank()) candidates += event.awayId
                }
                SportsDb.between(nba, from, to).forEach { found[it.id] = it }
            },
            onFailure = { if (failure == null) failure = it.message ?: "TheSportsDB did not answer." },
        )

        take(sportsDb.day(today), seedTeams = true)
        take(sportsDb.day(today.plusDays(1)), seedTeams = true)
        if (found.isEmpty()) {
            take(sportsDb.season(SportsDb.NBA_LEAGUE_ID, SportsDb.seasonFor(today)), seedTeams = true)
        }

        var probes = 0
        if (found.isEmpty()) {
            for (teamId in candidates) {
                if (probes >= nextTeams) break
                if (probes > 0) delay(pauseMs)
                take(sportsDb.next(teamId), seedTeams = false)
                probes++
            }
        }

        if (found.isEmpty()) {
            val note = failure ?: if (probes > 0) {
                "No NBA game in the next ${config.lookaheadHours} hours among the $probes team schedule(s) checked."
            } else {
                "No NBA game starts in the next ${config.lookaheadHours} hours."
            }
            return Step(false, note, 0)
        }

        val teamIds = found.values
            .flatMap { listOf(it.homeId, it.awayId) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(rosterTeams)
        val roster = LinkedHashMap<String, SportsDbPlayer>()
        var rosterFailure: String? = null
        for ((index, teamId) in teamIds.withIndex()) {
            if (index > 0) delay(pauseMs)
            sportsDb.players(teamId).fold(
                onSuccess = { rows -> rows.forEach { roster[Names.playerId(it.name)] = it } },
                onFailure = { if (rosterFailure == null) rosterFailure = it.message ?: "A roster did not answer." },
            )
        }

        val games = found.values.map { event ->
            GameEntity(
                id = "tsdb:${event.id}",
                startUtc = event.utcMillis,
                homeCode = Teams.abbreviation(event.homeTeam),
                awayCode = Teams.abbreviation(event.awayTeam),
                status = if (event.finished) "FINAL" else "SCHEDULED",
                externalId = null,
                homeName = event.homeTeam,
                awayName = event.awayTeam,
            )
        }
        val players = roster.values.map {
            PlayerEntity(Names.playerId(it.name), it.name, Teams.abbreviation(it.teamName))
        }
        db.withTransaction {
            db.slateDao().insertGames(games)
            db.slateDao().insertPlayers(players)
        }
        val note = buildString {
            append("${games.size} NBA game(s) and ${players.size} player(s) from TheSportsDB")
            append(", ${teamIds.size} roster(s) read.")
            rosterFailure?.let { append(" $it") }
        }
        db.auditDao().insert(
            AuditEventEntity(ts = now, kind = "schedule_populated", subjectId = "sportsdb", detail = note),
        )
        return Step(true, note, games.size)
    }

    // ------------------------------------------------------------------ box scores

    private data class ResultsStep(
        val step: Step,
        val settled: Int,
        val gamesRead: Int,
    )

    private suspend fun syncResults(config: FeedConfig, now: Long): ResultsStep {
        if (!config.resultsReady) return ResultsStep(Step.skipped("No results key is saved."), 0, 0)
        if (config.resultsProvider == ResultsProvider.ESPN) return syncEspnResults(config, now)

        val days = (0 until config.resultsLookbackDays.coerceIn(1, 5))
            .map { NbaCalendar.gameDate(now).minusDays(it.toLong()) }
        val gamesByDate = LinkedHashMap<LocalDate, List<ResultGame>>()
        var prefix = settings.resultsPrefix()
        var failure: FeedFailure? = null

        for (day in days) {
            when (val loaded = loadDay(day)) {
                is DayResult.Ok -> {
                    gamesByDate[day] = loaded.games
                    prefix = loaded.prefix
                }
                is DayResult.Failed -> {
                    failure = loaded.failure
                    break
                }
            }
        }
        if (gamesByDate.isEmpty()) {
            return ResultsStep(Step(false, failure?.message ?: "The results provider returned nothing.", 0), 0, 0)
        }

        val finals = gamesByDate.values.flatten()
            .filter { it.isFinal }
            .distinctBy { it.id }
            .take(config.maxResultGames)
        attachFinals(finals, now)
        var statsWritten = 0
        for (game in finals) {
            val response = fetcher.safeGet(ResultsApi.statsUrl(prefix, game.id)).getOrNull()
            if (response == null) {
                failure = FeedFailure(0, "The network request failed while reading a box score.")
                continue
            }
            if (!response.ok) {
                failure = FeedFailure.of(response, "The results provider")
                continue
            }
            val rows = ResultsApi.parseStats(response.body, now).map { it.toEntity(now) }
            statsWritten += db.feedDao().insertStats(rows).count { it != -1L }
        }

        val settled = settleOutstanding(gamesByDate, now)
        val step = when {
            failure != null && statsWritten == 0 && settled == 0 -> Step(false, failure.message, 0)
            failure != null -> Step(true, "${failure.message} $statsWritten box score lines were still read.", statsWritten)
            else -> Step(true, "${finals.size} finished games read, $statsWritten lines stored, $settled props settled.", statsWritten)
        }
        db.auditDao().insert(
            AuditEventEntity(
                ts = now,
                kind = "feed_results",
                subjectId = "box scores",
                detail = step.note,
            ),
        )
        return ResultsStep(step, settled, finals.size)
    }

    /**
     * ESPN's read-only box scores: a scoreboard call per day, then one summary per finished
     * game. No key, so this is the lane that makes a fresh install priceable immediately.
     */
    private suspend fun syncEspnResults(config: FeedConfig, now: Long): ResultsStep {
        // ESPN is free, so the window can be wider than the keyed provider's. Each league
        // picks its own lookback inside the loop: basketball is nightly, football weekly.
        val gamesByDate = LinkedHashMap<LocalDate, List<ResultGame>>()
        val finalsBySport = LinkedHashMap<String, List<ResultGame>>()
        var failure: String? = null

        for (sport in Sports.ALL) {
            val read = mutableListOf<ResultGame>()
            // Football plays once a week, so a two-day window is the one night a week the
            // results would be in it. The football lane looks back far enough to cover a
            // full slate; the endpoint is keyless, so the wider window costs requests
            // rather than credits. Already-read summaries are skipped below.
            val lookback = if (sport == Sports.FOOTBALL) {
                config.resultsLookbackDays.coerceIn(1, 10).coerceAtLeast(8)
            } else {
                config.resultsLookbackDays.coerceIn(1, 10)
            }
            val sportDays = (0 until lookback).map { NbaCalendar.gameDate(now).minusDays(it.toLong()) }
            // The football and basketball scoreboards are different endpoints with the same
            // shape, so one loop reads both and each result keeps the league that produced it.
            for (day in sportDays) {
                val response = fetcher.safeGet(EspnApi.scoreboardUrl(day.toString(), sport)).getOrNull()
                if (response == null) {
                    failure = "The network request failed while reading ${Sports.label(sport)} box scores."
                    continue
                }
                if (!response.ok) {
                    failure = "ESPN returned HTTP ${response.status} while reading ${Sports.label(sport)} ${day}."
                    continue
                }
                val games = EspnApi.parseGames(response.body, day.toString(), sport)
                gamesByDate[day] = (gamesByDate[day].orEmpty() + games).distinctBy { it.id }
                read += games
            }
            finalsBySport[sport] = read
        }
        if (gamesByDate.isEmpty()) {
            return ResultsStep(Step(false, failure ?: "ESPN returned no games.", 0), 0, 0)
        }

        val finals = finalsBySport.flatMap { (sport, list) ->
            list.filter { it.isFinal }.distinctBy { it.id }.take(config.maxResultGames)
                .map { sport to it }
        }
        attachFinals(finals.map { it.second }, now)
        var statsWritten = 0
        for ((sport, game) in finals) {
            // A game already read contributes its history to every later sync; re-downloading
            // it would spend a minute of requests to write the same rows again.
            if (db.feedDao().statsForGame(game.id) > 0) continue
            val response = fetcher.safeGet(EspnApi.summaryUrl(game.id, sport)).getOrNull()
            if (response == null) {
                failure = "The network request failed while reading a box score."
                continue
            }
            if (!response.ok) {
                failure = "ESPN returned HTTP ${response.status} for one box score."
                continue
            }
            val rows = if (sport == Sports.FOOTBALL) {
                EspnApi.parseFootballBoxScore(response.body, game.id, now)
            } else {
                EspnApi.parseBoxScore(response.body, game.id, now)
            }.map { it.toEntity(now) }
            statsWritten += db.feedDao().insertStats(rows).count { it != -1L }
        }

        val settled = settleOutstanding(gamesByDate, now)
        val leagues = finals.map { Sports.label(it.first) }.distinct().joinToString(" and ")
        val step = when {
            failure != null && statsWritten == 0 && settled == 0 -> Step(false, failure, 0)
            failure != null -> Step(true, "$failure $statsWritten box score lines were still read.", statsWritten)
            finals.isEmpty() -> Step(true, "No finished game in the lookback window.", 0)
            else -> Step(true, "${finals.size} finished $leagues game(s) read, $statsWritten lines stored, $settled props settled.", statsWritten)
        }
        db.auditDao().insert(
            AuditEventEntity(ts = now, kind = "feed_results", subjectId = "espn", detail = step.note),
        )
        return ResultsStep(step, settled, finals.size)
    }

    /**
     * Joins every finished game to its board row, not only the games that happen to settle a
     * prop. The game model and the learning ledger both need the final score of a game whose
     * props nobody bet, and the box score is keyed by the provider's id.
     */
    private suspend fun attachFinals(finals: List<ResultGame>, now: Long) {
        if (finals.isEmpty()) return
        val games = db.feedDao().gamesBetween(now - 10 * 86_400_000L, now + 86_400_000L)
        for (game in games) {
            if (!game.externalId.isNullOrBlank()) continue
            if (game.homeName.isBlank() || game.awayName.isBlank()) continue
            // A city can field two clubs with the same abbreviation (CHI is both the Bulls
            // and the Bears), so a final is only matched inside its own league.
            val match = finals.firstOrNull {
                it.sport == game.sport &&
                    Teams.sameTeam(it.homeName, game.homeName) &&
                    Teams.sameTeam(it.awayName, game.awayName)
            } ?: continue
            db.feedDao().attachResultGame(game.id, match.id, "FINAL")
        }
    }

    private suspend fun loadDay(day: LocalDate): DayResult {
        val preferred = settings.resultsPrefix()
        val order = listOf(preferred) + ResultsApi.PREFIXES.filter { it != preferred }
        var lastFailure: FeedFailure? = null
        for (prefix in order) {
            val response = fetcher.safeGet(ResultsApi.gamesUrl(prefix, day.toString())).getOrNull()
            if (response == null) {
                lastFailure = FeedFailure(0, "The network request failed.")
                continue
            }
            // The provider's docs and its OpenAPI spec disagree about the prefix, so a 404
            // is treated as "wrong path" and the other one is tried before giving up.
            if (response.status == 404) {
                lastFailure = FeedFailure.of(response, "The results provider")
                continue
            }
            if (!response.ok) return DayResult.Failed(FeedFailure.of(response, "The results provider"))
            settings.rememberResultsPrefix(prefix)
            return DayResult.Ok(prefix, ResultsApi.parseGames(response.body))
        }
        return DayResult.Failed(lastFailure ?: FeedFailure(0, "The results provider could not be reached."))
    }

    /**
     * Writes a result against every open prediction whose game has a box score.
     *
     * A prediction settles once. The row records the stat line it settled from, so a later
     * re-read can never change a result that has already been used to move a bankroll.
     */
    private suspend fun settleOutstanding(
        gamesByDate: Map<LocalDate, List<ResultGame>>,
        now: Long,
    ): Int {
        // Only games recent enough that a box score would still be on the provider's radar.
        // Eight days covers a weekly football slate as well as a nightly basketball one, so
        // a phone that was off over the weekend still settles its open rows.
        val open = db.feedDao().unsettled(nowUtc = now, sinceUtc = now - 8 * 86_400_000L)
        if (open.isEmpty()) return 0
        val rows = mutableListOf<PlayerResultEntity>()
        val attach = mutableListOf<Pair<String, String>>()
        for (row in open) {
            var externalId = row.externalId
            if (externalId == null) {
                val matched = matchGame(row, gamesByDate)?.takeIf { it.isFinal } ?: continue
                externalId = matched.id
                attach += row.gameId to matched.id
            }
            val stat = db.feedDao().statFor(Names.key(row.playerName), externalId) ?: continue
            val actual = GameLogModel.anyStatValue(
                points = stat.points,
                rebounds = stat.rebounds,
                assists = stat.assists,
                threes = stat.threes,
                passYards = stat.passYards,
                rushYards = stat.rushYards,
                receivingYards = stat.receivingYards,
                receptions = stat.receptions,
                market = row.market,
            ) ?: continue
            rows += PlayerResultEntity(
                predictionId = row.predictionId,
                market = row.market,
                actual = actual,
                statId = stat.id,
                recordedUtc = now,
            )
        }
        if (rows.isEmpty()) return 0
        return db.withTransaction {
            for ((gameId, id) in attach.distinct()) db.feedDao().attachResultGame(gameId, id, "FINAL")
            db.feedDao().insertResults(rows).count { it != -1L }
        }
    }

    private fun matchGame(row: UnsettledRow, gamesByDate: Map<LocalDate, List<ResultGame>>): ResultGame? {
        row.externalId?.let { id ->
            gamesByDate.values.flatten().firstOrNull { it.id == id }?.let { return it }
        }
        val day = NbaCalendar.gameDate(row.startUtc)
        val candidates = gamesByDate[day] ?: gamesByDate[day.plusDays(1)] ?: return null
        return candidates.firstOrNull { game ->
            game.sport == row.sport &&
                Teams.sameTeam(game.homeName, row.homeName) &&
                Teams.sameTeam(game.awayName, row.awayName)
        }
    }

    // --------------------------------------------------------------- late availability

    private data class SignalScan(val step: Step, val notes: Map<String, AvailabilityNote>)

    /**
     * Checks the freshest news for the players actually on the board.
     *
     * Only a headline that names the player and carries an explicit status phrase becomes a
     * signal. A news index is a hint channel — it is frequently minutes behind a team's own
     * report and sometimes simply wrong — so nothing weaker than a stated status is allowed
     * to move a number, and the screen always shows which signal did.
     */
    private suspend fun syncSignals(config: FeedConfig, now: Long): SignalScan {
        db.signalDao().purge(now)
        val board = db.slateDao().observeSlate(now - 12 * PredictionGate.HOUR_MS).first()
        val onBoard = board.filter { it.source == ODDS_FEED_SOURCE && it.status == Status.AVAILABLE }
        if (onBoard.isEmpty()) {
            return SignalScan(Step.skipped("No players on the board to check for news."), activeNotes(now))
        }

        // The league's own report first. It needs no key, and it is the designation every
        // headline ends up quoting, so it is the one worth reading before tip-off.
        var found = 0
        val notes = mutableListOf<String>()
        var leagueRead = false
        league.fetch(now).fold(
            onSuccess = { rows ->
                leagueRead = true
                val written = recordLeague(rows, onBoard, now)
                found += written
                notes += if (written > 0) "league report: $written signal(s)" else "league report: nothing for this board"
            },
            onFailure = { failure -> notes += (failure.message ?: "the league report could not be read") },
        )

        if (!config.newsReady) {
            val note = notes.joinToString(" · ") +
                ". No news key is saved, so the league report and signals entered by hand are what apply."
            return SignalScan(Step(leagueRead || found > 0, note, found), activeNotes(now))
        }

        val players = onBoard.map { it.playerName }.distinct().take(config.newsMaxPlayers.coerceIn(1, 12))
        var failure: String? = null
        for (name in players) {
            val hits = news.search(config.newsKey, name).getOrElse {
                failure = it.message
                emptyList()
            }
            if (hits.isEmpty()) continue
            val read = news.latestMention(
                hits, name, now,
                config.newsLookbackHours.coerceIn(1, 48) * PredictionGate.HOUR_MS,
            ) ?: continue
            val key = Names.key(name)
            val observed = if (read.hit.publishedUtc > 0L) read.hit.publishedUtc else now
            val detail = listOfNotNull(
                read.mention.area?.let { "reported: $it" },
                "matched \"${read.mention.phrase}\"",
            ).joinToString(" · ")
            db.signalDao().upsert(
                listOf(
                    AvailabilitySignalEntity(
                        id = signalId(key, read.mention.status, now),
                        playerKey = key,
                        playerName = name,
                        status = read.mention.status,
                        detail = detail,
                        source = SignalSource.NEWS,
                        headline = read.hit.title,
                        url = read.hit.url,
                        observedUtc = observed,
                        expiresUtc = now + AvailabilityModel.TTL_MS,
                    ),
                ),
            )
            found++
            db.auditDao().insert(
                AuditEventEntity(
                    ts = now,
                    kind = "signal_recorded",
                    subjectId = name,
                    detail = "read as ${read.mention.status.lowercase().replace('_', ' ')} from a news report: ${read.hit.title.take(160)}",
                ),
            )
        }
        failure?.let { notes += it }
        val problem = failure
        val step = if (problem != null && found == 0) {
            Step(false, notes.joinToString(" · "), 0)
        } else {
            Step(
                true,
                "checked ${players.size} players for late news, $found signal(s) recorded · " +
                    notes.joinToString(" · "),
                found,
            )
        }
        return SignalScan(step, activeNotes(now))
    }

    /**
     * Turns the league report into signals for the players actually on the board.
     *
     * A designation about somebody with no prop today changes no number, so it is not
     * recorded: the signal table is meant to be short enough that every row has an effect.
     */
    private suspend fun recordLeague(rows: List<LeagueInjuries.Row>, onBoard: List<SlateRow>, now: Long): Int {
        if (rows.isEmpty()) return 0
        val watched = onBoard.associateBy { Names.key(it.playerName) }
        val matched = rows.mapNotNull { row ->
            val slateRow = watched[Names.key(row.playerName)] ?: return@mapNotNull null
            row to slateRow.playerName
        }
        if (matched.isEmpty()) return 0
        db.signalDao().upsert(
            matched.map { (row, name) ->
                AvailabilitySignalEntity(
                    id = signalId(Names.key(name), row.status, now),
                    playerKey = Names.key(name),
                    playerName = name,
                    status = row.status,
                    detail = buildString {
                        if (row.team.isNotBlank()) append(row.team).append(" · ")
                        append(row.note.take(160).ifBlank { "listed on the league injury report" })
                    },
                    source = SignalSource.LEAGUE,
                    headline = "League injury report",
                    url = LeagueInjuries.URL,
                    observedUtc = row.observedUtc,
                    expiresUtc = now + AvailabilityModel.TTL_MS,
                )
            },
        )
        matched.forEach { (row, name) ->
            db.auditDao().insert(
                AuditEventEntity(
                    ts = now,
                    kind = "signal_recorded",
                    subjectId = name,
                    detail = "listed ${row.status.lowercase().replace('_', ' ')} on the league injury report",
                ),
            )
        }
        return matched.size
    }

    /** The newest live signal per player. Older entries are history, not state. */
    private suspend fun activeNotes(now: Long): Map<String, AvailabilityNote> =
        db.signalDao().active(now)
            .map { AvailabilityNote.from(it) }
            .sortedByDescending { it.observedUtc }
            .distinctBy { it.playerKey }
            .associateBy { it.playerKey }

    private fun signalId(playerKey: String, status: String, now: Long): String =
        "sig-" + Integer.toHexString("$playerKey|$status|${now / PredictionGate.HOUR_MS}".hashCode())

    // ------------------------------------------------------------------- the prices

    private data class PropsStep(
        val step: Step,
        val credits: Int?,
        val quotes: Int,
        val scored: Int,
        /** True on the turn that retired the synthetic board. */
        val retired: Boolean? = null,
    )

    /** One league's read: which markets to ask for, and how many games may be paid for. */
    private data class SportPlan(
        val sport: String,
        val markets: List<String>,
        val maxEvents: Int,
        val withLines: Boolean,
    )

    private fun apiSport(sport: String): String =
        if (sport == Sports.FOOTBALL) OddsApi.SPORT_NFL else OddsApi.SPORT_NBA

    private suspend fun syncProps(
        config: FeedConfig,
        now: Long,
        featureCutoff: Long,
        notes: Map<String, AvailabilityNote>,
    ): PropsStep {
        if (!config.oddsReady) return PropsStep(Step.skipped("No odds key is saved."), null, 0, 0)

        val horizon = now + config.lookaheadHours.coerceIn(1, 72) * PredictionGate.HOUR_MS
        // Game markets cost one credit each per game, so they refresh on their own slower
        // clock rather than on every half-hour pass over the props.
        val readGameLines = config.gameLines && now - settings.gameLinesUtc() >= GAME_LINES_REFRESH_MS
        // Football is capped lower on purpose: each event read costs one credit per market,
        // so a full NFL slate plus a full NBA slate would spend the month's allowance in a
        // day. Four football games is enough to know the lane works.
        val plans = listOf(
            SportPlan(Sports.BASKETBALL, config.pricedMarkets(), config.maxEvents.coerceIn(1, 20), readGameLines),
            SportPlan(Sports.FOOTBALL, Markets.FOOTBALL.toList(), config.maxEvents.coerceIn(1, 4), false),
        )

        val events = mutableListOf<OddsEvent>()
        val quotes = mutableListOf<RawQuote>()
        val lines = mutableListOf<RawGameLine>()
        val notesRead = mutableListOf<String>()
        val quotedSports = LinkedHashSet<String>()
        var credits: Int? = null
        var failure: String? = null

        for (plan in plans) {
            val eventsResponse = fetcher.safeGet(OddsApi.eventsUrl(config.oddsKey, apiSport(plan.sport))).getOrNull()
            if (eventsResponse == null) {
                failure = "The network request failed."
                continue
            }
            eventsResponse.header("x-requests-remaining")?.toIntOrNull()?.let { credits = it }
            if (!eventsResponse.ok) {
                failure = FeedFailure.of(eventsResponse, "The odds provider").message
                notesRead += "${Sports.label(plan.sport)}: ${failure}"
                continue
            }
            val found = OddsApi.parseEvents(eventsResponse.body)
                .filter { it.commenceUtc > now && it.commenceUtc <= horizon }
                .sortedBy { it.commenceUtc }
                .take(plan.maxEvents)
            if (found.isEmpty()) {
                notesRead += "no ${Sports.label(plan.sport)} game in the next ${config.lookaheadHours} hours"
                continue
            }
            events += found
            val wanted = if (plan.withLines) plan.markets + GameMarkets.ALL else plan.markets
            for (event in found) {
                val url = OddsApi.eventOddsUrl(
                    eventId = event.id,
                    apiKey = config.oddsKey,
                    markets = wanted,
                    bookmakers = config.quotedBookmakers(),
                    sport = apiSport(plan.sport),
                )
                val response = fetcher.safeGet(url).getOrNull()
                if (response == null) {
                    failure = "The network request failed while reading prices."
                    continue
                }
                response.header("x-requests-remaining")?.toIntOrNull()?.let { credits = it }
                if (!response.ok) {
                    failure = FeedFailure.of(response, "The odds provider").message
                    continue
                }
                val read = OddsApi.parseEventOdds(response.body, now)
                if (read.isNotEmpty()) quotedSports += plan.sport
                quotes += read
                if (plan.withLines) lines += OddsApi.parseGameLines(response.body, now)
            }
        }

        val combinedNote = notesRead.joinToString("; ")
        if (quotes.isEmpty()) {
            val note = failure ?: combinedNote.ifBlank { "No two-way prop prices came back from the selected books." }
            db.auditDao().insert(AuditEventEntity(ts = now, kind = "feed_props", subjectId = "odds", detail = note))
            return PropsStep(Step(false, note, 0), credits, 0, 0)
        }

        val board = FeedBoard.bestPrices(quotes)
        // Stored predictions are raw model output. The learned correction is applied where the
        // board is read, so the learning ledger always fits on raw probabilities and a refit
        // never compounds an earlier correction into its own training data.
        val rows = withContext(Dispatchers.Default) {
            buildBoard(events, board, featureCutoff, now, notes)
        }

        var retired: Boolean? = null
        db.withTransaction {
            db.slateDao().insertGames(rows.games)
            db.slateDao().insertPlayers(rows.players)
            db.feedDao().upsertQuotes(rows.quotes)
            db.feedDao().upsertPredictions(rows.predictions)
            // Retire the invented rows of exactly the leagues that just got a real price.
            for (sport in quotedSports) {
                if (db.feedDao().countQuotesForSourceInSport(FIXTURE_SOURCE, sport) == 0) continue
                db.feedDao().deleteSettlementsForSourceInSport(FIXTURE_SOURCE, sport)
                db.feedDao().deleteBetsForSourceInSport(FIXTURE_SOURCE, sport)
                db.feedDao().deletePredictionsForSourceInSport(FIXTURE_SOURCE, sport)
                db.feedDao().deleteQuotesForSourceInSport(FIXTURE_SOURCE, sport)
                db.feedDao().deleteOrphanFixturePlayers()
                db.feedDao().deleteFixtureGamesForSport(sport)
                retired = true
            }
            // Heals an install that retired its fixtures before the bets were cleaned up.
            if (readGameLines && lines.isNotEmpty()) {
                db.feedDao().clearGameLines()
                db.feedDao().upsertGameLines(
                    lines.map { l ->
                        val key = "${l.eventId}|${l.market}|${l.side}|${l.line}|${l.bookKey}"
                        GameLineEntity(
                            id = "gl-" + Integer.toHexString(key.hashCode()),
                            gameId = "oa:${l.eventId}",
                            book = l.bookTitle,
                            market = l.market,
                            side = l.side,
                            line = l.line,
                            price = l.price,
                            quoteTimeUtc = l.updatedUtc,
                        )
                    },
                )
                settings.rememberGameLines(now)
            }
            // Heals an install that retired its fixtures before the bets were cleaned up.
            val orphans = db.feedDao().countOrphanBets()
            android.util.Log.i("Cheat Code", "feed sync: $orphans orphaned paper bets")
            if (orphans > 0) {
                db.feedDao().deleteOrphanSettlements()
                val dropped = db.feedDao().deleteOrphanBets()
                android.util.Log.i("Cheat Code", "feed sync: dropped $dropped orphaned paper bets")
            }
        }

        val note = buildString {
            append("${rows.quotes.size} two-way prices from ${Bookmakers.titles(config.quotedBookmakers())} ")
            append("across ${events.size} ${quotedSports.joinToString(" and ") { Sports.label(it) }} games, ")
            append("${rows.scored} priced from box scores.")
            if (readGameLines && lines.isNotEmpty()) {
                append(" Moneyline, spread and total refreshed from ${lines.map { it.bookTitle }.distinct().size} book(s).")
            }
            if (combinedNote.isNotBlank()) append(" $combinedNote.")
            failure?.let { if (!combinedNote.contains(it)) append(" $it") }
            if (rows.thin > 0) append(" ${rows.thin} props used fewer than ${GameLogModel.MIN_GAMES} games, so their estimates are pulled toward 50%.")
        }
        db.auditDao().insert(AuditEventEntity(ts = now, kind = "feed_props", subjectId = "odds", detail = note))
        return PropsStep(Step(true, note, rows.quotes.size), credits, rows.quotes.size, rows.scored, retired)
    }

    private class BoardRows(
        val games: List<GameEntity>,
        val players: List<PlayerEntity>,
        val quotes: List<OddsSnapshotEntity>,
        val predictions: List<PredictionEntity>,
        val scored: Int,
        val thin: Int,
    )

    private suspend fun buildBoard(
        events: List<OddsEvent>,
        board: List<RawQuote>,
        featureCutoff: Long,
        now: Long,
        notes: Map<String, AvailabilityNote>,
    ): BoardRows {
        val eventById = events.associateBy { it.id }
        val games = LinkedHashMap<String, GameEntity>()
        val players = LinkedHashMap<String, PlayerEntity>()
        val quotes = mutableListOf<OddsSnapshotEntity>()
        val predictions = mutableListOf<PredictionEntity>()
        val historyCache = HashMap<String, List<PlayerGameStatEntity>>()
        var scored = 0
        var thin = 0

        for (quote in board) {
            val event = eventById[quote.eventId] ?: continue
            val sport = Sports.ofMarket(quote.market)
            val gameId = "oa:${event.id}"
            games.getOrPut(gameId) {
                GameEntity(
                    id = gameId,
                    startUtc = event.commenceUtc,
                    homeCode = Teams.abbreviation(event.homeTeam),
                    awayCode = Teams.abbreviation(event.awayTeam),
                    status = "SCHEDULED",
                    externalId = null,
                    homeName = event.homeTeam,
                    awayName = event.awayTeam,
                    sport = sport,
                )
            }
            val playerId = Names.playerId(quote.playerName)
            // A player key is unique to a person, but the two leagues never share one; the
            // cache is split by sport anyway so a stray football row can never be read as
            // basketball history.
            val history = historyCache.getOrPut("${quote.playerKey}|$sport") {
                db.feedDao().recentStats(quote.playerKey, GameLogModel.WINDOW)
                    .filter { it.sport == sport }
            }
            players.getOrPut(playerId) {
                PlayerEntity(playerId, quote.playerName, history.firstOrNull { it.teamAbbr.isNotBlank() }?.teamAbbr.orEmpty())
            }

            val values = history.mapNotNull {
                GameLogModel.anyStatValue(
                    points = it.points,
                    rebounds = it.rebounds,
                    assists = it.assists,
                    threes = it.threes,
                    passYards = it.passYards,
                    rushYards = it.rushYards,
                    receivingYards = it.receivingYards,
                    receptions = it.receptions,
                    market = quote.market,
                )
            }
            val note = notes[quote.playerKey]
            val liveNote = note?.takeIf { it.fresh(now) }
            val availability = liveNote?.let { AvailabilityModel.applied(it.status) } ?: 1.0
            val empirical = champion != null && quote.market == Markets.POINTS
            val score = GameLogModel.score(
                values = values,
                line = quote.line,
                availability = availability,
                market = quote.market,
                table = champion,
            )

            val reasons = PredictionGate.evaluate(
                line = quote.line,
                quoteTimeUtc = quote.updatedUtc,
                featureCutoffUtc = featureCutoff,
                nowUtc = now,
                playerMatched = history.isNotEmpty(),
                market = quote.market,
            ).toMutableList()
            if (values.size < GameLogModel.THIN_MIN_GAMES) reasons += RejectReason.INSUFFICIENT_SAMPLE
            if (liveNote?.isOut == true) reasons += RejectReason.OUT_SIGNAL

            val probs = (score as? ScoreResult.Scored)?.probabilities?.takeIf { reasons.isEmpty() }
            val flags = buildList {
                if (empirical) {
                    add("Priced from the Kaggle empirical points table (594k player-games) against a ${values.size}-game trailing mean")
                } else {
                    add("Priced from ${values.size} logged games, a normal approximation of this player's own results")
                }
                if (values.size < GameLogModel.MIN_GAMES) {
                    add("Thin sample: ${values.size} games, so the estimate is pulled part-way to 50%")
                }
                if (quote.line % 1.0 == 0.0) add("Integer line: the push mass is modelled, not assumed away")
                if (featureCutoff <= 0L) add("No box scores have been synced yet, so this estimate cannot be trusted")
                if (note != null) add(AvailabilityModel.flag(note, stale = liveNote == null))
            }
            val (mean, spread) = GameLogModel.meanAndSpread(values)
            val drivers = if (values.isEmpty()) "" else buildString {
                append("games=${values.size};mean=").append(one(mean)).append(";spread=").append(one(spread))
            }
            val explanation = if (probs != null) {
                runCatching {
                    assistant.explain(
                        ExplainInput(
                            player = quote.playerName,
                            line = quote.line,
                            probabilities = probs,
                            marketOver = Odds.noVigOver(quote.overPrice, quote.underPrice),
                            flags = flags,
                        ),
                    )
                }.getOrDefault("")
            } else ""

            val key = "${quote.eventId}|${quote.playerKey}|${quote.market}|${quote.line}"
            val hash = Integer.toHexString(key.hashCode())
            quotes += OddsSnapshotEntity(
                id = "q-oa-$hash",
                gameId = gameId,
                playerId = playerId,
                market = quote.market,
                book = quote.bookTitle,
                line = quote.line,
                overOdds = quote.overPrice,
                underOdds = quote.underPrice,
                quoteTimeUtc = quote.updatedUtc,
                source = ODDS_FEED_SOURCE,
            )
            predictions += PredictionEntity(
                id = "pred-oa-$hash",
                quoteId = "q-oa-$hash",
                gameId = gameId,
                playerId = playerId,
                market = quote.market,
                modelVersion = if (empirical) KAGGLE_TABLE_MODEL else GAMELOG_MODEL,
                featureSchemaVersion = 1,
                featureCutoffUtc = featureCutoff,
                pOver = probs?.over,
                pPush = probs?.push,
                pUnder = probs?.under,
                status = if (probs != null) Status.AVAILABLE else Status.UNAVAILABLE,
                unavailableReasons = reasons.joinToString(",") { it.name },
                qualityFlags = flags.joinToString("|"),
                drivers = drivers,
                explanation = explanation,
                createdUtc = now,
            )
            if (probs != null) scored++ else if (values.size < GameLogModel.MIN_GAMES) thin++
        }
        return BoardRows(games.values.toList(), players.values.toList(), quotes, predictions, scored, thin)
    }

    // ----------------------------------------------------------------------- state

    private suspend fun writeState(
        props: Step,
        results: Step,
        signals: Step,
        now: Long,
        credits: Int?,
        quotes: Int,
        scored: Int,
        settled: Int,
        fixturesRetired: Boolean?,
    ) {
        val previous = db.feedDao().state(FeedStateEntity.ID) ?: FeedStateEntity.EMPTY
        db.feedDao().upsertState(
            FeedStateEntity(
                id = FeedStateEntity.ID,
                lastPropsSyncUtc = if (props.ok) now else previous.lastPropsSyncUtc,
                lastResultsSyncUtc = if (results.ok) now else previous.lastResultsSyncUtc,
                propsNote = props.note,
                resultNote = results.note,
                signalNote = signals.note,
                creditsRemaining = credits ?: previous.creditsRemaining,
                quotesWritten = if (props.ok) quotes else previous.quotesWritten,
                predictionsScored = if (props.ok) scored else previous.predictionsScored,
                resultsRecorded = if (results.ok) settled else previous.resultsRecorded,
                fixturesRetired = fixturesRetired ?: previous.fixturesRetired,
            ),
        )
    }

    /** Provenance of the Kaggle points artifact, or null when the asset did not load. */
    fun championProvenance(): String? = champion?.provenance

    private companion object {
        fun one(v: Double): String = String.format(java.util.Locale.ROOT, "%.1f", v)
    }
}

private fun BoxScore.toEntity(now: Long) = PlayerGameStatEntity(
    id = statId,
    eventId = gameId,
    playedUtc = playedUtc,
    playerKey = playerKey,
    playerName = playerName,
    teamAbbr = teamAbbr,
    points = points,
    rebounds = rebounds,
    assists = assists,
    threes = threes,
    minutes = minutes,
    fetchedUtc = now,
    passYards = passYards,
    rushYards = rushYards,
    receivingYards = receivingYards,
    receptions = receptions,
    sport = sport,
)
