package com.salah.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GoogleReferenceComparison {
    @Test
    fun compare() = runBlocking {
        val fixture = PrayerLocation(12.820254, 80.037819, ZoneId.of("Asia/Kolkata"))
        val date = LocalDate.of(2026, 11, 21)
        val reference = listOf("04:56", "06:11", "11:55", "15:14", "17:40", "18:55", "04:57")
            .map(LocalTime::parse)
        val api = AlAdhanProvider()
        val offline = AdhanOfflineProvider()
        for (school in AsrJuristicMethod.entries) {
            val request = PrayerCalculationRequest(fixture, date, PrayerCalculationMethod.KARACHI, school)
            val following = request.copy(date = date.plusDays(1))
            val onlineToday = runCatching { api.fetch(request) }.getOrNull()
            val onlineTomorrow = runCatching { api.fetch(following) }.getOrNull()
            val offlineToday = offline.calculate(request)
            val offlineTomorrow = offline.calculate(following)
            println(school)
            println("Prayer | Google | API | Offline | API delta | Offline delta")
            for (index in reference.indices) {
                val tomorrow = index == 6
                val apiTime = (if (tomorrow) onlineTomorrow else onlineToday)
                    ?.timings?.get(if (tomorrow) 0 else index)?.instant?.atZone(fixture.zone)?.toLocalTime()
                val offlineTime = (if (tomorrow) offlineTomorrow.timings.first() else offlineToday.timings[index])
                    .instant.atZone(fixture.zone).toLocalTime()
                val name = if (tomorrow) "Next Fajr" else PrayerName.entries[index].apiName
                val expected = reference[index]
                println("$name | $expected | ${apiTime ?: "Unverified"} | $offlineTime | " +
                    "${apiTime?.let { ChronoUnit.MINUTES.between(expected, it) } ?: "—"} | " +
                    "${ChronoUnit.MINUTES.between(expected, offlineTime)}")
            }
        }
    }
}
