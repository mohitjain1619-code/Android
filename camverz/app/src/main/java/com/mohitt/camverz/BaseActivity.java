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

        // Enable edge-to-edge so window insets can be handled consistently across Android versions
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

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
        View targetView = (topView != null) ? topView : (bottomView != null ? bottomView : getWindow().getDecorView());
        
        final int initialTopPadding = (topView != null) ? topView.getPaddingTop() : 0;
        final int initialBottomPadding = (bottomView != null) ? bottomView.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(targetView, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            if (topView != null) {
                topView.setPadding(
                    topView.getPaddingLeft(),
                    initialTopPadding + statusBarInsets.top,
                    topView.getPaddingRight(),
                    topView.getPaddingBottom()
                );
            }
            if (bottomView != null) {
                ViewGroup.LayoutParams lp = bottomView.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    mlp.bottomMargin = navigationBarInsets.bottom;
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

        // Request insets dispatch
        ViewCompat.requestApplyInsets(targetView);
    }
}
