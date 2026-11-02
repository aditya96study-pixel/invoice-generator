package com.amitabhtours.invoice.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.amitabhtours.invoice.data.InvoiceEntity
import com.amitabhtours.invoice.domain.Money
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfActions {

    fun uriFor(context: Context, file: File) = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun share(context: Context, file: File, inv: InvoiceEntity, businessName: String) {
        val uri = uriFor(context, file)
        val greeting = inv.customerName.takeIf { it.isNotBlank() }?.let { "Dear $it,\n\n" } ?: ""
        val amount = if (inv.outstandingPaise > 0) "Amount due: ${Money.format(inv.outstandingPaise)}" else "Total: ${Money.format(inv.totalPaise)}"
        val text = "${greeting}Please find attached invoice ${inv.number} from $businessName.\n$amount\n\nThank you for travelling with us."
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${inv.number} — $businessName")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Send invoice").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    /** Saves to Downloads/Amitabh Tours. Returns a short description of where it went. */
    fun saveToDownloads(context: Context, file: File, name: String = file.name, mime: String = "application/pdf"): String {
        val folder = "Amitabh Tours"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folder")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Could not create the file")
            resolver.openOutputStream(uri)?.use { out -> FileInputStream(file).use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folder).apply { mkdirs() }
            file.copyTo(File(dir, name), overwrite = true)
        }
        return "Downloads › $folder › $name"
    }

    fun print(context: Context, file: File, jobName: String) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        pm.print(jobName, FilePrintAdapter(file, jobName), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    private class FilePrintAdapter(private val file: File, private val name: String) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback, extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
            callback.onLayoutFinished(PrintDocumentInfo.Builder(name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
        }

        override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
            try {
                FileInputStream(file).use { input -> FileOutputStream(destination.fileDescriptor).use { input.copyTo(it) } }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }

    /** Renders every page of a PDF to bitmaps [widthPx] wide (for the in-app preview). */
    fun renderPages(file: File, widthPx: Int, maxPages: Int = 20): List<Bitmap> {
        if (!file.exists()) return emptyList()
        val out = ArrayList<Bitmap>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r ->
                for (i in 0 until minOf(r.pageCount, maxPages)) {
                    r.openPage(i).use { page ->
                        val h = (widthPx.toFloat() * page.height / page.width).toInt()
                        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        out += bmp
                    }
                }
            }
        }
        return out
    }
}
