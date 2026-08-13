package com.mohitt.camverz;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class BaseActivity extends AppCompatActivity {

    private static final boolean ENABLE_SCREENSHOT_PROTECTION = false;
    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge window insets
        androidx.activity.EdgeToEdge.enable(this);
        
        // Ensure status bar icons are white on dark ambient backgrounds
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        boolean isTestDevice = getSharedPreferences("debug_prefs", MODE_PRIVATE).getBoolean("is_test_device", false);
        if (ENABLE_SCREENSHOT_PROTECTION && !isTestDevice) {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                );
            }
            Log.d(TAG, "✅ Screenshot/Recording Protection: ON");
        } else {
            Log.d(TAG, "⚠️ Screenshot/Recording Protection: OFF (DEBUG MODE OR BYPASS)");
        }

        // Fetch Advertising ID in a background thread to dynamically verify test device
        new Thread(() -> {
            try {
                com.google.android.gms.ads.identifier.AdvertisingIdClient.Info adInfo =
                        com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                if (adInfo != null) {
                    String adId = adInfo.getId();
                    boolean matches = "ba4350ab-4d9f-4139-af45-49472cf0dc7b".equalsIgnoreCase(adId);
                    boolean wasTestDevice = getSharedPreferences("debug_prefs", MODE_PRIVATE).getBoolean("is_test_device", false);
                    if (matches != wasTestDevice) {
                        getSharedPreferences("debug_prefs", MODE_PRIVATE).edit().putBoolean("is_test_device", matches).apply();
                        if (matches) {
                            runOnUiThread(() -> getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking Advertising ID: " + e.getMessage());
            }
        }).start();
    }

    public void applyWindowInsets(final View topView, final View bottomView) {
        // Set default padding to avoid layout jump before insets are applied
        if (topView != null) {
            topView.setPadding(
                topView.getPaddingLeft(),
                dpToPx(50),
                topView.getPaddingRight(),
                topView.getPaddingBottom()
            );
        }

        final View decorView = getWindow().getDecorView();

        ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets displayCutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            Insets navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Merge status bar and cutout top insets
            int topMargin = Math.max(statusBarInsets.top, displayCutoutInsets.top);
            if (topMargin <= 0) {
                topMargin = dpToPx(38); // Safe fallback
            }

            // Merge navigation bar and keyboard bottom insets
            int bottomMargin = Math.max(navigationBarInsets.bottom, imeInsets.bottom);
            if (bottomMargin <= 0) {
                bottomMargin = dpToPx(16); // Safe fallback
            }

            if (topView != null) {
                topView.setPadding(
                    topView.getPaddingLeft(),
                    topMargin + dpToPx(12),
                    topView.getPaddingRight(),
                    topView.getPaddingBottom()
                );
            }
            if (bottomView != null) {
                ViewGroup.LayoutParams lp = bottomView.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    mlp.bottomMargin = bottomMargin;
                    bottomView.setLayoutParams(mlp);
                } else {
                    bottomView.setPadding(
                        bottomView.getPaddingLeft(),
                        bottomView.getPaddingTop(),
                        bottomView.getPaddingRight(),
                        bottomMargin
                    );
                }
            }
            return insets;
        });

        ViewCompat.requestApplyInsets(decorView);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
