package com.motiondots.app.estimator

import com.motiondots.app.sensor.WorldAcceleration
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * VehicleMotion represents a minimal set of signals derived from filtered world-frame acceleration.
 */
data class VehicleMotion(
    val filteredX: Float,
    val filteredY: Float,
    val filteredZ: Float,
    val horizontalX: Float,
    val horizontalY: Float,
    val horizontalMagnitude: Float,
    val motionIntensity: Float, // normalized 0..1
    val timestamp: Long
)

/**
 * VehicleMotionEstimator computes a simple, observable vehicle-motion proxy from world-frame acceleration.
 *
 * It accepts a provider function that returns the latest WorldAcceleration (may be null if not available).
 * The estimator does not modify orientation or sensors; it simply derives horizontal magnitude and a
 * normalized motion intensity metric from the filtered world-frame acceleration.
 */
class VehicleMotionEstimator(private val provider: () -> WorldAcceleration?) {
    // heuristic normalization scale for motion intensity (m/s^2). Chosen as a moderate acceleration (≈0.3 g).
    private val NORMALIZATION_SCALE = 3.0f

    fun estimate(): VehicleMotion? {
        val w = provider() ?: return null

        // prefer filtered values if available; fall back to raw world-frame components
        val fx = w.filteredX ?: w.x
        val fy = w.filteredY ?: w.y
        val fz = w.filteredZ ?: w.z

        val hx = fx
        val hy = fy

        // horizontal magnitude (L2 norm)
        val hmag = hypot(hx.toDouble(), hy.toDouble()).toFloat()

        // normalized motion intensity in [0,1]
        val intensity = (hmag / NORMALIZATION_SCALE).let { v -> max(0f, min(1f, v)) }

        return VehicleMotion(
            filteredX = fx,
            filteredY = fy,
            filteredZ = fz,
            horizontalX = hx,
            horizontalY = hy,
            horizontalMagnitude = hmag,
            motionIntensity = intensity,
            timestamp = w.timestamp
        )
    }
}
