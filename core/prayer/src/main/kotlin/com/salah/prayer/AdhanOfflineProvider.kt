package com.salah.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.Instant

internal class AdhanOfflineProvider : OfflinePrayerProvider {
    override fun calculate(request: PrayerCalculationRequest): DailyPrayerSchedule {
        val method = when (request.method) {
            PrayerCalculationMethod.KARACHI -> CalculationMethod.KARACHI
            PrayerCalculationMethod.ISNA -> CalculationMethod.NORTH_AMERICA
            PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
            PrayerCalculationMethod.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        }
        val parameters = method.parameters.copy(
            madhab = when (request.asr) {
                AsrJuristicMethod.STANDARD -> Madhab.SHAFI
                AsrJuristicMethod.HANAFI -> Madhab.HANAFI
            },
            highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE,
        )
        val (latitude, longitude) = request.location
        val date = request.date
        val times = PrayerTimes(
            Coordinates(latitude, longitude),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters,
        )
        return DailyPrayerSchedule(
            request,
            listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)
                .map { Instant.ofEpochSecond(it.epochSeconds, it.nanosecondsOfSecond.toLong()) },
        )
    }
}
