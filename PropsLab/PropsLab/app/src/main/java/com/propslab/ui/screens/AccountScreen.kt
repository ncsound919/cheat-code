package com.propslab.ui.screens

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.propslab.account.Account
import com.propslab.ui.components.ActionButton
import com.propslab.ui.components.Badge
import com.propslab.ui.components.Panel
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * Optional email verification.
 *
 * Supabase's GoTrue server is open source, and the check is a passwordless email code.
 * Nothing in the app requires it and nothing is uploaded: the verified address stays on
 * this phone, and every feature works without an account.
 */
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Account.projectUrl) }
    var anon by remember { mutableStateOf(Account.anonKey) }
    var email by remember { mutableStateOf(Account.signedInEmail.orEmpty()) }
    var code by remember { mutableStateOf("") }
    var showSetup by remember { mutableStateOf(!Account.isConfigured) }
    var sent by remember { mutableStateOf(false) }

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
                Text("Account", style = MaterialTheme.typography.displayMedium, color = Palette.Chalk)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Optional. This release verifies your email on this phone; it does not upload your strategies or ledger.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.Dust,
                )
                Spacer(Modifier.height(18.dp))
            }
        }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("STATUS", style = MaterialTheme.typography.labelSmall, color = Palette.Faint, modifier = Modifier.weight(1f))
                    Badge(
                        if (Account.isSignedIn) "SIGNED IN" else "SIGNED OUT",
                        if (Account.isSignedIn) Palette.Gain else Palette.Dust,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    Account.signedInEmail ?: "No account on this phone.",
                    style = MaterialTheme.typography.titleSmall, color = Palette.Chalk,
                )
                if (Account.isSignedIn) {
                    Spacer(Modifier.height(12.dp))
                    ActionButton("Sign out", onClick = { Account.signOut() }, tone = Palette.Loss)
                }
                if (Account.status != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(Account.status.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Palette.Caution)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (Account.isSignedIn) {
            item {
                Panel(tone = Palette.Ink) {
                    Text("DATA STAYS LOCAL", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No strategies, bets, props or prices are uploaded. The only thing kept is the " +
                            "verified address, so the account screen can show who is signed in.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                }
            }
        }

        if (!Account.isSignedIn) item {
            Panel {
                Text("SIGN IN WITH A CODE", style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
                Spacer(Modifier.height(10.dp))
                AccountField("Email", email, KeyboardType.Email, enabled = !sent) { email = it }
                Spacer(Modifier.height(8.dp))
                if (!sent) {
                    ActionButton(if (Account.isBusy) "Sending…" else "Email me a code", enabled = !Account.isBusy && Account.isConfigured, onClick = {
                        scope.launch {
                            if (Account.requestCode(email) == null) sent = true
                        }
                    }, tone = Palette.Over)
                } else {
                    AccountField("Code from the email", code, KeyboardType.Number) { code = it }
                    Spacer(Modifier.height(10.dp))
                    ActionButton(if (Account.isBusy) "Checking…" else "Sign in", enabled = !Account.isBusy, onClick = {
                        scope.launch {
                            if (Account.verify(email, code) == null) {
                                code = ""
                                sent = false
                            }
                        }
                    }, tone = Palette.Gain)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Wrong address?",
                        Modifier.clickable { sent = false }.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Palette.Dust,
                    )
                }
                if (!Account.isConfigured) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "This build has no auth project configured yet. Add one below to turn sign-in on.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Caution,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Panel(tone = Palette.Ink) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AUTH SERVER", style = MaterialTheme.typography.labelSmall, color = Palette.Faint, modifier = Modifier.weight(1f))
                    Text(
                        if (showSetup) "Hide" else "Configure",
                        Modifier.clickable { showSetup = !showSetup }.padding(6.dp),
                        style = MaterialTheme.typography.labelSmall, color = Palette.Over,
                    )
                }
                if (showSetup) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Cheat Code uses Supabase Auth, which is open source and can be self-hosted. " +
                            "The anon key is public by design; it identifies the project, not a user.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Dust,
                    )
                    Spacer(Modifier.height(10.dp))
                    AccountField("Project URL · https://xyz.supabase.co", url, KeyboardType.Uri) { url = it }
                    Spacer(Modifier.height(8.dp))
                    AccountField("Anon key", anon, KeyboardType.Text) { anon = it }
                    Spacer(Modifier.height(10.dp))
                    ActionButton("Save", enabled = url.isNotBlank() && anon.isNotBlank(), onClick = {
                        Account.configure(url, anon)
                    })
                }
            }
        }

        item {
            Text(
                "No password and no session token is stored. Only the verified email is kept, in this " +
                    "app's private preferences, and it is never written to the database, log or audit trail.",
                Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
            )
        }
    }
}

@Composable
private fun AccountField(
    label: String,
    value: String,
    keyboard: KeyboardType,
    enabled: Boolean = true,
    onChange: (String) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Faint)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboard,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (enabled) Palette.Chalk else Palette.Faint),
            cursorBrush = SolidColor(Palette.Over),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Palette.Ink).padding(horizontal = 12.dp, vertical = 11.dp),
        )
    }
}
