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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.Date;
import java.util.Locale;

/**
 * Clean LevelPlay Dual-Auction Bidding Ad Dashboard.
 * - Single direct "Watch Ad & Post" action powered by Dual-Auction (Highest eCPM)
 * - Parallel auctions across Meta Audience Network, Google AdMob, Unity Ads, and ironSource
 * - Real-time eCPM comparison & live mediation console
 */
public class AdTestingActivity extends AppCompatActivity {

    private static final String TAG = "AdTestingActivity";
    public static final String LEVELPLAY_APP_KEY = "28890f0b5";

    // Dual-Auction Status Badges
    private TextView tvSdkStatus;
    private TextView tvDualAuctionWinner;
    private TextView tvInterstitialStatus;
    private TextView tvRewardedStatus;
    private Button btnWatchAdAndPost;

    // Native Ad UI
    private TextView tvNativeStatus;
    private Button btnLoadNative;
    private FrameLayout nativeAdContainer;
    private LevelPlayNativeAd levelPlayNativeAd;

    // Log Console
    private TextView tvConsoleLog;
    private ScrollView logScrollView;
    private final StringBuilder logBuilder = new StringBuilder();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Tracking
    private ProgressDialog adProgressDialog;
    private boolean isWaitingForDualAdToShow = false;
    private AdInfo cachedInterstitialAdInfo;
    private AdInfo cachedRewardedAdInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build Clean, Modern UI
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
     * LevelPlay SDK Initialization & Parallel Auction Listeners
     */
    private void initLevelPlayMediation() {
        try {
            // Test Suite mode for jainmohit8120@gmail.com
            String email = TokenManager.getInstance(this).getUserEmail();
            if (email != null && "jainmohit8120@gmail.com".equalsIgnoreCase(email.trim())) {
                IronSource.setMetaData("is_test_suite", "enable");
                try {
                    com.facebook.ads.AdSettings.setTestMode(true);
                    com.facebook.ads.AdSettings.turnOnSDKDebugger(this);
                    AdManager.applyMetaTestDeviceHash(this);
                } catch (Exception ignored) {}
                appendLog("🧪 [TEST SUITE MODE] Enabled for jainmohit8120@gmail.com");
            }

            // Setup Listeners for parallel auctions
            setupInterstitialListener();
            setupRewardedVideoListener();

            tvSdkStatus.setText("ACTIVE (v8.7.0)");
            tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
            appendLog("✅ [LEVELPLAY READY] App Key: " + LEVELPLAY_APP_KEY);
            appendLog("🌐 [BIDDERS ACTIVE] Meta Audience Network, Google AdMob, Unity Ads, ironSource");

            // Initiate parallel bidding
            IronSource.loadInterstitial();
            AdManager.getInstance().preloadDualBiddingAds(this);

            updateDualAuctionStatusUi();

        } catch (Exception e) {
            Log.e(TAG, "Error in initLevelPlayMediation", e);
            tvSdkStatus.setText("ERROR");
            tvSdkStatus.setTextColor(Color.parseColor("#EF4444"));
            appendLog("❌ [INIT ERROR] " + e.getMessage());
        }
    }

    // =========================================================================
    // DUAL-AUCTION LISTENERS (INTERSTITIAL + REWARDED)
    // =========================================================================

    private void setupInterstitialListener() {
        IronSource.setLevelPlayInterstitialListener(new LevelPlayInterstitialListener() {
            @Override
            public void onAdReady(AdInfo adInfo) {
                runOnUiThread(() -> {
                    cachedInterstitialAdInfo = adInfo;
                    String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "Meta/Mediation";
                    Double rev = adInfo != null ? adInfo.getRevenue() : null;
                    String revText = rev != null ? String.format(Locale.US, " ($%.4f)", rev) : "";

                    appendLog("🟢 [INTERSTITIAL READY] Winner: " + network + revText);
                    updateDualAuctionStatusUi();

                    if (isWaitingForDualAdToShow) {
                        isWaitingForDualAdToShow = false;
                        dismissProgress();
                        executeDualAuctionWatchAdAndPost();
                    }
                });
            }

            @Override
            public void onAdLoadFailed(IronSourceError error) {
                runOnUiThread(() -> {
                    cachedInterstitialAdInfo = null;
                    String msg = error != null ? error.getErrorMessage() : "Unknown";
                    int code = error != null ? error.getErrorCode() : -1;
                    appendLog("⚠️ [INTERSTITIAL AUCTION] " + msg + " (Code: " + code + ")");
                    updateDualAuctionStatusUi();
                });
            }

            @Override
            public void onAdOpened(AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String network = adInfo != null ? adInfo.getAdNetwork() : "Mediation";
                    appendLog("👁️ [AD OPENED] Displayed via " + network);
                });
            }

            @Override
            public void onAdShowSucceeded(AdInfo adInfo) {
                runOnUiThread(() -> appendLog("✅ [AD SHOW] Playback started successfully."));
            }

            @Override
            public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
                runOnUiThread(() -> {
                    dismissProgress();
                    String msg = error != null ? error.getErrorMessage() : "Show error";
                    appendLog("❌ [AD SHOW FAILED] " + msg);
                });
            }

            @Override
            public void onAdClicked(AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🖱️ [AD CLICK] User clicked the ad."));
            }

            @Override
            public void onAdClosed(AdInfo adInfo) {
                runOnUiThread(() -> {
                    appendLog("🚪 [AD CLOSED] User dismissed ad. Loading next auction...");
                    IronSource.loadInterstitial();
                    updateDualAuctionStatusUi();
                });
            }
        });
    }

    private void setupRewardedVideoListener() {
        IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
            @Override
            public void onAdAvailable(AdInfo adInfo) {
                runOnUiThread(() -> {
                    cachedRewardedAdInfo = adInfo;
                    String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "AdMob/Unity";
                    Double rev = adInfo != null ? adInfo.getRevenue() : null;
                    String revText = rev != null ? String.format(Locale.US, " ($%.4f)", rev) : "";

                    appendLog("🟢 [REWARDED AVAILABLE] Winner: " + network + revText);
                    updateDualAuctionStatusUi();

                    if (isWaitingForDualAdToShow) {
                        isWaitingForDualAdToShow = false;
                        dismissProgress();
                        executeDualAuctionWatchAdAndPost();
                    }
                });
            }

            @Override
            public void onAdUnavailable() {
                runOnUiThread(() -> {
                    cachedRewardedAdInfo = null;
                    appendLog("ℹ️ [REWARDED UNAVAILABLE] LevelPlay reported no fill for Rewarded Video currently.");
                    updateDualAuctionStatusUi();
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
                });
            }

            @Override
            public void onAdClicked(Placement placement, AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🖱️ [REWARDED CLICK] User clicked rewarded ad."));
            }

            @Override
            public void onAdRewarded(Placement placement, AdInfo adInfo) {
                runOnUiThread(() -> appendLog("🎁 [REWARD EARNED] Video reward unlocked for post publication!"));
            }

            @Override
            public void onAdClosed(AdInfo adInfo) {
                runOnUiThread(() -> {
                    appendLog("🚪 [REWARDED CLOSED] Rewarded video closed. Preloading next...");
                    updateDualAuctionStatusUi();
                });
            }
        });
    }

    /**
     * Live UI update for the Dual-Auction state
     */
    private void updateDualAuctionStatusUi() {
        boolean interReady = IronSource.isInterstitialReady();
        boolean rewReady = IronSource.isRewardedVideoAvailable();

        AdInfo interInfo = cachedInterstitialAdInfo != null ? cachedInterstitialAdInfo : AdManager.getInstance().getLatestInterstitialAdInfo();
        AdInfo rewInfo = cachedRewardedAdInfo != null ? cachedRewardedAdInfo : AdManager.getInstance().getLatestRewardedAdInfo();

        Double interRev = interInfo != null ? interInfo.getRevenue() : null;
        String interNet = interInfo != null && interInfo.getAdNetwork() != null ? interInfo.getAdNetwork() : "Meta/Mediation";

        Double rewRev = rewInfo != null ? rewInfo.getRevenue() : null;
        String rewNet = rewInfo != null && rewInfo.getAdNetwork() != null ? rewInfo.getAdNetwork() : "AdMob/Unity";

        // 1. Interstitial Status
        if (interReady) {
            String txt = "READY: " + interNet + (interRev != null ? String.format(Locale.US, " ($%.4f)", interRev) : "");
            tvInterstitialStatus.setText("🎬 Interstitial: " + txt);
            tvInterstitialStatus.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvInterstitialStatus.setText("🎬 Interstitial: Preloading in background...");
            tvInterstitialStatus.setTextColor(Color.parseColor("#FBBF24"));
        }

        // 2. Rewarded Status
        if (rewReady) {
            String txt = "READY: " + rewNet + (rewRev != null ? String.format(Locale.US, " ($%.4f)", rewRev) : "");
            tvRewardedStatus.setText("🎁 Rewarded: " + txt);
            tvRewardedStatus.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvRewardedStatus.setText("🎁 Rewarded: Auto-caching / Unavailable");
            tvRewardedStatus.setTextColor(Color.parseColor("#94A3B8"));
        }

        // 3. Leading Winner Badge
        if (interReady && rewReady) {
            double rVal = rewRev != null ? rewRev : 0.0;
            double iVal = interRev != null ? interRev : 0.0;
            if (iVal >= rVal) {
                tvDualAuctionWinner.setText("🏆 HIGHEST BIDDER: " + interNet + " (Interstitial) $" + String.format(Locale.US, "%.4f", iVal));
                tvDualAuctionWinner.setTextColor(Color.parseColor("#38BDF8"));
            } else {
                tvDualAuctionWinner.setText("🏆 HIGHEST BIDDER: " + rewNet + " (Rewarded) $" + String.format(Locale.US, "%.4f", rVal));
                tvDualAuctionWinner.setTextColor(Color.parseColor("#38BDF8"));
            }
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

    // =========================================================================
    // DUAL-AUCTION CORE FLOW: "WATCH AD & POST"
    // =========================================================================

    private void handleWatchAdAndPostClick() {
        appendLog("════════════════════════════════════════════");
        appendLog("🎬 [USER ACTION] Tap 'Watch Ad & Post'");
        appendLog("⚖️ [EVALUATING DUAL AUCTION] Comparing bids in real-time...");

        boolean interReady = IronSource.isInterstitialReady();
        boolean rewReady = IronSource.isRewardedVideoAvailable();

        if (!interReady && !rewReady) {
            appendLog("📡 Neither ad cached yet. Requesting parallel auctions...");
            showProgress("Running Dual Auction (Meta vs AdMob vs Unity)...");
            isWaitingForDualAdToShow = true;
            IronSource.loadInterstitial();
            AdManager.getInstance().preloadRewardedVideo();

            // 6-second timeout safety
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
        appendLog("🏆 [WINNER DETERMINED]");
        appendLog("   Format:  " + winnerFormat);
        appendLog("   Network: " + winnerNet);
        appendLog("   eCPM:    " + revStr);
        appendLog("▶️ Playing winning ad now...");

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
                    Toast.makeText(AdTestingActivity.this, "Ad playback notice: " + error, Toast.LENGTH_SHORT).show();
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

    // =========================================================================
    // NATIVE AD IMPLEMENTATION
    // =========================================================================

    private void loadNativeAd() {
        try {
            appendLog("📡 [REQUESTING NATIVE AD] Placement: " + AdManager.LEVELPLAY_NATIVE_ID);
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

    // =========================================================================
    // INTEGRATION VALIDATOR & TEST SUITE
    // =========================================================================

    private void runIntegrationValidator() {
        appendLog("🩺 [VALIDATING ADAPTERS & SDKs]");
        boolean metaSdkOk = checkClassExists("com.facebook.ads.AudienceNetworkAds");
        boolean metaAdapterOk = checkClassExists("com.ironsource.adapters.facebook.FacebookAdapter");
        appendLog(metaSdkOk ? "  ✅ Meta SDK: FOUND" : "  ❌ Meta SDK: MISSING");
        appendLog(metaAdapterOk ? "  ✅ Meta Adapter (FacebookAdapter): FOUND" : "  ❌ Meta Adapter: MISSING");

        boolean admobSdkOk = checkClassExists("com.google.android.gms.ads.MobileAds");
        boolean admobAdapterOk = checkClassExists("com.ironsource.adapters.admob.AdMobAdapter");
        appendLog(admobSdkOk ? "  ✅ AdMob SDK: FOUND" : "  ❌ AdMob SDK: MISSING");
        appendLog(admobAdapterOk ? "  ✅ AdMob Adapter: FOUND" : "  ❌ AdMob Adapter: MISSING");

        boolean unitySdkOk = checkClassExists("com.unity3d.ads.UnityAds");
        boolean unityAdapterOk = checkClassExists("com.ironsource.adapters.unityads.UnityAdsAdapter");
        appendLog(unitySdkOk ? "  ✅ Unity Ads SDK: FOUND" : "  ❌ Unity Ads SDK: MISSING");
        appendLog(unityAdapterOk ? "  ✅ Unity Adapter: FOUND" : "  ❌ Unity Adapter: MISSING");

        try {
            IntegrationHelper.validateIntegration(this);
            appendLog("🩺 [IntegrationHelper] Dispatched official LevelPlay validation.");
            Toast.makeText(this, "Adapters Verified! All adapters compiled.", Toast.LENGTH_SHORT).show();
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

    private void showMetaHashInputDialog() {
        String currentHash = getSharedPreferences("meta_ad_test", MODE_PRIVATE)
                .getString("meta_hash_id", AdManager.META_TEST_DEVICE_HASH_ID);
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Paste Meta Test Device Hash ID");
        input.setText(currentHash);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));

        new AlertDialog.Builder(this)
                .setTitle("Meta Test Device Hash ID")
                .setMessage("Logcat mein tag 'AdSettings' search karke wahan se hash ID copy karke yahan paste karein:")
                .setView(input)
                .setPositiveButton("Save & Activate", (dialog, which) -> {
                    String hash = input.getText().toString().trim();
                    AdManager.setMetaTestDeviceHashId(this, hash);
                    appendLog("📱 [META HASH SAVED] " + hash);
                    Toast.makeText(this, "Meta Test Device Hash Activated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // LIFECYCLE MANAGEMENT
    // =========================================================================

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
        if (levelPlayNativeAd != null) {
            levelPlayNativeAd.destroyAd();
            levelPlayNativeAd = null;
        }
    }

    // =========================================================================
    // UI BUILDER (Ultra-Clean, Focused Modern Dark Theme)
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
        tvTitle.setText("Dual-Auction Ad Dashboard");
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

        // CARD 1: SDK Status & Tools
        LinearLayout cardStatus = createCard();
        cardStatus.addView(createSectionHeader("Unity LevelPlay Mediation"));

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
        cardStatus.addView(tvNetworks);

        Button btnValidate = createButton("🩺 Validate Adapters (IntegrationHelper)", "#3B82F6");
        btnValidate.setOnClickListener(v -> runIntegrationValidator());
        cardStatus.addView(btnValidate);

        String activeEmail = TokenManager.getInstance(this).getUserEmail();
        if (activeEmail != null && "jainmohit8120@gmail.com".equalsIgnoreCase(activeEmail.trim())) {
            Button btnTestSuite = createButton("🚀 Launch LevelPlay Test Suite (Meta)", "#8B5CF6");
            btnTestSuite.setOnClickListener(v -> AdManager.getInstance().launchTestSuite(this));
            cardStatus.addView(btnTestSuite);

            Button btnSetHash = createButton("📱 Set Meta Test Device Hash ID", "#0EA5E9");
            btnSetHash.setOnClickListener(v -> showMetaHashInputDialog());
            cardStatus.addView(btnSetHash);
        }
        content.addView(cardStatus);

        // CARD 2: DUAL-AUCTION CORE (DIRECT BUTTON)
        LinearLayout cardDualAuction = createCard();
        cardDualAuction.addView(createSectionHeader("⚡ Dual-Auction Engine (Highest eCPM)"));

        TextView tvDualDesc = createSubText("Runs parallel bidding auctions across Interstitial and Rewarded formats. The network offering the highest real-time eCPM bid wins!");
        cardDualAuction.addView(tvDualDesc);

        tvDualAuctionWinner = createStatusBadge("🏆 HIGHEST BIDDER: Checking auctions...");
        tvDualAuctionWinner.setTextColor(Color.parseColor("#38BDF8"));
        cardDualAuction.addView(tvDualAuctionWinner);

        tvInterstitialStatus = createStatusBadge("🎬 Interstitial: PRELOADING...");
        cardDualAuction.addView(tvInterstitialStatus);

        tvRewardedStatus = createStatusBadge("🎁 Rewarded: AUTO-CACHING...");
        cardDualAuction.addView(tvRewardedStatus);

        // MAIN DIRECT BUTTON: "Watch Ad & Post"
        btnWatchAdAndPost = createButton("🎬 Watch Ad & Post (Dual-Auction Winner)", "#10B981");
        btnWatchAdAndPost.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnWatchAdAndPost.setOnClickListener(v -> handleWatchAdAndPostClick());
        cardDualAuction.addView(btnWatchAdAndPost);

        TextView tvButtonHint = createSubText("Simulates post publish: evaluates live bids, plays highest-paying ad, grants user reward, and publishes post!");
        tvButtonHint.setPadding(0, dp(6), 0, 0);
        cardDualAuction.addView(tvButtonHint);

        content.addView(cardDualAuction);

        // CARD 3: Native Ad Card
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

        // CARD 4: Real-Time Console
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
        logScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        logScrollView.setBackgroundColor(Color.parseColor("#020617")); // Slate 950
        logScrollView.setPadding(dp(10), dp(10), dp(10), dp(10));

        tvConsoleLog = new TextView(this);
        tvConsoleLog.setTextColor(Color.parseColor("#CBD5E1"));
        tvConsoleLog.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvConsoleLog.setTypeface(Typeface.MONOSPACE);
        tvConsoleLog.setText("Dual-Auction Mediation Console ready...\n");
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
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private Button createButton(String text, String hexColor) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
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
