package com.mohitt.camverz;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RealMeetAdapter extends RecyclerView.Adapter<RealMeetAdapter.ViewHolder> {

    public interface OnPostActionListener {
        void onConnectClicked(RealMeetPost post);
        void onDeleteClicked(RealMeetPost post);
    }

    private final Context context;
    private final List<RealMeetPost> postList;
    private final String currentUserId;
    private final OnPostActionListener listener;

    public RealMeetAdapter(Context context, List<RealMeetPost> postList, String currentUserId, OnPostActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_real_meet_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RealMeetPost post = postList.get(position);

        boolean isMale = post.getGender() == null || !post.getGender().toLowerCase().startsWith("f");
        boolean isVerified = isMale || post.isVerified();

        holder.tvNameAge.setText(TextHelper.getFormattedHeader(
                context,
                post.getUserName(),
                post.getGender(),
                post.getAge(),
                post.isPremium(),
                isVerified
        ));
        holder.tvCity.setText("📍 " + (post.getCity() != null ? post.getCity() : "Nearby"));

        if (post.isPremium()) {
            holder.itemView.setBackgroundResource(R.drawable.bg_community_hot_gradient);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_real_meet_gradient_card);
        }
        holder.tvPurpose.setText(post.getPurpose());
        holder.tvLocation.setText("🏢 " + post.getLocation());
        holder.tvTime.setText("⏰ " + post.getTime());
        holder.tvDescription.setText(post.getDescription());

        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                post.getCreatedAt(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        holder.tvTimeAgo.setText(relativeTime);

        AvatarHelper.loadAvatar(context, post.getPhotoUrl(), post.getUserAvatar(), post.getUserName(), holder.ivAvatar);

        RealMeetStore store = RealMeetStore.getInstance(context);
        boolean hasRequested = store.hasUserRequestedPost(currentUserId, post.getId());

        if (currentUserId != null && currentUserId.equalsIgnoreCase(post.getUserId())) {
            holder.btnConnect.setText("🗑️ Delete Post");
            holder.btnConnect.setBackgroundResource(R.drawable.bg_luxury_chip);
            holder.btnConnect.setTextColor(Color.WHITE);
            holder.btnConnect.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClicked(post);
            });
        } else if (hasRequested) {
            holder.btnConnect.setText("📩 Requested");
            holder.btnConnect.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
            holder.btnConnect.setTextColor(Color.parseColor("#8E8E93"));
            holder.btnConnect.setOnClickListener(v -> {
                android.widget.Toast.makeText(context, "You have already requested to connect for this post.", android.widget.Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.btnConnect.setText("⚡ Connect & Meet");
            holder.btnConnect.setBackgroundResource(R.drawable.bg_neon_amber_button);
            holder.btnConnect.setTextColor(Color.BLACK);
            holder.btnConnect.setOnClickListener(v -> {
                if (listener != null) listener.onConnectClicked(post);
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof RealMeetActivity) {
                ((RealMeetActivity) context).openFullPostDetailDialog(
                        post.getUserName(), post.getAge(), post.getCity(), post.getPurpose(), post.getLocation(), post.getTime(), post.getDescription(), post.getUserAvatar(), post.getUserId(), post.getId()
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
        ImageView ivAvatar;
        TextView tvNameAge, tvCity, tvTimeAgo, tvPurpose, tvLocation, tvTime, tvDescription, btnConnect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvNameAge = itemView.findViewById(R.id.tvNameAge);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}
