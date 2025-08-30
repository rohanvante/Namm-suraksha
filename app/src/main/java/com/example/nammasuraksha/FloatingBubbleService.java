package com.example.nammasuraksha;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.view.Gravity;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.util.Log;
import android.widget.Toast;

public class FloatingBubbleService extends Service {
    private static final String TAG = "FloatingBubbleService";

    private WindowManager windowManager;
    private View bubbleView;
    private static ImageView bubbleImage;
    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener clipChangedListener;
    private static Context appContext;

    public static void showDangerBubble() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        setupFloatingBubble();
        setupClipboardListener();
    }

    private void setupFloatingBubble() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null);
        bubbleImage = bubbleView.findViewById(R.id.bubbleImage);

        // Initial safe state
        updateBubbleVisual(true);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 100;

        windowManager.addView(bubbleView, params);
    }

    private void setupClipboardListener() {

    }

    public static void updateBubble(boolean isSafe) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (bubbleImage != null) {
                bubbleImage.setImageResource(isSafe ?
                        R.drawable.ic_safe : R.drawable.ic_unsafe);
                bubbleImage.setAlpha(isSafe ? 0.5f : 1.0f);
            }
        });
    }

    public static void showDangerIndicator(String threatSource) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (bubbleImage != null) {
                // Visual change
                bubbleImage.setImageResource(R.drawable.ic_unsafe);
                bubbleImage.setAlpha(1.0f);

                // Animation
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(bubbleImage, "scaleX", 1f, 1.2f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(bubbleImage, "scaleY", 1f, 1.2f, 1f);
                AnimatorSet pulse = new AnimatorSet();
                pulse.playTogether(scaleX, scaleY);
                pulse.setDuration(500).start();

                // Vibration
                try {
                    Vibrator vibrator = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(200);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Vibration failed", e);
                }

                // Notification
                Toast.makeText(appContext, "Threat detected by: " + threatSource,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void updateBubbleVisual(boolean isSafe) {
        bubbleImage.setImageResource(isSafe ?
                R.drawable.ic_safe : R.drawable.ic_unsafe);
        bubbleImage.setAlpha(isSafe ? 0.5f : 1.0f);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null && windowManager != null) {
            windowManager.removeView(bubbleView);
        }
        if (clipboardManager != null && clipChangedListener != null) {
            clipboardManager.removePrimaryClipChangedListener(clipChangedListener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Added callback interface inside the same file (optional: you can move it to a separate file)
    public interface UrlSafetyCallback {
        void onResult(boolean isSafe, String source);
    }
}
