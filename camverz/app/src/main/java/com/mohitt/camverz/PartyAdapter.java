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

        holder.tvHostNameAge.setText(post.getHostName() + ", " + post.getHostAge());
        holder.tvTargetGender.setText(post.getTargetGender());
        holder.tvCapacity.setText("👥 " + post.getCapacity() + " People Max");
        holder.tvPartyPurpose.setText(post.getPurpose());
        holder.tvPartyVenue.setText("📍 " + post.getVenue());
        holder.tvPartyTime.setText("⏰ " + post.getPartyTime());

        AvatarHelper.loadAvatar(context, post.getHostPhotoUrl(), post.getHostAvatar(), post.getHostName(), holder.ivHostAvatar);

        if (currentUserId != null && currentUserId.equalsIgnoreCase(post.getHostUserId())) {
            holder.btnJoinParty.setText("🗑️ Delete Party");
            holder.btnJoinParty.setOnClickListener(v -> {
                if (listener != null) listener.onDeletePartyClicked(post);
            });
        } else {
            holder.btnJoinParty.setText("Request Invite");
            holder.btnJoinParty.setOnClickListener(v -> {
                if (listener != null) listener.onJoinPartyClicked(post);
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
