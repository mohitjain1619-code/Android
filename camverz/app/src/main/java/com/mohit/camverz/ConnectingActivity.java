package com.mohit.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.mohit.camverz.api.TokenManager;
import org.json.JSONObject;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import android.view.LayoutInflater;
import android.widget.ImageView;
import androidx.activity.OnBackPressedCallback;


import android.graphics.Color;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;

import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener;
import com.ironsource.mediationsdk.ads.nativead.NativeAdLayout;
import com.ironsource.mediationsdk.logger.IronSourceError;

public class ConnectingActivity extends BaseActivity {

    private static final String TAG = "ConnectingActivity";
    private Socket socket;
    private Emitter.Listener matchFoundListener;
    private Emitter.Listener matchCancelledListener;
    private Emitter.Listener limitExceededListener;
    private Emitter.Listener connectListener;
    private TokenManager tokenManager;
    private Handler queueHeartbeatHandler;
    private Runnable queueHeartbeatRunnable;

    private String category = "";
    private String userGender = "";
    private String myUid = "";
    private String matchedPeerId = null;

    private boolean isWaiting = false;
    private boolean matchAccepted = false;

    private Handler countdownHandler;
    private Runnable countdownRunnable;

    private TextView waitingText;
    private long searchStartTime = 0;

    // LevelPlay Bidding Native Ad
    private LevelPlayNativeAd levelPlayNativeAd;
    private FrameLayout nativeAdContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leaveQueue();
                finish();
            }
        });

        tokenManager = TokenManager.getInstance(this);
        socket = SocketManager.getInstance();

        myUid = tokenManager.getUserId();
        category = getIntent().getStringExtra("category");
        if (category == null) category = "straight";

        waitingText = findViewById(R.id.waitingText);
        TextView categoryText = findViewById(R.id.categoryText);
        categoryText.setText("Category: " + category.substring(0, 1).toUpperCase() + category.substring(1));

        nativeAdContainer = findViewById(R.id.nativeAdContainer);

        // Start search timer
        searchStartTime = System.currentTimeMillis();

        setupSocketListeners();
        loadUserDataAndJoinQueue();
        loadBiddingNativeAd();

        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            leaveQueue();
            finish();
        });

    }

    private void loadUserDataAndJoinQueue() {
        userGender = tokenManager.getUserGender();
        if (userGender == null || userGender.isEmpty()) {
            userGender = "unknown";
        }
        joinQueue();
    }

    private void joinQueue() {
        if (!isWaiting) isWaiting = true;
        matchedPeerId = null;

        if (waitingText != null) {
            waitingText.setText("Finding a match...");
        }

        try {
            JSONObject obj = new JSONObject();
            obj.put("uid", myUid);
            obj.put("gender", userGender);
            obj.put("category", category);

            Log.d(TAG, "📤 join-queue (category: " + category + ")");
            socket.emit("join-queue", obj);
        } catch (Exception e) {
            Log.e(TAG, "join-queue failed: " + e.getMessage());
        }

        startQueueHeartbeat();
    }

    private void startQueueHeartbeat() {
        stopQueueHeartbeat();
        queueHeartbeatHandler = new Handler(Looper.getMainLooper());
        queueHeartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isWaiting && !isFinishing() && !isDestroyed()) {
                    Log.d(TAG, "💓 Queue heartbeat: Refreshing join-queue on backend...");
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("uid", myUid);
                        obj.put("gender", userGender);
                        obj.put("category", category);
                        socket.emit("join-queue", obj);
                    } catch (Exception e) {}
                    if (queueHeartbeatHandler != null) {
                        queueHeartbeatHandler.postDelayed(this, 8000); // 8 second heartbeat
                    }
                }
            }
        };
        queueHeartbeatHandler.postDelayed(queueHeartbeatRunnable, 8000);
    }

    private void stopQueueHeartbeat() {
        if (queueHeartbeatHandler != null && queueHeartbeatRunnable != null) {
            queueHeartbeatHandler.removeCallbacks(queueHeartbeatRunnable);
            queueHeartbeatHandler = null;
            queueHeartbeatRunnable = null;
        }
    }

    private void leaveQueue() {
        destroyNativeAd();
        isWaiting = false;
        stopQueueHeartbeat();
        cancelCountdown();

        // If user cancels during matched countdown, notify peer immediately
        if (matchedPeerId != null) {
            try {
                JSONObject cancelObj = new JSONObject();
                cancelObj.put("uid", myUid);
                cancelObj.put("peerId", matchedPeerId);
                socket.emit("cancel-match", cancelObj);
                Log.d(TAG, "📤 cancel-match emitted for peer: " + matchedPeerId);
            } catch (Exception e) {
                Log.e(TAG, "cancel-match failed", e);
            }
            matchedPeerId = null;
        }

        try {
            JSONObject obj = new JSONObject();
            obj.put("uid", myUid);

            Log.d(TAG, "📤 leave-queue");
            socket.emit("leave-queue", obj);
        } catch (Exception e) {
            Log.e(TAG, "leave-queue failed: " + e.getMessage());
        }
    }

    private void cancelCountdown() {
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownHandler = null;
            countdownRunnable = null;
        }
    }

    private void setupSocketListeners() {
        removeSocketListeners();

        connectListener = args -> {
            Log.d(TAG, "Socket reconnected in ConnectingActivity.");
            try {
                if (myUid != null && !myUid.isEmpty()) {
                    JSONObject regObj = new JSONObject();
                    regObj.put("uid", myUid);
                    socket.emit("register-user", regObj);
                }
            } catch (Exception e) {}
            if (isWaiting) {
                Log.d(TAG, "Re-emitting join-queue after socket reconnect");
                joinQueue();
            }
        };
        socket.on(Socket.EVENT_CONNECT, connectListener);

        limitExceededListener = args -> {
            Log.w(TAG, "⚠️ Received limit-exceeded socket event.");
            runOnUiThread(this::showLimitExceededDialog);
        };
        socket.on("limit-exceeded", limitExceededListener);

        matchCancelledListener = args -> runOnUiThread(() -> {
            Log.w(TAG, "⚠️ Peer cancelled match during countdown. Returning to queue.");
            cancelCountdown();
            matchedPeerId = null;
            isWaiting = false;
            setupSocketListeners();
            joinQueue();
        });
        socket.on("match-cancelled", matchCancelledListener);

        matchFoundListener = args -> {
            Log.d(TAG, "📥 match-found socket event received. isWaiting=" + isWaiting);
            if (!isWaiting) return;

            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");
                String peerName = data.has("peerName") ? data.getString("peerName") : "";
                String peerAvatar = data.has("peerAvatar") ? data.getString("peerAvatar") : "";
                Log.d(TAG, "📥 match-found details -> peerId: " + peerId + ", myUid: " + myUid);

                if (peerId.equals(myUid)) return;

                runOnUiThread(() -> {
                    isWaiting = false;
                    stopQueueHeartbeat();
                    matchedPeerId = peerId;
                    startMatchTransitionCountdown(peerId, peerName, peerAvatar);
                });

            } catch (Exception e) {
                Log.e(TAG, "match-found error: " + e.getMessage());
            }
        };
        socket.on("match-found", matchFoundListener);
    }

    private void startMatchTransitionCountdown(String peerId, String peerName, String peerAvatar) {
        cancelCountdown();
        if (waitingText != null) {
            waitingText.setText("Matched! Connecting...");
        }
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            transitionToCall(peerId, peerName, peerAvatar);
        };
        countdownHandler.postDelayed(countdownRunnable, 300);
    }

    private void transitionToCall(String peerId, String peerName, String peerAvatar) {
        destroyNativeAd();
        Log.d(TAG, "🚀 transitionToCall starting CallActivity with peerId: " + peerId);
        matchAccepted = true;
        Intent i = new Intent(ConnectingActivity.this, CallActivity.class);
        i.putExtra("peer", peerId);
        if (peerName != null && !peerName.isEmpty()) i.putExtra("peerName", peerName);
        if (peerAvatar != null && !peerAvatar.isEmpty()) i.putExtra("peerAvatar", peerAvatar);
        i.putExtra("category", category);
        startActivity(i);
        finish();
    }

    // =========================================================================
    // LEVELPLAY HEADER BIDDING NATIVE AD INTEGRATION
    // =========================================================================

    private void loadBiddingNativeAd() {
        try {
            if (levelPlayNativeAd != null) {
                levelPlayNativeAd.destroyAd();
                levelPlayNativeAd = null;
            }

            Log.d(TAG, "📡 Requesting LevelPlay Bidding Native Ad...");
            LevelPlayNativeAd.Builder builder = new LevelPlayNativeAd.Builder();
            builder.withListener(new LevelPlayNativeAdListener() {
                @Override
                public void onAdLoaded(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed() || !isWaiting) {
                            if (nativeAd != null) {
                                nativeAd.destroyAd();
                            }
                            return;
                        }
                        levelPlayNativeAd = nativeAd;
                        String network = adInfo != null && adInfo.getAdNetwork() != null ? adInfo.getAdNetwork() : "Bidding Winner";
                        Log.d(TAG, "🟢 LevelPlay Bidding Native Ad Loaded! Winner: " + network);
                        renderNativeAdView(nativeAd);
                    });
                }

                @Override
                public void onAdLoadFailed(LevelPlayNativeAd nativeAd, IronSourceError error) {
                    runOnUiThread(() -> {
                        String msg = error != null ? error.getErrorMessage() : "Unknown error";
                        int code = error != null ? error.getErrorCode() : -1;
                        Log.w(TAG, "🔴 LevelPlay Bidding Native Ad Failed: " + msg + " [Code: " + code + "]");
                        if (nativeAdContainer != null) {
                            nativeAdContainer.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onAdImpression(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    Log.d(TAG, "👁️ LevelPlay Native Ad Impression recorded");
                }

                @Override
                public void onAdClicked(LevelPlayNativeAd nativeAd, AdInfo adInfo) {
                    Log.d(TAG, "🖱️ LevelPlay Native Ad clicked by user");
                }
            });

            levelPlayNativeAd = builder.build();
            levelPlayNativeAd.loadAd();
        } catch (Exception e) {
            Log.e(TAG, "Exception initializing LevelPlay Native Ad", e);
        }
    }

    private void renderNativeAdView(LevelPlayNativeAd nativeAd) {
        if (nativeAd == null || nativeAdContainer == null || isFinishing() || isDestroyed()) return;

        try {
            nativeAdContainer.removeAllViews();

            NativeAdLayout nativeAdLayout = (NativeAdLayout) LayoutInflater.from(this)
                    .inflate(R.layout.layout_levelplay_native_ad, nativeAdContainer, false);

            TextView tvTitle = nativeAdLayout.findViewById(R.id.lp_native_title);
            TextView tvAdvertiser = nativeAdLayout.findViewById(R.id.lp_native_advertiser);
            TextView tvBody = nativeAdLayout.findViewById(R.id.lp_native_body);
            ImageView ivIcon = nativeAdLayout.findViewById(R.id.lp_native_icon);
            Button btnCta = nativeAdLayout.findViewById(R.id.lp_native_cta);
            LevelPlayMediaView mediaView = nativeAdLayout.findViewById(R.id.lp_native_media_view);

            if (tvTitle != null && nativeAd.getTitle() != null) {
                tvTitle.setText(nativeAd.getTitle());
                nativeAdLayout.setTitleView(tvTitle);
            }
            if (tvAdvertiser != null && nativeAd.getAdvertiser() != null) {
                tvAdvertiser.setText(nativeAd.getAdvertiser());
                nativeAdLayout.setAdvertiserView(tvAdvertiser);
            }
            if (tvBody != null && nativeAd.getBody() != null) {
                tvBody.setText(nativeAd.getBody());
                nativeAdLayout.setBodyView(tvBody);
            }
            if (btnCta != null && nativeAd.getCallToAction() != null) {
                btnCta.setText(nativeAd.getCallToAction());
                nativeAdLayout.setCallToActionView(btnCta);
            }
            if (ivIcon != null && nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                ivIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
                nativeAdLayout.setIconView(ivIcon);
            }
            if (mediaView != null) {
                nativeAdLayout.setMediaView(mediaView);
            }

            // Register native layout with SDK for click and impression tracking
            nativeAdLayout.registerNativeAdViews(nativeAd);

            nativeAdContainer.addView(nativeAdLayout);
            nativeAdContainer.setVisibility(View.VISIBLE);
            Log.d(TAG, "🖼️ LevelPlay Native Ad rendered successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Error rendering LevelPlay native ad", e);
        }
    }

    private void destroyNativeAd() {
        try {
            if (levelPlayNativeAd != null) {
                levelPlayNativeAd.destroyAd();
                levelPlayNativeAd = null;
            }
            if (nativeAdContainer != null) {
                nativeAdContainer.removeAllViews();
                nativeAdContainer.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error destroying native ad: " + e.getMessage());
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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
        destroyNativeAd();
        if (!matchAccepted) {
            leaveQueue();
        }
        removeSocketListeners();
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
        if (connectListener != null) {
            socket.off(Socket.EVENT_CONNECT, connectListener);
            connectListener = null;
        }
        if (matchFoundListener != null) {
            socket.off("match-found", matchFoundListener);
            matchFoundListener = null;
        }
        if (matchCancelledListener != null) {
            socket.off("match-cancelled", matchCancelledListener);
            matchCancelledListener = null;
        }
        if (limitExceededListener != null) {
            socket.off("limit-exceeded", limitExceededListener);
            limitExceededListener = null;
        }
        stopQueueHeartbeat();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!matchAccepted && isWaiting) {
            leaveQueue();
        }
    }
}
