package com.amitabhtours.invoice.ui.more

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.App
import com.amitabhtours.invoice.data.AppSettings
import com.amitabhtours.invoice.domain.InvoiceNumbering
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.pdf.MapMode
import com.amitabhtours.invoice.pdf.Qr
import com.amitabhtours.invoice.pdf.ThemeCatalog
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.ChoiceChips
import com.amitabhtours.invoice.ui.components.FieldLabel
import com.amitabhtours.invoice.ui.components.InfoBanner
import com.amitabhtours.invoice.ui.components.PlainField
import com.amitabhtours.invoice.ui.components.SectionCard
import com.amitabhtours.invoice.ui.components.SimpleTopBar
import com.amitabhtours.invoice.ui.components.moneyFilter
import com.amitabhtours.invoice.ui.invoice.ThemeTile
import com.amitabhtours.invoice.ui.theme.extra
import com.amitabhtours.invoice.util.Exporter
import com.amitabhtours.invoice.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(nav: NavHostController) {
    val app = app()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stored by app.settings.flow.collectAsState(initial = null)
    var draft by remember { mutableStateOf<AppSettings?>(null) }
    LaunchedEffect(stored) { if (draft == null && stored != null) draft = stored }
    // Save edits shortly after typing stops.
    LaunchedEffect(draft) {
        val d = draft ?: return@LaunchedEffect
        delay(350)
        app.settings.update { cur -> d.copy(lastTheme = cur.lastTheme, bundledAssetsInstalled = cur.bundledAssetsInstalled, onboardingDone = cur.onboardingDone || d.onboardingDone) }
    }
    val s = draft ?: return
    fun edit(f: (AppSettings) -> AppSettings) { draft = f(s) }

    Scaffold(topBar = { SimpleTopBar("Settings", onBack = { nav.popBackStack() }, subtitle = "Changes are saved automatically") }, contentWindowInsets = WindowInsets.navigationBars) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard("Business details", Icons.Rounded.Business, subtitle = "Printed at the top of every invoice") {
                PlainField("Business name", s.businessName, { v -> edit { it.copy(businessName = v) } }, capitalization = KeyboardCapitalization.Words)
                PlainField("Address", s.address, { v -> edit { it.copy(address = v) } }, singleLine = false, minLines = 2)
                PlainField("City", s.city, { v -> edit { it.copy(city = v) } }, capitalization = KeyboardCapitalization.Words)
                PlainField("Mobile", s.phone, { v -> edit { it.copy(phone = v) } }, keyboardType = KeyboardType.Phone)
                PlainField("Email", s.email, { v -> edit { it.copy(email = v) } }, keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None)
                PlainField("GSTIN (only if registered)", s.gstin, { v -> edit { it.copy(gstin = v.uppercase()) } }, capitalization = KeyboardCapitalization.Characters,
                    supporting = "Leave empty. While empty, invoices say \"GST not applicable\" and never \"Tax Invoice\".")
            }

            SectionCard("Car & driver defaults", Icons.Rounded.DirectionsCar, subtitle = "Used on the first invoice; later the last invoice's values are shown in grey") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlainField("Car No.", s.carNo, { v -> edit { it.copy(carNo = v.uppercase()) } }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
                    PlainField("Vehicle type", s.vehicleType, { v -> edit { it.copy(vehicleType = v) } }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlainField("Driver name", s.driverName, { v -> edit { it.copy(driverName = v) } }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
                    PlainField("Driver mobile", s.driverMobile, { v -> edit { it.copy(driverMobile = v) } }, Modifier.weight(1f), keyboardType = KeyboardType.Phone)
                }
            }

            SectionCard("Rates", Icons.Rounded.CurrencyRupee) {
                Column {
                    FieldLabel("Quick rate buttons (₹ per km)")
                    var newRate by remember { mutableStateOf("") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.rateChips.forEach { r ->
                            OutlinedButton(onClick = { if (s.rateChips.size > 1) edit { it.copy(rateChips = it.rateChips - r) } }) {
                                Text("₹$r")
                                if (s.rateChips.size > 1) { Spacer(Modifier.width(4.dp)); Icon(Icons.Rounded.Close, "Remove", Modifier.size(16.dp)) }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlainField("", newRate, { newRate = it.filter { c -> c.isDigit() }.take(3) }, Modifier.weight(1f), keyboardType = KeyboardType.Number, prefix = "₹ ", placeholder = "Add a rate")
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = {
                            newRate.toIntOrNull()?.takeIf { it > 0 }?.let { r -> edit { it.copy(rateChips = (it.rateChips + r).distinct().sorted()) } }
                            newRate = ""
                        }) { Text("Add") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneySetting("Waiting rate / hour", s.waitingRatePaise, Modifier.weight(1f)) { v -> edit { it.copy(waitingRatePaise = v) } }
                    PlainField("Free waiting (min)", s.freeWaitingMinutes.toString().takeIf { s.freeWaitingMinutes > 0 } ?: "", { v -> edit { it.copy(freeWaitingMinutes = v.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0) } },
                        Modifier.weight(1f), keyboardType = KeyboardType.Number, placeholder = "0")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneySetting("Usual night charge", s.nightChargePaise, Modifier.weight(1f)) { v -> edit { it.copy(nightChargePaise = v) } }
                    MoneySetting("Driver allowance / day", s.allowancePerDayPaise, Modifier.weight(1f)) { v -> edit { it.copy(allowancePerDayPaise = v) } }
                }
            }

            SectionCard("Trip types", Icons.Rounded.Route) {
                var newType by remember { mutableStateOf("") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.tripTypes.forEach { t ->
                        OutlinedButton(onClick = { if (s.tripTypes.size > 1) edit { it.copy(tripTypes = it.tripTypes - t) } }) {
                            Text(t); if (s.tripTypes.size > 1) { Spacer(Modifier.width(4.dp)); Icon(Icons.Rounded.Close, "Remove", Modifier.size(16.dp)) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlainField("", newType, { newType = it }, Modifier.weight(1f), placeholder = "New trip type", capitalization = KeyboardCapitalization.Words)
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = {
                        newType.trim().takeIf { it.isNotEmpty() }?.let { t -> edit { it.copy(tripTypes = (it.tripTypes + t).distinct()) } }
                        newType = ""
                    }) { Icon(Icons.Rounded.Add, null) }
                }
            }

            SectionCard("Invoice numbers", Icons.Rounded.Numbers) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlainField("Prefix", s.invoicePrefix, { v -> edit { it.copy(invoicePrefix = v.uppercase().filter { c -> c.isLetterOrDigit() }.take(6)) } }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Characters)
                    PlainField("Next number", s.nextSeq.toString(), { v ->
                        val n = v.filter { c -> c.isDigit() }.take(5).toIntOrNull() ?: 1
                        edit { it.copy(nextSeq = n.coerceAtLeast(1), counterFy = InvoiceNumbering.financialYearStart(LocalDate.now())) }
                    }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Restart at 0001 every April", style = MaterialTheme.typography.bodyLarge)
                        Text("Financial year April–March", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = s.fyReset, onCheckedChange = { v -> edit { it.copy(fyReset = v) } })
                }
                Text(
                    "Next invoice: " + InvoiceNumbering.preview(s.invoicePrefix, InvoiceNumbering.Counter(s.counterFy, s.nextSeq), LocalDate.now(), s.fyReset),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text("To continue from your paper bill book, set the next number here. Numbers are never reused.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            DefaultThemeSection(app, s) { id -> edit { it.copy(defaultTheme = id) } }

            SectionCard("UPI payment QR (optional)", Icons.Rounded.QrCode2, subtitle = "Printed on unpaid invoices with the exact amount filled in") {
                ToggleRow("Show UPI QR on invoices", s.upiEnabled) { v -> edit { it.copy(upiEnabled = v) } }
                if (s.upiEnabled) {
                    PlainField("UPI ID", s.upiId, { v -> edit { it.copy(upiId = v.trim()) } }, capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Email,
                        isError = s.upiId.isNotBlank() && !s.upiId.contains("@"), supporting = "Example: name@okhdfcbank")
                    PlainField("Payee name (as the bank shows it)", s.upiName, { v -> edit { it.copy(upiName = v) } }, capitalization = KeyboardCapitalization.Words)
                    if (s.upiReady) {
                        val qr = remember(s.upiId, s.upiName) { Qr.bitmap(Qr.upiUri(s.upiId, s.upiName, 100, "Test"), 6).asImageBitmap() }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(qr, "Test QR", Modifier.size(110.dp).clip(MaterialTheme.shapes.small).background(Color.White))
                            Spacer(Modifier.width(12.dp))
                            Text("Test: scan this with any UPI app. It should show ${s.upiName.ifBlank { "your name" }} and ₹1.00. Don't pay — just check the name.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            SectionCard("Signatures & stamp (optional)", Icons.Rounded.Draw) {
                ToggleRow("Customer signs on the phone", s.customerSignatureEnabled, "Adds a signature step before choosing the style") { v -> edit { it.copy(customerSignatureEnabled = v) } }
                ImageSetting(
                    app, "Owner signature", s.ownerSignatureEnabled, s.ownerSignaturePath, "owner_signature",
                    onToggle = { v -> edit { it.copy(ownerSignatureEnabled = v) } },
                    onPath = { p -> edit { it.copy(ownerSignaturePath = p, ownerSignatureEnabled = p != null) } },
                )
                ImageSetting(
                    app, "Business stamp", s.ownerStampEnabled, s.ownerStampPath, "owner_stamp",
                    onToggle = { v -> edit { it.copy(ownerStampEnabled = v) } },
                    onPath = { p -> edit { it.copy(ownerStampPath = p, ownerStampEnabled = p != null) } },
                )
            }

            SectionCard("Invoice footer", Icons.Rounded.Notes) {
                PlainField("Thank-you line", s.footerText, { v -> edit { it.copy(footerText = v) } }, singleLine = false)
                PlainField("GST note", s.gstNote, { v -> edit { it.copy(gstNote = v) } }, singleLine = false, supporting = "Printed only while GSTIN is empty.")
                ToggleRow("Attach receipts to the PDF by default", s.attachReceiptsDefault) { v -> edit { it.copy(attachReceiptsDefault = v) } }
            }

            SectionCard("Appearance", Icons.Rounded.Brightness6) {
                ChoiceChips(listOf("system", "light", "dark"), s.darkMode, { v -> edit { it.copy(darkMode = v) } }, {
                    when (it) { "light" -> "Light"; "dark" -> "Dark"; else -> "Same as phone" }
                })
            }

            DataSection(app)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoneySetting(label: String, paise: Long, modifier: Modifier, onChange: (Long) -> Unit) {
    var text by remember { mutableStateOf(Money.toInput(paise.takeIf { it > 0 })) }
    PlainField(label, text, { v -> text = moneyFilter(v); onChange(Money.parse(text) ?: 0) }, modifier, keyboardType = KeyboardType.Decimal, prefix = "₹ ")
}

@Composable
fun ToggleRow(title: String, checked: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ImageSetting(
    app: App,
    title: String,
    enabled: Boolean,
    path: String?,
    baseName: String,
    onToggle: (Boolean) -> Unit,
    onPath: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val img by produceState<ImageBitmap?>(null, path) {
        value = path?.let { p -> withContext(Dispatchers.IO) { Images.decodeSampled(p, 600)?.asImageBitmap() } }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val dest = File(app.repo.brandingDir, "${baseName}_${System.currentTimeMillis()}.png")
            val ok = withContext(Dispatchers.IO) {
                val tmp = File(app.cacheDir, "pick.tmp")
                if (!Images.copyUriToFile(context, uri, tmp)) return@withContext false
                val bmp = Images.decodeSampled(tmp.absolutePath, 1600) ?: return@withContext false
                Images.savePng(Images.removeBackground(bmp), dest)
                true
            }
            busy = false
            if (ok) onPath(dest.absolutePath) else Toast.makeText(context, "Could not read that image", Toast.LENGTH_SHORT).show()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleRow(title, enabled && path != null, if (path == null) "No image yet" else "Printed above \"Authorised Signatory\"") { v -> if (path != null) onToggle(v) }
        img?.let {
            Box(
                Modifier.fillMaxWidth().height(110.dp).clip(MaterialTheme.shapes.medium)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Image(it, title, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy) {
                Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(if (path == null) "Choose photo" else "Replace")
            }
            if (path != null) OutlinedButton(onClick = { onPath(null) }) { Text("Remove") }
        }
        Text(
            "Photograph it on plain white paper. The white background is removed automatically.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (busy) Text("Processing…", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DefaultThemeSection(app: App, s: AppSettings, onPick: (Int) -> Unit) {
    val thumbs = remember { mutableStateMapOf<Int, ImageBitmap>() }
    LaunchedEffect(s.ownerSignaturePath, s.upiEnabled, s.businessName) {
        withContext(Dispatchers.Default) {
            val doc = app.pdf.sampleDoc(s, withRoute = true)
            ThemeCatalog.all.forEach { t -> runCatching { app.pdf.thumbnail(doc, t, 300) }.getOrNull()?.let { thumbs[t.id] = it.asImageBitmap() } }
        }
    }
    SectionCard("Default invoice style", Icons.Rounded.Palette, subtitle = "Pre-selected for new invoices (the last style used is remembered)") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ThemeCatalog.all.filter { it.mapMode != MapMode.REQUIRED }, key = { it.id }) { t ->
                Box(Modifier.width(140.dp)) {
                    ThemeTile(t.name, t.tagline, thumbs[t.id], s.defaultTheme == t.id, locked = false, mapBadge = t.mapMode != MapMode.NONE) { onPick(t.id) }
                }
            }
        }
        Text("Map styles (Ride Receipt, Route Map) can be chosen for GPS-tracked trips.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DataSection(app: App) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val r = runCatching { Exporter.writeCsv(app, context.contentResolver.openOutputStream(uri)!!) }
            busy = false
            Toast.makeText(context, if (r.isSuccess) "Invoices exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val zip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val r = runCatching { Exporter.writePdfZip(app, context.contentResolver.openOutputStream(uri)!!) }
            busy = false
            Toast.makeText(context, if (r.isSuccess) "${r.getOrNull()} PDFs exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    SectionCard("Your data", Icons.Rounded.Storage) {
        InfoBanner(
            "Your data is stored only on this phone. If the phone is lost or reset, the data cannot be recovered. Export a copy now and then.",
            container = extra.warningContainer, content = extra.warning,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { csv.launch("Amitabh-Tours-invoices-${LocalDate.now()}.csv") }, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.TableChart, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Export list (CSV)")
            }
            FilledTonalButton(onClick = { zip.launch("Amitabh-Tours-invoice-PDFs-${LocalDate.now()}.zip") }, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.FolderZip, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Export all PDFs")
            }
        }
        if (busy) Text("Working…", style = MaterialTheme.typography.bodySmall)
    }
}
