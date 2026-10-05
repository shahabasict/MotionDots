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
