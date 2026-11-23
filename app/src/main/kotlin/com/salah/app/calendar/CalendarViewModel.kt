package com.salah.app.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.app.prayerLocation
import com.salah.location.SalahLocation
import com.salah.prayer.PrayerCalculationRequest
import com.salah.prayer.PrayerLocation
import com.salah.prayer.PrayerScheduleResult
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val clock: Clock,
) : ViewModel() {

    private val todayDate: LocalDate get() = LocalDate.now(clock)
    val displayedMonth = MutableStateFlow(YearMonth.from(todayDate))
    val selectedDate = MutableStateFlow(todayDate)
    private val retry = MutableStateFlow(0)
    private var lastHandledRetry = 0

    val settings: StateFlow<PrayerSettings?> = repository.prayerSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val locationSelection: StateFlow<SalahLocation?> = repository.selectedLocation
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val scheduleResultFlow: Flow<ScheduleStateSnapshot> = combine(
        repository.selectedLocation.map { it?.prayerLocation() }.distinctUntilChanged(),
        selectedDate,
        settings.filterNotNull(),
        retry,
    ) { site: PrayerLocation?, date: LocalDate, config: PrayerSettings, attempt: Int ->
        ScheduleQuery(site, date, config, attempt)
    }.flatMapLatest { query ->
        val site = query.site ?: return@flatMapLatest flowOf(ScheduleStateSnapshot(CalendarScheduleState.NoLocation, false))
        if (query.date.year !in 1900..9999) {
            return@flatMapLatest flowOf(ScheduleStateSnapshot(CalendarScheduleState.UnsupportedRange, false))
        }
        val force = query.attempt > 0 && query.attempt != lastHandledRetry
        if (force) lastHandledRetry = query.attempt

        val request = PrayerCalculationRequest(
            location = site,
            date = query.date,
            method = query.config.method,
            asr = query.config.asr,
            adjustments = query.config.adjustments,
        )

        flow {
            emit(ScheduleStateSnapshot(CalendarScheduleState.Loading, true))
            try {
                repository.schedule(request, refresh = force).collect { result ->
                    val scheduleState = when (result) {
                        is PrayerScheduleResult.Available -> CalendarScheduleState.Success(result.schedule, result.source)
                        is PrayerScheduleResult.Unavailable -> CalendarScheduleState.Error(result.retryable)
                    }
                    emit(ScheduleStateSnapshot(scheduleState, false))
                }
            } catch (_: Exception) {
                emit(ScheduleStateSnapshot(CalendarScheduleState.Error(retryable = true), false))
            }
        }
    }

    val state: StateFlow<CalendarUiState> = combine(
        displayedMonth,
        selectedDate,
        scheduleResultFlow,
        locationSelection,
        settings.filterNotNull(),
    ) { month: YearMonth, date: LocalDate, scheduleSnapshot: ScheduleStateSnapshot, location: SalahLocation?, config: PrayerSettings ->
        val monthData = CalendarDateConverter.createMonthData(
            yearMonth = month,
            selectedDate = date,
            todayDate = todayDate,
            locale = Locale.getDefault(),
        )
        CalendarUiState(
            displayedMonth = month,
            selectedDate = date,
            monthData = monthData,
            scheduleState = scheduleSnapshot.state,
            isScheduleLoading = scheduleSnapshot.isLoading,
            hasLocation = location != null,
            locationName = location?.displayName(),
            timeFormat = config.format,
            zone = location?.zone ?: ZoneId.systemDefault(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            displayedMonth = YearMonth.from(todayDate),
            selectedDate = todayDate,
            monthData = CalendarDateConverter.createMonthData(
                yearMonth = YearMonth.from(todayDate),
                selectedDate = todayDate,
                todayDate = todayDate,
                locale = Locale.getDefault(),
            ),
        ),
    )

    fun previousMonth() {
        displayedMonth.update { current ->
            val prev = current.minusMonths(1)
            if (selectedDate.value.year != prev.year || selectedDate.value.month != prev.month) {
                selectedDate.value = prev.atDay(1)
            }
            prev
        }
    }

    fun nextMonth() {
        displayedMonth.update { current ->
            val next = current.plusMonths(1)
            if (selectedDate.value.year != next.year || selectedDate.value.month != next.month) {
                selectedDate.value = next.atDay(1)
            }
            next
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        if (displayedMonth.value.year != date.year || displayedMonth.value.month != date.month) {
            displayedMonth.value = YearMonth.from(date)
        }
    }

    fun jumpToToday() {
        val today = todayDate
        displayedMonth.value = YearMonth.from(today)
        selectedDate.value = today
    }

    fun retry() {
        retry.update { it + 1 }
    }

    private data class ScheduleQuery(
        val site: PrayerLocation?,
        val date: LocalDate,
        val config: PrayerSettings,
        val attempt: Int,
    )

    private data class ScheduleStateSnapshot(
        val state: CalendarScheduleState,
        val isLoading: Boolean,
    )
}

private fun SalahLocation.displayName(): String {
    val place = names.locality ?: names.region ?: coordinates.toString()
    val country = names.country
    return if (country != null) "$place, $country" else place
}
