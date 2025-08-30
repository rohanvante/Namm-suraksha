package com.example.nammasuraksha;

import android.content.Context;
import android.util.Log;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.*;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class SafeBrowsingChecker {
    public interface SafetyCallback {
        void onResult(boolean isSafe, String threatSource);
    }

    private static final String TAG = "SafeBrowsingChecker";
    private static final String GOOGLE_API_KEY = "YOUR_GOOGLE_API_KEY";
    private static final String VIRUSTOTAL_API_KEY = "YOUR_VIRUSTOTAL_API_KEY";
    private static final String SAFE_BROWSING_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + GOOGLE_API_KEY;
    private static final String VIRUSTOTAL_API_URL = "https://www.virustotal.com/api/v3/urls/";

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void checkUrlSafety(Context context, String urlToCheck, FloatingBubbleService.UrlSafetyCallback callback) {
        if (urlToCheck == null || urlToCheck.isEmpty()) {
            callback.onResult(true, "Invalid URL");
            return;
        }

        executor.execute(() -> {
            try {
                boolean isSafe = true;
                String threatSource = "";

                // Google Safe Browsing
                boolean googleResult = checkWithGoogleSafeBrowsing(context, urlToCheck);
                if (!googleResult) {
                    isSafe = false;
                    threatSource = "Google Safe Browsing";
                }

                // VirusTotal (optional, you can comment out if you only want Google Safe Browsing)
                boolean virusTotalResult = checkWithVirusTotal(context, urlToCheck);
                if (!virusTotalResult) {
                    isSafe = false;
                    threatSource = "VirusTotal";
                }

                boolean finalIsSafe = isSafe;
                String finalThreatSource = threatSource.isEmpty() ? "Unknown" : threatSource;

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        callback.onResult(finalIsSafe, finalThreatSource));

            } catch (Exception e) {
                Log.e(TAG, "Exception in checkUrlSafety: " + e.getMessage());
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        callback.onResult(true, "error"));
            }
        });
    }

    private static boolean checkWithGoogleSafeBrowsing(Context context, String url) {
        try {
            RequestQueue queue = Volley.newRequestQueue(context);
            JSONObject requestBody = buildGoogleRequestBody(url);

            final boolean[] isSafe = {true};
            final Object lock = new Object();

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SAFE_BROWSING_URL, requestBody,
                    response -> {
                        if (response != null && response.length() > 0) {
                            isSafe[0] = false;
                        }
                        synchronized (lock) {
                            lock.notify();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Google Safe Browsing Error: " + error.toString());
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
            );

            queue.add(request);

            synchronized (lock) {
                lock.wait(5000); // Wait maximum 5 seconds
            }

            return isSafe[0];
        } catch (Exception e) {
            Log.e(TAG, "Exception in checkWithGoogleSafeBrowsing: " + e.getMessage());
            return true;
        }
    }

    private static boolean checkWithVirusTotal(Context context, String url) {
        try {
            RequestQueue queue = Volley.newRequestQueue(context);
            final boolean[] isSafe = {true};
            final Object lock = new Object();

            String encodedUrl = URLEncoder.encode(url, "UTF-8");
            String fullUrl = VIRUSTOTAL_API_URL + encodedUrl;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, fullUrl, null,
                    response -> {
                        try {
                            if (response.has("data")) {
                                JSONObject attributes = response.getJSONObject("data").getJSONObject("attributes");
                                int malicious = attributes.optInt("last_analysis_stats", 0);
                                if (malicious > 0) {
                                    isSafe[0] = false;
                                }
                            }
                        } catch (Exception ignored) {}
                        synchronized (lock) {
                            lock.notify();
                        }
                    },
                    error -> {
                        Log.e(TAG, "VirusTotal Error: " + error.toString());
                        synchronized (lock) {
                            lock.notify();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-apikey", VIRUSTOTAL_API_KEY);
                    return headers;
                }
            };

            queue.add(request);

            synchronized (lock) {
                lock.wait(5000); // Wait max 5 seconds
            }

            return isSafe[0];
        } catch (Exception e) {
            Log.e(TAG, "Exception in checkWithVirusTotal: " + e.getMessage());
            return true;
        }
    }

    private static JSONObject buildGoogleRequestBody(String url) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONObject client = new JSONObject();
        client.put("clientId", "yourcompanyname");
        client.put("clientVersion", "1.5.2");
        jsonObject.put("client", client);

        JSONObject threatInfo = new JSONObject();
        threatInfo.put("threatTypes", new JSONArray().put("MALWARE").put("SOCIAL_ENGINEERING").put("UNWANTED_SOFTWARE"));
        threatInfo.put("platformTypes", new JSONArray().put("ANY_PLATFORM"));
        threatInfo.put("threatEntryTypes", new JSONArray().put("URL"));

        JSONArray entries = new JSONArray();
        entries.put(new JSONObject().put("url", url));
        threatInfo.put("threatEntries", entries);

        jsonObject.put("threatInfo", threatInfo);
        return jsonObject;
    }
}
