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

Gravity Compensation & World-Frame Acceleration (Phase 6)

1) Why raw accelerometer values cannot directly drive MotionDots
- Raw accelerometer readings are provided in device coordinates and include the gravity vector. If the device is tilted, the gravity contribution projects onto all device axes. Using raw axes would conflate device orientation with linear motion and produce incorrect cues.

2) Coordinate system conventions used here
- World frame (chosen explicit convention):
  - X: horizontal axis (right-east in a local tangent plane)
  - Y: horizontal axis (forward-north in a local tangent plane)
  - Z: vertical axis, positive UP (opposite the gravity acceleration vector)

3) Quaternion / rotation transformation
- OrientationSensor provides a quaternion q = (w, x, y, z) that represents the rotation from device frame to world frame.
- To map a vector v_device (a_x, a_y, a_z) reported by the accelerometer into world coordinates we compute:

    v_world = q * v_device * q_conj

  where we treat v_device as a pure quaternion (0, a_x, a_y, a_z), and q_conj = (w, -x, -y, -z).

4) Gravity representation in world frame
- Under the chosen convention gravity is represented as g_world = (0, 0, +9.80665) m/s^2 (positive up). The accelerometer reports the proper acceleration including reaction forces; when stationary the device measures approximately +g_world in device coordinates transformed to world coordinates.

5) Gravity-compensation calculation
- Steps implemented in MotionProcessor:
  a) Read raw accelerometer vector a_dev = (a_x, a_y, a_z) in device coordinates.
  b) Read orientation quaternion q = (w, x, y, z) mapping device->world.
  c) Compute a_world = rotate(q, a_dev) = q * a_dev * q_conj.
  d) Compute linear acceleration (gravity-compensated):

       a_linear = a_world - g_world

     where g_world = (0, 0, +9.80665).

6) Resulting world-frame acceleration
- The output is a_linear = (a_x_world - 0, a_y_world - 0, a_z_world - g)
- This vector approximates the actual linear acceleration of the device in the world reference frame (subject to sensor noise and biases). No filtering or bias correction is applied at this stage.

Variables and definitions:
- a_dev = (a_x, a_y, a_z): raw accelerometer reading in device frame (m/s^2)
- q = (w, x, y, z): quaternion representing rotation from device frame to world frame
- q_conj = (w, -x, -y, -z)
- a_world = q * a_dev * q_conj
- g_world = (0, 0, +9.80665) m/s^2 (gravity vector expressed in world frame, positive up)
- a_linear = a_world - g_world (gravity-compensated acceleration)

These equations are implemented verbatim in MotionProcessor.kt and are intentionally left unfiltered so future phases can apply filtering and bias compensation as needed.
