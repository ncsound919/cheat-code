package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.PropsLabApp
import com.propslab.agent.ConsoleEngine
import com.propslab.agent.ConsoleTurnUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Panel
import com.propslab.ui.fmtTime
import com.propslab.ui.theme.Palette

class ConsoleViewModel(private val engine: ConsoleEngine) : ViewModel() {
    private val _turns = MutableStateFlow<List<ConsoleTurnUi>>(emptyList())
    val turns: StateFlow<List<ConsoleTurnUi>> = _turns

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    val modelLabel: String get() = engine.modelLabel

    init {
        viewModelScope.launch { _turns.value = engine.history() }
    }

    fun preview(line: String): String = engine.preview(line)

    fun send(line: String) {
        val text = line.trim()
        if (text.isEmpty() || _busy.value) return
        _busy.value = true
        viewModelScope.launch {
            engine.submit(text)
            _turns.value = engine.history()
            _busy.value = false
        }
    }

    fun clear() {
        viewModelScope.launch {
            engine.clear()
            _turns.value = engine.history()
        }
    }
}

/**
 * The console: one line in, either an operation or a conversation out.
 *
 * The hint under the input names the tool a line would call before the user presses send, so
 * routing is never a surprise. When the line is not an operation, the on-device model answers
 * and the turn is marked `said` rather than `ran` — the difference between the system doing
 * something and the system talking about it is kept visible on purpose.
 */
@Composable
fun ConsoleScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as PropsLabApp
    val engine = app.console
    val vm: ConsoleViewModel = viewModel(factory = viewModelFactory { initializer { ConsoleViewModel(engine) } })
    val turns by vm.turns.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val list = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    val hint = remember(draft) { vm.preview(draft) }
    val modelLabel = remember { vm.modelLabel }

    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) list.animateScrollToItem(turns.size - 1)
    }

    // The input row sits above the keyboard. Without this the row is drawn behind the IME
    // and the send button cannot be pressed at all — the transcript stops dead at the bottom.
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = 16.dp,
            ),
        ) {
            item { ConsoleHeader(modelLabel, onBack, vm::clear) }
            item { ConsoleIntro() }
            if (turns.isEmpty()) {
                item { ConsoleEmpty() }
            }
            items(turns, key = { it.id }) { turn -> TurnRow(turn) }
            if (busy) {
                item {
                    Text(
                        "working…",
                        Modifier.padding(start = 20.dp, top = 10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Faint,
                    )
                }
            }
        }
        QuickChips(onPick = { vm.send(it) })
        InputRow(
            draft = draft,
            hint = hint,
            busy = busy,
            onDraft = { draft = it },
            onSend = {
                vm.send(draft)
                draft = ""
            },
        )
    }
}

@Composable
private fun ConsoleHeader(modelLabel: String, onBack: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←",
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Dust,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(6.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Console", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk, modifier = Modifier.weight(1f))
            Text(
                "clear",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Faint,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClear).padding(8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(modelLabel, Palette.Faint)
        }
    }
}

@Composable
private fun ConsoleIntro() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Panel {
            Text("Ask, or operate", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
            Spacer(Modifier.height(6.dp))
            Text(
                "Type an instruction it recognises and it actually does the work — that turn is marked " +
                    "`ran`. Anything else is answered by the model on your phone and marked `said`. " +
                    "Asking about strategy returns your saved notes exactly as written, never rewritten.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nothing here can reach a book, move money or place a real bet. `promote` switches which " +
                    "strategy runs the practice book, and the bets it takes are recorded as practice bets.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun ConsoleEmpty() {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Type `?` for everything I can do.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Faint,
        )
    }
}

@Composable
private fun TurnRow(turn: ConsoleTurnUi) {
    if (turn.fromUser) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                turn.text,
                Modifier.clip(RoundedCornerShape(14.dp)).background(Palette.Raised)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Chalk,
            )
        }
        return
    }
    val tone = when (turn.kind) {
        "CALLED" -> Palette.Gain
        "CONSULTED" -> Palette.Over
        "REFUSED" -> Palette.Loss
        else -> Palette.Dust
    }
    val label = when (turn.kind) {
        "CALLED" -> "ran"
        "CONSULTED" -> "notes"
        "REFUSED" -> "refused"
        else -> "said"
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(tone, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tone)
            turn.toolName?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Faint,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(fmtTime(turn.createdUtc), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            turn.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (turn.refused) Palette.Caution else Palette.Chalk,
        )
    }
}

@Composable
private fun QuickChips(onPick: (String) -> Unit) {
    val chips = listOf("?", "status", "run a round", "sync", "promote best", "backtest", "check the news", "why kelly", "explain the gate")
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            Box(
                Modifier.padding(end = 8.dp).clip(RoundedCornerShape(12.dp))
                    .background(Palette.Raised).border(1.dp, Palette.Hairline, RoundedCornerShape(12.dp))
                    .clickable { onPick(chip) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(chip, style = MaterialTheme.typography.labelSmall, color = Palette.Dust)
            }
        }
    }
}

@Composable
private fun InputRow(
    draft: String,
    hint: String,
    busy: Boolean,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    val send = { if (draft.isNotBlank() && !busy) onSend() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "will run: $hint",
            style = MaterialTheme.typography.labelSmall,
            color = Palette.Faint,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                modifier = Modifier.weight(1f),
                placeholder = { Text("status, run a round, why kelly, Jokic is doubtful", color = Palette.Faint) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Palette.Court,
                    unfocusedContainerColor = Palette.Court,
                    focusedTextColor = Palette.Chalk,
                    unfocusedTextColor = Palette.Chalk,
                    cursorColor = Palette.Over,
                    focusedBorderColor = Palette.Hairline,
                    unfocusedBorderColor = Palette.Hairline,
                ),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(if (draft.isBlank() || busy) Palette.Raised else Palette.Court)
                    .border(1.dp, Palette.Hairline, CircleShape)
                    .clickable(enabled = draft.isNotBlank() && !busy) { send() },
                contentAlignment = Alignment.Center,
            ) {
                Text("→", style = MaterialTheme.typography.titleMedium, color = if (draft.isBlank() || busy) Palette.Faint else Palette.Chalk)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
