package com.salah.app.qibla

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object QiblaBearingCalculator {
    const val KAABA_LAT = KAABA_LATITUDE
    const val KAABA_LON = KAABA_LONGITUDE
    private const val EARTH_RADIUS_KM = 6371.0
    private const val AT_KAABA_THRESHOLD_KM = 0.1 // 100 meters

    /**
     * Calculates the great-circle initial bearing and distance from a given point on Earth to the Kaaba.
     * Uses spherical trigonometry forward azimuth.
     */
    fun calculate(userLat: Double, userLon: Double): QiblaBearing {
        require(userLat in -90.0..90.0) { "Latitude must be in [-90, 90], got $userLat" }
        require(userLon in -180.0..180.0) { "Longitude must be in [-180, 180], got $userLon" }

        val phi1 = Math.toRadians(userLat)
        val lambda1 = Math.toRadians(userLon)
        val phi2 = Math.toRadians(KAABA_LAT)
        val lambda2 = Math.toRadians(KAABA_LON)

        val deltaLambda = lambda2 - lambda1
        val deltaPhi = phi2 - phi1

        // Great-circle distance using Haversine formula
        val sinHalfLat = sin(deltaPhi / 2.0)
        val sinHalfLon = sin(deltaLambda / 2.0)
        val a = sinHalfLat * sinHalfLat + cos(phi1) * cos(phi2) * sinHalfLon * sinHalfLon
        val c = 2.0 * asin(min(1.0, kotlin.math.sqrt(max(0.0, a))))
        val distanceKm = EARTH_RADIUS_KM * c

        val isAtKaaba = distanceKm <= AT_KAABA_THRESHOLD_KM
        if (isAtKaaba) {
            return QiblaBearing(
                bearingDegrees = 0f,
                distanceKm = 0.0,
                isAtKaaba = true,
            )
        }

        // Great-circle initial bearing:
        // y = sin(Δλ) * cos(ϕ2)
        // x = cos(ϕ1) * sin(ϕ2) - sin(ϕ1) * cos(ϕ2) * cos(Δλ)
        // θ = atan2(y, x)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val initialBearingRad = atan2(y, x)
        val initialBearingDeg = ((Math.toDegrees(initialBearingRad) % 360.0) + 360.0) % 360.0

        return QiblaBearing(
            bearingDegrees = initialBearingDeg.toFloat(),
            distanceKm = distanceKm,
            isAtKaaba = false,
        )
    }
}

