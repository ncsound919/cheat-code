package com.propslab.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.propslab.brand.Brand
import com.propslab.hub.DeviceProfile
import com.propslab.hub.ReleaseInfo
import com.propslab.hub.UpdateCheck
import com.propslab.hub.Updater
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Panel
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Updates and help, in one place.
 *
 * The update path is a GitHub release feed: one tap checks, one tap downloads the attached
 * APK, and Android's own installer confirms. Community links and the bug-report mail carry
 * the device facts a maintainer would otherwise have to ask for.
 */
@Composable
fun UpdatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf<UpdateCheck?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var note by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
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
                Text("Updates & help", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Version ${Updater.versionName(context)} · installed from ${if (sideloaded(context)) "a direct download" else "a store"}",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(18.dp))
            }
        }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("APP UPDATE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint, modifier = Modifier.weight(1f))
                    Badge("GitHub releases", Palette.Over)
                }
                Spacer(Modifier.height(10.dp))
                val current = Updater.versionName(context)
                when (val state = check) {
                    null -> Text(
                        "Check whether a newer build is published.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    is UpdateCheck.Failed -> Text(state.reason, style = MaterialTheme.typography.bodySmall, color = Palette.Caution)
                    is UpdateCheck.Current -> Text(
                        if (state.release != null) "You are current on v$current (latest ${state.release.tag})."
                        else "You are current on v$current.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Gain,
                    )
                    is UpdateCheck.Available -> {
                        Text(
                            "${state.release.name} · ${state.release.tag}",
                            style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                        )
                        if (state.release.published.isNotBlank()) {
                            Text("Published ${state.release.published}", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                        }
                        if (state.release.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.release.notes.take(700),
                                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                                maxLines = 12,
                            )
                        }
                        if (!state.release.hasApk) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This release has no APK attached. Open the release page instead.",
                                style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(if (checking) "Checking…" else "Check for updates", enabled = !checking && !downloading, onClick = {
                        checking = true
                        note = null
                        scope.launch {
                            check = Updater.check(context)
                            checking = false
                        }
                    })
                    val available = (check as? UpdateCheck.Available)?.release
                    if (available != null && available.hasApk) {
                        ActionButton(if (downloading) "Downloading…" else "Install update", enabled = !downloading, tone = Palette.Gain, onClick = {
                            downloading = true
                            progress = 0f
                            scope.launch {
                                val result = Updater.download(context, available) { done, total ->
                                    progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                                }
                                downloading = false
                                result.onSuccess { file ->
                                    if (!Updater.install(context, file)) {
                                        note = "Android did not open the installer. Allow installing unknown apps for Cheat Code and try again."
                                    }
                                }.onFailure { note = it.message ?: "The download failed." }
                            }
                        })
                    }
                }
                if (downloading) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Palette.Gain,
                        trackColor = Palette.Hairline,
                    )
                }
                if (note != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Palette.Caution)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Open the download page",
                    Modifier.clickable { open(context, Brand.DOWNLOAD) }.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = Palette.Over,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Panel {
                Text("THE MANUAL", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Every screen, with pictures.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
                Spacer(Modifier.height(10.dp))
                ActionButton("Read the manual offline", onClick = { openAsset(context, "manual.html") }, tone = Palette.Over)
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Panel {
                Text("TELL US WHAT BROKE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Build, phone and what you expected are filled in for you.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
                Spacer(Modifier.height(12.dp))
                ActionButton("Email a bug report", onClick = { emailReport(context) }, tone = Palette.Caution)
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Panel {
                Text("COMMUNITY", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(6.dp))
                CommunityRow("Reddit", "r/Cheat Code") { open(context, Brand.REDDIT) }
                CommunityRow("Discord", "Chat during the slate") { open(context, Brand.DISCORD) }
                CommunityRow("Website", "Manual, download, pricing") { open(context, Brand.WEBSITE) }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Panel(tone = Palette.Ink) {
                Text("ABOUT THIS BUILD", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Predictions are paper bets. No account needed.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Not affiliated with the NBA or any sportsbook. Problem gambling helpline: 1-800-522-4700.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
            }
        }
    }
}

@Composable
private fun CommunityRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
        Text("\u2197", style = MaterialTheme.typography.titleMedium, color = Palette.Over)
    }
}

private fun open(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openAsset(context: Context, name: String) {
    val intent = Intent(context, ManualActivity::class.java)
        .putExtra(ManualActivity.EXTRA_ASSET, name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** Carries the facts a maintainer needs, so the first reply can be a fix. */
private fun emailReport(context: Context) {
    val profile = runCatching { DeviceProfile.read(context) }.getOrNull()
    val body = buildString {
        appendLine("What happened:")
        appendLine()
        appendLine("What I expected:")
        appendLine()
        appendLine("---")
        appendLine("App: ${Brand.APP} ${Updater.versionName(context)}")
        if (profile != null) {
            appendLine("Device: ${profile.summary}")
            appendLine("Android SDK ${profile.sdk} · ${profile.abi}")
        }
        appendLine("Model: ${com.propslab.mathx.LlamaCppModel.PREFERRED_MODEL}")
        appendLine("Tier: ${com.propslab.billing.TierStore.tierLabel}")
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(Brand.EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Cheat Code bug · ${Updater.versionName(context)} · ${profile?.model ?: "unknown device"}")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching { context.startActivity(intent) }
}

private fun sideloaded(context: Context): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName == null
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName) == null
    }
}.getOrDefault(true)
