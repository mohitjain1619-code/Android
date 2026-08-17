package com.mohitt.camverz;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

import java.util.ArrayList;
import java.util.List;

public class AdTestingActivity extends AppCompatActivity {
    private static final String TAG = "AdTestingActivity";

    // Meta Ads
    private InterstitialAd metaInterstitialAd;
    private RewardedVideoAd metaRewardedVideoAd;
    private NativeAd metaNativeAd;

    // LevelPlay Ads
    private LevelPlayInterstitialAd lpInterstitialAd;
    private LevelPlayRewardedAd lpRewardedAd;
    private LevelPlayNativeAd lpNativeAd;

    // AdMob Ads
    private com.google.android.gms.ads.interstitial.InterstitialAd admobInterstitialAd;
    private RewardedInterstitialAd admobRewardedInterstitialAd;
    private com.google.android.gms.ads.nativead.NativeAd admobNativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_testing);

        if (com.mohitt.camverz.BuildConfig.DEBUG) {
            AdSettings.setTestMode(true);
        }

        // Meta Buttons
        findViewById(R.id.btn_meta_interstitial).setOnClickListener(v -> testMetaInterstitial());
        findViewById(R.id.btn_meta_rewarded).setOnClickListener(v -> testMetaRewarded());
        findViewById(R.id.btn_meta_native).setOnClickListener(v -> testMetaNative());

        // Unity Buttons (Direct via UnityAds SDK)
        findViewById(R.id.btn_unity_interstitial).setOnClickListener(v -> testUnityInterstitial());
        findViewById(R.id.btn_unity_rewarded).setOnClickListener(v -> testUnityRewarded());

        // ironSource Direct Buttons (Uses LevelPlay APIs but conceptualized as direct)
        findViewById(R.id.btn_is_interstitial).setOnClickListener(v -> testLevelPlayInterstitial());
        findViewById(R.id.btn_is_rewarded).setOnClickListener(v -> testLevelPlayRewarded());

        // LevelPlay Mediation Buttons
        findViewById(R.id.btn_lp_interstitial).setOnClickListener(v -> testLevelPlayInterstitial());
        findViewById(R.id.btn_lp_rewarded).setOnClickListener(v -> testLevelPlayRewarded());
        findViewById(R.id.btn_lp_native).setOnClickListener(v -> testLevelPlayNative());

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
        metaRewardedVideoAd = new RewardedVideoAd(this, "1679167109809598_1679167723142870"); // Use same ID or specific rewarded ID if you have
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
    // UNITY ADS (DIRECT)
    // ==========================================
    private void testUnityInterstitial() {
        showToast("Loading Unity Interstitial...");
        UnityAds.load("Interstitial_Android", new IUnityAdsLoadListener() {
            @Override public void onUnityAdsAdLoaded(String placementId) {
                showToast("Unity Interstitial Loaded");
                UnityAds.show(AdTestingActivity.this, placementId, new IUnityAdsShowListener() {
                    @Override public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) { showToast("Unity Show Error: " + message); }
                    @Override public void onUnityAdsShowStart(String placementId) {}
                    @Override public void onUnityAdsShowClick(String placementId) {}
                    @Override public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {}
                });
            }
            @Override public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) { showToast("Unity Load Error: " + message); }
        });
    }

    private void testUnityRewarded() {
        showToast("Loading Unity Rewarded...");
        UnityAds.load("Rewarded_Android", new IUnityAdsLoadListener() {
            @Override public void onUnityAdsAdLoaded(String placementId) {
                showToast("Unity Rewarded Loaded");
                UnityAds.show(AdTestingActivity.this, placementId, new IUnityAdsShowListener() {
                    @Override public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {}
                    @Override public void onUnityAdsShowStart(String placementId) {}
                    @Override public void onUnityAdsShowClick(String placementId) {}
                    @Override public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) { showToast("Unity Rewarded Completed"); }
                });
            }
            @Override public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) { showToast("Unity Load Error: " + message); }
        });
    }

    // ==========================================
    // LEVELPLAY MEDIATION ADS
    // ==========================================
    private void testLevelPlayInterstitial() {
        showToast("Loading LevelPlay Interstitial...");
        lpInterstitialAd = new LevelPlayInterstitialAd("yw7j51u0q3eg5aai");
        lpInterstitialAd.setListener(new LevelPlayInterstitialAdListener() {
            @Override public void onAdLoaded(LevelPlayAdInfo adInfo) { showToast("LevelPlay Interstitial Loaded"); lpInterstitialAd.showAd(AdTestingActivity.this); }
            @Override public void onAdLoadFailed(LevelPlayAdError error) { showToast("LevelPlay Interstitial Error: " + error.getErrorMessage()); }
            @Override public void onAdDisplayed(LevelPlayAdInfo adInfo) {}
            @Override public void onAdDisplayFailed(LevelPlayAdError error, LevelPlayAdInfo adInfo) { showToast("LevelPlay Show Error: " + error.getErrorMessage()); }
            @Override public void onAdClicked(LevelPlayAdInfo adInfo) {}
            @Override public void onAdClosed(LevelPlayAdInfo adInfo) {}
        });
        lpInterstitialAd.loadAd();
    }

    private void testLevelPlayRewarded() {
        showToast("Loading LevelPlay Rewarded...");
        lpRewardedAd = new LevelPlayRewardedAd("onddt1lewzexkb5q");
        lpRewardedAd.setListener(new LevelPlayRewardedAdListener() {
            @Override public void onAdLoaded(LevelPlayAdInfo adInfo) { showToast("LevelPlay Rewarded Loaded"); lpRewardedAd.showAd(AdTestingActivity.this); }
            @Override public void onAdLoadFailed(LevelPlayAdError error) { showToast("LevelPlay Rewarded Error: " + error.getErrorMessage()); }
            @Override public void onAdDisplayed(LevelPlayAdInfo adInfo) {}
            @Override public void onAdDisplayFailed(LevelPlayAdError error, LevelPlayAdInfo adInfo) { showToast("LevelPlay Show Error: " + error.getErrorMessage()); }
            @Override public void onAdClicked(LevelPlayAdInfo adInfo) {}
            @Override public void onAdClosed(LevelPlayAdInfo adInfo) {}
            @Override public void onAdRewarded(LevelPlayReward reward, LevelPlayAdInfo adInfo) { showToast("LevelPlay Rewarded User"); }
        });
        lpRewardedAd.loadAd();
    }

    private void testLevelPlayNative() {
        showToast("Loading LevelPlay Native...");
        lpNativeAd = new LevelPlayNativeAd.Builder()
                .withPlacementName("4178n3sq2cj8dahz")
                .withListener(new LevelPlayNativeAdListener() {
                    @Override
                    public void onAdLoaded(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                        showToast("LevelPlay Native Loaded");
                        FrameLayout container = findViewById(R.id.ad_testing_lp_native_container);
                        com.ironsource.mediationsdk.ads.nativead.NativeAdLayout layout = findViewById(R.id.ad_testing_lp_native_layout);
                        container.setVisibility(View.VISIBLE);
                        layout.removeAllViews();
                        getLayoutInflater().inflate(R.layout.layout_native_ad_levelplay, layout, true);
                        
                        layout.setTitleView(layout.findViewById(R.id.ad_title));
                        layout.setBodyView(layout.findViewById(R.id.ad_body));
                        layout.setAdvertiserView(layout.findViewById(R.id.ad_advertiser));
                        layout.setCallToActionView(layout.findViewById(R.id.ad_cta));
                        layout.setIconView(layout.findViewById(R.id.ad_icon));
                        layout.setMediaView((com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView) layout.findViewById(R.id.ad_media));
                        layout.registerNativeAdViews(nativeAd);
                    }
                    @Override public void onAdLoadFailed(LevelPlayNativeAd nativeAd, IronSourceError error) { showToast("LevelPlay Native Error: " + error.getErrorMessage()); }
                    @Override public void onAdClicked(LevelPlayNativeAd nativeAd, AdInfo adInfo) {}
                    @Override public void onAdImpression(LevelPlayNativeAd nativeAd, AdInfo adInfo) {}
                })
                .build();
        lpNativeAd.loadAd();
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
        showToast("Loading AdMob Rewarded Interstitial...");
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(this, getString(R.string.admob_rewarded_interstitial_ad_unit_id), adRequest,
            new RewardedInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                    showToast("AdMob Rewarded Loaded");
                    admobRewardedInterstitialAd = ad;
                    admobRewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() { showToast("AdMob Rewarded Dismissed"); }
                        @Override public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) { showToast("AdMob Rewarded Show Failed: " + adError.getMessage()); }
                        @Override public void onAdShowedFullScreenContent() { showToast("AdMob Rewarded Displayed"); }
                    });
                    admobRewardedInterstitialAd.show(AdTestingActivity.this, new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                            showToast("AdMob Reward Earned: " + rewardItem.getAmount() + " " + rewardItem.getType());
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    showToast("AdMob Rewarded Load Failed: " + loadAdError.getMessage());
                    admobRewardedInterstitialAd = null;
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
}
