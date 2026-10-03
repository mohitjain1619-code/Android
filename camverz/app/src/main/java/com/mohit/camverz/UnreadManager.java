package com.mohit.camverz;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Singleton manager for real-time unread message counts across the entire application.
 * Listens to socket events 24/7 (including during video calls in CallActivity)
 * and broadcasts updates locally so all UI elements update immediately.
 */
public class UnreadManager {

    private static final String TAG = "UnreadManager";
    public static final String ACTION_UNREAD_COUNT_CHANGED = "com.mohit.camverz.ACTION_UNREAD_COUNT_CHANGED";
    
    private static UnreadManager instance;
    private int unreadCount = 0;
    private Socket socket;

    private final Emitter.Listener newMessageListener = args -> {
        Log.d(TAG, "⚡ Socket triggered new_message event! Updating unread count in real-time...");
        fetchUnreadCount(null);
    };

    private UnreadManager() {}

    public static synchronized UnreadManager getInstance() {
        if (instance == null) {
            instance = new UnreadManager();
        }
        return instance;
    }

    public void init(Context context, Socket socketInstance) {
        if (socketInstance != null && socketInstance != this.socket) {
            if (this.socket != null) {
                try {
                    this.socket.off("new_message", newMessageListener);
                    this.socket.off("unread_count_update", newMessageListener);
                    this.socket.off("messages_read", newMessageListener);
                } catch (Exception e) {
                    Log.e(TAG, "Error removing old socket listeners", e);
                }
            }
            this.socket = socketInstance;
            try {
                this.socket.on("new_message", newMessageListener);
                this.socket.on("unread_count_update", newMessageListener);
                this.socket.on("messages_read", newMessageListener);
                Log.d(TAG, "✅ UnreadManager registered on socket listeners successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error registering socket listeners", e);
            }
        }
        fetchUnreadCount(context);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void fetchUnreadCount(Context context) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        TokenManager tokenManager = TokenManager.getInstance(appContext);
        if (!tokenManager.isLoggedIn()) return;

        ApiService api = ApiClient.getInstance(appContext).getApi();
        if (api == null) return;

        api.getUnreadCount().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        int count = 0;
                        if (data.has("count")) {
                            count = data.get("count").getAsInt();
                        } else if (data.has("unreadCount")) {
                            count = data.get("unreadCount").getAsInt();
                        }
                        unreadCount = count;
                        notifyUnreadCountChanged(appContext);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to fetch unread count", t);
            }
        });
    }

    public void setUnreadCount(int count, Context context) {
        this.unreadCount = count;
        notifyUnreadCountChanged(context);
    }

    public void notifyUnreadCountChanged(Context context) {
        Intent intent = new Intent(ACTION_UNREAD_COUNT_CHANGED);
        intent.putExtra("unread_count", unreadCount);
        if (context != null) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }
}
