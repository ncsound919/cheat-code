package com.propslab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.data.BestPickUi
import com.propslab.data.BetUi
import com.propslab.data.CalibrationUi
import com.propslab.data.FormulaRecord
import com.propslab.data.FormulaUi
import com.propslab.data.LearningUi
import com.propslab.data.PracticeLedger.Milestone
import com.propslab.data.PlaceResult
import com.propslab.data.PracticeLedger
import com.propslab.data.UserFindingUi
import com.propslab.data.UserInsightsUi
import com.propslab.data.PropsRepository
import com.propslab.domain.Calibration
import com.propslab.domain.Outcome
import com.propslab.domain.Side
import com.propslab.sim.InsightKind
import com.propslab.sim.StrategyParams
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.BookMark
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Metric
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PlainLink
import com.propslab.ui.components.SectionTitle
import com.propslab.ui.components.bookLabel
import com.propslab.ui.fmtAgo
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.fmtPct
import com.propslab.ui.fmtSigned
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The three views the performance card can switch between. */
enum class PracticeChartMode(val label: String) {
    BANKROLL("Bankroll"),
    FORMULAS("Formulas"),
    LEARNING("Self-learning"),
}

data class PortfolioState(
    val open: List<BetUi> = emptyList(),
    val settled: List<BetUi> = emptyList(),
    val picks: List<BestPickUi> = emptyList(),
    val formulas: List<FormulaUi> = emptyList(),
    val formulaRecords: List<FormulaRecord> = emptyList(),
    val learning: LearningUi = LearningUi(),
    val promotedLabel: String? = null,
    val userInsights: UserInsightsUi = UserInsightsUi(),
) {
    val profit: Double get() = settled.sumOf { it.profit ?: 0.0 }
    val staked: Double get() = settled.sumOf { it.stake }
    val wins: Int get() = settled.count { it.outcome == Outcome.WIN }
    val losses: Int get() = settled.count { it.outcome == Outcome.LOSS }
    val pushes: Int get() = settled.count { it.outcome == Outcome.PUSH }
    val decisions: Int get() = wins + losses

    /** Profit per unit staked across decided practice bets; null until something settled. */
    val roi: Double? get() = if (staked > 0.0) profit / staked else null
    val winRate: Double? get() = if (decisions > 0) wins.toDouble() / decisions else null

    val balance: Double get() = PracticeLedger.balance(profit)
    val atRisk: Double get() = open.sumOf { it.stake }
    val available: Double get() = PracticeLedger.available(profit, atRisk)

    /** The balance after each settled bet, oldest first, starting at the opening bankroll. */
    val series: List<Double>
        get() = settled.sortedBy { it.settledUtc ?: 0L }
            .runningFold(PracticeLedger.STARTING_BANKROLL) { balance, bet -> balance + (bet.profit ?: 0.0) }

    val advice: String
        get() = PracticeLedger.advice(open + settled, formulaRecords, learning.calibration, promotedLabel)
}

class PortfolioViewModel(private val repo: PropsRepository) : ViewModel() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val state: StateFlow<PortfolioState> =
        combine(repo.bets(), repo.sandbox(), repo.learning(), repo.bestBets(), repo.userInsights()) { bets, sandbox, learning, best, insights ->
            PortfolioState(
                open = bets.filter { it.isOpen }.sortedBy { it.decidedUtc },
                settled = bets.filter { !it.isOpen }.sortedByDescending { it.settledUtc },
                picks = best.picks,
                formulas = sandbox.formulas,
                formulaRecords = PracticeLedger.records(bets, sandbox.trades, sandbox.formulas),
                learning = learning,
                promotedLabel = sandbox.promotedLabel,
                userInsights = insights,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioState())

    fun place(
        predictionId: String,
        side: Side,
        stake: Double,
        formulaId: String,
        formulaLabel: String,
    ) {
        viewModelScope.launch {
            val message = when (
                val result = repo.placeBet(predictionId, side, stake, formulaId, formulaLabel)
            ) {
                PlaceResult.Placed -> "Paper bet recorded. It is in your practice ledger."
                PlaceResult.Duplicate -> "You already have a paper bet on the ${side.label} for this prop."
                PlaceResult.Unavailable -> "This estimate is unavailable, so no bet was recorded."
                is PlaceResult.NotEnoughBankroll ->
                    "Only ${"%.2f".format(result.available)} practice units are available, so no bet was recorded."
            }
            _messages.emit(message)
        }
    }

    /**
     * Places a whole entry: one paper bet per leg at the same stake, all tagged to the
     * entry, so the slip reads as one slip and the record reads as one group.
     */
    fun placeEntry(legs: List<PropsRepository.EntryLeg>, stake: Double) {
        viewModelScope.launch {
            val label = "Entry · ${legs.size} legs"
            val result = repo.placeEntry(legs, stake, label)
            _messages.emit(
                when {
                    result.placed == 0 ->
                        "No legs could be placed. ${result.skipped.firstOrNull() ?: "Nothing was written."}"
                    result.skipped.isEmpty() ->
                        "Entry placed: ${result.placed} paper bets tagged \"$label\"."
                    else ->
                        "Entry partly placed: ${result.placed} of ${legs.size} legs. ${result.skipped.first()}"
                },
            )
        }
    }

    /**
     * Turns one of the user's own findings into a testable strategy: the proposal becomes
     * a formula, the bench tests it on a fresh simulated season, and the sandbox gate
     * decides whether it ever trades.
     */
    fun testFinding(proposal: StrategyParams, title: String, advice: String) {
        viewModelScope.launch {
            val outcome = runCatching { repo.createClaimFromParams(proposal, "Your $title rule", advice) }
            _messages.emit(
                outcome.fold(
                    onSuccess = { it.note },
                    onFailure = { "Could not test that rule: ${it.message ?: "unknown error"}" },
                ),
            )
        }
    }

    fun settle(betId: String, actual: Double) {
        viewModelScope.launch {
            val done = repo.settleBet(betId, actual)
            _messages.emit(if (done) "Result recorded. The practice bankroll has updated." else "That result was already recorded.")
        }
    }

    fun reset() {
        viewModelScope.launch {
            repo.resetPracticeAccount()
            _messages.emit("Practice account reset to ${"%.0f".format(PracticeLedger.STARTING_BANKROLL)} units.")
        }
    }
}

@Composable
fun PortfolioScreen(onOpenPlain: () -> Unit) {
    val repo = rememberRepo()
    val vm: PortfolioViewModel = viewModel(factory = viewModelFactory { initializer { PortfolioViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var settling by remember { mutableStateOf<BetUi?>(null) }
    var placing by remember { mutableStateOf(false) }
    var entering by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var chartMode by rememberSaveable { mutableStateOf(PracticeChartMode.BANKROLL.name) }
    var filterId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    val mode = PracticeChartMode.entries.firstOrNull { it.name == chartMode } ?: PracticeChartMode.BANKROLL
    val open = state.open.filter { filterId == null || it.formulaKey == filterId }
    val settled = state.settled.filter { filterId == null || it.formulaKey == filterId }

    Scaffold(
        containerColor = Palette.Ink,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = 120.dp,
            ),
        ) {
            item { Header(onOpenPlain) }
            item {
                BankrollCard(
                    state,
                    onPlace = { placing = true },
                    onEnter = { entering = true },
                    onReset = { resetting = true },
                )
            }
            item {
                PerformanceCard(
                    state,
                    mode = mode,
                    onMode = { chartMode = it.name },
                )
            }
            item {
                StreakCard(state)
            }
            item {
                UserInsightsCard(
                    insights = state.userInsights,
                    onTest = { proposal, title, advice -> vm.testFinding(proposal, title, advice) },
                )
            }
            if (state.formulaRecords.isNotEmpty()) {
                item {
                    FormulaFilter(
                        records = state.formulaRecords,
                        selected = filterId,
                        onSelect = { filterId = it },
                    )
                }
            }
            if (open.isNotEmpty()) {
                item { SectionTitle("Open bets", Modifier.padding(start = 20.dp, top = 30.dp, bottom = 2.dp)) }
                item {
                    Text(
                        "Each slip is the bet as you would enter it at the book: the market, the number, " +
                            "the price, the stake and the formula tag. Grouped by the book that quoted the price.",
                        Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                }
                open.groupBy { it.book.ifBlank { "Best price" } }.forEach { (book, bets) ->
                    item(key = "book-$book") { BookHeading(book, bets.size) }
                    items(bets, key = { it.id }) { bet ->
                        BetSlip(bet, action = {
                            Text(
                                "Record result",
                                Modifier.clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Palette.Hairline, RoundedCornerShape(10.dp))
                                    .clickable { settling = bet }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium, color = Palette.Chalk,
                            )
                        })
                    }
                }
            } else if (state.open.isNotEmpty()) {
                item { NoFilterResults("No open bets carry this formula tag.") }
            }
            if (settled.isNotEmpty()) {
                item { SectionTitle("Settled", Modifier.padding(start = 20.dp, top = 30.dp, bottom = 6.dp)) }
                items(settled, key = { it.id }) { bet ->
                    BetSlip(bet, action = { SettledResult(bet) })
                }
            } else if (state.settled.isNotEmpty()) {
                item { NoFilterResults("No settled bets carry this formula tag yet.") }
            }
            if (state.open.isEmpty() && state.settled.isEmpty()) {
                item { EmptyPortfolio(onPlace = { placing = true }) }
            }
            item { PracticeFooter(onOpenPlain) }
        }
    }

    if (entering) {
        EntrySheet(
            picks = state.picks,
            available = state.available,
            onDismiss = { entering = false },
            onPlace = { legs, stake -> vm.placeEntry(legs, stake) },
        )
    }

    if (placing) {
        PaperBetSheet(
            picks = state.picks,
            formulas = state.formulas,
            available = state.available,
            onDismiss = { placing = false },
            onPlace = { predictionId, side, stake, formulaId, formulaLabel ->
                vm.place(predictionId, side, stake, formulaId, formulaLabel)
            },
        )
    }

    settling?.let { bet ->
        ResultDialog(bet, onDismiss = { settling = null }, onConfirm = { actual ->
            vm.settle(bet.id, actual)
            settling = null
        })
    }

    if (resetting) {
        AlertDialog(
            onDismissRequest = { resetting = false },
            containerColor = Palette.Raised,
            title = { Text("Start the practice account over?", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk) },
            text = {
                Text(
                    "This clears every practice bet and returns the account to " +
                        "${"%.0f".format(PracticeLedger.STARTING_BANKROLL)} units. The formula sandbox and its " +
                        "separate bankroll are not touched.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.reset()
                    filterId = null
                    resetting = false
                }) { Text("Reset my account") }
            },
            dismissButton = { TextButton(onClick = { resetting = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Header(onOpenPlain: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Practice ledger", Modifier.weight(1f), style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
            PlainLink(onOpenPlain)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Fake bets, real records. Track what you would have played and see which formula earned it.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
    }
}

@Composable
private fun BankrollCard(state: PortfolioState, onPlace: () -> Unit, onEnter: () -> Unit, onReset: () -> Unit) {
    val tone = when {
        state.settled.isEmpty() -> Palette.Chalk
        state.profit > 0.005 -> Palette.Gain
        state.profit < -0.005 -> Palette.Loss
        else -> Palette.Chalk
    }
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PRACTICE BANKROLL", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Badge("PAPER ONLY", Palette.Caution)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "%.2f".format(state.balance),
                style = MaterialTheme.typography.displayMedium, color = Palette.Chalk,
            )
            Spacer(Modifier.width(6.dp))
            Text("units", Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
            Spacer(Modifier.weight(1f))
            Text(fmtSigned(state.profit, 2), style = MaterialTheme.typography.titleLarge, color = tone)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${"%.2f".format(state.available)} available · ${"%.2f".format(state.atRisk)} at risk",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(14.dp))
        Row {
            Metric(
                "Record",
                if (state.settled.isEmpty()) "—" else "${state.wins}–${state.losses}",
                Modifier.weight(1f),
                caption = if (state.pushes > 0) "${state.pushes} pushed" else "${state.decisions} decided",
                tone = tone,
            )
            Metric(
                "Win rate",
                state.winRate?.let { fmtPct(it) } ?: "—",
                Modifier.weight(1f),
                caption = "pushes excluded",
            )
            Metric(
                "Return",
                state.roi?.let { fmtSigned(it * 100, 1) + "%" } ?: "—",
                Modifier.weight(1f),
                caption = "on money staked",
                tone = tone,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row {
            ActionButton(
            if (state.picks.isEmpty()) "No priced picks to bet yet" else "Place a paper bet",
                onPlace,
                modifier = Modifier.weight(1f),
                enabled = state.picks.isNotEmpty() && state.available >= 0.5,
            )
            Spacer(Modifier.width(10.dp))
            ActionButton(
                "Build an entry",
                onEnter,
                modifier = Modifier.weight(1f),
                enabled = state.picks.size >= 2 && state.available >= 1.0,
            )
        }
        if (state.available < 0.5 && state.open.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Every unit is tied up in open bets. Record a result to release the stake.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "This is your account. The formula sandbox keeps its own separate fake bankroll.",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
            PlainLink(onReset, label = "Reset")
        }
    }
}

@Composable
private fun PerformanceCard(
    state: PortfolioState,
    mode: PracticeChartMode,
    onMode: (PracticeChartMode) -> Unit,
) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text("PERFORMANCE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PracticeChartMode.entries.forEach { option ->
                ChoiceChip(option.label, selected = mode == option, onClick = { onMode(option) })
            }
        }
        Spacer(Modifier.height(16.dp))
        when (mode) {
            PracticeChartMode.BANKROLL -> BankrollChart(state)
            PracticeChartMode.FORMULAS -> FormulaChart(state.formulaRecords)
            PracticeChartMode.LEARNING -> LearningChart(state.learning)
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Palette.Hairline)
        Spacer(Modifier.height(12.dp))
        Text("WHAT NEEDS ADJUSTING", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.height(5.dp))
        Text(state.advice, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
    }
}

@Composable
private fun BankrollChart(state: PortfolioState) {
    if (state.settled.isEmpty()) {
        Text(
            "No settled bets yet. The balance line starts at ${"%.0f".format(PracticeLedger.STARTING_BANKROLL)} units " +
                "and moves when you record a result.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        return
    }
    val values = state.series
    val lo = values.min()
    val hi = values.max()
    val span = (hi - lo).coerceAtLeast(1.0)
    val tone = if (state.profit >= 0.0) Palette.Gain else Palette.Loss
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("start ${"%.2f".format(values.first())}", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.weight(1f))
        Text("now ${"%.2f".format(values.last())}", style = MaterialTheme.typography.labelMedium, color = tone)
    }
    Spacer(Modifier.height(6.dp))
    Canvas(Modifier.fillMaxWidth().height(132.dp)) {
        val pad = 6.dp.toPx()
        fun x(i: Int) = if (values.size <= 1) size.width / 2f else pad + (size.width - 2 * pad) * i / (values.size - 1)
        fun y(v: Double) = (size.height - pad - (size.height - 2 * pad) * ((v - lo) / span)).toFloat()
        val baseY = y(PracticeLedger.STARTING_BANKROLL)
        drawLine(Palette.Hairline, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.dp.toPx())
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
        drawPath(path, tone, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        values.forEachIndexed { i, v -> drawCircle(tone.copy(alpha = 0.6f), 2.dp.toPx(), Offset(x(i), y(v))) }
        drawCircle(tone, 4.dp.toPx(), Offset(x(values.lastIndex), y(values.last())))
    }
    Row(Modifier.fillMaxWidth()) {
        Text("first finished", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.weight(1f))
        Text("latest", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "${state.wins} won, ${state.losses} lost, ${state.pushes} pushed. " +
            "The line is the cash balance, not profit alone.",
        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
    )
}

@Composable
private fun FormulaChart(records: List<FormulaRecord>) {
    val shown = records.filter { it.settled > 0 || it.open > 0 }
        .sortedWith(compareByDescending<FormulaRecord> { it.settled }.thenByDescending { it.profit })
        .take(7)
    if (shown.isEmpty()) {
        Text(
            "No practice bets are tagged to a formula yet. Choose a formula tag when you place a bet, " +
                "or let the sandbox's live book take a pick.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        return
    }
    val scale = shown.mapNotNull { it.roi?.let(::abs) }.maxOrNull()?.coerceAtLeast(0.01) ?: 0.01
    shown.forEach { record ->
        val roi = record.roi
        val tone = when {
            roi == null -> Palette.Faint
            roi >= 0.0 -> Palette.Gain
            else -> Palette.Loss
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                record.label + if (record.live) " · LIVE" else "",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                roi?.let { fmtSigned(it * 100, 1) + "%" } ?: "open",
                style = MaterialTheme.typography.labelLarge, color = tone,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Palette.Hairline)) {
            if (roi != null && roi != 0.0) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth((abs(roi) / scale).toFloat().coerceIn(0.04f, 1f))
                        .background(tone, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                "${record.settled} settled · ${record.open} open · " +
                    "${record.winRate?.let { fmtPct(it) } ?: "—"} win",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${fmtSigned(record.profit, 2)} units",
                style = MaterialTheme.typography.labelSmall, color = tone,
            )
        }
        if (record != shown.last()) Spacer(Modifier.height(12.dp))
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Your tagged paper bets and the formula live book are combined here. A formula with no settled " +
            "bets is still being tested.",
        style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
    )
}

@Composable
private fun LearningChart(learning: LearningUi) {
    val fits = learning.calibration
    if (fits.all { !it.fitted }) {
        Text(
            "Self-learning has not fitted a correction yet. Each model needs ${Calibration.MIN_SAMPLE} settled decisions; " +
                "the ledger currently holds ${learning.settled} settled decisions in total.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Until then every model probability is used unchanged. That is a stated wait, not a hidden adjustment.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        return
    }
    val scale = fits.maxOf { maxOf(it.brierBefore, it.brierAfter) }.coerceAtLeast(0.0001)
    fits.forEach { fit ->
        Text(fit.label, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
        Spacer(Modifier.height(2.dp))
        Text(
            if (fit.fitted) {
                "${fit.sample} settled decisions · slope ${"%.2f".format(fit.slope)} · " +
                    "Brier ${"%.4f".format(fit.brierBefore)} → ${"%.4f".format(fit.brierAfter)}"
            } else {
                "No fitted correction yet. Needs ${Calibration.MIN_SAMPLE} settled decisions."
            },
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        if (fit.fitted) {
            Spacer(Modifier.height(7.dp))
            BrierBar("Before", fit.brierBefore, scale, Palette.Faint)
            Spacer(Modifier.height(5.dp))
            BrierBar("After", fit.brierAfter, scale, if (fit.brierAfter <= fit.brierBefore) Palette.Gain else Palette.Loss)
        }
        Spacer(Modifier.height(14.dp))
    }
    Text(
        "Lower Brier is better. The after bar is the error after the in-sample correction, not a claimed " +
            "out-of-sample score.",
        style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
    )
}

@Composable
private fun BrierBar(label: String, value: Double, scale: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Palette.Hairline)) {
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth((value / scale).toFloat().coerceIn(0.02f, 1f))
                    .background(color, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("%.4f".format(value), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun FormulaFilter(records: List<FormulaRecord>, selected: String?, onSelect: (String?) -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
        Text("FILTER THE HISTORY", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("All", selected == null, { onSelect(null) })
            records.forEach { record ->
                ChoiceChip(record.label, selected == record.id, { onSelect(record.id) })
            }
        }
    }
}

@Composable
private fun NoFilterResults(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
    )
}

@Composable
private fun EmptyPortfolio(onPlace: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
            .clip(shape).border(1.dp, Palette.Hairline, shape).padding(20.dp),
    ) {
        Text("No paper bets yet", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Place a fake bet from the board and give it a formula tag. Nothing here moves real money.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        Spacer(Modifier.height(14.dp))
        ActionButton("Place a paper bet", onPlace)
    }
}

@Composable
private fun PracticeFooter(onOpenPlain: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
        Text(
            "Practice account only. A paper bet never contacts a sportsbook and never moves real money. " +
                "A stake is reserved until you record the result; a push returns it.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        Spacer(Modifier.height(8.dp))
        PlainLink(onOpenPlain, label = "What the numbers mean")
    }
}

/**
 * One paper bet drawn as the ticket a book would hand back.
 *
 * The order is the order a bettor checks it in: which book, what the pick is, the number and
 * the price, then what the stake risks and returns. The same card serves the open book and
 * the settled ledger, so a result is never read against a differently shaped row.
 */
@Composable
private fun BetSlip(bet: BetUi, action: @Composable () -> Unit) {
    val sideColor = if (bet.side == Side.OVER) Palette.Over else Palette.Under
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BookMark(bet.book)
            Spacer(Modifier.width(8.dp))
            Text(
                bookLabel(bet.book).uppercase(),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
            )
            Badge(
                when {
                    bet.outcome == Outcome.WIN -> "WON"
                    bet.outcome == Outcome.LOSS -> "LOST"
                    bet.outcome == Outcome.PUSH -> "PUSHED"
                    bet.outcome == Outcome.VOID -> "VOID"
                    else -> "OPEN"
                },
                when (bet.outcome) {
                    Outcome.WIN -> Palette.Gain
                    Outcome.LOSS -> Palette.Loss
                    Outcome.PUSH, Outcome.VOID -> Palette.Dust
                    else -> Palette.Caution
                },
                filled = bet.outcome != null,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "FORMULA · ${bet.formulaName.uppercase()}",
            style = MaterialTheme.typography.labelSmall, color = Palette.Over,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(bet.playerName, style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(sideColor, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${bet.side.label} ${fmtLine(bet.line)} ${bet.marketLabel}".trim(),
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fmtOdds(bet.odds), style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
                Text(
                    "to win ${"%.2f".format(bet.toWin)} u",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Palette.Hairline)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "RISK ${"%.2f".format(bet.stake)} u  ·  RETURN ${"%.2f".format(bet.toReturn)} u",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "placed ${fmtAgo(bet.decidedUtc)}" + if (bet.actual != null) "  ·  finished ${fmtLine(bet.actual)}" else "",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
            }
            action()
        }
    }
}

@Composable
private fun BookHeading(book: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            bookLabel(book).uppercase(),
            style = MaterialTheme.typography.labelMedium, color = Palette.Chalk,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$count open",
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
    }
}

@Composable
private fun SettledResult(bet: BetUi) {
    val profit = bet.profit ?: 0.0
    val color = when (bet.outcome) { Outcome.WIN -> Palette.Gain; Outcome.LOSS -> Palette.Loss; else -> Palette.Dust }
    Column(horizontalAlignment = Alignment.End) {
        Text(fmtSigned(profit, 2), style = MaterialTheme.typography.titleMedium, color = color)
        Text(
            "${bet.outcome?.name?.lowercase()?.replaceFirstChar { it.uppercase() }}, final ${fmtLine(bet.actual)}",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
    }
}

@Composable
private fun ResultDialog(bet: BetUi, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    val value = text.toDoubleOrNull()?.takeIf { it >= 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Raised,
        title = { Text("Record the result", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk) },
        text = {
            Column {
                Text(
                    "${bet.playerName}, ${bet.side.label} ${fmtLine(bet.line)}. Enter the " +
                        "${bet.marketLabel.lowercase()} the player finished with.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    label = { Text("Final ${bet.marketLabel.lowercase()}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = { TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) { Text("Settle bet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * The gamification strip: the current run, the last results as dots, and the milestones
 * the record has earned. Every number is read off the stored bets, so a badge can only
 * ever celebrate something that actually happened.
 */
@Composable
private fun StreakCard(state: PortfolioState) {
    val streak = PracticeLedger.streak(state.settled)
    val best = PracticeLedger.bestStreak(state.settled)
    val form = PracticeLedger.form(state.settled)
    val badges = PracticeLedger.badges(state.open, state.settled)
    val earned = badges.count { it.earned }
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "FORM AND STREAK",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                modifier = Modifier.weight(1f),
            )
            when {
                streak > 0 -> Badge("$streak IN A ROW", Palette.Gain, filled = true)
                streak < 0 -> Badge("${-streak} LOSING RUN", Palette.Loss, filled = true)
                else -> Badge("NO RUN YET", Palette.Faint)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (form.isEmpty()) {
            Text(
                "No settled bets yet. Your form strip starts with your first result.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                form.forEach { outcome ->
                    val tone = if (outcome == Outcome.WIN) Palette.Gain else Palette.Loss
                    val letter = if (outcome == Outcome.WIN) "W" else "L"
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(tone.copy(alpha = 0.18f))
                            .border(1.dp, tone, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(letter, style = MaterialTheme.typography.labelSmall, color = tone)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "best run $best",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "MILESTONES · $earned of ${badges.size}",
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            badges.forEach { badge -> MilestoneChip(badge) }
        }
    }
}

@Composable
private fun MilestoneChip(badge: Milestone) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.clip(shape)
            .background(if (badge.earned) Palette.Gain.copy(alpha = 0.12f) else Palette.Ink, shape)
            .border(1.dp, if (badge.earned) Palette.Gain.copy(alpha = 0.5f) else Palette.Hairline, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            badge.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (badge.earned) Palette.Chalk else Palette.Faint,
            maxLines = 1,
        )
        Text(
            badge.detail,
            style = MaterialTheme.typography.labelSmall,
            color = if (badge.earned) Palette.Gain else Palette.Faint,
            maxLines = 1,
        )
    }
}

/**
 * What the user's own settled bets teach. This is the self-learning loop with the user's
 * own record as the input: the same segment analysis the system runs on itself, naming
 * the sides, markets and price bands this bettor keeps taking. Each rule a finding
 * implies can be filed as a testable strategy with one tap — the bench checks it on a
 * fresh simulated season, and the sandbox gate decides whether it ever trades. Nothing
 * here changes a live probability by itself.
 */
@Composable
private fun UserInsightsCard(
    insights: UserInsightsUi,
    onTest: (StrategyParams, String, String) -> Unit,
) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "WHAT YOUR BETS TAUGHT THE SYSTEM",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                modifier = Modifier.weight(1f),
            )
            Badge("${insights.considered} SETTLED", Palette.Over)
        }
        Spacer(Modifier.height(10.dp))
        if (insights.findings.isEmpty()) {
            Text(
                if (insights.note.isNotBlank()) insights.note
                else "Place and settle paper bets and this card names your habits: which side, market or " +
                    "price band you keep taking, and whether it is paying.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        } else {
            insights.findings.forEachIndexed { index, finding ->
                if (index > 0) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Palette.Hairline)
                    Spacer(Modifier.height(12.dp))
                }
                UserFindingRow(finding, onTest)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "A finding describes your record; it is not a prediction. Testing a rule files it as a " +
                "strategy the sandbox has to clear like any other.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
    }
}

@Composable
private fun UserFindingRow(
    finding: UserFindingUi,
    onTest: (StrategyParams, String, String) -> Unit,
) {
    val tone = when (finding.kind) {
        InsightKind.LEAK -> Palette.Loss
        InsightKind.EDGE -> Palette.Gain
        InsightKind.PATTERN -> Palette.Over
        InsightKind.TREND -> Palette.Caution
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Badge(finding.kind.label.uppercase(), tone, filled = true)
        Spacer(Modifier.width(8.dp))
        Text(
            finding.title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(finding.detail, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
    Spacer(Modifier.height(4.dp))
    Text(finding.advice, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
    if (finding.proposalLabel != null) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Rule it implies: ${finding.proposalLabel}",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            val proposal = finding.proposal
            if (proposal != null && finding.status == "NOT TESTED YET") {
                Spacer(Modifier.width(8.dp))
                ChoiceChip(
                    "Test as a formula",
                    selected = false,
                    onClick = { onTest(proposal, finding.title, finding.advice) },
                )
            } else if (finding.status != null) {
                Spacer(Modifier.width(8.dp))
                Text(finding.status, style = MaterialTheme.typography.labelSmall, color = Palette.Dust)
            }
        }
    }
}
