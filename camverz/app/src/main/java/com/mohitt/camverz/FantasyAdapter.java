package com.mohitt.camverz;

import android.content.Context;
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

        holder.tvFantasyNameAge.setText(post.getUserName() + ", " + post.getAge());
        holder.tvStatusChip.setText(post.getRelationshipStatus());
        holder.tvInterests.setText("✨ " + post.getInterests());
        holder.tvFantasyText.setText(post.getDescription());

        AvatarHelper.loadAvatar(context, post.getPhotoUrl(), post.getUserAvatar(), post.getUserName(), holder.ivFantasyAvatar);

        if (currentUserId != null && currentUserId.equalsIgnoreCase(post.getUserId())) {
            holder.btnConnectFantasy.setText("🗑️ Delete Fantasy");
            holder.btnConnectFantasy.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteFantasyClicked(post);
            });
        } else {
            holder.btnConnectFantasy.setText("Send Whisper");
            holder.btnConnectFantasy.setOnClickListener(v -> {
                if (listener != null) listener.onFantasyConnectClicked(post);
            });
        }

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
