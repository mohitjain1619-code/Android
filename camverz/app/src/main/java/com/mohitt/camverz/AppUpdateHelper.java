package com.mohitt.camverz;

import android.app.Activity;
import android.util.Log;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

/**
 * Helper class for Google Play In-App Updates (Immediate).
 * Blocks application usage if a mandatory update is pending on the Play Store.
 */
public class AppUpdateHelper {
    private static final String TAG = "AppUpdateHelper";
    public static final int RC_APP_UPDATE = 9002;

    private final Activity activity;
    private final AppUpdateManager appUpdateManager;

    public AppUpdateHelper(Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    /**
     * Checks if a new update is available on the Play Store.
     * Triggers the fullscreen immediate update flow if found.
     */
    public void checkForUpdates() {
        try {
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                activity,
                                RC_APP_UPDATE);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting app update flow", e);
                    }
                }
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Failed to check for updates: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
        }
    }

    /**
     * Checks if a previously started update was interrupted (e.g. user restarted the app).
     * Resumes the fullscreen block to force the update completion.
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
