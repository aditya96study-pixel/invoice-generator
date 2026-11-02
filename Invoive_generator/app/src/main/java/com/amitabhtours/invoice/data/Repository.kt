package com.amitabhtours.invoice.data

import android.content.Context
import androidx.room.withTransaction
import com.amitabhtours.invoice.domain.CustomLine
import com.amitabhtours.invoice.domain.InvoiceNumbering
import com.amitabhtours.invoice.domain.Text
import com.amitabhtours.invoice.pdf.PdfService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** A receipt picked in the form but not yet saved with an invoice. */
data class DraftAttachment(
    val type: String,
    val mime: String,
    val path: String,
    val amountPaise: Long?,
)

class Repository(
    private val context: Context,
    val db: AppDatabase,
    val settings: SettingsStore,
    val pdf: PdfService,
) {
    val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
    val signaturesDir = File(context.filesDir, "signatures").apply { mkdirs() }
    val brandingDir = File(context.filesDir, "branding").apply { mkdirs() }

    /**
     * Saves a new invoice: issues the next number, links/creates the customer, stores custom
     * lines and receipts, links the tracked trip. Returns the new invoice id.
     */
    suspend fun createInvoice(
        draft: InvoiceEntity,
        customLines: List<CustomLine>,
        attachments: List<DraftAttachment>,
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // Issue the number atomically inside the settings store.
        var issued: InvoiceNumbering.Issued? = null
        settings.update { s ->
            var i = InvoiceNumbering.issue(s.invoicePrefix, InvoiceNumbering.Counter(s.counterFy, s.nextSeq), LocalDate.now(), s.fyReset)
            issued = i
            s.copy(counterFy = i.newCounter.fy, nextSeq = i.newCounter.nextSeq, lastTheme = draft.themeId)
        }
        var number = issued!!
        // Never reuse a number (e.g. if the counter was edited backwards in Settings).
        while (db.invoices().countWithNumber(number.number) > 0) {
            val s = settings.get()
            val next = InvoiceNumbering.issue(s.invoicePrefix, InvoiceNumbering.Counter(s.counterFy, s.nextSeq), LocalDate.now(), s.fyReset)
            settings.update { it.copy(counterFy = next.newCounter.fy, nextSeq = next.newCounter.nextSeq) }
            number = next
        }

        db.withTransaction {
            val customerId = upsertCustomer(draft.customerName, draft.customerMobile)
            val id = db.invoices().insert(
                draft.copy(
                    id = 0,
                    number = number.number,
                    fyYear = number.fy,
                    seq = number.seq,
                    createdAt = now,
                    customerId = customerId,
                )
            )
            db.charges().insertAll(customLines.filter { it.label.isNotBlank() && it.amountPaise != 0L }.mapIndexed { i, c ->
                CustomChargeEntity(invoiceId = id, label = c.label.trim(), amountPaise = c.amountPaise, position = i)
            })
            db.attachments().insertAll(attachments.map {
                AttachmentEntity(invoiceId = id, type = it.type, mime = it.mime, path = it.path, amountPaise = it.amountPaise)
            })
            draft.tripId?.let { tid -> db.trips().get(tid)?.let { db.trips().update(it.copy(invoiceId = id)) } }
            id
        }
    }

    private suspend fun upsertCustomer(name: String, mobile: String): Long? {
        val cleanName = name.trim()
        val key = Text.mobileKey(mobile)
        if (cleanName.isEmpty() && key.isEmpty()) return null
        val dao = db.customers()
        val existing = (if (key.length >= 6) dao.byMobileKey(key) else null)
            ?: (if (cleanName.isNotEmpty()) dao.byName(cleanName)?.takeIf { it.mobileKey.isEmpty() || key.isEmpty() || it.mobileKey == key } else null)
        return if (existing != null) {
            val updated = existing.copy(
                name = cleanName.ifEmpty { existing.name },
                mobile = mobile.trim().ifEmpty { existing.mobile },
                mobileKey = key.ifEmpty { existing.mobileKey },
            )
            if (updated != existing) dao.update(updated)
            existing.id
        } else {
            dao.insert(CustomerEntity(name = cleanName.ifEmpty { mobile.trim() }, mobile = mobile.trim(), mobileKey = key))
        }
    }

    suspend fun findCustomer(name: String, mobile: String): CustomerEntity? = withContext(Dispatchers.IO) {
        val key = Text.mobileKey(mobile)
        (if (key.length >= 10) db.customers().byMobileKey(key) else null)
            ?: name.trim().takeIf { it.length >= 3 }?.let { db.customers().byName(it) }
    }

    suspend fun cancelInvoice(id: Long, reason: String) = withContext(Dispatchers.IO) {
        val inv = db.invoices().get(id) ?: return@withContext
        db.invoices().update(inv.copy(status = InvoiceStatus.CANCELLED, cancelledAt = System.currentTimeMillis(), cancelReason = reason.ifBlank { null }))
        inv.tripId?.let { tid -> db.trips().get(tid)?.let { if (it.invoiceId == id) db.trips().update(it.copy(invoiceId = null)) } }
        pdf.render(id)
    }

    suspend fun markPaid(id: Long, mode: String, at: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val inv = db.invoices().get(id) ?: return@withContext
        db.invoices().update(inv.copy(paidAt = at, paidMode = mode))
        pdf.render(id)
    }

    suspend fun markUnpaid(id: Long) = withContext(Dispatchers.IO) {
        val inv = db.invoices().get(id) ?: return@withContext
        db.invoices().update(inv.copy(paidAt = null, paidMode = null))
        pdf.render(id)
    }

    suspend fun mergeCustomers(fromId: Long, intoId: Long) = withContext(Dispatchers.IO) {
        if (fromId == intoId) return@withContext
        db.withTransaction {
            db.invoices().reassignCustomer(fromId, intoId)
            db.customers().get(fromId)?.let { db.customers().delete(it) }
        }
    }

    suspend fun updateCustomer(c: CustomerEntity) = withContext(Dispatchers.IO) {
        db.customers().update(c.copy(mobileKey = Text.mobileKey(c.mobile)))
    }

    /** Ends a trip when the tracking service is not running (e.g. after a phone restart). */
    suspend fun endTripDirect(id: Long) = withContext(Dispatchers.IO) {
        val t = db.trips().get(id) ?: return@withContext
        if (t.status == TripStatus.ENDED) return@withContext
        val now = System.currentTimeMillis()
        val extra = t.pausedSince?.let { now - it } ?: 0L
        val endAt = db.trips().lastPoint(id)?.time?.coerceAtLeast(t.startedAt) ?: now
        db.trips().update(t.copy(status = TripStatus.ENDED, endedAt = maxOf(endAt, now - extra), pausedSince = null, pausedMs = t.pausedMs + extra))
    }

    suspend fun deleteTrip(id: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.trips().deletePoints(id)
            db.trips().delete(id)
        }
    }

    /** Copies the owner's signature shipped with the app on first launch. */
    suspend fun installBundledAssets() = withContext(Dispatchers.IO) {
        val s = settings.get()
        if (s.bundledAssetsInstalled) return@withContext
        val dest = File(brandingDir, "owner_signature.png")
        val ok = runCatching {
            context.assets.open("owner_signature.png").use { input -> dest.outputStream().use { input.copyTo(it) } }
            true
        }.getOrDefault(false)
        settings.update {
            it.copy(
                bundledAssetsInstalled = true,
                ownerSignaturePath = if (ok) dest.absolutePath else it.ownerSignaturePath,
                ownerSignatureEnabled = ok && it.ownerSignatureEnabled,
            )
        }
    }

    /** Removes receipts/signature files left behind by invoices that were never saved. */
    suspend fun cleanOrphanFiles() = withContext(Dispatchers.IO) {
        val dayAgo = System.currentTimeMillis() - 24 * 3600_000L
        val used = HashSet<String>()
        db.invoices().all().forEach { inv ->
            inv.customerSignaturePath?.let { used += it }
            db.attachments().forInvoice(inv.id).forEach { used += it.path }
        }
        listOf(attachmentsDir, signaturesDir).forEach { dir ->
            dir.listFiles()?.forEach { f -> if (f.absolutePath !in used && f.lastModified() < dayAgo) f.delete() }
        }
    }
}
