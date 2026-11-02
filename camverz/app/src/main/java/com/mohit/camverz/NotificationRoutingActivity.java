package com.mohit.camverz;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.mohit.camverz.api.TokenManager;

/**
 * Production-grade Trampoline Activity to handle all FCM notification click events safely.
 * Dispatches to ChatActivity, CallActivity, ProfileActivity, RealMeetActivity, or MainScreenActivity
 * while resolving all parameter key name variances and handling cold starts.
 */
public class NotificationRoutingActivity extends Activity {

    private static final String TAG = "NotifRoutingActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent incoming = getIntent();
            Bundle extras = incoming != null ? incoming.getExtras() : null;

            String type = extras != null ? extras.getString("type") : null;
            Log.d(TAG, "NotificationRoutingActivity invoked with type: " + type);

            TokenManager tokenManager = TokenManager.getInstance(this);
            boolean isLoggedIn = tokenManager.hasToken() && tokenManager.getUserId() != null;

            if (!isLoggedIn) {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                finish();
                return;
            }

            Intent targetIntent;

            if ("chat".equalsIgnoreCase(type)) {
                targetIntent = new Intent(this, ChatActivity.class);

                String userId = extras != null ? extras.getString("userId") : null;
                if (userId == null && extras != null) userId = extras.getString("targetUserId");
                if (userId == null && extras != null) userId = extras.getString("senderId");

                String userName = extras != null ? extras.getString("userName") : null;
                if (userName == null && extras != null) userName = extras.getString("targetUserName");
                if (userName == null && extras != null) userName = extras.getString("senderName");

                String userAvatar = extras != null ? extras.getString("userAvatar") : null;
                if (userAvatar == null && extras != null) userAvatar = extras.getString("targetUserAvatar");
                if (userAvatar == null && extras != null) userAvatar = extras.getString("senderAvatar");

                String chatId = extras != null ? extras.getString("chatId") : null;

                targetIntent.putExtra("userId", userId);
                targetIntent.putExtra("targetUserId", userId);
                targetIntent.putExtra("userName", userName);
                targetIntent.putExtra("targetUserName", userName);
                targetIntent.putExtra("userAvatar", userAvatar);
                targetIntent.putExtra("targetUserAvatar", userAvatar);
                targetIntent.putExtra("chatId", chatId);

            } else if ("call".equalsIgnoreCase(type)) {
                targetIntent = new Intent(this, CallActivity.class);

                String peer = extras != null ? extras.getString("peer") : null;
                if (peer == null && extras != null) peer = extras.getString("callerId");
                if (peer == null && extras != null) peer = extras.getString("targetUserId");

                String peerName = extras != null ? extras.getString("peerName") : null;
                if (peerName == null && extras != null) peerName = extras.getString("callerName");
                if (peerName == null && extras != null) peerName = extras.getString("targetUserName");

                String peerAvatar = extras != null ? extras.getString("peerAvatar") : null;
                if (peerAvatar == null && extras != null) peerAvatar = extras.getString("callerAvatar");
                if (peerAvatar == null && extras != null) peerAvatar = extras.getString("targetUserAvatar");

                String room = extras != null ? extras.getString("roomName") : null;
                if (room == null && extras != null) room = extras.getString("room");

                boolean isVideo = true;
                if (extras != null && extras.containsKey("isVideoCall")) {
                    isVideo = extras.getBoolean("isVideoCall", true);
                } else if (extras != null && extras.containsKey("isVideo")) {
                    String isVideoStr = extras.getString("isVideo");
                    isVideo = isVideoStr == null || Boolean.parseBoolean(isVideoStr);
                }

                targetIntent.putExtra("peer", peer);
                targetIntent.putExtra("callerId", peer);
                targetIntent.putExtra("targetUserId", peer);
                targetIntent.putExtra("peerName", peerName);
                targetIntent.putExtra("callerName", peerName);
                targetIntent.putExtra("targetUserName", peerName);
                targetIntent.putExtra("peerAvatar", peerAvatar);
                targetIntent.putExtra("callerAvatar", peerAvatar);
                targetIntent.putExtra("targetUserAvatar", peerAvatar);
                targetIntent.putExtra("isVideoCall", isVideo);
                targetIntent.putExtra("isCaller", false);
                targetIntent.putExtra("isPrivateCall", true);
                targetIntent.putExtra("roomName", room);

                NotificationHelper.cancelIncomingCallNotification(this);

            } else if ("friend_request".equalsIgnoreCase(type)) {
                targetIntent = new Intent(this, ProfileActivity.class);
                String fromUserId = extras != null ? extras.getString("fromUserId") : null;
                if (fromUserId == null && extras != null) fromUserId = extras.getString("triggeringUserId");
                targetIntent.putExtra("userId", fromUserId);
                targetIntent.putExtra("targetUserId", fromUserId);

            } else if ("realmeet_request".equalsIgnoreCase(type) || "realmeet_accepted".equalsIgnoreCase(type)) {
                targetIntent = new Intent(this, RealMeetActivity.class);

            } else {
                targetIntent = new Intent(this, MainScreenActivity.class);
            }

            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(targetIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error routing notification click", e);
            Intent fallback = new Intent(this, MainScreenActivity.class);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(fallback);
        } finally {
            finish();
        }
    }
}
