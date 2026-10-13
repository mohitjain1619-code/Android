package com.mohit.camverz;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Meta Audience Network Imports
import com.facebook.ads.Ad;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BidderTokenProvider;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.RewardedInterstitialAd;
import com.facebook.ads.RewardedInterstitialAdListener;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;

// Google Mobile Ads (AdMob) Imports
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

// Unity LevelPlay Mediation Imports
import com.unity3d.mediation.LevelPlay;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitRequest;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.integration.IntegrationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdTestingActivity extends AppCompatActivity {
    private static final String TAG = "AdTestingActivity";

    // 1. Meta Audience Network Configuration
    public static final String META_APP_ID = "1045884118260179";
    public static final String META_PLACEMENT_INTERSTITIAL = "1045884118260179_1063416116506979";
    public static final String META_PLACEMENT_REWARDED_INTERSTITIAL = "1045884118260179_1063416119840312";
    public static final String META_PLACEMENT_NATIVE = "1045884118260179_1063416113173646";

    // 2. Google AdMob Configuration (Live User Units)
    public static final String ADMOB_APP_ID = "ca-app-pub-6992986179206821~2715651414";
    public static final String ADMOB_PLACEMENT_INTERSTITIAL = "ca-app-pub-6992986179206821/9362140189";
    public static final String ADMOB_PLACEMENT_REWARDED = "ca-app-pub-6992986179206821/2063439948";
    public static final String ADMOB_PLACEMENT_NATIVE = "ca-app-pub-6992986179206821/9014655823";

    // Google AdMob Universal Sample Test Units
    public static final String ADMOB_SAMPLE_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    public static final String ADMOB_SAMPLE_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    public static final String ADMOB_SAMPLE_NATIVE = "ca-app-pub-3940256099942544/2247696110";

    // 3. Unity LevelPlay Mediation Configuration
    public static final String LEVELPLAY_APP_KEY = "28890f0b5";
    public static final String LEVELPLAY_PLACEMENT_INTERSTITIAL = "yw7j51u0q3eg5aai";

    public enum AdNetwork {
        META,
        ADMOB,
        LEVELPLAY
    }

    public enum AdType {
        INTERSTITIAL,
        REWARDED_INTERSTITIAL,
        NATIVE
    }

    private AdNetwork currentNetwork = AdNetwork.META;
    private AdType currentType = AdType.INTERSTITIAL;

    // Network Tabs
    private TextView tabNetworkMeta;
    private TextView tabNetworkAdMob;
    private TextView tabNetworkLevelPlay;

    // Format Tabs
    private View layoutFormatTabs;
    private TextView tabInterstitial;
    private TextView tabRewarded;
    private TextView tabNative;

    // Placement info Views
    private TextView tvAppIdLabel;
    private TextView tvAppId;
    private TextView tvPlacementIdLabel;
    private TextView tvPlacementId;
    private TextView tvAdFormat;
    private TextView tvSdkStatus;
    private TextView tvAdStateBadge;
    private View layoutRewardStatus;
    private TextView tvRewardBadge;

    // Sample Test Toggle (AdMob only)
    private View layoutSampleToggle;
    private CheckBox cbUseSampleAdUnit;

    // Whitelist / Integration Info Views
    private TextView tvWhitelistTitle;
    private TextView tvWhitelistDetails;
    private TextView tvWhitelistNote;
    private Button btnValidateAdapters;

    // Buttons & Console
    private Button btnLoadAd;
    private Button btnShowAd;
    private Button btnResetAd;
    private TextView tvConsoleLogs;
    private ScrollView consoleScrollView;

    // Native Ad Preview Containers
    private View cardNativePreview;
    private NativeAdLayout nativeAdContainer;
    private FrameLayout admobNativeContainer;

    // Meta Ad Instances
    private com.facebook.ads.InterstitialAd metaInterstitialAd;
    private RewardedInterstitialAd metaRewardedInterstitialAd;
    private RewardedVideoAd metaRewardedVideoAd;
    private NativeAd metaNativeAd;

    // AdMob Ad Instances
    private com.google.android.gms.ads.interstitial.InterstitialAd admobInterstitialAd;
    private RewardedAd admobRewardedAd;
    private com.google.android.gms.ads.nativead.NativeAd admobNativeAd;

    // LevelPlay Ad Instances
    private LevelPlayInterstitialAd levelPlayInterstitialAd;
    private boolean isLevelPlayInitialized = false;
    private boolean isLevelPlayLoadPending = false;
    private boolean isAutoShowPending = false;

    private boolean isAdLoading = false;
    private boolean isRewardEarned = false;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_testing);

        // Apply edge-to-edge window insets
        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
                return windowInsets;
            });
        }

        // Initialize UI components
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabNetworkMeta = findViewById(R.id.tabNetworkMeta);
        tabNetworkAdMob = findViewById(R.id.tabNetworkAdMob);
        tabNetworkLevelPlay = findViewById(R.id.tabNetworkLevelPlay);

        layoutFormatTabs = findViewById(R.id.layoutFormatTabs);
        tabInterstitial = findViewById(R.id.tabInterstitial);
        tabRewarded = findViewById(R.id.tabRewarded);
        tabNative = findViewById(R.id.tabNative);

        tvAppIdLabel = findViewById(R.id.tvAppIdLabel);
        tvAppId = findViewById(R.id.tvAppId);
        tvPlacementIdLabel = findViewById(R.id.tvPlacementIdLabel);
        tvPlacementId = findViewById(R.id.tvPlacementId);
        tvAdFormat = findViewById(R.id.tvAdFormat);
        tvSdkStatus = findViewById(R.id.tvSdkStatus);
        tvAdStateBadge = findViewById(R.id.tvAdStateBadge);
        layoutRewardStatus = findViewById(R.id.layoutRewardStatus);
        tvRewardBadge = findViewById(R.id.tvRewardBadge);

        layoutSampleToggle = findViewById(R.id.layoutSampleToggle);
        cbUseSampleAdUnit = findViewById(R.id.cbUseSampleAdUnit);

        tvWhitelistTitle = findViewById(R.id.tvWhitelistTitle);
        tvWhitelistDetails = findViewById(R.id.tvWhitelistDetails);
        tvWhitelistNote = findViewById(R.id.tvWhitelistNote);
        btnValidateAdapters = findViewById(R.id.btnValidateAdapters);

        btnLoadAd = findViewById(R.id.btnLoadAd);
        btnShowAd = findViewById(R.id.btnShowAd);
        btnResetAd = findViewById(R.id.btnResetAd);
        tvConsoleLogs = findViewById(R.id.tvConsoleLogs);
        consoleScrollView = findViewById(R.id.consoleScrollView);

        cardNativePreview = findViewById(R.id.cardNativePreview);
        nativeAdContainer = findViewById(R.id.nativeAdContainer);
        admobNativeContainer = findViewById(R.id.admobNativeContainer);

        TextView btnClearLogs = findViewById(R.id.btnClearLogs);
        TextView btnCopyLogs = findViewById(R.id.btnCopyLogs);

        btnClearLogs.setOnClickListener(v -> tvConsoleLogs.setText(""));
        btnCopyLogs.setOnClickListener(v -> copyLogsToClipboard());

        // Network switcher
        tabNetworkMeta.setOnClickListener(v -> selectNetwork(AdNetwork.META));
        tabNetworkAdMob.setOnClickListener(v -> selectNetwork(AdNetwork.ADMOB));
        tabNetworkLevelPlay.setOnClickListener(v -> selectNetwork(AdNetwork.LEVELPLAY));

        // Format switcher
        tabInterstitial.setOnClickListener(v -> selectAdType(AdType.INTERSTITIAL));
        tabRewarded.setOnClickListener(v -> selectAdType(AdType.REWARDED_INTERSTITIAL));
        tabNative.setOnClickListener(v -> selectAdType(AdType.NATIVE));

        cbUseSampleAdUnit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateUiForCurrentSelection();
            appendLog("ℹ️ [SAMPLE TOGGLE] Sample Test IDs " + (isChecked ? "ENABLED" : "DISABLED"));
        });

        btnValidateAdapters.setOnClickListener(v -> {
            appendLog("🔍 [VALIDATE] Running LevelPlay IntegrationHelper validation...");
            try {
                IntegrationHelper.validateIntegration(this);
                appendLog("✓ Validation executed! Check Android Studio Logcat for the complete adapter compatibility matrix.");
                Toast.makeText(this, "Adapter validation executed! Inspect Logcat for table.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                appendLog("⚠️ [VALIDATE ERROR] " + e.getMessage());
            }
        });

        btnLoadAd.setOnClickListener(v -> runCurrentAdNow());
        btnShowAd.setOnClickListener(v -> showCurrentAd());
        btnResetAd.setOnClickListener(v -> resetCurrentAd());

        // Initial setup
        updateUiForCurrentSelection();

        // Initialize SDKs
        initializeMetaSdk();
        initializeAdMobSdk();
        initializeLevelPlaySdk();
    }

    private void selectNetwork(AdNetwork network) {
        if (currentNetwork == network) return;

        resetCurrentAd();
        currentNetwork = network;
        currentType = AdType.INTERSTITIAL;

        updateUiForCurrentSelection();
        appendLog("🔄 [NETWORK SWITCH] Selected " + getNetworkName(network));
    }

    private String getNetworkName(AdNetwork network) {
        switch (network) {
            case META: return "Meta Audience Network";
            case ADMOB: return "Google AdMob";
            case LEVELPLAY: return "Unity LevelPlay Mediation";
            default: return "Unknown";
        }
    }

    private void selectAdType(AdType type) {
        if (currentType == type) return;

        resetCurrentAd();
        currentType = type;
        updateUiForCurrentSelection();
        appendLog("🔄 [FORMAT SWITCH] Selected " + getFormatName(type));
    }

    private String getFormatName(AdType type) {
        switch (type) {
            case INTERSTITIAL: return "Interstitial";
            case REWARDED_INTERSTITIAL: return "Rewarded";
            case NATIVE: return "Native Ad";
            default: return "Unknown";
        }
    }

    private void updateUiForCurrentSelection() {
        boolean useSample = cbUseSampleAdUnit.isChecked();

        // Reset Network Switcher Tabs
        tabNetworkMeta.setBackground(null);
        tabNetworkMeta.setTextColor(Color.parseColor("#9CA3AF"));
        tabNetworkAdMob.setBackground(null);
        tabNetworkAdMob.setTextColor(Color.parseColor("#9CA3AF"));
        tabNetworkLevelPlay.setBackground(null);
        tabNetworkLevelPlay.setTextColor(Color.parseColor("#9CA3AF"));

        layoutSampleToggle.setVisibility(View.GONE);
        btnValidateAdapters.setVisibility(View.GONE);
        layoutFormatTabs.setVisibility(View.VISIBLE);

        if (currentNetwork == AdNetwork.META) {
            tabNetworkMeta.setBackgroundResource(R.drawable.bg_button_primary);
            tabNetworkMeta.setTextColor(Color.WHITE);

            tabRewarded.setVisibility(View.VISIBLE);
            tabNative.setVisibility(View.VISIBLE);

            tvAppIdLabel.setText("Meta App ID:");
            tvAppId.setText(META_APP_ID);
            tvPlacementIdLabel.setText("Placement ID:");

            tvWhitelistTitle.setText("META CONSOLE TEST DEVICES");
            tvWhitelistDetails.setText("✓ Registered on Meta Monetization Manager:\n • r: b94e5118-43fc-4881-b07b-0ecf44bbb6ee\n • samsung: 1e278238-70d2-418a-acc3-10ca1ad1abe1");
            tvWhitelistNote.setText("Meta servers deliver test ads automatically to registered test devices.");

        } else if (currentNetwork == AdNetwork.ADMOB) {
            tabNetworkAdMob.setBackgroundResource(R.drawable.bg_button_primary);
            tabNetworkAdMob.setTextColor(Color.WHITE);

            tabRewarded.setVisibility(View.VISIBLE);
            tabNative.setVisibility(View.VISIBLE);
            layoutSampleToggle.setVisibility(View.VISIBLE);

            tvAppIdLabel.setText("AdMob App ID:");
            tvAppId.setText(ADMOB_APP_ID);
            tvPlacementIdLabel.setText(useSample ? "Sample Test ID:" : "Live Ad Unit ID:");

            tvWhitelistTitle.setText("GOOGLE ADMOB INTEGRATION");
            tvWhitelistDetails.setText("✓ Google Mobile Ads (GMA) SDK v23.6.0\n • Interstitial: " + (useSample ? ADMOB_SAMPLE_INTERSTITIAL : ADMOB_PLACEMENT_INTERSTITIAL) + "\n • Rewarded: " + (useSample ? ADMOB_SAMPLE_REWARDED : ADMOB_PLACEMENT_REWARDED) + "\n • Native: " + (useSample ? ADMOB_SAMPLE_NATIVE : ADMOB_PLACEMENT_NATIVE));
            tvWhitelistNote.setText("Client Test Devices configured via RequestConfiguration. Use toggle above to test with Google's official sample units if live unit propagation returns 403.");

        } else if (currentNetwork == AdNetwork.LEVELPLAY) {
            tabNetworkLevelPlay.setBackgroundResource(R.drawable.bg_button_primary);
            tabNetworkLevelPlay.setTextColor(Color.WHITE);

            // Currently focusing specifically on Interstitial for LevelPlay
            tabRewarded.setVisibility(View.GONE);
            tabNative.setVisibility(View.GONE);
            btnValidateAdapters.setVisibility(View.VISIBLE);

            tvAppIdLabel.setText("LevelPlay App Key:");
            tvAppId.setText(LEVELPLAY_APP_KEY);
            tvPlacementIdLabel.setText("Ad Unit ID:");

            tvWhitelistTitle.setText("LEVELPLAY BIDDING MEDIATION");
            tvWhitelistDetails.setText("✓ Unity LevelPlay SDK v8.7.0 (Unified)\n • Meta Audience Network Bidding Adapter: Active\n • Google AdMob Bidding Adapter: Active\n • Unity Ads Bidding Network: Active (Built-in)\n • Ad Unit: " + LEVELPLAY_PLACEMENT_INTERSTITIAL);
            tvWhitelistNote.setText("In-app bidding auction runs automatically across Meta, AdMob, and Unity Ads. Click 'Validate Adapters' below to inspect logs.");

            if (IronSource.isInterstitialReady()) {
                tvSdkStatus.setText("Initialized & Ready (Cached ✓)");
                tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
            } else if (isLevelPlayInitialized) {
                tvSdkStatus.setText("Initialized (LevelPlay ✓)");
                tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
            } else {
                tvSdkStatus.setText("Initializing (LevelPlay)...");
                tvSdkStatus.setTextColor(Color.parseColor("#FBBF24"));
            }
        }

        // Format Tabs Highlight
        tabInterstitial.setBackground(null);
        tabInterstitial.setTextColor(Color.parseColor("#9CA3AF"));
        tabRewarded.setBackground(null);
        tabRewarded.setTextColor(Color.parseColor("#9CA3AF"));
        tabNative.setBackground(null);
        tabNative.setTextColor(Color.parseColor("#9CA3AF"));

        layoutRewardStatus.setVisibility(View.GONE);
        cardNativePreview.setVisibility(View.GONE);
        btnShowAd.setVisibility(View.VISIBLE);

        if (currentNetwork == AdNetwork.LEVELPLAY) {
            tabInterstitial.setBackgroundResource(R.drawable.bg_button_primary);
            tabInterstitial.setTextColor(Color.WHITE);

            tvPlacementId.setText(LEVELPLAY_PLACEMENT_INTERSTITIAL);
            tvAdFormat.setText("Interstitial (LevelPlay Multi-Bidding)");

            btnLoadAd.setText("⚡ RUN LEVELPLAY AD (1-Click)");
            btnShowAd.setText("Show Cached Interstitial");

        } else if (currentNetwork == AdNetwork.ADMOB) {
            if (currentType == AdType.INTERSTITIAL) {
                tabInterstitial.setBackgroundResource(R.drawable.bg_button_primary);
                tabInterstitial.setTextColor(Color.WHITE);

                tvPlacementId.setText(useSample ? ADMOB_SAMPLE_INTERSTITIAL : ADMOB_PLACEMENT_INTERSTITIAL);
                tvAdFormat.setText("Interstitial (AdMob Fullscreen)");

                btnLoadAd.setText("⚡ RUN ADMOB AD (1-Click)");
                btnShowAd.setText("Show Cached Interstitial");

            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                tabRewarded.setBackgroundResource(R.drawable.bg_button_primary);
                tabRewarded.setTextColor(Color.WHITE);

                tvPlacementId.setText(useSample ? ADMOB_SAMPLE_REWARDED : ADMOB_PLACEMENT_REWARDED);
                tvAdFormat.setText("Rewarded Video (AdMob)");
                layoutRewardStatus.setVisibility(View.VISIBLE);
                resetRewardBadge();

                btnLoadAd.setText("⚡ RUN ADMOB REWARDED (1-Click)");
                btnShowAd.setText("Show Cached Rewarded");

            } else if (currentType == AdType.NATIVE) {
                tabNative.setBackgroundResource(R.drawable.bg_button_primary);
                tabNative.setTextColor(Color.WHITE);

                tvPlacementId.setText(useSample ? ADMOB_SAMPLE_NATIVE : ADMOB_PLACEMENT_NATIVE);
                tvAdFormat.setText("Native (AdMob Custom Layout)");

                btnLoadAd.setText("⚡ LOAD & RENDER ADMOB NATIVE");
                btnShowAd.setVisibility(View.GONE);
            }
        } else {
            if (currentType == AdType.INTERSTITIAL) {
                tabInterstitial.setBackgroundResource(R.drawable.bg_button_primary);
                tabInterstitial.setTextColor(Color.WHITE);

                tvPlacementId.setText(META_PLACEMENT_INTERSTITIAL);
                tvAdFormat.setText("Interstitial (Meta Fullscreen)");

                btnLoadAd.setText("⚡ RUN META AD (1-Click)");
                btnShowAd.setText("Show Cached Interstitial");

            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                tabRewarded.setBackgroundResource(R.drawable.bg_button_primary);
                tabRewarded.setTextColor(Color.WHITE);

                tvPlacementId.setText(META_PLACEMENT_REWARDED_INTERSTITIAL);
                tvAdFormat.setText("Rewarded Interstitial (Meta Video + Reward)");
                layoutRewardStatus.setVisibility(View.VISIBLE);
                resetRewardBadge();

                btnLoadAd.setText("⚡ RUN META REWARDED (1-Click)");
                btnShowAd.setText("Show Cached Rewarded");

            } else if (currentType == AdType.NATIVE) {
                tabNative.setBackgroundResource(R.drawable.bg_button_primary);
                tabNative.setTextColor(Color.WHITE);

                tvPlacementId.setText(META_PLACEMENT_NATIVE);
                tvAdFormat.setText("Native (Meta Custom Layout)");

                btnLoadAd.setText("Load Meta Native Ad");
                btnShowAd.setVisibility(View.GONE);
            }
        }

        updateAdState("IDLE", "#34D399");
    }

    private void resetRewardBadge() {
        isRewardEarned = false;
        if (tvRewardBadge != null) {
            tvRewardBadge.setText("NOT WATCHED");
            tvRewardBadge.setTextColor(Color.parseColor("#9CA3AF"));
            tvRewardBadge.setBackgroundColor(Color.parseColor("#269CA3AF"));
        }
    }

    private void setRewardEarnedBadge() {
        isRewardEarned = true;
        if (tvRewardBadge != null) {
            tvRewardBadge.setText("🎁 REWARD EARNED ✓");
            tvRewardBadge.setTextColor(Color.parseColor("#34D399"));
            tvRewardBadge.setBackgroundColor(Color.parseColor("#2634D399"));
        }
    }

    // =========================================================================
    // SDK INITIALIZATION
    // =========================================================================
    private void initializeMetaSdk() {
        AudienceNetworkAds.buildInitSettings(this)
            .withInitListener(initResult -> {
                runOnUiThread(() -> {
                    if (initResult.isSuccess()) {
                        if (currentNetwork == AdNetwork.META) {
                            tvSdkStatus.setText("Initialized (Meta ✓)");
                            tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
                        }
                        appendLog("🟢 [META INIT] Meta Audience Network SDK Initialized successfully.");
                    } else {
                        appendLog("🔴 [META INIT FAILED] " + initResult.getMessage());
                    }
                });
            })
            .initialize();
    }

    private void initializeAdMobSdk() {
        List<String> testDeviceIds = Arrays.asList(
            "1e278238-70d2-418a-acc3-10ca1ad1abe1",
            "b94e5118-43fc-4881-b07b-0ecf44bbb6ee",
            AdRequest.DEVICE_ID_EMULATOR
        );
        RequestConfiguration configuration = new RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build();
        MobileAds.setRequestConfiguration(configuration);

        MobileAds.initialize(this, initializationStatus -> {
            runOnUiThread(() -> {
                if (currentNetwork == AdNetwork.ADMOB) {
                    tvSdkStatus.setText("Initialized (AdMob ✓)");
                    tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
                }
                appendLog("🟢 [ADMOB INIT] Google Mobile Ads SDK Initialized successfully.");
            });
        });
    }

    private void initializeLevelPlaySdk() {
        appendLog("🚀 [LEVELPLAY INIT] Initializing via AdManager (IronSource LevelPlay Mediation)...");
        try {
            IronSource.setAdaptersDebug(true);

            IronSource.setLevelPlayInterstitialListener(new com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener() {
                @Override
                public void onAdReady(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnShowAd.setEnabled(true);
                        updateAdState("LOADED (READY)", "#34D399");
                        String network = adInfo != null ? adInfo.getAdNetwork() : "Mediation Winner";
                        appendLog("🟢 [LEVELPLAY READY] Auction won by " + network + "!");
                        if (isAutoShowPending) {
                            isAutoShowPending = false;
                            appendLog("🚀 [1-CLICK AUTO-SHOW] Showing LevelPlay Interstitial now!");
                            IronSource.showInterstitial(LEVELPLAY_PLACEMENT_INTERSTITIAL);
                        } else {
                            Toast.makeText(AdTestingActivity.this, "LevelPlay Interstitial Ready (" + network + ")!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onAdLoadFailed(com.ironsource.mediationsdk.logger.IronSourceError error) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        isAutoShowPending = false;
                        btnShowAd.setEnabled(false);
                        updateAdState("FAILED (" + (error != null ? error.getErrorCode() : -1) + ")", "#EF4444");
                        appendLog("🔴 [LEVELPLAY LOAD FAILED] " + (error != null ? error.getErrorMessage() : "Unknown"));
                        Toast.makeText(AdTestingActivity.this, "LevelPlay Load Failed: " + (error != null ? error.getErrorMessage() : ""), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onAdOpened(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("DISPLAYING", "#60A5FA");
                        appendLog("🔵 [LEVELPLAY DISPLAYED] Presented by " + (adInfo != null ? adInfo.getAdNetwork() : "network"));
                    });
                }

                @Override
                public void onAdShowSucceeded(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    runOnUiThread(() -> appendLog("🟢 [LEVELPLAY SHOW SUCCEEDED] Interstitial presented."));
                }

                @Override
                public void onAdShowFailed(com.ironsource.mediationsdk.logger.IronSourceError error, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("SHOW FAILED", "#EF4444");
                        appendLog("🔴 [LEVELPLAY SHOW FAILED] " + (error != null ? error.getErrorMessage() : "Unknown"));
                    });
                }

                @Override
                public void onAdClicked(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    runOnUiThread(() -> appendLog("🟡 [LEVELPLAY CLICKED] Clicked on " + (adInfo != null ? adInfo.getAdNetwork() : "ad")));
                }

                @Override
                public void onAdClosed(com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("IDLE (CLOSED)", "#34D399");
                        appendLog("⏹️ [LEVELPLAY CLOSED] Ad closed.");
                        // Preload next ad in background
                        IronSource.loadInterstitial();
                    });
                }
            });

            // Initialize IronSource with Interstitial & Rewarded Video
            IronSource.init(this, LEVELPLAY_APP_KEY, () -> {
                runOnUiThread(() -> {
                    isLevelPlayInitialized = true;
                    if (currentNetwork == AdNetwork.LEVELPLAY) {
                        tvSdkStatus.setText("Initialized (LevelPlay ✓)");
                        tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
                        btnLoadAd.setEnabled(true);
                    }
                    appendLog("🟢 [LEVELPLAY INIT SUCCESS] IronSource LevelPlay Mediation SDK initialized!");
                    appendLog("   App Key: " + LEVELPLAY_APP_KEY);
                    appendLog("   Bidding: Meta, AdMob, Unity Ads");

                    // Preload immediately
                    IronSource.loadInterstitial();
                    appendLog("📡 [PRELOAD] Background Interstitial load dispatched!");
                });
            }, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.REWARDED_VIDEO);

            isLevelPlayInitialized = true;
            if (currentNetwork == AdNetwork.LEVELPLAY) {
                tvSdkStatus.setText("Initialized (LevelPlay ✓)");
                tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
            }

        } catch (Exception e) {
            appendLog("❌ [LEVELPLAY INIT EXCEPTION] " + e.getMessage());
        }
    }

    // =========================================================================
    // 1-CLICK INSTANT RUNNER & DISPATCHER
    // =========================================================================
    private void runCurrentAdNow() {
        if (isAdLoading) {
            Toast.makeText(this, "Ad request is already in progress... Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentNetwork == AdNetwork.LEVELPLAY) {
            if (IronSource.isInterstitialReady()) {
                appendLog("🟢 [LEVELPLAY READY] Interstitial is in cache! Showing immediately...");
                IronSource.showInterstitial(LEVELPLAY_PLACEMENT_INTERSTITIAL);
                return;
            }

            // Not ready yet: 1-Click Auto-load & auto-show!
            isAutoShowPending = true;
            isAdLoading = true;
            updateAdState("LOADING & SHOWING...", "#FBBF24");
            appendLog("⏳ [LEVELPLAY 1-CLICK] Interstitial loading... will auto-show the instant it arrives!");
            Toast.makeText(this, "Loading LevelPlay Interstitial... Showing automatically.", Toast.LENGTH_SHORT).show();

            IronSource.loadInterstitial();
            loadLevelPlayInterstitial();

        } else if (currentNetwork == AdNetwork.ADMOB) {
            if (currentType == AdType.INTERSTITIAL) {
                if (admobInterstitialAd != null) {
                    appendLog("🟢 [ADMOB READY] Cached! Showing immediately...");
                    admobInterstitialAd.show(this);
                    return;
                }
                isAutoShowPending = true;
                isAdLoading = true;
                updateAdState("LOADING & SHOWING...", "#FBBF24");
                appendLog("⏳ [ADMOB 1-CLICK] Requesting Interstitial... will show automatically!");
                Toast.makeText(this, "Loading AdMob Interstitial... Will show automatically.", Toast.LENGTH_SHORT).show();
                loadAdMobInterstitial();

            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                if (admobRewardedAd != null) {
                    showAdMobRewarded();
                    return;
                }
                isAutoShowPending = true;
                isAdLoading = true;
                updateAdState("LOADING & SHOWING...", "#FBBF24");
                loadAdMobRewarded();
            } else {
                loadAdMobNative();
            }
        } else {
            // META
            if (currentType == AdType.INTERSTITIAL) {
                if (metaInterstitialAd != null && metaInterstitialAd.isAdLoaded()) {
                    appendLog("🟢 [META READY] Cached! Showing immediately...");
                    metaInterstitialAd.show();
                    return;
                }
                isAutoShowPending = true;
                isAdLoading = true;
                updateAdState("LOADING & SHOWING...", "#FBBF24");
                appendLog("⏳ [META 1-CLICK] Requesting Interstitial... will show automatically!");
                Toast.makeText(this, "Loading Meta Interstitial... Will show automatically.", Toast.LENGTH_SHORT).show();
                loadMetaInterstitial();

            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                if (metaRewardedVideoAd != null && metaRewardedVideoAd.isAdLoaded()) {
                    showMetaRewardedInterstitial();
                    return;
                }
                isAutoShowPending = true;
                isAdLoading = true;
                updateAdState("LOADING & SHOWING...", "#FBBF24");
                loadMetaRewardedInterstitial();
            } else {
                loadMetaNativeAd();
            }
        }
    }

    private void loadCurrentAd() {
        runCurrentAdNow();
    }

    private void showCurrentAd() {
        if (currentNetwork == AdNetwork.LEVELPLAY) {
            if (IronSource.isInterstitialReady()) {
                IronSource.showInterstitial(LEVELPLAY_PLACEMENT_INTERSTITIAL);
            } else {
                runCurrentAdNow();
            }
        } else if (currentNetwork == AdNetwork.ADMOB) {
            if (currentType == AdType.INTERSTITIAL) {
                if (admobInterstitialAd != null) {
                    admobInterstitialAd.show(this);
                } else {
                    runCurrentAdNow();
                }
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                if (admobRewardedAd != null) {
                    showAdMobRewarded();
                } else {
                    runCurrentAdNow();
                }
            }
        } else {
            if (currentType == AdType.INTERSTITIAL) {
                if (metaInterstitialAd != null && metaInterstitialAd.isAdLoaded()) {
                    metaInterstitialAd.show();
                } else {
                    runCurrentAdNow();
                }
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                if (metaRewardedVideoAd != null && metaRewardedVideoAd.isAdLoaded()) {
                    showMetaRewardedInterstitial();
                } else {
                    runCurrentAdNow();
                }
            }
        }
    }

    private void resetCurrentAd() {
        destroyLevelPlayInterstitial();
        destroyAdMobInterstitial();
        destroyAdMobRewarded();
        destroyAdMobNative();

        destroyMetaInterstitial();
        destroyMetaRewardedAds();
        destroyMetaNativeAd();

        resetRewardBadge();

        isAdLoading = false;
        btnLoadAd.setEnabled(true);
        btnShowAd.setEnabled(false);
        updateAdState("IDLE", "#34D399");
        appendLog("🔄 [RESET] Ad instances cleared and state reset.");
    }

    // =========================================================================
    // 1. UNITY LEVELPLAY INTERSTITIAL (BIDDING MEDIATION)
    // =========================================================================
    private void loadLevelPlayInterstitial() {
        destroyLevelPlayInterstitial();

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("LOADING (LEVELPLAY)...", "#FBBF24");

        appendLog("📡 [LEVELPLAY REQUEST] Requesting LevelPlay Interstitial Ad...");
        appendLog("   Ad Unit ID: " + LEVELPLAY_PLACEMENT_INTERSTITIAL);
        appendLog("   Auction Pool: Meta Audience Network, Google AdMob, Unity Ads");

        try {
            levelPlayInterstitialAd = new LevelPlayInterstitialAd(LEVELPLAY_PLACEMENT_INTERSTITIAL);

            levelPlayInterstitialAd.setListener(new LevelPlayInterstitialAdListener() {
                @Override
                public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(true);
                        updateAdState("LOADED (LEVELPLAY READY)", "#34D399");

                        String winner = adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "Mediation Winner";
                        appendLog("🟢 [LEVELPLAY LOADED] Interstitial auction won by " + winner + "!");
                        appendLog("   Network: " + winner);
                        appendLog("   Auction ID: " + adInfo.getAuctionId());
                        if (adInfo.getRevenue() > 0) {
                            appendLog("   Revenue: $" + adInfo.getRevenue());
                        }
                        Toast.makeText(AdTestingActivity.this, "LevelPlay Interstitial Ready (" + winner + ")!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdLoadFailed(@NonNull LevelPlayAdError adError) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(false);
                        updateAdState("FAILED (CODE " + adError.getErrorCode() + ")", "#EF4444");

                        appendLog("🔴 [LEVELPLAY ERROR] onAdLoadFailed() received:");
                        appendLog("   Code: " + adError.getErrorCode());
                        appendLog("   Message: " + adError.getErrorMessage());

                        Toast.makeText(AdTestingActivity.this, "LevelPlay Error " + adError.getErrorCode() + ": " + adError.getErrorMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("DISPLAYING (LEVELPLAY)", "#60A5FA");
                        appendLog("🔵 [LEVELPLAY DISPLAYED] Fullscreen interstitial presented by " + adInfo.getAdNetwork() + ".");
                    });
                }

                @Override
                public void onAdDisplayFailed(@NonNull LevelPlayAdError adError, @NonNull LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        btnShowAd.setEnabled(false);
                        btnLoadAd.setEnabled(true);
                        updateAdState("DISPLAY FAILED", "#EF4444");
                        appendLog("🔴 [LEVELPLAY DISPLAY FAILED] " + adError.getErrorMessage());
                    });
                }

                @Override
                public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        appendLog("🟡 [LEVELPLAY CLICKED] User clicked on ad from " + adInfo.getAdNetwork() + ".");
                    });
                }

                @Override
                public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        btnShowAd.setEnabled(false);
                        btnLoadAd.setEnabled(true);
                        updateAdState("DISMISSED / IDLE", "#9CA3AF");

                        appendLog("⚪ [LEVELPLAY CLOSED] Interstitial closed by user.");
                        Toast.makeText(AdTestingActivity.this, "LevelPlay Ad Closed.", Toast.LENGTH_SHORT).show();
                        destroyLevelPlayInterstitial();
                    });
                }
            });

            levelPlayInterstitialAd.loadAd();

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            btnShowAd.setEnabled(false);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [LEVELPLAY EXCEPTION] " + e.getMessage());
            Log.e(TAG, "Error loading LevelPlay interstitial", e);
        }
    }

    private void showLevelPlayInterstitial() {
        if (levelPlayInterstitialAd != null && levelPlayInterstitialAd.isAdReady()) {
            appendLog("▶️ [LEVELPLAY SHOW] Presenting LevelPlay Interstitial...");
            levelPlayInterstitialAd.showAd(this);
        } else {
            Toast.makeText(this, "LevelPlay ad is not ready yet. Please click Load first.", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ [LEVELPLAY WARNING] Interstitial ad is not ready.");
        }
    }

    private void destroyLevelPlayInterstitial() {
        levelPlayInterstitialAd = null;
    }

    // =========================================================================
    // 2. GOOGLE ADMOB INTERSTITIAL
    // =========================================================================
    private void loadAdMobInterstitial() {
        destroyAdMobInterstitial();

        String adUnitId = cbUseSampleAdUnit.isChecked() ? ADMOB_SAMPLE_INTERSTITIAL : ADMOB_PLACEMENT_INTERSTITIAL;

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("LOADING (ADMOB)...", "#FBBF24");

        appendLog("📡 [ADMOB REQUEST] Requesting Google AdMob Interstitial Ad...");
        appendLog("   Ad Unit ID: " + adUnitId);

        try {
            AdRequest adRequest = new AdRequest.Builder().build();

            com.google.android.gms.ads.interstitial.InterstitialAd.load(
                this,
                adUnitId,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                        runOnUiThread(() -> {
                            isAdLoading = false;
                            admobInterstitialAd = interstitialAd;
                            btnLoadAd.setEnabled(true);
                            btnShowAd.setEnabled(true);
                            updateAdState("LOADED (ADMOB READY)", "#34D399");

                            appendLog("🟢 [ADMOB LOADED] InterstitialAd.load() successful!");
                            appendLog("   Ad is ready to display. Click 'Show AdMob Interstitial'.");
                            Toast.makeText(AdTestingActivity.this, "AdMob Interstitial Loaded!", Toast.LENGTH_SHORT).show();

                            setupAdMobInterstitialCallbacks();
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        runOnUiThread(() -> {
                            isAdLoading = false;
                            admobInterstitialAd = null;
                            btnLoadAd.setEnabled(true);
                            btnShowAd.setEnabled(false);

                            int code = loadAdError.getCode();
                            String message = loadAdError.getMessage();
                            String explanation = getAdMobErrorExplanation(code);

                            updateAdState("FAILED (CODE " + code + ")", "#EF4444");
                            appendLog("🔴 [ADMOB ERROR] onAdFailedToLoad() received:");
                            appendLog("   Code: " + code + " (" + explanation + ")");
                            appendLog("   Message: " + message);

                            Toast.makeText(AdTestingActivity.this, "AdMob Error " + code + ": " + explanation, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            );

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            btnShowAd.setEnabled(false);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [ADMOB EXCEPTION] " + e.getMessage());
        }
    }

    private void setupAdMobInterstitialCallbacks() {
        if (admobInterstitialAd == null) return;

        admobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnUiThread(() -> {
                    updateAdState("DISPLAYING (ADMOB)", "#60A5FA");
                    appendLog("🔵 [ADMOB DISPLAYED] Interstitial presented fullscreen.");
                });
            }

            @Override
            public void onAdImpression() {
                runOnUiThread(() -> appendLog("🟣 [ADMOB IMPRESSION] Verified impression recorded by Google AdMob servers."));
            }

            @Override
            public void onAdClicked() {
                runOnUiThread(() -> appendLog("🟡 [ADMOB CLICKED] User clicked on AdMob Interstitial."));
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnUiThread(() -> {
                    btnShowAd.setEnabled(false);
                    btnLoadAd.setEnabled(true);
                    updateAdState("DISMISSED / IDLE", "#9CA3AF");

                    appendLog("⚪ [ADMOB DISMISSED] User closed AdMob Interstitial.");
                    Toast.makeText(AdTestingActivity.this, "AdMob Ad Closed.", Toast.LENGTH_SHORT).show();
                    destroyAdMobInterstitial();
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                runOnUiThread(() -> {
                    btnShowAd.setEnabled(false);
                    btnLoadAd.setEnabled(true);
                    updateAdState("SHOW FAILED", "#EF4444");
                    appendLog("🔴 [ADMOB SHOW FAILED] " + adError.getMessage() + " (Code: " + adError.getCode() + ")");
                    destroyAdMobInterstitial();
                });
            }
        });
    }

    private void showAdMobInterstitial() {
        if (admobInterstitialAd != null) {
            appendLog("▶️ [ADMOB SHOW] Presenting Google AdMob Interstitial...");
            admobInterstitialAd.show(this);
        } else {
            Toast.makeText(this, "AdMob ad is not ready. Please click Load first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void destroyAdMobInterstitial() {
        admobInterstitialAd = null;
    }

    // =========================================================================
    // 3. GOOGLE ADMOB REWARDED
    // =========================================================================
    private void loadAdMobRewarded() {
        destroyAdMobRewarded();
        resetRewardBadge();

        String adUnitId = cbUseSampleAdUnit.isChecked() ? ADMOB_SAMPLE_REWARDED : ADMOB_PLACEMENT_REWARDED;

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("LOADING REWARDED...", "#FBBF24");

        appendLog("📡 [ADMOB REQUEST] Requesting Google AdMob Rewarded Ad...");
        appendLog("   Ad Unit ID: " + adUnitId);

        try {
            AdRequest adRequest = new AdRequest.Builder().build();

            RewardedAd.load(
                this,
                adUnitId,
                adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        runOnUiThread(() -> {
                            isAdLoading = false;
                            admobRewardedAd = rewardedAd;
                            btnLoadAd.setEnabled(true);
                            btnShowAd.setEnabled(true);
                            updateAdState("LOADED (REWARDED READY)", "#34D399");

                            appendLog("🟢 [ADMOB REWARDED LOADED] RewardedAd.load() successful!");
                            Toast.makeText(AdTestingActivity.this, "AdMob Rewarded Loaded!", Toast.LENGTH_SHORT).show();

                            setupAdMobRewardedCallbacks();
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        runOnUiThread(() -> {
                            isAdLoading = false;
                            admobRewardedAd = null;
                            btnLoadAd.setEnabled(true);
                            btnShowAd.setEnabled(false);

                            int code = loadAdError.getCode();
                            String message = loadAdError.getMessage();
                            String explanation = getAdMobErrorExplanation(code);

                            updateAdState("FAILED (CODE " + code + ")", "#EF4444");
                            appendLog("🔴 [ADMOB ERROR] onAdFailedToLoad() received:");
                            appendLog("   Code: " + code + " (" + explanation + ")");
                            appendLog("   Message: " + message);

                            Toast.makeText(AdTestingActivity.this, "AdMob Error " + code + ": " + explanation, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            );

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            btnShowAd.setEnabled(false);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [ADMOB EXCEPTION] " + e.getMessage());
        }
    }

    private void setupAdMobRewardedCallbacks() {
        if (admobRewardedAd == null) return;

        admobRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                runOnUiThread(() -> {
                    updateAdState("DISPLAYING (REWARDED)", "#60A5FA");
                    appendLog("🔵 [ADMOB DISPLAYED] Rewarded video presented fullscreen.");
                });
            }

            @Override
            public void onAdImpression() {
                runOnUiThread(() -> appendLog("🟣 [ADMOB IMPRESSION] Rewarded Ad verified impression logged."));
            }

            @Override
            public void onAdClicked() {
                runOnUiThread(() -> appendLog("🟡 [ADMOB CLICKED] User clicked on Rewarded Ad."));
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                runOnUiThread(() -> {
                    btnShowAd.setEnabled(false);
                    btnLoadAd.setEnabled(true);
                    updateAdState("DISMISSED / IDLE", "#9CA3AF");

                    appendLog("⚪ [ADMOB DISMISSED] Rewarded ad dismissed by user.");
                    destroyAdMobRewarded();
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                runOnUiThread(() -> {
                    btnShowAd.setEnabled(false);
                    btnLoadAd.setEnabled(true);
                    updateAdState("SHOW FAILED", "#EF4444");
                    appendLog("🔴 [ADMOB SHOW FAILED] " + adError.getMessage());
                    destroyAdMobRewarded();
                });
            }
        });
    }

    private void showAdMobRewarded() {
        if (admobRewardedAd != null) {
            appendLog("▶️ [ADMOB SHOW] Presenting Google AdMob Rewarded Video...");
            admobRewardedAd.show(this, rewardItem -> {
                runOnUiThread(() -> {
                    setRewardEarnedBadge();
                    appendLog("🎁 [ADMOB REWARD EARNED] User successfully watched ad!");
                    appendLog("   Reward Amount: " + rewardItem.getAmount() + " Type: " + rewardItem.getType());
                    Toast.makeText(AdTestingActivity.this, "🎁 Reward Earned: " + rewardItem.getAmount() + " " + rewardItem.getType(), Toast.LENGTH_LONG).show();
                });
            });
        } else {
            Toast.makeText(this, "AdMob Rewarded ad is not ready. Please click Load first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void destroyAdMobRewarded() {
        admobRewardedAd = null;
    }

    // =========================================================================
    // 4. GOOGLE ADMOB NATIVE
    // =========================================================================
    private void loadAdMobNative() {
        destroyAdMobNative();

        String adUnitId = cbUseSampleAdUnit.isChecked() ? ADMOB_SAMPLE_NATIVE : ADMOB_PLACEMENT_NATIVE;

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        updateAdState("LOADING ADMOB NATIVE...", "#FBBF24");

        appendLog("📡 [ADMOB REQUEST] Requesting Google AdMob Native Ad...");
        appendLog("   Ad Unit ID: " + adUnitId);

        try {
            AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
                .forNativeAd(nativeAd -> {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        admobNativeAd = nativeAd;
                        btnLoadAd.setEnabled(true);
                        updateAdState("LOADED & DISPLAYED", "#34D399");

                        appendLog("🟢 [ADMOB NATIVE LOADED] NativeAd returned successfully!");
                        renderAdMobNativeAd(nativeAd);
                        Toast.makeText(AdTestingActivity.this, "AdMob Native Ad Rendered!", Toast.LENGTH_SHORT).show();
                    });
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        runOnUiThread(() -> {
                            isAdLoading = false;
                            btnLoadAd.setEnabled(true);
                            updateAdState("FAILED (CODE " + loadAdError.getCode() + ")", "#EF4444");

                            int code = loadAdError.getCode();
                            String message = loadAdError.getMessage();
                            String explanation = getAdMobErrorExplanation(code);

                            appendLog("🔴 [ADMOB NATIVE ERROR] onAdFailedToLoad() received:");
                            appendLog("   Code: " + code + " (" + explanation + ")");
                            appendLog("   Message: " + message);

                            Toast.makeText(AdTestingActivity.this, "AdMob Error " + code + ": " + explanation, Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onAdImpression() {
                        runOnUiThread(() -> appendLog("🟣 [ADMOB NATIVE IMPRESSION] Impression recorded by Google servers."));
                    }

                    @Override
                    public void onAdClicked() {
                        runOnUiThread(() -> appendLog("🟡 [ADMOB NATIVE CLICKED] User clicked on Native Ad."));
                    }
                })
                .build();

            adLoader.loadAd(new AdRequest.Builder().build());

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [ADMOB NATIVE EXCEPTION] " + e.getMessage());
        }
    }

    private void renderAdMobNativeAd(com.google.android.gms.ads.nativead.NativeAd nativeAd) {
        try {
            cardNativePreview.setVisibility(View.VISIBLE);
            nativeAdContainer.setVisibility(View.GONE);
            admobNativeContainer.setVisibility(View.VISIBLE);
            admobNativeContainer.removeAllViews();

            NativeAdView adView = (NativeAdView) LayoutInflater.from(this).inflate(R.layout.layout_admob_native_ad, admobNativeContainer, false);

            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_app_icon));
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
            adView.setMediaView(adView.findViewById(R.id.ad_media));

            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

            if (nativeAd.getBody() != null) {
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
                adView.getBodyView().setVisibility(View.VISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.GONE);
            }

            if (nativeAd.getCallToAction() != null) {
                ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
                adView.getCallToActionView().setVisibility(View.VISIBLE);
            } else {
                adView.getCallToActionView().setVisibility(View.GONE);
            }

            if (nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            } else {
                adView.getIconView().setVisibility(View.GONE);
            }

            if (nativeAd.getAdvertiser() != null) {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            } else {
                adView.getAdvertiserView().setVisibility(View.GONE);
            }

            adView.setNativeAd(nativeAd);

            admobNativeContainer.addView(adView);
            appendLog("🖼️ [ADMOB NATIVE RENDERED] Native Ad assets bound and operationalized.");

        } catch (Exception e) {
            appendLog("❌ [ADMOB RENDER EXCEPTION] " + e.getMessage());
        }
    }

    private void destroyAdMobNative() {
        if (admobNativeAd != null) {
            try {
                admobNativeAd.destroy();
            } catch (Exception ignored) {}
            admobNativeAd = null;
        }
        if (admobNativeContainer != null) {
            admobNativeContainer.removeAllViews();
            admobNativeContainer.setVisibility(View.GONE);
        }
        if (cardNativePreview != null && (metaNativeAd == null)) {
            cardNativePreview.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // 5. META INTERSTITIAL
    // =========================================================================
    private void loadMetaInterstitial() {
        destroyMetaInterstitial();

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("LOADING...", "#FBBF24");

        appendLog("📡 [META REQUEST] Requesting Meta Interstitial Ad...");
        appendLog("   Placement ID: " + META_PLACEMENT_INTERSTITIAL);

        try {
            metaInterstitialAd = new com.facebook.ads.InterstitialAd(this, META_PLACEMENT_INTERSTITIAL);

            InterstitialAdListener listener = new InterstitialAdListener() {
                @Override
                public void onAdLoaded(Ad ad) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(true);
                        updateAdState("LOADED (READY)", "#34D399");

                        appendLog("🟢 [META LOADED] onAdLoaded() received!");
                        Toast.makeText(AdTestingActivity.this, "Meta Interstitial Loaded!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Ad ad, com.facebook.ads.AdError adError) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(false);
                        updateAdState("FAILED (" + adError.getErrorCode() + ")", "#EF4444");

                        int code = adError.getErrorCode();
                        String message = adError.getErrorMessage();
                        String explanation = getMetaErrorExplanation(code);

                        appendLog("🔴 [META ERROR] onError() received:");
                        appendLog("   Code: " + code + " (" + explanation + ")");
                        appendLog("   Message: " + message);

                        Toast.makeText(AdTestingActivity.this, "Error " + code + ": " + explanation, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onInterstitialDisplayed(Ad ad) {
                    runOnUiThread(() -> {
                        updateAdState("DISPLAYING", "#60A5FA");
                        appendLog("🔵 [META DISPLAYED] Interstitial ad presented fullscreen.");
                    });
                }

                @Override
                public void onLoggingImpression(Ad ad) {
                    runOnUiThread(() -> appendLog("🟣 [META IMPRESSION] Verified impression recorded."));
                }

                @Override
                public void onAdClicked(Ad ad) {
                    runOnUiThread(() -> appendLog("🟡 [META CLICKED] User clicked on the interstitial ad."));
                }

                @Override
                public void onInterstitialDismissed(Ad ad) {
                    runOnUiThread(() -> {
                        btnShowAd.setEnabled(false);
                        btnLoadAd.setEnabled(true);
                        updateAdState("DISMISSED / IDLE", "#9CA3AF");

                        appendLog("⚪ [META DISMISSED] User closed the ad.");
                        Toast.makeText(AdTestingActivity.this, "Ad closed.", Toast.LENGTH_SHORT).show();
                        destroyMetaInterstitial();
                    });
                }
            };

            metaInterstitialAd.loadAd(
                metaInterstitialAd.buildLoadAdConfig()
                    .withAdListener(listener)
                    .build()
            );

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            btnShowAd.setEnabled(false);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [META EXCEPTION] " + e.getMessage());
        }
    }

    private void showMetaInterstitial() {
        if (metaInterstitialAd != null && metaInterstitialAd.isAdLoaded() && !metaInterstitialAd.isAdInvalidated()) {
            appendLog("▶️ [META SHOW] Presenting Meta Interstitial Ad...");
            metaInterstitialAd.show();
        } else {
            Toast.makeText(this, "Meta ad is not ready. Please click Load first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void destroyMetaInterstitial() {
        if (metaInterstitialAd != null) {
            try {
                metaInterstitialAd.destroy();
            } catch (Exception ignored) {}
            metaInterstitialAd = null;
        }
    }

    // =========================================================================
    // 6. META REWARDED INTERSTITIAL
    // =========================================================================
    private void loadMetaRewardedInterstitial() {
        destroyMetaRewardedAds();
        resetRewardBadge();

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("LOADING...", "#FBBF24");

        appendLog("📡 [META REQUEST] Requesting Meta Rewarded Interstitial Ad...");
        appendLog("   Placement ID: " + META_PLACEMENT_REWARDED_INTERSTITIAL);

        try {
            metaRewardedInterstitialAd = new RewardedInterstitialAd(this, META_PLACEMENT_REWARDED_INTERSTITIAL);

            RewardedInterstitialAdListener listener = new RewardedInterstitialAdListener() {
                @Override
                public void onAdLoaded(Ad ad) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(true);
                        updateAdState("LOADED (READY)", "#34D399");

                        appendLog("🟢 [META LOADED] Rewarded Interstitial loaded and ready.");
                        Toast.makeText(AdTestingActivity.this, "Rewarded Ad Loaded Successfully!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Ad ad, com.facebook.ads.AdError adError) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(false);

                        int code = adError.getErrorCode();
                        String message = adError.getErrorMessage();
                        String explanation = getMetaErrorExplanation(code);

                        if (code == 1011) {
                            appendLog("⚠️ [FORMAT MISMATCH 1011] Attempting RewardedVideoAd loader fallback...");
                            loadMetaRewardedVideoFallback();
                            return;
                        }

                        updateAdState("FAILED (" + code + ")", "#EF4444");
                        appendLog("🔴 [META ERROR] Code: " + code + " (" + explanation + ") - " + message);
                        Toast.makeText(AdTestingActivity.this, "Error " + code + ": " + explanation, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onLoggingImpression(Ad ad) {
                    runOnUiThread(() -> appendLog("🟣 [META IMPRESSION] Verified impression recorded."));
                }

                @Override
                public void onAdClicked(Ad ad) {
                    runOnUiThread(() -> appendLog("🟡 [META CLICKED] User clicked on rewarded ad."));
                }

                @Override
                public void onRewardedInterstitialCompleted() {
                    runOnUiThread(() -> {
                        setRewardEarnedBadge();
                        appendLog("🎁 [REWARD EARNED] User successfully completed video! Reward verified.");
                        Toast.makeText(AdTestingActivity.this, "🎁 Reward Earned!", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onRewardedInterstitialClosed() {
                    runOnUiThread(() -> {
                        btnShowAd.setEnabled(false);
                        btnLoadAd.setEnabled(true);
                        updateAdState("CLOSED / IDLE", "#9CA3AF");

                        appendLog("⚪ [META CLOSED] Rewarded ad closed by user.");
                        destroyMetaRewardedAds();
                    });
                }
            };

            metaRewardedInterstitialAd.loadAd(
                metaRewardedInterstitialAd.buildLoadAdConfig()
                    .withAdListener(listener)
                    .build()
            );

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [META EXCEPTION] " + e.getMessage());
        }
    }

    private void loadMetaRewardedVideoFallback() {
        destroyMetaRewardedAds();
        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("LOADING (RV)...", "#FBBF24");

        try {
            metaRewardedVideoAd = new RewardedVideoAd(this, META_PLACEMENT_REWARDED_INTERSTITIAL);

            RewardedVideoAdListener rvListener = new RewardedVideoAdListener() {
                @Override
                public void onAdLoaded(Ad ad) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(true);
                        updateAdState("LOADED (RV)", "#34D399");
                        appendLog("🟢 [META LOADED] Loaded via RewardedVideoAd!");
                    });
                }

                @Override
                public void onError(Ad ad, com.facebook.ads.AdError adError) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(false);
                        updateAdState("FAILED (" + adError.getErrorCode() + ")", "#EF4444");
                        appendLog("🔴 [RV ERROR] " + adError.getErrorCode() + ": " + adError.getErrorMessage());
                    });
                }

                @Override
                public void onLoggingImpression(Ad ad) {
                    runOnUiThread(() -> appendLog("🟣 [IMPRESSION] Rewarded Video impression logged."));
                }

                @Override
                public void onAdClicked(Ad ad) {
                    runOnUiThread(() -> appendLog("🟡 [CLICKED] Rewarded Video clicked."));
                }

                @Override
                public void onRewardedVideoCompleted() {
                    runOnUiThread(() -> {
                        setRewardEarnedBadge();
                        appendLog("🎁 [REWARD EARNED] Rewarded Video completed!");
                    });
                }

                @Override
                public void onRewardedVideoClosed() {
                    runOnUiThread(() -> {
                        btnShowAd.setEnabled(false);
                        btnLoadAd.setEnabled(true);
                        updateAdState("CLOSED / IDLE", "#9CA3AF");
                        destroyMetaRewardedAds();
                    });
                }
            };

            metaRewardedVideoAd.loadAd(
                metaRewardedVideoAd.buildLoadAdConfig()
                    .withAdListener(rvListener)
                    .build()
            );

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            updateAdState("EXCEPTION", "#EF4444");
        }
    }

    private void showMetaRewardedInterstitial() {
        if (metaRewardedInterstitialAd != null && metaRewardedInterstitialAd.isAdLoaded() && !metaRewardedInterstitialAd.isAdInvalidated()) {
            appendLog("▶️ [META SHOW] Presenting Meta Rewarded Interstitial Ad...");
            metaRewardedInterstitialAd.show();
        } else if (metaRewardedVideoAd != null && metaRewardedVideoAd.isAdLoaded() && !metaRewardedVideoAd.isAdInvalidated()) {
            appendLog("▶️ [META SHOW] Presenting Meta Rewarded Video Ad...");
            metaRewardedVideoAd.show();
        } else {
            Toast.makeText(this, "Rewarded ad is not ready. Please click Load first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void destroyMetaRewardedAds() {
        if (metaRewardedInterstitialAd != null) {
            try {
                metaRewardedInterstitialAd.destroy();
            } catch (Exception ignored) {}
            metaRewardedInterstitialAd = null;
        }
        if (metaRewardedVideoAd != null) {
            try {
                metaRewardedVideoAd.destroy();
            } catch (Exception ignored) {}
            metaRewardedVideoAd = null;
        }
    }

    // =========================================================================
    // 7. META NATIVE ADS
    // =========================================================================
    private void loadMetaNativeAd() {
        destroyMetaNativeAd();

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        updateAdState("LOADING NATIVE...", "#FBBF24");

        appendLog("📡 [META REQUEST] Requesting Meta Native Ad...");
        appendLog("   Placement ID: " + META_PLACEMENT_NATIVE);

        try {
            metaNativeAd = new NativeAd(this, META_PLACEMENT_NATIVE);

            NativeAdListener listener = new NativeAdListener() {
                @Override
                public void onAdLoaded(Ad ad) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        updateAdState("LOADED & DISPLAYED", "#34D399");

                        appendLog("🟢 [META LOADED] Native Ad downloaded successfully!");
                        renderMetaNativeAd();
                        Toast.makeText(AdTestingActivity.this, "Native Ad Rendered Successfully!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Ad ad, com.facebook.ads.AdError adError) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        updateAdState("FAILED (" + adError.getErrorCode() + ")", "#EF4444");

                        int code = adError.getErrorCode();
                        String message = adError.getErrorMessage();
                        String explanation = getMetaErrorExplanation(code);

                        appendLog("🔴 [NATIVE ERROR] onError() received:");
                        appendLog("   Code: " + code + " (" + explanation + ")");
                        appendLog("   Message: " + message);

                        Toast.makeText(AdTestingActivity.this, "Error " + code + ": " + explanation, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onLoggingImpression(Ad ad) {
                    runOnUiThread(() -> appendLog("🟣 [META IMPRESSION] Verified impression recorded for Native Ad."));
                }

                @Override
                public void onAdClicked(Ad ad) {
                    runOnUiThread(() -> appendLog("🟡 [META CLICKED] User clicked on Native Ad component."));
                }

                @Override
                public void onMediaDownloaded(Ad ad) {
                    runOnUiThread(() -> appendLog("🖼️ [META MEDIA DOWNLOADED] Main media assets cached."));
                }
            };

            metaNativeAd.loadAd(
                metaNativeAd.buildLoadAdConfig()
                    .withAdListener(listener)
                    .build()
            );

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            updateAdState("EXCEPTION", "#EF4444");
        }
    }

    private void renderMetaNativeAd() {
        if (metaNativeAd == null || !metaNativeAd.isAdLoaded() || metaNativeAd.isAdInvalidated()) {
            return;
        }

        try {
            metaNativeAd.unregisterView();

            cardNativePreview.setVisibility(View.VISIBLE);
            admobNativeContainer.setVisibility(View.GONE);
            nativeAdContainer.setVisibility(View.VISIBLE);
            nativeAdContainer.removeAllViews();

            View adView = LayoutInflater.from(this).inflate(R.layout.layout_meta_native_ad, nativeAdContainer, false);
            nativeAdContainer.addView(adView);

            TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
            TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
            TextView nativeAdSocial = adView.findViewById(R.id.native_ad_social_context);
            Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);
            MediaView nativeAdMedia = adView.findViewById(R.id.native_ad_media);
            MediaView nativeAdIcon = adView.findViewById(R.id.native_ad_icon);
            FrameLayout adChoicesContainer = adView.findViewById(R.id.ad_choices_container);

            nativeAdTitle.setText(metaNativeAd.getAdvertiserName());
            nativeAdBody.setText(metaNativeAd.getAdBodyText());
            nativeAdCallToAction.setText(metaNativeAd.getAdCallToAction());
            nativeAdCallToAction.setVisibility(metaNativeAd.hasCallToAction() ? View.VISIBLE : View.GONE);

            if (metaNativeAd.getAdSocialContext() != null && !metaNativeAd.getAdSocialContext().isEmpty()) {
                nativeAdSocial.setText(metaNativeAd.getAdSocialContext());
                nativeAdSocial.setVisibility(View.VISIBLE);
            } else {
                nativeAdSocial.setVisibility(View.GONE);
            }

            AdOptionsView adOptionsView = new AdOptionsView(this, metaNativeAd, nativeAdContainer);
            adChoicesContainer.removeAllViews();
            adChoicesContainer.addView(adOptionsView);

            List<View> clickableViews = new ArrayList<>();
            clickableViews.add(nativeAdTitle);
            clickableViews.add(nativeAdCallToAction);
            clickableViews.add(nativeAdMedia);
            clickableViews.add(nativeAdIcon);

            metaNativeAd.registerViewForInteraction(
                nativeAdContainer,
                nativeAdMedia,
                nativeAdIcon,
                clickableViews
            );

            appendLog("🖼️ [RENDERED] Native Ad views bound and registered for user interaction.");

        } catch (Exception e) {
            appendLog("❌ [RENDER EXCEPTION] " + e.getMessage());
        }
    }

    private void destroyMetaNativeAd() {
        if (metaNativeAd != null) {
            try {
                metaNativeAd.unregisterView();
                metaNativeAd.destroy();
            } catch (Exception ignored) {}
            metaNativeAd = null;
        }
        if (nativeAdContainer != null) {
            nativeAdContainer.removeAllViews();
            nativeAdContainer.setVisibility(View.GONE);
        }
        if (cardNativePreview != null && (admobNativeAd == null)) {
            cardNativePreview.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // UTILITIES & LOGGING
    // =========================================================================
    private void updateAdState(String state, String hexColor) {
        if (tvAdStateBadge != null) {
            tvAdStateBadge.setText(state);
            tvAdStateBadge.setTextColor(Color.parseColor(hexColor));
            tvAdStateBadge.setBackgroundColor(Color.parseColor("#22" + hexColor.substring(1)));
        }
    }

    private void appendLog(String message) {
        String timestamp = timeFormat.format(new Date());
        String logLine = "[" + timestamp + "] " + message;
        Log.d(TAG, logLine);

        runOnUiThread(() -> {
            if (tvConsoleLogs != null) {
                String current = tvConsoleLogs.getText().toString();
                if (current.isEmpty()) {
                    tvConsoleLogs.setText(logLine);
                } else {
                    tvConsoleLogs.setText(current + "\n" + logLine);
                }

                if (consoleScrollView != null) {
                    consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
                }
            }
        });
    }

    private void copyLogsToClipboard() {
        if (tvConsoleLogs != null) {
            String logs = tvConsoleLogs.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Ad Logs", logs);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMetaErrorExplanation(int errorCode) {
        switch (errorCode) {
            case 1000:
                return "NETWORK_ERROR (Check internet connection)";
            case 1001:
                return "NO_FILL (No ad inventory. Ensure Facebook App is logged in on test device)";
            case 1002:
                return "LOAD_TOO_FREQUENTLY (Wait a few seconds between requests)";
            case 1011:
                return "DISPLAY_FORMAT_MISMATCH (Check placement format in Monetization Manager)";
            case 1012:
                return "CLEARTEXT_HTTP_BLOCKED (Secure HTTPS connection required)";
            case 2000:
                return "SERVER_ERROR (Internal Meta server error)";
            case 2001:
                return "INTERNAL_ERROR (Cache or SDK error)";
            default:
                return "UNKNOWN_META_ERROR";
        }
    }

    private String getAdMobErrorExplanation(int errorCode) {
        switch (errorCode) {
            case com.google.android.gms.ads.AdRequest.ERROR_CODE_INTERNAL_ERROR:
                return "INTERNAL_ERROR (Code 0: Internal ad server error)";
            case com.google.android.gms.ads.AdRequest.ERROR_CODE_INVALID_REQUEST:
                return "INVALID_REQUEST (Code 1: Invalid Ad Unit ID or account pending)";
            case com.google.android.gms.ads.AdRequest.ERROR_CODE_NETWORK_ERROR:
                return "NETWORK_ERROR (Code 2: Network connectivity issue)";
            case com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL:
                return "NO_FILL (Code 3: No ad inventory available from Google. Note: HTTP 403 means Google ad servers are still propagating newly created ad units, which takes a few hours to 24-48 hours. Try using the sample test toggle above to verify)";
            default:
                return "UNKNOWN_ADMOB_ERROR";
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
        destroyLevelPlayInterstitial();
        destroyAdMobInterstitial();
        destroyAdMobRewarded();
        destroyAdMobNative();

        destroyMetaInterstitial();
        destroyMetaRewardedAds();
        destroyMetaNativeAd();

        super.onDestroy();
    }
}
