package com.mohit.camverz.telemetry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.manager.TokenManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AppTelemetry: Enterprise-grade client telemetry logger for Camverz.
 * 
 * Captures the complete user lifecycle:
 * 1. Play Store installs vs sideloads
 * 2. App Launches / Session Starts
 * 3. Google Sign-In attempts, successes, and failures
 * 4. Step-by-step Onboarding actions & drop-off points (Gender, DOB, Location permission/resolution)
 * 5. Screen view durations & user interactions
 * 6. Explicit Ad lifecycle events (Trigger reason, ad network, eCPM, impression, reward, error)
 * 7. System & runtime error logging
 * 
 * Non-blocking, fault-tolerant background executor.
 */
public class AppTelemetry {
    private static final String TAG = "AppTelemetry";
    private static final String PREF_NAME = "camverz_telemetry_prefs";
    private static final String KEY_DEVICE_ID = "telemetry_device_id";
    private static final String KEY_INSTALL_TRACKED = "telemetry_install_tracked";

    private static volatile AppTelemetry instance;

    private final Context appContext;
    private final String deviceId;
    private final boolean isPlayStore;
    private final String installerPackage;
    private final String deviceModel;
    private final String osVersion;
    private final String appVersion;
    private final int appVersionCode;
    private final ExecutorService executorService;
    private final Gson gson;

    private String currentScreenName = "Unknown";
    private long currentScreenStartTime = 0;

    private AppTelemetry(Context context) {
        this.appContext = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.gson = new Gson();

        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedId = prefs.getString(KEY_DEVICE_ID, null);
        if (savedId == null) {
            savedId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, savedId).apply();
        }
        this.deviceId = savedId;

        // Installer detection
        String detectedInstaller = "unknown";
        try {
            PackageManager pm = appContext.getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                InstallSourceInfo info = pm.getInstallSourceInfo(appContext.getPackageName());
                detectedInstaller = info.getInstallingPackageName();
            } else {
                detectedInstaller = pm.getInstallerPackageName(appContext.getPackageName());
            }
        } catch (Throwable ignored) {}
        this.installerPackage = detectedInstaller != null ? detectedInstaller : "direct_apk";
        this.isPlayStore = "com.android.vending".equalsIgnoreCase(this.installerPackage);

        this.deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        this.osVersion = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";

        String verName = "1.0.0";
        int verCode = 1;
        try {
            PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
            verName = pInfo.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                verCode = (int) pInfo.getLongVersionCode();
            } else {
                verCode = pInfo.versionCode;
            }
        } catch (Throwable ignored) {}
        this.appVersion = verName;
        this.appVersionCode = verCode;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new AppTelemetry(context);
        }
    }

    public static AppTelemetry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppTelemetry must be initialized in Application.onCreate() via AppTelemetry.init(this)");
        }
        return instance;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isPlayStoreInstall() {
        return isPlayStore;
    }

    // =========================================================================
    // HIGH-LEVEL LOGGING METHODS
    // =========================================================================

    /**
     * Checks if this is first install and logs APP_INSTALLED event.
     */
    public void checkAndLogInstall() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean alreadyTracked = prefs.getBoolean(KEY_INSTALL_TRACKED, false);
        if (!alreadyTracked) {
            Map<String, Object> details = new HashMap<>();
            details.put("installer", installerPackage);
            details.put("isPlayStore", isPlayStore);
            details.put("first_install_time", System.currentTimeMillis());

            logRawEvent("INSTALL", "APP_INSTALLED", details, 0, null, true, null);
            prefs.edit().putBoolean(KEY_INSTALL_TRACKED, true).apply();
            Log.d(TAG, "📦 First app install logged (PlayStore=" + isPlayStore + ")");
        }
    }

    /**
     * Logs App Open / Session Start
     */
    public void logAppOpen() {
        Map<String, Object> details = new HashMap<>();
        details.put("installer", installerPackage);
        details.put("isPlayStore", isPlayStore);
        details.put("launch_time", System.currentTimeMillis());

        logRawEvent("SESSION", "APP_OPENED", details, 0, null, true, null);
    }

    /**
     * Logs Google Sign In or other authentication lifecycle events
     */
    public void logAuthEvent(String eventName, @Nullable Map<String, Object> details, boolean isSuccess, @Nullable String errorMessage) {
        logRawEvent("AUTH", eventName, details, 0, null, isSuccess, errorMessage);
    }

    /**
     * Logs Onboarding Steps (Gender selection, DOB, Location permission & resolution, Completion)
     */
    public void logOnboardingStep(String stepName, @Nullable Map<String, Object> details, boolean isSuccess, @Nullable String errorMessage) {
        logRawEvent("ONBOARDING", stepName, details, 0, null, isSuccess, errorMessage);
    }

    /**
     * Called when a screen starts / becomes visible
     */
    public void onScreenEntered(String screenName) {
        long now = System.currentTimeMillis();
        // If there was a previous screen active, log its duration
        if (!TextUtils.isEmpty(currentScreenName) && !"Unknown".equalsIgnoreCase(currentScreenName) && currentScreenStartTime > 0) {
            long durationSec = (now - currentScreenStartTime) / 1000L;
            if (durationSec >= 1) {
                Map<String, Object> details = new HashMap<>();
                details.put("screen", currentScreenName);
                logRawEvent("NAVIGATION", "SCREEN_EXITED", details, durationSec, currentScreenName, true, null);
            }
        }
        currentScreenName = screenName;
        currentScreenStartTime = now;

        Map<String, Object> details = new HashMap<>();
        details.put("screen", screenName);
        logRawEvent("NAVIGATION", "SCREEN_VIEW", details, 0, screenName, true, null);
    }

    /**
     * Called when a screen exits
     */
    public void onScreenExited(String screenName) {
        if (screenName != null && screenName.equals(currentScreenName) && currentScreenStartTime > 0) {
            long durationSec = (System.currentTimeMillis() - currentScreenStartTime) / 1000L;
            if (durationSec >= 1) {
                Map<String, Object> details = new HashMap<>();
                details.put("screen", screenName);
                logRawEvent("NAVIGATION", "SCREEN_EXITED", details, durationSec, screenName, true, null);
            }
            currentScreenStartTime = 0;
            currentScreenName = "Background";
        }
    }

    /**
     * Logs User Interaction Clicks (e.g., button clicked, match initiated, story viewed)
     */
    public void logUserAction(String actionName, String screenName, @Nullable Map<String, Object> details) {
        logRawEvent("USER_ACTION", actionName, details, 0, screenName, true, null);
    }

    /**
     * Logs Ad Lifecycle events with full traceability:
     * - adType: "rewarded" | "interstitial"
     * - eventType: "REQUESTED" | "LOADED" | "IMPRESSION" | "CLICKED" | "REWARDED" | "FAILED"
     * - triggerReason: "call_swipe" | "manual_post_unlock" | "daily_bonus"
     */
    public void logAdLifecycle(String adType, String placement, String triggerReason,
                               String eventType, String network, Double revenue, Double ecpm,
                               String auctionId, String status, @Nullable String errorCode, @Nullable String errorMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put("adType", adType != null ? adType : "unknown");
        details.put("placement", placement != null ? placement : "general");
        details.put("triggerReason", triggerReason != null ? triggerReason : "unspecified");
        details.put("adNetwork", network != null ? network : "ironsource");
        details.put("revenue", revenue != null ? revenue : 0.0);
        details.put("ecpm", ecpm != null ? ecpm : 0.0);
        details.put("auctionId", auctionId != null ? auctionId : "");
        details.put("status", status != null ? status : "DELIVERED");

        logRawEvent("AD_LIFECYCLE", eventType, details, 0, currentScreenName, errorCode == null, errorMessage);
    }

    /**
     * Logs an unhandled error, crash, or permission denial
     */
    public void logError(String category, String errorType, String errorMessage, @Nullable String stackTrace, @Nullable Map<String, Object> extraContext) {
        Map<String, Object> details = new HashMap<>();
        if (extraContext != null) {
            details.putAll(extraContext);
        }
        details.put("errorType", errorType);
        if (stackTrace != null) {
            details.put("stackTrace", stackTrace.length() > 1000 ? stackTrace.substring(0, 1000) : stackTrace);
        }

        logRawEvent(category != null ? category : "ERROR", errorType, details, 0, currentScreenName, false, errorMessage);
    }

    // =========================================================================
    // INTERNAL TRANSPORT ENGINE
    // =========================================================================

    private void logRawEvent(final String category, final String eventName,
                             @Nullable final Map<String, Object> details,
                             final long durationSeconds,
                             @Nullable final String screenName,
                             final boolean isSuccess,
                             @Nullable final String errorMessage) {
        executorService.execute(() -> {
            try {
                TokenManager tokenManager = TokenManager.getInstance(appContext);
                String userId = tokenManager.getUserId();
                String userEmail = tokenManager.getUserEmail();

                Map<String, Object> payload = new HashMap<>();
                payload.put("device_id", deviceId);
                payload.put("user_id", !TextUtils.isEmpty(userId) ? userId : null);
                payload.put("user_email", !TextUtils.isEmpty(userEmail) ? userEmail : null);
                payload.put("category", category);
                payload.put("event_name", eventName);
                payload.put("details", details != null ? details : new HashMap<>());
                payload.put("screen_name", screenName != null ? screenName : currentScreenName);
                payload.put("duration_seconds", durationSeconds);
                payload.put("is_play_store", isPlayStore);
                payload.put("installer_package", installerPackage);
                payload.put("device_model", deviceModel);
                payload.put("os_version", osVersion);
                payload.put("app_version", appVersion);
                payload.put("country", detectCountry());
                payload.put("network_type", detectNetworkType());
                payload.put("is_success", isSuccess);
                payload.put("error_message", errorMessage);

                ApiService api = ApiClient.getInstance(appContext).getApi();
                Call<JsonObject> call = api.sendTelemetry(payload);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        // Successfully dispatched telemetry without blocking
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        // Silently degrade gracefully; never interrupt user experience
                    }
                });
            } catch (Throwable t) {
                // Completely safe fail-open
                Log.w(TAG, "Telemetry dispatch bypassed: " + t.getMessage());
            }
        });
    }

    private String detectCountry() {
        try {
            TelephonyManager tm = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String simCountry = tm.getSimCountryIso();
                if (!TextUtils.isEmpty(simCountry)) {
                    return simCountry.toUpperCase(Locale.ROOT);
                }
                String networkCountry = tm.getNetworkCountryIso();
                if (!TextUtils.isEmpty(networkCountry)) {
                    return networkCountry.toUpperCase(Locale.ROOT);
                }
            }
            return Locale.getDefault().getCountry().toUpperCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "UNKNOWN";
        }
    }

    private String detectNetworkType() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return "UNKNOWN";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) return "NONE";
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities == null) return "NONE";
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "WIFI";
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "CELLULAR";
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ETHERNET";
            }
            return "CONNECTED";
        } catch (Throwable ignored) {
            return "UNKNOWN";
        }
    }
}

