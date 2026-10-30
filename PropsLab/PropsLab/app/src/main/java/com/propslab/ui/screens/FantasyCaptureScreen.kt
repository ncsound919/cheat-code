package com.propslab.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.fantasy.CaptureKind
import com.propslab.fantasy.CapturedPlayer
import com.propslab.fantasy.CapturedTeam
import com.propslab.fantasy.FantasyCaptureParser
import com.propslab.fantasy.FantasyCaptureSave
import com.propslab.fantasy.FantasySaveResult
import com.propslab.fantasy.FantasySlots
import com.propslab.fantasy.FantasySport
import com.propslab.ocr.MlKitScreenshotReader
import com.propslab.ocr.ScreenshotReader
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.theme.Palette
import com.propslab.work.FantasyAssetsWorker
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class FantasyCaptureState(
    val sport: FantasySport = FantasySport.BASKETBALL,
    val kind: CaptureKind = CaptureKind.MY_TEAM,
    val leagueName: String = "",
    val myTeamName: String = "My team",
    val opponentName: String = "Opponent",
    val week: Int = FantasyCaptureParser.defaultWeek(FantasySport.BASKETBALL),
    val reading: Boolean = false,
    val imageIndex: Int = 0,
    val imageCount: Int = 0,
    val players: List<CapturedPlayer> = emptyList(),
    val teams: List<CapturedTeam> = emptyList(),
    val warnings: List<String> = emptyList(),
    val rawText: String = "",
    val error: String? = null,
    val saving: Boolean = false,
    val saved: FantasySaveResult? = null,
    val saveNote: String = "",
) {
    val activeTeam: String
        get() = when (kind) {
            CaptureKind.MY_TEAM -> myTeamName
            CaptureKind.OPPONENT -> opponentName
            CaptureKind.LEAGUE_TABLE -> ""
        }

    val hasRows: Boolean get() = if (kind == CaptureKind.LEAGUE_TABLE) teams.isNotEmpty() else players.isNotEmpty()
}

class FantasyCaptureViewModel(
    private val repo: com.propslab.data.PropsRepository,
    private val reader: ScreenshotReader,
) : ViewModel() {

    var state by mutableStateOf(FantasyCaptureState())
        private set

    init {
        loadExisting(state.sport)
    }

    fun setSport(next: FantasySport) {
        if (state.reading || state.saving || next == state.sport) return
        state = state.copy(
            sport = next,
            week = FantasyCaptureParser.defaultWeek(next),
            players = emptyList(),
            teams = emptyList(),
            warnings = emptyList(),
            rawText = "",
            error = null,
            saved = null,
            saveNote = "",
        )
        loadExisting(next)
    }

    fun setKind(next: CaptureKind) {
        if (state.reading || state.saving || next == state.kind) return
        state = state.copy(kind = next, error = null, saved = null)
    }

    fun setLeagueName(value: String) {
        if (state.saving || state.reading) return
        state = state.copy(leagueName = value.take(60), saved = null)
    }

    fun setMyTeam(value: String) {
        if (state.saving || state.reading) return
        state = state.copy(myTeamName = value.take(40), saved = null)
    }

    fun setOpponent(value: String) {
        if (state.saving || state.reading) return
        state = state.copy(opponentName = value.take(40), saved = null)
    }

    fun bumpWeek(delta: Int) {
        if (state.reading || state.saving) return
        state = state.copy(week = (state.week + delta).coerceIn(1, 30), saved = null)
    }

    fun removePlayer(key: String) {
        if (state.saving) return
        state = state.copy(players = state.players.filterNot { it.key == key })
    }

    fun read(context: Context, uris: List<Uri>) {
        if (uris.isEmpty() || state.reading) return
        val sport = state.sport
        val kind = state.kind
        val team = state.activeTeam
        state = state.copy(
            reading = true,
            imageIndex = 0,
            imageCount = uris.size,
            players = emptyList(),
            teams = emptyList(),
            warnings = emptyList(),
            rawText = "",
            error = null,
            saved = null,
            saveNote = "",
        )
        viewModelScope.launch {
            val players = LinkedHashMap<String, CapturedPlayer>()
            val teams = LinkedHashMap<String, CapturedTeam>()
            val warnings = ArrayList<String>()
            val texts = ArrayList<String>()
            var detectedWeek: Int? = null
            var detectedLeague = ""
            var failures = 0
            uris.forEachIndexed { index, uri ->
                state = state.copy(imageIndex = index + 1)
                runCatching { reader.read(context, uri) }.fold(
                    onSuccess = { text ->
                        if (text.isBlank()) {
                            failures++
                        } else {
                            val page = FantasyCaptureParser.parse(
                                text = text,
                                kind = kind,
                                sport = sport,
                                teamHint = team,
                            )
                            if (detectedWeek == null && page.week != null) detectedWeek = page.week
                            if (detectedLeague.isBlank() && page.leagueName.isNotBlank()) {
                                detectedLeague = page.leagueName
                            }
                            page.players.forEach { player ->
                                val old = players[player.key]
                                if (old == null || (old.projected == null && player.projected != null)) {
                                    players[player.key] = player
                                }
                            }
                            page.teams.forEach { teamRow ->
                                val old = teams[teamRow.key]
                                if (old == null || teamRow.rank > 0 || old.rank == 0) teams[teamRow.key] = teamRow
                            }
                            warnings += page.warnings
                            texts += text
                        }
                    },
                    onFailure = { failures++ },
                )
            }
            state = state.copy(
                reading = false,
                imageIndex = 0,
                players = players.values.sortedWith(
                    compareBy<CapturedPlayer> { FantasySlots.isBench(it.slot) }
                        .thenByDescending { it.projected ?: 0.0 }
                        .thenBy { it.name },
                ),
                teams = teams.values.sortedWith(
                    compareBy<CapturedTeam> { if (it.rank > 0) it.rank else 99 }
                        .thenByDescending { it.wins }
                        .thenBy { it.name },
                ),
                warnings = warnings.distinct(),
                rawText = texts.joinToString("\n\n--- screenshot ---\n\n"),
                leagueName = state.leagueName.ifBlank { detectedLeague },
                week = detectedWeek ?: state.week,
                error = if (failures == uris.size) {
                    "The screenshot could not be read. Try a clearer image or the gallery picker."
                } else {
                    null
                },
            )
        }
    }

    fun save(context: Context) {
        if (state.saving || state.reading || !state.hasRows) return
        val team = state.activeTeam
        if (state.kind != CaptureKind.LEAGUE_TABLE && team.isBlank()) {
            state = state.copy(error = "Give the team a name before saving.")
            return
        }
        state = state.copy(saving = true, error = null, saved = null, saveNote = "")
        viewModelScope.launch {
            val capture = FantasyCaptureSave(
                sport = state.sport,
                kind = state.kind,
                leagueName = state.leagueName,
                teamName = team,
                opponentName = state.opponentName,
                week = state.week,
                players = state.players,
                teams = state.teams,
                platform = platformFromRaw(state.rawText),
            )
            runCatching { repo.saveFantasyCapture(capture) }.fold(
                onSuccess = { result ->
                    state = state.copy(
                        saving = false,
                        saved = result,
                        saveNote = if (state.kind == CaptureKind.LEAGUE_TABLE) {
                            "Standings saved. Capture your lineup next for the weekly calls."
                        } else {
                            "Photos are loading in the background. The calls below use the projections you imported and the public ESPN form."
                        },
                    )
                    FantasyAssetsWorker.enqueue(context, result.leagueId)
                },
                onFailure = { failure ->
                    state = state.copy(
                        saving = false,
                        error = failure.message ?: "The league could not be saved.",
                    )
                },
            )
        }
    }

    private fun platformFromRaw(raw: String): String = when {
        raw.contains("ESPN", ignoreCase = true) -> "ESPN"
        raw.contains("Yahoo", ignoreCase = true) -> "Yahoo"
        raw.contains("Sleeper", ignoreCase = true) -> "Sleeper"
        else -> "Screenshot"
    }

    private fun loadExisting(sport: FantasySport) {
        viewModelScope.launch {
            val saved = repo.latestFantasyLeague(sport) ?: return@launch
            if (state.sport != sport) return@launch
            val opponent = saved.weeks.maxByOrNull { it.week }?.opponent.orEmpty()
            state = state.copy(
                leagueName = saved.league.name,
                myTeamName = saved.league.myTeam.ifBlank { state.myTeamName },
                opponentName = opponent.ifBlank { state.opponentName },
                week = maxOf(state.week, saved.league.week),
            )
        }
    }
}

/**
 * The capture screen.
 *
 * The camera and the photo picker both feed the same on-device OCR. The review list is the
 * important part: it shows exactly what was read, names the lines that were not player rows,
 * and lets a bad row be removed before it becomes a week of advice.
 */
@Composable
fun FantasyCaptureScreen(onBack: () -> Unit) {
    val repo = rememberRepo()
    val context = LocalContext.current
    val vm: FantasyCaptureViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FantasyCaptureViewModel(repo, MlKitScreenshotReader()) }
        },
    )
    val state = vm.state
    var pendingCamera by remember { mutableStateOf<Uri?>(null) }
    var showText by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris ->
        if (uris.isNotEmpty()) vm.read(context, uris)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCamera
        pendingCamera = null
        if (ok && uri != null) vm.read(context, listOf(uri))
    }

    fun takePhoto() {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pendingCamera = uri
        camera.launch(uri)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = 56.dp,
        ),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "‹  Fantasy desk",
                    Modifier.clickable(role = Role.Button, onClick = onBack).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.Dust,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Capture the league",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.displayMedium,
                        color = Palette.Chalk,
                    )
                    Badge("ON DEVICE", Palette.Over, filled = true)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "Photograph or screenshot a lineup, an opponent's lineup, or the league table. " +
                        "The text is read on this phone; the image is never uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Dust,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        if (state.saved != null) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Panel {
                        Text("Saved to your league", style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${state.saved.players} player${if (state.saved.players == 1) "" else "s"} and " +
                                "${state.saved.teams} team${if (state.saved.teams == 1) "" else "s"} stored for week ${state.week}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.Dust,
                        )
                        if (state.saveNote.isNotBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(state.saveNote, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
                        }
                        Spacer(Modifier.height(14.dp))
                        ActionButton("Back to the desk", onBack, tone = Palette.Over)
                    }
                }
            }
            item { CapturePrivacyNote() }
            return@LazyColumn
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("SPORT", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FantasySport.entries.forEach { sport ->
                        Box(Modifier.weight(1f)) {
                            ChoiceChip(
                                label = sport.label,
                                selected = state.sport == sport,
                                onClick = { vm.setSport(sport) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("WHAT IS IN THE IMAGE?", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CaptureKind.entries.forEach { kind ->
                        Box(Modifier.weight(1f)) {
                            ChoiceChip(
                                label = kind.label,
                                selected = state.kind == kind,
                                onClick = { vm.setKind(kind) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    state.kind.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Faint,
                )
                Spacer(Modifier.height(14.dp))
                CaptureField(
                    label = "LEAGUE NAME",
                    value = state.leagueName,
                    placeholder = "e.g. Sunday Scaries",
                    onChange = vm::setLeagueName,
                )
                if (state.kind != CaptureKind.LEAGUE_TABLE) {
                    Spacer(Modifier.height(10.dp))
                    CaptureField(
                        label = if (state.kind == CaptureKind.MY_TEAM) "YOUR TEAM NAME" else "OPPONENT TEAM NAME",
                        value = state.activeTeam,
                        placeholder = if (state.kind == CaptureKind.MY_TEAM) "My team" else "Opponent",
                        onChange = if (state.kind == CaptureKind.MY_TEAM) vm::setMyTeam else vm::setOpponent,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("WEEK", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.width(12.dp))
                    StepButton("−") { vm.bumpWeek(-1) }
                    Text(
                        "${state.week}",
                        Modifier.width(42.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Palette.Chalk,
                    )
                    StepButton("+") { vm.bumpWeek(1) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Read from the header when printed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.Faint,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        if (state.reading) "Reading…" else "Choose screenshots",
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !state.reading && !state.saving,
                        tone = Palette.Over,
                    )
                    ActionButton(
                        "Take a photo",
                        onClick = { takePhoto() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.reading && !state.saving,
                        tone = Palette.Over,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Up to 9 gallery images at once. A lineup, an opponent and the league table can be three separate captures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Faint,
                )
            }
        }

        if (state.reading) {
            item {
                Panel(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        "Reading screenshot ${state.imageIndex} of ${state.imageCount}…",
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.Chalk,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Text recognition runs on the device. Nothing is sent while it reads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Dust,
                    )
                }
            }
        }

        state.error?.let { message ->
            item {
                Panel(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), tone = Palette.Raised) {
                    Text("Capture problem", style = MaterialTheme.typography.titleSmall, color = Palette.Caution)
                    Spacer(Modifier.height(4.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                }
            }
        }

        if (state.players.isNotEmpty() && state.kind != CaptureKind.LEAGUE_TABLE) {
            item {
                SectionHeader(
                    title = "READ FROM THE IMAGE",
                    detail = "${state.players.size} player${if (state.players.size == 1) "" else "s"} · " +
                        "${state.players.count { it.projected != null }} with a projection",
                )
            }
            items(state.players, key = { it.key }) { player ->
                CapturedPlayerRow(player, onRemove = { vm.removePlayer(player.key) })
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Palette.Hairline)
            }
        }

        if (state.teams.isNotEmpty() && state.kind == CaptureKind.LEAGUE_TABLE) {
            item {
                SectionHeader(
                    title = "READ FROM THE TABLE",
                    detail = "${state.teams.size} team${if (state.teams.size == 1) "" else "s"} · records as printed",
                )
            }
            items(state.teams, key = { it.key }) { team ->
                CapturedTeamRow(team)
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Palette.Hairline)
            }
        }

        if (state.warnings.isNotEmpty()) {
            item {
                Panel(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), tone = Palette.Raised) {
                    Text("WHAT WAS NOT CLEAR", style = MaterialTheme.typography.labelSmall, color = Palette.Caution)
                    Spacer(Modifier.height(4.dp))
                    state.warnings.forEach { warning ->
                        Text(
                            "•  $warning",
                            Modifier.padding(vertical = 3.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Dust,
                        )
                    }
                }
            }
        }

        if (state.rawText.isNotBlank()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        if (showText) "Hide the raw text" else "Show the raw text that was read",
                        Modifier.clickable(role = Role.Button) { showText = !showText }
                            .padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Palette.Over,
                    )
                    if (showText) {
                        Panel(tone = Palette.Raised) {
                            Text(
                                state.rawText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.Dust,
                                maxLines = 120,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (state.hasRows) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    ActionButton(
                        if (state.saving) "Saving…" else "Save week ${state.week}",
                        onClick = { vm.save(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.saving && !state.reading,
                        tone = Palette.Over,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Saving replaces this team's rows for week ${state.week} only. Earlier weeks stay in the season track.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Faint,
                    )
                }
            }
        }

        item { CapturePrivacyNote() }
    }
}

@Composable
private fun CaptureField(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.Chalk),
            cursorBrush = SolidColor(Palette.Over),
            modifier = Modifier.fillMaxWidth().clip(shape).background(Palette.Raised)
                .border(1.dp, Palette.Hairline, shape).padding(horizontal = 12.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Palette.Faint)
                }
                inner()
            },
        )
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)).background(Palette.Raised)
            .border(1.dp, Palette.Hairline, RoundedCornerShape(11.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
    }
}

@Composable
private fun SectionHeader(title: String, detail: String) {
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        Spacer(Modifier.height(3.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
    }
}

@Composable
private fun CapturedPlayerRow(player: CapturedPlayer, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (player.slot.isNotBlank()) {
                    Badge(player.slot.uppercase(Locale.ROOT), Palette.Over)
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    player.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Palette.Chalk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                listOf(player.proTeam, player.opponent).filter { it.isNotBlank() }.joinToString(" · ")
                    .ifBlank { "pro team not read" },
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Faint,
            )
            if (player.projected != null || player.seasonAverage != null) {
                Text(
                    listOfNotNull(
                        player.projected?.let { "%.1f projected".format(it) },
                        player.seasonAverage?.let { "%.1f avg".format(it) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Dust,
                )
            }
        }
        Text(
            "Remove",
            Modifier.clickable(role = Role.Button, onClick = onRemove).padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.Caution,
        )
    }
}

@Composable
private fun CapturedTeamRow(team: CapturedTeam) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (team.rank > 0) "${team.rank}" else "—",
            Modifier.width(24.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.Faint,
        )
        Column(Modifier.weight(1f)) {
            Text(
                team.name,
                style = MaterialTheme.typography.titleSmall,
                color = Palette.Chalk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    team.record,
                    team.pointsFor?.let { "%.1f PF".format(it) },
                    team.pointsAgainst?.let { "%.1f PA".format(it) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun CapturePrivacyNote() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "What leaves the phone: nothing from the image. The OCR is bundled and offline. After " +
                "saving, only each player's name is sent to ESPN's public directory to fetch a headshot " +
                "and club logo. A missing match stays as initials.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Faint,
        )
    }
}
