package com.mohitt.camverz;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FantasyAdapter extends RecyclerView.Adapter<FantasyAdapter.ViewHolder> {

    public interface OnFantasyActionListener {
        void onFantasyConnectClicked(FantasyPost post);
        void onDeleteFantasyClicked(FantasyPost post);
    }

    private final Context context;
    private final List<FantasyPost> postList;
    private final String currentUserId;
    private final OnFantasyActionListener listener;

    public FantasyAdapter(Context context, List<FantasyPost> postList, String currentUserId, OnFantasyActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fantasy_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FantasyPost post = postList.get(position);

        String genderIcon = " ♂️ ";
        if (post.getGender() != null && post.getGender().toLowerCase().startsWith("f")) {
            genderIcon = " ♀️ ";
        }

        boolean isMale = post.getGender() == null || !post.getGender().toLowerCase().startsWith("f");
        boolean isVerified = isMale || post.isVerified();
        String verifiedBadge = isVerified ? " ✔️" : "";
        String crownBadge = post.isPremium() ? " 👑" : "";

        holder.tvFantasyNameAge.setText(post.getUserName() + " " + genderIcon + " " + post.getAge() + crownBadge + verifiedBadge);
        holder.tvStatusChip.setText(post.getRelationshipStatus());

        if (post.isPremium()) {
            holder.itemView.setBackgroundResource(R.drawable.bg_community_hot_gradient);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_fantasy_gradient_card);
        }
        holder.tvInterests.setText("✨ " + post.getInterests());
        holder.tvFantasyText.setText(post.getDescription());

        AvatarHelper.loadAvatar(context, post.getPhotoUrl(), post.getUserAvatar(), post.getUserName(), holder.ivFantasyAvatar);

        RealMeetStore store = RealMeetStore.getInstance(context);
        boolean hasRequested = store.hasUserRequestedPost(currentUserId, post.getId());

        if (currentUserId != null && currentUserId.equalsIgnoreCase(post.getUserId())) {
            holder.btnConnectFantasy.setText("🗑️ Delete Fantasy");
            holder.btnConnectFantasy.setBackgroundResource(R.drawable.bg_luxury_chip);
            holder.btnConnectFantasy.setTextColor(Color.WHITE);
            holder.btnConnectFantasy.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteFantasyClicked(post);
            });
        } else if (hasRequested) {
            holder.btnConnectFantasy.setText("📩 Requested");
            holder.btnConnectFantasy.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
            holder.btnConnectFantasy.setTextColor(Color.parseColor("#8E8E93"));
            holder.btnConnectFantasy.setOnClickListener(v -> {
                android.widget.Toast.makeText(context, "You have already requested to connect for this fantasy.", android.widget.Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.btnConnectFantasy.setText("✨ Secret Connect");
            holder.btnConnectFantasy.setBackgroundResource(R.drawable.bg_neon_magenta_button);
            holder.btnConnectFantasy.setTextColor(Color.WHITE);
            holder.btnConnectFantasy.setOnClickListener(v -> {
                if (listener != null) listener.onFantasyConnectClicked(post);
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof RealMeetActivity) {
                ((RealMeetActivity) context).openFullPostDetailDialog(
                        post.getUserName(), post.getAge(), post.getRelationshipStatus(), post.getInterests(), "Fantasy Vibe", "Anytime", post.getDescription(), post.getUserAvatar(), post.getUserId(), post.getId()
                );
            }
        });

        // Touch animation feedback
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFantasyAvatar;
        TextView tvFantasyNameAge, tvStatusChip, tvInterests, tvFantasyText, btnConnectFantasy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFantasyAvatar = itemView.findViewById(R.id.ivFantasyAvatar);
            tvFantasyNameAge = itemView.findViewById(R.id.tvFantasyNameAge);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);
            tvInterests = itemView.findViewById(R.id.tvInterests);
            tvFantasyText = itemView.findViewById(R.id.tvFantasyText);
            btnConnectFantasy = itemView.findViewById(R.id.btnConnectFantasy);
        }
    }
}
