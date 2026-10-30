package com.propslab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.propslab.PropsLabApp
import com.propslab.compute.ComputeSettings
import com.propslab.compute.KaggleClient
import com.propslab.data.AvailabilitySignalEntity
import com.propslab.data.AvailabilityStatus
import com.propslab.data.FeedStateEntity
import com.propslab.data.PropsRepository
import com.propslab.data.SignalSource
import com.propslab.domain.Markets
import com.propslab.feed.Bookmakers
import com.propslab.feed.FeedConfig
import com.propslab.feed.FeedSettings
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.KeyValue
import com.propslab.ui.components.Panel
import com.propslab.ui.fmtAgo
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedScreenState(
    val oddsKey: String = "",
    val resultsKey: String = "",
    val newsKey: String = "",
    val bookmakers: List<String> = listOf(Bookmakers.DRAFTKINGS),
    val markets: List<String> = listOf(Markets.POINTS, Markets.REBOUNDS, Markets.ASSISTS),
    /** Read the moneyline, spread and total as well as the props. */
    val gameLines: Boolean = true,
    val syncing: Boolean = false,
    val message: String? = null,
    /** The manual late-signal form. */
    val signalPlayer: String = "",
    val signalStatus: String = AvailabilityStatus.QUESTIONABLE,
    val signalDetail: String = "",
    /** The high-compute lane. The key is never read back into the field. */
    val kaggleUser: String = "",
    val kaggleKey: String = "",
    val kaggleSaved: Boolean = false,
    val kaggleBusy: Boolean = false,
)

class FeedViewModel(
    private val repo: PropsRepository,
    private val settings: FeedSettings,
    private val compute: ComputeSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(loadScreenState())
    val state: StateFlow<FeedScreenState> = _state.asStateFlow()

    val feedState: StateFlow<FeedStateEntity?> = repo.feedState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun loadScreenState(): FeedScreenState {
        val config = settings.load()
        val kaggle = compute.credentials()
        return FeedScreenState(
            oddsKey = config.oddsKey,
            resultsKey = config.resultsKey,
            newsKey = config.newsKey,
            bookmakers = config.quotedBookmakers(),
            markets = config.pricedMarkets(),
            gameLines = config.gameLines,
            kaggleUser = kaggle.username,
            kaggleSaved = kaggle.ready,
        )
    }

    fun setKaggleUser(value: String) = _state.update { it.copy(kaggleUser = value, message = null) }

    fun setKaggleKey(value: String) = _state.update { it.copy(kaggleKey = value, message = null) }

    /**
     * An empty key field keeps the key already saved: a credential is written once and never
     * echoed back, so "save" with no new key must not wipe it.
     */
    fun saveKaggle() {
        val current = _state.value
        val existing = compute.credentials()
        val key = current.kaggleKey.trim().ifBlank { existing.key }
        if (current.kaggleUser.isBlank() || key.isBlank()) {
            _state.update { it.copy(message = "A Kaggle username and API key are both needed.") }
            return
        }
        compute.save(KaggleClient.Credentials(current.kaggleUser.trim(), key))
        _state.update {
            it.copy(
                kaggleKey = "",
                kaggleSaved = true,
                message = "Kaggle account saved on this device. Nothing was sent anywhere.",
            )
        }
    }

    /** Asks the account what it has run. This is the only thing the lane does on its own. */
    fun checkKaggle() {
        if (_state.value.kaggleBusy) return
        viewModelScope.launch {
            _state.update { it.copy(kaggleBusy = true, message = null) }
            val report = runCatching { repo.kaggleExperiment("") }.getOrElse { it.message ?: "The account could not be read." }
            _state.update { it.copy(kaggleBusy = false, message = report) }
        }
    }

    val signals: StateFlow<List<AvailabilitySignalEntity>> = repo.signals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSignalPlayer(value: String) = _state.update { it.copy(signalPlayer = value, message = null) }

    fun setSignalStatus(value: String) = _state.update { it.copy(signalStatus = value) }

    fun setSignalDetail(value: String) = _state.update { it.copy(signalDetail = value) }

    fun recordSignal() {
        val current = _state.value
        if (current.signalPlayer.isBlank()) {
            _state.update { it.copy(message = "Name the player before recording a signal.") }
            return
        }
        viewModelScope.launch {
            val ok = runCatching {
                repo.recordSignal(current.signalPlayer, current.signalStatus, current.signalDetail)
            }.getOrDefault(false)
            _state.update {
                it.copy(
                    message = if (ok) {
                        "${AvailabilityStatus.label(current.signalStatus)} recorded for ${current.signalPlayer.trim()}. " +
                            "The next sync scores this player with it."
                    } else {
                        "That signal could not be recorded."
                    },
                    signalPlayer = if (ok) "" else it.signalPlayer,
                    signalDetail = if (ok) "" else it.signalDetail,
                )
            }
        }
    }

    fun expireSignals() {
        viewModelScope.launch {
            val cleared = runCatching { repo.expireSignals() }.getOrDefault(0)
            _state.update { it.copy(message = "$cleared signals expired.") }
        }
    }

    fun setOddsKey(value: String) = _state.update { it.copy(oddsKey = value, message = null) }

    fun setResultsKey(value: String) = _state.update { it.copy(resultsKey = value, message = null) }

    fun setNewsKey(value: String) = _state.update { it.copy(newsKey = value, message = null) }

    fun toggleBookmaker(key: String) = _state.update { current ->
        val next = current.bookmakers.toggle(key)
        current.copy(bookmakers = next.ifEmpty { current.bookmakers }, message = null)
    }

    fun toggleMarket(key: String) = _state.update { current ->
        val next = current.markets.toggle(key)
        current.copy(markets = next.ifEmpty { current.markets }, message = null)
    }

    /**
     * Written straight through, unlike the key fields: this switch decides what the next
     * sync spends credits on, so a person who flips it and taps Sync must get what they
     * asked for without also remembering to save.
     */
    fun toggleGameLines() {
        _state.update { it.copy(gameLines = !it.gameLines, message = null) }
        settings.save(settings.load().copy(gameLines = _state.value.gameLines))
    }

    fun save() {
        val current = _state.value
        // Load-then-copy: a FeedConfig also carries the board's numeric knobs (lookahead,
        // request caps, news depth). Rebuilding it from this form alone silently reset those
        // to defaults on every save, which is why the settings screen edits them separately.
        settings.save(
            settings.load().copy(
                oddsKey = current.oddsKey,
                resultsKey = current.resultsKey,
                newsKey = current.newsKey,
                bookmakers = current.bookmakers,
                markets = current.markets,
                gameLines = current.gameLines,
            ),
        )
        _state.update { it.copy(message = "Saved on this device. Nothing was sent anywhere.") }
    }

    fun sync() {
        if (_state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, message = null) }
            // Failures carry their reason to the screen. \u201cIt failed\u201d is not a report; a
            // wrong key, a missing tier and a dead network all look identical without this.
            var reportError: String? = null
            val report = runCatching { repo.refreshFeed() }
                .onFailure { reportError = it.message ?: it::class.java.simpleName }
                .getOrNull()
            var bookError: String? = null
            val book = runCatching { repo.refreshLiveBook() }
                .onFailure { bookError = it.message ?: it::class.java.simpleName }
                .getOrNull()
            val message = if (report == null) {
                "The sync failed: ${reportError ?: "no detail was reported"}"
            } else buildString {
                append(report.schedule.note)
                append(' ')
                append(report.results.note)
                append(' ')
                append(report.props.note)
                if (book != null && (book.placed > 0 || book.settled > 0)) {
                    append(" ${book.placed} picks taken, ${book.settled} settled on the live book.")
                }
                book?.retiredId?.let { append(" $it was retired on its real record.") }
                if (book == null && bookError != null) append(" Live book skipped: $bookError.")
            }
            _state.update { it.copy(syncing = false, message = message) }
        }
    }

    private fun List<String>.toggle(key: String): List<String> =
        if (contains(key)) filter { it != key } else this + key
}

/**
 * The Live data screen: the two keys, what they buy, and what the last sync actually did.
 *
 * It is deliberately explicit about cost and entitlement, because both providers have a
 * free plan that does not include everything this screen asks for, and a silent empty board
 * is worse than a sentence saying which tier is missing.
 */
@Composable
fun FeedScreen(onBack: () -> Unit) {
    val repo = rememberRepo()
    val context = LocalContext.current
    val app = context.applicationContext as PropsLabApp
    val vm: FeedViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FeedViewModel(repo, FeedSettings(context), app.computeSettings) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val feed by vm.feedState.collectAsStateWithLifecycle()
    val signals by vm.signals.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 48.dp,
        ),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Live data",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Faint,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onBack),
                )
                Spacer(Modifier.height(8.dp))
                Text("The feed", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Real prop prices and real game stats, so the graduated strategy is measured on " +
                        "something other than a simulation. Two read-only sources, no bets, nothing " +
                        "written back to either provider.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(18.dp))
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) { StatusPanel(feed) }
            Spacer(Modifier.height(18.dp))
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Panel {
                    Text("KEYS", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.height(8.dp))
                    Text("Odds provider", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Player prop prices for NBA games." +
                            (feed?.creditsRemaining?.let { " $it credits remain." } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    Spacer(Modifier.height(8.dp))
                    KeyField(state.oddsKey, vm::setOddsKey, "Paste the API key")
                    Spacer(Modifier.height(16.dp))
                    Text("Results provider", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Per-player box scores. Player stats need a paid tier on that side; a key " +
                            "without it answers with an error rather than empty data.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    Spacer(Modifier.height(8.dp))
                    KeyField(state.resultsKey, vm::setResultsKey, "Paste the API key")
                    Spacer(Modifier.height(16.dp))
                    Text("News search (optional)", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Headline search for late injury news. A headline only becomes a signal when " +
                            "it names the player and states a status, and a report you enter by hand " +
                            "always wins over anything this finds.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    Spacer(Modifier.height(8.dp))
                    KeyField(state.newsKey, vm::setNewsKey, "Paste the API key")
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Keys stay in this app's private storage on this phone. They are never " +
                            "written to the database, the audit trail or a log line.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                    )
                    Spacer(Modifier.height(14.dp))
                    ActionButton("Save keys", { vm.save() }, tone = Palette.Chalk)
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) { ComputeCard(state, vm) }
            Spacer(Modifier.height(18.dp))
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Panel {
                    Text("BOOKS", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Bookmakers.ALL.forEachIndexed { index, book ->
                            if (index > 0) Spacer(Modifier.width(6.dp))
                            Box(Modifier.weight(1f)) {
                                ChoiceChip(
                                    book.title,
                                    selected = book.key in state.bookmakers,
                                    onClick = { vm.toggleBookmaker(book.key) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Bookmakers.ALL.filter { it.key in state.bookmakers }.forEach { book ->
                        Text(
                            "${book.title}: ${book.note.lowercase().replaceFirstChar { it.uppercase() }}.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("MARKETS", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.height(8.dp))
                    val labels = listOf(
                        Markets.POINTS to "Points",
                        Markets.REBOUNDS to "Rebounds",
                        Markets.ASSISTS to "Assists",
                        Markets.THREES to "Threes",
                        Markets.PRA to "PRA",
                    )
                    labels.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            row.forEachIndexed { index, (key, label) ->
                                if (index > 0) Spacer(Modifier.width(6.dp))
                                Box(Modifier.weight(1f)) {
                                    ChoiceChip(
                                        label,
                                        selected = key in state.markets,
                                        onClick = { vm.toggleMarket(key) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("GAME LINES", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.height(8.dp))
                    ChoiceChip(
                        "Moneyline, spread & total",
                        selected = state.gameLines,
                        onClick = { vm.toggleGameLines() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Three extra markets per game, so three extra credits. Refreshed at most every three hours.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                    )
                    Spacer(Modifier.height(14.dp))
                    ActionButton(
                        if (state.syncing) "Syncing…" else "Sync now",
                        { vm.sync() },
                        enabled = !state.syncing,
                        tone = Palette.Over,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) { SignalsCard(state, signals, vm) }
            Spacer(Modifier.height(18.dp))
        }

        state.message?.let { message ->
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Panel(tone = Palette.Raised) {
                        Text(message, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Panel {
                    Text("What this does and does not do", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Prices are read, never written. A book that quotes only one side of a prop is " +
                            "dropped, and when two books quote the same line the pair with the smaller " +
                            "built-in margin is the one kept, so the estimate is never flattered by a " +
                            "made-up price.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "An estimate is the player's own recent games read as a distribution, and it is " +
                            "published only once at least eight games are on file. Below that the row " +
                            "keeps its real line and says why it has no estimate.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A strategy that graduated in practice still has to hold up here: once 150 of " +
                            "its real picks have finished, a losing record retires it. Synthetic results " +
                            "are never counted toward that.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No bet is ever placed. Neither provider accepts one, and all three books " +
                            "forbid automated play, so the live book stops at a ledger.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                    )
                }
            }
        }
    }
}

/**
 * The high-compute lane.
 *
 * The phone does not train models, so this card only sets up the account that does. It can
 * list the kernels that have run and ask their status; pushing a new kernel is deliberately
 * not wired to a text field on a phone.
 */
@Composable
private fun ComputeCard(state: FeedScreenState, vm: FeedViewModel) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("HIGH-COMPUTE LANE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Badge(if (state.kaggleSaved) "ACCOUNT SAVED" else "NO ACCOUNT", if (state.kaggleSaved) Palette.Gain else Palette.Faint)
        }
        Spacer(Modifier.height(8.dp))
        Text("Kaggle", style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
        Spacer(Modifier.height(4.dp))
        Text(
            "Where a real model gets trained. With an account saved, the console can list your " +
                "kernels and ask what state the latest run is in. Nothing is downloaded and no " +
                "notebook is pushed from here.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
        )
        Spacer(Modifier.height(10.dp))
        KeyField(state.kaggleUser, vm::setKaggleUser, "Kaggle username")
        Spacer(Modifier.height(8.dp))
        KeyField(
            state.kaggleKey,
            vm::setKaggleKey,
            if (state.kaggleSaved) "Saved · paste a new key to replace it" else "Kaggle API key",
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionButton(
                if (state.kaggleBusy) "Reading…" else "Check account",
                { vm.checkKaggle() },
                enabled = !state.kaggleBusy,
                tone = Palette.Over,
            )
            Spacer(Modifier.width(10.dp))
            ActionButton("Save account", { vm.saveKaggle() }, enabled = !state.kaggleBusy, tone = Palette.Chalk)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "The key is written to this app's private storage and is never read back onto the screen.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
    }
}

@Composable
private fun StatusPanel(feed: FeedStateEntity?) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LAST SYNC", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            val ok = feed != null && (feed.lastPropsSyncUtc > 0L || feed.lastResultsSyncUtc > 0L)
            Badge(
                when {
                    feed == null -> "NOT CONFIGURED"
                    ok -> "RUNNING"
                    else -> "WAITING FOR KEYS"
                },
                when {
                    feed == null -> Palette.Faint
                    ok -> Palette.Gain
                    else -> Palette.Caution
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        val state = feed ?: FeedStateEntity.EMPTY
        KeyValue("Prices", if (state.lastPropsSyncUtc > 0) fmtAgo(state.lastPropsSyncUtc) else "never")
        KeyValue("Box scores", if (state.lastResultsSyncUtc > 0) fmtAgo(state.lastResultsSyncUtc) else "never")
        KeyValue("Quotes on the board", "${state.quotesWritten}")
        KeyValue("Priced by the model", "${state.predictionsScored}")
        KeyValue("Results recorded", "${state.resultsRecorded}")
        state.creditsRemaining?.let { KeyValue("Credits left", "$it") }
        Spacer(Modifier.height(8.dp))
        Text(state.propsNote, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        Spacer(Modifier.height(4.dp))
        Text(state.resultNote, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        Spacer(Modifier.height(4.dp))
        Text(state.signalNote, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        if (state.fixturesRetired) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Real quotes have replaced the synthetic fixtures. The invented props are off the " +
                    "board; paper bets placed against them keep their own record.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun KeyField(value: String, onChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        placeholder = { Text(hint, style = MaterialTheme.typography.bodySmall, color = Palette.Faint) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Hairline,
            unfocusedBorderColor = Palette.Hairline,
            focusedTextColor = Palette.Chalk,
            unfocusedTextColor = Palette.Chalk,
            cursorColor = Palette.Over,
        ),
    )
}
