package com.mohit.camverz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.socket.client.Socket;

public class ProfileUpdateManager {

    private static final String TAG = "ProfileUpdateManager";
    public static final String ACTION_PROFILE_UPDATED = "com.mohit.camverz.ACTION_PROFILE_UPDATED";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_AVATAR = "avatar";

    public interface OnProfileUpdateListener {
        void onProfileUpdated(String userId, String name, String avatar);
    }

    public interface OnProfileUpdatedListener extends OnProfileUpdateListener {}

    private static ProfileUpdateManager instance;
    private final Context context;
    private final Map<OnProfileUpdateListener, BroadcastReceiver> listenerMap = new ConcurrentHashMap<>();

    public static synchronized ProfileUpdateManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileUpdateManager(context.getApplicationContext());
        }
        return instance;
    }

    private ProfileUpdateManager(Context context) {
        this.context = context;
    }

    public BroadcastReceiver registerListener(OnProfileUpdateListener listener) {
        if (listener == null) return null;
        BroadcastReceiver receiver = registerListener(context, listener);
        if (receiver != null) {
            listenerMap.put(listener, receiver);
        }
        return receiver;
    }

    public void unregisterListener(OnProfileUpdateListener listener) {
        if (listener == null) return;
        BroadcastReceiver receiver = listenerMap.remove(listener);
        if (receiver != null) {
            unregisterListener(context, receiver);
        }
    }

    /**
     * Broadcasts profile changes locally to all open screens AND emits Socket.IO event to backend.
     */
    public static void broadcastProfileUpdate(Context context, String userId, String name, String avatar) {
        if (context == null || userId == null) return;

        Log.d(TAG, "📢 Broadcasting profile update: userId=" + userId + " | name=" + name + " | avatar=" + avatar);

        // 1. Send Local Broadcast to update all active UI screens in this app process
        Intent intent = new Intent(ACTION_PROFILE_UPDATED);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_AVATAR, avatar);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);

        // 2. Emit real-time Socket.IO event to server so OTHER online users receive updates
        try {
            Socket socket = SocketManager.getInstance();
            if (socket != null && socket.connected()) {
                JSONObject data = new JSONObject();
                data.put("userId", userId);
                data.put("name", name);
                data.put("avatar", avatar);
                socket.emit("profile_updated", data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error emitting socket profile_updated", e);
        }
    }

    /**
     * Convenience method to register a BroadcastReceiver listening for profile updates.
     */
    public static BroadcastReceiver registerListener(Context context, OnProfileUpdateListener listener) {
        if (context == null || listener == null) return null;

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent != null && ACTION_PROFILE_UPDATED.equals(intent.getAction())) {
                    String userId = intent.getStringExtra(EXTRA_USER_ID);
                    String name = intent.getStringExtra(EXTRA_NAME);
                    String avatar = intent.getStringExtra(EXTRA_AVATAR);
                    listener.onProfileUpdated(userId, name, avatar);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
        return receiver;
    }

    /**
     * Unregisters the receiver safely.
     */
    public static void unregisterListener(Context context, BroadcastReceiver receiver) {
        if (context != null && receiver != null) {
            try {
                LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
    }
}
