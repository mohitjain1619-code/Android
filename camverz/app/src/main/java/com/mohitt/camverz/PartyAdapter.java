package com.mohitt.camverz;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PartyAdapter extends RecyclerView.Adapter<PartyAdapter.ViewHolder> {

    public interface OnPartyActionListener {
        void onJoinPartyClicked(PartyPost post);
        void onDeletePartyClicked(PartyPost post);
        void onSavePartyClicked(PartyPost post);
    }

    private final Context context;
    private final List<PartyPost> postList;
    private final String currentUserId;
    private final OnPartyActionListener listener;
    private final RealMeetStore store;
    private final boolean isSavedList;

    public PartyAdapter(Context context, List<PartyPost> postList, String currentUserId, OnPartyActionListener listener) {
        this(context, postList, currentUserId, false, listener);
    }

    public PartyAdapter(Context context, List<PartyPost> postList, String currentUserId, boolean isSavedList, OnPartyActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.isSavedList = isSavedList;
        this.listener = listener;
        this.store = RealMeetStore.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isSavedList ? R.layout.item_party_card_small : R.layout.item_party_card;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PartyPost post = postList.get(position);

        boolean isMale = post.getGender() == null || !post.getGender().toLowerCase().startsWith("f");
        boolean isVerified = isMale || post.isVerified();

        if (isSavedList) {
            holder.tvHostNameAge.setText("By " + post.getHostName() + ", " + post.getHostAge() + " Yrs");
            if (holder.tvHostMetadata != null) {
                holder.tvHostMetadata.setVisibility(View.GONE);
            }
            if (holder.tvCapacity != null) {
                holder.tvCapacity.setVisibility(View.GONE);
            }
        } else {
            holder.tvHostNameAge.setText(TextHelper.getPostHeader(
                    context,
                    post.getHostName(),
                    null,
                    isVerified,
                    null
            ));
            
            if (holder.tvHostMetadata != null) {
                holder.tvHostMetadata.setText(TextHelper.getHostMetadata(
                        context,
                        post.getHostAge(),
                        post.getGender()
                ));
            }
            holder.tvTargetGender.setVisibility(View.GONE);

            SpannableStringBuilder capacityBuilder = new SpannableStringBuilder();
            capacityBuilder.append("👥 " + post.getCapacity() + " Max • ");
            int start = capacityBuilder.length();
            String target = post.getTargetGender() != null ? post.getTargetGender().toUpperCase() : "EVERYONE";
            capacityBuilder.append(target);
            if (target.contains("FEMALE")) {
                capacityBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#FF2D55")), start, capacityBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (target.contains("MALE")) {
                capacityBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#007AFF")), start, capacityBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                capacityBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#34C759")), start, capacityBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (holder.tvCapacity != null) {
                holder.tvCapacity.setText(capacityBuilder);
            }
        }

        if (post.isPremium()) {
            holder.itemView.setBackgroundResource(R.drawable.bg_community_hot_gradient);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_party_gradient_card);
        }

        holder.tvPartyPurpose.setText(post.getPurpose());
        holder.tvPartyVenue.setText("📍 " + post.getVenue());
        holder.tvPartyTime.setText("⏰ " + post.getPartyTime());

        AvatarHelper.loadAvatar(context, post.getHostPhotoUrl(), post.getHostAvatar(), post.getHostName(), holder.ivHostAvatar);

        boolean hasRequested = store.hasUserRequestedPost(currentUserId, post.getId());

        if (isSavedList) {
            holder.btnSaveParty.setVisibility(View.VISIBLE);
            if (store.isPartySaved(post.getId())) {
                holder.btnSaveParty.setImageResource(R.drawable.ic_bookmark_selected);
            } else {
                holder.btnSaveParty.setImageResource(R.drawable.ic_bookmark_unselected);
            }

            holder.btnSaveParty.setOnClickListener(v -> {
                if (listener != null) listener.onSavePartyClicked(post);
            });
            
            if (holder.btnJoinParty != null) {
                holder.btnJoinParty.setVisibility(View.GONE);
            }
        } else {
            if (currentUserId != null && currentUserId.equalsIgnoreCase(post.getHostUserId())) {
                holder.btnJoinParty.setText("🗑️ Delete Party");
                holder.btnJoinParty.setBackgroundResource(R.drawable.bg_luxury_chip);
                holder.btnJoinParty.setTextColor(Color.WHITE);
                holder.btnJoinParty.setOnClickListener(v -> {
                    if (listener != null) listener.onDeletePartyClicked(post);
                });
                holder.btnSaveParty.setVisibility(View.GONE); // Host doesn't need to save their own party
            } else {
                holder.btnSaveParty.setVisibility(View.VISIBLE);
                if (store.isPartySaved(post.getId())) {
                    holder.btnSaveParty.setImageResource(R.drawable.ic_bookmark_selected);
                } else {
                    holder.btnSaveParty.setImageResource(R.drawable.ic_bookmark_unselected);
                }

                holder.btnSaveParty.setOnClickListener(v -> {
                    if (listener != null) listener.onSavePartyClicked(post);
                });

                if (hasRequested) {
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
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof RealMeetActivity) {
                ((RealMeetActivity) context).openFullPostDetailDialog(
                        post.getHostName(), post.getHostAge(), null, post.getPurpose(), post.getVenue(), post.getPartyTime(), "Capacity: " + post.getCapacity() + " Guests Max.", post.getHostAvatar(), post.getHostUserId(), post.getId(),
                        post.getGender(), isVerified, null, post.getTargetGender(), null
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
        ImageView ivHostAvatar, btnSaveParty;
        TextView tvTargetGender, tvHostNameAge, tvHostMetadata, tvCapacity, tvPartyPurpose, tvPartyVenue, tvPartyTime, btnJoinParty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHostAvatar = itemView.findViewById(R.id.ivHostAvatar);
            tvTargetGender = itemView.findViewById(R.id.tvTargetGender);
            tvHostNameAge = itemView.findViewById(R.id.tvHostNameAge);
            tvHostMetadata = itemView.findViewById(R.id.tvHostMetadata);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvPartyPurpose = itemView.findViewById(R.id.tvPartyPurpose);
            tvPartyVenue = itemView.findViewById(R.id.tvPartyVenue);
            tvPartyTime = itemView.findViewById(R.id.tvPartyTime);
            btnJoinParty = itemView.findViewById(R.id.btnJoinParty);
            btnSaveParty = itemView.findViewById(R.id.btnSaveParty);
        }
    }
}
