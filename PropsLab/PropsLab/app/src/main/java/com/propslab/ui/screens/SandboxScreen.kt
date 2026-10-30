package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.propslab.data.FormulaUi
import com.propslab.data.PropsRepository
import com.propslab.data.SandboxTradeUi
import com.propslab.data.SandboxUi
import com.propslab.settings.AppSettings
import com.propslab.sim.SandboxEngine
import com.propslab.domain.Outcome
import com.propslab.domain.Side
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Metric
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PlainLink
import com.propslab.ui.components.WorkToggle
import com.propslab.ui.fmtAgo
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.fmtPct
import com.propslab.ui.fmtSigned
import com.propslab.ui.fmtTime
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SandboxScreenState(
    val sandbox: SandboxUi = SandboxUi(),
    val running: Boolean = false,
    val lastNote: String? = null,
)

class SandboxViewModel(private val repo: PropsRepository) : ViewModel() {
    private val running = MutableStateFlow(false)
    private val lastNote = MutableStateFlow<String?>(null)

    val state: StateFlow<SandboxScreenState> =
        combine(repo.sandbox(), running, lastNote) { box, busy, note ->
            SandboxScreenState(box, busy, note)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SandboxScreenState())

    fun runRound() {
        if (running.value) return
        viewModelScope.launch {
            running.value = true
            val outcome = runCatching { repo.runSandboxRound() }.getOrNull()
            lastNote.value = outcome?.note ?: "The round failed. Nothing was written."
            running.value = false
        }
    }

    fun setAutoRun(enabled: Boolean) {
        viewModelScope.launch { repo.setAutoRun(enabled) }
    }
}

/**
 * The fake bankroll, the ladder of formulas, and the live book.
 *
 * This is the screen that answers "is the system allowed to bet today, and why": every
 * formula shows what it returned and which check it failed, so a formula that has not
 * been promoted has a visible reason rather than a silent one.
 */
@Composable
fun SandboxScreen(onOpenAudit: () -> Unit, onOpenPlain: () -> Unit) {
    val repo = rememberRepo()
    val vm: SandboxViewModel = viewModel(factory = viewModelFactory { initializer { SandboxViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val box = state.sandbox
    val easy = AppSettings.current.easyMode
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val showAll = !easy || showDetails

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 120.dp,
        ),
    ) {
        item {
            SandboxHeader(
                state,
                easy = easy,
                onRun = { vm.runRound() },
                onToggleAuto = { vm.setAutoRun(!box.autoRun) },
                onOpenPlain = onOpenPlain,
            )
        }
        if (easy) {
            item {
                WorkToggle(
                    showDetails,
                    { showDetails = !showDetails },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        item { if (showAll) BankrollPanel(box) else EasySandboxPanel(box) }
        item { PromotedPanel(box, simple = !showAll) }
        if (showAll) {
            item {
                Text(
                    "ALL CANDIDATES",
                    Modifier.padding(start = 20.dp, top = 26.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = Palette.Over,
                )
            }
            item {
                Text(
                    "${box.considered} strategies, each run over a fresh simulated season. Only one is allowed to pick the real board.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
            items(box.ranked, key = { it.id }) { formula ->
                FormulaRow(formula)
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Palette.Hairline)
            }
        }
        item { LiveBookSection(box) }
        if (showAll) item { SandboxFooter(box, onOpenAudit) }
    }
}

@Composable
private fun SandboxHeader(
    state: SandboxScreenState,
    easy: Boolean,
    onRun: () -> Unit,
    onToggleAuto: () -> Unit,
    onOpenPlain: () -> Unit,
) {
    val box = state.sandbox
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(if (easy) "Practice league" else "Sandbox", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
        Spacer(Modifier.height(2.dp))
        Text(
            if (easy) {
                "Strategies try out here first. The one on the real board has already earned it."
            } else {
                "A fake bankroll, a simulated season, and one rule: a strategy only picks real props after it proves an edge."
            },
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(
                if (box.autoRun) "AUTONOMOUS" else "PAUSED",
                if (box.autoRun) Palette.Gain else Palette.Caution,
                filled = true,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (box.lastRunUtc > 0) {
                    if (easy) "updates itself" else "round ${box.round}, ${fmtAgo(box.lastRunUtc)}"
                } else {
                    "no rounds yet"
                },
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            Spacer(Modifier.weight(1f))
            ActionButton(
                if (state.running) "Running…" else if (easy) "Update now" else "Run a round",
                onRun,
                enabled = !state.running,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!easy) {
                ActionButton(
                    if (box.autoRun) "Pause the loop" else "Resume the loop",
                    onToggleAuto,
                    tone = Palette.Dust,
                )
            }
            Spacer(Modifier.weight(1f))
            PlainLink(onOpenPlain)
        }
    }
}

@Composable
private fun EasySandboxPanel(box: SandboxUi) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Panel {
            Text("WHAT IT IS DOING", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(6.dp))
            Text(
                "${box.considered} strategies are competing in a practice league. " +
                    "${box.liveBets} paper pick${if (box.liveBets == 1) "" else "s"} have been made on the real board.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "The score below is play money. No real money moves here, and the app never contacts a book.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        }
    }
}

@Composable
private fun BankrollPanel(box: SandboxUi) {
    val starting = SandboxEngine.STARTING_BANKROLL
    val roi = box.liveRoi
    val profitTone = when {
        box.liveProfit > 0.005 -> Palette.Gain
        box.liveProfit < -0.005 -> Palette.Loss
        else -> Palette.Chalk
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Panel {
            Text("FAKE BANKROLL", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.2f".format(box.bankroll.coerceAtLeast(0.0)),
                    style = MaterialTheme.typography.displayMedium, color = Palette.Chalk,
                )
                Spacer(Modifier.width(6.dp))
                Text("units", Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
                Spacer(Modifier.weight(1f))
                Text(
                    fmtSigned(box.liveProfit, 2),
                    style = MaterialTheme.typography.titleLarge, color = profitTone,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Metric("Started", "%.0f".format(starting), Modifier.weight(1f), caption = "units, same stake each")
                Metric("Staked", "%.0f".format(box.liveStaked), Modifier.weight(1f), caption = "on the live list")
                Metric(
                    "Return", roi?.let { fmtSigned(it * 100, 1) + "%" } ?: "none",
                    Modifier.weight(1f),
                    caption = "on money staked",
                    tone = profitTone,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Metric("Bets", "${box.liveBets}", Modifier.weight(1f), caption = "${box.liveWins} won, ${box.liveLosses} lost")
                Metric(
                    "Drawdown",
                    fmtPct(box.liveDrawdown),
                    Modifier.weight(1f),
                    caption = "worst fall from peak",
                    tone = if (box.liveDrawdown > 0.25) Palette.Caution else Palette.Chalk,
                )
                Metric(
                    "Stake",
                    "%.1f".format(SandboxEngine.UNIT_STAKE),
                    Modifier.weight(1f),
                    caption = "units per bet",
                )
            }
        }
    }
}

@Composable
private fun PromotedPanel(box: SandboxUi, simple: Boolean = false) {
    val live = box.live
    val leader = box.leader
    Column(Modifier.padding(horizontal = 20.dp)) {
        Panel(tone = if (live != null) Palette.Court else Palette.Raised) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PICKING THE REAL BOARD", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.weight(1f))
                if (live != null) Badge("PROMOTED", Palette.Gain, filled = true) else Badge("NONE YET", Palette.Caution)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                live?.label ?: "No strategy has graduated yet",
                style = MaterialTheme.typography.titleLarge, color = Palette.Chalk,
            )
            if (live != null && !simple) {
                Spacer(Modifier.height(14.dp))
                Row {
                    Metric("Bets", "${live.bets}", Modifier.weight(1f), caption = "this season")
                    Metric("Return", fmtSigned(live.roi * 100, 1) + "%", Modifier.weight(1f), caption = "on stake", tone = Palette.Gain)
                    Metric("Luck check", "%.2f".format(live.tStat), Modifier.weight(1f), caption = "2.0+ to pass")
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Metric(
                        "Hit rate",
                        live.winRate?.let { fmtPct(it) } ?: "none",
                        Modifier.weight(1f),
                        caption = "settled bets",
                    )
                    Metric("Worst dip", fmtPct(live.maxDrawdown), Modifier.weight(1f), caption = "of bankroll")
                    Metric("Pushes", "${live.pushes}", Modifier.weight(1f), caption = "stake returned")
                }
            } else if (live == null && leader != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (simple) {
                        "Nothing has earned the real board yet. The closest strategy is still being tested."
                    } else {
                        "Closest so far: ${leader.label}, ${fmtSigned(leader.roi * 100, 1)}% on ${leader.bets} bets. " +
                            (leader.reasons.firstOrNull() ?: "It needs more evidence.")
                    },
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
            if (!simple) {
                Spacer(Modifier.height(14.dp))
                Text(box.note, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
            }
        }
    }
}

@Composable
private fun FormulaRow(f: FormulaUi) {
    val tone = when {
        f.isLive -> Palette.Gain
        f.isRetired -> Palette.Loss
        f.passed -> Palette.Caution
        else -> Palette.Faint
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(tone, CircleShape))
            Spacer(Modifier.width(9.dp))
            Text(
                f.label,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                fmtSigned(f.roi * 100, 1) + "%",
                style = MaterialTheme.typography.labelLarge,
                color = if (f.roi > 0) Palette.Gain else Palette.Loss,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${f.bets} bets · luck check ${"%.2f".format(f.tStat)} · worst dip ${fmtPct(f.maxDrawdown)}",
                style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
            )
            Spacer(Modifier.weight(1f))
            when {
                f.isLive -> Badge("LIVE", Palette.Gain)
                f.isRetired -> Badge("RETIRED", Palette.Loss)
                f.passed -> Badge("READY", Palette.Caution)
                else -> Badge("SANDBOX", Palette.Faint)
            }
        }
        if (!f.passed && f.reasons.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                f.reasons.first(),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (f.historyVerdict != "NOT RUN") {
            Spacer(Modifier.height(4.dp))
            Text(
                "History: " + if (f.historyPassed == true) {
                    "passed on ${f.historyBets} imported rows"
                } else {
                    "held on ${f.historyBets} imported rows" +
                        if (f.historyNote.isNotBlank()) " — ${f.historyNote}" else ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (f.historyPassed == true) Palette.Gain else Palette.Loss,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LiveBookSection(box: SandboxUi) {
    Column(Modifier.padding(top = 26.dp)) {
        Text(
            "LIVE BOOK",
            Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.labelSmall, color = Palette.Over,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            if (box.trades.isEmpty()) {
                "The live book is empty until a strategy graduates and today's board has picks for it."
            } else {
                "${box.openTrades.size} open, ${box.settledTrades.size} finished on today's board."
            },
            Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        val shown = (box.openTrades.sortedBy { it.startUtc } + box.settledTrades.sortedByDescending { it.settledUtc }).take(20)
        for (trade in shown) {
            TradeRow(trade)
            HorizontalDivider(Modifier.padding(start = 20.dp, end = 20.dp), color = Palette.Hairline)
        }
    }
}

@Composable
private fun TradeRow(t: SandboxTradeUi) {
    val tone = if (t.side == Side.OVER) Palette.Over else Palette.Under
    val resultTone = when (t.outcome) {
        Outcome.WIN -> Palette.Gain
        Outcome.LOSS -> Palette.Loss
        Outcome.PUSH -> Palette.Dust
        else -> Palette.Faint
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(tone, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(t.playerName, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
            }
            Text(
                "${t.side.label} ${fmtLine(t.line)} at ${fmtOdds(t.odds)} · ${"%.1f".format(t.stake)} units · ${fmtTime(t.startUtc)}",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        }
        if (t.isOpen) {
            Badge("OPEN", Palette.Faint)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(fmtSigned(t.profit ?: 0.0, 2), style = MaterialTheme.typography.titleSmall, color = resultTone)
                Text(
                    t.outcome?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
            }
        }
    }
}

@Composable
private fun SandboxFooter(box: SandboxUi, onOpenAudit: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text("How a strategy graduates", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Each round invents a season nobody has seen, runs every strategy over it, and grades them all " +
                "the same way: at least 400 bets, a return above 1.5% of everything staked, a luck check above " +
                "2.0 so the profit is not chance, a worst dip under 45% of the bankroll, and a win rate higher " +
                "than the break-even the prices require.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "The practice world is built to be winnable: what really happens there is 70% driven by the " +
                "model's read and 30% by the book's price, so a wide disagreement genuinely pays. That makes " +
                "the sandbox a test of the search, not proof that a real market can be beaten.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        if (box.trades.any { it.synthetic }) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Results marked synthetic come from the practice engine, because those games are invented test data.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(16.dp))
        ActionButton("Audit trail", onOpenAudit, tone = Palette.Dust)
    }
}
