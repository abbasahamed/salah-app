package com.salah.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class PrayerName(val apiName: String) {
    FAJR("Fajr"), SUNRISE("Sunrise"), DHUHR("Dhuhr"), ASR("Asr"),
    MAGHRIB("Maghrib"), ISHA("Isha"),
}

enum class PrayerCalculationMethod(val alAdhanId: Int) {
    KARACHI(1), ISNA(2), MUSLIM_WORLD_LEAGUE(3), UMM_AL_QURA(4),
}

enum class AsrJuristicMethod(val alAdhanId: Int) {
    STANDARD(0), HANAFI(1),
}

data class PrayerLocation(val latitude: Double, val longitude: Double, val zone: ZoneId) {
    init {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        require(zone.id in ZoneId.getAvailableZoneIds())
    }
}

data class PrayerTimeAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    init { require(PrayerName.entries.all { get(it) in -60..60 }) }

    operator fun get(prayer: PrayerName): Int = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.SUNRISE -> sunrise
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
    }
}

enum class PrayerTimeFormat { TWELVE_HOUR, TWENTY_FOUR_HOUR }

data class PrayerSettings(
    val method: PrayerCalculationMethod = PrayerCalculationMethod.KARACHI,
    val asr: AsrJuristicMethod = AsrJuristicMethod.STANDARD,
    val adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
    val format: PrayerTimeFormat = PrayerTimeFormat.TWENTY_FOUR_HOUR,
)

data class PrayerCalculationRequest(
    val location: PrayerLocation,
    val date: LocalDate,
    val method: PrayerCalculationMethod,
    val asr: AsrJuristicMethod,
    val adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
) {
    init { require(date.year in 1900..9999) }
}

data class PrayerTime(val name: PrayerName, val instant: Instant)

class DailyPrayerSchedule(
    val request: PrayerCalculationRequest,
    rawTimings: List<Instant>,
) {
    val rawTimings: List<Instant> = rawTimings.toList()
    val timings: List<PrayerTime>

    init {
        val zone = request.location.zone
        require(this.rawTimings.size == PrayerName.entries.size)
        require(this.rawTimings.zipWithNext().all { (a, b) -> a < b })
        require(this.rawTimings.dropLast(1).all { it.atZone(zone).toLocalDate() == request.date })
        require(this.rawTimings.last().atZone(zone).toLocalDate() in request.date..request.date.plusDays(1))
        require(this.rawTimings.last() < this.rawTimings.first().plusSeconds(86_400))
        timings = PrayerName.entries.zip(this.rawTimings) { prayer, instant ->
            PrayerTime(prayer, instant.plusSeconds(request.adjustments[prayer] * 60L))
        }
    }
}

enum class PrayerTimeSource { ONLINE_API, LOCAL_CACHE, OFFLINE_CALCULATION }

sealed interface PrayerScheduleResult {
    data class Available(val schedule: DailyPrayerSchedule, val source: PrayerTimeSource) : PrayerScheduleResult
    data class Unavailable(val retryable: Boolean) : PrayerScheduleResult
}

internal fun interface OfflinePrayerProvider {
    fun calculate(request: PrayerCalculationRequest): DailyPrayerSchedule
}

internal fun interface OnlinePrayerProvider {
    suspend fun fetch(request: PrayerCalculationRequest): DailyPrayerSchedule
}
