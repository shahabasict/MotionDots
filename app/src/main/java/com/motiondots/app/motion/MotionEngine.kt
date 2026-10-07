package com.motiondots.app.motion

import kotlin.math.*

/**
 * MotionState represents a compact passenger/screen-relative motion signal snapshot.
 */
data class MotionState(
    val forwardAcceleration: Float = 0f, // m/s^2 (positive forward)
    val lateralAcceleration: Float = 0f, // m/s^2 (positive right)
    val feltForward: Float = 0f, // felt force (negated accel)
    val feltLateral: Float = 0f,
    val handlingConfidence: Float = 0f, // 0..1 heuristic
    val timestamp: Long = 0L
)

/**
 * MotionEngine — pure Kotlin foundation for vehicle-like motion signals.
 *
 * Notes (heuristic):
 * - This engine estimates gravity as a slowly adapting low-pass of measured accelerometer
 *   vectors with gyro-influenced correction rate. This is intentionally simple and is a
 *   foundation for later fusion with orientation/gyro rotation.
 * - Linear acceleration is measured_accel - estimated_gravity.
 * - Horizontal projection uses the gravity direction estimate to remove vertical component.
 * - We represent forward/lateral in a screen-relative heuristic: forward is -y, lateral is x
 *   after projecting into the horizontal plane. This is a design decision and should be
 *   validated per device mounting.
 */
class MotionEngine {
    // gravity estimate in device coordinates (m/s^2)
    private var gx = 0f
    private var gy = 0f
    private var gz = 9.80665f // initialize pointing up in z

    // filtered linear accel (after horizontal projection) — used for felt force
    private var filtForward = 0f
    private var filtLateral = 0f

    // last timestamp seen (ns -> ms expected)
    private var lastTs: Long? = null

    // constants
    private val gravityTau = 8.0f // seconds for accelerometer gravity correction
    private val accelFilterTau = 0.1f // seconds for initial acceleration low-pass (~100ms)
    private val deadZone = 0.15f // m/s^2
    private val limit = 3.0f // soft limit scale for tanh

    fun reset() {
        gx = 0f; gy = 0f; gz = 9.80665f
        filtForward = 0f; filtLateral = 0f
        lastTs = null
    }

    /**
     * Expose the current gravity estimate for diagnostics/tests (m/s^2)
     */
    fun getEstimatedGravity(): Triple<Float, Float, Float> = Triple(gx, gy, gz)

    /**
     * Add a sample: accelerometer in m/s^2 (ax,ay,az) and gyroscope in rad/s (gx,gy,gz),
     * timestamp in milliseconds (monotonic). Returns the new MotionState.
     */
    fun addSample(
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
        timestampMs: Long
    ): MotionState {
        val last = lastTs
        val dt = if (last == null) 0.0f else ((timestampMs - last) / 1000.0f).coerceAtLeast(0f)
        lastTs = timestampMs

        // 1) Gyro-based gravity propagation: rotate current gravity estimate forward by
        // the angular velocity (gyroX/Y/Z in rad/s) over dt. This integrates rotation
        // to keep the gravity vector in the device frame as the device rotates.
        val gyroMag = sqrt((gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ).toDouble()).toFloat()
        if (dt > 0f) {
            val angle = gyroMag * dt
            if (angle > 1e-9f) {
                // axis
                val ax = gyroX / gyroMag
                val ay = gyroY / gyroMag
                val az = gyroZ / gyroMag
                // current gravity vector
                val vx = gx
                val vy = gy
                val vz = gz
                // Rodrigues' rotation
                val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
                val sinA = kotlin.math.sin(angle.toDouble()).toFloat()
                val dot = ax * vx + ay * vy + az * vz
                val rx = vx * cosA + (ay * vz - az * vy) * sinA + ax * dot * (1f - cosA)
                val ry = vy * cosA + (az * vx - ax * vz) * sinA + ay * dot * (1f - cosA)
                val rz = vz * cosA + (ax * vy - ay * vx) * sinA + az * dot * (1f - cosA)
                gx = rx
                gy = ry
                gz = rz
            }
        }

        // 2) Slow accelerometer gravity correction (exponential LPF): the accelerometer
        // provides a direct gravity measurement when linear accel is small; we correct
        // the propagated gravity vector slowly toward the measured accel vector.
        if (dt > 0f) {
            val mod = 1f + gyroMag * 5f
            val alpha = (dt / (gravityTau * mod)).coerceIn(0f, 1f)
            gx += (accelX - gx) * alpha
            gy += (accelY - gy) * alpha
            gz += (accelZ - gz) * alpha
        }

        // estimated gravity unit vector
        val gnorm = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat().coerceAtLeast(1e-6f)
        val gnx = gx / gnorm
        val gny = gy / gnorm
        val gnz = gz / gnorm

        // 3) linear acceleration (world/device) = measured - gravity
        val linX = accelX - gx
        val linY = accelY - gy
        val linZ = accelZ - gz

        // 4) horizontal projection: remove component along gravity
        val dot = linX * gnx + linY * gny + linZ * gnz
        val hx = linX - dot * gnx
        val hy = linY - dot * gny

        // 5) passenger/screen-relative mapping heuristic:
        // assume device Y points forward (negative when moving forward), X is rightward.
        val forwardRaw = -hy
        val lateralRaw = hx

        // 6) initial low-pass filter (~100ms)
        if (dt > 0f) {
            val alphaA = (dt / (accelFilterTau + dt)).coerceIn(0f, 1f)
            filtForward += (forwardRaw - filtForward) * alphaA
            filtLateral += (lateralRaw - filtLateral) * alphaA
        } else {
            filtForward = forwardRaw
            filtLateral = lateralRaw
        }

        // 7) felt force = -filtered acceleration (sign convention)
        var feltF = -filtForward
        var feltL = -filtLateral

        // 8) apply dead zone
        if (abs(feltF) < deadZone) feltF = 0f
        if (abs(feltL) < deadZone) feltL = 0f

        // 9) smooth tanh-based limiting
        val feltFlimited = tanhLimit(feltF, limit)
        val feltLlimited = tanhLimit(feltL, limit)

        // 10) handling confidence from gyro magnitude (0..1)
        val handlingConfidence = tanh(gyroMag * 0.5f)

        return MotionState(
            forwardAcceleration = forwardRaw,
            lateralAcceleration = lateralRaw,
            feltForward = feltFlimited,
            feltLateral = feltLlimited,
            handlingConfidence = handlingConfidence,
            timestamp = timestampMs
        )
    }

    private fun tanhLimit(x: Float, limit: Float): Float {
        // smooth limiting using hyperbolic tangent scaled to `limit`
        return (tanh((x / limit).toDouble()) * limit).toFloat()
    }
}
