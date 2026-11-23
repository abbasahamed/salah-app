package com.salah.app.qibla

import com.salah.location.LocationCoordinates
import com.salah.location.LocationMode
import com.salah.location.PlaceNames
import com.salah.location.SalahLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val fakeLocationFlow = MutableStateFlow<SalahLocation?>(null)
    private val fakeOrientationFlow = MutableSharedFlow<DeviceOrientationEvent>(replay = 1)

    private val fakeRepository = object : QiblaRepository {
        override val selectedLocation: Flow<SalahLocation?> = fakeLocationFlow
    }

    private val fakeOrientationProvider = object : DeviceOrientationProvider {
        override fun orientationEvents(): Flow<DeviceOrientationEvent> = fakeOrientationFlow
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateWithNoLocationAndNoSensor() = testScope.runTest {
        val vm = QiblaViewModel(fakeRepository, fakeOrientationProvider)
        fakeOrientationFlow.emit(DeviceOrientationEvent.SensorUnavailable)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(QiblaLocationState.NoLocation, state.locationState)
        assertEquals(CompassState.NoSensor, state.compassState)
        assertFalse(state.isFacingQibla)
    }

    @Test
    fun testLocationUpdateComputesBearing() = testScope.runTest {
        val vm = QiblaViewModel(fakeRepository, fakeOrientationProvider)
        fakeOrientationFlow.emit(DeviceOrientationEvent.SensorUnavailable)

        val dubaiLocation = SalahLocation(
            id = "dubai",
            coordinates = LocationCoordinates(25.2048, 55.2708),
            names = PlaceNames(locality = "Dubai", country = "United Arab Emirates"),
            zone = ZoneId.of("Asia/Dubai"),
            mode = LocationMode.MANUAL,
        )
        fakeLocationFlow.value = dubaiLocation
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.locationState is QiblaLocationState.Available)
        val available = state.locationState as QiblaLocationState.Available
        assertEquals("Dubai", available.locationName)
        assertTrue(available.isManual)
        assertEquals(258.2f, available.bearing.bearingDegrees, 0.5f)
        assertEquals(CompassState.NoSensor, state.compassState)
    }

    @Test
    fun testFacingQiblaTransitionsWithOrientationUpdate() = testScope.runTest {
        val vm = QiblaViewModel(fakeRepository, fakeOrientationProvider)

        val dubaiLocation = SalahLocation(
            id = "dubai",
            coordinates = LocationCoordinates(25.2048, 55.2708),
            names = PlaceNames(locality = "Dubai", country = "United Arab Emirates"),
            zone = ZoneId.of("Asia/Dubai"),
            mode = LocationMode.MANUAL,
        )
        fakeLocationFlow.value = dubaiLocation
        advanceUntilIdle()

        val available = vm.state.value.locationState as QiblaLocationState.Available
        val bearing = available.bearing.bearingDegrees

        // Orientation far from Qibla (bearing - 50°)
        fakeOrientationFlow.emit(
            DeviceOrientationEvent.OrientationUpdate(
                magneticHeadingDegrees = bearing - 50f,
                accuracy = SensorAccuracy.HIGH,
            )
        )
        advanceUntilIdle()
        assertFalse(vm.state.value.isFacingQibla)

        // Orientation aligned with Qibla (within 1°)
        // Repeating multiple updates to satisfy smoothing filter
        repeat(15) {
            fakeOrientationFlow.emit(
                DeviceOrientationEvent.OrientationUpdate(
                    magneticHeadingDegrees = bearing,
                    accuracy = SensorAccuracy.HIGH,
                )
            )
            advanceUntilIdle()
        }
        assertTrue(vm.state.value.isFacingQibla)

        // Calibration warning when accuracy is LOW suppresses isFacingQibla
        fakeOrientationFlow.emit(
            DeviceOrientationEvent.OrientationUpdate(
                magneticHeadingDegrees = bearing,
                accuracy = SensorAccuracy.LOW,
            )
        )
        advanceUntilIdle()
        assertTrue(vm.state.value.needsCalibration)
        assertFalse("Low accuracy must not report isFacingQibla", vm.state.value.isFacingQibla)
    }

    @Test
    fun testLowAccuracySuppressesFacingQiblaEvenWhenAligned() = testScope.runTest {
        val vm = QiblaViewModel(fakeRepository, fakeOrientationProvider)

        val dubaiLocation = SalahLocation(
            id = "dubai",
            coordinates = LocationCoordinates(25.2048, 55.2708),
            names = PlaceNames(locality = "Dubai", country = "United Arab Emirates"),
            zone = ZoneId.of("Asia/Dubai"),
            mode = LocationMode.MANUAL,
        )
        fakeLocationFlow.value = dubaiLocation
        advanceUntilIdle()

        val available = vm.state.value.locationState as QiblaLocationState.Available
        val bearing = available.bearing.bearingDegrees

        // Aligned heading, but UNRELIABLE accuracy
        repeat(10) {
            fakeOrientationFlow.emit(
                DeviceOrientationEvent.OrientationUpdate(
                    magneticHeadingDegrees = bearing,
                    accuracy = SensorAccuracy.UNRELIABLE,
                )
            )
            advanceUntilIdle()
        }
        assertTrue(vm.state.value.needsCalibration)
        assertFalse(vm.state.value.isFacingQibla)
    }

    @Test
    fun testSensorUnavailableProvidesStaticBearing() = testScope.runTest {
        val vm = QiblaViewModel(fakeRepository, fakeOrientationProvider)

        val dubaiLocation = SalahLocation(
            id = "dubai",
            coordinates = LocationCoordinates(25.2048, 55.2708),
            names = PlaceNames(locality = "Dubai", country = "United Arab Emirates"),
            zone = ZoneId.of("Asia/Dubai"),
            mode = LocationMode.MANUAL,
        )
        fakeLocationFlow.value = dubaiLocation
        advanceUntilIdle()

        fakeOrientationFlow.emit(DeviceOrientationEvent.SensorUnavailable)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(CompassState.NoSensor, state.compassState)
        assertFalse(state.isFacingQibla)
        assertFalse(state.needsCalibration)
        // Dial heading fixed to 0 (True North)
        assertEquals(0f, state.smoothedHeadingDegrees, 0.01f)
        // Relative angle equals calculated bearing
        assertEquals(258.2f, state.relativeQiblaAngleDegrees, 0.5f)
    }

    @Test
    fun testStopAndStartOrientationUpdatesLifecycle() = testScope.runTest {
        val vm = QiblaViewModel(fakeRepository, fakeOrientationProvider)
        fakeOrientationFlow.emit(
            DeviceOrientationEvent.OrientationUpdate(
                magneticHeadingDegrees = 100f,
                accuracy = SensorAccuracy.HIGH,
            )
        )
        advanceUntilIdle()
        assertTrue(vm.state.value.compassState is CompassState.Active)
        assertEquals(100f, (vm.state.value.compassState as CompassState.Active).headingDegrees, 0.1f)

        // Stop updates (e.g. leaving Qibla screen)
        vm.stopOrientationUpdates()
        advanceUntilIdle()

        // Emitting new updates should not change the state while stopped
        fakeOrientationFlow.emit(
            DeviceOrientationEvent.OrientationUpdate(
                magneticHeadingDegrees = 200f,
                accuracy = SensorAccuracy.HIGH,
            )
        )
        advanceUntilIdle()
        assertEquals(100f, (vm.state.value.compassState as CompassState.Active).headingDegrees, 0.1f)

        // Resume updates (e.g. returning to Qibla screen)
        vm.startOrientationUpdates()
        advanceUntilIdle()

        fakeOrientationFlow.emit(
            DeviceOrientationEvent.OrientationUpdate(
                magneticHeadingDegrees = 200f,
                accuracy = SensorAccuracy.HIGH,
            )
        )
        advanceUntilIdle()
        assertEquals(200f, (vm.state.value.compassState as CompassState.Active).headingDegrees, 0.1f)
    }
}

