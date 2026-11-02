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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.propslab.data.PropsRepository
import com.propslab.domain.Markets
import com.propslab.domain.Odds
import com.propslab.domain.Side
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.TeamMark
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.theme.Palette

/** PrizePicks sells an entry as More/Less, so its cards use the book's own words. */
private fun entryWord(book: String, side: Side): String =
    if (book.equals("PrizePicks", ignoreCase = true)) {
        if (side == Side.OVER) "More" else "Less"
    } else {
        side.label
    }

/**
 * The mini entry board: today's priced picks as tappable player cards, each with a
 * More/Less (or Over/Under) choice, an entry amount, and the slip underneath.
 *
 * It mimics the entry setup, not the entry economics. A book's entry pays one
 * all-or-nothing multiplier; this places one paper bet per leg at the same stake, all
 * tagged to the same entry, because per-leg results are what the self-learning loop
 * can actually learn from. The slip says exactly that, so the mimic never overstates
 * itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    picks: List<BestPickUi>,
    available: Double,
    onDismiss: () -> Unit,
    onPlace: (legs: List<PropsRepository.EntryLeg>, stake: Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember(picks) { picks.filter { it.prop.available }.distinctBy { it.prop.id }.take(12) }
    val chosen = remember { mutableStateMapOf<String, Side>() }
    var stake by remember { mutableDoubleStateOf(1.0) }

    LaunchedEffect(available) {
        if (stake > available) stake = if (available >= 0.5) 0.5 else 0.0
    }

    val legs = options.filter { chosen.containsKey(it.prop.id) }
    val total = stake * legs.size
    val canPlace = legs.size >= 2 && stake > 0.0 && total <= available + 1e-9
    val potential = legs.sumOf { leg ->
        val odds = if (chosen[leg.prop.id] == Side.OVER) leg.prop.overOdds else leg.prop.underOdds
        if (odds == null) 0.0 else stake * Odds.americanToDecimal(odds)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Court,
        contentColor = Palette.Chalk,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Build an entry", style = MaterialTheme.typography.headlineSmall, color = Palette.Chalk)
                    Text(
                        "Pick 2 or more players, take More or Less on each, set one amount per leg.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
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

            if (options.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "No priced props are on the board yet. Sync the feed or open Today after the next prices arrive.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(20.dp))
                ActionButton("Close", onDismiss, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 340.dp)) {
                items(options, key = { it.prop.id }) { pick ->
                    EntryCard(
                        pick = pick,
                        selected = chosen[pick.prop.id],
                        onSelect = { side ->
                            if (chosen[pick.prop.id] == side) chosen.remove(pick.prop.id)
                            else if (chosen.size < 6 || chosen.containsKey(pick.prop.id)) chosen[pick.prop.id] = side
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("ENTRY AMOUNT · PER LEG", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(0.5, 1.0, 2.0, 5.0).forEach { value ->
                    ChoiceChip(
                        if (value % 1.0 == 0.0) "%.0f u".format(value) else "%.1f u".format(value),
                        selected = stake == value,
                        onClick = { stake = value },
                        enabled = available + 1e-9 >= value * maxOf(legs.size, 1),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Palette.Hairline)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        legs.isEmpty() -> "Tap More or Less on at least 2 players"
                        legs.size == 1 -> "1 leg · add 1 more to place"
                        else -> "${legs.size} legs · ${"%.1f".format(total)} u staked · returns ${"%.2f".format(potential)} u"
                    },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Each leg is its own paper bet and settles on its own game — that is what lets the system learn from every leg.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
            Spacer(Modifier.height(10.dp))
            ActionButton(
                if (canPlace) "Place ${legs.size}-leg entry" else "Pick at least 2 legs",
                onClick = {
                    onPlace(
                        legs.map { PropsRepository.EntryLeg(it.prop.id, chosen[it.prop.id] ?: Side.OVER) },
                        stake,
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canPlace,
            )
            if (total > available + 1e-9 && legs.size >= 2) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "That entry costs ${"%.1f".format(total)} units and only ${"%.2f".format(available)} are free.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Loss,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EntryCard(pick: BestPickUi, selected: Side?, onSelect: (Side) -> Unit) {
    val prop = pick.prop
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected == null) Palette.Raised else Palette.Court)
            .border(1.dp, if (selected == null) Palette.Hairline else Palette.Chalk.copy(alpha = 0.55f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamMark(prop.teamCode, size = 28.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    prop.playerName,
                    style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${Markets.label(prop.market)} ${fmtLine(prop.line)} · ${prop.matchup}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val over = prop.overOdds
            val under = prop.underOdds
            EntrySideButton(
                "${entryWord(prop.book, Side.OVER)} ${over?.let(::fmtOdds) ?: "—"}",
                chosen = selected == Side.OVER,
                tone = Palette.Over,
                enabled = over != null,
                onClick = { onSelect(Side.OVER) },
                modifier = Modifier.weight(1f),
            )
            EntrySideButton(
                "${entryWord(prop.book, Side.UNDER)} ${under?.let(::fmtOdds) ?: "—"}",
                chosen = selected == Side.UNDER,
                tone = Palette.Under,
                enabled = under != null,
                onClick = { onSelect(Side.UNDER) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EntrySideButton(
    label: String,
    chosen: Boolean,
    tone: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.clip(shape)
            .background(if (chosen) tone.copy(alpha = 0.22f) else Palette.Ink, shape)
            .border(1.dp, if (chosen) tone else Palette.Hairline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (chosen) tone else Palette.Dust,
            maxLines = 1,
        )
    }
}
