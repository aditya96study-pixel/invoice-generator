package com.amitabhtours.invoice.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

const val PAGE_W = 595f
const val PAGE_H = 842f

/** Where pages go: a real PDF, a dry run (to count pages), or a bitmap thumbnail. */
interface PageSink {
    fun begin(index: Int): Canvas
    fun end()
}

class PdfSink(private val doc: PdfDocument) : PageSink {
    private var page: PdfDocument.Page? = null
    override fun begin(index: Int): Canvas {
        val info = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), index).create()
        val p = doc.startPage(info)
        page = p
        return p.canvas
    }

    override fun end() {
        page?.let { doc.finishPage(it) }
        page = null
    }
}

/** Records pages into throw-away Pictures; used to count pages before the real pass. */
class DryRunSink : PageSink {
    private var picture: Picture? = null
    var pages = 0
        private set

    override fun begin(index: Int): Canvas {
        pages++
        val p = Picture()
        picture = p
        return p.beginRecording(PAGE_W.toInt(), PAGE_H.toInt())
    }

    override fun end() {
        picture?.endRecording()
        picture = null
    }
}

/** Draws only the first page into a bitmap (theme thumbnails). */
class BitmapSink(private val widthPx: Int) : PageSink {
    val bitmap: Bitmap = Bitmap.createBitmap(widthPx, (widthPx * PAGE_H / PAGE_W).toInt(), Bitmap.Config.ARGB_8888)
    private val discard = DryRunSink()
    private var first = true

    override fun begin(index: Int): Canvas {
        if (!first) return discard.begin(index)
        val c = Canvas(bitmap)
        c.drawColor(Color.WHITE)
        val s = widthPx / PAGE_W
        c.scale(s, s)
        return c
    }

    override fun end() {
        if (first) first = false else discard.end()
    }
}

/**
 * Flow layout helper. Themes draw from the top with [y] as a cursor and call [ensure] before
 * each block; when space runs out a new page starts with the theme's continuation header.
 */
class PageWriter(
    private val sink: PageSink,
    val doc: InvoiceDoc,
    private val theme: InvoiceTheme,
    val totalPages: Int,
) {
    lateinit var canvas: Canvas
        private set
    var page = 0
        private set
    var y = 0f
    var bottomLimit = PAGE_H - 48f

    fun newPage() {
        if (page > 0) endPage()
        page++
        canvas = sink.begin(page)
        theme.drawBackground(this)
        y = if (page == 1) 0f else theme.drawContinuationHeader(this)
    }

    fun ensure(height: Float): Boolean {
        if (y + height > bottomLimit) {
            newPage()
            return true
        }
        return false
    }

    private fun endPage() {
        theme.drawPageDecorations(this)
        if (totalPages > 1) {
            val p = D.paint(7.5f, Color.parseColor("#8A8F98"), align = Paint.Align.CENTER)
            canvas.drawText("Page $page of $totalPages", PAGE_W / 2, PAGE_H - 16f, p)
        }
        if (doc.cancelled) D.cancelledStamp(canvas)
        sink.end()
    }

    fun finish() {
        if (page > 0) endPage()
    }
}

object F {
    val sans: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val sansMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val sansBold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    val sansBlack: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    val sansLight: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    val condensed: Typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    val condensedBold: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val serif: Typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    val serifItalic: Typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    val mono: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
}

fun color(hex: String): Int = Color.parseColor(hex)

/** Drawing helpers shared by every theme. Units are PDF points. */
object D {
    fun paint(
        size: Float,
        color: Int = Color.BLACK,
        typeface: Typeface = F.sans,
        align: Paint.Align = Paint.Align.LEFT,
        letterSpacing: Float = 0f,
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = align
        this.letterSpacing = letterSpacing
    }

    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    fun stroke(color: Int, width: Float = 0.75f, dash: FloatArray? = null) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        if (dash != null) pathEffect = DashPathEffect(dash, 0f)
    }

    fun text(c: Canvas, s: String, x: Float, baseline: Float, p: TextPaint) = c.drawText(s, x, baseline, p)

    /** Single line, shortened with … if wider than [maxWidth]. */
    fun fit(s: String, p: TextPaint, maxWidth: Float): String =
        TextUtils.ellipsize(s, p, maxWidth, TextUtils.TruncateAt.END).toString()

    fun layout(
        s: String,
        p: TextPaint,
        width: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        spacingMult: Float = 1.0f,
        maxLines: Int = Int.MAX_VALUE,
    ): StaticLayout {
        val b = StaticLayout.Builder.obtain(s, 0, s.length, p, width.toInt().coerceAtLeast(10))
            .setAlignment(align)
            .setLineSpacing(0f, spacingMult)
            .setIncludePad(false)
        if (maxLines != Int.MAX_VALUE) b.setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END)
        return b.build()
    }

    /** Draws wrapped text with its top at [top]; returns the height used. */
    fun block(
        c: Canvas, s: String, x: Float, top: Float, width: Float, p: TextPaint,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, spacingMult: Float = 1.0f, maxLines: Int = Int.MAX_VALUE,
    ): Float {
        if (s.isEmpty()) return 0f
        // StaticLayout ignores Paint.Align; use a left-aligned copy and the Layout alignment instead.
        val lp = TextPaint(p).apply { textAlign = Paint.Align.LEFT }
        val l = layout(s, lp, width, align, spacingMult, maxLines)
        c.save()
        c.translate(x, top)
        l.draw(c)
        c.restore()
        return l.height.toFloat()
    }

    fun blockHeight(s: String, p: TextPaint, width: Float, spacingMult: Float = 1.0f, maxLines: Int = Int.MAX_VALUE): Float {
        if (s.isEmpty()) return 0f
        val lp = TextPaint(p).apply { textAlign = Paint.Align.LEFT }
        return layout(s, lp, width, spacingMult = spacingMult, maxLines = maxLines).height.toFloat()
    }

    fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float = 0.75f) =
        c.drawLine(x1, y1, x2, y2, stroke(color, width))

    fun dashed(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float = 0.75f, on: Float = 3f, off: Float = 3f) =
        c.drawLine(x1, y1, x2, y2, stroke(color, width, floatArrayOf(on, off)))

    fun dotted(c: Canvas, x1: Float, y: Float, x2: Float, color: Int) {
        val p = fill(color)
        var x = x1
        while (x <= x2) {
            c.drawCircle(x, y, 0.55f, p)
            x += 3f
        }
    }

    fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) = c.drawRect(l, t, r, b, fill(color))

    fun round(c: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) =
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, fill(color))

    fun roundStroke(c: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int, width: Float = 0.75f) =
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, stroke(color, width))

    /** Draws [bmp] inside the box keeping its aspect ratio. vAlign: 0 top, 0.5 centre, 1 bottom. */
    fun bitmapFit(c: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float, hAlign: Float = 0.5f, vAlign: Float = 0.5f, smooth: Boolean = true): RectF {
        val s = minOf(w / bmp.width, h / bmp.height)
        val dw = bmp.width * s
        val dh = bmp.height * s
        val x = l + (w - dw) * hAlign
        val y = t + (h - dh) * vAlign
        val dst = RectF(x, y, x + dw, y + dh)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = smooth }
        c.drawBitmap(bmp, null, dst, p)
        return dst
    }

    /** Bitmap clipped into a rounded rectangle (maps). */
    fun bitmapRounded(c: Canvas, bmp: Bitmap, rect: RectF, radius: Float) {
        c.save()
        val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        c.clipPath(path)
        c.drawBitmap(bmp, null, rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
        c.restore()
    }

    fun cancelledStamp(c: Canvas) {
        c.save()
        c.rotate(-32f, PAGE_W / 2, PAGE_H / 2)
        val p = paint(96f, Color.argb(70, 200, 30, 30), F.sansBlack, Paint.Align.CENTER, 0.08f)
        c.drawText("CANCELLED", PAGE_W / 2, PAGE_H / 2 + 30f, p)
        val r = RectF(PAGE_W / 2 - 300f, PAGE_H / 2 - 52f, PAGE_W / 2 + 300f, PAGE_H / 2 + 62f)
        c.drawRoundRect(r, 14f, 14f, stroke(Color.argb(70, 200, 30, 30), 6f))
        c.restore()
    }

    /** Small arrow → used between pickup and drop. */
    fun arrow(c: Canvas, x: Float, y: Float, length: Float, color: Int, width: Float = 2.4f) {
        val p = stroke(color, width).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        c.drawLine(x, y, x + length, y, p)
        val head = length * 0.32f
        val path = Path().apply {
            moveTo(x + length - head, y - head * 0.75f)
            lineTo(x + length, y)
            lineTo(x + length - head, y + head * 0.75f)
        }
        c.drawPath(path, p)
    }

    /** A simple side-view car icon. */
    fun car(c: Canvas, x: Float, y: Float, w: Float, color: Int, windowColor: Int = Color.WHITE) {
        val h = w * 0.42f
        val body = Path().apply {
            moveTo(x, y + h * 0.78f)
            lineTo(x, y + h * 0.5f)
            quadTo(x + w * 0.02f, y + h * 0.38f, x + w * 0.16f, y + h * 0.36f)
            lineTo(x + w * 0.28f, y + h * 0.08f)
            quadTo(x + w * 0.32f, y, x + w * 0.4f, y)
            lineTo(x + w * 0.66f, y)
            quadTo(x + w * 0.72f, y, x + w * 0.76f, y + h * 0.08f)
            lineTo(x + w * 0.88f, y + h * 0.36f)
            quadTo(x + w, y + h * 0.4f, x + w, y + h * 0.55f)
            lineTo(x + w, y + h * 0.78f)
            close()
        }
        c.drawPath(body, fill(color))
        val win = Path().apply {
            moveTo(x + w * 0.22f, y + h * 0.36f)
            lineTo(x + w * 0.32f, y + h * 0.12f)
            lineTo(x + w * 0.49f, y + h * 0.12f)
            lineTo(x + w * 0.49f, y + h * 0.36f)
            close()
            moveTo(x + w * 0.53f, y + h * 0.36f)
            lineTo(x + w * 0.53f, y + h * 0.12f)
            lineTo(x + w * 0.69f, y + h * 0.12f)
            lineTo(x + w * 0.79f, y + h * 0.36f)
            close()
        }
        c.drawPath(win, fill(windowColor))
        val wheelR = h * 0.2f
        c.drawCircle(x + w * 0.24f, y + h * 0.8f, wheelR, fill(color))
        c.drawCircle(x + w * 0.76f, y + h * 0.8f, wheelR, fill(color))
        c.drawCircle(x + w * 0.24f, y + h * 0.8f, wheelR * 0.45f, fill(windowColor))
        c.drawCircle(x + w * 0.76f, y + h * 0.8f, wheelR * 0.45f, fill(windowColor))
    }
}

/** Draws the Amitabh Tours road-and-sun logo (same shapes as branding/logo-icon.svg). */
object Logo {
    enum class Style { FULL, ON_DARK, MONO, MONO_WHITE }

    private fun s(v: Float, size: Float) = v * size / 512f

    fun draw(c: Canvas, x: Float, y: Float, size: Float, style: Style = Style.FULL) {
        c.save()
        c.translate(x, y)
        val navy = color("#14213D")
        val marigold = color("#F5B82E")
        val roadFill = color("#24365F")
        when (style) {
            Style.FULL -> {
                D.round(c, 0f, 0f, size, size, s(112f, size), navy)
            }
            Style.ON_DARK -> Unit
            Style.MONO -> {
                val p = D.stroke(Color.BLACK, s(16f, size))
                c.drawRoundRect(RectF(s(8f, size), s(8f, size), s(504f, size), s(504f, size)), s(108f, size), s(108f, size), p)
            }
            Style.MONO_WHITE -> Unit
        }
        val mono = style == Style.MONO || style == Style.MONO_WHITE
        val monoColor = if (style == Style.MONO) Color.BLACK else Color.WHITE
        // Sun
        if (mono) {
            c.drawCircle(s(256f, size), s(118f, size), s(44f, size), D.stroke(monoColor, s(12f, size)))
        } else {
            c.drawCircle(s(256f, size), s(118f, size), s(44f, size), D.fill(marigold))
            c.drawPath(poly(size, 256f, 92f, 408f, 432f, 104f, 432f), D.fill(roadFill))
        }
        val edge = if (mono) monoColor else Color.WHITE
        c.drawPath(poly(size, 256f, 92f, 104f, 432f, 156f, 432f, 256f, 176f), D.fill(edge))
        c.drawPath(poly(size, 256f, 92f, 408f, 432f, 356f, 432f, 256f, 176f), D.fill(edge))
        val dash = if (mono) monoColor else marigold
        c.drawPath(poly(size, 247f, 432f, 265f, 432f, 263f, 380f, 249f, 380f), D.fill(dash))
        c.drawPath(poly(size, 249.5f, 352f, 262.5f, 352f, 261.2f, 314f, 250.8f, 314f), D.fill(dash))
        c.drawPath(poly(size, 251.5f, 292f, 260.5f, 292f, 259.6f, 264f, 252.4f, 264f), D.fill(dash))
        c.drawPath(poly(size, 253f, 247f, 259f, 247f, 258.5f, 228f, 253.5f, 228f), D.fill(dash))
        c.restore()
    }

    private fun poly(size: Float, vararg pts: Float): Path = Path().apply {
        moveTo(s(pts[0], size), s(pts[1], size))
        var i = 2
        while (i < pts.size) {
            lineTo(s(pts[i], size), s(pts[i + 1], size))
            i += 2
        }
        close()
    }

    /** Logo in a circle badge (Marigold theme). */
    fun badge(c: Canvas, cx: Float, cy: Float, r: Float, bg: Int) {
        c.drawCircle(cx, cy, r, D.fill(bg))
        val size = r * 1.55f
        draw(c, cx - size / 2, cy - size / 2 + r * 0.02f, size, Style.ON_DARK)
    }

    /** Icon + two-line wordmark. Returns the width used. */
    fun wordmark(c: Canvas, x: Float, y: Float, height: Float, businessName: String, dark: Boolean = true, mono: Boolean = false): Float {
        val style = if (mono) Style.MONO else Style.FULL
        draw(c, x, y, height, style)
        val (first, rest) = splitName(businessName)
        val nameColor = if (mono) Color.BLACK else if (dark) color("#14213D") else Color.WHITE
        val subColor = if (mono) Color.BLACK else if (dark) color("#C98A0E") else color("#F5B82E")
        val p1 = D.paint(height * 0.42f, nameColor, F.sansBlack, letterSpacing = 0.04f)
        val p2 = D.paint(height * 0.19f, subColor, F.sansBold, letterSpacing = 0.22f)
        val tx = x + height * 1.18f
        c.drawText(first, tx, y + height * 0.52f, p1)
        if (rest.isNotEmpty()) c.drawText(rest, tx + 1f, y + height * 0.84f, p2)
        return height * 1.18f + maxOf(p1.measureText(first), p2.measureText(rest))
    }

    /** "Amitabh Tours & Travels" -> ("AMITABH", "TOURS & TRAVELS") */
    fun splitName(name: String): Pair<String, String> {
        val words = name.trim().split(Regex("\\s+"))
        if (words.isEmpty()) return "" to ""
        return words.first().uppercase() to words.drop(1).joinToString(" ").uppercase()
    }
}
