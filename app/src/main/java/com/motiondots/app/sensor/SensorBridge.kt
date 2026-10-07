package com.motiondots.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.motiondots.app.motion.MotionEngine
import com.motiondots.app.motion.MotionState

/**
 * SensorBridge: forwards accelerometer and gyroscope samples into a MotionEngine instance.
 *
 * Behavior:
 * - Registers listeners for ACCELEROMETER and GYROSCOPE.
 * - On any sensor event, forwards a combined sample to MotionEngine using the sensor
 *   event timestamp (event.timestamp in ns converted to ms) and the latest other-sensor value.
 * - Safe to start/stop multiple times.
 */
class SensorBridge(private val context: Context) {
    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null

    private var listening = false

    private var latestAccel: FloatArray = floatArrayOf(0f, 0f, 9.80665f)
    private var latestGyro: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var listener: SensorEventListener? = null

    private var motionEngine: MotionEngine? = null
    private var onState: ((MotionState) -> Unit)? = null

    fun start(engine: MotionEngine, onStateUpdate: (MotionState) -> Unit) {
        if (listening) return
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        motionEngine = engine
        onState = onStateUpdate

        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                // timestamp is in nanoseconds; convert to ms for MotionEngine
                val tsMs = (event.timestamp / 1_000_000L)

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        latestAccel[0] = event.values[0]
                        latestAccel[1] = event.values[1]
                        latestAccel[2] = event.values[2]
                        // forward combined sample
                        motionEngine?.let { me ->
                            val s = me.addSample(
                                latestAccel[0], latestAccel[1], latestAccel[2],
                                latestGyro[0], latestGyro[1], latestGyro[2],
                                tsMs
                            )
                            onState?.invoke(s)
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        latestGyro[0] = event.values[0]
                        latestGyro[1] = event.values[1]
                        latestGyro[2] = event.values[2]
                        motionEngine?.let { me ->
                            val s = me.addSample(
                                latestAccel[0], latestAccel[1], latestAccel[2],
                                latestGyro[0], latestGyro[1], latestGyro[2],
                                tsMs
                            )
                            onState?.invoke(s)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // ignore
            }
        }

        // register listeners
        listener = l
        accelSensor?.let { sensorManager?.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroSensor?.let { sensorManager?.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }

        listening = true
    }

    fun stop() {
        // unregister the created listener
        listener?.let { l ->
            sensorManager?.unregisterListener(l)
            listener = null
        }
        listening = false
        motionEngine = null
        onState = null
    }
}
