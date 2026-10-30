package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.billing.Feature
import com.propslab.billing.TierStore
import com.propslab.data.AvailabilityStatus
import com.propslab.fantasy.FantasyAssets
import com.propslab.fantasy.FantasyCall
import com.propslab.fantasy.FantasyDesk
import com.propslab.fantasy.FantasyDeskUi
import com.propslab.fantasy.FantasyFeedApi
import com.propslab.fantasy.FantasyGameLog
import com.propslab.fantasy.FantasyGameLogApi
import com.propslab.fantasy.FantasyInjury
import com.propslab.fantasy.FantasyLine
import com.propslab.fantasy.FantasyPlayer
import com.propslab.fantasy.FantasySport
import com.propslab.fantasy.FantasyWatchlist
import com.propslab.fantasy.FantasyWatchlistStore
import com.propslab.fantasy.SplitHitRate
import com.propslab.fantasy.lineHitRate
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PremiumGate
import com.propslab.ui.components.RemoteImage
import com.propslab.ui.fmtAgo
import com.propslab.ui.fmtDay
import com.propslab.ui.initials
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * One sport's desk, plus the watchlist that survives a refresh.
 *
 * The view model owns the cache and the rebuild, not the screen: switching sports back and
 * forth is instant after the first read, and a refresh that lands after the user has moved on
 * is filed under the sport it was requested for rather than painted over the new one.
 */
/** The state of one expanded card's deeper log. */
data class PlayerLogUi(
    val open: Boolean = false,
    val loading: Boolean = false,
    val log: FantasyGameLog? = null,
    val error: String? = null,
    /** ESPN's season name for the split being shown; null follows the regular-season default. */
    val season: String? = null,
)

class FantasyViewModel(private val store: FantasyWatchlistStore) : ViewModel() {

    private data class Cache(
        val lines: List<FantasyLine>,
        val injuries: List<FantasyInjury>,
        val note: String,
        val fetchedUtc: Long,
    )

    var sport by mutableStateOf(FantasySport.BASKETBALL)
        private set
    var watchlist by mutableStateOf(store.load())
        private set
    var desk by mutableStateOf(FantasyDeskUi())
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    /** Keyed by `sport:player-key`, so the two desks cannot collide. */
    var logs by mutableStateOf<Map<String, PlayerLogUi>>(emptyMap())
        private set

    private val cache = HashMap<FantasySport, Cache>()

    fun select(next: FantasySport) {
        if (next == sport) return
        sport = next
        // The previous sport's read note belongs to the previous desk.
        message = null
        rebuild()
    }

    /**
     * Reads the sport's box scores. A cache younger than five minutes is reused unless the
     * user asked for a refresh, because the underlying feeds change only when a game ends.
     */
    fun refresh(force: Boolean = false) {
        if (!TierStore.allows(Feature.FANTASY_DESK) || busy) return
        val target = sport
        val cached = cache[target]
        if (!force && cached != null && System.currentTimeMillis() - cached.fetchedUtc < 5 * 60_000L) {
            rebuild()
            return
        }
        busy = true
        message = null
        viewModelScope.launch {
            val result = runCatching {
                FantasyFeedApi.fetch(sport = target, nowUtc = System.currentTimeMillis())
            }
            busy = false
            result.fold(
                onSuccess = { feed ->
                    cache[target] = Cache(feed.lines, feed.injuries, feed.note, feed.fetchedUtc)
                    message = feed.note
                    if (sport == target) rebuild()
                },
                onFailure = {
                    message = "The desk could not read ESPN just now. The last read is still on screen."
                },
            )
        }
    }

    fun toggle(player: FantasyPlayer) {
        val key = FantasyWatchlist.key(sport, player.key)
        watchlist = FantasyWatchlist.toggle(watchlist, key)
        store.save(watchlist)
        rebuild()
    }

    fun logState(player: FantasyPlayer): PlayerLogUi? = logs[logKey(player)]

    /**
     * Opens one player's deeper log, or closes it. The endpoint is called only on the first
     * open; a second open is the cached answer. A missing athlete id is a stated refusal,
     * not a silent no-op.
     */
    fun toggleLog(player: FantasyPlayer) {
        val key = logKey(player)
        val current = logs[key] ?: PlayerLogUi()
        if (current.open) {
            logs = logs + (key to current.copy(open = false))
            return
        }
        if (current.log != null || current.loading) {
            logs = logs + (key to current.copy(open = true))
            return
        }
        val athleteId = player.lines.firstOrNull { it.athleteId.isNotBlank() }?.athleteId
        if (athleteId == null) {
            logs = logs + (
                key to current.copy(
                    open = true,
                    error = "ESPN did not publish an athlete id for this player, so no deeper log exists to read.",
                )
            )
            return
        }
        logs = logs + (key to current.copy(open = true, loading = true, error = null))
        viewModelScope.launch {
            // The API hands back a Result already; unwrap it inside runCatching so the fold
            // below sees one failure shape rather than a Result<Result<…>>.
            val result = runCatching {
                FantasyGameLogApi.fetch(player.sport, athleteId, player.name).getOrThrow()
            }
            val latest = logs[key] ?: current
            val next = result.fold(
                onSuccess = { log ->
                    latest.copy(
                        loading = false,
                        log = log,
                        error = if (log.entries.isEmpty()) {
                            log.note.ifBlank { "ESPN returned no games for this player." }
                        } else {
                            null
                        },
                    )
                },
                onFailure = {
                    latest.copy(
                        loading = false,
                        error = it.message ?: "The ESPN game log could not be read.",
                    )
                },
            )
            logs = logs + (key to next)
        }
    }

    /** Switches one card's log to a split ESPN already returned; no second request. */
    fun selectLogSeason(player: FantasyPlayer, seasonName: String) {
        val key = logKey(player)
        val current = logs[key] ?: return
        logs = logs + (key to current.copy(season = seasonName))
    }

    private fun logKey(player: FantasyPlayer): String = "${player.sport.slug}:${player.key}"

    private fun rebuild() {
        val cached = cache[sport]
        desk = FantasyDesk.build(
            sport = sport,
            lines = cached?.lines.orEmpty(),
            injuries = cached?.injuries.orEmpty(),
            watchlist = watchlist,
            fetchedUtc = cached?.fetchedUtc ?: 0L,
            note = cached?.note.orEmpty(),
        )
    }
}

/**
 * The fantasy desk: what the box scores say, in form terms instead of promises.
 *
 * It is premium because it is a convenience built on the same free, keyless public feeds the
 * rest of the app reads. The free build sees a placeholder and spends no request.
 */
@Composable
fun FantasyScreen(onBack: () -> Unit, onOpenPremium: () -> Unit, onOpenCapture: () -> Unit) {
    val context = LocalContext.current
    val repo = rememberRepo()
    val vm: FantasyViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FantasyViewModel(FantasyWatchlistStore(context)) }
        },
    )
    val premium = TierStore.allows(Feature.FANTASY_DESK)
    val league by remember(vm.sport) { repo.observeFantasyLeague(vm.sport) }
        .collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(premium, vm.sport) {
        if (premium) vm.refresh()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
            bottom = 56.dp,
        ),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "\u2039  Lab",
                    Modifier.clickable(role = Role.Button, onClick = onBack).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Fantasy desk",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.displayMedium, color = Palette.Chalk,
                    )
                    Badge("PREMIUM", Palette.Caution, filled = true)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Capture your lineup and your opponent's, then get a short start/sit call for every " +
                        "player. Recent form and injuries come from ESPN's public box scores; no account and no key.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        if (!premium) {
            item {
                PremiumGate(
                    feature = Feature.FANTASY_DESK,
                    title = "Start/sit, form and the injury wire",
                    body = "Track players across refreshes, see their last games scored for fantasy, " +
                        "get a form call with the reasons behind it, and read the same league injury " +
                        "report the clubs publish.",
                    onOpenPremium = onOpenPremium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item { FantasyFooter(desk = FantasyDeskUi(), premium = false) }
            return@LazyColumn
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FantasySport.entries.forEach { option ->
                    Box(Modifier.weight(1f)) {
                        ChoiceChip(
                            label = option.label,
                            selected = option == vm.sport,
                            onClick = { vm.select(option) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            FantasyStatus(
                desk = vm.desk,
                busy = vm.busy,
                message = vm.message,
                onRefresh = { vm.refresh(force = true) },
            )
        }

        item {
            FantasyLeagueSection(
                data = league,
                desk = vm.desk,
                onCapture = onOpenCapture,
            )
        }

        if (vm.busy && !vm.desk.hasData) {
            item {
                Panel(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        "Reading ESPN's public box scores…",
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                    )
                }
            }
        }

        if (vm.desk.hasData) {
            item {
                DeskSection(
                    title = "MY LINEUP",
                    detail = if (vm.desk.tracked.isEmpty()) {
                        "Nobody tracked yet. Tap the star on a player to keep him here."
                    } else {
                        "${vm.desk.tracked.size} tracked · tipped first below"
                    },
                )
            }
            if (vm.desk.tracked.isEmpty()) {
                item {
                    Panel(Modifier.padding(horizontal = 20.dp, vertical = 2.dp), tone = Palette.Raised) {
                        Text(
                            "Tracking is local to this phone. It survives a refresh and can be " +
                                "cleared one star at a time.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                    }
                }
            } else {
                items(vm.desk.tracked, key = { "tracked-${it.key}" }) { player ->
                    PlayerCard(
                        player = player,
                        tracked = true,
                        onToggle = { vm.toggle(player) },
                        log = vm.logState(player),
                        onToggleLog = { vm.toggleLog(player) },
                        onSelectLogSeason = { vm.selectLogSeason(player, it) },
                    )
                }
            }

            item {
                DeskSection(
                    title = "TOP FORM",
                    detail = "The rest of the desk, ranked by a weighted recent average.",
                )
            }
            items(vm.desk.trending, key = { "trend-${it.key}" }) { player ->
                PlayerCard(
                    player = player,
                    tracked = false,
                    onToggle = { vm.toggle(player) },
                    log = vm.logState(player),
                    onToggleLog = { vm.toggleLog(player) },
                    onSelectLogSeason = { vm.selectLogSeason(player, it) },
                )
            }

            item {
                DeskSection(
                    title = "INJURY WIRE",
                    detail = "Designations from the league's own report, newest first.",
                )
            }
            if (vm.desk.injuries.isEmpty()) {
                item {
                    Panel(Modifier.padding(horizontal = 20.dp, vertical = 2.dp), tone = Palette.Raised) {
                        Text(
                            "No designations came back with this read. That is not a clean bill of " +
                                "health for every player; it means the report had nothing to quote.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                    }
                }
            } else {
                items(vm.desk.injuries, key = { "injury-${it.playerKey}-${it.status}" }) { injury ->
                    InjuryRow(injury)
                }
            }

            item {
                DeskSection(
                    title = "LINEUP TIPS",
                    detail = "Generic form advice. No league roster or scoring setting is connected.",
                )
            }
            item {
                Panel(Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
                    vm.desk.lineupTips.forEach { tip ->
                        Text(
                            "•  $tip",
                            Modifier.padding(vertical = 5.dp),
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                    }
                }
            }
        }

        item { FantasyFooter(desk = vm.desk, premium = true) }
    }
}

@Composable
private fun FantasyStatus(
    desk: FantasyDeskUi,
    busy: Boolean,
    message: String?,
    onRefresh: () -> Unit,
) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ESPN · PUBLIC BOX SCORES", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Badge(
                when {
                    busy -> "READING"
                    desk.fetchedUtc > 0L -> "UPDATED"
                    else -> "NOT READ"
                },
                when {
                    busy -> Palette.Caution
                    desk.fetchedUtc > 0L -> Palette.Gain
                    else -> Palette.Faint
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (desk.fetchedUtc > 0L) fmtAgo(desk.fetchedUtc) else "This sport has not been read yet.",
            style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
        )
        val note = message ?: desk.note
        if (note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
        Spacer(Modifier.height(10.dp))
        ActionButton(
            if (busy) "Reading…" else "Refresh the desk",
            onRefresh,
            enabled = !busy,
            tone = Palette.Over,
        )
    }
}

@Composable
private fun DeskSection(title: String, detail: String) {
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        Spacer(Modifier.height(3.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
    }
}

@Composable
private fun PlayerCard(
    player: FantasyPlayer,
    tracked: Boolean,
    onToggle: () -> Unit,
    log: PlayerLogUi?,
    onToggleLog: () -> Unit,
    onSelectLogSeason: (String) -> Unit,
) {
    val advice = FantasyDesk.advise(player)
    val athleteId = player.lines.firstOrNull { it.athleteId.isNotBlank() }?.athleteId.orEmpty()
    val hasAthleteId = athleteId.isNotBlank()
    val headshot = if (hasAthleteId) FantasyAssets.headshotUrl(player.sport, athleteId) else ""
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                    .background(Palette.Raised).border(1.dp, Palette.Hairline, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                RemoteImage(
                    url = headshot,
                    contentDescription = "${player.name} headshot",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp)),
                    fallback = {
                        Text(initials(player.name), style = MaterialTheme.typography.labelMedium, color = Palette.Dust)
                    },
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.titleLarge, color = Palette.Chalk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(player.team, player.role)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { player.sport.label },
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
            }
            Star(
                tracked = tracked,
                player = player.name,
                onToggle = onToggle,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(advice.call.label.uppercase(), callColor(advice.call), filled = true)
            Spacer(Modifier.width(10.dp))
            Text(
                "%.1f projected".format(player.projection),
                style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${player.games} game${if (player.games == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(advice.headline, style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
        if (player.lines.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Last " + player.lines.take(4).joinToString(" · ") { "%.0f".format(it.fantasyPoints) },
                style = MaterialTheme.typography.labelSmall, color = Palette.Over,
            )
            Text(
                player.lines.first().statLine,
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        advice.tips.take(2).forEach { tip ->
            Spacer(Modifier.height(6.dp))
            Text(tip, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
        if (hasAthleteId) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (log?.open == true) "Hide game log" else "Open game log",
                    Modifier.clickable(role = Role.Button, onClick = onToggleLog).padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = Palette.Over,
                )
                if (log?.loading == true) {
                    Spacer(Modifier.width(8.dp))
                    Text("reading…", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                }
            }
            if (log?.open == true) {
                Spacer(Modifier.height(4.dp))
                when {
                    log.loading -> Text(
                        "Reading ESPN's athlete game log…",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    log.log != null && log.log.entries.isNotEmpty() -> GameLogPanel(
                        log.log,
                        selected = log.season,
                        onSelect = onSelectLogSeason,
                    )
                    log.log != null -> Text(
                        log.log.note.ifBlank { log.error ?: "ESPN returned no games for this player." },
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    log.error != null -> Text(
                        log.error,
                        style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                    )
                }
            }
        }
    }
}

/**
 * The expanded deeper log: the last dozen games, newest first, in the card's own voice.
 * The split chips switch between the season blocks ESPN already returned, so selecting
 * postseason or preseason never triggers a second request.
 */
@Composable
private fun GameLogPanel(
    log: FantasyGameLog,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    val chosen = selected ?: log.season
    val split = log.seasons.firstOrNull { it.name == chosen }
        ?: log.seasons.firstOrNull { it.name == log.season }
    val entries = split?.entries ?: log.entries
    val splitName = split?.let { it.name.ifBlank { it.label } } ?: log.season
    var lineText by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Palette.Ink).padding(12.dp),
    ) {
        if (log.seasons.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                log.seasons.forEach { season ->
                    ChoiceChip(
                        label = season.label.ifBlank { season.name },
                        selected = season.name == (split?.name ?: chosen),
                        onClick = { onSelect(season.name) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            if (entries.isEmpty()) {
                "ESPN returned no games for " + (splitName.ifBlank { "this split" }) + "."
            } else {
                "ESPN game log · $splitName · ${entries.size} " +
                    "game${if (entries.size == 1) "" else "s"}"
            },
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        if (entries.isNotEmpty()) {
            val average = split?.averagePoints
            val best = split?.bestPoints
            Spacer(Modifier.height(3.dp))
            Text(
                if (average != null && best != null) {
                    "%.1f avg · %.1f best across ${split?.scoredGames ?: entries.size} " +
                        "scored game${if ((split?.scoredGames ?: entries.size) == 1) "" else "s"}"
                } else {
                    "No scorable games in this split; the rows below carry no fantasy total."
                },
                style = MaterialTheme.typography.labelMedium, color = Palette.Chalk,
            )
            Spacer(Modifier.height(10.dp))
            Text("HIT RATE VS A LINE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(5.dp))
            HitRateField(value = lineText, onChange = { lineText = it })
            val line = lineText.trim().toDoubleOrNull()
            val validLine = line != null && line.isFinite() && line >= 0.0
            val rate = if (line != null && line.isFinite() && line >= 0.0) {
                lineHitRate(entries, line)
            } else {
                null
            }
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    lineText.isBlank() -> "Enter a line to see how often this split finished above or below it."
                    !validLine -> "The line must be a non-negative number."
                    else -> hitRateText(rate)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (validLine) Palette.Chalk else Palette.Dust,
            )
        }
        entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = Palette.Hairline, modifier = Modifier.padding(vertical = 7.dp))
            Row(Modifier.fillMaxWidth().padding(top = if (index == 0) 8.dp else 0.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (entry.playedUtc > 0L) fmtDay(entry.playedUtc) else "Date unavailable",
                        style = MaterialTheme.typography.labelMedium, color = Palette.Chalk,
                    )
                    Text(
                        listOf(entry.homeAway, entry.opponent)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { "Opponent unavailable" },
                        style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                    )
                    Text(
                        entry.statLine,
                        style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        entry.fantasyPoints?.let { "%.1f".format(it) } ?: "—",
                        style = MaterialTheme.typography.titleMedium, color = Palette.Over,
                    )
                    Text(
                        listOf(entry.result, entry.score)
                            .filter { it.isNotBlank() }
                            .joinToString(" "),
                        style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                    )
                }
            }
        }
    }
}

/** A compact numeric field, matching the parlay builder's unit field. */
@Composable
private fun HitRateField(value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Palette.Raised)
            .border(1.dp, Palette.Hairline, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Line", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = Palette.Chalk),
            cursorBrush = SolidColor(Palette.Over),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("e.g. 22.5", style = MaterialTheme.typography.titleMedium, color = Palette.Faint)
                }
                inner()
            },
        )
        if (value.isNotEmpty()) {
            Text(
                "Clear",
                Modifier.clickable(role = Role.Button) { onChange("") }
                    .padding(start = 10.dp, top = 6.dp, bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
            )
        }
    }
}

private fun sharePercent(share: Double?): String =
    if (share == null) "—" else "%.0f%%".format(share * 100)

/** Names the split's record against the line, keeping pushes out of the denominator. */
private fun hitRateText(rate: SplitHitRate?): String {
    if (rate == null || rate.scored == 0) {
        return "This split has no scorable games, so it has no hit rate."
    }
    if (rate.decided == 0) {
        return "All ${rate.push} scored game${if (rate.push == 1) "" else "s"} landed exactly on the line."
    }
    return buildString {
        append("Over ${rate.over} (${sharePercent(rate.overShare)}) · ")
        append("Under ${rate.under} (${sharePercent(rate.underShare)}) of ${rate.decided} decided")
        if (rate.push > 0) {
            append(" · ${rate.push} push${if (rate.push == 1) "" else "es"}")
        }
        append(". Pushes are not counted as losses.")
    }
}

@Composable
private fun Star(tracked: Boolean, player: String, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = if (tracked) "Stop tracking $player" else "Track $player" }
            .clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (tracked) "\u2605" else "\u2606",
            style = MaterialTheme.typography.titleLarge,
            color = if (tracked) Palette.Caution else Palette.Faint,
        )
    }
}

@Composable
private fun InjuryRow(injury: FantasyInjury) {
    val tone = when (injury.status) {
        AvailabilityStatus.OUT, AvailabilityStatus.DOUBTFUL -> Palette.Loss
        AvailabilityStatus.QUESTIONABLE, AvailabilityStatus.GAME_TIME -> Palette.Caution
        AvailabilityStatus.PROBABLE -> Palette.Gain
        else -> Palette.Dust
    }
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), tone = Palette.Raised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                injury.playerName,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Badge(AvailabilityStatus.label(injury.status).uppercase(), tone)
        }
        Text(
            listOf(injury.team, fmtAgo(injury.observedUtc)).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        if (injury.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                injury.note,
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FantasyFooter(desk: FantasyDeskUi, premium: Boolean) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text(                    "Scoring: basketball is PTS + 1.2 REB + 1.5 AST + 3 3PM; football is half-PPR. " +
                "The league calls use your captured slots, the imported projection and ESPN's public form. " +
                "They do not know your league's exact scoring or the opponent's defensive matchup. It is " +
                "advice about form, not a forecast of a game, and it places nothing.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        if (premium && desk.fetchedUtc > 0L) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Headlines and box scores are ESPN's public data and remain ESPN's. The app shows " +
                    "the line it read and links out; it does not mirror articles.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

private fun callColor(call: FantasyCall): androidx.compose.ui.graphics.Color = when (call) {
    FantasyCall.START -> Palette.Gain
    FantasyCall.WATCH -> Palette.Caution
    FantasyCall.SIT -> Palette.Under
    FantasyCall.OUT -> Palette.Loss
}
