package com.salah.app.qibla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaBearingCalculatorTest {

    @Test
    fun testKaabaLocationIsAtKaaba() {
        val result = QiblaBearingCalculator.calculate(KAABA_LATITUDE, KAABA_LONGITUDE)
        assertTrue(result.isAtKaaba)
        assertEquals(0.0, result.distanceKm, 0.01)
        assertEquals(0f, result.bearingDegrees, 0.01f)
    }

    @Test
    fun testWithin100MetersIsAtKaaba() {
        // Offset by ~50 meters: 0.00045 degrees latitude is ~50m
        val result = QiblaBearingCalculator.calculate(KAABA_LATITUDE + 0.0004, KAABA_LONGITUDE)
        assertTrue(result.isAtKaaba)
        assertEquals(0.0, result.distanceKm, 0.01)
    }

    @Test
    fun testLondonBearing() {
        // London: 51.5074° N, 0.1278° W (-0.1278)
        val result = QiblaBearingCalculator.calculate(51.5074, -0.1278)
        assertFalse(result.isAtKaaba)
        // Expected ~118.98° (East-South-East)
        assertEquals(119.0f, result.bearingDegrees, 1.0f)
        assertTrue(result.distanceKm in 4700.0..4900.0)
    }

    @Test
    fun testNewYorkBearing() {
        // New York: 40.7128° N, 74.0060° W (-74.0060)
        val result = QiblaBearingCalculator.calculate(40.7128, -74.0060)
        assertFalse(result.isAtKaaba)
        // Expected ~58.5° (East-North-East)
        assertEquals(58.5f, result.bearingDegrees, 1.0f)
        assertTrue(result.distanceKm in 10000.0..10500.0)
    }

    @Test
    fun testDubaiBearing() {
        // Dubai: 25.2048° N, 55.2708° E
        val result = QiblaBearingCalculator.calculate(25.2048, 55.2708)
        assertFalse(result.isAtKaaba)
        // Calculated bearing from Dubai to Kaaba is ~258.2°
        assertEquals(258.2f, result.bearingDegrees, 0.5f)
        assertTrue(result.distanceKm in 1600.0..1700.0)
    }

    @Test
    fun testKattankulathurChennaiBearing() {
        // Chennai / Kattankulathur: 12.8231° N, 80.0442° E
        val result = QiblaBearingCalculator.calculate(12.8231, 80.0442)
        assertFalse(result.isAtKaaba)
        // Distance is ~4340 km, bearing ~288.3°
        assertEquals(288.3f, result.bearingDegrees, 0.5f)
        assertTrue("Distance ${result.distanceKm} should be around 4340km", result.distanceKm in 4200.0..4500.0)
    }

    @Test
    fun testSouthernHemisphereSydney() {
        // Sydney: -33.8688° S, 151.2093° E
        val result = QiblaBearingCalculator.calculate(-33.8688, 151.2093)
        assertFalse(result.isAtKaaba)
        // Expected ~277.5°
        assertEquals(277.5f, result.bearingDegrees, 1.5f)
        assertTrue(result.distanceKm in 12000.0..14000.0)
    }
}
