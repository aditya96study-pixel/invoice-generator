package com.amitabhtours.invoice.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import com.amitabhtours.invoice.domain.Money

enum class MapMode { NONE, REQUIRED, OPTIONAL }

abstract class InvoiceTheme(
    val id: Int,
    val name: String,
    val tagline: String,
    val mapMode: MapMode,
    /** Two or three colours for the picker's swatch. */
    val swatch: List<Int>,
) {
    open val margin: Float = 40f
    open val continuationColor: Int = color("#14213D")

    abstract fun draw(w: PageWriter)

    open fun drawBackground(w: PageWriter) {}

    open fun drawPageDecorations(w: PageWriter) {}

    /** Header strip on page 2+; returns the y where content continues. */
    open fun drawContinuationHeader(w: PageWriter): Float {
        val c = w.canvas
        val d = w.doc
        val m = margin
        Logo.draw(c, m, 26f, 22f)
        c.drawText(d.businessName, m + 30f, 41f, D.paint(10.5f, continuationColor, F.sansBold))
        c.drawText("Invoice ${d.number} (continued)", PAGE_W - m, 41f, D.paint(8.5f, color("#5B6478"), align = Paint.Align.RIGHT))
        D.line(c, m, 56f, PAGE_W - m, 56f, continuationColor, 1f)
        return 74f
    }

    fun isAvailable(doc: InvoiceDoc): Boolean = mapMode != MapMode.REQUIRED || doc.hasRoute
}

data class TableStyle(
    val text: Int,
    val muted: Int,
    val headerText: Int,
    val headerBg: Int? = null,
    val headerTypeface: Typeface = F.sansBold,
    val headerSize: Float = 8f,
    val rule: Int? = null,
    val headerRule: Int? = null,
    val zebra: Int? = null,
    val rowH: Float = 20f,
    val size: Float = 9.5f,
    val typeface: Typeface = F.sans,
    val amountTypeface: Typeface = F.sans,
    val labels: List<String> = listOf("DESCRIPTION", "RATE", "QTY", "AMOUNT"),
    val showRateQty: Boolean = true,
    val fillerRows: Int = 0,
    val verticalRules: Int? = null,
    val headerH: Float = 22f,
    val letterSpacing: Float = 0.06f,
)

data class TotalsStyle(
    val text: Int,
    val muted: Int,
    val strongText: Int,
    val strongBg: Int? = null,
    val rule: Int? = null,
    val size: Float = 9.5f,
    val strongSize: Float = 11.5f,
    val typeface: Typeface = F.sans,
    val strongTypeface: Typeface = F.sansBold,
    val rowH: Float = 18f,
    val strongH: Float = 26f,
)

data class ClosingStyle(
    val text: Int,
    val muted: Int,
    val line: Int,
    val typeface: Typeface = F.sans,
    val boldTypeface: Typeface = F.sansBold,
)

data class TotalRow(val label: String, val value: String, val strong: Boolean = false, val sub: Boolean = false)

/** Building blocks shared by the themes. All of them advance [PageWriter.y]. */
object Blocks {

    fun totalRows(d: InvoiceDoc, totalLabel: String = "Total fare", balanceLabel: String = "Balance due"): List<TotalRow> {
        val f = d.fare
        val rows = ArrayList<TotalRow>()
        if (f.roundOffPaise != 0L) {
            rows += TotalRow("Subtotal", Money.format(f.subtotalPaise))
            val sign = if (f.roundOffPaise > 0) "+ " else "− "
            rows += TotalRow("Rounded off", sign + Money.format(kotlin.math.abs(f.roundOffPaise)), sub = true)
        }
        if (f.advancePaise > 0) {
            rows += TotalRow(totalLabel, Money.format(f.totalPaise))
            rows += TotalRow("Advance paid", "− " + Money.format(f.advancePaise))
            rows += TotalRow(balanceLabel, Money.format(f.balanceDuePaise), strong = true)
        } else {
            rows += TotalRow(totalLabel, Money.format(f.totalPaise), strong = true)
        }
        return rows
    }

    fun chargeTable(w: PageWriter, x: Float, width: Float, s: TableStyle) {
        val d = w.doc
        val right = x + width
        val rateX = x + width * 0.50f
        val qtyX = x + width * 0.70f
        val pad = 8f

        fun header() {
            val c = w.canvas
            val top = w.y
            s.headerBg?.let { D.rect(c, x, top, right, top + s.headerH, it) }
            val hp = D.paint(s.headerSize, s.headerText, s.headerTypeface, letterSpacing = s.letterSpacing)
            val base = top + s.headerH / 2 + s.headerSize * 0.36f
            c.drawText(s.labels[0], x + pad, base, hp)
            if (s.showRateQty) {
                c.drawText(s.labels[1], rateX, base, hp)
                c.drawText(s.labels[2], qtyX, base, hp)
            }
            c.drawText(s.labels[3], right - pad, base, D.paint(s.headerSize, s.headerText, s.headerTypeface, Paint.Align.RIGHT, s.letterSpacing))
            s.headerRule?.let { D.line(c, x, top + s.headerH, right, top + s.headerH, it, 1f) }
            w.y = top + s.headerH
        }

        w.ensure(s.headerH + s.rowH * 2)
        header()
        val tp = D.paint(s.size, s.text, s.typeface)
        val mp = D.paint(s.size - 0.5f, s.muted, s.typeface)
        val ap = D.paint(s.size, s.text, s.amountTypeface, Paint.Align.RIGHT)
        val labelWidth = (if (s.showRateQty) rateX - x else right - x - 90f) - pad * 2
        val rows = d.fare.lines
        val total = rows.size + s.fillerRows
        for (i in 0 until total) {
            val line = rows.getOrNull(i)
            val labelH = if (line != null) D.blockHeight(line.label, tp, labelWidth) else 0f
            val h = maxOf(s.rowH, labelH + 8f)
            if (w.ensure(h)) header()
            val c = w.canvas
            val top = w.y
            s.zebra?.let { if (i % 2 == 1) D.rect(c, x, top, right, top + h, it) }
            if (line != null) {
                val textTop = top + (h - labelH) / 2
                D.block(c, line.label, x + pad, textTop, labelWidth, tp)
                val base = top + h / 2 + s.size * 0.36f
                if (s.showRateQty) {
                    line.rateText?.let { c.drawText(it, rateX, base, mp) }
                    line.qtyText?.let { c.drawText(it, qtyX, base, mp) }
                }
                c.drawText(Money.format(line.amountPaise), right - pad, base, ap)
            }
            s.rule?.let { D.line(c, x, top + h, right, top + h, it, 0.6f) }
            s.verticalRules?.let {
                D.line(c, x, top, x, top + h, it, 0.6f)
                D.line(c, right, top, right, top + h, it, 0.6f)
                if (s.showRateQty) {
                    D.line(c, rateX - pad, top, rateX - pad, top + h, it, 0.6f)
                    D.line(c, qtyX - pad, top, qtyX - pad, top + h, it, 0.6f)
                }
                D.line(c, right - 92f, top, right - 92f, top + h, it, 0.6f)
            }
            w.y = top + h
        }
        if (rows.isEmpty() && s.fillerRows == 0) {
            val c = w.canvas
            c.drawText("No charges entered", x + pad, w.y + 14f, mp)
            w.y += s.rowH
        }
    }

    /** Totals aligned to the right edge. */
    fun totals(w: PageWriter, left: Float, right: Float, s: TotalsStyle, rows: List<TotalRow> = totalRows(w.doc)) {
        val needed = rows.sumOf { (if (it.strong) s.strongH else s.rowH).toDouble() }.toFloat()
        w.ensure(needed + 4f)
        for (r in rows) {
            val c = w.canvas
            val h = if (r.strong) s.strongH else s.rowH
            val top = w.y
            if (r.strong && s.strongBg != null) D.rect(c, left, top + 2f, right, top + h, s.strongBg)
            val size = if (r.strong) s.strongSize else s.size
            val tf = if (r.strong) s.strongTypeface else s.typeface
            val col = if (r.strong) s.strongText else if (r.sub) s.muted else s.text
            val base = top + h / 2 + size * 0.38f + (if (r.strong && s.strongBg != null) 1f else 0f)
            val inset = if (r.strong && s.strongBg != null) 10f else 0f
            c.drawText(r.label, left + inset, base, D.paint(size, col, tf))
            c.drawText(r.value, right - inset, base, D.paint(size, col, tf, Paint.Align.RIGHT))
            if (!r.strong) s.rule?.let { D.line(c, left, top + h, right, top + h, it, 0.5f) }
            w.y = top + h
        }
    }

    fun amountInWords(w: PageWriter, x: Float, width: Float, color: Int, typeface: Typeface = F.sans, size: Float = 8.5f, label: String = "Amount in words: ") {
        val d = w.doc
        val p = D.paint(size, color, typeface)
        val s = label + d.amountInWords
        val h = D.blockHeight(s, p, width)
        w.ensure(h + 4f)
        D.block(w.canvas, s, x, w.y, width, p)
        w.y += h + 4f
    }

    fun paragraph(w: PageWriter, s: String, x: Float, width: Float, p: android.text.TextPaint, gapAfter: Float = 4f, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL) {
        if (s.isBlank()) return
        val h = D.blockHeight(s, p, width)
        w.ensure(h + gapAfter)
        D.block(w.canvas, s, x, w.y, width, p, align)
        w.y += h + gapAfter
    }

    /** Label/value grid, [cols] columns. */
    fun pairsGrid(
        w: PageWriter, pairs: List<Pair<String, String>>, x: Float, width: Float, cols: Int,
        labelP: android.text.TextPaint, valueP: android.text.TextPaint, rowGap: Float = 6f, labelAbove: Boolean = true,
    ) {
        if (pairs.isEmpty()) return
        val colW = width / cols
        pairs.chunked(cols).forEach { row ->
            val heights = row.map { (l, v) ->
                if (labelAbove) labelP.textSize + 3f + D.blockHeight(v, valueP, colW - 10f)
                else D.blockHeight("$l: $v", valueP, colW - 10f)
            }
            val h = heights.max()
            w.ensure(h + rowGap)
            val c = w.canvas
            row.forEachIndexed { i, (l, v) ->
                val cx = x + i * colW
                if (labelAbove) {
                    c.drawText(l.uppercase(), cx, w.y + labelP.textSize, labelP)
                    D.block(c, v, cx, w.y + labelP.textSize + 3f, colW - 10f, valueP)
                } else {
                    D.block(c, "$l: $v", cx, w.y, colW - 10f, valueP)
                }
            }
            w.y += h + rowGap
        }
    }

    /**
     * QR (optional) on the left, customer signature in the middle, owner signature/stamp on the right.
     */
    fun closing(w: PageWriter, s: ClosingStyle, left: Float, right: Float) {
        val d = w.doc
        val height = 98f
        w.ensure(height + 6f)
        val c = w.canvas
        val top = w.y
        var x = left
        if (d.upiQr != null) {
            val qs = 70f
            D.bitmapFit(c, d.upiQr, x, top, qs, qs, 0f, 0f, smooth = false)
            val cap = D.paint(7.2f, s.text, s.boldTypeface)
            c.drawText("Scan to pay ${Money.format(d.fare.balanceDuePaise)}", x, top + qs + 10f, cap)
            c.drawText("UPI: ${d.upiId}", x, top + qs + 19f, D.paint(6.5f, s.muted, s.typeface))
            x += 118f
        }
        val sigLine = top + 70f
        // Customer
        val custW = 140f
        d.customerSignature?.let { D.bitmapFit(c, it, x, sigLine - 46f, custW, 44f, 0.5f, 1f) }
        D.line(c, x, sigLine, x + custW, sigLine, s.line, 0.7f)
        c.drawText("Customer Signature", x, sigLine + 12f, D.paint(8f, s.muted, s.typeface))
        // Owner
        val ownW = 170f
        val ox = right - ownW
        val forP = D.paint(8f, s.text, s.boldTypeface, Paint.Align.RIGHT)
        c.drawText(D.fit("For ${d.businessName}", forP, ownW), right, top + 10f, forP)
        d.ownerStamp?.let { D.bitmapFit(c, it, ox, top + 14f, 62f, 62f, 0f, 0.5f) }
        d.ownerSignature?.let { D.bitmapFit(c, it, ox + 24f, sigLine - 48f, ownW - 24f, 46f, 0.5f, 1f) }
        D.line(c, ox + 20f, sigLine, right, sigLine, s.line, 0.7f)
        c.drawText("Authorised Signatory", right, sigLine + 12f, D.paint(8f, s.muted, s.typeface, Paint.Align.RIGHT))
        w.y = top + height
    }

    /** Thank-you line + GST note, pushed to the bottom of the last page. */
    fun footer(w: PageWriter, textColor: Int, mutedColor: Int, typeface: Typeface = F.sans, centerX: Float = PAGE_W / 2, width: Float = PAGE_W - 80f, rule: Int? = null) {
        val d = w.doc
        val p1 = D.paint(8.5f, textColor, F.sansMedium.takeIf { typeface == F.sans } ?: typeface)
        val p2 = D.paint(7.2f, mutedColor, typeface)
        val note = if (d.gstin.isBlank()) d.gstNote else ""
        val h1 = D.blockHeight(d.footerText, p1, width)
        val h2 = D.blockHeight(note, p2, width)
        val total = h1 + h2 + 14f
        // The footer may sit in the bottom margin (above the page number) rather than start a page of its own.
        if (w.y + 4f + total > PAGE_H - 28f) w.ensure(total)
        val top = maxOf(w.y + 4f, minOf(w.bottomLimit, PAGE_H - 28f) - total)
        val c = w.canvas
        rule?.let { D.line(c, centerX - width / 2, top, centerX + width / 2, top, it, 0.5f) }
        D.block(c, d.footerText, centerX - width / 2, top + 6f, width, p1, Layout.Alignment.ALIGN_CENTER)
        D.block(c, note, centerX - width / 2, top + 8f + h1, width, p2, Layout.Alignment.ALIGN_CENTER)
        w.y = top + total
    }

    fun contactLine(d: InvoiceDoc): String = buildList {
        if (d.phone.isNotBlank()) add("Mob: ${d.phone}")
        if (d.email.isNotBlank()) add("Email: ${d.email}")
    }.joinToString("  ·  ")

    fun gstinLine(d: InvoiceDoc): String? = d.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" }
}

object ThemeCatalog {
    val all: List<InvoiceTheme> = listOf(
        MarigoldTheme(),
        SaffronTheme(),
        ApricotTheme(),
        RideReceiptTheme(),
        ExecutiveNavyTheme(),
        PrintSaverTheme(),
        RouteMapTheme(),
        HeritageTheme(),
        JourneyPassTheme(),
        RoyalMaroonTheme(),
    )

    fun byId(id: Int): InvoiceTheme = all.firstOrNull { it.id == id } ?: all[4]

    val transparent = Color.TRANSPARENT
}
