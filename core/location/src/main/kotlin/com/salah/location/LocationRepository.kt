package com.salah.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository internal constructor(
    private val device: DeviceLocationProvider,
    private val labels: PlaceLabelResolver,
    private val timezones: TimezoneResolver,
    private val cities: CitySearch,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        AndroidLocationProvider(context), AndroidPlaceLabelResolver(context),
        BoundaryTimezoneResolver(context), CityIndex(context),
    )

    suspend fun search(query: String): List<LocationSearchResult> = cities.search(query)

    suspend fun detect(permission: LocationPermission, saved: SalahLocation?): LocationResolutionResult {
        if (permission == LocationPermission.NONE) return LocationResolutionResult.Failed(LocationFailure.PERMISSION)
        val result = device.current(permission)
        if (result is DeviceLocationResult.Failure) return LocationResolutionResult.Failed(result.reason)
        val fix = (result as DeviceLocationResult.Fix).value
        val zone = try { timezones.resolve(fix) } catch (_: IOException) { null }
            ?: return LocationResolutionResult.Failed(LocationFailure.TIMEZONE)
        val nearby = saved?.takeIf {
            !fix.stale && it.mode == LocationMode.AUTOMATIC && it.zone == zone &&
                it.accuracy?.approximate == fix.accuracy.approximate &&
                it.coordinates.distanceTo(fix.coordinates) < 2_000.0
        }
        val coordinates = nearby?.coordinates ?: fix.coordinates
        val accuracy = fix.accuracy.copy(meters = fix.accuracy.meters?.let {
            it + coordinates.distanceTo(fix.coordinates).toFloat()
        })
        val names = when {
            nearby?.names?.displayName != null -> nearby.names
            accuracy.approximate || (accuracy.meters ?: Float.POSITIVE_INFINITY) > 1_000f -> PlaceNames()
            else -> try { labels.names(coordinates) ?: PlaceNames() } catch (_: IOException) { PlaceNames() }
        }
        return LocationResolutionResult.Found(
            SalahLocation(
                id = null, coordinates = coordinates,
                names = names, zone = zone, mode = LocationMode.AUTOMATIC,
                accuracy = accuracy, observedAt = fix.observedAt, stale = fix.stale,
            ),
        )
    }
}

fun LocationSearchResult.toLocation() = SalahLocation(id, coordinates, names, zone, LocationMode.MANUAL)
