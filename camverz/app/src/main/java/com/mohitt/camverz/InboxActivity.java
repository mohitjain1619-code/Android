package com.mohitt.camverz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InboxActivity extends BaseActivity {

    private static final String TAG = "InboxActivity";
    private RecyclerView inboxRecyclerView;
    private InboxAdapter inboxAdapter;
    private List<Conversation> conversationList;
    private List<Conversation> fullConversationList;
    private TextView noMessagesText;
    private FrameLayout notificationLayout;
    private TextView notificationBadge;
    private EditText searchEditText;
    private LinearLayout activeNowLayout, activeUsersContainer;
    
    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

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
        searchEditText = findViewById(R.id.search_edit_text);
        activeNowLayout = findViewById(R.id.active_now_layout);
        activeUsersContainer = findViewById(R.id.active_users_container);

        inboxRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        conversationList = new ArrayList<>();
        fullConversationList = new ArrayList<>();
        inboxAdapter = new InboxAdapter(this, conversationList);
        inboxRecyclerView.setAdapter(inboxAdapter);

        notificationLayout.setOnClickListener(v -> {
            Intent intent = new Intent(InboxActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        setupSearchFilter();
    }

    private void setupSearchFilter() {
        if (searchEditText == null) return;
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterConversations(String query) {
        if (query == null || query.trim().isEmpty()) {
            conversationList.clear();
            conversationList.addAll(fullConversationList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            conversationList.clear();
            for (Conversation conv : fullConversationList) {
                if ((conv.getName() != null && conv.getName().toLowerCase().contains(lowerQuery)) ||
                    (conv.getLastMessage() != null && conv.getLastMessage().toLowerCase().contains(lowerQuery))) {
                    conversationList.add(conv);
                }
            }
        }
        if (conversationList.isEmpty()) {
            noMessagesText.setVisibility(View.VISIBLE);
            inboxRecyclerView.setVisibility(View.GONE);
        } else {
            noMessagesText.setVisibility(View.GONE);
            inboxRecyclerView.setVisibility(View.VISIBLE);
        }
        inboxAdapter.notifyDataSetChanged();
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
                        fullConversationList.clear();
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
                                fullConversationList.add(conversation);
                            }
                        }
                        
                        conversationList.addAll(fullConversationList);
                        updateActiveUsersRow();

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

    private void updateActiveUsersRow() {
        if (activeNowLayout == null || activeUsersContainer == null) return;
        if (fullConversationList == null || fullConversationList.isEmpty()) {
            activeNowLayout.setVisibility(View.GONE);
            return;
        }

        activeNowLayout.setVisibility(View.VISIBLE);
        activeUsersContainer.removeAllViews();

        for (Conversation conv : fullConversationList) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_active_user_avatar, activeUsersContainer, false);
            CircleImageView avatarView = itemView.findViewById(R.id.active_user_avatar);
            TextView nameView = itemView.findViewById(R.id.active_user_name);

            nameView.setText(conv.getName());
            
            // Set image using Glide or avatar helper
            int resId = getResources().getIdentifier(conv.getProfileImageUrl(), "drawable", getPackageName());
            if (resId != 0) {
                avatarView.setImageResource(resId);
            } else {
                avatarView.setImageResource(R.drawable.ic_user_placeholder);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(InboxActivity.this, ChatActivity.class);
                intent.putExtra("targetUserId", conv.getUserId());
                intent.putExtra("targetUserName", conv.getName());
                intent.putExtra("targetUserAvatar", conv.getProfileImageUrl());
                startActivity(intent);
            });

            activeUsersContainer.addView(itemView);
        }
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
