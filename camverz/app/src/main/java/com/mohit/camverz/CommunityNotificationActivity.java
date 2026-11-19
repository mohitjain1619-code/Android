package com.mohit.camverz;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityNotificationActivity extends BaseActivity {
    private static final String TAG = "CommunityNotifAct";

    private enum Tab { REAL_MEET, FANTASY, PARTY }
    private Tab activeTab = Tab.REAL_MEET;

    private TextView tabRealMeet, tabFantasy, tabParty;
    private RecyclerView recyclerView;
    private LinearLayout emptyView;

    private ApiService api;
    private Gson gson;

    private final List<CommunityNotification> allNotifications = new ArrayList<>();
    private final List<CommunityNotification> displayedList = new ArrayList<>();
    private CommunityNotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_community_notifications);

            View topHeader = findViewById(R.id.topHeader);
            if (topHeader != null) {
                applyWindowInsets(topHeader, null);
            }

            api = ApiClient.getInstance(this).getApi();
            gson = new Gson();

            tabRealMeet = findViewById(R.id.tabRealMeet);
            tabFantasy = findViewById(R.id.tabFantasy);
            tabParty = findViewById(R.id.tabParty);
            recyclerView = findViewById(R.id.notificationsRecyclerView);
            emptyView = findViewById(R.id.emptyView);

            View btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new CommunityNotificationAdapter(this, displayedList);
                recyclerView.setAdapter(adapter);
            }

            if (tabRealMeet != null) tabRealMeet.setOnClickListener(v -> switchTab(Tab.REAL_MEET));
            if (tabFantasy != null) tabFantasy.setOnClickListener(v -> switchTab(Tab.FANTASY));
            if (tabParty != null) tabParty.setOnClickListener(v -> switchTab(Tab.PARTY));

            loadNotifications();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    private void switchTab(Tab tab) {
        activeTab = tab;

        // Visual feedback
        if (tabRealMeet != null) {
            tabRealMeet.setBackgroundResource(tab == Tab.REAL_MEET ? R.drawable.bg_luxury_tab_selected : R.drawable.bg_luxury_tab_unselected);
            tabRealMeet.setTextColor(tab == Tab.REAL_MEET ? Color.BLACK : Color.parseColor("#8E8E93"));
        }

        if (tabFantasy != null) {
            tabFantasy.setBackgroundResource(tab == Tab.FANTASY ? R.drawable.bg_luxury_tab_selected : R.drawable.bg_luxury_tab_unselected);
            tabFantasy.setTextColor(tab == Tab.FANTASY ? Color.BLACK : Color.parseColor("#8E8E93"));
        }

        if (tabParty != null) {
            tabParty.setBackgroundResource(tab == Tab.PARTY ? R.drawable.bg_luxury_tab_selected : R.drawable.bg_luxury_tab_unselected);
            tabParty.setTextColor(tab == Tab.PARTY ? Color.BLACK : Color.parseColor("#8E8E93"));
        }

        filterDisplayedList();
    }

    private void loadNotifications() {
        if (api == null) return;
        api.getCommunityNotifications().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject body = response.body();
                        if (body.has("ok") && body.get("ok").getAsBoolean() && body.has("notifications") && !body.get("notifications").isJsonNull()) {
                            allNotifications.clear();
                            JsonArray arr = body.getAsJsonArray("notifications");
                            if (arr != null) {
                                for (JsonElement el : arr) {
                                    try {
                                        CommunityNotification notif = gson.fromJson(el, CommunityNotification.class);
                                        if (notif != null) {
                                            allNotifications.add(notif);
                                        }
                                    } catch (Exception itemErr) {
                                        Log.w(TAG, "Error deserializing notification item: " + el, itemErr);
                                    }
                                }
                            }
                            filterDisplayedList();
                            return;
                        }
                    }
                    filterDisplayedList();
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing notifications response", e);
                    filterDisplayedList();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error loading notifications", t);
                try {
                    Toast.makeText(CommunityNotificationActivity.this, getString(R.string.network_error_loading_alerts), Toast.LENGTH_SHORT).show();
                    filterDisplayedList();
                } catch (Exception ignored) {}
            }
        });
    }

    private void filterDisplayedList() {
        try {
            displayedList.clear();
            for (CommunityNotification n : allNotifications) {
                if (n == null) continue;
                String postType = n.getPostType() != null ? n.getPostType().trim() : "";
                String type = n.getType() != null ? n.getType().trim() : "";

                if (activeTab == Tab.REAL_MEET) {
                    // Match RealMeet category: explicit post_type, or default request/accepted types, or empty type fallback
                    if ("REAL_MEET".equalsIgnoreCase(postType)
                            || "realmeet_request".equalsIgnoreCase(type)
                            || "realmeet_accepted".equalsIgnoreCase(type)
                            || (postType.isEmpty() && !"party_announcement".equalsIgnoreCase(type) && !"PARTY".equalsIgnoreCase(postType) && !"FANTASY".equalsIgnoreCase(postType))) {
                        displayedList.add(n);
                    }
                } else if (activeTab == Tab.FANTASY) {
                    if ("FANTASY".equalsIgnoreCase(postType)) {
                        displayedList.add(n);
                    }
                } else if (activeTab == Tab.PARTY) {
                    if ("PARTY".equalsIgnoreCase(postType) || "party_announcement".equalsIgnoreCase(type)) {
                        displayedList.add(n);
                    }
                }
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            if (emptyView != null && recyclerView != null) {
                if (displayedList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering displayed list", e);
        }
    }
}
