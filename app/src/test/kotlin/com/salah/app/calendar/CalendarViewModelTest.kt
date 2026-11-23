package com.salah.app.calendar

import com.salah.location.LocationCoordinates
import com.salah.location.LocationMode
import com.salah.location.PlaceNames
import com.salah.location.SalahLocation
import com.salah.prayer.DailyPrayerSchedule
import com.salah.prayer.PrayerCalculationRequest
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerScheduleResult
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val fixedNow = Instant.parse("2026-11-22T10:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val expectedToday = LocalDate.of(2026, 11, 22)
    private val expectedMonth = YearMonth.of(2026, 11)

    private val dubaiCoordinates = LocationCoordinates(25.2048, 55.2708)
    private val dubaiLocation = SalahLocation(
        id = "dubai",
        coordinates = dubaiCoordinates,
        zone = ZoneId.of("Asia/Dubai"),
        mode = LocationMode.MANUAL,
        names = PlaceNames(locality = "Dubai", country = "United Arab Emirates"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialViewModelStateHasTodayAndCurrentMonth() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = dubaiLocation)
        val vm = CalendarViewModel(repo, clock)

        assertEquals(expectedMonth, vm.displayedMonth.value)
        assertEquals(expectedToday, vm.selectedDate.value)
    }

    @Test
    fun previousMonthDecrementsMonthAndUpdatesSelectedDate() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = dubaiLocation)
        val vm = CalendarViewModel(repo, clock)

        vm.previousMonth()

        assertEquals(YearMonth.of(2026, 10), vm.displayedMonth.value)
        assertEquals(LocalDate.of(2026, 10, 1), vm.selectedDate.value)
    }

    @Test
    fun nextMonthIncrementsMonthAndUpdatesSelectedDate() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = dubaiLocation)
        val vm = CalendarViewModel(repo, clock)

        vm.nextMonth()

        assertEquals(YearMonth.of(2026, 12), vm.displayedMonth.value)
        assertEquals(LocalDate.of(2026, 12, 1), vm.selectedDate.value)
    }

    @Test
    fun selectDateUpdatesSelectedDateAndMonth() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = dubaiLocation)
        val vm = CalendarViewModel(repo, clock)

        val targetDate = LocalDate.of(2026, 11, 15)
        vm.selectDate(targetDate)

        assertEquals(targetDate, vm.selectedDate.value)
        assertEquals(YearMonth.of(2026, 11), vm.displayedMonth.value)

        val nextMonthDate = LocalDate.of(2026, 12, 25)
        vm.selectDate(nextMonthDate)

        assertEquals(nextMonthDate, vm.selectedDate.value)
        assertEquals(YearMonth.of(2026, 12), vm.displayedMonth.value)
    }

    @Test
    fun jumpToTodayRestoresCurrentDateAndMonth() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = dubaiLocation)
        val vm = CalendarViewModel(repo, clock)

        vm.nextMonth()
        vm.nextMonth()
        assertEquals(YearMonth.of(2027, 1), vm.displayedMonth.value)

        vm.jumpToToday()

        assertEquals(expectedMonth, vm.displayedMonth.value)
        assertEquals(expectedToday, vm.selectedDate.value)
    }

    @Test
    fun noLocationYieldsNoLocationScheduleState() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = null)
        val vm = CalendarViewModel(repo, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect()
        }

        advanceUntilIdle()

        val uiState = vm.state.value
        assertFalse(uiState.hasLocation)
        assertEquals(CalendarScheduleState.NoLocation, uiState.scheduleState)
    }

    @Test
    fun validLocationCalculatesPrayerTimesForSelectedDate() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = dubaiLocation)
        val vm = CalendarViewModel(repo, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect()
        }

        advanceUntilIdle()

        val uiState = vm.state.value
        assertTrue(uiState.hasLocation)
        assertTrue(
            "Schedule state should be Success for valid location",
            uiState.scheduleState is CalendarScheduleState.Success,
        )

        val success = uiState.scheduleState as CalendarScheduleState.Success
        assertEquals(expectedToday, success.schedule.request.date)
        assertEquals(6, success.schedule.timings.size)
        assertEquals(
            listOf(
                PrayerName.FAJR,
                PrayerName.SUNRISE,
                PrayerName.DHUHR,
                PrayerName.ASR,
                PrayerName.MAGHRIB,
                PrayerName.ISHA,
            ),
            success.schedule.timings.map { it.name },
        )
    }

    @Test
    fun retryIncrementsAttemptAndRefreshesSchedule() = testScope.runTest {
        val repo = FakeCalendarRepository(initialLocation = dubaiLocation)
        val vm = CalendarViewModel(repo, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect()
        }

        advanceUntilIdle()
        assertEquals(0, repo.refreshCallCount)

        vm.retry()
        advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    private class FakeCalendarRepository(
        initialLocation: SalahLocation?,
        initialSettings: PrayerSettings = PrayerSettings(),
    ) : CalendarRepository {
        val locationFlow = MutableStateFlow(initialLocation)
        val settingsFlow = MutableStateFlow(initialSettings)
        var refreshCallCount = 0

        override val selectedLocation: Flow<SalahLocation?> = locationFlow
        override val prayerSettings: Flow<PrayerSettings> = settingsFlow

        override fun schedule(
            request: PrayerCalculationRequest,
            refresh: Boolean,
        ): Flow<PrayerScheduleResult> = flow {
            if (refresh) refreshCallCount++
            val z = request.location.zone
            val d = request.date
            val timings = listOf(
                d.atTime(5, 10).atZone(z).toInstant(),
                d.atTime(6, 30).atZone(z).toInstant(),
                d.atTime(12, 15).atZone(z).toInstant(),
                d.atTime(15, 25).atZone(z).toInstant(),
                d.atTime(17, 35).atZone(z).toInstant(),
                d.atTime(18, 55).atZone(z).toInstant(),
            )
            val schedule = DailyPrayerSchedule(request, timings)
            emit(PrayerScheduleResult.Available(schedule, PrayerTimeSource.LOCAL_CACHE))
        }
    }
}
