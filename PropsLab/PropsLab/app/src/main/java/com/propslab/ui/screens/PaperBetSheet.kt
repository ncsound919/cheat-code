package com.propslab.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.propslab.data.BestPickUi
import com.propslab.data.FormulaUi
import com.propslab.domain.Markets
import com.propslab.domain.Side
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.TeamMark
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.theme.Palette

/**
 * One compact entry point for the user's practice account.
 *
 * The sheet only ever writes a paper bet. It shows the available practice bankroll before
 * the stake, tags the pick to a formula when the user asks for that, and refuses a stake
 * larger than the account can cover. No book is contacted and no real money is involved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperBetSheet(
    picks: List<BestPickUi>,
    formulas: List<FormulaUi>,
    available: Double,
    onDismiss: () -> Unit,
    onPlace: (predictionId: String, side: Side, stake: Double, formulaId: String, formulaLabel: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember(picks) { picks.distinctBy { it.prop.id }.take(8) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = options.firstOrNull { it.prop.id == selectedId } ?: options.firstOrNull()
    var side by remember { mutableStateOf(selected?.side ?: Side.OVER) }
    var stake by remember { mutableDoubleStateOf(1.0) }
    var formulaId by remember { mutableStateOf("") }

    LaunchedEffect(selected?.prop?.id) {
        side = selected?.side ?: Side.OVER
    }
    LaunchedEffect(available) {
        if (stake > available) stake = if (available >= 0.5) 0.5 else 0.0
    }

    val odds = selected?.prop?.let { if (side == Side.OVER) it.overOdds else it.underOdds }
    val formula = formulas.firstOrNull { it.id == formulaId }
    val canPlace = selected != null && odds != null && stake > 0.0 && stake <= available + 1e-9

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Court,
        contentColor = Palette.Chalk,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Place a paper bet", style = MaterialTheme.typography.headlineSmall, color = Palette.Chalk)
                    Text(
                        "Uses the practice bankroll only. No book is contacted and no real money moves.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                }
                Badge("FAKE MONEY", Palette.Caution)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Available ${"%.2f".format(available)} units",
                style = MaterialTheme.typography.titleMedium,
                color = if (available > 0.0) Palette.Chalk else Palette.Loss,
            )

            if (options.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "No priced props are on the board yet. Sync the feed or open Today after the next prices arrive.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(20.dp))
                ActionButton("Close", onDismiss, modifier = Modifier.fillMaxWidth())
                return@Column
            }

            Spacer(Modifier.height(18.dp))
            Text("1 · CHOOSE A PICK", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(8.dp))
            options.forEach { pick ->
                PickOption(
                    pick = pick,
                    selected = pick.prop.id == selected?.prop?.id,
                    onClick = { selectedId = pick.prop.id },
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(14.dp))
            Text("2 · SIDE AND PRICE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val over = selected?.prop?.overOdds
                val under = selected?.prop?.underOdds
                ChoiceChip(
                    "Over ${over?.let(::fmtOdds) ?: "—"}",
                    selected = side == Side.OVER,
                    onClick = { side = Side.OVER },
                    enabled = over != null,
                )
                ChoiceChip(
                    "Under ${under?.let(::fmtOdds) ?: "—"}",
                    selected = side == Side.UNDER,
                    onClick = { side = Side.UNDER },
                    enabled = under != null,
                )
            }

            Spacer(Modifier.height(14.dp))
            Text("3 · STAKE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(0.5, 1.0, 2.0, 5.0, 10.0).forEach { value ->
                    ChoiceChip(
                        if (value % 1.0 == 0.0) "%.0f u".format(value) else "%.1f u".format(value),
                        selected = stake == value,
                        onClick = { stake = value },
                        enabled = available + 1e-9 >= value,
                    )
                }
            }
            if (stake <= 0.0 || available + 1e-9 < stake) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "This account cannot cover that stake. Record an open result or reset the practice account.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Loss,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "A stake is reserved while the bet is open. Win or lose, the result is added back when you record it.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )

            Spacer(Modifier.height(14.dp))
            Text("4 · TRACK UNDER", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("My pick", selected = formulaId.isEmpty(), onClick = { formulaId = "" })
                formulas.take(6).forEach { f ->
                    ChoiceChip(
                        f.label,
                        selected = formulaId == f.id,
                        onClick = { formulaId = f.id },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tagging only groups this bet in your history and the formula chart. It does not change the price.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )

            Spacer(Modifier.height(18.dp))
            val summary = selected?.prop?.let {
                "${it.playerName} ${side.label} ${fmtLine(it.line)} ${Markets.label(it.market)}"
            } ?: "Choose a pick"
            Text(summary, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
            Spacer(Modifier.height(10.dp))
            ActionButton(
                if (canPlace) "Place ${if (stake % 1.0 == 0.0) "%.0f".format(stake) else "%.1f".format(stake)} unit practice bet" else "Choose a priced side",
                onClick = {
                    val pick = selected ?: return@ActionButton
                    onPlace(pick.prop.id, side, stake, formulaId, formula?.label.orEmpty())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canPlace,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Formula: ${formula?.label ?: "My pick"}. Paper analysis only; 18+.",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PickOption(pick: BestPickUi, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val prop = pick.prop
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .border(1.dp, if (selected) Palette.Chalk else Palette.Hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeamMark(prop.teamCode, size = 28.dp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(prop.playerName, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${pick.side.label} ${fmtLine(prop.line)} ${Markets.label(prop.market)} · ${prop.matchup}",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(fmtOdds(pick.odds), style = MaterialTheme.typography.labelLarge, color = Palette.Chalk)
            Text(
                "${"%.1f".format(pick.edgePts)} pt gap",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
    }
}
