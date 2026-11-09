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
    private static CamverzApp instance;
    private static WeakReference<Activity> currentActivityRef = new WeakReference<>(null);

    public static CamverzApp getInstance() {
        return instance;
    }

    public static Activity getCurrentActivity() {
        return currentActivityRef != null ? currentActivityRef.get() : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize Enterprise Telemetry & Monitoring System
        try {
            com.mohit.camverz.telemetry.AppTelemetry.init(this);
            com.mohit.camverz.telemetry.AppTelemetry.getInstance().checkAndLogInstall();
            com.mohit.camverz.telemetry.AppTelemetry.getInstance().logAppOpen();
        } catch (Throwable t) {
            Log.w(TAG, "Error initializing AppTelemetry: " + t.getMessage());
        }

        // Install top-level crash logger to guarantee instant crashes never fail silently
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("FATAL_CAMVERZ_CRASH", "💥 CRASH ON THREAD [" + thread.getName() + "]: " + throwable.getMessage(), throwable);
            try {
                com.mohit.camverz.telemetry.AppTelemetry.getInstance().logError(
                    "CRASH",
                    throwable.getClass().getSimpleName(),
                    throwable.getMessage(),
                    Log.getStackTraceString(throwable),
                    null
                );
            } catch (Throwable ignored) {}
            try {
                java.io.File file = new java.io.File(getFilesDir(), "last_crash.txt");
                java.io.FileWriter writer = new java.io.FileWriter(file);
                writer.write("Timestamp: " + new java.util.Date() + "\n");
                writer.write("Thread: " + thread.getName() + "\n");
                writer.write("Exception: " + throwable.getClass().getName() + " -> " + throwable.getMessage() + "\n");
                java.io.PrintWriter pw = new java.io.PrintWriter(writer);
                throwable.printStackTrace(pw);
                pw.flush();
                writer.close();
            } catch (Throwable ignored) {}

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        try {
            java.io.File crashFile = new java.io.File(getFilesDir(), "last_crash.txt");
            if (crashFile.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(crashFile));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                Log.e("PREVIOUS_SESSION_CRASH", "🚨 Found previous session crash:\n" + sb.toString());
            }
        } catch (Throwable ignored) {}

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
                try {
                    com.mohit.camverz.telemetry.AppTelemetry.getInstance().onScreenEntered(activity.getClass().getSimpleName());
                } catch (Throwable ignored) {}
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Do not clear immediately if transitioning to ad activity
                try {
                    com.mohit.camverz.telemetry.AppTelemetry.getInstance().onScreenExited(activity.getClass().getSimpleName());
                } catch (Throwable ignored) {}
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
