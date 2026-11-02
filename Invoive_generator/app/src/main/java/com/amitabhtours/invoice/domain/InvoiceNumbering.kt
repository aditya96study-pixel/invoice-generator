package com.amitabhtours.invoice.domain

import java.time.LocalDate

/** ATT-2026-0001: prefix, financial-year start year (April–March), 4-digit running number. */
object InvoiceNumbering {

    data class Counter(val fy: Int, val nextSeq: Int)

    data class Issued(val number: String, val fy: Int, val seq: Int, val newCounter: Counter)

    fun financialYearStart(date: LocalDate): Int = if (date.monthValue >= 4) date.year else date.year - 1

    fun format(prefix: String, fy: Int, seq: Int): String {
        val p = prefix.trim().ifEmpty { "INV" }
        return "$p-$fy-${seq.toString().padStart(4, '0')}"
    }

    /**
     * Issues the next number. When [fyReset] is on and the financial year has changed since the
     * last invoice, the running number restarts at 1.
     */
    fun issue(prefix: String, counter: Counter, today: LocalDate, fyReset: Boolean): Issued {
        val fy = financialYearStart(today)
        val seq = when {
            counter.fy == 0 -> counter.nextSeq.coerceAtLeast(1)
            fyReset && counter.fy != fy -> 1
            else -> counter.nextSeq.coerceAtLeast(1)
        }
        return Issued(format(prefix, fy, seq), fy, seq, Counter(fy, seq + 1))
    }

    /** What the next number will look like, without issuing it. */
    fun preview(prefix: String, counter: Counter, today: LocalDate, fyReset: Boolean): String =
        issue(prefix, counter, today, fyReset).number
}
