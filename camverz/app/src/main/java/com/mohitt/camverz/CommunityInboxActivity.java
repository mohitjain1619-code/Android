package com.mohitt.camverz;

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

import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

public class CommunityInboxActivity extends BaseActivity {

    private ImageView btnBack;
    private TextView tvActiveCount;
    private RecyclerView inboxRecyclerView;
    private LinearLayout emptyView;

    private RealMeetStore store;
    private TokenManager tokenManager;
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
        currentUserId = tokenManager.getUserId();

        btnBack = findViewById(R.id.btnBack);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        inboxRecyclerView = findViewById(R.id.inboxRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        btnBack.setOnClickListener(v -> finish());

        inboxRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityInboxAdapter(activeConnections, request -> {
            Intent intent = new Intent(CommunityInboxActivity.this, CommunityChatActivity.class);
            boolean isPoster = currentUserId != null && currentUserId.equalsIgnoreCase(request.getPosterUserId());
            String partnerId = isPoster ? request.getApplicantUserId() : request.getPosterUserId();
            String partnerName = isPoster ? request.getApplicantName() : "Community Poster";
            String partnerAvatar = isPoster ? request.getApplicantAvatar() : "";

            intent.putExtra("targetUserId", partnerId);
            intent.putExtra("targetUserName", partnerName);
            intent.putExtra("targetUserAvatar", partnerAvatar);
            intent.putExtra("contactPreference", request.getContactPreference());
            startActivity(intent);
        });
        inboxRecyclerView.setAdapter(adapter);

        loadAcceptedConnections();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAcceptedConnections();
    }

    private void loadAcceptedConnections() {
        activeConnections.clear();
        List<RealMeetRequest> allRequests = store.getMeetRequests();
        for (RealMeetRequest req : allRequests) {
            boolean isMyPoster = currentUserId != null && currentUserId.equalsIgnoreCase(req.getPosterUserId());
            boolean isMyApplicant = currentUserId != null && currentUserId.equalsIgnoreCase(req.getApplicantUserId());
            
            // Show accepted connections or requests involving the current user
            if ((isMyPoster || isMyApplicant) && ("ACCEPTED".equalsIgnoreCase(req.getStatus()) || isMyApplicant)) {
                activeConnections.add(req);
            }
        }

        if (activeConnections.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            inboxRecyclerView.setVisibility(View.GONE);
            tvActiveCount.setText("0 Chats");
        } else {
            emptyView.setVisibility(View.GONE);
            inboxRecyclerView.setVisibility(View.VISIBLE);
            tvActiveCount.setText(activeConnections.size() + " Active");
        }

        adapter.notifyDataSetChanged();
    }

    private static class CommunityInboxAdapter extends RecyclerView.Adapter<CommunityInboxAdapter.ViewHolder> {

        public interface OnItemClickListener {
            void onItemClick(RealMeetRequest request);
        }

        private final List<RealMeetRequest> list;
        private final OnItemClickListener listener;

        public CommunityInboxAdapter(List<RealMeetRequest> list, OnItemClickListener listener) {
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

            String genderBadge = " ♂️ ";
            String verifiedBadge = " ✔️";
            holder.tvInboxNameAge.setText(req.getApplicantName() + genderBadge + req.getApplicantAge() + verifiedBadge);
            holder.tvInboxSubtext.setText("📍 " + (req.getApplicantCity() != null ? req.getApplicantCity() : "Nearby") + " • For: " + (req.getPostTitle() != null ? req.getPostTitle() : "Community Meet"));
            holder.tvInboxLastMessage.setText(req.getMessage() != null && !req.getMessage().isEmpty() ? req.getMessage() : "Tap to open 1-on-1 community chat");

            String pref = req.getContactPreference() != null ? req.getContactPreference() : "Private Call";
            boolean isVideoPref = pref.toLowerCase().contains("video");
            holder.tvInboxPrefBadge.setText(isVideoPref ? "🎥 Video Call" : "💬 Direct Chat");

            AvatarHelper.loadAvatar(holder.itemView.getContext(), req.getApplicantPhotoUrl(), req.getApplicantAvatar(), req.getApplicantName(), holder.ivInboxAvatar);

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
