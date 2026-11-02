package com.amitabhtours.invoice.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Text

// ---------------------------------------------------------------------------------------------
// 1. Marigold Trip Bill — layout idea from Type1.jpeg, deeper marigold band.
// ---------------------------------------------------------------------------------------------
class MarigoldTheme : InvoiceTheme(
    1, "Marigold Trip Bill", "Bright marigold band, trip-style layout", MapMode.NONE,
    listOf(color("#F5B82E"), color("#262626")),
) {
    private val band = color("#F5B82E")
    private val ink = color("#262626")
    private val rule = color("#BDBDBD")
    private val accent = color("#E39B12")
    private val muted = color("#6B6B6B")
    override val margin = 36f
    override val continuationColor = ink

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.newPage()
        var c = w.canvas

        // Header band
        D.rect(c, m, 28f, right, 96f, band)
        D.line(c, m, 96f, right, 96f, ink, 1.4f)
        Logo.badge(c, m + 33f, 62f, 23f, color("#1E1E1E"))
        c.drawText(d.businessName, m + 66f, 56f, D.paint(14f, ink, F.sansBold))
        D.block(c, d.address, m + 66f, 61f, 270f, D.paint(7.3f, ink), maxLines = 2)
        c.drawText(D.fit(Blocks.contactLine(d), D.paint(7.3f, ink), 270f), m + 66f, 88f, D.paint(7.3f, ink))
        c.drawText("INVOICE", right - 10f, 56f, D.paint(18f, ink, F.sansBlack, Paint.Align.RIGHT, 0.08f))
        c.drawText(d.number, right - 10f, 72f, D.paint(9.5f, ink, F.sansBold, Paint.Align.RIGHT))
        c.drawText(d.dateTimeText, right - 10f, 85f, D.paint(8f, ink, align = Paint.Align.RIGHT))
        Blocks.gstinLine(d)?.let { c.drawText(it, m + 66f, 106f, D.paint(7.5f, muted)) }

        val greet = if (d.customerName.isNotBlank()) "Dear ${d.customerName}, thank you for travelling with us." else "Thank you for travelling with us."
        c.drawText(greet, m, 122f, D.paint(9.5f, ink))

        // TRIP #n
        val tripP = D.paint(30f, ink, F.sansBold)
        c.drawText("TRIP", m, 162f, tripP)
        c.drawText("#${d.seq}", m + tripP.measureText("TRIP") + 4f, 162f, D.paint(30f, accent, F.sansLight))
        c.drawText(d.tripType.ifBlank { "Trip" }, PAGE_W / 2 + 20f, 150f, D.paint(8f, muted))
        c.drawText(Fmt.dayDate(d.createdAt), PAGE_W / 2 + 20f, 164f, D.paint(13f, ink, F.sansBold))
        D.line(c, m, 172f, right, 172f, ink, 1.2f)

        // Legs
        val colW = PAGE_W / 2 - m - 44f
        val rx = PAGE_W / 2 + 20f
        val top = 186f
        val hL = leg(w, d.pickup, d.startTime, m + 2f, top, colW)
        val hR = leg(w, d.drop, d.endTime, rx, top, right - rx)
        if (d.pickup.isNotBlank() || d.drop.isNotBlank()) D.arrow(c, PAGE_W / 2 - 26f, top + 9f, 28f, accent, 2.6f)
        w.y = top + maxOf(hL, hR) + 14f

        // Billed by / customer
        c = w.canvas
        val by = w.y
        c.drawText("Billed by:", m + 2f, by + 8f, D.paint(7.5f, muted))
        c.drawText(D.fit(d.driverName.uppercase(), D.paint(13f, ink, F.sansBold), colW), m + 2f, by + 24f, D.paint(13f, ink, F.sansBold))
        c.drawText(d.carNo, m + 2f, by + 38f, D.paint(9f, ink))
        c.drawText(d.vehicleType, m + 2f, by + 51f, D.paint(9f, ink))
        c.drawText("Customer:", rx, by + 8f, D.paint(7.5f, muted))
        c.drawText(D.fit(d.customerName.ifBlank { "—" }, D.paint(13f, ink, F.sansBold), right - rx), rx, by + 24f, D.paint(13f, ink, F.sansBold))
        if (d.customerMobile.isNotBlank()) c.drawText(d.customerMobile, rx, by + 38f, D.paint(9f, ink))
        w.y = by + 64f

        // Trip facts
        val facts = buildList {
            d.startKmText?.let { add("Start km" to it) }
            d.endKmText?.let { add("End km" to it) }
            if (d.billedKm > 0) add("Total km" to d.billedKmWithSource)
            d.totalHoursText?.let { add("Total hours" to it) }
        }
        Blocks.pairsGrid(w, facts, m + 2f, right - m, 4, D.paint(6.8f, muted, F.sansBold, letterSpacing = 0.08f), D.paint(9.5f, ink, F.sansMedium))

        // FARE
        w.ensure(60f)
        c = w.canvas
        D.line(c, m, w.y + 4f, right, w.y + 4f, ink, 0.8f)
        c.drawText("FARE", m + 2f, w.y + 26f, D.paint(14f, ink, F.sansBold))
        D.line(c, m, w.y + 38f, right, w.y + 38f, ink, 0.8f)
        w.y += 44f
        Blocks.chargeTable(
            w, m, right - m,
            TableStyle(text = ink, muted = ink, headerText = muted, headerSize = 7.5f, labels = listOf("ITEM", "RATE", "QUANTITY", "AMOUNT"), rowH = 22f, size = 10f),
        )
        w.ensure(10f)
        D.line(w.canvas, PAGE_W * 0.32f, w.y + 4f, right, w.y + 4f, ink, 0.8f)
        w.y += 8f
        Blocks.totals(
            w, m + 2f, right - 8f,
            TotalsStyle(text = ink, muted = muted, strongText = ink, size = 10f, strongSize = 12f),
            Blocks.totalRows(d, "Fare", "Total Fare Payable").let { rows ->
                if (d.fare.advancePaise > 0) rows else rows.map { if (it.strong) it.copy(label = "Total Fare Payable") else it }
            },
        )
        w.ensure(12f)
        D.line(w.canvas, m, w.y + 4f, right, w.y + 4f, ink, 0.8f)
        w.y += 12f
        Blocks.amountInWords(w, m + 2f, right - m, ink, size = 8.8f)
        d.paymentLine?.let { Blocks.paragraph(w, it, m + 2f, right - m, D.paint(8.8f, ink, F.sansMedium)) }
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Remarks: ${d.feedback}", m + 2f, right - m, D.paint(8.8f, ink))
        w.y += 10f
        Blocks.closing(w, ClosingStyle(ink, muted, ink), m, right)
        Blocks.footer(w, ink, muted, rule = rule)
        w.finish()
    }

    private fun leg(w: PageWriter, address: String, time: Long?, x: Float, top: Float, width: Float): Float {
        val c = w.canvas
        if (address.isBlank() && time == null) return 0f
        var y = top
        val title = Text.shortPlace(address).ifBlank { "—" }
        c.drawText(D.fit(title, D.paint(15f, ink, F.sansBold), width), x, y + 14f, D.paint(15f, ink, F.sansBold))
        y += 22f
        y += D.block(c, address, x, y, width, D.paint(8.3f, ink), maxLines = 3) + 6f
        if (time != null) {
            c.drawText(Fmt.time(time), x, y + 12f, D.paint(12f, ink, F.sansBold))
            c.drawText(Fmt.dayDate(time), x, y + 25f, D.paint(8.5f, muted))
            y += 28f
        }
        return y - top
    }
}

// ---------------------------------------------------------------------------------------------
// 2. Saffron Route — layout idea from Type2.jpeg, saffron arrows.
// ---------------------------------------------------------------------------------------------
class SaffronTheme : InvoiceTheme(
    2, "Saffron Route", "Clean trip receipt with saffron accents", MapMode.NONE,
    listOf(color("#EE8A2B"), color("#1E2A3A")),
) {
    private val saffron = color("#EE8A2B")
    private val ink = color("#1E2A3A")
    private val rule = color("#2F2F2F")
    private val muted = color("#5F6B7A")
    override val margin = 40f
    override val continuationColor = ink

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.newPage()
        var c = w.canvas

        Logo.wordmark(c, m, 30f, 44f, d.businessName)
        // Boxed INVOICE COPY
        val bp = D.paint(12f, ink, F.sansBold, Paint.Align.CENTER, 0.08f)
        val bw = bp.measureText("INVOICE COPY") + 24f
        D.line(c, right - bw, 34f, right, 34f, ink, 1f)
        c.drawRect(right - bw, 34f, right, 58f, D.stroke(ink, 1.1f))
        c.drawText("INVOICE COPY", right - bw / 2, 50.5f, bp)
        c.drawText(d.dateTimeText, right, 72f, D.paint(8f, muted, align = Paint.Align.RIGHT))
        var hy = 86f
        hy += D.block(c, d.address, m, hy, 300f, D.paint(7.6f, muted), maxLines = 2)
        c.drawText(Blocks.contactLine(d), m, hy + 9f, D.paint(7.6f, muted))
        hy += 12f
        Blocks.gstinLine(d)?.let { c.drawText(it, m, hy + 9f, D.paint(7.6f, muted)); hy += 12f }

        val greet = if (d.customerName.isNotBlank()) "Hi ${Text.firstName(d.customerName)}, thanks for travelling with us." else "Thanks for travelling with us."
        c.drawText(greet, m, hy + 22f, D.paint(9.5f, ink))
        val ty = hy + 60f
        c.drawText("TRIP", m, ty, D.paint(26f, ink, F.sansBold))
        c.drawText(d.number, PAGE_W / 2 + 34f, ty - 2f, D.paint(14f, ink, F.sansBold))
        D.line(c, m, ty + 8f, right, ty + 8f, rule, 1.4f)
        w.y = ty + 18f

        // Leg row
        val colW = PAGE_W / 2 - m - 30f
        val rx = PAGE_W / 2 + 34f
        val top = w.y
        val h1 = place(w, d.pickup, d.startTime, m + 2f, top, colW)
        val h2 = place(w, d.drop, d.endTime, rx, top, right - rx)
        if (d.pickup.isNotBlank() || d.drop.isNotBlank()) D.arrow(w.canvas, PAGE_W / 2 - 6f, top + 9f, 26f, saffron, 2.6f)
        w.y = top + maxOf(h1, h2) + 12f

        // Billed by
        c = w.canvas
        val by = w.y
        c.drawText("Billed by:", m + 2f, by + 8f, D.paint(7.5f, muted))
        c.drawText(D.fit(d.driverName.ifBlank { d.businessName }, D.paint(12f, ink, F.sansBold), colW), m + 2f, by + 23f, D.paint(12f, ink, F.sansBold))
        c.drawText(listOf(d.carNo, d.vehicleType).filter { it.isNotBlank() }.joinToString("  ·  "), m + 2f, by + 37f, D.paint(9f, ink))
        c.drawText("Customer:", rx, by + 8f, D.paint(7.5f, muted))
        c.drawText(D.fit(d.customerName.ifBlank { "—" }, D.paint(12f, ink, F.sansBold), right - rx), rx, by + 23f, D.paint(12f, ink, F.sansBold))
        c.drawText(listOf(d.customerMobile, d.tripType).filter { it.isNotBlank() }.joinToString("  ·  "), rx, by + 37f, D.paint(9f, ink))
        w.y = by + 52f

        // FARE BREAKUP — 4-column grid
        w.ensure(60f)
        c = w.canvas
        D.line(c, m, w.y, right, w.y, rule, 0.8f)
        c.drawText("FARE BREAKUP", m + 2f, w.y + 22f, D.paint(11.5f, ink, F.sansBold, letterSpacing = 0.04f))
        D.line(c, m, w.y + 34f, right, w.y + 34f, rule, 0.8f)
        w.y += 46f
        val cells = buildList {
            d.fare.lines.forEach { line ->
                val detail = listOfNotNull(line.rateText, line.qtyText).joinToString(" × ")
                add(Triple(line.label, detail, Money.format(line.amountPaise)))
            }
        }
        val half = (right - m) / 2
        val lp = D.paint(9.5f, ink)
        val dp = D.paint(8f, muted)
        val vp = D.paint(9.5f, ink, F.sansMedium, Paint.Align.RIGHT)
        cells.chunked(2).forEach { pair ->
            w.ensure(30f)
            val cc = w.canvas
            pair.forEachIndexed { i, (label, detail, value) ->
                val x = m + 2f + i * half
                cc.drawText(D.fit(label, lp, half * 0.55f), x, w.y + 10f, lp)
                if (detail.isNotBlank()) cc.drawText(detail, x, w.y + 22f, dp)
                cc.drawText(value, x + half - 16f, w.y + 10f, vp)
            }
            w.y += 30f
        }
        w.ensure(10f)
        D.line(w.canvas, PAGE_W * 0.35f, w.y, right, w.y, rule, 0.8f)
        w.y += 6f
        Blocks.totals(
            w, m + 2f, right - 4f,
            TotalsStyle(text = ink, muted = muted, strongText = ink, size = 10f, strongSize = 11.5f, rowH = 20f, strongH = 28f),
            Blocks.totalRows(d, "Fare", "Total Fare Payable").let { rows ->
                if (d.fare.advancePaise > 0) rows else rows.map { if (it.strong) it.copy(label = "Total Fare Payable") else it }
            },
        )
        w.ensure(30f)
        c = w.canvas
        D.line(c, m, w.y + 2f, right, w.y + 2f, rule, 1.4f)
        w.y += 8f
        val mode = d.paidMode?.takeIf { d.paidAt != null } ?: d.paymentMode
        if (mode.isNotBlank()) {
            c.drawText(if (d.paidAt != null) "Paid through $mode" else "Payment mode: $mode", m + 2f, w.y + 14f, D.paint(10f, ink))
            val paidAmount = if (d.paidAt != null) d.fare.totalPaise else d.fare.advancePaise
            if (paidAmount > 0) c.drawText(Money.format(paidAmount), right - 4f, w.y + 14f, D.paint(10f, ink, align = Paint.Align.RIGHT))
            w.y += 22f
            D.line(c, m, w.y, right, w.y, rule, 0.8f)
            w.y += 8f
        }
        Blocks.amountInWords(w, m + 2f, right - m, ink, size = 8.8f)
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Remarks: ${d.feedback}", m + 2f, right - m, D.paint(8.8f, ink))
        w.y += 10f
        Blocks.closing(w, ClosingStyle(ink, muted, ink), m, right)
        Blocks.footer(w, ink, muted)
        w.finish()
    }

    private fun place(w: PageWriter, address: String, time: Long?, x: Float, top: Float, width: Float): Float {
        if (address.isBlank() && time == null) return 0f
        val c = w.canvas
        var y = top
        c.drawText(D.fit(Text.shortPlace(address).ifBlank { "—" }, D.paint(13f, ink, F.sansBold), width), x, y + 13f, D.paint(13f, ink, F.sansBold))
        y += 19f
        y += D.block(c, address, x, y, width, D.paint(8.3f, ink), maxLines = 3) + 6f
        if (time != null) {
            c.drawText(Fmt.time(time), x, y + 12f, D.paint(11f, ink, F.sansBold))
            c.drawText(Fmt.dayDate(time), x + 70f, y + 12f, D.paint(11f, ink, F.sansBold))
            y += 18f
        }
        return y - top
    }
}

// ---------------------------------------------------------------------------------------------
// 3. Apricot Ledger — layout idea from Type3.jpeg, softer brick red and apricot.
// ---------------------------------------------------------------------------------------------
class ApricotTheme : InvoiceTheme(
    3, "Apricot Ledger", "Classic ledger table with a soft apricot strip", MapMode.NONE,
    listOf(color("#F8CFAE"), color("#C4553E")),
) {
    private val brick = color("#C4553E")
    private val apricot = color("#F8CFAE")
    private val lines = color("#9A9A9A")
    private val ink = color("#222222")
    private val muted = color("#666666")
    override val margin = 42f
    override val continuationColor = brick

    override fun drawBackground(w: PageWriter) {
        // Faint car watermark
        val c = w.canvas
        c.save()
        D.car(c, 92f, 330f, 410f, Color.argb(16, 122, 122, 122), Color.WHITE)
        c.restore()
    }

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.newPage()
        var c = w.canvas
        c.drawText("${d.city.ifBlank { "Pune" }}", PAGE_W / 2, 26f, D.paint(7.5f, muted, F.serifItalic, Paint.Align.CENTER))
        Logo.draw(c, m, 40f, 34f)
        c.drawText(d.businessName, m + 42f, 60f, D.paint(19f, brick, F.serif))
        var y = 70f
        y += D.block(c, d.address, m + 42f, y, 280f, D.paint(8f, muted, F.serif), maxLines = 2)
        c.drawText(Blocks.contactLine(d), m + 42f, y + 10f, D.paint(8f, muted, F.serif))
        y += 14f
        Blocks.gstinLine(d)?.let { c.drawText(it, m + 42f, y + 10f, D.paint(8f, muted, F.serif)); y += 12f }
        val tp = D.paint(15f, color("#7A7A7A"), F.serifBold, Paint.Align.RIGHT, 0.06f)
        c.drawText("TAXI", right, 52f, tp)
        c.drawText("SERVICE", right, 70f, tp)
        c.drawText("INVOICE", right, 88f, tp)

        val sy = maxOf(y + 18f, 112f)
        D.rect(c, m, sy, right, sy + 22f, apricot)
        c.drawText("Invoice # ${d.number}", m + 8f, sy + 15f, D.paint(9.5f, ink, F.serifBold))
        c.drawText("Date: ${d.dateTimeText}", right - 8f, sy + 15f, D.paint(9.5f, ink, F.serifBold, Paint.Align.RIGHT))
        w.y = sy + 36f

        // Customer block
        c = w.canvas
        if (d.customerName.isNotBlank()) {
            c.drawText(d.customerName, m, w.y + 10f, D.paint(10.5f, ink, F.serifBold)); w.y += 15f
        }
        if (d.customerMobile.isNotBlank()) { c.drawText("Mobile: ${d.customerMobile}", m, w.y + 10f, D.paint(9f, ink, F.serif)); w.y += 14f }
        val sp = D.paint(9f, ink, F.serif)
        if (d.pickup.isNotBlank()) Blocks.paragraph(w, "Pick-up: ${d.pickup}", m, right - m, sp, 2f)
        if (d.drop.isNotBlank()) Blocks.paragraph(w, "Drop: ${d.drop}", m, right - m, sp, 2f)
        val vehicle = listOf(d.vehicleType, d.carNo).filter { it.isNotBlank() }.joinToString(" · ")
        val driver = listOf(d.driverName, d.driverMobile).filter { it.isNotBlank() }.joinToString(" · ")
        val meta = listOfNotNull(
            vehicle.takeIf { it.isNotBlank() }?.let { "Vehicle: $it" },
            driver.takeIf { it.isNotBlank() }?.let { "Driver: $it" },
            d.tripType.takeIf { it.isNotBlank() }?.let { "Trip: $it" },
        ).joinToString("     ")
        Blocks.paragraph(w, meta, m, right - m, sp, 2f)
        val kmLine = listOfNotNull(
            d.startKmText?.let { "Start km: $it" }, d.endKmText?.let { "End km: $it" },
            d.startTime?.let { "Start: ${Fmt.dateTime(it)}" }, d.endTime?.let { "End: ${Fmt.dateTime(it)}" },
        ).joinToString("     ")
        Blocks.paragraph(w, kmLine, m, right - m, D.paint(8.5f, muted, F.serif), 2f)
        w.y += 10f

        // Ledger table: Description / Hours / Km / Amount
        tableWithHoursKm(w, d, m, right)

        // Comments box + totals box
        val rows = Blocks.totalRows(d, "Total", "Balance")
        val boxH = maxOf(80f, rows.size * 18f + 14f)
        w.ensure(boxH + 30f)
        c = w.canvas
        val top = w.y + 14f
        val lw = (right - m) * 0.55f
        c.drawRect(m, top, m + lw, top + boxH, D.stroke(lines, 0.8f))
        c.drawText("Comments or Special Instructions", m + 8f, top + 14f, D.paint(8.5f, ink, F.serifBold))
        val notes = listOfNotNull(d.feedback.takeIf { it.isNotBlank() }, d.paymentLine).joinToString("\n")
        D.block(c, notes.ifBlank { "—" }, m + 8f, top + 20f, lw - 16f, D.paint(8.5f, ink, F.serif), maxLines = 4)
        val rx = m + lw + 16f
        var ry = top + 4f
        rows.forEach { r ->
            val col = if (r.strong) brick else ink
            val tf = if (r.strong) F.serifBold else F.serif
            c.drawText(r.label.uppercase(), rx, ry + 12f, D.paint(8.5f, col, tf))
            c.drawText(r.value, right - 4f, ry + 12f, D.paint(if (r.strong) 11f else 9f, col, tf, Paint.Align.RIGHT))
            D.line(c, rx, ry + 17f, right, ry + 17f, lines, 0.5f)
            ry += 18f
        }
        w.y = top + boxH + 10f
        Blocks.amountInWords(w, m, right - m, ink, F.serifItalic, 9f)
        w.y += 8f
        Blocks.closing(w, ClosingStyle(ink, muted, ink, F.serif, F.serifBold), m, right)
        Blocks.footer(w, ink, muted, F.serif)
        w.finish()
    }

    private fun tableWithHoursKm(w: PageWriter, d: InvoiceDoc, m: Float, right: Float) {
        val hoursX = m + (right - m) * 0.50f
        val kmX = m + (right - m) * 0.66f
        val amtL = m + (right - m) * 0.80f
        fun header() {
            val c = w.canvas
            D.rect(c, m, w.y, right, w.y + 20f, apricot)
            val hp = D.paint(9f, ink, F.serifBold, Paint.Align.CENTER)
            c.drawText("Description", (m + hoursX) / 2, w.y + 14f, hp)
            c.drawText("Hours", (hoursX + kmX) / 2, w.y + 14f, hp)
            c.drawText("Km", (kmX + amtL) / 2, w.y + 14f, hp)
            c.drawText("Amount", (amtL + right) / 2, w.y + 14f, hp)
            c.drawRect(m, w.y, right, w.y + 20f, D.stroke(lines, 0.6f))
            w.y += 20f
        }
        w.ensure(60f)
        header()
        val tp = D.paint(9f, ink, F.serif)
        val cp = D.paint(9f, ink, F.serif, Paint.Align.CENTER)
        val ap = D.paint(9f, ink, F.serif, Paint.Align.RIGHT)
        val fareLines = d.fare.lines
        val minRows = 8
        val count = maxOf(fareLines.size, minRows)
        for (i in 0 until count) {
            val line = fareLines.getOrNull(i)
            val desc = when {
                line == null -> ""
                line.kind == com.amitabhtours.invoice.domain.LineKind.DISTANCE ->
                    listOf(d.vehicleType, d.tripType).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { line.label } + " (${line.rateText})"
                line.rateText != null -> "${line.label} (${line.rateText})"
                else -> line.label
            }
            val descH = D.blockHeight(desc, tp, hoursX - m - 12f)
            val h = maxOf(18f, descH + 6f)
            if (w.ensure(h)) header()
            val c = w.canvas
            val top = w.y
            if (line != null) {
                D.block(c, desc, m + 6f, top + (h - descH) / 2, hoursX - m - 12f, tp)
                val base = top + h / 2 + 3.2f
                when (line.kind) {
                    com.amitabhtours.invoice.domain.LineKind.DISTANCE -> {
                        d.totalHoursText?.let { c.drawText(it, (hoursX + kmX) / 2, base, cp) }
                        c.drawText(line.qtyText ?: "", (kmX + amtL) / 2, base, cp)
                    }
                    com.amitabhtours.invoice.domain.LineKind.WAITING -> c.drawText(line.qtyText ?: "", (hoursX + kmX) / 2, base, cp)
                    com.amitabhtours.invoice.domain.LineKind.ALLOWANCE -> c.drawText(line.qtyText ?: "", (hoursX + kmX) / 2, base, cp)
                    else -> Unit
                }
                c.drawText(Money.plain(line.amountPaise), right - 6f, base, ap)
            }
            val lc = lines
            c.drawRect(m, top, right, top + h, D.stroke(lc, 0.5f))
            D.line(c, hoursX, top, hoursX, top + h, lc, 0.5f)
            D.line(c, kmX, top, kmX, top + h, lc, 0.5f)
            D.line(c, amtL, top, amtL, top + h, lc, 0.5f)
            w.y = top + h
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 4. Ride Receipt — layout idea from Uber_style.jpeg. Needs a GPS-tracked trip.
// ---------------------------------------------------------------------------------------------
class RideReceiptTheme : InvoiceTheme(
    4, "Ride Receipt", "App-style receipt with your route map", MapMode.REQUIRED,
    listOf(color("#111111"), color("#E6E6E6")),
) {
    private val ink = color("#111111")
    private val muted = color("#6B6B6B")
    private val divider = color("#E6E6E6")
    override val margin = 44f
    override val continuationColor = ink

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.newPage()
        var c = w.canvas

        c.drawText(d.businessName, m, 46f, D.paint(15f, ink, F.sansMedium))
        c.drawText(d.dateText, right, 40f, D.paint(8f, muted, align = Paint.Align.RIGHT))
        c.drawText(d.timeText, right, 50f, D.paint(8f, muted, align = Paint.Align.RIGHT))
        c.drawText("Invoice ${d.number}", right, 60f, D.paint(8f, muted, align = Paint.Align.RIGHT))
        c.drawText("Thank you for riding, ${Text.firstName(d.customerName)}", m, 96f, D.paint(24f, ink, F.sansBold))
        val ref = d.startTime ?: d.createdAt
        val part = Fmt.partOfDay(ref)
        c.drawText(if (part == "night") "We hope you enjoyed your ride tonight." else "We hope you enjoyed your ride this $part.", m, 114f, D.paint(10f, ink))

        D.line(c, m, 130f, right, 130f, divider, 1f)
        val label = if (d.fare.advancePaise > 0) "Balance due" else "Total"
        val amount = if (d.fare.advancePaise > 0) d.fare.balanceDuePaise else d.fare.totalPaise
        c.drawText(label, m, 160f, D.paint(18f, ink, F.sansBold))
        c.drawText(Money.format(amount), right, 160f, D.paint(18f, ink, F.sansBold, Paint.Align.RIGHT))
        D.line(c, m, 178f, right, 178f, divider, 1f)
        w.y = 186f

        val lp = D.paint(9f, ink)
        val rp = D.paint(9f, ink, align = Paint.Align.RIGHT)
        val items = d.fare.lines.map { l ->
            val detail = listOfNotNull(l.qtyText, l.rateText?.let { "@ $it" }).joinToString(" ")
            Triple(l.label, detail, Money.format(l.amountPaise))
        } + Blocks.totalRows(d).filter { !it.strong }.map { Triple(it.label, "", it.value) }
        items.forEach { (name, detail, value) ->
            w.ensure(20f)
            val cc = w.canvas
            cc.drawText(name, m, w.y + 12f, lp)
            if (detail.isNotBlank()) cc.drawText(detail, m + 150f, w.y + 12f, D.paint(8f, muted))
            cc.drawText(value, right, w.y + 12f, if (value.startsWith("−")) D.paint(9f, color("#1E8E3E"), align = Paint.Align.RIGHT) else rp)
            w.y += 20f
        }
        w.ensure(10f)
        D.line(w.canvas, m, w.y + 4f, right, w.y + 4f, divider, 1f)
        w.y += 14f

        // Payments
        val mode = (if (d.paidAt != null) d.paidMode else null) ?: d.paymentMode
        if (mode.isNotBlank() || d.fare.advancePaise > 0) {
            w.ensure(56f)
            c = w.canvas
            c.drawText("Payments", m, w.y + 16f, D.paint(14f, ink, F.sansBold))
            w.y += 30f
            val iconTop = w.y
            D.roundStroke(c, m, iconTop, m + 22f, iconTop + 14f, 2f, color("#1E8E3E"), 1.2f)
            c.drawCircle(m + 11f, iconTop + 7f, 3f, D.stroke(color("#1E8E3E"), 1.1f))
            val (title, sub, amt) = when {
                d.paidAt != null -> Triple(mode.ifBlank { "Paid" }, "Paid on ${Fmt.dateTime(d.paidAt)}", d.fare.totalPaise)
                d.fare.advancePaise > 0 -> Triple("Advance" + if (mode.isNotBlank()) " · $mode" else "", "Received before the trip", d.fare.advancePaise)
                else -> Triple(mode, "To be paid", d.fare.totalPaise)
            }
            c.drawText(title, m + 34f, iconTop + 6f, D.paint(9f, ink))
            c.drawText(sub, m + 34f, iconTop + 16f, D.paint(7.5f, muted))
            c.drawText(Money.format(amt), right, iconTop + 10f, rp)
            w.y = iconTop + 26f
            D.line(c, m, w.y, right, w.y, divider, 1f)
            w.y += 10f
        }

        // Note
        val note = listOf(d.amountInWords + ".", if (d.gstin.isBlank()) d.gstNote else "GSTIN: ${d.gstin}")
            .joinToString(" ")
        Blocks.paragraph(w, note, m, right - m, D.paint(7.6f, ink), 6f)
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Remarks: ${d.feedback}", m, right - m, D.paint(7.6f, ink), 6f)
        w.ensure(8f)
        D.line(w.canvas, m, w.y + 2f, right, w.y + 2f, divider, 1f)
        w.y += 12f

        // Trip details
        w.ensure(60f)
        c = w.canvas
        c.drawText("Trip details", m, w.y + 16f, D.paint(14f, ink, F.sansBold))
        w.y += 30f
        D.car(c, m, w.y + 2f, 34f, color("#D5D8DC"), Color.WHITE)
        c.drawText(d.vehicleType.ifBlank { "Car" }, m + 46f, w.y + 7f, D.paint(9f, ink))
        val mins = d.tripDurationMs?.let { ", ${it / 60000} minutes" } ?: ""
        c.drawText("${com.amitabhtours.invoice.domain.Num.clean(d.billedKm)} kilometres$mins", m + 46f, w.y + 18f, D.paint(7.8f, muted))
        c.drawText("Licence plate:", right, w.y + 7f, D.paint(9f, ink, align = Paint.Align.RIGHT))
        c.drawText(d.carNo, right, w.y + 18f, D.paint(7.8f, muted, align = Paint.Align.RIGHT))
        w.y += 34f
        D.rect(c, 0f, w.y, PAGE_W, w.y + 6f, color("#F3F3F3"))
        w.y += 18f

        // Timeline + map
        val mapW = 230f
        val mapH = 180f
        w.ensure(mapH + 10f)
        c = w.canvas
        val top = w.y
        val tlW = right - m - mapW - 22f
        val dotX = m + 4f
        var ty = top + 6f
        c.drawCircle(dotX, ty + 4f, 3.4f, D.fill(ink))
        c.drawCircle(dotX, ty + 4f, 1.3f, D.fill(Color.WHITE))
        d.startTime?.let { c.drawText(Fmt.time(it), m + 16f, ty + 7f, D.paint(9f, ink)) }
        val pH = D.block(c, d.pickup.ifBlank { "Pickup" }, m + 16f, ty + 11f, tlW - 16f, D.paint(7.6f, muted), maxLines = 5)
        val endY = ty + 11f + pH + 24f
        D.line(c, dotX, ty + 8f, dotX, endY, ink, 1.1f)
        c.drawRect(dotX - 3.4f, endY, dotX + 3.4f, endY + 6.8f, D.fill(ink))
        d.endTime?.let { c.drawText(Fmt.time(it), m + 16f, endY + 7f, D.paint(9f, ink)) }
        D.block(c, d.drop.ifBlank { "Drop" }, m + 16f, endY + 11f, tlW - 16f, D.paint(7.6f, muted), maxLines = 5)
        val mapRect = RectF(right - mapW, top, right, top + mapH)
        val map = d.mapProvider?.invoke((mapW * 1.6f).toInt(), (mapH * 1.6f).toInt(), ink)
        if (map != null) D.bitmapRounded(c, map, mapRect, 10f) else D.round(c, mapRect.left, mapRect.top, mapRect.right, mapRect.bottom, 10f, color("#EEF1F4"))
        w.y = top + mapH + 14f

        // Your driver
        w.ensure(40f)
        c = w.canvas
        D.roundStroke(c, m, w.y, right, w.y + 28f, 6f, divider, 1f)
        c.drawText("Your driver: ${d.driverName.ifBlank { d.businessName }}", m + 12f, w.y + 18f, D.paint(9f, ink))
        if (d.driverMobile.isNotBlank()) c.drawText(d.driverMobile, right - 12f, w.y + 18f, D.paint(9f, ink, F.sansMedium, Paint.Align.RIGHT))
        w.y += 42f

        Blocks.closing(w, ClosingStyle(ink, muted, color("#9A9A9A")), m, right)
        val small = listOfNotNull(d.address, Blocks.contactLine(d)).joinToString("  ·  ")
        Blocks.paragraph(w, small, m, right - m, D.paint(7f, muted), 2f, Layout.Alignment.ALIGN_CENTER)
        Blocks.footer(w, ink, muted)
        w.finish()
    }
}

// ---------------------------------------------------------------------------------------------
// 5. Executive Navy — formal / standard.
// ---------------------------------------------------------------------------------------------
class ExecutiveNavyTheme : InvoiceTheme(
    5, "Executive Navy", "Formal navy and gold, suits corporate clients", MapMode.NONE,
    listOf(color("#1B2A4A"), color("#C9A227")),
) {
    private val navy = color("#1B2A4A")
    private val gold = color("#C9A227")
    private val zebra = color("#F3F5F9")
    private val ink = color("#1F2937")
    private val muted = color("#6B7280")
    private val border = color("#D9DEE7")
    override val margin = 40f
    override val continuationColor = navy

    override fun draw(w: PageWriter) {
        val d = w.doc
        val m = margin
        val right = PAGE_W - m
        w.newPage()
        var c = w.canvas

        // Header
        val addrP = D.paint(8f, color("#D5DBE7"))
        val addrH = D.blockHeight(d.address, addrP, 290f, maxLines = 2)
        val headerH = maxOf(128f, 76f + addrH + 26f)
        D.rect(c, 0f, 0f, PAGE_W, headerH, navy)
        D.rect(c, 0f, headerH, PAGE_W, headerH + 3f, gold)
        Logo.draw(c, m, 30f, 40f, Logo.Style.ON_DARK)
        c.drawText(d.businessName, m + 50f, 50f, D.paint(18f, Color.WHITE, F.serifBold))
        c.drawText(d.city.ifBlank { "Pune" }.uppercase() + "  ·  CAB & TAXI SERVICE", m + 50f, 64f, D.paint(7f, gold, F.sansBold, letterSpacing = 0.18f))
        D.block(c, d.address, m, 80f, 290f, addrP, maxLines = 2)
        c.drawText(Blocks.contactLine(d), m, 80f + addrH + 12f, addrP)
        Blocks.gstinLine(d)?.let { c.drawText(it, m, 80f + addrH + 24f, addrP) }
        c.drawText("INVOICE", right, 58f, D.paint(28f, gold, F.serifBold, Paint.Align.RIGHT, 0.08f))
        c.drawText("No. ${d.number}", right, 80f, D.paint(10f, Color.WHITE, F.sansMedium, Paint.Align.RIGHT))
        c.drawText(d.dateTimeText, right, 94f, D.paint(8.5f, color("#D5DBE7"), align = Paint.Align.RIGHT))
        w.y = headerH + 22f

        // Two cards
        val gap = 14f
        val cw = (right - m - gap) / 2
        val lp = D.paint(6.8f, muted, F.sansBold, letterSpacing = 0.1f)
        val vp = D.paint(9f, ink)
        val bill = buildList {
            add(d.customerName.ifBlank { "—" })
            if (d.customerMobile.isNotBlank()) add("Mobile: ${d.customerMobile}")
            if (d.pickup.isNotBlank()) add("From: ${d.pickup}")
            if (d.drop.isNotBlank()) add("To: ${d.drop}")
        }.joinToString("\n")
        val trip = d.tripPairs().joinToString("\n") { "${it.first}: ${it.second}" }
        val h1 = D.blockHeight(bill, vp, cw - 24f, spacingMult = 1.08f)
        val h2 = D.blockHeight(trip, vp, cw - 24f, spacingMult = 1.08f)
        val cardH = maxOf(h1, h2) + 36f
        w.ensure(cardH + 10f)
        c = w.canvas
        val top = w.y
        listOf(Triple(m, "BILLED TO", bill), Triple(m + cw + gap, "TRIP DETAILS", trip)).forEach { (x, title, body) ->
            D.round(c, x, top, x + cw, top + cardH, 6f, color("#FAFBFD"))
            D.roundStroke(c, x, top, x + cw, top + cardH, 6f, border)
            D.rect(c, x, top + 8f, x + 3f, top + 26f, gold)
            c.drawText(title, x + 12f, top + 20f, lp)
            D.block(c, body, x + 12f, top + 28f, cw - 24f, vp, spacingMult = 1.08f)
        }
        w.y = top + cardH + 18f

        Blocks.chargeTable(
            w, m, right - m,
            TableStyle(text = ink, muted = muted, headerText = Color.WHITE, headerBg = navy, zebra = zebra, rowH = 22f, size = 9.5f, headerSize = 7.8f, rule = border),
        )
        w.y += 10f
        // Totals box on the right, payment info on the left
        val rows = Blocks.totalRows(d)
        val boxW = 220f
        val boxH = rows.sumOf { (if (it.strong) 28f else 19f).toDouble() }.toFloat()
        w.ensure(boxH + 8f)
        val tTop = w.y
        c = w.canvas
        d.paymentLine?.let {
            c.drawText("PAYMENT", m, tTop + 10f, lp)
            D.block(c, it, m, tTop + 14f, right - m - boxW - 20f, D.paint(9f, ink))
        }
        Blocks.totals(w, right - boxW, right, TotalsStyle(text = ink, muted = muted, strongText = Color.WHITE, strongBg = navy, rule = border, rowH = 19f, strongH = 28f, strongSize = 11f), rows)
        w.y += 10f
        w.ensure(30f)
        c = w.canvas
        D.round(c, m, w.y, right, w.y + 24f, 4f, zebra)
        D.block(c, "Amount in words: ${d.amountInWords}", m + 10f, w.y + 7f, right - m - 20f, D.paint(8.5f, navy, F.sansMedium), maxLines = 2)
        w.y += 34f
        if (d.feedback.isNotBlank()) Blocks.paragraph(w, "Remarks: ${d.feedback}", m, right - m, D.paint(8.5f, ink))
        w.y += 8f
        Blocks.closing(w, ClosingStyle(ink, muted, navy), m, right)
        Blocks.footer(w, navy, muted, rule = gold)
        w.finish()
    }
}
