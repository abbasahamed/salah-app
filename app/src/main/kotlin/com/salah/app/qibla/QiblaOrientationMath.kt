package com.salah.app.qibla

object QiblaOrientationMath {

    /**
     * Converts a magnetic heading to a true north heading using the magnetic declination in degrees.
     * Positive declination means magnetic north is east of true north.
     */
    fun magneticToTrueHeading(magneticHeading: Float, declinationDegrees: Float): Float {
        return ((magneticHeading + declinationDegrees) % 360f + 360f) % 360f
    }

    /**
     * Circular smoothing using shortest angular distance.
     * Prevents wrap-around jumps across 0° / 360°.
     */
    fun smoothAngle(currentSmoothed: Float, targetAngle: Float, alpha: Float = 0.2f): Float {
        val delta = ((targetAngle - currentSmoothed + 540f) % 360f) - 180f
        return ((currentSmoothed + alpha * delta) % 360f + 360f) % 360f
    }

    /**
     * Computes relative angle of Qibla on the device screen (0° = top of screen).
     */
    fun computeRelativeQiblaAngle(qiblaBearing: Float, deviceHeading: Float): Float {
        return ((qiblaBearing - deviceHeading) % 360f + 360f) % 360f
    }

    /**
     * Checks if device is facing Qibla with hysteresis.
     * Enters aligned state within [enterThreshold] (default 3°).
     * Remains aligned until deviating past [exitThreshold] (default 5°).
     */
    fun checkFacingQibla(
        relativeAngle: Float,
        currentlyFacing: Boolean,
        enterThreshold: Float = ALIGNMENT_THRESHOLD_DEGREES,
        exitThreshold: Float = ALIGNMENT_EXIT_THRESHOLD_DEGREES,
    ): Boolean {
        val diffFromNorth = minOf(relativeAngle, 360f - relativeAngle)
        return if (currentlyFacing) {
            diffFromNorth <= exitThreshold
        } else {
            diffFromNorth <= enterThreshold
        }
    }

    /**
     * Calculates angular difference to turn towards Qibla:
     * Negative values indicate turning left, positive values indicate turning right.
     */
    fun turnAngleDegrees(relativeAngle: Float): Float {
        return ((relativeAngle + 180f) % 360f) - 180f
    }
}

