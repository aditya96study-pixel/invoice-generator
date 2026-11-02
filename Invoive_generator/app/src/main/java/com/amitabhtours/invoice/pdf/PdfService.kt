package com.amitabhtours.invoice.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import com.amitabhtours.invoice.data.AppDatabase
import com.amitabhtours.invoice.data.AppSettings
import com.amitabhtours.invoice.data.AttachmentEntity
import com.amitabhtours.invoice.data.AttachmentType
import com.amitabhtours.invoice.data.InvoiceEntity
import com.amitabhtours.invoice.data.KmSource
import com.amitabhtours.invoice.data.SettingsStore
import com.amitabhtours.invoice.domain.AmountInWords
import com.amitabhtours.invoice.domain.CustomLine
import com.amitabhtours.invoice.domain.Fare
import com.amitabhtours.invoice.domain.FareInput
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfService(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {
    private val mapRenderer = MapRenderer(context)
    private val dir = File(context.filesDir, "invoices").apply { mkdirs() }

    /** Renders a saved invoice to its PDF file and records the theme used. */
    suspend fun render(invoiceId: Long, themeId: Int? = null): File = withContext(Dispatchers.IO) {
        val inv = db.invoices().get(invoiceId) ?: error("Invoice not found")
        val s = settings.get()
        val charges = db.charges().forInvoice(inv.id).map { CustomLine(it.label, it.amountPaise) }
        val doc = buildDoc(inv, charges, s, online = true)
        val requested = ThemeCatalog.byId(themeId ?: inv.themeId)
        val theme = if (requested.isAvailable(doc)) requested else ThemeCatalog.byId(5)
        val atts = if (inv.attachReceipts) db.attachments().forInvoice(inv.id) else emptyList()
        val file = File(dir, "${inv.number}.pdf")
        write(doc, theme, atts, file)
        db.invoices().update(inv.copy(themeId = theme.id, pdfPath = file.absolutePath))
        file
    }

    fun fileFor(inv: InvoiceEntity): File = File(dir, "${inv.number}.pdf")

    private fun write(doc: InvoiceDoc, theme: InvoiceTheme, attachments: List<AttachmentEntity>, file: File) {
        val dry = DryRunSink()
        theme.draw(PageWriter(dry, doc, theme, 1))
        val pdf = PdfDocument()
        try {
            theme.draw(PageWriter(PdfSink(pdf), doc, theme, dry.pages))
            Annexure.append(pdf, doc, attachments, dry.pages + 1)
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.outputStream().use { pdf.writeTo(it) }
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        } finally {
            pdf.close()
        }
    }

    /** Writes [doc] with [theme] to [file] (no receipts). Used for samples and checks. */
    fun writeSample(doc: InvoiceDoc, theme: InvoiceTheme, file: File) = write(doc, theme, emptyList(), file)

    /** First page as a bitmap, for the theme picker. Maps are drawn offline (fast). */
    fun thumbnail(doc: InvoiceDoc, theme: InvoiceTheme, widthPx: Int): Bitmap {
        val sink = BitmapSink(widthPx)
        theme.draw(PageWriter(sink, doc, theme, 1))
        return sink.bitmap
    }

    suspend fun buildDoc(
        inv: InvoiceEntity,
        charges: List<CustomLine>,
        s: AppSettings,
        online: Boolean,
        customerSignature: Bitmap? = null,
    ): InvoiceDoc = withContext(Dispatchers.IO) {
        val fare = Fare.calculate(
            FareInput(
                billedKm = inv.billedKm,
                ratePerKmPaise = inv.ratePerKmPaise,
                tollPaise = inv.tollPaise,
                parkingPaise = inv.parkingPaise,
                waitingHours = inv.waitingHours,
                waitingRatePaise = inv.waitingRatePaise,
                freeWaitingHours = inv.freeWaitingHours,
                nightPaise = inv.nightPaise,
                allowanceDays = inv.allowanceDays,
                allowancePerDayPaise = inv.allowancePerDayPaise,
                customLines = charges,
                advancePaise = inv.advancePaise,
            )
        )
        val showQr = s.upiReady && !inv.isCancelled && inv.paidAt == null && fare.balanceDuePaise > 0
        val qr = if (showQr) Qr.bitmap(Qr.upiUri(s.upiId, s.upiName.ifBlank { s.businessName }, fare.balanceDuePaise, "Invoice ${inv.number}")) else null
        val custSig = customerSignature ?: inv.customerSignaturePath?.let { Images.decodeSampled(it, 900) }
        val ownerSig = if (s.ownerSignatureEnabled) s.ownerSignaturePath?.let { Images.decodeSampled(it, 1000) } else null
        val stamp = if (s.ownerStampEnabled) s.ownerStampPath?.let { Images.decodeSampled(it, 700) } else null

        var route: List<LatLng>? = null
        var activeMs: Long? = null
        inv.tripId?.let { tripId ->
            val trip = db.trips().get(tripId)
            val pts = db.trips().points(tripId)
            if (pts.size >= 2) {
                val step = maxOf(1, pts.size / 1500)
                route = pts.filterIndexed { i, _ -> i % step == 0 || i == pts.size - 1 }.map { LatLng(it.lat, it.lng) }
            }
            activeMs = trip?.activeMs()
        }
        docFrom(inv, s, fare, qr, custSig, ownerSig, stamp, route, activeMs, online)
    }

    fun docFrom(
        inv: InvoiceEntity,
        s: AppSettings,
        fare: com.amitabhtours.invoice.domain.FareResult,
        qr: Bitmap?,
        custSig: Bitmap?,
        ownerSig: Bitmap?,
        stamp: Bitmap?,
        route: List<LatLng>?,
        activeMs: Long?,
        online: Boolean,
    ): InvoiceDoc {
        val cache = HashMap<String, Bitmap?>()
        val provider: ((Int, Int, Int) -> Bitmap?)? = route?.takeIf { it.size >= 2 }?.let { r ->
            { w: Int, h: Int, col: Int ->
                cache.getOrPut("$w-$h-$col") {
                    runCatching {
                        mapRenderer.render(r, w, h, MapRenderer.Style(routeColor = col, startColor = col, endColor = col, routeWidthPx = maxOf(4f, w / 110f)), online)
                    }.getOrNull()
                }
            }
        }
        return InvoiceDoc(
            businessName = s.businessName,
            address = s.address,
            phone = s.phone,
            email = s.email,
            gstin = s.gstin,
            city = s.city,
            footerText = s.footerText,
            gstNote = s.gstNote,
            number = inv.number,
            seq = inv.seq,
            createdAt = inv.createdAt,
            cancelled = inv.isCancelled,
            customerName = inv.customerName,
            customerMobile = inv.customerMobile,
            pickup = inv.pickup,
            drop = inv.dropAt,
            tripType = inv.tripType,
            carNo = inv.carNo,
            vehicleType = inv.vehicleType,
            driverName = inv.driverName,
            driverMobile = inv.driverMobile,
            startKm = inv.startKm,
            endKm = inv.endKm,
            gpsKm = inv.gpsKm,
            billedKm = inv.billedKm,
            kmSource = inv.billedKmSource,
            startTime = inv.startTime,
            endTime = inv.endTime,
            fare = fare,
            amountInWords = AmountInWords.rupees(if (fare.advancePaise > 0) fare.balanceDuePaise else fare.totalPaise),
            paymentMode = inv.paymentMode,
            paidAt = inv.paidAt,
            paidMode = inv.paidMode,
            feedback = inv.feedback,
            upiQr = qr,
            upiId = s.upiId,
            customerSignature = custSig,
            ownerSignature = ownerSig,
            ownerStamp = stamp,
            route = route,
            tripActiveMs = activeMs,
            mapProvider = provider,
        )
    }

    /** Realistic sample used before any invoice exists (e.g. Settings > default style). */
    fun sampleDoc(s: AppSettings, withRoute: Boolean): InvoiceDoc {
        val inv = sampleEntity(s)
        val fare = Fare.calculate(
            FareInput(billedKm = 23.0, ratePerKmPaise = 15_00, parkingPaise = 60_00, waitingHours = 0.0)
        )
        val route = if (withRoute) sampleRoute() else null
        val qr = if (s.upiReady) Qr.bitmap(Qr.upiUri(s.upiId, s.upiName, fare.totalPaise, "Sample")) else null
        val ownerSig = if (s.ownerSignatureEnabled) s.ownerSignaturePath?.let { Images.decodeSampled(it, 800) } else null
        val stamp = if (s.ownerStampEnabled) s.ownerStampPath?.let { Images.decodeSampled(it, 500) } else null
        return docFrom(inv, s, fare, qr, null, ownerSig, stamp, route, 44 * 60_000L, online = false)
    }

    fun sampleEntity(s: AppSettings): InvoiceEntity {
        val now = System.currentTimeMillis()
        return InvoiceEntity(
            number = "${s.invoicePrefix}-2026-0042", fyYear = 2026, seq = 42, createdAt = now,
            customerName = "Rahul Deshpande", customerMobile = "+91 98220 12345",
            pickup = "Kubera Garden, NIBM Road, Kondhwa, Pune – 411048",
            dropAt = "Pune International Airport, Lohegaon, Pune – 411032",
            carNo = s.carNo, vehicleType = s.vehicleType, driverName = s.driverName, driverMobile = s.driverMobile,
            tripType = "Airport Transfer", startKm = 45210.0, endKm = 45233.0, gpsKm = 22.6,
            billedKmSource = KmSource.ODOMETER, billedKm = 23.0,
            startTime = now - 52 * 60_000L, endTime = now - 8 * 60_000L,
            ratePerKmPaise = 15_00, tollPaise = 0, parkingPaise = 60_00, nightPaise = 0,
            paymentMode = "UPI", themeId = 5,
        )
    }

    companion object {
        fun sampleRoute(): List<LatLng> {
            val pts = listOf(
                LatLng(18.4672, 73.8988), LatLng(18.4741, 73.8960), LatLng(18.4835, 73.8992), LatLng(18.4932, 73.9010),
                LatLng(18.5012, 73.9001), LatLng(18.5096, 73.9055), LatLng(18.5175, 73.9071), LatLng(18.5262, 73.9087),
                LatLng(18.5368, 73.9031), LatLng(18.5462, 73.9022), LatLng(18.5560, 73.9043), LatLng(18.5651, 73.9081),
                LatLng(18.5735, 73.9095), LatLng(18.5803, 73.9196),
            )
            return pts
        }
    }
}

/** "Annexure — Receipts": each attached image or PDF page on its own A4 page. */
object Annexure {
    fun append(pdf: PdfDocument, doc: InvoiceDoc, atts: List<AttachmentEntity>, startIndex: Int) {
        var index = startIndex
        atts.forEachIndexed { n, a ->
            val caption = buildString {
                append(AttachmentType.label(a.type))
                a.amountPaise?.takeIf { it > 0 }?.let { append(" — ${Money.format(it)}") }
            }
            val file = File(a.path)
            if (!file.exists()) return@forEachIndexed
            if (a.mime == "application/pdf") {
                runCatching {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                        PdfRenderer(fd).use { r ->
                            for (p in 0 until minOf(r.pageCount, 10)) {
                                r.openPage(p).use { page ->
                                    val scale = 2f
                                    val bmp = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1), (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                    drawPage(pdf, index++, doc, n + 1, atts.size, "$caption (page ${p + 1} of ${r.pageCount})", bmp)
                                }
                            }
                        }
                    }
                }
            } else {
                Images.decodeSampled(a.path, 2200)?.let { bmp ->
                    drawPage(pdf, index++, doc, n + 1, atts.size, caption, bmp)
                }
            }
        }
    }

    private fun drawPage(pdf: PdfDocument, index: Int, doc: InvoiceDoc, n: Int, total: Int, caption: String, bmp: Bitmap) {
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), index).create())
        val c: Canvas = page.canvas
        val m = 36f
        val navy = color("#14213D")
        Logo.draw(c, m, 26f, 22f)
        c.drawText("Annexure — Receipts", m + 30f, 41f, D.paint(11f, navy, F.sansBold))
        c.drawText("Invoice ${doc.number}  ·  ${doc.businessName}", PAGE_W - m, 41f, D.paint(8f, color("#5B6478"), align = Paint.Align.RIGHT))
        D.line(c, m, 54f, PAGE_W - m, 54f, navy, 0.8f)
        c.drawText("Receipt $n of $total:  $caption", m, 72f, D.paint(9.5f, color("#1F2937"), F.sansMedium))
        D.bitmapFit(c, bmp, m, 84f, PAGE_W - 2 * m, PAGE_H - 84f - 40f, 0.5f, 0f)
        D.block(c, "Copies of receipts paid on the customer's behalf.", m, PAGE_H - 30f, PAGE_W - 2 * m, D.paint(7.5f, color("#8A8F98")), Layout.Alignment.ALIGN_CENTER)
        if (doc.cancelled) D.cancelledStamp(c)
        pdf.finishPage(page)
    }
}
