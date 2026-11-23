package com.salah.prayer

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import org.junit.Assert.*
import org.junit.Test

internal val fixtureRequest = PrayerCalculationRequest(
    PrayerLocation(12.820254, 80.037819, ZoneId.of("Asia/Kolkata")),
    LocalDate.of(2026, 11, 21), PrayerCalculationMethod.KARACHI, AsrJuristicMethod.STANDARD,
)
internal val zeroAdjustmentSchedule = AdhanOfflineProvider().calculate(fixtureRequest)

class PrayerDomainTest {
    @Test fun coordinatesAndTimezonesValidate() {
        val zone = fixtureRequest.location.zone
        PrayerLocation(-90.0, 180.0, zone)
        PrayerLocation(90.0, -180.0, zone)
        assertThrows(IllegalArgumentException::class.java) { PrayerLocation(Double.NaN, 0.0, zone) }
        assertThrows(IllegalArgumentException::class.java) { PrayerLocation(90.01, 0.0, zone) }
        assertThrows(IllegalArgumentException::class.java) { PrayerLocation(0.0, -181.0, zone) }
        assertThrows(IllegalArgumentException::class.java) { PrayerLocation(0.0, 0.0, ZoneOffset.UTC) }
        assertThrows(IllegalArgumentException::class.java) {
            fixtureRequest.copy(date = LocalDate.of(1899, 1, 1))
        }
    }

    @Test fun cacheKeyIncludesEveryCalculationInput() {
        val request = fixtureRequest
        val settings = listOf(
            request, request.copy(date = request.date.plusDays(1)),
            request.copy(location = request.location.copy(latitude = 12.82)),
            request.copy(location = request.location.copy(longitude = 80.03)),
            request.copy(location = request.location.copy(zone = ZoneId.of("Asia/Colombo"))),
            request.copy(method = PrayerCalculationMethod.ISNA),
            request.copy(asr = AsrJuristicMethod.HANAFI),
            request.copy(adjustments = PrayerTimeAdjustments(sunrise = 1)),
        )
        assertEquals(settings.size, settings.map { it.cacheKey() }.toSet().size)
    }

    @Test fun adjustmentsDoNotChangeProviderRawTimes() {
        assertThrows(IllegalArgumentException::class.java) { PrayerTimeAdjustments(isha = 61) }
        val adjusted = AdhanOfflineProvider().calculate(
            fixtureRequest.copy(adjustments = PrayerTimeAdjustments(fajr = -3, isha = 4)),
        )
        assertEquals(zeroAdjustmentSchedule.rawTimings, adjusted.rawTimings)
        assertEquals(adjusted.rawTimings.first().minusSeconds(180), adjusted.timings.first().instant)
        assertEquals(adjusted.rawTimings.last().plusSeconds(240), adjusted.timings.last().instant)
    }

    @Test fun boundarySkipsSunriseAndSelectsTomorrowAfterIsha() {
        val today = zeroAdjustmentSchedule
        val tomorrow = AdhanOfflineProvider().calculate(fixtureRequest.copy(date = fixtureRequest.date.plusDays(1)))
        val fajr = today.timings.first().instant
        val isha = today.timings.last().instant
        val clock = Clock.fixed(fajr.minusNanos(1), ZoneOffset.UTC)
        val resolver = NextPrayerResolver(clock)
        assertEquals(Duration.ofNanos(1), resolver.resolve(listOf(today))?.remaining)
        assertEquals(PrayerName.DHUHR, resolver.resolve(listOf(today), fajr)?.time?.name)
        assertNull(resolver.resolve(listOf(today), isha))
        assertEquals(tomorrow.timings.first(), resolver.resolve(listOf(today, tomorrow), isha)?.time)
        assertEquals(tomorrow.timings.first(), resolver.resolve(
            listOf(today, tomorrow), tomorrow.request.date.atStartOfDay(fixtureRequest.location.zone).toInstant(),
        )?.time)
    }

    @Test fun adjacentDatesCanOverlapAfterMinuteAdjustments() {
        val zone = fixtureRequest.location.zone
        val midnight = fixtureRequest.date.plusDays(1).atStartOfDay(zone).toInstant()
        val todayRaw = zeroAdjustmentSchedule.rawTimings.dropLast(1) + midnight.minusSeconds(900)
        val request = fixtureRequest.copy(adjustments = PrayerTimeAdjustments(isha = 30, fajr = -60))
        val today = DailyPrayerSchedule(request, todayRaw)
        val following = request.copy(date = request.date.plusDays(1))
        val nextRaw = AdhanOfflineProvider().calculate(following).rawTimings
        val tomorrow = DailyPrayerSchedule(following, listOf(midnight.plusSeconds(1_800)) + nextRaw.drop(1))
        val resolver = NextPrayerResolver(Clock.fixed(midnight.minusSeconds(2_000), zone))

        assertEquals(tomorrow.timings.first(), resolver.resolve(listOf(today, tomorrow))?.time)
        assertEquals(today.timings.last(), resolver.resolve(listOf(today, tomorrow), midnight)?.time)
    }

    @Test fun dstAndTimezoneUseElapsedInstantTime() {
        val ny = ZoneId.of("America/New_York")
        val now = Instant.parse("2026-03-08T06:30:00Z")
        val request = fixtureRequest.copy(
            date = LocalDate.of(2026, 3, 8), location = PrayerLocation(40.7128, -74.006, ny),
            method = PrayerCalculationMethod.ISNA,
        )
        val schedule = AdhanOfflineProvider().calculate(request)
        val next = NextPrayerResolver(Clock.fixed(now, ZoneOffset.UTC)).resolve(listOf(schedule))!!
        assertEquals(PrayerName.FAJR, next.time.name)
        assertEquals(ZoneOffset.ofHours(-5), ny.rules.getOffset(now))
        assertEquals(ZoneOffset.ofHours(-4), ny.rules.getOffset(next.time.instant))
        val apparent = Duration.between(now.atZone(ny).toLocalTime(), next.time.instant.atZone(ny).toLocalTime())
        assertEquals(apparent.minusHours(1), next.remaining)
    }

    @Test fun bothAsrSchoolsAndAllApiMethodIdsAreExplicit() {
        assertEquals(listOf(1, 2, 3, 4), PrayerCalculationMethod.entries.map { it.alAdhanId })
        assertEquals(listOf(0, 1), AsrJuristicMethod.entries.map { it.alAdhanId })
        PrayerCalculationMethod.entries.forEach { method ->
            val standard = AdhanOfflineProvider().calculate(fixtureRequest.copy(method = method))
            val hanafi = AdhanOfflineProvider().calculate(fixtureRequest.copy(method = method, asr = AsrJuristicMethod.HANAFI))
            assertTrue(standard.timings[3].instant < hanafi.timings[3].instant)
        }
    }

    @Test fun independentPrayTimeFixtures() {
        val rows = listOf(
            Triple("Kattankulathur", fixtureRequest, "04:57,06:11,11:56,15:14,17:40,18:55"),
            Triple("Makkah", request("2026-11-01", 21.4225, 39.8262, "Asia/Riyadh", PrayerCalculationMethod.UMM_AL_QURA),
                "05:06,06:24,12:04,15:20,17:45,19:15"),
            Triple("London", request("2026-06-21", 51.5074, -0.1278, "Europe/London", PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
                "02:31,04:43,13:02,17:25,21:22,23:27"),
            Triple("New York", request("2026-03-08", 40.7128, -74.006, "America/New_York", PrayerCalculationMethod.ISNA),
                "06:04,07:19,13:07,16:22,18:55,20:10"),
        )
        for ((city, request, reference) in rows) {
            val actual = AdhanOfflineProvider().calculate(request).rawTimings.map {
                it.atZone(request.location.zone).toLocalTime()
            }
            val deltas = reference.split(',').map(LocalTime::parse).zip(actual) { expect, got ->
                ChronoUnit.MINUTES.between(expect, got)
            }
            println("$city: ${actual.joinToString()} deltas $deltas")
            assertTrue("$city deltas: $deltas", deltas.all { it in -2..2 })
        }
    }

    private fun request(date: String, lat: Double, lon: Double, zone: String, method: PrayerCalculationMethod) =
        PrayerCalculationRequest(PrayerLocation(lat, lon, ZoneId.of(zone)), LocalDate.parse(date), method, AsrJuristicMethod.STANDARD)
}
