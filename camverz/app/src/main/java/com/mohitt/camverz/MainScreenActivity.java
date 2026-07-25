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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.applovin.sdk.AppLovinSdk;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

public class MainScreenActivity extends BaseActivity {

    private static final String TAG = "MainScreen";
    private Socket socket;
    private TokenManager tokenManager;
    private ApiService api;

    private LinearLayout cardGay, cardLesbian, cardStraight;
    private ImageView menuIcon;

    private FrameLayout videoNav, profileNav, imageNav, messageNav;
    private ImageView iconVideo, iconProfile, iconImage, iconMessage;
    private TextView messageBadge;

    private MaxAdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        tokenManager = TokenManager.getInstance(this);
        api = ApiClient.getInstance(this).getApi();
        socket = SocketManager.getInstance();

        // Initialize AppLovin MAX SDK for ads
        AppLovinSdk.getInstance(this).setMediationProvider("max");
        AppLovinSdk.initializeSdk(this, configuration -> {
            runOnUiThread(this::loadBannerAd);
        });

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
        menuIcon = findViewById(R.id.menu_icon);

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
            // Already on this screen, no navigation needed.
        });

        imageNav.setOnClickListener(v -> {
            iconImage.startAnimation(pop);
            Intent intent = new Intent(this, FeedActivity.class);
            startActivity(intent);
        });

        messageNav.setOnClickListener(v -> {
            iconMessage.startAnimation(pop);
            Intent intent = new Intent(this, InboxActivity.class);
            startActivity(intent);
        });

        profileNav.setOnClickListener(v -> {
            iconProfile.startAnimation(pop);

            // Set active for immediate feedback before navigating
            iconProfile.setColorFilter(Color.WHITE);
            iconVideo.setColorFilter(Color.parseColor("#AAAAAA"));
            iconImage.setColorFilter(Color.parseColor("#AAAAAA"));

            Intent i = new Intent(this, ProfileActivity.class);
            i.putExtra("userId", tokenManager.getUserId());
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        menuIcon.setOnClickListener(this::showPopupMenu);

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
                AppLovinSdk.getInstance(this).showMediationDebugger();
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset the icon state to ensure Video tab is always active here
        iconVideo.setColorFilter(Color.WHITE);
        iconProfile.setColorFilter(Color.parseColor("#AAAAAA"));
        iconImage.setColorFilter(Color.parseColor("#AAAAAA"));
        iconMessage.setColorFilter(Color.parseColor("#AAAAAA"));

        // Ensure user is registered on socket
        if (tokenManager.isLoggedIn()) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("uid", tokenManager.getUserId());
                socket.emit("register-user", obj);
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
        socket.on("match-found", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");
                Log.d(TAG, "Matched with: " + peerId);
                runOnUiThread(() -> {
                    Toast.makeText(MainScreenActivity.this, "Matched with: " + peerId, Toast.LENGTH_SHORT).show();
                });
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing match-found", e);
            }
        });

        // Listener for incoming private calls
        socket.on("incoming-private-call", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callerId = data.getString("callerId");
                String callerName = data.optString("callerName", "Unknown");
                String callerAvatar = data.optString("callerAvatar", "");
                boolean isVideo = data.optBoolean("isVideo", true);
                String roomName = data.getString("room");

                runOnUiThread(() -> {
                    // Start ChatCallActivity as Receiver
                    Intent intent = new Intent(MainScreenActivity.this, CallActivity.class);
                    intent.putExtra("targetUserId", callerId);
                    intent.putExtra("targetUserName", callerName);
                    intent.putExtra("targetUserAvatar", callerAvatar);
                    intent.putExtra("isVideoCall", isVideo);
                    intent.putExtra("isCaller", false); // I am the receiver
                    intent.putExtra("roomName", roomName);
                    startActivity(intent);
                });
            } catch (JSONException e) {
                Log.e(TAG, "Error handling incoming-private-call", e);
            }
        });

        socket.on("new_message", args -> {
            runOnUiThread(this::fetchUnreadMessages);
        });
    }

    private void logout() {
        tokenManager.clearToken();
        socket.disconnect();
        Intent intent = new Intent(MainScreenActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToConnecting(String category) {
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
        adView = new MaxAdView("YOUR_BANNER_AD_UNIT_ID", this);

        // Set size (Match parent width, 50dp height for phones)
        int heightPx = AppLovinSdkUtils.dpToPx(this, 50);
        adView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heightPx
        ));

        // Background color is required for banners to function properly
        adView.setBackgroundColor(Color.TRANSPARENT);

        // Add to your layout
        FrameLayout adContainer = findViewById(R.id.banner_ad_container);
        if (adContainer != null) {
            adContainer.addView(adView);
            // Load the ad
            adView.loadAd();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        socket.off("match-found");
        socket.off("incoming-private-call");
        socket.off("new_message");
    }
}
