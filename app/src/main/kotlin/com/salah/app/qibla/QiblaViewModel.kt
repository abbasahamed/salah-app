package com.salah.app.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.location.LocationMode
import com.salah.location.SalahLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val repository: QiblaRepository,
    private val orientationProvider: DeviceOrientationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(QiblaUiState())
    val state: StateFlow<QiblaUiState> = _state.asStateFlow()

    private var currentFacingState = false
    private var smoothedHeading = 0f
    private var currentLocation: SalahLocation? = null
    private var orientationJob: kotlinx.coroutines.Job? = null

    init {
        // Observe Location
        viewModelScope.launch {
            repository.selectedLocation.collectLatest { location ->
                currentLocation = location
                updateLocation(location)
            }
        }

        // Start listening to orientation updates
        startOrientationUpdates()
    }

    fun startOrientationUpdates() {
        if (orientationJob?.isActive == true) return
        orientationJob = viewModelScope.launch {
            orientationProvider.orientationEvents().collectLatest { event ->
                handleOrientationEvent(event)
            }
        }
    }

    fun stopOrientationUpdates() {
        orientationJob?.cancel()
        orientationJob = null
    }

    private fun updateLocation(location: SalahLocation?) {
        if (location == null) {
            _state.value = _state.value.copy(
                locationState = QiblaLocationState.NoLocation,
                relativeQiblaAngleDegrees = 0f,
                isFacingQibla = false,
            )
            return
        }

        val bearing = QiblaBearingCalculator.calculate(
            location.coordinates.latitude,
            location.coordinates.longitude,
        )

        val name = location.names.displayName
            ?: location.names.qualifier.ifBlank { "Selected location" }

        val isManual = location.mode == LocationMode.MANUAL

        val locState = QiblaLocationState.Available(
            locationName = name,
            isManual = isManual,
            bearing = bearing,
            latitude = location.coordinates.latitude,
            longitude = location.coordinates.longitude,
        )

        val relAngle = if (_state.value.compassState is CompassState.Active) {
            QiblaOrientationMath.computeRelativeQiblaAngle(bearing.bearingDegrees, smoothedHeading)
        } else {
            bearing.bearingDegrees
        }

        val facing = if (_state.value.compassState is CompassState.Active && !_state.value.needsCalibration) {
            QiblaOrientationMath.checkFacingQibla(relAngle, currentFacingState)
        } else {
            false
        }
        currentFacingState = facing

        _state.value = _state.value.copy(
            locationState = locState,
            relativeQiblaAngleDegrees = relAngle,
            isFacingQibla = facing,
        )
    }

    private fun handleOrientationEvent(event: DeviceOrientationEvent) {
        val currentLoc = currentLocation
        when (event) {
            is DeviceOrientationEvent.SensorUnavailable -> {
                val locState = _state.value.locationState
                val bearing = (locState as? QiblaLocationState.Available)?.bearing?.bearingDegrees ?: 0f
                _state.value = _state.value.copy(
                    compassState = CompassState.NoSensor,
                    smoothedHeadingDegrees = 0f,
                    relativeQiblaAngleDegrees = bearing,
                    isFacingQibla = false,
                    needsCalibration = false,
                )
            }
            is DeviceOrientationEvent.OrientationUpdate -> {
                val declination = if (currentLoc != null) {
                    getDeclination(currentLoc.coordinates.latitude, currentLoc.coordinates.longitude)
                } else {
                    0f
                }

                val trueHeading = QiblaOrientationMath.magneticToTrueHeading(
                    event.magneticHeadingDegrees,
                    declination,
                )

                smoothedHeading = QiblaOrientationMath.smoothAngle(smoothedHeading, trueHeading, alpha = 0.25f)

                val bearingDeg = (_state.value.locationState as? QiblaLocationState.Available)
                    ?.bearing?.bearingDegrees ?: 0f

                val relAngle = QiblaOrientationMath.computeRelativeQiblaAngle(bearingDeg, smoothedHeading)

                val needsCalib = event.accuracy == SensorAccuracy.UNRELIABLE ||
                    event.accuracy == SensorAccuracy.LOW

                val facing = if (!needsCalib) {
                    QiblaOrientationMath.checkFacingQibla(relAngle, currentFacingState)
                } else {
                    false
                }
                currentFacingState = facing

                _state.value = _state.value.copy(
                    compassState = CompassState.Active(
                        headingDegrees = trueHeading,
                        accuracy = event.accuracy,
                    ),
                    smoothedHeadingDegrees = smoothedHeading,
                    relativeQiblaAngleDegrees = relAngle,
                    isFacingQibla = facing,
                    needsCalibration = needsCalib,
                )
            }
        }
    }

    private fun getDeclination(latitude: Double, longitude: Double): Float {
        return try {
            val geomagneticField = android.hardware.GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                0f,
                System.currentTimeMillis(),
            )
            geomagneticField.declination
        } catch (_: Throwable) {
            0f
        }
    }
}
