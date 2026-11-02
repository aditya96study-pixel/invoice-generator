package com.amitabhtours.invoice.pdf

import android.graphics.Bitmap
import com.amitabhtours.invoice.data.KmSource
import com.amitabhtours.invoice.domain.FareResult
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Num

data class LatLng(val lat: Double, val lng: Double)

/** Everything a theme needs to draw one invoice. Built from the database or from sample data. */
data class InvoiceDoc(
    val businessName: String,
    val address: String,
    val phone: String,
    val email: String,
    val gstin: String,
    val city: String,
    val footerText: String,
    val gstNote: String,

    val number: String,
    val seq: Int,
    val createdAt: Long,
    val cancelled: Boolean,

    val customerName: String,
    val customerMobile: String,
    val pickup: String,
    val drop: String,
    val tripType: String,

    val carNo: String,
    val vehicleType: String,
    val driverName: String,
    val driverMobile: String,

    val startKm: Double?,
    val endKm: Double?,
    val gpsKm: Double?,
    val billedKm: Double,
    val kmSource: String,
    val startTime: Long?,
    val endTime: Long?,

    val fare: FareResult,
    val amountInWords: String,
    val paymentMode: String,
    val paidAt: Long?,
    val paidMode: String?,
    val feedback: String,

    val upiQr: Bitmap?,
    val upiId: String,
    val customerSignature: Bitmap?,
    val ownerSignature: Bitmap?,
    val ownerStamp: Bitmap?,

    val route: List<LatLng>?,
    val tripActiveMs: Long?,
    /** Renders a map of [route] (width px, height px, route colour); null when not tracked. Blocking. */
    val mapProvider: ((Int, Int, Int) -> Bitmap?)?,
) {
    val hasRoute: Boolean get() = route != null && route.size >= 2

    val tripDurationMs: Long?
        get() = tripActiveMs ?: if (startTime != null && endTime != null && endTime - startTime >= 60_000) endTime - startTime else null

    val totalHoursText: String?
        get() {
            val ms = if (startTime != null && endTime != null && endTime - startTime >= 60_000) endTime - startTime else return null
            return Fmt.duration(ms)
        }

    val startKmText: String? get() = startKm?.let { Num.clean(it) }
    val endKmText: String? get() = endKm?.let { Num.clean(it) }
    val billedKmText: String get() = "${Num.clean(billedKm)} km"

    /** e.g. "45 km (GPS tracked)" when not the odometer. */
    val billedKmWithSource: String
        get() = if (kmSource == KmSource.ODOMETER) billedKmText else "$billedKmText (${KmSource.label(kmSource)})"

    val dateText: String get() = Fmt.date(createdAt)
    val timeText: String get() = Fmt.time(createdAt)
    val dateTimeText: String get() = Fmt.dateTime(createdAt)

    val showAdvance: Boolean get() = fare.advancePaise > 0
    val isPaidInFull: Boolean get() = paidAt != null || (fare.balanceDuePaise == 0L && fare.totalPaise > 0)

    val paymentLine: String?
        get() = when {
            paidAt != null -> "Paid in full on ${Fmt.date(paidAt)}" + (paidMode?.takeIf { it.isNotBlank() }?.let { " via $it" } ?: "")
            paymentMode.isNotBlank() -> "Payment mode: $paymentMode"
            else -> null
        }

    val avgSpeedKmh: Double?
        get() {
            val ms = tripDurationMs ?: return null
            if (ms < 60_000 || billedKm <= 0) return null
            return billedKm / (ms / 3_600_000.0)
        }

    /** Label/value pairs for themes that show a details list. Only non-empty values. */
    fun tripPairs(): List<Pair<String, String>> = buildList {
        if (tripType.isNotBlank()) add("Trip type" to tripType)
        if (carNo.isNotBlank()) add("Car No." to carNo)
        if (vehicleType.isNotBlank()) add("Vehicle type" to vehicleType)
        if (driverName.isNotBlank()) add("Driver" to driverName)
        if (driverMobile.isNotBlank()) add("Driver mobile" to driverMobile)
        startKmText?.let { add("Start km" to it) }
        endKmText?.let { add("End km" to it) }
        if (billedKm > 0) add("Total km" to billedKmWithSource)
        startTime?.let { add("Start time" to Fmt.dateTime(it)) }
        endTime?.let { add("End time" to Fmt.dateTime(it)) }
        totalHoursText?.let { add("Total hours" to it) }
    }
}
