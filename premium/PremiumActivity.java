package com.example.lockvideorecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Paid, ad-free recorder UI. Camera frames are never uploaded or analyzed. */
public class PremiumActivity extends Activity {
    private static final int PERMISSION_REQUEST = 250;
    private static final int REAR = 301;
    private static final int FRONT = 302;
    private static final int DARK = Color.rgb(11, 17, 27);
    private static final int PANEL = Color.rgb(24, 34, 49);
    private static final int WHITE = Color.rgb(246, 248, 252);
    private static final int SUBTLE = Color.rgb(163, 178, 195);
    private static final int BLUE = Color.rgb(83, 174, 245);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextureView cameraPreview;
    private Surface previewSurface;
    private boolean previewAttached;
    private RadioGroup lensGroup;
    private CheckBox audioBox;
    private TextView status;
    private TextView previewHint;
    private Button action;
    private boolean visible;

    private final Runnable statusLoop = new Runnable() {
        @Override public void run() {
            syncStatus();
            if (visible) handler.postDelayed(this, 500);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        createUI();
    }

    private void createUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(DARK);
        scroll.setFillViewport(true);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(column);

        column.addView(text("PREMIUM  •  NO ADS", 12, BLUE, Typeface.BOLD));
        TextView title = text("Lock Video Recorder", 27, WHITE, Typeface.BOLD);
        LinearLayout.LayoutParams headParams = new LinearLayout.LayoutParams(-1, -2);
        headParams.topMargin = dp(8);
        column.addView(title, headParams);

        TextView intro = text("Frame your shot with a live preview while recording. When you lock the phone, the app keeps saving video where supported.", 14, SUBTLE, Typeface.NORMAL);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.topMargin = dp(10);
        introParams.bottomMargin = dp(22);
        column.addView(intro, introParams);

        FrameLayout previewCard = new FrameLayout(this);
        previewCard.setBackground(round(Color.BLACK, 18));
        previewCard.setClipToOutline(true);
        int previewHeight = (int) ((getResources().getDisplayMetrics().widthPixels - dp(40)) * 9f / 16f);
        cameraPreview = new TextureView(this);
        previewCard.addView(cameraPreview, new FrameLayout.LayoutParams(-1, -1));
        cameraPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                syncPreview();
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                detachPreview();
                return true;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
        });
        previewHint = text("LIVE CAMERA PREVIEW\nStart recording to see your camera angle", 14, WHITE, Typeface.NORMAL);
        previewHint.setGravity(Gravity.CENTER);
        previewHint.setPadding(dp(22), dp(10), dp(22), dp(10));
        previewHint.setBackgroundColor(Color.rgb(18, 24, 35));
        previewCard.addView(previewHint, new FrameLayout.LayoutParams(-1, -1));
        column.addView(previewCard, new LinearLayout.LayoutParams(-1, Math.max(previewHeight, dp(175))));

        TextView advisory = text("Preview is shown while the app is open. Recording continues without the preview when the screen locks.", 12, SUBTLE, Typeface.NORMAL);
        LinearLayout.LayoutParams advisoryParams = new LinearLayout.LayoutParams(-1, -2);
        advisoryParams.topMargin = dp(10);
        advisoryParams.bottomMargin = dp(20);
        column.addView(advisory, advisoryParams);

        LinearLayout config = new LinearLayout(this);
        config.setOrientation(LinearLayout.VERTICAL);
        config.setPadding(dp(18), dp(18), dp(18), dp(18));
        config.setBackground(round(PANEL, 16));
        column.addView(config, new LinearLayout.LayoutParams(-1, -2));
        config.addView(text("RECORDING STATUS", 12, SUBTLE, Typeface.BOLD));
        status = text("Ready to record", 17, WHITE, Typeface.BOLD);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(7);
        statusParams.bottomMargin = dp(19);
        config.addView(status, statusParams);

        config.addView(text("CAMERA", 12, SUBTLE, Typeface.BOLD));
        lensGroup = new RadioGroup(this);
        lensGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rear = radio(REAR, "Rear");
        RadioButton front = radio(FRONT, "Front");
        lensGroup.addView(rear);
        lensGroup.addView(front);
        lensGroup.check(REAR);
        LinearLayout.LayoutParams lensParams = new LinearLayout.LayoutParams(-1, -2);
        lensParams.topMargin = dp(8);
        config.addView(lensGroup, lensParams);

        audioBox = new CheckBox(this);
        audioBox.setText("Include microphone audio");
        audioBox.setTextSize(15);
        audioBox.setTextColor(WHITE);
        audioBox.setButtonTintList(ColorStateList.valueOf(BLUE));
        LinearLayout.LayoutParams audioParams = new LinearLayout.LayoutParams(-1, -2);
        audioParams.topMargin = dp(12);
        config.addView(audioBox, audioParams);

        action = new Button(this);
        action.setTextSize(17);
        action.setAllCaps(false);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(58));
        actionParams.topMargin = dp(20);
        column.addView(action, actionParams);
        action.setOnClickListener(v -> {
            if (RecordingService.isActive) {
                detachPreview();
                Intent stop = new Intent(this, RecordingService.class);
                stop.setAction(RecordingService.ACTION_STOP);
                startService(stop);
                status.setText("Finishing video…");
            } else {
                requestRecording();
            }
        });

        Button gallery = new Button(this);
        gallery.setAllCaps(false);
        gallery.setText("Open saved videos");
        gallery.setTextColor(WHITE);
        gallery.setBackground(round(PANEL, 12));
        LinearLayout.LayoutParams galleryParams = new LinearLayout.LayoutParams(-1, dp(54));
        galleryParams.topMargin = dp(10);
        column.addView(gallery, galleryParams);
        gallery.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Saved in Movies / LockVideoRecorder", Toast.LENGTH_LONG).show();
            }
        });

        TextView footer = text("All videos stay on your phone. No ads, accounts, tracking, analytics or uploads. Recording always shows a notification with a Stop & save action.", 12, SUBTLE, Typeface.NORMAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(22);
        column.addView(footer, footerParams);
        TextView policy = text("Read privacy policy", 13, BLUE, Typeface.BOLD);
        LinearLayout.LayoutParams policyParams = new LinearLayout.LayoutParams(-1, dp(40));
        policyParams.topMargin = dp(9);
        column.addView(policy, policyParams);
        policy.setOnClickListener(v -> {
            Intent viewPolicy = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/zyeleevangelista/PROJECT-APK/blob/main/PRIVACY.md"));
            try { startActivity(viewPolicy); }
            catch (Exception e) { Toast.makeText(this, "Privacy policy available on GitHub", Toast.LENGTH_LONG).show(); }
        });
        setContentView(scroll);
        syncStatus();
    }

    private void syncStatus() {
        if (action == null) return;
        boolean recording = RecordingService.isActive;
        action.setText(recording ? "Stop & save recording" : "Start video recording");
        action.setTextColor(recording ? WHITE : DARK);
        action.setBackground(round(recording ? Color.rgb(216, 65, 77) : BLUE, 12));
        status.setText(recording
            ? (RecordingService.hasStartedFrames ? "● Recording • lock screen when ready" : "Starting camera…")
            : "Ready to record");
        for (int i = 0; i < lensGroup.getChildCount(); i++) {
            lensGroup.getChildAt(i).setEnabled(!recording);
        }
        audioBox.setEnabled(!recording);
        if (recording && RecordingService.hasStartedFrames) syncPreview();
        if (!recording && previewAttached) detachPreview();
        if (previewHint != null) {
            previewHint.setVisibility(recording && previewAttached ? View.GONE : View.VISIBLE);
            if (recording && !previewAttached) {
                previewHint.setText("Preparing live camera preview…");
            } else if (!recording) {
                previewHint.setText("LIVE CAMERA PREVIEW\nStart recording to see your camera angle");
            }
        }
    }

    private void syncPreview() {
        if (!visible || previewAttached || cameraPreview == null || !cameraPreview.isAvailable()
            || !RecordingService.isActive || !RecordingService.hasStartedFrames) return;
        try {
            cameraPreview.getSurfaceTexture().setDefaultBufferSize(1280, 720);
            previewSurface = new Surface(cameraPreview.getSurfaceTexture());
            Intent attach = new Intent(this, RecordingService.class);
            attach.setAction(RecordingService.ACTION_PREVIEW);
            attach.putExtra(RecordingService.EXTRA_PREVIEW_SURFACE, previewSurface);
            startService(attach);
            previewAttached = true;
        } catch (RuntimeException ex) {
            Toast.makeText(this, "Preview unavailable on this camera", Toast.LENGTH_LONG).show();
        }
    }

    private void detachPreview() {
        if (!previewAttached) return;
        previewAttached = false;
        if (RecordingService.isActive) {
            Intent detach = new Intent(this, RecordingService.class);
            detach.setAction(RecordingService.ACTION_PREVIEW);
            try { startService(detach); } catch (Exception ignored) { }
        }
        if (previewSurface != null) {
            final Surface old = previewSurface;
            previewSurface = null;
            handler.postDelayed(old::release, 1200);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        visible = true;
        handler.removeCallbacks(statusLoop);
        handler.post(statusLoop);
    }

    @Override protected void onPause() {
        visible = false;
        detachPreview();
        handler.removeCallbacks(statusLoop);
        super.onPause();
    }

    private List<String> neededPermissions() {
        List<String> needed = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.CAMERA);
        if (audioBox.isChecked() && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 33
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        return needed;
    }

    private void requestRecording() {
        List<String> needed = neededPermissions();
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), PERMISSION_REQUEST);
            return;
        }
        Intent start = new Intent(this, RecordingService.class);
        start.setAction(RecordingService.ACTION_START);
        start.putExtra(RecordingService.EXTRA_FRONT_CAMERA, lensGroup.getCheckedRadioButtonId() == FRONT);
        start.putExtra(RecordingService.EXTRA_AUDIO, audioBox.isChecked());
        start.putExtra(RecordingService.EXTRA_DISPLAY_ROTATION, getWindowManager().getDefaultDisplay().getRotation());
        try {
            startForegroundService(start);
            status.setText("Starting camera…");
        } catch (Exception error) {
            Toast.makeText(this, "Unable to record: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == PERMISSION_REQUEST) {
            if (neededPermissions().isEmpty()) requestRecording();
            else Toast.makeText(this, "Camera and notification permissions are required.", Toast.LENGTH_LONG).show();
        }
    }

    private RadioButton radio(int id, String name) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(name);
        button.setTextColor(WHITE);
        button.setTextSize(15);
        button.setButtonTintList(ColorStateList.valueOf(BLUE));
        button.setPadding(0, 0, dp(30), 0);
        return button;
    }

    private TextView text(String value, int size, int color, int style) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setTypeface(Typeface.DEFAULT, style);
        label.setGravity(Gravity.CENTER_VERTICAL);
        return label;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
