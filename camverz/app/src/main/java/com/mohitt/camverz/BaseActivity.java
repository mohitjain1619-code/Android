package com.mohitt.camverz;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    // ⚙️ DEBUG MODE: Change to false to disable screenshot/recording protection
    private static final boolean ENABLE_SCREENSHOT_PROTECTION = false; // record = off (for testing)
    // When you need production: change to true                          // record = on

    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ENABLE_SCREENSHOT_PROTECTION) {
            // 🚫 Disable Screenshot + Screen Recording (FLAG_SECURE)
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            );

            // Extra protection for WebRTC/Video calls (Block secure surface)
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
}
