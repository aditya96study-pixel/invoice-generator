package com.amitabhtours.invoice.domain

import kotlin.math.max
import kotlin.math.roundToLong

enum class LineKind { DISTANCE, TOLL, PARKING, WAITING, NIGHT, ALLOWANCE, CUSTOM }

data class CustomLine(val label: String, val amountPaise: Long)

data class FareInput(
    val billedKm: Double = 0.0,
    val ratePerKmPaise: Long = 0,
    val tollPaise: Long = 0,
    val parkingPaise: Long = 0,
    val waitingHours: Double = 0.0,
    val waitingRatePaise: Long = 0,
    val freeWaitingHours: Double = 0.0,
    val nightPaise: Long = 0,
    val allowanceDays: Int = 0,
    val allowancePerDayPaise: Long = 0,
    val customLines: List<CustomLine> = emptyList(),
    val advancePaise: Long = 0,
)

/** One printed charge row. rateText / qtyText are shown only by themes that have those columns. */
data class FareLine(
    val kind: LineKind,
    val label: String,
    val rateText: String?,
    val qtyText: String?,
    val amountPaise: Long,
)

data class FareResult(
    val lines: List<FareLine>,
    val distanceFarePaise: Long,
    val waitingFarePaise: Long,
    val allowanceFarePaise: Long,
    val customTotalPaise: Long,
    val chargeableWaitingHours: Double,
    val subtotalPaise: Long,
    val roundOffPaise: Long,
    val totalPaise: Long,
    val advancePaise: Long,
    val balanceDuePaise: Long,
) {
    val advanceExceedsTotal: Boolean get() = advancePaise > totalPaise
}

object Fare {

    /** Rounds hours to the nearest half hour. */
    fun roundHalfHour(h: Double): Double = Math.round(max(0.0, h) * 2) / 2.0

    /** Rounds paise to the nearest whole rupee (half up). */
    fun roundToRupee(paise: Long): Long {
        if (paise >= 0) return ((paise + 50) / 100) * 100
        return -roundToRupee(-paise)
    }

    fun calculate(i: FareInput): FareResult {
        val lines = ArrayList<FareLine>()

        val km = max(0.0, i.billedKm)
        val distanceFare = (km * i.ratePerKmPaise).roundToLong()
        if (distanceFare > 0) {
            lines += FareLine(
                LineKind.DISTANCE, "Distance fare",
                rateText = "${Money.short(i.ratePerKmPaise)}/km",
                qtyText = "${Num.clean(km)} km",
                amountPaise = distanceFare,
            )
        }

        if (i.tollPaise > 0) lines += FareLine(LineKind.TOLL, "Toll charges", null, null, i.tollPaise)
        if (i.parkingPaise > 0) lines += FareLine(LineKind.PARKING, "Parking charges", null, null, i.parkingPaise)

        val waitingHours = roundHalfHour(i.waitingHours)
        val chargeable = max(0.0, waitingHours - max(0.0, i.freeWaitingHours))
        val waitingFare = (chargeable * i.waitingRatePaise).roundToLong()
        if (waitingFare > 0) {
            lines += FareLine(
                LineKind.WAITING, "Waiting charges",
                rateText = "${Money.short(i.waitingRatePaise)}/hr",
                qtyText = "${Num.clean(chargeable)} hr",
                amountPaise = waitingFare,
            )
        }

        if (i.nightPaise > 0) lines += FareLine(LineKind.NIGHT, "Night driver charges", null, null, i.nightPaise)

        val allowanceFare = max(0, i.allowanceDays).toLong() * i.allowancePerDayPaise
        if (allowanceFare > 0) {
            lines += FareLine(
                LineKind.ALLOWANCE, "Driver allowance",
                rateText = "${Money.short(i.allowancePerDayPaise)}/day",
                qtyText = if (i.allowanceDays == 1) "1 day" else "${i.allowanceDays} days",
                amountPaise = allowanceFare,
            )
        }

        var customTotal = 0L
        for (c in i.customLines) {
            if (c.amountPaise != 0L && c.label.isNotBlank()) {
                lines += FareLine(LineKind.CUSTOM, c.label.trim(), null, null, c.amountPaise)
                customTotal += c.amountPaise
            }
        }

        val subtotal = distanceFare + i.tollPaise.coerceAtLeast(0) + i.parkingPaise.coerceAtLeast(0) +
            waitingFare + i.nightPaise.coerceAtLeast(0) + allowanceFare + customTotal
        val total = roundToRupee(subtotal)
        val roundOff = total - subtotal
        val advance = max(0, i.advancePaise)
        val balance = max(0, total - advance)

        return FareResult(
            lines = lines,
            distanceFarePaise = distanceFare,
            waitingFarePaise = waitingFare,
            allowanceFarePaise = allowanceFare,
            customTotalPaise = customTotal,
            chargeableWaitingHours = chargeable,
            subtotalPaise = subtotal,
            roundOffPaise = roundOff,
            totalPaise = total,
            advancePaise = advance,
            balanceDuePaise = balance,
        )
    }
}
