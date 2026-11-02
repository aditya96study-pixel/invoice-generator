package com.amitabhtours.invoice.ui.invoice

import android.graphics.Bitmap
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amitabhtours.invoice.App
import com.amitabhtours.invoice.data.AppSettings
import com.amitabhtours.invoice.data.AttachmentType
import com.amitabhtours.invoice.data.CustomerEntity
import com.amitabhtours.invoice.data.DraftAttachment
import com.amitabhtours.invoice.data.InvoiceEntity
import com.amitabhtours.invoice.data.KmSource
import com.amitabhtours.invoice.data.TripEntity
import com.amitabhtours.invoice.domain.CustomLine
import com.amitabhtours.invoice.domain.Fare
import com.amitabhtours.invoice.domain.FareInput
import com.amitabhtours.invoice.domain.FareResult
import com.amitabhtours.invoice.domain.InvoiceNumbering
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.pdf.LatLng
import com.amitabhtours.invoice.pdf.Qr
import com.amitabhtours.invoice.pdf.ThemeCatalog
import com.amitabhtours.invoice.ui.components.MemField
import com.amitabhtours.invoice.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

enum class Step { FORM, REVIEW, SIGN, THEME }

class CustomLineInput(label: String = "", amount: String = "") {
    var label by mutableStateOf(label)
    var amount by mutableStateOf(amount)
}

class InvoiceFormViewModel(
    private val app: App,
    private val initialTripId: Long?,
    private val initialCustomerId: Long?,
) : ViewModel() {

    var loading by mutableStateOf(true); private set
    var settings by mutableStateOf(AppSettings()); private set
    var previewNumber by mutableStateOf(""); private set
    var step by mutableStateOf(Step.FORM)
    var saving by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)

    // Customer
    var customerName by mutableStateOf(MemField())
    var customerMobile by mutableStateOf(MemField())
    var pickup by mutableStateOf(MemField())
    var drop by mutableStateOf(MemField())
    var customers by mutableStateOf<List<CustomerEntity>>(emptyList()); private set

    // Vehicle & driver
    var carNo by mutableStateOf(MemField())
    var vehicleType by mutableStateOf(MemField())
    var driverName by mutableStateOf(MemField())
    var driverMobile by mutableStateOf(MemField())

    // Trip
    var tripType by mutableStateOf(MemField())
    var startKm by mutableStateOf(MemField())
    var endKm by mutableStateOf("")
    var startTime by mutableStateOf<Long?>(null)
    var endTime by mutableStateOf<Long?>(null)
    var trip by mutableStateOf<TripEntity?>(null); private set
    var route by mutableStateOf<List<LatLng>?>(null); private set
    var uninvoicedTrips by mutableStateOf<List<TripEntity>>(emptyList()); private set
    var kmSource by mutableStateOf(KmSource.ODOMETER)
    var manualKm by mutableStateOf("")

    // Charges
    var rateChoice by mutableStateOf("")          // "12", "15" or "other"
    var customRate by mutableStateOf("")
    var toll by mutableStateOf("")
    var parking by mutableStateOf("")
    var waitingHours by mutableStateOf(0.0)
    var waitingRate by mutableStateOf(MemField())
    var nightAmount by mutableStateOf("")
    var allowanceDays by mutableStateOf(0)
    var allowancePerDay by mutableStateOf(MemField())
    val customLines = mutableStateListOf<CustomLineInput>()
    var advance by mutableStateOf("")

    // Payment & notes
    var paymentMode by mutableStateOf("")
    var feedback by mutableStateOf("")

    // Receipts
    val attachments = mutableStateListOf<DraftAttachment>()
    var attachReceipts by mutableStateOf(true)

    // Signature & style
    var customerSignaturePath by mutableStateOf<String?>(null)
    var themeId by mutableStateOf(5)
    val thumbnails = mutableStateMapOf<Int, ImageBitmap>()
    private var thumbJob: Job? = null
    private var ownerSig: Bitmap? = null
    private var stamp: Bitmap? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val s = app.settings.get()
        settings = s
        previewNumber = InvoiceNumbering.preview(s.invoicePrefix, InvoiceNumbering.Counter(s.counterFy, s.nextSeq), LocalDate.now(), s.fyReset)
        val last = app.db.invoices().latest()
        customers = app.db.customers().all()
        uninvoicedTrips = app.db.trips().observeUninvoiced().first()

        customerName = MemField(hint = last?.customerName.orEmpty())
        customerMobile = MemField(hint = last?.customerMobile.orEmpty())
        pickup = MemField(hint = last?.pickup.orEmpty())
        drop = MemField(hint = last?.dropAt.orEmpty())
        carNo = MemField(hint = last?.carNo?.takeIf { it.isNotBlank() } ?: s.carNo)
        vehicleType = MemField(hint = last?.vehicleType?.takeIf { it.isNotBlank() } ?: s.vehicleType)
        driverName = MemField(hint = last?.driverName?.takeIf { it.isNotBlank() } ?: s.driverName)
        driverMobile = MemField(hint = last?.driverMobile?.takeIf { it.isNotBlank() } ?: s.driverMobile)
        tripType = MemField(hint = last?.tripType?.takeIf { it.isNotBlank() } ?: s.tripTypes.firstOrNull().orEmpty())
        startKm = MemField(hint = last?.endKm?.let { Num.clean(it) }.orEmpty())

        val lastRate = last?.ratePerKmPaise?.takeIf { it > 0 }
        val chips = s.rateChips.map { it.toString() }
        val lastRateText = lastRate?.let { Money.toInput(it) }
        if (lastRateText != null && lastRateText !in chips) {
            rateChoice = "other"; customRate = lastRateText
        } else rateChoice = lastRateText ?: chips.firstOrNull().orEmpty()

        waitingRate = MemField(hint = Money.toInput(last?.waitingRatePaise?.takeIf { it > 0 } ?: s.waitingRatePaise))
        allowancePerDay = MemField(hint = Money.toInput(last?.allowancePerDayPaise?.takeIf { it > 0 } ?: s.allowancePerDayPaise))
        paymentMode = last?.paymentMode.orEmpty()
        attachReceipts = s.attachReceiptsDefault
        themeId = if (s.lastTheme > 0) s.lastTheme else s.defaultTheme

        initialCustomerId?.let { id -> app.db.customers().get(id)?.let { applyCustomer(it, withPlaces = true) } }
        initialTripId?.let { id -> app.db.trips().get(id)?.let { linkTrip(it) } }

        withContext(Dispatchers.IO) {
            ownerSig = if (s.ownerSignatureEnabled) s.ownerSignaturePath?.let { Images.decodeSampled(it, 900) } else null
            stamp = if (s.ownerStampEnabled) s.ownerStampPath?.let { Images.decodeSampled(it, 600) } else null
        }
        loading = false
    }

    suspend fun applyCustomerSuspend(c: CustomerEntity) = applyCustomer(c, withPlaces = true)

    private suspend fun applyCustomer(c: CustomerEntity, withPlaces: Boolean) {
        customerName = customerName.copy(text = c.name, cleared = false)
        customerMobile = customerMobile.copy(text = c.mobile, cleared = false)
        if (withPlaces && trip == null) {
            val p = app.db.invoices().topPickups(c.id).firstOrNull()?.place
            val d = app.db.invoices().topDrops(c.id).firstOrNull()?.place
            if (p != null) pickup = MemField(hint = p)
            if (d != null) drop = MemField(hint = d)
        }
    }

    fun pickCustomer(c: CustomerEntity) {
        viewModelScope.launch { applyCustomer(c, withPlaces = true) }
    }

    fun suggestions(): List<CustomerEntity> {
        val q = customerName.text.trim()
        val m = customerMobile.text.filter { it.isDigit() }
        if (q.length < 2 && m.length < 3) return emptyList()
        return customers.filter { c ->
            (q.length >= 2 && c.name.contains(q, ignoreCase = true) && !c.name.equals(q, ignoreCase = true)) ||
                (m.length >= 3 && c.mobileKey.contains(m) && c.mobileKey != m.takeLast(10))
        }.take(3)
    }

    suspend fun linkTrip(t: TripEntity) {
        trip = t
        val pts = app.db.trips().points(t.id)
        route = if (pts.size >= 2) {
            val stepN = max(1, pts.size / 1500)
            pts.filterIndexed { i, _ -> i % stepN == 0 || i == pts.size - 1 }.map { LatLng(it.lat, it.lng) }
        } else null
        if (t.startAddress.isNotBlank()) pickup = pickup.copy(text = t.startAddress, cleared = false)
        if (t.endAddress.isNotBlank()) drop = drop.copy(text = t.endAddress, cleared = false)
        startTime = t.startedAt
        endTime = t.endedAt
        kmSource = KmSource.GPS
    }

    fun useTrip(t: TripEntity) { viewModelScope.launch { linkTrip(t) } }

    fun unlinkTrip() {
        trip = null
        route = null
        if (kmSource == KmSource.GPS) kmSource = KmSource.ODOMETER
        val current = ThemeCatalog.byId(themeId)
        if (current.mapMode == com.amitabhtours.invoice.pdf.MapMode.REQUIRED) themeId = settings.defaultTheme
    }

    // ---------------------------------------------------------------- derived values

    val odometerKm: Double?
        get() {
            val s = Num.parse(startKm.value) ?: return null
            val e = Num.parse(endKm) ?: return null
            return if (e >= s) e - s else null
        }

    val gpsKm: Double? get() = trip?.let { Math.round(it.km * 10) / 10.0 }

    val billedKm: Double
        get() = when (kmSource) {
            KmSource.GPS -> gpsKm ?: odometerKm ?: 0.0
            KmSource.MANUAL -> Num.parse(manualKm) ?: 0.0
            else -> odometerKm ?: if (trip != null && Num.parse(endKm) == null) gpsKm ?: 0.0 else 0.0
        }

    val effectiveKmSource: String
        get() = when {
            kmSource == KmSource.GPS && gpsKm != null -> KmSource.GPS
            kmSource == KmSource.MANUAL -> KmSource.MANUAL
            odometerKm != null -> KmSource.ODOMETER
            gpsKm != null -> KmSource.GPS
            else -> KmSource.MANUAL
        }

    /** True when GPS and odometer differ by more than 10%. */
    val kmMismatch: Boolean
        get() {
            val g = gpsKm ?: return false
            val o = odometerKm ?: return false
            if (max(g, o) < 1) return false
            return abs(g - o) / max(g, o) > 0.10
        }

    val ratePaise: Long
        get() = if (rateChoice == "other") Money.parse(customRate) ?: 0 else Money.parse(rateChoice) ?: 0

    private val fareState = derivedStateOf {
        Fare.calculate(
            FareInput(
                billedKm = billedKm,
                ratePerKmPaise = ratePaise,
                tollPaise = Money.parse(toll) ?: 0,
                parkingPaise = Money.parse(parking) ?: 0,
                waitingHours = waitingHours,
                waitingRatePaise = Money.parse(waitingRate.value) ?: 0,
                freeWaitingHours = settings.freeWaitingHours,
                nightPaise = Money.parse(nightAmount) ?: 0,
                allowanceDays = allowanceDays,
                allowancePerDayPaise = Money.parse(allowancePerDay.value) ?: 0,
                customLines = customLines.map { CustomLine(it.label, Money.parse(it.amount) ?: 0) },
                advancePaise = Money.parse(advance) ?: 0,
            )
        )
    }
    val fare: FareResult get() = fareState.value

    /** Customer-specific fields that were carried over from the last invoice unchanged. */
    val carriedOver: List<String>
        get() = buildList {
            if (customerName.usingHint) add("Customer name")
            if (customerMobile.usingHint) add("Customer mobile")
            if (pickup.usingHint) add("Pickup")
            if (drop.usingHint) add("Drop")
        }

    val warnings: List<String>
        get() = buildList {
            if (customerName.value.isBlank()) add("Customer name is empty.")
            if (fare.totalPaise <= 0) add("The total is ₹0. Add the km and rate, or a charge.")
            if (kmSource == KmSource.ODOMETER && Num.parse(endKm) != null && odometerKm == null) add("End km is smaller than start km.")
            if (kmMismatch) add("GPS km and odometer km differ by more than 10%. Check which one to bill.")
            if (fare.advanceExceedsTotal) add("Advance paid is more than the total.")
        }

    // ---------------------------------------------------------------- receipts

    fun newAttachmentFile(ext: String): File = File(app.repo.attachmentsDir, "rcpt-${UUID.randomUUID()}.$ext")

    fun addAttachment(a: DraftAttachment) {
        attachments += a
        val amt = a.amountPaise ?: return
        when (a.type) {
            AttachmentType.TOLL -> toll = Money.toInput((Money.parse(toll) ?: 0) + amt)
            AttachmentType.PARKING -> parking = Money.toInput((Money.parse(parking) ?: 0) + amt)
        }
    }

    fun removeAttachment(a: DraftAttachment) {
        attachments.remove(a)
        val amt = a.amountPaise ?: return
        when (a.type) {
            AttachmentType.TOLL -> toll = Money.toInput(((Money.parse(toll) ?: 0) - amt).coerceAtLeast(0)).let { if (it == "0") "" else it }
            AttachmentType.PARKING -> parking = Money.toInput(((Money.parse(parking) ?: 0) - amt).coerceAtLeast(0)).let { if (it == "0") "" else it }
        }
        runCatching { File(a.path).delete() }
    }

    fun saveSignature(bmp: Bitmap) {
        val f = File(app.repo.signaturesDir, "sig-${UUID.randomUUID()}.png")
        Images.savePng(bmp, f)
        customerSignaturePath = f.absolutePath
    }

    // ---------------------------------------------------------------- build & save

    fun draftEntity(): InvoiceEntity {
        val f = fare
        val now = System.currentTimeMillis()
        return InvoiceEntity(
            number = previewNumber,
            fyYear = 0,
            seq = previewNumber.takeLast(4).toIntOrNull() ?: 0,
            createdAt = now,
            customerName = customerName.value.trim(),
            customerMobile = customerMobile.value.trim(),
            pickup = pickup.value.trim(),
            dropAt = drop.value.trim(),
            carNo = carNo.value.trim(),
            vehicleType = vehicleType.value.trim(),
            driverName = driverName.value.trim(),
            driverMobile = driverMobile.value.trim(),
            tripType = tripType.value.trim(),
            startKm = Num.parse(startKm.value),
            endKm = Num.parse(endKm),
            gpsKm = gpsKm,
            billedKmSource = effectiveKmSource,
            billedKm = Math.round(billedKm * 100) / 100.0,
            startTime = startTime,
            endTime = endTime,
            ratePerKmPaise = ratePaise,
            distanceFarePaise = f.distanceFarePaise,
            tollPaise = Money.parse(toll) ?: 0,
            parkingPaise = Money.parse(parking) ?: 0,
            waitingHours = Fare.roundHalfHour(waitingHours),
            waitingRatePaise = Money.parse(waitingRate.value) ?: 0,
            freeWaitingHours = settings.freeWaitingHours,
            waitingFarePaise = f.waitingFarePaise,
            nightPaise = Money.parse(nightAmount) ?: 0,
            allowanceDays = allowanceDays,
            allowancePerDayPaise = Money.parse(allowancePerDay.value) ?: 0,
            allowanceFarePaise = f.allowanceFarePaise,
            customTotalPaise = f.customTotalPaise,
            subtotalPaise = f.subtotalPaise,
            roundOffPaise = f.roundOffPaise,
            totalPaise = f.totalPaise,
            advancePaise = f.advancePaise,
            balanceDuePaise = f.balanceDuePaise,
            paymentMode = paymentMode,
            feedback = feedback.trim(),
            themeId = themeId,
            tripId = trip?.id,
            customerSignaturePath = customerSignaturePath,
            attachReceipts = attachReceipts && attachments.isNotEmpty(),
        )
    }

    fun hasRoute(): Boolean = (route?.size ?: 0) >= 2

    fun renderThumbnails(widthPx: Int) {
        thumbJob?.cancel()
        thumbnails.clear()
        val s = settings
        val inv = draftEntity()
        val f = fare
        val r = route
        val active = trip?.activeMs()
        val sig = customerSignaturePath
        thumbJob = viewModelScope.launch(Dispatchers.Default) {
            val custSig = sig?.let { Images.decodeSampled(it, 600) }
            val qr = if (s.upiReady && f.balanceDuePaise > 0) Qr.bitmap(Qr.upiUri(s.upiId, s.upiName, f.balanceDuePaise, "Invoice ${inv.number}"), 4) else null
            val doc = app.pdf.docFrom(inv, s, f, qr, custSig, ownerSig, stamp, r, active, online = false)
            val sampleRouteDoc = if (r == null) doc.copy(route = null) else doc
            for (t in ThemeCatalog.all) {
                val bmp = runCatching { app.pdf.thumbnail(sampleRouteDoc, t, widthPx) }.getOrNull() ?: continue
                thumbnails[t.id] = bmp.asImageBitmap()
            }
        }
    }

    fun generate(onDone: (Long) -> Unit) {
        if (saving) return
        saving = true
        error = null
        viewModelScope.launch {
            try {
                val draft = draftEntity()
                val lines = customLines.map { CustomLine(it.label, Money.parse(it.amount) ?: 0) }
                val id = app.repo.createInvoice(draft, lines, attachments.toList())
                app.pdf.render(id, themeId)
                onDone(id)
            } catch (e: Exception) {
                error = "Could not create the invoice: ${e.message}"
            } finally {
                saving = false
            }
        }
    }
}
