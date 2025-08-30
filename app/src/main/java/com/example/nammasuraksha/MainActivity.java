package com.example.nammasuraksha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1001;
    private static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 1002;

    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            } else if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission();
            } else {
                startServices();
            }
        });
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
    }

    private void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE);
        Toast.makeText(this, "Please enable Namma Suraksha in Accessibility Services", Toast.LENGTH_LONG).show();
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/" + LinkDetectionService.class.getName();
        int accessibilityEnabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
        );

        if (accessibilityEnabled == 1) {
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return enabledServices != null && enabledServices.contains(serviceName);
        }
        return false;
    }

    private void startServices() {
        startService(new Intent(this, FloatingBubbleService.class));
        startService(new Intent(this, LinkDetectionService.class));
        Toast.makeText(this, "Protection activated", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                if (!isAccessibilityServiceEnabled()) {
                    requestAccessibilityPermission();
                } else {
                    startServices();
                }
            }
        }
    }
}