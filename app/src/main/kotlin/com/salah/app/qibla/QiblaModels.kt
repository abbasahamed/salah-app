package com.salah.app.qibla

import androidx.compose.runtime.Immutable

const val KAABA_LATITUDE = 21.422487
const val KAABA_LONGITUDE = 39.826206

const val ALIGNMENT_THRESHOLD_DEGREES = 3.0f
const val ALIGNMENT_EXIT_THRESHOLD_DEGREES = 5.0f

@Immutable
data class QiblaBearing(
    val bearingDegrees: Float,
    val distanceKm: Double,
    val isAtKaaba: Boolean,
)

enum class SensorAccuracy {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH,
}

sealed interface CompassState {
    data object NoSensor : CompassState
    data class Active(
        val headingDegrees: Float,
        val accuracy: SensorAccuracy,
    ) : CompassState
}

sealed interface QiblaLocationState {
    data object NoLocation : QiblaLocationState
    data class Available(
        val locationName: String,
        val isManual: Boolean,
        val bearing: QiblaBearing,
        val latitude: Double,
        val longitude: Double,
    ) : QiblaLocationState
}

@Immutable
data class QiblaUiState(
    val locationState: QiblaLocationState = QiblaLocationState.NoLocation,
    val compassState: CompassState = CompassState.NoSensor,
    val smoothedHeadingDegrees: Float = 0f,
    val relativeQiblaAngleDegrees: Float = 0f,
    val isFacingQibla: Boolean = false,
    val needsCalibration: Boolean = false,
)

