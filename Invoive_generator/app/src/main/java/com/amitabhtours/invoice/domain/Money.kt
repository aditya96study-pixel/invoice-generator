package com.amitabhtours.invoice.domain

import kotlin.math.abs
import kotlin.math.roundToLong

/** All money is stored as paise (Long) to avoid rounding errors. */
object Money {

    /** ₹1,23,456.00 (Indian digit grouping). */
    fun format(paise: Long, symbol: Boolean = true, decimals: Boolean = true): String {
        val negative = paise < 0
        val p = abs(paise)
        val rupees = p / 100
        val fraction = p % 100
        val grouped = groupIndian(rupees)
        val body = if (decimals) "$grouped.${fraction.toString().padStart(2, '0')}" else grouped
        val withSymbol = if (symbol) "₹$body" else body
        return if (negative) "-$withSymbol" else withSymbol
    }

    /** Short form for tiles: ₹1,234 (no paise when whole). */
    fun short(paise: Long): String = if (paise % 100 == 0L) format(paise, decimals = false) else format(paise)

    /** "Rs." version for places where the ₹ glyph should be avoided. */
    fun plain(paise: Long): String = format(paise, symbol = false)

    fun groupIndian(n: Long): String {
        val s = n.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        var rest = s.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    /** Parses user input like "1,234.5" into paise. Returns null for blank/invalid. */
    fun parse(text: String?): Long? {
        if (text == null) return null
        val cleaned = text.replace(",", "").replace("₹", "").trim()
        if (cleaned.isEmpty()) return null
        val d = cleaned.toDoubleOrNull() ?: return null
        return (d * 100).roundToLong()
    }

    fun fromRupees(rupees: Double): Long = (rupees * 100).roundToLong()

    /** Paise to an editable string: 12345 -> "123.45", 12300 -> "123". */
    fun toInput(paise: Long?): String {
        if (paise == null) return ""
        return if (paise % 100 == 0L) (paise / 100).toString()
        else "${paise / 100}.${(abs(paise) % 100).toString().padStart(2, '0')}"
    }
}

object Num {
    /** 40838.5 -> "40,838.5" (Indian grouping, up to 1 decimal) */
    fun grouped(d: Double): String {
        val tenths = Math.round(d * 10)
        val whole = Money.groupIndian(tenths / 10)
        return if (tenths % 10 == 0L) whole else "$whole.${tenths % 10}"
    }

    /** 12.0 -> "12", 12.5 -> "12.5", 12.25 -> "12.25" */
    fun clean(d: Double?): String {
        if (d == null) return ""
        val r = Math.round(d * 100) / 100.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString().trimEnd('0').trimEnd('.')
    }

    fun parse(text: String?): Double? = text?.replace(",", "")?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
}
