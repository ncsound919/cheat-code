package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.propslab.data.AvailabilitySignalEntity
import com.propslab.data.AvailabilityStatus
import com.propslab.data.SignalSource
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.fmtAgo
import com.propslab.ui.theme.Palette

/**
 * Late availability: what is live, and the way in.
 *
 * The manual form lives here rather than behind the console because a person watching a
 * shootaround is the fastest and most reliable source this app has, and entering a report
 * should never be harder than reading a headline. A news signal appears in the same list,
 * marked as such, so there is never any doubt about which one moved the number.
 */
@Composable
internal fun SignalsCard(
    state: FeedScreenState,
    signals: List<AvailabilitySignalEntity>,
    vm: FeedViewModel,
) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LATE SIGNALS", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text(
                if (signals.isEmpty()) "none live" else "${signals.size} recorded",
                style = MaterialTheme.typography.labelSmall,
                color = if (signals.isEmpty()) Palette.Faint else Palette.Gain,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "A report shifts the projection, never the price. Out withholds the estimate entirely; " +
                "anything else cuts the expected stat by a named amount and flags the row.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )

        if (signals.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            signals.take(8).forEach { signal -> SignalRow(signal) }
            Spacer(Modifier.height(6.dp))
            ActionButton("Expire all signals", { vm.expireSignals() }, tone = Palette.Dust)
        }

        Spacer(Modifier.height(16.dp))
        Text("RECORD ONE BY HAND", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.signalPlayer,
            onValueChange = vm::setSignalPlayer,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Player", style = MaterialTheme.typography.bodySmall, color = Palette.Faint) },
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Hairline,
                unfocusedBorderColor = Palette.Hairline,
                focusedTextColor = Palette.Chalk,
                unfocusedTextColor = Palette.Chalk,
                cursorColor = Palette.Over,
            ),
        )
        Spacer(Modifier.height(8.dp))
        AvailabilityStatus.ALL.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                row.forEachIndexed { index, status ->
                    if (index > 0) Spacer(Modifier.width(6.dp))
                    Box(Modifier.weight(1f)) {
                        ChoiceChip(
                            AvailabilityStatus.label(status),
                            selected = state.signalStatus == status,
                            onClick = { vm.setSignalStatus(status) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = state.signalDetail,
            onValueChange = vm::setSignalDetail,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("What was reported (optional)", style = MaterialTheme.typography.bodySmall, color = Palette.Faint) },
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Hairline,
                unfocusedBorderColor = Palette.Hairline,
                focusedTextColor = Palette.Chalk,
                unfocusedTextColor = Palette.Chalk,
                cursorColor = Palette.Over,
            ),
        )
        Spacer(Modifier.height(12.dp))
        ActionButton("Record signal", { vm.recordSignal() }, tone = Palette.Caution)
    }
}

@Composable
private fun SignalRow(signal: AvailabilitySignalEntity) {
    val tone = when (signal.status) {
        AvailabilityStatus.OUT -> Palette.Loss
        AvailabilityStatus.DOUBTFUL -> Palette.Caution
        AvailabilityStatus.QUESTIONABLE -> Palette.Caution
        AvailabilityStatus.PROBABLE -> Palette.Gain
        else -> Palette.Over
    }
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(tone, androidx.compose.foundation.shape.CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                signal.playerName,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = Palette.Chalk,
                maxLines = 1,
            )
            Badge(AvailabilityStatus.label(signal.status), tone)
        }
        Row(Modifier.padding(start = 14.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (signal.detail.isNotBlank()) signal.detail else "no detail") +
                    " · " + SignalSource.label(signal.source) +
                    " · " + fmtAgo(signal.observedUtc),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Dust,
                maxLines = 2,
            )
        }
    }
}
