package com.propslab.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.propslab.PropsLabApp
import com.propslab.billing.TierStore
import com.propslab.data.BestBetsUi
import com.propslab.data.BestPickUi
import com.propslab.data.BetUi
import com.propslab.domain.Markets
import com.propslab.domain.Outcome
import com.propslab.domain.Side
import com.propslab.flyer.FlyerContent
import com.propslab.flyer.FlyerRenderer
import com.propslab.flyer.FlyerRow
import com.propslab.flyer.FlyerShare
import com.propslab.flyer.FlyerTemplate
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.ChoiceChip
import com.propslab.ui.components.Panel
import com.propslab.ui.fmtOdds
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Share cards drawn from the real board and the real ledger.
 *
 * Nothing here is decorative fiction: each template reads a flow the app already shows.
 * A card carries the same numbers as the screen it came from, plus the date, because a
 * screenshot of a pick is worthless a day later and a card that says when it was made is not.
 */
@Composable
fun FlyerScreen(onBack: () -> Unit, onOpenPremium: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PropsLabApp
    val best by app.repo.bestBets().collectAsStateWithLifecycle(BestBetsUi())
    val bets by app.repo.bets().collectAsStateWithLifecycle(emptyList())
    var template by remember { mutableStateOf(FlyerTemplate.BET_OF_DAY) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var note by remember { mutableStateOf<String?>(null) }

    val content = remember(template, best, bets) { buildFlyer(template, best, bets) }

    LaunchedEffect(content) {
        bitmap = withContext(Dispatchers.Default) {
            runCatching { FlyerRenderer.render(context, content) }.getOrNull()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
            bottom = 48.dp,
        ),
    ) {
        item {
            Text(
                "\u2039  Back",
                Modifier.clickable(onClick = onBack).padding(horizontal = 20.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
            )
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Share a card", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(6.dp))
                Text(
                    "A clean 1080 × 1350 post, straight from today's numbers.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlyerTemplate.entries.take(2).forEach { entry ->
                        ChoiceChip(
                            entry.kicker.lowercase().replaceFirstChar { it.uppercase() },
                            entry == template,
                            onClick = { template = entry },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlyerTemplate.entries.drop(2).forEach { entry ->
                        ChoiceChip(
                            entry.kicker.lowercase().replaceFirstChar { it.uppercase() },
                            entry == template,
                            onClick = { template = entry },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        item {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Preview of the ${template.kicker.lowercase()} card",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
            } else {
                Panel {
                    Text("Drawing the card…", style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("Share", enabled = bitmap != null, onClick = {
                        val image = bitmap ?: return@ActionButton
                        note = if (FlyerShare.share(context, image, fileName(template))) null else "No app accepted the image."
                    }, tone = Palette.Over)
                    ActionButton("Save to photos", enabled = bitmap != null, onClick = {
                        val image = bitmap ?: return@ActionButton
                        note = if (FlyerShare.saveToGallery(context, image, fileName(template))) "Saved to Pictures/Cheat Code."
                        else "The image could not be saved."
                    })
                }
                if (!TierStore.isPremium) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Free cards carry a small Cheat Code mark. Premium removes it.",
                        Modifier.clickable(onClick = onOpenPremium),
                        style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                    )
                }
                if (note != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        item {
            Text(
                "Bet slips and results are paper. A card is a record of what the model said, not advice.",
                Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

/** Pure content assembly, kept out of composition so it can be reasoned about on its own. */
fun buildFlyer(template: FlyerTemplate, best: BestBetsUi, bets: List<BetUi>): FlyerContent {
    val today = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US).format(LocalDate.now())
    val watermark = !TierStore.isPremium
    return when (template) {
        FlyerTemplate.BET_OF_DAY -> {
            val rows = best.picks.take(3).map { it.toRow() }.ifEmpty {
                best.gamePicks.take(3).map {
                    FlyerRow(it.selection, "${it.matchup} · ${it.book}", fmtOdds(it.odds))
                }
            }
            val top = best.picks.firstOrNull()?.modelProb
            FlyerContent(
                template = template,
                rows = rows.ifEmpty { listOf(FlyerRow("No picks yet", "Open Today to load the board", "")) },
                metricValue = top?.let { pct(it) } ?: "—",
                metricCaption = if (top != null) "on the day's cleanest edge" else "the board is empty",
                footer = best.strategyLabel ?: "Cheat Code board",
                dateLine = today,
                watermark = watermark,
            )
        }
        FlyerTemplate.MY_BET -> {
            val open = bets.filter { it.isOpen }.take(3)
            val shown = open.ifEmpty { bets.take(3) }
            val atRisk = shown.sumOf { it.stake }
            FlyerContent(
                template = template,
                rows = shown.map { bet ->
                    FlyerRow(
                        "${bet.playerName} ${bet.side.label} ${line(bet.line)} ${Markets.label(bet.market)}",
                        "${bet.teamCode} · ${bet.book.ifBlank { "best price" }}",
                        fmtOdds(bet.odds),
                    )
                }.ifEmpty { listOf(FlyerRow("No bets logged", "Place a paper bet from Best", "")) },
                metricValue = "%.2f".format(atRisk),
                metricCaption = "units at risk in ${shown.size} paper bets",
                footer = "Paper ledger · Cheat Code",
                dateLine = today,
                watermark = watermark,
            )
        }
        FlyerTemplate.WIN -> {
            val won = bets.filter { it.outcome == Outcome.WIN }.take(4)
            val profit = won.sumOf { it.profit ?: 0.0 }
            FlyerContent(
                template = template,
                rows = won.map { bet ->
                    FlyerRow(
                        "${bet.playerName} ${bet.side.label} ${line(bet.line)}",
                        "${Markets.label(bet.market)} · ${bet.teamCode}",
                        "+%.2f".format(bet.profit ?: 0.0),
                    )
                }.ifEmpty { listOf(FlyerRow("Nothing settled yet", "Wins appear here once results land", "")) },
                metricValue = if (won.isEmpty()) "—" else "+%.2f".format(profit),
                metricCaption = if (won.isEmpty()) "no settled wins on this phone" else "units of paper profit",
                footer = "Settled on real box scores",
                dateLine = today,
                watermark = watermark,
            )
        }
        FlyerTemplate.STRATEGY -> FlyerContent(
            template = template,
            rows = best.picks.take(3).map { FlyerRow(it.prop.playerName, it.prop.matchup, pct(it.modelProb)) }
                .ifEmpty { listOf(FlyerRow("No live strategy", "Run a sandbox round in the Lab", "")) },
            metricValue = best.strategyLabel?.let { "${best.round}" } ?: "—",
            metricCaption = if (best.live) "live formula, round ${best.round}" else "the sandbox is still choosing",
            footer = best.strategyLabel ?: "Formula lab",
            dateLine = today,
            watermark = watermark,
        )
    }
}

private fun BestPickUi.toRow(): FlyerRow = FlyerRow(
    primary = "${prop.playerName} ${side.label} ${line(prop.line)}",
    secondary = "${Markets.label(prop.market)} · ${prop.matchup} · ${prop.book.ifBlank { "best price" }}",
    price = fmtOdds(odds),
)

private fun line(value: Double?): String {
    if (value == null) return ""
    return if (abs(value - Math.rint(value)) < 1e-9) "%.0f".format(value) else "%.1f".format(value)
}

private fun pct(value: Double): String =
    if (value >= 0.995) "99%" else "${(value * 100).toInt()}%"

private fun fileName(template: FlyerTemplate): String =
    "propslab-${template.name.lowercase()}-${System.currentTimeMillis() / 1000}"

private val Side.label: String get() = if (this == Side.OVER) "Over" else "Under"
