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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.data.AutopilotUi
import com.propslab.data.BestBetsUi
import com.propslab.data.BestPickUi
import com.propslab.data.GamePickUi
import com.propslab.data.PropsRepository
import com.propslab.domain.Odds
import com.propslab.domain.Side
import com.propslab.settings.AppSettings
import com.propslab.settings.stakeWord
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.AutopilotCard
import com.propslab.ui.components.Badge
import com.propslab.ui.components.BookMark
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PlainLink
import com.propslab.ui.components.SplitBar
import com.propslab.ui.components.TeamAvatar
import com.propslab.ui.components.WorkToggle
import com.propslab.ui.components.bookLabel
import com.propslab.ui.fmtDay
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.fmtPct
import com.propslab.ui.fmtSigned
import com.propslab.ui.fmtTime
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class BestViewModel(private val repo: PropsRepository) : ViewModel() {
    val state: StateFlow<BestBetsUi> =
        repo.bestBets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BestBetsUi())

    val auto: StateFlow<AutopilotUi> =
        repo.autopilot().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AutopilotUi())

    var running by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun runEverything() {
        if (running) return
        running = true
        message = null
        viewModelScope.launch {
            val result = runCatching { repo.runEverythingNow() }
            message = result.getOrElse { it.message ?: "The automatic pass failed. Nothing was written." }
            running = false
        }
    }
}

/**
 * Today's best bets: the promoted formula's picks on the board that is live now.
 *
 * The screen never invents a pick. If no formula has cleared the gate yet, it shows what
 * the closest candidate would take, marked as a shadow, and says so.
 */
@Composable
fun BestScreen(
    onOpen: (String) -> Unit,
    onOpenSandbox: () -> Unit,
    onOpenPlain: () -> Unit,
    onOpenLenses: () -> Unit,
    onOpenParlay: () -> Unit,
    onOpenFeed: () -> Unit,
) {
    val repo = rememberRepo()
    val vm: BestViewModel = viewModel(factory = viewModelFactory { initializer { BestViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val auto by vm.auto.collectAsStateWithLifecycle()
    val easy = AppSettings.current.easyMode
    var showDetails by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 120.dp,
        ),
    ) {
        item { BestHeader(state, easy, onOpenPlain) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                AutopilotCard(auto, vm.running, vm.message, onRunAll = vm::runEverything, onOpenFeed = onOpenFeed)
            }
        }
        if (easy) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    WorkToggle(showDetails, { showDetails = !showDetails })
                }
            }
        }
        if (!easy || showDetails) {
            item { StrategyPanel(state, onOpenSandbox, onOpenLenses) }
        }
        item { ParlayEntry(state.picks.size, state.gamePicks.size, onOpenParlay) }
        if (state.gamePicks.isEmpty()) {
            // No game rating yet: the prop list below is still the useful part of the board.
        } else {
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
                    Text(
                        "GAME MARKETS · MONEYLINE, SPREAD, TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Over,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Scored from team form in the box scores, not from the book's own price. " +
                            "Only sides the model makes positive value on are shown.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                }
            }
            items(state.gamePicks, key = { "game-${it.key}" }) { pick ->
                GamePickCard(pick, easy = easy && !showDetails)
            }
        }
        if (state.picks.isEmpty()) {
            item { EmptyBest(onOpenSandbox) }
        } else {
            item {
                Text(
                    if (easy && !showDetails) "PICKS TO REVIEW" else "BEST FIRST · BIGGEST GAP VS THE BOOK",
                    Modifier.padding(start = 20.dp, top = 28.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Over,
                )
            }
            items(state.picks, key = { it.prop.id }) { pick ->
                BestRow(pick, easy = easy && !showDetails, onClick = { onOpen(pick.prop.id) })
                HorizontalDivider(Modifier.padding(start = 74.dp, end = 20.dp), color = Palette.Hairline)
            }
        }
        if (!easy || showDetails) item { BestFooter(state) }
    }
}

@Composable
private fun BestHeader(state: BestBetsUi, easy: Boolean, onOpenPlain: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Best bets", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk, modifier = Modifier.weight(1f))
            if (state.round > 0 && !easy) {
                Badge("ROUND ${state.round}", Palette.Faint)
                Spacer(Modifier.width(8.dp))
            }
            PlainLink(onOpenPlain)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (easy) {
                "${fmtDay(System.currentTimeMillis())}. ${state.picks.size} pick" +
                    if (state.picks.size == 1) " to review." else "s to review."
            } else {
                "${fmtDay(System.currentTimeMillis())}. ${state.picks.size} picks from the top strategy, " +
                    "${state.picks.size} units if you backed them all for the same amount."
            },
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        if (state.thinPicks > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.thinPicks} of them rest on fewer than eight games; those are the weakest picks here, not the strongest.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
            )
        }
    }
}

@Composable
private fun StrategyPanel(state: BestBetsUi, onOpenSandbox: () -> Unit, onOpenLenses: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Panel(tone = if (state.live) Palette.Court else Palette.Raised) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FORMULA", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.weight(1f))
                when {
                    state.live -> Badge("LIVE BOOK", Palette.Gain, filled = true)
                    state.hasFormula -> Badge("SANDBOX", Palette.Caution, filled = true)
                    else -> Badge("NOT YET", Palette.Faint)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                state.strategyLabel ?: "No strategy has graduated yet",
                style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    state.live -> "This strategy passed every check on a brand-new simulated season, so its picks below are the live list."
                    state.hasFormula -> "The leading strategy is still practising in the sandbox, so these picks are try-out picks, not the live list."
                    else -> "The system has not settled on a strategy yet. Run a round in the sandbox to compare them all."
                },
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "How this lenses a bet \u2192",
                Modifier.clickable(onClick = onOpenLenses).padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = Palette.Over,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.ready} of ${state.considered} passed every check",
                    style = MaterialTheme.typography.labelMedium, color = Palette.Dust,
                )
                Spacer(Modifier.weight(1f))
                ActionButton("Sandbox", onOpenSandbox)
            }
        }
    }
}

@Composable
private fun BestRow(pick: BestPickUi, easy: Boolean, onClick: () -> Unit) {
    val p = pick.prop
    val probs = p.probabilities
    val tone = if (pick.side == Side.OVER) Palette.Over else Palette.Under
    val belowBar = pick.shadow && pick.edgePts < 0.5

    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamAvatar(p.playerName, p.teamCode, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.playerName,
                    style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${p.market} ${fmtLine(p.line)} · ${p.matchup} · ${fmtTime(p.startUtc)}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(tone, RoundedCornerShape(50)))
                    Spacer(Modifier.width(6.dp))
                    Text(pick.side.label, style = MaterialTheme.typography.titleMedium, color = tone)
                }
                Text(fmtOdds(pick.odds), style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            }
        }

        if (probs != null && !easy) {
            Row(Modifier.padding(start = 54.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                SplitBar(
                    over = probs.over.toFloat(),
                    push = probs.push.toFloat(),
                    market = p.marketOver?.let { (it * (1 - probs.push)).toFloat() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${fmtSigned(pick.edgePts)} pts",
                    style = MaterialTheme.typography.labelMedium,
                    color = tone,
                )
            }
        }

        if (easy) {
            Text(
                plainValue(pick),
                Modifier.padding(start = 54.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        } else {
            Row(Modifier.padding(start = 54.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "our estimate ${fmtPct(pick.modelProb)} · book implies ${fmtPct(pick.marketProb)}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "value ${fmtSigned(pick.ev, 3)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (pick.ev > 0) Palette.Gain else Palette.Dust,
                )
            }
            Row(Modifier.padding(start = 54.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                // The stake shown is the fraction the settings screen selected, and the same
                // fraction the pick paragraph words as \u201cquarter-Kelly\u201d \u2014 one knob, both places.
                val fraction = AppSettings.current.stakeFraction
                Text(
                    "need ${fmtPct(pick.breakeven)} to break even · ${stakeWord(fraction)}-Kelly stake ${fmtPct((pick.kelly * fraction).coerceAtLeast(0.0))}",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
                Spacer(Modifier.weight(1f))
            }
        }
        Row(Modifier.padding(start = 54.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            when {
                pick.live && !belowBar -> Badge("LIVE PICK", Palette.Gain)
                pick.live -> Badge(if (easy) "NOT TODAY" else "BELOW BAR", Palette.Faint)
                else -> Badge(if (easy) "TRY-OUT" else "SHADOW", Palette.Caution)
            }
        }
        if (pick.explanation.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(Palette.Hairline))
                Text(
                    pick.explanation,
                    Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Dust,
                )
            }
        }
        BetToPlace(pick)
    }
}

/**
 * The bet as you would enter it at a book, directly under the analysis.
 *
 * The shape is the one every sportsbook uses: the book's own mark, the selection in that
 * book's wording, the price as a selectable button to its right, and the stake underneath
 * what it returns. A pick still in the sandbox is labelled as a try-out rather than a bet
 * to place, so the ticket never overstates what the app is telling you to do.
 */
@Composable
private fun BetToPlace(pick: BestPickUi) {
    val p = pick.prop
    val tone = if (pick.side == Side.OVER) Palette.Over else Palette.Under
    val stake = 1.0
    val toWin = stake * (Odds.americanToDecimal(pick.odds) - 1.0)
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.padding(start = 54.dp, top = 14.dp).fillMaxWidth()
            .clip(shape).background(Palette.Raised).border(1.dp, Palette.Hairline, shape),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookMark(p.book)
            Spacer(Modifier.width(8.dp))
            Text(
                bookLabel(p.book).uppercase(),
                style = MaterialTheme.typography.labelSmall, color = Palette.Chalk,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (pick.shadow) "TRY-OUT SLIP" else "BET TO PLACE",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint, maxLines = 1,
            )
        }
        HorizontalDivider(color = Palette.Hairline)
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(selection(pick), style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${p.playerName} \u00b7 ${p.matchup}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp))
                    .background(tone.copy(alpha = 0.16f))
                    .border(1.dp, tone.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(fmtOdds(pick.odds), style = MaterialTheme.typography.titleMedium, color = tone)
            }
        }
        Row(
            Modifier.fillMaxWidth().background(tone.copy(alpha = 0.06f)).padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "1.00 u to win ${"%.2f".format(toWin)} u \u00b7 returns ${"%.2f".format(stake + toWin)} u",
                style = MaterialTheme.typography.labelSmall, color = Palette.Dust, maxLines = 1,
            )
        }
    }
}

/** PrizePicks sells the same bet as More/Less, so its slip uses the book's own word. */
private fun selection(pick: BestPickUi): String {
    val word = if (pick.prop.book.equals("PrizePicks", ignoreCase = true)) {
        if (pick.side == Side.OVER) "More" else "Less"
    } else {
        pick.side.label
    }
    return "$word ${fmtLine(pick.prop.line)} ${pick.prop.market}".trim()
}

/**
 * The way into the automated parlays. It sits under the formula card because a parlay is
 * built from the picks that formula produced, not from a separate opinion.
 */
@Composable
private fun ParlayEntry(picks: Int, games: Int, onOpen: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Parlay builder", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "3, 5 and 10 legs built from today's $picks props and $games game markets, " +
                            "each with the real chance of landing.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                }
                Spacer(Modifier.width(10.dp))
                ActionButton("Build", onOpen)
            }
        }
    }
}

/**
 * A game market as a bet to place: the selection, the book's price, and what the model
 * thinks beside it. There is no click-through because a game line has no detail screen;
 * the note is the explanation.
 */
@Composable
private fun GamePickCard(pick: GamePickUi, easy: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    val tone = if ((pick.edgePts ?: 0.0) > 0) Palette.Gain else Palette.Dust
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()
            .clip(shape).background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    pick.matchup,
                    style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${pick.marketLabel} · ${pick.confidence.lowercase()} confidence · ${pick.sample} games of team form",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Badge(fmtOdds(pick.odds), if (pick.ev > 0) Palette.Gain else Palette.Dust)
        }
        Spacer(Modifier.height(10.dp))
        Text(pick.selection, style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        if (!easy) {
            Text(
                "${pick.book.ifBlank { "best price" }} · model ${fmtPct(pick.chance)} · " +
                    "book ${pick.marketChance?.let { fmtPct(it) } ?: "—"} · gap ${fmtSigned(pick.edgePts ?: 0.0)} pts",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(pick.note, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "1.00 u returns ${"%.2f".format(Odds.americanToDecimal(pick.odds))} u",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (easy) "PAPER ONLY" else "GAME MARKET · PAPER ONLY",
                style = MaterialTheme.typography.labelSmall, color = tone,
            )
        }
    }
}

/** One sentence for a reader who does not want a probability. The numbers are one tap away in Settings. */
private fun plainValue(pick: BestPickUi): String = when {
    pick.edgePts >= 4.0 -> "The price is clearly better than the app's estimate. This is the size of gap the strategy waits for."
    pick.edgePts >= 1.5 -> "The price is a little better than the app's estimate. A real gap, not a jackpot."
    pick.edgePts >= 0.5 -> "A thin edge: the app and the book mostly agree, and the price tips slightly this way."
    else -> "The app and the book broadly agree. This is a marginal pick, not a strong one."
}

@Composable
private fun EmptyBest(onOpenSandbox: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
        Panel {
            Text("Nothing clears the bar today", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
            Spacer(Modifier.height(8.dp))
            Text(
                "Either the board is thin, or no formula has earned the right to trade it yet. " +
                    "The sandbox keeps searching either way.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
            )
            Spacer(Modifier.height(14.dp))
            ActionButton("Open the sandbox", onOpenSandbox)
        }
    }
}

@Composable
private fun BestFooter(state: BestBetsUi) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text(
            state.note,
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.anyFixture) {
                "Part of this board is still synthetic fixture data, so those rows describe an invented " +
                    "game. Nothing here contacts a book or moves money."
            } else {
                "Prices come from the read-only feed and results settle from the box scores it reads. " +
                    "Nothing here contacts a book or moves money."
            },
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        if (state.picks.any { it.shadow }) {
            Spacer(Modifier.height(8.dp))
            Text(
                "A shadow pick is what the best candidate would take. It is shown for review only.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(8.dp))
        val best = state.picks.maxByOrNull { abs(it.ev) }
        if (best != null) {
            Text(
                "Biggest value on the board: ${best.prop.playerName} ${best.side.label} at " +
                    "${fmtOdds(best.odds)} (${fmtSigned(best.ev, 3)} per unit on average).",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        }
    }
}
