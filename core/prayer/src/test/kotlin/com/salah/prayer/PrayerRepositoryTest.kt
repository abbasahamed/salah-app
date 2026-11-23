package com.salah.prayer

import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PrayerRepositoryTest {
    private val now = Instant.parse("2026-11-21T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test fun freshCacheIsServedWithoutNetwork() = runTest {
        val cache = MemoryCache(CachedSchedule(zeroAdjustmentSchedule, now.minusSeconds(45)))
        var fetches = 0
        val repository = PrayerTimeRepository({ fetches++; error("unexpected") }, cache, AdhanOfflineProvider(), clock, backgroundScope)
        assertEquals(PrayerTimeSource.LOCAL_CACHE, repository.schedule(fixtureRequest).toList().single().source())
        assertEquals(0, fetches)
    }

    @Test fun staleMatchingCacheSurvivesFailedRefresh() = runTest {
        val cached = CachedSchedule(zeroAdjustmentSchedule, now.minusSeconds(90_000))
        val cache = MemoryCache(cached)
        val repository = PrayerTimeRepository({ throw IOException() }, cache, AdhanOfflineProvider(), clock, backgroundScope)
        assertEquals(PrayerTimeSource.LOCAL_CACHE, repository.schedule(fixtureRequest).toList().single().source())
        assertSame(cached, cache.read(fixtureRequest))
    }

    @Test fun oldDateIsNeverServedAndRetryBypassesCooldown() = runTest {
        val request = fixtureRequest.copy(date = fixtureRequest.date.plusDays(1))
        val cache = MemoryCache(CachedSchedule(zeroAdjustmentSchedule, now))
        var fetches = 0
        val repository = PrayerTimeRepository({
            if (++fetches == 1) throw IOException("offline")
            AdhanOfflineProvider().calculate(it)
        }, cache, AdhanOfflineProvider(), clock, backgroundScope)

        val fallback = repository.schedule(request).toList().single() as PrayerScheduleResult.Available
        assertEquals(PrayerTimeSource.OFFLINE_CALCULATION, fallback.source)
        assertEquals(request, fallback.schedule.request)
        assertEquals(PrayerTimeSource.OFFLINE_CALCULATION, repository.schedule(request).toList().single().source())
        assertEquals(1, fetches)
        assertEquals(PrayerTimeSource.ONLINE_API, repository.schedule(request, refresh = true).toList().single().source())
        assertEquals(2, fetches)
        assertEquals(request, cache.read(request)?.schedule?.request)
    }

    @Test fun concurrentCollectorsShareOneOnlineCall() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var fetches = 0
        val repository = PrayerTimeRepository({
            fetches++
            started.complete(Unit)
            release.await()
            zeroAdjustmentSchedule
        }, MemoryCache(), AdhanOfflineProvider(), clock, backgroundScope)
        val first = async { repository.schedule(fixtureRequest).toList() }
        started.await()
        val second = async { repository.schedule(fixtureRequest).toList() }
        runCurrent()
        release.complete(Unit)
        assertEquals(PrayerTimeSource.ONLINE_API, first.await().single().source())
        assertEquals(PrayerTimeSource.ONLINE_API, second.await().single().source())
        assertEquals(1, fetches)
    }

    @Test fun changingLocationCancelsObsoleteNetworkAndKeepsCacheIsolated() = runTest {
        val started = CompletableDeferred<Unit>()
        val obsoleteCancelled = CompletableDeferred<Unit>()
        val london = fixtureRequest.copy(location = PrayerLocation(51.51, -0.12, ZoneId.of("Europe/London")))
        val cache = MemoryCache()
        val repository = PrayerTimeRepository({ request ->
            if (request == fixtureRequest) {
                started.complete(Unit)
                try { awaitCancellation() } finally { obsoleteCancelled.complete(Unit) }
            }
            AdhanOfflineProvider().calculate(request)
        }, cache, AdhanOfflineProvider(), clock, backgroundScope)
        val obsolete = async { repository.schedule(fixtureRequest).toList() }
        started.await()
        obsolete.cancelAndJoin()
        obsoleteCancelled.await()
        val result = repository.schedule(london).toList().single() as PrayerScheduleResult.Available
        assertEquals(london, result.schedule.request)
        assertNull(cache.read(fixtureRequest))
        assertNotEquals(fixtureRequest.cacheKey(), london.cacheKey())
        assertEquals(london, cache.read(london)?.schedule?.request)
        repository.clearLocationData()
        assertNull(cache.read(london))
    }

    @Test fun offlineFollowingFajrUsesNewLocationsLocalDay() = runTest {
        val repository = PrayerTimeRepository({ throw IOException() }, MemoryCache(), AdhanOfflineProvider(), clock, backgroundScope)
        val selected = PrayerLocation(51.51, -0.12, ZoneId.of("Europe/London"))
        val afterIsha = Instant.parse("2026-07-01T23:30:00Z")
        val localDay = afterIsha.atZone(selected.zone).toLocalDate()
        val request = fixtureRequest.copy(location = selected, date = localDay)
        val previous = repository.localSchedule(request.copy(date = localDay.minusDays(1))) as PrayerScheduleResult.Available
        val current = repository.schedule(request).toList().single() as PrayerScheduleResult.Available
        val following = repository.schedule(request.copy(date = localDay.plusDays(1))).toList().single() as PrayerScheduleResult.Available
        val next = NextPrayerResolver(clock).resolve(listOf(previous.schedule, current.schedule, following.schedule), afterIsha)
        assertEquals(PrayerName.FAJR, next?.time?.name)
        assertEquals(localDay, next?.time?.instant?.atZone(selected.zone)?.toLocalDate())
        assertEquals(PrayerTimeSource.OFFLINE_CALCULATION, current.source)
    }

    @Test fun allSourcesFailExplicitly() = runTest {
        val repository = PrayerTimeRepository(
            { throw IOException() }, MemoryCache(), { error("No astronomical result") }, clock, backgroundScope,
        )
        assertEquals(PrayerScheduleResult.Unavailable(true), repository.schedule(fixtureRequest).toList().single())
    }

    private fun PrayerScheduleResult.source() = (this as PrayerScheduleResult.Available).source

    private class MemoryCache(initial: CachedSchedule? = null) : PrayerCache {
        private val rows = mutableMapOf<PrayerCalculationRequest, CachedSchedule>()
        init { initial?.let { rows[it.schedule.request] = it } }
        override suspend fun read(request: PrayerCalculationRequest): CachedSchedule? = rows[request]
        override suspend fun clear() = rows.clear()
        override suspend fun write(schedule: CachedSchedule) { rows[schedule.schedule.request] = schedule }
    }
}
