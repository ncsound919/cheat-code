package com.propslab.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.propslab.data.ParlayCandidate
import com.propslab.data.ParlayKind
import com.propslab.data.ParlayLegUi
import com.propslab.data.ParlayTicketUi
import com.propslab.data.ParlayTickets
import com.propslab.domain.ParlayBook
import com.propslab.domain.ParlayLeg
import com.propslab.domain.SameGame
import com.propslab.domain.Side
import com.propslab.slip.BetSlip
import com.propslab.slip.BetSlipBuilder
import com.propslab.slip.BetSlipImage
import com.propslab.slip.BetSlipShare
import com.propslab.slip.SlipBook
import com.propslab.slip.SlipLeg
import com.propslab.slip.SlipTracker
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.components.TeamMark
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The slip as the user takes it out of the app.
 *
 * The legs are the ticket's own, so the player selection is already done. From here a leg
 * can be swapped for another priced candidate on the board, and each player leg can move
 * its line up to four points against the user. The chance and price are recomputed from the
 * same distribution the parlay screen uses, so the number on the slip is the number sent.
 *
 * The two tracker buttons match what those apps document: Gambly receives an image through
 * the share sheet, and Pikkit's QuickPickBot receives pasted text on the web. The book
 * button opens the book; it cannot fill the book's slip, and the sheet says so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetSlipSheet(
    ticket: ParlayTicketUi,
    candidates: List<ParlayCandidate> = emptyList(),
    circadian: Boolean = true,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bookName by rememberSaveable { mutableStateOf(SlipBook.DRAFTKINGS.name) }
    var stakeText by rememberSaveable(ticket.key) {
        mutableStateOf(if (ticket.allocatedStake > 0.0) "%.2f".format(ticket.allocatedStake) else "1.00")
    }
    var legs by remember(ticket) { mutableStateOf(ticket.legs) }
    var swapIndex by remember { mutableStateOf<Int?>(null) }
    val book = SlipBook.entries.firstOrNull { it.name == bookName } ?: SlipBook.DRAFTKINGS
    val stake = stakeText.toDoubleOrNull()?.coerceIn(0.0, 100_000.0) ?: 0.0
    val metrics = remember(legs, ticket.kind) { slipMetrics(legs, ticket.kind) }
    val label = BetSlipBuilder.labelFor(ticket)
    val slip = remember(label, legs, metrics, book, stake) {
        BetSlipBuilder.from(
            label = label,
            legs = legs,
            decimal = metrics.decimal,
            probability = metrics.probability,
            book = book,
            stake = stake,
        )
    }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val edited = legs != ticket.legs

    fun prepare(action: suspend (Bitmap, BetSlip) -> String?) {
        if (busy) return
        busy = true
        note = null
        scope.launch {
            val result = runCatching {
                val logos = BetSlipImage.loadLogos(slip.legs)
                val bitmap = withContext(Dispatchers.Default) { BetSlipImage.render(context, slip, logos) }
                action(bitmap, slip)
            }.getOrElse { "The slip could not be prepared." }
            note = result
            busy = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Court,
        contentColor = Palette.Chalk,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 40.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Bet slip", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${legs.size} legs · ${"%.2f".format(metrics.decimal)}x · " +
                            "model ${pct(metrics.probability)} · ${book.displayName}",
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                    )
                }
                if (edited) {
                    Text(
                        "Reset",
                        Modifier.clickable {
                            legs = ticket.legs
                            swapIndex = null
                        }.padding(start = 12.dp, top = 6.dp),
                        style = MaterialTheme.typography.labelMedium, color = Palette.Over,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "The board's top-ranked players are already on the slip, with each club's mark. " +
                    "Swap a leg or move its line either way; the chance and price follow.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
            val sharedGames = legs.size - legs.map { it.candidate.gameId }.distinct().size
            if (sharedGames > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "$sharedGames ${if (sharedGames == 1) "leg shares" else "legs share"} a game, " +
                        "so their chances move together; the model chance is an estimate.",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Caution,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("BOOK", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                SlipBook.entries.forEach { entry ->
                    ChoiceChip(
                        label = entry.displayName,
                        selected = entry == book,
                        onClick = { bookName = entry.name },
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("PRACTICE STAKE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                        Text(
                            "No money moves. This only prices the slip.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                    }
                    StakeField(stakeText) { value ->
                        stakeText = value.filter { it.isDigit() || it == '.' }.take(9)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Palette.Hairline)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    SlipMetric("MODEL CHANCE", pct(metrics.probability), Modifier.weight(1f), Palette.Chalk)
                    SlipMetric("RETURNS", "${"%.2f".format(slip.potentialReturn)} u", Modifier.weight(1f), Palette.Over)
                    SlipMetric("PROFIT", "${"%.2f".format(slip.profit)} u", Modifier.weight(1f), Palette.Gain)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Palette.Hairline)
            legs.forEachIndexed { index, leg ->
                EditableLegRow(
                    number = index + 1,
                    leg = leg,
                    canSwap = candidates.isNotEmpty(),
                    onSwap = { swapIndex = if (swapIndex == index) null else index },
                    onShift = { points ->
                        legs = legs.toMutableList().also {
                            it[index] = ParlayTickets.reprice(leg.candidate, points, circadian)
                        }
                    },
                )
                if (swapIndex == index) {
                    SwapPicker(
                        index = index + 1,
                        current = leg,
                        legs = legs,
                        candidates = candidates,
                        sameGame = ticket.kind == ParlayKind.SAME_GAME,
                        circadian = circadian,
                        onPick = { replacement ->
                            legs = legs.toMutableList().also { it[index] = replacement }
                            swapIndex = null
                        },
                        onClose = { swapIndex = null },
                    )
                }
                if (index < legs.lastIndex) {
                    HorizontalDivider(color = Palette.Hairline.copy(alpha = 0.5f))
                }
            }
            HorizontalDivider(color = Palette.Hairline)

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    label = if (busy) "Preparing…" else "Share slip",
                    enabled = !busy,
                    tone = Palette.Over,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        prepare { bitmap, ready ->
                            if (BetSlipShare.share(context, ready, bitmap)) null
                            else "No app accepted the slip."
                        }
                    },
                )
                ActionButton(
                    label = "Send to Gambly",
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        prepare { bitmap, ready ->
                            if (BetSlipShare.sendToTracker(context, ready, bitmap, SlipTracker.GAMBLY)) null
                            else "Gambly could not be opened."
                        }
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    label = "Copy for QuickPickBot",
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        note = if (BetSlipShare.copy(context, slip)) {
                            "Copied. Paste it into QuickPickBot."
                        } else {
                            "The clipboard was unavailable."
                        }
                    },
                )
                ActionButton(
                    label = "Open QuickPickBot",
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!BetSlipShare.openUrl(context, BetSlipShare.QUICK_PICK_URL)) {
                            note = "No browser accepted the link."
                        }
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            ActionButton(
                label = "Open ${book.displayName}",
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!BetSlipShare.openBook(context, book)) note = "No browser accepted the link."
                },
            )

            if (note != null) {
                Spacer(Modifier.height(10.dp))
                Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Palette.Caution)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "A moved line is the app's repricing from the player's own distribution, not a " +
                    "price a book quoted. No sportsbook lets an outside app pre-fill a slip. Gambly " +
                    "imports the shared image; Pikkit's QuickPickBot reads the copied text. Check " +
                    "every leg and price in the book before you confirm. Practice analysis, not a " +
                    "placed bet. 18+.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun EditableLegRow(
    number: Int,
    leg: ParlayLegUi,
    canSwap: Boolean,
    onSwap: () -> Unit,
    onShift: (Double) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                number.toString(),
                Modifier.width(18.dp),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            TeamMark(leg.candidate.teamCode ?: leg.candidate.venueCode.orEmpty(), size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                val player = leg.candidate.subject
                    ?: leg.candidate.detail.substringBefore(" · ").ifBlank { leg.candidate.label }
                val pick = BetSlipBuilder.pickLabel(leg)
                Text(
                    player,
                    style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pick.removePrefix(player).trim().ifBlank { pick },
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${leg.candidate.teamCode ?: leg.candidate.venueCode.orEmpty()} · ${pct(leg.winChance)} model",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fmtOdds(leg.odds),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (leg.candidate.side == Side.OVER) Palette.Over else Palette.Under,
                )
                Text(leg.candidate.side.label, style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                if (canSwap) {
                    Text(
                        "SWAP",
                        Modifier.clickable(onClick = onSwap).padding(top = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Palette.Over,
                    )
                }
            }
        }

        if (leg.candidate.pushable) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(start = 58.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "LINE ${fmtLine(leg.line)}",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${pct(leg.winChance)} · ${fmtOdds(leg.odds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (leg.shiftPts > 0.0) Palette.Caution else Palette.Dust,
                )
            }
            Slider(
                value = leg.shiftPts.toFloat().coerceIn(-4f, 4f),
                onValueChange = { onShift(it.toDouble()) },
                valueRange = -4f..4f,
                steps = 15,
                modifier = Modifier.fillMaxWidth().padding(start = 46.dp).height(34.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Palette.Chalk,
                    activeTrackColor = Palette.Over,
                    inactiveTrackColor = Palette.Hairline,
                ),
            )
            Row(Modifier.fillMaxWidth().padding(start = 58.dp)) {
                Text("−4 pts", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.weight(1f))
                Text("quoted", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.weight(1f))
                Text("+4 pts", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            }
            if (leg.note.isNotBlank()) {
                Text(
                    leg.note,
                    Modifier.padding(start = 58.dp, top = 4.dp),
                    style = MaterialTheme.typography.labelSmall, color = Palette.Caution,
                )
            }
        } else {
            Text(
                "Quoted line — this market has no player game log to reprice.",
                Modifier.padding(start = 58.dp, top = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
    }
}

/**
 * The board's other priced legs, filtered to what the ticket can actually become.
 *
 * A same-game ticket may only take another leg from its own game, and never a second market
 * on a player already on it. A normal ticket can take any unused leg; different games are
 * listed first because they keep the product closer to independent.
 */
@Composable
private fun SwapPicker(
    index: Int,
    current: ParlayLegUi,
    legs: List<ParlayLegUi>,
    candidates: List<ParlayCandidate>,
    sameGame: Boolean,
    circadian: Boolean,
    onPick: (ParlayLegUi) -> Unit,
    onClose: () -> Unit,
) {
    val used = legs.map { it.candidate.key }.toSet()
    val subjects = legs.mapNotNull { it.candidate.subject }.toSet()
    val games = legs.filterIndexed { i, _ -> i != index - 1 }.map { it.candidate.gameId }.toSet()
    val options = remember(candidates, used, subjects, games, current, sameGame, circadian) {
        candidates.asSequence()
            .filter { it.key !in used }
            .filter { !sameGame || it.gameId == current.candidate.gameId }
            // Two markets on one player move together far more than two players in one
            // game, so a swap never introduces that correlation silently.
            .filter { it.subject == null || it.subject !in subjects }
            .filter { it.pWin > 0.0 && it.pWin + it.pPush <= 1.0 }
            .map { ParlayTickets.reprice(it, 0.0, circadian) }
            .sortedWith(
                compareByDescending<ParlayLegUi> { it.candidate.gameId !in games }
                    .thenByDescending { it.candidate.edgePts },
            )
            .take(10)
            .toList()
    }
    Panel(Modifier.padding(start = 30.dp, top = 4.dp, bottom = 8.dp), tone = Palette.Raised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SWAP LEG $index",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall, color = Palette.Over,
            )
            Text(
                "Close",
                Modifier.clickable(onClick = onClose).padding(start = 8.dp),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (sameGame) "Same game only · another player from this game"
            else "Any unused leg · different games first",
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        if (options.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "No other priced leg fits this slot on today's board.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
            return@Panel
        }
        options.forEach { option ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(option) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamMark(option.candidate.teamCode ?: option.candidate.venueCode.orEmpty(), size = 26.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        option.candidate.subject
                            ?: option.candidate.detail.substringBefore(" · ").ifBlank { option.candidate.label },
                        style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        option.candidate.label,
                        style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmtOdds(option.odds), style = MaterialTheme.typography.labelMedium, color = Palette.Chalk)
                    Text(pct(option.winChance), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                }
            }
        }
    }
}

private data class SlipMetrics(val probability: Double, val decimal: Double)

/** The same product the parlay card uses, with the same-game correction applied again. */
private fun slipMetrics(legs: List<ParlayLegUi>, kind: ParlayKind): SlipMetrics {
    val built = ParlayBook.build(legs.map { ParlayLeg(odds = it.odds, pWin = it.winChance) })
        ?: return SlipMetrics(0.0, 1.0)
    if (kind == ParlayKind.SAME_GAME) {
        val subjects = legs.mapNotNull { it.candidate.subject }
        val adjusted = SameGame.adjust(
            built.probability,
            built.decimal,
            together = subjects.size != subjects.distinct().size,
        )
        if (adjusted != null) return SlipMetrics(adjusted.probability, adjusted.decimal)
    }
    return SlipMetrics(built.probability, built.decimal)
}

@Composable
private fun SlipMetric(label: String, value: String, modifier: Modifier, tone: androidx.compose.ui.graphics.Color) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.Faint, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = tone, maxLines = 1)
    }
}

/** The same quiet bordered field the parlay budget uses, sized for a stake. */
@Composable
private fun StakeField(value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.width(112.dp).clip(shape).background(Palette.Raised)
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
                    Text("1.00", style = MaterialTheme.typography.titleMedium, color = Palette.Faint)
                }
                inner()
            },
        )
        Text("u", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
    }
}

private fun pct(value: Double): String = when {
    value >= 0.1 -> "%.0f%%".format(value * 100)
    value >= 0.01 -> "%.1f%%".format(value * 100)
    else -> "%.2f%%".format(value * 100)
}
