package com.amitabhtours.invoice.ui.invoice

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.App
import com.amitabhtours.invoice.data.AttachmentEntity
import com.amitabhtours.invoice.data.AttachmentType
import com.amitabhtours.invoice.data.InvoiceEntity
import com.amitabhtours.invoice.data.PAYMENT_MODES
import com.amitabhtours.invoice.domain.CustomLine
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.pdf.MapMode
import com.amitabhtours.invoice.pdf.ThemeCatalog
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.appViewModel
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.ChoiceChips
import com.amitabhtours.invoice.ui.components.IconCircle
import com.amitabhtours.invoice.ui.components.InvoiceStatusPill
import com.amitabhtours.invoice.ui.components.PlainField
import com.amitabhtours.invoice.ui.components.SimpleTopBar
import com.amitabhtours.invoice.ui.theme.extra
import com.amitabhtours.invoice.util.PdfActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class InvoiceDetailViewModel(private val app: App, val id: Long) : ViewModel() {
    val invoice = app.db.invoices().observe(id)
    val attachments = app.db.attachments().observeForInvoice(id)
    var pages by mutableStateOf<List<ImageBitmap>>(emptyList()); private set
    var busy by mutableStateOf(false); private set
    var file by mutableStateOf<File?>(null); private set
    val themeThumbs = mutableStateMapOf<Int, ImageBitmap>()
    var hasRoute by mutableStateOf(false); private set

    init {
        viewModelScope.launch {
            val inv = app.db.invoices().get(id) ?: return@launch
            hasRoute = inv.tripId?.let { app.db.trips().points(it).size >= 2 } ?: false
            val f = inv.pdfPath?.let { File(it) }
            if (f == null || !f.exists()) rerender(null) else load(f)
        }
    }

    private suspend fun load(f: File) {
        file = f
        pages = withContext(Dispatchers.IO) { runCatching { PdfActions.renderPages(f, 1100) }.getOrDefault(emptyList()) }.map { it.asImageBitmap() }
    }

    fun rerender(themeId: Int?, after: (() -> Unit)? = null) {
        viewModelScope.launch {
            busy = true
            try {
                val f = app.pdf.render(id, themeId)
                load(f)
                after?.invoke()
            } finally {
                busy = false
            }
        }
    }

    fun loadThemeThumbs() {
        if (themeThumbs.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            val inv = app.db.invoices().get(id) ?: return@launch
            val s = app.settings.get()
            val charges = app.db.charges().forInvoice(id).map { CustomLine(it.label, it.amountPaise) }
            val doc = app.pdf.buildDoc(inv, charges, s, online = false)
            ThemeCatalog.all.forEach { t ->
                runCatching { app.pdf.thumbnail(doc, t, 360) }.getOrNull()?.let { themeThumbs[t.id] = it.asImageBitmap() }
            }
        }
    }

    fun markPaid(mode: String) = viewModelScope.launch { busy = true; app.repo.markPaid(id, mode); app.db.invoices().get(id)?.pdfPath?.let { load(File(it)) }; busy = false }
    fun markUnpaid() = viewModelScope.launch { busy = true; app.repo.markUnpaid(id); app.db.invoices().get(id)?.pdfPath?.let { load(File(it)) }; busy = false }
    fun cancel(reason: String) = viewModelScope.launch { busy = true; app.repo.cancelInvoice(id, reason); app.db.invoices().get(id)?.pdfPath?.let { load(File(it)) }; busy = false }

    suspend fun businessName() = app.settings.flow.first().businessName
}

@Composable
fun InvoiceDetailScreen(nav: NavHostController, id: Long) {
    val vm = appViewModel(key = "inv-$id") { InvoiceDetailViewModel(it, id) }
    val inv by vm.invoice.collectAsState(initial = null)
    val atts by vm.attachments.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoomPage by remember { mutableStateOf<ImageBitmap?>(null) }
    var showThemes by remember { mutableStateOf(false) }
    var showPaid by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }

    val storagePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val f = vm.file
        if (ok && f != null) runCatching { PdfActions.saveToDownloads(context, f) }
            .onSuccess { Toast.makeText(context, "Saved to $it", Toast.LENGTH_LONG).show() }
            .onFailure { Toast.makeText(context, "Could not save: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    val current = inv
    Scaffold(
        topBar = {
            SimpleTopBar(current?.number ?: "Invoice", onBack = { nav.popBackStack() }, subtitle = current?.customerName?.ifBlank { null })
        },
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (current != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val f = vm.file
                        Button(
                            onClick = { if (f != null) scope.launch { PdfActions.share(context, f, current, vm.businessName()) } },
                            enabled = f != null && !vm.busy,
                            modifier = Modifier.weight(1.4f).height(54.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) { Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
                        FilledTonalButton(
                            onClick = {
                                if (f == null) return@FilledTonalButton
                                if (Build.VERSION.SDK_INT < 29) storagePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                else runCatching { PdfActions.saveToDownloads(context, f) }
                                    .onSuccess { Toast.makeText(context, "Saved to $it", Toast.LENGTH_LONG).show() }
                                    .onFailure { Toast.makeText(context, "Could not save: ${it.message}", Toast.LENGTH_LONG).show() }
                            },
                            enabled = f != null && !vm.busy,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(4.dp)); Text("Save") }
                        FilledTonalButton(
                            onClick = { if (f != null) PdfActions.print(context, f, current.number) },
                            enabled = f != null && !vm.busy,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { Icon(Icons.Rounded.Print, null); Spacer(Modifier.width(4.dp)); Text("Print") }
                    }
                }
            }
        },
    ) { pad ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { StatusCard(current, onPaid = { showPaid = true }, onUnpaid = { vm.markUnpaid() }) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Invoice PDF", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.loadThemeThumbs(); showThemes = true }, enabled = !vm.busy) {
                        Icon(Icons.Rounded.Palette, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text("Style: ${ThemeCatalog.byId(current.themeId).name}")
                    }
                }
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            items(vm.pages) { page ->
                Image(
                    page, "Invoice page",
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Color.White).clickable { zoomPage = page },
                    contentScale = ContentScale.FillWidth,
                )
            }
            if (vm.pages.isEmpty() && !vm.busy) item { Text("Preparing preview…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Text("Tap a page to zoom.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp)) {
                    current.customerId?.let { cid ->
                        LinkRow(Icons.Rounded.Person, "Customer", current.customerName.ifBlank { current.customerMobile }) { nav.navigate(Routes.customer(cid)) }
                    }
                    current.tripId?.let { tid ->
                        LinkRow(Icons.Rounded.GpsFixed, "GPS trip", "${current.gpsKm?.let { "$it km" } ?: ""} · view route") { nav.navigate(Routes.trip(tid)) }
                    }
                    atts.forEach { a -> AttachmentInfo(a) }
                }
            }
            if (!current.isCancelled) {
                item {
                    TextButton(onClick = { showCancel = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Rounded.Block, null); Spacer(Modifier.width(6.dp)); Text("Cancel this invoice")
                    }
                }
            } else {
                item {
                    Text(
                        "Cancelled on ${current.cancelledAt?.let { Fmt.dateTime(it) } ?: ""}" + (current.cancelReason?.let { " — $it" } ?: "") +
                            ". The number stays used and it is not counted in earnings.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    zoomPage?.let { page -> ZoomDialog(page) { zoomPage = null } }

    if (showThemes && current != null) {
        AlertDialog(
            onDismissRequest = { showThemes = false },
            title = { Text("Change style") },
            text = {
                Column {
                    Text("Same invoice number and details, new look.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(GridCells.Fixed(2), Modifier.heightIn(max = 520.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ThemeCatalog.all, key = { it.id }) { t ->
                            val locked = t.mapMode == MapMode.REQUIRED && !vm.hasRoute
                            ThemeTile(t.name, if (locked) "Needs a GPS-tracked trip" else t.tagline, vm.themeThumbs[t.id], current.themeId == t.id, locked, t.mapMode != MapMode.NONE) {
                                if (!locked) { showThemes = false; vm.rerender(t.id) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemes = false }) { Text("Close") } },
        )
    }

    if (showPaid && current != null) {
        var mode by remember { mutableStateOf(current.paymentMode.ifBlank { "Cash" }) }
        AlertDialog(
            onDismissRequest = { showPaid = false },
            title = { Text("Mark as paid") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Balance of ${Money.format(current.balanceDuePaise)} received today. How was it paid?")
                    ChoiceChips(PAYMENT_MODES, mode, { mode = it }, { it })
                }
            },
            confirmButton = { TextButton(onClick = { showPaid = false; vm.markPaid(mode) }) { Text("Mark paid") } },
            dismissButton = { TextButton(onClick = { showPaid = false }) { Text("Cancel") } },
        )
    }

    if (showCancel) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text("Cancel this invoice?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("It stays in History with a CANCELLED stamp and is removed from earnings. The number is not reused. This cannot be undone.")
                    PlainField("Reason (optional)", reason, { reason = it })
                }
            },
            confirmButton = { TextButton(onClick = { showCancel = false; vm.cancel(reason) }) { Text("Cancel invoice", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun StatusCard(inv: InvoiceEntity, onPaid: () -> Unit, onUnpaid: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    Money.format(inv.totalPaise), style = MaterialTheme.typography.headlineMedium,
                    textDecoration = if (inv.isCancelled) TextDecoration.LineThrough else null,
                )
                Text(Fmt.dateTime(inv.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            InvoiceStatusPill(inv)
        }
        if (!inv.isCancelled) {
            if (inv.advancePaise > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Advance ${Money.format(inv.advancePaise)} · Balance ${Money.format(inv.balanceDuePaise)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (inv.outstandingPaise > 0) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onPaid, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = extra.success, contentColor = Color.White)) {
                    Icon(Icons.Rounded.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("Mark ${Money.format(inv.outstandingPaise)} as paid")
                }
            } else if (inv.paidAt != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paid on ${Fmt.date(inv.paidAt)}${inv.paidMode?.let { " via $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium, color = extra.success, modifier = Modifier.weight(1f))
                    TextButton(onClick = onUnpaid) { Text("Undo") }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
    }
}

@Composable
private fun AttachmentInfo(a: AttachmentEntity) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircle(Icons.Rounded.Receipt, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant, 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(AttachmentType.label(a.type) + (a.amountPaise?.let { " — ${Money.format(it)}" } ?: ""), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ZoomDialog(page: ImageBitmap, onClose: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var ox by remember { mutableStateOf(0f) }
    var oy by remember { mutableStateOf(0f) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
            Image(
                page, "Zoomed page",
                modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            ox += pan.x
                            oy += pan.y
                        }
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = ox; translationY = oy },
                contentScale = ContentScale.Fit,
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp)) {
                Icon(Icons.Rounded.Close, "Close", tint = Color.White)
            }
        }
    }
}
