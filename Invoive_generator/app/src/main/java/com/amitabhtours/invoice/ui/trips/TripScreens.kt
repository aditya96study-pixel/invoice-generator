package com.amitabhtours.invoice.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.data.TripStatus
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.ConfirmDialog
import com.amitabhtours.invoice.ui.components.EmptyState
import com.amitabhtours.invoice.ui.components.IconCircle
import com.amitabhtours.invoice.ui.components.SimpleTopBar
import com.amitabhtours.invoice.ui.components.StatusPill
import com.amitabhtours.invoice.ui.theme.extra
import kotlinx.coroutines.launch

@Composable
fun TripsScreen(nav: NavHostController) {
    val app = app()
    val trips by remember { app.db.trips().observeAll() }.collectAsState(initial = emptyList())
    Scaffold(topBar = { SimpleTopBar("Tracked trips", onBack = { nav.popBackStack() }) }, contentWindowInsets = WindowInsets.navigationBars) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (trips.isEmpty()) item { EmptyState(Icons.Rounded.Route, "No tracked trips", "Start trip tracking from the Home screen.") }
            items(trips, key = { it.id }) { t ->
                AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.trip(t.id)) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(Icons.Rounded.Route, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${Num.clean(Math.round(t.km * 10) / 10.0)} km · ${Fmt.duration(t.activeMs())}", style = MaterialTheme.typography.titleSmall)
                            Text("${Fmt.date(t.startedAt)}, ${Fmt.time(t.startedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when {
                            t.status != TripStatus.ENDED -> StatusPill("In progress", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                            t.invoiceId != null -> StatusPill("Invoiced", extra.successContainer, extra.success)
                            else -> StatusPill("No invoice", extra.warningContainer, extra.warning)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripDetailScreen(nav: NavHostController, id: Long) {
    val app = app()
    val scope = rememberCoroutineScope()
    val trip by remember(id) { app.db.trips().observe(id) }.collectAsState(initial = null)
    val points by remember(id) { app.db.trips().observePoints(id) }.collectAsState(initial = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }
    val t = trip
    Scaffold(
        topBar = {
            SimpleTopBar(
                if (t?.status == TripStatus.ENDED) "Trip" else "Trip in progress",
                onBack = { nav.popBackStack() },
                subtitle = t?.let { "${Fmt.date(it.startedAt)}, ${Fmt.time(it.startedAt)}" },
                actions = {
                    if (t != null && t.status == TripStatus.ENDED && t.invoiceId == null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete trip") }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { pad ->
        if (t == null) return@Scaffold
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RouteMap(points, Modifier.fillMaxWidth().height(320.dp).clip(MaterialTheme.shapes.large), follow = t.status != TripStatus.ENDED)
            if (points.isEmpty()) {
                Text(
                    "No GPS points yet. Make sure location is on and you are outdoors; the first fix can take a minute.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (t.status != TripStatus.ENDED) {
                LiveTripCard(t, onOpen = {})
            } else {
                AppCard(Modifier.fillMaxWidth()) {
                    TripStats(t)
                    Spacer(Modifier.height(14.dp))
                    Place(Icons.Rounded.TripOrigin, "Start · ${Fmt.time(t.startedAt)}", t.startAddress.ifBlank { "Address not found (no internet at the time)" })
                    Spacer(Modifier.height(10.dp))
                    Place(Icons.Rounded.Flag, "End · ${t.endedAt?.let { Fmt.time(it) } ?: ""}", t.endAddress.ifBlank { "Address not found (no internet at the time)" })
                }
                if (t.invoiceId == null) {
                    Button(onClick = { nav.navigate(Routes.newInvoice(tripId = t.id)) { popUpTo(Routes.trip(t.id)) { inclusive = true } } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null); Spacer(Modifier.width(8.dp)); Text("Create invoice for this trip")
                    }
                } else {
                    OutlinedButton(onClick = { nav.navigate(Routes.invoice(t.invoiceId)) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Open invoice")
                    }
                }
            }
            Text("Map data © OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this trip?",
            text = "The route and distance will be removed. This cannot be undone.",
            confirm = "Delete",
            destructive = true,
            onConfirm = { scope.launch { app.repo.deleteTrip(id); nav.popBackStack() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Place(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
