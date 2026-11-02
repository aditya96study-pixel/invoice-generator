package com.amitabhtours.invoice.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.data.TripEntity
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.tracking.TrackingState
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.AppLogo
import com.amitabhtours.invoice.ui.components.IconCircle
import com.amitabhtours.invoice.ui.components.InvoiceRow
import com.amitabhtours.invoice.ui.components.SectionHeader
import com.amitabhtours.invoice.ui.components.StatTile
import com.amitabhtours.invoice.ui.goTab
import com.amitabhtours.invoice.ui.theme.extra
import com.amitabhtours.invoice.ui.trips.LiveTripCard
import com.amitabhtours.invoice.ui.trips.rememberStartTracking
import java.time.LocalDate

@Composable
fun HomeScreen(nav: NavHostController) {
    val app = app()
    val settings by app.settings.flow.collectAsState(initial = null)
    val invoices by remember { app.db.invoices().observeAll() }.collectAsState(initial = emptyList())
    val current by remember { app.db.trips().observeCurrent() }.collectAsState(initial = null)
    val uninvoiced by remember { app.db.trips().observeUninvoiced() }.collectAsState(initial = emptyList())
    val justEnded by TrackingState.justEnded.collectAsState()
    val startTracking = rememberStartTracking()

    LaunchedEffect(justEnded) {
        justEnded?.let {
            TrackingState.consumeEnded()
            nav.navigate(Routes.trip(it))
        }
    }

    val today = LocalDate.now()
    val todays = invoices.filter { !it.isCancelled && Fmt.localDate(it.createdAt) == today }
    val todayTotal = todays.sumOf { it.totalPaise }
    val due = invoices.sumOf { it.outstandingPaise }
    val now = System.currentTimeMillis()

    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                AppLogo(46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(Fmt.greeting(now) + (settings?.driverName?.takeIf { it.isNotBlank() }?.let { ", ${it.split(" ").first()}" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(settings?.businessName ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                IconButton(onClick = { nav.navigate(Routes.help()) }) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, "How to use") }
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        }

        // Hero: new invoice
        item {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(extra.hero).padding(20.dp),
            ) {
                Text("Create an invoice", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Fill in a few details — the total and the PDF are made for you.",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { nav.navigate(Routes.newInvoice()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Invoice", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Tracking
        item {
            val t = current
            if (t != null) {
                LiveTripCard(t, onOpen = { nav.navigate(Routes.trip(t.id)) })
            } else {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(Icons.Rounded.GpsFixed, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Track a trip with GPS", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Start when the customer gets in. Measures km and draws the route — needed for map invoice styles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = startTracking, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.Rounded.GpsFixed, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start trip tracking")
                    }
                }
            }
        }

        if (uninvoiced.isNotEmpty()) {
            item { SectionHeader("Trips waiting for an invoice", action = { TextButton(onClick = { nav.navigate(Routes.TRIPS) }) { Text("All trips") } }) }
            items(uninvoiced.take(3), key = { "trip${it.id}" }) { t -> UninvoicedTrip(t, onInvoice = { nav.navigate(Routes.newInvoice(tripId = t.id)) }, onOpen = { nav.navigate(Routes.trip(t.id)) }) }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Today", Money.short(todayTotal), Modifier.weight(1f), sub = "${todays.size} invoice${if (todays.size == 1) "" else "s"}", icon = Icons.Rounded.Today)
                StatTile(
                    "Balance due", Money.short(due), Modifier.weight(1f),
                    sub = if (due > 0) "Tap Earnings to collect" else "All collected",
                    icon = Icons.Rounded.CurrencyRupee,
                    accent = if (due > 0) extra.warning else extra.success,
                )
            }
        }

        item {
            SectionHeader("Recent invoices", action = {
                if (invoices.isNotEmpty()) TextButton(onClick = { nav.goTab(Routes.HISTORY) }) { Text("See all") }
            })
        }
        if (invoices.isEmpty()) {
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(Icons.AutoMirrored.Rounded.ReceiptLong, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("No invoices yet", style = MaterialTheme.typography.titleSmall)
                            Text("Tap New Invoice to make your first one. New here? Open How to use.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    invoices.take(4).forEach { inv -> InvoiceRow(inv, onClick = { nav.navigate(Routes.invoice(inv.id)) }) }
                }
            }
        }
    }
}

@Composable
private fun UninvoicedTrip(t: TripEntity, onInvoice: () -> Unit, onOpen: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onOpen, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(Icons.Rounded.Route, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${Num.clean(Math.round(t.km * 10) / 10.0)} km · ${Fmt.duration(t.activeMs())}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${Fmt.date(t.startedAt)}, ${Fmt.time(t.startedAt)}" + (t.endAddress.takeIf { it.isNotBlank() }?.let { " → ${it.split(",").first()}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
            TextButton(onClick = onInvoice) {
                Text("Invoice")
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
