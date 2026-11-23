package com.salah.app.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoField
import java.util.Locale

class CalendarDateConverterTest {

    @Test
    fun convertsGregorianDateToCalculatedUmmAlQuraHijriDate() {
        // Standard fixture: 22 November 2026 -> 12 Jumada II 1448 AH
        val date = LocalDate.of(2026, 11, 22)
        val hijrah = CalendarDateConverter.toHijrahDate(date)

        assertNotNull(hijrah)
        assertEquals(1448, hijrah?.get(ChronoField.YEAR))
        assertEquals(6, hijrah?.get(ChronoField.MONTH_OF_YEAR)) // Jumada al-Thani / Jumada II
        assertEquals(12, hijrah?.get(ChronoField.DAY_OF_MONTH))

        val dayFormatted = CalendarDateConverter.formatHijriDay(hijrah)
        assertEquals("12", dayFormatted)

        val fullFormatted = CalendarDateConverter.formatHijriDateFull(hijrah, Locale.ENGLISH)
        assertTrue(fullFormatted.contains("12"))
        assertTrue(fullFormatted.contains("1448"))
        assertTrue(fullFormatted.contains("AH"))
    }

    @Test
    fun computesHijriMonthSummaryWithMonthSpanning() {
        // November 2026 spans Jumada I and Jumada II in year 1448 AH
        val ym = YearMonth.of(2026, 11)
        val summary = CalendarDateConverter.computeHijriMonthSummary(ym, Locale.ENGLISH)
        val fullSummary = CalendarDateConverter.computeHijriMonthFullSummary(ym, Locale.ENGLISH)

        assertEquals("Jumada I–II · 1448 AH", summary)
        assertTrue("Full summary should contain unabbreviated names", fullSummary.contains("Jumada al-Awwal"))
        assertTrue("Full summary should mention year 1448 AH", fullSummary.contains("1448 AH"))
    }

    @Test
    fun identifiesFirstDayOfHijriMonthInGrid() {
        val ym = YearMonth.of(2026, 11)
        val selected = LocalDate.of(2026, 11, 22)
        val today = LocalDate.of(2026, 11, 22)
        val monthData = CalendarDateConverter.createMonthData(ym, selected, today, Locale.US)

        val days = monthData.weeks.flatten()
        val firstOfHijri = days.filter { it.isCurrentMonth && it.isHijriMonthStart }
        assertEquals("November 2026 should contain exactly 1 Hijri month start (1 Jumada II)", 1, firstOfHijri.size)
        assertEquals(11, firstOfHijri.first().date.dayOfMonth)
        assertEquals(1, firstOfHijri.first().hijriDate?.get(ChronoField.DAY_OF_MONTH))
    }

    @Test
    fun computesHijriMonthSummaryAcrossHijriYearBoundary() {
        // June 2025 crosses from Dhu'l-Hijjah 1446 to Muharram 1447
        val ym = YearMonth.of(2025, 6)
        val summary = CalendarDateConverter.computeHijriMonthSummary(ym, Locale.ENGLISH)

        assertTrue("Summary should mention 1446", summary.contains("1446"))
        assertTrue("Summary should mention 1447", summary.contains("1447"))
    }

    @Test
    fun handlesOutOfRangeDatesGracefully() {
        // Year 1800 is outside Umm al-Qura range (starts ~1882)
        val oldDate = LocalDate.of(1800, 1, 1)
        val oldHijrah = CalendarDateConverter.toHijrahDate(oldDate)
        assertNull("Dates before 1882 should return null safely", oldHijrah)
        assertEquals("", CalendarDateConverter.formatHijriDay(oldHijrah))
        assertEquals("", CalendarDateConverter.formatHijriDateFull(oldHijrah))

        // Year 2500 is outside Umm al-Qura range
        val futureDate = LocalDate.of(2500, 1, 1)
        val futureHijrah = CalendarDateConverter.toHijrahDate(futureDate)
        assertNull("Dates far in future should return null safely", futureHijrah)
    }

    @Test
    fun generatesCorrect7ColumnGridForStandardMonth() {
        // November 2026 in US locale (starts Sunday)
        val ym = YearMonth.of(2026, 11)
        val selected = LocalDate.of(2026, 11, 22)
        val today = LocalDate.of(2026, 11, 22)
        val monthData = CalendarDateConverter.createMonthData(ym, selected, today, Locale.US)

        assertEquals(ym, monthData.yearMonth)
        // All weeks should have exactly 7 days
        assertTrue("Must have 4, 5, or 6 weeks", monthData.weeks.size in 4..6)
        monthData.weeks.forEach { week ->
            assertEquals(7, week.size)
        }

        // Total current-month days must match lengthOfMonth (30)
        val allDays = monthData.weeks.flatten()
        val currentMonthDays = allDays.filter { it.isCurrentMonth }
        assertEquals(30, currentMonthDays.size)
        assertEquals(1, currentMonthDays.first().date.dayOfMonth)
        assertEquals(30, currentMonthDays.last().date.dayOfMonth)

        // Selected day check
        val selectedDay = allDays.first { it.date == selected }
        assertTrue(selectedDay.isSelected)
        assertTrue(selectedDay.isToday)
    }

    @Test
    fun handlesFebruaryLeapYearAndNonLeapYear() {
        // Feb 2024 (Leap year -> 29 days)
        val leapData = CalendarDateConverter.createMonthData(
            YearMonth.of(2024, 2),
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 2, 1),
            Locale.US,
        )
        assertEquals(29, leapData.weeks.flatten().count { it.isCurrentMonth })

        // Feb 2025 (Non-leap year -> 28 days)
        val nonLeapData = CalendarDateConverter.createMonthData(
            YearMonth.of(2025, 2),
            LocalDate.of(2025, 2, 1),
            LocalDate.of(2025, 2, 1),
            Locale.US,
        )
        assertEquals(28, nonLeapData.weeks.flatten().count { it.isCurrentMonth })
    }

    @Test
    fun ordersWeekdaysCorrectlyBasedOnLocale() {
        val usWeekdays = CalendarDateConverter.orderedWeekdays(Locale.US)
        assertEquals(7, usWeekdays.size)
        assertEquals(DayOfWeek.SUNDAY, usWeekdays.first())
        assertEquals(DayOfWeek.SATURDAY, usWeekdays.last())

        val gbWeekdays = CalendarDateConverter.orderedWeekdays(Locale.UK)
        assertEquals(7, gbWeekdays.size)
        assertEquals(DayOfWeek.MONDAY, gbWeekdays.first())
        assertEquals(DayOfWeek.SUNDAY, gbWeekdays.last())
    }
}

