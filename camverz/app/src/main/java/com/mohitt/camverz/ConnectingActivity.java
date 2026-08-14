package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mohitt.camverz.api.TokenManager;
import org.json.JSONObject;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.util.ArrayList;
import java.util.List;

// Import Google AdMob classes
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import androidx.annotation.NonNull;

// Import Meta Audience Network classes
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.MediaView;

public class ConnectingActivity extends BaseActivity {

    private static final String TAG = "ConnectingActivity";
    private Socket socket;
    private Emitter.Listener matchFoundListener;
    private Emitter.Listener limitExceededListener;
    private TokenManager tokenManager;

    private String category = "";
    private String userGender = "";
    private String myUid = "";

    private boolean isWaiting = false;
    private boolean matchAccepted = false;

    // Google AdMob Mediated Native Ad & Delay
    private NativeAd nativeAd;
    private com.facebook.ads.NativeAd metaNativeAd;
    private RewardedInterstitialAd rewardedInterstitialAd;

    // InMobi Interstitial Ad & Timer
    private com.inmobi.ads.InMobiInterstitial inmobiInterstitialAd;
    private Handler inmobiHandler;
    private Runnable inmobiRunnable;
    private long searchStartTime = 0;
    private static final long MIN_SEARCH_DURATION_MS = 5000; // Enforce minimum 5 seconds search duration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting);

        tokenManager = TokenManager.getInstance(this);
        socket = SocketManager.getInstance();

        myUid = tokenManager.getUserId();
        category = getIntent().getStringExtra("category");
        if (category == null) category = "straight";

        TextView categoryText = findViewById(R.id.categoryText);
        categoryText.setText("Category: " + category.substring(0, 1).toUpperCase() + category.substring(1));

        // Start search timer
        searchStartTime = System.currentTimeMillis();

        // Load AdMob mediated native ads and rewarded interstitial ads after initialization
        com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {
            runOnUiThread(() -> {
                loadNativeAd();
                loadRewardedInterstitialAd();
            });
        });

        // Load Meta Native Ad immediately without waiting for AdMob SDK initialization
        loadMetaNativeAd();

        checkAndRequestLocationPermission();
        setupSocketListeners();
        loadUserDataAndJoinQueue();

        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            leaveQueue();
            finish();
        });

        // Initialize and start InMobi 2-minute recurring timer
        inmobiHandler = new Handler(Looper.getMainLooper());
        inmobiRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                loadInMobiInterstitial();
                // Repeat every 2 minutes (120,000 milliseconds)
                inmobiHandler.postDelayed(this, 120000);
            }
        };
        // Schedule first load after 5 seconds for quick local testing, then repeat every 2 minutes
        inmobiHandler.postDelayed(inmobiRunnable, 5000);
    }

    @Override
    public void onBackPressed() {
        leaveQueue();
        finish();
    }

    private void loadUserDataAndJoinQueue() {
        userGender = tokenManager.getUserGender();
        if (userGender == null || userGender.isEmpty()) {
            userGender = "unknown";
        }
        joinQueue();
    }

    private void joinQueue() {
        if (isWaiting) return;
        isWaiting = true;

        try {
            JSONObject obj = new JSONObject();
            obj.put("uid", myUid);
            obj.put("gender", userGender);
            obj.put("category", category);

            Log.d(TAG, "📤 join-queue");
            socket.emit("join-queue", obj);
        } catch (Exception e) {
            Log.e(TAG, "join-queue failed: " + e.getMessage());
        }
    }

    private void leaveQueue() {
        isWaiting = false;
        try {
            JSONObject obj = new JSONObject();
            obj.put("uid", myUid);

            Log.d(TAG, "📤 leave-queue");
            socket.emit("leave-queue", obj);
        } catch (Exception e) {
            Log.e(TAG, "leave-queue failed: " + e.getMessage());
        }
    }

    private void setupSocketListeners() {
        removeSocketListeners();

        limitExceededListener = args -> {
            Log.w(TAG, "⚠️ Received limit-exceeded socket event.");
            runOnUiThread(this::showLimitExceededDialog);
        };
        socket.on("limit-exceeded", limitExceededListener);

        matchFoundListener = args -> {
            Log.d(TAG, "📥 match-found socket event received. isWaiting=" + isWaiting);
            if (!isWaiting) return;

            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");
                Log.d(TAG, "📥 match-found details -> peerId: " + peerId + ", myUid: " + myUid);

                if (peerId.equals(myUid)) return;

                runOnUiThread(() -> {
                    isWaiting = false;
                    removeMatchFoundListener();

                    // Calculate elapsed search duration
                    long elapsed = System.currentTimeMillis() - searchStartTime;
                    Log.d(TAG, "⏱️ elapsed matchmaking time: " + elapsed + "ms, searchStartTime: " + searchStartTime);
                    if (elapsed < MIN_SEARCH_DURATION_MS) {
                        long remainingDelay = MIN_SEARCH_DURATION_MS - elapsed;
                        Log.d(TAG, "⏳ Delaying match transition by " + remainingDelay + "ms to show Native ad");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            transitionToCall(peerId);
                        }, remainingDelay);
                    } else {
                        Log.d(TAG, "⚡ Elapsed time is already >= 5s. Transitioning immediately.");
                        transitionToCall(peerId);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "match-found error: " + e.getMessage());
            }
        };
        socket.on("match-found", matchFoundListener);
    }

    private void transitionToCall(String peerId) {
        Log.d(TAG, "🚀 transitionToCall starting CallActivity with peerId: " + peerId);
        matchAccepted = true;
        Intent i = new Intent(ConnectingActivity.this, CallActivity.class);
        i.putExtra("peer", peerId);
        i.putExtra("category", category);
        startActivity(i);
        finish();
    }

    // --- Google AdMob Mediated Native Ad Integration ---

    private void loadNativeAd() {
        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.admob_native_ad_unit_id))
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd ad) {
                        if (isFinishing() || isDestroyed()) {
                            ad.destroy();
                            return;
                        }
                        if (nativeAd != null) {
                            nativeAd.destroy();
                        }
                        nativeAd = ad;
                        inflateAdMobNativeAd(ad);
                    }
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e(TAG, "AdMob Native ad failed to load: " + adError.toString());
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void inflateAdMobNativeAd(NativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.nativeAdContainer);
        if (adContainer == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        // Create the NativeAdView container
        NativeAdView adView = new NativeAdView(this);
        adView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // Build native ad components inside a LinearLayout dynamically
        LinearLayout innerContainer = new LinearLayout(this);
        innerContainer.setOrientation(LinearLayout.VERTICAL);
        innerContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        innerContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        // Headline
        TextView adHeadline = new TextView(this);
        adHeadline.setText(ad.getHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(adHeadline);
        adView.setHeadlineView(adHeadline);

        // Body
        TextView adBody = new TextView(this);
        adBody.setText(ad.getBody());
        adBody.setTextColor(getResources().getColor(R.color.text_secondary));
        adBody.setTextSize(12);
        adBody.setPadding(0, dpToPx(4), 0, dpToPx(8));
        innerContainer.addView(adBody);
        adView.setBodyView(adBody);

        // Call to action button
        Button callToAction = new Button(this);
        callToAction.setText(ad.getCallToAction());
        callToAction.setBackgroundResource(R.drawable.bg_glass_card_premium);
        callToAction.setTextColor(getResources().getColor(R.color.accent_primary));
        callToAction.setTextSize(14);
        callToAction.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(callToAction);
        adView.setCallToActionView(callToAction);

        // Add elements to NativeAdView wrapper
        adView.addView(innerContainer);

        // Register native ad object with NativeAdView
        adView.setNativeAd(ad);

        adContainer.addView(adView);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // --- Meta Audience Network Direct Native Ad Integration ---

    private void loadMetaNativeAd() {
        // Enable test ads for development builds only
        if (com.mohitt.camverz.BuildConfig.DEBUG) {
            com.facebook.ads.AdSettings.setTestMode(true);
        }
        metaNativeAd = new com.facebook.ads.NativeAd(this, "1679167109809598_1679167733142869");
        com.facebook.ads.NativeAdListener nativeAdListener = new com.facebook.ads.NativeAdListener() {
            @Override
            public void onMediaDownloaded(com.facebook.ads.Ad ad) {
                Log.d(TAG, "Meta Media Downloaded");
            }

            @Override
            public void onError(com.facebook.ads.Ad ad, com.facebook.ads.AdError adError) {
                Log.e(TAG, "Meta Native Ad failed to load: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(com.facebook.ads.Ad ad) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (metaNativeAd != null && metaNativeAd == ad) {
                    inflateMetaNativeAd(metaNativeAd);
                }
            }

            @Override
            public void onAdClicked(com.facebook.ads.Ad ad) {
                Log.d(TAG, "Meta Ad Clicked");
            }

            @Override
            public void onLoggingImpression(com.facebook.ads.Ad ad) {
                Log.d(TAG, "Meta Logging Impression");
            }
        };

        metaNativeAd.loadAd(metaNativeAd.buildLoadAdConfig()
                .withAdListener(nativeAdListener)
                .build());
    }

    private void inflateMetaNativeAd(com.facebook.ads.NativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.metaNativeAdContainer);
        if (adContainer == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        // Main native ad container from Meta
        com.facebook.ads.NativeAdLayout adLayout = new com.facebook.ads.NativeAdLayout(this);
        adLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // Build native ad components inside a LinearLayout dynamically
        LinearLayout innerContainer = new LinearLayout(this);
        innerContainer.setOrientation(LinearLayout.VERTICAL);
        innerContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        innerContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        // Headline (Title)
        TextView adHeadline = new TextView(this);
        adHeadline.setText(ad.getAdHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(adHeadline);

        // Body (Description)
        TextView adBody = new TextView(this);
        adBody.setText(ad.getAdBodyText());
        adBody.setTextColor(getResources().getColor(R.color.text_secondary));
        adBody.setTextSize(12);
        adBody.setPadding(0, dpToPx(4), 0, dpToPx(8));
        innerContainer.addView(adBody);

        // Call to action button
        Button callToAction = new Button(this);
        callToAction.setText(ad.getAdCallToAction());
        callToAction.setBackgroundResource(R.drawable.bg_glass_card_premium);
        callToAction.setTextColor(getResources().getColor(R.color.accent_primary));
        callToAction.setTextSize(14);
        callToAction.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(callToAction);

        adLayout.addView(innerContainer);

        // Register views for interaction (clicks)
        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(adHeadline);
        clickableViews.add(callToAction);
        
        MediaView mediaView = new MediaView(this);
        ad.registerViewForInteraction(adLayout, mediaView, clickableViews);

        adContainer.addView(adLayout);
    }

    private void loadRewardedInterstitialAd() {
        RewardedInterstitialAd.load(this, getString(R.string.admob_rewarded_interstitial_ad_unit_id),
                new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        Log.d(TAG, "✅ Rewarded Interstitial Ad loaded.");

                        // Setup Server-Side Verification (SSV) options using our user ID
                        com.google.android.gms.ads.rewarded.ServerSideVerificationOptions options =
                                new com.google.android.gms.ads.rewarded.ServerSideVerificationOptions.Builder()
                                        .setUserId(myUid)
                                        .setCustomData(myUid)
                                        .build();
                        rewardedInterstitialAd.setServerSideVerificationOptions(options);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "❌ Rewarded Interstitial Ad failed to load: " + loadAdError.getMessage());
                        rewardedInterstitialAd = null;
                    }
                });
    }

    private void showRewardedAd() {
        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd.show(this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                    Log.d(TAG, "🎉 User earned reward from video ad!");
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(ConnectingActivity.this, "Extra call credited! Resuming matchmaking...", android.widget.Toast.LENGTH_SHORT).show();
                        // Reset start time and rejoin queue
                        searchStartTime = System.currentTimeMillis();
                        setupSocketListeners();
                        joinQueue();
                    });
                }
            });
        } else {
            android.widget.Toast.makeText(this, "Ad is not ready yet. Please try again in a moment.", android.widget.Toast.LENGTH_SHORT).show();
            loadRewardedInterstitialAd();
        }
    }

    private void showLimitExceededDialog() {
        if (isFinishing() || isDestroyed()) return;

        // Stop waiting state
        isWaiting = false;
        removeSocketListeners();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Daily Limit Reached")
                .setMessage("You have reached your daily free video call limit.\n\nWatch a quick video ad to get another free call instantly, or upgrade to premium for unlimited calls!")
                .setCancelable(false)
                .setPositiveButton("Watch Video Ad", (dialog, which) -> {
                    showRewardedAd();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    leaveQueue();
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!matchAccepted) {
            leaveQueue();
        }
        removeSocketListeners();

        // Clean up InMobi timer
        if (inmobiHandler != null && inmobiRunnable != null) {
            inmobiHandler.removeCallbacks(inmobiRunnable);
        }

        // Clean up AdMob ads
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        if (metaNativeAd != null) {
            metaNativeAd.destroy();
        }
    }

    private void loadInMobiInterstitial() {
        if (isFinishing() || isDestroyed()) return;
        Log.d(TAG, "InMobi - Starting direct interstitial load...");

        inmobiInterstitialAd = new com.inmobi.ads.InMobiInterstitial(
            this,
            10000770044L,
            new com.inmobi.ads.listeners.InterstitialAdEventListener() {
                @Override
                public void onAdLoadSucceeded(@NonNull com.inmobi.ads.InMobiInterstitial ad, @NonNull com.inmobi.ads.AdMetaInfo adMetaInfo) {
                    Log.d(TAG, "InMobi - Interstitial Ad load succeeded.");
                    if (!isFinishing() && !isDestroyed() && ad.isReady()) {
                        runOnUiThread(() -> {
                            Log.d(TAG, "InMobi - Showing Interstitial Ad.");
                            ad.show();
                        });
                    }
                }

                @Override
                public void onAdLoadFailed(@NonNull com.inmobi.ads.InMobiInterstitial ad, @NonNull com.inmobi.ads.InMobiAdRequestStatus status) {
                    Log.w(TAG, "InMobi - Interstitial Ad failed to load: " + status.getMessage() + ", code: " + status.getStatusCode());
                }

                @Override
                public void onAdClicked(@NonNull com.inmobi.ads.InMobiInterstitial ad, @NonNull java.util.Map<Object, Object> map) {
                    Log.d(TAG, "InMobi - Interstitial Ad clicked.");
                }

                @Override
                public void onAdDismissed(@NonNull com.inmobi.ads.InMobiInterstitial ad) {
                    Log.d(TAG, "InMobi - Interstitial Ad dismissed.");
                }

                @Override
                public void onAdDisplayed(@NonNull com.inmobi.ads.InMobiInterstitial ad, @NonNull com.inmobi.ads.AdMetaInfo adMetaInfo) {
                    Log.d(TAG, "InMobi - Interstitial Ad displayed.");
                }

                @Override
                public void onAdDisplayFailed(@NonNull com.inmobi.ads.InMobiInterstitial ad) {
                    Log.e(TAG, "InMobi - Interstitial Ad display failed.");
                }

                @Override
                public void onUserLeftApplication(@NonNull com.inmobi.ads.InMobiInterstitial ad) {
                    Log.d(TAG, "InMobi - User left application.");
                }
            }
        );

        inmobiInterstitialAd.load();
    }

    private void removeMatchFoundListener() {
        if (socket != null && matchFoundListener != null) {
            socket.off("match-found", matchFoundListener);
            matchFoundListener = null;
        }
    }

    private void checkAndRequestLocationPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1001);
        }
    }

    private void removeSocketListeners() {
        if (socket == null) return;
        if (matchFoundListener != null) {
            socket.off("match-found", matchFoundListener);
            matchFoundListener = null;
        }
        if (limitExceededListener != null) {
            socket.off("limit-exceeded", limitExceededListener);
            limitExceededListener = null;
        }
    }
}
