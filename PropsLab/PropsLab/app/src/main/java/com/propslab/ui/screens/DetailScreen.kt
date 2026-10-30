package com.propslab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.data.Driver
import com.propslab.data.PlaceResult
import com.propslab.data.PropUi
import com.propslab.data.PropsRepository
import com.propslab.domain.Pricing
import com.propslab.domain.Side
import com.propslab.domain.Staking
import com.propslab.ui.components.KeyValue
import com.propslab.ui.components.SectionTitle
import com.propslab.ui.components.SplitRing
import com.propslab.ui.components.TeamMark
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.fmtPct
import com.propslab.ui.fmtSigned
import com.propslab.ui.fmtStamp
import com.propslab.ui.fmtTime
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class DetailViewModel(private val repo: PropsRepository, private val id: String) : ViewModel() {
    val prop: StateFlow<PropUi?> = repo.prop(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun place(side: Side, stake: Double) {
        viewModelScope.launch {
            _messages.emit(
                when (val result = repo.placeBet(id, side, stake)) {
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
fun DetailScreen(id: String, onBack: () -> Unit) {
    val repo = rememberRepo()
    val vm: DetailViewModel = viewModel(key = id, factory = viewModelFactory { initializer { DetailViewModel(repo, id) } })
    val prop by vm.prop.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var stake by remember { mutableDoubleStateOf(1.0) }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    val p = prop
    Scaffold(
        containerColor = Palette.Ink,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = { if (p != null && p.available) BetBar(p, stake, { stake = it }) { side -> vm.place(side, stake) } },
    ) { pad ->
        if (p == null) return@Scaffold
        LazyColumn(Modifier.fillMaxSize(), contentPadding = pad) {
            item { BackRow(onBack) }
            item { PlayerHeader(p) }
            item { if (p.available) Hero(p) else Unavailable(p) }
            if (p.available) {
                item { Prices(p) }
                item { MarketCrossCheck(p) }
                item { Why(p) }
                if (p.drivers.isNotEmpty()) item { Drivers(p.drivers) }
            }
            item { Quality(p) }
            item { Provenance(p) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    Text(
        "\u2039  Slate",
        Modifier.clickable(onClick = onBack).padding(horizontal = 20.dp, vertical = 14.dp),
        style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
    )
}

@Composable
private fun PlayerHeader(p: PropUi) {
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        TeamMark(p.teamCode, size = 52.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(p.playerName, style = MaterialTheme.typography.headlineMedium, color = Palette.Chalk)
            Text("${p.matchup}, ${fmtTime(p.startUtc)}. ${p.market} ${fmtLine(p.line)}", style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
        }
    }
}

@Composable
private fun Hero(p: PropUi) {
    val probs = p.probabilities ?: return
    val push = probs.push.toFloat()
    val lean = p.lean
    val edge = p.edgePts ?: 0.0
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SplitRing(
            over = probs.over.toFloat(), push = push,
            market = p.marketOver?.let { (it * (1 - push)).toFloat() },
            modifier = Modifier.width(260.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fmtPct(probs.over), style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp), color = Palette.Chalk)
                Text("chance of the over", style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(IntrinsicSize.Min)) {
            Stat("Our estimate", fmtPct(probs.overGivenNoPush), Palette.Chalk, Modifier.weight(1f))
            VerticalRule()
            Stat("Book, no margin", p.marketOver?.let { fmtPct(it) } ?: "none", Palette.Chalk, Modifier.weight(1f))
            VerticalRule()
            Stat(
                "Gap vs book · ${lean?.label ?: "line"}",
                "${fmtSigned(abs(edge))} pts",
                when (lean) { Side.OVER -> Palette.Over; Side.UNDER -> Palette.Under; null -> Palette.Chalk },
                Modifier.weight(1f),
            )
        }
        if (probs.push > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Push (tie) chance ${fmtPct(probs.push)}: land exactly on the line and the stake comes back.", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
    }
}

@Composable
private fun VerticalRule() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Hairline))
}

@Composable
private fun Unavailable(p: PropUi) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)) {
        Text("No estimate", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
        Spacer(Modifier.height(8.dp))
        p.reasons.forEach {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Box(Modifier.size(8.dp).background(Palette.Caution, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = Palette.Chalk)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Cheat Code shows nothing rather than a guess when an input is old, missing or outside the range it was validated on.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
    }
}

@Composable
private fun Prices(p: PropUi) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PriceCell("Over ${fmtLine(p.line)}", fmtOdds(p.overOdds), Palette.Over, Modifier.weight(1f))
        PriceCell("Under ${fmtLine(p.line)}", fmtOdds(p.underOdds), Palette.Under, Modifier.weight(1f))
    }
}

@Composable
private fun PriceCell(label: String, odds: String, color: Color, modifier: Modifier) {
    Row(modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(color, CircleShape))
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
            Text(odds, style = MaterialTheme.typography.headlineMedium, color = Palette.Chalk)
        }
    }
}

/**
 * The price check a bettor actually needs: what the book charges, what the model
 * says is fair, and the expected value of one unit at the quoted price.
 */
@Composable
private fun MarketCrossCheck(p: PropUi) {
    val probs = p.probabilities ?: return
    val over = p.overOdds ?: return
    val under = p.underOdds ?: return
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle("Book price vs fair price")
        Spacer(Modifier.height(6.dp))
        Text(
            "Fair is the price with nothing built in for the book — expected value exactly zero. Value is the average profit per 1 unit at that price, counting ties.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Side", Modifier.weight(0.22f), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Text("Book", Modifier.weight(0.20f), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Text("Fair", Modifier.weight(0.20f), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Text(
                "Value / unit", Modifier.weight(0.38f),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        HorizontalDivider(Modifier.padding(top = 8.dp), color = Palette.Hairline)
        PriceCheckRow(
            "Over", over,
            fairPrice(probs.over, probs.push),
            Staking.expectedValue(probs.over, probs.push, over),
            Palette.Over,
        )
        HorizontalDivider(color = Palette.Hairline)
        PriceCheckRow(
            "Under", under,
            fairPrice(probs.under, probs.push),
            Staking.expectedValue(probs.under, probs.push, under),
            Palette.Under,
        )
        Spacer(Modifier.height(28.dp))
    }
}

private fun fairPrice(pWin: Double, push: Double): Int? =
    if (pWin > 0.0 && pWin + push < 1.0) {
        runCatching { Pricing.fairAmerican(pWin, push) }.getOrNull()
    } else {
        null
    }

@Composable
private fun PriceCheckRow(label: String, book: Int, fair: Int?, ev: Double, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(0.22f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
        }
        Text(fmtOdds(book), Modifier.weight(0.20f), style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
        Text(fmtOdds(fair), Modifier.weight(0.20f), style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
        Text(
            "${fmtSigned(ev * 100.0, 1)}%",
            Modifier.weight(0.38f),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                ev > 0.0005 -> Palette.Gain
                ev < -0.0005 -> Palette.Loss
                else -> Palette.Dust
            },
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun Why(p: PropUi) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle("Why this estimate")
        Spacer(Modifier.height(8.dp))
        Text(p.explanation, style = MaterialTheme.typography.bodyLarge, color = Palette.Chalk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Written from the saved prediction record. The percentages come from the model's own math, never from this text.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Drivers(drivers: List<Driver>) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle("What moved it")
        Text("How many points each input added to or took off the over.", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        Spacer(Modifier.height(12.dp))
        val scale = (drivers.maxOf { abs(it.value) }).coerceAtLeast(1.0)
        drivers.forEach { d ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(d.label, Modifier.weight(0.38f), style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
                Canvas(Modifier.weight(0.46f).height(10.dp)) {
                    val mid = size.width / 2f
                    val len = (mid * (abs(d.value) / scale)).toFloat()
                    val r = CornerRadius(size.height / 2f)
                    drawRoundRect(Palette.Hairline, Offset(mid - 1f, 0f), Size(2f, size.height))
                    if (d.value >= 0) drawRoundRect(Palette.Over, Offset(mid, 0f), Size(len.coerceAtLeast(4f), size.height), r)
                    else drawRoundRect(Palette.Under, Offset(mid - len.coerceAtLeast(4f), 0f), Size(len.coerceAtLeast(4f), size.height), r)
                }
                Text(
                    fmtSigned(d.value), Modifier.weight(0.16f),
                    style = MaterialTheme.typography.labelMedium, color = Palette.Dust,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Quality(p: PropUi) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle("Data quality")
        Spacer(Modifier.height(6.dp))
        if (p.flags.isEmpty()) {
            Text("No flags were raised for this estimate.", style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
        } else p.flags.forEach {
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 7.dp).size(6.dp).background(Palette.Caution, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Provenance(p: PropUi) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle("Where this came from")
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Palette.Hairline)
        KeyValue("Model", p.modelVersion)
        KeyValue("Inputs schema", "Version ${p.schemaVersion}")
        KeyValue("Data as of", fmtStamp(p.featureCutoffUtc))
        KeyValue("Price from", "${p.book}, ${fmtStamp(p.quoteTimeUtc)}")
        KeyValue("Source", if (p.isFixture) "Synthetic fixture" else "Recorded quote")
    }
}

@Composable
private fun BetBar(p: PropUi, stake: Double, onStake: (Double) -> Unit, onBet: (Side) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Palette.Court).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Paper stake", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            Spacer(Modifier.weight(1f))
            listOf(0.5, 1.0, 2.0, 5.0).forEach { v ->
                val on = v == stake
                Text(
                    "%.1f".format(v),
                    Modifier.padding(start = 8.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (on) Palette.Chalk else Palette.Hairline, RoundedCornerShape(10.dp))
                        .clickable { onStake(v) }.heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = if (on) Palette.Chalk else Palette.Dust,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SideButton(p.lean == Side.OVER, "Over ${fmtOdds(p.overOdds)}", Palette.Over, Modifier.weight(1f)) { onBet(Side.OVER) }
            SideButton(p.lean == Side.UNDER, "Under ${fmtOdds(p.underOdds)}", Palette.Under, Modifier.weight(1f)) { onBet(Side.UNDER) }
        }
    }
}

@Composable
private fun SideButton(filled: Boolean, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val text = "Paper bet $label"
    if (filled) {
        Button(onClick, modifier.heightIn(min = 52.dp), shape = shape, colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Palette.Ink)) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(onClick, modifier.heightIn(min = 52.dp), shape = shape, border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color)) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
