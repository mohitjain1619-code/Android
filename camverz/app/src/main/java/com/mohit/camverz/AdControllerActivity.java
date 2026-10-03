package com.mohit.camverz;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.window.OnBackInvokedDispatcher;

public class AdControllerActivity extends com.ironsource.sdk.controller.ControllerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Disable Predictive Back Gesture on Android 13+ (API 33+) while ad is playing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        // Do nothing: block back gesture from closing full screen ad
                    }
                );
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onBackPressed() {
        // Block back button press so full-screen ad cannot be dismissed early
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Consume BACK key press
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
