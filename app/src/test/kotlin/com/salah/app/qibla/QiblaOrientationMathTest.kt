package com.salah.app.qibla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class QiblaOrientationMathTest {

    @Test
    fun testMagneticToTrueHeading() {
        // Positive declination (e.g. +5°)
        assertEquals(95f, QiblaOrientationMath.magneticToTrueHeading(90f, 5f), 0.01f)
        // Negative declination (e.g. -10°)
        assertEquals(80f, QiblaOrientationMath.magneticToTrueHeading(90f, -10f), 0.01f)
        // Wrapping around 360°
        assertEquals(5f, QiblaOrientationMath.magneticToTrueHeading(355f, 10f), 0.01f)
        assertEquals(355f, QiblaOrientationMath.magneticToTrueHeading(5f, -10f), 0.01f)
    }

    @Test
    fun testSmoothAngleAcrossZeroDiscontinuity() {
        // Smoothing from 359° to 1° should take the short route (delta = +2°), NOT wrap around 358°!
        val smoothed = QiblaOrientationMath.smoothAngle(359f, 1f, alpha = 0.5f)
        // Delta is +2°, with alpha 0.5 smoothed is 359 + 1 = 360 = 0°
        assertEquals(0f, smoothed, 0.01f)

        // Smoothing from 1° to 359° (delta = -2°)
        val smoothedReverse = QiblaOrientationMath.smoothAngle(1f, 359f, alpha = 0.5f)
        assertEquals(0f, smoothedReverse, 0.01f)
    }

    @Test
    fun testComputeRelativeQiblaAngle() {
        // Qibla is 290°, device heading is 290° -> relative angle 0° (facing Qibla)
        assertEquals(0f, QiblaOrientationMath.computeRelativeQiblaAngle(290f, 290f), 0.01f)
        // Qibla is 290°, device heading is 270° -> relative angle 20° (turn right 20°)
        assertEquals(20f, QiblaOrientationMath.computeRelativeQiblaAngle(290f, 270f), 0.01f)
        // Qibla is 290°, device heading is 300° -> relative angle 350° (turn left 10°)
        assertEquals(350f, QiblaOrientationMath.computeRelativeQiblaAngle(290f, 300f), 0.01f)
    }

    @Test
    fun testCheckFacingQiblaWithHysteresis() {
        // Initial state: not facing
        assertFalse(QiblaOrientationMath.checkFacingQibla(relativeAngle = 4.0f, currentlyFacing = false))
        assertTrue(QiblaOrientationMath.checkFacingQibla(relativeAngle = 2.5f, currentlyFacing = false))
        assertTrue(QiblaOrientationMath.checkFacingQibla(relativeAngle = 358.0f, currentlyFacing = false))

        // Aligned state: within exit threshold (5°) it stays facing
        assertTrue(QiblaOrientationMath.checkFacingQibla(relativeAngle = 4.0f, currentlyFacing = true))
        assertTrue(QiblaOrientationMath.checkFacingQibla(relativeAngle = 356.0f, currentlyFacing = true))

        // Exits when deviation > 5°
        assertFalse(QiblaOrientationMath.checkFacingQibla(relativeAngle = 5.5f, currentlyFacing = true))
        assertFalse(QiblaOrientationMath.checkFacingQibla(relativeAngle = 354.0f, currentlyFacing = true))
    }

    @Test
    fun testTurnAngleDegrees() {
        // Relative angle 20° -> turn right +20°
        assertEquals(20f, QiblaOrientationMath.turnAngleDegrees(20f), 0.01f)
        // Relative angle 350° -> turn left -10°
        assertEquals(-10f, QiblaOrientationMath.turnAngleDegrees(350f), 0.01f)
        // Relative angle 180° boundary condition
        assertEquals(180f, abs(QiblaOrientationMath.turnAngleDegrees(180f)), 0.01f)
        // Relative angle 0° -> aligned
        assertEquals(0f, QiblaOrientationMath.turnAngleDegrees(0f), 0.01f)
    }

    @Test
    fun testRapidAngleWraparoundTransitions() {
        // Test multiple consecutive transitions across the 0° / 360° meridian
        var heading = 358f
        // Move clockwise to 2°
        heading = QiblaOrientationMath.smoothAngle(heading, 2f, alpha = 0.5f)
        assertEquals(0f, heading, 0.01f)
        heading = QiblaOrientationMath.smoothAngle(heading, 2f, alpha = 0.5f)
        assertEquals(1f, heading, 0.01f)

        // Move counter-clockwise to 355°
        heading = QiblaOrientationMath.smoothAngle(1f, 355f, alpha = 0.5f)
        assertEquals(358f, heading, 0.01f)
    }
}

