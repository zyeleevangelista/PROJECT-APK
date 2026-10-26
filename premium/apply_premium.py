#!/usr/bin/env python3
"""Apply the maintained premium overrides to the original Android source archive."""
from pathlib import Path
import re
import shutil
import sys

project = Path(sys.argv[1])
repo = Path(__file__).resolve().parent.parent
java = project / "app/src/main/java/com/example/lockvideorecorder"
activity = java / "PremiumActivity.java"
shutil.copyfile(repo / "premium/PremiumActivity.java", activity)

manifest = project / "app/src/main/AndroidManifest.xml"
source = manifest.read_text()
assert 'android:name=".MainActivity"' in source
source = source.replace('android:name=".MainActivity"', 'android:name=".PremiumActivity"')
source = source.replace('android:label="Lock Video Recorder"', 'android:label="Lock Video Recorder Premium"')
manifest.write_text(source)

service = java / "RecordingService.java"
source = service.read_text()
source = source.replace("import java.util.Arrays;", "import java.util.ArrayList;\nimport java.util.Arrays;\nimport java.util.List;")
source = source.replace(
    'public static final String ACTION_STOP = "com.example.lockvideorecorder.STOP";',
    'public static final String ACTION_STOP = "com.example.lockvideorecorder.STOP";\n'
    '    public static final String ACTION_PREVIEW = "com.example.lockvideorecorder.PREVIEW";\n'
    '    public static final String EXTRA_PREVIEW_SURFACE = "preview_surface";'
)
source = source.replace(
    'private MediaRecorder recorder;',
    'private MediaRecorder recorder;\n'
    '    private Surface previewSurface;\n'
    '    private int sessionGeneration;'
)
anchor = '''        if (!ACTION_START.equals(action) || isActive) {'''
assert anchor in source
source = source.replace(anchor, '''        if (ACTION_PREVIEW.equals(action)) {
            final Surface surface;
            if (intent == null) surface = null;
            else if (Build.VERSION.SDK_INT >= 33)
                surface = intent.getParcelableExtra(EXTRA_PREVIEW_SURFACE, Surface.class);
            else
                surface = intent.getParcelableExtra(EXTRA_PREVIEW_SURFACE);
            if (cameraHandler != null) cameraHandler.post(() -> {
                previewSurface = surface;
                // Keep MediaRecorder running while the camera output session changes.
                if (isActive && !stopping && cameraDevice != null && recorderStarted)
                    configureSession();
            });
            return START_NOT_STICKY;
        }
''' + anchor)
source = source.replace("new Intent(this, MainActivity.class)", "new Intent(this, PremiumActivity.class)")
start = source.index("    private void configureSession() {")
end = source.index("    private void failCapture(String message)", start)
source = source[:start] + '''    private void configureSession() {
        if (stopping || cameraDevice == null || recorder == null) return;
        final int generation = ++sessionGeneration;
        // The preview surface is removed from the capture session when the app is
        // backgrounded; the MediaRecorder surface remains active throughout.
        if (session != null) {
            try { session.stopRepeating(); } catch (Exception ignored) { }
            try { session.close(); } catch (Exception ignored) { }
            session = null;
        }
        try {
            final Surface videoSurface = recorder.getSurface();
            final Surface livePreview = previewSurface != null && previewSurface.isValid()
                    ? previewSurface : null;
            final List<Surface> targets = new ArrayList<>();
            targets.add(videoSurface);

            final CaptureRequest.Builder request =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            request.addTarget(videoSurface);
            if (livePreview != null) {
                targets.add(livePreview);
                request.addTarget(livePreview);
            }
            request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            cameraDevice.createCaptureSession(targets,
                    new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(CameraCaptureSession created) {
                            if (stopping || generation != sessionGeneration) {
                                created.close();
                                return;
                            }
                            session = created;
                            try {
                                session.setRepeatingRequest(request.build(), null, cameraHandler);
                                if (!recorderStarted) {
                                    recorder.start();
                                    recorderStarted = true;
                                    hasStartedFrames = true;
                                    notifier.notify(ACTIVE_NOTIFICATION_ID,
                                        activeNotification("Recording video • Tap Stop to save"));
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Cannot start/update video session", ex);
                                if (livePreview != null && recorderStarted) {
                                    previewSurface = null;
                                    cameraHandler.post(() -> configureSession());
                                } else {
                                    failCapture("Recording session error");
                                }
                            }
                        }
                        @Override public void onConfigureFailed(CameraCaptureSession created) {
                            created.close();
                            if (stopping || generation != sessionGeneration) return;
                            if (livePreview != null) {
                                Log.w(TAG, "Preview output unsupported; continuing recording");
                                previewSurface = null;
                                cameraHandler.post(() -> configureSession());
                            } else {
                                failCapture("Camera session configuration failed");
                            }
                        }
                    }, cameraHandler);
        } catch (Exception ex) {
            Log.e(TAG, "Could not configure camera session", ex);
            if (previewSurface != null && recorderStarted) {
                previewSurface = null;
                cameraHandler.post(() -> configureSession());
            } else {
                failCapture("Camera session setup failed");
            }
        }
    }

''' + source[end:]
service.write_text(source)

app_gradle = project / "app/build.gradle"
app_gradle.write_text("""plugins {
    id 'com.android.application'
}

android {
    namespace 'com.example.lockvideorecorder'
    compileSdk 36

    defaultConfig {
        applicationId 'com.example.lockvideorecorder'
        minSdk 29
        targetSdk 36
        versionCode 2
        versionName '1.1.0'
    }

    signingConfigs {
        playUpload {
            def path = System.getenv('LVR_KEYSTORE_PATH')
            if (path) {
                storeFile file(path)
                storePassword System.getenv('LVR_KEYSTORE_PASSWORD')
                keyAlias System.getenv('LVR_KEY_ALIAS')
                keyPassword System.getenv('LVR_KEY_PASSWORD')
            }
        }
    }
    buildTypes {
        release {
            // The upload certificate must remain stable across Play Store updates.
            if (System.getenv('LVR_KEYSTORE_PATH')) {
                signingConfig signingConfigs.playUpload
            }
            minifyEnabled false
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}
""")
(project / "build.gradle").write_text("""plugins {
    id 'com.android.application' version '8.13.2' apply false
}
""")
print("Applied premium UI, camera preview session handoff, target SDK 36, and signing config.")
