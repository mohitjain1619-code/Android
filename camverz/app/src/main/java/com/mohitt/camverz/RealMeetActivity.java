package com.mohitt.camverz;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RealMeetActivity extends BaseActivity {

    private enum Tab { REAL_MEET, FANTASY, PARTY, REQUESTS, PROFILE }
    private Tab currentTab = Tab.REAL_MEET;

    private boolean isCityFilterActive = false;
    private String searchQuery = "";

    private TokenManager tokenManager;
    private ApiService api;
    private RealMeetStore store;
    private Gson gson;
    private Socket socket;

    // Header & Search
    private LinearLayout btnReturnToVideo;
    private View btnHeaderRequests, btnHeaderInbox;
    private LinearLayout cityFilterContainer;
    private TextView chipFilterGlobal, chipFilterCity;
    private EditText etSearch;

    // Bottom Floating Dock Views
    private View dockTabMeet, dockTabFantasy, dockTabParty, dockTabProfile;
    private TextView tvTextMeet, tvTextFantasy, tvTextParty, tvTextProfile;
    private View fabCreate;

    // Content Views
    private RecyclerView recyclerView;
    private ScrollView profileContainer;
    private LinearLayout emptyView;
    private TextView tvEmptyText;

    // Profile Views
    private ImageView ivProfileAvatar;
    private TextView tvProfileNameAge, tvProfileCityGender, tvProfileBio;
    private RecyclerView profileRecyclerView;

    // User state loaded from profile
    private String currentUserId;
    private String currentUserName;
    private String currentUserAvatar;
    private String currentUserCity;
    private String currentUserGender;
    private int currentUserAge = 22;

    // Auto-refresh handler for live real-time sync
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_meet);

        applyWindowInsets(findViewById(R.id.top_realmeet_bar), findViewById(R.id.bottomDockNav));

        tokenManager = TokenManager.getInstance(this);
        api = ApiClient.getInstance(this).getApi();
        store = RealMeetStore.getInstance(this);
        gson = new Gson();
        socket = SocketManager.getInstance();

        currentUserId = tokenManager.getUserId();
        currentUserName = tokenManager.getUserName();
        currentUserAvatar = tokenManager.getUserAvatar();
        currentUserGender = tokenManager.getUserGender();

        btnReturnToVideo = findViewById(R.id.btnReturnToVideo);
        btnHeaderRequests = findViewById(R.id.btnHeaderRequests);
        btnHeaderInbox = findViewById(R.id.btnHeaderInbox);
        etSearch = findViewById(R.id.etSearch);

        dockTabMeet = findViewById(R.id.dockTabMeet);
        dockTabFantasy = findViewById(R.id.dockTabFantasy);
        dockTabParty = findViewById(R.id.dockTabParty);
        dockTabProfile = findViewById(R.id.dockTabProfile);
        fabCreate = findViewById(R.id.fabCreate);

        tvTextMeet = findViewById(R.id.tvTextMeet);
        tvTextFantasy = findViewById(R.id.tvTextFantasy);
        tvTextParty = findViewById(R.id.tvTextParty);
        tvTextProfile = findViewById(R.id.tvTextProfile);

        cityFilterContainer = findViewById(R.id.cityFilterContainer);
        chipFilterGlobal = findViewById(R.id.chipFilterGlobal);
        chipFilterCity = findViewById(R.id.chipFilterCity);

        recyclerView = findViewById(R.id.recyclerView);
        profileContainer = findViewById(R.id.profileContainer);
        emptyView = findViewById(R.id.emptyView);
        tvEmptyText = findViewById(R.id.tvEmptyText);

        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileNameAge = findViewById(R.id.tvProfileNameAge);
        tvProfileCityGender = findViewById(R.id.tvProfileCityGender);
        tvProfileBio = findViewById(R.id.tvProfileBio);
        profileRecyclerView = findViewById(R.id.profileRecyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        profileRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Return to Random Video Calling listener
        btnReturnToVideo.setOnClickListener(v -> finish());
        if (btnHeaderRequests != null) btnHeaderRequests.setOnClickListener(v -> switchTab(Tab.REQUESTS));
        if (btnHeaderInbox != null) btnHeaderInbox.setOnClickListener(v -> startActivity(new Intent(this, CommunityInboxActivity.class)));

        // Dock Tab click listeners
        if (dockTabMeet != null) dockTabMeet.setOnClickListener(v -> switchTab(Tab.REAL_MEET));
        if (dockTabFantasy != null) dockTabFantasy.setOnClickListener(v -> switchTab(Tab.FANTASY));
        if (dockTabParty != null) dockTabParty.setOnClickListener(v -> switchTab(Tab.PARTY));
        if (dockTabProfile != null) dockTabProfile.setOnClickListener(v -> switchTab(Tab.PROFILE));

        // Search bar watcher
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    loadCurrentTabFeed();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // City filter listeners
        chipFilterGlobal.setOnClickListener(v -> {
            isCityFilterActive = false;
            updateFilterChipsUI();
            loadCurrentTabFeed();
        });

        chipFilterCity.setOnClickListener(v -> {
            if (currentUserCity == null || currentUserCity.isEmpty()) {
                Toast.makeText(this, "Please set your city in your profile first", Toast.LENGTH_SHORT).show();
                return;
            }
            isCityFilterActive = true;
            updateFilterChipsUI();
            loadCurrentTabFeed();
        });

        // FAB listener
        fabCreate.setOnClickListener(v -> onFabClicked());

        setupSocketListeners();
        fetchUserProfileDetails();
        fetchFeedFromServer();
        loadCurrentTabFeed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAutoRefreshLoop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefreshLoop();
    }

    private void startAutoRefreshLoop() {
        stopAutoRefreshLoop();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchFeedFromServer();
                refreshHandler.postDelayed(this, 5000); // Poll server every 5 seconds
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    private void stopAutoRefreshLoop() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshRunnable = null;
        }
    }

    private void setupSocketListeners() {
        if (socket != null) {
            socket.on("realmeet-request-sent", args -> {
                if (args != null && args.length > 0) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        String posterId = obj.optString("posterUserId");
                        String applicantName = obj.optString("applicantName");
                        if (currentUserId != null && currentUserId.equalsIgnoreCase(posterId)) {
                            runOnUiThread(() -> {
                                Toast.makeText(RealMeetActivity.this, "📩 Live Meet Request from " + applicantName + "!", Toast.LENGTH_LONG).show();
                                fetchFeedFromServer();
                            });
                        }
                    } catch (Exception e) {}
                }
            });

            socket.on("realmeet-post-deleted", args -> {
                runOnUiThread(() -> fetchFeedFromServer());
            });
        }
    }

    private void fetchFeedFromServer() {
        api.getRealMeetFeed().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("ok") && body.get("ok").getAsBoolean()) {
                        if (body.has("realMeetPosts")) {
                            JsonArray arr = body.getAsJsonArray("realMeetPosts");
                            List<RealMeetPost> serverPosts = new ArrayList<>();
                            for (JsonElement el : arr) {
                                serverPosts.add(gson.fromJson(el, RealMeetPost.class));
                            }
                            store.setRealMeetPosts(serverPosts);
                        }
                        if (body.has("partyPosts")) {
                            JsonArray arr = body.getAsJsonArray("partyPosts");
                            List<PartyPost> serverParties = new ArrayList<>();
                            for (JsonElement el : arr) {
                                serverParties.add(gson.fromJson(el, PartyPost.class));
                            }
                            store.setPartyPosts(serverParties);
                        }
                        if (body.has("fantasyPosts")) {
                            JsonArray arr = body.getAsJsonArray("fantasyPosts");
                            List<FantasyPost> serverFantasies = new ArrayList<>();
                            for (JsonElement el : arr) {
                                serverFantasies.add(gson.fromJson(el, FantasyPost.class));
                            }
                            store.setFantasyPosts(serverFantasies);
                        }
                        runOnUiThread(() -> loadCurrentTabFeed());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });

        api.getRealMeetServerRequests().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("ok") && body.get("ok").getAsBoolean() && body.has("requests")) {
                        JsonArray arr = body.getAsJsonArray("requests");
                        List<RealMeetRequest> serverReqs = new ArrayList<>();
                        for (JsonElement el : arr) {
                            RealMeetRequest req = gson.fromJson(el, RealMeetRequest.class);
                            serverReqs.add(req);
                        }
                        store.saveMeetRequests(serverReqs);
                        runOnUiThread(() -> loadCurrentTabFeed());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void fetchUserProfileDetails() {
        if (currentUserId == null || currentUserId.isEmpty()) return;
        api.getMe().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("user")) {
                        JsonObject userObj = data.getAsJsonObject("user");
                        if (userObj.has("city") && !userObj.get("city").isJsonNull()) {
                            currentUserCity = userObj.get("city").getAsString();
                        }
                        if (userObj.has("dob") && !userObj.get("dob").isJsonNull()) {
                            currentUserAge = calculateAgeFromDob(userObj.get("dob").getAsString());
                        }
                        if (userObj.has("name") && !userObj.get("name").isJsonNull()) {
                            currentUserName = userObj.get("name").getAsString();
                        }
                        if (userObj.has("gender") && !userObj.get("gender").isJsonNull()) {
                            currentUserGender = userObj.get("gender").getAsString();
                        }
                        runOnUiThread(() -> {
                            updateProfileUI();
                            if (isCityFilterActive) loadCurrentTabFeed();
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private int calculateAgeFromDob(String dob) {
        if (dob == null || dob.isEmpty()) return 22;
        try {
            String[] parts = dob.split("/");
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1;
                int year = Integer.parseInt(parts[2]);
                Calendar today = Calendar.getInstance();
                Calendar dobCal = Calendar.getInstance();
                dobCal.set(year, month, day);
                int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
                if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                    age--;
                }
                return Math.max(18, age);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 22;
    }

    private void switchTab(Tab tab) {
        currentTab = tab;

        if (tvTextMeet != null) tvTextMeet.setTextColor(tab == Tab.REAL_MEET ? Color.WHITE : Color.parseColor("#8E8E93"));
        if (tvTextFantasy != null) tvTextFantasy.setTextColor(tab == Tab.FANTASY ? Color.WHITE : Color.parseColor("#8E8E93"));
        if (tvTextParty != null) tvTextParty.setTextColor(tab == Tab.PARTY ? Color.WHITE : Color.parseColor("#8E8E93"));
        if (tvTextProfile != null) tvTextProfile.setTextColor(tab == Tab.PROFILE ? Color.WHITE : Color.parseColor("#8E8E93"));

        cityFilterContainer.setVisibility(tab == Tab.REAL_MEET ? View.VISIBLE : View.GONE);
        profileContainer.setVisibility(tab == Tab.PROFILE ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(tab == Tab.PROFILE ? View.GONE : View.VISIBLE);

        if (tab == Tab.REQUESTS) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        }

        if (tab == Tab.PROFILE) {
            updateProfileUI();
        } else {
            loadCurrentTabFeed();
        }
    }

    private void updateFilterChipsUI() {
        chipFilterGlobal.setBackgroundResource(isCityFilterActive ? R.drawable.bg_luxury_tab_unselected : R.drawable.bg_luxury_tab_selected);
        chipFilterGlobal.setTextColor(isCityFilterActive ? Color.parseColor("#8E8E93") : Color.BLACK);

        String cityText = (currentUserCity != null && !currentUserCity.isEmpty()) ? "🏙️ In " + currentUserCity : "🏙️ In My City";
        chipFilterCity.setText(cityText);
        chipFilterCity.setBackgroundResource(isCityFilterActive ? R.drawable.bg_luxury_tab_selected : R.drawable.bg_luxury_tab_unselected);
        chipFilterCity.setTextColor(isCityFilterActive ? Color.BLACK : Color.parseColor("#8E8E93"));
    }

    private void loadCurrentTabFeed() {
        emptyView.setVisibility(View.GONE);
        if (currentTab == Tab.REAL_MEET) {
            List<RealMeetPost> allPosts = store.getRealMeetPosts();
            List<RealMeetPost> filtered = new ArrayList<>();
            for (RealMeetPost p : allPosts) {
                boolean cityMatches = !isCityFilterActive || (currentUserCity != null && currentUserCity.equalsIgnoreCase(p.getCity()));
                boolean searchMatches = searchQuery.isEmpty() ||
                        (p.getPurpose() != null && p.getPurpose().toLowerCase().contains(searchQuery)) ||
                        (p.getLocation() != null && p.getLocation().toLowerCase().contains(searchQuery)) ||
                        (p.getCity() != null && p.getCity().toLowerCase().contains(searchQuery)) ||
                        (p.getUserName() != null && p.getUserName().toLowerCase().contains(searchQuery));
                if (cityMatches && searchMatches) {
                    filtered.add(p);
                }
            }
            if (filtered.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                tvEmptyText.setText(isCityFilterActive ? "No Real Meet posts in " + currentUserCity + " yet." : "No Real Meet posts available right now.");
            }
            RealMeetAdapter adapter = new RealMeetAdapter(this, filtered, currentUserId, new RealMeetAdapter.OnPostActionListener() {
                @Override
                public void onConnectClicked(RealMeetPost post) {
                    openSendRequestModal(post.getId(), post.getPurpose() + " at " + post.getLocation(), post.getUserId(), post.getUserName());
                }

                @Override
                public void onDeleteClicked(RealMeetPost post) {
                    store.deleteRealMeetPost(post.getId());
                    api.deleteRealMeetServerPost(post.getId()).enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
                    loadCurrentTabFeed();
                    Toast.makeText(RealMeetActivity.this, "Post deleted", Toast.LENGTH_SHORT).show();
                }
            });
            recyclerView.setAdapter(adapter);

        } else if (currentTab == Tab.PARTY) {
            List<PartyPost> partyPosts = store.getPartyPosts();
            List<PartyPost> filtered = new ArrayList<>();
            for (PartyPost p : partyPosts) {
                boolean searchMatches = searchQuery.isEmpty() ||
                        (p.getPurpose() != null && p.getPurpose().toLowerCase().contains(searchQuery)) ||
                        (p.getVenue() != null && p.getVenue().toLowerCase().contains(searchQuery)) ||
                        (p.getHostName() != null && p.getHostName().toLowerCase().contains(searchQuery));
                if (searchMatches) {
                    filtered.add(p);
                }
            }
            if (filtered.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                tvEmptyText.setText("No parties hosted right now matching search.");
            }
            PartyAdapter adapter = new PartyAdapter(this, filtered, currentUserId, new PartyAdapter.OnPartyActionListener() {
                @Override
                public void onJoinPartyClicked(PartyPost post) {
                    openSendRequestModal(post.getId(), "Party: " + post.getPurpose(), post.getHostUserId(), post.getHostName());
                }

                @Override
                public void onDeletePartyClicked(PartyPost post) {
                    store.deletePartyPost(post.getId());
                    api.deleteRealMeetServerPost(post.getId()).enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
                    loadCurrentTabFeed();
                    Toast.makeText(RealMeetActivity.this, "Party deleted", Toast.LENGTH_SHORT).show();
                }
            });
            recyclerView.setAdapter(adapter);

        } else if (currentTab == Tab.FANTASY) {
            List<FantasyPost> fantasyPosts = store.getFantasyPosts();
            List<FantasyPost> filtered = new ArrayList<>();
            for (FantasyPost p : fantasyPosts) {
                boolean searchMatches = searchQuery.isEmpty() ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchQuery)) ||
                        (p.getInterests() != null && p.getInterests().toLowerCase().contains(searchQuery)) ||
                        (p.getRelationshipStatus() != null && p.getRelationshipStatus().toLowerCase().contains(searchQuery));
                if (searchMatches) {
                    filtered.add(p);
                }
            }
            if (filtered.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                tvEmptyText.setText("No fantasy posts matching search.");
            }
            FantasyAdapter adapter = new FantasyAdapter(this, filtered, currentUserId, new FantasyAdapter.OnFantasyActionListener() {
                @Override
                public void onFantasyConnectClicked(FantasyPost post) {
                    openSendRequestModal(post.getId(), "Fantasy: " + post.getInterests(), post.getUserId(), post.getUserName());
                }

                @Override
                public void onDeleteFantasyClicked(FantasyPost post) {
                    store.deleteFantasyPost(post.getId());
                    api.deleteRealMeetServerPost(post.getId()).enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
                    loadCurrentTabFeed();
                    Toast.makeText(RealMeetActivity.this, "Fantasy deleted", Toast.LENGTH_SHORT).show();
                }
            });
            recyclerView.setAdapter(adapter);

        } else if (currentTab == Tab.REQUESTS) {
            List<RealMeetRequest> allRequests = store.getMeetRequests();
            List<RealMeetRequest> myIncomingRequests = new ArrayList<>();
            for (RealMeetRequest r : allRequests) {
                if (currentUserId == null || r.getPosterUserId() == null || currentUserId.equalsIgnoreCase(r.getPosterUserId()) || r.getPosterUserId().isEmpty()) {
                    myIncomingRequests.add(r);
                }
            }
            if (myIncomingRequests.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                tvEmptyText.setText("No incoming meet requests right now.");
            }
            MeetRequestAdapter adapter = new MeetRequestAdapter(this, myIncomingRequests, new MeetRequestAdapter.OnRequestActionListener() {
                @Override
                public void onStartCallClicked(RealMeetRequest request) {
                    Intent intent = new Intent(RealMeetActivity.this, CommunityChatActivity.class);
                    intent.putExtra("targetUserId", request.getApplicantUserId());
                    intent.putExtra("targetUserName", request.getApplicantName());
                    intent.putExtra("targetUserAvatar", request.getApplicantAvatar());
                    intent.putExtra("contactPreference", "Private Video Call");
                    startActivity(intent);
                }

                @Override
                public void onOpenChatClicked(RealMeetRequest request) {
                    Intent intent = new Intent(RealMeetActivity.this, CommunityChatActivity.class);
                    intent.putExtra("targetUserId", request.getApplicantUserId());
                    intent.putExtra("targetUserName", request.getApplicantName());
                    intent.putExtra("targetUserAvatar", request.getApplicantAvatar());
                    intent.putExtra("contactPreference", request.getContactPreference());
                    startActivity(intent);
                }

                @Override
                public void onToggleAcceptClicked(RealMeetRequest request) {
                    String newStatus = "ACCEPTED".equalsIgnoreCase(request.getStatus()) ? "PENDING" : "ACCEPTED";
                    store.updateRequestStatus(request.getId(), newStatus);

                    Map<String, Object> body = new HashMap<>();
                    body.put("requestId", request.getId());
                    body.put("status", newStatus);
                    api.updateRealMeetServerRequestStatus(body).enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });

                    loadCurrentTabFeed();
                    Toast.makeText(RealMeetActivity.this, "Request status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onDeleteRequestClicked(RealMeetRequest request) {
                    store.deleteMeetRequest(request.getId());
                    loadCurrentTabFeed();
                    Toast.makeText(RealMeetActivity.this, "Request deleted", Toast.LENGTH_SHORT).show();
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }

    private void openSendRequestModal(String postId, String postTitle, String posterUserId, String posterName) {
        if (currentUserId != null && currentUserId.equalsIgnoreCase(posterUserId)) {
            Toast.makeText(this, "This is your own post!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (store.hasUserRequestedPost(currentUserId, postId)) {
            Toast.makeText(this, "Request already sent for this post!", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_meet_request, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        TextView tvDialogSub = dialogView.findViewById(R.id.tvDialogSub);
        TextView chipIce1 = dialogView.findViewById(R.id.chipIce1);
        TextView chipIce2 = dialogView.findViewById(R.id.chipIce2);
        TextView chipIce3 = dialogView.findViewById(R.id.chipIce3);
        EditText etRequestMessage = dialogView.findViewById(R.id.etRequestMessage);
        TextView btnPrefVideo = dialogView.findViewById(R.id.btnPrefVideo);
        TextView btnPrefChat = dialogView.findViewById(R.id.btnPrefChat);
        TextView btnCancelReq = dialogView.findViewById(R.id.btnCancelReq);
        TextView btnSendReq = dialogView.findViewById(R.id.btnSendReq);

        tvDialogSub.setText("Connecting with " + (posterName != null ? posterName : "Post Owner"));

        final String[] selectedPref = {"Private Video Call"};

        chipIce1.setOnClickListener(v -> etRequestMessage.setText(chipIce1.getText().toString()));
        chipIce2.setOnClickListener(v -> etRequestMessage.setText(chipIce2.getText().toString()));
        chipIce3.setOnClickListener(v -> etRequestMessage.setText(chipIce3.getText().toString()));

        btnPrefVideo.setOnClickListener(v -> {
            selectedPref[0] = "Private Video Call";
            btnPrefVideo.setBackgroundResource(R.drawable.bg_luxury_tab_selected);
            btnPrefVideo.setTextColor(Color.BLACK);
            btnPrefChat.setBackgroundResource(R.drawable.bg_luxury_tab_unselected);
            btnPrefChat.setTextColor(Color.parseColor("#8E8E93"));
        });

        btnPrefChat.setOnClickListener(v -> {
            selectedPref[0] = "Direct Chat";
            btnPrefChat.setBackgroundResource(R.drawable.bg_luxury_tab_selected);
            btnPrefChat.setTextColor(Color.BLACK);
            btnPrefVideo.setBackgroundResource(R.drawable.bg_luxury_tab_unselected);
            btnPrefVideo.setTextColor(Color.parseColor("#8E8E93"));
        });

        btnCancelReq.setOnClickListener(v -> dialog.dismiss());

        btnSendReq.setOnClickListener(v -> {
            String msg = etRequestMessage.getText().toString().trim();
            if (msg.isEmpty()) {
                msg = "Hey! I'm interested in your meet post.";
            }

            RealMeetRequest req = new RealMeetRequest(
                    UUID.randomUUID().toString(),
                    postId,
                    postTitle,
                    posterUserId,
                    currentUserId,
                    currentUserName != null ? currentUserName : "User",
                    currentUserAvatar,
                    "",
                    currentUserAge,
                    currentUserCity != null ? currentUserCity : "Nearby",
                    msg,
                    selectedPref[0],
                    "PENDING",
                    System.currentTimeMillis()
            );

            store.addMeetRequest(req);

            Map<String, Object> reqPayload = new HashMap<>();
            reqPayload.put("request", req);
            api.createRealMeetServerRequest(reqPayload).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {}
            });

            dialog.dismiss();
            Toast.makeText(this, "📩 Request sent to " + (posterName != null ? posterName : "post owner") + "!", Toast.LENGTH_LONG).show();
        });

        dialog.show();
    }

    public void openFullPostDetailDialog(String name, int age, String city, String title, String venue, String time, String description, String avatar, String posterUserId, String postId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_full_post, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        ImageView ivFullAvatar = dialogView.findViewById(R.id.ivFullAvatar);
        TextView tvFullNameAge = dialogView.findViewById(R.id.tvFullNameAge);
        TextView tvFullSubtext = dialogView.findViewById(R.id.tvFullSubtext);
        TextView tvFullTitle = dialogView.findViewById(R.id.tvFullTitle);
        TextView tvFullVenue = dialogView.findViewById(R.id.tvFullVenue);
        TextView tvFullTime = dialogView.findViewById(R.id.tvFullTime);
        TextView tvFullDescription = dialogView.findViewById(R.id.tvFullDescription);
        TextView btnCloseFullDialog = dialogView.findViewById(R.id.btnCloseFullDialog);
        TextView btnFullDialogAction = dialogView.findViewById(R.id.btnFullDialogAction);

        tvFullNameAge.setText(name + " ♂️ " + age + " ✔️");
        tvFullSubtext.setText("📍 " + (city != null ? city : "Nearby"));
        tvFullTitle.setText(title != null ? title : "Community Post");
        tvFullVenue.setText(venue != null ? "📍 " + venue : "📍 Nearby Venue");
        tvFullTime.setText(time != null ? "⏰ " + time : "⏰ Scheduled");
        tvFullDescription.setText(description != null ? description : "No additional details.");

        AvatarHelper.loadAvatar(this, null, avatar, name, ivFullAvatar);

        btnCloseFullDialog.setOnClickListener(v -> dialog.dismiss());

        boolean hasRequested = store.hasUserRequestedPost(currentUserId, postId);
        if (currentUserId != null && currentUserId.equalsIgnoreCase(posterUserId)) {
            btnFullDialogAction.setText("🗑️ Your Post");
            btnFullDialogAction.setBackgroundResource(R.drawable.bg_luxury_chip);
            btnFullDialogAction.setTextColor(Color.WHITE);
            btnFullDialogAction.setEnabled(false);
        } else if (hasRequested) {
            btnFullDialogAction.setText("📩 Request Sent");
            btnFullDialogAction.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
            btnFullDialogAction.setTextColor(Color.parseColor("#8E8E93"));
            btnFullDialogAction.setEnabled(false);
        } else {
            btnFullDialogAction.setText("⚡ Send Request");
            btnFullDialogAction.setOnClickListener(v -> {
                dialog.dismiss();
                openSendRequestModal(postId, title, posterUserId, name);
            });
        }

        dialog.show();
    }

    private void updateProfileUI() {
        tvProfileNameAge.setText((currentUserName != null ? currentUserName : "User") + ", " + currentUserAge);
        tvProfileCityGender.setText("📍 " + (currentUserCity != null ? currentUserCity : "N/A") + " • " + (currentUserGender != null ? currentUserGender : "Unspecified"));

        AvatarHelper.loadAvatar(this, null, currentUserAvatar, currentUserName, ivProfileAvatar);

        List<RealMeetPost> myMeetPosts = new ArrayList<>();
        for (RealMeetPost p : store.getRealMeetPosts()) {
            if (currentUserId != null && currentUserId.equalsIgnoreCase(p.getUserId())) {
                myMeetPosts.add(p);
            }
        }

        RealMeetAdapter profileAdapter = new RealMeetAdapter(this, myMeetPosts, currentUserId, new RealMeetAdapter.OnPostActionListener() {
            @Override
            public void onConnectClicked(RealMeetPost post) {}

            @Override
            public void onDeleteClicked(RealMeetPost post) {
                store.deleteRealMeetPost(post.getId());
                api.deleteRealMeetServerPost(post.getId()).enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
                updateProfileUI();
                Toast.makeText(RealMeetActivity.this, "Post deleted", Toast.LENGTH_SHORT).show();
            }
        });
        profileRecyclerView.setAdapter(profileAdapter);
    }

    private void onFabClicked() {
        if (currentTab == Tab.REAL_MEET) {
            checkAndOpenRealMeetDialog();
        } else if (currentTab == Tab.PARTY) {
            openPartyDialog();
        } else if (currentTab == Tab.FANTASY) {
            openFantasyDialog();
        } else {
            checkAndOpenRealMeetDialog();
        }
    }

    private void checkAndOpenRealMeetDialog() {
        RealMeetPost existingPost = store.getUserPostToday(currentUserId);
        if (existingPost != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Active Post Exists Today")
                    .setMessage("You already have an active Real Meet post today:\n\"" + existingPost.getPurpose() + "\" at " + existingPost.getLocation() + ".\n\nYou can only post 1 Real Meet per day. Would you like to replace your previous post?")
                    .setPositiveButton("Replace Post", (dialog, which) -> openRealMeetDialog(existingPost))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            openRealMeetDialog(null);
        }
    }

    private void openRealMeetDialog(RealMeetPost oldPostToReplace) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_real_meet, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        EditText etPurpose = dialogView.findViewById(R.id.etPurpose);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        LinearLayout btnPickTime = dialogView.findViewById(R.id.btnPickTime);
        TextView tvSelectedTime = dialogView.findViewById(R.id.tvSelectedTime);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        TextView tvCharCounter = dialogView.findViewById(R.id.tvCharCounter);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnPublish = dialogView.findViewById(R.id.btnPublish);

        Calendar nowCal = Calendar.getInstance();
        int curHour = nowCal.get(Calendar.HOUR_OF_DAY);
        int curMin = nowCal.get(Calendar.MINUTE);
        String initialAmPm = curHour >= 12 ? "PM" : "AM";
        int initialFormattedHour = curHour % 12;
        if (initialFormattedHour == 0) initialFormattedHour = 12;
        String initialTime = String.format(Locale.getDefault(), "Today at %d:%02d %s", initialFormattedHour, curMin, initialAmPm);

        final String[] selectedTime = {initialTime};
        tvSelectedTime.setText("⏰ " + initialTime);

        btnPickTime.setOnClickListener(v -> {
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(RealMeetActivity.this, (timePicker, selectedHour, selectedMinute) -> {
                String amPm = selectedHour >= 12 ? "PM" : "AM";
                int formattedHour = selectedHour % 12;
                if (formattedHour == 0) formattedHour = 12;
                selectedTime[0] = String.format(Locale.getDefault(), "Today at %d:%02d %s", formattedHour, selectedMinute, amPm);
                tvSelectedTime.setText("⏰ " + selectedTime[0]);
            }, hour, minute, false);
            mTimePicker.setTitle("Select Meeting Time");
            mTimePicker.show();
        });

        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCounter.setText(s.length() + " / 200");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPublish.setOnClickListener(v -> {
            String purpose = etPurpose.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (purpose.isEmpty()) {
                Toast.makeText(this, "Please enter meeting purpose", Toast.LENGTH_SHORT).show();
                return;
            }
            if (location.isEmpty()) {
                Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show();
                return;
            }
            if (description.isEmpty()) {
                Toast.makeText(this, "Please write a brief description", Toast.LENGTH_SHORT).show();
                return;
            }

            if (oldPostToReplace != null) {
                store.deleteRealMeetPost(oldPostToReplace.getId());
                api.deleteRealMeetServerPost(oldPostToReplace.getId()).enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
            }

            RealMeetPost newPost = new RealMeetPost(
                    UUID.randomUUID().toString(),
                    currentUserId,
                    currentUserName != null ? currentUserName : "User",
                    currentUserAvatar,
                    "",
                    currentUserAge,
                    currentUserCity != null ? currentUserCity : "Nearby",
                    purpose,
                    location,
                    selectedTime[0],
                    description,
                    System.currentTimeMillis()
            );

            store.addRealMeetPost(newPost);

            Map<String, Object> body = new HashMap<>();
            body.put("type", "REAL_MEET");
            body.put("post", newPost);
            api.createRealMeetServerPost(body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {}
            });

            dialog.dismiss();
            loadCurrentTabFeed();
            Toast.makeText(this, "✨ Real Meet post published!", Toast.LENGTH_LONG).show();
        });

        dialog.show();
    }

    private void openPartyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_party, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        EditText etPartyPurpose = dialogView.findViewById(R.id.etPartyPurpose);
        EditText etPartyVenue = dialogView.findViewById(R.id.etPartyVenue);
        EditText etPartyCapacity = dialogView.findViewById(R.id.etPartyCapacity);
        Spinner spinnerTargetGender = dialogView.findViewById(R.id.spinnerTargetGender);
        LinearLayout btnPickPartyTime = dialogView.findViewById(R.id.btnPickPartyTime);
        TextView tvSelectedPartyTime = dialogView.findViewById(R.id.tvSelectedPartyTime);
        TextView btnCancelParty = dialogView.findViewById(R.id.btnCancelParty);
        TextView btnPublishParty = dialogView.findViewById(R.id.btnPublishParty);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Everyone", "Female Only", "Couples Only", "Male Only"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTargetGender.setAdapter(adapter);

        Calendar nowPartyCal = Calendar.getInstance();
        int curPartyHour = nowPartyCal.get(Calendar.HOUR_OF_DAY);
        int curPartyMin = nowPartyCal.get(Calendar.MINUTE);
        String initialPartyAmPm = curPartyHour >= 12 ? "PM" : "AM";
        int initialPartyFormattedHour = curPartyHour % 12;
        if (initialPartyFormattedHour == 0) initialPartyFormattedHour = 12;
        String initialPartyTime = String.format(Locale.getDefault(), "Upcoming %d:%02d %s", initialPartyFormattedHour, curPartyMin, initialPartyAmPm);

        final String[] selectedTime = {initialPartyTime};
        tvSelectedPartyTime.setText("⏰ " + initialPartyTime);

        btnPickPartyTime.setOnClickListener(v -> {
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(RealMeetActivity.this, (timePicker, selectedHour, selectedMinute) -> {
                String amPm = selectedHour >= 12 ? "PM" : "AM";
                int formattedHour = selectedHour % 12;
                if (formattedHour == 0) formattedHour = 12;
                selectedTime[0] = String.format(Locale.getDefault(), "Upcoming %d:%02d %s", formattedHour, selectedMinute, amPm);
                tvSelectedPartyTime.setText("⏰ " + selectedTime[0]);
            }, hour, minute, false);
            mTimePicker.setTitle("Select Party Time");
            mTimePicker.show();
        });

        btnCancelParty.setOnClickListener(v -> dialog.dismiss());

        btnPublishParty.setOnClickListener(v -> {
            String purpose = etPartyPurpose.getText().toString().trim();
            String venue = etPartyVenue.getText().toString().trim();
            String capStr = etPartyCapacity.getText().toString().trim();
            String targetGender = spinnerTargetGender.getSelectedItem().toString();

            if (purpose.isEmpty() || venue.isEmpty() || capStr.isEmpty()) {
                Toast.makeText(this, "Please fill out all party details", Toast.LENGTH_SHORT).show();
                return;
            }

            int capacity = Integer.parseInt(capStr);

            PartyPost partyPost = new PartyPost(
                    UUID.randomUUID().toString(),
                    currentUserId,
                    currentUserName != null ? currentUserName : "User",
                    currentUserAvatar,
                    "",
                    currentUserAge,
                    venue,
                    purpose,
                    capacity,
                    targetGender,
                    selectedTime[0],
                    System.currentTimeMillis()
            );

            store.addPartyPost(partyPost);

            Map<String, Object> body = new HashMap<>();
            body.put("type", "PARTY");
            body.put("post", partyPost);
            api.createRealMeetServerPost(body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {}
            });

            dialog.dismiss();
            loadCurrentTabFeed();
            Toast.makeText(this, "🎉 Party event published!", Toast.LENGTH_LONG).show();
        });

        dialog.show();
    }

    private void openFantasyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_fantasy, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        Spinner spinnerStatus = dialogView.findViewById(R.id.spinnerStatus);
        EditText etInterests = dialogView.findViewById(R.id.etInterests);
        EditText etFantasyDescription = dialogView.findViewById(R.id.etFantasyDescription);
        TextView tvFantasyCharCounter = dialogView.findViewById(R.id.tvFantasyCharCounter);
        TextView btnCancelFantasy = dialogView.findViewById(R.id.btnCancelFantasy);
        TextView btnPublishFantasy = dialogView.findViewById(R.id.btnPublishFantasy);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Single", "Married", "Divorced", "Widowed"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        etFantasyDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvFantasyCharCounter.setText(s.length() + " / 200");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancelFantasy.setOnClickListener(v -> dialog.dismiss());

        btnPublishFantasy.setOnClickListener(v -> {
            String status = spinnerStatus.getSelectedItem().toString();
            String interests = etInterests.getText().toString().trim();
            String description = etFantasyDescription.getText().toString().trim();

            if (description.isEmpty()) {
                Toast.makeText(this, "Please enter your fantasy text", Toast.LENGTH_SHORT).show();
                return;
            }

            FantasyPost fantasyPost = new FantasyPost(
                    UUID.randomUUID().toString(),
                    currentUserId,
                    currentUserName != null ? currentUserName : "User",
                    currentUserAvatar,
                    "",
                    currentUserAge,
                    status,
                    description,
                    interests.isEmpty() ? "General" : interests,
                    System.currentTimeMillis()
            );

            store.addFantasyPost(fantasyPost);

            Map<String, Object> body = new HashMap<>();
            body.put("type", "FANTASY");
            body.put("post", fantasyPost);
            api.createRealMeetServerPost(body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {}
            });

            dialog.dismiss();
            loadCurrentTabFeed();
            Toast.makeText(this, "💭 Fantasy shared!", Toast.LENGTH_LONG).show();
        });

        dialog.show();
    }
}
