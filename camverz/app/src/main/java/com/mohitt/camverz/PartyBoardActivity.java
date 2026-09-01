package com.mohitt.camverz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PartyBoardActivity extends BaseActivity {

    private ImageView btnBack;
    private TextView tvPartyTitle;
    private LinearLayout layoutMembersSection, layoutPostAnnouncement;
    private RecyclerView rvMembers, rvAnnouncements;
    private EditText etAnnouncement;
    private FrameLayout btnPost;

    private String postId;
    private boolean isHost;
    private String partyTitle;

    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_board);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.toolbar), findViewById(R.id.layoutPostAnnouncement));

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        postId = getIntent().getStringExtra("postId");
        isHost = getIntent().getBooleanExtra("isHost", false);
        partyTitle = getIntent().getStringExtra("title");

        btnBack = findViewById(R.id.btnBack);
        tvPartyTitle = findViewById(R.id.tvPartyTitle);
        layoutMembersSection = findViewById(R.id.layoutMembersSection);
        layoutPostAnnouncement = findViewById(R.id.layoutPostAnnouncement);
        rvMembers = findViewById(R.id.rvMembers);
        rvAnnouncements = findViewById(R.id.rvAnnouncements);
        etAnnouncement = findViewById(R.id.etAnnouncement);
        btnPost = findViewById(R.id.btnPost);

        if (partyTitle != null) {
            tvPartyTitle.setText(partyTitle);
        }

        btnBack.setOnClickListener(v -> finish());

        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));

        if (isHost) {
            layoutMembersSection.setVisibility(View.VISIBLE);
            layoutPostAnnouncement.setVisibility(View.VISIBLE);

            rvMembers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            fetchPartyMembers();

            btnPost.setOnClickListener(v -> postAnnouncementMessage());
        }

        fetchAnnouncements();
    }

    private void fetchPartyMembers() {
        if (postId == null) return;

        api.getPartyMembers(postId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("members")) {
                        JsonArray membersArr = data.getAsJsonArray("members");
                        List<JsonObject> membersList = new ArrayList<>();
                        for (JsonElement el : membersArr) {
                            membersList.add(el.getAsJsonObject());
                        }
                        rvMembers.setAdapter(new MembersAdapter(PartyBoardActivity.this, membersList));
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(PartyBoardActivity.this, "Failed to load members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAnnouncements() {
        if (postId == null) return;

        api.getAnnouncements(postId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("announcements")) {
                        JsonArray annArr = data.getAsJsonArray("announcements");
                        List<JsonObject> annList = new ArrayList<>();
                        for (JsonElement el : annArr) {
                            annList.add(el.getAsJsonObject());
                        }
                        rvAnnouncements.setAdapter(new AnnouncementsAdapter(PartyBoardActivity.this, annList));
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(PartyBoardActivity.this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postAnnouncementMessage() {
        String text = etAnnouncement.getText().toString().trim();
        if (text.isEmpty() || postId == null) return;

        etAnnouncement.setText("");
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);

        api.postAnnouncement(postId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    fetchAnnouncements();
                    Toast.makeText(PartyBoardActivity.this, "Announcement posted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PartyBoardActivity.this, "Failed to post", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(PartyBoardActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.ViewHolder> {
        private final List<JsonObject> members;
        private final Context context;

        public MembersAdapter(Context context, List<JsonObject> members) {
            this.context = context;
            this.members = members;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_active_user_avatar, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JsonObject m = members.get(position);
            String name = m.has("name") ? m.get("name").getAsString() : "Member";
            String avatar = m.has("avatar") ? m.get("avatar").getAsString() : "";
            String photoUrl = m.has("photoUrl") && !m.get("photoUrl").isJsonNull() ? m.get("photoUrl").getAsString() : null;

            holder.tvName.setText(name);
            AvatarHelper.loadAvatar(context, photoUrl, avatar, name, holder.ivAvatar);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvName;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.active_user_avatar);
                tvName = itemView.findViewById(R.id.active_user_name);
            }
        }
    }

    private static class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder> {
        private final List<JsonObject> announcements;
        private final Context context;

        public AnnouncementsAdapter(Context context, List<JsonObject> announcements) {
            this.context = context;
            this.announcements = announcements;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(13);
            tv.setPadding(24, 18, 24, 18);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            tv.setLayoutParams(lp);
            tv.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
            tv.setTextColor(Color.WHITE);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JsonObject a = announcements.get(position);
            String text = a.has("text") ? a.get("text").getAsString() : "";
            holder.tvText.setText(text);
        }

        @Override
        public int getItemCount() {
            return announcements.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvText = (TextView) itemView;
            }
        }
    }
}
