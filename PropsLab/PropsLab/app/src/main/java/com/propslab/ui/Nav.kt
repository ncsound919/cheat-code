package com.propslab.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.propslab.ui.components.glow
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.propslab.ui.screens.AccountScreen
import com.propslab.ui.screens.AuditScreen
import com.propslab.ui.screens.BestScreen
import com.propslab.ui.screens.ConsoleScreen
import com.propslab.ui.screens.FlyerScreen
import com.propslab.ui.screens.ModelShopScreen
import com.propslab.ui.screens.NewsScreen
import com.propslab.ui.screens.PremiumScreen
import com.propslab.ui.screens.UpdatesScreen
import com.propslab.ui.screens.DetailScreen
import com.propslab.ui.screens.FantasyCaptureScreen
import com.propslab.ui.screens.FantasyScreen
import com.propslab.ui.screens.FeedScreen
import com.propslab.ui.screens.ImportScreen
import com.propslab.ui.screens.LabScreen
import com.propslab.ui.screens.LensesScreen
import com.propslab.ui.screens.ParlayScreen
import com.propslab.ui.screens.PlainEnglishScreen
import com.propslab.ui.screens.PortfolioScreen
import com.propslab.ui.screens.SandboxScreen
import com.propslab.ui.screens.SettingsScreen
import com.propslab.ui.screens.SlateScreen
import com.propslab.ui.theme.Palette

private data class Tab(val route: String, val label: String)

/**
 * Five destinations, in the order the work happens: read the board, see what the system
 * would take, watch the sandbox decide, check the ledger, then the formula lab.
 * The audit trail sits one level down, behind the sandbox.
 */
private val Tabs = listOf(
    Tab("slate", "Today"),
    Tab("best", "Best"),
    Tab("sandbox", "Sandbox"),
    Tab("portfolio", "Bets"),
    Tab("lab", "Lab"),
)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
        NavHost(
            navController = nav,
            startDestination = "slate",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable("slate") {
                SlateScreen(
                    onOpen = { nav.navigate("detail/$it") },
                    onOpenPlain = { nav.navigate("plain") },
                )
            }
            composable("best") {
                BestScreen(
                    onOpen = { nav.navigate("detail/$it") },
                    onOpenSandbox = { nav.navigate("sandbox") { launchSingleTop = true } },
                    onOpenPlain = { nav.navigate("plain") },
                    onOpenLenses = { nav.navigate("lenses") },
                    onOpenParlay = { nav.navigate("parlay") },
                    onOpenFeed = { nav.navigate("feed") },
                )
            }
            composable("parlay") {
                ParlayScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPlain = { nav.navigate("plain") },
                    onOpenPremium = { nav.navigate("premium") },
                )
            }
            composable("sandbox") {
                SandboxScreen(
                    onOpenAudit = { nav.navigate("audit") },
                    onOpenPlain = { nav.navigate("plain") },
                )
            }
            composable("portfolio") { PortfolioScreen(onOpenPlain = { nav.navigate("plain") }) }
            composable("lab") {
                LabScreen(
                    onOpenImport = { nav.navigate("import") },
                    onOpenFeed = { nav.navigate("feed") },
                    onOpenConsole = { nav.navigate("console") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenPlain = { nav.navigate("plain") },
                    onOpenLenses = { nav.navigate("lenses") },
                    onOpenModels = { nav.navigate("models") },
                    onOpenPremium = { nav.navigate("premium") },
                    onOpenUpdates = { nav.navigate("updates") },
                    onOpenFlyer = { nav.navigate("flyer") },
                    onOpenFantasy = { nav.navigate("fantasy") },
                    onOpenNews = { nav.navigate("news") },
                )
            }
            composable("fantasy") {
                FantasyScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPremium = { nav.navigate("premium") },
                    onOpenCapture = { nav.navigate("fantasy-capture") },
                )
            }
            composable("fantasy-capture") {
                FantasyCaptureScreen(onBack = { nav.popBackStack() })
            }
            composable("news") {
                NewsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPremium = { nav.navigate("premium") },
                )
            }
            composable("models") {
                ModelShopScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPremium = { nav.navigate("premium") },
                )
            }
            composable("premium") {
                PremiumScreen(
                    onBack = { nav.popBackStack() },
                    onOpenAccount = { nav.navigate("account") },
                )
            }
            composable("account") { AccountScreen(onBack = { nav.popBackStack() }) }
            composable("updates") { UpdatesScreen(onBack = { nav.popBackStack() }) }
            composable("flyer") {
                FlyerScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPremium = { nav.navigate("premium") },
                )
            }
            composable("lenses") {
                LensesScreen(
                    onBack = { nav.popBackStack() },
                    onOpenLab = { nav.navigate("lab") { popUpTo("slate") { saveState = true }; launchSingleTop = true } },
                    onOpenBest = { nav.navigate("best") { popUpTo("slate") { saveState = true }; launchSingleTop = true } },
                )
            }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable("plain") { PlainEnglishScreen(onBack = { nav.popBackStack() }) }
            composable("console") { ConsoleScreen(onBack = { nav.popBackStack() }) }
            composable("feed") { FeedScreen(onBack = { nav.popBackStack() }) }
            composable("audit") { AuditScreen(onBack = { nav.popBackStack() }) }
            composable("import") { ImportScreen(onDone = { nav.popBackStack() }) }
            composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                DetailScreen(id = e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
        }
        if (Tabs.any { it.route == route }) {
            Dock(
                selected = route,
                onSelect = { target ->
                    nav.navigate(target) {
                        popUpTo("slate") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The dock is sized by weight rather than by padding so five labels fit any phone width
 * without shrinking the touch target below 48dp.
 */
@Composable
private fun Dock(selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(shape).background(Palette.Raised).border(1.dp, Palette.Hairline, shape).padding(4.dp),
    ) {
        Tabs.forEach { tab ->
            val on = tab.route == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onSelect(tab.route) }
                    .semantics { this.selected = on }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 2.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (on) Palette.Chalk else Palette.Dust,
                    maxLines = 1,
                )
                Spacer(Modifier.height(5.dp))
                // The selected tab gets a drawn halo and a bar that grows into place, so the
                // dock reads as one moving object rather than five labels blinking.
                val width by animateDpAsState(if (on) 18.dp else 0.dp, tween(220), label = "tab")
                Box(
                    Modifier.height(2.dp).width(width)
                        .glow(Palette.Chalk, 18.dp, 0.55f)
                        .background(Palette.Chalk, CircleShape),
                )
            }
        }
    }
}
