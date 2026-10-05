package com.motiondots.app.sensor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WorldAcceleration(val x: Float, val y: Float, val z: Float, val timestamp: Long)

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

            _latest = WorldAcceleration(lin_x.toFloat(), lin_y.toFloat(), lin_z.toFloat(), accel.timestamp)
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
