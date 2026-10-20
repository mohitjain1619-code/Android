package com.mohit.camverz;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Production-grade, Multilingual In-App Update System for Camverz.
 * 
 * Features:
 * 1. Google Play Core In-App Update (Immediate / Flexible) for Play Store installs.
 * 2. Guaranteed Server-Driven Version Check fallback to overcome Play Store 24h cache & sideload blocks.
 * 3. 100% Multilingual dialog support across all 10 app languages (English, Hindi, Arabic, German, Spanish, French, Indonesian, Portuguese, Russian, Turkish).
 */
public class AppUpdateHelper {
    private static final String TAG = "AppUpdateHelper";
    public static final int RC_APP_UPDATE = 9002;

    private final Activity activity;
    private final AppUpdateManager appUpdateManager;
    private AlertDialog updateDialog;

    public AppUpdateHelper(Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    /**
     * Checks if a new update is available.
     * First queries Google Play Core API, and falls back to Server Version check for 100% reliability.
     */
    public void checkForUpdates() {
        try {
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    int updateType = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                            ? AppUpdateType.IMMEDIATE
                            : (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ? AppUpdateType.FLEXIBLE : -1);

                    if (updateType != -1) {
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    updateType,
                                    activity,
                                    RC_APP_UPDATE);
                            return;
                        } catch (Exception e) {
                            Log.e(TAG, "Error starting Google Play in-app update flow", e);
                        }
                    }
                    // If Google Play flow cannot start, show our multilingual dialog
                    showMultilingualUpdateDialog(false);
                } else {
                    // Google Play says not available (e.g. dev build or Play cache delay) -> check server
                    checkServerVersion();
                }
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Google Play update check failed (" + e.getMessage() + "), checking server version...");
                checkServerVersion();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates with Play Core", e);
            checkServerVersion();
        }
    }

    /**
     * Checks backend server endpoint for real-time version status.
     * Guarantees all users immediately see update alerts without waiting for Play Store caching.
     */
    public void checkServerVersion() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        try {
            ApiService api = ApiClient.getInstance(activity).getApi();
            api.getAppVersion().enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject data = response.body();
                        int latestCode = data.has("latestVersionCode") ? data.get("latestVersionCode").getAsInt() : 0;
                        int minCode = data.has("minVersionCode") ? data.get("minVersionCode").getAsInt() : 0;
                        boolean force = data.has("forceUpdate") && data.get("forceUpdate").getAsBoolean();

                        int currentCode = BuildConfig.VERSION_CODE;
                        Log.d(TAG, "🚀 App Version Check -> Installed: " + currentCode + ", Server Latest: " + latestCode + ", Min: " + minCode);

                        if (currentCode < minCode) {
                            showMultilingualUpdateDialog(true);
                        } else if (currentCode < latestCode) {
                            showMultilingualUpdateDialog(force);
                        }
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.w(TAG, "Server version check network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in checkServerVersion", e);
        }
    }

    /**
     * Displays a clean, native multilingual update dialog in the user's chosen language.
     * Localized dynamically via Android strings.xml (values, values-hi, values-ar, values-de, etc.).
     */
    public void showMultilingualUpdateDialog(boolean forceUpdate) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (updateDialog != null && updateDialog.isShowing()) return;

        activity.runOnUiThread(() -> {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.update_dialog_title))
                        .setMessage(activity.getString(R.string.update_dialog_message))
                        .setCancelable(!forceUpdate)
                        .setPositiveButton(activity.getString(R.string.update_dialog_btn_update), (dialog, which) -> {
                            openPlayStorePage();
                            if (forceUpdate) {
                                activity.finish();
                            }
                        });

                if (!forceUpdate) {
                    builder.setNegativeButton(activity.getString(R.string.update_dialog_btn_later), (dialog, which) -> dialog.dismiss());
                }

                updateDialog = builder.create();
                updateDialog.setCanceledOnTouchOutside(!forceUpdate);
                updateDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing multilingual update dialog", e);
            }
        });
    }

    /**
     * Directly redirects the user to Camverz on Google Play Store.
     */
    public void openPlayStorePage() {
        if (activity == null) return;
        String packageName = activity.getPackageName();
        try {
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            activity.startActivity(playStoreIntent);
        } catch (ActivityNotFoundException e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            activity.startActivity(webIntent);
        }
    }

    /**
     * Resumes in-progress updates if interrupted.
     */
    public void checkUpdateInProgress() {
        try {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                activity,
                                RC_APP_UPDATE);
                    } catch (Exception e) {
                        Log.e(TAG, "Error resuming app update flow", e);
                    }
                }
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Failed to check update in progress: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking update in progress", e);
        }
    }
}
