package com.propslab.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.propslab.account.Account
import com.propslab.billing.Feature
import com.propslab.billing.Tier
import com.propslab.billing.TierStore
import com.propslab.brand.Brand
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Panel
import com.propslab.ui.theme.Palette

/**
 * Premium: what five dollars buys, how to pay, and how to unlock.
 *
 * The pricing is one plan with no tiers of tiers, and the free build is a real build — the
 * board, the picks, the ledger and the learning loop are not held back. Premium adds the
 * things that cost the owner money or are pure convenience: the ten-leg ticket, clean
 * cards, cloud inference and unlimited saved strategies.
 *
 * Payment runs through Stripe's hosted checkout, so no card ever touches this app.
 */
@Composable
fun PremiumScreen(onBack: () -> Unit, onOpenAccount: () -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    var showPaid by remember { mutableStateOf(false) }

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
                Text("Premium", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Brand.PRICE_LABEL} a ${Brand.PRICE_PERIOD}. Cancel any time.",
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                        modifier = Modifier.weight(1f),
                    )
                    Badge(
                        TierStore.tierLabel.uppercase(),
                        when (TierStore.tier) {
                            Tier.PREMIUM -> Palette.Gain
                            Tier.TRIAL -> Palette.Caution
                            Tier.FREE -> Palette.Dust
                        },
                        filled = true,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        item {
            Panel {
                Text("WHAT THIS BUILD DOES FREE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(8.dp))
                Text(
                    "The board, every pick, the game model, the parlay builder, the paper ledger and " +
                        "the self-learning loop. No account, no ads, no telemetry.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
                Spacer(Modifier.height(10.dp))
                Text("WHAT PREMIUM ADDS", style = MaterialTheme.typography.labelSmall, color = Palette.Over)
                Spacer(Modifier.height(8.dp))
                Feature.entries.forEach { feature ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text("\u2713", Modifier.width(22.dp), style = MaterialTheme.typography.titleSmall, color = Palette.Gain)
                        Column(Modifier.weight(1f)) {
                            Text(feature.label, style = MaterialTheme.typography.bodyMedium, color = Palette.Chalk)
                            Text("Free: ${feature.freeVersion}", style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Panel(tone = Palette.Ink) {
                Text("START", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(10.dp))
                when {
                    TierStore.tier == Tier.PREMIUM -> {
                        Text(
                            "Premium is active" + TierStore.licensedEmail?.let { " for $it" }.orEmpty() + ".",
                            style = MaterialTheme.typography.titleSmall, color = Palette.Gain,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Thank you for backing the build. Billing is handled by Stripe.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                    }
                    TierStore.tier == Tier.TRIAL -> {
                        Text(
                            "${TierStore.trialDaysLeft} days left in the trial.",
                            style = MaterialTheme.typography.titleSmall, color = Palette.Caution,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Everything in Premium is unlocked until the trial ends.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                        Spacer(Modifier.height(12.dp))
                        ActionButton("Continue to checkout", onClick = { open(context, TierStore.checkoutUrl) }, tone = Palette.Gain)
                    }
                    else -> {
                        ActionButton("Get Premium · ${Brand.PRICE_LABEL}/${Brand.PRICE_PERIOD}", onClick = {
                            open(context, TierStore.checkoutUrl)
                        }, tone = Palette.Gain)
                        Spacer(Modifier.height(8.dp))
                        ActionButton(
                            "Try everything for ${TierStore.TRIAL_DAYS} days",
                            onClick = {
                                note = if (TierStore.startTrial()) "Trial started. No card needed." else "A trial has already been used."
                            },
                            tone = Palette.Over,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Payments are handled by Stripe on the web. This app never sees a card number.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        if (TierStore.tier != Tier.PREMIUM) {
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ALREADY PAID?", style = MaterialTheme.typography.labelSmall, color = Palette.Faint, modifier = Modifier.weight(1f))
                        Text(
                            if (showPaid) "Hide" else "Enter code",
                            Modifier.clickable { showPaid = !showPaid }.padding(6.dp),
                            style = MaterialTheme.typography.labelSmall, color = Palette.Over,
                        )
                    }
                    if (showPaid) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Your code is on the page Stripe returned you to. Paste it here.",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                        )
                        Spacer(Modifier.height(10.dp))
                        TierField("Email used at checkout", email) { email = it }
                        Spacer(Modifier.height(8.dp))
                        TierField("Unlock code · PL5-…", code) { code = it }
                        Spacer(Modifier.height(10.dp))
                        ActionButton("Unlock Premium", onClick = {
                            when (val result = TierStore.unlock(code)) {
                                is TierStore.LicenseResult.Unlocked -> {
                                    TierStore.rememberEmail(email.ifBlank { "this phone" })
                                    note = "Premium unlocked on this phone."
                                    code = ""
                                }
                                is TierStore.LicenseResult.Invalid -> note = result.reason
                            }
                        }, tone = Palette.Gain)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Manage or cancel a subscription",
                            Modifier.clickable { open(context, Brand.BILLING_PORTAL) }.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        item {
            Panel {
                Text("ACCOUNT (OPTIONAL)", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (Account.isSignedIn) "Signed in as ${Account.signedInEmail}. Local data stays local."
                    else "Optional email verification. It does not upload your strategies; everything works signed out.",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                )
                Spacer(Modifier.height(10.dp))
                ActionButton(if (Account.isSignedIn) "Manage account" else "Sign in with email", onClick = onOpenAccount, tone = Palette.Over)
            }
            Spacer(Modifier.height(10.dp))
        }

        if (note != null) {
            item {
                Text(
                    note.orEmpty(),
                    Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                )
            }
        }

        item {
            Text(
                "Cheat Code is a paper-trading and analysis tool. It does not accept wagers, hold funds, " +
                    "or connect to a sportsbook account. If gambling stops being fun, call 1-800-522-4700.",
                Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun TierField(label: String, value: String, onChange: (String) -> Unit) {
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
                .background(Palette.Ink).padding(horizontal = 12.dp, vertical = 11.dp),
        )
    }
}

private fun open(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
