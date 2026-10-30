package com.propslab.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.agent.Sportsbook
import com.propslab.data.ImportOutcome
import com.propslab.data.PropsRepository
import com.propslab.domain.Side
import com.propslab.ocr.MlKitScreenshotReader
import com.propslab.ocr.ScreenshotReader
import com.propslab.ocr.SlipParser
import com.propslab.ocr.SlipPick
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.fmtLine
import com.propslab.ui.fmtOdds
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportScreenState(
    val book: String = Sportsbook.PRIZEPICKS.slug,
    val reading: Boolean = false,
    val text: String = "",
    val picks: List<SlipPick> = emptyList(),
    val error: String? = null,
    val result: ImportOutcome? = null,
    val importing: Boolean = false,
)

class ImportViewModel(
    private val repo: PropsRepository,
    private val reader: ScreenshotReader,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportScreenState())
    val state: StateFlow<ImportScreenState> = _state.asStateFlow()

    fun read(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(reading = true, error = null, result = null) }
            val text = runCatching { reader.read(context, uri) }.getOrElse { failure ->
                _state.update {
                    it.copy(
                        reading = false,
                        error = "The screenshot could not be read (${failure.message ?: "unknown error"}).",
                    )
                }
                return@launch
            }
            _state.update { it.copy(reading = false, text = text, picks = SlipParser.parse(text)) }
        }
    }

    fun setBook(slug: String) = _state.update { it.copy(book = slug) }

    fun addToBoard() {
        val picks = _state.value.picks
        if (picks.isEmpty() || _state.value.importing) return
        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            val outcome = runCatching { repo.importSlip(picks, _state.value.book) }.getOrNull()
            _state.update {
                it.copy(
                    importing = false,
                    result = outcome,
                    error = if (outcome == null) "The picks could not be written to the board." else null,
                )
            }
        }
    }

    fun reset() = _state.update { ImportScreenState(book = it.book) }
}

/**
 * Screenshot import. The OCR runs on the device, the parser is pure Kotlin, and the screen
 * shows the text it actually saw: if nothing was read, that is visible rather than silent.
 */
@Composable
fun ImportScreen(onDone: () -> Unit) {
    val repo = rememberRepo()
    val context = LocalContext.current
    val vm: ImportViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ImportViewModel(repo, MlKitScreenshotReader()) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.read(context, uri)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 48.dp,
        ),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Import a slip", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Photograph or screenshot a bet slip and Cheat Code reads the picks off it. The text " +
                        "recognition is bundled with the app and runs offline.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(18.dp))
                Text("BOOK", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(6.dp))
                Row {
                    Sportsbook.entries.forEachIndexed { index, book ->
                        if (index > 0) Spacer(Modifier.width(8.dp))
                        ChoiceChip(
                            book.displayName.removeSuffix(" Sportsbook"),
                            selected = state.book == book.slug,
                            onClick = { vm.setBook(book.slug) },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                ActionButton(
                    if (state.reading) "Reading…" else "Choose a screenshot",
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    enabled = !state.reading,
                    tone = Palette.Over,
                )
            }
        }

        state.error?.let { message ->
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Panel(tone = Palette.Raised) {
                        Text("Import problem", style = MaterialTheme.typography.titleSmall, color = Palette.Caution)
                        Spacer(Modifier.height(4.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                    }
                }
            }
        }

        val result = state.result
        if (result != null) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Panel {
                        Text("On the board", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${result.added} picks added" +
                                if (result.skipped > 0) ", ${result.skipped} were already there." else ".",
                            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "They arrive with only the book's estimate, so there is no gap for the " +
                                "app to act on and the sandbox will not bet them. The lines and prices " +
                                "are real text from your screenshot.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row {
                            ActionButton("Done", onDone)
                            Spacer(Modifier.width(8.dp))
                            ActionButton("Import another", { vm.reset() }, tone = Palette.Dust)
                        }
                    }
                }
            }
        }

        if (state.picks.isNotEmpty() && result == null) {
            item {
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("READ FROM THE IMAGE", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
                    Spacer(Modifier.weight(1f))
                    Text("${state.picks.size} picks", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                }
            }
            items(state.picks, key = { it.raw + it.line + it.side.name }) { pick ->
                ParsedRow(pick)
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Palette.Hairline)
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    ActionButton(
                        if (state.importing) "Adding…" else "Add ${state.picks.size} picks to the board",
                        onClick = { vm.addToBoard() },
                        enabled = !state.importing,
                    )
                }
            }
        }

        if (!state.reading && state.text.isNotEmpty() && state.picks.isEmpty() && result == null) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Panel(tone = Palette.Raised) {
                        Text("No picks found", style = MaterialTheme.typography.titleSmall, color = Palette.Caution)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "The image was read, but no line matched a side and a number. Crop the slip so " +
                                "the player names and the Over or Under lines are both visible.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                    }
                }
            }
        }

        if (state.text.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("WHAT IT READ", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.text.take(1200),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Faint,
                        maxLines = 24,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ParsedRow(pick: SlipPick) {
    val tone = if (pick.side == Side.OVER) Palette.Over else Palette.Under
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(tone, CircleShape))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(pick.playerName, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk, maxLines = 1)
            Text(
                "${pick.side.label} ${fmtLine(pick.line)} ${pick.marketLabel}",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                pick.odds?.let { fmtOdds(it) } ?: "no price",
                style = MaterialTheme.typography.titleSmall,
                color = if (pick.odds == null) Palette.Caution else Palette.Chalk,
            )
            if (pick.odds == null) {
                Badge("ASSUMES -110", Palette.Caution)
            }
        }
    }
}
