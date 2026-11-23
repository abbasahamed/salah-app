package com.salah.prayer

import java.time.Clock
import java.time.Duration
import java.time.Instant

data class NextPrayer(val time: PrayerTime, val remaining: Duration)

class NextPrayerResolver(private val clock: Clock) {
    fun resolve(
        schedules: List<DailyPrayerSchedule>,
        now: Instant = clock.instant(),
    ): NextPrayer? = schedules.asSequence()
        .flatMap { it.timings.asSequence() }
        .filter { it.name != PrayerName.SUNRISE && it.instant > now }
        .minByOrNull { it.instant }
        ?.let { NextPrayer(it, Duration.between(now, it.instant)) }
}
