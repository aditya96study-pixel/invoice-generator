package com.amitabhtours.invoice.ui.invoice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.data.KmSource
import com.amitabhtours.invoice.data.PAYMENT_MODES
import com.amitabhtours.invoice.domain.Fare
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.appViewModel
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.ChoiceChips
import com.amitabhtours.invoice.ui.components.DateTimeField
import com.amitabhtours.invoice.ui.components.FieldLabel
import com.amitabhtours.invoice.ui.components.InfoBanner
import com.amitabhtours.invoice.ui.components.MemField
import com.amitabhtours.invoice.ui.components.MemoryField
import com.amitabhtours.invoice.ui.components.PlainField
import com.amitabhtours.invoice.ui.components.SectionCard
import com.amitabhtours.invoice.ui.components.SimpleTopBar
import com.amitabhtours.invoice.ui.components.Stepper
import com.amitabhtours.invoice.ui.components.moneyFilter
import com.amitabhtours.invoice.ui.theme.extra

@Composable
fun NewInvoiceScreen(nav: NavHostController, tripId: Long?, customerId: Long?) {
    val vm = appViewModel(key = "new-$tripId-$customerId") { InvoiceFormViewModel(it, tripId, customerId) }
    var confirmLeave by remember { mutableStateOf(false) }

    fun back() {
        when (vm.step) {
            Step.FORM -> confirmLeave = true
            Step.REVIEW -> vm.step = Step.FORM
            Step.SIGN -> vm.step = Step.REVIEW
            Step.THEME -> vm.step = if (vm.settings.customerSignatureEnabled) Step.SIGN else Step.REVIEW
        }
    }
    BackHandler(enabled = !vm.saving) { back() }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Discard this invoice?") },
            text = { Text("Nothing has been saved yet.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; nav.popBackStack() }) { Text("Discard", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep editing") } },
        )
    }

    if (vm.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when (vm.step) {
        Step.FORM -> FormStep(vm, onBack = ::back, onNext = { vm.step = Step.REVIEW }, nav = nav)
        Step.REVIEW -> ReviewStep(vm, onBack = ::back, onNext = { vm.step = if (vm.settings.customerSignatureEnabled) Step.SIGN else Step.THEME })
        Step.SIGN -> SignatureStep(vm, onBack = ::back, onNext = { vm.step = Step.THEME })
        Step.THEME -> ThemeStep(vm, nav, onBack = ::back, onDone = { id ->
            nav.navigate(Routes.invoice(id)) {
                popUpTo(Routes.NEW_INVOICE) { inclusive = true }
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormStep(vm: InvoiceFormViewModel, onBack: () -> Unit, onNext: () -> Unit, nav: NavHostController) {
    val s = vm.settings
    Scaffold(
        topBar = { SimpleTopBar("New invoice", onBack = onBack, subtitle = vm.previewNumber) },
        bottomBar = { TotalBar(vm, "Review", onNext) },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoBanner(
                "Grey text is from your last invoice. It is printed as it is unless you type over it. Tap × to leave a field blank.",
                icon = Icons.Rounded.Info,
            )

            // Invoice meta
            AppCard(Modifier.fillMaxWidth()) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Invoice No.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vm.previewNumber, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Date & time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Set automatically", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            TripLinkCard(vm)

            // Customer
            SectionCard("Customer", Icons.Rounded.Person) {
                MemoryField("Customer name", vm.customerName, { vm.customerName = it }, leading = Icons.Rounded.Person, placeholder = "Full name")
                val sugg = vm.suggestions()
                if (sugg.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sugg.forEach { c ->
                            SuggestionChip(onClick = { vm.pickCustomer(c) }, label = { Text("${c.name}${if (c.mobile.isNotBlank()) " · ${c.mobile}" else ""}") })
                        }
                    }
                }
                MemoryField("Customer mobile", vm.customerMobile, { vm.customerMobile = it.copy(text = it.text.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }) },
                    keyboardType = KeyboardType.Phone, leading = Icons.Rounded.Phone, placeholder = "10-digit mobile")
                MemoryField("Pickup from", vm.pickup, { vm.pickup = it }, singleLine = false, minLines = 2, placeholder = "Pickup address")
                MemoryField("Drop to", vm.drop, { vm.drop = it }, singleLine = false, minLines = 2, placeholder = "Drop address")
            }

            // Vehicle & driver
            SectionCard("Vehicle & driver", Icons.Rounded.DirectionsCar) {
                MemoryField("Car No.", vm.carNo, { vm.carNo = it }, capitalization = KeyboardCapitalization.Characters, leading = Icons.Rounded.DirectionsCar)
                MemoryField("Vehicle type", vm.vehicleType, { vm.vehicleType = it })
                MemoryField("Driver name", vm.driverName, { vm.driverName = it }, leading = Icons.Rounded.Person)
                MemoryField("Driver mobile", vm.driverMobile, { vm.driverMobile = it }, keyboardType = KeyboardType.Phone, leading = Icons.Rounded.Phone)
            }

            // Trip
            SectionCard("Trip", Icons.Rounded.Route) {
                Column {
                    FieldLabel("Trip type")
                    val types = (s.tripTypes + listOfNotNull(vm.tripType.value.takeIf { it.isNotBlank() && it !in s.tripTypes })).distinct()
                    ChoiceChips(types, vm.tripType.value.takeIf { it.isNotBlank() }, { vm.tripType = MemField(text = it, hint = vm.tripType.hint) }, { it })
                    Spacer(Modifier.height(8.dp))
                    var otherType by remember { mutableStateOf(false) }
                    if (otherType) {
                        PlainField("", vm.tripType.text.takeIf { it !in s.tripTypes }.orEmpty(), { vm.tripType = vm.tripType.copy(text = it) }, placeholder = "Type another trip type", capitalization = KeyboardCapitalization.Words)
                    } else {
                        TextButton(onClick = { otherType = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text("Other trip type") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MemoryField("Start km", vm.startKm, { vm.startKm = it.copy(text = it.text.filter { c -> c.isDigit() || c == '.' }) }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal, placeholder = "Odometer")
                    PlainField("End km", vm.endKm, { vm.endKm = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal, placeholder = "Odometer")
                }
                KmChooser(vm)
                DateTimeField("Start date & time", vm.startTime, { vm.startTime = it })
                DateTimeField("End date & time", vm.endTime, { vm.endTime = it })
                val st = vm.startTime
                val et = vm.endTime
                if (st != null && et != null) {
                    Text(
                        if (et > st) "Total time: ${Fmt.duration(et - st)}" else "End time is before start time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (et > st) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }
            }

            ChargesSection(vm)

            ReceiptsSection(vm)

            // Payment & notes
            SectionCard("Payment & remarks", Icons.Rounded.Payments) {
                Column {
                    FieldLabel("Payment mode (optional)")
                    ChoiceChips(PAYMENT_MODES, vm.paymentMode.takeIf { it.isNotBlank() }, { vm.paymentMode = if (vm.paymentMode == it) "" else it }, { it })
                }
                PlainField("Customer feedback / remarks (optional)", vm.feedback, { vm.feedback = it }, singleLine = false, minLines = 2, leading = Icons.Rounded.EditNote)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TripLinkCard(vm: InvoiceFormViewModel) {
    val t = vm.trip
    var choose by remember { mutableStateOf(false) }
    if (t != null) {
        AppCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.tertiaryContainer, border = null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GpsFixed, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("GPS-tracked trip linked", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        "${Num.clean(vm.gpsKm ?: 0.0)} km · ${Fmt.duration(t.activeMs())} · ${Fmt.time(t.startedAt)}" + (t.endedAt?.let { " – ${Fmt.time(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text("Map invoice styles are available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                IconButton(onClick = { vm.unlinkTrip() }) { Icon(Icons.Rounded.Close, "Remove trip", tint = MaterialTheme.colorScheme.onTertiaryContainer) }
            }
        }
    } else if (vm.uninvoicedTrips.isNotEmpty()) {
        AppCard(Modifier.fillMaxWidth(), onClick = { choose = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GpsFixed, null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Use a tracked trip", style = MaterialTheme.typography.titleSmall)
                    Text("${vm.uninvoicedTrips.size} GPS trip(s) without an invoice. Fills km, times and places.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
            }
        }
    }
    if (choose) {
        AlertDialog(
            onDismissRequest = { choose = false },
            title = { Text("Choose a trip") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    vm.uninvoicedTrips.forEach { trip ->
                        Surface(onClick = { vm.useTrip(trip); choose = false }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${Num.clean(Math.round(trip.km * 10) / 10.0)} km · ${Fmt.duration(trip.activeMs())}", style = MaterialTheme.typography.titleSmall)
                                Text("${Fmt.date(trip.startedAt)}, ${Fmt.time(trip.startedAt)}", style = MaterialTheme.typography.bodySmall)
                                if (trip.endAddress.isNotBlank()) Text("To: ${trip.endAddress}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choose = false }) { Text("Close") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KmChooser(vm: InvoiceFormViewModel) {
    val odo = vm.odometerKm
    val gps = vm.gpsKm
    val options = buildList {
        add(KmSource.ODOMETER)
        if (gps != null) add(KmSource.GPS)
        add(KmSource.MANUAL)
    }
    Column {
        FieldLabel("Km to bill")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, o ->
                val label = when (o) {
                    KmSource.ODOMETER -> "Odometer" + (odo?.let { "\n${Num.clean(it)} km" } ?: "")
                    KmSource.GPS -> "GPS\n${Num.clean(gps ?: 0.0)} km"
                    else -> "Type km"
                }
                SegmentedButton(
                    selected = vm.kmSource == o,
                    onClick = { vm.kmSource = o },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    label = { Text(label, style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                )
            }
        }
        if (vm.kmSource == KmSource.MANUAL) {
            Spacer(Modifier.height(10.dp))
            PlainField("", vm.manualKm, { vm.manualKm = it.filter { c -> c.isDigit() || c == '.' } }, keyboardType = KeyboardType.Decimal, placeholder = "Km to bill", suffix = "km")
        }
        if (vm.kmMismatch) {
            Spacer(Modifier.height(8.dp))
            InfoBanner("GPS and odometer differ by more than 10%. Check which one is right.", container = extra.warningContainer, content = extra.warning)
        }
        Spacer(Modifier.height(6.dp))
        Text("Billed: ${Num.clean(vm.billedKm)} km", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChargesSection(vm: InvoiceFormViewModel) {
    val s = vm.settings
    SectionCard("Charges", Icons.Rounded.CurrencyRupee, subtitle = "A line is printed only if it has an amount") {
        Column {
            FieldLabel("Rate per km")
            val chips = s.rateChips.map { it.toString() } + "other"
            ChoiceChips(chips, vm.rateChoice, { vm.rateChoice = it }, { if (it == "other") "Other" else "₹$it" })
            if (vm.rateChoice == "other") {
                Spacer(Modifier.height(8.dp))
                PlainField("", vm.customRate, { vm.customRate = moneyFilter(it) }, keyboardType = KeyboardType.Decimal, prefix = "₹ ", suffix = "/km", placeholder = "Rate")
            }
            val f = vm.fare
            Spacer(Modifier.height(6.dp))
            Text(
                "Distance fare: ${Num.clean(vm.billedKm)} km × ${Money.short(vm.ratePaise)} = ${Money.format(f.distanceFarePaise)}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlainField("Toll charges", vm.toll, { vm.toll = moneyFilter(it) }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal, prefix = "₹ ")
            PlainField("Parking charges", vm.parking, { vm.parking = moneyFilter(it) }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal, prefix = "₹ ")
        }
        // Waiting
        Column {
            FieldLabel("Waiting time")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(Num.clean(vm.waitingHours), { vm.waitingHours = Fare.roundHalfHour(vm.waitingHours - 0.5) }, { vm.waitingHours = vm.waitingHours + 0.5 }, unit = "hr")
                Spacer(Modifier.width(12.dp))
                MemoryField("Rate / hour", vm.waitingRate, { vm.waitingRate = it.copy(text = moneyFilter(it.text)) }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal, prefix = "₹ ")
            }
            if (s.freeWaitingMinutes > 0) Text("First ${s.freeWaitingMinutes} min free (Settings).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (vm.fare.waitingFarePaise > 0) Text("Waiting charges: ${Money.format(vm.fare.waitingFarePaise)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Night
        Column {
            FieldLabel("Night driver charges")
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlainField("", vm.nightAmount, { vm.nightAmount = moneyFilter(it) }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal, prefix = "₹ ")
                if (vm.nightAmount.isBlank() && s.nightChargePaise > 0) {
                    Spacer(Modifier.width(8.dp))
                    SuggestionChip(onClick = { vm.nightAmount = Money.toInput(s.nightChargePaise) }, label = { Text("Usual ${Money.short(s.nightChargePaise)}") })
                }
            }
        }
        // Allowance
        Column {
            FieldLabel("Driver allowance (outstation)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(vm.allowanceDays.toString(), { vm.allowanceDays = (vm.allowanceDays - 1).coerceAtLeast(0) }, { vm.allowanceDays = vm.allowanceDays + 1 }, unit = if (vm.allowanceDays == 1) "day" else "days")
                Spacer(Modifier.width(12.dp))
                MemoryField("Per day", vm.allowancePerDay, { vm.allowancePerDay = it.copy(text = moneyFilter(it.text)) }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal, prefix = "₹ ")
            }
        }
        // Custom lines
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("Other charges")
            vm.customLines.forEachIndexed { i, line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlainField("", line.label, { line.label = it }, Modifier.weight(1.4f), placeholder = "What for?", capitalization = KeyboardCapitalization.Sentences)
                    Spacer(Modifier.width(8.dp))
                    PlainField("", line.amount, { v -> line.amount = if (v.startsWith("-")) "-" + moneyFilter(v.drop(1)) else moneyFilter(v) }, Modifier.weight(1f), keyboardType = KeyboardType.Text, prefix = "₹ ", placeholder = "0")
                    IconButton(onClick = { vm.customLines.removeAt(i) }) { Icon(Icons.Rounded.Close, "Remove line") }
                }
            }
            OutlinedButton(onClick = { vm.customLines.add(CustomLineInput()) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Add a charge (or discount with −)")
            }
        }
        PlainField("Advance paid", vm.advance, { vm.advance = moneyFilter(it) }, keyboardType = KeyboardType.Decimal, prefix = "₹ ", supporting = "Leave empty if no advance was taken.")
        FareSummary(vm)
    }
}

@Composable
fun FareSummary(vm: InvoiceFormViewModel) {
    val f = vm.fare
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (f.lines.isEmpty()) Text("No charges yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            f.lines.forEach { l ->
                Row {
                    Text(l.label + (l.qtyText?.let { " · $it" } ?: ""), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(Money.format(l.amountPaise), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (f.roundOffPaise != 0L) Row {
                Text("Rounded off", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text((if (f.roundOffPaise > 0) "+" else "−") + Money.format(kotlin.math.abs(f.roundOffPaise)), style = MaterialTheme.typography.bodySmall)
            }
            Row {
                Text("Total fare", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(Money.format(f.totalPaise), style = MaterialTheme.typography.titleMedium)
            }
            if (f.advancePaise > 0) {
                Row {
                    Text("Advance paid", Modifier.weight(1f))
                    Text("− " + Money.format(f.advancePaise))
                }
                Row {
                    Text("Balance due", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(Money.format(f.balanceDuePaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TotalBar(vm: InvoiceFormViewModel, label: String, onClick: () -> Unit, enabled: Boolean = true, busy: Boolean = false) {
    val f = vm.fare
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (f.advancePaise > 0) "Balance due" else "Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(if (f.advancePaise > 0) f.balanceDuePaise else f.totalPaise), style = MaterialTheme.typography.headlineSmall)
                if (f.advancePaise > 0) Text("Total ${Money.format(f.totalPaise)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.height(54.dp), shape = MaterialTheme.shapes.medium) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                }
                Text(label, style = MaterialTheme.typography.titleSmall)
                if (!busy) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            }
        }
    }
}
