package com.mohit.camverz;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;

import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.RewardedInterstitialAd;
import com.facebook.ads.RewardedInterstitialAdListener;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;

import java.util.ArrayList;
import java.util.List;

/**
 * Production-ready Ad Testing Dashboard for administrators.
 * Provides isolated testing for:
 * 1. Meta Audience Network Direct (Interstitial, Rewarded Video, Native)
 * 2. Mediation via ironSource LevelPlay (Meta, AdMob, Unity Bidding auction)
 * 3. AdMob Direct / Waterfall (Interstitial, Rewarded Video, Native)
 */
public class AdTestingActivity extends BaseActivity {
    private static final String TAG = "AdTestingActivity";

    // Meta Placement IDs
    public static final String META_INTERSTITIAL_PLACEMENT = "1045884118260179_1063416116506979";
    public static final String META_REWARDED_PLACEMENT = "1045884118260179_1063416119840312";
    public static final String META_NATIVE_PLACEMENT = "1045884118260179_1063416113173646";

    // Meta Ad Instances
    private InterstitialAd metaInterstitialAd;
    private RewardedVideoAd metaRewardedVideoAd;
    private RewardedInterstitialAd metaRewardedInterstitialAd;
    private NativeAd metaNativeAd;

    // AdMob Ad Instances
    private com.google.android.gms.ads.interstitial.InterstitialAd admobInterstitialAd;
    private RewardedAd admobRewardedAd;
    private com.google.android.gms.ads.nativead.NativeAd admobNativeAd;

    // LevelPlay Native Instance
    private LevelPlayNativeAd levelPlayNativeAd;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_testing);

        // Ensure LevelPlay and mediation adapters are initialized
        BaseActivity.initializeIronSource(this);



        if (BuildConfig.DEBUG) {
            com.ironsource.mediationsdk.integration.IntegrationHelper.validateIntegration(this);
        }

        setupMetaButtons();
        setupLevelPlayMediationButtons();
        setupAdMobButtons();
    }

    private void showToast(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(AdTestingActivity.this, msg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, msg);
        });
    }

    // =========================================================================
    // 1. META AUDIENCE NETWORK (DIRECT)
    // =========================================================================

    private void setupMetaButtons() {
        findViewById(R.id.btn_meta_interstitial).setOnClickListener(v -> testMetaInterstitial());
        findViewById(R.id.btn_meta_rewarded).setOnClickListener(v -> testMetaRewarded());
        findViewById(R.id.btn_meta_native).setOnClickListener(v -> testMetaNative());
    }

    // State flags for anti-flood protection
    private long lastMetaRewardedRequestTime = 0;
    private long lastMetaInterstitialRequestTime = 0;
    private boolean isMetaRewardedLoading = false;
    private boolean isMetaInterstitialLoading = false;

    private void testMetaInterstitial() {
        if (metaInterstitialAd != null && metaInterstitialAd.isAdLoaded()) {
            showToast("Meta Interstitial already loaded! Showing now...");
            metaInterstitialAd.show();
            return;
        }

        if (AdManager.getInstance().isMetaFallbackInterstitialLoaded()) {
            showToast("Showing cached Meta Interstitial from AdManager...");
            AdManager.getInstance().showFallbackMetaInterstitialAd(this, success -> {
                showToast("Meta Interstitial Dismissed (success=" + success + ")");
            });
            return;
        }

        if (isMetaInterstitialLoading || AdManager.getInstance().isMetaFallbackInterstitialLoading()) {
            showToast("Meta Interstitial is already loading in background. Please wait...");
            return;
        }

        long timeSinceLast = System.currentTimeMillis() - lastMetaInterstitialRequestTime;
        if (timeSinceLast < 30000 && lastMetaInterstitialRequestTime > 0) {
            long remaining = (30000 - timeSinceLast) / 1000;
            showToast("Meta SDK requires 30s cooldown. Please wait " + remaining + "s (Prevents Error 1002)");
            return;
        }
        lastMetaInterstitialRequestTime = System.currentTimeMillis();
        isMetaInterstitialLoading = true;

        showToast("Loading Meta Interstitial (Direct)...");
        if (metaInterstitialAd != null) {
            try { metaInterstitialAd.destroy(); } catch (Exception ignored) {}
        }
        metaInterstitialAd = new InterstitialAd(this, META_INTERSTITIAL_PLACEMENT);
        metaInterstitialAd.loadAd(metaInterstitialAd.buildLoadAdConfig().withAdListener(new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                showToast("Meta Interstitial Displayed");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                showToast("Meta Interstitial Dismissed");
                isMetaInterstitialLoading = false;
                metaInterstitialAd = null;
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                isMetaInterstitialLoading = false;
                showToast("Meta Interstitial Error (" + adError.getErrorCode() + "): " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                isMetaInterstitialLoading = false;
                showToast("Meta Interstitial Loaded! Showing...");
                if (metaInterstitialAd != null && metaInterstitialAd.isAdLoaded()) {
                    metaInterstitialAd.show();
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                showToast("Meta Interstitial Clicked");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "Meta Interstitial Impression Logged");
            }
        }).build());
    }

    private void testMetaRewarded() {
        if (metaRewardedVideoAd != null && metaRewardedVideoAd.isAdLoaded()) {
            showToast("Meta Rewarded Video already loaded! Showing now...");
            metaRewardedVideoAd.show();
            return;
        }

        if (AdManager.getInstance().isMetaFallbackRewardedLoaded()) {
            showToast("Showing cached Meta Rewarded Video from AdManager...");
            AdManager.getInstance().showFallbackMetaRewardedAd(this, new AdManager.RewardedAdCallback() {
                @Override public void onRewardEarned() { showToast("Meta Reward Earned!"); }
                @Override public void onAdDismissed(boolean rewarded) { showToast("Meta Rewarded Dismissed (rewarded=" + rewarded + ")"); }
                @Override public void onAdFailed(String error) { showToast("Meta Show Failed: " + error); }
            });
            return;
        }

        if (isMetaRewardedLoading || AdManager.getInstance().isMetaFallbackRewardedLoading()) {
            showToast("Meta Rewarded Video is already loading in background. Please wait...");
            return;
        }

        long timeSinceLast = System.currentTimeMillis() - lastMetaRewardedRequestTime;
        if (timeSinceLast < 30000 && lastMetaRewardedRequestTime > 0) {
            long remaining = (30000 - timeSinceLast) / 1000;
            showToast("Meta SDK requires 30s cooldown. Please wait " + remaining + "s (Prevents Error 1002)");
            return;
        }
        lastMetaRewardedRequestTime = System.currentTimeMillis();
        isMetaRewardedLoading = true;

        showToast("Loading Meta Rewarded Video (Direct)...");
        if (metaRewardedVideoAd != null) {
            try { metaRewardedVideoAd.destroy(); } catch (Exception ignored) {}
        }
        metaRewardedVideoAd = new RewardedVideoAd(this, META_REWARDED_PLACEMENT);
        metaRewardedVideoAd.loadAd(metaRewardedVideoAd.buildLoadAdConfig().withAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoCompleted() {
                showToast("Meta Rewarded Video Completed! Reward earned.");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "Meta Rewarded Video Impression Logged");
            }

            @Override
            public void onRewardedVideoClosed() {
                showToast("Meta Rewarded Video Closed");
                isMetaRewardedLoading = false;
                metaRewardedVideoAd = null;
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                isMetaRewardedLoading = false;
                String errMsg = adError.getErrorMessage();
                int errCode = adError.getErrorCode();
                Log.e(TAG, "Meta Rewarded Video Error (" + errCode + "): " + errMsg);

                // If placement format in console is Rewarded Interstitial rather than Rewarded Video
                if (errMsg != null && (errMsg.contains("format") || errMsg.contains("display format"))) {
                    showToast("Format mismatch detected. Testing as Rewarded Interstitial...");
                    testMetaRewardedInterstitialFallback();
                } else {
                    showToast("Meta Rewarded Error (" + errCode + "): " + errMsg);
                }
            }

            @Override
            public void onAdLoaded(Ad ad) {
                isMetaRewardedLoading = false;
                showToast("Meta Rewarded Video Loaded! Showing...");
                if (metaRewardedVideoAd != null && metaRewardedVideoAd.isAdLoaded()) {
                    metaRewardedVideoAd.show();
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                showToast("Meta Rewarded Video Clicked");
            }
        }).build());
    }

    private void testMetaRewardedInterstitialFallback() {
        if (metaRewardedInterstitialAd != null) {
            metaRewardedInterstitialAd.destroy();
        }
        metaRewardedInterstitialAd = new RewardedInterstitialAd(this, META_REWARDED_PLACEMENT);
        metaRewardedInterstitialAd.loadAd(metaRewardedInterstitialAd.buildLoadAdConfig().withAdListener(new RewardedInterstitialAdListener() {
            @Override
            public void onRewardedInterstitialCompleted() {
                showToast("Meta Rewarded Interstitial Completed!");
            }

            @Override
            public void onRewardedInterstitialClosed() {
                showToast("Meta Rewarded Interstitial Closed");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                showToast("Meta Rewarded Interstitial Error (" + adError.getErrorCode() + "): " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                showToast("Meta Rewarded Interstitial Loaded! Showing...");
                if (metaRewardedInterstitialAd != null && metaRewardedInterstitialAd.isAdLoaded()) {
                    metaRewardedInterstitialAd.show();
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                showToast("Meta Rewarded Interstitial Clicked");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "Meta Rewarded Interstitial Impression Logged");
            }
        }).build());
    }

    private void testMetaNative() {
        showToast("Loading Meta Native Ad (Direct)...");
        if (metaNativeAd != null) {
            metaNativeAd.destroy();
        }
        metaNativeAd = new NativeAd(this, META_NATIVE_PLACEMENT);
        metaNativeAd.loadAd(metaNativeAd.buildLoadAdConfig().withAdListener(new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {
                Log.d(TAG, "Meta Native media downloaded");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                showToast("Meta Native Error (" + adError.getErrorCode() + "): " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                showToast("Meta Native Loaded! Rendering preview...");
                inflateMetaNativeAd(metaNativeAd);
            }

            @Override
            public void onAdClicked(Ad ad) {
                showToast("Meta Native Clicked");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "Meta Native Impression Logged");
            }
        }).build());
    }

    private void inflateMetaNativeAd(NativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.ad_testing_meta_native_container);
        if (adContainer == null || ad == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        NativeAdLayout adLayout = new NativeAdLayout(this);
        adLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout innerContainer = new LinearLayout(this);
        innerContainer.setOrientation(LinearLayout.VERTICAL);
        innerContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        int p = (int) (12 * getResources().getDisplayMetrics().density);
        innerContainer.setPadding(p, p, p, p);

        // Header with AdChoices icon
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        MediaView adIconView = new MediaView(this);
        int iconSize = (int) (40 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        adIconView.setLayoutParams(iconParams);
        headerRow.addView(adIconView);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView adHeadline = new TextView(this);
        adHeadline.setText(ad.getAdHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textCol.addView(adHeadline);

        TextView adAdvertiser = new TextView(this);
        adAdvertiser.setText(ad.getAdvertiserName() != null ? ad.getAdvertiserName() : "Sponsored");
        adAdvertiser.setTextColor(getResources().getColor(R.color.text_secondary));
        adAdvertiser.setTextSize(12);
        textCol.addView(adAdvertiser);

        headerRow.addView(textCol);

        AdOptionsView adOptionsView = new AdOptionsView(this, ad, adLayout);
        headerRow.addView(adOptionsView);
        innerContainer.addView(headerRow);

        // Media creative
        MediaView mediaView = new MediaView(this);
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (180 * getResources().getDisplayMetrics().density));
        mediaParams.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));
        mediaView.setLayoutParams(mediaParams);
        innerContainer.addView(mediaView);

        // Body text
        TextView adBody = new TextView(this);
        adBody.setText(ad.getAdBodyText());
        adBody.setTextColor(getResources().getColor(R.color.text_secondary));
        adBody.setTextSize(13);
        innerContainer.addView(adBody);

        // Call to action button
        Button callToAction = new Button(this);
        callToAction.setText(ad.getAdCallToAction() != null ? ad.getAdCallToAction() : "Learn More");
        callToAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF38BDF8));
        callToAction.setTextColor(0xFF0F172A);
        LinearLayout.LayoutParams ctaParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ctaParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        callToAction.setLayoutParams(ctaParams);
        innerContainer.addView(callToAction);

        adLayout.addView(innerContainer);

        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(adHeadline);
        clickableViews.add(callToAction);
        clickableViews.add(mediaView);

        ad.registerViewForInteraction(adLayout, mediaView, adIconView, clickableViews);
        adContainer.addView(adLayout);
    }

    // =========================================================================
    // 2. LEVELPLAY MEDIATION (META + ADMOB + UNITY BIDDING AUCTION)
    // =========================================================================

    private void setupLevelPlayMediationButtons() {
        findViewById(R.id.btn_lp_interstitial).setOnClickListener(v -> testLevelPlayInterstitial());
        findViewById(R.id.btn_lp_rewarded).setOnClickListener(v -> testLevelPlayRewarded());
        findViewById(R.id.btn_lp_native).setOnClickListener(v -> testLevelPlayNative());
    }

    private void testLevelPlayInterstitial() {
        showToast("Requesting LevelPlay Interstitial (Meta/AdMob/Unity Auction)...");
        if (IronSource.isInterstitialReady()) {
            showToast("LevelPlay Interstitial ready! Showing...");
            IronSource.showInterstitial();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading LevelPlay Interstitial (Running header bidding)...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        AdManager.getInstance().preloadInterstitial(this);

        final long startTime = System.currentTimeMillis();
        final Runnable[] checker = new Runnable[1];
        checker[0] = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    safeDismiss(progressDialog);
                    return;
                }

                if (IronSource.isInterstitialReady()) {
                    safeDismiss(progressDialog);
                    showToast("LevelPlay Interstitial loaded! Showing...");
                    IronSource.showInterstitial();
                } else if (System.currentTimeMillis() - startTime < 8000) {
                    mainHandler.postDelayed(checker[0], 500);
                } else {
                    safeDismiss(progressDialog);
                    showToast("LevelPlay Interstitial load timed out (No Fill from bidding networks)");
                }
            }
        };
        mainHandler.postDelayed(checker[0], 500);
    }

    private void testLevelPlayRewarded() {
        showToast("Requesting LevelPlay Rewarded Video (Meta/AdMob/Unity Auction)...");
        if (IronSource.isRewardedVideoAvailable()) {
            showToast("LevelPlay Rewarded Video ready! Showing...");
            IronSource.showRewardedVideo();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading LevelPlay Rewarded Video (Running header bidding)...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        AdManager.getInstance().preloadRewardedVideo();

        final long startTime = System.currentTimeMillis();
        final Runnable[] checker = new Runnable[1];
        checker[0] = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    safeDismiss(progressDialog);
                    return;
                }

                if (IronSource.isRewardedVideoAvailable()) {
                    safeDismiss(progressDialog);
                    showToast("LevelPlay Rewarded Video loaded! Showing...");
                    IronSource.showRewardedVideo();
                } else if (System.currentTimeMillis() - startTime < 8000) {
                    mainHandler.postDelayed(checker[0], 500);
                } else {
                    safeDismiss(progressDialog);
                    showToast("LevelPlay Rewarded Video load timed out (No Fill from bidding networks)");
                }
            }
        };
        mainHandler.postDelayed(checker[0], 500);
    }

    private void testLevelPlayNative() {
        showToast("Loading LevelPlay Native Ad (Mediation)...");
        if (levelPlayNativeAd != null) {
            levelPlayNativeAd.destroyAd();
        }

        levelPlayNativeAd = new LevelPlayNativeAd.Builder()
                .withPlacementName("default")
                .withListener(new LevelPlayNativeAdListener() {
                    @Override
                    public void onAdLoaded(LevelPlayNativeAd ad, AdInfo adInfo) {
                        String network = adInfo != null ? adInfo.getAdNetwork() : "unknown";
                        showToast("LevelPlay Native Ad Loaded (Winning Network: " + network + ")");
                        inflateLevelPlayNativeAd(ad);
                    }

                    @Override
                    public void onAdLoadFailed(LevelPlayNativeAd ad, IronSourceError error) {
                        String errMsg = error != null ? error.getErrorMessage() : "Unknown error";
                        int errCode = error != null ? error.getErrorCode() : -1;
                        showToast("LevelPlay Native Load Failed (" + errCode + "): " + errMsg);
                    }

                    @Override
                    public void onAdClicked(LevelPlayNativeAd ad, AdInfo adInfo) {
                        showToast("LevelPlay Native Ad Clicked");
                    }

                    @Override
                    public void onAdImpression(LevelPlayNativeAd ad, AdInfo adInfo) {
                        Log.d(TAG, "LevelPlay Native Ad Impression Logged");
                    }
                })
                .build();
        levelPlayNativeAd.loadAd();
    }

    private void inflateLevelPlayNativeAd(LevelPlayNativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.ad_testing_lp_native_container);
        if (adContainer == null || ad == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        com.ironsource.mediationsdk.ads.nativead.NativeAdLayout nativeAdLayout = new com.ironsource.mediationsdk.ads.nativead.NativeAdLayout(this);
        View adView = android.view.LayoutInflater.from(this).inflate(R.layout.layout_native_ad_levelplay, nativeAdLayout, true);

        ImageView adIcon = adView.findViewById(R.id.ad_icon);
        TextView adTitle = adView.findViewById(R.id.ad_title);
        TextView adAdvertiser = adView.findViewById(R.id.ad_advertiser);
        TextView adBody = adView.findViewById(R.id.ad_body);
        LevelPlayMediaView mediaView = adView.findViewById(R.id.ad_media);
        Button adCta = adView.findViewById(R.id.ad_cta);

        if (ad.getIcon() != null && adIcon != null) {
            adIcon.setImageDrawable(ad.getIcon().getDrawable());
        }
        if (ad.getTitle() != null && adTitle != null) {
            adTitle.setText(ad.getTitle());
        }
        if (ad.getAdvertiser() != null && adAdvertiser != null) {
            adAdvertiser.setText(ad.getAdvertiser());
        }
        if (ad.getBody() != null && adBody != null) {
            adBody.setText(ad.getBody());
        }
        if (ad.getCallToAction() != null && adCta != null) {
            adCta.setText(ad.getCallToAction());
        }

        nativeAdLayout.setTitleView(adTitle);
        nativeAdLayout.setIconView(adIcon);
        nativeAdLayout.setMediaView(mediaView);
        nativeAdLayout.setCallToActionView(adCta);

        nativeAdLayout.registerNativeAdViews(ad);
        adContainer.addView(nativeAdLayout);
    }

    // =========================================================================
    // 3. ADMOB DIRECT (WATERFALL)
    // =========================================================================

    private void setupAdMobButtons() {
        findViewById(R.id.btn_admob_interstitial).setOnClickListener(v -> testAdMobInterstitial());
        findViewById(R.id.btn_admob_rewarded).setOnClickListener(v -> testAdMobRewarded());
        findViewById(R.id.btn_admob_native).setOnClickListener(v -> testAdMobNative());
    }

    private void testAdMobInterstitial() {
        showToast("Loading AdMob Interstitial (Direct)...");
        AdRequest adRequest = new AdRequest.Builder().build();
        com.google.android.gms.ads.interstitial.InterstitialAd.load(this,
                getString(R.string.admob_interstitial_ad_unit_id), adRequest,
                new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                        showToast("AdMob Interstitial Loaded! Showing...");
                        admobInterstitialAd = interstitialAd;
                        admobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                showToast("AdMob Interstitial Dismissed");
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                showToast("AdMob Show Failed: " + adError.getMessage());
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                showToast("AdMob Interstitial Displayed");
                            }
                        });
                        admobInterstitialAd.show(AdTestingActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        showToast("AdMob Interstitial Load Failed (" + loadAdError.getCode() + "): " + loadAdError.getMessage());
                        admobInterstitialAd = null;
                    }
                });
    }

    private void testAdMobRewarded() {
        showToast("Loading AdMob Rewarded Video (Direct)...");
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.admob_rewarded_ad_unit_id), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        showToast("AdMob Rewarded Video Loaded! Showing...");
                        admobRewardedAd = ad;
                        admobRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                showToast("AdMob Rewarded Dismissed");
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                showToast("AdMob Rewarded Show Failed: " + adError.getMessage());
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                showToast("AdMob Rewarded Displayed");
                            }
                        });
                        admobRewardedAd.show(AdTestingActivity.this, new OnUserEarnedRewardListener() {
                            @Override
                            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                showToast("AdMob Reward Earned: " + rewardItem.getAmount() + " " + rewardItem.getType());
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        showToast("AdMob Rewarded Load Failed (" + loadAdError.getCode() + "): " + loadAdError.getMessage());
                        admobRewardedAd = null;
                    }
                });
    }

    private void testAdMobNative() {
        showToast("Loading AdMob Native Ad (Direct)...");
        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.admob_native_ad_unit_id))
                .forNativeAd(new com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(@NonNull com.google.android.gms.ads.nativead.NativeAd ad) {
                        if (isFinishing() || isDestroyed()) {
                            ad.destroy();
                            return;
                        }
                        showToast("AdMob Native Loaded! Rendering preview...");
                        admobNativeAd = ad;
                        inflateAdMobNativeAd(ad);
                    }
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        showToast("AdMob Native Failed to Load (" + adError.getCode() + "): " + adError.getMessage());
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void inflateAdMobNativeAd(com.google.android.gms.ads.nativead.NativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.ad_testing_admob_native_container);
        if (adContainer == null || ad == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        NativeAdView adView = new NativeAdView(this);
        adView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout innerContainer = new LinearLayout(this);
        innerContainer.setOrientation(LinearLayout.VERTICAL);
        innerContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        int p = (int) (12 * getResources().getDisplayMetrics().density);
        innerContainer.setPadding(p, p, p, p);

        // Headline
        TextView adHeadline = new TextView(this);
        adHeadline.setText(ad.getHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(adHeadline);
        adView.setHeadlineView(adHeadline);

        // MediaView
        com.google.android.gms.ads.nativead.MediaView mediaView = new com.google.android.gms.ads.nativead.MediaView(this);
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (180 * getResources().getDisplayMetrics().density));
        mediaParams.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));
        mediaView.setLayoutParams(mediaParams);
        innerContainer.addView(mediaView);
        adView.setMediaView(mediaView);

        // Body text
        if (ad.getBody() != null) {
            TextView adBody = new TextView(this);
            adBody.setText(ad.getBody());
            adBody.setTextColor(getResources().getColor(R.color.text_secondary));
            adBody.setTextSize(13);
            innerContainer.addView(adBody);
            adView.setBodyView(adBody);
        }

        // Call to action
        if (ad.getCallToAction() != null) {
            Button callToAction = new Button(this);
            callToAction.setText(ad.getCallToAction());
            callToAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF38BDF8));
            callToAction.setTextColor(0xFF0F172A);
            LinearLayout.LayoutParams ctaParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ctaParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            callToAction.setLayoutParams(ctaParams);
            innerContainer.addView(callToAction);
            adView.setCallToActionView(callToAction);
        }

        adView.addView(innerContainer);
        adView.setNativeAd(ad);

        adContainer.addView(adView);
    }

    private void safeDismiss(ProgressDialog dialog) {
        if (dialog != null && dialog.isShowing() && !isFinishing() && !isDestroyed()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {}
        }
    }

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
        if (metaInterstitialAd != null) metaInterstitialAd.destroy();
        if (metaRewardedVideoAd != null) metaRewardedVideoAd.destroy();
        if (metaRewardedInterstitialAd != null) metaRewardedInterstitialAd.destroy();
        if (metaNativeAd != null) metaNativeAd.destroy();
        if (admobNativeAd != null) admobNativeAd.destroy();
        if (levelPlayNativeAd != null) levelPlayNativeAd.destroyAd();
    }
}
