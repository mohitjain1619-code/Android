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

    private com.google.android.gms.ads.AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.top_nav_bar), findViewById(R.id.bottom_nav_bar));

        tokenManager = TokenManager.getInstance(this);
        api = ApiClient.getInstance(this).getApi();
        socket = SocketManager.getInstance();

        if (tokenManager.isLoggedIn()) {
            // Register current user on socket for private calls
            try {
                JSONObject obj = new JSONObject();
                obj.put("uid", tokenManager.getUserId());
                socket.emit("register-user", obj);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        cardGay = findViewById(R.id.cardGay);
        cardLesbian = findViewById(R.id.cardLesbian);
        cardStraight = findViewById(R.id.cardStraight);
        tvUserName = findViewById(R.id.tvUserName);
        tvLiveCount = findViewById(R.id.tvLiveCount);
        menuIcon = findViewById(R.id.menu_icon);

        chipCommunityHub = findViewById(R.id.chip_community_hub);
        chipFriends = findViewById(R.id.chip_friends);

        if (tvUserName != null && tokenManager.getUserName() != null && !tokenManager.getUserName().isEmpty()) {
            tvUserName.setText(tokenManager.getUserName());
        }

        if (chipCommunityHub != null) {
            chipCommunityHub.setOnClickListener(v -> 
                Toast.makeText(MainScreenActivity.this, "💬 Community, Real Meet & Fantasy features are coming soon!", Toast.LENGTH_LONG).show());
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
        java.util.List<String> testDeviceIds = java.util.Arrays.asList("6FC7D2B1D7C9B618A6D9FDC8114FDEF5");
        com.google.android.gms.ads.RequestConfiguration requestConfiguration =
                new com.google.android.gms.ads.RequestConfiguration.Builder()
                        .setTestDeviceIds(testDeviceIds)
                        .build();
        com.google.android.gms.ads.MobileAds.setRequestConfiguration(requestConfiguration);
        com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {});
        com.facebook.ads.AudienceNetworkAds.initialize(this);

        FrameLayout adContainer = findViewById(R.id.banner_ad_container);
        if (adContainer != null) {
            adContainer.setVisibility(View.GONE);
        }
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
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                logout();
                return true;
            } else if (item.getItemId() == R.id.action_mediation_debugger) {
                Toast.makeText(this, "Mediation debugger is disabled", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
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
        }
    }

    private void fetchUnreadMessages() {
        if (!tokenManager.isLoggedIn()) return;

        api.getUnreadCount().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        int unreadCount = data.has("unreadCount") ? data.get("unreadCount").getAsInt() : 0;
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

        incomingPrivateCallListener = args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callerId = data.getString("callerId");
                String callerName = data.optString("callerName", "Unknown");
                String callerAvatar = data.optString("callerAvatar", "");
                boolean isVideo = data.optBoolean("isVideo", true);
                String roomName = data.getString("room");

                runOnUiThread(() -> {
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(MainScreenActivity.this);
                    builder.setTitle("Incoming Call");
                    builder.setMessage(callerName + " is calling you" + (isVideo ? " with video." : " with voice."));
                    builder.setPositiveButton("Accept", (dialog, which) -> {
                        Intent intent = new Intent(MainScreenActivity.this, CallActivity.class);
                        intent.putExtra("targetUserId", callerId);
                        intent.putExtra("targetUserName", callerName);
                        intent.putExtra("targetUserAvatar", callerAvatar);
                        intent.putExtra("isVideoCall", isVideo);
                        intent.putExtra("isCaller", false);
                        intent.putExtra("isPrivateCall", true);
                        intent.putExtra("roomName", roomName);
                        startActivity(intent);
                    });
                    builder.setNegativeButton("Reject", (dialog, which) -> {
                        try {
                            JSONObject reject = new JSONObject();
                            reject.put("callerId", callerId);
                            socket.emit("reject-private-call", reject);
                        } catch (JSONException ex) {
                            Log.e(TAG, "Error emitting reject-private-call", ex);
                        }
                        Toast.makeText(MainScreenActivity.this, "Call rejected.", Toast.LENGTH_SHORT).show();
                    });
                    builder.setOnCancelListener(dialog -> {
                        try {
                            JSONObject reject = new JSONObject();
                            reject.put("callerId", callerId);
                            socket.emit("reject-private-call", reject);
                        } catch (JSONException ex) {
                            Log.e(TAG, "Error emitting reject-private-call", ex);
                        }
                    });
                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
                    }
                    dialog.show();
                });
            } catch (JSONException e) {
                Log.e(TAG, "Error handling incoming-private-call", e);
            }
        };
        socket.on("incoming-private-call", incomingPrivateCallListener);

        newMessageListener = args -> runOnUiThread(this::fetchUnreadMessages);
        socket.on("new_message", newMessageListener);
    }

    private void logout() {
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
        Intent intent = new Intent(MainScreenActivity.this, ConnectingActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }

    private void loadBannerAd() {
        adView = new com.google.android.gms.ads.AdView(this);
        adView.setAdUnitId(getString(R.string.admob_banner_ad_unit_id));
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);

        FrameLayout adContainer = findViewById(R.id.banner_ad_container);
        if (adContainer != null) {
            adContainer.removeAllViews();
            adContainer.addView(adView);
            com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
            adView.loadAd(adRequest);
            Log.d(TAG, "✅ AdMob Banner Ad requested for MainScreen");
        }
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
            if (incomingPrivateCallListener != null) socket.off("incoming-private-call", incomingPrivateCallListener);
            if (newMessageListener != null) socket.off("new_message", newMessageListener);
        }
    }
}
