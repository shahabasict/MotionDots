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

## Phase 7 - One Euro Filter (scientific notes)

The One Euro filter is a lightweight adaptive filter designed for smoothing signals while preserving responsiveness to rapid changes. It adapts its cutoff frequency based on the signal's estimated derivative.

Filter equations (scalar form):

Let x(t) be the input scalar signal sampled at times t_k. We maintain the previous filtered value x_hat_{k-1} and an estimate of the derivative dx_hat_{k-1}.

1) Compute the time difference:
   dt = t_k - t_{k-1}

2) Raw derivative estimate:
   dx_k = (x_k - x_{k-1}) / dt

3) Smooth the derivative with a low-pass filter whose smoothing coefficient depends on a derivative cutoff f_d (Hz):
   alpha_d = 1 / (1 + (1 / (2*pi*f_d)) / dt)
   dx_hat_k = dx_hat_{k-1} + alpha_d * (dx_k - dx_hat_{k-1})

4) Adaptive cutoff frequency for the signal is computed as:
   f_c_k = f_min + beta * |dx_hat_k|

   where f_min is the minimum cutoff (Hz) and beta is the speed coefficient.

5) Compute the smoothing coefficient for the signal using f_c_k:
   alpha_k = 1 / (1 + (1 / (2*pi*f_c_k)) / dt)

6) Low-pass filter the signal:
   x_hat_k = x_hat_{k-1} + alpha_k * (x_k - x_hat_{k-1})

Notes on variables and conventions:
- x_k: input sample at time t_k (units: m/s^2 for acceleration).
- x_hat_k: filtered signal estimate at t_k.
- dx_k: raw derivative (m/s^3) estimated from samples.
- dx_hat_k: low-pass estimate of derivative.
- dt: sample interval (s).
- f_d: derivative cutoff frequency (Hz).
- f_min: minimum cutoff frequency (Hz).
- beta: speed coefficient (Hz per unit of |dx_hat|) which increases the cutoff when the signal is changing fast.

The repository implementation follows the scalar equations above and applies the filter independently to each axis (X, Y, Z) via a small vector wrapper.

## Phase 8 - Vehicle Motion Estimation (analysis)

This section outlines the experimental reasoning and assumptions for a first vehicle-motion estimator built on top of the existing processed sensor pipeline.

Coordinate conventions (repeated):
- World frame: X = horizontal, Y = horizontal, Z = vertical (positive UP).

1) What "vehicle motion" means for MotionDots
- For the scope of MotionDots Phase 8, "vehicle motion" denotes translational acceleration of the vehicle expressed in an Earth-fixed or local tangent frame, projected into the horizontal plane (i.e., components that correspond to forward/backward and lateral accelerations experienced by the vehicle). We do not attempt full vehicle pose estimation or turn-rate estimation in this phase.

2) Difference between frames
- Phone-frame acceleration: raw accelerometer readings in device axes (dependent on how the user holds the phone).
- World-frame acceleration: phone-frame acceleration rotated by the device->world quaternion and gravity-compensated; this is what MotionProcessor produces (x_world, y_world, z_world).
- Vehicle-frame acceleration: acceleration resolved into vehicle longitudinal/lateral/vertical axes. This requires knowing the rotation from world->vehicle which is unknown unless the phone is mounted or calibrated to the vehicle.

3) Why world-frame != vehicle-frame
- The phone may be arbitrarily oriented within the vehicle (pocket, dash, cup holder). Without a known rotation between phone/world and vehicle axes, world-frame horizontal axes do not generally align with vehicle forward/lateral axes.

4) Unknown phone orientation relative to the vehicle
- Because the phone orientation relative to the vehicle is unknown, we cannot directly map world X/Y to vehicle longitudinal/lateral components. The estimator in this phase therefore treats world X and world Y as two orthogonal horizontal measurements and exposes both components individually.

5) Utility of horizontal acceleration
- Even without knowing vehicle axes, the magnitude of horizontal acceleration (sqrt(x_world^2 + y_world^2)) provides a rotation-invariant measure of translational activity in the horizontal plane and is therefore a useful proxy for vehicle motion intensity.

6) Phone rotation and hand movement contamination
- Phone rotations (roll/pitch/yaw) and hand-induced translations produce accelerations in the phone sensors that, after rotation into world frame, can look similar to vehicle-induced accelerations. Rapid rotations are often visible in the gyroscope; manual manipulations can produce transient spikes in the accelerometer that the One Euro filter may partially attenuate but not fully remove.

7) Role of gyroscope/angular velocity
- The gyroscope provides angular velocity (rad/s) in device axes and can be used as a diagnostic to detect phone rotation events coincident with accelerometer spikes. In this phase we do not fuse gyroscope into the translational estimator — we expose the gyroscope as context so future phases can use it to classify or reject phone-motion artifacts.

8) Whether acceleration should be the initial motion cue signal
- Acceleration is the immediate sensor-provided cue that best correlates with sudden vehicle maneuvers (braking, acceleration, turns). It is therefore the natural starting point for an observable vehicle-motion estimator. However, acceleration alone is noisy and transient; additional processing (filtering, classification) will be required for robust cues.

9) Velocity / dead-reckoning caveats
- Integrating acceleration to obtain velocity and position (dead-reckoning) accumulates bias and noise, leading to unbounded drift. This is why Phase 8 avoids velocity-based cues derived from open-loop integration and focuses on short-time acceleration magnitude and simple statistics.

10) Assumptions for the first estimator
- Use filtered world-frame linear acceleration (One Euro filtered) as the input.
- Treat world X and Y as horizontal components without assuming alignment to vehicle axes.
- Compute horizontal magnitude as sqrt(x^2 + y^2) and derive a bounded "motion intensity" metric by normalizing this magnitude with a heuristic scale.
- Expose gyroscope values as diagnostics but do not fuse them into the estimator.
- Keep the estimator deliberately simple and observable for later iteration.

Estimator output (to be implemented):
- filtered_world_x, filtered_world_y, filtered_world_z (m/s^2)
- horizontal_x (same as filtered_world_x)
- horizontal_y (same as filtered_world_y)
- horizontal_magnitude = sqrt(horizontal_x^2 + horizontal_y^2) (m/s^2)
- motion_intensity = clamp(horizontal_magnitude / S, 0.0, 1.0) where S is a chosen scale (e.g., 3.0 m/s^2) representing a moderate acceleration magnitude used to normalize intensity.

The implementation will document the chosen S and rationale; it is heuristic and intended for experimentation only.
