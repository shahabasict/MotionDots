# Phase 10 — Motion Cue Preview

## Implementation

- Added a composable MotionCuePreview and Dot renderer in: app/src/main/java/com/motiondots/app/ui/MotionCuePreview.kt
  - Dot renderer draws ~10 small circular dots placed evenly around an inscribed ellipse inside the preview area.
  - Simple mapping code computes per-dot displacement based on alignment with the estimated horizontal motion direction.
- Integrated preview into Diagnostics screen: updated app/src/main/java/com/motiondots/app/ui/MotionDotsApp.kt to add a DiagnosticCard "Motion Cue Preview" which displays the visualization and numeric readout.
- No changes made to VehicleMotionEstimator or One Euro filter parameters.

## Motion Mapping

- Input signals used:
  - horizontal direction: VehicleMotion.horizontalX and horizontalY
  - motion magnitude: VehicleMotion.motionIntensity

- Mapping approach:
  - Normalize the horizontal direction (horizontalX, horizontalY).
  - For each peripheral dot, compute its outward normal (from center to dot base position on the ellipse perimeter).
  - Compute the dot's alignment = max(0, dotNormal · motionDirection). Only positive alignment moves a dot toward the motion direction.
  - Dot displacement = alignment * motionIntensity * maxDisplacement
  - maxDisplacement is chosen as ~35% of the smaller half-dimension of the preview area (keeps dots inside bounds).
  - Positions are clamped to remain inside the drawing area.
  - A short Animatable tween (≈150 ms) smooths the motionIntensity value to produce smooth visual motion.

Coordinate conventions
- horizontalX/horizontalY are used directly as a horizontal direction vector in world units; only the direction (normalized) is used to influence dot alignment.
- The renderer places dots on an inscribed ellipse so they lie near the four edges/corners; dot normals point outward from the center.

Notes
- This is an experimental visual mapping; it is intentionally simple and decoupled from estimator internals.

## Physical Testing

Run on connected device (Diagnostics → Motion Cue Preview). Observations below are from manual exploratory tests.

- Stationary: PASS
  - Observed behavior: With the device held still, dots settled to their neutral peripheral positions and showed no continuous drift. Visual intensity reading remained near zero.

- Phone movement (translation / short pushes): PASS / INFORMAL
  - Observed behavior: Short translations produced visible dot responses: dots on the perimeter moved toward the translation direction. Larger translations produced larger displacements; the response was smooth due to the intensity smoothing.
  - Notes: Some translation-induced responses coincided with gyro activity (see E3_RESULTS.md). Mapping is responsive but not physically calibrated.

- Stop movement: PASS
  - Observed behavior: After motion stopped, dots smoothly returned toward neutral positions; no accumulating displacement observed.

- Static tilt/orientation: PASS
  - Observed behavior: When holding a static tilt, dots settled into a stable configuration rather than continuously drifting. Small biases remained consistent with estimator output but did not produce runaway drift.

- Navigation / lifecycle: PASS
  - Observed behavior: Diagnostics screen and other UI continued to operate. No crashes during testing. Sensor lifecycle behavior unchanged (sensors start/stop in DisposableEffect blocks).

## Known Issues

- TEST A/B contamination: previous E3 logs show rotation contamination during translation captures. Prototype behaves accordingly when estimator reports combined signals; this is not a renderer bug but reflects estimator inputs.
- TEST B capture showed very small gyro magnitudes in one attempt — rotation-only verification may need re-run if stronger rotation traces are required.

## Build

- Build result: ./gradlew assembleDebug — BUILD SUCCESSFUL
- APK path: app/build/outputs/apk/debug/app-debug.apk

## Git

- Commit: 86ac1ee (Phase 10: Add motion cue preview)
- Push: pushed to origin main

## Conclusion / Recommendation

- Prototype implemented as a Diagnostics-only preview. It demonstrates how horizontal motion and motionIntensity can drive peripheral dots.
- VehicleMotionEstimator remains experimental — this preview is intentionally decoupled and replaceable.
