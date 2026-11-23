package com.salah.app

import com.salah.prayer.AsrJuristicMethod
import com.salah.prayer.DailyPrayerSchedule
import com.salah.prayer.NextPrayerResolver
import com.salah.prayer.PrayerCalculationMethod
import com.salah.prayer.PrayerCalculationRequest
import com.salah.prayer.PrayerLocation
import com.salah.prayer.PrayerName
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayPresentationTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val location = PrayerLocation(12.820254, 80.037819, zone)
    private val todayDate = LocalDate.of(2026, 11, 21)
    private val tomorrowDate = todayDate.plusDays(1)

    private val requestToday = PrayerCalculationRequest(
        location, todayDate, PrayerCalculationMethod.KARACHI, AsrJuristicMethod.STANDARD,
    )
    private val requestTomorrow = requestToday.copy(date = tomorrowDate)

    private fun createSchedule(request: PrayerCalculationRequest): DailyPrayerSchedule {
        val z = request.location.zone
        val d = request.date
        val timings = listOf(
            d.atTime(5, 0).atZone(z).toInstant(),
            d.atTime(6, 15).atZone(z).toInstant(),
            d.atTime(12, 10).atZone(z).toInstant(),
            d.atTime(15, 30).atZone(z).toInstant(),
            d.atTime(18, 5).atZone(z).toInstant(),
            d.atTime(19, 20).atZone(z).toInstant(),
        )
        return DailyPrayerSchedule(request, timings)
    }

    private val scheduleToday = createSchedule(requestToday)
    private val scheduleTomorrow = createSchedule(requestTomorrow)

    @Test
    fun consecutiveFajrTimesHaveDifferentInstants() {
        val todayFajr = scheduleToday.timings.first { it.name == PrayerName.FAJR }
        val tomorrowFajr = scheduleTomorrow.timings.first { it.name == PrayerName.FAJR }

        assertEquals(PrayerName.FAJR, todayFajr.name)
        assertEquals(PrayerName.FAJR, tomorrowFajr.name)
        assertNotEquals(todayFajr.instant, tomorrowFajr.instant)
        assertTrue(tomorrowFajr.instant > todayFajr.instant)
        val diffHours = Duration.between(todayFajr.instant, tomorrowFajr.instant).toHours()
        assertEquals(24L, diffHours)
    }

    @Test
    fun noIncorrectNextHighlightOnTodayScheduleAfterIsha() {
        // Given a time after Isha today
        val ishaInstant = scheduleToday.timings.first { it.name == PrayerName.ISHA }.instant
        val afterIsha = ishaInstant.plusSeconds(1800) // 30 minutes after Isha

        val resolver = NextPrayerResolver(Clock.fixed(afterIsha, zone))
        val next = resolver.resolve(listOf(scheduleToday, scheduleTomorrow), afterIsha)

        assertNotNull(next)
        val upcoming = next!!.time

        // Upcoming is tomorrow's Fajr
        assertEquals(PrayerName.FAJR, upcoming.name)
        assertEquals(scheduleTomorrow.timings.first { it.name == PrayerName.FAJR }.instant, upcoming.instant)

        // On today's schedule, verify that NONE of today's prayer occurrences match the upcoming prayer
        scheduleToday.timings.forEach { occurrence ->
            val isUpcoming = occurrence.name == upcoming.name && occurrence.instant == upcoming.instant
            assertFalse(
                "Today's ${occurrence.name} should NOT be marked as upcoming when next prayer is tomorrow's Fajr",
                isUpcoming,
            )
        }
    }

    @Test
    fun upcomingMatchesSameDayPrayerBeforeIsha() {
        // Given a time between Dhuhr and Asr
        val dhuhrInstant = scheduleToday.timings.first { it.name == PrayerName.DHUHR }.instant
        val timeBetween = dhuhrInstant.plusSeconds(3600) // 1 hour after Dhuhr

        val resolver = NextPrayerResolver(Clock.fixed(timeBetween, zone))
        val next = resolver.resolve(listOf(scheduleToday), timeBetween)

        assertNotNull(next)
        val upcoming = next!!.time
        assertEquals(PrayerName.ASR, upcoming.name)

        // In today's schedule, Asr matches and others do not
        val asrOccurrence = scheduleToday.timings.first { it.name == PrayerName.ASR }
        val isAsrUpcoming = asrOccurrence.name == upcoming.name && asrOccurrence.instant == upcoming.instant
        assertTrue("Asr should be marked as upcoming", isAsrUpcoming)

        val fajrOccurrence = scheduleToday.timings.first { it.name == PrayerName.FAJR }
        val isFajrUpcoming = fajrOccurrence.name == upcoming.name && fajrOccurrence.instant == upcoming.instant
        assertFalse("Fajr should NOT be marked as upcoming", isFajrUpcoming)
    }

    @Test
    fun countdownFormattingVerification() {
        fun formatCountdown(duration: Duration): String {
            val seconds = duration.seconds + if (duration.nano > 0) 1 else 0
            val hours = (seconds / 3600).toInt()
            val minutes = (seconds / 60 % 60).toInt()
            val secondsPart = (seconds % 60).toInt()
            return buildString {
                append("in ")
                if (hours > 0) {
                    append("$hours hr ")
                }
                append("$minutes min")
                if (hours == 0 && secondsPart > 0) {
                    append(" $secondsPart s")
                }
            }
        }

        assertEquals("in 10 hr 48 min", formatCountdown(Duration.ofHours(10).plusMinutes(48)))
        assertEquals("in 48 min", formatCountdown(Duration.ofMinutes(48)))
        assertEquals("in 0 min 45 s", formatCountdown(Duration.ofSeconds(45)))
        assertEquals("in 1 hr 0 min", formatCountdown(Duration.ofHours(1)))
    }

    @Test
    fun heroDateLabelFormattingVerification() {
        fun resolveDateLabel(prayerDate: LocalDate, selectedDate: LocalDate?): String? {
            return prayerDate.takeIf { it != selectedDate }?.let {
                if (it == selectedDate?.plusDays(1)) "Tomorrow"
                else it.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.US))
            }
        }

        // Same day -> null (no date label needed)
        assertEquals(null, resolveDateLabel(todayDate, todayDate))
        // Tomorrow -> "Tomorrow"
        assertEquals("Tomorrow", resolveDateLabel(tomorrowDate, todayDate))
        // Two days later -> "23 Nov"
        assertEquals("23 Nov", resolveDateLabel(todayDate.plusDays(2), todayDate))
    }
}
