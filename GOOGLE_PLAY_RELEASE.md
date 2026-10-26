# Google Play release checklist — Lock Video Recorder Premium

**Current state:** premium APK compiled in GitHub Actions. Google Play release requires a signed Android App Bundle (AAB), a developer-controlled upload key, Play Console access, listings/assets, review and real-device testing.

## App identity

- Google Play listing name: Lock Video Recorder Premium
- Application ID (immutable after publication): `com.zyeleevangelista.lockvideorecorder`
- Version: `1.1.0` / versionCode 2
- Minimum OS: Android 10 / API 29
- Target OS: Android 16 / API 36
- Distribution: **Paid** download, no advertising or additional in-app purchase. Set the paid price in Play Console before the first public release. Play Console does not let you change a previously free app to paid.
- Privacy: https://github.com/zyeleevangelista/PROJECT-APK/blob/main/PRIVACY.md

## Create the private upload signing key (on your own computer)

Keep the keystore **outside** the GitHub repository. Back it up in a secure place. Do not share the key, its password, or your Play Console credentials in chat.

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore lvr-upload.jks -alias lvr-upload -keyalg RSA -keysize 3072 -validity 10000
```

You can use the same chosen password for both prompts, if desired. In a local PowerShell terminal, convert the file to base64:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ".\lvr-upload.jks"))) | Set-Clipboard
```

Add these GitHub Actions secrets in **Repository → Settings → Secrets and variables → Actions**:

- `LVR_KEYSTORE_BASE64`: pasted base64 from your clipboard
- `LVR_KEYSTORE_PASSWORD`: keystore password
- `LVR_KEY_ALIAS`: `lvr-upload`
- `LVR_KEY_PASSWORD`: key password

Once configured, the workflow builds `LockVideoRecorder-Premium-Play-AAB` as well as a debug test APK. Upload the signed release AAB, **not the debug APK**, to a Google Play internal testing track, and enroll in Play App Signing. Retain your upload key permanently for updates.

## Required in Play Console

1. Register and verify the owner account, including Google's one-time developer registration fee if needed.
2. Create an **App**, select **Paid** and provide a real developer support email.
3. Complete the privacy policy, **Contains ads: No**, Data safety and content-rating questionnaires accurately. The app itself has no internet permission and saves media locally.
4. Describe and demonstrate the **camera** foreground service, optional **microphone** foreground service, notification, and user-initiated lock-screen recording, including the impact if Android interrupts the session. Play requests a video demonstrating foreground service behavior.
5. Provide a 512×512 Play Store icon, 1024×500 feature graphic, phone screenshots, suitable descriptions, category and contact details.
6. Upload the signed AAB to internal testing, install through Google Play, and test on real phones: start/stop, rear/front cameras, microphone consent, angle preview, lock/unlock, storage and media playback.
7. For newly created personal developer accounts, complete closed testing with at least 12 opted-in testers for 14 continuous days and obtain production access.
8. Set price and supported countries, send for Google review, then publish after approval.

## Proposed Play listing

**Short description:** Ad-free video recorder with live camera preview and screen-lock recording.

**Full description:**

Lock Video Recorder Premium lets you record with your Android phone while keeping control of your footage. See your camera angle in a live preview while the app is open, then lock your screen to continue recording where supported by your device.

Choose rear or front camera, switch optional microphone audio on or off, and stop the recording whenever you want. Finished MP4 videos are saved locally in Movies / LockVideoRecorder.

No advertising, accounts, tracking, uploads, cloud subscription or in-app purchases. Camera recording is started by you and remains visible through a persistent Android notification with Stop & save. Android camera/microphone indicators are preserved.

Important: Background camera behavior depends on Android and device manufacturer restrictions. Recordings can be interrupted by camera policies, battery management or other apps. Recording automatically stops at approximately 60 minutes or 2 GB.

## Before public release

- Real-device validation of preview angle and recording continuation after lock is **not yet completed**.
- Switch this GitHub repository **private** before charging for the app. At present its source ZIP and debug APK artifacts are publicly accessible.
- Replace platform default icon with distinctive app and Play Store artwork.
- Set a real Play Console contact email and review privacy-policy wording against actual app behavior.
