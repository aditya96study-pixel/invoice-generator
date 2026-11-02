package com.amitabhtours.invoice.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.data.CustomerEntity
import com.amitabhtours.invoice.data.CustomerSummary
import com.amitabhtours.invoice.data.PlaceCount
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Text as TextUtil
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.Avatar
import com.amitabhtours.invoice.ui.components.BigTopBar
import com.amitabhtours.invoice.ui.components.EmptyState
import com.amitabhtours.invoice.ui.components.InvoiceRow
import com.amitabhtours.invoice.ui.components.PlainField
import com.amitabhtours.invoice.ui.components.SectionHeader
import com.amitabhtours.invoice.ui.components.SimpleTopBar
import com.amitabhtours.invoice.ui.components.StatTile
import com.amitabhtours.invoice.ui.components.StatusPill
import com.amitabhtours.invoice.ui.theme.extra
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(nav: NavHostController) {
    val app = app()
    val list by remember { app.db.customers().observeSummaries() }.collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val q = query.trim()
    val digits = q.filter { it.isDigit() }
    val filtered = list.filter { c -> q.isEmpty() || c.name.contains(q, true) || (digits.length >= 3 && c.mobile.filter { it.isDigit() }.contains(digits)) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BigTopBar("Customers", scroll) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { PlainField("", query, { query = it }, placeholder = "Search by name or mobile", leading = Icons.Rounded.Search) }
            item {
                Text(
                    "Customers are saved automatically from your invoices (matched by mobile number).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (filtered.isEmpty()) {
                item { EmptyState(Icons.Rounded.Groups, if (list.isEmpty()) "No customers yet" else "No match", if (list.isEmpty()) "They appear here after your first invoice." else "Try another name or number.") }
            }
            items(filtered, key = { it.id }) { c -> CustomerCard(c) { nav.navigate(Routes.customer(c.id)) } }
        }
    }
}

@Composable
private fun CustomerCard(c: CustomerSummary, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(c.mobile, "${c.trips} trip${if (c.trips == 1) "" else "s"}").filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.short(c.totalPaise), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (c.duePaise > 0) StatusPill("Due ${Money.short(c.duePaise)}", extra.warningContainer, extra.warning)
            }
        }
    }
}

@Composable
fun CustomerDetailScreen(nav: NavHostController, id: Long) {
    val app = app()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customer by remember(id) { app.db.customers().observe(id) }.collectAsState(initial = null)
    val invoices by remember(id) { app.db.invoices().observeForCustomer(id) }.collectAsState(initial = emptyList())
    val all by remember { app.db.customers().observeAll() }.collectAsState(initial = emptyList())
    var pickups by remember { mutableStateOf<List<PlaceCount>>(emptyList()) }
    var drops by remember { mutableStateOf<List<PlaceCount>>(emptyList()) }
    var edit by remember { mutableStateOf(false) }
    var merge by remember { mutableStateOf(false) }
    LaunchedEffect(id, invoices.size) {
        pickups = app.db.invoices().topPickups(id)
        drops = app.db.invoices().topDrops(id)
    }
    val c = customer ?: return
    val active = invoices.filter { !it.isCancelled }
    val digits = TextUtil.mobileKey(c.mobile)

    Scaffold(
        topBar = {
            SimpleTopBar(c.name, onBack = { nav.popBackStack() }, actions = {
                IconButton(onClick = { edit = true }) { Icon(Icons.Rounded.Edit, "Edit") }
                IconButton(onClick = { merge = true }) { Icon(Icons.Rounded.MergeType, "Merge duplicate") }
            })
        },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.name, 56)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleLarge)
                            if (c.mobile.isNotBlank()) Text(c.mobile, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (c.notes.isNotBlank()) Text(c.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.mobile.filter { it.isDigit() || it == '+' }}"))) },
                            enabled = digits.isNotEmpty(), modifier = Modifier.weight(1f).height(48.dp),
                        ) { Icon(Icons.Rounded.Call, null); Spacer(Modifier.width(4.dp)); Text("Call") }
                        FilledTonalButton(
                            onClick = {
                                val num = if (digits.length == 10) "91$digits" else digits
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num"))) }
                            },
                            enabled = digits.isNotEmpty(), modifier = Modifier.weight(1f).height(48.dp),
                        ) { Icon(Icons.AutoMirrored.Rounded.Chat, null); Spacer(Modifier.width(4.dp)); Text("WhatsApp") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { nav.navigate(Routes.newInvoice(customerId = c.id)) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("New invoice for ${TextUtil.firstName(c.name)}")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Trips", active.size.toString(), Modifier.weight(1f))
                    StatTile("Total billed", Money.short(active.sumOf { it.totalPaise }), Modifier.weight(1f))
                    StatTile("Due", Money.short(active.sumOf { it.outstandingPaise }), Modifier.weight(1f))
                }
            }
            if (pickups.isNotEmpty() || drops.isNotEmpty()) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Text("Usual places", style = MaterialTheme.typography.titleMedium)
                        pickups.forEach { Text("From: ${it.place}  (${it.count}×)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
                        drops.forEach { Text("To: ${it.place}  (${it.count}×)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
                    }
                }
            }
            item { SectionHeader("Invoices") }
            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    if (invoices.isEmpty()) Text("No invoices.", modifier = Modifier.padding(8.dp))
                    invoices.forEach { inv -> InvoiceRow(inv, onClick = { nav.navigate(Routes.invoice(inv.id)) }) }
                }
            }
            item {
                Text(
                    "Customer since ${Fmt.date(c.createdAt)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (edit) {
        var name by remember { mutableStateOf(c.name) }
        var mobile by remember { mutableStateOf(c.mobile) }
        var notes by remember { mutableStateOf(c.notes) }
        AlertDialog(
            onDismissRequest = { edit = false },
            title = { Text("Edit customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlainField("Name", name, { name = it })
                    PlainField("Mobile", mobile, { mobile = it }, keyboardType = KeyboardType.Phone)
                    PlainField("Notes", notes, { notes = it }, singleLine = false)
                    Text("Old invoices keep the details they were printed with.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.repo.updateCustomer(c.copy(name = name.trim().ifEmpty { c.name }, mobile = mobile.trim(), notes = notes.trim())) }
                    edit = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { edit = false }) { Text("Cancel") } },
        )
    }

    if (merge) {
        val others = all.filter { it.id != c.id }
        AlertDialog(
            onDismissRequest = { merge = false },
            title = { Text("Merge a duplicate into ${c.name}") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text("Choose the duplicate. Its invoices move to ${c.name} and the duplicate is removed.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (others.isEmpty()) Text("No other customers.")
                    others.forEach { o -> MergeOption(o) { scope.launch { app.repo.mergeCustomers(o.id, c.id) }; merge = false } }
                }
            },
            confirmButton = { TextButton(onClick = { merge = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun MergeOption(o: CustomerEntity, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(o.name, 36)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(o.name, style = MaterialTheme.typography.titleSmall)
                if (o.mobile.isNotBlank()) Text(o.mobile, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
