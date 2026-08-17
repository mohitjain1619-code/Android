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

public class MeetRequestAdapter extends RecyclerView.Adapter<MeetRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onStartCallClicked(RealMeetRequest request);
        void onOpenChatClicked(RealMeetRequest request);
        void onToggleAcceptClicked(RealMeetRequest request);
        void onDeleteRequestClicked(RealMeetRequest request);
    }

    private final Context context;
    private final List<RealMeetRequest> requestList;
    private final OnRequestActionListener listener;

    public MeetRequestAdapter(Context context, List<RealMeetRequest> requestList, OnRequestActionListener listener) {
        this.context = context;
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meet_request_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RealMeetRequest req = requestList.get(position);

        String genderBadge = " ♂️ ";
        String verifiedBadge = " ✔️";
        holder.tvApplicantNameAge.setText(req.getApplicantName() + genderBadge + req.getApplicantAge() + verifiedBadge);
        holder.tvApplicantCity.setText("📍 " + (req.getApplicantCity() != null ? req.getApplicantCity() : "Nearby"));
        holder.tvPostContext.setText("For: " + (req.getPostTitle() != null ? req.getPostTitle() : "Real Meet Post"));
        holder.tvRequestMessage.setText(req.getMessage());

        String pref = req.getContactPreference() != null ? req.getContactPreference() : "Private Video Call";
        boolean isVideoPref = pref.toLowerCase().contains("video");
        holder.tvContactPreference.setText(isVideoPref ? "🎥 Prefers Private Video Call" : "💬 Prefers Direct Chat");

        if (isVideoPref) {
            holder.btnStartCall.setVisibility(View.VISIBLE);
            holder.btnOpenChat.setVisibility(View.GONE);
        } else {
            holder.btnStartCall.setVisibility(View.GONE);
            holder.btnOpenChat.setVisibility(View.VISIBLE);
        }

        holder.tvStatusBadge.setText(req.getStatus());
        if ("ACCEPTED".equalsIgnoreCase(req.getStatus())) {
            holder.tvStatusBadge.setTextColor(Color.parseColor("#4ADE80")); // Green
            holder.btnToggleAccept.setText("✅ Accepted");
            holder.btnToggleAccept.setEnabled(false); // Locked once accepted
            holder.btnToggleAccept.setAlpha(0.8f);
        } else if ("DECLINED".equalsIgnoreCase(req.getStatus())) {
            holder.tvStatusBadge.setTextColor(Color.parseColor("#F87171")); // Red
            holder.btnToggleAccept.setText("❌ Declined");
            holder.btnToggleAccept.setEnabled(true);
            holder.btnToggleAccept.setAlpha(1.0f);
        } else {
            holder.tvStatusBadge.setTextColor(Color.parseColor("#E5E5EA"));
            holder.btnToggleAccept.setText("✅ Accept");
            holder.btnToggleAccept.setEnabled(true);
            holder.btnToggleAccept.setAlpha(1.0f);
        }

        AvatarHelper.loadAvatar(context, req.getApplicantPhotoUrl(), req.getApplicantAvatar(), req.getApplicantName(), holder.ivApplicantAvatar);

        holder.btnStartCall.setOnClickListener(v -> {
            if (listener != null) listener.onStartCallClicked(req);
        });

        holder.btnOpenChat.setOnClickListener(v -> {
            if (listener != null) listener.onOpenChatClicked(req);
        });

        holder.btnToggleAccept.setOnClickListener(v -> {
            if (listener != null && holder.btnToggleAccept.isEnabled()) {
                listener.onToggleAcceptClicked(req);
            }
        });

        if (holder.btnDeleteRequest != null) {
            holder.btnDeleteRequest.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteRequestClicked(req);
            });
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivApplicantAvatar;
        TextView tvApplicantNameAge, tvApplicantCity, tvStatusBadge, tvPostContext, tvRequestMessage, tvContactPreference, btnStartCall, btnOpenChat, btnToggleAccept, btnDeleteRequest;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivApplicantAvatar = itemView.findViewById(R.id.ivApplicantAvatar);
            tvApplicantNameAge = itemView.findViewById(R.id.tvApplicantNameAge);
            tvApplicantCity = itemView.findViewById(R.id.tvApplicantCity);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvPostContext = itemView.findViewById(R.id.tvPostContext);
            tvRequestMessage = itemView.findViewById(R.id.tvRequestMessage);
            tvContactPreference = itemView.findViewById(R.id.tvContactPreference);
            btnStartCall = itemView.findViewById(R.id.btnStartCall);
            btnOpenChat = itemView.findViewById(R.id.btnOpenChat);
            btnToggleAccept = itemView.findViewById(R.id.btnToggleAccept);
            btnDeleteRequest = itemView.findViewById(R.id.btnDeleteRequest);
        }
    }
}
