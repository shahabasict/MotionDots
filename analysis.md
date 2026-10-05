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

Orientation / Rotation Vector (Phase 5)

Why device orientation matters
- To interpret accelerometer measurements in a world-fixed frame (or vehicle frame), we must know device orientation. Without orientation, accelerometer axes rotate with the device and include gravity; transforming measurements requires a stable orientation estimate.

What the rotation-vector sensor provides
- The rotation-vector sensor is a sensor fusion product provided by Android (typically combining accelerometer, gyroscope and magnetometer where available) that represents the device's orientation as a rotation from the device coordinate frame to the world frame. Android exposes this as a rotation vector which can be converted to a rotation matrix or quaternion using the SensorManager API.

Quaternion representation
- Quaternions provide a compact, non-singular representation of orientation suitable for composing rotations and transforming vectors. We expose quaternion values as (w, x, y, z) for debugging and future use.

Roll / Pitch / Yaw
- For human-readable diagnostics, we also derive roll, pitch, and yaw (in radians/degrees) using SensorManager.getRotationMatrixFromVector and SensorManager.getOrientation.

Why orientation is needed before transforming accelerometer measurements
- Raw accelerometer readings are provided in device coordinates and include gravity. To compute linear acceleration in a consistent world frame, we must (a) estimate device orientation, (b) remove gravity in the world or device frame, and (c) rotate the vector into the desired coordinate frame. Orientation estimation is therefore a prerequisite for meaningful motion analysis.

Separation of concerns
- In this phase we only collect and display orientation (rotation-vector -> quaternion -> roll/pitch/yaw). No additional fusion, filtering, or motion algorithms are implemented yet.
