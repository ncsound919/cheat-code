package com.propslab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.propslab.ui.theme.Palette

/** One term the app uses, and what it means in ordinary words. */
data class PlainEntry(val term: String, val plain: String)

/** A group of terms, ordered the way a new user runs into them. */
data class PlainSection(val title: String, val entries: List<PlainEntry>)

/**
 * The whole vocabulary of the app, translated.
 *
 * The terms stay in the interface because they are precise and the people who read them
 * expect them; this page exists so that never becomes a barrier. Nothing here is a
 * setting or a hidden feature — it is only what the words mean.
 */
val Glossary: List<PlainSection> = listOf(
    PlainSection(
        "THE BOARD",
        listOf(
            PlainEntry(
                "Prop",
                "One number on one player for one game — \"Jokic over 27.5 points\". " +
                    "You are guessing whether the final total beats that number.",
            ),
            PlainEntry(
                "Line",
                "The number in the middle. Over 27.5 means you need 28 or more; " +
                    "under 27.5 means 27 or fewer.",
            ),
            PlainEntry(
                "Over / Under",
                "Over wins if the player finishes above the line, under wins if they finish below it. " +
                    "On a whole number like 27 the player can land exactly on it, which is a push.",
            ),
            PlainEntry(
                "Push",
                "The player lands exactly on the line, so nobody wins and your stake comes straight back. " +
                    "It can only happen on whole-number lines.",
            ),
            PlainEntry(
                "Odds",
                "The price of the bet. +150 means a 1-unit bet wins 1.50 in profit; " +
                    "−130 means you risk 1.30 to win 1.",
            ),
            PlainEntry(
                "Model / estimator",
                "The app's own probability for each side, worked out on your phone from stats. " +
                    "It is not a tipster, and it never calls out to a service to get it.",
            ),
            PlainEntry(
                "Lean",
                "Which side the app's number prefers when it disagrees with what the book is offering.",
            ),
        ),
    ),
    PlainSection(
        "THE NUMBERS ON A PICK",
        listOf(
            PlainEntry(
                "Probability",
                "How likely a side is to hit, as a percentage, according to the app's own model.",
            ),
            PlainEntry(
                "Market / book",
                "What the sportsbook is charging. In Cheat Code it is only ever read from a feed — " +
                    "the app never places a bet or moves money.",
            ),
            PlainEntry(
                "No vig / fair price",
                "The book's price with its built-in margin stripped out, so you see what it would " +
                    "charge if it made nothing on the bet. \"Fair\" means this price has zero value either way.",
            ),
            PlainEntry(
                "Book's cut (hold)",
                "The margin baked into both sides of a price, so the book wins a little no matter the result. " +
                    "Around 5% is normal; noticeably higher means a worse deal for you.",
            ),
            PlainEntry(
                "Edge",
                "The gap, in percentage points, between the app's probability and the book's no-margin " +
                    "probability. A bigger gap means a bigger disagreement — and a bigger chance of value.",
            ),
            PlainEntry(
                "Value (EV)",
                "The average profit of a 1-unit bet at this price, if you placed the same bet thousands " +
                    "of times. Positive means the price is cheap compared to what the app believes.",
            ),
            PlainEntry(
                "Break-even",
                "The win rate you need just to stop losing at this price. The app's estimate has to be " +
                    "higher than it for a bet to be worth taking.",
            ),
            PlainEntry(
                "Kelly (suggested stake)",
                "A staking formula that sizes each bet from its edge: a strong edge gets a larger stake, " +
                    "a weak one a smaller stake. The percentage is the share of bankroll it suggests.",
            ),
            PlainEntry(
                "Drivers",
                "The inputs that moved the estimate up or down — matchup, rest, pace and so on — " +
                    "each shown in points.",
            ),
        ),
    ),
    PlainSection(
        "TODAY'S BEST BETS",
        listOf(
            PlainEntry(
                "Pick / best bet",
                "A bet the app's top strategy actually wants today, ranked by how far its number sits " +
                    "from the book's price.",
            ),
            PlainEntry(
                "Unit",
                "Your standard stake size. Everything is counted in units so results stay comparable " +
                    "whatever the dollar amount is.",
            ),
            PlainEntry(
                "Flat",
                "The same stake on every bet, rather than varying it from bet to bet.",
            ),
            PlainEntry(
                "Shadow pick",
                "A try-out pick from a strategy that has not graduated yet. It is shown for review and " +
                    "is never part of the live list.",
            ),
            PlainEntry(
                "Below bar",
                "A pick from the graduated strategy that is too thin to bother with today.",
            ),
            PlainEntry(
                "Round",
                "One full practice cycle: a fresh set of simulated games, every strategy scored, " +
                    "the rankings updated. It runs on its own every six hours.",
            ),
        ),
    ),
    PlainSection(
        "THE SANDBOX",
        listOf(
            PlainEntry(
                "Sandbox",
                "A practice league that runs on play money and invented games, where strategies compete " +
                    "before any of them is allowed near the real board.",
            ),
            PlainEntry(
                "Fake bankroll",
                "Play money the strategies bet with, starting at 100 units. It exists only so they can " +
                    "be measured against each other.",
            ),
            PlainEntry(
                "Simulated season",
                "A whole season of invented games, generated fresh each round, so strategies are tested " +
                    "on games they have never seen before.",
            ),
            PlainEntry(
                "The gate (promotion)",
                "The graduation test. A strategy must clear every threshold — enough bets, a return above " +
                    "1.5%, a luck check above 2.0, a shallow dip, and a win rate above break-even — before " +
                    "it is allowed to pick on the real board.",
            ),
            PlainEntry(
                "t-stat (luck check)",
                "How likely the profit is to be pure luck. Above 2.0 means a run this good would happen " +
                    "by chance less than about one time in twenty.",
            ),
            PlainEntry(
                "Drawdown / dip",
                "The worst losing streak you would have sat through: the largest fall from a peak to a low, " +
                    "as a share of the bankroll.",
            ),
            PlainEntry(
                "Hit rate",
                "The share of finished bets that won.",
            ),
            PlainEntry(
                "ROI / return",
                "Profit divided by everything staked, as a percent. +4.2% means 4.2 units of profit for " +
                    "every 100 units risked.",
            ),
            PlainEntry(
                "Live book / promoted",
                "The one strategy currently allowed to pick the real board — what the screens call the " +
                    "live list. Reaching it means graduating, and everything else stays in practice.",
            ),
            PlainEntry(
                "Retired",
                "A strategy that was live and has been pulled back into practice.",
            ),
            PlainEntry(
                "Autonomous loop",
                "The scheduler that runs rounds, price refreshes and news checks on its own, so the app " +
                    "keeps working without you touching it.",
            ),
        ),
    ),
    PlainSection(
        "YOUR LEDGER",
        listOf(
            PlainEntry(
                "Paper bet",
                "A practice bet written into the app. No book is contacted and no money moves — " +
                    "it exists to keep score.",
            ),
            PlainEntry(
                "Stake",
                "How much of the bankroll goes on one bet, measured in units.",
            ),
            PlainEntry(
                "Settle",
                "Recording the final result so the bet starts counting toward profit and loss.",
            ),
            PlainEntry(
                "Win / loss / push",
                "Win pays out at the listed odds, loss costs the stake, push lands exactly on the line " +
                    "and returns the stake untouched.",
            ),
        ),
    ),
    PlainSection(
        "THE SYSTEM",
        listOf(
            PlainEntry(
                "Console",
                "A chat box that both talks and acts. An instruction it recognises runs a real function; " +
                    "anything else is answered by the model on your phone.",
            ),
            PlainEntry(
                "ran / said",
                "\"ran\" means the app actually did the thing. \"said\" means the model only answered in " +
                    "words. The difference is always shown so nothing is mistaken for action.",
            ),
            PlainEntry(
                "Local model",
                "The language model running entirely on your phone. No cloud, no API key, and nothing " +
                    "you type leaves the device.",
            ),
            PlainEntry(
                "Screenshot import",
                "Point it at a photo of a bet slip: on-device text recognition reads it, and only what is " +
                    "clearly legible is used. The image is never uploaded.",
            ),
            PlainEntry(
                "Feed / keys",
                "Two read-only connections — one for prices, one for final game stats. They can only " +
                    "read; they can never write or spend anything.",
            ),
            PlainEntry(
                "Fixture",
                "Invented test data used before real quotes arrive. It is always labelled so it can never " +
                    "be mistaken for a real price.",
            ),
            PlainEntry(
                "Provenance",
                "Where a number came from: which model version, how fresh the data, which book, and when " +
                    "it was recorded.",
            ),
            PlainEntry(
                "Kaggle",
                "An optional lane for running large experiments in the cloud. With no account saved the " +
                    "app says so plainly instead of pretending it worked.",
            ),
            PlainEntry(
                "Handoff",
                "Opens the official page of one of three named books. The app never places a bet for you, " +
                    "and the link only ever goes to a book's own site.",
            ),
        ),
    ),
)

/**
 * The glossary itself: one screen, plain words, no settings.
 *
 * It is reachable from the header of every main screen, because the moment a reader
 * stalls on a word is the moment they should be one tap from the answer.
 */
@Composable
fun PlainEnglishScreen(onBack: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 120.dp,
        ),
    ) {
        item {
            Text(
                "‹ Back",
                Modifier.clickable(onClick = onBack).padding(horizontal = 20.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge, color = Palette.Dust,
            )
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Plain English", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Every word this app uses, said the way you would say it out loud. " +
                        "The terms stay on the screens because they are precise — this page is " +
                        "here so they never stop you.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
            }
        }
        items(Glossary, key = { it.title }) { section ->
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(28.dp))
                Text(section.title, style = MaterialTheme.typography.labelSmall, color = Palette.Over)
                Spacer(Modifier.height(6.dp))
                section.entries.forEachIndexed { i, entry ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Palette.Hairline)
                    PlainEntryRow(entry)
                }
            }
        }
        item {
            Text(
                "Nothing on this page changes a number. It only explains one.",
                Modifier.padding(horizontal = 20.dp, vertical = 30.dp),
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun PlainEntryRow(entry: PlainEntry) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.padding(top = 6.dp).size(5.dp)
                .background(Palette.Hairline, RoundedCornerShape(50)),
        )
        Spacer(Modifier.width(11.dp))
        Column {
            Text(entry.term, style = MaterialTheme.typography.titleSmall, color = Palette.Chalk)
            Spacer(Modifier.height(3.dp))
            Text(entry.plain, style = MaterialTheme.typography.bodySmall, color = Palette.Dust)
        }
    }
}
