package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mohitt.camverz.api.TokenManager;
import org.json.JSONObject;
import io.socket.client.Socket;
import java.util.ArrayList;
import java.util.List;

// Import Facebook Audience Network SDK classes
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.MediaView;

public class ConnectingActivity extends BaseActivity {

    private static final String TAG = "ConnectingActivity";
    private Socket socket;
    private TokenManager tokenManager;

    private String category = "";
    private String userGender = "";
    private String myUid = "";

    private boolean isWaiting = false;
    private boolean matchAccepted = false;

    // Meta Audience Network Ads & Delays
    private NativeAd nativeAd;
    private InterstitialAd interstitialAd;
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

        // Load Meta Ads
        loadNativeAd();
        loadInterstitialAd();

        setupSocketListeners();
        loadUserDataAndJoinQueue();

        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            leaveQueue();
            showInterstitialAndFinish();
        });
    }

    @Override
    public void onBackPressed() {
        leaveQueue();
        showInterstitialAndFinish();
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
        socket.off("match-found");

        socket.on("match-found", args -> {
            if (!isWaiting) return;

            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");

                if (peerId.equals(myUid)) return;

                runOnUiThread(() -> {
                    isWaiting = false;
                    socket.off("match-found");

                    // Calculate elapsed search duration
                    long elapsed = System.currentTimeMillis() - searchStartTime;
                    if (elapsed < MIN_SEARCH_DURATION_MS) {
                        long remainingDelay = MIN_SEARCH_DURATION_MS - elapsed;
                        Log.d(TAG, "⏳ Delaying match transition by " + remainingDelay + "ms to show Native ad");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            transitionToCall(peerId);
                        }, remainingDelay);
                    } else {
                        transitionToCall(peerId);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "match-found error: " + e.getMessage());
            }
        });
    }

    private void transitionToCall(String peerId) {
        matchAccepted = true;
        Intent i = new Intent(ConnectingActivity.this, CallActivity.class);
        i.putExtra("peer", peerId);
        i.putExtra("category", category);
        startActivity(i);
        finish();
    }

    // --- Meta Ads Integration ---

    private void loadNativeAd() {
        nativeAd = new NativeAd(this, "1679167109809598_1679167733142869");
        NativeAdListener nativeAdListener = new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {
                Log.d(TAG, "Meta Native ad media downloaded.");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                Log.e(TAG, "Meta Native ad error: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                Log.d(TAG, "Meta Native ad loaded.");
                if (nativeAd == null || nativeAd != ad) {
                    return;
                }
                inflateAd(nativeAd);
            }

            @Override
            public void onAdClicked(Ad ad) {
                Log.d(TAG, "Meta Native ad clicked.");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "Meta Native ad impression logged.");
            }
        };

        nativeAd.loadAd(
                nativeAd.buildLoadAdConfig()
                        .withAdListener(nativeAdListener)
                        .build());
    }

    private void inflateAd(NativeAd nativeAd) {
        nativeAd.unregisterView();

        NativeAdLayout nativeAdLayout = findViewById(R.id.nativeAdLayout);
        if (nativeAdLayout == null) return;
        nativeAdLayout.removeAllViews();
        nativeAdLayout.setVisibility(View.VISIBLE);

        // Build native ad container dynamically
        LinearLayout adContainer = new LinearLayout(this);
        adContainer.setOrientation(LinearLayout.VERTICAL);
        adContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        adContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        // Headline
        TextView adHeadline = new TextView(this);
        adHeadline.setText(nativeAd.getAdHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        adContainer.addView(adHeadline);

        // Body
        TextView adBody = new TextView(this);
        adBody.setText(nativeAd.getAdBodyText());
        adBody.setTextColor(getResources().getColor(R.color.text_secondary));
        adBody.setTextSize(12);
        adBody.setPadding(0, dpToPx(4), 0, dpToPx(8));
        adContainer.addView(adBody);

        // Call to action button
        Button callToAction = new Button(this);
        callToAction.setText(nativeAd.getAdCallToAction());
        callToAction.setBackgroundResource(R.drawable.bg_glass_card_premium);
        callToAction.setTextColor(getResources().getColor(R.color.accent_primary));
        callToAction.setTextSize(14);
        callToAction.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        adContainer.addView(callToAction);

        nativeAdLayout.addView(adContainer);

        // Register interactive views
        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(adHeadline);
        clickableViews.add(callToAction);
        nativeAd.registerViewForInteraction(adContainer, new MediaView(this), clickableViews);
    }

    private void loadInterstitialAd() {
        interstitialAd = new InterstitialAd(this, "1679167109809598_1679167723142870");
        InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                Log.d(TAG, "Meta Interstitial displayed.");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                Log.d(TAG, "Meta Interstitial dismissed.");
                finish();
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                Log.e(TAG, "Meta Interstitial error: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                Log.d(TAG, "Meta Interstitial loaded.");
            }

            @Override
            public void onAdClicked(Ad ad) {}

            @Override
            public void onLoggingImpression(Ad ad) {}
        };

        interstitialAd.loadAd(
                interstitialAd.buildLoadAdConfig()
                        .withAdListener(interstitialAdListener)
                        .build());
    }

    private void showInterstitialAndFinish() {
        if (interstitialAd != null && interstitialAd.isAdLoaded()) {
            interstitialAd.show();
        } else {
            finish();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!matchAccepted) {
            leaveQueue();
        }
        socket.off("match-found");

        // Clean up Meta ads
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
    }
}
