# Build Instructions for ARM64 APK

## Prerequisites
- Flutter SDK installed (stable channel recommended)
- Java JDK 17 or higher
- Android SDK with NDK

## Building Locally

1. **Install Flutter** (if not already installed):
   ```bash
   # Follow instructions at https://docs.flutter.dev/get-started/install
   # Or use snap on Linux:
   sudo snap install flutter --classic
   ```

2. **Get dependencies**:
   ```bash
   cd /path/to/message-mirror
   flutter pub get
   ```

3. **Build ARM64 APK**:
   ```bash
   # Build split-per-ABI APKs (recommended for smaller size)
   flutter build apk --release --split-per-abi -t lib/main.dart
   
   # The ARM64 APK will be located at:
   # build/app/outputs/flutter-apk/app-arm64-v8a-release.apk
   ```

   Alternatively, build a universal APK (larger size, works on all architectures):
   ```bash
   flutter build apk --release -t lib/main.dart
   
   # Output: build/app/outputs/flutter-apk/app-release.apk
   ```

## Building via GitHub Actions

The repository has an automated build workflow that you can trigger:

1. **Trigger the workflow**:
   - Go to the repository's Actions tab
   - Select "Android Release" workflow
   - Click "Run workflow" (requires write permissions)

2. **Or push a version tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **Download the artifacts**:
   - The workflow will build split-per-ABI APKs for:
     - armeabi-v7a (32-bit ARM)
     - arm64-v8a (64-bit ARM) ← **This is what you need**
     - x86_64 (64-bit Intel/AMD)
   - Download from the workflow run artifacts or from the GitHub Release (if tagged)

## Output Files

After a successful build, you'll get:
- `message-mirror-{version}-arm64-v8a.apk` - For modern ARM64 devices
- `message-mirror-{version}-armeabi-v7a.apk` - For older 32-bit ARM devices
- `message-mirror-{version}-x86_64.apk` - For x86 devices (emulators, tablets)

## Verifying the Fix

After installing the APK:
1. Open the app
2. Grant necessary permissions
3. Tap "Start Monitoring Service"
4. The service should start without crashing
5. Check the Logs screen - you should see "AlwaysOnService startForeground" or a descriptive error message if there are any issues

## Changes in This Version

- Added `FOREGROUND_SERVICE_DATA_SYNC` permission to support Android 14+ (targetSdk 34+)
- Added exception handling in `startAsForeground()` to prevent crashes if permission is denied
- Improved error logging for debugging foreground service issues
