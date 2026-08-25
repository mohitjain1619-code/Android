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

    // ironSource LevelPlay Native Ad
    private com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd levelPlayNativeAd;
    private com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd rewardedInterstitialAd;

    private long searchStartTime = 0;
    private static final long MIN_SEARCH_DURATION_MS = 5000; // 5 seconds minimum to show native ad

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

        // Load native ads only for non ad-free users
        if (!tokenManager.isVideoCallAdFree()) {
            loadLevelPlayNativeAd();
        }

        checkAndRequestLocationPermission();
        setupSocketListeners();
        loadUserDataAndJoinQueue();

        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            leaveQueue();
            finish();
        });

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

    // --- ironSource LevelPlay Native Ad Integration ---

    private void loadLevelPlayNativeAd() {
        if (isFinishing() || isDestroyed()) return;

        levelPlayNativeAd = new com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd.Builder()
                .withAdUnitId("16c31bfd5") // ironSource App Key / Native Placement ID
                .withListener(new com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener() {
                    @Override
                    public void onAdLoaded(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {
                        Log.d(TAG, "LevelPlay Native Ad loaded");
                        runOnUiThread(() -> inflateLevelPlayNativeAd(ad));
                    }

                    @Override
                    public void onAdLoadFailed(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.logger.IronSourceError error) {
                        Log.w(TAG, "LevelPlay Native Ad load failed: " + error.getErrorMessage());
                    }

                    @Override
                    public void onAdClicked(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}

                    @Override
                    public void onAdOpened(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}

                    @Override
                    public void onAdClosed(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad, com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo adInfo) {}
                })
                .build();
        levelPlayNativeAd.loadAd();
    }

    private void inflateLevelPlayNativeAd(com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd ad) {
        FrameLayout adContainer = findViewById(R.id.nativeAdContainer);
        if (adContainer == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(View.VISIBLE);

        View adView = LayoutInflater.from(this).inflate(R.layout.layout_native_ad_levelplay, null);

        ImageView adIcon = adView.findViewById(R.id.ad_icon);
        TextView adTitle = adView.findViewById(R.id.ad_title);
        TextView adAdvertiser = adView.findViewById(R.id.ad_advertiser);
        TextView adBody = adView.findViewById(R.id.ad_body);
        com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView mediaView = adView.findViewById(R.id.ad_media);
        Button adCta = adView.findViewById(R.id.ad_cta);

        if (ad.getIcon() != null && adIcon != null) {
            adIcon.setImageDrawable(ad.getIcon());
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

        java.util.List<View> clickableViews = new java.util.ArrayList<>();
        if (adTitle != null) clickableViews.add(adTitle);
        if (adCta != null) clickableViews.add(adCta);

        ad.registerView(adView, mediaView, adIcon, clickableViews);

        adContainer.addView(adView);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }



    private void loadRewardedInterstitialAd() {
        // Ads disabled in main app
    }

    private void showRewardedAd() {
        Log.d(TAG, "🎉 User earned reward directly (ads disabled)!");
        runOnUiThread(() -> {
            android.widget.Toast.makeText(ConnectingActivity.this, "Extra call credited! Resuming matchmaking...", android.widget.Toast.LENGTH_SHORT).show();
            // Reset start time and rejoin queue
            searchStartTime = System.currentTimeMillis();
            setupSocketListeners();
            joinQueue();
        });
    }

    private void showLimitExceededDialog() {
        if (isFinishing() || isDestroyed()) return;

        // Stop waiting state
        isWaiting = false;
        removeSocketListeners();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Daily Limit Reached")
                .setMessage("You have reached your daily free video call limit.\n\nSupport Camverz! Rate us 5 Stars on Play Store to get another free call instantly, or upgrade to premium for unlimited calls!")
                .setCancelable(false)
                .setPositiveButton("Rate 5 Stars ⭐", (dialog, which) -> {
                    redirectToPlayStoreAndReward();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    leaveQueue();
                    finish();
                })
                .show();
    }

    private void redirectToPlayStoreAndReward() {
        try {
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + getPackageName()));
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(playStoreIntent);
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
        showRewardedAd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!matchAccepted) {
            leaveQueue();
        }
        removeSocketListeners();

        // Clean up ironSource ad objects
        if (levelPlayNativeAd != null) {
            levelPlayNativeAd.destroy();
        }
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

    @Override
    protected void onStop() {
        super.onStop();
        if (!matchAccepted && isWaiting) {
            leaveQueue();
        }
    }
}
