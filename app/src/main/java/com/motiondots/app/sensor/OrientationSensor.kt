package com.motiondots.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI

data class OrientationReading(
    val qw: Float,
    val qx: Float,
    val qy: Float,
    val qz: Float,
    val roll: Float, // radians
    val pitch: Float, // radians
    val yaw: Float, // radians (azimuth)
    val timestamp: Long
)

/**
 * Orientation provider using the rotation-vector sensor.
 * Converts rotation-vector values into a quaternion and roll/pitch/yaw.
 */
class OrientationSensor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _reading = MutableStateFlow<OrientationReading?>(null)
    val reading: StateFlow<OrientationReading?> = _reading.asStateFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

            val rv = event.values
            // rotation matrix
            val rotMat = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotMat, rv)
            // orientation angles (azimuth, pitch, roll)
            val orient = FloatArray(3)
            SensorManager.getOrientation(rotMat, orient)

            // quaternion from rotation vector
            val quat = FloatArray(4)
            try {
                // q[0]=w, q[1]=x, q[2]=y, q[3]=z
                SensorManager.getQuaternionFromVector(quat, rv)
            } catch (t: Throwable) {
                // fallback: if not available, approximate
                quat[0] = 1f; quat[1] = 0f; quat[2] = 0f; quat[3] = 0f
            }

            _reading.value = OrientationReading(
                qw = quat[0], qx = quat[1], qy = quat[2], qz = quat[3],
                roll = orient[2], pitch = orient[1], yaw = orient[0],
                timestamp = System.currentTimeMillis()
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun hasSensor(): Boolean = rotationVector != null

    fun start() {
        rotationVector?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
