package com.amitabhtours.invoice.util

import com.amitabhtours.invoice.App
import com.amitabhtours.invoice.data.KmSource
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Manual safety net: the user chose "phone only, no backup". */
object Exporter {

    suspend fun writeCsv(app: App, out: OutputStream) = withContext(Dispatchers.IO) {
        val rows = app.db.invoices().all().sortedBy { it.createdAt }
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        fun m(p: Long) = Money.format(p, symbol = false).replace(",", "")
        out.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿") // so Excel reads ₹ and accents correctly
            w.write(
                listOf(
                    "Invoice No", "Date", "Status", "Customer", "Mobile", "Pickup", "Drop", "Trip type", "Car No", "Vehicle", "Driver",
                    "Km billed", "Km source", "Rate/km", "Distance fare", "Toll", "Parking", "Waiting", "Night", "Allowance", "Other",
                    "Round off", "Total", "Advance", "Balance due", "Payment mode", "Paid on", "Remarks",
                ).joinToString(",") + "\n"
            )
            rows.forEach { i ->
                w.write(
                    listOf(
                        q(i.number), q(Fmt.dateTime(i.createdAt)), q(if (i.isCancelled) "Cancelled" else if (i.outstandingPaise > 0) "Due" else "Paid"),
                        q(i.customerName), q(i.customerMobile), q(i.pickup), q(i.dropAt), q(i.tripType), q(i.carNo), q(i.vehicleType), q(i.driverName),
                        Num.clean(i.billedKm), q(KmSource.label(i.billedKmSource)), m(i.ratePerKmPaise), m(i.distanceFarePaise), m(i.tollPaise),
                        m(i.parkingPaise), m(i.waitingFarePaise), m(i.nightPaise), m(i.allowanceFarePaise), m(i.customTotalPaise), m(i.roundOffPaise),
                        m(i.totalPaise), m(i.advancePaise), m(i.balanceDuePaise), q(i.paidMode ?: i.paymentMode), q(i.paidAt?.let { Fmt.date(it) } ?: ""),
                        q(i.feedback),
                    ).joinToString(",") + "\n"
                )
            }
        }
    }

    /** Zips every invoice PDF (re-creating any that are missing). Returns the count. */
    suspend fun writePdfZip(app: App, out: OutputStream): Int = withContext(Dispatchers.IO) {
        var n = 0
        ZipOutputStream(out.buffered()).use { zip ->
            app.db.invoices().all().forEach { inv ->
                val f = inv.pdfPath?.let { File(it) }?.takeIf { it.exists() } ?: runCatching { app.pdf.render(inv.id) }.getOrNull()
                if (f != null && f.exists()) {
                    zip.putNextEntry(ZipEntry("${inv.number}.pdf"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    n++
                }
            }
        }
        n
    }
}
