package com.amitabhtours.invoice.ui.invoice

import android.graphics.Bitmap
import android.graphics.Paint as APaint
import android.graphics.Path as APath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.amitabhtours.invoice.data.AttachmentType
import com.amitabhtours.invoice.data.DraftAttachment
import com.amitabhtours.invoice.data.KmSource
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.pdf.MapMode
import com.amitabhtours.invoice.pdf.ThemeCatalog
import com.amitabhtours.invoice.ui.Routes
import com.amitabhtours.invoice.ui.components.AppCard
import com.amitabhtours.invoice.ui.components.ChoiceChips
import com.amitabhtours.invoice.ui.components.InfoBanner
import com.amitabhtours.invoice.ui.components.KeyValue
import com.amitabhtours.invoice.ui.components.PlainField
import com.amitabhtours.invoice.ui.components.SectionCard
import com.amitabhtours.invoice.ui.components.SimpleTopBar
import com.amitabhtours.invoice.ui.components.moneyFilter
import com.amitabhtours.invoice.ui.theme.extra
import com.amitabhtours.invoice.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ------------------------------------------------------------------------------------ Receipts

@Composable
fun ReceiptsSection(vm: InvoiceFormViewModel) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<String, String>?>(null) } // path, mime
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null && f.exists() && f.length() > 0) pending = f.absolutePath to "image/jpeg" else f?.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = if (mime.contains("png")) "png" else "jpg"
            val dest = vm.newAttachmentFile(ext)
            if (Images.copyUriToFile(context, uri, dest)) pending = dest.absolutePath to mime
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val dest = vm.newAttachmentFile("pdf")
            if (Images.copyUriToFile(context, uri, dest)) pending = dest.absolutePath to "application/pdf"
        }
    }

    SectionCard("Receipts (optional)", Icons.Rounded.AttachFile, subtitle = "Toll bills, parking tickets — added to the end of the PDF") {
        vm.attachments.forEach { a -> AttachmentRow(a, onRemove = { vm.removeAttachment(a) }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                val f = vm.newAttachmentFile("jpg")
                cameraFile = f
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
                camera.launch(uri)
            }, modifier = Modifier.weight(1f).height(52.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Rounded.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Camera")
            }
            FilledTonalButton(onClick = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, modifier = Modifier.weight(1f).height(52.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Gallery")
            }
            FilledTonalButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, modifier = Modifier.weight(1f).height(52.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Rounded.PictureAsPdf, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("PDF")
            }
        }
    }

    pending?.let { (path, mime) ->
        var type by remember(path) { mutableStateOf(AttachmentType.TOLL) }
        var amount by remember(path) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("What is this receipt?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChoiceChips(listOf(AttachmentType.TOLL, AttachmentType.PARKING, AttachmentType.OTHER), type, { type = it }, {
                        when (it) { AttachmentType.TOLL -> "Toll"; AttachmentType.PARKING -> "Parking"; else -> "Other" }
                    })
                    PlainField("Amount on the receipt (optional)", amount, { amount = moneyFilter(it) }, keyboardType = KeyboardType.Decimal, prefix = "₹ ",
                        supporting = if (type != AttachmentType.OTHER) "This amount is added to the ${if (type == AttachmentType.TOLL) "toll" else "parking"} charges." else "Shown under the receipt in the PDF.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addAttachment(DraftAttachment(type, mime, path, Money.parse(amount)?.takeIf { it > 0 }))
                    pending = null
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { File(path).delete(); pending = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AttachmentRow(a: DraftAttachment, onRemove: () -> Unit) {
    val thumb by produceState<ImageBitmap?>(null, a.path) {
        value = if (a.mime.startsWith("image")) withContext(Dispatchers.IO) { Images.decodeSampled(a.path, 160)?.asImageBitmap() } else null
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            val t = thumb
            if (t != null) Image(t, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(if (a.mime == "application/pdf") Icons.Rounded.PictureAsPdf else Icons.Rounded.Image, null)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(AttachmentType.label(a.type), style = MaterialTheme.typography.titleSmall)
            Text(a.amountPaise?.let { Money.format(it) } ?: "No amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove receipt") }
    }
}

// ------------------------------------------------------------------------------------ Review

@Composable
fun ReviewStep(vm: InvoiceFormViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val f = vm.fare
    val carried = vm.carriedOver
    val e = vm.draftEntity()
    Scaffold(
        topBar = { SimpleTopBar("Check the details", onBack = onBack, subtitle = vm.previewNumber) },
        bottomBar = { TotalBar(vm, if (vm.settings.customerSignatureEnabled) "Customer signs" else "Choose style", onNext) },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (carried.isNotEmpty()) {
                InfoBanner(
                    "Same as last invoice — check: ${carried.joinToString(", ")}. These are highlighted below.",
                    icon = Icons.Rounded.Warning,
                    container = extra.warningContainer, content = extra.warning,
                )
            }
            vm.warnings.forEach { w -> InfoBanner(w, icon = Icons.Rounded.Warning, container = extra.dangerContainer, content = extra.danger) }

            ReviewCard("Customer", onBack) {
                KeyValue("Name", e.customerName, highlight = vm.customerName.usingHint)
                KeyValue("Mobile", e.customerMobile, highlight = vm.customerMobile.usingHint)
                KeyValue("Pickup", e.pickup, highlight = vm.pickup.usingHint)
                KeyValue("Drop", e.dropAt, highlight = vm.drop.usingHint)
            }
            ReviewCard("Vehicle & driver", onBack) {
                KeyValue("Car No.", e.carNo)
                KeyValue("Vehicle type", e.vehicleType)
                KeyValue("Driver", listOf(e.driverName, e.driverMobile).filter { it.isNotBlank() }.joinToString(" · "))
            }
            ReviewCard("Trip", onBack) {
                KeyValue("Trip type", e.tripType)
                if (e.startKm != null || e.endKm != null) KeyValue("Odometer", "${e.startKm?.let { Num.clean(it) } ?: "—"} → ${e.endKm?.let { Num.clean(it) } ?: "—"}")
                vm.gpsKm?.let { KeyValue("GPS distance", "${Num.clean(it)} km") }
                KeyValue("Billed km", "${Num.clean(e.billedKm)} km (${KmSource.label(e.billedKmSource)})", strong = true)
                e.startTime?.let { KeyValue("Start", Fmt.dateTime(it)) }
                e.endTime?.let { KeyValue("End", Fmt.dateTime(it)) }
            }
            ReviewCard("Charges", onBack) {
                FareSummary(vm)
                Spacer(Modifier.height(4.dp))
                Text(com.amitabhtours.invoice.domain.AmountInWords.rupees(if (f.advancePaise > 0) f.balanceDuePaise else f.totalPaise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ReviewCard("Payment & extras", onBack) {
                KeyValue("Payment mode", vm.paymentMode.ifBlank { "Not set" })
                if (vm.feedback.isNotBlank()) KeyValue("Remarks", vm.feedback)
                KeyValue("Receipts", if (vm.attachments.isEmpty()) "None" else "${vm.attachments.size} attached")
                KeyValue("UPI QR on invoice", if (vm.settings.upiReady && f.balanceDuePaise > 0) "Yes (${Money.format(f.balanceDuePaise)})" else "No")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReviewCard(title: String, onEdit: () -> Unit, content: @Composable () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") }
        }
        content()
    }
}

// ------------------------------------------------------------------------------------ Signature

@Composable
fun SignatureStep(vm: InvoiceFormViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val ink = Color(0xFF14286B)
    Scaffold(
        topBar = { SimpleTopBar("Customer signature", onBack = onBack, subtitle = "Ask the customer to sign in the box") },
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = { strokes.clear(); current = emptyList() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Clear") }
                OutlinedButton(onClick = { vm.customerSignaturePath = null; onNext() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Skip") }
                Button(
                    onClick = {
                        if (strokes.isNotEmpty() && size.width > 0) vm.saveSignature(exportSignature(strokes, size))
                        onNext()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = strokes.isNotEmpty(),
                ) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(4.dp)); Text("Done") }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large).background(Color.White)
                    .border(2.dp, extra.cardBorder, MaterialTheme.shapes.large)
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { current = listOf(it) },
                            onDrag = { change, _ -> current = current + change.position },
                            onDragEnd = { if (current.size > 1) strokes.add(current); current = emptyList() },
                            onDragCancel = { current = emptyList() },
                        )
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    (strokes + listOf(current)).forEach { pts ->
                        if (pts.size > 1) {
                            val p = Path().apply {
                                moveTo(pts[0].x, pts[0].y)
                                for (i in 1 until pts.size) {
                                    val prev = pts[i - 1]
                                    val cur = pts[i]
                                    quadraticTo(prev.x, prev.y, (prev.x + cur.x) / 2, (prev.y + cur.y) / 2)
                                }
                                lineTo(pts.last().x, pts.last().y)
                            }
                            drawPath(p, ink, style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                    val y = size.height * 0.72f
                    drawLine(Color(0xFFCCD2DD), Offset(40f, y), Offset(size.width - 40f, y), strokeWidth = 3f)
                }
                if (strokes.isEmpty() && current.isEmpty()) {
                    Text("Sign here", color = Color(0xFF9AA1AE), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Tip: turn the phone sideways for more room.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun exportSignature(strokes: List<List<Offset>>, size: IntSize): Bitmap {
    val all = strokes.flatten()
    val pad = 16f
    val minX = (all.minOf { it.x } - pad).coerceAtLeast(0f)
    val minY = (all.minOf { it.y } - pad).coerceAtLeast(0f)
    val maxX = (all.maxOf { it.x } + pad).coerceAtMost(size.width.toFloat())
    val maxY = (all.maxOf { it.y } + pad).coerceAtMost(size.height.toFloat())
    val w = (maxX - minX).toInt().coerceAtLeast(10)
    val h = (maxY - minY).toInt().coerceAtLeast(10)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val paint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#14286B")
        style = APaint.Style.STROKE
        strokeWidth = 7f
        strokeCap = APaint.Cap.ROUND
        strokeJoin = APaint.Join.ROUND
    }
    strokes.forEach { pts ->
        val p = APath()
        p.moveTo(pts[0].x - minX, pts[0].y - minY)
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val cur = pts[i]
            p.quadTo(prev.x - minX, prev.y - minY, (prev.x + cur.x) / 2 - minX, (prev.y + cur.y) / 2 - minY)
        }
        c.drawPath(p, paint)
    }
    return bmp
}

// ------------------------------------------------------------------------------------ Theme picker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStep(vm: InvoiceFormViewModel, nav: NavHostController, onBack: () -> Unit, onDone: (Long) -> Unit) {
    val hasRoute = vm.hasRoute()
    var lockedInfo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.renderThumbnails(420)
        val current = ThemeCatalog.byId(vm.themeId)
        if (current.mapMode == MapMode.REQUIRED && !hasRoute) vm.themeId = vm.settings.defaultTheme.let { if (ThemeCatalog.byId(it).mapMode == MapMode.REQUIRED) 5 else it }
    }
    Scaffold(
        topBar = { SimpleTopBar("Choose a style", onBack = onBack, subtitle = ThemeCatalog.byId(vm.themeId).name) },
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                    if (vm.attachments.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Attach ${vm.attachments.size} receipt(s) to the PDF", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = vm.attachReceipts, onCheckedChange = { vm.attachReceipts = it })
                        }
                    }
                    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                    TotalBar(vm, if (vm.saving) "Creating…" else "Create invoice", onClick = { vm.generate(onDone) }, busy = vm.saving)
                }
            }
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!hasRoute) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InfoBanner("Map styles are locked because this trip was not tracked with GPS.", icon = Icons.Rounded.Lock, action = {
                        TextButton(onClick = { nav.navigate(Routes.help("tracking")) }) { Text("How?") }
                    })
                }
            }
            items(ThemeCatalog.all, key = { it.id }) { t ->
                val locked = t.mapMode == MapMode.REQUIRED && !hasRoute
                ThemeTile(
                    name = t.name,
                    tagline = if (locked) "Track the trip with GPS to use this style" else t.tagline,
                    thumb = vm.thumbnails[t.id],
                    selected = vm.themeId == t.id,
                    locked = locked,
                    mapBadge = t.mapMode != MapMode.NONE,
                    onClick = { if (locked) lockedInfo = true else vm.themeId = t.id },
                )
            }
        }
    }
    if (lockedInfo) {
        AlertDialog(
            onDismissRequest = { lockedInfo = false },
            icon = { Icon(Icons.Rounded.Lock, null) },
            title = { Text("This style needs a GPS-tracked trip") },
            text = { Text("Map styles show the route you drove. Next time, tap \"Start trip tracking\" on the Home screen when the customer gets in, and \"End trip\" at the drop. Then create the invoice from that trip.") },
            confirmButton = { TextButton(onClick = { lockedInfo = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { lockedInfo = false; nav.navigate(Routes.help("tracking")) }) { Text("Read more") } },
        )
    }
}

@Composable
fun ThemeTile(
    name: String,
    tagline: String,
    thumb: ImageBitmap?,
    selected: Boolean,
    locked: Boolean,
    mapBadge: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else extra.cardBorder
    Column(
        Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, borderColor), MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(595f / 842f).clip(MaterialTheme.shapes.small).background(Color.White)) {
            if (thumb != null) {
                Image(thumb, name, modifier = Modifier.fillMaxSize().alpha(if (locked) 0.35f else 1f), contentScale = ContentScale.Fit)
            } else {
                CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), strokeWidth = 2.dp)
            }
            if (locked) {
                Box(Modifier.align(Alignment.Center).size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.inverseSurface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, "Locked", tint = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).background(Color.White, CircleShape))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            if (mapBadge) Text("MAP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        }
        Text(tagline, style = MaterialTheme.typography.bodySmall, color = if (locked) extra.warning else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2, textAlign = TextAlign.Start)
    }
}
