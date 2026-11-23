package com.salah.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import java.time.Instant
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

fun Context.locationPermission(): LocationPermission = when {
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED -> LocationPermission.PRECISE
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED -> LocationPermission.APPROXIMATE
    else -> LocationPermission.NONE
}

internal class AndroidLocationProvider(private val context: Context) : DeviceLocationProvider {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission") // Rechecked immediately before using either provider.
    override suspend fun current(permission: LocationPermission): DeviceLocationResult {
        val effective = context.locationPermission()
        if (permission == LocationPermission.NONE || effective != permission) {
            return failed(LocationFailure.PERMISSION)
        }
        val approximate = effective == LocationPermission.APPROXIMATE
        if (!LocationManagerCompat.isLocationEnabled(manager)) return failed(LocationFailure.DISABLED)

        return try {
            val providers = buildList {
                if (!approximate) add(LocationManager.GPS_PROVIDER)
                if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
                add(LocationManager.NETWORK_PROVIDER)
            }
            val enabled = providers.filter { manager.isProviderEnabled(it) }
            val client = if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                ConnectionResult.SUCCESS
            ) LocationServices.getFusedLocationProviderClient(context) else null
            val granularity = if (approximate) Granularity.GRANULARITY_COARSE else Granularity.GRANULARITY_FINE
            val request = CurrentLocationRequest.Builder()
                .setPriority(if (approximate) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY)
                .setGranularity(granularity)
                .setMaxUpdateAgeMillis(0)
                .setDurationMillis(8_000)
                .build()

            val fused = client?.let {
                withTimeoutOrNull(9_000) {
                    val token = CancellationTokenSource()
                    it.getCurrentLocation(request, token.token).locationOrNull(token)
                }
            }
            val platform = if (fused != null && ageMillis(fused) <= FRESH_AGE_MS) null else {
                enabled.firstNotNullOfOrNull { provider ->
                    withTimeoutOrNull(4_000) { platformFix(provider) }
                }
            }
            val lastRequest = LastLocationRequest.Builder().setGranularity(granularity)
                .setMaxUpdateAgeMillis(MAX_SAVED_AGE_MS).build()
            val candidates = listOfNotNull(fused, platform).filter { ageMillis(it) <= MAX_SAVED_AGE_MS }
            val current = candidates.minByOrNull(::ageMillis)?.takeIf { ageMillis(it) <= FRESH_AGE_MS }
            val fix = current ?: lastKnown(client, lastRequest, candidates, enabled)

            when {
                context.locationPermission() == LocationPermission.NONE -> failed(LocationFailure.PERMISSION)
                !LocationManagerCompat.isLocationEnabled(manager) -> failed(LocationFailure.DISABLED)
                !approximate && context.locationPermission() != LocationPermission.PRECISE -> failed(LocationFailure.PERMISSION)
                fix == null -> failed(LocationFailure.NO_FIX)
                else -> DeviceLocationResult.Fix(LocationFix(
                    LocationCoordinates(fix.latitude, fix.longitude),
                    LocationAccuracy(approximate, fix.accuracy.takeIf { fix.hasAccuracy() && it.isFinite() && it >= 0f }),
                    Instant.ofEpochMilli(fix.time), current == null,
                ))
            }
        } catch (_: SecurityException) {
            failed(LocationFailure.PERMISSION)
        } catch (_: IllegalArgumentException) {
            failed(LocationFailure.NO_FIX)
        }
    }

    @SuppressLint("MissingPermission") // Called by current() with the same permission and granularity.
    private suspend fun lastKnown(
        client: FusedLocationProviderClient?,
        request: LastLocationRequest,
        candidates: List<Location>,
        providers: List<String>,
    ): Location? {
        val fused = withTimeoutOrNull(2_000) { client?.getLastLocation(request)?.locationOrNull() }
        return (candidates + listOfNotNull(fused) + providers.mapNotNull(manager::getLastKnownLocation))
            .filter { ageMillis(it) <= MAX_SAVED_AGE_MS }
            .minByOrNull(::ageMillis)
    }

    @SuppressLint("MissingPermission")
    private suspend fun platformFix(provider: String): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationSignal()
        continuation.invokeOnCancellation { cancellation.cancel() }
        LocationManagerCompat.getCurrentLocation(
            manager, provider, cancellation, ContextCompat.getMainExecutor(context),
        ) { continuation.resume(it) }
    }

    private suspend fun Task<Location>.locationOrNull(token: CancellationTokenSource? = null): Location? =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { token?.cancel() }
            addOnCompleteListener { task -> continuation.resume(if (task.isSuccessful) task.result else null) }
        }

    private fun ageMillis(location: Location): Long =
        ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000).coerceAtLeast(0)

    private fun failed(reason: LocationFailure) = DeviceLocationResult.Failure(reason)

    private companion object {
        const val FRESH_AGE_MS = 30_000L
        const val MAX_SAVED_AGE_MS = 86_400_000L
    }
}
