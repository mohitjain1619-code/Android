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
            || name.contains("google.android.gms.ads")
            || name.contains("levelplay")
            || name.contains("rewarded")
            || name.contains("interstitial");
    }

    public static void blockBack(Window window) {
        blockBack(window, false);
    }

    public static void blockBack(Window window, boolean isAd) {
        if (window == null) return;
        try {
            Window.Callback current = window.getCallback();
            if (!(current instanceof WindowCallbackWrapper)) {
                window.setCallback(new WindowCallbackWrapper(current, isAd));
                Log.d(TAG, "🔒 WindowCallbackWrapper attached to Window directly (isAd=" + isAd + ")");
            } else if (isAd) {
                Window.Callback delegate = ((WindowCallbackWrapper) current).getDelegate();
                window.setCallback(new WindowCallbackWrapper(delegate, true));
                Log.d(TAG, "🔒 WindowCallbackWrapper updated to forceBlockBack=true");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error attaching WindowCallbackWrapper to Window", e);
        }
    }

    public static void blockBack(Activity activity) {
        if (activity == null) return;
        boolean isAd = isAdActivity(activity);

        // If it's a regular app activity and no ad is playing, do NOT block back!
        if (!isAd && !BaseActivity.isAdShowing && !AdManager.getInstance().isAdWatchPending(activity)) {
            return;
        }

        // 1. Wrap Window.Callback to capture KEYCODE_BACK for ad activity or active ad
        blockBack(activity.getWindow(), isAd);

        // 2. Disable Predictive Back Gestures on Android 13+ (API 33+) ONLY for actual Ad activities
        if (isAd && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                    () -> Log.d(TAG, "🚫 Predictive back gesture blocked on ad activity: " + activity.getClass().getSimpleName())
                );
            } catch (Exception e) {
                Log.e(TAG, "Error registering OnBackInvokedCallback on ad activity", e);
            }
        }
    }
}
