package com.propslab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.propslab.data.AutoStep
import com.propslab.data.AutoStepState
import com.propslab.data.AutopilotUi
import com.propslab.ui.fmtAgo
import com.propslab.ui.theme.Palette

/**
 * The one card a novice needs: what is working, what is waiting, and one button that runs
 * the whole loop in the right order.
 *
 * The four steps are the app's own components, not features to configure. Opening a step
 * is deliberately not offered here; the card reports, and the one action is the action.
 */
@Composable
fun AutopilotCard(
    ui: AutopilotUi,
    running: Boolean,
    message: String?,
    onRunAll: () -> Unit,
    onOpenFeed: (() -> Unit)? = null,
) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AUTOPILOT", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Badge(
                when {
                    running -> "RUNNING NOW"
                    ui.needsSetup -> "ONE KEY"
                    ui.live -> "LIVE"
                    else -> "SEARCHING"
                },
                when {
                    running -> Palette.Over
                    ui.needsSetup -> Palette.Caution
                    ui.live -> Palette.Gain
                    else -> Palette.Faint
                },
                filled = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(ui.headline, style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(ui.detail, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        if (ui.lastRunUtc > 0L) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Last automatic update ${fmtAgo(ui.lastRunUtc)}.",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(14.dp))
        ui.steps.forEach { step ->
            AutoStepRow(step)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                if (running) "Running everything…" else "Run everything now",
                onRunAll,
                enabled = !running,
                tone = Palette.Over,
            )
            if (ui.needsSetup && onOpenFeed != null) {
                PlainLink(onOpenFeed, label = "Open Live data")
            }
        }
        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = Palette.Chalk)
        }
    }
}

@Composable
private fun AutoStepRow(step: AutoStep) {
    val tone = when (step.state) {
        AutoStepState.DONE -> Palette.Gain
        AutoStepState.WORKING -> Palette.Over
        AutoStepState.WAITING -> Palette.Faint
        AutoStepState.NEEDS_YOU -> Palette.Caution
    }
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(6.dp).background(tone, CircleShape))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(step.title, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
            Text(step.detail, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
    }
}
