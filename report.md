# MotionDots - Phase Reports

## Phase 1 - Project Foundation
Status: Completed

Known report:
- Clean Android project created.
- Kotlin + Jetpack Compose configured.
- Gradle wrapper created.
- Android SDK configured using Homebrew command-line tools.
- APK build verified.
- Git commit/push completed.
- Final commit: 5fab239.

## Phase 2 - UI Foundation
Status: Completed

Known report:
- Two-tab Compose UI created: Home and Diagnostics.
- Home contains MotionDots status/control UI and settings placeholders.
- Diagnostics contains Accelerometer, Gyroscope and Processed Motion graph areas.
- Initial graphs used mock data.
- Build verified.
- Git commits: 08ab4b8 and 1536830.

## Phase 3 - Accelerometer Integration
Status: Completed

Known report:
- Raw accelerometer integrated.
- Live X/Y/Z values and graph added.
- Physical Android device tested successfully.
- Sensor lifecycle handled.
- Build verified.
- Git commit: 5dfec69.

## Phase 4 - Gyroscope Integration
Status: Completed

Known report:
- Raw gyroscope integrated.
- Live X/Y/Z values and graph added.
- Physical Android device tested successfully.
- Sensor lifecycle handled.
- Build verified.
- Git commit: d812e4b.

## Phase 5 - Orientation Foundation
Status: Completed

Known report:
- Android rotation-vector sensor integrated.
- Quaternion and roll/pitch/yaw exposed.
- Orientation diagnostics added.
- Physical Android device tested successfully.
- Build verified.
- Git commit: e6a855c.

## Phase 6 - Gravity Compensation and World-Frame Acceleration
Status: Completed

Known report:
- Accelerometer transformed from device frame to world frame using quaternion rotation.
- Gravity compensation implemented.
- World X/Y/Z linear acceleration exposed.
- Stationary and different-orientation tests performed on physical device.
- Build verified.
- Git commit: 1be2295.

## Phase 7 - Motion Signal Filtering
Status: VERIFIED

Facts observed in the repository (code + build):
- One Euro filter implementation exists: app/src/main/java/com/motiondots/app/sensor/MotionProcessor.kt defines OneEuroFilter and OneEuroVectorFilter.
- The filter is connected to MotionProcessor: MotionProcessor constructs a OneEuroVectorFilter and calls filter(...) when recomputing world-frame linear acceleration.
- X/Y/Z are filtered independently: OneEuroVectorFilter instantiates three OneEuroFilter instances (fx, fy, fz) and filters each axis separately.
- Raw and filtered acceleration values are both available: WorldAcceleration holds raw components (x,y,z) and optional filteredX/filteredY/filteredZ fields which are populated by MotionProcessor.
- Filtered values are displayed in Diagnostics: app/src/main/java/com/motiondots/app/ui/MotionDotsApp.kt shows numeric readouts for "Filtered X/Y/Z" and draws filtered traces in the "Processed motion" Canvas.
- Filter parameters are present in code with documented defaults: OneEuroFilter constructor and OneEuroVectorFilter default to minCutoff=0.4 Hz, beta=0.007, dCutoff=1.0 Hz (documented inline as comments).
- Build result: `./gradlew assembleDebug` completed successfully and produced app/build/outputs/apk/debug/app-debug.apk.
- Current Git commit (HEAD): ba8f199 "Phase 7: Add motion signal filtering".

Notes (what is missing from documentation):
- analysis.md does not include Phase 7 scientific documentation or equations for the One Euro filter; analysis.md remains at Phase 6 explanatory content.
- working.md has no Phase 7 implementation entry; working.md stops at Phase 6.

## Phase 8 - Motion Analysis
Status: Not Started

## Phase 8 - Vehicle Motion Estimation
Status: In Progress (implemented, partially validated)

Objective:
- Create a simple, experimental vehicle-motion estimation layer that derives a motion-intensity proxy from filtered world-frame acceleration.

Implementation (facts):
- A VehicleMotionEstimator component was added at app/src/main/java/com/motiondots/app/estimator/VehicleMotionEstimator.kt. It consumes filtered world-frame acceleration (WorldAcceleration) and computes:
  - filtered_world_x, filtered_world_y, filtered_world_z (m/s^2)
  - horizontal_x, horizontal_y
  - horizontal_magnitude = sqrt(x^2 + y^2)
  - motion_intensity = clamp(horizontal_magnitude / 3.0, 0..1) (heuristic normalization)
- Diagnostics UI updated (app/src/main/java/com/motiondots/app/ui/MotionDotsApp.kt) to replace the previous "Processed motion" section with "Vehicle motion estimate (experimental)", showing numeric readouts for filtered components, horizontal magnitude, motion intensity, and a small graph.

Tests performed:
- Code build: `./gradlew assembleDebug` succeeded and produced app/build/outputs/apk/debug/app-debug.apk.
- Physical-device stationary baseline: Performed manually with a connected Android device. The estimator emitted logs (MotionDotsEst) observed via adb logcat showing filtered world X/Y remained small; horizontalMagnitude (hmag) was approximately 0.005–0.016 m/s² and motionIntensity approximately 0.002–0.005 during the stationary period. Other physical tests were not performed in this session.

Physical validation:

- Stationary baseline: PASS — estimator horizontal magnitude and motion intensity remained near zero while the phone was stationary.
- Phone movement: NOT TESTED / NOT RECORDED.
- Phone rotation (in-place): NOT TESTED / NOT RECORDED.
- Different orientations: NOT TESTED / NOT RECORDED.
- Real vehicle (passenger): NOT TESTED / NOT RECORDED.

Overall validation status: PARTIAL — stationary baseline observed, remaining tests pending manual validation.

Limitations:
- The estimator is intentionally simple and experimental. It does not map phone axes to vehicle axes, does not perform dead-reckoning, and does not fuse gyroscope data into the translational estimate.
- Motion intensity uses a heuristic normalization scale (3.0 m/s^2) and should be tuned with physical tests.

Build result:
- BUILD SUCCESSFUL; APK path: app/build/outputs/apk/debug/app-debug.apk

Git commit:
- Current commit contains Phase 8 implementation: (will appear in git log) "Phase 8: Add vehicle motion estimation"
