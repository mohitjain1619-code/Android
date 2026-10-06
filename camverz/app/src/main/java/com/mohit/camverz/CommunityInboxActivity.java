package com.mohit.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CommunityInboxActivity extends BaseActivity {

    private ImageView btnBack;
    private TextView tvActiveCount;
    private RecyclerView inboxRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyView;

    private LinearLayout activeNowLayout;
    private LinearLayout activeUsersContainer;

    private RealMeetStore store;
    private TokenManager tokenManager;
    private ApiService api;
    private String currentUserId;
    private CommunityInboxAdapter adapter;
    private final List<RealMeetRequest> activeConnections = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_inbox);

        applyWindowInsets(findViewById(R.id.topInboxHeader), null);

        store = RealMeetStore.getInstance(this);
        tokenManager = TokenManager.getInstance(this);
        api = ApiClient.getInstance(this).getApi();
        currentUserId = tokenManager.getUserId();

        btnBack = findViewById(R.id.btnBack);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        inboxRecyclerView = findViewById(R.id.inboxRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);
        activeNowLayout = findViewById(R.id.active_now_layout);
        activeUsersContainer = findViewById(R.id.active_users_container);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.accent_primary, R.color.accent_cyan);
            swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.surface_dark);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                fetchServerRequests();
                loadAcceptedConnections();
                updateActiveUsersRow();
                UnreadManager.getInstance().fetchUnreadCount(CommunityInboxActivity.this);
            });
        }

        btnBack.setOnClickListener(v -> finish());

        inboxRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityInboxAdapter(currentUserId, activeConnections, request -> {
            Intent intent = new Intent(CommunityInboxActivity.this, ChatActivity.class);
            boolean isPoster = currentUserId != null && currentUserId.equalsIgnoreCase(request.getPosterUserId());
            String partnerId = isPoster ? request.getApplicantUserId() : request.getPosterUserId();
            String partnerName = isPoster ? request.getApplicantName() : (request.getPosterName() != null && !request.getPosterName().isEmpty() ? request.getPosterName() : getString(R.string.community_host));
            String partnerAvatar = isPoster ? request.getApplicantAvatar() : request.getPosterAvatar();
            String partnerPhotoUrl = isPoster ? request.getApplicantPhotoUrl() : request.getPosterPhotoUrl();

            intent.putExtra("userId", partnerId);
            intent.putExtra("userName", partnerName);
            intent.putExtra("userAvatar", partnerAvatar);
            intent.putExtra("userPhotoUrl", partnerPhotoUrl);
            startActivity(intent);
        });
        inboxRecyclerView.setAdapter(adapter);

        loadAcceptedConnections();
        fetchServerRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAcceptedConnections();
        fetchServerRequests();
        updateActiveUsersRow();
    }

    private void fetchServerRequests() {
        if (api == null || !tokenManager.isLoggedIn()) return;
        api.getRealMeetServerRequests().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("ok") && body.get("ok").getAsBoolean() && body.has("requests")) {
                        JsonArray arr = body.getAsJsonArray("requests");
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        List<RealMeetRequest> serverReqs = new ArrayList<>();
                        for (JsonElement el : arr) {
                            RealMeetRequest req = gson.fromJson(el, RealMeetRequest.class);
                            serverReqs.add(req);
                        }
                        store.saveMeetRequests(serverReqs);
                        runOnUiThread(() -> loadAcceptedConnections());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void updateActiveUsersRow() {
        if (activeNowLayout == null || activeUsersContainer == null) return;
        if (!tokenManager.isLoggedIn()) {
            activeNowLayout.setVisibility(View.GONE);
            return;
        }

        api.getOnlineFriends().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("friends")) {
                        JsonArray friendsArray = data.getAsJsonArray("friends");
                        if (friendsArray.size() > 0) {
                            activeNowLayout.setVisibility(View.VISIBLE);
                            activeUsersContainer.removeAllViews();

                            for (JsonElement element : friendsArray) {
                                JsonObject friendObj = element.getAsJsonObject();
                                String friendId = friendObj.has("id") ? friendObj.get("id").getAsString() : "";
                                String friendName = friendObj.has("name") ? friendObj.get("name").getAsString() : "";
                                String friendAvatar = friendObj.has("avatar") ? friendObj.get("avatar").getAsString() : "";
                                String friendPhotoUrl = friendObj.has("photoUrl") && !friendObj.get("photoUrl").isJsonNull() ? friendObj.get("photoUrl").getAsString() : "";

                                View itemView = LayoutInflater.from(CommunityInboxActivity.this).inflate(R.layout.item_active_user_avatar, activeUsersContainer, false);
                                ImageView avatarView = itemView.findViewById(R.id.active_user_avatar);
                                TextView nameView = itemView.findViewById(R.id.active_user_name);

                                String displayName = friendName;
                                if (displayName.contains(" ")) {
                                    displayName = displayName.substring(0, displayName.indexOf(" "));
                                }
                                if (displayName.length() > 8) {
                                    displayName = displayName.substring(0, 8) + "..";
                                }
                                nameView.setText(displayName);

                                AvatarHelper.loadAvatar(CommunityInboxActivity.this, friendPhotoUrl, friendAvatar, friendName, avatarView);

                                itemView.setOnClickListener(v -> {
                                    Intent intent = new Intent(CommunityInboxActivity.this, ChatActivity.class);
                                    intent.putExtra("userId", friendId);
                                    intent.putExtra("userName", friendName);
                                    intent.putExtra("userAvatar", friendAvatar);
                                    startActivity(intent);
                                });

                                activeUsersContainer.addView(itemView);
                            }
                        } else {
                            activeNowLayout.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                activeNowLayout.setVisibility(View.GONE);
            }
        });
    }

    private void loadAcceptedConnections() {
        activeConnections.clear();
        List<RealMeetRequest> allRequests = store.getMeetRequests();
        java.util.Map<String, RealMeetRequest> partnerMap = new java.util.LinkedHashMap<>();

        for (RealMeetRequest req : allRequests) {
            boolean isMyPoster = currentUserId != null && currentUserId.equalsIgnoreCase(req.getPosterUserId());
            boolean isMyApplicant = currentUserId != null && currentUserId.equalsIgnoreCase(req.getApplicantUserId());
            
            if ((isMyPoster || isMyApplicant) && ("ACCEPTED".equalsIgnoreCase(req.getStatus()) || isMyApplicant)) {
                String partnerId = isMyPoster ? req.getApplicantUserId() : req.getPosterUserId();
                if (partnerId == null || partnerId.trim().isEmpty() || partnerId.equalsIgnoreCase(currentUserId)) {
                    continue;
                }

                if (!partnerMap.containsKey(partnerId)) {
                    partnerMap.put(partnerId, req);
                } else {
                    RealMeetRequest existing = partnerMap.get(partnerId);
                    if (existing != null && req.getCreatedAt() > existing.getCreatedAt()) {
                        partnerMap.put(partnerId, req);
                    }
                }
            }
        }

        activeConnections.addAll(partnerMap.values());

        if (activeConnections.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            inboxRecyclerView.setVisibility(View.GONE);
            tvActiveCount.setText(getString(R.string.chats_count_plural, 0));
        } else {
            emptyView.setVisibility(View.GONE);
            inboxRecyclerView.setVisibility(View.VISIBLE);
            if (activeConnections.size() == 1) {
                tvActiveCount.setText(getString(R.string.chats_count_single));
            } else {
                tvActiveCount.setText(getString(R.string.chats_count_plural, activeConnections.size()));
            }
        }

        adapter.notifyDataSetChanged();
    }

    private static class CommunityInboxAdapter extends RecyclerView.Adapter<CommunityInboxAdapter.ViewHolder> {

        public interface OnItemClickListener {
            void onItemClick(RealMeetRequest request);
        }

        private final String currentUserId;
        private final List<RealMeetRequest> list;
        private final OnItemClickListener listener;

        public CommunityInboxAdapter(String currentUserId, List<RealMeetRequest> list, OnItemClickListener listener) {
            this.currentUserId = currentUserId;
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_inbox, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RealMeetRequest req = list.get(position);

            boolean isCurrentApplicant = currentUserId != null && currentUserId.equalsIgnoreCase(req.getApplicantUserId());
            
            String partnerName = isCurrentApplicant ? 
                    (req.getPosterName() != null && !req.getPosterName().isEmpty() ? req.getPosterName() : holder.itemView.getContext().getString(R.string.community_host)) :
                    (req.getApplicantName() != null && !req.getApplicantName().isEmpty() ? req.getApplicantName() : holder.itemView.getContext().getString(R.string.community_member));

            String partnerGender = isCurrentApplicant ? req.getPosterGender() : req.getApplicantGender();
            int partnerAge = isCurrentApplicant ? (req.getPosterAge() > 0 ? req.getPosterAge() : 22) : (req.getApplicantAge() > 0 ? req.getApplicantAge() : 22);
            boolean partnerVerified = isCurrentApplicant ? req.isPosterVerified() : req.isApplicantVerified();
            String partnerAvatar = isCurrentApplicant ? req.getPosterAvatar() : req.getApplicantAvatar();
            String partnerPhotoUrl = isCurrentApplicant ? req.getPosterPhotoUrl() : req.getApplicantPhotoUrl();
            String partnerCity = isCurrentApplicant ? holder.itemView.getContext().getString(R.string.nearby) : (req.getApplicantCity() != null ? req.getApplicantCity() : holder.itemView.getContext().getString(R.string.nearby));

            String genderBadge = " ♂️ ";
            if (partnerGender != null && partnerGender.toLowerCase().startsWith("f")) {
                genderBadge = " ♀️ ";
            }

            boolean isMale = partnerGender == null || !partnerGender.toLowerCase().startsWith("f");
            boolean isVerified = isMale || partnerVerified;
            String verifiedBadge = isVerified ? " ✔️" : "";

            holder.tvInboxNameAge.setText(partnerName + " " + genderBadge + " " + partnerAge + verifiedBadge);
            holder.tvInboxSubtext.setText("📍 " + partnerCity + " • " + holder.itemView.getContext().getString(R.string.for_prefix) + " " + (req.getPostTitle() != null ? req.getPostTitle() : holder.itemView.getContext().getString(R.string.community_meet)));
            
            String tapToOpenStr = holder.itemView.getContext().getString(R.string.tap_to_open_chat);
            holder.tvInboxLastMessage.setText(req.getMessage() != null && !req.getMessage().isEmpty() ? req.getMessage() : tapToOpenStr);

            String pref = req.getContactPreference() != null ? req.getContactPreference() : "Private Call";
            boolean isVideoPref = pref.toLowerCase().contains("video");
            holder.tvInboxPrefBadge.setText(isVideoPref ? holder.itemView.getContext().getString(R.string.video_call_pref) : holder.itemView.getContext().getString(R.string.direct_chat_pref));

            AvatarHelper.loadAvatar(holder.itemView.getContext(), partnerPhotoUrl, partnerAvatar, partnerName, holder.ivInboxAvatar);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(req);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivInboxAvatar;
            TextView tvInboxNameAge, tvInboxSubtext, tvInboxLastMessage, tvInboxPrefBadge;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivInboxAvatar = itemView.findViewById(R.id.ivInboxAvatar);
                tvInboxNameAge = itemView.findViewById(R.id.tvInboxNameAge);
                tvInboxSubtext = itemView.findViewById(R.id.tvInboxSubtext);
                tvInboxLastMessage = itemView.findViewById(R.id.tvInboxLastMessage);
                tvInboxPrefBadge = itemView.findViewById(R.id.tvInboxPrefBadge);
            }
        }
    }
}
