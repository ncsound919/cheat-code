package com.propslab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.data.AuditEventEntity
import com.propslab.data.PropsRepository
import com.propslab.ui.components.ActionButton
import com.propslab.ui.fmtAgo
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AuditViewModel(repo: PropsRepository) : ViewModel() {
    val events: StateFlow<List<AuditEventEntity>> =
        repo.audit().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private fun kindTitle(kind: String) = when (kind) {
    "fixtures_loaded" -> "Fixtures loaded"
    "model_registered" -> "Model registered"
    "prediction_created" -> "Prediction recorded"
    "prediction_rejected" -> "Prediction rejected"
    "prediction_expired" -> "Prediction expired"
    "paper_bet_placed" -> "Paper bet placed"
    "result_recorded" -> "Result recorded"
    else -> kind.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun kindColor(kind: String): Color = when (kind) {
    "prediction_rejected", "prediction_expired" -> Palette.Caution
    "paper_bet_placed", "result_recorded" -> Palette.Chalk
    else -> Palette.Dust
}

@Composable
fun AuditScreen(onBack: (() -> Unit)? = null) {
    val repo = rememberRepo()
    val vm: AuditViewModel = viewModel(factory = viewModelFactory { initializer { AuditViewModel(repo) } })
    val events by vm.events.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, bottom = 120.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Audit", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Every import, prediction, bet and result. The app can add entries but cannot edit or delete them.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                if (onBack != null) {
                    Spacer(Modifier.height(14.dp))
                    ActionButton("Back to the sandbox", onBack, tone = Palette.Dust)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        itemsIndexed(events, key = { _, e -> e.id }) { index, e ->
            TimelineRow(e, first = index == 0, last = index == events.lastIndex)
        }
    }
}

@Composable
private fun TimelineRow(e: AuditEventEntity, first: Boolean, last: Boolean) {
    val color = kindColor(e.kind)
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(IntrinsicSize.Min)) {
        Canvas(Modifier.width(24.dp).fillMaxHeight()) {
            val x = 6.dp.toPx()
            val dotY = 22.dp.toPx()
            val line = 1.5.dp.toPx()
            if (!first) drawLine(Palette.Hairline, Offset(x, 0f), Offset(x, dotY), strokeWidth = line)
            if (!last) drawLine(Palette.Hairline, Offset(x, dotY), Offset(x, size.height), strokeWidth = line)
            drawCircle(color, 5.dp.toPx(), Offset(x, dotY))
        }
        Column(Modifier.padding(start = 6.dp, top = 12.dp, bottom = 14.dp).weight(1f)) {
            Row {
                Text(kindTitle(e.kind), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
                Text(fmtAgo(e.ts), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            }
            Text(e.detail, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
    }
}
