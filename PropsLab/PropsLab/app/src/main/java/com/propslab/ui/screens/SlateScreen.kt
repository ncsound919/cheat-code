package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.data.GameGroup
import com.propslab.data.GameLinesUi
import com.propslab.data.GameMarketUi
import com.propslab.data.GameSideUi
import com.propslab.data.PlaceResult
import com.propslab.data.PracticeLedger
import com.propslab.data.PropUi
import com.propslab.data.PropsRepository
import com.propslab.domain.Pricing
import com.propslab.domain.Side
import com.propslab.domain.Sports
import com.propslab.feed.GameMarkets
import com.propslab.settings.AppSettings
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.FixtureNotice
import com.propslab.ui.components.LiveBadge
import com.propslab.ui.components.PlainLink
import com.propslab.ui.components.SplitBar
import com.propslab.ui.components.TeamMark
import com.propslab.ui.fmtDay
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.fmtPct
import com.propslab.ui.fmtSigned
import com.propslab.ui.fmtTime
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

data class SlateState(
    val games: List<GameGroup> = emptyList(),
    val lines: Map<String, GameLinesUi> = emptyMap(),
    val loaded: Boolean = false,
) {
    val total: Int get() = games.sumOf { it.props.size }
    val scored: Int get() = games.sumOf { g -> g.props.count { it.available } }
    val hasFixture: Boolean get() = games.any { g -> g.props.any { it.isFixture } }
    val model: String? get() = games.firstOrNull()?.props?.firstOrNull()?.modelVersion

    val overLeans: Int get() = games.sumOf { g -> g.props.count { it.available && it.lean == Side.OVER } }
    val underLeans: Int get() = games.sumOf { g -> g.props.count { it.available && it.lean == Side.UNDER } }

    /** Priced props the model will actually trade, so this number matches the header count. */
    val priced: Int get() = games.sumOf { g -> g.props.count { it.available && it.overOdds != null && it.underOdds != null } }

    val averageHold: Double?
        get() {
            val holds = games.flatMap { it.props }.mapNotNull { p ->
                val over = p.overOdds
                val under = p.underOdds
                if (over != null && under != null) runCatching { Pricing.hold(over, under) }.getOrNull() else null
            }
            return if (holds.isEmpty()) null else holds.average()
        }

    val biggestEdge: PropUi?
        get() = games.flatMap { it.props }
            .filter { it.available && it.edgePts != null }
            .maxByOrNull { abs(it.edgePts ?: 0.0) }
}

class SlateViewModel(private val repo: PropsRepository) : ViewModel() {
    val state: StateFlow<SlateState> = combine(repo.slate(), repo.gameLines()) { props, lines ->
        SlateState(
            games = props.groupBy { it.gameId }.map { (id, ps) ->
                GameGroup(id, ps.first().matchup, ps.first().startUtc, ps, ps.first().sport)
            },
            lines = lines.associateBy { it.gameId },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SlateState())
    /** What the practice account can still stake, so the bet sheet never offers more. */
    val available: StateFlow<Double> = repo.bets().map { bets ->
        PracticeLedger.available(
            bets.filter { !it.isOpen }.sumOf { it.profit ?: 0.0 },
            bets.filter { it.isOpen }.sumOf { it.stake },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeLedger.STARTING_BANKROLL)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun place(propId: String, side: Side, stake: Double) {
        viewModelScope.launch {
            _messages.emit(
                when (val result = repo.placeBet(propId, side, stake)) {
                    PlaceResult.Placed -> "Paper bet recorded. It is in your Bets tab."
                    PlaceResult.Duplicate -> "You already have a paper bet on the ${side.label} for this prop."
                    PlaceResult.Unavailable -> "This estimate is unavailable, so no bet was recorded."
                    is PlaceResult.NotEnoughBankroll ->
                        "Only ${"%.2f".format(result.available)} practice units are available, so no bet was recorded."
                },
            )
        }
    }
}

@Composable
fun SlateScreen(onOpen: (String) -> Unit, onOpenPlain: () -> Unit) {
    val repo = rememberRepo()
    val vm: SlateViewModel = viewModel(factory = viewModelFactory { initializer { SlateViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    var byEdge by rememberSaveable { mutableStateOf(false) }
    // Null is the whole board. The filter is saved across rotation so the league a reader
    // picked survives a re-render.
    var league by rememberSaveable { mutableStateOf<String?>(null) }
    val games = remember(state.games, byEdge, league) {
        ordered(state.games.filter { league == null || it.sport == league }, byEdge)
    }
    val easy = AppSettings.current.easyMode
    val available by vm.available.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var betting by remember { mutableStateOf<PropUi?>(null) }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = WindowInsets.statusBars.asPaddingValues().let {
            PaddingValues(top = it.calculateTopPadding() + 12.dp, bottom = 120.dp)
        }) {
        item { SlateHeader(state, league, { league = it }, byEdge, easy, { byEdge = it }, onOpenPlain) }
        if (state.loaded && state.total == 0) {
            item { EmptySlate(state) }
        }
        if (state.loaded && state.total > 0 && games.isEmpty()) {
            item { EmptyLeague(league) }
        }
        games.forEach { game ->
            item(key = "game-${game.id}") { GameHeader(game) }
            state.lines[game.id]?.let { lines ->
                item(key = "lines-${game.id}") { GameLinesCard(lines) }
            }
            items(game.props, key = { it.id }) { prop ->
                PropRow(prop, onClick = { onOpen(prop.id) }, onBet = { betting = prop })
                HorizontalDivider(Modifier.padding(start = 74.dp), color = Palette.Hairline)
            }
        }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }

    betting?.let { prop ->
        PropBetSheet(
            prop = prop,
            suggestedSide = prop.lean ?: Side.OVER,
            available = available,
            onDismiss = { betting = null },
            onPlace = { side, stake -> vm.place(prop.id, side, stake) },
        )
    }
}

/** The slate keeps game order by default. Edge order surfaces what the model disagrees with most. */
private fun ordered(games: List<GameGroup>, byEdge: Boolean): List<GameGroup> {
    if (!byEdge) return games
    return games
        .map { game -> game.copy(props = game.props.sortedByDescending { abs(it.edgePts ?: -1.0) }) }
        .sortedByDescending { game -> game.props.maxOfOrNull { abs(it.edgePts ?: 0.0) } ?: 0.0 }
}

@Composable
private fun SlateHeader(
    state: SlateState,
    league: String?,
    onLeague: (String?) -> Unit,
    byEdge: Boolean,
    easy: Boolean,
    onOrder: (Boolean) -> Unit,
    onOpenPlain: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tonight", Modifier.weight(1f), style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
            if (state.total > 0 && !state.hasFixture) {
                LiveBadge(if (state.loaded) "LIVE" else "…")
                Spacer(Modifier.width(10.dp))
            }
            PlainLink(onOpenPlain)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "${fmtDay(System.currentTimeMillis())}. ${state.total} props on the board, ${state.scored} priced.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        if (!easy) {
            state.model?.let {
                Text(
                    "Props from $it; game markets from game-form-v1. Inputs schema 1.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
            }
        }
        if (state.total > 0) {
            Spacer(Modifier.height(12.dp))
            // The board holds two leagues, so it says so. Counts are games, not props, so a
            // league with a quiet slate still shows up instead of looking unsupported.
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoiceChip("All", selected = league == null, onClick = { onLeague(null) })
                Spacer(Modifier.width(8.dp))
                ChoiceChip(
                    "${Sports.label(Sports.BASKETBALL)} ${state.games.count { it.sport == Sports.BASKETBALL }}",
                    selected = league == Sports.BASKETBALL,
                    onClick = { onLeague(Sports.BASKETBALL) },
                )
                Spacer(Modifier.width(8.dp))
                ChoiceChip(
                    "${Sports.label(Sports.FOOTBALL)} ${state.games.count { it.sport == Sports.FOOTBALL }}",
                    selected = league == Sports.FOOTBALL,
                    onClick = { onLeague(Sports.FOOTBALL) },
                )
            }
        }
        if (state.hasFixture) {
            Spacer(Modifier.height(18.dp))
            FixtureNotice()
        }
        if (state.total > 0 && !easy) {
            Spacer(Modifier.height(18.dp))
            BoardPulse(state)
        }
        Spacer(Modifier.height(18.dp))
        if (!easy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoiceChip("Game order", selected = !byEdge, onClick = { onOrder(false) })
                Spacer(Modifier.width(8.dp))
                ChoiceChip("Biggest edge", selected = byEdge, onClick = { onOrder(true) })
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(Palette.Over, "Says over")
            Spacer(Modifier.width(16.dp))
            LegendDot(Palette.Under, "Says under")
            Spacer(Modifier.width(16.dp))
            Box(Modifier.width(2.dp).height(12.dp).background(Palette.Chalk))
            Spacer(Modifier.width(6.dp))
            Text("Book price", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
    }
}

/**
 * One glance at the shape of the board: which way the model leans, its largest
 * disagreement, and what the book is charging. All three are computed from the
 * same props the rows below show.
 */
@Composable
private fun BoardPulse(state: SlateState) {
    val shape = RoundedCornerShape(18.dp)
    val edge = state.biggestEdge
    val edgeColor = when (edge?.lean) {
        Side.OVER -> Palette.Over
        Side.UNDER -> Palette.Under
        null -> Palette.Dust
    }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Palette.Court)
            .border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Board at a glance", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("${state.priced} priced", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            PulseStat(
                label = "Estimate leans",
                value = "${state.overLeans} / ${state.underLeans}",
                caption = "over / under",
                color = Palette.Chalk,
                modifier = Modifier.weight(1f),
            )
            PulseRule()
            PulseStat(
                label = "Biggest gap",
                value = edge?.edgePts?.let { "${fmtSigned(abs(it))} pts" } ?: "none",
                caption = edge?.let { "${it.lean?.label ?: ""} ${it.playerName}".trim() } ?: "nothing priced yet",
                color = edgeColor,
                modifier = Modifier.weight(1f),
            )
            PulseRule()
            val hold = state.averageHold
            PulseStat(
                label = "Book's cut",
                value = hold?.let { fmtPct(it) } ?: "none",
                caption = "average of both sides",
                color = if (hold != null && hold > 0.06) Palette.Caution else Palette.Chalk,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PulseStat(label: String, value: String, caption: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.padding(horizontal = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.Faint, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = Palette.Dust, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PulseRule() {
    Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(46.dp).background(Palette.Hairline))
}

@Composable
private fun EmptySlate(state: SlateState) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)
            .clip(shape).border(1.dp, Palette.Hairline, shape).padding(20.dp),
    ) {
        Text("No props on the board — checked, not broken", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.hasFixture) "The board was read and only synthetic test rows are present, with nothing priced yet. " +
                "Add the read-only price key in Lab → Live data and the board fills itself."
            else "The board was read and nothing on today's slate is priced yet. " +
                "Prices and estimates appear here once a price feed syncs; nothing is guessed to fill the space.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${state.total} props · ${state.scored} priced · ${state.priced} tradable. Checked just now from this phone.",
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
    }
}

@Composable
private fun EmptyLeague(league: String?) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)
            .clip(shape).border(1.dp, Palette.Hairline, shape).padding(20.dp),
    ) {
        Text(
            "No ${Sports.label(league ?: Sports.BASKETBALL)} props today",
            style = MaterialTheme.typography.titleLarge, color = Palette.Chalk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Switch back to All to see the other league. A league with no game scheduled has nothing to price, and this screen does not invent one.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Box(Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
    Spacer(Modifier.width(6.dp))
    Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
}

@Composable
private fun GameHeader(game: GameGroup) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(game.matchup, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, color = Palette.Chalk)
        Text(
            "${game.props.count { it.available }}/${game.props.size}",
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        Spacer(Modifier.width(10.dp))
        Text("${game.league} · ${fmtTime(game.startUtc)}", style = MaterialTheme.typography.labelLarge, color = Palette.Dust)
    }
}

@Composable
private fun PropRow(p: PropUi, onClick: () -> Unit, onBet: (PropUi) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .alpha(if (p.available) 1f else 0.65f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamMark(p.teamCode, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.playerName, style = MaterialTheme.typography.titleMedium, color = Palette.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${p.teamCode}, ${p.market}", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fmtLine(p.line), style = MaterialTheme.typography.headlineSmall, color = Palette.Chalk)
                Text("${fmtOdds(p.overOdds)} / ${fmtOdds(p.underOdds)}", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            }
        }
        val probs = p.probabilities
        Row(Modifier.padding(start = 54.dp, top = 8.dp, end = 0.dp), verticalAlignment = Alignment.CenterVertically) {
            if (probs != null) {
                val push = probs.push.toFloat()
                SplitBar(
                    over = probs.over.toFloat(), push = push,
                    market = p.marketOver?.let { (it * (1 - push)).toFloat() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(14.dp))
                EdgeLabel(p)
            } else {
                Text(p.reasons.firstOrNull() ?: "Unavailable", style = MaterialTheme.typography.bodySmall, color = Palette.Caution)
            }
        }
        if (p.available) {
            Row(Modifier.padding(start = 54.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                BetRowButton(onClick = { onBet(p) })
            }
        }
    }
}

/**
 * The three straight markets a book puts above its props, each side at the best price any
 * followed book is offering. The small percentage is the book's own no-vig number, not an
 * estimate: it says what the market thinks, which is the thing a model has to disagree with.
 */
@Composable
private fun GameLinesCard(lines: GameLinesUi) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)
            .clip(shape).background(Palette.Court).border(1.dp, Palette.Hairline, shape),
    ) {
        lines.markets.forEachIndexed { index, market ->
            if (index > 0) HorizontalDivider(color = Palette.Hairline)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(74.dp)) {
                    Text(market.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    market.hold?.let {
                        Text("cut ${fmtPct(it)}", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    }
                }
                MarketSide(market.left, sideHandicap(market, left = true), Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                MarketSide(market.right, sideHandicap(market, left = false), Modifier.weight(1f), end = true)
            }
        }
    }
}

/** A spread is written with a sign; a total is written plain. */
private fun sideHandicap(market: GameMarketUi, left: Boolean): String? {
    val line = market.line ?: return null
    if (market.market == GameMarkets.TOTAL) return fmtLine(line)
    val value = if (left) line else -line
    val sign = if (value > 0) "+" else "\u2212"
    return sign + "%.1f".format(abs(value))
}

@Composable
private fun MarketSide(side: GameSideUi, handicap: String?, modifier: Modifier, end: Boolean = false) {
    Column(modifier, horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(side.label, style = MaterialTheme.typography.labelLarge, color = Palette.Chalk, maxLines = 1)
            if (handicap != null) {
                Spacer(Modifier.width(6.dp))
                Text(handicap, style = MaterialTheme.typography.labelSmall, color = Palette.Dust, maxLines = 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fmtOdds(side.price), style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
            side.noVig?.let {
                Spacer(Modifier.width(6.dp))
                Text(fmtPct(it), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            }
        }
        side.modelChance?.let { model ->
            Text(
                "us ${fmtPct(model)}" + (side.edgePts?.let { " · ${fmtSigned(it)} pts" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = if ((side.edgePts ?: 0.0) > 0.0) Palette.Gain else Palette.Dust,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EdgeLabel(p: PropUi) {
    val edge = p.edgePts
    val lean = p.lean
    if (edge == null || lean == null || abs(edge) < 1.0) {
        Text("Even", style = MaterialTheme.typography.labelMedium, color = Palette.Dust)
    } else {
        Text(
            "${lean.label} +${"%.1f".format(abs(edge))}",
            style = MaterialTheme.typography.labelMedium,
            color = if (lean == Side.OVER) Palette.Over else Palette.Under,
            textAlign = TextAlign.End,
        )
    }
}
