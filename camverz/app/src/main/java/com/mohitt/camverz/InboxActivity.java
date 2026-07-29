package com.mohitt.camverz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InboxActivity extends BaseActivity {

    private static final String TAG = "InboxActivity";
    private RecyclerView inboxRecyclerView;
    private InboxAdapter inboxAdapter;
    private List<Conversation> conversationList;
    private TextView noMessagesText;
    private FrameLayout notificationLayout;
    private TextView notificationBadge;
    
    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.toolbar), findViewById(R.id.floating_menu_container));

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> finish());

        // Floating Menu Buttons
        findViewById(R.id.menu_home_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, MainScreenActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_explore_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, FeedActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_messages_btn).setOnClickListener(v -> {
            // Already on Messages
        });
        findViewById(R.id.menu_profile_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("userId", tokenManager.getUserId());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        inboxRecyclerView = findViewById(R.id.inboxRecyclerView);
        noMessagesText = findViewById(R.id.no_messages_text);
        notificationLayout = findViewById(R.id.notification_layout);
        notificationBadge = findViewById(R.id.notification_badge);
        inboxRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        conversationList = new ArrayList<>();
        inboxAdapter = new InboxAdapter(this, conversationList);
        inboxRecyclerView.setAdapter(inboxAdapter);

        notificationLayout.setOnClickListener(v -> {
            Intent intent = new Intent(InboxActivity.this, NotificationActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
        checkForUnreadNotifications();
    }

    private void loadConversations() {
        if (!tokenManager.isLoggedIn()) return;

        api.getChats().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        conversationList.clear();
                        if (data.has("chats")) {
                            JsonArray chatsArray = data.getAsJsonArray("chats");
                            for (JsonElement element : chatsArray) {
                                JsonObject chatObj = element.getAsJsonObject();
                                Conversation conversation = new Conversation();
                                conversation.setUserId(chatObj.has("targetUserId") ? chatObj.get("targetUserId").getAsString() : "");
                                conversation.setName(chatObj.has("targetUserName") ? chatObj.get("targetUserName").getAsString() : "Unknown");
                                conversation.setProfileImageUrl(chatObj.has("targetUserAvatar") ? chatObj.get("targetUserAvatar").getAsString() : "av1");
                                conversation.setLastMessage(chatObj.has("lastMessage") ? chatObj.get("lastMessage").getAsString() : "");
                                conversation.setLastActivity(chatObj.has("lastActivity") ? chatObj.get("lastActivity").getAsLong() : 0);
                                conversation.setUnread(chatObj.has("unread") && chatObj.get("unread").getAsBoolean());
                                conversationList.add(conversation);
                            }
                        }
                        
                        if (conversationList.isEmpty()) {
                            noMessagesText.setVisibility(View.VISIBLE);
                            inboxRecyclerView.setVisibility(View.GONE);
                        } else {
                            noMessagesText.setVisibility(View.GONE);
                            inboxRecyclerView.setVisibility(View.VISIBLE);
                            inboxAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to load chats", t);
                Toast.makeText(InboxActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkForUnreadNotifications() {
        if (!tokenManager.isLoggedIn()) return;

        api.getNotificationUnreadCount().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        int count = data.has("unreadCount") ? data.get("unreadCount").getAsInt() : 0;
                        updateNotificationBadge(count);
                    }
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching notification unread count", t);
            }
        });
    }

    private void updateNotificationBadge(int count) {
        if (count > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText(String.valueOf(count));
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }
}
