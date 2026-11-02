package com.amitabhtours.invoice.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object Images {

    fun decodeSampled(path: String, maxDim: Int): Bitmap? {
        val f = File(path)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return rotateByExif(path, bmp)
    }

    private fun rotateByExif(path: String, bmp: Bitmap): Bitmap {
        val deg = runCatching {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (deg == 0f) return bmp
        val m = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    fun savePng(bmp: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun copyUriToFile(context: Context, uri: Uri, dest: File): Boolean = runCatching {
        dest.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use { input.copyTo(it) } } != null
    }.getOrDefault(false)

    /**
     * Makes paper background transparent: estimates the paper brightness and turns pixels
     * close to it transparent, keeping the ink with soft edges. Crops to the ink.
     */
    fun removeBackground(src: Bitmap): Bitmap {
        val maxSide = 1400
        val scaled = if (max(src.width, src.height) > maxSide) {
            val s = maxSide.toFloat() / max(src.width, src.height)
            Bitmap.createScaledBitmap(src, (src.width * s).toInt(), (src.height * s).toInt(), true)
        } else src
        val w = scaled.width
        val h = scaled.height
        val px = IntArray(w * h)
        scaled.getPixels(px, 0, w, 0, 0, w, h)
        val lum = IntArray(px.size) {
            val c = px[it]
            (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000
        }
        // Local paper brightness on a coarse grid (handles uneven lighting).
        val cell = 48
        val gw = (w + cell - 1) / cell
        val gh = (h + cell - 1) / cell
        val bg = IntArray(gw * gh)
        for (gy in 0 until gh) for (gx in 0 until gw) {
            val hist = IntArray(256)
            var n = 0
            for (y in gy * cell until minOf(h, (gy + 1) * cell)) for (x in gx * cell until minOf(w, (gx + 1) * cell)) {
                hist[lum[y * w + x]]++; n++
            }
            // 80th percentile = paper
            var acc = 0
            var v = 255
            for (i in 0..255) { acc += hist[i]; if (acc >= n * 0.8) { v = i; break } }
            bg[gy * gw + gx] = v
        }
        val out = IntArray(px.size)
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            val paper = bg[(y / cell) * gw + (x / cell)]
            val c = px[i]
            val blueness = Color.blue(c) - (Color.red(c) + Color.green(c)) / 2
            val score = maxOf((paper - lum[i]).toFloat(), blueness * 1.4f)
            val a = ((score - 22f) / 40f).coerceIn(0f, 1f)
            if (a > 0.02f) {
                out[i] = Color.argb((a * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))
                if (a > 0.5f) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            } else out[i] = Color.TRANSPARENT
        }
        val result = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        if (maxX < 0) return result
        val pad = 10
        val l = (minX - pad).coerceAtLeast(0)
        val t = (minY - pad).coerceAtLeast(0)
        val r = (maxX + pad).coerceAtMost(w - 1)
        val b = (maxY + pad).coerceAtMost(h - 1)
        return Bitmap.createBitmap(result, l, t, r - l + 1, b - t + 1)
    }
}
