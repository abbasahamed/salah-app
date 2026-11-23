package com.salah.app.widget

import com.salah.location.LocationCoordinates
import com.salah.location.LocationMode
import com.salah.location.PlaceNames
import com.salah.location.SalahLocation
import com.salah.prayer.AsrJuristicMethod
import com.salah.prayer.DailyPrayerSchedule
import com.salah.prayer.PrayerCalculationMethod
import com.salah.prayer.PrayerCalculationRequest
import com.salah.prayer.PrayerLocation
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerScheduleResult
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTimeAdjustments
import com.salah.prayer.PrayerTimeFormat
import com.salah.prayer.PrayerTimeSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDataRepositoryTest {

    private val zone = ZoneId.of("Asia/Dubai")
    private val coordinates = LocationCoordinates(25.2048, 55.2708)
    private val location = SalahLocation(
        id = "dubai",
        coordinates = coordinates,
        names = PlaceNames("Dubai", "Dubai", "United Arab Emirates"),
        zone = zone,
        mode = LocationMode.MANUAL,
    )
    private val prayerLocation = PrayerLocation(coordinates.latitude, coordinates.longitude, zone)

    private class FakeWidgetScheduleSource(
        var location: SalahLocation? = null,
        var settings: PrayerSettings = PrayerSettings(),
    ) : WidgetScheduleSource {
        private val schedules = mutableMapOf<LocalDate, DailyPrayerSchedule>()

        fun setSchedule(date: LocalDate, schedule: DailyPrayerSchedule) {
            schedules[date] = schedule
        }

        override suspend fun getSelectedLocation(): SalahLocation? = location
        override suspend fun getPrayerSettings(): PrayerSettings = settings
        override suspend fun getSchedule(request: PrayerCalculationRequest): PrayerScheduleResult {
            val sched = schedules[request.date]
            return if (sched != null) {
                PrayerScheduleResult.Available(sched, PrayerTimeSource.LOCAL_CACHE)
            } else {
                PrayerScheduleResult.Unavailable(retryable = false)
            }
        }
    }

    private fun createSchedule(
        date: LocalDate,
        adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
    ): DailyPrayerSchedule {
        val request = PrayerCalculationRequest(
            location = prayerLocation,
            date = date,
            method = PrayerCalculationMethod.KARACHI,
            asr = AsrJuristicMethod.STANDARD,
            adjustments = adjustments,
        )
        // Dubai timings: Fajr 05:15, Sunrise 06:35, Dhuhr 12:15, Asr 15:25, Maghrib 17:35, Isha 19:00
        val timings = listOf(
            date.atTime(5, 15).atZone(zone).toInstant(),
            date.atTime(6, 35).atZone(zone).toInstant(),
            date.atTime(12, 15).atZone(zone).toInstant(),
            date.atTime(15, 25).atZone(zone).toInstant(),
            date.atTime(17, 35).atZone(zone).toInstant(),
            date.atTime(19, 0).atZone(zone).toInstant(),
        )
        return DailyPrayerSchedule(request, timings)
    }

    @Test
    fun noLocationSelectedReturnsCleanEmptyState() = runTest {
        val source = FakeWidgetScheduleSource(location = null)
        val now = Instant.parse("2026-11-22T10:00:00Z")
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))

        val snapshot = repo.loadSnapshot()
        assertFalse("Widget must indicate no location", snapshot.hasLocation)
        assertNull("Next prayer name must be null", snapshot.nextPrayerName)
        assertTrue("Timeline must be empty", snapshot.timeline.isEmpty())
    }

    @Test
    fun daytimeNextPrayerAndTimelineResolvedAccurately() = runTest {
        val source = FakeWidgetScheduleSource(location = location)
        val todayDate = LocalDate.of(2026, 11, 22)
        val yesterdayDate = todayDate.minusDays(1)
        val tomorrowDate = todayDate.plusDays(1)

        source.setSchedule(yesterdayDate, createSchedule(yesterdayDate))
        source.setSchedule(todayDate, createSchedule(todayDate))
        source.setSchedule(tomorrowDate, createSchedule(tomorrowDate))

        // 10:00 AM in Dubai (UTC+4) on 2026-11-22 is 06:00 UTC
        val now = todayDate.atTime(10, 0).atZone(zone).toInstant()
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))

        val snapshot = repo.loadSnapshot()
        assertTrue("Snapshot has location", snapshot.hasLocation)
        assertEquals("Dubai", snapshot.localityName)
        assertEquals("Dhuhr", snapshot.nextPrayerName)
        assertEquals("12:15", snapshot.nextPrayerTimeFormatted)
        assertTrue("Countdown starts with 'in'", snapshot.countdownText!!.startsWith("in "))
        assertEquals("in 2 hr 15 min", snapshot.countdownText)
        assertEquals("2h 15m", snapshot.countdownCompactText)
        assertFalse("Is not tomorrow", snapshot.isTomorrow)
        assertNotNull("Progress fraction computed", snapshot.progressFraction)

        assertEquals("Timeline has 5 obligatory prayers", 5, snapshot.timeline.size)
        assertEquals("Until Dhuhr · 12:15", snapshot.heroContextLine)

        val fajr = snapshot.timeline.first { it.prayer == PrayerName.FAJR }
        assertTrue("Fajr is passed", fajr.isPassed)
        assertFalse("Fajr is not next", fajr.isNext)

        val sunrise = snapshot.timeline.firstOrNull { it.prayer == PrayerName.SUNRISE }
        assertNull("Sunrise is omitted from the widget timeline", sunrise)

        val dhuhr = snapshot.timeline.first { it.prayer == PrayerName.DHUHR }
        assertTrue("Dhuhr is the active next prayer", dhuhr.isNext)
        assertFalse("Dhuhr is not passed", dhuhr.isPassed)

        val asr = snapshot.timeline.first { it.prayer == PrayerName.ASR }
        assertFalse("Asr is not next", asr.isNext)
        assertFalse("Asr is not passed", asr.isPassed)
    }

    @Test
    fun afterIshaRollsOverToTomorrowFajr() = runTest {
        val source = FakeWidgetScheduleSource(location = location)
        val todayDate = LocalDate.of(2026, 11, 22)
        val yesterdayDate = todayDate.minusDays(1)
        val tomorrowDate = todayDate.plusDays(1)

        source.setSchedule(yesterdayDate, createSchedule(yesterdayDate))
        source.setSchedule(todayDate, createSchedule(todayDate))
        source.setSchedule(tomorrowDate, createSchedule(tomorrowDate))

        // 10:30 PM in Dubai is after Isha (19:00)
        val now = todayDate.atTime(22, 30).atZone(zone).toInstant()
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))

        val snapshot = repo.loadSnapshot()
        assertTrue("Snapshot has location", snapshot.hasLocation)
        assertEquals("Fajr", snapshot.nextPrayerName)
        assertEquals("05:15", snapshot.nextPrayerTimeFormatted)
        assertTrue("Fajr is tomorrow", snapshot.isTomorrow)
        assertEquals("Until Fajr · 05:15 tomorrow", snapshot.heroContextLine)

        // Timeline after Isha shows tomorrow's schedule with Fajr highlighted (5 obligatory prayers)
        assertEquals(5, snapshot.timeline.size)
        val tomorrowFajr = snapshot.timeline.first { it.prayer == PrayerName.FAJR }
        assertTrue("Tomorrow's Fajr is marked as next", tomorrowFajr.isNext)
        assertFalse("Tomorrow's Fajr is not passed", tomorrowFajr.isPassed)
    }

    @Test
    fun twelveHourFormatDisplaysPeriod() = runTest {
        val source = FakeWidgetScheduleSource(
            location = location,
            settings = PrayerSettings(format = PrayerTimeFormat.TWELVE_HOUR),
        )
        val todayDate = LocalDate.of(2026, 11, 22)
        source.setSchedule(todayDate, createSchedule(todayDate))

        val now = todayDate.atTime(10, 0).atZone(zone).toInstant()
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))

        val snapshot = repo.loadSnapshot()
        assertEquals("Dhuhr", snapshot.nextPrayerName)
        assertNotNull("12-hour format must include period", snapshot.nextPrayerPeriod)
        assertEquals("PM", snapshot.nextPrayerPeriod)
        assertEquals("12:15", snapshot.nextPrayerTimeFormatted)
    }

    @Test
    fun twentyFourHourFormatOmitsPeriod() = runTest {
        val source = FakeWidgetScheduleSource(
            location = location,
            settings = PrayerSettings(format = PrayerTimeFormat.TWENTY_FOUR_HOUR),
        )
        val todayDate = LocalDate.of(2026, 11, 22)
        source.setSchedule(todayDate, createSchedule(todayDate))

        val now = todayDate.atTime(10, 0).atZone(zone).toInstant()
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))

        val snapshot = repo.loadSnapshot()
        assertEquals("Dhuhr", snapshot.nextPrayerName)
        assertNull("24-hour format period must be null", snapshot.nextPrayerPeriod)
        assertEquals("12:15", snapshot.nextPrayerTimeFormatted)
    }

    @Test
    fun minuteAdjustmentsShiftPrayerTimes() = runTest {
        val todayDate = LocalDate.of(2026, 11, 22)
        val now = todayDate.atTime(10, 0).atZone(zone).toInstant()

        val sourceBaseline = FakeWidgetScheduleSource(location = location)
        sourceBaseline.setSchedule(todayDate, createSchedule(todayDate))
        val repoBaseline = WidgetDataRepository(sourceBaseline, Clock.fixed(now, zone))
        val baselineSnapshot = repoBaseline.loadSnapshot()
        assertEquals("12:15", baselineSnapshot.nextPrayerTimeFormatted)

        // Adjust Dhuhr by +5 minutes
        val adjustedSettings = PrayerSettings(adjustments = PrayerTimeAdjustments(dhuhr = 5))
        val sourceAdjusted = FakeWidgetScheduleSource(
            location = location,
            settings = adjustedSettings,
        )
        sourceAdjusted.setSchedule(todayDate, createSchedule(todayDate, adjustments = PrayerTimeAdjustments(dhuhr = 5)))
        val repoAdjusted = WidgetDataRepository(sourceAdjusted, Clock.fixed(now, zone))
        val adjustedSnapshot = repoAdjusted.loadSnapshot()
        assertEquals("12:20", adjustedSnapshot.nextPrayerTimeFormatted)
    }

    @Test
    fun exactlyOnePrayerMarkedNextInDaytimeSchedule() = runTest {
        val source = FakeWidgetScheduleSource(location = location)
        val todayDate = LocalDate.of(2026, 11, 22)
        source.setSchedule(todayDate, createSchedule(todayDate))

        // Test at 14:00 (between Dhuhr 12:15 and Asr 15:25) -> Asr must be the only isNext
        val now = todayDate.atTime(14, 0).atZone(zone).toInstant()
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))
        val snapshot = repo.loadSnapshot()

        assertEquals("Exactly one prayer must be marked isNext", 1, snapshot.timeline.count { it.isNext })
        val nextItem = snapshot.timeline.first { it.isNext }
        assertEquals("Next prayer must be Asr", PrayerName.ASR, nextItem.prayer)
        assertFalse("Active next prayer must not be passed", nextItem.isPassed)

        val passedItems = snapshot.timeline.filter { it.isPassed }
        assertEquals("Fajr and Dhuhr must be marked passed", 2, passedItems.size)
        assertTrue("Fajr is passed", passedItems.any { it.prayer == PrayerName.FAJR })
        assertTrue("Dhuhr is passed", passedItems.any { it.prayer == PrayerName.DHUHR })
    }

    @Test
    fun afterIshaOnlyTomorrowFajrIsNextAndNoPastPrayerMarkedUpcoming() = runTest {
        val source = FakeWidgetScheduleSource(location = location)
        val todayDate = LocalDate.of(2026, 11, 22)
        val tomorrowDate = todayDate.plusDays(1)
        source.setSchedule(todayDate, createSchedule(todayDate))
        source.setSchedule(tomorrowDate, createSchedule(tomorrowDate))

        val now = todayDate.atTime(22, 0).atZone(zone).toInstant()
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))
        val snapshot = repo.loadSnapshot()

        assertEquals("Exactly one prayer must be marked isNext after rollover", 1, snapshot.timeline.count { it.isNext })
        val nextItem = snapshot.timeline.first { it.isNext }
        assertEquals("Tomorrow's Fajr must be the only isNext", PrayerName.FAJR, nextItem.prayer)
        assertEquals(0, snapshot.timeline.count { it.isPassed })
    }

    @Test
    fun dayRolloverWhenTimelineIsTodayHasZeroUpcomingPrayers() = runTest {
        val source = FakeWidgetScheduleSource(location = location)
        val todayDate = LocalDate.of(2026, 11, 22)
        val tomorrowDate = todayDate.plusDays(1)
        source.setSchedule(todayDate, createSchedule(todayDate))
        source.setSchedule(tomorrowDate, createSchedule(tomorrowDate))

        val now = todayDate.atTime(22, 0).atZone(zone).toInstant()
        val repo = WidgetDataRepository(source, Clock.fixed(now, zone))

        // If today's timings are evaluated against tomorrow's next prayer:
        val todaySched = createSchedule(todayDate)
        val tomorrowSched = createSchedule(tomorrowDate)
        val resolver = com.salah.prayer.NextPrayerResolver(Clock.fixed(now, zone))
        val nextPrayer = resolver.resolve(listOf(todaySched, tomorrowSched), now)!!
        assertEquals("Next prayer is tomorrow Fajr", PrayerName.FAJR, nextPrayer.time.name)

        // Verify that matching today's timings against tomorrow's next prayer produces 0 isNext
        val isNextOnToday = todaySched.timings.filter { it.name != PrayerName.SUNRISE }.any { timing ->
            timing.name == nextPrayer.time.name && timing.instant == nextPrayer.time.instant
        }
        assertFalse("Today's past Fajr must NOT be marked isNext when next prayer is tomorrow", isNextOnToday)
    }

    @Test
    fun locationSwitchingUpdatesScheduleAndPreservesSingleSelection() = runTest {
        val londonZone = ZoneId.of("Europe/London")
        val londonCoords = LocationCoordinates(51.5074, -0.1278)
        val londonLocation = SalahLocation(
            id = "london",
            coordinates = londonCoords,
            names = PlaceNames("London", "England", "United Kingdom"),
            zone = londonZone,
            mode = LocationMode.MANUAL,
        )

        val source = FakeWidgetScheduleSource(location = location)
        val todayDate = LocalDate.of(2026, 11, 22)
        source.setSchedule(todayDate, createSchedule(todayDate))

        val now = todayDate.atTime(10, 0).atZone(zone).toInstant()
        val repoDubai = WidgetDataRepository(source, Clock.fixed(now, zone))
        val dubaiSnapshot = repoDubai.loadSnapshot()
        assertEquals("Dubai", dubaiSnapshot.localityName)
        assertEquals(1, dubaiSnapshot.timeline.count { it.isNext })

        // Switch source location to London
        source.location = londonLocation
        val londonReq = PrayerCalculationRequest(
            location = PrayerLocation(londonCoords.latitude, londonCoords.longitude, londonZone),
            date = todayDate,
            method = PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE,
            asr = AsrJuristicMethod.STANDARD,
        )
        val londonTimings = listOf(
            todayDate.atTime(5, 30).atZone(londonZone).toInstant(),
            todayDate.atTime(7, 30).atZone(londonZone).toInstant(),
            todayDate.atTime(11, 50).atZone(londonZone).toInstant(),
            todayDate.atTime(13, 40).atZone(londonZone).toInstant(),
            todayDate.atTime(16, 5).atZone(londonZone).toInstant(),
            todayDate.atTime(18, 0).atZone(londonZone).toInstant(),
        )
        source.setSchedule(todayDate, DailyPrayerSchedule(londonReq, londonTimings))

        val repoLondon = WidgetDataRepository(source, Clock.fixed(now, londonZone))
        val londonSnapshot = repoLondon.loadSnapshot()
        assertEquals("London", londonSnapshot.localityName)
        assertEquals("Exactly one prayer must be selected after location change", 1, londonSnapshot.timeline.count { it.isNext })
    }

    @Test
    fun countdownCompactFormattingMatchesRequirements() = runTest {
        val source = FakeWidgetScheduleSource(location = location)
        val todayDate = LocalDate.of(2026, 11, 22)
        source.setSchedule(todayDate, createSchedule(todayDate))

        // Dhuhr is at 12:15. At 10:15 -> exactly 2 hr 0 min
        val now2h = todayDate.atTime(10, 15).atZone(zone).toInstant()
        val repo2h = WidgetDataRepository(source, Clock.fixed(now2h, zone))
        val snap2h = repo2h.loadSnapshot()
        assertEquals("in 2 hr 0 min", snap2h.countdownText)
        assertEquals("2h 0m", snap2h.countdownCompactText)

        // At 11:45 -> 30 min
        val now30m = todayDate.atTime(11, 45).atZone(zone).toInstant()
        val repo30m = WidgetDataRepository(source, Clock.fixed(now30m, zone))
        val snap30m = repo30m.loadSnapshot()
        assertEquals("in 30 min", snap30m.countdownText)
        assertEquals("30m", snap30m.countdownCompactText)

        // At 12:14 -> 1 min
        val now1m = todayDate.atTime(12, 14).atZone(zone).toInstant()
        val repo1m = WidgetDataRepository(source, Clock.fixed(now1m, zone))
        val snap1m = repo1m.loadSnapshot()
        assertEquals("in 1 min", snap1m.countdownText)
        assertEquals("1m", snap1m.countdownCompactText)
    }
}
