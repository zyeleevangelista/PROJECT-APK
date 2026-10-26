# Lock Video Recorder — Android

A native Android application written in Java (no third-party SDKs) for **user-initiated, visible video recording** that is designed to continue after the phone screen is locked, subject to manufacturer and Android restrictions.

## Features
- Rear/front camera selector
- Optional microphone recording (off by default)
- MP4 (H.264 + optional AAC)
- Standard MediaStore video files at `Movies/LockVideoRecorder`
- Foreground service with a visible, ongoing notification and a **Stop & save** action
- Keeps the CPU active with a time-limited partial wake lock while recording
- Automatically stops after 60 minutes or about 2GB, whichever comes first
- No internet permission, no uploads, no analytics, no background autostart, no stealth features

## Compatibility
- Android 10+ (minSdk 29), target SDK 35.
- **You MUST press Start while the app is visible and phone unlocked.** Android 14+ will not allow arbitrary initiation of camera/microphone foreground services after the app has gone into the background.
- A foreground camera service can remain active after the phone locks, but device/OEM restrictions, battery optimization, camera policies, or other camera apps can interrupt recording.
- The operating system may show the camera/microphone privacy indicator.
- Battery and storage consumption can be substantial. Test on your device before relying on it.

## Get an APK through GitHub Actions (no local Android SDK needed)
1. Create a **new private GitHub repository** in your account.
2. Upload all files and folders from this project (including `.github/workflows/build-apk.yml`) to the root of that repository.
3. Open the repository's **Actions** tab and choose **Build Android APK**, then **Run workflow** (or let it trigger on a push to `main`).
4. When the run succeeds, download the **LockVideoRecorder-debug-APK** artifact.
5. Unzip it. The actual installable APK is `app-debug.apk`. Transfer it to the Android phone and install it. Android may prompt you to allow installs from that file manager.

## Build on your computer using Android Studio
1. Install Android Studio and Android SDK Platform 35, Android SDK Build Tools 35.0.0, JDK 17, and Gradle 8.9.
2. Open this project in Android Studio. For a project without a wrapper JAR, configure a local Gradle 8.9 distribution if prompted.
3. Build > Build APK(s) (or run `gradle :app:assembleDebug` from a terminal with Android SDK environment configured).
4. Install `app/build/outputs/apk/debug/app-debug.apk` on your phone.

This archive is the **Android Studio source project, not a precompiled APK**.

## Use
1. Open the app and select rear/front camera. Turn on microphone audio if desired.
2. Tap **Start video recording**, grant camera/notification (and optional mic) permission.
3. Confirm the recording notification says **Recording video**.
4. Lock the device with the power button. The recorder should continue if supported by the phone's Android build.
5. Unlock, open the app or notification, and choose **Stop & save**.
6. Find your recording in Gallery/Photos/Files under `Movies/LockVideoRecorder`.

## Security and privacy
This app is for recording with your own phone and the knowledge/consent of everyone as required by local law. The persistent foreground notification and Android privacy indicators are deliberately not suppressed. There is no remote server, internet access, disguised icon, or trigger to record without opening the app.

## Developer notes
- package / applicationId: `com.example.lockvideorecorder`
- foreground service types: camera and optionally microphone
- storage: MediaStore, with `IS_PENDING` until finalized
- screen-off behavior: camera2 + MediaRecorder Surface + ongoing camera foreground service + partial wake lock
- phone hardware is not available in this build environment; device behavior must be tested.
