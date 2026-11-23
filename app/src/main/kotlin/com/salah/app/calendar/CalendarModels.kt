package com.salah.app.calendar

import com.salah.prayer.DailyPrayerSchedule
import com.salah.prayer.PrayerTimeFormat
import com.salah.prayer.PrayerTimeSource
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.chrono.HijrahDate

data class CalendarDay(
    val date: LocalDate,
    val hijriDate: HijrahDate?,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isHijriMonthStart: Boolean = false,
)

data class CalendarMonthData(
    val yearMonth: YearMonth,
    val weeks: List<List<CalendarDay>>,
    val hijriMonthSummary: String,
    val hijriMonthFullSummary: String = hijriMonthSummary,
)

sealed interface CalendarScheduleState {
    data object Loading : CalendarScheduleState
    data class Success(
        val schedule: DailyPrayerSchedule,
        val source: PrayerTimeSource,
    ) : CalendarScheduleState
    data class Error(val retryable: Boolean) : CalendarScheduleState
    data object NoLocation : CalendarScheduleState
    data object UnsupportedRange : CalendarScheduleState
}

data class CalendarUiState(
    val displayedMonth: YearMonth,
    val selectedDate: LocalDate,
    val monthData: CalendarMonthData,
    val scheduleState: CalendarScheduleState = CalendarScheduleState.Loading,
    val isScheduleLoading: Boolean = false,
    val hasLocation: Boolean = true,
    val locationName: String? = null,
    val timeFormat: PrayerTimeFormat = PrayerTimeFormat.TWENTY_FOUR_HOUR,
    val zone: ZoneId = ZoneId.systemDefault(),
)

