package com.amitabhtours.invoice.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.domain.Text

// ---------------------------------------------------------------------------------------------
// 6. Print Saver — black only, no filled areas.
// ---------------------------------------------------------------------------------------------
class PrintSaverTheme : InvoiceTheme(
    6, "Print Saver", "Black and white, cheap to print and photocopy", MapMode.NONE,
    listOf(Color.BLACK, Color.WHITE),
) {
    private val ink = Color.BLACK
    private val muted = color("#444444")
    override val margin = 44f
    override val continuationColor = Color.BLACK

    override fun drawBackground(w: PageWriter) {
        w.canvas.drawRect(26f, 26f, PAGE_W - 26f, PAGE_H - 26f, D.stroke(ink, 0.8f))
    }

    override fun drawContinuationHeader(w: PageWriter): Float {
        val c = w.canvas
        c.drawText("${w.doc.businessName} — Invoice ${w.doc.number} (continued)", PAGE_W / 2, 50f, D.paint(9f, ink, F.sansBold, Paint.Align.CENTER))
        D.line(c, margin, 58f, PAGE_W - margin, 58f, ink, 0.6f)
        return 72f
    }

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.bottomLimit = PAGE_H - 52f
        w.newPage()
        var c = w.canvas
        Logo.draw(c, PAGE_W / 2 - 15f, 38f, 30f, Logo.Style.MONO)
        c.drawText(d.businessName, PAGE_W / 2, 90f, D.paint(19f, ink, F.sansBold, Paint.Align.CENTER))
        var y = 96f
        y += D.block(c, d.address, m, y, right - m, D.paint(8.5f, ink), Layout.Alignment.ALIGN_CENTER)
        c.drawText(Blocks.contactLine(d), PAGE_W / 2, y + 10f, D.paint(8.5f, ink, align = Paint.Align.CENTER))
        y += 14f
        Blocks.gstinLine(d)?.let { c.drawText(it, PAGE_W / 2, y + 10f, D.paint(8.5f, ink, align = Paint.Align.CENTER)); y += 13f }
        y += 14f
        val tp = D.paint(11f, ink, F.sansBold, Paint.Align.CENTER, 0.3f)
        val tw = tp.measureText("INVOICE")
        D.line(c, m, y, PAGE_W / 2 - tw / 2 - 12f, y, ink, 0.6f)
        D.line(c, PAGE_W / 2 + tw / 2 + 12f, y, right, y, ink, 0.6f)
        c.drawText("INVOICE", PAGE_W / 2, y + 4f, tp)
        w.y = y + 18f

        val lp = D.paint(9f, muted)
        val vp = D.paint(9.5f, ink, F.sansMedium)
        fun row(vararg pairs: Pair<String, String?>) {
            val items = pairs.filter { !it.second.isNullOrBlank() }
            if (items.isEmpty()) return
            val colW = (right - m) / pairs.size
            val heights = items.map { D.blockHeight(it.second!!, vp, colW - 100f) }
            val h = maxOf(18f, heights.max() + 6f)
            w.ensure(h)
            val cc = w.canvas
            pairs.forEachIndexed { i, (label, value) ->
                if (value.isNullOrBlank()) return@forEachIndexed
                val x = m + i * colW
                cc.drawText("$label:", x, w.y + 11f, lp)
                D.block(cc, value, x + 88f, w.y + 2f, colW - 100f, vp)
            }
            D.dotted(cc, m, w.y + h - 2f, right, color("#999999"))
            w.y += h + 2f
        }
        row("Invoice No." to d.number, "Date" to d.dateTimeText)
        row("Customer Name" to d.customerName)
        row("Mobile" to d.customerMobile, "Trip Type" to d.tripType)
        row("Pickup From" to d.pickup)
        row("Drop To" to d.drop)
        row("Car No." to d.carNo, "Vehicle Type" to d.vehicleType)
        row("Driver Name" to d.driverName, "Mobile" to d.driverMobile)
        row("Start Km" to d.startKmText, "End Km" to d.endKmText)
        row("Start Time" to d.startTime?.let { Fmt.dateTime(it) }, "End Time" to d.endTime?.let { Fmt.dateTime(it) })
        row("Total Km" to d.billedKmWithSource.takeIf { d.billedKm > 0 }, "Total Hours" to d.totalHoursText)
        w.y += 8f

        Blocks.chargeTable(
            w, m, right - m,
            TableStyle(text = ink, muted = muted, headerText = ink, headerRule = ink, rule = color("#BBBBBB"), rowH = 20f, size = 9.5f, headerSize = 8f),
        )
        w.y += 8f
        val rows = Blocks.totalRows(d)
        val boxW = 230f
        val boxH = rows.sumOf { (if (it.strong) 26f else 18f).toDouble() }.toFloat() + 8f
        w.ensure(boxH + 8f)
        c = w.canvas
        val top = w.y
        c.drawRect(right - boxW, top, right, top + boxH, D.stroke(ink, 1f))
        w.y = top + 4f
        Blocks.totals(w, right - boxW + 10f, right - 10f, TotalsStyle(text = ink, muted = muted, strongText = ink, rule = color("#BBBBBB"), strongSize = 11.5f), rows)
        val leftW = right - m - boxW - 16f
        var ly = top + 2f
        ly += D.block(c, "In words: ${d.amountInWords}", m, ly, leftW, D.paint(9f, ink), maxLines = 4) + 6f
        d.paymentLine?.let { ly += D.block(c, it, m, ly, leftW, D.paint(9f, ink, F.sansMedium), maxLines = 2) }
        w.y = maxOf(top + boxH, ly) + 12f
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Feedback: ${d.feedback}", m, right - m, D.paint(9f, ink))
        w.y += 6f
        Blocks.closing(w, ClosingStyle(ink, muted, ink), m, right)
        Blocks.footer(w, ink, muted)
        w.finish()
    }
}

// ---------------------------------------------------------------------------------------------
// 7. Route Map — map-led, needs a GPS-tracked trip.
// ---------------------------------------------------------------------------------------------
class RouteMapTheme : InvoiceTheme(
    7, "Route Map", "Big route map with distance, time and speed", MapMode.REQUIRED,
    listOf(color("#0F766E"), color("#ECF7F5")),
) {
    private val teal = color("#0F766E")
    private val panel = color("#ECF7F5")
    private val ink = color("#17302D")
    private val muted = color("#5B7470")
    override val margin = 36f
    override val continuationColor = teal

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.newPage()
        var c = w.canvas

        val mapRect = RectF(0f, 0f, PAGE_W, 250f)
        val map = d.mapProvider?.invoke((PAGE_W * 1.5f).toInt(), (250f * 1.5f).toInt(), teal)
        if (map != null) c.drawBitmap(map, null, mapRect, Paint(Paint.FILTER_BITMAP_FLAG)) else D.rect(c, 0f, 0f, PAGE_W, 250f, panel)
        // Logo pill
        val nameP = D.paint(11f, ink, F.sansBold)
        val pillW = 46f + nameP.measureText(d.businessName)
        D.round(c, m, 22f, m + pillW, 58f, 18f, Color.WHITE)
        Logo.draw(c, m + 6f, 26f, 28f)
        c.drawText(d.businessName, m + 40f, 44f, nameP)
        // Invoice pill
        val invP = D.paint(9f, Color.WHITE, F.sansBold, Paint.Align.RIGHT)
        val invW = invP.measureText("INVOICE ${d.number}") + 24f
        D.round(c, right - invW, 26f, right, 52f, 13f, teal)
        c.drawText("INVOICE ${d.number}", right - 12f, 42.5f, invP)
        w.y = 264f

        // Stats tiles
        val tiles = listOf(
            "DISTANCE" to d.billedKmText,
            "DURATION" to (d.tripDurationMs?.let { Fmt.duration(it) } ?: "—"),
            "AVERAGE SPEED" to (d.avgSpeedKmh?.let { "${Num.clean(Math.round(it * 10) / 10.0)} km/h" } ?: "—"),
        )
        val gap = 12f
        val tw = (right - m - gap * 2) / 3
        tiles.forEachIndexed { i, (label, value) ->
            val x = m + i * (tw + gap)
            D.round(c, x, w.y, x + tw, w.y + 52f, 8f, panel)
            c.drawText(label, x + 12f, w.y + 18f, D.paint(7f, muted, F.sansBold, letterSpacing = 0.12f))
            c.drawText(value, x + 12f, w.y + 40f, D.paint(15f, teal, F.sansBold))
        }
        w.y += 70f

        // Timeline (left) + details (right)
        val colW = (right - m) / 2 - 10f
        val top = w.y
        var ly = top
        c.drawCircle(m + 5f, ly + 6f, 5f, D.fill(teal))
        c.drawText("PICKUP" + (d.startTime?.let { " · ${Fmt.time(it)}" } ?: ""), m + 18f, ly + 9f, D.paint(7.5f, muted, F.sansBold, letterSpacing = 0.1f))
        ly += 14f
        ly += D.block(c, d.pickup.ifBlank { "—" }, m + 18f, ly, colW - 18f, D.paint(9f, ink), maxLines = 4) + 14f
        D.line(c, m + 5f, top + 12f, m + 5f, ly, teal, 1.6f)
        c.drawRect(m, ly, m + 10f, ly + 10f, D.fill(teal))
        c.drawText("DROP" + (d.endTime?.let { " · ${Fmt.time(it)}" } ?: ""), m + 18f, ly + 9f, D.paint(7.5f, muted, F.sansBold, letterSpacing = 0.1f))
        ly += 14f
        ly += D.block(c, d.drop.ifBlank { "—" }, m + 18f, ly, colW - 18f, D.paint(9f, ink), maxLines = 4)

        val rx = m + colW + 20f
        val details = buildList {
            add("Customer" to listOf(d.customerName, d.customerMobile).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" })
            add("Vehicle" to listOf(d.vehicleType, d.carNo).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" })
            add("Driver" to listOf(d.driverName, d.driverMobile).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" })
            add("Date" to d.dateTimeText)
            if (d.tripType.isNotBlank()) add("Trip type" to d.tripType)
        }
        var ry = top
        details.forEach { (l, v) ->
            c.drawText(l.uppercase(), rx, ry + 8f, D.paint(7f, muted, F.sansBold, letterSpacing = 0.1f))
            ry += 11f
            ry += D.block(c, v, rx, ry, right - rx, D.paint(9f, ink), maxLines = 2) + 6f
        }
        w.y = maxOf(ly, ry) + 16f

        Blocks.chargeTable(
            w, m, right - m,
            TableStyle(text = ink, muted = muted, headerText = teal, headerRule = teal, rule = color("#D6E7E4"), rowH = 21f, size = 9.5f),
        )
        w.y += 8f
        val rows = Blocks.totalRows(d)
        val nonStrong = rows.filter { !it.strong }
        if (nonStrong.isNotEmpty()) Blocks.totals(w, right - 240f, right, TotalsStyle(text = ink, muted = muted, strongText = ink), nonStrong)
        val strong = rows.first { it.strong }
        w.ensure(40f)
        c = w.canvas
        D.round(c, m, w.y + 4f, right, w.y + 36f, 8f, teal)
        c.drawText(strong.label.uppercase(), m + 14f, w.y + 25f, D.paint(10f, Color.WHITE, F.sansBold, letterSpacing = 0.1f))
        c.drawText(strong.value, right - 14f, w.y + 26f, D.paint(15f, Color.WHITE, F.sansBold, Paint.Align.RIGHT))
        w.y += 46f
        Blocks.amountInWords(w, m, right - m, ink)
        d.paymentLine?.let { Blocks.paragraph(w, it, m, right - m, D.paint(8.5f, ink, F.sansMedium)) }
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Remarks: ${d.feedback}", m, right - m, D.paint(8.5f, ink))
        val contact = listOf(d.address, Blocks.contactLine(d), Blocks.gstinLine(d) ?: "").filter { it.isNotBlank() }.joinToString("  ·  ")
        Blocks.paragraph(w, contact, m, right - m, D.paint(7.5f, muted), 6f)
        Blocks.closing(w, ClosingStyle(ink, muted, teal), m, right)
        Blocks.footer(w, teal, muted)
        w.finish()
    }
}

// ---------------------------------------------------------------------------------------------
// 8. Heritage Bill Book — mirrors the owner's paper bill book.
// ---------------------------------------------------------------------------------------------
class HeritageTheme : InvoiceTheme(
    8, "Heritage Bill Book", "Looks like your paper bill book, neatly typed", MapMode.NONE,
    listOf(color("#1F5F4A"), color("#9DB8AD")),
) {
    private val green = color("#1F5F4A")
    private val dots = color("#9DB8AD")
    private val ink = color("#111111")
    override val margin = 48f
    override val continuationColor = green

    override fun drawBackground(w: PageWriter) {
        val c = w.canvas
        c.drawRect(24f, 24f, PAGE_W - 24f, PAGE_H - 24f, D.stroke(green, 1.6f))
        c.drawRect(29f, 29f, PAGE_W - 29f, PAGE_H - 29f, D.stroke(green, 0.6f))
    }

    override fun drawContinuationHeader(w: PageWriter): Float {
        val c = w.canvas
        c.drawText(w.doc.businessName, PAGE_W / 2, 56f, D.paint(13f, green, F.serifBold, Paint.Align.CENTER))
        c.drawText("Invoice ${w.doc.number} (continued)", PAGE_W / 2, 70f, D.paint(8.5f, green, align = Paint.Align.CENTER))
        D.line(c, 29f, 80f, PAGE_W - 29f, 80f, green, 0.8f)
        return 96f
    }

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.bottomLimit = PAGE_H - 50f
        w.newPage()
        var c = w.canvas
        c.drawText(d.businessName, PAGE_W / 2, 70f, D.paint(25f, green, F.serifBold, Paint.Align.CENTER))
        var y = 78f
        y += D.block(c, d.address, m, y, right - m, D.paint(9.5f, ink), Layout.Alignment.ALIGN_CENTER)
        c.drawText(Blocks.contactLine(d), PAGE_W / 2, y + 12f, D.paint(9.5f, ink, align = Paint.Align.CENTER))
        y += 16f
        Blocks.gstinLine(d)?.let { c.drawText(it, PAGE_W / 2, y + 12f, D.paint(9f, ink, align = Paint.Align.CENTER)); y += 14f }
        y += 10f
        D.line(c, 29f, y, PAGE_W - 29f, y, green, 1.2f)
        // Boxed INVOICE label
        val lp = D.paint(9f, green, F.sansBold, Paint.Align.CENTER, 0.25f)
        val lw = lp.measureText("INVOICE") + 26f
        D.rect(c, PAGE_W / 2 - lw / 2, y - 9f, PAGE_W / 2 + lw / 2, y + 9f, Color.WHITE)
        c.drawRect(PAGE_W / 2 - lw / 2, y - 9f, PAGE_W / 2 + lw / 2, y + 9f, D.stroke(green, 1f))
        c.drawText("INVOICE", PAGE_W / 2, y + 3.2f, lp)
        w.y = y + 22f

        val labelP = D.paint(10f, green, F.sansMedium)
        val valueP = D.paint(10.5f, ink, F.sansMedium)
        fun field(x: Float, width: Float, label: String, value: String) {
            val cc = w.canvas
            cc.drawText("$label :", x, w.y + 13f, labelP)
            val lx = x + labelP.measureText("$label :") + 6f
            D.dotted(cc, lx, w.y + 16f, x + width - 6f, dots)
            cc.drawText(D.fit(value, valueP, x + width - lx - 8f), lx + 2f, w.y + 12f, valueP)
        }
        fun fullRow(label: String, value: String?) {
            if (value.isNullOrBlank()) return
            val avail = right - m - labelP.measureText("$label :") - 14f
            val lines = D.layout(value, valueP, avail).lineCount
            if (lines <= 1) {
                w.ensure(24f); field(m, right - m, label, value); w.y += 24f
            } else {
                // Wrap long addresses onto extra dotted lines.
                w.ensure(24f * lines)
                val cc = w.canvas
                cc.drawText("$label :", m, w.y + 13f, labelP)
                val lx = m + labelP.measureText("$label :") + 6f
                val l = D.layout(value, valueP, right - lx - 8f)
                for (i in 0 until l.lineCount) {
                    val text = value.substring(l.getLineStart(i), l.getLineEnd(i)).trim()
                    val sx = if (i == 0) lx else m
                    D.dotted(cc, sx, w.y + 16f, right - 6f, dots)
                    cc.drawText(text, sx + 2f, w.y + 12f, valueP)
                    w.y += 24f
                }
            }
        }
        fun twoRow(l1: String, v1: String?, l2: String, v2: String?) {
            if (v1.isNullOrBlank() && v2.isNullOrBlank()) return
            w.ensure(24f)
            val half = (right - m) / 2
            field(m, half, l1, v1 ?: "")
            field(m + half + 6f, half - 6f, l2, v2 ?: "")
            w.y += 24f
        }
        twoRow("Invoice No.", d.number, "Date", d.dateTimeText)
        fullRow("Customer Name", d.customerName)
        fullRow("Mobile", d.customerMobile)
        fullRow("Pickup From", d.pickup)
        fullRow("Drop To", d.drop)
        twoRow("Car No.", d.carNo, "Vehicle Type", d.vehicleType)
        twoRow("Driver Name", d.driverName, "Mobile", d.driverMobile)
        twoRow("Start Km.", d.startKmText, "End Km.", d.endKmText)
        twoRow("Start Time", d.startTime?.let { Fmt.dateTime(it) }, "End Time", d.endTime?.let { Fmt.dateTime(it) })
        twoRow("Total Km.", d.billedKmWithSource.takeIf { d.billedKm > 0 }, "Total Hours", d.totalHoursText)
        fullRow("Trip Type", d.tripType)
        d.fare.lines.forEach { line ->
            val detail = listOfNotNull(line.qtyText, line.rateText?.let { "@ $it" }).joinToString(" ")
            fullRow(line.label, Money.format(line.amountPaise) + if (detail.isNotBlank()) "   ($detail)" else "")
        }
        w.y += 10f

        // In words (left) + totals box (right)
        val rows = Blocks.totalRows(d, "Total", "Total Balance")
        val boxW = 200f
        val rowH = 26f
        val boxH = rows.size * rowH
        w.ensure(boxH + 10f)
        c = w.canvas
        val top = w.y
        val bx = right - boxW
        c.drawRect(bx, top, right, top + boxH, D.stroke(green, 1.2f))
        D.line(c, bx + 100f, top, bx + 100f, top + boxH, green, 0.8f)
        rows.forEachIndexed { i, r ->
            val ry = top + i * rowH
            if (i > 0) D.line(c, bx, ry, right, ry, green, 0.8f)
            val label = when (r.label) { "Advance paid" -> "Advance Booking"; else -> r.label }
            c.drawText(label, bx + 8f, ry + 17f, D.paint(9.5f, green, if (r.strong) F.sansBold else F.sansMedium))
            c.drawText(r.value, right - 8f, ry + 17f, D.paint(if (r.strong) 11f else 10f, ink, if (r.strong) F.sansBold else F.sansMedium, Paint.Align.RIGHT))
        }
        val lwid = bx - m - 16f
        c.drawText("In Words :", m, top + 13f, labelP)
        val wl = D.layout(d.amountInWords, valueP, lwid)
        var wy = top
        for (i in 0 until maxOf(wl.lineCount, 2)) {
            val sx = if (i == 0) m + labelP.measureText("In Words :") + 6f else m
            D.dotted(c, sx, wy + 16f, m + lwid, dots)
            if (i < wl.lineCount) {
                val t = d.amountInWords.substring(wl.getLineStart(i), wl.getLineEnd(i)).trim()
                c.drawText(D.fit(t, valueP, m + lwid - sx), sx + 2f, wy + 12f, valueP)
            }
            wy += 24f
        }
        d.paymentLine?.let { c.drawText(D.fit(it, D.paint(9f, ink), lwid), m, wy + 12f, D.paint(9f, ink)) }
        w.y = maxOf(top + boxH, wy + 18f) + 10f
        fullRow("Feedback", d.feedback.ifBlank { null })
        w.y += 6f
        Blocks.closing(w, ClosingStyle(ink, green, green, F.sans, F.serifBold), m, right)
        Blocks.footer(w, green, color("#4E6B60"))
        w.finish()
    }
}

// ---------------------------------------------------------------------------------------------
// 9. Journey Pass — boarding-pass style. Map strip when tracked, dotted route otherwise.
// ---------------------------------------------------------------------------------------------
class JourneyPassTheme : InvoiceTheme(
    9, "Journey Pass", "Ticket-style pass; shows a map strip when tracked", MapMode.OPTIONAL,
    listOf(color("#3F3D8F"), color("#F26D5B")),
) {
    private val indigo = color("#3F3D8F")
    private val coral = color("#F26D5B")
    private val stub = color("#F1F0FA")
    private val ink = color("#1E1D3A")
    private val muted = color("#6C6A93")
    private val pageBg = color("#E9E8F4")
    override val margin = 34f
    override val continuationColor = indigo

    override fun drawBackground(w: PageWriter) {
        w.canvas.drawColor(pageBg)
    }

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.newPage()
        var c = w.canvas
        val cardTop = 30f

        // Top (indigo) section
        val topH = 220f
        val path = Path().apply {
            addRoundRect(RectF(m, cardTop, right, cardTop + topH), floatArrayOf(16f, 16f, 16f, 16f, 0f, 0f, 0f, 0f), Path.Direction.CW)
        }
        c.drawPath(path, D.fill(indigo))
        Logo.draw(c, m + 18f, cardTop + 16f, 30f)
        c.drawText(d.businessName, m + 56f, cardTop + 30f, D.paint(12f, Color.WHITE, F.sansBold))
        c.drawText(D.fit(Blocks.contactLine(d), D.paint(7f, color("#C9C7EE")), 260f), m + 56f, cardTop + 42f, D.paint(7f, color("#C9C7EE")))
        c.drawText("INVOICE", right - 18f, cardTop + 28f, D.paint(8f, color("#C9C7EE"), F.sansBold, Paint.Align.RIGHT, 0.2f))
        c.drawText(d.number, right - 18f, cardTop + 42f, D.paint(11f, Color.WHITE, F.sansBold, Paint.Align.RIGHT))

        val colW = (right - m) / 2 - 50f
        val fx = m + 22f
        val tx = right - 22f
        c.drawText("FROM", fx, cardTop + 72f, D.paint(7.5f, color("#C9C7EE"), F.sansBold, letterSpacing = 0.2f))
        c.drawText("TO", tx, cardTop + 72f, D.paint(7.5f, color("#C9C7EE"), F.sansBold, Paint.Align.RIGHT, 0.2f))
        val bigP = D.paint(22f, Color.WHITE, F.sansBold)
        c.drawText(D.fit(Text.shortPlace(d.pickup).ifBlank { "Pickup" }, bigP, colW), fx, cardTop + 98f, bigP)
        val bigR = D.paint(22f, Color.WHITE, F.sansBold, Paint.Align.RIGHT)
        c.drawText(D.fit(Text.shortPlace(d.drop).ifBlank { "Drop" }, bigR, colW), tx, cardTop + 98f, bigR)
        D.car(c, PAGE_W / 2 - 18f, cardTop + 80f, 36f, coral, indigo)
        D.dashed(c, PAGE_W / 2 - 60f, cardTop + 104f, PAGE_W / 2 + 60f, cardTop + 104f, color("#8C89D6"), 1f, 3f, 3f)
        val timeP = D.paint(9f, color("#E3E2F7"))
        d.startTime?.let { c.drawText("${Fmt.time(it)} · ${Fmt.date(it)}", fx, cardTop + 114f, timeP) }
        d.endTime?.let { c.drawText("${Fmt.time(it)} · ${Fmt.date(it)}", tx, cardTop + 114f, D.paint(9f, color("#E3E2F7"), align = Paint.Align.RIGHT)) }
        val smallP = D.paint(7.2f, color("#C9C7EE"))
        D.block(c, d.pickup, fx, cardTop + 120f, colW, smallP, maxLines = 2)
        D.block(c, d.drop, tx - colW, cardTop + 120f, colW, smallP, Layout.Alignment.ALIGN_OPPOSITE, maxLines = 2)

        // Map strip or stylised route
        val stripTop = cardTop + 146f
        val stripRect = RectF(m + 18f, stripTop, right - 18f, stripTop + 62f)
        val map = if (d.hasRoute) d.mapProvider?.invoke((stripRect.width() * 1.6f).toInt(), (stripRect.height() * 1.6f).toInt(), coral) else null
        if (map != null) {
            D.bitmapRounded(c, map, stripRect, 8f)
        } else {
            D.round(c, stripRect.left, stripRect.top, stripRect.right, stripRect.bottom, 8f, color("#4B49A0"))
            val cy = stripRect.centerY()
            val wave = Path().apply {
                moveTo(stripRect.left + 24f, cy)
                cubicTo(stripRect.left + 140f, cy - 22f, stripRect.centerX() - 40f, cy + 22f, stripRect.centerX() + 30f, cy)
                cubicTo(stripRect.centerX() + 110f, cy - 20f, stripRect.right - 120f, cy + 18f, stripRect.right - 24f, cy)
            }
            c.drawPath(wave, D.stroke(color("#C9C7EE"), 1.6f, floatArrayOf(4f, 4f)))
            c.drawCircle(stripRect.left + 24f, cy, 5f, D.fill(Color.WHITE))
            c.drawRect(stripRect.right - 29f, cy - 5f, stripRect.right - 19f, cy + 5f, D.fill(coral))
        }

        // Perforation
        val perfY = cardTop + topH
        D.rect(c, m, perfY, right, perfY + 1f, stub)
        val stubTop = perfY
        // Stub background (drawn tall; trimmed by content height later)
        val rows = Blocks.totalRows(d)
        val lineCount = d.fare.lines.size
        val stubH = 150f + lineCount * 18f + rows.size * 18f + 30f
        val stubPath = Path().apply {
            addRoundRect(RectF(m, stubTop, right, stubTop + stubH), floatArrayOf(0f, 0f, 0f, 0f, 16f, 16f, 16f, 16f), Path.Direction.CW)
        }
        c.drawPath(stubPath, D.fill(Color.WHITE))
        c.drawCircle(m, perfY, 11f, D.fill(pageBg))
        c.drawCircle(right, perfY, 11f, D.fill(pageBg))
        D.dashed(c, m + 16f, perfY, right - 16f, perfY, color("#B9B6E4"), 1.2f, 6f, 5f)

        // Grid of labelled cells
        val cells = listOf(
            "PASSENGER" to d.customerName.ifBlank { "—" },
            "MOBILE" to d.customerMobile.ifBlank { "—" },
            "DATE" to d.dateText,
            "VEHICLE" to d.vehicleType.ifBlank { "—" },
            "CAR NO." to d.carNo.ifBlank { "—" },
            "TRIP TYPE" to d.tripType.ifBlank { "—" },
            "DRIVER" to d.driverName.ifBlank { "—" },
            "DISTANCE" to d.billedKmText,
            "DURATION" to (d.tripDurationMs?.let { Fmt.duration(it) } ?: "—"),
        )
        val qrW = if (d.upiQr != null) 112f else 0f
        val gridW = right - m - 36f - qrW
        val cellW = gridW / 3
        var gy = stubTop + 16f
        cells.chunked(3).forEach { row ->
            row.forEachIndexed { i, (l, v) ->
                val x = m + 18f + i * cellW
                c.drawText(l, x, gy + 8f, D.paint(6.6f, muted, F.sansBold, letterSpacing = 0.14f))
                c.drawText(D.fit(v, D.paint(10f, ink, F.sansBold), cellW - 8f), x, gy + 22f, D.paint(10f, ink, F.sansBold))
            }
            gy += 34f
        }
        d.upiQr?.let {
            val qx = right - 18f - 96f
            D.round(c, qx - 6f, stubTop + 12f, right - 12f, stubTop + 124f, 8f, stub)
            D.bitmapFit(c, it, qx + 6f, stubTop + 18f, 84f, 84f, smooth = false)
            c.drawText("SCAN TO PAY", qx + 48f, stubTop + 114f, D.paint(6.8f, indigo, F.sansBold, Paint.Align.CENTER, 0.12f))
        }
        // Fare breakdown
        var fy = maxOf(gy, stubTop + 130f) + 4f
        D.dashed(c, m + 18f, fy, right - 18f, fy, color("#D7D5F0"), 0.8f)
        fy += 8f
        d.fare.lines.forEach { l ->
            c.drawText(l.label + (l.qtyText?.let { "  ·  $it" } ?: ""), m + 18f, fy + 11f, D.paint(9f, ink))
            c.drawText(Money.format(l.amountPaise), right - 18f, fy + 11f, D.paint(9f, ink, align = Paint.Align.RIGHT))
            fy += 18f
        }
        rows.filter { !it.strong }.forEach { r ->
            c.drawText(r.label, m + 18f, fy + 11f, D.paint(9f, muted))
            c.drawText(r.value, right - 18f, fy + 11f, D.paint(9f, muted, align = Paint.Align.RIGHT))
            fy += 18f
        }
        val strong = rows.first { it.strong }
        val pillP = D.paint(13f, Color.WHITE, F.sansBold, Paint.Align.RIGHT)
        val pillText = "${strong.label}  ${strong.value}"
        val pw = pillP.measureText(pillText) + 30f
        D.round(c, right - 18f - pw, fy + 4f, right - 18f, fy + 32f, 14f, coral)
        c.drawText(pillText, right - 33f, fy + 23f, pillP)
        fy += 40f
        // Trim stub to content: redraw the bottom edge as rounded card end.
        w.y = fy + 10f
        val realBottom = w.y
        if (realBottom < stubTop + stubH) {
            c.drawRect(m - 1f, realBottom, right + 1f, stubTop + stubH + 1f, D.fill(pageBg))
            val endPath = Path().apply {
                addRoundRect(RectF(m, realBottom - 16f, right, realBottom), floatArrayOf(0f, 0f, 0f, 0f, 16f, 16f, 16f, 16f), Path.Direction.CW)
            }
            c.drawPath(endPath, D.fill(Color.WHITE))
        }
        w.y = realBottom + 16f

        Blocks.amountInWords(w, m + 4f, right - m - 8f, ink)
        d.paymentLine?.let { Blocks.paragraph(w, it, m + 4f, right - m - 8f, D.paint(8.5f, ink, F.sansMedium)) }
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Remarks: ${d.feedback}", m + 4f, right - m - 8f, D.paint(8.5f, ink))
        Blocks.paragraph(w, listOf(d.address, Blocks.gstinLine(d) ?: "").filter { it.isNotBlank() }.joinToString("  ·  "), m + 4f, right - m - 8f, D.paint(7.5f, muted), 8f)
        val noQr = d.copy(upiQr = null)
        val save = w.doc
        if (save.upiQr != null) {
            // QR already printed on the stub; draw signatures without it.
            closingWithoutQr(w, noQr, m + 4f, right - 4f)
        } else {
            Blocks.closing(w, ClosingStyle(ink, muted, indigo), m + 4f, right - 4f)
        }
        Blocks.footer(w, indigo, muted)
        w.finish()
    }

    private fun closingWithoutQr(w: PageWriter, d: InvoiceDoc, left: Float, right: Float) {
        // Same as Blocks.closing, but for a doc copy without the QR.
        val height = 98f
        w.ensure(height + 6f)
        val c = w.canvas
        val top = w.y
        val sigLine = top + 70f
        d.customerSignature?.let { D.bitmapFit(c, it, left, sigLine - 46f, 140f, 44f, 0.5f, 1f) }
        D.line(c, left, sigLine, left + 140f, sigLine, indigo, 0.7f)
        c.drawText("Customer Signature", left, sigLine + 12f, D.paint(8f, muted))
        val ownW = 170f
        val ox = right - ownW
        val forP = D.paint(8f, ink, F.sansBold, Paint.Align.RIGHT)
        c.drawText(D.fit("For ${d.businessName}", forP, ownW), right, top + 10f, forP)
        d.ownerStamp?.let { D.bitmapFit(c, it, ox, top + 14f, 62f, 62f, 0f, 0.5f) }
        d.ownerSignature?.let { D.bitmapFit(c, it, ox + 24f, sigLine - 48f, ownW - 24f, 46f, 0.5f, 1f) }
        D.line(c, ox + 20f, sigLine, right, sigLine, indigo, 0.7f)
        c.drawText("Authorised Signatory", right, sigLine + 12f, D.paint(8f, muted, align = Paint.Align.RIGHT))
        w.y = top + height
    }
}

// ---------------------------------------------------------------------------------------------
// 10. Royal Maroon — elegant, centred, cream paper.
// ---------------------------------------------------------------------------------------------
class RoyalMaroonTheme : InvoiceTheme(
    10, "Royal Maroon", "Elegant cream and maroon for outstation and family trips", MapMode.NONE,
    listOf(color("#7A1F2B"), color("#B8963E")),
) {
    private val maroon = color("#7A1F2B")
    private val cream = color("#FBF6EC")
    private val gold = color("#B8963E")
    private val ink = color("#2B1A1C")
    private val muted = color("#7D6A62")
    override val margin = 54f
    override val continuationColor = maroon

    override fun drawBackground(w: PageWriter) {
        val c = w.canvas
        c.drawColor(cream)
        c.drawRect(20f, 20f, PAGE_W - 20f, PAGE_H - 20f, D.stroke(maroon, 1.4f))
        c.drawRect(26f, 26f, PAGE_W - 26f, PAGE_H - 26f, D.stroke(gold, 0.6f))
        listOf(26f to 26f, PAGE_W - 26f to 26f, 26f to PAGE_H - 26f, PAGE_W - 26f to PAGE_H - 26f).forEach { (x, y) ->
            diamond(c, x, y, 5f, gold)
        }
    }

    private fun diamond(c: android.graphics.Canvas, x: Float, y: Float, r: Float, col: Int) {
        val p = Path().apply { moveTo(x, y - r); lineTo(x + r, y); lineTo(x, y + r); lineTo(x - r, y); close() }
        c.drawPath(p, D.fill(col))
    }

    private fun ornament(c: android.graphics.Canvas, cx: Float, y: Float, halfWidth: Float, gap: Float) {
        D.line(c, cx - halfWidth, y, cx - gap, y, gold, 0.8f)
        D.line(c, cx + gap, y, cx + halfWidth, y, gold, 0.8f)
        diamond(c, cx - gap + 4f, y, 3f, gold)
        diamond(c, cx + gap - 4f, y, 3f, gold)
    }

    override fun drawContinuationHeader(w: PageWriter): Float {
        val c = w.canvas
        c.drawText(w.doc.businessName, PAGE_W / 2, 56f, D.paint(14f, maroon, F.serifBold, Paint.Align.CENTER))
        c.drawText("Invoice ${w.doc.number} (continued)", PAGE_W / 2, 70f, D.paint(8.5f, muted, F.serifItalic, Paint.Align.CENTER))
        ornament(c, PAGE_W / 2, 80f, 120f, 10f)
        return 96f
    }

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.bottomLimit = PAGE_H - 46f
        w.newPage()
        var c = w.canvas
        Logo.draw(c, PAGE_W / 2 - 24f, 40f, 48f)
        c.drawText(d.businessName, PAGE_W / 2, 116f, D.paint(24f, maroon, F.serifBold, Paint.Align.CENTER, 0.02f))
        var y = 124f
        y += D.block(c, d.address, m, y, right - m, D.paint(8.8f, muted, F.serif), Layout.Alignment.ALIGN_CENTER)
        c.drawText(Blocks.contactLine(d), PAGE_W / 2, y + 11f, D.paint(8.8f, muted, F.serif, Paint.Align.CENTER))
        y += 14f
        Blocks.gstinLine(d)?.let { c.drawText(it, PAGE_W / 2, y + 11f, D.paint(8.8f, muted, F.serif, Paint.Align.CENTER)); y += 13f }
        y += 22f
        ornament(c, PAGE_W / 2, y, 200f, 66f)
        c.drawText("INVOICE", PAGE_W / 2, y + 5f, D.paint(14f, maroon, F.serifBold, Paint.Align.CENTER, 0.3f))
        y += 22f
        c.drawText("No. ${d.number}   ·   ${d.dateTimeText}", PAGE_W / 2, y + 4f, D.paint(9.5f, ink, F.serif, Paint.Align.CENTER))
        w.y = y + 24f

        // Two columns
        val colW = (right - m) / 2 - 12f
        val lp = D.paint(7.5f, gold, F.sansBold, letterSpacing = 0.2f)
        val vp = D.paint(9.5f, ink, F.serif)
        val bill = listOf(d.customerName, d.customerMobile, d.pickup.takeIf { it.isNotBlank() }?.let { "From: $it" } ?: "", d.drop.takeIf { it.isNotBlank() }?.let { "To: $it" } ?: "")
            .filter { it.isNotBlank() }.joinToString("\n").ifBlank { "—" }
        val journey = d.tripPairs().joinToString("\n") { "${it.first}: ${it.second}" }.ifBlank { "—" }
        val h = maxOf(D.blockHeight(bill, vp, colW, spacingMult = 1.1f), D.blockHeight(journey, vp, colW, spacingMult = 1.1f)) + 18f
        w.ensure(h)
        c = w.canvas
        c.drawText("BILLED TO", m, w.y + 8f, lp)
        D.block(c, bill, m, w.y + 14f, colW, vp, spacingMult = 1.1f)
        c.drawText("JOURNEY", m + colW + 24f, w.y + 8f, lp)
        D.block(c, journey, m + colW + 24f, w.y + 14f, colW, vp, spacingMult = 1.1f)
        D.line(c, PAGE_W / 2, w.y + 4f, PAGE_W / 2, w.y + h, color("#E2D5BC"), 0.6f)
        w.y += h + 14f

        Blocks.chargeTable(
            w, m, right - m,
            TableStyle(
                text = ink, muted = muted, headerText = maroon, headerTypeface = F.serifBold, headerRule = maroon,
                rule = color("#E2CFC8"), rowH = 22f, size = 9.5f, typeface = F.serif, amountTypeface = F.serif,
                labels = listOf("Particulars", "Rate", "Quantity", "Amount"), letterSpacing = 0.02f, headerSize = 9f,
            ),
        )
        w.y += 8f
        Blocks.totals(
            w, right - 230f, right,
            TotalsStyle(text = ink, muted = muted, strongText = cream, strongBg = maroon, rule = color("#E2CFC8"), typeface = F.serif, strongTypeface = F.serifBold, strongH = 28f),
        )
        w.y += 10f
        Blocks.paragraph(w, d.amountInWords, m, right - m, D.paint(10f, maroon, F.serifItalic), 4f, Layout.Alignment.ALIGN_CENTER)
        d.paymentLine?.let { Blocks.paragraph(w, it, m, right - m, D.paint(8.8f, ink, F.serif), 4f, Layout.Alignment.ALIGN_CENTER) }
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Remarks: ${d.feedback}", m, right - m, D.paint(8.8f, ink, F.serif), 4f, Layout.Alignment.ALIGN_CENTER)
        w.y += 10f
        Blocks.closing(w, ClosingStyle(ink, muted, maroon, F.serif, F.serifBold), m, right)
        w.ensure(20f)
        ornament(w.canvas, PAGE_W / 2, maxOf(w.y + 6f, w.bottomLimit - 44f), 120f, 10f)
        Blocks.footer(w, maroon, muted, F.serif)
        w.finish()
    }
}
