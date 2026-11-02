package com.amitabhtours.invoice.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.amitabhtours.invoice.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Draws a route on OpenStreetMap tiles (downloaded once and cached on the phone).
 * Without internet it falls back to a plain light background with the route and pins.
 */
class MapRenderer(private val context: Context) {

    data class Style(
        val routeColor: Int = Color.parseColor("#111111"),
        val routeWidthPx: Float = 5f,
        val startColor: Int = Color.parseColor("#111111"),
        val endColor: Int = Color.parseColor("#111111"),
    )

    private val tileDir = File(context.cacheDir, "tiles").apply { mkdirs() }

    fun render(points: List<LatLng>, widthPx: Int, heightPx: Int, style: Style = Style(), online: Boolean = true): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (points.size < 2) {
            c.drawColor(Color.parseColor("#EEF1F4"))
            return bmp
        }
        val pad = min(widthPx, heightPx) * 0.14f
        val zoom = chooseZoom(points, widthPx - 2 * pad, heightPx - 2 * pad)
        val pxPts = points.map { project(it, zoom) }
        val minX = pxPts.minOf { it.first }
        val maxX = pxPts.maxOf { it.first }
        val minY = pxPts.minOf { it.second }
        val maxY = pxPts.maxOf { it.second }
        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2
        val originX = cx - widthPx / 2.0
        val originY = cy - heightPx / 2.0

        val tilesOk = online && drawTiles(c, zoom, originX, originY, widthPx, heightPx)
        if (!tilesOk) drawPlainBackground(c, widthPx, heightPx)

        // Route with a white casing for contrast.
        val path = Path()
        pxPts.forEachIndexed { i, p ->
            val x = (p.first - originX).toFloat()
            val y = (p.second - originY).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val casing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; color = Color.WHITE; strokeWidth = style.routeWidthPx + 4f
            strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND; alpha = 220
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; color = style.routeColor; strokeWidth = style.routeWidthPx
            strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
        }
        c.drawPath(path, casing)
        c.drawPath(path, line)

        val s = pxPts.first()
        val e = pxPts.last()
        val r = style.routeWidthPx * 2.1f
        val sx = (s.first - originX).toFloat()
        val sy = (s.second - originY).toFloat()
        c.drawCircle(sx, sy, r + 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        c.drawCircle(sx, sy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.startColor })
        c.drawCircle(sx, sy, r * 0.4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        val ex = (e.first - originX).toFloat()
        val ey = (e.second - originY).toFloat()
        c.drawRect(ex - r - 2.5f, ey - r - 2.5f, ex + r + 2.5f, ey + r + 2.5f, Paint().apply { color = Color.WHITE })
        c.drawRect(ex - r, ey - r, ex + r, ey + r, Paint().apply { color = style.endColor })
        c.drawRect(ex - r * 0.4f, ey - r * 0.4f, ex + r * 0.4f, ey + r * 0.4f, Paint().apply { color = Color.WHITE })

        // Attribution (required by OpenStreetMap whenever tiles are shown).
        if (tilesOk) {
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = max(9f, heightPx * 0.04f); color = Color.parseColor("#333333") }
            val label = "© OpenStreetMap contributors"
            val tw = tp.measureText(label)
            val bh = tp.textSize * 1.4f
            c.drawRect(widthPx - tw - 10f, heightPx - bh, widthPx.toFloat(), heightPx.toFloat(), Paint().apply { color = Color.argb(200, 255, 255, 255) })
            c.drawText(label, widthPx - tw - 5f, heightPx - bh * 0.3f, tp)
        }
        return bmp
    }

    private fun drawPlainBackground(c: Canvas, w: Int, h: Int) {
        c.drawColor(Color.parseColor("#EEF1F4"))
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DDE2E8"); strokeWidth = 1.2f }
        val step = max(w, h) / 12f
        var x = 0f
        while (x < w) { c.drawLine(x, 0f, x, h.toFloat(), grid); x += step }
        var y = 0f
        while (y < h) { c.drawLine(0f, y, w.toFloat(), y, grid); y += step }
    }

    private fun drawTiles(c: Canvas, z: Int, ox: Double, oy: Double, w: Int, h: Int): Boolean {
        val n = 2.0.pow(z).toInt()
        val tx0 = floor(ox / 256).toInt()
        val ty0 = floor(oy / 256).toInt()
        val tx1 = floor((ox + w - 1) / 256).toInt()
        val ty1 = floor((oy + h - 1) / 256).toInt()
        if ((tx1 - tx0 + 1) * (ty1 - ty0 + 1) > 40) return false
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        for (tx in tx0..tx1) for (ty in ty0..ty1) {
            if (ty < 0 || ty >= n) continue
            val wrappedX = ((tx % n) + n) % n
            val tile = tile(z, wrappedX, ty) ?: return false
            val left = (tx * 256 - ox).toFloat()
            val top = (ty * 256 - oy).toFloat()
            c.drawBitmap(tile, null, RectF(left, top, left + 256, top + 256), paint)
        }
        return true
    }

    private fun tile(z: Int, x: Int, y: Int): Bitmap? {
        val f = File(tileDir, "${z}_${x}_$y.png")
        if (f.exists() && f.length() > 0) {
            BitmapFactory.decodeFile(f.absolutePath)?.let { return it }
        }
        return try {
            val conn = URL("https://tile.openstreetmap.org/$z/$x/$y.png").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "AmitabhToursInvoice/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})")
            conn.connectTimeout = 7000
            conn.readTimeout = 9000
            conn.inputStream.use { input -> f.outputStream().use { input.copyTo(it) } }
            conn.disconnect()
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (e: Exception) {
            f.delete()
            null
        }
    }

    companion object {
        fun project(p: LatLng, z: Int): Pair<Double, Double> {
            val scale = 256.0 * 2.0.pow(z)
            val x = (p.lng + 180.0) / 360.0 * scale
            val latRad = Math.toRadians(p.lat.coerceIn(-85.0, 85.0))
            val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale
            return x to y
        }

        fun unproject(x: Double, y: Double, z: Int): LatLng {
            val scale = 256.0 * 2.0.pow(z)
            val lng = x / scale * 360.0 - 180.0
            val lat = Math.toDegrees(atan(sinh(PI * (1 - 2 * y / scale))))
            return LatLng(lat, lng)
        }

        fun chooseZoom(points: List<LatLng>, w: Float, h: Float): Int {
            for (z in 17 downTo 3) {
                val px = points.map { project(it, z) }
                val dx = px.maxOf { it.first } - px.minOf { it.first }
                val dy = px.maxOf { it.second } - px.minOf { it.second }
                if (dx <= w && dy <= h) return z
            }
            return 3
        }
    }
}
