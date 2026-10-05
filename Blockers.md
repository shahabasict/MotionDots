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
