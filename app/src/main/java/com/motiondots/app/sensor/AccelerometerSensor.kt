package com.motiondots.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccelerometerReading(val x: Float, val y: Float, val z: Float, val timestamp: Long)

/**
 * Simple accelerometer provider.
 * Exposes the latest accelerometer reading via a StateFlow. Call start()/stop() to control listening.
 */
class AccelerometerSensor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _reading = MutableStateFlow<AccelerometerReading?>(null)
    val reading: StateFlow<AccelerometerReading?> = _reading.asStateFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && event.values.size >= 3) {
                _reading.value = AccelerometerReading(event.values[0], event.values[1], event.values[2], System.currentTimeMillis())
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // no-op for now
        }
    }

    fun hasSensor(): Boolean = accelerometer != null

    fun start() {
        accelerometer?.also {
            // moderate sampling rate suitable for UI diagnostics
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
