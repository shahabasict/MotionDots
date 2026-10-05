# MotionDots - Working Log

## Phase 1 - Project Foundation

What was created:

- Minimal Android app module (Kotlin + Jetpack Compose) showing a single screen with the text "MotionDots".
- Gradle build files and wrapper generated.
- .gitignore, analysis.md, working.md, Blockers.md (this file).

Build verification:

- Executed `./gradlew assembleDebug` and verified the debug APK exists under app/build/outputs/apk/debug/.

## Phase 2 - UI Foundation

What was created:

- Two-tab UI (Home, Diagnostics) using Jetpack Compose and a bottom navigation bar.
- Home screen with branding, an ON/OFF state card, a primary toggle control (visual only), a short status indicator, and settings placeholders for Dot count, Dot size, Dot opacity, Dot color, Edge distance, and Motion sensitivity.
- Diagnostics screen with diagnostic cards for Accelerometer, Gyroscope, and Processed motion; each contains a mocked simple line graph to represent future live data.

Build verification:

- Executed `./gradlew assembleDebug` successfully and verified `app/build/outputs/apk/debug/app-debug.apk` exists.

## Phase 3 - Accelerometer Integration

What was created:

- AccelerometerSensor component (app/src/main/java/com/motiondots/app/sensor/AccelerometerSensor.kt) that exposes raw X/Y/Z readings over a StateFlow. It handles registration/unregistration with SensorManager and detects missing hardware.
- Diagnostics screen updated to start/stop accelerometer listening while visible and to render a live accelerometer graph (X/Y/Z) using a small in-memory buffer for recent samples.
- Numeric readout of current X/Y/Z values placed above the accelerometer graph to help physical-device testing.

Build & test verification:

- Built APK with `./gradlew assembleDebug`.
- Installed APK on a connected Android phone via `adb install -r` and verified that the Diagnostics screen shows live accelerometer data and numerical X/Y/Z values while the device is moved.

## Phase 4 - Gyroscope Integration

What was created:

- GyroscopeSensor component (app/src/main/java/com/motiondots/app/sensor/GyroscopeSensor.kt) that exposes raw X/Y/Z angular velocity readings over a StateFlow. It handles registration/unregistration with SensorManager and detects missing hardware.
- Diagnostics screen updated to start/stop gyroscope listening while visible and to render a live gyroscope graph (X/Y/Z) using a small in-memory buffer for recent samples.
- Numeric readout of current gyroscope X/Y/Z values placed above the gyroscope graph to help physical-device testing.

Build & test verification:

- Built APK with `./gradlew assembleDebug`.
- Installed APK on a connected Android phone via `adb install -r` and verified that the Diagnostics screen shows live gyroscope data and numerical X/Y/Z values while the device is rotated.

## Phase 6 - Gravity Compensation and World-Frame Acceleration

What was created:

- MotionProcessor component (app/src/main/java/com/motiondots/app/sensor/MotionProcessor.kt) that consumes AccelerometerReading and OrientationReading StateFlows and produces gravity-compensated world-frame acceleration output (WorldAcceleration).
- Diagnostics updated: "Processed motion" graph now displays world-frame gravity-compensated acceleration (World X/Y/Z) with numeric readouts.

Coordinate convention:
- World Z is vertical and positive UP. World X/Y are horizontal.

Build & test verification:

- Built APK with `./gradlew assembleDebug` and installed on a connected phone.
- Tested stationary and moving orientations: gravity-compensated horizontal acceleration is near zero when device is stationary across different orientations; Z value reflects gravity-subtracted vertical component.

## Phase 7 - Motion Signal Filtering

What was created:

- One Euro filter implementation (app/src/main/java/com/motiondots/app/sensor/MotionProcessor.kt). The scalar OneEuroFilter implements the standard derivative-smoothing + adaptive-cutoff equations and is wrapped by OneEuroVectorFilter to filter X/Y/Z independently.
- MotionProcessor now computes gravity-compensated world-frame acceleration and applies the One Euro vector filter, storing both raw (gravity-compensated) and filtered values in WorldAcceleration.

Build & verification:

- Built APK with `./gradlew assembleDebug`. Diagnostics UI displays filtered traces and numeric readouts. The One Euro defaults are minCutoff=0.4 Hz, beta=0.007, dCutoff=1.0 Hz.

## Phase 8 - Vehicle Motion Estimation

What will be created:

- A dedicated estimator component that consumes the filtered world-frame acceleration and produces a small set of vehicle-motion signals (horizontal magnitude, horizontal components, vertical component, motion intensity).
- Diagnostics will be updated to show filtered world X/Y/Z, horizontal acceleration magnitude, and a motion intensity metric. Gyroscope and orientation remain available for context.

Planned tests:

1. Stationary phone in a stationary environment.
2. Move the phone manually while keeping orientation approximately fixed.
3. Rotate the phone without translating it.
4. Move the phone in different directions.
5. If practical, test as passenger in a vehicle.

Limitations and notes:

- The Phase 8 estimator is experimental and assumes no fixed relationship between phone and vehicle axes. It uses a heuristic normalization for motion intensity and does not perform velocity dead-reckoning or vehicle-frame calibration.
- Physical tests are required to tune normalization and to determine thresholds for use in later phases.

Build result:

- Will run `./gradlew assembleDebug` after implementation to verify the APK.
