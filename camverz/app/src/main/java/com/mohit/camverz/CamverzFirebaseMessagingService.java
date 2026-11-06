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

            // 2. CALL SUPPRESSION & CANCELLATION CHECK
            if ("call_cancelled".equalsIgnoreCase(type) || "call_ended".equalsIgnoreCase(type)) {
                Log.d(TAG, "Incoming call cancelled/ended push received. Cancelling notification banner.");
                NotificationHelper.cancelIncomingCallNotification(this);
                return;
            }

            if ("call".equalsIgnoreCase(type) && isCallScreenActive) {
                Log.d(TAG, "User currently in an active call. Suppressing incoming call push.");
                return;
            }

            // Ensure channels are created
            NotificationHelper.createNotificationChannels(this);

            // Build Target Deep Link Intent
            Intent intent = createTargetIntent(type, data);
            int notificationId = "call".equalsIgnoreCase(type)
                    ? NotificationHelper.INCOMING_CALL_NOTIFICATION_ID
                    : notificationCounter.incrementAndGet();

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
                builder.setVibrate(new long[]{0, 800, 300, 800, 300, 800});
                builder.setFullScreenIntent(pendingIntent, true);
                builder.setOngoing(true);
                CallRingtoneManager.getInstance().startIncomingRinging(this);
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
            String uid = data.containsKey("userId") ? data.get("userId") : data.get("targetUserId");
            if (uid == null) uid = data.get("senderId");
            String name = data.containsKey("userName") ? data.get("userName") : data.get("targetUserName");
            if (name == null) name = data.get("senderName");
            String avatar = data.containsKey("userAvatar") ? data.get("userAvatar") : data.get("targetUserAvatar");
            if (avatar == null) avatar = data.get("senderAvatar");
            String chatId = data.get("chatId");

            intent.putExtra("userId", uid);
            intent.putExtra("targetUserId", uid);
            intent.putExtra("userName", name);
            intent.putExtra("targetUserName", name);
            intent.putExtra("userAvatar", avatar);
            intent.putExtra("targetUserAvatar", avatar);
            intent.putExtra("chatId", chatId);
        } else if ("call".equalsIgnoreCase(type)) {
            intent = new Intent(this, CallActivity.class);
            String callerId = data.containsKey("callerId") ? data.get("callerId") : data.get("targetUserId");
            if (callerId == null) callerId = data.get("peer");
            String callerName = data.containsKey("callerName") ? data.get("callerName") : data.get("targetUserName");
            String callerAvatar = data.containsKey("callerAvatar") ? data.get("callerAvatar") : data.get("targetUserAvatar");
            String room = data.containsKey("roomName") ? data.get("roomName") : data.get("room");
            boolean isVideo = true;
            if (data.containsKey("isVideo")) isVideo = Boolean.parseBoolean(data.get("isVideo"));
            if (data.containsKey("isVideoCall")) isVideo = Boolean.parseBoolean(data.get("isVideoCall"));

            intent.putExtra("peer", callerId);
            intent.putExtra("callerId", callerId);
            intent.putExtra("targetUserId", callerId);
            intent.putExtra("peerName", callerName);
            intent.putExtra("callerName", callerName);
            intent.putExtra("targetUserName", callerName);
            intent.putExtra("peerAvatar", callerAvatar);
            intent.putExtra("callerAvatar", callerAvatar);
            intent.putExtra("targetUserAvatar", callerAvatar);
            intent.putExtra("isVideoCall", isVideo);
            intent.putExtra("isCaller", false);
            intent.putExtra("isPrivateCall", true);
            intent.putExtra("roomName", room);
        } else if ("realmeet_request".equalsIgnoreCase(type) || "realmeet_accepted".equalsIgnoreCase(type)) {
            intent = new Intent(this, RealMeetActivity.class);
        } else if ("friend_request".equalsIgnoreCase(type)) {
            intent = new Intent(this, ProfileActivity.class);
            String fromUserId = data.get("fromUserId");
            if (fromUserId == null) fromUserId = data.get("triggeringUserId");
            intent.putExtra("userId", fromUserId);
            intent.putExtra("targetUserId", fromUserId);
        } else {
            // General / Social / Marketing default opens Main Screen
            intent = new Intent(this, MainScreenActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }
}
