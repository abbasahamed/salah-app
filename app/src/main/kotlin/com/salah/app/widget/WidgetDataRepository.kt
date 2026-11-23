package com.salah.app.widget

import android.content.Context
import com.salah.app.prayerLocation
import com.salah.location.LocationPreferences
import com.salah.location.SalahLocation
import com.salah.preferences.PrayerPreferences
import com.salah.prayer.DailyPrayerSchedule
import com.salah.prayer.NextPrayer
import com.salah.prayer.NextPrayerResolver
import com.salah.prayer.PrayerCalculationRequest
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerScheduleResult
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTime
import com.salah.prayer.PrayerTimeFormat
import com.salah.prayer.PrayerTimeRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

interface WidgetScheduleSource {
    suspend fun getSelectedLocation(): SalahLocation?
    suspend fun getPrayerSettings(): PrayerSettings
    suspend fun getSchedule(request: PrayerCalculationRequest): PrayerScheduleResult
}

class DefaultWidgetScheduleSource(
    private val locationPreferences: LocationPreferences,
    private val prayerPreferences: PrayerPreferences,
    private val prayerTimeRepository: PrayerTimeRepository,
) : WidgetScheduleSource {
    override suspend fun getSelectedLocation(): SalahLocation? = locationPreferences.selected.first()
    override suspend fun getPrayerSettings(): PrayerSettings = prayerPreferences.settings.first()
    override suspend fun getSchedule(request: PrayerCalculationRequest): PrayerScheduleResult =
        prayerTimeRepository.localSchedule(request)
}

class WidgetDataRepository(
    private val source: WidgetScheduleSource,
    private val clock: Clock,
) {
    constructor(context: Context, clock: Clock = Clock.systemUTC()) : this(
        source = DefaultWidgetScheduleSource(
            locationPreferences = LocationPreferences(context),
            prayerPreferences = PrayerPreferences(context),
            prayerTimeRepository = PrayerTimeRepository(context, clock),
        ),
        clock = clock,
    )

    private val resolver = NextPrayerResolver(clock)

    suspend fun loadSnapshot(): WidgetSnapshot {
        val location: SalahLocation? = source.getSelectedLocation()
        if (location == null) {
            return WidgetSnapshot(hasLocation = false)
        }

        val settings: PrayerSettings = source.getPrayerSettings()
        val site = location.prayerLocation()
        val now = clock.instant()
        val zone = location.zone
        val todayDate = now.atZone(zone).toLocalDate()

        val todayReq = PrayerCalculationRequest(
            location = site,
            date = todayDate,
            method = settings.method,
            asr = settings.asr,
            adjustments = settings.adjustments,
        )
        val todayResult = source.getSchedule(todayReq)
        val todaySchedule = (todayResult as? PrayerScheduleResult.Available)?.schedule

        if (todaySchedule == null) {
            val locality = location.names.locality ?: location.names.region ?: location.names.country ?: "Salah"
            return WidgetSnapshot(
                hasLocation = true,
                localityName = locality,
                isAvailable = false,
            )
        }

        val tomorrowDate = todayDate.plusDays(1)
        val tomorrowReq = PrayerCalculationRequest(
            location = site,
            date = tomorrowDate,
            method = settings.method,
            asr = settings.asr,
            adjustments = settings.adjustments,
        )
        val tomorrowResult = source.getSchedule(tomorrowReq)
        val tomorrowSchedule = (tomorrowResult as? PrayerScheduleResult.Available)?.schedule

        val yesterdayDate = todayDate.minusDays(1)
        val yesterdayReq = PrayerCalculationRequest(
            location = site,
            date = yesterdayDate,
            method = settings.method,
            asr = settings.asr,
            adjustments = settings.adjustments,
        )
        val yesterdayResult = source.getSchedule(yesterdayReq)
        val yesterdaySchedule = (yesterdayResult as? PrayerScheduleResult.Available)?.schedule

        val candidates = listOfNotNull(yesterdaySchedule, todaySchedule, tomorrowSchedule)
        val nextPrayer = resolver.resolve(candidates, now)
        val precedingPrayer = candidates.flatMap { it.timings }
            .filter { it.name != PrayerName.SUNRISE && it.instant <= now }
            .maxByOrNull { it.instant }

        val locale = Locale.getDefault()
        val is12Hour = settings.format == PrayerTimeFormat.TWELVE_HOUR
        val timeDigitsFormatter = DateTimeFormatter.ofPattern(if (is12Hour) "h:mm" else "HH:mm", locale)
        val periodFormatter = DateTimeFormatter.ofPattern("a", locale)

        val locality = location.names.locality ?: location.names.region ?: location.names.country ?: "Salah"

        if (nextPrayer == null) {
            return WidgetSnapshot(
                hasLocation = true,
                localityName = locality,
                isAvailable = false,
            )
        }

        val nextZoned = nextPrayer.time.instant.atZone(zone)
        val isTomorrow = nextZoned.toLocalDate() != todayDate
        val nextTimeFormatted = nextZoned.format(timeDigitsFormatter)
        val nextPeriod = if (is12Hour) nextZoned.format(periodFormatter) else null

        // Remaining countdown calculation
        val remainingSeconds = nextPrayer.remaining.seconds + if (nextPrayer.remaining.nano > 0) 1 else 0
        val remainingHours = (remainingSeconds / 3600).toInt()
        val remainingMinutes = (remainingSeconds / 60 % 60).toInt()
        val countdownText = when {
            remainingHours > 0 -> "in $remainingHours hr $remainingMinutes min"
            remainingMinutes > 1 -> "in $remainingMinutes min"
            remainingMinutes == 1 -> "in 1 min"
            else -> "in < 1 min"
        }
        val countdownCompactText = when {
            remainingHours > 0 -> "${remainingHours}h ${remainingMinutes}m"
            remainingMinutes > 1 -> "${remainingMinutes}m"
            remainingMinutes == 1 -> "1m"
            else -> "< 1m"
        }

        // Progress calculation
        val interval = precedingPrayer?.let { Duration.between(it.instant, nextPrayer.time.instant) }
        val progressFraction = interval?.takeIf { !it.isZero && !it.isNegative && nextPrayer.remaining <= it }?.let {
            val totalSec = it.seconds.toDouble() + it.nano / 1_000_000_000.0
            val remSec = nextPrayer.remaining.seconds.toDouble() + nextPrayer.remaining.nano / 1_000_000_000.0
            (1.0 - remSec / totalSec).toFloat().coerceIn(0.01f, 1f)
        }

        // Timeline items
        // Filter strictly to the 5 obligatory prayers: Fajr, Dhuhr, Asr, Maghrib, Isha
        val timelineSchedule = if (isTomorrow && tomorrowSchedule != null) tomorrowSchedule else todaySchedule
        val isTomorrowTimeline = isTomorrow && tomorrowSchedule != null && timelineSchedule == tomorrowSchedule
        val timelineItems = timelineSchedule.timings
            .filter { it.name != PrayerName.SUNRISE }
            .map { timing ->
                val zonedTiming = timing.instant.atZone(zone)
                val isNext = timing.name == nextPrayer.time.name && timing.instant == nextPrayer.time.instant
                val isPassed = if (isTomorrowTimeline) false else timing.instant <= now
                WidgetTimelineItem(
                    prayer = timing.name,
                    label = timing.name.displayName(),
                    timeFormatted = zonedTiming.format(timeDigitsFormatter),
                    period = if (is12Hour) zonedTiming.format(periodFormatter) else null,
                    isNext = isNext,
                    isSunrise = false,
                    isPassed = isPassed,
                    instant = timing.instant,
                )
            }

        return WidgetSnapshot(
            hasLocation = true,
            localityName = locality,
            nextPrayerName = nextPrayer.time.name.displayName(),
            nextPrayerTimeFormatted = nextTimeFormatted,
            nextPrayerPeriod = nextPeriod,
            countdownText = countdownText,
            countdownCompactText = countdownCompactText,
            progressFraction = progressFraction,
            isTomorrow = isTomorrow,
            timeline = timelineItems,
            isAvailable = true,
        )
    }

    private fun PrayerName.displayName(): String = when (this) {
        PrayerName.FAJR -> "Fajr"
        PrayerName.SUNRISE -> "Sunrise"
        PrayerName.DHUHR -> "Dhuhr"
        PrayerName.ASR -> "Asr"
        PrayerName.MAGHRIB -> "Maghrib"
        PrayerName.ISHA -> "Isha"
    }
}
