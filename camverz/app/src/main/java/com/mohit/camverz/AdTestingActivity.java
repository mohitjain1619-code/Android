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
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.initialization.AdapterStatus;
import java.util.Map;

// Unity LevelPlay Mediation Imports
import com.unity3d.mediation.LevelPlay;
import com.unity3d.mediation.LevelPlayInitRequest;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdTestingActivity extends AppCompatActivity {
    private static final String TAG = "AdTestingActivity";

    // Meta Configuration
    public static final String META_APP_ID = "1045884118260179";
    public static final String META_PLACEMENT_INTERSTITIAL = "1045884118260179_1063416116506979";
    public static final String META_PLACEMENT_REWARDED_INTERSTITIAL = "1045884118260179_1063416119840312";
    public static final String META_PLACEMENT_NATIVE = "1045884118260179_1063416113173646";

    // Google AdMob Configuration (Live User Units)
    public static final String ADMOB_APP_ID = "ca-app-pub-6992986179206821~2715651414";
    public static final String ADMOB_PLACEMENT_INTERSTITIAL = "ca-app-pub-6992986179206821/9362140189";
    public static final String ADMOB_PLACEMENT_REWARDED = "ca-app-pub-6992986179206821/2063439948";
    public static final String ADMOB_PLACEMENT_NATIVE = "ca-app-pub-6992986179206821/9014655823";

    // Google AdMob Universal Sample Test Units (for instant verification if live units are propagating / 403)
    public static final String ADMOB_SAMPLE_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    public static final String ADMOB_SAMPLE_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    public static final String ADMOB_SAMPLE_NATIVE = "ca-app-pub-3940256099942544/2247696110";

    // LevelPlay Configuration (ironSource Multi-Network Bidding)
    public static final String LEVELPLAY_APP_KEY = "28890f0b5";
    public static final String LEVELPLAY_AD_UNIT_INTERSTITIAL = "bspxbvo7aw4r8ybv";
    public static final String LEVELPLAY_AD_UNIT_REWARDED = "inn2ugrw04ep2tzc";
    public static final String LEVELPLAY_AD_UNIT_NATIVE = "6bpplx6inl1ahz01";

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

    private AdNetwork currentNetwork = AdNetwork.ADMOB;
    private AdType currentType = AdType.INTERSTITIAL;

    // Network Tabs
    private TextView tabNetworkMeta;
    private TextView tabNetworkAdMob;
    private TextView tabNetworkLevelPlay;

    // Format Tabs & Container
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
    private FrameLayout levelplayNativeContainer;

    // Meta Ad Instances
    private com.facebook.ads.InterstitialAd metaInterstitialAd;
    private RewardedInterstitialAd metaRewardedInterstitialAd;
    private RewardedVideoAd metaRewardedVideoAd;
    private NativeAd metaNativeAd;

    // AdMob Ad Instances
    private com.google.android.gms.ads.interstitial.InterstitialAd admobInterstitialAd;
    private RewardedAd admobRewardedAd;
    private com.google.android.gms.ads.nativead.NativeAd admobNativeAd;
    private NativeAdView currentAdMobNativeAdView;

    // LevelPlay Ad Instances
    private LevelPlayInterstitialAd levelPlayInterstitialAd;
    private LevelPlayRewardedAd levelPlayRewardedAd;
    private LevelPlayNativeAd levelPlayNativeAd;

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

        btnLoadAd = findViewById(R.id.btnLoadAd);
        btnShowAd = findViewById(R.id.btnShowAd);
        btnResetAd = findViewById(R.id.btnResetAd);
        tvConsoleLogs = findViewById(R.id.tvConsoleLogs);
        consoleScrollView = findViewById(R.id.consoleScrollView);

        cardNativePreview = findViewById(R.id.cardNativePreview);
        nativeAdContainer = findViewById(R.id.nativeAdContainer);
        admobNativeContainer = findViewById(R.id.admobNativeContainer);
        levelplayNativeContainer = findViewById(R.id.levelplayNativeContainer);

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

        btnLoadAd.setOnClickListener(v -> loadCurrentAd());
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
        String netName;
        if (network == AdNetwork.META) netName = "Meta Audience Network (Direct)";
        else if (network == AdNetwork.ADMOB) netName = "Google AdMob Mediation (Meta Bidding)";
        else netName = "Unity LevelPlay Mediation";
        appendLog("🔄 [NETWORK SWITCH] Selected " + netName);
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

        // 1. Update Network Switcher Tabs
        if (currentNetwork == AdNetwork.META) {
            tabNetworkMeta.setBackgroundResource(R.drawable.bg_button_primary);
            tabNetworkMeta.setTextColor(Color.WHITE);
            tabNetworkAdMob.setBackground(null);
            tabNetworkAdMob.setTextColor(Color.parseColor("#9CA3AF"));
            tabNetworkLevelPlay.setBackground(null);
            tabNetworkLevelPlay.setTextColor(Color.parseColor("#9CA3AF"));

            layoutSampleToggle.setVisibility(View.GONE);
            if (layoutFormatTabs != null) layoutFormatTabs.setVisibility(View.VISIBLE);

            tvAppIdLabel.setText("Meta App ID:");
            tvAppId.setText(META_APP_ID);
            tvPlacementIdLabel.setText("Placement ID:");

            tvWhitelistTitle.setText("META CONSOLE TEST DEVICES");
            tvWhitelistDetails.setText("✓ Registered on Meta Monetization Manager:\n • r: b94e5118-43fc-4881-b07b-0ecf44bbb6ee\n • samsung: 1e278238-70d2-418a-acc3-10ca1ad1abe1");
            tvWhitelistNote.setText("Meta servers deliver test ads automatically to registered test devices.");

        } else if (currentNetwork == AdNetwork.ADMOB) {
            tabNetworkAdMob.setBackgroundResource(R.drawable.bg_button_primary);
            tabNetworkAdMob.setTextColor(Color.WHITE);
            tabNetworkMeta.setBackground(null);
            tabNetworkMeta.setTextColor(Color.parseColor("#9CA3AF"));
            tabNetworkLevelPlay.setBackground(null);
            tabNetworkLevelPlay.setTextColor(Color.parseColor("#9CA3AF"));

            layoutSampleToggle.setVisibility(View.VISIBLE);
            if (layoutFormatTabs != null) layoutFormatTabs.setVisibility(View.VISIBLE);

            tvAppIdLabel.setText("AdMob App ID:");
            tvAppId.setText(ADMOB_APP_ID);
            tvPlacementIdLabel.setText(useSample ? "Sample Test ID:" : "AdMob Unit ID (Meta Mapped):");

            tvWhitelistTitle.setText("GOOGLE ADMOB MEDIATION (META BIDDING)");
            tvWhitelistDetails.setText("✓ AdMob Unit Mapped to Meta Audience Network:\n • Interstitial: " + (useSample ? ADMOB_SAMPLE_INTERSTITIAL : ADMOB_PLACEMENT_INTERSTITIAL) + " ➔ Meta: " + META_PLACEMENT_INTERSTITIAL + "\n • Rewarded: " + (useSample ? ADMOB_SAMPLE_REWARDED : ADMOB_PLACEMENT_REWARDED) + " ➔ Meta: " + META_PLACEMENT_REWARDED_INTERSTITIAL + "\n • Native: " + (useSample ? ADMOB_SAMPLE_NATIVE : ADMOB_PLACEMENT_NATIVE) + " ➔ Meta: " + META_PLACEMENT_NATIVE + "\n • Bidding Adapter: com.google.ads.mediation.facebook.FacebookMediationAdapter");
            tvWhitelistNote.setText("AdMob runs real-time in-app bidding with Meta Audience Network. When loaded, check console below to see which network won!");
        } else {
            // LEVELPLAY MEDIATION
            tabNetworkLevelPlay.setBackgroundResource(R.drawable.bg_button_primary);
            tabNetworkLevelPlay.setTextColor(Color.WHITE);
            tabNetworkMeta.setBackground(null);
            tabNetworkMeta.setTextColor(Color.parseColor("#9CA3AF"));
            tabNetworkAdMob.setBackground(null);
            tabNetworkAdMob.setTextColor(Color.parseColor("#9CA3AF"));

            layoutSampleToggle.setVisibility(View.GONE);
            if (layoutFormatTabs != null) layoutFormatTabs.setVisibility(View.VISIBLE);

            tvAppIdLabel.setText("LevelPlay App Key:");
            tvAppId.setText(LEVELPLAY_APP_KEY);
            tvPlacementIdLabel.setText("Ad Unit ID:");

            tvWhitelistTitle.setText("LEVELPLAY MULTI-BIDDER MEDIATION");
            tvWhitelistDetails.setText("✓ In-App Bidders Active:\n • Meta Audience Network (Bidding)\n • Google AdMob (Bidding)\n • Unity Ads (Bidding)\n • ironSource (Bidding)");
            tvWhitelistNote.setText("Real-time header bidding auction runs client-side between all networks on load request.");
        }

        // 2. Update Format Tabs
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
            if (currentType == AdType.INTERSTITIAL) {
                tabInterstitial.setBackgroundResource(R.drawable.bg_button_primary);
                tabInterstitial.setTextColor(Color.WHITE);

                tvPlacementId.setText(LEVELPLAY_AD_UNIT_INTERSTITIAL);
                tvAdFormat.setText("Interstitial (Multi-Network Bidding)");

                btnLoadAd.setText("Load LevelPlay Interstitial");
                btnShowAd.setText("Show LevelPlay Interstitial");
                btnShowAd.setEnabled(levelPlayInterstitialAd != null && levelPlayInterstitialAd.isAdReady());
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                tabRewarded.setBackgroundResource(R.drawable.bg_button_primary);
                tabRewarded.setTextColor(Color.WHITE);

                tvPlacementId.setText(LEVELPLAY_AD_UNIT_REWARDED);
                tvAdFormat.setText("Rewarded Video (LevelPlay Bidding)");
                layoutRewardStatus.setVisibility(View.VISIBLE);
                resetRewardBadge();

                btnLoadAd.setText("Load LevelPlay Rewarded");
                btnShowAd.setText("Show LevelPlay Rewarded");
                btnShowAd.setEnabled(levelPlayRewardedAd != null && levelPlayRewardedAd.isAdReady());
            } else if (currentType == AdType.NATIVE) {
                tabNative.setBackgroundResource(R.drawable.bg_button_primary);
                tabNative.setTextColor(Color.WHITE);

                tvPlacementId.setText(LEVELPLAY_AD_UNIT_NATIVE);
                tvAdFormat.setText("Native Ad (LevelPlay Mediation)");

                btnLoadAd.setText("Load LevelPlay Native Ad");
                btnShowAd.setVisibility(View.GONE);
            }
        } else if (currentNetwork == AdNetwork.ADMOB) {
            if (currentType == AdType.INTERSTITIAL) {
                tabInterstitial.setBackgroundResource(R.drawable.bg_button_primary);
                tabInterstitial.setTextColor(Color.WHITE);

                tvPlacementId.setText(useSample ? ADMOB_SAMPLE_INTERSTITIAL : ADMOB_PLACEMENT_INTERSTITIAL);
                tvAdFormat.setText("Interstitial (AdMob Fullscreen)");

                btnLoadAd.setText("Load AdMob Interstitial");
                btnShowAd.setText("Show AdMob Interstitial");

            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                tabRewarded.setBackgroundResource(R.drawable.bg_button_primary);
                tabRewarded.setTextColor(Color.WHITE);

                tvPlacementId.setText(useSample ? ADMOB_SAMPLE_REWARDED : ADMOB_PLACEMENT_REWARDED);
                tvAdFormat.setText("Rewarded Video (AdMob)");
                layoutRewardStatus.setVisibility(View.VISIBLE);
                resetRewardBadge();

                btnLoadAd.setText("Load AdMob Rewarded");
                btnShowAd.setText("Show AdMob Rewarded");

            } else if (currentType == AdType.NATIVE) {
                tabNative.setBackgroundResource(R.drawable.bg_button_primary);
                tabNative.setTextColor(Color.WHITE);

                tvPlacementId.setText(useSample ? ADMOB_SAMPLE_NATIVE : ADMOB_PLACEMENT_NATIVE);
                tvAdFormat.setText("Native (AdMob Custom Layout)");

                btnLoadAd.setText("Load AdMob Native Ad");
                btnShowAd.setVisibility(View.GONE);
            }
        } else {
            if (currentType == AdType.INTERSTITIAL) {
                tabInterstitial.setBackgroundResource(R.drawable.bg_button_primary);
                tabInterstitial.setTextColor(Color.WHITE);

                tvPlacementId.setText(META_PLACEMENT_INTERSTITIAL);
                tvAdFormat.setText("Interstitial (Meta Fullscreen)");

                btnLoadAd.setText("Load Meta Interstitial");
                btnShowAd.setText("Show Meta Interstitial");

            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                tabRewarded.setBackgroundResource(R.drawable.bg_button_primary);
                tabRewarded.setTextColor(Color.WHITE);

                tvPlacementId.setText(META_PLACEMENT_REWARDED_INTERSTITIAL);
                tvAdFormat.setText("Rewarded Interstitial (Meta Video + Reward)");
                layoutRewardStatus.setVisibility(View.VISIBLE);
                resetRewardBadge();

                btnLoadAd.setText("Load Rewarded Interstitial");
                btnShowAd.setText("Show Rewarded Interstitial");

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
        // Configure Test Devices in RequestConfiguration
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
                    tvSdkStatus.setText("Initialized (AdMob Mediation ✓)");
                    tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
                }
                appendLog("🟢 [ADMOB INIT] Google Mobile Ads SDK Initialized.");
                Map<String, AdapterStatus> adapterStatusMap = initializationStatus.getAdapterStatusMap();
                if (adapterStatusMap != null) {
                    for (Map.Entry<String, AdapterStatus> entry : adapterStatusMap.entrySet()) {
                        String adapterName = entry.getKey();
                        String state = entry.getValue().getInitializationState().toString();
                        if (adapterName.contains("Facebook") || adapterName.contains("facebook")) {
                            appendLog("   🔗 Meta Bidding Adapter: " + adapterName + " -> " + state);
                        }
                    }
                }
            });
        });
    }

    private void logAdMobMediationInfo(String format, ResponseInfo responseInfo) {
        if (responseInfo == null) {
            appendLog("⚠️ [ADMOB MEDIATION - " + format.toUpperCase(Locale.US) + "] No ResponseInfo returned.");
            return;
        }
        String winningAdapter = responseInfo.getMediationAdapterClassName();
        appendLog("🏆 [ADMOB MEDIATION - " + format.toUpperCase(Locale.US) + "]");
        if (winningAdapter != null && !winningAdapter.isEmpty()) {
            appendLog("   Winning Adapter: " + winningAdapter);
            if (winningAdapter.toLowerCase(Locale.US).contains("facebook")) {
                appendLog("   🎯 Meta Audience Network (Facebook) WON the bidding auction!");
            } else if (winningAdapter.toLowerCase(Locale.US).contains("admob")) {
                appendLog("   🎯 Google AdMob Network served this ad.");
            } else {
                appendLog("   🎯 Mediation Partner: " + winningAdapter);
            }
        } else {
            appendLog("   Winning Adapter: None (No bidder filled)");
        }

        List<AdapterResponseInfo> responses = responseInfo.getAdapterResponses();
        if (responses != null && !responses.isEmpty()) {
            appendLog("📊 [MEDIATION BIDDING RESPONSES (" + responses.size() + ")]: ");
            for (AdapterResponseInfo info : responses) {
                String name = info.getAdapterClassName();
                long latency = info.getLatencyMillis();
                if (info.getAdError() != null) {
                    appendLog("   ✖ " + name + " -> " + info.getAdError().getMessage() + " (Code: " + info.getAdError().getCode() + ")");
                } else {
                    appendLog("   ✔ " + name + " -> Bid Ready (Latency: " + latency + "ms)");
                }
            }
        }
    }

    private void initializeLevelPlaySdk() {
        try {
            // LevelPlay Native Ad requires underlying IronSource engine initialization with NATIVE_AD
            com.ironsource.mediationsdk.IronSource.init(
                this,
                LEVELPLAY_APP_KEY,
                () -> {
                    runOnUiThread(() -> {
                        appendLog("🟢 [IRONSOURCE ENGINE] Initialized with Native, Interstitial & Rewarded.");
                    });
                },
                com.ironsource.mediationsdk.IronSource.AD_UNIT.INTERSTITIAL,
                com.ironsource.mediationsdk.IronSource.AD_UNIT.REWARDED_VIDEO,
                com.ironsource.mediationsdk.IronSource.AD_UNIT.NATIVE_AD
            );

            LevelPlayInitRequest initRequest = new LevelPlayInitRequest.Builder(LEVELPLAY_APP_KEY).build();
            LevelPlay.init(this, initRequest, new LevelPlayInitListener() {
                @Override
                public void onInitSuccess(LevelPlayConfiguration configuration) {
                    runOnUiThread(() -> {
                        if (currentNetwork == AdNetwork.LEVELPLAY) {
                            tvSdkStatus.setText("Initialized (LevelPlay ✓)");
                            tvSdkStatus.setTextColor(Color.parseColor("#34D399"));
                        }
                        appendLog("🟢 [LEVELPLAY INIT] Unity LevelPlay Mediation Initialized (App Key: " + LEVELPLAY_APP_KEY + ")");
                    });
                }

                @Override
                public void onInitFailed(LevelPlayInitError error) {
                    runOnUiThread(() -> {
                        String msg = error != null ? error.getErrorMessage() : "Unknown Error";
                        int code = error != null ? error.getErrorCode() : -1;
                        appendLog("🔴 [LEVELPLAY INIT FAILED] " + msg + " (Code: " + code + ")");
                    });
                }
            });
        } catch (Exception e) {
            appendLog("⚠️ [LEVELPLAY INIT EXCEPTION] " + e.getMessage());
        }
    }

    // =========================================================================
    // DISPATCHER
    // =========================================================================
    private void loadCurrentAd() {
        if (isAdLoading) {
            Toast.makeText(this, "Ad request is already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentNetwork == AdNetwork.LEVELPLAY) {
            if (currentType == AdType.INTERSTITIAL) {
                loadLevelPlayInterstitial();
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                loadLevelPlayRewarded();
            } else if (currentType == AdType.NATIVE) {
                loadLevelPlayNative();
            }
        } else if (currentNetwork == AdNetwork.ADMOB) {
            if (currentType == AdType.INTERSTITIAL) {
                loadAdMobInterstitial();
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                loadAdMobRewarded();
            } else if (currentType == AdType.NATIVE) {
                loadAdMobNative();
            }
        } else {
            if (currentType == AdType.INTERSTITIAL) {
                loadMetaInterstitial();
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                loadMetaRewardedInterstitial();
            } else if (currentType == AdType.NATIVE) {
                loadMetaNativeAd();
            }
        }
    }

    private void showCurrentAd() {
        if (currentNetwork == AdNetwork.LEVELPLAY) {
            if (currentType == AdType.INTERSTITIAL) {
                showLevelPlayInterstitial();
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                showLevelPlayRewarded();
            }
        } else if (currentNetwork == AdNetwork.ADMOB) {
            if (currentType == AdType.INTERSTITIAL) {
                showAdMobInterstitial();
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                showAdMobRewarded();
            }
        } else {
            if (currentType == AdType.INTERSTITIAL) {
                showMetaInterstitial();
            } else if (currentType == AdType.REWARDED_INTERSTITIAL) {
                showMetaRewardedInterstitial();
            }
        }
    }

    private void resetCurrentAd() {
        destroyAdMobInterstitial();
        destroyAdMobRewarded();
        destroyAdMobNative();

        destroyMetaInterstitial();
        destroyMetaRewardedAds();
        destroyMetaNativeAd();

        if (levelPlayInterstitialAd != null) {
            levelPlayInterstitialAd = null;
        }
        if (levelPlayRewardedAd != null) {
            levelPlayRewardedAd = null;
        }
        destroyLevelPlayNative();

        resetRewardBadge();

        isAdLoading = false;
        btnLoadAd.setEnabled(true);
        btnShowAd.setEnabled(false);
        updateAdState("IDLE", "#34D399");
        appendLog("🔄 [RESET] Ad instances cleared and state reset.");
    }

    // =========================================================================
    // 1. GOOGLE ADMOB INTERSTITIAL
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
                            logAdMobMediationInfo("Interstitial", interstitialAd.getResponseInfo());
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
                            logAdMobMediationInfo("Interstitial (Failed)", loadAdError.getResponseInfo());

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
            Log.e(TAG, "Error loading AdMob interstitial", e);
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
                runOnUiThread(() -> {
                    appendLog("🟣 [ADMOB IMPRESSION] Verified impression recorded by Google AdMob servers.");
                });
            }

            @Override
            public void onAdClicked() {
                runOnUiThread(() -> {
                    appendLog("🟡 [ADMOB CLICKED] User clicked on AdMob Interstitial.");
                });
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
    // 2. GOOGLE ADMOB REWARDED
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
                            logAdMobMediationInfo("Rewarded", rewardedAd.getResponseInfo());
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
                            logAdMobMediationInfo("Rewarded (Failed)", loadAdError.getResponseInfo());

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
                runOnUiThread(() -> {
                    appendLog("🟣 [ADMOB IMPRESSION] Rewarded Ad verified impression logged.");
                });
            }

            @Override
            public void onAdClicked() {
                runOnUiThread(() -> {
                    appendLog("🟡 [ADMOB CLICKED] User clicked on Rewarded Ad.");
                });
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
    // 3. GOOGLE ADMOB NATIVE
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
                        logAdMobMediationInfo("Native", nativeAd.getResponseInfo());
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
                            logAdMobMediationInfo("Native (Failed)", loadAdError.getResponseInfo());

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

            if (currentAdMobNativeAdView != null) {
                try {
                    currentAdMobNativeAdView.destroy();
                } catch (Exception ignored) {}
                currentAdMobNativeAdView = null;
            }
            admobNativeContainer.removeAllViews();

            NativeAdView adView = (NativeAdView) LayoutInflater.from(this).inflate(R.layout.layout_admob_native_ad, admobNativeContainer, false);
            currentAdMobNativeAdView = adView;

            // Bind views to NativeAdView
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_app_icon));
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
            adView.setMediaView(adView.findViewById(R.id.ad_media));

            com.google.android.gms.ads.nativead.AdChoicesView adChoicesView = adView.findViewById(R.id.ad_choices_view);
            if (adChoicesView != null) {
                adView.setAdChoicesView(adChoicesView);
            }

            // Populate text & content
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

            // Register native ad with view
            adView.setNativeAd(nativeAd);

            admobNativeContainer.addView(adView);
            appendLog("🖼️ [ADMOB NATIVE RENDERED] Native Ad assets bound and operationalized.");

        } catch (Exception e) {
            appendLog("❌ [ADMOB RENDER EXCEPTION] " + e.getMessage());
        }
    }

    private void destroyAdMobNative() {
        if (currentAdMobNativeAdView != null) {
            try {
                currentAdMobNativeAdView.destroy();
            } catch (Exception ignored) {}
            currentAdMobNativeAdView = null;
        }
        if (admobNativeAd != null) {
            try {
                admobNativeAd.destroy();
            } catch (Exception ignored) {}
            admobNativeAd = null;
        }
        if (admobNativeContainer != null) {
            try {
                for (int i = 0; i < admobNativeContainer.getChildCount(); i++) {
                    View child = admobNativeContainer.getChildAt(i);
                    if (child instanceof NativeAdView) {
                        ((NativeAdView) child).destroy();
                    }
                }
            } catch (Exception ignored) {}
            admobNativeContainer.removeAllViews();
            admobNativeContainer.setVisibility(View.GONE);
        }
        if (cardNativePreview != null && (metaNativeAd == null && levelPlayNativeAd == null)) {
            cardNativePreview.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // 4. META INTERSTITIAL
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
    // 5. META REWARDED INTERSTITIAL
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
            btnShowAd.setEnabled(false);
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
            btnShowAd.setEnabled(false);
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
    // 6. META NATIVE ADS
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
    // 3. UNITY LEVELPLAY INTERSTITIAL (MULTI-NETWORK BIDDING)
    // =========================================================================
    private void loadLevelPlayInterstitial() {
        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("BIDDING AUCTION...", "#FBBF24");

        appendLog("🚀 [LEVELPLAY LOAD] Requesting Interstitial Ad Unit: " + LEVELPLAY_AD_UNIT_INTERSTITIAL);
        appendLog("📡 [AUCTION DISPATCHED] Sending real-time bid requests to Meta, Google AdMob, UnityAds, and ironSource...");

        try {
            if (levelPlayInterstitialAd == null) {
                levelPlayInterstitialAd = new LevelPlayInterstitialAd(LEVELPLAY_AD_UNIT_INTERSTITIAL);
            }

            levelPlayInterstitialAd.setListener(new LevelPlayInterstitialAdListener() {
                @Override
                public void onAdLoaded(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(true);
                        updateAdState("READY (AUCTION WON)", "#34D399");

                        String winnerNetwork = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "Mediation Partner";
                        String auctionId = (adInfo != null && adInfo.getAuctionId() != null) ? adInfo.getAuctionId() : "N/A";
                        appendLog("🟢 [LEVELPLAY LOADED] Interstitial Ad loaded successfully!");
                        appendLog("🏆 [AUCTION WINNER] " + winnerNetwork + " won the real-time bidding auction!");
                        appendLog("ℹ️ [AUCTION DETAILS] Auction ID: " + auctionId);
                        Toast.makeText(AdTestingActivity.this, "LevelPlay Ready! (Won by " + winnerNetwork + ")", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdLoadFailed(LevelPlayAdError error) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(false);
                        updateAdState("NO FILL / FAILED", "#EF4444");

                        String errorMsg = (error != null) ? error.getErrorMessage() : "Unknown Error";
                        int errorCode = (error != null) ? error.getErrorCode() : -1;
                        appendLog("🔴 [LEVELPLAY LOAD FAILED] " + errorMsg + " (Code: " + errorCode + ")");
                        Toast.makeText(AdTestingActivity.this, "LevelPlay failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdDisplayed(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("DISPLAYED", "#818CF8");
                        String network = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "Mediation Partner";
                        appendLog("📺 [LEVELPLAY DISPLAYED] Fullscreen Interstitial shown from: " + network);
                    });
                }

                @Override
                public void onAdDisplayFailed(LevelPlayAdError error, LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("DISPLAY FAILED", "#EF4444");
                        String errorMsg = (error != null) ? error.getErrorMessage() : "Unknown Error";
                        appendLog("❌ [LEVELPLAY DISPLAY FAILED] " + errorMsg);
                        Toast.makeText(AdTestingActivity.this, "Display failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdClicked(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        appendLog("🟡 [LEVELPLAY CLICKED] User clicked the Interstitial ad.");
                    });
                }

                @Override
                public void onAdClosed(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("CLOSED (IDLE)", "#34D399");
                        btnShowAd.setEnabled(false);
                        appendLog("🚪 [LEVELPLAY CLOSED] Interstitial closed by user. Ready to load next auction.");
                    });
                }

                @Override
                public void onAdInfoChanged(LevelPlayAdInfo adInfo) {
                    // Optional info updates
                }
            });

            levelPlayInterstitialAd.loadAd();

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [LEVELPLAY EXCEPTION] " + e.getMessage());
        }
    }

    private void showLevelPlayInterstitial() {
        if (levelPlayInterstitialAd != null && levelPlayInterstitialAd.isAdReady()) {
            appendLog("🚀 [LEVELPLAY SHOW] Displaying Interstitial on screen...");
            levelPlayInterstitialAd.showAd(this);
        } else {
            Toast.makeText(this, "Please load LevelPlay Interstitial first", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ [LEVELPLAY NOT READY] Interstitial is not loaded yet. Tap 'Load' first.");
        }
    }

    // =========================================================================
    // 4. UNITY LEVELPLAY REWARDED VIDEO (MULTI-NETWORK BIDDING)
    // =========================================================================
    private void loadLevelPlayRewarded() {
        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        btnShowAd.setEnabled(false);
        updateAdState("BIDDING AUCTION...", "#FBBF24");

        appendLog("🚀 [LEVELPLAY REWARDED LOAD] Requesting Rewarded Video Unit: " + LEVELPLAY_AD_UNIT_REWARDED);
        appendLog("📡 [AUCTION DISPATCHED] Sending real-time bid requests to Meta, Google AdMob, UnityAds, and ironSource...");

        try {
            if (levelPlayRewardedAd == null) {
                levelPlayRewardedAd = new LevelPlayRewardedAd(LEVELPLAY_AD_UNIT_REWARDED);
            }

            levelPlayRewardedAd.setListener(new LevelPlayRewardedAdListener() {
                @Override
                public void onAdLoaded(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(true);
                        updateAdState("READY (AUCTION WON)", "#34D399");

                        String winnerNetwork = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "Mediation Partner";
                        String auctionId = (adInfo != null && adInfo.getAuctionId() != null) ? adInfo.getAuctionId() : "N/A";
                        appendLog("🟢 [LEVELPLAY REWARDED LOADED] Rewarded Video ready to show!");
                        appendLog("🏆 [AUCTION WINNER] " + winnerNetwork + " won the real-time bidding auction!");
                        appendLog("ℹ️ [AUCTION DETAILS] Auction ID: " + auctionId);
                        Toast.makeText(AdTestingActivity.this, "LevelPlay Rewarded Ready! (" + winnerNetwork + ")", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdLoadFailed(LevelPlayAdError error) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        btnShowAd.setEnabled(false);
                        updateAdState("NO FILL / FAILED", "#EF4444");

                        String errorMsg = (error != null) ? error.getErrorMessage() : "Unknown Error";
                        int errorCode = (error != null) ? error.getErrorCode() : -1;
                        appendLog("🔴 [LEVELPLAY REWARDED FAILED] " + errorMsg + " (Code: " + errorCode + ")");
                        Toast.makeText(AdTestingActivity.this, "LevelPlay Rewarded failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdDisplayed(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("PLAYING VIDEO", "#818CF8");
                        String network = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "Mediation Partner";
                        appendLog("📺 [LEVELPLAY REWARDED DISPLAYED] Fullscreen video playing from: " + network);
                    });
                }

                @Override
                public void onAdDisplayFailed(LevelPlayAdError error, LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("DISPLAY FAILED", "#EF4444");
                        String errorMsg = (error != null) ? error.getErrorMessage() : "Unknown Error";
                        appendLog("❌ [LEVELPLAY REWARDED DISPLAY FAILED] " + errorMsg);
                        Toast.makeText(AdTestingActivity.this, "Display failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdClicked(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        appendLog("🟡 [LEVELPLAY REWARDED CLICKED] User clicked the rewarded ad.");
                    });
                }

                @Override
                public void onAdRewarded(LevelPlayReward reward, LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        isRewardEarned = true;
                        if (tvRewardBadge != null) {
                            tvRewardBadge.setText("REWARD EARNED ✓");
                            tvRewardBadge.setTextColor(Color.parseColor("#34D399"));
                            tvRewardBadge.setBackgroundColor(Color.parseColor("#2634D399"));
                        }
                        String name = (reward != null && reward.getName() != null) ? reward.getName() : "Reward";
                        int amount = (reward != null) ? reward.getAmount() : 1;
                        appendLog("🎁 [LEVELPLAY REWARD EARNED] User watched full video! Reward: " + amount + " " + name);
                        Toast.makeText(AdTestingActivity.this, "🎁 Reward Earned! (" + amount + " " + name + ")", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdClosed(LevelPlayAdInfo adInfo) {
                    runOnUiThread(() -> {
                        updateAdState("CLOSED (IDLE)", "#34D399");
                        btnShowAd.setEnabled(false);
                        appendLog("🚪 [LEVELPLAY REWARDED CLOSED] Video ad closed. Reward granted: " + isRewardEarned);
                    });
                }

                @Override
                public void onAdInfoChanged(LevelPlayAdInfo adInfo) {
                }
            });

            levelPlayRewardedAd.loadAd();

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [LEVELPLAY REWARDED EXCEPTION] " + e.getMessage());
        }
    }

    private void showLevelPlayRewarded() {
        if (levelPlayRewardedAd != null && levelPlayRewardedAd.isAdReady()) {
            appendLog("🚀 [LEVELPLAY SHOW REWARDED] Playing Rewarded Video...");
            levelPlayRewardedAd.showAd(this);
        } else {
            Toast.makeText(this, "Please load LevelPlay Rewarded Video first", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ [LEVELPLAY NOT READY] Rewarded Video is not loaded yet.");
        }
    }

    // =========================================================================
    // 5. UNITY LEVELPLAY NATIVE AD (MULTI-NETWORK MEDIATION)
    // =========================================================================
    private void loadLevelPlayNative() {
        destroyLevelPlayNative();

        isAdLoading = true;
        btnLoadAd.setEnabled(false);
        updateAdState("BIDDING AUCTION...", "#FBBF24");

        appendLog("🚀 [LEVELPLAY NATIVE LOAD] Requesting Native Ad Unit: " + LEVELPLAY_AD_UNIT_NATIVE);
        appendLog("📡 [AUCTION DISPATCHED] Sending real-time Native auction requests...");

        try {
            LevelPlayNativeAd.Builder builder = new LevelPlayNativeAd.Builder()
                    .withActivity(this)
                    .withPlacementName(LEVELPLAY_AD_UNIT_NATIVE);

            builder.withListener(new LevelPlayNativeAdListener() {
                @Override
                public void onAdLoaded(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        levelPlayNativeAd = nativeAd;
                        btnLoadAd.setEnabled(true);
                        updateAdState("READY & RENDERED", "#34D399");

                        String winner = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "Mediation Partner";
                        appendLog("🟢 [LEVELPLAY NATIVE LOADED] Native Ad loaded successfully!");
                        appendLog("🏆 [AUCTION WINNER] " + winner + " won the Native bidding auction!");

                        renderLevelPlayNativeAd(nativeAd);
                    });
                }

                @Override
                public void onAdLoadFailed(LevelPlayNativeAd nativeAd, IronSourceError error) {
                    runOnUiThread(() -> {
                        isAdLoading = false;
                        btnLoadAd.setEnabled(true);
                        updateAdState("NO FILL / FAILED", "#EF4444");

                        String msg = error != null ? error.getErrorMessage() : "Unknown Error";
                        int code = error != null ? error.getErrorCode() : -1;
                        appendLog("🔴 [LEVELPLAY NATIVE FAILED] " + msg + " (Code: " + code + ")");
                        Toast.makeText(AdTestingActivity.this, "LevelPlay Native failed: " + msg, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAdImpression(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> {
                        appendLog("👁️ [LEVELPLAY NATIVE IMPRESSION] Impression logged on screen.");
                    });
                }

                @Override
                public void onAdClicked(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> {
                        appendLog("🟡 [LEVELPLAY NATIVE CLICKED] User clicked the native ad.");
                    });
                }
            });

            levelPlayNativeAd = builder.build();
            levelPlayNativeAd.loadAd();

        } catch (Exception e) {
            isAdLoading = false;
            btnLoadAd.setEnabled(true);
            updateAdState("EXCEPTION", "#EF4444");
            appendLog("❌ [LEVELPLAY NATIVE EXCEPTION] " + e.getMessage());
        }
    }

    private void renderLevelPlayNativeAd(LevelPlayNativeAd nativeAd) {
        if (nativeAd == null || levelplayNativeContainer == null) return;

        try {
            levelplayNativeContainer.removeAllViews();
            com.ironsource.mediationsdk.ads.nativead.NativeAdLayout nativeAdLayout = 
                    (com.ironsource.mediationsdk.ads.nativead.NativeAdLayout) LayoutInflater.from(this).inflate(R.layout.layout_levelplay_native_ad, levelplayNativeContainer, false);

            TextView tvTitle = nativeAdLayout.findViewById(R.id.lp_native_title);
            TextView tvAdvertiser = nativeAdLayout.findViewById(R.id.lp_native_advertiser);
            TextView tvBody = nativeAdLayout.findViewById(R.id.lp_native_body);
            ImageView ivIcon = nativeAdLayout.findViewById(R.id.lp_native_icon);
            Button btnCta = nativeAdLayout.findViewById(R.id.lp_native_cta);
            LevelPlayMediaView mediaView = nativeAdLayout.findViewById(R.id.lp_native_media_view);

            if (tvTitle != null && nativeAd.getTitle() != null) {
                tvTitle.setText(nativeAd.getTitle());
            }
            if (tvAdvertiser != null && nativeAd.getAdvertiser() != null) {
                tvAdvertiser.setText(nativeAd.getAdvertiser());
            }
            if (tvBody != null && nativeAd.getBody() != null) {
                tvBody.setText(nativeAd.getBody());
            }
            if (btnCta != null && nativeAd.getCallToAction() != null) {
                btnCta.setText(nativeAd.getCallToAction());
            }
            if (ivIcon != null && nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                ivIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
            }

            // Register asset views with NativeAdLayout
            if (tvTitle != null) nativeAdLayout.setTitleView(tvTitle);
            if (tvAdvertiser != null) nativeAdLayout.setAdvertiserView(tvAdvertiser);
            if (tvBody != null) nativeAdLayout.setBodyView(tvBody);
            if (ivIcon != null) nativeAdLayout.setIconView(ivIcon);
            if (btnCta != null) nativeAdLayout.setCallToActionView(btnCta);
            if (mediaView != null) nativeAdLayout.setMediaView(mediaView);

            // LevelPlay handles wrapping inside AdMob/Meta native ad views internally
            nativeAdLayout.registerNativeAdViews(nativeAd);

            levelplayNativeContainer.addView(nativeAdLayout);
            levelplayNativeContainer.setVisibility(View.VISIBLE);

            if (nativeAdContainer != null) nativeAdContainer.setVisibility(View.GONE);
            if (admobNativeContainer != null) admobNativeContainer.setVisibility(View.GONE);
            if (cardNativePreview != null) cardNativePreview.setVisibility(View.VISIBLE);

            appendLog("🖼️ [NATIVE RENDERED] LevelPlay Native Ad views bound & rendered successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Error rendering LevelPlay native ad", e);
            appendLog("❌ [NATIVE RENDER ERROR] " + e.getMessage());
        }
    }

    private void destroyLevelPlayNative() {
        if (levelPlayNativeAd != null) {
            try {
                levelPlayNativeAd.destroyAd();
            } catch (Exception ignored) {}
            levelPlayNativeAd = null;
        }
        if (levelplayNativeContainer != null) {
            levelplayNativeContainer.removeAllViews();
            levelplayNativeContainer.setVisibility(View.GONE);
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
    protected void onPause() {
        if (isFinishing()) {
            destroyAdMobNative();
            destroyLevelPlayNative();
            destroyMetaNativeAd();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        destroyAdMobNative();
        destroyAdMobInterstitial();
        destroyAdMobRewarded();

        destroyMetaNativeAd();
        destroyMetaInterstitial();
        destroyMetaRewardedAds();

        destroyLevelPlayNative();
        if (levelPlayInterstitialAd != null) {
            levelPlayInterstitialAd = null;
        }
        if (levelPlayRewardedAd != null) {
            levelPlayRewardedAd = null;
        }

        super.onDestroy();
    }
}
