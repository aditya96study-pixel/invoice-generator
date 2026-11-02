package com.amitabhtours.invoice.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    val zone: ZoneId get() = ZoneId.systemDefault()
    private val dateF = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val dayDateF = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)
    private val timeF = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private val dateTimeF = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    private val shortDateF = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    private val monthF = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val fileStampF = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ENGLISH)

    fun ldt(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), zone)
    fun localDate(ms: Long): LocalDate = ldt(ms).toLocalDate()
    fun startOfDay(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endOfDay(d: LocalDate): Long = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun date(ms: Long): String = ldt(ms).format(dateF)
    fun dayDate(ms: Long): String = ldt(ms).format(dayDateF)
    fun time(ms: Long): String = ldt(ms).format(timeF).replace("AM", "am").replace("PM", "pm")
    fun dateTime(ms: Long): String = ldt(ms).format(dateTimeF).replace("AM", "am").replace("PM", "pm")
    fun shortDate(d: LocalDate): String = d.format(shortDateF)
    fun month(d: LocalDate): String = d.format(monthF)
    fun fileStamp(ms: Long): String = ldt(ms).format(fileStampF)

    /** "2 h 35 min" */
    fun duration(ms: Long): String {
        val totalMin = (ms / 60000).coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h == 0L -> "$m min"
            m == 0L -> "$h h"
            else -> "$h h $m min"
        }
    }

    /** "2:05" style clock for the live tracking card. */
    fun clock(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun hoursText(ms: Long): String {
        val hours = ms / 3_600_000.0
        return Num.clean(Math.round(hours * 10) / 10.0) + if (hours == 1.0) " hour" else " hours"
    }

    fun partOfDay(ms: Long): String = when (ldt(ms).hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..20 -> "evening"
        else -> "night"
    }

    fun greeting(ms: Long): String = when (ldt(ms).hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

object Text {
    /** Keeps only digits; returns the last 10 for Indian mobiles. */
    fun mobileKey(mobile: String): String = mobile.filter { it.isDigit() }.takeLast(10)

    fun firstName(name: String): String = name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"

    /** First part of an address, e.g. "Kubera Garden, NIBM Road, Pune" -> "Kubera Garden". */
    fun shortPlace(address: String): String {
        val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        // Skip house/plot numbers like "33/H/16" or "Flat 1204" so the place name shows.
        val first = parts.firstOrNull { p -> p.count { it.isLetter() } >= 5 || p.none { it.isDigit() } } ?: parts.firstOrNull() ?: address
        return if (first.length > 28) first.take(26).trimEnd() + "…" else first
    }
}
