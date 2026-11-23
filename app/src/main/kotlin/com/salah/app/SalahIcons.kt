package com.salah.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.salah.prayer.PrayerName

internal object SalahIcons {
    val Today = glyph("Today") {
        moveTo(4f, 20f); lineTo(4f, 10f); lineTo(12f, 3f); lineTo(20f, 10f)
        lineTo(20f, 20f); close()
        moveTo(9f, 20f); lineTo(9f, 13f); lineTo(15f, 13f); lineTo(15f, 20f)
    }
    val Calendar = glyph("Calendar") {
        moveTo(6f, 5f); lineTo(18f, 5f); quadTo(20f, 5f, 20f, 7f)
        verticalLineTo(19f); quadTo(20f, 21f, 18f, 21f); horizontalLineTo(6f)
        quadTo(4f, 21f, 4f, 19f); verticalLineTo(7f); quadTo(4f, 5f, 6f, 5f)
        moveTo(8f, 3f); verticalLineTo(7f); moveTo(16f, 3f); verticalLineTo(7f)
        moveTo(4f, 10f); horizontalLineTo(20f)
    }
    val Qibla = glyph("Qibla") {
        moveTo(21f, 12f); arcTo(9f, 9f, 0f, true, true, 3f, 12f)
        arcTo(9f, 9f, 0f, true, true, 21f, 12f); close()
        moveTo(16f, 8f); lineTo(14f, 14f); lineTo(8f, 16f); lineTo(10f, 10f); close()
    }
    val Settings = glyph("Settings") {
        moveTo(4f, 6f); horizontalLineTo(14f); moveTo(17f, 6f); horizontalLineTo(20f)
        moveTo(15.5f, 4f); verticalLineTo(8f)
        moveTo(4f, 12f); horizontalLineTo(7f); moveTo(10f, 12f); horizontalLineTo(20f)
        moveTo(8.5f, 10f); verticalLineTo(14f)
        moveTo(4f, 18f); horizontalLineTo(13f); moveTo(16f, 18f); horizontalLineTo(20f)
        moveTo(14.5f, 16f); verticalLineTo(20f)
    }
    val Place = glyph("Location") {
        moveTo(12f, 21f); quadTo(5f, 14.5f, 5f, 10f)
        arcTo(7f, 7f, 0f, false, true, 19f, 10f); quadTo(19f, 14.5f, 12f, 21f)
        moveTo(14f, 10f); arcTo(2f, 2f, 0f, true, true, 10f, 10f)
        arcTo(2f, 2f, 0f, true, true, 14f, 10f)
    }
    val NearMe = glyph("NearMe") {
        moveTo(3f, 11f); lineTo(22f, 2f); lineTo(13f, 21f); lineTo(11f, 13f); close()
    }
    val Chevron = glyph("Expand") { moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f) }
    val ChevronLeft = glyph("ChevronLeft") { moveTo(15f, 7f); lineTo(10f, 12f); lineTo(15f, 17f) }
    val ChevronRight = glyph("ChevronRight") { moveTo(9f, 7f); lineTo(14f, 12f); lineTo(9f, 17f) }
    val Info = glyph("Info") {
        moveTo(21f, 12f); arcTo(9f, 9f, 0f, true, true, 3f, 12f)
        arcTo(9f, 9f, 0f, true, true, 21f, 12f); close()
        moveTo(12f, 8f); verticalLineTo(8.5f)
        moveTo(12f, 11.5f); verticalLineTo(16.5f)
    }
    val Minus = glyph("Decrease") { moveTo(7f, 12f); horizontalLineTo(17f) }
    val Plus = glyph("Increase") {
        moveTo(7f, 12f); horizontalLineTo(17f); moveTo(12f, 7f); verticalLineTo(17f)
    }

    private val Fajr = glyph("Fajr") {
        horizon(16f)
        moveTo(8.5f, 16f); arcTo(3.5f, 3.5f, 0f, false, true, 15.5f, 16f)
        moveTo(12f, 6.5f); verticalLineTo(9.5f)
        moveTo(6.5f, 9.5f); lineTo(8.5f, 11.5f); moveTo(17.5f, 9.5f); lineTo(15.5f, 11.5f)
    }
    private val Sunrise = glyph("Sunrise") {
        horizon(16f)
        moveTo(7.5f, 16f); arcTo(4.5f, 4.5f, 0f, false, true, 16.5f, 16f)
        moveTo(12f, 5f); verticalLineTo(8.5f)
        moveTo(6f, 8.5f); lineTo(8.2f, 10.7f); moveTo(18f, 8.5f); lineTo(15.8f, 10.7f)
        moveTo(3.5f, 13.5f); lineTo(5.5f, 14.5f); moveTo(20.5f, 13.5f); lineTo(18.5f, 14.5f)
    }
    private val Dhuhr = glyph("Dhuhr") {
        sun(12f, 12f)
        moveTo(12f, 3.2f); verticalLineTo(5.7f); moveTo(12f, 18.3f); verticalLineTo(20.8f)
        moveTo(3.2f, 12f); horizontalLineTo(5.7f); moveTo(18.3f, 12f); horizontalLineTo(20.8f)
        moveTo(5.8f, 5.8f); lineTo(7.6f, 7.6f); moveTo(16.4f, 16.4f); lineTo(18.2f, 18.2f)
        moveTo(18.2f, 5.8f); lineTo(16.4f, 7.6f); moveTo(7.6f, 16.4f); lineTo(5.8f, 18.2f)
    }
    private val Asr = glyph("Asr") {
        sun(8.5f, 8.5f)
        moveTo(8.5f, 3.2f); verticalLineTo(4.7f); moveTo(3.2f, 8.5f); horizontalLineTo(4.7f)
        moveTo(4.8f, 4.8f); lineTo(5.8f, 5.8f)
        moveTo(4f, 18f); horizontalLineTo(20f)
        moveTo(10f, 18f); verticalLineTo(12f)
        moveTo(10f, 18f); horizontalLineTo(17f)
    }
    private val Maghrib = glyph("Maghrib") {
        horizon(14f)
        moveTo(7.5f, 14f); arcTo(4.5f, 4.5f, 0f, false, true, 16.5f, 14f)
        moveTo(12f, 5.5f); verticalLineTo(8.5f)
        moveTo(7f, 17.5f); horizontalLineTo(17f)
        moveTo(9.5f, 20.5f); horizontalLineTo(14.5f)
    }
    private val Isha = glyph("Isha") {
        moveTo(15.5f, 3.5f)
        arcTo(8.5f, 8.5f, 0f, true, false, 20.5f, 15.5f)
        arcTo(8f, 8f, 0f, false, true, 15.5f, 3.5f)
        close()
        moveTo(8.5f, 6.5f); verticalLineTo(9.5f)
        moveTo(7f, 8f); horizontalLineTo(10f)
    }

    fun prayer(name: PrayerName) = when (name) {
        PrayerName.FAJR -> Fajr
        PrayerName.SUNRISE -> Sunrise
        PrayerName.DHUHR -> Dhuhr
        PrayerName.ASR -> Asr
        PrayerName.MAGHRIB -> Maghrib
        PrayerName.ISHA -> Isha
    }
}

private fun PathBuilder.horizon(y: Float) { moveTo(3f, y); horizontalLineTo(21f) }

private fun PathBuilder.sun(x: Float, y: Float) {
    moveTo(x + 3.8f, y)
    arcTo(3.8f, 3.8f, 0f, true, true, x - 3.8f, y)
    arcTo(3.8f, 3.8f, 0f, true, true, x + 3.8f, y)
}

private fun glyph(name: String, draw: PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = draw,
    ).build()
