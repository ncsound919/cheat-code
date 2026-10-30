package com.propslab.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.propslab.data.ParlayTicketUi
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
import com.propslab.ui.fmtOdds
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The slip as the user takes it out of the app.
 *
 * The legs are the ticket's own, so the player selection is already done. The only input
 * is the practice stake and which book the slip is addressed to. The two tracker buttons
 * match what those apps document: Gambly receives an image through the share sheet, and
 * Pikkit's QuickPickBot receives pasted text on the web. The book button opens the book;
 * it cannot fill the book's slip, and the sheet says so instead of implying otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetSlipSheet(ticket: ParlayTicketUi, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bookName by rememberSaveable { mutableStateOf(SlipBook.DRAFTKINGS.name) }
    var stakeText by rememberSaveable(ticket.key) {
        mutableStateOf(if (ticket.allocatedStake > 0.0) "%.2f".format(ticket.allocatedStake) else "1.00")
    }
    val book = SlipBook.entries.firstOrNull { it.name == bookName } ?: SlipBook.DRAFTKINGS
    val stake = stakeText.toDoubleOrNull()?.coerceIn(0.0, 100_000.0) ?: 0.0
    val slip = remember(ticket, book, stake) { BetSlipBuilder.from(ticket, book, stake) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

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
            Text("Bet slip", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
            Spacer(Modifier.height(6.dp))
            Text(
                "${slip.legs.size} legs · ${"%.2f".format(slip.decimal)}x · " +
                    "model ${pct(slip.probability)} · ${book.displayName}",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "The board's top-ranked players are already on the slip, with each club's mark. " +
                    "Nothing needs importing or setting up.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )

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
                    SlipMetric("MODEL CHANCE", pct(slip.probability), Modifier.weight(1f), Palette.Chalk)
                    SlipMetric("RETURNS", "${"%.2f".format(slip.potentialReturn)} u", Modifier.weight(1f), Palette.Over)
                    SlipMetric("PROFIT", "${"%.2f".format(slip.profit)} u", Modifier.weight(1f), Palette.Gain)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Palette.Hairline)
            slip.legs.forEachIndexed { index, leg ->
                SlipLegRow(index + 1, leg)
                if (index < slip.legs.lastIndex) {
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
                "No sportsbook lets an outside app pre-fill a slip. Gambly imports the shared " +
                    "image; Pikkit's QuickPickBot reads the copied text. Check every leg and price " +
                    "in the book before you confirm. Practice analysis, not a placed bet. 18+.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun SlipLegRow(number: Int, leg: SlipLeg) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            number.toString(),
            Modifier.width(18.dp),
            style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
        )
        TeamMark(leg.teamCode, size = 30.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                leg.player,
                style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                leg.pick.removePrefix(leg.player).trim().ifBlank { leg.pick },
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${leg.teamCode} · ${pct(leg.modelChance)} model",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                fmtOdds(leg.odds),
                style = MaterialTheme.typography.labelLarge,
                color = if (leg.side == Side.OVER) Palette.Over else Palette.Under,
            )
            Text(leg.side.label, style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        }
    }
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
