package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.propslab.data.PropUi
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
 * One tap from a board row into the practice account.
 *
 * Today and Best both open this sheet with the prop already chosen, so placing a paper
 * bet never means leaving the screen that convinced you. It writes exactly the same
 * paper bet the Bets tab writes, tagged as the user's own pick: side, stake, and the
 * bankroll check are identical, only the walk to reach them is shorter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropBetSheet(
    prop: PropUi,
    suggestedSide: Side,
    available: Double,
    onDismiss: () -> Unit,
    onPlace: (side: Side, stake: Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var side by remember(prop.id) { mutableStateOf(suggestedSide) }
    var stake by remember(prop.id) { mutableDoubleStateOf(1.0) }

    LaunchedEffect(available) {
        if (stake > available) stake = if (available >= 0.5) 0.5 else 0.0
    }

    val odds = if (side == Side.OVER) prop.overOdds else prop.underOdds
    val canPlace = odds != null && stake > 0.0 && stake <= available + 1e-9

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
                TeamMark(prop.teamCode, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        prop.playerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Palette.Chalk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${Markets.label(prop.market)} ${fmtLine(prop.line)} · ${prop.matchup}",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Badge("FAKE MONEY", Palette.Caution)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Available ${"%.2f".format(available)} units",
                style = MaterialTheme.typography.titleMedium,
                color = if (available > 0.0) Palette.Chalk else Palette.Loss,
            )

            Spacer(Modifier.height(18.dp))
            Text("1 · SIDE AND PRICE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(
                    "Over ${prop.overOdds?.let(::fmtOdds) ?: "—"}",
                    selected = side == Side.OVER,
                    onClick = { side = Side.OVER },
                    enabled = prop.overOdds != null,
                )
                ChoiceChip(
                    "Under ${prop.underOdds?.let(::fmtOdds) ?: "—"}",
                    selected = side == Side.UNDER,
                    onClick = { side = Side.UNDER },
                    enabled = prop.underOdds != null,
                )
            }

            Spacer(Modifier.height(14.dp))
            Text("2 · STAKE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
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

            Spacer(Modifier.height(18.dp))
            Text(
                "${prop.playerName} ${side.label} ${fmtLine(prop.line)} ${Markets.label(prop.market)}",
                style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
            )
            Spacer(Modifier.height(10.dp))
            ActionButton(
                if (canPlace) "Place ${"%.1f".format(stake)} unit practice bet" else "Choose a priced side",
                onClick = {
                    onPlace(side, stake)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canPlace,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Tagged as your own pick. Paper analysis only; 18+.",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * The small shared affordance that opens the quick bet sheet from any board row.
 * It says what it does and nothing more, so Today and Best read the same.
 */
@Composable
fun BetRowButton(onClick: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    androidx.compose.foundation.layout.Box(
        Modifier.clip(shape)
            .background(Palette.Chalk.copy(alpha = 0.10f), shape)
            .border(1.dp, Palette.Chalk.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Place bet",
            style = MaterialTheme.typography.labelMedium, color = Palette.Chalk,
        )
    }
}
