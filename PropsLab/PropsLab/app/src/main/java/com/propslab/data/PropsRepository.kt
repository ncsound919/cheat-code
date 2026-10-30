package com.propslab.data

import androidx.room.withTransaction
import com.propslab.agent.AgentContext
import com.propslab.agent.AgentData
import com.propslab.agent.AgentProp
import com.propslab.agent.PlaceOutcome
import com.propslab.agent.Sportsbook
import com.propslab.agent.SystemPort
import com.propslab.claim.ClaimBench
import com.propslab.claim.ClaimCompiler
import com.propslab.claim.ClaimFormula
import com.propslab.claim.ClaimLens
import com.propslab.claim.DeepTest
import com.propslab.compute.ComputeSettings
import com.propslab.settings.AppSettings
import com.propslab.compute.KaggleClient
import com.propslab.availability.AvailabilityModel
import com.propslab.domain.BetNarrative
import com.propslab.domain.Calibration
import com.propslab.domain.CalibrationPoint
import com.propslab.domain.PlattFit
import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.Outcome
import com.propslab.domain.OutcomeProbabilities
import com.propslab.domain.PredictionGate
import com.propslab.domain.Pricing
import com.propslab.domain.RejectReason
import com.propslab.domain.Settlement
import com.propslab.domain.Side
import com.propslab.domain.Staking
import com.propslab.feed.FeedSync
import com.propslab.feed.GameMarkets
import com.propslab.feed.Names
import com.propslab.fantasy.CaptureKind
import com.propslab.fantasy.FantasyAssetReport
import com.propslab.fantasy.FantasyAssets
import com.propslab.fantasy.FantasyCaptureSave
import com.propslab.fantasy.FantasySaveResult
import com.propslab.fantasy.FantasySlots
import com.propslab.fantasy.FantasySport
import com.propslab.inference.LocalAssistant
import com.propslab.ocr.SlipPick
import com.propslab.sim.Autopilot
import com.propslab.sim.GateThresholds
import com.propslab.sim.LossInsights
import com.propslab.sim.LossObservation
import com.propslab.sim.PromotionGate
import com.propslab.sim.SandboxEngine
import com.propslab.sim.SandboxTruth
import com.propslab.sim.Season
import com.propslab.sim.SimProp
import com.propslab.sim.SideFilter
import com.propslab.sim.SimStats
import com.propslab.sim.StrategyParams
import com.propslab.sim.Verdict
import com.propslab.theory.HistoryCsv
import com.propslab.theory.TheoryBench
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** A deep test that was pushed and has not been read back yet. */
private const val RUNNING = "RUNNING"

/** One formula with the picks it would take today. Internal to the parlay board. */
private data class ParlaySeed(
    val id: String,
    val label: String,
    val tier: String,
    val profit: Double,
    val params: StrategyParams,
    val picks: List<BestPickUi>,
)

class PropsRepository(
    private val db: PropsDatabase,
    private val assistant: LocalAssistant,
    private val feed: FeedSync,
    private val kaggle: KaggleClient? = null,
    private val compute: ComputeSettings? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) : AgentData, SystemPort {

    /** The app and the scheduled worker can both ask for a round; only one may run at a time. */
    private val roundLock = Mutex()

    /** One automatic pass at a time: boot, the button and a worker must not overlap. */
    private val autoLock = Mutex()

    // ---------------------------------------------------------------- slate and ledger

    /** The board as the app trades it: the learned correction applied to the stored raw estimate. */
    fun slate(): Flow<List<PropUi>> = combine(
        db.slateDao().observeSlate(clock() - 12 * PredictionGate.HOUR_MS),
        db.learningDao().observeCalibration(),
    ) { rows, fits ->
        val fit = fits.firstOrNull { it.track == LearningTrack.PROP }?.toFit() ?: PlattFit.IDENTITY
        rows.map { it.toUi(fit) }
    }

    /** The raw board, before the correction. The learning ledger fits on this. */
    private fun rawSlate(): Flow<List<PropUi>> =
        db.slateDao().observeSlate(clock() - 12 * PredictionGate.HOUR_MS)
            .map { rows -> rows.map { it.toUi() } }

    /**
     * The moneyline, spread and total for every game on the board.
     *
     * These are the books' prices, not the model's: nothing here is scored, so the screen
     * shows them exactly as quoted and keeps them out of the strategy's picks.
     */
    /**
     * The game board: quoted lines, team ratings and the model's forecast, all in one
     * flow. Ratings come from the box scores already stored, so there is no second feed
     * to keep in sync and no game score invented anywhere.
     */
    fun gameBoard(): Flow<GameBoardUi> = combine(
        db.feedDao().observeGameLines(clock() - 12 * PredictionGate.HOUR_MS),
        db.feedDao().observeTeamTotals(),
        db.learningDao().observeCalibration(),
    ) { rows, totals, fits ->
        val fit = fits.firstOrNull { it.track == LearningTrack.GAME }?.toFit() ?: PlattFit.IDENTITY
        GameForecastBoard.build(rows, totals, fit)
    }

    fun gameLines(): Flow<List<GameLinesUi>> = gameBoard().map { it.lines }

    // ---------------------------------------------------------------- captured fantasy league

    /** The newest captured league for a sport, with its teams, weekly rosters and snapshots. */
    fun observeFantasyLeague(sport: FantasySport): Flow<FantasyLeagueWithData?> =
        db.fantasyLeagueDao().observeLatest(sport.slug)

    suspend fun latestFantasyLeague(sport: FantasySport): FantasyLeagueWithData? =
        db.fantasyLeagueDao().latest(sport.slug)

    /**
     * Writes one screenshot's reading. A roster import replaces only that team's rows for
     * that week; a league-table import replaces the standings. Earlier weeks are untouched,
     * which is what lets the season track show real history instead of the latest snapshot.
     */
    suspend fun saveFantasyCapture(capture: FantasyCaptureSave): FantasySaveResult {
        val dao = db.fantasyLeagueDao()
        val sport = capture.sport
        val now = clock()
        val existing = dao.latest(sport.slug)
        val teamName = capture.teamName.trim().take(40)
        val week = capture.week.coerceIn(1, 30)
        if (capture.kind != CaptureKind.LEAGUE_TABLE) {
            require(teamName.isNotBlank()) { "Give the team a name before saving." }
            require(capture.players.isNotEmpty()) { "No player rows were read, so there is nothing to save." }
        } else {
            require(capture.teams.isNotEmpty()) { "No team rows were read, so there is nothing to save." }
        }

        val leagueName = capture.leagueName.trim().take(60)
            .ifBlank { existing?.league?.name ?: "My ${sport.label} league" }
        val leagueId = existing?.league?.id
            ?: "fl-${sport.slug}-${java.util.UUID.randomUUID().toString().take(12)}"
        val myTeam = when (capture.kind) {
            CaptureKind.MY_TEAM -> teamName
            else -> existing?.league?.myTeam.orEmpty()
        }
        val oldWeek = dao.week("$leagueId|$week")
        val roster = if (capture.kind == CaptureKind.LEAGUE_TABLE) emptyList() else {
            capture.players
                .filter { it.name.isNotBlank() }
                .distinctBy { it.key }
                .map { player ->
                    FantasyRosterEntity(
                        id = "$leagueId|$week|${Names.key(teamName)}|${player.key}",
                        leagueId = leagueId,
                        week = week,
                        teamName = teamName,
                        playerKey = player.key,
                        playerName = player.name.take(60),
                        slot = player.slot.take(20),
                        position = player.position.take(12),
                        proTeam = player.proTeam.take(12),
                        opponent = player.opponent.take(12),
                        projected = player.projected,
                        seasonPoints = player.seasonAverage,
                        athleteId = "",
                        headshotUrl = "",
                        teamLogoUrl = "",
                        source = capture.platform.ifBlank { "Screenshot" },
                        updatedUtc = now,
                    )
                }
        }
        val league = FantasyLeagueEntity(
            id = leagueId,
            sport = sport.slug,
            platform = capture.platform.ifBlank { existing?.league?.platform ?: "Screenshot" },
            name = leagueName,
            scoring = existing?.league?.scoring?.ifBlank { "Imported screenshot" }
                ?: "Imported screenshot",
            myTeam = myTeam,
            week = if (capture.kind == CaptureKind.LEAGUE_TABLE) {
                existing?.league?.week ?: week
            } else {
                maxOf(existing?.league?.week ?: 0, week)
            },
            importedUtc = existing?.league?.importedUtc ?: now,
            updatedUtc = now,
            note = capture.note.take(240),
        )

        val teamRows = if (capture.kind == CaptureKind.LEAGUE_TABLE) {
            capture.teams
                .filter { it.name.isNotBlank() }
                .distinctBy { it.key }
                .map { team ->
                    FantasyTeamEntity(
                        leagueId = leagueId,
                        name = team.name.take(60),
                        owner = team.owner.take(60),
                        rank = team.rank,
                        wins = team.wins,
                        losses = team.losses,
                        ties = team.ties,
                        pointsFor = team.pointsFor,
                        pointsAgainst = team.pointsAgainst,
                        isMine = myTeam.isNotBlank() && team.key == Names.key(myTeam),
                        updatedUtc = now,
                    )
                }
        } else {
            val old = dao.team(leagueId, teamName)
            listOf(
                FantasyTeamEntity(
                    leagueId = leagueId,
                    name = teamName,
                    owner = old?.owner.orEmpty(),
                    rank = old?.rank ?: 0,
                    wins = old?.wins ?: 0,
                    losses = old?.losses ?: 0,
                    ties = old?.ties ?: 0,
                    pointsFor = old?.pointsFor,
                    pointsAgainst = old?.pointsAgainst,
                    isMine = capture.kind == CaptureKind.MY_TEAM || old?.isMine == true,
                    updatedUtc = now,
                ),
            )
        }

        val weekRow = if (capture.kind == CaptureKind.LEAGUE_TABLE) null else {
            val projected = capture.players
                .filter { FantasySlots.isStarter(it.slot) }
                .mapNotNull { it.projected }
                .takeIf { it.isNotEmpty() }
                ?.sum()
            FantasyWeekEntity(
                id = "$leagueId|$week",
                leagueId = leagueId,
                week = week,
                myTeam = if (capture.kind == CaptureKind.MY_TEAM) teamName else oldWeek?.myTeam.orEmpty(),
                opponent = if (capture.kind == CaptureKind.OPPONENT) teamName else oldWeek?.opponent.orEmpty(),
                myProjected = if (capture.kind == CaptureKind.MY_TEAM) projected else oldWeek?.myProjected,
                oppProjected = if (capture.kind == CaptureKind.OPPONENT) projected else oldWeek?.oppProjected,
                myScore = oldWeek?.myScore,
                oppScore = oldWeek?.oppScore,
                note = oldWeek?.note ?: "Imported from ${capture.platform.ifBlank { "a screenshot" }}.",
                updatedUtc = now,
            )
        }

        db.withTransaction {
            dao.upsertLeague(league)
            if (capture.kind == CaptureKind.LEAGUE_TABLE) {
                dao.deleteTeams(leagueId)
            } else if (capture.kind == CaptureKind.MY_TEAM) {
                dao.clearMine(leagueId)
            }
            dao.upsertTeams(teamRows)
            if (capture.kind != CaptureKind.LEAGUE_TABLE) {
                dao.clearRoster(leagueId, week, teamName)
                dao.upsertRoster(roster)
            }
            if (weekRow != null) dao.upsertWeek(weekRow)
        }

        return FantasySaveResult(
            leagueId = leagueId,
            players = roster.size,
            teams = teamRows.size,
        )
    }

    /**
     * Looks up headshots and club logos for a saved league. These are public ESPN assets;
     * the screenshot never leaves the device. A miss leaves the initials placeholder rather
     * than putting a different player's face on the card.
     */
    suspend fun enrichFantasyLeague(leagueId: String): FantasyAssetReport {
        val dao = db.fantasyLeagueDao()
        val saved = dao.byId(leagueId) ?: return FantasyAssetReport()
        val sport = FantasySport.fromSlug(saved.league.sport) ?: return FantasyAssetReport()
        val teams = runCatching { FantasyAssets.fetchTeams(sport) }.getOrDefault(emptyList())
        val targets = saved.roster
            .groupBy { it.playerKey }
            .values
            .mapNotNull { rows -> rows.firstOrNull() }
            .filter { it.playerName.isNotBlank() && (it.headshotUrl.isBlank() || it.teamLogoUrl.isBlank()) }
            .take(160)
        val gate = Semaphore(4)
        val found = coroutineScope {
            targets.map { row ->
                async {
                    gate.withPermit {
                        runCatching {
                            FantasyAssets.findPlayer(
                                name = row.playerName,
                                sport = sport,
                                teams = teams,
                                preferredTeam = row.proTeam,
                            )
                        }.getOrNull()?.let { asset ->
                            val headshot = asset.headshotUrl.ifBlank {
                                FantasyAssets.headshotUrl(sport, asset.athleteId)
                            }
                            if (headshot.isBlank() && asset.teamLogoUrl.isBlank()) return@withPermit false
                            dao.setPlayerAssets(
                                leagueId = leagueId,
                                playerKey = row.playerKey,
                                athleteId = asset.athleteId,
                                headshotUrl = headshot.ifBlank { row.headshotUrl },
                                teamLogoUrl = asset.teamLogoUrl.ifBlank { row.teamLogoUrl },
                                proTeam = asset.teamCode.ifBlank { row.proTeam },
                                position = row.position,
                            )
                            true
                        } ?: false
                    }
                }
            }.awaitAll()
        }
        return FantasyAssetReport(tried = targets.size, found = found.count { it }, teams = teams.size)
    }

    /** A snapshot of the slate for the agent tools. */
    override suspend fun props(): List<AgentProp> = slate().first().map { ui ->
        AgentProp(
            id = ui.id, playerName = ui.playerName, teamCode = ui.teamCode, matchup = ui.matchup,
            startUtc = ui.startUtc, market = ui.market, book = ui.book, line = ui.line,
            overOdds = ui.overOdds, underOdds = ui.underOdds,
            pOver = ui.probabilities?.over, pPush = ui.probabilities?.push, pUnder = ui.probabilities?.under,
            isFixture = ui.isFixture,
        )
    }

    /**
     * The live facts an agent tool needs, and the defaults that make "the best pick" enough.
     *
     * A novice should not have to read a prediction id off the screen or choose a book from
     * a list. These are the same values the Best screen is showing: the top pick, its side,
     * the 1-unit paper stake and the leading formula. Supplied arguments always win.
     */
    override suspend fun toolContext(): AgentContext {
        val bets = bestBets().first()
        val box = sandbox().first()
        val top = bets.picks.firstOrNull()
        val prop = top?.prop
        val supportedBook = prop?.book?.let { Sportsbook.parse(it) }
            ?: Sportsbook.DRAFTKINGS
        val line = prop?.line?.let { if (it % 1.0 == 0.0) "%.0f".format(it) else "%.1f".format(it) }
            ?: "no line"
        val defaults = buildMap {
            if (top != null && prop != null) {
                put("get_edge", mapOf("predictionId" to prop.id))
                put(
                    "stage_ticket",
                    mapOf(
                        "predictionId" to prop.id,
                        "side" to top.side.name,
                        "book" to supportedBook.slug,
                        "stakeUnits" to "1.0",
                    ),
                )
                put("open_book", mapOf("book" to supportedBook.slug))
            }
            put("set_formula", mapOf("formulaId" to "best"))
            put("list_props", mapOf("status" to "available"))
        }
        val settled = db.learningDao().settledCount()
        val history = db.signalDao().historyCount()
        val brief = buildString {
            if (top != null && prop != null) {
                append("Top pick: ").append(prop.playerName).append(' ').append(top.side.name.lowercase())
                append(' ').append(line).append(' ').append(prop.market).append(" at ")
                append(fmtOddsPlain(top.odds)).append(", id ").append(prop.id).append(". ")
            } else {
                append("There is no usable pick on the board right now. ")
            }
            append("Live formula: ").append(bets.strategyLabel ?: "none yet")
            append(if (bets.live) " (promoted). " else " (still in practice). ")
            append("The board has ").append(bets.picks.size).append(" pick")
            append(if (bets.picks.size == 1) "" else "s").append(". ")
            append("Paper bankroll: ").append(num2Plain(box.bankroll)).append(" units. ")
            append("Learning ledger: ").append(settled).append(" settled. ")
            append("Imported history: ").append(history).append(" rows.")
        }
        return AgentContext(brief = brief, defaults = defaults)
    }

    override suspend fun placePaperBet(predictionId: String, side: Side, stakeUnits: Double): PlaceOutcome =
        when (placeBet(predictionId, side, stakeUnits)) {
            PlaceResult.Placed -> PlaceOutcome.PLACED
            PlaceResult.Duplicate -> PlaceOutcome.DUPLICATE
            PlaceResult.Unavailable -> PlaceOutcome.UNAVAILABLE
        }

    fun prop(id: String): Flow<PropUi?> = combine(
        db.slateDao().observeRow(id),
        db.learningDao().observeCalibration(),
    ) { row, fits ->
        val fit = fits.firstOrNull { it.track == LearningTrack.PROP }?.toFit() ?: PlattFit.IDENTITY
        row?.toUi(fit)
    }

    fun bets(): Flow<List<BetUi>> = db.paperDao().observeBets().map { rows -> rows.map { it.toUi() } }

    fun audit(): Flow<List<AuditEventEntity>> = db.auditDao().observe()

    // ------------------------------------------------------------------------- live feed

    /** What the last odds and box-score sync did, for the Live data screen. */
    fun feedState(): Flow<FeedStateEntity?> = db.feedDao().observeState(FeedStateEntity.ID)

    // ------------------------------------------------------------ late availability

    fun signals(): Flow<List<AvailabilitySignalEntity>> = db.signalDao().observe()

    /**
     * Records a late-availability report by hand.
     *
     * This is the primary signal path, not a fallback: a person watching a shootaround knows
     * more than a news index does, and the app should never make that knowledge harder to
     * enter than a headline it happened to find.
     */
    suspend fun recordSignal(playerName: String, status: String, detail: String): Boolean {
        val name = playerName.trim()
        if (name.isEmpty()) return false
        val parsed = AvailabilityStatus.parse(status) ?: return false
        val now = clock()
        val key = Names.key(name)
        val onBoard = slate().first().firstOrNull { Names.key(it.playerName) == key }
        val canonical = onBoard?.playerName ?: name
        db.signalDao().upsert(
            listOf(
                AvailabilitySignalEntity(
                    id = "sig-" + Integer.toHexString("$key|$parsed|${now / PredictionGate.HOUR_MS}".hashCode()),
                    playerKey = key,
                    playerName = canonical,
                    status = parsed,
                    detail = detail.trim(),
                    source = SignalSource.MANUAL,
                    headline = "",
                    url = "",
                    observedUtc = now,
                    expiresUtc = now + AvailabilityModel.TTL_MS,
                ),
            ),
        )
        audit(
            "signal_recorded", canonical,
            "${AvailabilityStatus.label(parsed)} recorded by hand" +
                (if (detail.isNotBlank()) ": ${detail.trim()}" else "") +
                (if (onBoard == null) " (this player is not on the board yet)" else ""),
        )
        return true
    }

    /** Expires every stored signal. Recorded as an event, because clearing the record matters. */
    suspend fun expireSignals(): Int {
        val cleared = db.signalDao().purge(Long.MAX_VALUE)
        if (cleared > 0) audit("signals_expired", "signals", "$cleared availability signals expired by the user")
        return cleared
    }

    // ---------------------------------------------------------------- the theory lab

    /**
     * Imports a dataset of historical props: prices, the model's own probability at the
     * time, and the result. Re-importing the same file adds nothing.
     */
    suspend fun importHistory(text: String): String {
        val parsed = HistoryCsv.parse(text)
        if (parsed.rows.isEmpty()) {
            val why = parsed.problems.joinToString(" ").ifEmpty { "No readable rows were found." }
            audit("history_import_failed", "dataset", why)
            return "Import failed. $why"
        }
        val inserted = db.withTransaction {
            db.signalDao().insertHistory(
                parsed.rows.map { row ->
                    HistoryPropEntity(
                        id = row.id,
                        playedUtc = row.playedUtc,
                        playerKey = Names.key(row.playerName),
                        playerName = row.playerName,
                        market = row.market,
                        line = row.line,
                        overOdds = row.overOdds,
                        underOdds = row.underOdds,
                        pOver = row.pOver,
                        pPush = row.pPush,
                        actual = row.actual,
                        source = "import",
                    )
                },
            ).count { it != -1L }
        }
        val total = db.signalDao().historyCount()
        audit(
            "history_imported", "dataset",
            "$inserted rows added (${parsed.skipped} skipped), $total rows in the dataset",
        )
        return buildString {
            append("$inserted rows imported")
            if (parsed.skipped > 0) append(", ${parsed.skipped} skipped")
            append(". The dataset now holds $total rows. Run `backtest` to put the grid over it.")
        }
    }

    suspend fun historyCount(): Int = db.signalDao().historyCount()

    // ------------------------------------------------------------------ system port

    override suspend fun statusLine(): String {
        val box = sandbox().first()
        val props = slate().first()
        val feedState = db.feedDao().state(FeedStateEntity.ID)
        val signals = db.signalDao().active(clock())
        val history = db.signalDao().historyCount()
        val priced = props.count { it.available }
        return buildString {
            append("Bankroll ").append(num1(box.bankroll)).append(" units")
            append(" (").append(num2(box.liveProfit)).append(" on ").append(box.trades.size).append(" live picks). ")
            append("Round ").append(box.round).append(". ")
            append(
                box.promotedLabel?.let { "The live book is held by $it (${box.promotedId}). " }
                    ?: "No formula holds the live book. ",
            )
            append("Board: ").append(props.size).append(" props, ").append(priced).append(" priced. ")
            append("Feed: prices ")
                .append(if ((feedState?.lastPropsSyncUtc ?: 0L) > 0L) "synced" else "never synced")
                .append(", box scores ")
                .append(if ((feedState?.lastResultsSyncUtc ?: 0L) > 0L) "synced" else "never synced")
                .append(". ")
            append(signals.size).append(" live signal(s). ")
            append("Dataset: ").append(history).append(" rows.")
            if (box.trades.any { !it.synthetic }) {
                append(" The live record uses real results only for retirement.")
            }
        }
    }

    override suspend fun runRound(): String {
        val outcome = runSandboxRound()
        return "Round ${outcome.round}: ${outcome.note} ${outcome.livePicks} live pick(s) taken. " +
            "${outcome.ready} of ${outcome.considered} formulas cleared the gate."
    }

    override suspend fun syncFeed(): String {
        val report = refreshFeed()
        val book = refreshLiveBook()
        return buildString {
            append(report.schedule.note).append(' ')
            append(report.results.note).append(' ')
            append(report.signals.note).append(' ')
            append(report.props.note).append(' ')
            append("${book.placed} pick(s) taken, ${book.settled} settled.")
            book.retiredId?.let { append(" $it was retired on its real record.") }
        }
    }

    override suspend fun setFormula(target: String, reason: String): String {
        val now = clock()
        val current = db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)
        if (target.equals("none", ignoreCase = true)) {
            val previous = current?.promotedId
            db.withTransaction {
                if (previous != null) db.sandboxDao().setTier(previous, Tier.SANDBOX)
                db.sandboxDao().upsertState(
                    (current ?: SandboxStateEntity()).copy(
                        promotedId = null,
                        promotedLabel = null,
                        note = "The live book was emptied by hand." + reasonSuffix(reason),
                    ),
                )
            }
            audit("formula_cleared", previous ?: "-", "the live book was emptied by hand" + reasonSuffix(reason))
            return "The live book is empty. Nothing trades until a formula is promoted, by the gate or by hand."
        }

        val id = if (target.equals("best", ignoreCase = true)) {
            sandbox().first().ready.firstOrNull()?.id
                ?: return "No formula has cleared the gate, so there is no best candidate to promote."
        } else target
        val params = Autopilot.grid.firstOrNull { it.id == id }
            ?: db.sandboxDao().formula(id)?.params?.let { StrategyParams.parse(it) }
            ?: return "There is no formula '$id' in the grid or the sandbox's measured formulas."
        val previous = current?.promotedId
        val cleared = sandbox().first().formulas.firstOrNull { it.id == params.id }?.passed == true
        db.withTransaction {
            if (previous != null && previous != params.id) db.sandboxDao().setTier(previous, Tier.SANDBOX)
            db.sandboxDao().setTier(params.id, Tier.LIVE)
            db.sandboxDao().upsertState(
                (current ?: SandboxStateEntity()).copy(
                    promotedId = params.id,
                    promotedLabel = params.label,
                    note = "${params.label} holds the live book, put there by hand." + reasonSuffix(reason),
                    lastRunUtc = now,
                ),
            )
        }
        audit(
            "formula_promoted", params.id,
            "${params.label} was promoted by hand" +
                (if (cleared) " after clearing the gate" else ", without clearing the gate") +
                reasonSuffix(reason),
        )
        return "${params.label} now holds the live book" +
            (if (cleared) ", and it had cleared the gate." else ", though the gate has not passed it.") +
            (if (previous != null && previous != params.id) " $previous went back to the sandbox." else "")
    }

    override suspend fun noteSignal(player: String, status: String, detail: String): String {
        val parsed = AvailabilityStatus.parse(status) ?: return "'$status' is not a status I know."
        if (!recordSignal(player, status, detail)) return "That signal could not be recorded."
        val factor = AvailabilityModel.applied(parsed)
        val effect = when {
            factor <= 0.0 -> "the estimate is withheld until the report changes"
            factor >= 1.0 -> "the projection is unchanged"
            else -> "the projection is cut by ${AvailabilityModel.shiftPct(parsed)}%"
        }
        return "${AvailabilityStatus.label(parsed)} recorded for $player — $effect. It applies on the next sync."
    }

    override suspend fun backtest(): String {
        val rows = db.signalDao().history()
        if (rows.isEmpty()) {
            return "No dataset is imported. Import a CSV of historical props first: date, player, market, " +
                "line, over, under, p_over, actual (and optionally p_push)."
        }
        val mapped = historyRows()
        val result = withContext(Dispatchers.Default) { TheoryBench.run(mapped) }
        audit(
            "backtest_run", "dataset",
            "${result.rows} rows, ${result.usable} usable, ${result.passed.size} formulas cleared the gate",
        )
        return TheoryBench.summarise(result)
    }

    /** The imported dataset, in the shape the history bench consumes. */
    private suspend fun historyRows(): List<HistoryCsv.Row> = db.signalDao().history().map { entity ->
        HistoryCsv.Row(
            id = entity.id,
            playedUtc = entity.playedUtc,
            playerName = entity.playerName,
            market = entity.market,
            line = entity.line,
            overOdds = entity.overOdds,
            underOdds = entity.underOdds,
            pOver = entity.pOver,
            pPush = entity.pPush,
            actual = entity.actual,
        )
    }

    override suspend fun kaggleExperiment(note: String): String {
        val credentials = compute?.credentials()
        if (credentials == null || !credentials.ready) {
            return "No Kaggle account is saved. Open Lab → Live data and add the username and API key. " +
                "The app only reads the account: it lists runs and asks their status."
        }
        val client = kaggle ?: return "The compute lane is not wired in this build."
        val kernels = client.recent(credentials).getOrElse { failure ->
            audit("kaggle_checked", credentials.username, failure.message ?: "the account could not be read")
            return failure.message ?: "The Kaggle account could not be read."
        }
        if (kernels.isEmpty()) return "The account has no kernels yet."
        val lines = kernels.take(4).map { kernel ->
            val state = client.status(credentials, kernel.ref).getOrElse { "status unavailable" }
            "· ${kernel.ref} — $state"
        }
        audit(
            "kaggle_checked", credentials.username,
            "listed ${kernels.size} kernels" + (if (note.isNotBlank()) " while asked: ${note.take(120)}" else ""),
        )
        return buildString {
            append("Recent kernels on the wired account:\n")
            append(lines.joinToString("\n"))
            append("\nOpen them at https://www.kaggle.com/code — nothing is downloaded automatically.")
        }
    }

    // ------------------------------------------------------------ plain-language claims

    /** Every user-created strategy, newest first. The lens board reads this. */
    fun claims(): Flow<List<ClaimFormulaEntity>> = db.claimDao().observe()

    data class ClaimOutcome(
        val claim: ClaimFormulaEntity,
        val bench: ClaimBench.Result,
        val note: String,
    )

    /**
     * The whole on-device pipeline for one sentence: compile, test, persist.
     *
     * The bench runs on a fresh simulated season, never on the board that is live now, so a
     * claim cannot be tuned to the games it will be asked to bet. A claim that passes is
     * entered into the sandbox grid; it still has to win a normal promotion round before any
     * of its picks reach the live book.
     */
    suspend fun createClaim(text: String): ClaimOutcome {
        val now = clock()
        val rows = db.slateDao().observeSlate(now - 12 * PredictionGate.HOUR_MS).first()
        val board = ClaimCompiler.Board(
            playerNames = rows.map { it.playerName }.distinct(),
            marketByPlayer = rows.groupBy({ it.playerName }, { it.market }).mapValues { it.value.distinct() },
        )
        val formula = ClaimCompiler.compile(text, board)
        val bench = withContext(Dispatchers.Default) { ClaimBench.run(formula) }
        val params = bench.chosen.params
        val status = if (bench.ready) ClaimStatus.CANDIDATE else ClaimStatus.REJECTED

        val entity = ClaimFormulaEntity(
            id = formula.id,
            claim = formula.claim,
            name = formula.name,
            factorTag = formula.factorTag,
            headline = formula.headline,
            lens = params.paramsText,
            formulaId = params.id,
            status = status,
            bets = bench.chosen.stats.bets,
            wins = bench.chosen.stats.wins,
            losses = bench.chosen.stats.losses,
            profit = bench.chosen.stats.profit,
            roi = bench.chosen.stats.roi,
            tStat = bench.chosen.stats.tStat,
            maxDrawdown = bench.chosen.stats.maxDrawdown,
            passed = bench.ready,
            plain = bench.plain,
            boardNote = bench.boardNote,
            gameTerms = formula.inGameTerms.joinToString("\n"),
            falsifier = formula.falsifier,
            subject = formula.subject,
            deepVerdict = "",
            deepNote = "",
            deepAtUtc = 0L,
            createdUtc = now,
        )

        val round = db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)?.round ?: 0
        db.withTransaction {
            db.claimDao().upsert(entity)
            if (bench.ready && db.sandboxDao().formula(params.id) == null) {
                db.sandboxDao().upsertFormulas(
                    listOf(
                        FormulaRunEntity(
                            id = params.id,
                            label = formula.name,
                            params = params.paramsText,
                            tier = Tier.SANDBOX,
                            bets = bench.chosen.stats.bets,
                            wins = bench.chosen.stats.wins,
                            losses = bench.chosen.stats.losses,
                            pushes = bench.chosen.stats.pushes,
                            staked = bench.chosen.stats.staked,
                            profit = bench.chosen.stats.profit,
                            roi = bench.chosen.stats.roi,
                            tStat = bench.chosen.stats.tStat,
                            maxDrawdown = bench.chosen.stats.maxDrawdown,
                            finalBankroll = bench.chosen.stats.finalBankroll,
                            verdict = "PASS",
                            reasons = "",
                            round = round,
                            updatedUtc = now,
                        ),
                    ),
                )
            }
            audit(
                "claim_created", formula.id,
                "\"${formula.claim.take(160)}\" compiled to ${params.id} " +
                    "(${if (bench.ready) "cleared" else "failed"} the bench on ${bench.seasonProps} props)",
            )
        }

        return ClaimOutcome(
            claim = entity,
            bench = bench,
            note = if (bench.ready) {
                "${formula.name} cleared the bench and joins the sandbox. ${bench.plain}"
            } else {
                "${formula.name} did not clear the bench, so it was kept as a note. ${bench.plain}"
            },
        )
    }

    /**
     * The slow lane. Pushes a generated kernel that tests this claim's idea on 665k real
     * player-games, waits for it, downloads its JSON, and stores the verdict on the claim.
     *
     * It is user-initiated on purpose: a kernel run spends the account's quota, and nothing
     * on this phone pushes compute without a tap.
     */
    suspend fun runDeepTest(claimId: String): String {
        val entity = db.claimDao().byId(claimId) ?: return "That strategy is no longer on the device."
        val credentials = compute?.credentials()
        if (credentials == null || !credentials.ready) {
            return "No Kaggle account is saved. Open Lab → Live data, add the username and a KGAT_ token, then try again."
        }
        val client = kaggle ?: return "The compute lane is not wired in this build."
        val params = StrategyParams.parse(entity.lens)
            ?: return "This strategy's lens could not be read back."
        val formula = ClaimFormula(
            id = entity.id,
            claim = entity.claim,
            name = entity.name,
            factorTag = entity.factorTag,
            factorTitle = entity.name,
            lens = ClaimLens(
                tiltPts = params.tiltPts,
                side = params.sideFilter,
                markets = params.markets,
                minEdgePts = params.minEdgePts,
                minPrice = params.minPrice,
                maxPrice = params.maxPrice,
            ),
            headline = entity.headline,
            inGameTerms = entity.inGameTerms,
            falsifier = entity.falsifier,
            subject = entity.subject,
        )

        val pushed = client.push(
            credentials = credentials,
            slug = DeepTest.KERNEL_SLUG.substringAfter('/'),
            title = "Cheat Code claim lens",
            script = DeepTest.script(formula),
            datasetRef = DeepTest.DATASET,
        ).getOrElse { failure ->
            audit("claim_deep_failed", entity.id, failure.message ?: "the kernel could not be pushed")
            return failure.message ?: "Kaggle refused the run."
        }

        // Mark it running before the wait, so a user who leaves the screen still gets the
        // result: the screen resumes this run the next time it opens.
        db.claimDao().recordDeepTest(entity.id, RUNNING, "Pushed to Kaggle. This takes a few minutes.", clock())

        var status: KaggleClient.Status? = null
        for (attempt in 0 until 45) {
            delay(4_000)
            status = client.sessionStatus(credentials, pushed.ref).getOrNull()
            if (status?.done == true) break
        }
        val final = status
        if (final == null) {
            return "The Kaggle run was pushed (${pushed.ref}) but its status could not be read. " +
                "Open it at ${pushed.url}."
        }
        if (final.state != KaggleClient.State.COMPLETE) {
            if (!final.done) {
                return "The Kaggle run is still ${final.label}. Reopen this screen and it will pick the result up."
            }
            val note = "The Kaggle run ended ${final.label}${if (final.failure.isBlank()) "" else ": ${final.failure}"}."
            db.claimDao().recordDeepTest(entity.id, "FAIL", note, clock())
            audit("claim_deep_failed", entity.id, note)
            return note
        }

        return recordDeepResult(client, credentials, entity.id, pushed.ref)
    }

    /**
     * Picks up a run that was pushed but not read, e.g. because the user left the screen or
     * the phone slept through the wait. Cheap when there is nothing to do: one status call.
     */
    suspend fun resumeDeepTest(claimId: String): String? {
        val entity = db.claimDao().byId(claimId) ?: return null
        if (entity.deepVerdict != RUNNING) return null
        val credentials = compute?.credentials() ?: return null
        if (!credentials.ready) return null
        val client = kaggle ?: return null
        val status = client.sessionStatus(credentials, DeepTest.KERNEL_SLUG).getOrNull() ?: return null
        if (!status.done) return null
        if (status.state != KaggleClient.State.COMPLETE) {
            val note = "The Kaggle run ended ${status.label}${if (status.failure.isBlank()) "" else ": ${status.failure}"}."
            db.claimDao().recordDeepTest(entity.id, "FAIL", note, clock())
            return note
        }
        return recordDeepResult(client, credentials, entity.id, DeepTest.KERNEL_SLUG)
    }

    private suspend fun recordDeepResult(
        client: KaggleClient,
        credentials: KaggleClient.Credentials,
        claimId: String,
        ref: String,
    ): String {
        val files = client.output(credentials, ref).getOrElse { failure ->
            return failure.message ?: "The run finished but its files could not be listed."
        }
        val result = files.firstOrNull { it.name == DeepTest.RESULT_FILE }
            ?: return "The run finished but wrote no ${DeepTest.RESULT_FILE}."
        val body = client.download(result.url).getOrElse { failure ->
            return failure.message ?: "The result file could not be downloaded."
        }
        val outcome = DeepTest.parse(body)
        db.claimDao().recordDeepTest(claimId, outcome.verdict, outcome.plain, clock())
        audit("claim_deep_tested", claimId, "${outcome.verdict} on ${outcome.rows} real player-games")
        return outcome.plain
    }

    private fun reasonSuffix(reason: String): String =
        if (reason.isBlank()) "" else " Reason given: ${reason.trim().take(160)}"

    /**
     * One turn of the real feed: box scores first, then prices, then settlement.
     *
     * It is deliberately separate from a sandbox round. A round decides which formula has
     * earned the live book; the feed only keeps that book supplied with real prices and
     * real results. Running one never implies the other.
     */
    suspend fun refreshFeed(): FeedSync.FeedReport = feed.run()

    /**
     * Keeps the live book current without re-running the season: settle what has finished,
     * take the promoted formula's picks on whatever is newly on the board, and move the
     * bankroll. Called after every feed sync.
     */
    suspend fun refreshLiveBook(place: Boolean = true): LiveBookOutcome {
        val now = clock()
        var state = db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)
        // Settlement always runs: a finished game has already happened, whether or not the
        // user paused new picks. Only placement respects the pause.
        val settled = settleLiveBook(now)
        // A formula that earned the book in a simulation still has to hold up against real
        // results. Only a real sample can retire it, and only a large one.
        val retired = if (settled > 0) reviewLiveRecord(state) else null
        if (retired != null) state = db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)
        // The promoted formula may be a learned candidate that is not in the fixed grid, so
        // its stored params are authoritative. Falling back to the grid keeps old installs
        // working, where every promoted id was a grid row.
        val params = state?.promotedId?.let { id ->
            db.sandboxDao().formula(id)?.params?.let { StrategyParams.parse(it) }
                ?: Autopilot.grid.firstOrNull { it.id == id }
        }
        val placed = if (place) params?.let { placeLivePicks(it, now) } ?: 0 else 0
        if (state != null && (settled > 0 || placed > 0)) writeBookTotals(state)
        if (settled > 0 || placed > 0) {
            audit(
                "live_book_refreshed", state?.promotedId ?: "-",
                "$placed picks taken on the live board, $settled settled",
            )
        }
        // The ledger is written after the live book so both see the same board. Its own
        // work is idempotent, so a sync that changes nothing costs two cheap queries.
        learn()
        return LiveBookOutcome(placed = placed, settled = settled, promotedId = params?.id, retiredId = retired)
    }

    /**
     * Reviews the promoted formula against the bets it has actually settled.
     *
     * The promotion gate only ever sees a simulated season, so a formula can hold a book it
     * has not earned in reality. This is the one check that uses real results, it ignores
     * every synthetic trade, and it needs [LIVE_REVIEW_MIN_BETS] real settlements before it
     * will move anything, because a bad fortnight is not evidence either.
     */
    private suspend fun reviewLiveRecord(state: SandboxStateEntity?): String? {
        val state = state ?: return null
        val id = state.promotedId ?: return null
        val trades = db.sandboxDao().allTrades()
            .filter { it.formulaId == id && !it.synthetic && it.outcome != null }
            .sortedBy { it.settledUtc ?: it.placedUtc }
        if (trades.size < LIVE_REVIEW_MIN_BETS) return null

        val stats = statsOf(trades)
        val gate = PromotionGate.review(stats)
        if (gate.verdict != Verdict.RETIRE) return null

        db.withTransaction {
            db.sandboxDao().setTier(id, Tier.RETIRED)
            db.sandboxDao().upsertState(
                state.copy(
                    promotedId = null,
                    promotedLabel = null,
                    note = "No formula holds the live book: $id was retired on ${trades.size} real results. ${gate.summary}.",
                ),
            )
        }
        audit(
            "formula_retired", id,
            "retired on ${trades.size} settled live bets: ${gate.summary}",
        )
        return id
    }

    /** Turns settled live trades into the same statistics the sandbox gate already understands. */
    private fun statsOf(trades: List<SandboxTradeEntity>): SimStats {
        var wins = 0
        var losses = 0
        var pushes = 0
        var staked = 0.0
        var profit = 0.0
        var bankroll = SandboxEngine.STARTING_BANKROLL
        var peak = bankroll
        var maxDrawdown = 0.0
        var impliedSum = 0.0
        val perBet = ArrayList<Double>(trades.size)
        for (trade in trades) {
            val pnl = trade.profit ?: 0.0
            bankroll += pnl
            staked += trade.stake
            profit += pnl
            when (trade.outcome) {
                Outcome.WIN.name -> wins++
                Outcome.LOSS.name -> losses++
                Outcome.PUSH.name -> pushes++
            }
            if (trade.stake > 0.0) perBet += pnl / trade.stake
            runCatching { Odds.impliedProbability(trade.odds) }.getOrNull()?.let { impliedSum += it }
            peak = maxOf(peak, bankroll)
            if (peak > 0.0) maxDrawdown = maxOf(maxDrawdown, (peak - bankroll) / peak)
        }
        val mean = if (perBet.isEmpty()) 0.0 else perBet.average()
        val variance = if (perBet.size > 1) {
            perBet.sumOf { (it - mean) * (it - mean) } / (perBet.size - 1)
        } else 0.0
        val sd = kotlin.math.sqrt(variance)
        return SimStats(
            bets = trades.size,
            wins = wins,
            losses = losses,
            pushes = pushes,
            staked = staked,
            profit = profit,
            tStat = if (perBet.size >= 2 && sd > 0.0) mean / (sd / kotlin.math.sqrt(perBet.size.toDouble())) else 0.0,
            maxDrawdown = maxDrawdown,
            finalBankroll = bankroll,
            peak = peak,
            lowest = bankroll,
            avgImplied = if (trades.isEmpty()) 0.0 else impliedSum / trades.size,
            ruined = bankroll <= SandboxEngine.RUIN_FLOOR,
        )
    }

    suspend fun placeBet(predictionId: String, side: Side, stake: Double): PlaceResult =
        db.withTransaction<PlaceResult> {
            val ui = db.slateDao().row(predictionId)?.toUi()
            val line = ui?.line
            val odds = if (side == Side.OVER) ui?.overOdds else ui?.underOdds
            if (ui == null || !ui.available || line == null || odds == null) return@withTransaction PlaceResult.Unavailable
            val now = clock()
            val id = "bet-$predictionId-${side.name.lowercase()}"
            val inserted = db.paperDao().insertBet(PaperBetEntity(id, predictionId, side.name, odds, line, stake, now))
            if (inserted == -1L) return@withTransaction PlaceResult.Duplicate
            audit(
                "paper_bet_placed", id,
                "${ui.playerName} ${side.label} $line at ${fmtOddsPlain(odds)}, ${num1(stake)} units, model ${ui.modelVersion}",
            )
            PlaceResult.Placed
        }

    suspend fun settleBet(betId: String, actual: Double): Boolean = db.withTransaction<Boolean> {
        val bet = db.paperDao().bet(betId) ?: return@withTransaction false
        val outcome = Settlement.outcome(Side.valueOf(bet.side), bet.line, actual)
        val profit = Settlement.profit(outcome, bet.stakeUnits, bet.quotedOdds)
        val now = clock()
        val inserted = db.paperDao().insertSettlement(
            SettlementEntity(betId, actual, outcome.name, profit, Settlement.RULE_VERSION, now, "manual"),
        )
        if (inserted == -1L) return@withTransaction false
        audit("result_recorded", betId, "Actual $actual against ${bet.line}: ${outcome.name.lowercase()}, ${num2(profit)} units")
        true
    }

    /** Marks predictions unavailable when their inputs have gone stale. Fixtures never expire. */
    suspend fun expireStalePredictions() {
        val now = clock()
        for (r in db.slateDao().freshnessCandidates()) {
            if (r.source == FIXTURE_SOURCE) continue
            val reasons = PredictionGate.evaluate(r.line, r.quoteTimeUtc, r.featureCutoffUtc, now, true, r.market)
            if (reasons.isEmpty()) continue
            db.withTransaction {
                db.slateDao().markUnavailable(r.predictionId, reasons.joinToString(",") { it.name })
                audit("prediction_expired", r.predictionId, reasons.joinToString("; ") { it.message })
            }
        }
    }

    suspend fun seedFixturesIfEmpty() {
        if (db.slateDao().predictionCount() > 0) return
        FixtureSeeder(db, assistant, clock()).seed()
    }

    override suspend fun audit(kind: String, subjectId: String, detail: String) =
        db.auditDao().insert(AuditEventEntity(ts = clock(), kind = kind, subjectId = subjectId, detail = detail))

    // ---------------------------------------------------------------- screenshot import

    /**
     * Turns picks read off a screenshot into quotes on the board.
     *
     * Nothing here pretends a model has scored the prop. The estimate is the market's own
     * no-vig price, so the edge is zero by construction and the sandbox will not bet it.
     * What the import buys is a real line, a real price and a result to settle against.
     */
    suspend fun importSlip(picks: List<SlipPick>, bookSlug: String): ImportOutcome {
        if (picks.isEmpty()) return ImportOutcome(0, 0, "")
        val now = clock()
        val book = Sportsbook.parse(bookSlug) ?: Sportsbook.DRAFTKINGS
        val gameId = "g-import-${book.slug}"
        val game = GameEntity(
            id = gameId,
            startUtc = now + 8 * PredictionGate.HOUR_MS,
            homeCode = book.displayName.split(" ").first().uppercase().take(4),
            awayCode = "SLIP",
            status = "SCHEDULED",
        )
        val quotes = mutableListOf<OddsSnapshotEntity>()
        val predictions = mutableListOf<PredictionEntity>()
        val players = mutableListOf<PlayerEntity>()
        var added = 0
        var skipped = 0

        for (pick in picks) {
            val key = "${book.slug}|${pick.playerName.lowercase()}|${pick.market}|${pick.line}"
            val hash = Integer.toHexString(key.hashCode())
            val predictionId = "pred-import-$hash"
            if (db.slateDao().predictionExists(predictionId) > 0) {
                skipped++
                continue
            }

            val price = pick.odds ?: DEFAULT_IMPORT_PRICE
            val flags = buildList {
                add("Market-only quote: no model has scored this prop, so the market no-vig price is the estimate")
                if (pick.odds == null) add("No price was legible; ${fmtOddsPlain(DEFAULT_IMPORT_PRICE)} is assumed")
                if (pick.line % 1.0 == 0.0) add("Integer line: a push returns the stake and is not priced here")
            }
            val playerId = "p-import-${Integer.toHexString(pick.playerName.lowercase().hashCode())}"
            players += PlayerEntity(playerId, pick.playerName, game.homeCode)
            quotes += OddsSnapshotEntity(
                id = "q-import-$hash",
                gameId = gameId,
                playerId = playerId,
                market = pick.market,
                book = book.displayName,
                line = pick.line,
                overOdds = price,
                underOdds = price,
                quoteTimeUtc = now,
                source = IMPORT_SOURCE,
            )
            predictions += PredictionEntity(
                id = predictionId,
                quoteId = "q-import-$hash",
                gameId = gameId,
                playerId = playerId,
                market = pick.market,
                modelVersion = IMPORT_MODEL,
                featureSchemaVersion = 1,
                featureCutoffUtc = now,
                pOver = 0.5,
                pPush = 0.0,
                pUnder = 0.5,
                status = Status.AVAILABLE,
                unavailableReasons = "",
                qualityFlags = flags.joinToString("|"),
                drivers = "",
                explanation = "",
                createdUtc = now,
            )
            added++
        }

        if (added > 0) {
            db.withTransaction {
                db.slateDao().insertGames(listOf(game))
                db.slateDao().insertPlayers(players)
                db.slateDao().insertQuotes(quotes)
                db.slateDao().insertPredictions(predictions)
                audit(
                    "slip_imported", gameId,
                    "$added picks read from a ${book.displayName} screenshot" + if (skipped > 0) ", $skipped already on the board" else "",
                )
            }
        }
        return ImportOutcome(added = added, skipped = skipped, gameLabel = "SLIP at ${game.homeCode}")
    }

    // ------------------------------------------------------------------------- sandbox

    fun sandbox(): Flow<SandboxUi> = combine(
        db.sandboxDao().observeState(SandboxStateEntity.AUTOPILOT_ID),
        db.sandboxDao().observeFormulas(),
        db.sandboxDao().observeTrades(),
    ) { state, formulas, trades ->
        SandboxUi(
            round = state?.round ?: 0,
            promotedId = state?.promotedId,
            promotedLabel = state?.promotedLabel,
            note = state?.note ?: "The loop has not run yet.",
            lastRunUtc = state?.lastRunUtc ?: 0L,
            autoRun = state?.autoRun ?: true,
            bankroll = state?.bankroll ?: 0.0,
            peak = state?.peak ?: 0.0,
            formulas = formulas.map { it.toUi() },
            trades = trades.map { it.toUi() },
        )
    }

    /** Where the Kaggle points model came from, for the Lab. Null when no artifact loaded. */
    fun championProvenance(): String? = feed.championProvenance()

    // --------------------------------------------------------------------- learning

    /**
     * The ledger, both fitted corrections, and what the settled props are saying about the
     * formula rules. The insights are recomputed from the stored results, so a new
     * settlement updates them without another write or a separate cache to go stale.
     */
    fun learning(): Flow<LearningUi> {
        val evidence = combine(
            db.sandboxDao().observeFormulas(),
            db.learningDao().observeSettled(LearningTrack.PROP),
        ) { formulas, settled -> formulas to settled }
        return combine(
            db.learningDao().observeTotals(),
            db.learningDao().observeCalibration(),
            db.learningDao().observeRecent(),
            evidence,
        ) { totals, fits, rows, pair ->
            val (formulas, settled) = pair
            val seed = formulas.firstOrNull { it.tier == Tier.LIVE }
                ?.params?.let { StrategyParams.parse(it) }
                ?: StrategyParams(minEdgePts = 2.0)
            val report = LossInsights.analyze(
                observations = settled.mapNotNull { it.toLossObservation() },
                seed = seed,
            )
            val byId = formulas.associateBy { it.id }
            LearningUi(
                props = totals.props,
                games = totals.games,
                settled = totals.settled,
                open = totals.open,
                staked = totals.staked,
                profit = totals.profit,
                calibration = listOf(LearningTrack.PROP, LearningTrack.GAME).map { track ->
                    val row = fits.firstOrNull { it.track == track }
                    if (row == null) {
                        CalibrationUi(track, 1.0, 0.0, 0, 0.0, 0.0, 0, 0L)
                    } else {
                        CalibrationUi(row.track, row.slope, row.intercept, row.sample, row.brierBefore, row.brierAfter, row.generation, row.updatedUtc)
                    }
                },
                recent = rows.take(30).map { it.toUi() },
                insights = report.findings.map { finding ->
                    val proposal = finding.proposal
                    LearningInsightUi(
                        id = finding.id,
                        kind = finding.kind,
                        title = finding.title,
                        detail = finding.detail,
                        advice = finding.advice,
                        proposalLabel = proposal?.label,
                        status = proposal?.let { candidate ->
                            when (val run = byId[candidate.id]) {
                                null -> "NOT TESTED YET"
                                else -> {
                                    val history = when (run.historyVerdict) {
                                        "PASS" -> " · HISTORY PASS"
                                        "FAIL" -> " · HISTORY FAIL"
                                        else -> ""
                                    }
                                    when {
                                        run.tier == Tier.LIVE -> "ON THE LIVE BOOK$history"
                                        run.tier == Tier.RETIRED -> "TESTED · RETIRED$history"
                                        run.verdict == "PASS" -> "TESTED · ROUND ${run.round} · PASSED$history"
                                        else -> "TESTED · ROUND ${run.round} · HELD$history"
                                    }
                                }
                            }
                        },
                    )
                },
                insightNote = report.note,
            )
        }
    }

    /**
     * The whole autonomous state, in the words a novice needs.
     *
     * It reads only what is stored: the board, the formula round, the learning totals, the
     * feed state and the optional history. Opening this flow never starts work, so a card
     * that says "running" is showing a real round rather than an optimistic animation.
     */
    fun autopilot(): Flow<AutopilotUi> = combine(
        bestBets(),
        sandbox(),
        db.learningDao().observeTotals(),
        feedState(),
        db.signalDao().observeHistoryCount(),
    ) { bets, box, totals, feed, history ->
        AutopilotStatus.build(bets, box, totals, feed, history)
    }

    /**
     * The first pass after launch.
     *
     * It fills whatever is missing without making the user choose an order: one price sync,
     * the first formula round when there has never been one, a learning update, and a
     * refresh of the live book. Every step is idempotent, and a failed network step is
     * reported in the audit trail rather than blocking the loop.
     */
    suspend fun bootstrap() = autoLock.withLock {
        val failures = mutableListOf<String>()
        fun failed(step: String, error: Throwable) {
            failures += "$step (${error::class.java.simpleName})"
        }
        runCatching { refreshFeed() }.onFailure { failed("feed", it) }
        if (sandboxRoundNumber() == 0) {
            runCatching { runSandboxRound() }.onFailure { failed("first round", it) }
        }
        runCatching { learn() }.onFailure { failed("learning", it) }
        runCatching { refreshLiveBook(place = autoRunEnabled()) }.onFailure { failed("live book", it) }
        if (failures.isNotEmpty()) {
            audit(
                "bootstrap_partial", "autopilot",
                "The first pass continued after: ${failures.joinToString(", ")}.",
            )
        }
        Unit
    }

    /**
     * The one button behind easy mode: sync, search, learn, then let the live formula take
     * today's picks. It cannot place a real bet and it cannot promote a formula that fails
     * the gate; it only runs the same loop the scheduler runs.
     */
    suspend fun runEverythingNow(): String = autoLock.withLock {
        val feedNote = runCatching { refreshFeed() }
            .fold(
                onSuccess = { report -> report.schedule.note },
                onFailure = { "The feed could not be refreshed: ${it.message ?: "unknown error"}." },
            )
        val roundNote = runCatching { runSandboxRound() }
            .fold(
                onSuccess = { outcome ->
                    "Round ${outcome.round}: ${outcome.ready} of ${outcome.considered} cleared the gate; " +
                        "${outcome.livePicks} live pick(s) taken."
                },
                onFailure = { "The formula round failed: ${it.message ?: "unknown error"}." },
            )
        val learnNote = runCatching { learn() }
            .fold(onSuccess = { it.note }, onFailure = { "The learning update failed: ${it.message ?: "unknown error"}." })
        runCatching { refreshLiveBook() }
        "$feedNote $roundNote $learnNote"
    }

    /**
     * One turn of self-learning, and nothing else.
     *
     * It writes the model's lean on every priced row to the ledger, settles what has
     * finished, and refits the two corrections once enough new decisions have arrived.
     * It never places a real bet, never spends money, and never touches a formula's place
     * on the live book: the promotion gate still owns that.
     */
    suspend fun learn(): LearningReport {
        val now = clock()
        val settled = settleLearning(now)
        val logged = logLearningBoard(now)
        val refit = refitCalibrations(now)
        val total = db.learningDao().settledCount()
        val note = if (logged == 0 && settled == 0 && refit == 0) {
            "The ledger is already up to date: $total settled bets, no new rows or fits."
        } else {
            "$logged new bets logged, $settled settled, $refit calibration(s) refitted. $total settled in total."
        }
        audit("learning_round", "ledger", note)
        return LearningReport(logged, settled, refit, note)
    }

    /**
     * Logs every priced prop and every positive game pick at its own line and price.
     *
     * Nothing is filtered by confidence. A low-confidence row is exactly the evidence the
     * calibration needs: a ledger of only the picks that looked good cannot tell whether
     * the model knows what "good" means.
     */
    private suspend fun logLearningBoard(now: Long): Int {
        val props = rawSlate().first().filter { it.available && !it.isFixture }
        val propBets = props.mapNotNull { prop ->
            val probs = prop.probabilities ?: return@mapNotNull null
            val market = prop.marketOver ?: return@mapNotNull null
            val line = prop.line ?: return@mapNotNull null
            val edge = Pricing.edgePoints(probs.overGivenNoPush, market)
            val side = if (edge >= 0.0) Side.OVER else Side.UNDER
            val odds = (if (side == Side.OVER) prop.overOdds else prop.underOdds) ?: return@mapNotNull null
            LearningBetEntity(
                id = "learn-p-${prop.id}-${side.name.lowercase()}",
                track = LearningTrack.PROP,
                market = prop.market,
                gameId = prop.gameId,
                refId = prop.id,
                subject = prop.playerName,
                homeCode = "",
                awayCode = "",
                side = side.name,
                line = line,
                odds = odds,
                pModel = probs.forSide(side),
                pPush = probs.push,
                pMarket = if (side == Side.OVER) market else 1.0 - market,
                edgePts = kotlin.math.abs(edge),
                confidence = confidenceOf(edge),
                stake = 1.0,
                outcome = null,
                profit = null,
                startUtc = prop.startUtc,
                loggedUtc = now,
                settledUtc = null,
            )
        }

        val board = gameBoard().first()
        val gameBets = board.picks.map { pick ->
            LearningBetEntity(
                id = "learn-g-" + Integer.toHexString(pick.key.hashCode()),
                track = LearningTrack.GAME,
                market = pick.market,
                gameId = pick.gameId,
                refId = pick.gameId,
                subject = pick.matchup,
                homeCode = pick.homeCode,
                awayCode = pick.awayCode,
                side = pick.sideKey,
                line = pick.line ?: 0.0,
                odds = pick.odds,
                pModel = pick.rawChance,
                pPush = pick.push,
                pMarket = pick.marketChance,
                edgePts = pick.edgePts,
                confidence = pick.edgePts?.let { confidenceOf(it) } ?: pick.confidence,
                stake = 1.0,
                outcome = null,
                profit = null,
                startUtc = pick.startUtc,
                loggedUtc = now,
                settledUtc = null,
            )
        }

        val items = propBets + gameBets
        if (items.isEmpty()) return 0
        return db.learningDao().insertBets(items).count { it != -1L }
    }

    /** Settles every ledger row whose game has a final score, and nothing else. */
    private suspend fun settleLearning(now: Long): Int {
        val open = db.learningDao().open()
        if (open.isEmpty()) return 0
        val results = HashMap<String, Double>()
        db.feedDao().results().forEach { results[it.predictionId] = it.actual }
        var settled = 0
        for (bet in open) {
            val outcome = when (bet.track) {
                LearningTrack.PROP -> {
                    val actual = results[bet.refId] ?: continue
                    val side = runCatching { Side.valueOf(bet.side) }.getOrNull() ?: continue
                    Settlement.outcome(side, bet.line, actual)
                }
                LearningTrack.GAME -> settleGame(bet) ?: continue
                else -> continue
            }
            val profit = Settlement.profit(outcome, bet.stake, bet.odds)
            db.learningDao().settle(bet.id, outcome.name, profit, now)
            settled++
        }
        return settled
    }

    private suspend fun settleGame(bet: LearningBetEntity): Outcome? {
        val external = db.feedDao().externalId(bet.refId) ?: return null
        val totals = db.feedDao().teamTotals(external)
        if (totals.size < 2) return null
        val home = totals.firstOrNull { it.teamAbbr == bet.homeCode }?.points ?: return null
        val away = totals.firstOrNull { it.teamAbbr == bet.awayCode }?.points ?: return null
        if (home <= 0 && away <= 0) return null
        return LearningSettlement.game(bet.market, bet.side, bet.line, home, away)
    }

    /**
     * Refits each model's Platt correction from its own settled decisions. A refit needs
     * both the minimum sample and at least fifty new results since the last one, so the
     * correction moves on evidence rather than on every sync.
     */
    private suspend fun refitCalibrations(now: Long): Int {
        var refit = 0
        for (track in listOf(LearningTrack.PROP, LearningTrack.GAME)) {
            val rows = db.learningDao().settled(track)
            if (rows.size < Calibration.MIN_SAMPLE) continue
            val prior = db.learningDao().calibration(track)?.toFit() ?: PlattFit.IDENTITY
            if (prior.sample > 0 && rows.size < prior.sample + 50) continue
            val points = rows.mapNotNull { row ->
                val room = 1.0 - row.pPush
                if (room <= 0.001) null else CalibrationPoint(
                    probability = (row.pModel / room).coerceIn(0.001, 0.999),
                    won = row.outcome == Outcome.WIN.name,
                )
            }
            val fit = Calibration.fit(points, prior) ?: continue
            db.learningDao().upsertCalibration(listOf(fit.toEntity(track, now)))
            audit(
                "calibration_refit", track,
                "${fit.sample} settled decisions, slope ${num2Plain(fit.slope)} " +
                    "shift ${num2Plain(fit.intercept)}, Brier ${num2Plain(fit.brierBefore)} -> ${num2Plain(fit.brierAfter)}",
            )
            refit++
        }
        return refit
    }

    private fun confidenceOf(edgePts: Double): String = when {
        kotlin.math.abs(edgePts) >= 2.0 -> "HIGH"
        kotlin.math.abs(edgePts) >= 1.0 -> "MEDIUM"
        else -> "LOW"
    }

    /**
     * What the promoted formula would take on the board that is live right now, plus the
     * board itself so the screen is still useful before any formula has been promoted.
     */
    fun bestBets(): Flow<BestBetsUi> = combine(slate(), sandbox(), gameBoard()) { props, box, games ->
        // A promoted formula may be a user's claim, which is not in the plain grid. Its
        // params are stored on the formula row, so resolve from there first.
        fun paramsOf(id: String?): StrategyParams? {
            if (id == null) return null
            box.formulas.firstOrNull { it.id == id }?.params?.let { text -> StrategyParams.parse(text) }?.let { return it }
            return Autopilot.grid.firstOrNull { it.id == id }
        }
        val liveParams = paramsOf(box.promotedId)
        val shadow = box.ready.firstOrNull() ?: box.ranked.firstOrNull()
        val shadowParams = paramsOf(shadow?.id)
        val params = liveParams ?: shadowParams
        val live = liveParams != null
        val available = props.filter { it.available }

        val onFormula = params?.let { p -> available.mapNotNull { prop -> pickFrom(prop, p, live) } }
            ?.sortedByDescending { it.edgePts }
            .orEmpty()

        // A thin board can leave a strict formula with almost nothing to say. Rather than an
        // empty screen, show the closest props to its bar, clearly marked as below it.
        val belowBar = when {
            available.isEmpty() -> emptyList()
            onFormula.size >= 4 -> emptyList()
            else -> available
                .filter { prop -> onFormula.none { it.prop.id == prop.id } }
                .mapNotNull { prop -> pickUnfiltered(prop, live) }
                .sortedByDescending { it.edgePts }
                .take(6 - onFormula.size)
        }

        BestBetsUi(
            picks = onFormula + belowBar,
            gamePicks = games.picks,
            strategyLabel = params?.label ?: box.promotedLabel,
            strategyId = params?.id ?: box.promotedId,
            live = live,
            round = box.round,
            note = box.note,
            ready = box.ready.size,
            considered = box.considered,
        )
    }

    /**
     * Every formula the parlay builder can be pointed at, with the picks it would take on
     * today's board and a written scope.
     *
     * The list is the sandbox's own ladder: the promoted formula first, then the five best
     * by measured profit. A board where the loop has not run yet falls back to three rungs of
     * the plain grid — broad, strict, and over-only — so the picker still demonstrates what a
     * formula changes instead of showing an empty control.
     */
    fun parlayStrategies(): Flow<ParlayBoardUi> = combine(slate(), sandbox(), gameBoard()) { props, box, games ->
        fun paramsOf(id: String): StrategyParams? =
            box.formulas.firstOrNull { it.id == id }?.params?.let { StrategyParams.parse(it) }
                ?: Autopilot.grid.firstOrNull { it.id == id }

        val available = props.filter { it.available }
        fun picksFor(params: StrategyParams, live: Boolean): List<BestPickUi> =
            available.mapNotNull { pickFrom(it, params, live) }.sortedByDescending { it.edgePts }

        val rows = buildList {
            box.promotedId?.let { id -> box.formulas.firstOrNull { it.id == id } }?.let { add(it) }
            box.formulas.sortedByDescending { it.profit }.take(5).forEach { row ->
                if (none { it.id == row.id }) add(row)
            }
        }
        val seeds = rows.mapNotNull { row ->
            val params = paramsOf(row.id) ?: return@mapNotNull null
            ParlaySeed(
                id = row.id,
                label = row.label,
                tier = row.tier,
                profit = row.profit,
                params = params,
                picks = picksFor(params, live = row.id == box.promotedId),
            )
        }.ifEmpty {
            // No round has run: three fixed rungs from the grid, labelled as practice.
            listOfNotNull(
                Autopilot.grid.firstOrNull { it.minEdgePts == 0.0 && it.sideFilter == SideFilter.BOTH },
                Autopilot.grid.firstOrNull { it.minEdgePts == 4.0 && it.sideFilter == SideFilter.BOTH },
                Autopilot.grid.firstOrNull { it.minEdgePts == 4.0 && it.sideFilter == SideFilter.OVER_ONLY },
            ).distinctBy { it.id }.map { params ->
                ParlaySeed(params.id, params.label, Tier.SANDBOX, 0.0, params, picksFor(params, live = false))
            }
        }

        val widest = seeds.minByOrNull { it.params.minEdgePts }
        val strategies = seeds.map { seed ->
            val neighbour = widest?.takeIf { it.id != seed.id }?.let { it.params to it.picks.size }
            ParlayStrategyUi(
                id = seed.id,
                label = seed.label,
                tier = seed.tier,
                params = seed.params,
                picks = seed.picks,
                scope = FormulaScope.describe(seed.params, seed.picks.size, available.size, neighbour),
                profit = seed.profit,
            )
        }
        ParlayBoardUi(
            strategies = strategies,
            gamePicks = games.picks,
            live = box.promotedId != null,
            note = box.note,
            round = box.round,
            priced = available.size,
        )
    }

    /**
     * One turn of the autonomous loop: gate every formula on a season it has never seen,
     * promote the best one that clears every check, then let the promoted formula trade the
     * board that is actually live.
     *
     * The season is generated outside the transaction; only the conclusions are written.
     */
    suspend fun runSandboxRound(): RoundOutcome = roundLock.withLock {
        val now = clock()
        val previous = db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)
        val round = (previous?.round ?: 0) + 1
        val seed = Season.BASE_SEED + round

        // Claims that cleared their own bench compete on exactly the same season as the
        // plain grid. A claim is an addition to the search space, not a bypass of it.
        val claimRows = db.claimDao().candidates()
        val claimGrid = claimRows.mapNotNull { row -> StrategyParams.parse(row.lens) }
        // The incumbent is the starting point for the next generation: its neighbourhood is
        // searched alongside the fixed grid, so the loop refines a strategy that is working
        // rather than only re-running the same candidates every round.
        val incumbentParams = previous?.promotedId?.let { id ->
            db.sandboxDao().formula(id)?.params?.let { StrategyParams.parse(it) }
                ?: Autopilot.grid.firstOrNull { it.id == id }
        }
        val mutants = incumbentParams?.let { Autopilot.mutations(it) }.orEmpty()

        // The losing record is an input to the search, not a promotion. Each finding that
        // implies a rule becomes a candidate on the same fresh season as every other row;
        // the gate still decides whether it trades anything.
        val settledProps = db.learningDao().settled(LearningTrack.PROP)
        val lessons = LossInsights.analyze(
            observations = settledProps.mapNotNull { it.toLossObservation() },
            seed = incumbentParams ?: StrategyParams(minEdgePts = 2.0),
        )
        val lessonGrid = lessons.proposals
        val challengerIds = lessonGrid.map { it.id }.toSet()
        val grid = (Autopilot.grid + claimGrid + mutants + lessonGrid).distinctBy { it.id }
        val claimNames = claimRows.associate { it.formulaId to it.name }

        // The historical dataset is a second gate for the learned candidates only. A
        // formula that earned a place in the simulation still has to pass on prices
        // someone actually paid; if the dataset is absent or too thin, the loop says so
        // rather than treating silence as proof.
        val historicalRows = historyRows()
        val historicalProps = TheoryBench.toProps(historicalRows)
        val historySkipNote = when {
            lessonGrid.isEmpty() -> ""
            historicalRows.isEmpty() -> "No historical dataset is imported, so the learned candidates were only simulated."
            historicalProps.size < GateThresholds.STANDARD.minBets ->
                "The historical gate was skipped: ${historicalProps.size} usable rows and " +
                    "${GateThresholds.STANDARD.minBets} are needed."
            else -> ""
        }
        val historyResult = if (lessonGrid.isNotEmpty() && historySkipNote.isEmpty()) {
            withContext(Dispatchers.Default) { TheoryBench.run(historicalRows, grid = lessonGrid) }
        } else {
            null
        }
        val historyChecks = historyResult?.candidates?.associateBy { it.id }.orEmpty()
        val historyFailed = historyChecks.filterValues { !it.gate.allPassed }.keys

        // Generating a season and running the grid over it is real CPU work; it must
        // never happen on the main thread, whichever caller asked for the round.
        val season = withContext(Dispatchers.Default) { Season.generate(seed = seed, endUtc = now) }
        val plan = withContext(Dispatchers.Default) {
            Autopilot.plan(
                season,
                incumbent = previous?.promotedId,
                grid = grid,
                challengers = challengerIds,
                vetoed = historyFailed,
            )
        }
        val promotedId = plan.promoted?.id
        val note = buildString {
            append(plan.note)
            if (lessonGrid.isNotEmpty()) {
                append(" ").append(lessonGrid.size).append(" candidate")
                    .append(if (lessonGrid.size == 1) "" else "s")
                    .append(" came from ").append(lessons.findings.size)
                    .append(" finding").append(if (lessons.findings.size == 1) "" else "s")
                    .append(" in the losing record.")
            }
            if (historyResult != null) {
                append(" History gate: ").append(historyResult.passed.size).append(" of ")
                    .append(lessonGrid.size).append(" learned candidate")
                    .append(if (lessonGrid.size == 1) "" else "s")
                    .append(" passed on ").append(historyResult.usable).append(" rows.")
            } else if (historySkipNote.isNotBlank()) {
                append(" ").append(historySkipNote)
            }
        }

        val rows = plan.candidates.map { candidate ->
            FormulaRunEntity(
                id = candidate.id,
                label = claimNames[candidate.id] ?: candidate.params.label,
                params = candidate.params.paramsText,
                tier = when {
                    candidate.id == promotedId -> Tier.LIVE
                    candidate.id == previous?.promotedId -> Tier.RETIRED
                    else -> Tier.SANDBOX
                },
                bets = candidate.stats.bets,
                wins = candidate.stats.wins,
                losses = candidate.stats.losses,
                pushes = candidate.stats.pushes,
                staked = candidate.stats.staked,
                profit = candidate.stats.profit,
                roi = candidate.stats.roi,
                tStat = candidate.stats.tStat,
                maxDrawdown = candidate.stats.maxDrawdown,
                finalBankroll = candidate.stats.finalBankroll,
                verdict = if (candidate.gate.allPassed) "PASS" else "FAIL",
                reasons = candidate.gate.failed.joinToString(";"),
                round = round,
                updatedUtc = now,
                historyVerdict = when (val check = historyChecks[candidate.id]) {
                    null -> "NOT RUN"
                    else -> if (check.gate.allPassed) "PASS" else "FAIL"
                },
                historyBets = historyChecks[candidate.id]?.stats?.bets ?: 0,
                historyRoi = historyChecks[candidate.id]?.stats?.roi ?: 0.0,
                historyNote = when {
                    candidate.id !in challengerIds -> ""
                    historyChecks[candidate.id] != null ->
                        historyChecks[candidate.id]?.gate?.failed?.joinToString(";").orEmpty()
                    else -> historySkipNote
                },
            )
        }

        val settled = settleLiveBook(now)
        val placed = plan.promoted?.params?.let { placeLivePicks(it, now) } ?: 0

        val trades = db.sandboxDao().allTrades()
        val profit = trades.sumOf { it.profit ?: 0.0 }
        val bankroll = SandboxEngine.STARTING_BANKROLL + profit
        val peak = maxOf(previous?.peak ?: 0.0, bankroll)

        db.withTransaction {
            db.sandboxDao().upsertFormulas(rows)
            db.sandboxDao().upsertState(
                SandboxStateEntity(
                    id = SandboxStateEntity.AUTOPILOT_ID,
                    round = round,
                    seed = seed,
                    lastRunUtc = now,
                    promotedId = promotedId,
                    promotedLabel = promotedId?.let { claimNames[it] } ?: plan.promoted?.params?.label,
                    note = note,
                    bankroll = bankroll,
                    peak = peak,
                    bets = trades.size,
                    profit = profit,
                    autoRun = previous?.autoRun ?: true,
                ),
            )
            audit(
                "sandbox_round", "round-$round",
                "${plan.candidates.size} formulas over ${season.size} simulated props; " +
                    "${plan.ready.size} cleared the gate. $note",
            )
            if (lessons.findings.isNotEmpty()) {
                audit("loss_lessons", "round-$round", lessons.note)
            }
            if (historyResult != null) {
                audit(
                    "formula_history_gate", "round-$round",
                    "${historyResult.passed.size} of ${lessonGrid.size} learned candidates passed on " +
                        "${historyResult.usable} imported rows; ${historyFailed.size} vetoed.",
                )
            } else if (lessonGrid.isNotEmpty() && historySkipNote.isNotBlank()) {
                audit("formula_history_gate", "round-$round", historySkipNote)
            }
            if (promotedId != null && promotedId != previous?.promotedId) {
                val name = claimNames[promotedId] ?: plan.promoted?.params?.label
                val learned = if (promotedId in challengerIds) {
                    " It was proposed by the loss analysis and passed every gate" +
                        (if (historyResult != null) ", including the historical dataset." else ".")
                } else {
                    ""
                }
                audit("formula_promoted", promotedId, "$name moved to the live book. $note$learned")
            }
            if (promotedId == null && previous?.promotedId != null) {
                audit("formula_retired", previous.promotedId, "the live formula stopped clearing the gate and returned to the sandbox")
            }
            if (placed > 0) audit("live_bet_placed", promotedId ?: "-", "$placed new picks taken on the live board")
            if (settled > 0) audit("live_bet_settled", promotedId ?: "-", "$settled live picks settled")
        }

        RoundOutcome(
            round = round,
            promotedId = promotedId,
            note = note,
            ready = plan.ready.size,
            considered = plan.candidates.size,
            livePicks = placed,
        )
    }

    suspend fun autoRunEnabled(): Boolean =
        db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)?.autoRun ?: true

    suspend fun sandboxRoundNumber(): Int = db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)?.round ?: 0

    suspend fun setAutoRun(enabled: Boolean) {
        val current = db.sandboxDao().state(SandboxStateEntity.AUTOPILOT_ID)
            ?: SandboxStateEntity().copy(round = 0, lastRunUtc = clock())
        db.sandboxDao().upsertState(current.copy(autoRun = enabled))
        audit(if (enabled) "autopilot_enabled" else "autopilot_paused", "autopilot", "scheduled rounds ${if (enabled) "resumed" else "paused"}")
    }

    /** Rewrites the bankroll, profit and peak after a refresh, leaving the round alone. */
    private suspend fun writeBookTotals(state: SandboxStateEntity) {
        val trades = db.sandboxDao().allTrades()
        val profit = trades.sumOf { it.profit ?: 0.0 }
        val bankroll = SandboxEngine.STARTING_BANKROLL + profit
        db.sandboxDao().upsertState(
            state.copy(
                profit = profit,
                bankroll = bankroll,
                peak = maxOf(state.peak, bankroll),
                bets = trades.size,
            ),
        )
    }

    /** Places the promoted formula's picks that are already sitting on the board, once each. */
    private suspend fun placeLivePicks(params: StrategyParams, now: Long): Int {
        val picks = Autopilot.livePicks(liveBoard(), params, limit = 12)
        if (picks.isEmpty()) return 0
        val items = picks.map { (prop, decision) ->
            SandboxTradeEntity(
                id = "${params.id}|${prop.id}",
                formulaId = params.id,
                predictionId = prop.id,
                playerName = prop.playerName,
                side = decision.side.name,
                line = prop.line,
                odds = decision.odds,
                stake = SandboxEngine.UNIT_STAKE,
                outcome = null,
                profit = null,
                startUtc = prop.startUtc,
                placedUtc = now,
                settledUtc = null,
                synthetic = prop.synthetic,
            )
        }
        return db.sandboxDao().insertTrades(items).count { it != -1L }
    }

    /** Settles live picks whose game has started and whose result is knowable. */
    private suspend fun settleLiveBook(now: Long): Int {
        val open = db.sandboxDao().openTrades()
        if (open.isEmpty()) return 0
        val board = liveBoard().associateBy { it.id }
        var settled = 0
        for (trade in open) {
            val prop = board[trade.predictionId] ?: continue
            val actual = prop.actual ?: continue
            // A synthetic prop's result is known to the oracle already, so the live book can
            // close it on the next round. A real quote waits for its game to start.
            if (prop.startUtc > now && !prop.synthetic) continue
            val side = runCatching { Side.valueOf(trade.side) }.getOrNull() ?: continue
            val outcome = Settlement.outcome(side, trade.line, actual)
            val profit = Settlement.profit(outcome, trade.stake, trade.odds)
            db.sandboxDao().settle(trade.id, outcome.name, profit, now)
            settled++
        }
        return settled
    }

    /**
     * The live board as the sandbox sees it. A fixture's result comes from the sandbox
     * oracle and is flagged synthetic; anything else only has an outcome once a real
     * result has been recorded against it.
     */
    private suspend fun liveBoard(): List<SimProp> {
        val now = clock()
        val rows = db.slateDao().observeSlate(now - 12 * PredictionGate.HOUR_MS).first()
        // A real prop settles from the box score the feed recorded. A result entered by
        // hand against a paper bet wins, because a person looked at it.
        val results = HashMap<String, Double>()
        db.feedDao().results().forEach { results[it.predictionId] = it.actual }
        db.slateDao().recordedResults().forEach { results[it.predictionId] = it.actual }
        return rows.mapNotNull { row ->
            if (row.status != Status.AVAILABLE) return@mapNotNull null
            val prop = SimProp(
                id = row.predictionId,
                playerName = row.playerName,
                line = row.line ?: return@mapNotNull null,
                overOdds = row.overOdds ?: return@mapNotNull null,
                underOdds = row.underOdds ?: return@mapNotNull null,
                pOver = row.pOver ?: return@mapNotNull null,
                pPush = row.pPush ?: return@mapNotNull null,
                pUnder = row.pUnder ?: return@mapNotNull null,
                startUtc = row.startUtc,
                actual = null,
                synthetic = row.source == FIXTURE_SOURCE,
                market = row.market,
            )
            prop.copy(actual = if (prop.synthetic) SandboxTruth.actualFor(prop) else results[row.predictionId])
        }
    }

    private fun pickFrom(prop: PropUi, params: StrategyParams, live: Boolean): BestPickUi? {
        val sim = prop.toSim() ?: return null
        val decision = SandboxEngine.decide(sim, params) ?: return null
        return measure(prop, decision.side, decision.odds, decision.edgePts, live = live, shadow = !live)
    }

    private fun pickUnfiltered(prop: PropUi, live: Boolean): BestPickUi? {
        val sim = prop.toSim() ?: return null
        val edge = sim.probabilities?.overGivenNoPush?.let { model ->
            sim.marketOver?.let { market -> Pricing.edgePoints(model, market) }
        } ?: return null
        val side = if (edge >= 0) Side.OVER else Side.UNDER
        val odds = sim.oddsFor(side)
        return measure(prop, side, odds, kotlin.math.abs(edge), live = live, shadow = !live)
    }

    private fun measure(
        prop: PropUi,
        side: Side,
        odds: Int,
        edgePts: Double,
        live: Boolean,
        shadow: Boolean,
    ): BestPickUi? {
        val probs = prop.probabilities ?: return null
        val decimal = runCatching { Odds.americanToDecimal(odds) }.getOrNull() ?: return null
        val pWin = probs.forSide(side)
        val ev = runCatching { Staking.expectedValue(pWin, probs.push, odds, 1.0) }.getOrNull() ?: return null
        val kelly = runCatching { Staking.kellyFraction(pWin, probs.push, odds) }.getOrNull() ?: 0.0
        val market = prop.marketOver ?: return null
        val marketProb = if (side == Side.OVER) market else 1.0 - market
        val explanation = BetNarrative.paragraph(
            BetNarrative.Input(
                player = prop.playerName,
                market = prop.market,
                matchup = prop.matchup,
                book = prop.book,
                line = prop.line ?: 0.0,
                side = side,
                odds = odds,
                modelProb = pWin,
                marketProb = marketProb,
                edgePts = edgePts,
                ev = ev,
                breakeven = 1.0 / decimal,
                kelly = kelly,
                stakeFraction = AppSettings.current.stakeFraction,
                gamesLogged = prop.drivers.firstOrNull { it.label == "games" }?.value?.toInt(),
                flags = prop.flags,
                live = live,
                isFixture = prop.isFixture,
            ),
        )
        return BestPickUi(
            prop = prop,
            side = side,
            odds = odds,
            edgePts = edgePts,
            modelProb = pWin,
            marketProb = marketProb,
            ev = ev,
            breakeven = 1.0 / decimal,
            kelly = kelly,
            live = live,
            shadow = shadow,
            explanation = explanation,
        )
    }
}

data class RoundOutcome(
    val round: Int,
    val promotedId: String?,
    val note: String,
    val ready: Int,
    val considered: Int,
    val livePicks: Int,
)

data class ImportOutcome(val added: Int, val skipped: Int, val gameLabel: String)

/** What one refresh of the live book did, without re-running the formula search. */
data class LiveBookOutcome(
    val placed: Int,
    val settled: Int,
    val promotedId: String?,
    val retiredId: String? = null,
)

/**
 * Real settlements a promoted formula needs before its live record can retire it. A
 * smaller sample says more about variance than about the formula.
 */
const val LIVE_REVIEW_MIN_BETS = 150

/** A screenshot quote is never a fixture: it is a real line someone photographed. */
const val IMPORT_SOURCE = "screenshot_import"

/** The version string written on any prediction that came from a screenshot. */
const val IMPORT_MODEL = "market-only-v0"

/** Used only when the screenshot carries no legible price. The UI says when it was used. */
const val DEFAULT_IMPORT_PRICE = -110

internal fun fmtOddsPlain(odds: Int) = if (odds > 0) "+$odds" else "$odds"

private fun num1(v: Double) = String.format(java.util.Locale.ROOT, "%.1f", v)

private fun num2(v: Double) = String.format(java.util.Locale.ROOT, "%+.2f", v)

private fun num2Plain(v: Double) = String.format(java.util.Locale.ROOT, "%.2f", v)

/** Only props with a line, both prices and a usable distribution can be simulated. */
private fun PropUi.toSim(): SimProp? {
    val line = line ?: return null
    val over = overOdds ?: return null
    val under = underOdds ?: return null
    val probs = probabilities ?: return null
    return SimProp(
        id = id,
        playerName = playerName,
        line = line,
        overOdds = over,
        underOdds = under,
        pOver = probs.over,
        pPush = probs.push,
        pUnder = probs.under,
        startUtc = startUtc,
        actual = null,
        synthetic = isFixture,
    )
}

private fun FormulaRunEntity.toUi() = FormulaUi(
    id = id, label = label, params = params, tier = tier, bets = bets, wins = wins, losses = losses,
    pushes = pushes, staked = staked, profit = profit, roi = roi, tStat = tStat,
    maxDrawdown = maxDrawdown, finalBankroll = finalBankroll, passed = verdict == "PASS",
    reasons = reasons.split(";").filter { it.isNotBlank() }, round = round,
    historyVerdict = historyVerdict, historyBets = historyBets, historyRoi = historyRoi,
    historyNote = historyNote,
)

private fun SandboxTradeEntity.toUi() = SandboxTradeUi(
    id = id, formulaId = formulaId, playerName = playerName,
    side = runCatching { Side.valueOf(side) }.getOrDefault(Side.OVER),
    line = line, odds = odds, stake = stake,
    outcome = outcome?.let { runCatching { Outcome.valueOf(it) }.getOrNull() },
    profit = profit, startUtc = startUtc, settledUtc = settledUtc, synthetic = synthetic,
)

private fun SlateRow.toUi(fit: PlattFit = PlattFit.IDENTITY): PropUi {
    val over = pOver
    val push = pPush
    val under = pUnder
    val raw = if (status == Status.AVAILABLE && over != null && push != null && under != null) {
        runCatching { OutcomeProbabilities(over, push, under) }.getOrNull()
    } else null
    val probs = raw?.let { Calibration.apply(fit, it) }
    val overPrice = overOdds
    val underPrice = underOdds
    val marketProb = if (overPrice != null && underPrice != null) {
        runCatching { Odds.noVigOver(overPrice, underPrice) }.getOrNull()
    } else null
    val reasons = unavailableReasons.split(",").filter { it.isNotBlank() }
        .mapNotNull { runCatching { RejectReason.valueOf(it).message }.getOrNull() }
    return PropUi(
        id = predictionId, playerName = playerName, teamCode = teamCode, gameId = gameId,
        matchup = "$awayCode at $homeCode", startUtc = startUtc, market = marketLabel(),
        book = book, line = line, overOdds = overOdds, underOdds = underOdds, quoteTimeUtc = quoteTimeUtc,
        probabilities = probs, marketOver = marketProb,
        edgePts = if (probs != null && marketProb != null) Pricing.edgePoints(probs.overGivenNoPush, marketProb) else null,
        reasons = reasons.ifEmpty { if (probs == null) listOf("This estimate failed its probability checks") else emptyList() },
        flags = qualityFlags.split("|").filter { it.isNotBlank() },
        drivers = drivers.split(";").mapNotNull { d ->
            val p = d.split("=")
            val v = p.getOrNull(1)?.toDoubleOrNull()
            if (p.size == 2 && v != null) Driver(p[0], v) else null
        },
        explanation = explanation, modelVersion = modelVersion, schemaVersion = schemaVersion,
        featureCutoffUtc = featureCutoffUtc, isFixture = source == FIXTURE_SOURCE,
    )
}

private fun SlateRow.marketLabel(): String = Markets.label(market)

/** Reads one ledger row back as a settled observation the loss analysis can use. */
private fun LearningBetEntity.toLossObservation(): LossObservation? {
    val parsedSide = Side.entries.firstOrNull { it.name == side } ?: return null
    val parsedOutcome = Outcome.entries.firstOrNull { it.name == outcome } ?: return null
    if (parsedOutcome != Outcome.WIN && parsedOutcome != Outcome.LOSS) return null
    return LossObservation(
        market = market,
        side = parsedSide,
        line = line,
        odds = odds,
        edgePts = edgePts,
        outcome = parsedOutcome,
        startUtc = startUtc,
    )
}

private fun LearningBetEntity.toUi(): LearningBetUi {
    val marketLabel = if (track == LearningTrack.GAME) GameMarkets.label(market) else Markets.label(market)
    val lineText = if (line % 1.0 == 0.0) "%.0f".format(line) else "%.1f".format(line)
    val sideText = when (side) {
        LearningSide.HOME -> "home"
        LearningSide.AWAY -> "away"
        else -> side.lowercase()
    }
    val label = if (track == LearningTrack.GAME) {
        "$subject · $sideText $lineText $marketLabel"
    } else {
        "$subject $sideText $lineText $marketLabel"
    }
    val detail = buildString {
        append(confidence.lowercase()).append(" confidence · model ")
        append("%.0f".format(pModel * 100)).append('%')
        pMarket?.let { append(" · book ").append("%.0f".format(it * 100)).append('%') }
        edgePts?.let { append(" · ").append("%+.1f".format(it)).append(" pts") }
    }
    return LearningBetUi(
        id = id,
        label = label,
        detail = detail,
        odds = odds,
        pModel = pModel,
        edgePts = edgePts,
        confidence = confidence,
        outcome = outcome,
        profit = profit,
    )
}

private fun BetRow.toUi() = BetUi(
    id = betId, playerName = playerName, teamCode = teamCode, side = Side.valueOf(side), line = line,
    odds = quotedOdds, stake = stakeUnits, decidedUtc = decidedUtc, actual = actual,
    outcome = outcome?.let { Outcome.valueOf(it) }, profit = profitUnits, settledUtc = settledUtc,
    market = market, book = book,
)
