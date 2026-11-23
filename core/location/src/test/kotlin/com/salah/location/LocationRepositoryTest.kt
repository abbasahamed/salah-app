package com.salah.location

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocationRepositoryTest {
    @get:Rule val directory = TemporaryFolder()
    private val coordinates = LocationCoordinates(51.5085, -0.1257)
    private val london = ZoneId.of("Europe/London")
    private val observed = Instant.parse("2026-11-21T00:00:00Z")
    private val fix = LocationFix(coordinates, LocationAccuracy(false, 12f), observed, false)
    private val londonCity = LocationSearchResult("geonames:2643743", PlaceNames("London", "England", "United Kingdom"), coordinates, london)

    @Test fun rejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException::class.java) { LocationCoordinates(91.0, 10.0) }
        assertThrows(IllegalArgumentException::class.java) { LocationCoordinates(50.0, -181.0) }
        assertThrows(IllegalArgumentException::class.java) { LocationCoordinates(Double.NaN, 10.0) }
    }

    @Test fun denialDoesNotReadProvidersAndDisabledAndNoFixAreDistinct() = runTest {
        val repo = repository(device = { error("Provider must not run without permission") })
        assertEquals(LocationResolutionResult.Failed(LocationFailure.PERMISSION), repo.detect(LocationPermission.NONE, null))
        for (failure in listOf(LocationFailure.DISABLED, LocationFailure.NO_FIX, LocationFailure.PERMISSION)) {
            val unavailable = repository(device = { DeviceLocationResult.Failure(failure) })
            assertEquals(LocationResolutionResult.Failed(failure), unavailable.detect(LocationPermission.PRECISE, null))
        }
    }

    @Test fun preciseFixReturnsReadableAutomaticSelection() = runTest {
        val resolved = repository().detect(LocationPermission.PRECISE, null) as LocationResolutionResult.Found
        assertEquals(LocationMode.AUTOMATIC, resolved.location.mode)
        assertEquals(london, resolved.location.zone)
        assertEquals(londonCity.names, resolved.location.names)
        assertEquals(coordinates, resolved.location.coordinates)
        assertEquals(observed, resolved.location.observedAt)
        assertFalse(resolved.location.stale)
    }

    @Test fun approximateFixDoesNotReverseGeocodeAndPreservesStaleness() = runTest {
        val approximate = fix.copy(accuracy = LocationAccuracy(true, 1_000f), stale = true)
        val repo = repository(device = { DeviceLocationResult.Fix(approximate) }, labels = { error("Unexpected geocoding") })
        val resolved = repo.detect(LocationPermission.APPROXIMATE, null) as LocationResolutionResult.Found
        assertNull(resolved.location.names.displayName)
        assertEquals(approximate.accuracy, resolved.location.accuracy)
        assertTrue(resolved.location.stale)
    }

    @Test fun reverseGeocodingFailurePreservesCoordinatesButUnknownZoneRejectsThem() = runTest {
        val resolved = repository(labels = { throw IOException() })
            .detect(LocationPermission.PRECISE, null) as LocationResolutionResult.Found
        assertEquals(coordinates, resolved.location.coordinates)
        assertNull(resolved.location.names.displayName)
        assertEquals(LocationResolutionResult.Failed(LocationFailure.TIMEZONE),
            repository(timezones = { null }).detect(LocationPermission.PRECISE, null))
    }

    @Test fun smallMovementKeepsScheduleKeyUnlessTimezoneOrModeChanges() = runTest {
        val priorPoint = LocationCoordinates(51.51, -0.1257)
        val prior = londonCity.toLocation().copy(
            mode = LocationMode.AUTOMATIC, coordinates = priorPoint, accuracy = fix.accuracy,
        )
        val same = repository(labels = { error("Nearby label should be reused") })
            .detect(LocationPermission.PRECISE, prior) as LocationResolutionResult.Found
        assertEquals(priorPoint, same.location.coordinates)
        val newZone = ZoneId.of("Europe/Paris")
        val changed = repository(timezones = { newZone }).detect(LocationPermission.PRECISE, prior)
            as LocationResolutionResult.Found
        assertEquals(coordinates, changed.location.coordinates)
        assertEquals(newZone, changed.location.zone)
        val manual = repository().detect(LocationPermission.PRECISE, prior.copy(mode = LocationMode.MANUAL))
            as LocationResolutionResult.Found
        assertEquals(coordinates, manual.location.coordinates)
    }

    @Test fun persistsManualAndAutomaticModeAndClearsSelection() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(directory.root, "location.preferences_pb")
        }
        val preferences = LocationPreferences(store)
        assertNull(preferences.selected.first())
        val manual = londonCity.toLocation()
        preferences.select(manual)
        assertEquals(manual, LocationPreferences(store).selected.first())
        val automatic = (repository().detect(LocationPermission.PRECISE, null) as LocationResolutionResult.Found).location
        preferences.select(automatic)
        assertEquals(automatic, LocationPreferences(store).selected.first())
        preferences.select(null)
        assertNull(LocationPreferences(store).selected.first())
    }

    @Test fun searchPropagatesProviderError() = runTest {
        val repo = repository(cities = { throw IOException("Could not read index") })
        assertTrue(runCatching { repo.search("London") }.exceptionOrNull() is IOException)
    }

    private fun repository(
        device: DeviceLocationProvider = DeviceLocationProvider { DeviceLocationResult.Fix(fix) },
        labels: PlaceLabelResolver = PlaceLabelResolver { londonCity.names },
        timezones: TimezoneResolver = TimezoneResolver { london },
        cities: CitySearch = CitySearch { listOf(londonCity) },
    ) = LocationRepository(device, labels, timezones, cities)
}
