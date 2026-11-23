package com.salah.location

import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OfflineLocationDataTest {
    private val assets = File("src/main/assets/location")
    private val index = CityIndex { File(assets, "cities.tsv.gzip").inputStream() }
    private val resolver = BoundaryTimezoneResolver { File(assets, "timezones.bin.gzip").inputStream() }

    @Test fun aliasesAndCountriesResolveOffline() = runTest {
        val makkah = index.search("Mecca").first { it.names.country == "Saudi Arabia" }
        assertEquals("Makkah", makkah.names.locality)
        assertEquals("Asia/Riyadh", makkah.zone.id)
        val london = index.search("London").first { it.names.country == "United Kingdom" }
        assertEquals("Europe/London", london.zone.id)
        val kattankulathur = index.search("Kattankulathur").single()
        assertEquals("Tamil Nadu", kattankulathur.names.region)
        assertEquals("Asia/Kolkata", kattankulathur.zone.id)
        val newYork = index.search("New York")
        assertEquals("New York City", newYork.first().names.locality)
        assertTrue(newYork.none { it.names.locality == "Jakarta" || it.names.locality == "Pittsburg" })
        assertTrue(index.search("qzyxcv abc").isEmpty())
    }

    @Test fun coordinatesResolveZonesAndHandleDaylightSavingAndLocalDate() = runTest {
        data class Sample(val lat: Double, val lng: Double, val zone: String)
        val points = listOf(
            Sample(12.820254, 80.037819, "Asia/Kolkata"),
            Sample(21.4225, 39.8262, "Asia/Riyadh"),
            Sample(51.5085, -0.1257, "Europe/London"),
            Sample(40.7128, -74.0060, "America/New_York"),
        )
        val instant = Instant.parse("2026-07-01T23:30:00Z")
        for (point in points) {
            val zone = resolver.resolve(fix(point.lat, point.lng))
            assertEquals(point.zone, zone?.id)
        }
        val london = ZoneId.of("Europe/London")
        assertEquals(LocalDate.parse("2026-07-02"), instant.atZone(london).toLocalDate())
        assertEquals(ZoneOffset.ofHours(1), london.rules.getOffset(instant))
        assertEquals(ZoneOffset.UTC, london.rules.getOffset(Instant.parse("2026-01-01T23:30:00Z")))
        assertEquals(LocalDate.parse("2026-07-01"), instant.atZone(ZoneId.of("America/New_York")).toLocalDate())
    }

    @Test fun refusesAnUncertainTimezoneAtTheBorderOrOutAtSea() = runTest {
        assertNull(resolver.resolve(fix(30.0, -40.0)))
        val nearInternationalBorder = fix(48.99, -122.7).copy(accuracy = LocationAccuracy(true, 6_000f))
        assertNull(resolver.resolve(nearInternationalBorder))
    }

    private fun fix(lat: Double, lng: Double) =
        LocationFix(LocationCoordinates(lat, lng), LocationAccuracy(false, 20f), Instant.EPOCH, false)
}
