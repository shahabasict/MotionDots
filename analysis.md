MotionDots - Analysis Notes

Phase 1: Project foundation only. This document will contain future scientific and technical reasoning for sensors, coordinate frames, filtering, and motion physics. For now, a short introduction placeholder.

Introduction
This project will use phone inertial sensors and motion processing to provide peripheral motion cues. Detailed theory, equations, coordinate systems, and algorithms will be added in later phases.

Accelerometer (Phase 3)
The Android accelerometer measures proper acceleration applied to the device along three orthogonal axes (X, Y, Z) in device coordinates. Values are given in meters/second^2 (m/s^2), typically including gravity. For Phase 3 we collect raw accelerometer readings:

- X, Y, Z: instantaneous axis accelerations reported by Sensor.TYPE_ACCELEROMETER

Why raw accelerometer is not yet sufficient
- The raw accelerometer contains gravity and device orientation effects — it does not directly represent linear motion in a world-fixed frame.
- Later phases will apply filtering, gravity compensation, and coordinate transforms before using accelerometer data to drive MotionDots.

Gyroscope (Phase 4)
The gyroscope sensor measures angular velocity around the device's three axes (X, Y, Z). Values are typically reported in radians per second (rad/s).

- X, Y, Z: angular velocity about each device axis reported by Sensor.TYPE_GYROSCOPE.

Why raw gyroscope is not yet sufficient
- Raw gyroscope measures rotation rate in device coordinates and must be integrated or fused with accelerometer/orientation data to derive meaningful orientation or rotational motion. Integration without drift compensation will accumulate error.
- Later phases will perform sensor fusion and filtering before using gyroscope data for MotionDots decisions.
