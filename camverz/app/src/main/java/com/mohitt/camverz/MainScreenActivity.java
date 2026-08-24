package com.mohitt.camverz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// import com.applovin.sdk.AppLovinSdk;
// import com.applovin.mediation.ads.MaxAdView;
// import com.applovin.sdk.AppLovinSdkUtils;

public class MainScreenActivity extends BaseActivity {

    private static final String TAG = "MainScreen";
    private Socket socket;
    private Emitter.Listener connectListener;
    private Emitter.Listener onlineUsersListener;
    private Emitter.Listener onlineCountListener;
    private Emitter.Listener matchFoundListener;
    private Emitter.Listener incomingPrivateCallListener;
    private Emitter.Listener newMessageListener;
    private TokenManager tokenManager;
    private ApiService api;

    private LinearLayout cardGay, cardLesbian, cardStraight;
    private LinearLayout chipCommunityHub, chipFriends;
    private TextView tvUserName, tvLiveCount;
    private ImageView menuIcon;

    private FrameLayout videoNav, profileNav, imageNav, messageNav;
    private ImageView iconVideo, iconProfile, iconImage, iconMessage;
    private TextView messageBadge;
    private FrameLayout notificationLayout;
    private TextView notificationBadge;

    private com.google.android.gms.ads.AdView adView;
    private boolean isRewardedVideoAvailable = false;
    private String pendingCategory = "straight";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.top_nav_bar), findViewById(R.id.bottom_nav_bar));

        tokenManager = TokenManager.getInstance(this);
        if (!tokenManager.isLoggedIn()) {
            Log.w(TAG, "Session inactive on MainScreen launch. Redirecting to LoginActivity.");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        api = ApiClient.getInstance(this).getApi();
        socket = SocketManager.getInstance();

        // Register current user on socket for private calls
        try {
            JSONObject obj = new JSONObject();
            obj.put("uid", tokenManager.getUserId());
            socket.emit("register-user", obj);
        } catch (JSONException e) { e.printStackTrace(); }
        
        // Initialize global incoming call handler (works from any screen)
        IncomingCallHandler.getInstance().init(getApplication(), socket);

        // Sync user ad-free plan details
        api.getMe().enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("user")) {
                        JsonObject userObj = data.getAsJsonObject("user");
                        boolean planIsAdFree = userObj.has("planIsAdFree") && userObj.get("planIsAdFree").getAsBoolean();
                        tokenManager.savePlanIsAdFree(planIsAdFree);
                    }
                } else if (response.code() == 401) {
                    Log.w(TAG, "Sync failed: 401 Unauthorized. Redirecting to LoginActivity...");
                    logout();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {}
        });

        cardGay = findViewById(R.id.cardGay);
        cardLesbian = findViewById(R.id.cardLesbian);
        cardStraight = findViewById(R.id.cardStraight);
        tvUserName = findViewById(R.id.tvUserName);
        tvLiveCount = findViewById(R.id.tvLiveCount);
        menuIcon = findViewById(R.id.menu_icon);
        notificationLayout = findViewById(R.id.notification_layout);
        notificationBadge = findViewById(R.id.notification_badge);

        if (notificationLayout != null) {
            notificationLayout.setOnClickListener(v -> {
                Intent intent = new Intent(MainScreenActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        chipCommunityHub = findViewById(R.id.chip_community_hub);
        chipFriends = findViewById(R.id.chip_friends);

        if (tvUserName != null && tokenManager.getUserName() != null && !tokenManager.getUserName().isEmpty()) {
            tvUserName.setText(tokenManager.getUserName());
        }

        if (tvUserName != null) {
            tvUserName.setOnLongClickListener(v -> {
                com.google.android.gms.ads.MobileAds.openAdInspector(this, error -> {
                    if (error != null) {
                        android.widget.Toast.makeText(this, "Ad Inspector error: " + error.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            });
        }

        if (chipCommunityHub != null) {
            chipCommunityHub.setOnClickListener(v -> {
                Intent intent = new Intent(MainScreenActivity.this, RealMeetActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        if (chipFriends != null) {
            chipFriends.setOnClickListener(v -> {
                Intent intent = new Intent(this, InboxActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        videoNav = findViewById(R.id.nav_video);
        profileNav = findViewById(R.id.nav_profile);
        imageNav = findViewById(R.id.nav_image);
        messageNav = findViewById(R.id.nav_message);
        iconVideo = findViewById(R.id.icon_video);
        iconProfile = findViewById(R.id.icon_profile);
        iconImage = findViewById(R.id.icon_image);
        iconMessage = findViewById(R.id.icon_message);
        messageBadge = findViewById(R.id.message_badge);

        setupSocketListeners();
        fetchUnreadMessages();

        Animation pop = AnimationUtils.loadAnimation(this, R.anim.icon_pop);

        videoNav.setOnClickListener(v -> {
            iconVideo.startAnimation(pop);
        });

        imageNav.setOnClickListener(v -> {
            iconImage.startAnimation(pop);
            Intent intent = new Intent(this, FeedActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        messageNav.setOnClickListener(v -> {
            iconMessage.startAnimation(pop);
            Intent intent = new Intent(this, InboxActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        profileNav.setOnClickListener(v -> {
            iconProfile.startAnimation(pop);

            iconProfile.setColorFilter(Color.parseColor("#4F46E5"));
            iconVideo.setColorFilter(Color.parseColor("#9CA3AF"));
            iconImage.setColorFilter(Color.parseColor("#9CA3AF"));
            iconMessage.setColorFilter(Color.parseColor("#9CA3AF"));

            Intent i = new Intent(this, ProfileActivity.class);
            i.putExtra("userId", tokenManager.getUserId());
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        menuIcon.setOnClickListener(this::showPopupMenu);

        addTouchScaleFeedback(cardStraight, cardGay, cardLesbian, chipCommunityHub, chipFriends, videoNav, profileNav, imageNav, messageNav);

        cardGay.setOnClickListener(v -> {
            if (!"male".equalsIgnoreCase(tokenManager.getUserGender())) {
                Toast.makeText(MainScreenActivity.this, "Only males can join Gay section", Toast.LENGTH_SHORT).show();
                return;
            }
            goToConnecting("gay");
        });
        cardLesbian.setOnClickListener(v -> {
            if (!"female".equalsIgnoreCase(tokenManager.getUserGender())) {
                Toast.makeText(MainScreenActivity.this, "Only females can join Lesbian section", Toast.LENGTH_SHORT).show();
                return;
            }
            goToConnecting("lesbian");
        });
        cardStraight.setOnClickListener(v -> goToConnecting("straight"));

        // Initialize AdMob SDK for ads
        com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {});
        com.facebook.ads.AudienceNetworkAds.initialize(this);

        FrameLayout adContainer = findViewById(R.id.banner_ad_container);
        if (adContainer != null) {
            adContainer.setVisibility(View.GONE);
        }

        // AdMob is initialized, no LevelPlay/ironSource initialization anymore.
    }

    private void addTouchScaleFeedback(View... views) {
        for (View v : views) {
            if (v == null) continue;
            v.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                        break;
                }
                return false;
            });
        }
    }

    private void showPopupMenu(View view) {
        Context wrapper = new ContextThemeWrapper(this, R.style.CustomPopupStyle);
        PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.top_menu, popup.getMenu());

        // Dynamic developer check to prevent normal users from seeing debugging options
        String userEmail = tokenManager.getUserEmail();
        boolean isAdmin = "neerajjain0220@gmail.com".equalsIgnoreCase(userEmail) || 
                          (userEmail != null && userEmail.contains("admin")) ||
                          (userEmail != null && userEmail.contains("mohitjain1619"));

        if (!isAdmin) {
            popup.getMenu().removeItem(R.id.action_mediation_debugger);
            popup.getMenu().removeItem(R.id.action_ad_testing);
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_logout) {
                logout();
                return true;
            } else if (itemId == R.id.action_mediation_debugger) {
                Toast.makeText(this, "Mediation debugger is disabled", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.action_ad_testing) {
                startActivity(new Intent(this, AdTestingActivity.class));
                return true;
            } else if (itemId == R.id.action_share_app) {
                shareApp();
                return true;
            } else if (itemId == R.id.action_rate_app) {
                openPlayStoreForRating();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void openPlayStoreForRating() {
        try {
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + getPackageName()));
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(playStoreIntent);
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
        Toast.makeText(this, "Thank you for rating Camverz 5 Stars! ⭐", Toast.LENGTH_LONG).show();
    }

    private void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Camverz: Video Call & Real Meet");
            String shareMessage = "\nHey! Download Camverz: Video Call & Real Meet App to make random video calls and connect with friends instantly!\n\n" +
                    "https://play.google.com/store/apps/details?id=com.mohitt.camverz\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share App via"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share app", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        iconVideo.setColorFilter(Color.parseColor("#4F46E5"));
        iconProfile.setColorFilter(Color.parseColor("#9CA3AF"));
        iconImage.setColorFilter(Color.parseColor("#9CA3AF"));
        iconMessage.setColorFilter(Color.parseColor("#9CA3AF"));

        if (tokenManager.isLoggedIn()) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("uid", tokenManager.getUserId());
                socket.emit("register-user", obj);
                socket.emit("get-online-count");
            } catch (JSONException e) { e.printStackTrace(); }
            fetchUnreadMessages();
            fetchUnreadNotificationsCount();
        }
        checkPreferenceSelection();
    }

    private void fetchUnreadNotificationsCount() {
        if (!tokenManager.isLoggedIn()) return;
        api.getNotificationUnreadCount().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("unreadCount")) {
                        int count = data.get("unreadCount").getAsInt();
                        runOnUiThread(() -> {
                            if (count > 0 && notificationBadge != null) {
                                notificationBadge.setVisibility(View.VISIBLE);
                                if (count > 9) {
                                    notificationBadge.setText("9+");
                                } else {
                                    notificationBadge.setText(String.valueOf(count));
                                }
                            } else if (notificationBadge != null) {
                                notificationBadge.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to fetch notification unread count", t);
            }
        });
    }

    private void fetchUnreadMessages() {
        if (!tokenManager.isLoggedIn()) return;

        api.getUnreadCount().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        int unreadCount = 0;
                        if (data.has("count")) {
                            unreadCount = data.get("count").getAsInt();
                        } else if (data.has("unreadCount")) {
                            unreadCount = data.get("unreadCount").getAsInt();
                        }
                        updateUnreadMessagesCount(unreadCount);
                    }
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching unread count", t);
            }
        });
    }

    private void updateUnreadMessagesCount(int count) {
        if (messageBadge != null) {
            if (count > 0) {
                messageBadge.setText(String.valueOf(count));
                messageBadge.setVisibility(View.VISIBLE);
            } else {
                messageBadge.setVisibility(View.GONE);
            }
        }
    }

    private void setupSocketListeners() {
        connectListener = args -> runOnUiThread(() -> {
            if (tvLiveCount != null) {
                tvLiveCount.setText("Live Online");
            }
        });
        socket.on(Socket.EVENT_CONNECT, connectListener);

        onlineUsersListener = args -> {
            if (args != null && args.length > 0 && tvLiveCount != null) {
                try {
                    int count = Integer.parseInt(args[0].toString());
                    runOnUiThread(() -> tvLiveCount.setText(count + " Online"));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing online count", e);
                }
            }
        };
        socket.on("online-users", onlineUsersListener);

        onlineCountListener = args -> {
            if (args != null && args.length > 0 && tvLiveCount != null) {
                try {
                    int count = Integer.parseInt(args[0].toString());
                    runOnUiThread(() -> tvLiveCount.setText(count + " Online"));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing online count", e);
                }
            }
        };
        socket.on("online_count", onlineCountListener);

        matchFoundListener = args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");
                Log.d(TAG, "Matched with: " + peerId);
                runOnUiThread(() -> Toast.makeText(MainScreenActivity.this, "Matched with: " + peerId, Toast.LENGTH_SHORT).show());
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing match-found", e);
            }
        };
        socket.on("match-found", matchFoundListener);

        // incoming-private-call is handled globally by IncomingCallHandler

        newMessageListener = args -> runOnUiThread(this::fetchUnreadMessages);
        socket.on("new_message", newMessageListener);
    }

    private void logout() {
        // Destroy the global incoming call handler
        IncomingCallHandler.getInstance().destroy(getApplication());
        CallManager.reset();
        
        tokenManager.clearToken();
        socket.disconnect();
        Intent intent = new Intent(MainScreenActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isVpnActive() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.net.Network activeNetwork = cm.getActiveNetwork();
                    android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    return caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN);
                } else {
                    java.util.List<java.net.NetworkInterface> interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
                    for (java.net.NetworkInterface networkInterface : interfaces) {
                        if (networkInterface.isUp()) {
                            String name = networkInterface.getName().toLowerCase();
                            if (name.contains("tun") || name.contains("ppp") || name.contains("p2p") || name.contains("tap")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking VPN connection state", e);
        }
        return false;
    }

    private void goToConnecting(String category) {
        if (isVpnActive()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("VPN Detected")
                .setMessage("Please turn off your VPN for video calling to work properly. VPN connections cause calls to fail.\n\nBehtar video call quality ke liye kripya apna VPN/Proxy band kijiye.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        if ("female".equalsIgnoreCase(tokenManager.getUserGender()) && !tokenManager.isVerified()) {
            Toast.makeText(this, "Verification is required to start calling", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(MainScreenActivity.this, VerificationInfoActivity.class);
            startActivity(intent);
            return;
        }
        pendingCategory = category;

        checkCallLimitsAndProceed(() -> {
            // Increment call attempt counter
            android.content.SharedPreferences prefs = getSharedPreferences("app_stats", MODE_PRIVATE);
            int attempts = prefs.getInt("call_attempts", 0);
            attempts++;
            prefs.edit().putInt("call_attempts", attempts).apply();

            Log.d(TAG, "Call attempt #" + attempts);
            proceedToConnectingDirectly();
        });
    }

    private void checkCallLimitsAndProceed(Runnable onPassed) {
        if (tokenManager.isPlanAdFree()) {
            onPassed.run();
            return;
        }

        if (CallLimitManager.isBlocked(this)) {
            showExceededBlockDialog();
            return;
        }

        long secondsLeft = CallLimitManager.getFreeSecondsLeft(this);
        if (secondsLeft <= 0) {
            int tier = CallLimitManager.getRewardedTier(this);
            if (tier == 1) {
                showWatchAdsDialog(onPassed);
            } else {
                CallLimitManager.setLimitBlockedTime(this, System.currentTimeMillis());
                CallLimitManager.setRewardedTier(this, 3);
                showExceededBlockDialog();
            }
            return;
        }

        onPassed.run();
    }

    private void showExceededBlockDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Limit Exceeded")
            .setMessage("For today you have exceeded all your limit, come back after 10 hours.")
            .setCancelable(false)
            .setPositiveButton("OK", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://camverz.com"));
                startActivity(browserIntent);
            })
            .show();
    }

    private void showWatchAdsDialog(Runnable onPassed) {
        int watched = CallLimitManager.getRewardedAdsWatched(this);
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Daily Limit Hit")
            .setMessage("Daily free call limit reached.\n\nWatch 3 short video ads to get 5 more minutes of calling!\n\nAds Watched: " + watched + "/3")
            .setCancelable(false)
            .setPositiveButton("Watch Ad", (dialog, which) -> {
                loadAndShowRewardedAd(() -> {
                    CallLimitManager.incrementRewardedAdsWatched(this);
                    int newWatched = CallLimitManager.getRewardedAdsWatched(this);
                    if (newWatched >= 3) {
                        CallLimitManager.setRewardedTier(this, 2);
                        CallLimitManager.addFreeSeconds(this, 300);
                        CallLimitManager.resetRewardedAdsWatched(this);
                        Toast.makeText(this, "Success! You received 5 extra minutes.", Toast.LENGTH_LONG).show();
                        onPassed.run();
                    } else {
                        showWatchAdsDialog(onPassed);
                    }
                }, () -> {
                    Toast.makeText(this, "Failed to load ad. Please try again.", Toast.LENGTH_SHORT).show();
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadAndShowRewardedAd(Runnable onSuccess, Runnable onFailure) {
        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Loading ad...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        com.google.android.gms.ads.rewarded.RewardedAd.load(this, 
            getString(R.string.admob_rewarded_ad_unit_id), adRequest,
            new com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull com.google.android.gms.ads.rewarded.RewardedAd ad) {
                    progressDialog.dismiss();
                    ad.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {}
                        @Override
                        public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                            runOnUiThread(onFailure);
                        }
                    });
                    ad.show(MainScreenActivity.this, rewardItem -> {
                        runOnUiThread(onSuccess);
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                    progressDialog.dismiss();
                    runOnUiThread(onFailure);
                }
            });
    }

    private void loadBannerAd() {
        // Ads disabled in main app
    }

    private void proceedToConnectingDirectly() {
        Intent intent = new Intent(MainScreenActivity.this, ConnectingActivity.class);
        intent.putExtra("category", pendingCategory);
        startActivity(intent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        if (socket != null) {
            if (connectListener != null) socket.off(Socket.EVENT_CONNECT, connectListener);
            if (onlineUsersListener != null) socket.off("online-users", onlineUsersListener);
            if (onlineCountListener != null) socket.off("online_count", onlineCountListener);
            if (matchFoundListener != null) socket.off("match-found", matchFoundListener);
            // incoming-private-call is managed by IncomingCallHandler (global, not removed here)
            if (newMessageListener != null) socket.off("new_message", newMessageListener);
        }
    }

    private void checkPreferenceSelection() {
        if (tokenManager.isLoggedIn() && tokenManager.getSexPreference().isEmpty()) {
            PreferenceSelectionDialog dialog = new PreferenceSelectionDialog(this, preference -> {
                Toast.makeText(this, "Sex preference updated: " + preference, Toast.LENGTH_SHORT).show();
            });
            dialog.show();
        }
    }
}
