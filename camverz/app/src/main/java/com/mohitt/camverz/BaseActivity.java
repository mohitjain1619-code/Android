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

        // Initialize InMobi SDK
        org.json.JSONObject consentObject = new org.json.JSONObject();
        try {
            consentObject.put("gdpr", "0");
        } catch (Exception e) {}
        
        if (com.mohitt.camverz.BuildConfig.DEBUG) {
            com.inmobi.sdk.InMobiSdk.setLogLevel(com.inmobi.sdk.InMobiSdk.LogLevel.DEBUG);
        }

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

        // Initialize Unity Ads SDK
        if (!com.unity3d.ads.UnityAds.isInitialized()) {
            com.unity3d.ads.UnityAds.initialize(getApplicationContext(), "800356158", false, new com.unity3d.ads.IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    android.util.Log.d("BaseActivity", "Unity Ads Init Successful");
                }

                @Override
                public void onInitializationFailed(com.unity3d.ads.UnityAds.UnityAdsInitializationError error, String message) {
                    android.util.Log.e("BaseActivity", "Unity Ads Init Failed: " + message);
                }
            });
        }

        // Enable edge-to-edge window insets
        androidx.activity.EdgeToEdge.enable(this);
        
        // Ensure status bar icons are white on dark ambient backgrounds
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        boolean isDebug = com.mohitt.camverz.BuildConfig.DEBUG;
        if (ENABLE_SCREENSHOT_PROTECTION && !isDebug) {
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

        // Initialize LevelPlay SDK
        initLevelPlaySDK();
    }

    public static boolean isLevelPlayInitialized = false;
    public static final java.util.List<Runnable> levelPlayInitCallbacks = new java.util.ArrayList<>();

    public static void runOnLevelPlayInit(Runnable runnable) {
        if (isLevelPlayInitialized) {
            runnable.run();
        } else {
            synchronized (levelPlayInitCallbacks) {
                levelPlayInitCallbacks.add(runnable);
            }
        }
    }

    // Initialize ironSource (LevelPlay) SDK
    private void initLevelPlaySDK() {
        if (isLevelPlayInitialized) return;
        try {
            com.unity3d.mediation.LevelPlay.setAdaptersDebug(true);
            com.unity3d.mediation.LevelPlayInitRequest request = new com.unity3d.mediation.LevelPlayInitRequest.Builder("27a0e2125").build();
            com.unity3d.mediation.LevelPlay.init(this, request, new com.unity3d.mediation.LevelPlayInitListener() {
                @Override
                public void onInitSuccess(com.unity3d.mediation.LevelPlayConfiguration configuration) {
                    android.util.Log.d("BaseActivity", "✅ ironSource LevelPlay initialized successfully with App Key 27a0e2125");
                    
                    // Print clear bidding network check status
                    android.util.Log.d("BiddingCheck", "--------------------------------------------------");
                    android.util.Log.d("BiddingCheck", "🔎 CHECKING ACTIVE BIDDING ADAPTERS IN CODE:");
                    checkAdapterClass("com.ironsource.adapters.unityads.UnityAdsAdapter", "Unity Ads (Bidding)");
                    checkAdapterClass("com.ironsource.adapters.facebook.FacebookAdapter", "Meta / Facebook (Bidding)");
                    checkAdapterClass("com.ironsource.adapters.inmobi.InMobiAdapter", "InMobi");
                    android.util.Log.d("BiddingCheck", "--------------------------------------------------");

                    isLevelPlayInitialized = true;
                    synchronized (levelPlayInitCallbacks) {
                        for (Runnable cb : levelPlayInitCallbacks) {
                            try { cb.run(); } catch (Exception e) {}
                        }
                        levelPlayInitCallbacks.clear();
                    }
                }

                @Override
                public void onInitFailed(com.unity3d.mediation.LevelPlayInitError error) {
                    android.util.Log.e("BaseActivity", "ironSource LevelPlay initialization failed: " + error.getErrorMessage());
                }
            });
        } catch (Exception e) {
            android.util.Log.e("BaseActivity", "ironSource initialization exception: " + e.getMessage());
        }
    }

    private void checkAdapterClass(String className, String networkName) {
        try {
            Class.forName(className);
            android.util.Log.d("BiddingCheck", "✅ " + networkName + " Adapter: ACTIVE (Code integrated & ready for auction!)");
        } catch (ClassNotFoundException e) {
            android.util.Log.e("BiddingCheck", "❌ " + networkName + " Adapter: MISSING (Not integrated in code)");
        }
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
