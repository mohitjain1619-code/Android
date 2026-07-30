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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Ensure status bar icons are white on dark ambient backgrounds
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        if (ENABLE_SCREENSHOT_PROTECTION) {
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
            Log.d(TAG, "⚠️ Screenshot/Recording Protection: OFF (DEBUG MODE)");
        }
    }

    public void applyWindowInsets(final View topView, final View bottomView) {
        final View decorView = getWindow().getDecorView();
        
        final int initialTopPadding = (topView != null) ? topView.getPaddingTop() : 0;
        final int initialBottomPadding = (bottomView != null) ? bottomView.getPaddingBottom() : 0;

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        final int fallbackStatusBar = (resourceId > 0) ? getResources().getDimensionPixelSize(resourceId) : dpToPx(36);

        ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            int topMargin = Math.max(statusBarInsets.top, fallbackStatusBar);

            if (topView != null) {
                topView.setPadding(
                    topView.getPaddingLeft(),
                    topMargin + Math.max(initialTopPadding, dpToPx(16)),
                    topView.getPaddingRight(),
                    topView.getPaddingBottom()
                );
            }
            if (bottomView != null) {
                ViewGroup.LayoutParams lp = bottomView.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    mlp.bottomMargin = Math.max(navigationBarInsets.bottom, dpToPx(16));
                    bottomView.setLayoutParams(mlp);
                } else {
                    bottomView.setPadding(
                        bottomView.getPaddingLeft(),
                        bottomView.getPaddingTop(),
                        bottomView.getPaddingRight(),
                        initialBottomPadding + navigationBarInsets.bottom
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
