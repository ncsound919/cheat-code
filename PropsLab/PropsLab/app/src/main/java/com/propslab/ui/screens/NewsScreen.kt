package com.propslab.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.propslab.billing.Feature
import com.propslab.billing.TierStore
import com.propslab.fantasy.NewsFeed
import com.propslab.fantasy.NewsItem
import com.propslab.fantasy.SportsNews
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.components.PremiumGate
import com.propslab.ui.fmtAgo
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {

    var feed by mutableStateOf(NewsFeed())
        private set
    var league by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    val items: List<NewsItem>
        get() = feed.items.filter { league == null || it.league == league }

    fun select(next: String?) {
        league = next
    }

    fun refresh(force: Boolean = false) {
        if (!TierStore.allows(Feature.SPORTS_WIRE) || busy) return
        if (!force && feed.items.isNotEmpty()) return
        busy = true
        message = null
        viewModelScope.launch {
            val result = runCatching { SportsNews.fetch(nowUtc = System.currentTimeMillis()) }
            busy = false
            result.fold(
                onSuccess = {
                    feed = it
                    message = it.note
                },
                onFailure = {
                    message = "The wire could not be reached just now. Nothing on screen is stale " +
                        "in a way the app can fix by itself."
                },
            )
        }
    }
}

/**
 * General sports news: one headline per story, four free leagues, a link out.
 *
 * The screen is deliberately text-only. It does not download article images, mirror a story
 * or hide the publisher: every row names ESPN and opens ESPN's own page.
 */
@Composable
fun NewsScreen(onBack: () -> Unit, onOpenPremium: () -> Unit) {
    val vm: NewsViewModel = viewModel()
    val premium = TierStore.allows(Feature.SPORTS_WIRE)
    val context = LocalContext.current

    LaunchedEffect(premium) {
        if (premium) vm.refresh()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
            bottom = 56.dp,
        ),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "\u2039  Lab",
                    Modifier.clickable(role = Role.Button, onClick = onBack).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Sports wire",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.displayMedium, color = Palette.Chalk,
                    )
                    Badge("PREMIUM", Palette.Caution, filled = true)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "NFL, NBA, MLB and NHL headlines from ESPN's public news feed. One tap opens " +
                        "the story on ESPN; the app never mirrors it.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        if (!premium) {
            item {
                PremiumGate(
                    feature = Feature.SPORTS_WIRE,
                    title = "The day's sports wire, without the scroll",
                    body = "A single, league-filtered feed of general sports headlines with a short " +
                        "summary, how long ago it broke, and a link to the full story on ESPN.",
                    onOpenPremium = onOpenPremium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item { NewsFooter(feed = NewsFeed(), premium = false) }
            return@LazyColumn
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LeagueChip("All", vm.league == null) { vm.select(null) }
                SportsNews.GENERAL.forEach { league ->
                    LeagueChip(league.label, vm.league == league.label) { vm.select(league.label) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            Panel(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ESPN NEWS", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.weight(1f))
                    Badge(
                        when {
                            vm.busy -> "READING"
                            vm.feed.items.isNotEmpty() -> "UPDATED"
                            else -> "NOT READ"
                        },
                        when {
                            vm.busy -> Palette.Caution
                            vm.feed.items.isNotEmpty() -> Palette.Gain
                            else -> Palette.Faint
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (vm.feed.fetchedUtc > 0L) fmtAgo(vm.feed.fetchedUtc) else "Not read yet.",
                    style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                )
                val note = vm.message ?: vm.feed.note
                if (note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                }
                Spacer(Modifier.height(10.dp))
                ActionButton(
                    if (vm.busy) "Reading…" else "Refresh the wire",
                    { vm.refresh(force = true) },
                    enabled = !vm.busy,
                    tone = Palette.Over,
                )
            }
        }

        if (vm.items.isEmpty() && !vm.busy) {
            item {
                Panel(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        if (vm.feed.items.isEmpty()) {
                            "No headlines came back. The free feed can be quiet or rate-limited; " +
                                "refresh, and the note above will say which leagues answered."
                        } else {
                            "No ${vm.league.orEmpty()} headlines in this read. Try All, or refresh."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                    )
                }
            }
        }

        items(vm.items, key = { it.id + it.headline }) { item ->
            NewsCard(item) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }

        item { NewsFooter(feed = vm.feed, premium = true) }
    }
}

@Composable
private fun LeagueChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Palette.Court)) {
        ChoiceChip(label = label, selected = selected, onClick = onClick)
    }
}

@Composable
private fun NewsCard(item: NewsItem, onOpen: () -> Unit) {
    Panel(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(item.league, Palette.Over, filled = true)
            Spacer(Modifier.width(8.dp))
            Text(
                fmtAgo(item.publishedUtc),
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "ESPN",
                style = MaterialTheme.typography.labelSmall, color = Palette.Faint,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.headline,
            style = MaterialTheme.typography.titleMedium, color = Palette.Chalk,
        )
        if (item.summary.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                item.summary,
                style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (item.url.isBlank()) "No link was published" else "Read on ESPN \u2192",
            Modifier.clickable(enabled = item.url.isNotBlank(), role = Role.Button, onClick = onOpen)
                .padding(vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (item.url.isBlank()) Palette.Faint else Palette.Over,
        )
    }
}

@Composable
private fun NewsFooter(feed: NewsFeed, premium: Boolean) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text(
            "Headlines, summaries and links are ESPN's public data and remain ESPN's. The wire " +
                "shows the headline it read and sends you to the publisher; it does not republish " +
                "articles, and it is not a betting signal.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        if (premium && feed.items.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "The four leagues are read in parallel. A league that fails is named in the note " +
                    "instead of taking the other three down with it.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}
