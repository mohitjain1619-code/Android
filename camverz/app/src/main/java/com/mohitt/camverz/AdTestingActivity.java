package com.mohitt.camverz;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.ArrayList;
import java.util.List;

public class AdTestingActivity extends AppCompatActivity {
    private static final String TAG = "AdTestingActivity";

    // Meta Ads
    private InterstitialAd metaInterstitialAd;
    private RewardedVideoAd metaRewardedVideoAd;
    private NativeAd metaNativeAd;

    // AdMob Ads
    private com.google.android.gms.ads.interstitial.InterstitialAd admobInterstitialAd;
    private RewardedAd admobRewardedAd;
    private com.google.android.gms.ads.nativead.NativeAd admobNativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_testing);

        // Initialize LevelPlay SDK for testing
        BaseActivity.initializeIronSource(this);

        if (com.mohitt.camverz.BuildConfig.DEBUG) {
            AdSettings.setTestMode(true);
            com.ironsource.mediationsdk.integration.IntegrationHelper.validateIntegration(this);
        }

        // Meta Buttons
        findViewById(R.id.btn_meta_interstitial).setOnClickListener(v -> testMetaInterstitial());
        findViewById(R.id.btn_meta_rewarded).setOnClickListener(v -> testMetaRewarded());
        findViewById(R.id.btn_meta_native).setOnClickListener(v -> testMetaNative());

        // Unity Buttons (Direct Unity Ads SDK is removed; redirecting to AdMob testing)
        findViewById(R.id.btn_unity_interstitial).setOnClickListener(v -> {
            showToast("Unity direct SDK is removed. Loading AdMob Interstitial instead...");
            testAdMobInterstitial();
        });
        findViewById(R.id.btn_unity_rewarded).setOnClickListener(v -> {
            showToast("Unity direct SDK is removed. Loading AdMob Rewarded Interstitial instead...");
            testAdMobRewarded();
        });

        // ironSource Direct Buttons (Direct ironSource SDK is removed; redirecting to AdMob testing)
        findViewById(R.id.btn_is_interstitial).setOnClickListener(v -> {
            showToast("ironSource direct SDK is removed. Loading AdMob Interstitial instead...");
            testAdMobInterstitial();
        });
        findViewById(R.id.btn_is_rewarded).setOnClickListener(v -> {
            showToast("ironSource direct SDK is removed. Loading AdMob Rewarded Interstitial instead...");
            testAdMobRewarded();
        });

        // LevelPlay Mediation Buttons
        findViewById(R.id.btn_lp_interstitial).setOnClickListener(v -> {
            showToast("Loading LevelPlay Interstitial...");
            com.ironsource.mediationsdk.IronSource.setLevelPlayInterstitialListener(new com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener() {
                @Override
                public void onAdReady(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    showToast("LevelPlay Interstitial Ready");
                    com.ironsource.mediationsdk.IronSource.showInterstitial();
                }
                @Override
                public void onAdLoadFailed(com.ironsource.mediationsdk.logger.IronSourceError error) {
                    showToast("LevelPlay Interstitial Load Failed: " + error.getErrorMessage());
                }
                @Override public void onAdOpened(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                @Override public void onAdShowSucceeded(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                @Override public void onAdShowFailed(com.ironsource.mediationsdk.logger.IronSourceError error, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    showToast("LevelPlay Interstitial Show Failed: " + error.getErrorMessage());
                }
                @Override public void onAdClicked(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                @Override public void onAdClosed(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
            });
            com.ironsource.mediationsdk.IronSource.loadInterstitial();
        });
        findViewById(R.id.btn_lp_rewarded).setOnClickListener(v -> {
            showToast("Checking LevelPlay Rewarded Video...");
            if (com.ironsource.mediationsdk.IronSource.isRewardedVideoAvailable()) {
                com.ironsource.mediationsdk.IronSource.setLevelPlayRewardedVideoListener(new com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener() {
                    @Override public void onAdAvailable(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                    @Override public void onAdUnavailable() {}
                    @Override public void onAdOpened(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                    @Override public void onAdShowFailed(com.ironsource.mediationsdk.logger.IronSourceError error, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                        showToast("LevelPlay Rewarded Show Failed: " + error.getErrorMessage());
                    }
                    @Override public void onAdClicked(com.ironsource.mediationsdk.model.Placement placement, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                    @Override public void onAdRewarded(com.ironsource.mediationsdk.model.Placement placement, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                        showToast("LevelPlay Rewarded Video Completed! User rewarded.");
                    }
                    @Override public void onAdClosed(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                });
                com.ironsource.mediationsdk.IronSource.showRewardedVideo();
            } else {
                showToast("LevelPlay Rewarded Video not available yet.");
            }
        });
        findViewById(R.id.btn_lp_native).setOnClickListener(v -> {
            showToast("Loading LevelPlay Native Ad...");
            com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd nativeAd = new com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd.Builder()
                .withPlacementName("default")
                .withListener(new com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener() {
                    @Override
                    public void onAdLoaded(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                        showToast("LevelPlay Native Ad Loaded");
                        inflateLevelPlayNativeAd(ad);
                    }
                    @Override
                    public void onAdLoadFailed(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.logger.IronSourceError error) {
                        showToast("LevelPlay Native Ad Load Failed: " + error.getErrorMessage());
                    }
                    @Override public void onAdClicked(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                    @Override public void onAdImpression(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                })
                .build();
            nativeAd.loadAd();
        });

        // AdMob Mediation Buttons
        findViewById(R.id.btn_admob_interstitial).setOnClickListener(v -> testAdMobInterstitial());
        findViewById(R.id.btn_admob_rewarded).setOnClickListener(v -> testAdMobRewarded());
        findViewById(R.id.btn_admob_native).setOnClickListener(v -> testAdMobNative());
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
    }

    // ==========================================
    // META ADS (DIRECT)
    // ==========================================
    private void testMetaInterstitial() {
        showToast("Loading Meta Interstitial...");
        metaInterstitialAd = new InterstitialAd(this, "1679167109809598_1679167723142870");
        metaInterstitialAd.loadAd(metaInterstitialAd.buildLoadAdConfig().withAdListener(new InterstitialAdListener() {
            @Override public void onInterstitialDisplayed(Ad ad) { showToast("Meta Interstitial Displayed"); }
            @Override public void onInterstitialDismissed(Ad ad) { showToast("Meta Interstitial Dismissed"); }
            @Override public void onError(Ad ad, AdError adError) { showToast("Meta Interstitial Error: " + adError.getErrorMessage()); }
            @Override public void onAdLoaded(Ad ad) { showToast("Meta Interstitial Loaded"); metaInterstitialAd.show(); }
            @Override public void onAdClicked(Ad ad) { showToast("Meta Interstitial Clicked"); }
            @Override public void onLoggingImpression(Ad ad) {}
        }).build());
    }

    private void testMetaRewarded() {
        showToast("Loading Meta Rewarded...");
        metaRewardedVideoAd = new RewardedVideoAd(this, "1679167109809598_1679167723142870");
        metaRewardedVideoAd.loadAd(metaRewardedVideoAd.buildLoadAdConfig().withAdListener(new RewardedVideoAdListener() {
            @Override public void onRewardedVideoCompleted() { showToast("Meta Rewarded Completed"); }
            @Override public void onLoggingImpression(Ad ad) {}
            @Override public void onRewardedVideoClosed() { showToast("Meta Rewarded Closed"); }
            @Override public void onError(Ad ad, AdError adError) { showToast("Meta Rewarded Error: " + adError.getErrorMessage()); }
            @Override public void onAdLoaded(Ad ad) { showToast("Meta Rewarded Loaded"); metaRewardedVideoAd.show(); }
            @Override public void onAdClicked(Ad ad) {}
        }).build());
    }

    private void testMetaNative() {
        showToast("Loading Meta Native...");
        metaNativeAd = new NativeAd(this, "1679167109809598_1679167733142869");
        metaNativeAd.loadAd(metaNativeAd.buildLoadAdConfig().withAdListener(new NativeAdListener() {
            @Override public void onMediaDownloaded(Ad ad) {}
            @Override public void onError(Ad ad, AdError adError) { showToast("Meta Native Error: " + adError.getErrorMessage()); }
            @Override public void onAdLoaded(Ad ad) { 
                showToast("Meta Native Loaded"); 
                inflateMetaNativeAd(metaNativeAd); 
            }
            @Override public void onAdClicked(Ad ad) {}
            @Override public void onLoggingImpression(Ad ad) {}
        }).build());
    }

    private void inflateMetaNativeAd(NativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.ad_testing_meta_native_container);
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        NativeAdLayout adLayout = new NativeAdLayout(this);
        adLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout innerContainer = new LinearLayout(this);
        innerContainer.setOrientation(LinearLayout.VERTICAL);
        innerContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        int p = (int)(12 * getResources().getDisplayMetrics().density);
        innerContainer.setPadding(p, p, p, p);

        TextView adHeadline = new TextView(this);
        adHeadline.setText(ad.getAdHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(adHeadline);

        TextView adBody = new TextView(this);
        adBody.setText(ad.getAdBodyText());
        adBody.setTextColor(getResources().getColor(R.color.text_secondary));
        adBody.setTextSize(12);
        innerContainer.addView(adBody);

        Button callToAction = new Button(this);
        callToAction.setText(ad.getAdCallToAction());
        innerContainer.addView(callToAction);

        adLayout.addView(innerContainer);

        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(adHeadline);
        clickableViews.add(callToAction);
        
        com.facebook.ads.MediaView mediaView = new com.facebook.ads.MediaView(this);
        ad.registerViewForInteraction(adLayout, mediaView, clickableViews);

        adContainer.addView(adLayout);
    }

    // ==========================================
    // ADMOB MEDIATION ADS
    // ==========================================
    private void testAdMobInterstitial() {
        showToast("Loading AdMob Interstitial...");
        AdRequest adRequest = new AdRequest.Builder().build();
        com.google.android.gms.ads.interstitial.InterstitialAd.load(this, getString(R.string.admob_interstitial_ad_unit_id), adRequest,
            new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                    showToast("AdMob Interstitial Loaded");
                    admobInterstitialAd = interstitialAd;
                    admobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() { showToast("AdMob Interstitial Dismissed"); }
                        @Override public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) { showToast("AdMob Show Failed: " + adError.getMessage()); }
                        @Override public void onAdShowedFullScreenContent() { showToast("AdMob Interstitial Displayed"); }
                    });
                    admobInterstitialAd.show(AdTestingActivity.this);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    showToast("AdMob Interstitial Load Failed: " + loadAdError.getMessage());
                    admobInterstitialAd = null;
                }
            });
    }

    private void testAdMobRewarded() {
        showToast("Loading AdMob Rewarded...");
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.admob_rewarded_ad_unit_id), adRequest,
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    showToast("AdMob Rewarded Loaded");
                    admobRewardedAd = ad;
                    admobRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() { showToast("AdMob Rewarded Dismissed"); }
                        @Override public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) { showToast("AdMob Rewarded Show Failed: " + adError.getMessage()); }
                        @Override public void onAdShowedFullScreenContent() { showToast("AdMob Rewarded Displayed"); }
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
                    showToast("AdMob Rewarded Load Failed: " + loadAdError.getMessage());
                    admobRewardedAd = null;
                }
            });
    }

    private void testAdMobNative() {
        showToast("Loading AdMob Native...");
        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.admob_native_ad_unit_id))
            .forNativeAd(new com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener() {
                @Override
                public void onNativeAdLoaded(@NonNull com.google.android.gms.ads.nativead.NativeAd ad) {
                    if (isFinishing() || isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    showToast("AdMob Native Loaded");
                    admobNativeAd = ad;
                    inflateAdMobNativeAd(ad);
                }
            })
            .withAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    showToast("AdMob Native Failed to Load: " + adError.getMessage());
                }
            })
            .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void inflateAdMobNativeAd(com.google.android.gms.ads.nativead.NativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.ad_testing_admob_native_container);
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        NativeAdView adView = new NativeAdView(this);
        adView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout innerContainer = new LinearLayout(this);
        innerContainer.setOrientation(LinearLayout.VERTICAL);
        innerContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        int p = (int)(12 * getResources().getDisplayMetrics().density);
        innerContainer.setPadding(p, p, p, p);

        TextView adHeadline = new TextView(this);
        adHeadline.setText(ad.getHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(adHeadline);
        adView.setHeadlineView(adHeadline);

        TextView adBody = new TextView(this);
        adBody.setText(ad.getBody());
        adBody.setTextColor(getResources().getColor(R.color.text_secondary));
        adBody.setTextSize(12);
        innerContainer.addView(adBody);
        adView.setBodyView(adBody);

        Button callToAction = new Button(this);
        callToAction.setText(ad.getCallToAction());
        innerContainer.addView(callToAction);
        adView.setCallToActionView(callToAction);

        adView.addView(innerContainer);
        adView.setNativeAd(ad);

        adContainer.addView(adView);
    }

    private void inflateLevelPlayNativeAd(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.ad_testing_lp_native_container);
        if (adContainer == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        com.ironsource.mediationsdk.ads.nativead.NativeAdLayout nativeAdLayout = new com.ironsource.mediationsdk.ads.nativead.NativeAdLayout(this);
        View adView = android.view.LayoutInflater.from(this).inflate(R.layout.layout_native_ad_levelplay, nativeAdLayout, true);

        ImageView adIcon = adView.findViewById(R.id.ad_icon);
        TextView adTitle = adView.findViewById(R.id.ad_title);
        TextView adAdvertiser = adView.findViewById(R.id.ad_advertiser);
        TextView adBody = adView.findViewById(R.id.ad_body);
        com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView mediaView = adView.findViewById(R.id.ad_media);
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

    @Override
    protected void onResume() {
        super.onResume();
        com.ironsource.mediationsdk.IronSource.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        com.ironsource.mediationsdk.IronSource.onPause(this);
    }
}
