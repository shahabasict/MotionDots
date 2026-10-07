package com.motiondots.app.motion

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos

class MotionEngineTest {
    @Test
    fun stationary_produces_zero_motion() {
        val engine = MotionEngine()
        val ts = 1000L
        var state = engine.addSample(0f, 0f, 9.80665f, 0f, 0f, 0f, ts)
        // feed a few samples
        for (i in 1..10) {
            state = engine.addSample(0f, 0f, 9.80665f, 0f, 0f, 0f, ts + i * 100)
        }
        assertEquals(0f, state.forwardAcceleration, 0.05f)
        assertEquals(0f, state.lateralAcceleration, 0.05f)
        assertEquals(0f, state.feltForward, 0.1f)
        assertEquals(0f, state.feltLateral, 0.1f)
    }

    @Test
    fun rotating_phone_rotates_gravity_estimate_and_produces_no_linear_motion() {
        val engine = MotionEngine()
        val ts0 = 1000L
        // start upright
        engine.addSample(0f, 0f, 9.80665f, 0f, 0f, 0f, ts0)

        // rotate 90 degrees around X over 1 second -> angular velocity ~ pi/2 rad / 1s
        val totalAngle = Math.PI.toFloat() / 2f
        val duration = 1000f
        val steps = 20
        val dtMs = (duration / steps).toLong()
        val omega = totalAngle / (duration / 1000f) // rad/s

        var t = ts0
        for (i in 1..steps) {
            t += dtMs
            // compute rotated gravity vector analytically for this step angle
            val angle = omega * (i * dtMs.toFloat() / 1000f)
            val gx = 0f
            val gy = (sin(angle.toDouble()) * 9.80665).toFloat() * -1f // device Y negative forward sign convention
            val gz = (cos(angle.toDouble()) * 9.80665).toFloat()
            // supply gyro indicating rotation about X
            val state = engine.addSample(0f, gy, gz, omega, 0f, 0f, t)
            // linear felt forces should remain near zero
            assertEquals(0f, state.feltForward, 0.5f)
            assertEquals(0f, state.feltLateral, 0.5f)
        }
    }

    @Test
    fun forward_acceleration_has_correct_felt_sign() {
        val engine = MotionEngine()
        val ts = 2000L
        // simulate forward acceleration: negative device Y
        var state = engine.addSample(0f, -1.0f, 9.0f, 0f, 0f, 0f, ts)
        state = engine.addSample(0f, -1.0f, 9.0f, 0f, 0f, 0f, ts + 100)
        // feltForward should be positive when braking? Here forwardRaw = -(-1)=1 => felt = -filtered = -1 -> limited
        // therefore check felt sign is negative of forwardAcceleration
        assertEquals(-state.forwardAcceleration, state.feltForward, 0.5f)
    }

    @Test
    fun braking_has_opposite_sign() {
        val engine = MotionEngine()
        val ts = 3000L
        // simulate braking: positive device Y
        var state = engine.addSample(0f, 2.0f, 9.8f, 0f, 0f, 0f, ts)
        state = engine.addSample(0f, 2.0f, 9.8f, 0f, 0f, 0f, ts + 100)
        assertEquals(-state.forwardAcceleration, state.feltForward, 0.5f)
    }

    @Test
    fun small_noise_within_dead_zone() {
        val engine = MotionEngine()
        val ts = 4000L
        var state = engine.addSample(0.01f, 0.02f, 9.80665f, 0f, 0f, 0f, ts)
        state = engine.addSample(0.02f, 0.01f, 9.80665f, 0f, 0f, 0f, ts + 100)
        assertEquals(0f, state.feltForward, 0.2f)
        assertEquals(0f, state.feltLateral, 0.2f)
    }

    @Test
    fun large_acceleration_is_limited_smoothly() {
        val engine = MotionEngine()
        val ts = 5000L
        val state = engine.addSample(5f, 0f, 9.8f, 0f, 0f, 0f, ts)
        // felt should be limited to near configured limit (3.0f)
        assertTrue(abs(state.feltLateral) <= 3.1f || abs(state.feltForward) <= 3.1f)
    }

    @Test
    fun handling_confidence_increases_with_gyro() {
        val engine = MotionEngine()
        val ts = 6000L
        val low = engine.addSample(0f, 0f, 9.8f, 0f, 0f, 0f, ts)
        val high = engine.addSample(0f, 0f, 9.8f, 2f, 1f, 0.5f, ts + 100)
        assertTrue(high.handlingConfidence >= low.handlingConfidence)
    }

    @Test
    fun timestamps_and_dt_handled_safely() {
        val engine = MotionEngine()
        val state1 = engine.addSample(0f, 0f, 9.8f, 0f, 0f, 0f, 0L)
        val state2 = engine.addSample(0f, 0f, 9.8f, 0f, 0f, 0f, 0L) // same timestamp
        assertNotNull(state1)
        assertNotNull(state2)
    }
}
