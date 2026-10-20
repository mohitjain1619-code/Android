package com.mohit.camverz;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Camverz Application class that manages global activity lifecycle callbacks
 * and automatically blocks the BACK button / gesture on all full-screen ad activities.
 */
public class CamverzApp extends Application {
    private static final String TAG = "CamverzApp";
    private static Activity currentActivity;

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 CamverzApp onCreate - Registering global lifecycle back-blocker");

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                currentActivity = activity;
                WindowBackBlocker.blockBack(activity);
                if (WindowBackBlocker.isAdActivity(activity)) {
                    Log.d(TAG, "🔒 Back button blocked on ad activity created: " + activity.getClass().getName());
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                currentActivity = activity;
                WindowBackBlocker.blockBack(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivity = activity;
                WindowBackBlocker.blockBack(activity);
                if (WindowBackBlocker.isAdActivity(activity)) {
                    Log.d(TAG, "🔒 Back button blocked on ad activity resumed: " + activity.getClass().getName());
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                if (currentActivity == activity) {
                    // Do not clear immediately if transitioning to ad activity
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                }
            }
        });
    }
}
