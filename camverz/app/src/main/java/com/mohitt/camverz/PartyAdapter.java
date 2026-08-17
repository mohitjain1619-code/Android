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

public class PartyAdapter extends RecyclerView.Adapter<PartyAdapter.ViewHolder> {

    public interface OnPartyActionListener {
        void onJoinPartyClicked(PartyPost post);
        void onDeletePartyClicked(PartyPost post);
    }

    private final Context context;
    private final List<PartyPost> postList;
    private final String currentUserId;
    private final OnPartyActionListener listener;

    public PartyAdapter(Context context, List<PartyPost> postList, String currentUserId, OnPartyActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_party_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PartyPost post = postList.get(position);

        String genderIcon = " ♂️ ";
        String verifiedBadge = " ✔️";
        holder.tvHostNameAge.setText(post.getHostName() + genderIcon + post.getHostAge() + verifiedBadge);
        holder.tvTargetGender.setText(post.getTargetGender());
        holder.tvCapacity.setText("👥 " + post.getCapacity() + " People Max");
        holder.tvPartyPurpose.setText(post.getPurpose());
        holder.tvPartyVenue.setText("📍 " + post.getVenue());
        holder.tvPartyTime.setText("⏰ " + post.getPartyTime());

        AvatarHelper.loadAvatar(context, post.getHostPhotoUrl(), post.getHostAvatar(), post.getHostName(), holder.ivHostAvatar);

        RealMeetStore store = RealMeetStore.getInstance(context);
        boolean hasRequested = store.hasUserRequestedPost(currentUserId, post.getId());

        if (currentUserId != null && currentUserId.equalsIgnoreCase(post.getHostUserId())) {
            holder.btnJoinParty.setText("🗑️ Delete Party");
            holder.btnJoinParty.setBackgroundResource(R.drawable.bg_luxury_chip);
            holder.btnJoinParty.setTextColor(Color.WHITE);
            holder.btnJoinParty.setOnClickListener(v -> {
                if (listener != null) listener.onDeletePartyClicked(post);
            });
        } else if (hasRequested) {
            holder.btnJoinParty.setText("📩 Requested");
            holder.btnJoinParty.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
            holder.btnJoinParty.setTextColor(Color.parseColor("#8E8E93"));
            holder.btnJoinParty.setOnClickListener(v -> {
                android.widget.Toast.makeText(context, "You have already requested an invite for this party.", android.widget.Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.btnJoinParty.setText("🎉 Join Party");
            holder.btnJoinParty.setBackgroundResource(R.drawable.bg_neon_emerald_button);
            holder.btnJoinParty.setTextColor(Color.BLACK);
            holder.btnJoinParty.setOnClickListener(v -> {
                if (listener != null) listener.onJoinPartyClicked(post);
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof RealMeetActivity) {
                ((RealMeetActivity) context).openFullPostDetailDialog(
                        post.getHostName(), post.getHostAge(), post.getTargetGender(), post.getPurpose(), post.getVenue(), post.getPartyTime(), "Capacity: " + post.getCapacity() + " Guests Max.", post.getHostAvatar(), post.getHostUserId(), post.getId()
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
        ImageView ivHostAvatar;
        TextView tvTargetGender, tvHostNameAge, tvCapacity, tvPartyPurpose, tvPartyVenue, tvPartyTime, btnJoinParty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHostAvatar = itemView.findViewById(R.id.ivHostAvatar);
            tvTargetGender = itemView.findViewById(R.id.tvTargetGender);
            tvHostNameAge = itemView.findViewById(R.id.tvHostNameAge);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvPartyPurpose = itemView.findViewById(R.id.tvPartyPurpose);
            tvPartyVenue = itemView.findViewById(R.id.tvPartyVenue);
            tvPartyTime = itemView.findViewById(R.id.tvPartyTime);
            btnJoinParty = itemView.findViewById(R.id.btnJoinParty);
        }
    }
}
