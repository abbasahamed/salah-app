package com.salah.prayer

import android.content.Context
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PrayerTimeRepository internal constructor(
    private val online: OnlinePrayerProvider,
    private val cache: PrayerCache,
    private val offline: OfflinePrayerProvider,
    private val clock: Clock,
    private val worker: CoroutineScope,
) {
    constructor(context: Context, clock: Clock) : this(
        AlAdhanProvider(), RoomPrayerCache(context), AdhanOfflineProvider(), clock,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    private data class Attempt(val result: PrayerScheduleResult?, val at: Instant)
    private val lock = Mutex()
    private class Refresh(val task: Deferred<Attempt>, var subscribers: Int = 1)
    private val pending = mutableMapOf<PrayerCalculationRequest, Refresh>()
    private val recent = LinkedHashMap<PrayerCalculationRequest, Attempt>()
    private val offlineSchedules = LinkedHashMap<PrayerCalculationRequest, PrayerScheduleResult>()

    fun schedule(request: PrayerCalculationRequest, refresh: Boolean = false): Flow<PrayerScheduleResult> = flow {
        val cached = recover { cache.read(request) }
        if (cached != null) {
            emit(PrayerScheduleResult.Available(cached.schedule, PrayerTimeSource.LOCAL_CACHE))
            val age = Duration.between(cached.retrieved, clock.instant())
            if (!refresh && !age.isNegative && age < Duration.ofHours(24)) return@flow
        }

        val attempt = refreshOnline(request, refresh)
        if (attempt.result != null) {
            emit(attempt.result)
        } else if (cached == null) {
            emit(loadOffline(request))
        }
    }

    suspend fun localSchedule(request: PrayerCalculationRequest): PrayerScheduleResult {
        val cached = recover { cache.read(request) }
        return cached?.let { PrayerScheduleResult.Available(it.schedule, PrayerTimeSource.LOCAL_CACHE) }
            ?: loadOffline(request)
    }

    private suspend fun loadOffline(request: PrayerCalculationRequest): PrayerScheduleResult = lock.withLock {
        offlineSchedules[request]?.let { return it }
        val schedule = recover {
            withContext(Dispatchers.Default) { offline.calculate(request) }
        }
        val result = schedule?.let { PrayerScheduleResult.Available(it, PrayerTimeSource.OFFLINE_CALCULATION) }
            ?: PrayerScheduleResult.Unavailable(retryable = true)
        if (result is PrayerScheduleResult.Available) offlineSchedules[request] = result
        if (offlineSchedules.size > 16) offlineSchedules.remove(offlineSchedules.keys.first())
        result
    }

    private suspend fun refreshOnline(request: PrayerCalculationRequest, force: Boolean): Attempt {
        val refresh = lock.withLock {
            pending[request]?.let {
                it.subscribers++
                return@withLock it
            }
            val previous = recent[request]
            if (!force && previous != null) {
                val ttl = if (previous.result != null) Duration.ofHours(24) else Duration.ofMinutes(5)
                val age = Duration.between(previous.at, clock.instant())
                if (!age.isNegative && age < ttl) return previous
            }
            val task = worker.async(start = CoroutineStart.LAZY) {
                val schedule = recover { online.fetch(request) }
                val now = clock.instant()
                lock.withLock {
                    val result = schedule?.let {
                        recover { cache.write(CachedSchedule(it, now)) }
                        PrayerScheduleResult.Available(it, PrayerTimeSource.ONLINE_API)
                    }
                    Attempt(result, now).also {
                        recent[request] = it
                        if (recent.size > 16) recent.remove(recent.keys.first())
                    }
                }
            }
            Refresh(task).also { pending[request] = it }
        }
        return try {
            refresh.task.await()
        } finally {
            withContext(NonCancellable) {
                lock.withLock {
                    refresh.subscribers--
                    if (refresh.subscribers == 0 && pending[request] === refresh) {
                        pending.remove(request)
                        refresh.task.cancel()
                    }
                }
            }
        }
    }

    suspend fun clearLocationData() = lock.withLock {
        pending.values.forEach { it.task.cancel() }
        pending.clear()
        recent.clear()
        offlineSchedules.clear()
        cache.clear()
    }

    private suspend fun <T> recover(operation: suspend () -> T): T? = try {
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
