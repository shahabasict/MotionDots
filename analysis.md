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
