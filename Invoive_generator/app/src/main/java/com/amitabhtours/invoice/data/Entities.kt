package com.amitabhtours.invoice.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object InvoiceStatus {
    const val ACTIVE = "ACTIVE"
    const val CANCELLED = "CANCELLED"
}

object KmSource {
    const val ODOMETER = "ODOMETER"
    const val GPS = "GPS"
    const val MANUAL = "MANUAL"

    fun label(s: String) = when (s) {
        ODOMETER -> "Odometer"
        GPS -> "GPS tracked"
        else -> "Entered manually"
    }
}

object AttachmentType {
    const val TOLL = "TOLL"
    const val PARKING = "PARKING"
    const val OTHER = "OTHER"

    fun label(s: String) = when (s) {
        TOLL -> "Toll receipt"
        PARKING -> "Parking ticket"
        else -> "Receipt"
    }
}

object TripStatus {
    const val ACTIVE = "ACTIVE"
    const val PAUSED = "PAUSED"
    const val ENDED = "ENDED"
}

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["number"], unique = true), Index("customerId"), Index("createdAt"), Index("tripId")]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val fyYear: Int,
    val seq: Int,
    val createdAt: Long,
    val status: String = InvoiceStatus.ACTIVE,
    val customerId: Long? = null,
    val customerName: String = "",
    val customerMobile: String = "",
    val pickup: String = "",
    val dropAt: String = "",
    val carNo: String = "",
    val vehicleType: String = "",
    val driverName: String = "",
    val driverMobile: String = "",
    val tripType: String = "",
    val startKm: Double? = null,
    val endKm: Double? = null,
    val gpsKm: Double? = null,
    val billedKmSource: String = KmSource.ODOMETER,
    val billedKm: Double = 0.0,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val ratePerKmPaise: Long = 0,
    val distanceFarePaise: Long = 0,
    val tollPaise: Long = 0,
    val parkingPaise: Long = 0,
    val waitingHours: Double = 0.0,
    val waitingRatePaise: Long = 0,
    val freeWaitingHours: Double = 0.0,
    val waitingFarePaise: Long = 0,
    val nightPaise: Long = 0,
    val allowanceDays: Int = 0,
    val allowancePerDayPaise: Long = 0,
    val allowanceFarePaise: Long = 0,
    val customTotalPaise: Long = 0,
    val subtotalPaise: Long = 0,
    val roundOffPaise: Long = 0,
    val totalPaise: Long = 0,
    val advancePaise: Long = 0,
    val balanceDuePaise: Long = 0,
    val paymentMode: String = "",
    val paidAt: Long? = null,
    val paidMode: String? = null,
    val feedback: String = "",
    val themeId: Int = 1,
    val tripId: Long? = null,
    val customerSignaturePath: String? = null,
    val pdfPath: String? = null,
    val attachReceipts: Boolean = true,
    val cancelledAt: Long? = null,
    val cancelReason: String? = null,
) {
    val isCancelled: Boolean get() = status == InvoiceStatus.CANCELLED
    val outstandingPaise: Long get() = if (isCancelled || paidAt != null) 0 else balanceDuePaise
    val isPaid: Boolean get() = !isCancelled && (balanceDuePaise == 0L || paidAt != null)
}

@Entity(tableName = "custom_charges", indices = [Index("invoiceId")])
data class CustomChargeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val label: String,
    val amountPaise: Long,
    val position: Int,
)

@Entity(tableName = "attachments", indices = [Index("invoiceId")])
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val type: String,
    val mime: String,
    val path: String,
    val amountPaise: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "customers", indices = [Index("mobileKey")])
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val mobileKey: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val distanceMeters: Double = 0.0,
    val pausedMs: Long = 0,
    val pausedSince: Long? = null,
    val startAddress: String = "",
    val endAddress: String = "",
    val status: String = TripStatus.ACTIVE,
    val invoiceId: Long? = null,
    val segment: Int = 0,
) {
    /** Driving time, excluding pauses. */
    fun activeMs(now: Long = System.currentTimeMillis()): Long {
        val end = endedAt ?: now
        val pausedExtra = pausedSince?.let { (end - it).coerceAtLeast(0) } ?: 0
        return (end - startedAt - pausedMs - pausedExtra).coerceAtLeast(0)
    }

    val km: Double get() = distanceMeters / 1000.0
}

@Entity(tableName = "trip_points", indices = [Index("tripId")])
data class TripPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val time: Long,
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val segment: Int,
)

@Entity(tableName = "other_income", indices = [Index("date")])
data class OtherIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val amountPaise: Long,
    val source: String,
    val note: String = "",
)
