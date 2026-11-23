package com.salah.app.calendar

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.WeekFields
import java.util.Locale

object CalendarDateConverter {
    val chronology: HijrahChronology = HijrahChronology.INSTANCE

    /**
     * Converts a civil Gregorian [LocalDate] to a calculated Umm al-Qura [HijrahDate].
     * Returns null if the date is outside the supported Umm al-Qura chronology range (1300 to 1600 AH).
     */
    fun toHijrahDate(date: LocalDate): HijrahDate? = try {
        HijrahDate.from(date)
    } catch (_: DateTimeException) {
        null
    } catch (_: Exception) {
        null
    }

    /**
     * Standard Islamic month names for the 12 Hijri months.
     */
    fun hijriMonthName(month: Int): String = when (month) {
        1 -> "Muharram"
        2 -> "Safar"
        3 -> "Rabiʻ al-Awwal"
        4 -> "Rabiʻ al-Thani"
        5 -> "Jumada al-Awwal"
        6 -> "Jumada al-Akhirah"
        7 -> "Rajab"
        8 -> "Shaʻban"
        9 -> "Ramadan"
        10 -> "Shawwal"
        11 -> "Dhu al-Qiʻdah"
        12 -> "Dhu al-Hijjah"
        else -> month.toString()
    }

    /**
     * Concise Islamic month names for compact subtitles and badges (e.g., "Jumada I", "Rabiʻ II").
     */
    fun hijriMonthConciseName(month: Int): String = when (month) {
        1 -> "Muharram"
        2 -> "Safar"
        3 -> "Rabiʻ I"
        4 -> "Rabiʻ II"
        5 -> "Jumada I"
        6 -> "Jumada II"
        7 -> "Rajab"
        8 -> "Shaʻban"
        9 -> "Ramadan"
        10 -> "Shawwal"
        11 -> "Dhu al-Qiʻdah"
        12 -> "Dhu al-Hijjah"
        else -> month.toString()
    }

    /**
     * Formats the Hijri day of the month as a number (e.g., "12").
     */
    fun formatHijriDay(hijrahDate: HijrahDate?): String =
        hijrahDate?.get(ChronoField.DAY_OF_MONTH)?.toString().orEmpty()

    /**
     * Formats the full Hijri date with day, month name, year, and era (e.g., "12 Jumada al-Akhirah 1448 AH").
     */
    fun formatHijriDateFull(hijrahDate: HijrahDate?, locale: Locale = Locale.getDefault()): String {
        if (hijrahDate == null) return ""
        val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
        val month = hijriMonthName(hijrahDate.get(ChronoField.MONTH_OF_YEAR))
        val year = hijrahDate.get(ChronoField.YEAR)
        return "$day $month $year AH"
    }

    /**
     * Computes the concise calculated Hijri month summary spanning the Gregorian [yearMonth].
     * Example: "Jumada I–II · 1448 AH" or "Dhu al-Hijjah 1446 – Muharram 1447 AH".
     */
    fun computeHijriMonthSummary(yearMonth: YearMonth, locale: Locale = Locale.getDefault()): String {
        val startHijrah = toHijrahDate(yearMonth.atDay(1)) ?: return ""
        val endHijrah = toHijrahDate(yearMonth.atEndOfMonth()) ?: return ""

        val startMonthNum = startHijrah.get(ChronoField.MONTH_OF_YEAR)
        val endMonthNum = endHijrah.get(ChronoField.MONTH_OF_YEAR)
        val startYear = startHijrah.get(ChronoField.YEAR)
        val endYear = endHijrah.get(ChronoField.YEAR)

        if (startYear != endYear) {
            val startMonth = hijriMonthConciseName(startMonthNum)
            val endMonth = hijriMonthConciseName(endMonthNum)
            return "$startMonth $startYear – $endMonth $endYear AH"
        }

        // Shared base for paired months like Jumada I & II or Rabiʻ I & II
        if (startMonthNum == 5 && endMonthNum == 6) {
            return "Jumada I–II · $startYear AH"
        }
        if (startMonthNum == 3 && endMonthNum == 4) {
            return "Rabiʻ I–II · $startYear AH"
        }

        val startMonth = hijriMonthConciseName(startMonthNum)
        val endMonth = hijriMonthConciseName(endMonthNum)
        return if (startMonthNum != endMonthNum) {
            "$startMonth – $endMonth · $startYear AH"
        } else {
            "$startMonth · $startYear AH"
        }
    }

    /**
     * Computes full unabbreviated Hijri month summary for screen-reader accessibility semantics.
     * Example: "Jumada al-Awwal to Jumada al-Akhirah 1448 AH".
     */
    fun computeHijriMonthFullSummary(yearMonth: YearMonth, locale: Locale = Locale.getDefault()): String {
        val startHijrah = toHijrahDate(yearMonth.atDay(1)) ?: return ""
        val endHijrah = toHijrahDate(yearMonth.atEndOfMonth()) ?: return ""

        val startMonth = hijriMonthName(startHijrah.get(ChronoField.MONTH_OF_YEAR))
        val endMonth = hijriMonthName(endHijrah.get(ChronoField.MONTH_OF_YEAR))
        val startYear = startHijrah.get(ChronoField.YEAR)
        val endYear = endHijrah.get(ChronoField.YEAR)

        return when {
            startYear != endYear -> "$startMonth $startYear to $endMonth $endYear AH"
            startMonth != endMonth -> "$startMonth to $endMonth $startYear AH"
            else -> "$startMonth $startYear AH"
        }
    }

    /**
     * Builds the complete 7-column calendar matrix for [yearMonth], aligning with the locale's
     * first day of the week, and padding with non-current month dates to complete rows.
     */
    fun createMonthData(
        yearMonth: YearMonth,
        selectedDate: LocalDate,
        todayDate: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): CalendarMonthData {
        val weekFields = WeekFields.of(locale)
        val firstDayOfWeek = weekFields.firstDayOfWeek
        val firstOfMonth = yearMonth.atDay(1)

        val leadingDays = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val days = mutableListOf<CalendarDay>()

        // Preceding month adjacent days
        if (leadingDays > 0) {
            val prevMonth = yearMonth.minusMonths(1)
            val prevMonthLength = prevMonth.lengthOfMonth()
            for (i in (leadingDays - 1) downTo 0) {
                val date = prevMonth.atDay(prevMonthLength - i)
                val hijrah = toHijrahDate(date)
                val isMonthStart = hijrah != null && hijrah.get(ChronoField.DAY_OF_MONTH) == 1
                days.add(
                    CalendarDay(
                        date = date,
                        hijriDate = hijrah,
                        isCurrentMonth = false,
                        isToday = (date == todayDate),
                        isSelected = (date == selectedDate),
                        isHijriMonthStart = isMonthStart,
                    ),
                )
            }
        }

        // Current month days
        val currentMonthLength = yearMonth.lengthOfMonth()
        for (day in 1..currentMonthLength) {
            val date = yearMonth.atDay(day)
            val hijrah = toHijrahDate(date)
            val isMonthStart = hijrah != null && hijrah.get(ChronoField.DAY_OF_MONTH) == 1
            days.add(
                CalendarDay(
                    date = date,
                    hijriDate = hijrah,
                    isCurrentMonth = true,
                    isToday = (date == todayDate),
                    isSelected = (date == selectedDate),
                    isHijriMonthStart = isMonthStart,
                ),
            )
        }

        // Trailing next month adjacent days to complete the final row
        val trailingDays = (7 - (days.size % 7)) % 7
        if (trailingDays > 0) {
            val nextMonth = yearMonth.plusMonths(1)
            for (day in 1..trailingDays) {
                val date = nextMonth.atDay(day)
                val hijrah = toHijrahDate(date)
                val isMonthStart = hijrah != null && hijrah.get(ChronoField.DAY_OF_MONTH) == 1
                days.add(
                    CalendarDay(
                        date = date,
                        hijriDate = hijrah,
                        isCurrentMonth = false,
                        isToday = (date == todayDate),
                        isSelected = (date == selectedDate),
                        isHijriMonthStart = isMonthStart,
                    ),
                )
            }
        }

        val weeks = days.chunked(7)
        val hijriSummary = computeHijriMonthSummary(yearMonth, locale)
        val hijriFullSummary = computeHijriMonthFullSummary(yearMonth, locale)

        return CalendarMonthData(
            yearMonth = yearMonth,
            weeks = weeks,
            hijriMonthSummary = hijriSummary,
            hijriMonthFullSummary = hijriFullSummary,
        )
    }

    /**
     * Returns the 7 localized weekday header labels ordered starting from the locale's first day of the week.
     */
    fun orderedWeekdays(locale: Locale = Locale.getDefault()): List<DayOfWeek> {
        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
        return (0..6).map { firstDayOfWeek.plus(it.toLong()) }
    }
}

