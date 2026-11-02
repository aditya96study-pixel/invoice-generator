package com.amitabhtours.invoice.ui.history

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.InvoiceNumbering
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.BigTopBar
import com.amitabhtours.invoice.ui.components.EmptyState
import com.amitabhtours.invoice.ui.components.InvoiceRow
import com.amitabhtours.invoice.ui.components.PlainField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class StatusFilter(val label: String) { ALL("All"), DUE("Balance due"), PAID("Paid"), CANCELLED("Cancelled") }
private enum class DateFilter(val label: String) { ALL("Any date"), MONTH("This month"), LAST_MONTH("Last month"), FY("This fin. year"), CUSTOM("Pick dates") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(nav: NavHostController) {
    val app = app()
    val all by remember { app.db.invoices().observeAll() }.collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var dates by rememberSaveable { mutableStateOf(DateFilter.ALL) }
    var cStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var cEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var pick by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val today = LocalDate.now()
    val range: Pair<Long, Long>? = when (dates) {
        DateFilter.ALL -> null
        DateFilter.MONTH -> Fmt.startOfDay(today.withDayOfMonth(1)) to Fmt.endOfDay(today)
        DateFilter.LAST_MONTH -> today.withDayOfMonth(1).minusMonths(1).let { Fmt.startOfDay(it) to Fmt.endOfDay(it.plusMonths(1).minusDays(1)) }
        DateFilter.FY -> Fmt.startOfDay(LocalDate.of(InvoiceNumbering.financialYearStart(today), 4, 1)) to Fmt.endOfDay(today)
        DateFilter.CUSTOM -> {
            val s = cStart?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: today
            val e = cEnd?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: s
            Fmt.startOfDay(s) to Fmt.endOfDay(e)
        }
    }
    val q = query.trim()
    val digits = q.filter { it.isDigit() }
    val filtered = all.filter { inv ->
        (q.isEmpty() || inv.customerName.contains(q, true) || inv.number.contains(q, true) ||
            (digits.length >= 3 && inv.customerMobile.filter { it.isDigit() }.contains(digits)) ||
            inv.pickup.contains(q, true) || inv.dropAt.contains(q, true)) &&
            when (status) {
                StatusFilter.ALL -> true
                StatusFilter.DUE -> inv.outstandingPaise > 0
                StatusFilter.PAID -> inv.isPaid
                StatusFilter.CANCELLED -> inv.isCancelled
            } &&
            (range == null || inv.createdAt in range.first..range.second)
    }
    val grouped = filtered.groupBy { Fmt.localDate(it.createdAt).withDayOfMonth(1) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BigTopBar("History", scroll) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PlainField("", query, { query = it }, placeholder = "Search name, mobile, invoice no. or place", leading = Icons.Rounded.Search)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StatusFilter.entries) { f ->
                        FilterChip(selected = status == f, onClick = { status = f }, label = { Text(f.label) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DateFilter.entries) { f ->
                        FilterChip(
                            selected = dates == f,
                            onClick = { dates = f; if (f == DateFilter.CUSTOM) pick = true },
                            label = {
                                Text(if (f == DateFilter.CUSTOM && dates == f && range != null) "${Fmt.date(range.first)} – ${Fmt.date(range.second)}" else f.label)
                            },
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        Icons.AutoMirrored.Rounded.ReceiptLong,
                        if (all.isEmpty()) "No invoices yet" else "Nothing matches",
                        if (all.isEmpty()) "Invoices you create will appear here." else "Try a different search or filter.",
                    )
                }
            }
            grouped.forEach { (month, list) ->
                item(key = "h$month") {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(Fmt.month(month), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            "${list.count { !it.isCancelled }} · ${Money.short(list.filter { !it.isCancelled }.sumOf { it.totalPaise })}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item(key = "l$month") {
                    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        list.forEach { inv -> InvoiceRow(inv, onClick = { nav.navigate(Routes.invoice(inv.id)) }) }
                    }
                }
            }
        }
    }

    if (pick) {
        val st = rememberDateRangePickerState(initialSelectedStartDateMillis = cStart, initialSelectedEndDateMillis = cEnd)
        DatePickerDialog(
            onDismissRequest = { pick = false },
            confirmButton = { TextButton(onClick = { cStart = st.selectedStartDateMillis; cEnd = st.selectedEndDateMillis; pick = false }) { Text("Apply") } },
            dismissButton = { TextButton(onClick = { pick = false }) { Text("Cancel") } },
        ) { DateRangePicker(st, Modifier.height(480.dp)) }
    }
}
