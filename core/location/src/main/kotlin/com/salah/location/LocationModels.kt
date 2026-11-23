package com.salah.location

import java.time.Instant
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class LocationMode { AUTOMATIC, MANUAL }

data class LocationCoordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
    }

    internal fun distanceTo(other: LocationCoordinates): Double {
        val lat = Math.toRadians(latitude)
        val otherLat = Math.toRadians(other.latitude)
        val lng = Math.toRadians(other.longitude - longitude)
        val h = sin((otherLat - lat) / 2).let { it * it } +
            cos(lat) * cos(otherLat) * sin(lng / 2).let { it * it }
        return 12_742_000 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}

data class LocationAccuracy(val approximate: Boolean, val meters: Float?) {
    init { require(meters == null || (meters.isFinite() && meters >= 0f)) }
}

data class SalahLocation(
    val id: String?,
    val coordinates: LocationCoordinates,
    val names: PlaceNames,
    val zone: ZoneId,
    val mode: LocationMode,
    val accuracy: LocationAccuracy? = null,
    val observedAt: Instant? = null,
    val stale: Boolean = false,
) {
    init { require(zone.id in ZoneId.getAvailableZoneIds()) }
}

data class PlaceNames(
    val locality: String? = null,
    val region: String? = null,
    val country: String? = null,
) {
    val displayName: String?
        get() = (locality ?: region ?: country)?.takeIf(String::isNotBlank)

    val qualifier: String
        get() = listOfNotNull(region, country).distinct().filter { it != displayName }.joinToString(", ")
}

data class LocationSearchResult(
    val id: String,
    val names: PlaceNames,
    val coordinates: LocationCoordinates,
    val zone: ZoneId,
)

enum class LocationPermission { NONE, APPROXIMATE, PRECISE }

sealed interface LocationResolutionResult {
    data class Found(val location: SalahLocation) : LocationResolutionResult
    data class Failed(val reason: LocationFailure) : LocationResolutionResult
}

enum class LocationFailure { PERMISSION, DISABLED, NO_FIX, TIMEZONE }

internal data class LocationFix(
    val coordinates: LocationCoordinates,
    val accuracy: LocationAccuracy,
    val observedAt: Instant,
    val stale: Boolean,
)

internal sealed interface DeviceLocationResult {
    data class Fix(val value: LocationFix) : DeviceLocationResult
    data class Failure(val reason: LocationFailure) : DeviceLocationResult
}

internal fun interface DeviceLocationProvider {
    suspend fun current(permission: LocationPermission): DeviceLocationResult
}

internal fun interface PlaceLabelResolver {
    suspend fun names(coordinates: LocationCoordinates): PlaceNames?
}

internal fun interface TimezoneResolver {
    suspend fun resolve(fix: LocationFix): ZoneId?
}
