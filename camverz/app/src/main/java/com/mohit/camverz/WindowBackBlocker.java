package com.mohit.camverz;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

/**
 * Utility to aggressively disable and block the hardware/software BACK button
 * and modern predictive back gestures on any Activity (especially 3rd-party ad activities).
 */
public class WindowBackBlocker {
    private static final String TAG = "WindowBackBlocker";

    public static boolean isAdActivity(Activity activity) {
        if (activity == null) return false;
        String name = activity.getClass().getName().toLowerCase();
        return name.contains("ironsource")
            || name.contains("adactivity")
            || name.contains("audiencenetwork")
            || name.contains("unity3d")
            || name.contains("controlleractivity")
            || name.contains("adcontrolleractivity")
            || name.contains("applovin")
            || name.contains("vungle")
            || name.contains("inmobi")
            || name.contains("mintegral")
            || name.contains("chartboost")
            || name.contains("google.android.gms.ads");
    }

    public static void blockBack(Window window) {
        if (window == null) return;
        try {
            Window.Callback current = window.getCallback();
            if (!(current instanceof WindowCallbackWrapper)) {
                window.setCallback(new WindowCallbackWrapper(current));
                Log.d(TAG, "🔒 WindowCallbackWrapper attached to Window directly");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error attaching WindowCallbackWrapper to Window", e);
        }
    }

    public static void blockBack(Activity activity) {
        if (activity == null) return;

        // 1. Wrap Window.Callback to capture KEYCODE_BACK at the absolute lowest system level
        blockBack(activity.getWindow());

        try {
            // 2. Disable Predictive Back Gestures on Android 13+ (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                    new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            Log.d(TAG, "🚫 Predictive back gesture blocked on " + activity.getClass().getSimpleName());
                        }
                    }
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering OnBackInvokedCallback", e);
        }

        try {
            // 3. AndroidX ComponentActivity OnBackPressedCallback
            if (activity instanceof ComponentActivity) {
                ((ComponentActivity) activity).getOnBackPressedDispatcher().addCallback(
                    (ComponentActivity) activity,
                    new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            Log.d(TAG, "🚫 ComponentActivity back press blocked on " + activity.getClass().getSimpleName());
                        }
                    }
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding OnBackPressedCallback", e);
        }
    }
}
