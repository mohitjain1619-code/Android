package com.mohitt.camverz;

import android.graphics.Color;
import android.os.Bundle;
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
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityNotificationActivity extends BaseActivity {

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
        setContentView(R.layout.activity_community_notifications);

        applyWindowInsets(findViewById(R.id.topHeader), null);

        api = ApiClient.getInstance(this).getApi();
        gson = new Gson();

        tabRealMeet = findViewById(R.id.tabRealMeet);
        tabFantasy = findViewById(R.id.tabFantasy);
        tabParty = findViewById(R.id.tabParty);
        recyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityNotificationAdapter(this, displayedList);
        recyclerView.setAdapter(adapter);

        tabRealMeet.setOnClickListener(v -> switchTab(Tab.REAL_MEET));
        tabFantasy.setOnClickListener(v -> switchTab(Tab.FANTASY));
        tabParty.setOnClickListener(v -> switchTab(Tab.PARTY));

        loadNotifications();
    }

    private void switchTab(Tab tab) {
        activeTab = tab;

        // Visual feedback
        tabRealMeet.setBackgroundResource(tab == Tab.REAL_MEET ? R.drawable.bg_luxury_tab_selected : R.drawable.bg_luxury_tab_unselected);
        tabRealMeet.setTextColor(tab == Tab.REAL_MEET ? Color.BLACK : Color.parseColor("#8E8E93"));

        tabFantasy.setBackgroundResource(tab == Tab.FANTASY ? R.drawable.bg_luxury_tab_selected : R.drawable.bg_luxury_tab_unselected);
        tabFantasy.setTextColor(tab == Tab.FANTASY ? Color.BLACK : Color.parseColor("#8E8E93"));

        tabParty.setBackgroundResource(tab == Tab.PARTY ? R.drawable.bg_luxury_tab_selected : R.drawable.bg_luxury_tab_unselected);
        tabParty.setTextColor(tab == Tab.PARTY ? Color.BLACK : Color.parseColor("#8E8E93"));

        filterDisplayedList();
    }

    private void loadNotifications() {
        api.getCommunityNotifications().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("ok") && body.get("ok").getAsBoolean() && body.has("notifications")) {
                        allNotifications.clear();
                        JsonArray arr = body.getAsJsonArray("notifications");
                        for (JsonElement el : arr) {
                            allNotifications.add(gson.fromJson(el, CommunityNotification.class));
                        }
                        filterDisplayedList();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CommunityNotificationActivity.this, "Network error loading alerts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterDisplayedList() {
        displayedList.clear();
        for (CommunityNotification n : allNotifications) {
            String postType = n.getPostType() != null ? n.getPostType() : "";
            if (activeTab == Tab.REAL_MEET && "REAL_MEET".equalsIgnoreCase(postType)) {
                displayedList.add(n);
            } else if (activeTab == Tab.FANTASY && "FANTASY".equalsIgnoreCase(postType)) {
                displayedList.add(n);
            } else if (activeTab == Tab.PARTY && ("PARTY".equalsIgnoreCase(postType) || "party_announcement".equalsIgnoreCase(n.getType()))) {
                displayedList.add(n);
            }
        }
        adapter.notifyDataSetChanged();

        if (displayedList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
