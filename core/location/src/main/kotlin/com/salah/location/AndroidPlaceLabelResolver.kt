package com.salah.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal class AndroidPlaceLabelResolver(private val context: Context) : PlaceLabelResolver {
    private val legacyGeocoding = CoroutineScope(Dispatchers.IO.limitedParallelism(1))

    override suspend fun names(coordinates: LocationCoordinates): PlaceNames? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = withTimeoutOrNull(4_000) {
            if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { result ->
                geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = result.resume(addresses.firstOrNull())
                    override fun onError(errorMessage: String?) = result.resume(null)
                })
            } else legacy(geocoder, coordinates)
        }
        return address?.let {
            PlaceNames(
                it.locality.clean() ?: it.subLocality.clean() ?: it.subAdminArea.clean(),
                it.adminArea.clean(), it.countryName.clean(),
            )
        }
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    @Suppress("DEPRECATION")
    private suspend fun legacy(geocoder: Geocoder, point: LocationCoordinates): Address? =
        suspendCancellableCoroutine { result ->
            // The blocking API cannot be interrupted on every Android version.
            val job = legacyGeocoding.launch {
                val address = try {
                    geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
                } catch (_: IOException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
                result.resume(address)
            }
            result.invokeOnCancellation { job.cancel() }
        }
}
