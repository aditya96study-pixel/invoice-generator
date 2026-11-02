package com.amitabhtours.invoice.domain

import kotlin.math.abs

/** Indian numbering system: "Rupees One Lakh Twenty Thousand Five Hundred Only". */
object AmountInWords {
    private val ones = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    private val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    fun rupees(paise: Long): String {
        val p = abs(paise)
        val r = p / 100
        val ps = p % 100
        val rupeeWords = if (r == 0L) "Zero" else words(r)
        val sb = StringBuilder("Rupees ").append(rupeeWords)
        if (ps > 0) sb.append(" and ").append(words(ps)).append(" Paise")
        sb.append(" Only")
        return sb.toString()
    }

    private fun twoDigits(n: Int): String = when {
        n < 20 -> ones[n]
        n % 10 == 0 -> tens[n / 10]
        else -> tens[n / 10] + "-" + ones[n % 10]
    }

    private fun threeDigits(n: Int): String {
        val h = n / 100
        val rest = n % 100
        val parts = ArrayList<String>()
        if (h > 0) parts.add(ones[h] + " Hundred")
        if (rest > 0) parts.add(twoDigits(rest))
        return parts.joinToString(" ")
    }

    fun words(n: Long): String {
        if (n == 0L) return "Zero"
        val parts = ArrayList<String>()
        var x = n
        val crore = x / 10_000_000
        x %= 10_000_000
        val lakh = (x / 100_000).toInt()
        x %= 100_000
        val thousand = (x / 1000).toInt()
        x %= 1000
        val rest = x.toInt()
        if (crore > 0) parts.add(words(crore) + " Crore")
        if (lakh > 0) parts.add(twoDigits(lakh) + " Lakh")
        if (thousand > 0) parts.add(twoDigits(thousand) + " Thousand")
        if (rest > 0) parts.add(threeDigits(rest))
        return parts.joinToString(" ")
    }
}
