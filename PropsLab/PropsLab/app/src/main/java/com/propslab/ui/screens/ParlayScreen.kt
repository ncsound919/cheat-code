package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import com.propslab.data.ParlayControls
import com.propslab.data.ParlayKind
import com.propslab.data.ParlayLab
import com.propslab.data.ParlayLegUi
import com.propslab.data.ParlayStrategyUi
import com.propslab.data.ParlayTickets
import com.propslab.data.ParlayTicketUi
import com.propslab.data.ParlayUi
import com.propslab.data.PropsRepository
import com.propslab.settings.AppSettings
import com.propslab.settings.stakeWord
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.AnimatedPercent
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Metric
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PlainLink
import com.propslab.ui.components.TeamMark
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class ParlayViewModel(repo: PropsRepository) : ViewModel() {

    private val controls = MutableStateFlow(ParlayControls())
    private val board = repo.parlayStrategies()

    val state: StateFlow<ParlayUi> = combine(board, controls) { b, c ->
        // The ten-leg ticket is the one paid feature in the builder. Everything else on this
        // screen, including the maths and the prices, is the same build.
        val sizes = ParlayTickets.SIZES.filter { it != 10 || TierStore.allows(Feature.PARLAY_10) }
        ParlayLab.compose(b, c, sizes, AppSettings.current.stakeFraction)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ParlayUi())

    fun setPush(points: Double) = controls.update {
        it.copy(pushPoints = (points * 2.0).roundToInt() / 2.0)
    }

    fun setBudget(text: String) = controls.update {
        it.copy(budget = text.filter { ch -> ch.isDigit() || ch == '.' }.take(9))
    }

    fun setCircadian(on: Boolean) = controls.update { it.copy(circadian = on) }

    fun setStrategy(ticket: String, id: String) = controls.update {
        it.copy(byTicket = it.byTicket + (ticket to id))
    }
}

/**
 * The day's parlays, built from the picks the chosen formula takes.
 *
 * Three things are on this screen that the old builder did not have, and each one is a
 * trade the user makes explicitly rather than a number the app quietly rearranges:
 *
 *  - **Budget.** A stated amount is split across the tickets at the settings screen's Kelly
 *    fraction, capped per ticket, and the remainder stays unallocated.
 *  - **Line push.** Moving a leg's line in your favour buys a bigger price with a lower
 *    chance. The slider applies it to every player leg the app can reprice, and the ticket's
 *    probability and price move together.
 *  - **Formula per ticket.** Each card can be built from a different rung of the sandbox
 *    ladder, so the same board can show a strict ticket beside a broad one.
 */
@Composable
fun ParlayScreen(onBack: () -> Unit, onOpenPlain: () -> Unit, onOpenPremium: () -> Unit) {
    val repo = rememberRepo()
    val vm: ParlayViewModel = viewModel(factory = viewModelFactory { initializer { ParlayViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    var slipTicket by remember { mutableStateOf<ParlayTicketUi?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = 44.dp,
            ),
        ) {
            item {
                Text(
                    "\u2039  Best bets",
                    Modifier.clickable(onClick = onBack).padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
                )
            }
            item { ParlayHeader(state, onOpenPlain) }
            item { BudgetPanel(state, vm) }
            item { PushPanel(state, vm) }
            if (state.loaded && state.tickets.isEmpty()) {
                item { EmptyParlay(state) }
            }
            items(state.tickets, key = { it.key }) { ticket ->
                ParlayCard(
                    ticket = ticket,
                    strategies = state.strategies,
                    eligible = state.eligible[ticket.key].orEmpty(),
                    onPick = { id -> vm.setStrategy(ticket.key, id) },
                    onOpenSlip = { slipTicket = ticket },
                )
            }
            if (!TierStore.allows(Feature.PARLAY_10)) {
                item { TenLegLock(onOpenPremium) }
            }
            if (state.strategies.isNotEmpty()) {
                item { ScopePanel(state.strategies) }
            }
            item { ParlayFooter(state) }
        }
        slipTicket?.let { ticket ->
            BetSlipSheet(
                ticket = ticket,
                candidates = state.candidates,
                circadian = state.circadian,
                onDismiss = { slipTicket = null },
            )
        }
    }
}

@Composable
private fun ParlayHeader(state: ParlayUi, onOpenPlain: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Parlay lab", Modifier.weight(1f), style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
            PlainLink(onOpenPlain)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Built from the ${state.priced} ranked picks on today's board.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        if (state.strategy != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    if (state.live) "LIVE STRATEGY" else "TRY-OUT STRATEGY",
                    if (state.live) Palette.Gain else Palette.Caution,
                    filled = true,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    state.strategy,
                    style = MaterialTheme.typography.labelMedium, color = Palette.Dust,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The budget and where it went. The numbers are shown before the tickets because the point
 * of a budget is the split, not the individual card.
 */
@Composable
private fun BudgetPanel(state: ParlayUi, vm: ParlayViewModel) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BUDGET", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            UnitField(state.budgetText, vm::setBudget, "100")
        }
        Spacer(Modifier.height(12.dp))
        if (state.budget <= 0.0) {
            Text(
                "Enter a practice budget and every ticket below gets a stake. Nothing is placed, and no money moves.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
            return@Panel
        }
        Row(Modifier.fillMaxWidth()) {
            Metric("Staked", "%.2f u".format(state.staked), Modifier.weight(1f), tone = Palette.Chalk)
            Metric("Unallocated", "%.2f u".format(state.unallocated), Modifier.weight(1f), tone = Palette.Dust)
            Metric("Budget", "%.2f u".format(state.budget), Modifier.weight(1f), tone = Palette.Faint)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Split at ${stakeWord(AppSettings.current.stakeFraction)}-Kelly, with no ticket allowed more than " +
                "a quarter of the budget. Whatever the edges do not justify is left alone instead of being " +
                "pushed onto the longest shot.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
    }
    state.warnings.forEach { warning ->
        Text(
            warning,
            Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
        )
    }
}

/** A small bordered number field in the app's own voice, in practice units. */
@Composable
private fun UnitField(value: String, onChange: (String) -> Unit, placeholder: String) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.width(120.dp).clip(shape).background(Palette.Raised)
            .border(1.dp, Palette.Hairline, shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                    Text(placeholder, style = MaterialTheme.typography.titleMedium, color = Palette.Faint)
                }
                inner()
            },
        )
        Text("u", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
    }
}

/**
 * The two adjustments, side by side with what they cost.
 *
 * The push is applied to player legs only. A game market has no player game log, so the app
 * cannot honestly reprice a pushed spread or total; those legs keep their quote and say so.
 */
@Composable
private fun PushPanel(state: ParlayUi, vm: ParlayViewModel) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LINE PUSH", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Text(
                    if (state.pushPoints <= 0.0) "Quoted lines"
                    else "+%.1f points out for a longer price".format(state.pushPoints),
                    style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
                )
            }
            Badge(if (state.pushPoints <= 0.0) "OFF" else "ESTIMATED", if (state.pushPoints <= 0.0) Palette.Faint else Palette.Caution)
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = state.pushPoints.toFloat(),
            onValueChange = { vm.setPush(it.toDouble()) },
            valueRange = 0f..4f,
            colors = SliderDefaults.colors(
                thumbColor = Palette.Chalk,
                activeTrackColor = Palette.Over,
                inactiveTrackColor = Palette.Hairline,
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Text("0", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("2", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("4 pts", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Each point pushes the number away from you — an Over needs one more, an Under one fewer — so " +
                "the bet gets less likely and the price gets longer. Every player leg is repriced against " +
                "its own distribution, holding the book's markup where it was quoted, so the ticket's " +
                "chance and price move in opposite directions. A pushed price is an estimate: no book " +
                "quoted it. Game markets keep their quoted line.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Palette.Hairline)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CIRCADIAN CORRECTION", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Text("Up to 1.5% of the projection", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
            }
            Switch(
                checked = state.circadian,
                onCheckedChange = vm::setCircadian,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Palette.Ink,
                    checkedTrackColor = Palette.Over,
                    uncheckedThumbColor = Palette.Dust,
                    uncheckedTrackColor = Palette.Raised,
                ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Tip-off hour on the player's own clock and the zones crossed to get there, folded into each " +
                "player projection. The cap is deliberately small: this is a stated heuristic, not a fitted " +
                "effect, and every leg that is moved says by how much.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
    }
}

@Composable
private fun ParlayCard(
    ticket: ParlayTicketUi,
    strategies: List<ParlayStrategyUi>,
    eligible: Set<String>,
    onPick: (String) -> Unit,
    onOpenSlip: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(shape).background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (ticket.kind == ParlayKind.SAME_GAME) "SAME-GAME PARLAY" else "${ticket.size}-LEG PARLAY",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            if (ticket.kind == ParlayKind.SAME_GAME) {
                Badge("SGP", Palette.Caution, filled = true)
                Spacer(Modifier.width(6.dp))
            }
            Badge("${ticket.games} game${if (ticket.games == 1) "" else "s"}", Palette.Dust)
        }
        Spacer(Modifier.height(10.dp))
        AnimatedPercent(
            value = ticket.probability,
            style = MaterialTheme.typography.displayLarge,
            color = Palette.Chalk,
        )
        Text(
            "chance all ${ticket.size} legs land",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        if (ticket.naiveProbability != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "independent product ${pct(ticket.naiveProbability)} · correlation-adjusted ${pct(ticket.probability)}",
                style = MaterialTheme.typography.labelSmall, color = Palette.Caution,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${"%.2f".format(ticket.decimal)}x",
                style = MaterialTheme.typography.titleLarge, color = Palette.Over,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "1 u returns ${"%.2f".format(ticket.decimal)} u",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
            )
        }
        if (ticket.allocatedStake > 0.0) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Palette.Over.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "STAKE ${"%.2f".format(ticket.allocatedStake)} u",
                    style = MaterialTheme.typography.labelMedium, color = Palette.Over,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "returns ${"%.2f".format(ticket.returns)} u · profit ${"%.2f".format(ticket.profit)} u",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("FORMULA", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            strategies.forEach { strategy ->
                // An empty eligibility set means the board did not report a capacity for
                // this key; in that case the chip stays tappable rather than locking
                // every choice out on a stale state.
                val canFill = eligible.isEmpty() || strategy.id in eligible
                ChoiceChip(
                    label = strategy.label,
                    selected = strategy.id == ticket.strategyId,
                    onClick = { onPick(strategy.id) },
                    enabled = canFill,
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        val disabled = strategies.count { eligible.isNotEmpty() && it.id !in eligible }
        if (disabled > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "$disabled darker ${if (disabled == 1) "formula cannot" else "formulas cannot"} fill " +
                    "${ticket.size} legs from today's board, so ${if (disabled == 1) "it is" else "they are"} " +
                    "not selectable on this card.",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
        val chosen = strategies.firstOrNull { it.id == ticket.strategyId }
        if (chosen != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "This formula qualifies ${chosen.picks.size} props today; the ticket takes its top " +
                    "${ticket.size} and prices them here.",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Palette.Hairline)
        ticket.legs.forEachIndexed { index, leg ->
            ParlayLegRow(index + 1, leg)
            if (index < ticket.legs.lastIndex) HorizontalDivider(color = Palette.Hairline.copy(alpha = 0.5f))
        }
        HorizontalDivider(color = Palette.Hairline)
        Spacer(Modifier.height(10.dp))
        Text(
            ticket.explanation,
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(14.dp))
        ActionButton(
            label = "Make bet slip",
            onClick = onOpenSlip,
            tone = Palette.Over,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ParlayLegRow(number: Int, leg: ParlayLegUi) {
    val pick = leg.candidate
    val lower = pick.label.lowercase()
    val tone = if (lower.contains("under") || lower.contains("less")) Palette.Under else Palette.Over
    val title = pick.subject ?: pick.detail.substringBefore(" · ").ifBlank { pick.label }
    val moved = leg.shiftPts > 0.0 || leg.circadianPct > 0.0
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            number.toString(),
            Modifier.width(20.dp),
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        if (!pick.teamCode.isNullOrBlank()) {
            TeamMark(pick.teamCode, size = 28.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(tone, RoundedCornerShape(50)))
                Spacer(Modifier.width(6.dp))
                Text(
                    pick.label,
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (leg.shiftPts > 0.0) {
                Text(
                    "pushed to ${fmtLine(leg.line ?: 0.0)} · ${if (leg.inferred) "inferred curve" else "from the game log"}",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Caution,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (leg.note.isNotBlank()) {
                Text(
                    leg.note,
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                fmtOdds(leg.odds),
                style = MaterialTheme.typography.labelLarge,
                color = if (moved) Palette.Caution else Palette.Chalk,
            )
            Text(
                pct(leg.winChance),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Every formula the picker offers, with the scope it owns and how much of today it takes. */
@Composable
private fun ScopePanel(strategies: List<ParlayStrategyUi>) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(
            "WHAT EACH FORMULA DOES",
            style = MaterialTheme.typography.labelSmall, color = Palette.Over,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "A formula is a selection rule: an edge bar, a side, a price band and sometimes a tilt or a market " +
                "list. The same board therefore produces different legs from each rung. None of them can " +
                "change a price or a line.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(10.dp))
        strategies.forEach { strategy ->
            Panel(Modifier.padding(bottom = 8.dp), tone = Palette.Raised) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strategy.label,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Badge(
                        when (strategy.tier) {
                            "LIVE" -> "LIVE"
                            "RETIRED" -> "RETIRED"
                            else -> "SANDBOX"
                        },
                        when (strategy.tier) {
                            "LIVE" -> Palette.Gain
                            "RETIRED" -> Palette.Faint
                            else -> Palette.Caution
                        },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    strategy.scope,
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
        }
    }
}

@Composable
private fun EmptyParlay(state: ParlayUi) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Not enough priced picks", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.strategies.isEmpty()) {
                "No formula has reached the board yet, so there is no ladder to build from. The sandbox " +
                    "produces the formulas; a round takes a moment."
            } else {
                "A 3-leg ticket needs at least three picks with a model estimate that the chosen formula " +
                    "will take. The board has ${state.priced}, and the strictest formulas can refuse all of " +
                    "them. That is the honest blocker, not an error: odds without an estimate cannot be " +
                    "multiplied into a probability. Try a broader formula on a ticket."
            },
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
    }
}

@Composable
private fun TenLegLock(onOpenPremium: () -> Unit) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), tone = Palette.Ink) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("10-leg ticket", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
                Text(
                    "Premium builds the long shot. Free keeps the 3- and 5-leg tickets.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
            Badge("PREMIUM", Palette.Caution)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Unlock the 10-leg",
            Modifier.clickable(onClick = onOpenPremium).padding(vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium, color = Palette.Over,
        )
    }
}

@Composable
private fun ParlayFooter(state: ParlayUi) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text(
            "A parlay multiplies both the payout and the book's margin. A pushed price is the app's estimate, " +
                "not a quote, and a same-game chance is an assumption about correlation rather than a " +
                "measurement. Practice only.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        if (state.note.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(state.note, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
        }
    }
}

private fun pct(p: Double): String = when {
    p >= 0.1 -> "%.0f%%".format(p * 100)
    p >= 0.01 -> "%.1f%%".format(p * 100)
    else -> "%.2f%%".format(p * 100)
}
