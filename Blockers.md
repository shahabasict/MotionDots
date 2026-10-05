## Blockers


## Blocker #001 - Android SDK not found
### Problem
Build failed when running `./gradlew assembleDebug` with the error:

```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/Users/shahabas/Documents/GitHub/MotionDots/local.properties'.
```

### Investigation
I ran the Gradle build which failed early during task dependency resolution. The environment has Gradle installed and I generated the Gradle wrapper successfully, but there is no Android SDK path configured.

I checked common SDK locations and environment variables; none were set on this machine.

### Root Cause
The local development environment does not have an Android SDK installed or the SDK path is not configured via ANDROID_HOME / ANDROID_SDK_ROOT or a `local.properties` file.

### Solution
On your machine, either install the Android SDK (via Android Studio or sdkmanager) and set ANDROID_SDK_ROOT/ANDROID_HOME, or create a `local.properties` in the project root with the line:

```
sdk.dir=/path/to/Android/Sdk
```

Replace `/path/to/Android/Sdk` with the real SDK path on your machine.

### Status
Blocked — resolved. Installed Android command-line tools via Homebrew and used sdkmanager to install platform-tools, platforms;android-33, and build-tools;33.0.2. Added `local.properties` pointing to the SDK.

## Blocker #002 - AndroidX property not set
### Problem
Build failed with:

```
Configuration `:app:debugRuntimeClasspath` contains AndroidX dependencies, but the `android.useAndroidX` property is not enabled
```

### Investigation
Gradle detected AndroidX libraries in dependencies (Compose, activity-compose) but `gradle.properties` did not enable AndroidX.

### Root Cause
Missing `android.useAndroidX=true` in `gradle.properties`.

### Solution
Added `gradle.properties` with `android.useAndroidX=true` and `android.enableJetifier=true`.

### Status
Resolved.

## Blocker #003 - Physical-device test access
### Problem
Physical-device motion tests for Phase 8 initially were not executed from the automated environment because a connected Android device was not available to the agent.

### Investigation
The build step (`./gradlew assembleDebug`) completed successfully and produced an APK. Subsequently a physical Android device was connected and used to perform a stationary baseline test observed via adb logcat.

### Solution / Workaround
Manual device testing steps (used and recommended):

1. Install the APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Launch the app on the phone, open Diagnostics, and observe the "Vehicle motion estimate (experimental)" card.
3. For this session, TEST A (Stationary baseline) was executed and logs captured via adb logcat.

### Status
Status: Resolved for interactive testing — guided physical validation tests (1–5) were executed using a connected Android device; stationary baseline and sideways movement produced clear results. TEST 6 (real vehicle) was skipped because the user was not a passenger. No remaining blockers for running local interactive tests.
