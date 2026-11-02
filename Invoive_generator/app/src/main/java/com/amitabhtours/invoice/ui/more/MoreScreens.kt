package com.amitabhtours.invoice.ui.more

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.BuildConfig
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.AppLogo
import com.amitabhtours.invoice.ui.components.BigTopBar
import com.amitabhtours.invoice.ui.components.IconCircle
import com.amitabhtours.invoice.ui.components.InfoBanner
import com.amitabhtours.invoice.ui.components.PlainField
import com.amitabhtours.invoice.ui.components.SimpleTopBar
import com.amitabhtours.invoice.ui.theme.extra
import com.amitabhtours.invoice.ui.trips.openAppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavHostController) {
    val app = app()
    val settings by app.settings.flow.collectAsState(initial = null)
    val context = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(modifier = Modifier.nestedScroll(scroll.nestedScrollConnection), topBar = { BigTopBar("More", scroll) }) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(52.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(settings?.businessName ?: "", style = MaterialTheme.typography.titleMedium)
                            Text(settings?.phone ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(settings?.carNo ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                    MenuRow(Icons.AutoMirrored.Rounded.HelpOutline, "How to use", "Step-by-step guide to every feature") { nav.navigate(Routes.help()) }
                    MenuRow(Icons.Rounded.Settings, "Settings", "Business details, rates, numbers, signature, UPI") { nav.navigate(Routes.SETTINGS) }
                    MenuRow(Icons.Rounded.Route, "Tracked trips", "All GPS trips and their routes") { nav.navigate(Routes.TRIPS) }
                    MenuRow(Icons.Rounded.BatterySaver, "Phone settings for tracking", "Allow the app to run during trips") { openAppSettings(context) }
                    MenuRow(Icons.Rounded.Info, "About", "Version, data and credits") { nav.navigate(Routes.ABOUT) }
                }
            }
            item {
                InfoBanner("Your data is stored only on this phone. Export a copy from Settings → Your data now and then.", icon = Icons.Rounded.Lock)
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HelpScreen(nav: NavHostController, topic: String?) {
    var query by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf(topic ?: "") }
    val q = query.trim()
    val topics = HELP_TOPICS.filter { t ->
        q.isEmpty() || t.title.contains(q, true) || t.intro.contains(q, true) || t.steps.any { it.contains(q, true) } || t.tips.any { it.contains(q, true) }
    }
    Scaffold(topBar = { SimpleTopBar("How to use", onBack = { nav.popBackStack() }) }, contentWindowInsets = WindowInsets.navigationBars) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { PlainField("", query, { query = it }, placeholder = "Search help (e.g. toll, GPS, WhatsApp)", leading = Icons.Rounded.Search) }
            item {
                Text("Tap a topic to open it. Works without internet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(topics, key = { it.id }) { t ->
                val expanded = open == t.id || q.isNotEmpty()
                AppCard(Modifier.fillMaxWidth().animateContentSize(), onClick = { open = if (open == t.id) "" else t.id }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(t.icon, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(t.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    }
                    if (expanded) {
                        Spacer(Modifier.height(10.dp))
                        Text(t.intro, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        t.steps.forEachIndexed { i, step ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Box(
                                    Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(10.dp))
                                Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                        t.tips.forEach { tip ->
                            Spacer(Modifier.height(6.dp))
                            InfoBanner(tip, icon = Icons.Rounded.Info, container = MaterialTheme.colorScheme.surfaceContainer, content = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            if (topics.isEmpty()) item { Text("No help topic matches \"$q\".", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun AboutScreen(nav: NavHostController) {
    val app = app()
    val settings by app.settings.flow.collectAsState(initial = null)
    Scaffold(topBar = { SimpleTopBar("About", onBack = { nav.popBackStack() }) }, contentWindowInsets = WindowInsets.navigationBars) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            AppLogo(96.dp)
            Text(settings?.businessName ?: "", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("Invoices, trips and earnings · Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppCard(Modifier.fillMaxWidth()) {
                Text("Your data", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Invoices, customers, trips and settings are stored only on this phone. Nothing is uploaded. " +
                        "Internet is used only to download map tiles and look up addresses for tracked trips.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AppCard(Modifier.fillMaxWidth()) {
                Text("Credits", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Maps: © OpenStreetMap contributors (openstreetmap.org/copyright), shown with osmdroid. " +
                        "QR codes: ZXing. Built with Kotlin and Jetpack Compose.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private data class WelcomePage(val icon: ImageVector, val title: String, val text: String)

private val welcomePages = listOf(
    WelcomePage(
        Icons.AutoMirrored.Rounded.ReceiptLong, "Invoices in a minute",
        "Fill in the customer, km and charges. The total, amount in words, invoice number, date and time are done for you. Share the PDF on WhatsApp straight away.",
    ),
    WelcomePage(
        Icons.Rounded.TouchApp, "Grey text saves typing",
        "Details from your last invoice appear in grey. Leave them as they are and they are printed. Type to change them, or tap × to leave a box empty.",
    ),
    WelcomePage(
        Icons.Rounded.GpsFixed, "Track trips, see earnings",
        "Track a trip with GPS for map-style invoices. The Earnings tab shows your income with charts — only for work recorded in the app.",
    ),
)

@Composable
fun OnboardingScreen() {
    val app = app()
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { welcomePages.size + 1 }
    fun finish() { scope.launch { app.settings.update { it.copy(onboardingDone = true) } } }
    Column(
        Modifier.fillMaxSize().background(extra.hero).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppLogo(40.dp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { finish() }) { Text("Skip", color = Color.White) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (page == 0) {
                    AppLogo(120.dp)
                    Spacer(Modifier.height(24.dp))
                    Text("Amitabh Tours & Travels", style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("Your invoice book, trip meter and earnings diary — in one app.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                } else {
                    val p = welcomePages[page - 1]
                    Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                        Icon(p.icon, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(p.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(p.text, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.82f), textAlign = TextAlign.Center)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
            repeat(welcomePages.size + 1) { i ->
                Box(
                    Modifier.padding(4.dp).size(if (pager.currentPage == i) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                        .background(if (pager.currentPage == i) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.35f)),
                )
            }
        }
        val last = pager.currentPage == welcomePages.size
        Button(
            onClick = { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
            shape = MaterialTheme.shapes.medium,
        ) { Text(if (last) "Get started" else "Next", style = MaterialTheme.typography.titleMedium) }
        Text(
            "Full guide any time: More → How to use",
            color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
        )
    }
}
