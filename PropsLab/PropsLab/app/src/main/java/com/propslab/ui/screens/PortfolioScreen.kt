package com.propslab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.data.BetUi
import com.propslab.data.PropsRepository
import com.propslab.domain.Outcome
import com.propslab.domain.Side
import com.propslab.ui.components.Badge
import com.propslab.ui.components.BookMark
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PlainLink
import com.propslab.ui.components.SectionTitle
import com.propslab.ui.components.bookLabel
import com.propslab.ui.fmtAgo
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.fmtSigned
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PortfolioState(val open: List<BetUi> = emptyList(), val settled: List<BetUi> = emptyList()) {
    val profit: Double get() = settled.sumOf { it.profit ?: 0.0 }
    val staked: Double get() = settled.sumOf { it.stake }
    val wins: Int get() = settled.count { it.outcome == Outcome.WIN }
    val losses: Int get() = settled.count { it.outcome == Outcome.LOSS }
    val pushes: Int get() = settled.count { it.outcome == Outcome.PUSH }

    /** Profit per unit staked across settled bets; null until something is settled. */
    val roi: Double? get() = if (staked > 0.0) profit / staked else null

    /** Running total after each settled bet, oldest first, starting from zero. */
    val series: List<Double>
        get() = settled.sortedBy { it.settledUtc ?: 0L }.runningFold(0.0) { acc, b -> acc + (b.profit ?: 0.0) }
}

class PortfolioViewModel(private val repo: PropsRepository) : ViewModel() {
    val state: StateFlow<PortfolioState> = repo.bets().map { bets ->
        PortfolioState(open = bets.filter { it.isOpen }, settled = bets.filter { !it.isOpen }.sortedByDescending { it.settledUtc })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioState())

    fun settle(betId: String, actual: Double) {
        viewModelScope.launch { repo.settleBet(betId, actual) }
    }
}

@Composable
fun PortfolioScreen(onOpenPlain: () -> Unit) {
    val repo = rememberRepo()
    val vm: PortfolioViewModel = viewModel(factory = viewModelFactory { initializer { PortfolioViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    var settling by remember { mutableStateOf<BetUi?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, bottom = 120.dp),
    ) {
        item { Summary(state, onOpenPlain) }
        if (state.open.isEmpty() && state.settled.isEmpty()) {
            item { EmptyPortfolio() }
        }
        if (state.open.isNotEmpty()) {
            item { SectionTitle("Open bets", Modifier.padding(start = 20.dp, top = 32.dp, bottom = 2.dp)) }
            item {
                Text(
                    "Each slip is the bet as you would enter it at the book: the market, the number, " +
                        "the price and the stake. Grouped by the book that quoted the price.",
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
            state.open.groupBy { it.book.ifBlank { "Best price" } }.forEach { (book, bets) ->
                item(key = "book-$book") { BookHeading(book, bets.size) }
                items(bets, key = { it.id }) { bet ->
                    BetSlip(bet, action = {
                        Text(
                            "Record result",
                            Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Palette.Hairline, RoundedCornerShape(10.dp))
                                .clickable { settling = bet }.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium, color = Palette.Chalk,
                        )
                    })
                }
            }
        }
        if (state.settled.isNotEmpty()) {
            item { SectionTitle("Settled", Modifier.padding(start = 20.dp, top = 32.dp, bottom = 6.dp)) }
            items(state.settled, key = { it.id }) { bet ->
                BetSlip(bet, action = { SettledResult(bet) })
            }
        }
    }

    settling?.let { bet ->
        ResultDialog(bet, onDismiss = { settling = null }, onConfirm = { vm.settle(bet.id, it); settling = null })
    }
}

@Composable
private fun Summary(s: PortfolioState, onOpenPlain: () -> Unit) {
    val tone = when {
        s.settled.isEmpty() -> Palette.Chalk
        s.profit > 0.005 -> Palette.Gain
        s.profit < -0.005 -> Palette.Loss
        else -> Palette.Chalk
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Portfolio", Modifier.weight(1f), style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
            PlainLink(onOpenPlain)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (s.settled.isEmpty()) "0.00" else fmtSigned(s.profit, 2),
            style = MaterialTheme.typography.displayLarge, color = tone,
        )
        Text("practice units from finished bets", style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
        if (s.settled.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "return ${fmtSigned((s.roi ?: 0.0) * 100, 1)}%",
                    style = MaterialTheme.typography.labelLarge, color = tone,
                )
                Spacer(Modifier.weight(1f))
                Text("${s.settled.size} settled", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            }
        }
        if (s.series.size >= 3) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Running profit", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                Spacer(Modifier.weight(1f))
                Text(fmtSigned(s.series.last(), 2), style = MaterialTheme.typography.labelMedium, color = tone)
            }
            Spacer(Modifier.height(4.dp))
            Sparkline(s.series, tone)
            Row(Modifier.fillMaxWidth()) {
                Text("first finished", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.weight(1f))
                Text("latest", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "${s.wins} won, ${s.losses} lost, ${s.pushes} pushed (stake back), ${"%.1f".format(s.staked)} units staked. Practice only — no real money moves.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
    }
}

@Composable
private fun EmptyPortfolio() {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
            .clip(shape).border(1.dp, Palette.Hairline, shape).padding(20.dp),
    ) {
        Text("No paper bets yet", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Open a prop on Tonight and place a practice bet to start your ledger. Nothing here moves real money.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
    }
}

@Composable
private fun Sparkline(values: List<Double>, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        val lo = minOf(values.min(), 0.0)
        val hi = maxOf(values.max(), 0.0)
        val span = (hi - lo).coerceAtLeast(0.5)
        val pad = 6.dp.toPx()
        fun x(i: Int) = pad + (size.width - 2 * pad) * i / (values.size - 1)
        fun y(v: Double) = (size.height - pad - (size.height - 2 * pad) * ((v - lo) / span)).toFloat()
        val zeroY = y(0.0)
        drawLine(Palette.Hairline, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1.dp.toPx())
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, 4.dp.toPx(), Offset(x(values.lastIndex), y(values.last())))
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
                    else -> "OPEN"
                },
                when (bet.outcome) {
                    Outcome.WIN -> Palette.Gain
                    Outcome.LOSS -> Palette.Loss
                    Outcome.PUSH -> Palette.Dust
                    else -> Palette.Caution
                },
                filled = bet.outcome != null,
            )
        }
        Spacer(Modifier.height(12.dp))
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
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    "${bet.playerName}, ${bet.side.label} ${fmtLine(bet.line)}. Enter the points the player finished with.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true, label = { Text("Final points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = { TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) { Text("Settle bet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
