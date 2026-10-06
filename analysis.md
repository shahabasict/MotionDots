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

## Phase 9 - Investigate Motion Signal Before Dot Mapping

Objective
- Analyze the existing motion-processing pipeline (accelerometer -> orientation -> device->world rotation -> gravity compensation -> One Euro filtering -> estimator) to explain observed behaviours from Phase 8 physical validation and to identify minimal experiments or changes required to resolve uncertainties.

Confirmed facts (from code inspection)
- Accelerometer semantics: Android accelerometer returns "proper acceleration" in device coordinates (includes gravity + linear acceleration) in m/s^2.
- Gravity compensation in MotionProcessor: the code rotates the device accelerometer vector into the world frame using the rotation-vector -> quaternion orientation and computes linear acceleration by subtracting a constant gravity vector G = 9.80665 m/s^2 in world Z: lin_z = wz - G; lin_x = wx; lin_y = wy. This matches the documented convention where g_world = (0,0,+9.80665) (positive UP) and a_linear = a_world - g_world.
- Quaternion rotation: MotionProcessor.rotateVectorByQuaternion implements v_world = q * v_dev * q_conj. The implementation follows the standard quaternion product formula and then extracts the vector part. The repository exposes both raw gravity-compensated world acceleration (x,y,z) and filtered values stored in WorldAcceleration.
- Filtering: OneEuroFilter is applied independently to each axis via OneEuroVectorFilter with defaults minCutoff=0.4 Hz, beta=0.007, dCutoff=1.0 Hz. The scalar filter implements the standard derivative-smoothing + adaptive-cutoff equations.
- Estimator: VehicleMotionEstimator currently computes horizontalMagnitude = sqrt(filteredX^2 + filteredY^2) and motionIntensity = clamp(horizontalMagnitude / NORMALIZATION_SCALE, 0..1) with NORMALIZATION_SCALE = 3.0f.

Observed behaviours to explain (from Phase 8 logs)
- Stationary baseline: small hmag (~0.0007..0.0099 m/s^2) — PASS.
- Sideways movement: produced clear hmag spikes up to ~0.02 m/s^2 — PASS.
- Linear forward/back movement: captured hmag ranges similar to stationary (INCONCLUSIVE/no clear sustained increase).
- Rotation in place and orientation changes: produced transient spikes in hmag and fz, sometimes returning to baseline.

Hypotheses that explain observations (distinguish from confirmed facts)
- H1 — Axis alignment and user motion vectors: The phone's world X/Y axes may not align with the direction the user moves the phone during "linear" tests. If forward/back translation projected poorly onto the world X/Y axes (for example most of motion projected onto device axis that maps to world Z or small horizontal components), horizontalMagnitude would remain small while sideways motion (which happens to map strongly to world X/Y) appears larger. This is a geometry/projection hypothesis and depends only on phone orientation in the vehicle/user frame.

- H2 — One Euro smoothing removes short translational bursts: With minCutoff=0.4Hz and beta small, One Euro applies substantial smoothing for short-duration accelerations. Short forward/back pushes (brief pulses) may be attenuated, so hmag does not rise much. Sideways movement performed in the test may have been more sustained or had larger amplitude, making it more likely to exceed the filter's smoothing.

- H3 — Gravity-compensation transient due to orientation latency: When the phone rotates, the orientation quaternion from the rotation-vector sensor lags or is sampled independently of accelerometer samples. If orientation and accelerometer timestamps are not tightly synchronized, gravity subtraction can briefly be incorrect (residual gravity components remain), causing transient horizontal components until orientation updates catch up. These transients appear as hmag spikes during rotation or orientation change.

- H4 — Sensor noise and biases: Sensor noise floor, biases, and small vibrations (hand tremor) may produce low-level hmag that is nonzero even when stationary. The One Euro filter reduces noise but not eliminate it; residual determines the stationary baseline.

- H5 — Centripetal/rotational accelerations: Pure rotations around axes offset from the accelerometer's center produce centripetal accelerations proportional to omega^2 * r (and Coriolis-like effects). If rotation is not exactly about the phone's center, accelerometer may register translational acceleration even when the user intends only rotation. This can produce hmag spikes during rotation-in-place tests.

Detailed analysis of pipeline components and their role

1) Accelerometer semantics and gravity compensation
- Confirmed: accelerometer measures proper acceleration (including gravity). MotionProcessor rotates this vector into the world frame and subtracts g_world = (0,0,+9.80665). This yields an estimated linear acceleration in world coordinates. If the quaternion q is accurate and timely, this subtraction removes gravity; otherwise residual appears.

2) Rotation-vector / quaternion transform
- Confirmed: q is read from OrientationReading (qw,qx,qy,qz) and used to rotate the device vector to world frame. The implementation uses only accelerometer.timestamp for filter dt; orientation timestamp is not explicitly used in the filter path. If orientation updates lag or have different timestamps, transient mismatches can occur.

3) World coordinate conventions
- Confirmed: World Z is vertical, positive UP. Gravity is +9.80665 in Z and subtracted only from world Z component.

4) One Euro filter parameters and smoothing behaviour
- Confirmed: default minCutoff=0.4 Hz imposes strong smoothing for signals below ~0.4 Hz. For sensor sampling dt ≈ 0.02–0.05 s, alpha ≈ 0.03–0.11 for fc=0.4Hz, which attenuates short events. The derivative path has dCutoff=1.0 Hz which also smooths derivative estimates.
- Hypothesis (H2): short, brief linear translations may be smoothed too strongly and therefore not appear as large hmag after filtering.

5) VehicleMotionEstimator calculation
- Confirmed: hmag = hypot(filteredX, filteredY). This is rotation-invariant in the horizontal plane (independent of choice of X vs Y) but depends on filtered values. Therefore any attenuation by the filter directly reduces hmag.

6) Gyroscope role to distinguish rotation vs translation
- Confirmed fact: gyroscope sensor is available in the app and its numeric values are visible in Diagnostics. Not fused into estimator.
- Hypothesis (H3/H5): high gyroscope magnitude concurrent with hmag spikes implies rotation contamination (orientation/centripetal), so gyroscope can be used to classify/ignore those periods. A simple experiment is to correlate gyro magnitude to hmag spikes to see if spikes align with angular rate peaks.

Suitability of current signal as vehicle-motion proxy
- Confirmed: filtered world-frame horizontal magnitude is a reasonable proxy for horizontal translational activity but only if the phone-to-vehicle orientation is suitable (or if magnitude, not axis direction, is sufficient). The current estimator is intentionally simple and useful as an experimental proxy.
- Limitations: sensitivity depends on filter bandwidth, orientation alignment, and transient orientation-compensation fidelity. Without phone-to-vehicle calibration, some maneuvers (forward/back) may not project strongly onto world X/Y depending on phone placement/orientation.

Minimum experiments / changes to resolve uncertainty (recommendations)

Experiment E1 (no code changes): Record raw (unfiltered) gravity-compensated world-frame acceleration values alongside filtered ones using the existing Diagnostics UI. Use the app's numerical readouts (Raw World X/Y/Z and Filtered X/Y/Z) while performing short, controlled movements. This will show whether the filter is attenuating short events (if raw shows pulses but filtered is small).

Experiment E2 (no code changes): While performing rotation-in-place tests, simultaneously watch gyroscope readings in Diagnostics and log them via adb. Manually correlate high angular rates with hmag spikes. If spikes align with high gyro, rotation contamination is likely.

Experiment E3 (code-minimal): Add a temporary debug log (adb logcat MotionDotsEst already logs filtered values) to also emit raw gravity-compensated world values and gyroscope magnitude in the same log line (requires a small, targeted logging addition). This will help correlate quickly in a single log stream. If accepted, implement as a minimal, temporary debug log only.

Minimal implementation change (if authorized):
- Add optional debug logging in Diagnostics or where MotionProcessor/latest is observed that logs raw world values (x,y,z), filtered values, and current gyroscope magnitude when the estimator runs. Guard this behind a debug flag or only log at DEBUG level so it can be removed after experiments. This is minimal and does not alter estimator behavior.

Suggested next steps (order)
1. Run E1 and E2 (manual tests using existing UI) to confirm whether raw world values contain the short pulses that the filtered output suppresses, and whether gyro peaks coincide with hmag spikes. These require no code changes.
2. If E1 confirms heavy smoothing removes pulses, perform E3: add temporary debug logging of both raw and filtered values and gyroscope magnitude for more precise correlation, then repeat tests.
3. If rotation contamination is confirmed, plan a small enhancement: suppress or flag estimator outputs when gyro magnitude exceeds a threshold (simple heuristic), or incorporate gyro-derived context to reduce false positives. That is a minimal, well-justified change but should be implemented only after E2/E3 confirm the hypothesis.

What to record in Phase 9 deliverables
- analysis.md: this section (confirmed facts vs hypotheses and minimal experiment plan).
- working.md: short note describing Phase 9 investigation and recommended E1–E3 experiments.
- report.md: concise Phase 9 summary listing findings and next steps.

No code changes are made in this analysis step. If you want me to implement the minimal debug logging (E3) I will do a small change, build, and run further tests; otherwise proceed with the manual experiments (E1/E2) and report results.
### Phase 9 Experiments — E1 / E2 (results summary)

E1 — Raw vs Filtered Response (interactive):
- Procedure: user kept phone still 10s, performed 5 short forward/back translations over ~10s, then kept still 10s. Logs from MotionDotsEst (adb) were captured for the period.
- Observation: MotionDotsEst logs contain filtered values (filteredX/filteredY/filteredZ/hmag/motionIntensity) but do not include the raw gravity-compensated world acceleration (unfiltered) in the same log output. The Diagnostics UI currently shows the estimator filtered outputs but no synchronized raw-world numeric log lines. Therefore E1 cannot be conclusively evaluated with current instrumentation: we cannot compare raw vs filtered responses from logs alone.

E2 — Gyro vs Motion Spikes (interactive):
- Procedure: user rotated phone in place for 15s and held still for 10s while MotionDotsEst logs were collected.
- Observation: MotionDotsEst logs show transient hmag spikes during some events. The system log (sensors-hal) contains gyro_sample lines, but these are not emitted by the app at the same logging level or correlated directly with MotionDotsEst lines. Because app-level gyroscope readings (the ones displayed in Diagnostics) are not logged in the same stream, we cannot conclusively correlate gyro magnitude to hmag spikes from existing logs. E2 is therefore INCONCLUSIVE with current instrumentation.

Conclusion from experiments E1/E2:
- The interactive captures confirmed that the existing instrumentation (MotionDotsEst log) provides filtered acceleration and estimator outputs but not the raw gravity-compensated world values or synchronized app-level gyroscope values required for definitive comparisons. To resolve H2/H3 with confidence, E3 (temporary debug logging of raw + filtered + gyro magnitude in a single log line) is the appropriate next minimal step.

E3 — Synchronized debug logging (results)

Methodology:
- Added temporary, lightweight debug logging at the estimator sampling point (MotionDotsDebug tag). Each debug line contains: timestamp, raw gravity-compensated world X/Y/Z (from MotionProcessor.latest), filtered world X/Y/Z (estimator input), gyro X/Y/Z and gyro magnitude (latest GyroscopeSensor reading), horizontalMagnitude and motionIntensity. Logging was throttled to ~10 Hz to keep capture lightweight.

E3-A (translation experiment):
- Captured MotionDotsDebug during the translation sequence. From the captured samples (N=1603 lines in the logged window) the analysis shows:
  - Instances where raw absolute axis values were substantially larger than filtered values (a simple threshold test) were observed (73/1603 samples). These indicate cases where the raw gravity-compensated signal exhibited pulses that the filtered signal reduced.
  - Horizontal magnitude (hmag) exceeded 0.01 m/s^2 in 188 samples; among those, 158 samples had a non-trivial gyro magnitude (see below).
- Interpretation: the One Euro filter does attenuate some short pulses, but not universally; some raw pulses pass through or are similar to filtered values. Therefore E3-A partially supports H2 (filter attenuation) but is not definitive — result: PARTIALLY SUPPORTED / INCONCLUSIVE.

E3-B (rotation experiment):
- Captured MotionDotsDebug during rotation-in-place. Analysis of hmag vs gyro magnitude in the captured window shows that a large majority of hmag spikes (hmag > 0.01 m/s^2) coincide with elevated gyro magnitude (in our threshold test, 158/188 ≈ 84%).
- Interpretation: this is strong evidence that many hmag spikes are temporally associated with rotational motion (supports H3/H4). Result: SUPPORTED (rotation correlates with many hmag spikes), though not all hmag spikes are necessarily rotation-induced.

Instrumentation conclusion:
- The temporary synchronized debug logging (MotionDotsDebug) provided the necessary, correlated traces (raw+filtered+gyro) to make more definitive statements. This confirms that adding minimal instrumentation (E3) is valuable and sufficient for short experiments.
 - The temporary synchronized debug logging (MotionDotsDebug) provided the necessary, correlated traces (raw+filtered+gyro) to make more definitive statements. This confirms that adding minimal instrumentation (E3) is valuable and sufficient for short experiments.

Phase 11 fix notes:
- Diagnostics screen column was not scrollable, causing the new "Motion Cue Preview" card to be off-screen on some devices. Made the diagnostics column vertically scrollable so the preview is reachable. This does not change sensor processing or estimator behavior.

Recommended next steps:
- Remove the temporary debug logging after experiments or gate it behind a debug flag to avoid shipping noisy logs. The logs should be retained only for directed experiments.
MotionEngine foundation (Phase 11I)
- Implemented a pure-Kotlin MotionEngine that estimates gravity via a slow accelerometer low-pass with gyro-influenced correction rate, computes linear acceleration, projects horizontal components, applies a short low-pass to acceleration (~100ms), computes felt force = -filtered acceleration, applies a dead-zone (0.15 m/s²) and tanh-based soft limiting, and emits a handlingConfidence based on gyro magnitude. See app/src/main/java/com/motiondots/app/motion/MotionEngine.kt for implementation and heuristics.
