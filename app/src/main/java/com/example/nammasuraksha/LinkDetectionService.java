package com.example.nammasuraksha;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.text.TextUtils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class LinkDetectionService extends AccessibilityService {
    private static final String TAG = "LinkDetectionService";
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\"(https?://[\\\\w\\\\.-]+(?:\\\\.[\\\\w\\\\.-]+)+[/\\\\w\\\\-._~:?#[\\\\]@!$&'()*+,;=]*)\"\n"
    );

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            processEventText(event);
        }
    }

    private void processEventText(AccessibilityEvent event) {
        if (event.getText() == null || event.getText().isEmpty()) {
            return;
        }

        for (CharSequence sequence : event.getText()) {
            String text = sequence.toString();
            if (!TextUtils.isEmpty(text)) {
                extractAndCheckUrls(text);
            }
        }
    }

    private void extractAndCheckUrls(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            Log.d(TAG, "Detected URL: " + url);

            SafeBrowsingChecker.checkUrlSafety(this, url, (isSafe, threatSource) -> {
                if (!isSafe) {
                    FloatingBubbleService.showDangerIndicator(threatSource);
                } else {
                    FloatingBubbleService.updateBubble(true);
                }
            });
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility Service interrupted");
    }
}
