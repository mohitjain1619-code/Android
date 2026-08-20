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

    private long searchStartTime = 0;
    private static final long MIN_SEARCH_DURATION_MS = 0; // Delay disabled (ads removed)

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
        // Ads disabled in main app
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
        // Ads disabled in main app
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

        // Clean up AdMob ads
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        if (metaNativeAd != null) {
            metaNativeAd.destroy();
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
