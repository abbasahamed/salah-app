package com.salah.app.widget

import com.salah.prayer.PrayerName
import java.time.Instant

data class WidgetTimelineItem(
    val prayer: PrayerName,
    val label: String,
    val timeFormatted: String,
    val period: String? = null,
    val isNext: Boolean = false,
    val isSunrise: Boolean = false,
    val isPassed: Boolean = false,
    val instant: Instant? = null,
)

data class WidgetSnapshot(
    val hasLocation: Boolean,
    val localityName: String? = null,
    val nextPrayerName: String? = null,
    val nextPrayerTimeFormatted: String? = null,
    val nextPrayerPeriod: String? = null,
    val countdownText: String? = null,
    val countdownCompactText: String? = null,
    val progressFraction: Float? = null,
    val isTomorrow: Boolean = false,
    val timeline: List<WidgetTimelineItem> = emptyList(),
    val isAvailable: Boolean = true,
) {
    val heroContextLine: String
        get() {
            val name = nextPrayerName.orEmpty()
            val time = nextPrayerTimeFormatted.orEmpty() + if (!nextPrayerPeriod.isNullOrEmpty()) " $nextPrayerPeriod" else ""
            return if (isTomorrow) {
                "Until $name · $time tomorrow"
            } else {
                "Until $name · $time"
            }
        }
}
