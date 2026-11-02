package com.amitabhtours.invoice.pdf

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder

object Qr {
    fun upiUri(upiId: String, payeeName: String, amountPaise: Long, note: String): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        val amount = "%d.%02d".format(amountPaise / 100, amountPaise % 100)
        return "upi://pay?pa=${enc(upiId.trim())}&pn=${enc(payeeName.trim())}&am=$amount&cu=INR&tn=${enc(note)}"
    }

    /** Black-on-white QR bitmap; [moduleSize] pixels per module (keep crisp, no smoothing when drawn). */
    fun bitmap(content: String, moduleSize: Int = 8): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val m = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val w = m.width
        val h = m.height
        val pixels = IntArray(w * moduleSize * h * moduleSize)
        val outW = w * moduleSize
        for (y in 0 until h) for (x in 0 until w) {
            val c = if (m.get(x, y)) Color.BLACK else Color.WHITE
            for (dy in 0 until moduleSize) {
                val row = (y * moduleSize + dy) * outW
                for (dx in 0 until moduleSize) pixels[row + x * moduleSize + dx] = c
            }
        }
        return Bitmap.createBitmap(pixels, outW, h * moduleSize, Bitmap.Config.ARGB_8888)
    }
}
