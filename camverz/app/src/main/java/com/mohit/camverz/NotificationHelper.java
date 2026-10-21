package com.mohit.camverz;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * Production-Grade Notification Channels Manager for Camverz.
 * Ensures full compliance with Android 8.0+ (API 26+) and Android 13+ (API 33+) notification policies.
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    public static final String CHANNEL_CALLS = "camverz_calls";
    public static final String CHANNEL_CHATS = "camverz_chats";
    public static final String CHANNEL_SOCIAL = "camverz_social";
    public static final String CHANNEL_MARKETING = "camverz_marketing";

    public static void createNotificationChannels(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        try {
            // 1. URGENT CALL CHANNEL (Max Importance, Ringtone Sound, Heads-up)
            NotificationChannel callChannel = new NotificationChannel(
                    CHANNEL_CALLS,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            callChannel.setDescription("High-priority alerts for incoming 1-on-1 audio and video calls");
            callChannel.enableLights(true);
            callChannel.setLightColor(0xFF6366F1); // Brand Indigo
            callChannel.enableVibration(true);
            callChannel.setVibrationPattern(new long[]{0, 800, 300, 800, 300, 800});
            callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            Uri callSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (callSoundUri == null) {
                callSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            AudioAttributes callAudioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();
            callChannel.setSound(callSoundUri, callAudioAttributes);
            manager.createNotificationChannel(callChannel);

            // 2. CHAT MESSAGES CHANNEL (High Importance, Popup Banner)
            NotificationChannel chatChannel = new NotificationChannel(
                    CHANNEL_CHATS,
                    "Chat Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("Direct messages from other users");
            chatChannel.enableLights(true);
            chatChannel.setLightColor(0xFF6366F1);
            chatChannel.enableVibration(true);
            chatChannel.setVibrationPattern(new long[]{0, 250, 150, 250});
            chatChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            manager.createNotificationChannel(chatChannel);

            // 3. SOCIAL & COMMUNITY CHANNEL (Likes, Comments, RealMeet, Friend Requests)
            NotificationChannel socialChannel = new NotificationChannel(
                    CHANNEL_SOCIAL,
                    "Social & Community Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            socialChannel.setDescription("Notifications for likes, comments, RealMeet requests, and friends");
            socialChannel.enableLights(true);
            socialChannel.enableVibration(true);
            manager.createNotificationChannel(socialChannel);

            // 4. MARKETING & ANNOUNCEMENTS CHANNEL (Broadcast Campaigns, Offers)
            NotificationChannel marketingChannel = new NotificationChannel(
                    CHANNEL_MARKETING,
                    "Offers & Announcements",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            marketingChannel.setDescription("Important app updates, rewards, and promotions");
            marketingChannel.enableLights(false);
            marketingChannel.enableVibration(false);
            manager.createNotificationChannel(marketingChannel);

            Log.d(TAG, "✅ All 4 Camverz Notification Channels initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channels", e);
        }
    }
}
