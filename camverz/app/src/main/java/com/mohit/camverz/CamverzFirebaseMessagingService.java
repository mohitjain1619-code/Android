package com.mohit.camverz;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mohit.camverz.api.TokenManager;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production-Grade Firebase Cloud Messaging (FCM) Service for Camverz.
 * Handles high-concurrency push events for Chats, Calls, Social interactions, and Marketing campaigns.
 */
public class CamverzFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "CamverzFCM";
    private static final AtomicInteger notificationCounter = new AtomicInteger(1000);

    // Active state tracking to prevent annoying in-app sound spam
    public static volatile String activeChatUserId = null;
    public static volatile boolean isCallScreenActive = false;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "🔑 New FCM Device Token received: " + token);
        try {
            TokenManager tokenManager = TokenManager.getInstance(getApplicationContext());
            tokenManager.saveFcmToken(token);
            tokenManager.syncFcmTokenToServer(getApplicationContext(), token);
        } catch (Exception e) {
            Log.e(TAG, "Error handling new FCM token", e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "📨 FCM Message Received from: " + remoteMessage.getFrom());

        try {
            Map<String, String> data = remoteMessage.getData();

            // Extract title and body from either data payload or notification payload
            String title = data.containsKey("title") ? data.get("title") :
                    (remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "Camverz");

            String body = data.containsKey("body") ? data.get("body") :
                    (remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "");

            String type = data.containsKey("type") ? data.get("type") : "general";

            // 1. CHAT SUPPRESSION CHECK: If user is actively chatting with this sender, skip notification banner
            if ("chat".equalsIgnoreCase(type)) {
                String targetUserId = data.get("targetUserId");
                if (targetUserId != null && targetUserId.equals(activeChatUserId)) {
                    Log.d(TAG, "User currently in active chat with " + targetUserId + ". Suppressing notification.");
                    return;
                }
            }

            // 2. CALL SUPPRESSION CHECK: If user is already inside a live call
            if ("call".equalsIgnoreCase(type) && isCallScreenActive) {
                Log.d(TAG, "User currently in an active call. Suppressing incoming call push.");
                return;
            }

            // Ensure channels are created
            NotificationHelper.createNotificationChannels(this);

            // Build Target Deep Link Intent
            Intent intent = createTargetIntent(type, data);
            int notificationId = notificationCounter.incrementAndGet();

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            // Channel & Priority Routing
            String channelId;
            int priority;
            String category;

            if ("call".equalsIgnoreCase(type)) {
                channelId = NotificationHelper.CHANNEL_CALLS;
                priority = NotificationCompat.PRIORITY_MAX;
                category = NotificationCompat.CATEGORY_CALL;
            } else if ("chat".equalsIgnoreCase(type)) {
                channelId = NotificationHelper.CHANNEL_CHATS;
                priority = NotificationCompat.PRIORITY_HIGH;
                category = NotificationCompat.CATEGORY_MESSAGE;
            } else if ("marketing".equalsIgnoreCase(type)) {
                channelId = NotificationHelper.CHANNEL_MARKETING;
                priority = NotificationCompat.PRIORITY_DEFAULT;
                category = NotificationCompat.CATEGORY_PROMO;
            } else {
                channelId = NotificationHelper.CHANNEL_SOCIAL;
                priority = NotificationCompat.PRIORITY_DEFAULT;
                category = NotificationCompat.CATEGORY_SOCIAL;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notification_bell)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(priority)
                    .setCategory(category)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(this, R.color.accent_primary));

            if ("call".equalsIgnoreCase(type)) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (soundUri != null) {
                    builder.setSound(soundUri);
                }
                builder.setOngoing(true);
                builder.setVibrate(new long[]{0, 800, 300, 800, 300, 800});
                builder.setFullScreenIntent(pendingIntent, true);
            }

            // Asynchronously load avatar if provided, otherwise dispatch immediately
            String avatarUrl = data.get("targetUserAvatar");
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                avatarUrl = data.get("callerAvatar");
            }

            if (avatarUrl != null && !avatarUrl.isEmpty() && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))) {
                final int finalNotifId = notificationId;
                Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(avatarUrl)
                        .circleCrop()
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                builder.setLargeIcon(resource);
                                dispatchNotification(finalNotifId, builder.build());
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                dispatchNotification(finalNotifId, builder.build());
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                dispatchNotification(finalNotifId, builder.build());
                            }
                        });
            } else {
                dispatchNotification(notificationId, builder.build());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing FCM notification", e);
        }
    }

    private void dispatchNotification(int id, Notification notification) {
        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.notify(id, notification);
            Log.d(TAG, "✅ Notification dispatched successfully with ID: " + id);
        } catch (SecurityException se) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to display notification", e);
        }
    }

    private Intent createTargetIntent(String type, Map<String, String> data) {
        Intent intent;
        if ("chat".equalsIgnoreCase(type)) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("targetUserId", data.get("targetUserId"));
            intent.putExtra("targetUserName", data.get("targetUserName"));
            intent.putExtra("targetUserAvatar", data.get("targetUserAvatar"));
            intent.putExtra("chatId", data.get("chatId"));
        } else if ("call".equalsIgnoreCase(type)) {
            intent = new Intent(this, CallActivity.class);
            intent.putExtra("targetUserId", data.get("callerId"));
            intent.putExtra("targetUserName", data.get("callerName"));
            intent.putExtra("targetUserAvatar", data.get("callerAvatar"));
            intent.putExtra("isVideoCall", Boolean.parseBoolean(data.get("isVideo")));
            intent.putExtra("isCaller", false);
            intent.putExtra("isPrivateCall", true);
            intent.putExtra("roomName", data.get("roomName"));
        } else if ("realmeet_request".equalsIgnoreCase(type) || "realmeet_accepted".equalsIgnoreCase(type)) {
            intent = new Intent(this, RealMeetActivity.class);
        } else if ("friend_request".equalsIgnoreCase(type)) {
            intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("targetUserId", data.get("fromUserId"));
        } else {
            // General / Social / Marketing default opens Main Screen
            intent = new Intent(this, MainScreenActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }
}
