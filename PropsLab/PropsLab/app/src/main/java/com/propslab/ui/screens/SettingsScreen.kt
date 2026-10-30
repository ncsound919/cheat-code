package com.propslab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.propslab.PropsLabApp
import com.propslab.feed.FeedConfig
import com.propslab.feed.FeedSettings
import com.propslab.feed.ResultsProvider
import com.propslab.mathx.LlamaCppModel
import com.propslab.settings.AppSettings
import com.propslab.settings.OddsFormat
import com.propslab.settings.stakeWord
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.components.WorkToggle
import com.propslab.ui.fmtOdds
import com.propslab.ui.fmtPct
import com.propslab.ui.theme.Palette

/**
 * Settings: every knob the app has, in one place, each one wired to something real.
 *
 * Three stores feed this screen and nothing else writes them: [FeedSettings] for the board's
 * request shape, [AppSettings] for display, staking and the sampler, and the model runtime
 * for the context reload. A knob that changed nothing would be a lie told in the UI, so
 * every control here names where its effect lands.
 *
 * Numbers are shown in plain words next to their jargon (the glossary behind "Plain English"
 * covers the terms) because a slider labelled "top-p" alone teaches nobody anything.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PropsLabApp
    val feed = remember { FeedSettings(context) }
    var config by remember { mutableStateOf(feed.load()) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val easy = AppSettings.current.easyMode
    val showAll = !easy || showAdvanced

    fun setFeed(transform: (FeedConfig) -> FeedConfig) {
        config = transform(config)
        feed.save(config)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
            bottom = 48.dp,
        ),
    ) {
        item { SettingsBack(onBack) }
        item { SettingsHeader(easy) }

        item {
            Section(
                title = "READING THE APP",
                blurb = "Easy mode leads with a sentence and hides the probability arithmetic behind a tap. " +
                    "Full mode puts every number on screen at once. The maths is identical either way.",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Easy \u2014 plain words", AppSettings.current.easyMode, onClick = {
                        AppSettings.update { it.copy(easyMode = true) }
                    })
                    ChoiceChip("Full \u2014 every number", !AppSettings.current.easyMode, onClick = {
                        AppSettings.update { it.copy(easyMode = false) }
                    })
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Easy mode is the default. It does not change a pick, a stake or a probability; " +
                        "it changes how much of the reasoning is on the first screen.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
        }

        if (easy) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
                    WorkToggle(showAdvanced, { showAdvanced = !showAdvanced }, label = "Show advanced settings")
                    Text(
                        "Easy mode keeps the knobs that can spoil a first experience out of the way. " +
                            "The defaults are already the safe ones.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                    )
                }
            }
        }

        if (showAll) {
        item {
            Section(
                title = "BOARD & FEED",
                blurb = "How much the app asks the price and results providers for on each sync. " +
                    "A smaller ask is faster and uses less of the free tier; a wider board finds more games.",
            ) {
                Stepper("Board window", config.lookaheadHours, "h", 6..96, step = 6) {
                    setFeed { c -> c.copy(lookaheadHours = it) }
                }
                Stepper("Games pulled per sync", config.maxEvents, "", 1..24, step = 1) {
                    setFeed { c -> c.copy(maxEvents = it) }
                }
                Stepper("Results read back", config.resultsLookbackDays, "days", 1..10, step = 1) {
                    setFeed { c -> c.copy(resultsLookbackDays = it) }
                }
                Stepper("Box scores per sync", config.maxResultGames, "", 1..120, step = 5) {
                    setFeed { c -> c.copy(maxResultGames = it) }
                }
                Stepper("Players checked for news", config.newsMaxPlayers, "", 0..24, step = 1) {
                    setFeed { c -> c.copy(newsMaxPlayers = it) }
                }
                Stepper("News younger than", config.newsLookbackHours, "h", 6..72, step = 6) {
                    setFeed { c -> c.copy(newsLookbackHours = it) }
                }
                Note("Applies to the next sync — the one you start from Live data, or the automatic one every six hours.")
                Spacer(Modifier.height(10.dp))
                Text("Box scores come from", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(
                        "ESPN (no key)",
                        config.resultsProvider == ResultsProvider.ESPN,
                        onClick = { setFeed { c -> c.copy(resultsProvider = ResultsProvider.ESPN) } },
                    )
                    ChoiceChip(
                        "BALLDONTLIE",
                        config.resultsProvider == ResultsProvider.BALLDONTLIE,
                        onClick = { setFeed { c -> c.copy(resultsProvider = ResultsProvider.BALLDONTLIE) } },
                    )
                }
                Text(
                    ResultsProvider.note(config.resultsProvider),
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
            }
        }

        item {
            Section(
                title = "STAKING",
                blurb = "Kelly turns the model's edge into a share of bankroll. Full Kelly is " +
                    "growth-optimal only if the probability is right — this app's is a baseline, " +
                    "so the shipped value is a quarter of it.",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Full", AppSettings.current.stakeFraction >= 0.999, onClick = {
                        AppSettings.update { it.copy(stakeFraction = 1.0) }
                    })
                    ChoiceChip("Half", AppSettings.current.stakeFraction in 0.499..0.501, onClick = {
                        AppSettings.update { it.copy(stakeFraction = 0.5) }
                    })
                    ChoiceChip("Quarter", AppSettings.current.stakeFraction in 0.249..0.251, onClick = {
                        AppSettings.update { it.copy(stakeFraction = 0.25) }
                    })
                }
                Spacer(Modifier.height(10.dp))
                Slider(
                    value = AppSettings.current.stakeFraction.toFloat(),
                    onValueChange = { v -> AppSettings.update { it.copy(stakeFraction = v.toDouble().coerceIn(0.05, 1.0)) } },
                    valueRange = 0.05f..1.0f,
                )
                Text(
                    "Staking ${stakeWord(AppSettings.current.stakeFraction)} Kelly " +
                        "(${fmtPct(AppSettings.current.stakeFraction, 0)} of every suggestion). " +
                        "Every pick row and pick paragraph sizes from this one number.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
        }

        }

        item {
            Section(
                title = "PRICES ON SCREEN",
                blurb = "How a price is printed. The books speak American; decimals are the raw multiplier.",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("American", AppSettings.current.oddsFormat == OddsFormat.AMERICAN, onClick = {
                        AppSettings.update { it.copy(oddsFormat = OddsFormat.AMERICAN) }
                    })
                    ChoiceChip("Decimal", AppSettings.current.oddsFormat == OddsFormat.DECIMAL, onClick = {
                        AppSettings.update { it.copy(oddsFormat = OddsFormat.DECIMAL) }
                    })
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "On screen right now: −110 → ${fmtOdds(-110)}, +150 → ${fmtOdds(150)}.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
            }
        }

        if (showAll) {
        item {
            Section(
                title = "ON-DEVICE MODEL",
                blurb = "How the local model writes. None of this touches the network, and none " +
                    "of it changes any number — only the words around them.",
            ) {
                val s = AppSettings.current
                SliderRow("Creativity (temperature)", s.temperature, 0f..1.5f) { v ->
                    AppSettings.update { it.copy(temperature = v) }
                }
                Text(
                    "0 = the same question always gets the same wording. 1.5 = loose and varied. Default 0.7.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
                SliderRow("Vocabulary spread (top-p)", s.topP, 0.5f..1f) { v ->
                    AppSettings.update { it.copy(topP = v) }
                }
                Text(
                    "How wide a net of possible next words it draws from. Default 0.9 — lower is steadier.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
                Stepper("Word candidates (top-k)", s.topK, "", 0..100, step = 5) { v ->
                    AppSettings.update { it.copy(topK = v) }
                }
                Stepper("Answer length cap", s.maxTokens, "tokens", 64..2048, step = 64) { v ->
                    AppSettings.update { it.copy(maxTokens = v) }
                }
                Text(
                    "A shorter cap answers sooner on the phone. It never shortens a formula answer " +
                        "that asked for less already.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2048, 4096, 8192).forEach { ctx ->
                        ChoiceChip("${ctx / 1024}K context", s.contextSize == ctx, onClick = {
                            AppSettings.update { it.copy(contextSize = ctx) }
                        })
                    }
                }
                Text(
                    "How much conversation the model keeps in working memory. Bigger costs more memory. " +
                        "Applies when the weights load — press below to load them again now.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
                Spacer(Modifier.height(10.dp))
                ActionButton("Reload the model now", onClick = { (app.mathModel as? LlamaCppModel)?.unload() }, tone = Palette.Over)
            }
        }

        item {
            Section(
                title = "MAINTENANCE",
                blurb = "Back to the shipped behaviour: quarter Kelly, American prices, default " +
                    "sampling, default board shape. Keys and data are never touched by this.",
            ) {
                ActionButton("Reset all settings to defaults", onClick = {
                    AppSettings.reset()
                    config = feed.load()
                }, tone = Palette.Loss)
            }
        }

        }

        item {
            Text(
                "Keys live in this app's private storage and are never shown back or logged. " +
                    "Nothing on this screen sends anything anywhere.",
                Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun SettingsBack(onBack: () -> Unit) {
    Text(
        "\u2039  Lab",
        Modifier.clickable(onClick = onBack).padding(horizontal = 20.dp, vertical = 14.dp),
        style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
    )
}

@Composable
private fun SettingsHeader(easy: Boolean) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Settings", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
        Spacer(Modifier.height(6.dp))
        Text(
            if (easy) {
                "The settings most people need. The advanced ones are one tap away and already have safe defaults."
            } else {
                "Everything the app lets you tune, and what each knob actually changes. " +
                    "Defaults are the values the app shipped with."
            },
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        Spacer(Modifier.height(18.dp))
    }
}

/** A titled block with the same rhythm as the lab's cards. */
@Composable
private fun Section(title: String, blurb: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 9.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        Spacer(Modifier.height(6.dp))
        Text(blurb, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        Spacer(Modifier.height(12.dp))
        Panel { content() }
    }
}

@Composable
private fun Note(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = Palette.Faint, maxLines = 3)
}

/**
 * Label, live value, and 48dp minus/plus targets. The value is read from state on every
 * recomposition, so stepping and the summary line above never disagree.
 */
@Composable
private fun Stepper(
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    step: Int = 1,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
        StepButton("\u2212") { onChange((value - step).coerceIn(range)) }
        Text(
            if (unit.isEmpty()) "$value" else "$value $unit",
            Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            color = Palette.Chalk,
            maxLines = 1,
        )
        StepButton("+") { onChange((value + step).coerceIn(range)) }
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = MaterialTheme.typography.titleMedium, color = Palette.Over)
    }
}

/** A slider with its label and current value on one line. */
@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
            Text(
                "%.2f".format(value),
                style = MaterialTheme.typography.titleSmall, color = Palette.Chalk, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
