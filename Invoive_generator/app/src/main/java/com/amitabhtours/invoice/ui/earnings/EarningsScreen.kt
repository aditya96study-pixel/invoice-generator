package com.amitabhtours.invoice.ui.earnings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.data.InvoiceEntity
import com.amitabhtours.invoice.data.OTHER_INCOME_SOURCES
import com.amitabhtours.invoice.data.OtherIncomeEntity
import com.amitabhtours.invoice.data.PAYMENT_MODES
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.InvoiceNumbering
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.BigTopBar
import com.amitabhtours.invoice.ui.components.ChoiceChips
import com.amitabhtours.invoice.ui.components.InfoBanner
import com.amitabhtours.invoice.ui.components.InvoiceRow
import com.amitabhtours.invoice.ui.components.KeyValue
import com.amitabhtours.invoice.ui.components.PlainField
import com.amitabhtours.invoice.ui.components.SectionHeader
import com.amitabhtours.invoice.ui.components.StatTile
import com.amitabhtours.invoice.ui.components.moneyFilter
import com.amitabhtours.invoice.ui.theme.extra
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private enum class Period(val label: String) { WEEK("Week"), MONTH("Month"), FY("Fin. year"), CUSTOM("Custom") }
private enum class ChartRange(val label: String) { DAYS("30 days"), WEEKS("12 weeks"), MONTHS("12 months") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(nav: NavHostController) {
    val app = app()
    val scope = rememberCoroutineScope()
    val invoicesAll by remember { app.db.invoices().observeAll() }.collectAsState(initial = emptyList())
    val incomeAll by remember { app.db.income().observeAll() }.collectAsState(initial = emptyList())
    var period by rememberSaveable { mutableStateOf(Period.MONTH) }
    var customStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var customEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var includeOther by rememberSaveable { mutableStateOf(true) }
    var chartRange by rememberSaveable { mutableStateOf(ChartRange.DAYS) }
    var pickRange by remember { mutableStateOf(false) }
    var addIncome by remember { mutableStateOf(false) }
    var payInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val today = LocalDate.now()
    val (start, end) = when (period) {
        Period.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to today
        Period.MONTH -> today.withDayOfMonth(1) to today
        Period.FY -> LocalDate.of(InvoiceNumbering.financialYearStart(today), 4, 1) to today
        Period.CUSTOM -> (customStart?.let { utcDate(it) } ?: today.minusDays(29)) to (customEnd?.let { utcDate(it) } ?: today)
    }
    val startMs = Fmt.startOfDay(start)
    val endMs = Fmt.endOfDay(end)
    val active = invoicesAll.filter { !it.isCancelled }
    val inRange = active.filter { it.createdAt in startMs..endMs }
    val otherInRange = incomeAll.filter { it.date in startMs..endMs }

    val billed = inRange.sumOf { it.totalPaise }
    val passThrough = inRange.sumOf { it.tollPaise + it.parkingPaise }
    val other = if (includeOther) otherInRange.sumOf { it.amountPaise } else 0L
    val net = billed - passThrough + other
    val trips = inRange.size
    val km = inRange.sumOf { it.billedKm }
    val outstanding = active.sumOf { it.outstandingPaise }
    val daysWorked = (inRange.map { Fmt.localDate(it.createdAt) } + if (includeOther) otherInRange.map { Fmt.localDate(it.date) } else emptyList()).toSet().size
    val calendarDays = (ChronoUnit.DAYS.between(start, end) + 1).coerceAtLeast(1)

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BigTopBar("Earnings", scroll) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                InfoBanner(
                    "These figures include only trips recorded in this app. Income from other sources is not shown unless you add it below.",
                    icon = Icons.Rounded.Info,
                )
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Period.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = period == p,
                            onClick = { period = p; if (p == Period.CUSTOM) pickRange = true },
                            shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size),
                            label = { Text(p.label, maxLines = 1) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${Fmt.shortDate(start)} ${start.year} – ${Fmt.shortDate(end)} ${end.year}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (period == Period.CUSTOM) TextButton(onClick = { pickRange = true }) { Text("Change dates") }
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Net earnings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.short(net), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Billed ${Money.short(billed)} − tolls & parking ${Money.short(passThrough)}" + if (other > 0) " + other income ${Money.short(other)}" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Include other income", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = includeOther, onCheckedChange = { includeOther = it })
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Total billed", Money.short(billed), Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.CurrencyRupee)
                        StatTile("Trips", trips.toString(), Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.LocalTaxi)
                    }
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Total km", Num.grouped(km), Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.Route)
                        StatTile("Avg per trip", if (trips > 0) rupees((net - other) / trips) else "—", Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.Wallet)
                    }
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Avg per km", if (km > 0) Money.format(((net - other) / km).toLong()) else "—", Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.Speed)
                        StatTile("Balance due", Money.short(outstanding), Modifier.weight(1f).fillMaxHeight(), sub = "All unpaid invoices", icon = Icons.Rounded.Savings,
                            accent = if (outstanding > 0) extra.warning else extra.success)
                    }
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Averages", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    KeyValue("Per day worked", if (daysWorked > 0) rupees(net / daysWorked) + "  (${days(daysWorked.toLong())})" else "—")
                    KeyValue("Per calendar day", rupees(net / calendarDays) + "  (${days(calendarDays)})")
                    KeyValue("Per week", rupees((net * 7.0 / calendarDays).toLong()))
                    KeyValue("Per month", rupees((net * 30.44 / calendarDays).toLong()))
                }
            }

            // Bar chart
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Earnings over time", style = MaterialTheme.typography.titleMedium)
                    Text("Net of tolls and parking. Tap a bar for details.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ChartRange.entries.forEachIndexed { i, r ->
                            SegmentedButton(selected = chartRange == r, onClick = { chartRange = r }, shape = SegmentedButtonDefaults.itemShape(i, ChartRange.entries.size), label = { Text(r.label) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val groups = buildGroups(chartRange, active, if (includeOther) incomeAll else emptyList(), today)
                    val colors = listOf(extra.chart1, extra.chart2)
                    BarChart(
                        groups, colors,
                        seriesNames = if (includeOther) listOf("Invoices", "Other income") else listOf("Invoices"),
                        format = { compactRupees(it) },
                        minScale = 1000f,
                    )
                }
            }

            // Donut
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Where the money came from", style = MaterialTheme.typography.titleMedium)
                    Text("Selected period · tap a slice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    val slices = listOf(
                        "Distance" to inRange.sumOf { it.distanceFarePaise },
                        "Waiting" to inRange.sumOf { it.waitingFarePaise },
                        "Night charges" to inRange.sumOf { it.nightPaise },
                        "Driver allowance" to inRange.sumOf { it.allowanceFarePaise },
                        "Other charges" to inRange.sumOf { it.customTotalPaise + it.roundOffPaise },
                        "Tolls & parking" to passThrough,
                    ).map { it.first to (it.second / 100f).coerceAtLeast(0f) }
                    Donut(
                        slices,
                        listOf(extra.chart1, extra.chart2, extra.chart3, extra.chart4, extra.chart5, extra.chart6),
                        format = { "₹" + Money.groupIndian(it.toLong()) },
                        centerLabel = Money.short(billed),
                    )
                }
            }

            // Trips per week
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Trips per week", style = MaterialTheme.typography.titleMedium)
                    Text("Last 12 weeks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weeks = (11 downTo 0).map { monday.minusWeeks(it.toLong()) }
                    val pts = weeks.map { w ->
                        val s = Fmt.startOfDay(w); val e = Fmt.endOfDay(w.plusDays(6))
                        Fmt.shortDate(w) to active.count { it.createdAt in s..e }.toFloat()
                    }
                    LineChart(pts, extra.chart1, format = { Num.clean(it.toDouble()) }, title = { "Week of ${Fmt.shortDate(weeks[it])}" })
                }
            }

            // Payments due
            val due = active.filter { it.outstandingPaise > 0 }
            item { SectionHeader("Payments due (${due.size})") }
            if (due.isEmpty()) {
                item { Text("Nothing pending. All invoices are paid.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            } else {
                item {
                    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        due.forEach { inv ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InvoiceRow(inv, onClick = { nav.navigate(Routes.invoice(inv.id)) }, modifier = Modifier.weight(1f))
                                TextButton(onClick = { payInvoice = inv }) { Text("Paid") }
                            }
                        }
                    }
                }
            }

            // Other income
            item {
                SectionHeader("Other income", action = {
                    FilledTonalButton(onClick = { addIncome = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text("Add") }
                })
                Text("Cash trips without an invoice, Ola, Uber, Rapido… Add them here for a fuller picture.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(otherInRange, key = { "inc${it.id}" }) { e ->
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${Money.short(e.amountPaise)} · ${e.source}", style = MaterialTheme.typography.titleSmall)
                            Text(Fmt.date(e.date) + (e.note.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { scope.launch { app.db.income().delete(e) } }) { Icon(Icons.Rounded.Delete, "Delete") }
                    }
                }
            }
        }
    }

    if (pickRange) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            initialSelectedEndDateMillis = end.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickRange = false },
            confirmButton = {
                TextButton(onClick = {
                    customStart = state.selectedStartDateMillis
                    customEnd = state.selectedEndDateMillis ?: state.selectedStartDateMillis
                    pickRange = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { pickRange = false }) { Text("Cancel") } },
        ) { DateRangePicker(state = state, modifier = Modifier.height(480.dp)) }
    }

    if (addIncome) AddIncomeDialog(onDismiss = { addIncome = false }) { e -> scope.launch { app.db.income().insert(e) } }

    payInvoice?.let { inv ->
        var mode by remember(inv.id) { mutableStateOf(inv.paymentMode.ifBlank { "Cash" }) }
        AlertDialog(
            onDismissRequest = { payInvoice = null },
            title = { Text("Mark ${inv.number} as paid") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${Money.format(inv.outstandingPaise)} received from ${inv.customerName.ifBlank { "the customer" }}.")
                    ChoiceChips(PAYMENT_MODES, mode, { mode = it }, { it })
                }
            },
            confirmButton = { TextButton(onClick = { val id = inv.id; payInvoice = null; scope.launch { app.repo.markPaid(id, mode) } }) { Text("Mark paid") } },
            dismissButton = { TextButton(onClick = { payInvoice = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeDialog(onDismiss: () -> Unit, onSave: (OtherIncomeEntity) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(OTHER_INCOME_SOURCES.first()) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var pickDate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add other income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlainField("Amount", amount, { amount = moneyFilter(it) }, keyboardType = KeyboardType.Decimal, prefix = "₹ ")
                ChoiceChips(OTHER_INCOME_SOURCES, source, { source = it }, { it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Date: ${Fmt.date(date)}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { pickDate = true }) { Text("Change") }
                }
                PlainField("Note (optional)", note, { note = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = Money.parse(amount) ?: 0
                    if (p > 0) onSave(OtherIncomeEntity(date = date, amountPaise = p, source = source, note = note.trim()))
                    onDismiss()
                },
                enabled = (Money.parse(amount) ?: 0) > 0,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (pickDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = Fmt.localDate(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { date = Fmt.startOfDay(utcDate(it)) + 12 * 3600_000L }
                    pickDate = false
                }) { Text("OK") }
            },
        ) { DatePicker(st) }
    }
}

private fun rupees(paise: Long) = Money.format(com.amitabhtours.invoice.domain.Fare.roundToRupee(paise), decimals = false)
private fun days(n: Long) = if (n == 1L) "1 day" else "$n days"

private fun utcDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

private fun compactRupees(v: Float): String = when {
    v >= 100_000f -> "₹" + Num.clean(Math.round(v / 10_000.0) / 10.0) + "L"
    v >= 1000f -> "₹" + Num.clean(Math.round(v / 100.0) / 10.0) + "k"
    else -> "₹" + v.toInt()
}

private fun buildGroups(range: ChartRange, invoices: List<InvoiceEntity>, income: List<OtherIncomeEntity>, today: LocalDate): List<BarGroup> {
    fun netOf(inv: InvoiceEntity) = (inv.totalPaise - inv.tollPaise - inv.parkingPaise) / 100f
    return when (range) {
        ChartRange.DAYS -> (29 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            val s = Fmt.startOfDay(d); val e = Fmt.endOfDay(d)
            BarGroup(
                d.dayOfMonth.toString(),
                listOf(invoices.filter { it.createdAt in s..e }.sumOf { netOf(it).toDouble() }.toFloat(), income.filter { it.date in s..e }.sumOf { it.amountPaise } / 100f),
                tooltipTitle = "${Fmt.shortDate(d)} ${d.year}",
            )
        }
        ChartRange.WEEKS -> {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (11 downTo 0).map { back ->
                val w = monday.minusWeeks(back.toLong())
                val s = Fmt.startOfDay(w); val e = Fmt.endOfDay(w.plusDays(6))
                BarGroup(
                    Fmt.shortDate(w),
                    listOf(invoices.filter { it.createdAt in s..e }.sumOf { netOf(it).toDouble() }.toFloat(), income.filter { it.date in s..e }.sumOf { it.amountPaise } / 100f),
                    tooltipTitle = "Week of ${Fmt.shortDate(w)}",
                )
            }
        }
        ChartRange.MONTHS -> {
            val first = today.withDayOfMonth(1)
            (11 downTo 0).map { back ->
                val m = first.minusMonths(back.toLong())
                val s = Fmt.startOfDay(m); val e = Fmt.endOfDay(m.plusMonths(1).minusDays(1))
                BarGroup(
                    m.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                    listOf(invoices.filter { it.createdAt in s..e }.sumOf { netOf(it).toDouble() }.toFloat(), income.filter { it.date in s..e }.sumOf { it.amountPaise } / 100f),
                    tooltipTitle = Fmt.month(m),
                )
            }
        }
    }
}
