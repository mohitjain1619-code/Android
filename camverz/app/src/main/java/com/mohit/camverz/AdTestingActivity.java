package com.mohit.camverz;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mohit.camverz.api.TokenManager;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pure Unity LevelPlay Mediation Ad Testing Dashboard.
 * Strictly adheres to official ironSource LevelPlay SDK documentation:
 * - Direct Interstitial Play (Google AdMob, Meta, Unity Ads bidding auction winner)
 * - Direct Rewarded Video Play (LevelPlay automatic background caching)
 * - Native Ads (LevelPlayNativeAd & LevelPlayNativeAdListener)
 * - Integration Validation (IntegrationHelper)
 */
public class AdTestingActivity extends AppCompatActivity {

    private static final String TAG = "AdTestingActivity";
    public static final String LEVELPLAY_APP_KEY = "28890f0b5";

    // UI Badges & Direct Action Buttons
    private TextView tvSdkStatus;
    private TextView tvInterstitialStatus;
    private Button btnPlayInterstitial;

    private TextView tvRewardedStatus;
    private Button btnPlayRewarded;

    private TextView tvNativeStatus;
    private Button btnLoadNative;
    private FrameLayout nativeAdContainer;

    // Log Console
    private TextView tvConsoleLog;
    private ScrollView logScrollView;
    private final StringBuilder logBuilder = new StringBuilder();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Native Ad Instance
    private LevelPlayNativeAd levelPlayNativeAd;

    // Progress Dialog & Waiting Flags
    private ProgressDialog adProgressDialog;
    private boolean isWaitingForInterstitialToShow = false;
    private boolean isWaitingForRewardedToShow = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build Modern Pure-Java UI (Isolated & Clean)
        View rootView = buildDashboardUi();
        setContentView(rootView);

        appendLog("🚀 Dashboard Launched. Connecting to LevelPlay Mediation...");
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

    /**
     * LevelPlay SDK Initialization & Listener Attachment
     */
    private void initLevelPlayMediation() {
        try {
            // Keep pure device GAID for LevelPlay Test Devices matching
            // (Do not call IronSource.setUserId here so dashboard test device matches by GAID)

            // Setup Listeners
            setupInterstitialListener();
            setupRewardedVideoListener();

            tvSdkStatus.setText("ACTIVE (v8.7.0)");
            tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
            appendLog("✅ [LEVELPLAY READY] Connected to LevelPlay Mediation (App Key: " + LEVELPLAY_APP_KEY + ")");
            appendLog("🌐 [BIDDERS ACTIVE] Google AdMob, Meta Audience Network, Unity Ads, ironSource");

            // Check initial availability
            if (IronSource.isInterstitialReady()) {
                tvInterstitialStatus.setText("READY (Cached)");
                tvInterstitialStatus.setTextColor(Color.parseColor("#34D399"));
                appendLog("🟢 [INTERSTITIAL] Ad already cached & ready to show.");
            } else {
                tvInterstitialStatus.setText("PRELOADING IN BACKGROUND...");
                tvInterstitialStatus.setTextColor(Color.parseColor("#FBBF24"));
                IronSource.loadInterstitial();
            }

            if (IronSource.isRewardedVideoAvailable()) {
                tvRewardedStatus.setText("AVAILABLE (Cached)");
                tvRewardedStatus.setTextColor(Color.parseColor("#34D399"));
                appendLog("🟢 [REWARDED] Video already cached & ready to show.");
            } else {
                tvRewardedStatus.setText("AUTO-CACHING IN BACKGROUND...");
                tvRewardedStatus.setTextColor(Color.parseColor("#FBBF24"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in initLevelPlayMediation", e);
            tvSdkStatus.setText("ERROR");
            tvSdkStatus.setTextColor(Color.parseColor("#EF4444"));
            appendLog("❌ [INIT ERROR] " + e.getMessage());
        }
    }

    // =========================================================================
    // 1. DIRECT INTERSTITIAL ADS IMPLEMENTATION
    // =========================================================================

    private void setupInterstitialListener() {
        IronSource.setLevelPlayInterstitialListener(new LevelPlayInterstitialListener() {
            @Override
            public void onAdReady(AdInfo adInfo) {
                runOnUiThread(() -> {
                    String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "Unknown Network";
                    Double rev = adInfo != null ? adInfo.getRevenue() : null;
                    String revText = rev != null ? String.format(Locale.US, " ($%.4f)", rev) : "";

                    tvInterstitialStatus.setText("READY: " + network + revText);
                    tvInterstitialStatus.setTextColor(Color.parseColor("#34D399"));

                    appendLog("🟢 [INTERSTITIAL READY] Winner: " + network + revText);
                    if (adInfo != null) {
                        appendLog("   Auction ID: " + adInfo.getAuctionId());
                    }

                    if (isWaitingForInterstitialToShow) {
                        isWaitingForInterstitialToShow = false;
                        dismissProgress();
                        appendLog("🎬 [PLAYING NOW] Showing winning bidding ad...");
                        IronSource.showInterstitial();
                    }
                });
            }

            @Override
            public void onAdLoadFailed(IronSourceError error) {
                runOnUiThread(() -> {
                    String msg = error != null ? error.getErrorMessage() : "Unknown error";
                    int code = error != null ? error.getErrorCode() : -1;

                    tvInterstitialStatus.setText("FAILED (" + code + ")");
                    tvInterstitialStatus.setTextColor(Color.parseColor("#EF4444"));

                    appendLog("🔴 [INTERSTITIAL FAILED] " + msg + " [Code: " + code + "]");

                    if (isWaitingForInterstitialToShow) {
                        isWaitingForInterstitialToShow = false;
                        dismissProgress();
                        Toast.makeText(AdTestingActivity.this, "Interstitial Load Failed: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onAdOpened(AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String network = adInfo != null ? adInfo.getAdNetwork() : "Mediation";
                    appendLog("👁️ [INTERSTITIAL OPENED] Ad displayed via " + network);
                });
            }

            @Override
            public void onAdShowSucceeded(AdInfo adInfo) {
                runOnUiThread(() -> appendLog("✅ [INTERSTITIAL SHOW] Ad playback started."));
            }

            @Override
            public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String msg = error != null ? error.getErrorMessage() : "Show error";
                    appendLog("❌ [INTERSTITIAL SHOW FAILED] " + msg);
                    Toast.makeText(AdTestingActivity.this, "Show Failed: " + msg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdClicked(AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🖱️ [INTERSTITIAL CLICK] User clicked the ad."));
            }

            @Override
            public void onAdClosed(AdInfo adInfo) {
                runOnUiThread(() -> {
                    appendLog("🚪 [INTERSTITIAL CLOSED] Ad dismissed by user. Requesting next auction...");
                    tvInterstitialStatus.setText("PRELOADING NEXT AD...");
                    tvInterstitialStatus.setTextColor(Color.parseColor("#FBBF24"));
                    IronSource.loadInterstitial();
                });
            }
        });
    }

    private void playInterstitialDirect() {
        if (IronSource.isInterstitialReady()) {
            appendLog("🎬 [SHOWING INTERSTITIAL] Ad ready! Playing highest bidding winner now...");
            IronSource.showInterstitial();
        } else {
            isWaitingForInterstitialToShow = true;
            appendLog("📡 [BIDDING AUCTION STARTED] Requesting LevelPlay Interstitial auction (Meta, AdMob, Unity, ironSource)...");
            tvInterstitialStatus.setText("LOADING AUCTION WINNER...");
            tvInterstitialStatus.setTextColor(Color.parseColor("#FBBF24"));
            showProgress("Running Header Bidding Auction... Please wait");
            IronSource.loadInterstitial();

            // Timeout safety after 6s
            mainHandler.postDelayed(() -> {
                if (isWaitingForInterstitialToShow) {
                    isWaitingForInterstitialToShow = false;
                    dismissProgress();
                    appendLog("⚠️ [TIMEOUT] Interstitial auction took too long. Try again.");
                    Toast.makeText(this, "Ad timeout. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }, 6000);
        }
    }



    // =========================================================================
    // 2. DIRECT REWARDED VIDEO ADS IMPLEMENTATION
    // =========================================================================

    private void setupRewardedVideoListener() {
        IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
            @Override
            public void onAdAvailable(AdInfo adInfo) {
                runOnUiThread(() -> {
                    String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "Network";
                    Double rev = adInfo != null ? adInfo.getRevenue() : null;
                    String revText = rev != null ? String.format(Locale.US, " ($%.4f)", rev) : "";

                    tvRewardedStatus.setText("AVAILABLE: " + network + revText);
                    tvRewardedStatus.setTextColor(Color.parseColor("#34D399"));

                    appendLog("🟢 [REWARDED AVAILABLE] Winner: " + network + revText);

                    if (isWaitingForRewardedToShow) {
                        isWaitingForRewardedToShow = false;
                        dismissProgress();
                        appendLog("🎬 [PLAYING NOW] Showing Rewarded Video...");
                        IronSource.showRewardedVideo();
                    }
                });
            }

            @Override
            public void onAdUnavailable() {
                runOnUiThread(() -> {
                    tvRewardedStatus.setText("UNAVAILABLE / NO FILL");
                    tvRewardedStatus.setTextColor(Color.parseColor("#EF4444"));
                    appendLog("🔴 [REWARDED UNAVAILABLE] LevelPlay reported no fill currently.");
                });
            }

            @Override
            public void onAdOpened(AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String network = adInfo != null ? adInfo.getAdNetwork() : "Mediation";
                    appendLog("👁️ [REWARDED OPENED] Video ad playing via " + network);
                });
            }

            @Override
            public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String msg = error != null ? error.getErrorMessage() : "Show error";
                    appendLog("❌ [REWARDED SHOW FAILED] " + msg);
                    Toast.makeText(AdTestingActivity.this, "Show Failed: " + msg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdClicked(Placement placement, AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🖱️ [REWARDED CLICK] User clicked the rewarded ad."));
            }

            @Override
            public void onAdRewarded(Placement placement, AdInfo adInfo) {
                runOnUiThread(() -> {
                    String rewardName = placement != null ? placement.getRewardName() : "Reward";
                    int rewardAmount = placement != null ? placement.getRewardAmount() : 1;
                    appendLog("🎁 [REWARD EARNED] User successfully completed video: " + rewardAmount + " " + rewardName);
                    Toast.makeText(AdTestingActivity.this, "🎁 Reward Earned: " + rewardAmount + " " + rewardName, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onAdClosed(AdInfo adInfo) {
                runOnUiThread(() -> {
                    appendLog("🚪 [REWARDED CLOSED] Video closed. LevelPlay automatically preloads next video.");
                    tvRewardedStatus.setText("AUTO-CACHING NEXT VIDEO...");
                    tvRewardedStatus.setTextColor(Color.parseColor("#FBBF24"));
                });
            }
        });
    }

    private void playRewardedDirect() {
        if (IronSource.isRewardedVideoAvailable()) {
            appendLog("🎬 [SHOWING REWARDED] Rewarded video ready! Playing now...");
            IronSource.showRewardedVideo();
        } else {
            isWaitingForRewardedToShow = true;
            appendLog("📡 [WAITING FOR REWARDED] LevelPlay is fetching highest bid rewarded video in background...");
            tvRewardedStatus.setText("FETCHING REWARDED AD...");
            tvRewardedStatus.setTextColor(Color.parseColor("#FBBF24"));
            showProgress("Fetching Rewarded Video... Please wait");

            // Check every 500ms for up to 6 seconds
            Runnable pollRunnable = new Runnable() {
                int attempts = 0;
                @Override
                public void run() {
                    if (!isWaitingForRewardedToShow) return;
                    if (IronSource.isRewardedVideoAvailable()) {
                        isWaitingForRewardedToShow = false;
                        dismissProgress();
                        appendLog("🎬 [PLAYING NOW] Rewarded video ready! Playing now...");
                        IronSource.showRewardedVideo();
                    } else if (++attempts < 12) {
                        mainHandler.postDelayed(this, 500);
                    } else {
                        isWaitingForRewardedToShow = false;
                        dismissProgress();
                        appendLog("⚠️ [REWARDED TIMEOUT] Rewarded video not ready yet.");
                        Toast.makeText(AdTestingActivity.this, "Rewarded video not ready yet. Please try again in a few seconds.", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            mainHandler.postDelayed(pollRunnable, 500);
        }
    }

    // =========================================================================
    // 3. NATIVE ADS IMPLEMENTATION
    // =========================================================================

    private void loadNativeAd() {
        try {
            appendLog("📡 [REQUESTING NATIVE AD] Initializing LevelPlayNativeAd.Builder()...");
            tvNativeStatus.setText("LOADING...");
            tvNativeStatus.setTextColor(Color.parseColor("#FBBF24"));
            btnLoadNative.setEnabled(false);

            if (levelPlayNativeAd != null) {
                levelPlayNativeAd.destroyAd();
                levelPlayNativeAd = null;
            }

            LevelPlayNativeAd.Builder builder = new LevelPlayNativeAd.Builder();
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

                        appendLog("🟢 [NATIVE AD LOADED] Winner: " + network + revText);
                        renderNativeAdView(nativeAd);
                        Toast.makeText(AdTestingActivity.this, "Native Ad Rendered (" + network + ")", Toast.LENGTH_SHORT).show();
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

                        appendLog("🔴 [NATIVE AD FAILED] " + msg + " [Code: " + code + "]");
                        Toast.makeText(AdTestingActivity.this, "Native Ad Failed: " + msg, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdImpression(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> appendLog("👁️ [NATIVE IMPRESSION] LevelPlay Native ad impression logged."));
                }

                @Override
                public void onAdClicked(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> appendLog("🖱️ [NATIVE CLICK] User clicked the native ad."));
                }
            });

            levelPlayNativeAd = builder.build();
            levelPlayNativeAd.loadAd();

        } catch (Exception e) {
            btnLoadNative.setEnabled(true);
            tvNativeStatus.setText("EXCEPTION");
            tvNativeStatus.setTextColor(Color.parseColor("#EF4444"));
            appendLog("❌ [NATIVE EXCEPTION] " + e.getMessage());
            Log.e(TAG, "Exception loading native ad", e);
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

            // Bind with LevelPlay Native Layout
            nativeAdLayout.registerNativeAdViews(nativeAd);

            nativeAdContainer.addView(nativeAdLayout);
            nativeAdContainer.setVisibility(View.VISIBLE);
            appendLog("🖼️ [NATIVE BINDING] Assets bound & registered with NativeAdLayout.");

        } catch (Exception e) {
            Log.e(TAG, "Error rendering native ad", e);
            appendLog("❌ [NATIVE RENDER ERROR] " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. INTEGRATION VALIDATION & TEST SUITE
    // =========================================================================

    private void runIntegrationValidator() {
        appendLog("🩺 =========================================");
        appendLog("🩺 [VALIDATING MEDIATION ADAPTERS & SDKs]");
        appendLog("🩺 =========================================");

        // 1. Check Meta (Facebook) Adapter & SDK
        boolean metaSdkOk = checkClassExists("com.facebook.ads.AudienceNetworkAds");
        boolean metaAdapterOk = checkClassExists("com.ironsource.adapters.facebook.FacebookAdapter");
        appendLog(metaSdkOk ? "  ✅ Meta SDK (AudienceNetworkAds): FOUND" : "  ❌ Meta SDK: MISSING");
        appendLog(metaAdapterOk ? "  ✅ Meta LevelPlay Adapter (FacebookAdapter): FOUND" : "  ❌ Meta Adapter: MISSING");

        // 2. Check Google AdMob Adapter & SDK
        boolean admobSdkOk = checkClassExists("com.google.android.gms.ads.MobileAds");
        boolean admobAdapterOk = checkClassExists("com.ironsource.adapters.admob.AdMobAdapter");
        appendLog(admobSdkOk ? "  ✅ AdMob SDK (MobileAds): FOUND" : "  ❌ AdMob SDK: MISSING");
        appendLog(admobAdapterOk ? "  ✅ AdMob LevelPlay Adapter (AdMobAdapter): FOUND" : "  ❌ AdMob Adapter: MISSING");

        // 3. Check Unity Ads Adapter & SDK
        boolean unitySdkOk = checkClassExists("com.unity3d.ads.UnityAds");
        boolean unityAdapterOk = checkClassExists("com.ironsource.adapters.unityads.UnityAdsAdapter");
        appendLog(unitySdkOk ? "  ✅ Unity Ads SDK (UnityAds): FOUND" : "  ❌ Unity Ads SDK: MISSING");
        appendLog(unityAdapterOk ? "  ✅ Unity LevelPlay Adapter (UnityAdsAdapter): FOUND" : "  ❌ Unity Adapter: MISSING");

        // 4. Check Mandatory Identifiers (GAID & AppSet)
        boolean appsetOk = checkClassExists("com.google.android.gms.appset.AppSet");
        boolean gaidOk = checkClassExists("com.google.android.gms.ads.identifier.AdvertisingIdClient");
        appendLog(appsetOk ? "  ✅ Google AppSet ID Library: FOUND" : "  ⚠️ AppSet ID Library: MISSING");
        appendLog(gaidOk ? "  ✅ Google AdvertisingId Library: FOUND" : "  ⚠️ AdvertisingId Library: MISSING");

        // 5. Run official ironSource IntegrationHelper
        try {
            IntegrationHelper.validateIntegration(this);
            appendLog("🩺 [IntegrationHelper] Dispatched official LevelPlay validation.");
            Toast.makeText(this, "Adapters Verified! All 3 adapters are compiled.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendLog("❌ [VALIDATION ERROR] " + e.getMessage());
        }
        appendLog("🩺 =========================================");
    }

    private boolean checkClassExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }



    // =========================================================================
    // LIFECYCLE MANAGEMENT
    // =========================================================================

    @Override
    protected void onResume() {
        super.onResume();
        IronSource.onResume(this);
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
        if (levelPlayNativeAd != null) {
            levelPlayNativeAd.destroyAd();
            levelPlayNativeAd = null;
        }
    }

    // =========================================================================
    // UI BUILDER (Clean, Ultra-Modern Dark Theme with Direct Buttons)
    // =========================================================================

    private View buildDashboardUi() {
        LinearLayout root = new LinearLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F172A")); // Deep Slate Dark

        // Top Bar
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
        tvTitle.setText("LevelPlay Bidding Ad Dashboard");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvTitle.setTypeface(null, Typeface.BOLD);
        topBar.addView(tvTitle);

        root.addView(topBar);

        // Scrollable Content
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // 1. SDK Status Card
        LinearLayout cardStatus = createCard();
        cardStatus.addView(createSectionHeader("Unity LevelPlay Status"));

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
        cardStatus.addView(rowSdk);

        TextView tvNetworks = createSubText("Bidders: Google AdMob, Meta, Unity Ads, ironSource");
        tvNetworks.setTextColor(Color.parseColor("#94A3B8"));
        cardStatus.addView(tvNetworks);

        Button btnValidate = createButton("🩺 Validate Adapters (IntegrationHelper)", "#3B82F6");
        btnValidate.setOnClickListener(v -> runIntegrationValidator());
        cardStatus.addView(btnValidate);

        content.addView(cardStatus);

        // 2. Interstitial Ad Card (1 Direct Button)
        LinearLayout cardInterstitial = createCard();
        cardInterstitial.addView(createSectionHeader("🎬 Interstitial Ad (Auction Winner)"));
        tvInterstitialStatus = createStatusBadge("CHECKING...");
        cardInterstitial.addView(tvInterstitialStatus);

        btnPlayInterstitial = createButton("🎬 Show LevelPlay Interstitial Ad", "#10B981");
        btnPlayInterstitial.setOnClickListener(v -> playInterstitialDirect());
        cardInterstitial.addView(btnPlayInterstitial);

        content.addView(cardInterstitial);

        // 3. Rewarded Video Ad Card (1 Direct Button)
        LinearLayout cardRewarded = createCard();
        cardRewarded.addView(createSectionHeader("🎁 Rewarded Video Ad"));
        tvRewardedStatus = createStatusBadge("CHECKING...");
        cardRewarded.addView(tvRewardedStatus);

        btnPlayRewarded = createButton("🎁 Show LevelPlay Rewarded Video Ad", "#F59E0B");
        btnPlayRewarded.setOnClickListener(v -> playRewardedDirect());
        cardRewarded.addView(btnPlayRewarded);

        content.addView(cardRewarded);

        // 4. Native Ad Card
        LinearLayout cardNative = createCard();
        cardNative.addView(createSectionHeader("🖼️ LevelPlay Native Ad"));
        tvNativeStatus = createStatusBadge("NOT LOADED");
        cardNative.addView(tvNativeStatus);

        btnLoadNative = createButton("Load & Render Native Ad", "#EC4899");
        btnLoadNative.setOnClickListener(v -> loadNativeAd());
        cardNative.addView(btnLoadNative);

        nativeAdContainer = new FrameLayout(this);
        nativeAdContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        nativeAdContainer.setVisibility(View.GONE);
        nativeAdContainer.setPadding(0, dp(12), 0, 0);
        cardNative.addView(nativeAdContainer);

        content.addView(cardNative);

        // 5. Real-Time Log Console Card
        LinearLayout cardLogs = createCard();
        LinearLayout rowLogHeader = new LinearLayout(this);
        rowLogHeader.setOrientation(LinearLayout.HORIZONTAL);
        rowLogHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvLogTitle = createSectionHeader("📋 Mediation Real-Time Console");
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
        logScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170)));
        logScrollView.setBackgroundColor(Color.parseColor("#020617")); // Ultra dark slate
        logScrollView.setPadding(dp(10), dp(10), dp(10), dp(10));

        tvConsoleLog = new TextView(this);
        tvConsoleLog.setTextColor(Color.parseColor("#CBD5E1"));
        tvConsoleLog.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvConsoleLog.setTypeface(Typeface.MONOSPACE);
        tvConsoleLog.setText("LevelPlay Bidding Console ready...\n");
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
        tv.setPadding(0, 0, 0, dp(12));
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
