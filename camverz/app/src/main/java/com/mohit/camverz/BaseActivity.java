package com.mohit.camverz;

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
    private static boolean isIronSourceInitialized = false;

    public static void initializeIronSource(android.app.Activity activity) {
        if (!isIronSourceInitialized) {
            String ironSourceAppKey = "27a0e2125";
            com.ironsource.mediationsdk.IronSource.init(activity, ironSourceAppKey);
            isIronSourceInitialized = true;
            Log.d(TAG, "✅ ironSource LevelPlay Mediation SDK Initialized Globally");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        try {
            // Enable edge-to-edge window insets
            androidx.activity.EdgeToEdge.enable(this);
            
            // Ensure status bar icons are white on dark ambient backgrounds
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable edge-to-edge / status bar styling", e);
        }

        boolean isDebug = com.mohit.camverz.BuildConfig.DEBUG;
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
        View contentRoot = findViewById(android.R.id.content);
        if (contentRoot == null) {
            contentRoot = getWindow().getDecorView();
        }

        // Store initial top padding and bottom margin/padding so insets scale predictably on all devices
        if (topView != null && topView.getTag(R.id.tag_initial_padding_top) == null) {
            topView.setTag(R.id.tag_initial_padding_top, topView.getPaddingTop());
        }
        if (bottomView != null) {
            ViewGroup.LayoutParams lp = bottomView.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                if (bottomView.getTag(R.id.tag_initial_margin_bottom) == null) {
                    bottomView.setTag(R.id.tag_initial_margin_bottom, ((ViewGroup.MarginLayoutParams) lp).bottomMargin);
                }
            } else {
                if (bottomView.getTag(R.id.tag_initial_padding_bottom) == null) {
                    bottomView.setTag(R.id.tag_initial_padding_bottom, bottomView.getPaddingBottom());
                }
            }
        }

        final View rootForInsets = contentRoot;

        ViewCompat.setOnApplyWindowInsetsListener(rootForInsets, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            int statusBarInset = Math.max(systemBars.top, displayCutout.top);
            if (statusBarInset <= 0) {
                statusBarInset = getStatusBarHeightFallback();
            }

            int navBarInset = Math.max(systemBars.bottom, ime.bottom);

            if (topView != null) {
                Object initialTag = topView.getTag(R.id.tag_initial_padding_top);
                int initialTopPadding = initialTag instanceof Integer ? (Integer) initialTag : 0;
                topView.setPadding(
                    topView.getPaddingLeft(),
                    statusBarInset + initialTopPadding,
                    topView.getPaddingRight(),
                    topView.getPaddingBottom()
                );
            }

            if (bottomView != null) {
                ViewGroup.LayoutParams lp = bottomView.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    Object initialTag = bottomView.getTag(R.id.tag_initial_margin_bottom);
                    int initialMarginBottom = initialTag instanceof Integer ? (Integer) initialTag : 0;
                    mlp.bottomMargin = navBarInset + initialMarginBottom;
                    bottomView.setLayoutParams(mlp);
                } else {
                    Object initialTag = bottomView.getTag(R.id.tag_initial_padding_bottom);
                    int initialPaddingBottom = initialTag instanceof Integer ? (Integer) initialTag : 0;
                    bottomView.setPadding(
                        bottomView.getPaddingLeft(),
                        bottomView.getPaddingTop(),
                        bottomView.getPaddingRight(),
                        navBarInset + initialPaddingBottom
                    );
                }
            }
            return insets;
        });

        ViewCompat.requestApplyInsets(rootForInsets);
    }

    private int getStatusBarHeightFallback() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        if (result <= 0) {
            result = dpToPx(38); // Safe fallback
        }
        return result;
    }

    public static boolean isAdShowing = false;

    @Override
    public void onBackPressed() {
        if (isAdShowing) {
            Log.d(TAG, "Back press blocked because an ad is currently playing.");
            return;
        }
        super.onBackPressed();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
