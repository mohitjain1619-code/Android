package com.mohit.camverz;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * Camverz Application class that manages global activity lifecycle callbacks
 * and automatically blocks the BACK button / gesture on all full-screen ad activities.
 * Uses WeakReference to avoid static activity memory leaks across lifecycle transitions.
 */
public class CamverzApp extends Application {
    private static final String TAG = "CamverzApp";
    private static WeakReference<Activity> currentActivityRef = new WeakReference<>(null);

    public static Activity getCurrentActivity() {
        return currentActivityRef != null ? currentActivityRef.get() : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 CamverzApp onCreate - Registering global lifecycle back-blocker");

        // Initialize Google Play Billing Client globally & restore purchases
        try {
            BillingManager.getInstance(this).startConnection(null);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BillingManager in CamverzApp", e);
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                currentActivityRef = new WeakReference<>(activity);
                if (WindowBackBlocker.isAdActivity(activity)) {
                    WindowBackBlocker.blockBack(activity);
                    Log.d(TAG, "🔒 Back button blocked on ad activity created: " + activity.getClass().getName());
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                currentActivityRef = new WeakReference<>(activity);
                if (WindowBackBlocker.isAdActivity(activity)) {
                    WindowBackBlocker.blockBack(activity);
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivityRef = new WeakReference<>(activity);
                if (WindowBackBlocker.isAdActivity(activity)) {
                    WindowBackBlocker.blockBack(activity);
                    Log.d(TAG, "🔒 Back button blocked on ad activity resumed: " + activity.getClass().getName());
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Do not clear immediately if transitioning to ad activity
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (currentActivityRef != null && currentActivityRef.get() == activity) {
                    currentActivityRef.clear();
                }
            }
        });
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        try {
            com.bumptech.glide.Glide.get(this).trimMemory(level);
        } catch (Exception e) {
            Log.w(TAG, "Error trimming Glide memory: " + e.getMessage());
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            com.bumptech.glide.Glide.get(this).clearMemory();
        } catch (Exception e) {
            Log.w(TAG, "Error clearing Glide memory on low-memory: " + e.getMessage());
        }
    }
}
