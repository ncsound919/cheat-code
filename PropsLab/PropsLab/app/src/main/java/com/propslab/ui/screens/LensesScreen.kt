package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.claim.ClaimLens
import com.propslab.data.ClaimFormulaEntity
import com.propslab.data.FormulaUi
import com.propslab.data.PropsRepository
import com.propslab.data.SandboxUi
import com.propslab.domain.Markets
import com.propslab.settings.AppSettings
import com.propslab.sim.SideFilter
import com.propslab.sim.StrategyParams
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PlainLink
import com.propslab.ui.fmtPct
import com.propslab.ui.fmtSigned
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class LensesViewModel(private val repo: PropsRepository) : ViewModel() {
    val claims: StateFlow<List<ClaimFormulaEntity>> =
        repo.claims().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sandbox: StateFlow<SandboxUi> =
        repo.sandbox().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SandboxUi())

    var busyClaim: String? by mutableStateOf(null)
        private set
    var message: String? by mutableStateOf(null)
        private set

    fun clearMessage() {
        message = null
    }

    fun deepTest(id: String) {
        if (busyClaim != null) return
        busyClaim = id
        message = null
        viewModelScope.launch {
            val reply = runCatching { repo.runDeepTest(id) }
                .getOrElse { it.message ?: "The deep test could not run." }
            busyClaim = null
            message = reply
        }
    }

    /** Collected once when the board opens: a run pushed last time may have finished since. */
    fun resumePending() {
        viewModelScope.launch {
            val open = claims.value.filter { it.deepVerdict == "RUNNING" }
            for (claim in open) {
                val reply = runCatching { repo.resumeDeepTest(claim.id) }.getOrNull() ?: continue
                message = reply
            }
        }
    }
}

/**
 * The lens board.
 *
 * One card per formula, and every card answers the same three questions in the same order:
 * what it looks at, what it does with a price, and what would prove it wrong. The claim
 * cards are the user's own sentences; the system cards are the grid the autopilot searches.
 * The arithmetic is present but never leads — a non-bettor can read the first two lines and
 * know what the thing does.
 */
@Composable
fun LensesScreen(onBack: () -> Unit, onOpenLab: () -> Unit, onOpenBest: () -> Unit) {
    val repo = rememberRepo()
    val vm: LensesViewModel = viewModel(factory = viewModelFactory { initializer { LensesViewModel(repo) } })
    val claims by vm.claims.collectAsStateWithLifecycle()
    val sandbox by vm.sandbox.collectAsStateWithLifecycle()
    val easy = AppSettings.current.easyMode
    LaunchedEffect(Unit) { vm.resumePending() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
            // Clear the navigation bar: a button sitting in the gesture zone swallows taps
            // as "back", which made the deep-test action unreachable on a gesture-nav phone.
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 48.dp,
        ),
    ) {
        item { LensBack(onBack, onOpenBest) }
        item { LensHeader() }
        if (vm.message != null) {
            item { LensMessage(vm.message.orEmpty()) { vm.clearMessage() } }
        }
        if (claims.isEmpty()) {
            item { NoClaimsYet(onOpenLab) }
        } else {
            item { SectionLabel("YOUR IDEAS") }
            items(claims, key = { "claim-${it.id}" }) { claim ->
                ClaimCard(
                    claim = claim,
                    live = sandbox.live?.id == claim.formulaId,
                    busy = vm.busyClaim == claim.id,
                    onDeepTest = { vm.deepTest(claim.id) },
                )
                Spacer(Modifier.height(14.dp))
            }
        }
        item {
            SectionLabel("THE SYSTEM'S FORMULAS")
            Spacer(Modifier.height(2.dp))
        }
        val claimFormulaIds = claims.map { it.formulaId }.toSet()
        items(sandbox.formulas.filter { it.id !in claimFormulaIds }, key = { "sys-${it.id}" }) { formula ->
            SystemCard(formula, easy)
            Spacer(Modifier.height(14.dp))
        }
        item { LensFooter() }
    }
}

@Composable
private fun LensBack(onBack: () -> Unit, onOpenBest: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "\u2039  Lab",
            Modifier.clickable(onClick = onBack),
            style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
        )
        Spacer(Modifier.weight(1f))
        PlainLink(onOpenBest, label = "Best bets")
    }
}

@Composable
private fun LensHeader() {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Strategy lenses", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
        Spacer(Modifier.height(6.dp))
        Text(
            "A lens is not a prediction. It is the rule that decides which disagreements the app is " +
                "allowed to bet, and which ones it has to leave alone. Every card shows that rule in " +
                "plain words before it shows a single number.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall, color = Palette.Over,
    )
}

@Composable
private fun NoClaimsYet(onOpenLab: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Panel {
            Text("No ideas of your own yet", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
            Spacer(Modifier.height(8.dp))
            Text(
                "In the Lab, write what you think will happen — \"their defense decides this one\" or " +
                    "\"he will go under because the game gets away from them\" — and the app turns it " +
                    "into a lens it can test.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
            )
            Spacer(Modifier.height(14.dp))
            ActionButton("Make a strategy", onOpenLab, tone = Palette.Over)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LensMessage(text: String, onDismiss: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Panel(tone = Palette.Raised) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
            Spacer(Modifier.height(10.dp))
            Text(
                "Dismiss",
                Modifier.clickable(onClick = onDismiss),
                style = MaterialTheme.typography.labelMedium, color = Palette.Over,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ClaimCard(claim: ClaimFormulaEntity, live: Boolean, busy: Boolean, onDeepTest: () -> Unit) {
    var showNumbers by remember(claim.id) { mutableStateOf(false) }
    var showWorking by remember(claim.id) { mutableStateOf(false) }
    val tone = if (claim.passed) Palette.Gain else Palette.Caution
    Column(Modifier.padding(horizontal = 20.dp)) {
        Panel(tone = if (claim.passed) Palette.Court else Palette.Raised) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    claim.name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, color = Palette.Chalk,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Badge(
                    when {
                        live -> "LIVE"
                        claim.passed -> "IN THE SANDBOX"
                        else -> "DIDN'T PASS"
                    },
                    tone,
                    filled = true,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "\u201c${claim.claim}\u201d",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
            Spacer(Modifier.height(10.dp))
            Text(claim.headline, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
            Spacer(Modifier.height(4.dp))
            Text(claim.boardNote, style = MaterialTheme.typography.bodySmall, color = tone)
            Spacer(Modifier.height(14.dp))
            Text("IN REAL TERMS", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
            Spacer(Modifier.height(4.dp))
            claim.inGameTerms.forEach { term ->
                LensBullet(term)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(6.dp).background(Palette.Loss, CircleShape))
                Spacer(Modifier.width(9.dp))
                Text(
                    "What would prove it wrong: ${claim.falsifier}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (showNumbers) "Hide the numbers" else "Show the numbers",
                Modifier.clickable { showNumbers = !showNumbers },
                style = MaterialTheme.typography.labelMedium, color = Palette.Over,
            )
            if (showNumbers) {
                Spacer(Modifier.height(8.dp))
                NumberGrid(
                    bets = claim.bets,
                    roi = claim.roi,
                    tStat = claim.tStat,
                    drawdown = claim.maxDrawdown,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "A positive return is not proof. The t-statistic asks whether a run this size could " +
                        "be luck; below 2 the answer is yes.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (showWorking) "Hide the working" else "Show the working",
                Modifier.clickable { showWorking = !showWorking },
                style = MaterialTheme.typography.labelMedium, color = Palette.Over,
            )
            if (showWorking) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Compiled lens: ${claim.lens}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tested on the app's simulated season, ${claim.bets} bets taken. The claim's own " +
                        "sentence is never executed; it selects a factor, and the factor supplies the numbers.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            claim.deepVerdict == "PASS" -> "DEEP TEST PASSED"
                            claim.deepVerdict == "FAIL" -> "DEEP TEST FAILED"
                            claim.deepVerdict == "RUNNING" -> "DEEP TEST RUNNING"
                            claim.deepAtUtc > 0 -> "DEEP TEST: " + claim.deepVerdict.replace('_', ' ')
                            else -> "DEEP TEST NOT RUN"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            claim.deepVerdict == "PASS" -> Palette.Gain
                            claim.deepVerdict == "FAIL" -> Palette.Loss
                            claim.deepVerdict == "RUNNING" -> Palette.Caution
                            claim.deepAtUtc > 0 -> Palette.Dust
                            else -> Palette.Faint
                        },
                    )
                    if (claim.deepNote.isNotBlank()) {
                        Text(
                            claim.deepNote,
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                            maxLines = 4, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                ActionButton(
                    if (busy) "Running…" else "Deep test on Kaggle",
                    onDeepTest,
                    enabled = !busy,
                    tone = Palette.Over,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SystemCard(formula: FormulaUi, easy: Boolean) {
    var showNumbers by remember(formula.id) { mutableStateOf(!easy) }
    val params = remember(formula.params) { StrategyParams.parse(formula.params) }
    val tone = when {
        formula.isLive -> Palette.Gain
        formula.passed -> Palette.Caution
        else -> Palette.Faint
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Panel(tone = Palette.Court) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formula.label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Badge(
                    when {
                        formula.isLive -> "LIVE BOOK"
                        formula.isRetired -> "RETIRED"
                        formula.passed -> "READY"
                        else -> "PRACTISING"
                    },
                    tone,
                    filled = formula.isLive,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (params != null) {
                Text(
                    lensHeadline(params),
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk,
                )
                Spacer(Modifier.height(10.dp))
                Text("IN REAL TERMS", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
                Spacer(Modifier.height(4.dp))
                lensTerms(params).forEach { LensBullet(it) }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (formula.passed) {
                        "It cleared every check on a season it had not seen, so its picks are allowed on the board."
                    } else {
                        "It has not cleared the checks yet. Its picks are shown as a rehearsal, never as the live list."
                    },
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            } else {
                Text(formula.params, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (showNumbers) "Hide the numbers" else "Show the numbers",
                Modifier.clickable { showNumbers = !showNumbers },
                style = MaterialTheme.typography.labelMedium, color = Palette.Over,
            )
            if (showNumbers) {
                Spacer(Modifier.height(8.dp))
                NumberGrid(
                    bets = formula.bets,
                    roi = formula.roi,
                    tStat = formula.tStat,
                    drawdown = formula.maxDrawdown,
                )
                if (formula.reasons.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "What is holding it back: ${formula.reasons.first()}",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun LensBullet(text: String) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 6.dp).size(5.dp).background(Palette.Over, CircleShape))
        Spacer(Modifier.width(9.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
    }
}

@Composable
private fun NumberGrid(bets: Int, roi: Double, tStat: Double, drawdown: Double) {
    Row(Modifier.fillMaxWidth()) {
        Number("Bets", "$bets", Modifier.weight(1f))
        Number("Return", fmtPct(roi), Modifier.weight(1f), if (roi >= 0) Palette.Gain else Palette.Loss)
        Number("Luck check", "%.2f".format(tStat), Modifier.weight(1f), if (tStat >= 2.0) Palette.Gain else Palette.Dust)
        Number("Worst dip", fmtPct(drawdown, 0), Modifier.weight(1f))
    }
}

@Composable
private fun Number(label: String, value: String, modifier: Modifier = Modifier, tone: androidx.compose.ui.graphics.Color = Palette.Chalk) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Text(value, style = MaterialTheme.typography.titleSmall, color = tone)
    }
}

/** The same lens, worded for a formula the autopilot found rather than a sentence a user wrote. */
private fun lensHeadline(params: StrategyParams): String {
    val side = when (params.sideFilter) {
        SideFilter.OVER_ONLY -> "It only backs overs"
        SideFilter.UNDER_ONLY -> "It only backs unders"
        SideFilter.BOTH -> "It backs whichever side the app likes"
    }
    val tilt = when {
        params.tiltPts >= 0.05 -> ", after nudging the app's number ${"%.1f".format(params.tiltPts)} points higher"
        params.tiltPts <= -0.05 -> ", after nudging the app's number ${"%.1f".format(abs(params.tiltPts))} points lower"
        else -> ""
    }
    val market = if (params.markets.isEmpty()) "" else " on ${params.markets.joinToString("/") { Markets.label(it).lowercase() }}"
    return "$side$market$tilt."
}

private fun lensTerms(params: StrategyParams): List<String> = buildList {
    add(
        "It waits for a gap of at least ${"%.1f".format(params.minEdgePts)} points between the app's " +
            "probability and the book's own no-margin price. Anything closer is a coin flip it refuses.",
    )
    add(
        "It only takes a price between ${"%.2f".format(params.minPrice)} and " +
            "${"%.2f".format(params.maxPrice)} decimal. Outside that band the payout does not cover the risk.",
    )
    if (params.tiltPts >= 0.05) {
        add(
            "It adds ${"%.1f".format(params.tiltPts)} points to the app's chance of the over before comparing. " +
                "That is the idea doing the work: without it, this is just the app's own number.",
        )
    } else if (params.tiltPts <= -0.05) {
        add(
            "It takes ${"%.1f".format(abs(params.tiltPts))} points off the app's chance of the over before comparing. " +
                "That is the idea doing the work: without it, this is just the app's own number.",
        )
    } else {
        add("It does not move the app's probability at all — the filter is the whole strategy.")
    }
    if (params.markets.isNotEmpty()) {
        add("It never bets a market outside ${params.markets.joinToString(", ") { Markets.label(it) }}.")
    }
}

@Composable
private fun LensFooter() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text(
            "A formula is only ever a rule for selecting bets. It cannot change a price, place a wager, " +
                "or reach a sportsbook. The numbers above come from the app's own simulated season; the " +
                "Kaggle deep test is the slower, real-data check, and it is the one that can say an idea " +
                "is not there.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "The claim sentence is never executed. It selects one factor from a fixed vocabulary; the " +
                "factor supplies a tilt, a side and a market, and those numbers go through the same gate " +
                "as every other formula.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
    }
}
