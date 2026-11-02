package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.agent.ToolCapability
import com.propslab.agent.ToolRegistry
import com.propslab.agent.ToolSpec
import com.propslab.data.AutopilotUi
import com.propslab.data.ClaimFormulaEntity
import com.propslab.data.LearningUi
import com.propslab.data.PropsRepository
import com.propslab.mathx.FormulaReport
import com.propslab.mathx.FormulaStep
import com.propslab.mathx.FormulaCheckResult
import com.propslab.mathx.FormulaLab
import com.propslab.mathx.LocalModelStatus
import com.propslab.mathx.LocalMathModel
import com.propslab.settings.AppSettings
import com.propslab.sim.InsightKind
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.AutopilotCard
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PlainLink
import com.propslab.ui.components.WorkToggle
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The strategy studio. One sentence in, one tested lens out.
 *
 * The point of this screen is that the user does not have to know what a tilt, a no-vig
 * probability or a t-statistic is. They write what they believe; the app compiles it, tests
 * it on a season it has not seen, and reports whether the idea survived in words. The
 * arithmetic is still one tap away, because hiding it is not the same as not having it.
 */
class LabViewModel(private val repo: PropsRepository) : ViewModel() {
    var claimText by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var last by mutableStateOf<ClaimFormulaEntity?>(null)
        private set

    val learning: StateFlow<LearningUi> = repo.learning()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningUi())

    val auto: StateFlow<AutopilotUi> = repo.autopilot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AutopilotUi())

    var autoBusy by mutableStateOf(false)
        private set
    var autoMessage by mutableStateOf<String?>(null)
        private set

    fun runEverything() {
        if (autoBusy) return
        autoBusy = true
        autoMessage = null
        viewModelScope.launch {
            autoMessage = runCatching { repo.runEverythingNow() }
                .getOrElse { it.message ?: "The automatic pass failed. Nothing was written." }
            autoBusy = false
        }
    }

    var learningBusy by mutableStateOf(false)
        private set
    var learningMessage by mutableStateOf<String?>(null)
        private set

    fun learnNow() {
        if (learningBusy) return
        learningBusy = true
        learningMessage = null
        viewModelScope.launch {
            val outcome = runCatching { repo.learn() }
            learningBusy = false
            outcome.fold(
                onSuccess = { learningMessage = it.note },
                onFailure = { learningMessage = it.message ?: "The learning round failed." },
            )
        }
    }

    fun setText(value: String) {
        claimText = value.take(280)
        if (message != null) message = null
    }

    fun build() {
        val text = claimText.trim()
        if (busy) return
        if (text.length < 8) {
            message = "Say a little more — one sentence about why this game or this player is different."
            return
        }
        busy = true
        message = null
        viewModelScope.launch {
            val outcome = runCatching { repo.createClaim(text) }
            busy = false
            outcome.fold(
                onSuccess = {
                    last = it.claim
                    message = it.note
                    claimText = ""
                },
                onFailure = { message = it.message ?: "The strategy could not be built." },
            )
        }
    }
}

@Composable
fun LabScreen(
    onOpenImport: () -> Unit,
    onOpenFeed: () -> Unit,
    onOpenConsole: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlain: () -> Unit,
    onOpenLenses: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenFlyer: () -> Unit,
    onOpenFantasy: () -> Unit,
    onOpenNews: () -> Unit,
) {
    val model = rememberModel()
    val tools = rememberTools()
    val reports = remember { FormulaLab.report() }
    val status = remember(model) { model.status() }
    val passed = reports.sumOf { it.passed }
    val total = reports.sumOf { it.total }
    val repo = rememberRepo()
    val vm: LabViewModel = viewModel(factory = viewModelFactory { initializer { LabViewModel(repo) } })
    val learning by vm.learning.collectAsStateWithLifecycle()
    val auto by vm.auto.collectAsStateWithLifecycle()
    val provenance = remember(repo) { repo.championProvenance() }
    val easy = AppSettings.current.easyMode
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val showAll = !easy || showAdvanced

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 120.dp,
        ),
    ) {
        item { LabHeader(passed, total, reports.size, easy, onOpenSettings, onOpenPlain, onOpenLenses) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                AutopilotCard(
                    ui = auto,
                    running = vm.autoBusy,
                    message = vm.autoMessage,
                    onRunAll = vm::runEverything,
                    onOpenFeed = onOpenFeed,
                )
            }
            Spacer(Modifier.height(18.dp))
        }
        item {
            StudioCard(vm, onOpenLenses)
            Spacer(Modifier.height(18.dp))
        }
        item {
            LearningCard(learning, vm.learningBusy, vm.learningMessage, provenance, vm::learnNow, easy = !showAll)
            Spacer(Modifier.height(10.dp))
            if (easy) {
                WorkToggle(
                    showAdvanced,
                    { showAdvanced = !showAdvanced },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    label = "Show settings and tools",
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        if (showAll) {
            item {
                LabShortcuts(onOpenModels, onOpenFlyer, onOpenUpdates, onOpenPremium)
                Spacer(Modifier.height(18.dp))
            }
            item {
                ImportCard(onOpenImport)
                Spacer(Modifier.height(18.dp))
            }
            item {
                FeedCard(onOpenFeed)
                Spacer(Modifier.height(18.dp))
            }
            item {
                FantasyDeskCard(onOpenFantasy)
                Spacer(Modifier.height(12.dp))
                SportsWireCard(onOpenNews)
                Spacer(Modifier.height(18.dp))
            }
            item {
                ConsoleCard(onOpenConsole)
                Spacer(Modifier.height(18.dp))
            }
            item {
                ModelCard(model, status)
                Spacer(Modifier.height(18.dp))
            }
            item {
                ToolsCard(tools)
                Spacer(Modifier.height(18.dp))
            }
            items(reports, key = { it.spec.id }) { report ->
                FormulaCard(report)
                Spacer(Modifier.height(16.dp))
            }
            item { ProvenanceCard() }
        }
    }
}

@Composable
private fun LabHeader(
    passed: Int,
    total: Int,
    formulas: Int,
    easy: Boolean,
    onOpenSettings: () -> Unit,
    onOpenPlain: () -> Unit,
    onOpenLenses: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Strategy lab", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
        Spacer(Modifier.height(6.dp))
        Text(
            if (easy) "Say what you believe. The app tests it in a practice league."
            else "Say what you believe. The app tests it on a season it has not seen.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
        )
        if (!easy) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$passed / $total checks pass",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (passed == total) Palette.Gain else Palette.Caution,
                )
                Spacer(Modifier.weight(1f))
                Text("$formulas formulas", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            }
        }
        Spacer(Modifier.height(8.dp))
        // The links get their own row: on a 411dp phone the checks text and three links do not
        // fit on one line, and Compose answers an over-full Row by drawing them on top of each other.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PlainLink(onOpenLenses, label = "Lenses")
            Spacer(Modifier.width(10.dp))
            PlainLink(onOpenSettings, label = "Settings")
            Spacer(Modifier.width(10.dp))
            PlainLink(onOpenPlain)
        }
        Spacer(Modifier.height(14.dp))
    }
}

/**
 * The shop, the cards, updates and Premium, one tap from the lab. Kept as a single row of
 * small links rather than four more cards: these are destinations, not features.
 */
@Composable
private fun LabShortcuts(
    onOpenModels: () -> Unit,
    onOpenFlyer: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlainLink(onOpenModels, label = "Models", modifier = Modifier.weight(1f))
        PlainLink(onOpenFlyer, label = "Cards", modifier = Modifier.weight(1f))
        PlainLink(onOpenUpdates, label = "Updates", modifier = Modifier.weight(1f))
        PlainLink(onOpenPremium, label = "Premium", modifier = Modifier.weight(1f))
    }
}

/** The one control a casual user needs: a sentence, a few examples, one button. */
@Composable
private fun StudioCard(vm: LabViewModel, onOpenLenses: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val examples = listOf(
        "Their defense decides this one",
        "He goes under because the game gets away from them",
        "Back-to-back, his legs are gone",
        "He feasts because their big man is out",
    )
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Strategy studio", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("ONE SENTENCE IN", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        }
        Spacer(Modifier.height(6.dp))
        Text("What do you think will happen?", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "No numbers needed. Say it the way you would say it to a friend; the app works out what " +
                "that means for a bet, and tests it before it is allowed near the board.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = vm.claimText,
            onValueChange = vm::setText,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    "e.g. their defense is too good and the game stays low",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Over.copy(alpha = 0.6f),
                unfocusedBorderColor = Palette.Hairline,
                focusedTextColor = Palette.Chalk,
                unfocusedTextColor = Palette.Chalk,
                cursorColor = Palette.Over,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text("TRY ONE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            examples.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { example ->
                        Box(Modifier.weight(1f)) { ChoiceChip(example, false, { vm.setText(example) }) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ActionButton(
            if (vm.busy) "Testing it now…" else "Build my strategy",
            vm::build,
            enabled = !vm.busy,
            tone = Palette.Over,
        )
        if (vm.message != null) {
            Spacer(Modifier.height(12.dp))
            Text(vm.message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Palette.Chalk)
        }
        val last = vm.last
        if (last != null) {
            Spacer(Modifier.height(12.dp))
            Panel(tone = Palette.Raised) {
                Text(last.name, style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
                Spacer(Modifier.height(4.dp))
                Text(last.plain, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "How it reads a bet →",
                        Modifier.clickable(onClick = onOpenLenses),
                        style = MaterialTheme.typography.labelMedium, color = Palette.Over,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportCard(onOpenImport: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Screenshot import", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("ON DEVICE", style = MaterialTheme.typography.labelSmall, color = Palette.Gain)
        }
        Spacer(Modifier.height(6.dp))
        Text("Read a slip off a screenshot", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "The reading happens on your phone — nothing is uploaded. It only claims what it can " +
                "actually read; anything unclear is skipped, never guessed.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        com.propslab.ui.components.ActionButton("Open the importer", onOpenImport, tone = Palette.Over)
    }
}

@Composable
private fun FeedCard(onOpenFeed: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Live data", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("TWO READS", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
        }
        Spacer(Modifier.height(6.dp))
        Text("Price the live book", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add two read-only keys: one for prices, one for final game stats. Prices fill the " +
                "board; stats feed the model and settle what has finished. Neither can write or spend anything.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        com.propslab.ui.components.ActionButton("Open the feed", onOpenFeed, tone = Palette.Over)
    }
}

/**
 * The two consumer surfaces added on top of the lab. Both open even for a free build: the
 * destination explains itself and shows the placeholder, which is more useful than a card
 * that looks broken because its button does nothing.
 */
@Composable
private fun FantasyDeskCard(onOpen: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val premium = com.propslab.billing.TierStore.allows(com.propslab.billing.Feature.FANTASY_DESK)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Fantasy desk", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Badge(
                if (premium) "INCLUDED" else "PREMIUM",
                if (premium) Palette.Gain else Palette.Caution,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("Start/sit for football and basketball", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "Track players on this phone, see their last games scored for fantasy, read the " +
                "league injury report and get a form call with the reasons behind it.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        com.propslab.ui.components.ActionButton("Open the fantasy desk", onOpen, tone = Palette.Over)
    }
}

@Composable
private fun SportsWireCard(onOpen: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val premium = com.propslab.billing.TierStore.allows(com.propslab.billing.Feature.SPORTS_WIRE)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sports wire", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Badge(
                if (premium) "INCLUDED" else "PREMIUM",
                if (premium) Palette.Gain else Palette.Caution,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("The day's headlines, one feed", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "NFL, NBA, MLB and NHL from ESPN's public news feed, filterable by league and linked " +
                "back to the publisher. No article text is copied.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        com.propslab.ui.components.ActionButton("Open the wire", onOpen, tone = Palette.Over)
    }
}

/**
 * The ledger, the two fitted corrections and the button that runs a learning round. The
 * point of the card is that the loop is inspectable: a number that changes the model is
 * never allowed to be a number the user cannot see.
 */
@Composable
private fun LearningCard(
    learning: LearningUi,
    busy: Boolean,
    message: String?,
    provenance: String?,
    onLearn: () -> Unit,
    easy: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Learning ledger", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text(
                if (learning.settled > 0) "SELF-LEARNING" else "WAITING FOR RESULTS",
                style = MaterialTheme.typography.labelSmall,
                color = if (learning.settled > 0) Palette.Gain else Palette.Caution,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (easy) "The app learns from every result" else "Every bet, high confidence or not",
            style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (easy) {
                "Finished bets correct the model automatically, and losing patterns become ideas for " +
                    "strategies the app tests before they can reach the real board. Nothing here moves real money."
            } else {
                "The app writes its lean on every priced prop and game market to this ledger at one fake " +
                    "unit. Settled results refit a correction that is applied to new predictions, and every " +
                    "round reads the losing segments for rule changes to test. The promotion gate still owns " +
                    "the live book, and nothing here moves real money."
            },
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        Text(learning.summary, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
        if (!easy) {
            learning.calibration.forEach { calibration ->
                Spacer(Modifier.height(6.dp))
                Text(calibration.summary, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            }
        }
        if (learning.insights.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("WHAT THE LOSSES SAY", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.height(3.dp))
            Text(learning.insightNote, style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            learning.insights.take(3).forEach { insight ->
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(insight.kind.label.uppercase(), insightTone(insight.kind))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        insight.title,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!easy) {
                    Spacer(Modifier.height(3.dp))
                    Text(insight.detail, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                }
                if (!easy) {
                    val candidate = insight.proposalLabel?.let { "Candidate: $it" }
                    val footer = listOfNotNull(candidate, insight.status).joinToString(" · ")
                    if (footer.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(footer, style = MaterialTheme.typography.labelSmall, color = Palette.Over)
                    } else if (insight.advice.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(insight.advice, style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    }
                }
            }
            if (!easy) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "A segment needs at least 12 settled props. A candidate is only a hypothesis: the gate has " +
                        "to test it on a fresh season, and an insight never promotes itself.",
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
            }
        }
        if (!easy) provenance?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                "Points run on the Kaggle empirical table: $it. Every other market keeps the game-log " +
                    "normal, because the promoted GBM's feature set is not in this feed.",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
        if (!easy && learning.recent.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("RECENT LEDGER", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            learning.recent.take(4).forEach { row ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "${row.label} · ${row.outcome ?: "open"}" +
                        (row.profit?.let { " · ${if (it >= 0) "+" else ""}${ "%.2f".format(it) } u" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (row.outcome) {
                        "WIN" -> Palette.Gain
                        "LOSS" -> Palette.Loss
                        else -> Palette.Dust
                    },
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ActionButton(
            if (busy) "Learning…" else if (easy) "Update what it has learned" else "Run a learning round",
            onLearn,
            enabled = !busy,
            tone = Palette.Over,
        )
        message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.Chalk)
        }
    }
}

private fun insightTone(kind: InsightKind): Color = when (kind) {
    InsightKind.LEAK -> Palette.Loss
    InsightKind.TREND -> Palette.Caution
    InsightKind.EDGE -> Palette.Gain
    InsightKind.PATTERN -> Palette.Over
}

/**
 * The way into the console. It is a card rather than a sixth tab because it is a tool you
 * pick up, not a place the work lives.
 */
@Composable
private fun ConsoleCard(onOpenConsole: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Agent console", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("ON DEVICE", style = MaterialTheme.typography.labelSmall, color = Palette.Gain)
        }
        Spacer(Modifier.height(6.dp))
        Text("Talk to it, or tell it to work", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "A sentence that names an operation runs it — a round, a sync, a promotion, a news check, " +
                "a theory note. Anything else is answered by the local model and marked as said, not run.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(12.dp))
        com.propslab.ui.components.ActionButton("Open the console", onOpenConsole, tone = Palette.Over)
    }
}

@Composable
private fun ModelCard(model: LocalMathModel, status: LocalModelStatus) {
    val (tone, label, detail) = when (status) {
        LocalModelStatus.Ready -> Triple(Palette.Gain, "READY", "On-device inference is available. No network call is made.")
        is LocalModelStatus.NotInstalled -> Triple(
            Palette.Caution,
            "NOT INSTALLED",
            status.note,
        )
        is LocalModelStatus.Failed -> Triple(Palette.Loss, "FAILED", status.reason)
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Local model", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tone)
        }
        Spacer(Modifier.height(6.dp))
        Text(model.displayName, style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        if (status is LocalModelStatus.NotInstalled) {
            Spacer(Modifier.height(6.dp))
            Text(
                status.expectedPath,
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Faint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "No cloud service and no API key. The only thing that touches the network is the read-only price feed.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
    }
}

@Composable
private fun ToolsCard(registry: ToolRegistry) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Agent tools", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Text("${registry.specs.size} tools", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        }
        Spacer(Modifier.height(6.dp))
        Text("What the local model is allowed to do", style = MaterialTheme.typography.titleMedium, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "The wording of a request cannot bend these rules — each tool enforces its own: reads " +
                "only read, bets wait for your confirmation, and the handoff only ever opens an official " +
                "book's own page.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "When a prop or side is omitted, the tool fills it from today's top pick, its price and the " +
                "1-unit paper stake. A value you supply always replaces the default.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        Spacer(Modifier.height(12.dp))
        registry.specs.forEach { spec -> ToolRow(spec) }
        Spacer(Modifier.height(8.dp))
        Text(
            "submit_live_wager always returns Blocked: none of the three books offers a public " +
                "betting API, and all three forbid automated betting.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
        )
    }
}

@Composable
private fun ToolRow(spec: ToolSpec) {
    val tone = when (spec.capability) {
        ToolCapability.READ -> Palette.Gain
        ToolCapability.ACT -> Palette.Over
        ToolCapability.STAGE -> Palette.Chalk
        ToolCapability.HANDOFF -> Palette.Over
        ToolCapability.SUBMIT_LIVE -> Palette.Loss
    }
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(6.dp).background(tone, CircleShape))
        Spacer(Modifier.width(9.dp))
        Column {
            Text(spec.name, style = MaterialTheme.typography.bodySmall, color = tone)
            Text(
                "${spec.capability.label} · ${spec.summary}",
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
            )
        }
    }
}

@Composable
private fun FormulaCard(report: FormulaReport) {
    val shape = RoundedCornerShape(18.dp)
    val tone = if (report.allPassed) Palette.Gain else Palette.Loss
    var showMath by remember(report.spec.id) { mutableStateOf(!AppSettings.current.easyMode) }
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(report.spec.title, style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
                Spacer(Modifier.height(3.dp))
                Text(
                    report.spec.symbols.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
                )
            }
            Text(
                "${report.passed}/${report.total}",
                style = MaterialTheme.typography.labelLarge, color = tone,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (report.spec.plain.isNotBlank()) {
            Text("IN PLAIN ENGLISH", style = MaterialTheme.typography.labelSmall, color = Palette.Gain)
            Spacer(Modifier.height(4.dp))
            Text(report.spec.plain, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
            if (report.spec.tryThis.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(report.spec.tryThis, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(report.spec.statement, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
        Spacer(Modifier.height(12.dp))
        Text(
            if (showMath) "Hide the math" else "Show the math",
            Modifier.clickable { showMath = !showMath },
            style = MaterialTheme.typography.labelMedium, color = Palette.Over,
        )
        if (showMath) {
            Spacer(Modifier.height(10.dp))
            Text("DERIVATION", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
            Spacer(Modifier.height(4.dp))
            report.spec.steps.forEach { StepRow(it) }
            Spacer(Modifier.height(12.dp))
            Text("CHECKS", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
            Spacer(Modifier.height(4.dp))
            report.results.forEach { CheckRow(it) }
        }
    }
}

@Composable
private fun StepRow(step: FormulaStep) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text(step.operation.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
        Text(step.from, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        Text("\u2192 ${step.to}", style = MaterialTheme.typography.bodySmall, color = Palette.Chalk)
    }
}

@Composable
private fun CheckRow(result: FormulaCheckResult) {
    val color = if (result.passed) Palette.Gain else Palette.Loss
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(9.dp))
        Text(
            result.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.passed) Palette.Chalk else color,
        )
    }
}

@Composable
private fun ProvenanceCard() {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(Palette.Court).border(1.dp, Palette.Hairline, shape).padding(16.dp),
    ) {
        Text("Provenance", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
        Spacer(Modifier.height(6.dp))
        Text(
            "Prompt layer adapted from Math X (MIT). Derivations and checks are native Kotlin, with an independent SymPy reference in research/mathx. The phone never calls Math X, Anthropic or any network.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
    }
}
