package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.propslab.PropsLabApp
import com.propslab.billing.Feature
import com.propslab.billing.TierStore
import com.propslab.hub.CuratedModel
import com.propslab.hub.DeviceProfile
import com.propslab.hub.HubFile
import com.propslab.hub.ModelHub
import com.propslab.inference.CloudModel
import com.propslab.mathx.LlamaCppModel
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Metric
import com.propslab.ui.components.Panel
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The model shop.
 *
 * It opens by measuring the phone, not by advertising a model: RAM, free storage and cores
 * decide which shelf is honest. Downloads go straight from Hugging Face into the app's own
 * models directory, and the file the user picks becomes the one the console loads.
 *
 * No model account, no token, no upload.
 */
@Composable
fun ModelShopScreen(onBack: () -> Unit, onOpenPremium: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PropsLabApp
    val scope = rememberCoroutineScope()
    val root = context.filesDir

    var profile by remember { mutableStateOf<DeviceProfile?>(null) }
    var installed by remember { mutableStateOf<List<File>>(emptyList()) }
    var active by remember { mutableStateOf<String?>(null) }
    var shelf by remember { mutableStateOf<List<CuratedModel>>(ModelHub.curated) }
    var selected by remember { mutableStateOf<CuratedModel?>(null) }
    var files by remember { mutableStateOf<List<HubFile>>(emptyList()) }
    var filesBusy by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var message by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Tapping a model has to reveal its files. The panel is item 5 in the list above
    // (back, header, device, installed, shelf, panel), so selection scrolls straight to it.
    LaunchedEffect(selected) {
        if (selected != null) listState.animateScrollToItem(5)
    }

    fun refresh() {
        installed = ModelHub.installed(root)
        active = ModelHub.activeName(root)
    }

    LaunchedEffect(Unit) {
        profile = withContext(Dispatchers.IO) { DeviceProfile.read(context) }
        refresh()
    }

    fun choose(repo: CuratedModel) {
        selected = repo
        files = emptyList()
        filesBusy = true
        scope.launch {
            files = ModelHub.files(repo.repo)
            filesBusy = false
            if (files.isEmpty()) message = "This repository publishes no GGUF file right now."
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
            bottom = 56.dp,
        ),
    ) {
        item {
            Text(
                "\u2039  Lab",
                Modifier.clickable(onClick = onBack).padding(horizontal = 20.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
            )
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Model shop", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Download a language model that fits this phone. It stays on the device.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(18.dp))
            }
        }

        item {
            DeviceCard(profile)
            Spacer(Modifier.height(10.dp))
        }

        item {
            InstalledCard(
                installed = installed,
                active = active,
                onUse = { file ->
                    ModelHub.setActive(root, file.name)
                    (app.mathModel as? LlamaCppModel)?.unload()
                    refresh()
                    message = "${file.name} is now the model the console loads."
                },
                onDelete = { file ->
                    ModelHub.delete(root, file.name)
                    (app.mathModel as? LlamaCppModel)?.unload()
                    refresh()
                    message = "Deleted ${file.name}."
                },
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            Panel {
                Text("Suggested for this phone", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(10.dp))
                shelf.forEach { model ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { choose(model) }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(model.label, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                model.blurb,
                                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Badge(model.params, Palette.Over)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // The file list for the tapped repository sits directly under the shelf: selecting a
        // model has to show its files without the user scrolling to find them.
        val repo = selected
        if (repo != null) {
            item {
                Panel(tone = Palette.Ink) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(repo.label, style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
                            Text(repo.repo, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
                        }
                        Text(
                            "Close",
                            Modifier.clickable { selected = null; files = emptyList() }
                                .padding(8.dp),
                            style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (filesBusy) {
                        Text("Reading the file list…", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                    }
                    files.forEach { file ->
                        val fits = profile?.modelBudgetBytes?.let { file.bytes in 1..it } ?: false
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name.substringAfterLast('/'),
                                    style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Badge(file.quant, Palette.Over)
                                    Text(file.sizeLabel, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                                    if (file.bytes > 0L) {
                                        Badge(
                                            if (fits) "Fits" else "Too big",
                                            if (fits) Palette.Gain else Palette.Caution,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            ActionButton(
                                if (downloading == file.name) "…" else "Get",
                                enabled = downloading == null,
                                onClick = {
                                    downloading = file.name
                                    progress = 0f
                                    message = null
                                    scope.launch {
                                        val result = ModelHub.download(file, root) { done, total ->
                                            progress = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                                        }
                                        downloading = null
                                        result.onSuccess { stored ->
                                            ModelHub.setActive(root, stored.name)
                                            (app.mathModel as? LlamaCppModel)?.unload()
                                            refresh()
                                            message = "${stored.name} downloaded and set as the active model."
                                        }.onFailure { failure ->
                                            message = failure.message ?: "The download failed."
                                        }
                                    }
                                },
                            )
                        }
                    }
                    if (downloading != null) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Palette.Over,
                            trackColor = Palette.Hairline,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "%.0f%% of ${downloading}".format(progress * 100),
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        item {
            Panel {
                Text("Search Hugging Face", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Palette.Chalk),
                    cursorBrush = SolidColor(Palette.Over),
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.Ink)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Qwen, Llama, Phi, Gemma…", style = MaterialTheme.typography.bodyMedium, color = Palette.Faint)
                        }
                        inner()
                    },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(if (searching) "Searching…" else "Search", enabled = !searching && query.isNotBlank(), onClick = {
                        searching = true
                        scope.launch {
                            val found = ModelHub.search(query)
                            if (found.isNotEmpty()) shelf = found
                            else message = "No public GGUF repositories matched \"$query\"."
                            searching = false
                        }
                    })
                    if (shelf !== ModelHub.curated) {
                        ActionButton("Back to picks", onClick = { shelf = ModelHub.curated }, tone = Palette.Dust)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            CloudCard(onOpenPremium)
            Spacer(Modifier.height(10.dp))
        }

        if (message != null) {
            item {
                Text(
                    message.orEmpty(),
                    Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                )
            }
        }

        item {
            Text(
                "Weights are stored only in this app's private files. Removing a model deletes it. " +
                    "Every model listed here is published by its own authors under their own licence.",
                Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun DeviceCard(profile: DeviceProfile?) {
    Panel {
        Text("THIS PHONE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.height(10.dp))
        if (profile == null) {
            Text("Measuring…", style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
            return@Panel
        }
        Row(Modifier.fillMaxWidth()) {
            Metric("Memory", "%.1f GB".format(profile.totalRamGb), Modifier.weight(1f))
            Metric("Free disk", "%.1f GB".format(profile.freeStorageGb), Modifier.weight(1f))
            Metric("Cores", "${profile.cores}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge("Best fit ${profile.recommendedBand}", Palette.Over, filled = true)
            Badge(profile.recommendedQuant, Palette.Dust)
            Badge(profile.abi, Palette.Dust)
        }
        Spacer(Modifier.height(10.dp))
        Text(profile.ramLine, style = MaterialTheme.typography.bodySmall, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(profile.verdict, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
    }
}

@Composable
private fun InstalledCard(
    installed: List<File>,
    active: String?,
    onUse: (File) -> Unit,
    onDelete: (File) -> Unit,
) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ON THIS PHONE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint, modifier = Modifier.weight(1f))
            Badge("${installed.size} installed", if (installed.isEmpty()) Palette.Dust else Palette.Gain)
        }
        Spacer(Modifier.height(8.dp))
        if (installed.isEmpty()) {
            Text(
                "No weights yet. The built-in explanations keep working until one is installed.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
            return@Panel
        }
        installed.forEach { file ->
            val isActive = file.name == active
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("%.2f GB".format(file.length() / 1e9), style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                }
                if (isActive) {
                    Badge("ACTIVE", Palette.Gain, filled = true)
                } else {
                    Text(
                        "Use",
                        Modifier.clickable { onUse(file) }.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium, color = Palette.Over,
                    )
                }
                Text(
                    "Delete",
                    Modifier.clickable { onDelete(file) }.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium, color = Palette.Loss,
                )
            }
        }
    }
}

@Composable
private fun CloudCard(onOpenPremium: () -> Unit) {
    val premium = TierStore.allows(Feature.CLOUD_MODEL)
    var expanded by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf(CloudModel.baseUrl) }
    var model by remember { mutableStateOf(CloudModel.model) }
    var key by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Panel(tone = if (premium) Palette.Court else Palette.Ink) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CLOUD MODEL", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (CloudModel.hasKey) "Connected · key ${CloudModel.keyHint}" else "Optional fallback",
                    style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                )
            }
            Badge("PREMIUM", if (premium) Palette.Gain else Palette.Caution)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "For phones that cannot hold a useful model: send one question to an OpenAI-compatible endpoint you control.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        if (!premium) {
            Spacer(Modifier.height(10.dp))
            ActionButton("Unlock with Premium", onClick = onOpenPremium, tone = Palette.Caution)
            return@Panel
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (expanded) "Hide setup" else "Set up a key",
            Modifier.clickable { expanded = !expanded },
            style = MaterialTheme.typography.labelMedium, color = Palette.Over,
        )
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            CloudField("Base URL", base) { base = it }
            Spacer(Modifier.height(8.dp))
            CloudField("Model", model) { model = it }
            Spacer(Modifier.height(8.dp))
            CloudField("API key (stored privately)", key) { key = it }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Save", onClick = {
                    CloudModel.save(base, key, model)
                    note = if (CloudModel.hasKey) "Key saved." else "A key is required."
                })
                ActionButton(if (busy) "Testing…" else "Test", enabled = !busy && CloudModel.hasKey, onClick = {
                    busy = true
                    scope.launch {
                        note = CloudModel.ping().fold(
                            onSuccess = { "Cloud answered: $it" },
                            onFailure = { it.message ?: "The test failed." },
                        )
                        busy = false
                    }
                })
            }
        }
        if (note != null) {
            Spacer(Modifier.height(8.dp))
            Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Palette.Caution)
        }
    }
}

@Composable
private fun CloudField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Palette.Chalk),
            cursorBrush = SolidColor(Palette.Over),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Palette.Ink).padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
