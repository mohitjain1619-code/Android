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
import java.util.ArrayList;
import java.util.List;

// Import Google AdMob classes
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

public class ConnectingActivity extends BaseActivity {

    private static final String TAG = "ConnectingActivity";
    private Socket socket;
    private TokenManager tokenManager;

    private String category = "";
    private String userGender = "";
    private String myUid = "";

    private boolean isWaiting = false;
    private boolean matchAccepted = false;

    // Google AdMob Mediated Native Ad & Delay
    private NativeAd nativeAd;
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

        // Load AdMob mediated native ads after initialization
        com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {
            runOnUiThread(() -> {
                loadNativeAd();
            });
        });

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
        socket.off("match-found");

        socket.on("match-found", args -> {
            Log.d(TAG, "📥 match-found socket event received. isWaiting=" + isWaiting);
            if (!isWaiting) return;

            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");
                Log.d(TAG, "📥 match-found details -> peerId: " + peerId + ", myUid: " + myUid);

                if (peerId.equals(myUid)) return;

                runOnUiThread(() -> {
                    isWaiting = false;
                    socket.off("match-found");

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
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!matchAccepted) {
            leaveQueue();
        }
        socket.off("match-found");

        // Clean up AdMob ads
        if (nativeAd != null) {
            nativeAd.destroy();
        }
    }
}
