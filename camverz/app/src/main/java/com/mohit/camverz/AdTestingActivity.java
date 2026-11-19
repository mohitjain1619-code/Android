package com.mohit.camverz;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// Google Mobile Ads (AdMob) SDK & Meta Mediation
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

// Meta Audience Network
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;

// Unity LevelPlay Mediation
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener;
import com.ironsource.mediationsdk.ads.nativead.NativeAdLayout;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.mohit.camverz.api.TokenManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Production Testing Dashboard for Camverz:
 * 1. Unity LevelPlay Mediation Dual-Auction Bidding (LevelPlay Main Mediator)
 * 2. Standalone Meta Audience Network Testing
 * 3. Live Adapter Diagnostics & Real-Time Console
 */
public class AdTestingActivity extends AppCompatActivity {

    private static final String TAG = "AdTestingActivity";

    // ─────────────────────────────────────────────────────────────────────────────
    // LEVELPLAY MEDIATION CONFIGURATION
    // ─────────────────────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────────
    // ADMOB MEDIATION CONFIGURATION (1 Active Group Per Unit: Meta Audience Network Bidding)
    // ─────────────────────────────────────────────────────────────────────────────
    public static final String ADMOB_APP_ID = "ca-app-pub-6992986179206821~2715651414";
    public static final String ADMOB_MEDIATION_INTERSTITIAL = "ca-app-pub-6992986179206821/9362140189";
    public static final String ADMOB_MEDIATION_REWARDED_INTERSTITIAL = "ca-app-pub-6992986179206821/3305765629";
    public static final String ADMOB_MEDIATION_NATIVE = "ca-app-pub-6992986179206821/9014655823";

    private com.google.android.gms.ads.interstitial.InterstitialAd admobMediatedInterstitialAd;
    private RewardedInterstitialAd admobMediatedRewardedInterstitialAd;
    private NativeAd admobMediatedNativeAd;
    private boolean isAdMobInterLoading = false;
    private boolean isAdMobRewardedInterLoading = false;
    private boolean isAdMobNativeLoading = false;

    private TextView tvAdMobSdkStatus;
    private TextView tvAdMobInterStatus;
    private TextView tvAdMobRewardedInterStatus;
    private TextView tvAdMobNativeStatus;
    private Button btnLoadAdMobInter;
    private Button btnShowAdMobInter;
    private Button btnLoadAdMobRewardedInter;
    private Button btnShowAdMobRewardedInter;
    private Button btnLoadAdMobNative;
    private FrameLayout admobNativeContainer;

    public static final String LEVELPLAY_APP_KEY = "28890f0b5";
    private TextView tvSdkStatus;
    private TextView tvDualAuctionWinner;
    private TextView tvInterstitialStatus;
    private TextView tvRewardedStatus;
    private Button btnWatchAdAndPost;
    private TextView tvNativeStatus;
    private Button btnLoadNative;
    private FrameLayout nativeAdContainer;
    private LevelPlayNativeAd levelPlayNativeAd;

    // Meta Audience Network Direct Placement Testing
    private InterstitialAd metaDirectInterstitial;
    private RewardedVideoAd metaDirectRewarded;
    private TextView tvMetaDirectInterStatus;
    private TextView tvMetaDirectRewardedStatus;
    private Button btnLoadMetaDirectInter;
    private Button btnShowMetaDirectInter;
    private Button btnLoadMetaDirectRewarded;
    private Button btnShowMetaDirectRewarded;

    // LevelPlay Interstitial Bidding UI & State
    private TextView tvLevelPlayInterStatus;
    private Button btnLoadLevelPlayInter;
    private Button btnShowLevelPlayInter;
    private boolean isLevelPlayInterLoading = false;

    // Log Console & Tracking
    private TextView tvConsoleLog;
    private ScrollView logScrollView;
    private final StringBuilder logBuilder = new StringBuilder();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ProgressDialog adProgressDialog;
    private boolean isWaitingForDualAdToShow = false;
    private AdInfo cachedInterstitialAdInfo;
    private AdInfo cachedRewardedAdInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String userEmail = TokenManager.getInstance(this).getUserEmail();
        if (userEmail == null || (!"mohitj8120@gmail.com".equalsIgnoreCase(userEmail.trim()) && !"jainmohit8120@gmail.com".equalsIgnoreCase(userEmail.trim()))) {
            Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Build Clean, Modern UI
        View rootView = buildDashboardUi();
        setContentView(rootView);

        appendLog("🚀 Dashboard Launched. Initializing Mediation Engines...");

        // 1. Initialize Google Mobile Ads SDK (AdMob Main Mediator) with Meta Bidding
        initAdMobMediationWithMeta();

        // 2. Initialize LevelPlay Mediation SDK
        initLevelPlayMediation();
    }

    private void showProgress(String message) {
        if (isFinishing() || isDestroyed()) return;
        try {
            if (adProgressDialog == null) {
                adProgressDialog = new ProgressDialog(this);
                adProgressDialog.setCancelable(false);
            }
            adProgressDialog.setMessage(message);
            if (!adProgressDialog.isShowing()) {
                adProgressDialog.show();
            }
        } catch (Exception ignored) {}
    }

    private void dismissProgress() {
        try {
            if (adProgressDialog != null && adProgressDialog.isShowing()) {
                adProgressDialog.dismiss();
            }
        } catch (Exception ignored) {}
    }

    
    // =========================================================================
    // GOOGLE ADMOB MEDIATION WITH META (1 ACTIVE MEDIATION GROUP PER AD FORMAT)
    // =========================================================================

    private void initAdMobMediationWithMeta() {
        try {
            // Initialize Meta Audience Network first so bidder token can be produced
            try {
                AdSettings.addTestDevice("b94e5118-43fc-4881-b07b-0ecf44bbb6ee");
                AudienceNetworkAds.initialize(this);
                appendLog("✅ [META AUDIENCE NETWORK] Initialized (Test Device GAID: b94e5118-43fc-4881-b07b-0ecf44bbb6ee)");
            } catch (Exception e) {
                appendLog("⚠️ [META INIT NOTICE] " + e.getMessage());
            }

            appendLog("🔄 Initializing Google Mobile Ads SDK with Meta Bidding...");
            MobileAds.initialize(this, initializationStatus -> {
                Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                boolean metaAdapterFound = false;
                for (Map.Entry<String, AdapterStatus> entry : statusMap.entrySet()) {
                    String className = entry.getKey();
                    AdapterStatus status = entry.getValue();
                    if (className.toLowerCase().contains("facebook") || className.toLowerCase().contains("meta")) {
                        metaAdapterFound = true;
                        appendLog("🔌 [ADMOB ADAPTER] Meta Adapter: " + status.getInitializationState()
                                + " (" + status.getDescription() + ", latency=" + status.getLatency() + "ms)");
                    }
                }

                final boolean hasMetaAdapter = metaAdapterFound;
                runOnUiThread(() -> {
                    if (tvAdMobSdkStatus != null) {
                        tvAdMobSdkStatus.setText("ACTIVE (v23.6.0" + (hasMetaAdapter ? " + Meta)" : ")"));
                        tvAdMobSdkStatus.setTextColor(Color.parseColor("#34D399"));
                    }
                    appendLog("✅ [ADMOB MEDIATION READY] Main Mediator: Google Mobile Ads");
                    appendLog("   AdMob App ID: " + ADMOB_APP_ID);
                    appendLog("   Meta Bidding Partner: " + (hasMetaAdapter ? "CONNECTED 🔥" : "STANDALONE"));
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdMob Mediation", e);
            if (tvAdMobSdkStatus != null) {
                tvAdMobSdkStatus.setText("ERROR");
                tvAdMobSdkStatus.setTextColor(Color.parseColor("#EF4444"));
            }
            appendLog("❌ [ADMOB INIT ERROR] " + e.getMessage());
        }
    }

    /**
     * 1. Loads Interstitial Ad using Google AdMob Mediation (1 active group with Meta Bidding).
     */
    private void loadAdMobMediatedInterstitial() {
        if (isAdMobInterLoading) {
            appendLog("⏳ [ADMOB INTERSTITIAL] Request already in-flight.");
            return;
        }

        destroyAdMobMediatedInterstitial();
        isAdMobInterLoading = true;
        if (btnLoadAdMobInter != null) btnLoadAdMobInter.setEnabled(false);
        if (btnShowAdMobInter != null) btnShowAdMobInter.setEnabled(false);
        if (tvAdMobInterStatus != null) {
            tvAdMobInterStatus.setText("AUCTION IN-FLIGHT (AdMob vs Meta)...");
            tvAdMobInterStatus.setTextColor(Color.parseColor("#FBBF24"));
        }

        appendLog("════════════════════════════════════════════");
        appendLog("📡 [ADMOB MEDIATION INTERSTITIAL REQUEST]");
        appendLog("   Ad Unit ID: " + ADMOB_MEDIATION_INTERSTITIAL);
        appendLog("   Mediation Group: 1 Active (Meta Audience Network Bidding)");

        AdRequest adRequest = new AdRequest.Builder().build();

        com.google.android.gms.ads.interstitial.InterstitialAd.load(
                this,
                ADMOB_MEDIATION_INTERSTITIAL,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                        runOnUiThread(() -> {
                            isAdMobInterLoading = false;
                            admobMediatedInterstitialAd = interstitialAd;
                            if (btnLoadAdMobInter != null) btnLoadAdMobInter.setEnabled(true);
                            if (btnShowAdMobInter != null) btnShowAdMobInter.setEnabled(true);

                            inspectAdMobResponseInfo(
                                    "AdMob Mediated Interstitial",
                                    interstitialAd.getResponseInfo(),
                                    tvAdMobInterStatus
                            );

                            setupAdMobInterstitialCallbacks(interstitialAd);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        runOnUiThread(() -> {
                            isAdMobInterLoading = false;
                            if (btnLoadAdMobInter != null) btnLoadAdMobInter.setEnabled(true);
                            if (btnShowAdMobInter != null) btnShowAdMobInter.setEnabled(false);
                            if (tvAdMobInterStatus != null) {
                                tvAdMobInterStatus.setText("LOAD FAILED: " + getAdMobErrorMessage(loadAdError.getCode()));
                                tvAdMobInterStatus.setTextColor(Color.parseColor("#EF4444"));
                            }

                            appendLog("════════════════════════════════════════════");
                            appendLog("❌ [ADMOB INTERSTITIAL LOAD FAILED]");
                            appendLog("   Code: " + loadAdError.getCode() + " (" + getAdMobErrorMessage(loadAdError.getCode()) + ")");
                            appendLog("   Message: " + loadAdError.getMessage());
                            if (loadAdError.getResponseInfo() != null) {
                                appendLog("   Mediation Adapter: " + loadAdError.getResponseInfo().getMediationAdapterClassName());
                                List<AdapterResponseInfo> responses = loadAdError.getResponseInfo().getAdapterResponses();
                                if (responses != null) {
                                    for (AdapterResponseInfo info : responses) {
                                        appendLog("   - Adapter: " + parseAdapterNetworkName(info.getAdapterClassName()));
                                        appendLog("     Status: " + (info.getAdError() == null ? "OK" : "Error " + info.getAdError().getMessage()));
                                    }
                                }
                            }
                        });
                    }
                }
        );
    }

    private void setupAdMobInterstitialCallbacks(com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("📺 [ADMOB INTERSTITIAL] Ad Showed Full Screen.");
                });
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("👋 [ADMOB INTERSTITIAL] Ad Dismissed by User.");
                    destroyAdMobMediatedInterstitial();
                    if (tvAdMobInterStatus != null) {
                        tvAdMobInterStatus.setText("DISMISSED");
                        tvAdMobInterStatus.setTextColor(Color.parseColor("#9CA3AF"));
                    }
                    if (btnShowAdMobInter != null) btnShowAdMobInter.setEnabled(false);
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("❌ [ADMOB INTERSTITIAL SHOW FAILED] " + adError.getMessage());
                    if (tvAdMobInterStatus != null) {
                        tvAdMobInterStatus.setText("SHOW FAILED");
                        tvAdMobInterStatus.setTextColor(Color.parseColor("#EF4444"));
                    }
                    destroyAdMobMediatedInterstitial();
                    if (btnShowAdMobInter != null) btnShowAdMobInter.setEnabled(false);
                });
            }

            @Override
            public void onAdClicked() {
                runOnUiThread(() -> appendLog("🖱️ [ADMOB INTERSTITIAL] User Clicked Ad."));
            }

            @Override
            public void onAdImpression() {
                runOnUiThread(() -> appendLog("👁️ [ADMOB INTERSTITIAL] Impression Recorded (Revenue Logged)."));
            }
        });
    }

    private void showAdMobMediatedInterstitial() {
        if (admobMediatedInterstitialAd != null) {
            showProgress("Presenting Mediated Interstitial...");
            admobMediatedInterstitialAd.show(this);
        } else {
            Toast.makeText(this, "AdMob Interstitial not loaded yet!", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ [ADMOB INTERSTITIAL] Cannot show: Ad not loaded.");
        }
    }

    private void destroyAdMobMediatedInterstitial() {
        if (admobMediatedInterstitialAd != null) {
            admobMediatedInterstitialAd = null;
        }
    }

    /**
     * 2. Loads Rewarded Interstitial using Google AdMob Mediation (1 active group with Meta Bidding).
     */
    private void loadAdMobMediatedRewardedInterstitial() {
        if (isAdMobRewardedInterLoading) {
            appendLog("⏳ [ADMOB REWARDED INTERSTITIAL] Request already in-flight.");
            return;
        }

        destroyAdMobMediatedRewardedInterstitial();
        isAdMobRewardedInterLoading = true;
        if (btnLoadAdMobRewardedInter != null) btnLoadAdMobRewardedInter.setEnabled(false);
        if (btnShowAdMobRewardedInter != null) btnShowAdMobRewardedInter.setEnabled(false);
        if (tvAdMobRewardedInterStatus != null) {
            tvAdMobRewardedInterStatus.setText("AUCTION IN-FLIGHT...");
            tvAdMobRewardedInterStatus.setTextColor(Color.parseColor("#FBBF24"));
        }

        appendLog("════════════════════════════════════════════");
        appendLog("📡 [ADMOB REWARDED INTERSTITIAL REQUEST]");
        appendLog("   Ad Unit ID: " + ADMOB_MEDIATION_REWARDED_INTERSTITIAL);
        appendLog("   Format: Rewarded Interstitial (Meta Bidding)");

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedInterstitialAd.load(
                this,
                ADMOB_MEDIATION_REWARDED_INTERSTITIAL,
                adRequest,
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                        runOnUiThread(() -> {
                            isAdMobRewardedInterLoading = false;
                            admobMediatedRewardedInterstitialAd = rewardedInterstitialAd;
                            if (btnLoadAdMobRewardedInter != null) btnLoadAdMobRewardedInter.setEnabled(true);
                            if (btnShowAdMobRewardedInter != null) btnShowAdMobRewardedInter.setEnabled(true);

                            inspectAdMobResponseInfo(
                                    "AdMob Rewarded Interstitial",
                                    rewardedInterstitialAd.getResponseInfo(),
                                    tvAdMobRewardedInterStatus
                            );

                            setupAdMobRewardedInterstitialCallbacks(rewardedInterstitialAd);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        runOnUiThread(() -> {
                            isAdMobRewardedInterLoading = false;
                            if (btnLoadAdMobRewardedInter != null) btnLoadAdMobRewardedInter.setEnabled(true);
                            if (btnShowAdMobRewardedInter != null) btnShowAdMobRewardedInter.setEnabled(false);
                            if (tvAdMobRewardedInterStatus != null) {
                                tvAdMobRewardedInterStatus.setText("LOAD FAILED: " + getAdMobErrorMessage(loadAdError.getCode()));
                                tvAdMobRewardedInterStatus.setTextColor(Color.parseColor("#EF4444"));
                            }

                            appendLog("❌ [ADMOB REWARDED INTERSTITIAL FAILED] " + loadAdError.getMessage());
                        });
                    }
                }
        );
    }

    private void setupAdMobRewardedInterstitialCallbacks(RewardedInterstitialAd ad) {
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("📺 [ADMOB REWARDED INTERSTITIAL] Ad Showed Full Screen.");
                });
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("👋 [ADMOB REWARDED INTERSTITIAL] Ad Dismissed.");
                    destroyAdMobMediatedRewardedInterstitial();
                    if (tvAdMobRewardedInterStatus != null) {
                        tvAdMobRewardedInterStatus.setText("DISMISSED");
                        tvAdMobRewardedInterStatus.setTextColor(Color.parseColor("#9CA3AF"));
                    }
                    if (btnShowAdMobRewardedInter != null) btnShowAdMobRewardedInter.setEnabled(false);
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("❌ [ADMOB REWARDED INTERSTITIAL SHOW FAILED] " + adError.getMessage());
                    if (tvAdMobRewardedInterStatus != null) {
                        tvAdMobRewardedInterStatus.setText("SHOW FAILED");
                        tvAdMobRewardedInterStatus.setTextColor(Color.parseColor("#EF4444"));
                    }
                    destroyAdMobMediatedRewardedInterstitial();
                    if (btnShowAdMobRewardedInter != null) btnShowAdMobRewardedInter.setEnabled(false);
                });
            }
        });
    }

    private void showAdMobMediatedRewardedInterstitial() {
        if (admobMediatedRewardedInterstitialAd != null) {
            showProgress("Presenting Mediated Rewarded Interstitial...");
            admobMediatedRewardedInterstitialAd.show(this, rewardItem -> runOnUiThread(() -> {
                appendLog("🎁 [ADMOB REWARDED INTERSTITIAL GRANTED] +" + rewardItem.getAmount() + " " + rewardItem.getType());
                Toast.makeText(this, "🎉 Reward Granted: +" + rewardItem.getAmount() + " Coins!", Toast.LENGTH_LONG).show();
            }));
        } else {
            Toast.makeText(this, "Rewarded Interstitial not loaded yet!", Toast.LENGTH_SHORT).show();
        }
    }

    private void destroyAdMobMediatedRewardedInterstitial() {
        if (admobMediatedRewardedInterstitialAd != null) {
            admobMediatedRewardedInterstitialAd = null;
        }
    }

    /**
     * 3. Loads Native Advanced Ad using Google AdMob Mediation (1 active group with Meta Bidding).
     */
    private void loadAdMobMediatedNative() {
        if (isAdMobNativeLoading) {
            appendLog("⏳ [ADMOB NATIVE] Request already in-flight.");
            return;
        }

        destroyAdMobMediatedNative();
        isAdMobNativeLoading = true;
        if (btnLoadAdMobNative != null) btnLoadAdMobNative.setEnabled(false);
        if (tvAdMobNativeStatus != null) {
            tvAdMobNativeStatus.setText("AUCTION IN-FLIGHT (AdMob vs Meta Native)...");
            tvAdMobNativeStatus.setTextColor(Color.parseColor("#FBBF24"));
        }

        appendLog("════════════════════════════════════════════");
        appendLog("📡 [ADMOB MEDIATION NATIVE AD REQUEST]");
        appendLog("   Ad Unit ID: " + ADMOB_MEDIATION_NATIVE);
        appendLog("   Format: Native Advanced (Meta Bidding)");

        AdLoader adLoader = new AdLoader.Builder(this, ADMOB_MEDIATION_NATIVE)
                .forNativeAd(nativeAd -> runOnUiThread(() -> {
                    isAdMobNativeLoading = false;
                    if (btnLoadAdMobNative != null) btnLoadAdMobNative.setEnabled(true);
                    admobMediatedNativeAd = nativeAd;

                    inspectAdMobResponseInfo("AdMob Mediated Native", nativeAd.getResponseInfo(), tvAdMobNativeStatus);

                    renderAdMobNativeView(nativeAd);
                }))
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        runOnUiThread(() -> {
                            isAdMobNativeLoading = false;
                            if (btnLoadAdMobNative != null) btnLoadAdMobNative.setEnabled(true);
                            if (tvAdMobNativeStatus != null) {
                                tvAdMobNativeStatus.setText("FAILED: " + getAdMobErrorMessage(loadAdError.getCode()));
                                tvAdMobNativeStatus.setTextColor(Color.parseColor("#EF4444"));
                            }
                            appendLog("❌ [ADMOB NATIVE LOAD FAILED] " + loadAdError.getMessage());
                        });
                    }

                    @Override
                    public void onAdClicked() {
                        runOnUiThread(() -> appendLog("🖱️ [ADMOB NATIVE] User Clicked Native Ad."));
                    }

                    @Override
                    public void onAdImpression() {
                        runOnUiThread(() -> appendLog("👁️ [ADMOB NATIVE] Native Impression Recorded."));
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void renderAdMobNativeView(NativeAd nativeAd) {
        if (admobNativeContainer == null) return;
        admobNativeContainer.removeAllViews();

        NativeAdView nativeAdView = new NativeAdView(this);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1F2937"));
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.parseColor("#374151"));
        inner.setBackground(bg);

        // Header: Icon + Headline + Advertiser
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView iconView = new ImageView(this);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        nativeAdView.setIconView(iconView);
        if (nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
            iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
            topRow.addView(iconView);
        }

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleLp.setMarginStart(dp(10));
        titleCol.setLayoutParams(titleLp);

        TextView tvHeadline = new TextView(this);
        tvHeadline.setText(nativeAd.getHeadline());
        tvHeadline.setTextColor(Color.WHITE);
        tvHeadline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvHeadline.setTypeface(null, Typeface.BOLD);
        nativeAdView.setHeadlineView(tvHeadline);
        titleCol.addView(tvHeadline);

        if (nativeAd.getAdvertiser() != null) {
            TextView tvAdv = new TextView(this);
            tvAdv.setText("Promoted by " + nativeAd.getAdvertiser());
            tvAdv.setTextColor(Color.parseColor("#9CA3AF"));
            tvAdv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            nativeAdView.setAdvertiserView(tvAdv);
            titleCol.addView(tvAdv);
        }
        topRow.addView(titleCol);
        inner.addView(topRow);

        // Body Text
        if (nativeAd.getBody() != null) {
            TextView tvBody = new TextView(this);
            tvBody.setText(nativeAd.getBody());
            tvBody.setTextColor(Color.parseColor("#D1D5DB"));
            tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvBody.setPadding(0, dp(8), 0, dp(8));
            nativeAdView.setBodyView(tvBody);
            inner.addView(tvBody);
        }

        // Media Content
        MediaView mediaView = new MediaView(this);
        mediaView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        nativeAdView.setMediaView(mediaView);
        inner.addView(mediaView);

        // CTA Button
        if (nativeAd.getCallToAction() != null) {
            Button btnCta = createButton(nativeAd.getCallToAction().toUpperCase(), "#10B981");
            nativeAdView.setCallToActionView(btnCta);
            inner.addView(btnCta);
        }

        nativeAdView.setNativeAd(nativeAd);
        nativeAdView.addView(inner);

        admobNativeContainer.addView(nativeAdView);
        admobNativeContainer.setVisibility(View.VISIBLE);

        appendLog("🖼️ [ADMOB NATIVE] Rendered successfully onto dashboard screen!");
    }

    private void destroyAdMobMediatedNative() {
        if (admobMediatedNativeAd != null) {
            admobMediatedNativeAd.destroy();
            admobMediatedNativeAd = null;
        }
        if (admobNativeContainer != null) {
            admobNativeContainer.removeAllViews();
            admobNativeContainer.setVisibility(View.GONE);
        }
    }

    private void inspectAdMobResponseInfo(String adType, ResponseInfo responseInfo, TextView statusBadge) {
        if (responseInfo == null) return;

        String winningAdapter = responseInfo.getMediationAdapterClassName();
        String winningNetworkName = parseAdapterNetworkName(winningAdapter);

        appendLog("════════════════════════════════════════════");
        appendLog("🏆 [ADMOB MEDIATION AUCTION RESULT]");
        appendLog("   Ad Format: " + adType);
        appendLog("   Response ID: " + responseInfo.getResponseId());
        appendLog("   WINNING MEDIATION ADAPTER: " + winningAdapter);
        appendLog("   WINNING NETWORK: " + winningNetworkName.toUpperCase() + " 🔥");

        AdapterResponseInfo loadedAdapter = responseInfo.getLoadedAdapterResponseInfo();
        if (loadedAdapter != null) {
            appendLog("   Loaded Ad Source: " + loadedAdapter.getAdSourceName());
            appendLog("   Latency: " + loadedAdapter.getLatencyMillis() + "ms");
        }

        if (statusBadge != null) {
            statusBadge.setText("LOADED (" + winningNetworkName + ")");
            statusBadge.setTextColor(Color.parseColor("#34D399"));
        }

        List<AdapterResponseInfo> adapterList = responseInfo.getAdapterResponses();
        if (adapterList != null && !adapterList.isEmpty()) {
            appendLog("📊 [AUCTION PARTICIPANTS & BIDS]:");
            for (AdapterResponseInfo adapterInfo : adapterList) {
                String network = parseAdapterNetworkName(adapterInfo.getAdapterClassName());
                long latency = adapterInfo.getLatencyMillis();
                boolean won = adapterInfo.getAdapterClassName().equals(winningAdapter);
                appendLog(String.format(
                        Locale.getDefault(),
                        "   • %s %s (Latency: %dms, Status: %s)",
                        (won ? "👑 [WINNER]" : "   [BIDDER]"),
                        network,
                        latency,
                        (adapterInfo.getAdError() == null ? "OK" : "Rejected: " + adapterInfo.getAdError().getMessage())
                ));
            }
        }
        appendLog("════════════════════════════════════════════");
    }

    private String parseAdapterNetworkName(String adapterClassName) {
        if (adapterClassName == null) return "Unknown";
        String lower = adapterClassName.toLowerCase();
        if (lower.contains("facebook") || lower.contains("meta")) {
            return "Meta Audience Network (Bidding)";
        } else if (lower.contains("admob") || lower.contains("google")) {
            return "Google AdMob";
        } else if (lower.contains("unity")) {
            return "Unity Ads";
        } else if (lower.contains("applovin")) {
            return "AppLovin";
        } else if (lower.contains("ironsource") || lower.contains("levelplay")) {
            return "ironSource / LevelPlay";
        }
        return adapterClassName;
    }

    private String getAdMobErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                return "INTERNAL_ERROR (0)";
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                return "INVALID_REQUEST (1)";
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                return "NETWORK_ERROR (2)";
            case AdRequest.ERROR_CODE_NO_FILL:
                return "NO_FILL (3 - Auction ran, no ad returned from Meta/Google)";
            default:
                return "CODE_" + errorCode;
        }
    }

    private void validateAdMobMetaMediationAdapters() {
        appendLog("════════════════════════════════════════════");
        appendLog("🩺 [VALIDATING ADMOB-META MEDIATION INTEGRATION]");

        boolean admobSdk = checkClassExists("com.google.android.gms.ads.MobileAds");
        boolean metaSdk = checkClassExists("com.facebook.ads.AudienceNetworkAds");
        boolean admobMetaAdapter = checkClassExists("com.google.ads.mediation.facebook.FacebookMediationAdapter")
                || checkClassExists("com.google.ads.mediation.facebook.FacebookAdapter");

        appendLog(admobSdk ? "  ✅ Google Mobile Ads SDK: COMPILED & FOUND" : "  ❌ Google Mobile Ads SDK: MISSING");
        appendLog(metaSdk ? "  ✅ Meta Audience Network SDK: COMPILED & FOUND" : "  ❌ Meta Audience Network SDK: MISSING");
        appendLog(admobMetaAdapter ? "  ✅ AdMob-Meta Mediation Adapter (FacebookMediationAdapter): COMPILED & FOUND 🔥"
                : "  ❌ AdMob-Meta Mediation Adapter: MISSING from build.gradle!");

        if (admobSdk && metaSdk && admobMetaAdapter) {
            appendLog("🎉 All AdMob-Meta Mediation components verified 100%! Ready to load live mediated ads.");
            Toast.makeText(this, "AdMob + Meta Mediation Ready!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "AdMob-Meta component missing!", Toast.LENGTH_LONG).show();
        }
    }

    private void openAdMobInspector() {
        appendLog("════════════════════════════════════════════");
        appendLog("🔍 [OPENING ADMOB AD INSPECTOR]");
        appendLog("   Opening mediation diagnostics overlay to inspect Meta bidding status...");
        MobileAds.openAdInspector(this, error -> {
            if (error != null) {
                appendLog("❌ [AD INSPECTOR ERROR] " + error.getCode() + ": " + error.getMessage());
                Toast.makeText(this, "Ad Inspector: " + error.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                appendLog("🔎 [AD INSPECTOR CLOSED]");
            }
        });
    }

    // =========================================================================
    // 2. LEVELPLAY SDK & DUAL-AUCTION LISTENERS
    // =========================================================================

    private void initLevelPlayMediation() {
        try {
            setupLevelPlayDebugListener();

            tvSdkStatus.setText("ACTIVE (v8.7.0)");
            tvSdkStatus.setTextColor(Color.parseColor("#34D399"));

            IronSource.loadInterstitial();
            AdManager.getInstance().preloadDualBiddingAds(this);
            updateDualAuctionStatusUi();

        } catch (Exception e) {
            Log.e(TAG, "Error in initLevelPlayMediation", e);
            tvSdkStatus.setText("ERROR");
            tvSdkStatus.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void setupLevelPlayDebugListener() {
        AdManager.getInstance().rebindLevelPlayListeners();
        AdManager.getInstance().setDebugAdListener(new AdManager.DebugAdListener() {
            @Override
            public void onInterstitialReady(AdInfo adInfo) {
                runOnUiThread(() -> {
                    isLevelPlayInterLoading = false;
                    cachedInterstitialAdInfo = adInfo;
                    String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "Meta/Mediation";
                    Double rev = adInfo != null ? adInfo.getRevenue() : null;
                    String revText = rev != null ? String.format(Locale.US, " ($%.4f)", rev) : "";

                    if (btnLoadLevelPlayInter != null) btnLoadLevelPlayInter.setEnabled(true);
                    if (btnShowLevelPlayInter != null) btnShowLevelPlayInter.setEnabled(true);
                    if (tvLevelPlayInterStatus != null) {
                        tvLevelPlayInterStatus.setText("READY: " + network + revText);
                        tvLevelPlayInterStatus.setTextColor(Color.parseColor("#34D399"));
                    }

                    appendLog("🟢 [LEVELPLAY INTERSTITIAL READY] Winner: " + network + revText);
                    updateDualAuctionStatusUi();

                    if (isWaitingForDualAdToShow) {
                        isWaitingForDualAdToShow = false;
                        dismissProgress();
                        executeDualAuctionWatchAdAndPost();
                    }
                });
            }

            @Override
            public void onInterstitialLoadFailed(IronSourceError error) {
                runOnUiThread(() -> {
                    isLevelPlayInterLoading = false;
                    cachedInterstitialAdInfo = null;
                    String msg = error != null ? error.getErrorMessage() : "Unknown";
                    int code = error != null ? error.getErrorCode() : -1;

                    if (btnLoadLevelPlayInter != null) btnLoadLevelPlayInter.setEnabled(true);
                    if (btnShowLevelPlayInter != null) btnShowLevelPlayInter.setEnabled(false);
                    if (tvLevelPlayInterStatus != null) {
                        tvLevelPlayInterStatus.setText("FAILED (" + code + ")");
                        tvLevelPlayInterStatus.setTextColor(Color.parseColor("#EF4444"));
                    }

                    appendLog("⚠️ [LEVELPLAY INTERSTITIAL] " + msg + " (Code: " + code + ")");
                    updateDualAuctionStatusUi();
                });
            }

            @Override
            public void onInterstitialOpened(AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String network = adInfo != null ? adInfo.getAdNetwork() : "Mediation";
                    appendLog("👁️ [LEVELPLAY OPENED] Displayed via " + network);
                });
            }

            @Override
            public void onInterstitialShowSucceeded(AdInfo adInfo) {
                runOnUiThread(() -> appendLog("✅ [LEVELPLAY SHOW] Playback started."));
            }

            @Override
            public void onInterstitialShowFailed(IronSourceError error, AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String msg = error != null ? error.getErrorMessage() : "Show error";
                    appendLog("❌ [LEVELPLAY SHOW FAILED] " + msg);
                });
            }

            @Override
            public void onInterstitialClicked(AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🖱️ [LEVELPLAY CLICK] User clicked the ad."));
            }

            @Override
            public void onInterstitialClosed(AdInfo adInfo) {
                runOnUiThread(() -> {
                    appendLog("🚪 [LEVELPLAY CLOSED] Interstitial closed.");
                    if (btnShowLevelPlayInter != null) btnShowLevelPlayInter.setEnabled(false);
                    if (tvLevelPlayInterStatus != null) {
                        tvLevelPlayInterStatus.setText("CLOSED (Ready to load next auction)");
                        tvLevelPlayInterStatus.setTextColor(Color.parseColor("#94A3B8"));
                    }
                    IronSource.loadInterstitial();
                    updateDualAuctionStatusUi();
                });
            }

            @Override
            public void onRewardedAvailable(AdInfo adInfo) {
                runOnUiThread(() -> {
                    cachedRewardedAdInfo = adInfo;
                    String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "AdMob/Unity";
                    Double rev = adInfo != null ? adInfo.getRevenue() : null;
                    String revText = rev != null ? String.format(Locale.US, " ($%.4f)", rev) : "";

                    appendLog("🟢 [LEVELPLAY REWARDED READY] Winner: " + network + revText);
                    updateDualAuctionStatusUi();

                    if (isWaitingForDualAdToShow) {
                        isWaitingForDualAdToShow = false;
                        dismissProgress();
                        executeDualAuctionWatchAdAndPost();
                    }
                });
            }

            @Override
            public void onRewardedUnavailable() {
                runOnUiThread(() -> {
                    cachedRewardedAdInfo = null;
                    appendLog("ℹ️ [LEVELPLAY REWARDED UNAVAILABLE] Currently no fill for Rewarded Video.");
                    updateDualAuctionStatusUi();
                });
            }

            @Override
            public void onRewardedOpened(AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String network = adInfo != null ? adInfo.getAdNetwork() : "Mediation";
                    appendLog("👁️ [LEVELPLAY REWARDED OPENED] Video ad playing via " + network);
                });
            }

            @Override
            public void onRewardedShowFailed(IronSourceError error, AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String msg = error != null ? error.getErrorMessage() : "Show error";
                    appendLog("❌ [LEVELPLAY REWARDED SHOW FAILED] " + msg);
                });
            }

            @Override
            public void onRewardedClicked(Placement placement, AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🖱️ [LEVELPLAY REWARDED CLICK] User clicked rewarded ad."));
            }

            @Override
            public void onRewardedAdRewarded(Placement placement, AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🎁 [LEVELPLAY REWARD EARNED] Video reward unlocked for post publication!"));
            }

            @Override
            public void onRewardedClosed(AdInfo adInfo) {
                runOnUiThread(() -> {
                    appendLog("🚪 [LEVELPLAY REWARDED CLOSED] Rewarded video closed.");
                    updateDualAuctionStatusUi();
                });
            }
        });
    }

    private void updateDualAuctionStatusUi() {
        boolean interReady = IronSource.isInterstitialReady();
        boolean rewReady = IronSource.isRewardedVideoAvailable();

        AdInfo interInfo = cachedInterstitialAdInfo != null ? cachedInterstitialAdInfo : AdManager.getInstance().getLatestInterstitialAdInfo();
        AdInfo rewInfo = cachedRewardedAdInfo != null ? cachedRewardedAdInfo : AdManager.getInstance().getLatestRewardedAdInfo();

        Double interRev = interInfo != null ? interInfo.getRevenue() : null;
        String interNet = interInfo != null && interInfo.getAdNetwork() != null ? interInfo.getAdNetwork() : "Meta/Mediation";

        Double rewRev = rewInfo != null ? rewInfo.getRevenue() : null;
        String rewNet = rewInfo != null && rewInfo.getAdNetwork() != null ? rewInfo.getAdNetwork() : "AdMob/Unity";

        if (interReady) {
            String txt = "READY: " + interNet + (interRev != null ? String.format(Locale.US, " ($%.4f)", interRev) : "");
            tvInterstitialStatus.setText("🎬 Interstitial: " + txt);
            tvInterstitialStatus.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvInterstitialStatus.setText("🎬 Interstitial: Preloading in background...");
            tvInterstitialStatus.setTextColor(Color.parseColor("#FBBF24"));
        }

        if (btnShowLevelPlayInter != null) {
            btnShowLevelPlayInter.setEnabled(interReady);
        }
        if (tvLevelPlayInterStatus != null && !isLevelPlayInterLoading) {
            if (interReady) {
                String txt = "READY: " + interNet + (interRev != null ? String.format(Locale.US, " ($%.4f)", interRev) : "");
                tvLevelPlayInterStatus.setText(txt);
                tvLevelPlayInterStatus.setTextColor(Color.parseColor("#34D399"));
            } else {
                tvLevelPlayInterStatus.setText("NOT LOADED (Ready to request auction)");
                tvLevelPlayInterStatus.setTextColor(Color.parseColor("#94A3B8"));
            }
        }

        if (rewReady) {
            String txt = "READY: " + rewNet + (rewRev != null ? String.format(Locale.US, " ($%.4f)", rewRev) : "");
            tvRewardedStatus.setText("🎁 Rewarded: " + txt);
            tvRewardedStatus.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvRewardedStatus.setText("🎁 Rewarded: Auto-caching / Unavailable");
            tvRewardedStatus.setTextColor(Color.parseColor("#94A3B8"));
        }

        if (interReady && rewReady) {
            double rVal = rewRev != null ? rewRev : 0.0;
            double iVal = interRev != null ? interRev : 0.0;
            if (iVal >= rVal) {
                tvDualAuctionWinner.setText("🏆 HIGHEST BIDDER: " + interNet + " (Interstitial) $" + String.format(Locale.US, "%.4f", iVal));
            } else {
                tvDualAuctionWinner.setText("🏆 HIGHEST BIDDER: " + rewNet + " (Rewarded) $" + String.format(Locale.US, "%.4f", rVal));
            }
            tvDualAuctionWinner.setTextColor(Color.parseColor("#38BDF8"));
        } else if (interReady) {
            tvDualAuctionWinner.setText("🏆 HIGHEST BIDDER: " + interNet + " (Interstitial" + (interRev != null ? " $" + String.format(Locale.US, "%.4f", interRev) : "") + ")");
            tvDualAuctionWinner.setTextColor(Color.parseColor("#34D399"));
        } else if (rewReady) {
            tvDualAuctionWinner.setText("🏆 HIGHEST BIDDER: " + rewNet + " (Rewarded" + (rewRev != null ? " $" + String.format(Locale.US, "%.4f", rewRev) : "") + ")");
            tvDualAuctionWinner.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvDualAuctionWinner.setText("⏳ Running Dual Auction across all networks...");
            tvDualAuctionWinner.setTextColor(Color.parseColor("#FBBF24"));
        }
    }

    private void handleWatchAdAndPostClick() {
        appendLog("════════════════════════════════════════════");
        appendLog("🎬 [USER ACTION] Tap 'Watch Ad & Post' (LevelPlay Dual-Auction)");

        boolean interReady = IronSource.isInterstitialReady();
        boolean rewReady = IronSource.isRewardedVideoAvailable();

        if (!interReady && !rewReady) {
            appendLog("📡 Neither ad cached yet. Requesting parallel auctions...");
            showProgress("Running Dual Auction (Meta vs AdMob vs Unity)...");
            isWaitingForDualAdToShow = true;
            IronSource.loadInterstitial();
            AdManager.getInstance().preloadRewardedVideo();

            mainHandler.postDelayed(() -> {
                if (isWaitingForDualAdToShow) {
                    isWaitingForDualAdToShow = false;
                    dismissProgress();
                    appendLog("⚠️ [AUCTION TIMEOUT] Proceeding with post publish (Zero user block).");
                    showPostPublishedDialog("Post Published (Auction Timeout)", false);
                }
            }, 6000);
            return;
        }

        executeDualAuctionWatchAdAndPost();
    }

    private void executeDualAuctionWatchAdAndPost() {
        boolean interReady = IronSource.isInterstitialReady();
        boolean rewReady = IronSource.isRewardedVideoAvailable();

        AdInfo interInfo = cachedInterstitialAdInfo != null ? cachedInterstitialAdInfo : AdManager.getInstance().getLatestInterstitialAdInfo();
        AdInfo rewInfo = cachedRewardedAdInfo != null ? cachedRewardedAdInfo : AdManager.getInstance().getLatestRewardedAdInfo();

        Double interRev = interInfo != null ? interInfo.getRevenue() : null;
        String interNet = interInfo != null && interInfo.getAdNetwork() != null ? interInfo.getAdNetwork() : "Meta Audience Network";

        Double rewRev = rewInfo != null ? rewInfo.getRevenue() : null;
        String rewNet = rewInfo != null && rewInfo.getAdNetwork() != null ? rewInfo.getAdNetwork() : "Google AdMob / Unity";

        String winnerFormat;
        String winnerNet;
        Double winnerRev;

        if (interReady && rewReady) {
            double rVal = rewRev != null ? rewRev : 0.0;
            double iVal = interRev != null ? interRev : 0.0;
            if (iVal >= rVal) {
                winnerFormat = "Interstitial";
                winnerNet = interNet;
                winnerRev = interRev;
            } else {
                winnerFormat = "Rewarded Video";
                winnerNet = rewNet;
                winnerRev = rewRev;
            }
        } else if (interReady) {
            winnerFormat = "Interstitial";
            winnerNet = interNet;
            winnerRev = interRev;
        } else {
            winnerFormat = "Rewarded Video";
            winnerNet = rewNet;
            winnerRev = rewRev;
        }

        String revStr = winnerRev != null ? String.format(Locale.US, "$%.5f", winnerRev) : "Standard Bid";
        appendLog("🏆 [LEVELPLAY WINNER] Format: " + winnerFormat + " | Network: " + winnerNet + " | eCPM: " + revStr);

        AdManager.getInstance().showHighestBidRewardAd(this, "dashboard_watch_ad_and_post", new AdManager.RewardedAdCallback() {
            @Override
            public void onRewardEarned() {
                runOnUiThread(() -> appendLog("🎁 [REWARD EARNED] Full reward unlocked for post publication."));
            }

            @Override
            public void onAdDismissed(boolean rewardGranted) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("🚪 [AD DISMISSED] Return to app. Post publication approved!");
                    showPostPublishedDialog("Post Published Successfully! 🎉\n\n"
                            + "Winner: " + winnerNet + " (" + winnerFormat + ")\n"
                            + "Revenue: " + revStr + "\n"
                            + "Reward Granted: " + rewardGranted, true);
                    updateDualAuctionStatusUi();
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("❌ [AD FAILED] " + error + ". Publishing post fallback.");
                    showPostPublishedDialog("Post Published (Fallback)", false);
                    updateDualAuctionStatusUi();
                });
            }
        });
    }

    private void showPostPublishedDialog(String message, boolean isSuccess) {
        new AlertDialog.Builder(this)
                .setTitle(isSuccess ? "🎉 Post Published!" : "Post Status")
                .setMessage(message)
                .setPositiveButton("Awesome!", null)
                .show();
    }

    private void loadLevelPlayInterstitialBidding() {
        if (isLevelPlayInterLoading) {
            appendLog("⏳ [LEVELPLAY INTERSTITIAL] Auction request already in-flight.");
            return;
        }

        isLevelPlayInterLoading = true;
        if (btnLoadLevelPlayInter != null) btnLoadLevelPlayInter.setEnabled(false);
        if (btnShowLevelPlayInter != null) btnShowLevelPlayInter.setEnabled(false);

        if (tvLevelPlayInterStatus != null) {
            tvLevelPlayInterStatus.setText("AUCTION IN-FLIGHT (Meta vs Unity vs ironSource)...");
            tvLevelPlayInterStatus.setTextColor(Color.parseColor("#FBBF24"));
        }

        appendLog("════════════════════════════════════════════");
        appendLog("📡 [LEVELPLAY INTERSTITIAL BIDDING REQUEST]");
        appendLog("   Placement: " + AdManager.LEVELPLAY_INTERSTITIAL_ID);
        appendLog("   Bidders: Meta Audience Network, Unity Ads, ironSource Bidding");
        appendLog("   Dispatched: IronSource.loadInterstitial()");

        IronSource.loadInterstitial();
    }

    private void showLevelPlayInterstitialBidding() {
        if (!IronSource.isInterstitialReady()) {
            Toast.makeText(this, "Interstitial not ready yet. Tap 'Load Interstitial Bidding' first.", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ [LEVELPLAY INTERSTITIAL] Cannot show: Ad is not ready.");
            return;
        }

        AdInfo interInfo = cachedInterstitialAdInfo != null ? cachedInterstitialAdInfo : AdManager.getInstance().getLatestInterstitialAdInfo();
        String winnerNet = interInfo != null && interInfo.getAdNetwork() != null ? interInfo.getAdNetwork() : "Meta/Mediation";
        Double rev = interInfo != null ? interInfo.getRevenue() : null;
        String revStr = rev != null ? String.format(Locale.US, "$%.5f", rev) : "Standard Bid";

        appendLog("════════════════════════════════════════════");
        appendLog("▶️ [SHOWING LEVELPLAY INTERSTITIAL BIDDING]");
        appendLog("🏆 Winning Bidder: " + winnerNet);
        appendLog("💰 eCPM / Revenue: " + revStr);

        AdManager.getInstance().showLevelPlayPureInterstitialAd(this, "dashboard_interstitial_bidding", success -> {
            runOnUiThread(() -> {
                appendLog("⏹️ [LEVELPLAY INTERSTITIAL FINISHED] Success: " + success);
                showPostPublishedDialog("LevelPlay Interstitial Bidding Completed! 🎉\n\n"
                        + "Winning Network: " + winnerNet + "\n"
                        + "Revenue/eCPM: " + revStr + "\n"
                        + "Dismissed Cleanly: " + success, true);
                if (btnShowLevelPlayInter != null) {
                    btnShowLevelPlayInter.setEnabled(IronSource.isInterstitialReady());
                }
                updateDualAuctionStatusUi();
            });
        });
    }

    private void handleWatchPureRewardedClick() {
        appendLog("════════════════════════════════════════════");
        appendLog("🎁 [USER ACTION] Tap 'LevelPlay Pure Rewarded Video' (Only Rewarded Bidding)");

        boolean rewReady = IronSource.isRewardedVideoAvailable();
        AdInfo rewInfo = cachedRewardedAdInfo != null ? cachedRewardedAdInfo : AdManager.getInstance().getLatestRewardedAdInfo();
        String rewNet = rewInfo != null && rewInfo.getAdNetwork() != null ? rewInfo.getAdNetwork() : "LevelPlay Bidding Partner";
        Double rewRev = rewInfo != null ? rewInfo.getRevenue() : null;
        String revStr = rewRev != null ? String.format(Locale.US, "$%.5f", rewRev) : "Standard Bid";

        if (!rewReady) {
            appendLog("📡 LevelPlay Rewarded Video not ready yet. Preloading Rewarded Video auction...");
            showProgress("Loading LevelPlay Rewarded Video auction...");
            AdManager.getInstance().preloadRewardedVideo();

            mainHandler.postDelayed(() -> {
                if (!IronSource.isRewardedVideoAvailable()) {
                    dismissProgress();
                    appendLog("⚠️ [REWARDED TIMEOUT] Rewarded video still caching. Please try again in a few seconds.");
                    Toast.makeText(this, "Rewarded video is caching, please try again in a few seconds", Toast.LENGTH_SHORT).show();
                } else {
                    dismissProgress();
                    playPureRewardedNow();
                }
            }, 3500);
            return;
        }

        playPureRewardedNow();
    }

    private void playPureRewardedNow() {
        AdInfo rewInfo = cachedRewardedAdInfo != null ? cachedRewardedAdInfo : AdManager.getInstance().getLatestRewardedAdInfo();
        String rewNet = rewInfo != null && rewInfo.getAdNetwork() != null ? rewInfo.getAdNetwork() : "LevelPlay Bidding Partner";
        Double rewRev = rewInfo != null ? rewInfo.getRevenue() : null;
        String revStr = rewRev != null ? String.format(Locale.US, "$%.5f", rewRev) : "Standard Bid";

        appendLog("🎬 [LEVELPLAY PURE REWARDED] Playing via " + rewNet + " | eCPM/Rev: " + revStr);

        AdManager.getInstance().showLevelPlayPureRewardedAd(this, "dashboard_pure_rewarded", new AdManager.RewardedAdCallback() {
            @Override
            public void onRewardEarned() {
                runOnUiThread(() -> appendLog("🎁 [REWARD EARNED] Video reward unlocked successfully!"));
            }

            @Override
            public void onAdDismissed(boolean rewardGranted) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("🚪 [REWARDED CLOSED] Return to dashboard. Reward granted: " + rewardGranted);
                    showPostPublishedDialog("LevelPlay Pure Rewarded Completed! 🎉\n\n"
                            + "Winning Network: " + rewNet + "\n"
                            + "Revenue/eCPM: " + revStr + "\n"
                            + "Reward Granted: " + rewardGranted, true);
                    updateDualAuctionStatusUi();
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("❌ [REWARDED FAILED] " + error);
                    Toast.makeText(AdTestingActivity.this, "Rewarded Ad Failed: " + error, Toast.LENGTH_SHORT).show();
                    updateDualAuctionStatusUi();
                });
            }
        });
    }

    // =========================================================================
    // LEVELPLAY NATIVE AD
    // =========================================================================

    private void loadNativeAd() {
        try {
            appendLog("📡 [REQUESTING LEVELPLAY NATIVE AD] Placement: " + AdManager.LEVELPLAY_NATIVE_ID);
            tvNativeStatus.setText("LOADING...");
            tvNativeStatus.setTextColor(Color.parseColor("#FBBF24"));
            btnLoadNative.setEnabled(false);

            if (levelPlayNativeAd != null) {
                levelPlayNativeAd.destroyAd();
                levelPlayNativeAd = null;
            }

            LevelPlayNativeAd.Builder builder = new LevelPlayNativeAd.Builder();
            builder.withPlacementName(AdManager.LEVELPLAY_NATIVE_ID);
            builder.withActivity(this);
            builder.withListener(new LevelPlayNativeAdListener() {
                @Override
                public void onAdLoaded(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> {
                        levelPlayNativeAd = nativeAd;
                        btnLoadNative.setEnabled(true);
                        String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "Mediation Partner";
                        Double rev = adInfo != null ? adInfo.getRevenue() : null;
                        String revText = rev != null ? String.format(Locale.US, " ($%.4f)", rev) : "";

                        tvNativeStatus.setText("RENDERED: " + network + revText);
                        tvNativeStatus.setTextColor(Color.parseColor("#34D399"));
                        appendLog("🟢 [NATIVE LOADED] Winner: " + network + revText);
                        renderNativeAdView(nativeAd);
                    });
                }

                @Override
                public void onAdLoadFailed(LevelPlayNativeAd nativeAd, IronSourceError error) {
                    runOnUiThread(() -> {
                        btnLoadNative.setEnabled(true);
                        String msg = error != null ? error.getErrorMessage() : "Unknown error";
                        int code = error != null ? error.getErrorCode() : -1;
                        tvNativeStatus.setText("FAILED (" + code + ")");
                        tvNativeStatus.setTextColor(Color.parseColor("#EF4444"));
                        appendLog("🔴 [NATIVE FAILED] " + msg + " (Code: " + code + ")");
                    });
                }

                @Override
                public void onAdImpression(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> appendLog("👁️ [NATIVE IMPRESSION] Impression logged."));
                }

                @Override
                public void onAdClicked(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> appendLog("🖱️ [NATIVE CLICK] Click logged."));
                }
            });

            levelPlayNativeAd = builder.build();
            levelPlayNativeAd.loadAd();

        } catch (Exception e) {
            btnLoadNative.setEnabled(true);
            tvNativeStatus.setText("EXCEPTION");
            tvNativeStatus.setTextColor(Color.parseColor("#EF4444"));
            appendLog("❌ [NATIVE ERROR] " + e.getMessage());
        }
    }

    private void renderNativeAdView(LevelPlayNativeAd nativeAd) {
        if (nativeAd == null || nativeAdContainer == null) return;
        try {
            nativeAdContainer.removeAllViews();
            NativeAdLayout nativeAdLayout = (NativeAdLayout) LayoutInflater.from(this)
                    .inflate(R.layout.layout_levelplay_native_ad, nativeAdContainer, false);

            TextView tvTitle = nativeAdLayout.findViewById(R.id.lp_native_title);
            TextView tvAdvertiser = nativeAdLayout.findViewById(R.id.lp_native_advertiser);
            TextView tvBody = nativeAdLayout.findViewById(R.id.lp_native_body);
            ImageView ivIcon = nativeAdLayout.findViewById(R.id.lp_native_icon);
            Button btnCta = nativeAdLayout.findViewById(R.id.lp_native_cta);
            LevelPlayMediaView mediaView = nativeAdLayout.findViewById(R.id.lp_native_media_view);

            if (tvTitle != null && nativeAd.getTitle() != null) {
                tvTitle.setText(nativeAd.getTitle());
                nativeAdLayout.setTitleView(tvTitle);
            }
            if (tvAdvertiser != null && nativeAd.getAdvertiser() != null) {
                tvAdvertiser.setText(nativeAd.getAdvertiser());
                nativeAdLayout.setAdvertiserView(tvAdvertiser);
            }
            if (tvBody != null && nativeAd.getBody() != null) {
                tvBody.setText(nativeAd.getBody());
                nativeAdLayout.setBodyView(tvBody);
            }
            if (btnCta != null && nativeAd.getCallToAction() != null) {
                btnCta.setText(nativeAd.getCallToAction());
                nativeAdLayout.setCallToActionView(btnCta);
            }
            if (ivIcon != null && nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                ivIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
                nativeAdLayout.setIconView(ivIcon);
            }
            if (mediaView != null) {
                nativeAdLayout.setMediaView(mediaView);
            }

            nativeAdLayout.registerNativeAdViews(nativeAd);
            nativeAdContainer.addView(nativeAdLayout);
            nativeAdContainer.setVisibility(View.VISIBLE);
            appendLog("🖼️ [NATIVE VIEW] Layout registered & rendered on screen.");

        } catch (Exception e) {
            Log.e(TAG, "Error rendering native ad", e);
            appendLog("❌ [NATIVE RENDER ERROR] " + e.getMessage());
        }
    }

    private void runIntegrationValidator() {
        appendLog("🩺 [VALIDATING LEVELPLAY ADAPTERS]");
        boolean metaSdkOk = checkClassExists("com.facebook.ads.AudienceNetworkAds");
        boolean metaAdapterOk = checkClassExists("com.ironsource.adapters.facebook.FacebookAdapter");
        appendLog(metaSdkOk ? "  ✅ Meta SDK: FOUND" : "  ❌ Meta SDK: MISSING");
        appendLog(metaAdapterOk ? "  ✅ Meta Adapter (LevelPlay): FOUND" : "  ❌ Meta Adapter: MISSING");

        boolean admobSdkOk = checkClassExists("com.google.android.gms.ads.MobileAds");
        boolean admobAdapterOk = checkClassExists("com.ironsource.adapters.admob.AdMobAdapter");
        appendLog(admobSdkOk ? "  ✅ AdMob SDK: FOUND" : "  ❌ AdMob SDK: MISSING");
        appendLog(admobAdapterOk ? "  ✅ AdMob Adapter (LevelPlay): FOUND" : "  ❌ AdMob Adapter: MISSING");

        try {
            IntegrationHelper.validateIntegration(this);
            appendLog("🩺 [IntegrationHelper] Dispatched official LevelPlay validation.");
            Toast.makeText(this, "LevelPlay Adapters Verified!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendLog("❌ [VALIDATION ERROR] " + e.getMessage());
        }
    }

    private boolean checkClassExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IronSource.onResume(this);
        updateDualAuctionStatusUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IronSource.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgress();
        AdManager.getInstance().setDebugAdListener(null);
        AdManager.getInstance().rebindLevelPlayListeners();
        if (levelPlayNativeAd != null) {
            levelPlayNativeAd.destroyAd();
            levelPlayNativeAd = null;
        }
        destroyAdMobMediatedInterstitial();
        destroyAdMobMediatedRewardedInterstitial();
        destroyAdMobMediatedNative();
        destroyDirectMetaInterstitial();
        destroyDirectMetaRewarded();
    }
    // =========================================================================
    // STANDALONE META AUDIENCE NETWORK DIRECT PLACEMENT TESTING
    // =========================================================================

    private void loadDirectMetaInterstitial() {
        destroyDirectMetaInterstitial();
        appendLog("════════════════════════════════════════════");
        appendLog("📡 [META DIRECT INTERSTITIAL] Requesting placement: " + AdManager.META_INTERSTITIAL_PLACEMENT);
        if (tvMetaDirectInterStatus != null) {
            tvMetaDirectInterStatus.setText("LOADING...");
            tvMetaDirectInterStatus.setTextColor(Color.parseColor("#FBBF24"));
        }
        if (btnLoadMetaDirectInter != null) btnLoadMetaDirectInter.setEnabled(false);
        if (btnShowMetaDirectInter != null) btnShowMetaDirectInter.setEnabled(false);

        metaDirectInterstitial = new InterstitialAd(this, AdManager.META_INTERSTITIAL_PLACEMENT);
        InterstitialAdListener listener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("📺 [META DIRECT INTERSTITIAL] Displayed on screen.");
                });
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("👋 [META DIRECT INTERSTITIAL] Dismissed by user.");
                    if (tvMetaDirectInterStatus != null) {
                        tvMetaDirectInterStatus.setText("DISMISSED");
                        tvMetaDirectInterStatus.setTextColor(Color.parseColor("#9CA3AF"));
                    }
                    if (btnShowMetaDirectInter != null) btnShowMetaDirectInter.setEnabled(false);
                    destroyDirectMetaInterstitial();
                });
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                runOnUiThread(() -> {
                    dismissProgress();
                    if (btnLoadMetaDirectInter != null) btnLoadMetaDirectInter.setEnabled(true);
                    if (btnShowMetaDirectInter != null) btnShowMetaDirectInter.setEnabled(false);
                    String msg = adError != null ? adError.getErrorMessage() : "Unknown";
                    int code = adError != null ? adError.getErrorCode() : -1;
                    if (tvMetaDirectInterStatus != null) {
                        tvMetaDirectInterStatus.setText("FAILED (" + code + ")");
                        tvMetaDirectInterStatus.setTextColor(Color.parseColor("#EF4444"));
                    }
                    appendLog("❌ [META DIRECT INTERSTITIAL FAILED] Code " + code + ": " + msg);
                    Toast.makeText(AdTestingActivity.this, "Meta Error " + code + ": " + msg, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onAdLoaded(Ad ad) {
                runOnUiThread(() -> {
                    if (btnLoadMetaDirectInter != null) btnLoadMetaDirectInter.setEnabled(true);
                    if (btnShowMetaDirectInter != null) btnShowMetaDirectInter.setEnabled(true);
                    if (tvMetaDirectInterStatus != null) {
                        tvMetaDirectInterStatus.setText("READY (Direct Meta Fill 🔥)");
                        tvMetaDirectInterStatus.setTextColor(Color.parseColor("#34D399"));
                    }
                    appendLog("🟢 [META DIRECT INTERSTITIAL READY] Ad loaded directly from Meta servers!");
                    Toast.makeText(AdTestingActivity.this, "Meta Interstitial Ready!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdClicked(Ad ad) {
                runOnUiThread(() -> appendLog("🖱️ [META DIRECT INTERSTITIAL] Clicked by user."));
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                runOnUiThread(() -> appendLog("👁️ [META DIRECT INTERSTITIAL] Impression recorded on Meta server!"));
            }
        };

        try {
            metaDirectInterstitial.loadAd(metaDirectInterstitial.buildLoadAdConfig().withAdListener(listener).build());
        } catch (Exception e) {
            appendLog("❌ [META DIRECT LOAD EXCEPTION] " + e.getMessage());
            if (btnLoadMetaDirectInter != null) btnLoadMetaDirectInter.setEnabled(true);
        }
    }

    private void showDirectMetaInterstitial() {
        if (metaDirectInterstitial != null && metaDirectInterstitial.isAdLoaded() && !metaDirectInterstitial.isAdInvalidated()) {
            showProgress("Displaying Direct Meta Interstitial...");
            metaDirectInterstitial.show();
        } else {
            Toast.makeText(this, "Meta Interstitial not loaded yet!", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ [META DIRECT INTERSTITIAL] Cannot show: Ad not ready.");
        }
    }

    private void destroyDirectMetaInterstitial() {
        if (metaDirectInterstitial != null) {
            try { metaDirectInterstitial.destroy(); } catch (Exception ignored) {}
            metaDirectInterstitial = null;
        }
    }

    private void loadDirectMetaRewarded() {
        destroyDirectMetaRewarded();
        appendLog("════════════════════════════════════════════");
        appendLog("📡 [META DIRECT REWARDED] Requesting placement: " + AdManager.META_REWARDED_PLACEMENT);
        if (tvMetaDirectRewardedStatus != null) {
            tvMetaDirectRewardedStatus.setText("LOADING...");
            tvMetaDirectRewardedStatus.setTextColor(Color.parseColor("#FBBF24"));
        }
        if (btnLoadMetaDirectRewarded != null) btnLoadMetaDirectRewarded.setEnabled(false);
        if (btnShowMetaDirectRewarded != null) btnShowMetaDirectRewarded.setEnabled(false);

        metaDirectRewarded = new RewardedVideoAd(this, AdManager.META_REWARDED_PLACEMENT);
        RewardedVideoAdListener listener = new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoCompleted() {
                runOnUiThread(() -> {
                    appendLog("🎁 [META DIRECT REWARD EARNED] Video completed! Reward unlocked.");
                    Toast.makeText(AdTestingActivity.this, "🎉 Meta Reward Granted!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                runOnUiThread(() -> appendLog("👁️ [META DIRECT REWARDED] Impression logged to Meta server."));
            }

            @Override
            public void onRewardedVideoClosed() {
                runOnUiThread(() -> {
                    dismissProgress();
                    appendLog("👋 [META DIRECT REWARDED] Video closed.");
                    if (tvMetaDirectRewardedStatus != null) {
                        tvMetaDirectRewardedStatus.setText("CLOSED");
                        tvMetaDirectRewardedStatus.setTextColor(Color.parseColor("#9CA3AF"));
                    }
                    if (btnShowMetaDirectRewarded != null) btnShowMetaDirectRewarded.setEnabled(false);
                    destroyDirectMetaRewarded();
                });
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                runOnUiThread(() -> {
                    dismissProgress();
                    if (btnLoadMetaDirectRewarded != null) btnLoadMetaDirectRewarded.setEnabled(true);
                    if (btnShowMetaDirectRewarded != null) btnShowMetaDirectRewarded.setEnabled(false);
                    String msg = adError != null ? adError.getErrorMessage() : "Unknown";
                    int code = adError != null ? adError.getErrorCode() : -1;
                    if (tvMetaDirectRewardedStatus != null) {
                        tvMetaDirectRewardedStatus.setText("FAILED (" + code + ")");
                        tvMetaDirectRewardedStatus.setTextColor(Color.parseColor("#EF4444"));
                    }
                    appendLog("❌ [META DIRECT REWARDED FAILED] Code " + code + ": " + msg);
                    Toast.makeText(AdTestingActivity.this, "Meta Rewarded Error " + code + ": " + msg, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onAdLoaded(Ad ad) {
                runOnUiThread(() -> {
                    if (btnLoadMetaDirectRewarded != null) btnLoadMetaDirectRewarded.setEnabled(true);
                    if (btnShowMetaDirectRewarded != null) btnShowMetaDirectRewarded.setEnabled(true);
                    if (tvMetaDirectRewardedStatus != null) {
                        tvMetaDirectRewardedStatus.setText("READY (Direct Meta Fill 🔥)");
                        tvMetaDirectRewardedStatus.setTextColor(Color.parseColor("#34D399"));
                    }
                    appendLog("🟢 [META DIRECT REWARDED READY] Video loaded directly from Meta!");
                    Toast.makeText(AdTestingActivity.this, "Meta Rewarded Video Ready!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdClicked(Ad ad) {
                runOnUiThread(() -> appendLog("🖱️ [META DIRECT REWARDED] User clicked ad."));
            }
        };

        try {
            metaDirectRewarded.loadAd(metaDirectRewarded.buildLoadAdConfig().withAdListener(listener).build());
        } catch (Exception e) {
            appendLog("❌ [META DIRECT LOAD EXCEPTION] " + e.getMessage());
            if (btnLoadMetaDirectRewarded != null) btnLoadMetaDirectRewarded.setEnabled(true);
        }
    }

    private void showDirectMetaRewarded() {
        if (metaDirectRewarded != null && metaDirectRewarded.isAdLoaded() && !metaDirectRewarded.isAdInvalidated()) {
            showProgress("Displaying Direct Meta Rewarded Video...");
            metaDirectRewarded.show();
        } else {
            Toast.makeText(this, "Meta Rewarded Video not loaded yet!", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ [META DIRECT REWARDED] Cannot show: Ad not ready.");
        }
    }

    private void destroyDirectMetaRewarded() {
        if (metaDirectRewarded != null) {
            try { metaDirectRewarded.destroy(); } catch (Exception ignored) {}
            metaDirectRewarded = null;
        }
    }


    // =========================================================================
    // UI BUILDER (Modern Dark Dashboard with AdMob Mediation & LevelPlay)
    // =========================================================================

    private View buildDashboardUi() {
        LinearLayout root = new LinearLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F172A")); // Slate 900

        // 1. Top Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(16), 0, dp(16), 0);
        topBar.setBackgroundColor(Color.parseColor("#1E293B"));

        TextView btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        btnBack.setPadding(dp(4), dp(4), dp(16), dp(4));
        btnBack.setOnClickListener(v -> finish());
        topBar.addView(btnBack);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Mediation Testing Dashboard");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvTitle.setTypeface(null, Typeface.BOLD);
        topBar.addView(tvTitle);

        root.addView(topBar);

        // 2. Scrollable Content
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));



                // ─────────────────────────────────────────────────────────────────────
        // CARD 1: GOOGLE ADMOB MEDIATION (WITH META AUDIENCE NETWORK BIDDING)
        // ─────────────────────────────────────────────────────────────────────
        LinearLayout cardAdMobMediation = createCard();
        cardAdMobMediation.addView(createSectionHeader("🔥 Google AdMob Mediation (Meta Bidding 1-Active)"));

        LinearLayout rowAdMobSdk = new LinearLayout(this);
        rowAdMobSdk.setOrientation(LinearLayout.HORIZONTAL);
        rowAdMobSdk.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvAdMobLabel = createSubText("Bidding Partner: Meta Audience Network");
        tvAdMobSdkStatus = new TextView(this);
        tvAdMobSdkStatus.setText("INITIALIZING...");
        tvAdMobSdkStatus.setTextColor(Color.parseColor("#FBBF24"));
        tvAdMobSdkStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvAdMobSdkStatus.setTypeface(null, Typeface.BOLD);
        tvAdMobSdkStatus.setGravity(Gravity.END);
        tvAdMobSdkStatus.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rowAdMobSdk.addView(tvAdMobLabel);
        rowAdMobSdk.addView(tvAdMobSdkStatus);
        cardAdMobMediation.addView(rowAdMobSdk);

        Button btnValidateAdMob = createButton("🩺 Verify AdMob-Meta Mediation Pipeline", "#3B82F6");
        btnValidateAdMob.setOnClickListener(v -> validateAdMobMetaMediationAdapters());
        cardAdMobMediation.addView(btnValidateAdMob);

        Button btnOpenInspector = createButton("🔍 Open AdMob Ad Inspector (Live Diagnostics)", "#4F46E5");
        btnOpenInspector.setOnClickListener(v -> openAdMobInspector());
        cardAdMobMediation.addView(btnOpenInspector);

        // 1.A AdMob Mediated Interstitial
        cardAdMobMediation.addView(createSubHeader("🎬 AdMob Interstitial (" + ADMOB_MEDIATION_INTERSTITIAL.substring(ADMOB_MEDIATION_INTERSTITIAL.length() - 10) + ")"));
        tvAdMobInterStatus = createStatusBadge("NOT LOADED");
        cardAdMobMediation.addView(tvAdMobInterStatus);

        LinearLayout rowInterBtns = new LinearLayout(this);
        rowInterBtns.setOrientation(LinearLayout.HORIZONTAL);
        rowInterBtns.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnLoadAdMobInter = createButton("1. Load Interstitial", "#0284C7");
        LinearLayout.LayoutParams pLoadInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pLoadInter.setMarginEnd(dp(6));
        btnLoadAdMobInter.setLayoutParams(pLoadInter);
        btnLoadAdMobInter.setOnClickListener(v -> loadAdMobMediatedInterstitial());

        btnShowAdMobInter = createButton("2. Show Interstitial", "#10B981");
        LinearLayout.LayoutParams pShowInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pShowInter.setMarginStart(dp(6));
        btnShowAdMobInter.setLayoutParams(pShowInter);
        btnShowAdMobInter.setEnabled(false);
        btnShowAdMobInter.setOnClickListener(v -> showAdMobMediatedInterstitial());

        rowInterBtns.addView(btnLoadAdMobInter);
        rowInterBtns.addView(btnShowAdMobInter);
        cardAdMobMediation.addView(rowInterBtns);

        // 1.B AdMob Mediated Rewarded Interstitial
        cardAdMobMediation.addView(createSubHeader("💎 AdMob Rewarded Interstitial (" + ADMOB_MEDIATION_REWARDED_INTERSTITIAL.substring(ADMOB_MEDIATION_REWARDED_INTERSTITIAL.length() - 10) + ")"));
        tvAdMobRewardedInterStatus = createStatusBadge("NOT LOADED");
        cardAdMobMediation.addView(tvAdMobRewardedInterStatus);

        LinearLayout rowRewInterBtns = new LinearLayout(this);
        rowRewInterBtns.setOrientation(LinearLayout.HORIZONTAL);
        rowRewInterBtns.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnLoadAdMobRewardedInter = createButton("1. Load Rewarded Interstitial", "#D97706");
        LinearLayout.LayoutParams pLoadRewInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pLoadRewInter.setMarginEnd(dp(6));
        btnLoadAdMobRewardedInter.setLayoutParams(pLoadRewInter);
        btnLoadAdMobRewardedInter.setOnClickListener(v -> loadAdMobMediatedRewardedInterstitial());

        btnShowAdMobRewardedInter = createButton("2. Show Rewarded Interstitial", "#10B981");
        LinearLayout.LayoutParams pShowRewInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pShowRewInter.setMarginStart(dp(6));
        btnShowAdMobRewardedInter.setLayoutParams(pShowRewInter);
        btnShowAdMobRewardedInter.setEnabled(false);
        btnShowAdMobRewardedInter.setOnClickListener(v -> showAdMobMediatedRewardedInterstitial());

        rowRewInterBtns.addView(btnLoadAdMobRewardedInter);
        rowRewInterBtns.addView(btnShowAdMobRewardedInter);
        cardAdMobMediation.addView(rowRewInterBtns);

        // 1.C AdMob Mediated Native Advanced
        cardAdMobMediation.addView(createSubHeader("🖼️ AdMob Native Advanced (" + ADMOB_MEDIATION_NATIVE.substring(ADMOB_MEDIATION_NATIVE.length() - 10) + ")"));
        tvAdMobNativeStatus = createStatusBadge("NOT LOADED");
        cardAdMobMediation.addView(tvAdMobNativeStatus);

        btnLoadAdMobNative = createButton("Load & Render AdMob Native (Meta Bidding)", "#EC4899");
        btnLoadAdMobNative.setOnClickListener(v -> loadAdMobMediatedNative());
        cardAdMobMediation.addView(btnLoadAdMobNative);

        admobNativeContainer = new FrameLayout(this);
        admobNativeContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        admobNativeContainer.setVisibility(View.GONE);
        admobNativeContainer.setPadding(0, dp(10), 0, 0);
        cardAdMobMediation.addView(admobNativeContainer);

        content.addView(cardAdMobMediation);

        // ─────────────────────────────────────────────────────────────────────
        // CARD 2: LEVELPLAY MEDIATION & DUAL AUCTION ENGINE
        // ─────────────────────────────────────────────────────────────────────
        LinearLayout cardLevelPlay = createCard();
        cardLevelPlay.addView(createSectionHeader("⚡ Unity LevelPlay Mediation (LevelPlay Main Mediator)"));

        LinearLayout rowSdk = new LinearLayout(this);
        rowSdk.setOrientation(LinearLayout.HORIZONTAL);
        rowSdk.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvSdkLabel = createSubText("App Key: " + LEVELPLAY_APP_KEY);
        tvSdkStatus = new TextView(this);
        tvSdkStatus.setText("ACTIVE");
        tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
        tvSdkStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSdkStatus.setTypeface(null, Typeface.BOLD);
        tvSdkStatus.setGravity(Gravity.END);
        tvSdkStatus.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rowSdk.addView(tvSdkLabel);
        rowSdk.addView(tvSdkStatus);
        cardLevelPlay.addView(rowSdk);

        Button btnValidate = createButton("🩺 Validate LevelPlay Adapters (IntegrationHelper)", "#475569");
        btnValidate.setOnClickListener(v -> runIntegrationValidator());
        cardLevelPlay.addView(btnValidate);

        Button btnTestSuite = createButton("🚀 Launch LevelPlay Test Suite", "#6366F1");
        btnTestSuite.setOnClickListener(v -> AdManager.getInstance().launchTestSuite(this));
        cardLevelPlay.addView(btnTestSuite);

        tvDualAuctionWinner = createStatusBadge("🏆 HIGHEST BIDDER: Checking auctions...");
        tvDualAuctionWinner.setTextColor(Color.parseColor("#38BDF8"));
        cardLevelPlay.addView(tvDualAuctionWinner);

        tvInterstitialStatus = createStatusBadge("🎬 Interstitial: PRELOADING...");
        cardLevelPlay.addView(tvInterstitialStatus);

        tvRewardedStatus = createStatusBadge("🎁 Rewarded: AUTO-CACHING...");
        cardLevelPlay.addView(tvRewardedStatus);

        // 2.A LevelPlay Interstitial Bidding Controls (Load & Show)
        cardLevelPlay.addView(createSubHeader("🎬 LevelPlay Interstitial Bidding (Direct Header Bidding)"));
        tvLevelPlayInterStatus = createStatusBadge(IronSource.isInterstitialReady() ? "READY" : "NOT LOADED");
        cardLevelPlay.addView(tvLevelPlayInterStatus);

        LinearLayout rowLpInterBtns = new LinearLayout(this);
        rowLpInterBtns.setOrientation(LinearLayout.HORIZONTAL);
        rowLpInterBtns.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnLoadLevelPlayInter = createButton("1. Load Interstitial Bidding", "#0284C7");
        LinearLayout.LayoutParams pLoadLpInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pLoadLpInter.setMarginEnd(dp(6));
        btnLoadLevelPlayInter.setLayoutParams(pLoadLpInter);
        btnLoadLevelPlayInter.setOnClickListener(v -> loadLevelPlayInterstitialBidding());

        btnShowLevelPlayInter = createButton("2. Show Interstitial Bidding", "#10B981");
        LinearLayout.LayoutParams pShowLpInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pShowLpInter.setMarginStart(dp(6));
        btnShowLevelPlayInter.setLayoutParams(pShowLpInter);
        btnShowLevelPlayInter.setEnabled(IronSource.isInterstitialReady());
        btnShowLevelPlayInter.setOnClickListener(v -> showLevelPlayInterstitialBidding());

        rowLpInterBtns.addView(btnLoadLevelPlayInter);
        rowLpInterBtns.addView(btnShowLevelPlayInter);
        cardLevelPlay.addView(rowLpInterBtns);

        // 2.B Dual Auction & Rewarded Video
        cardLevelPlay.addView(createSubHeader("⚖️ Dual-Auction & Rewarded Bidding"));
        btnWatchAdAndPost = createButton("🎬 Watch Ad & Post (LevelPlay Dual-Auction)", "#10B981");
        btnWatchAdAndPost.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnWatchAdAndPost.setOnClickListener(v -> handleWatchAdAndPostClick());
        cardLevelPlay.addView(btnWatchAdAndPost);

        Button btnWatchPureRewarded = createButton("🎁 LevelPlay Rewarded Video (Only Rewarded Bidding)", "#8B5CF6");
        btnWatchPureRewarded.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnWatchPureRewarded.setOnClickListener(v -> handleWatchPureRewardedClick());
        cardLevelPlay.addView(btnWatchPureRewarded);

        content.addView(cardLevelPlay);

        // ─────────────────────────────────────────────────────────────────────
        // CARD 2: LEVELPLAY NATIVE AD (RANDOM VIDEO CALL SEARCH)
        // ─────────────────────────────────────────────────────────────────────
        LinearLayout cardNative = createCard();
        cardNative.addView(createSectionHeader("🖼️ LevelPlay Native Ad (Random Call Search)"));
        cardNative.addView(createSubText("Placement ID: " + AdManager.LEVELPLAY_NATIVE_ID + " (Preloaded in ConnectingActivity)"));

        tvNativeStatus = createStatusBadge("NOT LOADED");
        cardNative.addView(tvNativeStatus);

        btnLoadNative = createButton("Load & Render LevelPlay Native Ad", "#EC4899");
        btnLoadNative.setOnClickListener(v -> loadNativeAd());
        cardNative.addView(btnLoadNative);

        nativeAdContainer = new FrameLayout(this);
        nativeAdContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        nativeAdContainer.setVisibility(View.GONE);
        nativeAdContainer.setPadding(0, dp(10), 0, 0);
        cardNative.addView(nativeAdContainer);

        content.addView(cardNative);

        // ─────────────────────────────────────────────────────────────────────
        // CARD 3: STANDALONE META AUDIENCE NETWORK DIRECT TESTING
        // ─────────────────────────────────────────────────────────────────────
        LinearLayout cardMetaDirect = createCard();
        cardMetaDirect.addView(createSectionHeader("🌐 Meta Audience Network (Direct Placements)"));
        cardMetaDirect.addView(createSubText("Bypasses mediation to verify Meta server delivery directly"));

        // Direct Interstitial
        cardMetaDirect.addView(createSubHeader("🎬 Meta Direct Interstitial (1063416116506979)"));
        tvMetaDirectInterStatus = createStatusBadge("NOT LOADED");
        cardMetaDirect.addView(tvMetaDirectInterStatus);

        LinearLayout rowMetaInterBtns = new LinearLayout(this);
        rowMetaInterBtns.setOrientation(LinearLayout.HORIZONTAL);
        rowMetaInterBtns.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnLoadMetaDirectInter = createButton("1. Load Meta Interstitial", "#0284C7");
        LinearLayout.LayoutParams pLoadMetaInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pLoadMetaInter.setMarginEnd(dp(6));
        btnLoadMetaDirectInter.setLayoutParams(pLoadMetaInter);
        btnLoadMetaDirectInter.setOnClickListener(v -> loadDirectMetaInterstitial());

        btnShowMetaDirectInter = createButton("2. Show Meta Interstitial", "#10B981");
        LinearLayout.LayoutParams pShowMetaInter = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pShowMetaInter.setMarginStart(dp(6));
        btnShowMetaDirectInter.setLayoutParams(pShowMetaInter);
        btnShowMetaDirectInter.setEnabled(false);
        btnShowMetaDirectInter.setOnClickListener(v -> showDirectMetaInterstitial());

        rowMetaInterBtns.addView(btnLoadMetaDirectInter);
        rowMetaInterBtns.addView(btnShowMetaDirectInter);
        cardMetaDirect.addView(rowMetaInterBtns);

        // Direct Rewarded
        cardMetaDirect.addView(createSubHeader("🎁 Meta Direct Rewarded (1063416119840312)"));
        tvMetaDirectRewardedStatus = createStatusBadge("NOT LOADED");
        cardMetaDirect.addView(tvMetaDirectRewardedStatus);

        LinearLayout rowMetaRewBtns = new LinearLayout(this);
        rowMetaRewBtns.setOrientation(LinearLayout.HORIZONTAL);
        rowMetaRewBtns.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnLoadMetaDirectRewarded = createButton("1. Load Meta Rewarded", "#8B5CF6");
        LinearLayout.LayoutParams pLoadMetaRew = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pLoadMetaRew.setMarginEnd(dp(6));
        btnLoadMetaDirectRewarded.setLayoutParams(pLoadMetaRew);
        btnLoadMetaDirectRewarded.setOnClickListener(v -> loadDirectMetaRewarded());

        btnShowMetaDirectRewarded = createButton("2. Show Meta Rewarded", "#10B981");
        LinearLayout.LayoutParams pShowMetaRew = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
        pShowMetaRew.setMarginStart(dp(6));
        btnShowMetaDirectRewarded.setLayoutParams(pShowMetaRew);
        btnShowMetaDirectRewarded.setEnabled(false);
        btnShowMetaDirectRewarded.setOnClickListener(v -> showDirectMetaRewarded());

        rowMetaRewBtns.addView(btnLoadMetaDirectRewarded);
        rowMetaRewBtns.addView(btnShowMetaDirectRewarded);
        cardMetaDirect.addView(rowMetaRewBtns);

        content.addView(cardMetaDirect);

        // ─────────────────────────────────────────────────────────────────────
        // CARD 4: REAL-TIME CONSOLE
        // ─────────────────────────────────────────────────────────────────────
        LinearLayout cardLogs = createCard();
        LinearLayout rowLogHeader = new LinearLayout(this);
        rowLogHeader.setOrientation(LinearLayout.HORIZONTAL);
        rowLogHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvLogTitle = createSectionHeader("📋 Live Mediation Console");
        TextView btnClearLogs = new TextView(this);
        btnClearLogs.setText("Clear");
        btnClearLogs.setTextColor(Color.parseColor("#38BDF8"));
        btnClearLogs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnClearLogs.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnClearLogs.setOnClickListener(v -> {
            logBuilder.setLength(0);
            tvConsoleLog.setText("");
        });
        rowLogHeader.addView(tvLogTitle);
        LinearLayout.LayoutParams pClear = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pClear.setMarginStart(dp(8));
        btnClearLogs.setLayoutParams(pClear);
        rowLogHeader.addView(btnClearLogs);
        cardLogs.addView(rowLogHeader);

        logScrollView = new ScrollView(this);
        logScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        logScrollView.setBackgroundColor(Color.parseColor("#020617")); // Slate 950
        logScrollView.setPadding(dp(10), dp(10), dp(10), dp(10));

        tvConsoleLog = new TextView(this);
        tvConsoleLog.setTextColor(Color.parseColor("#CBD5E1"));
        tvConsoleLog.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvConsoleLog.setTypeface(Typeface.MONOSPACE);
        tvConsoleLog.setText("Mediation Diagnostics Console ready...\n");
        logScrollView.addView(tvConsoleLog);

        cardLogs.addView(logScrollView);
        content.addView(cardLogs);

        scrollView.addView(content);
        root.addView(scrollView);

        return root;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E293B")); // Slate 800
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.parseColor("#334155"));
        card.setBackground(bg);

        return card;
    }

    private TextView createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private TextView createSubHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(12), 0, dp(4));
        return tv;
    }

    private TextView createSubText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private TextView createStatusBadge(String initialText) {
        TextView tv = new TextView(this);
        tv.setText(initialText);
        tv.setTextColor(Color.parseColor("#9CA3AF"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private Button createButton(String text, String hexColor) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        lp.setMargins(0, dp(6), 0, 0);
        btn.setLayoutParams(lp);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setTypeface(null, Typeface.BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(hexColor));
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);

        return btn;
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            String time = timeFormat.format(new Date());
            String line = "[" + time + "] " + message + "\n";
            logBuilder.append(line);
            if (tvConsoleLog != null) {
                tvConsoleLog.setText(logBuilder.toString());
            }
            if (logScrollView != null) {
                logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
            }
            Log.d(TAG, message);
        });
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
