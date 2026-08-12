package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class NotificationActivity extends BaseActivity implements NotificationAdapter.OnNotificationInteractionListener {

    private static final String TAG = "NotificationActivity";
    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private TextView noNotificationsText;
    
    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

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
            startActivity(new Intent(this, InboxActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_profile_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("userId", tokenManager.getUserId());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        noNotificationsText = findViewById(R.id.no_notifications_text);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationList, this);
        notificationsRecyclerView.setAdapter(notificationAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadNotifications();
    }

    private void loadNotifications() {
        if (!tokenManager.isLoggedIn()) return;

        api.getNotifications(50, 0).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        notificationList.clear();
                        if (data.has("notifications")) {
                            JsonArray notifArray = data.getAsJsonArray("notifications");
                            for (JsonElement element : notifArray) {
                                JsonObject obj = element.getAsJsonObject();
                                Notification notif = new Notification();
                                notif.setNotificationId(obj.has("id") ? obj.get("id").getAsString() : "");
                                notif.setType(obj.has("type") ? obj.get("type").getAsString() : "");
                                notif.setTargetUserId(tokenManager.getUserId());
                                notif.setPostId(obj.has("postId") && !obj.get("postId").isJsonNull() ? obj.get("postId").getAsString() : "");
                                notif.setPostText(obj.has("postText") && !obj.get("postText").isJsonNull() ? obj.get("postText").getAsString() : "");
                                
                                // Parse nested triggeringUser object
                                if (obj.has("triggeringUser") && !obj.get("triggeringUser").isJsonNull()) {
                                    JsonObject trigUser = obj.getAsJsonObject("triggeringUser");
                                    notif.setTriggeringUserId(trigUser.has("id") && !trigUser.get("id").isJsonNull() ? trigUser.get("id").getAsString() : "");
                                    notif.setTriggeringUserName(trigUser.has("name") && !trigUser.get("name").isJsonNull() ? trigUser.get("name").getAsString() : "Someone");
                                    notif.setTriggeringUserAvatar(trigUser.has("avatar") && !trigUser.get("avatar").isJsonNull() ? trigUser.get("avatar").getAsString() : "");
                                    notif.setTriggeringUserPhotoUrl(trigUser.has("photoUrl") && !trigUser.get("photoUrl").isJsonNull() ? trigUser.get("photoUrl").getAsString() : "");
                                } else {
                                    notif.setTriggeringUserId("");
                                    notif.setTriggeringUserName("Someone");
                                    notif.setTriggeringUserAvatar("");
                                    notif.setTriggeringUserPhotoUrl("");
                                }

                                if (obj.has("friendRequestId") && !obj.get("friendRequestId").isJsonNull()) {
                                    notif.setFriendRequestId(obj.get("friendRequestId").getAsString());
                                }
                                if (obj.has("friendshipStatus") && !obj.get("friendshipStatus").isJsonNull()) {
                                    notif.setFriendshipStatus(obj.get("friendshipStatus").getAsString());
                                }

                                // Parse createdAt timestamp from ISO string
                                if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
                                    try {
                                        String dateStr = obj.get("createdAt").getAsString();
                                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                        java.util.Date parsedDate = sdf.parse(dateStr);
                                        if (parsedDate != null) {
                                            notif.setTimestamp(parsedDate.getTime());
                                        }
                                    } catch (Exception e) {
                                        try {
                                            String dateStr = obj.get("createdAt").getAsString();
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                            java.util.Date parsedDate = sdf.parse(dateStr);
                                            if (parsedDate != null) {
                                                notif.setTimestamp(parsedDate.getTime());
                                            }
                                        } catch (Exception ex) {
                                            notif.setTimestamp(System.currentTimeMillis());
                                        }
                                    }
                                } else {
                                    notif.setTimestamp(System.currentTimeMillis());
                                }
                                
                                notif.setRead(obj.has("read") && obj.get("read").getAsBoolean());
                                notificationList.add(notif);
                            }
                        }

                        if (notificationList.isEmpty()) {
                            noNotificationsText.setVisibility(View.VISIBLE);
                            notificationsRecyclerView.setVisibility(View.GONE);
                        } else {
                            noNotificationsText.setVisibility(View.GONE);
                            notificationsRecyclerView.setVisibility(View.VISIBLE);
                            notificationAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching notifications", t);
                Toast.makeText(NotificationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        markNotificationsAsRead();
    }

    private void markNotificationsAsRead() {
        if (!tokenManager.isLoggedIn()) return;
        
        api.markAllNotificationsRead().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    @Override
    public void onNotificationDelete(Notification notification) {
        // Not implemented in backend yet, so just remove from list
        notificationList.remove(notification);
        notificationAdapter.notifyDataSetChanged();
    }
}
