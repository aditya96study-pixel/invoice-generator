package com.amitabhtours.invoice

import com.amitabhtours.invoice.domain.AmountInWords
import com.amitabhtours.invoice.domain.CustomLine
import com.amitabhtours.invoice.domain.Fare
import com.amitabhtours.invoice.domain.FareInput
import com.amitabhtours.invoice.domain.InvoiceNumbering
import com.amitabhtours.invoice.domain.LineKind
import com.amitabhtours.invoice.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainTest {

    @Test fun indianGrouping() {
        assertEquals("₹0.00", Money.format(0))
        assertEquals("₹999.00", Money.format(99_900))
        assertEquals("₹1,234.50", Money.format(123_450))
        assertEquals("₹1,23,456.00", Money.format(12_345_600))
        assertEquals("₹12,34,56,789.00", Money.format(123_456_789_00))
        assertEquals("₹1,000", Money.short(100_000))
    }

    @Test fun parseMoney() {
        assertEquals(123_450L, Money.parse("1,234.5"))
        assertEquals(null, Money.parse(""))
        assertEquals("12", Money.toInput(1200))
        assertEquals("12.50", Money.toInput(1250))
    }

    @Test fun words() {
        assertEquals("Rupees Zero Only", AmountInWords.rupees(0))
        assertEquals("Rupees One Thousand Two Hundred Thirty-Four Only", AmountInWords.rupees(123_400))
        assertEquals("Rupees One Lakh Only", AmountInWords.rupees(100_000_00))
        assertEquals("Rupees Two Crore Fifty Lakh Only", AmountInWords.rupees(2_50_00_000_00))
        assertEquals("Rupees Ten and Fifty Paise Only", AmountInWords.rupees(1050))
        assertEquals("Rupees Ninety-Nine Thousand Nine Hundred Ninety-Nine Only", AmountInWords.rupees(99_999_00))
    }

    @Test fun fareBasic() {
        val r = Fare.calculate(FareInput(billedKm = 45.0, ratePerKmPaise = 12_00, tollPaise = 120_00, parkingPaise = 50_00))
        assertEquals(540_00L, r.distanceFarePaise)
        assertEquals(710_00L, r.totalPaise)
        assertEquals(0L, r.roundOffPaise)
        assertEquals(listOf(LineKind.DISTANCE, LineKind.TOLL, LineKind.PARKING), r.lines.map { it.kind })
    }

    @Test fun onlyLinesWithValuesPrint() {
        val r = Fare.calculate(FareInput(billedKm = 10.0, ratePerKmPaise = 15_00, customLines = listOf(CustomLine("", 100), CustomLine("Snacks", 0))))
        assertEquals(1, r.lines.size)
    }

    @Test fun roundingAndAdvance() {
        val r = Fare.calculate(FareInput(billedKm = 23.3, ratePerKmPaise = 15_00, advancePaise = 100_00))
        assertEquals(349_50L, r.subtotalPaise)
        assertEquals(350_00L, r.totalPaise)
        assertEquals(50L, r.roundOffPaise)
        assertEquals(250_00L, r.balanceDuePaise)
    }

    @Test fun waitingWithFreeTime() {
        val r = Fare.calculate(FareInput(waitingHours = 2.0, waitingRatePaise = 100_00, freeWaitingHours = 0.5))
        assertEquals(150_00L, r.waitingFarePaise)
        assertEquals(1.5, r.chargeableWaitingHours, 0.001)
    }

    @Test fun allowanceAndDiscount() {
        val r = Fare.calculate(FareInput(allowanceDays = 3, allowancePerDayPaise = 300_00, customLines = listOf(CustomLine("Discount", -100_00))))
        assertEquals(900_00L, r.allowanceFarePaise)
        assertEquals(800_00L, r.totalPaise)
    }

    @Test fun advanceMoreThanTotal() {
        val r = Fare.calculate(FareInput(billedKm = 10.0, ratePerKmPaise = 12_00, advancePaise = 500_00))
        assertEquals(0L, r.balanceDuePaise)
        assertTrue(r.advanceExceedsTotal)
    }

    @Test fun shortPlaceSkipsHouseNumbers() {
        assertEquals("Mohamadwadi", com.amitabhtours.invoice.domain.Text.shortPlace("33/H/16, Mohamadwadi, Pune"))
        assertEquals("Tower B", com.amitabhtours.invoice.domain.Text.shortPlace("Flat 1204, Tower B, Marvel Fria"))
        assertEquals("Kubera Garden", com.amitabhtours.invoice.domain.Text.shortPlace("Kubera Garden, NIBM Road"))
    }

    @Test fun kmGrouping() {
        assertEquals("40,838", com.amitabhtours.invoice.domain.Num.grouped(40838.0))
        assertEquals("1,23,456.5", com.amitabhtours.invoice.domain.Num.grouped(123456.46))
        assertEquals("12", com.amitabhtours.invoice.domain.Num.grouped(12.0))
    }

    /** Chart x labels never overlap, never cross the y-axis, and the newest is always shown. */
    @Test fun chartLabelsNeverOverlap() {
        val gap = 8f
        for (count in listOf(1, 2, 7, 12, 13, 30, 31, 52)) {
            for (width in listOf(300f, 600f, 900f)) {
                for (labelW in listOf(15f, 40f, 70f)) {
                    val left = 60f
                    val slot = (width - left) / count
                    val centers = (0 until count).map { left + slot * it + slot / 2 }
                    val placed = com.amitabhtours.invoice.ui.earnings.xLabelPositions(List(count) { labelW }, centers, slot, left, width, gap)
                    assertTrue("count=$count width=$width", placed.isNotEmpty())
                    assertEquals(count - 1, placed.last().first)
                    placed.forEach { (_, x) -> assertTrue(x >= left && x + labelW <= width + 0.01f) }
                    placed.zipWithNext().forEach { (a, b) -> assertTrue("overlap count=$count w=$width lw=$labelW", a.second + labelW + gap <= b.second + 0.01f) }
                }
            }
        }
    }

    @Test fun financialYear() {
        assertEquals(2026, InvoiceNumbering.financialYearStart(LocalDate.of(2026, 4, 1)))
        assertEquals(2025, InvoiceNumbering.financialYearStart(LocalDate.of(2026, 3, 31)))
    }

    @Test fun numbering() {
        val first = InvoiceNumbering.issue("ATT", InvoiceNumbering.Counter(0, 1), LocalDate.of(2026, 10, 27), true)
        assertEquals("ATT-2026-0001", first.number)
        val second = InvoiceNumbering.issue("ATT", first.newCounter, LocalDate.of(2026, 11, 1), true)
        assertEquals("ATT-2026-0002", second.number)
        // New financial year resets the count
        val april = InvoiceNumbering.issue("ATT", second.newCounter, LocalDate.of(2027, 4, 2), true)
        assertEquals("ATT-2027-0001", april.number)
        // Reset off: count continues
        val noReset = InvoiceNumbering.issue("ATT", second.newCounter, LocalDate.of(2027, 4, 2), false)
        assertEquals("ATT-2027-0003", noReset.number)
        // Continue from the paper book
        val paper = InvoiceNumbering.issue("ATT", InvoiceNumbering.Counter(2026, 151), LocalDate.of(2026, 10, 27), true)
        assertEquals("ATT-2026-0151", paper.number)
        assertFalse(paper.number.contains("Tax"))
    }
}
