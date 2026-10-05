package com.motiondots.app.sensor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.PI


/** One Euro filter implementation for scalar signals. */
class OneEuroFilter(
    private var minCutoff: Double = 0.4, // Hz
    private var beta: Double = 0.007, // speed coefficient
    private var dCutoff: Double = 1.0 // derivative cutoff Hz
) {
    private var xPrev: Double? = null
    private var dxPrev: Double? = null
    private var tPrev: Double? = null

    private fun alpha(cutoff: Double, dt: Double): Double {
        val tau = 1.0 / (2 * PI * cutoff)
        return 1.0 / (1.0 + tau / dt)
    }

    fun reset() {
        xPrev = null
        dxPrev = null
        tPrev = null
    }

    /**
     * Filter a new sample x at time t (seconds). Returns filtered value.
     */
    fun filter(x: Double, t: Double): Double {
        val t0 = tPrev
        if (t0 == null || t <= t0) {
            // first sample or non-increasing time: initialize
            xPrev = x
            dxPrev = 0.0
            tPrev = t
            return x
        }

        val dt = t - t0
        if (dt <= 0.0) {
            return xPrev ?: x
        }

        // derivative
        val dx = (x - (xPrev ?: x)) / dt

        // smooth derivative
        val alphaD = alpha(dCutoff, dt)
        val dxHat = (dxPrev ?: dx) + alphaD * (dx - (dxPrev ?: dx))

        // adaptive cutoff
        val cutoff = minCutoff + beta * abs(dxHat)

        val a = alpha(cutoff, dt)
        val xHat = (xPrev ?: x) + a * (x - (xPrev ?: x))

        // update state
        xPrev = xHat
        dxPrev = dxHat
        tPrev = t

        return xHat
    }
}

/** Simple vector wrapper for three OneEuroFilters */
class OneEuroVectorFilter(minCutoff: Double = 0.4, beta: Double = 0.007, dCutoff: Double = 1.0) {
    private val fx = OneEuroFilter(minCutoff, beta, dCutoff)
    private val fy = OneEuroFilter(minCutoff, beta, dCutoff)
    private val fz = OneEuroFilter(minCutoff, beta, dCutoff)

    fun reset() { fx.reset(); fy.reset(); fz.reset() }

    fun filter(x: Double, y: Double, z: Double, t: Double): Triple<Double, Double, Double> {
        val rx = fx.filter(x, t)
        val ry = fy.filter(y, t)
        val rz = fz.filter(z, t)
        return Triple(rx, ry, rz)
    }
}

/**
 * WorldAcceleration contains raw gravity-compensated world-frame acceleration plus optional filtered values.
 */
data class WorldAcceleration(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
    val filteredX: Float? = null,
    val filteredY: Float? = null,
    val filteredZ: Float? = null
)

/**
 * MotionProcessor computes gravity-compensated acceleration in a world frame given
 * accelerometer readings (device frame) and orientation readings (rotation-vector -> quaternion).
 *
 * Coordinate convention (explicit):
 * - World frame: X (horizontal), Y (horizontal), Z (vertical, positive UP)
 * - Device frame: as reported by Android accelerometer (device-specific axes)
 *
 * Gravity in world frame is represented as g = (0, 0, +9.80665) m/s^2 (positive up).
 */
class MotionProcessor(
    private val accelFlow: StateFlow<com.motiondots.app.sensor.AccelerometerReading?>,
    private val orientFlow: StateFlow<com.motiondots.app.sensor.OrientationReading?>
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var job: Job? = null
    private val mutex = Mutex()

    // latest processed value
    private var _latest: WorldAcceleration? = null
    val latest: WorldAcceleration?
        get() = _latest

    // filtered latest
    private var _latestFiltered: WorldAcceleration? = null
    val latestFiltered: WorldAcceleration?
        get() = _latestFiltered

    // vector filter for world-frame components
    private val vectorFilter = OneEuroVectorFilter(0.4, 0.007, 1.0)

    fun start() {
        if (job != null) return
        job = scope.launch {
            // whenever either accelerometer or orientation updates, recompute
            accelFlow.collectLatest { accel ->
                recomputeIfPossible(accel, orientFlow.value)
            }
        }
        // Also collect orientation changes to update when orientation changes but accel stable
        scope.launch {
            orientFlow.collectLatest { orient ->
                recomputeIfPossible(accelFlow.value, orient)
            }
        }
    }

    suspend fun recomputeIfPossible(accel: com.motiondots.app.sensor.AccelerometerReading?, orient: com.motiondots.app.sensor.OrientationReading?) {
        mutex.withLock {
            if (accel == null || orient == null) return

            val ax = accel.x
            val ay = accel.y
            val az = accel.z

            // quaternion from orientation (w, x, y, z)
            val qw = orient.qw.toDouble()
            val qx = orient.qx.toDouble()
            val qy = orient.qy.toDouble()
            val qz = orient.qz.toDouble()

            // rotate device acceleration into world frame: v_world = q * v_dev * q_conj
            val (wx, wy, wz) = rotateVectorByQuaternion(ax.toDouble(), ay.toDouble(), az.toDouble(), qw, qx, qy, qz)

            // gravity in world frame (positive up)
            val G = 9.80665

            // gravity-compensated linear acceleration in world frame
            val lin_x = wx
            val lin_y = wy
            val lin_z = wz - G

            // compute filtered values using One Euro filter; timestamps are in seconds
            val tSec = accel.timestamp.toDouble() / 1000.0
            val (fxv, fyv, fzv) = vectorFilter.filter(lin_x, lin_y, lin_z, tSec)

            _latest = WorldAcceleration(lin_x.toFloat(), lin_y.toFloat(), lin_z.toFloat(), accel.timestamp, fxv.toFloat(), fyv.toFloat(), fzv.toFloat())
            _latestFiltered = _latest
        }
    }

    private fun rotateVectorByQuaternion(vx: Double, vy: Double, vz: Double, qw: Double, qx: Double, qy: Double, qz: Double): Triple<Double, Double, Double> {
        // q * v * q_conj where v is pure quaternion (0, vx, vy, vz)
        // compute q * v
        val rw = - qx * vx - qy * vy - qz * vz
        val rx =   qw * vx + qy * vz - qz * vy
        val ry =   qw * vy + qz * vx - qx * vz
        val rz =   qw * vz + qx * vy - qy * vx

        // multiply (rw, rx, ry, rz) by q_conj (qw, -qx, -qy, -qz)
        val outx = rw * -qx + rx * qw + ry * -qz - rz * -qy
        val outy = rw * -qy - rx * -qz + ry * qw + rz * -qx
        val outz = rw * -qz + rx * -qy - ry * -qx + rz * qw

        return Triple(outx, outy, outz)
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
