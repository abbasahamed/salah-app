package com.salah.app.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeviceOrientationEvent {
    data object SensorUnavailable : DeviceOrientationEvent
    data class OrientationUpdate(
        val magneticHeadingDegrees: Float,
        val accuracy: SensorAccuracy,
    ) : DeviceOrientationEvent
}

interface DeviceOrientationProvider {
    fun orientationEvents(): Flow<DeviceOrientationEvent>
}

@Singleton
class DefaultDeviceOrientationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceOrientationProvider {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    override fun orientationEvents(): Flow<DeviceOrientationEvent> = callbackFlow {
        val sm = sensorManager
        if (sm == null) {
            trySend(DeviceOrientationEvent.SensorUnavailable)
            close()
            return@callbackFlow
        }

        val rotationVectorSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometerSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val hasRotationVector = rotationVectorSensor != null
        val hasAccelAndMag = accelerometerSensor != null && magneticSensor != null

        if (!hasRotationVector && !hasAccelAndMag) {
            trySend(DeviceOrientationEvent.SensorUnavailable)
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)

        // State for accelerometer + magnetometer fallback
        val lastAccelerometer = FloatArray(3)
        val lastMagnetometer = FloatArray(3)
        var hasAccelerometerData = false
        var hasMagnetometerData = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var accuracy = when (event.accuracy) {
                    SensorManager.SENSOR_STATUS_UNRELIABLE -> SensorAccuracy.UNRELIABLE
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
                    else -> SensorAccuracy.MEDIUM
                }

                val hasMatrix: Boolean
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    hasMatrix = true
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    hasAccelerometerData = true
                    hasMatrix = hasMagnetometerData && SensorManager.getRotationMatrix(
                        rotationMatrix, null, lastAccelerometer, lastMagnetometer
                    )
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    hasMagnetometerData = true
                    hasMatrix = hasAccelerometerData && SensorManager.getRotationMatrix(
                        rotationMatrix, null, lastAccelerometer, lastMagnetometer
                    )
                } else {
                    hasMatrix = false
                }

                if (!hasMatrix) return

                val displayRotation = getDisplayRotation(context)
                var axisX = SensorManager.AXIS_X
                var axisY = SensorManager.AXIS_Y

                when (displayRotation) {
                    Surface.ROTATION_90 -> {
                        axisX = SensorManager.AXIS_Y
                        axisY = SensorManager.AXIS_MINUS_X
                    }
                    Surface.ROTATION_180 -> {
                        axisX = SensorManager.AXIS_MINUS_X
                        axisY = SensorManager.AXIS_MINUS_Y
                    }
                    Surface.ROTATION_270 -> {
                        axisX = SensorManager.AXIS_MINUS_Y
                        axisY = SensorManager.AXIS_X
                    }
                    else -> {
                        axisX = SensorManager.AXIS_X
                        axisY = SensorManager.AXIS_Y
                    }
                }

                val remapSuccess = SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
                val targetMatrix = if (remapSuccess) remappedMatrix else rotationMatrix

                SensorManager.getOrientation(targetMatrix, orientationValues)
                val azimuthRad = orientationValues[0]
                val azimuthDeg = ((Math.toDegrees(azimuthRad.toDouble()) % 360.0) + 360.0).toFloat() % 360f

                trySend(
                    DeviceOrientationEvent.OrientationUpdate(
                        magneticHeadingDegrees = azimuthDeg,
                        accuracy = accuracy,
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (hasRotationVector) {
            sm.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sm.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magneticSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sm.unregisterListener(listener)
        }
    }

    private fun getDisplayRotation(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
            }
        } catch (_: Exception) {
            Surface.ROTATION_0
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class QiblaModule {
    @Binds
    @Singleton
    abstract fun bindDeviceOrientationProvider(impl: DefaultDeviceOrientationProvider): DeviceOrientationProvider
}

