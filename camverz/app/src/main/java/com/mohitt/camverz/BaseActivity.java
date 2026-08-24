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

        // Initialize InMobi SDK safely
        org.json.JSONObject consentObject = new org.json.JSONObject();
        try {
            consentObject.put("gdpr", "0");
        } catch (Exception e) {}
        
        if (com.mohitt.camverz.BuildConfig.DEBUG) {
            try {
                com.inmobi.sdk.InMobiSdk.setLogLevel(com.inmobi.sdk.InMobiSdk.LogLevel.DEBUG);
            } catch (Exception e) {
                Log.e("InMobi", "Failed to set log level", e);
            }
        }

        try {
            com.inmobi.sdk.InMobiSdk.init(this, "cf32cb0f880544468e1a4077d1febf0d", consentObject, new com.inmobi.sdk.SdkInitializationListener() {
                @Override
                public void onInitializationComplete(@androidx.annotation.Nullable Error error) {
                    if (error != null) {
                        android.util.Log.e("InMobi", "InMobi Init failed: " + error.getMessage());
                    } else {
                        android.util.Log.d("InMobi", "InMobi Init Successful");
                    }
                }
            });
        } catch (Exception e) {
            Log.e("InMobi", "Failed to initialize InMobi SDK", e);
        }

        try {
            // Enable edge-to-edge window insets
            androidx.activity.EdgeToEdge.enable(this);
            
            // Ensure status bar icons are white on dark ambient backgrounds
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable edge-to-edge / status bar styling", e);
        }

        boolean isDebug = com.mohitt.camverz.BuildConfig.DEBUG;
        if (ENABLE_SCREENSHOT_PROTECTION && !isDebug) {
            try {
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
            } catch (Exception e) {
                Log.e(TAG, "Failed to set SECURE flag", e);
            }
        } else {
            Log.d(TAG, "⚠️ Screenshot/Recording Protection: OFF (DEBUG MODE OR BYPASS)");
        }
    } // end onCreate

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
