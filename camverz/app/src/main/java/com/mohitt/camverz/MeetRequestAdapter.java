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

        holder.tvApplicantNameAge.setText(req.getApplicantName() + ", " + req.getApplicantAge());
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
        } else if ("DECLINED".equalsIgnoreCase(req.getStatus())) {
            holder.tvStatusBadge.setTextColor(Color.parseColor("#F87171")); // Red
            holder.btnToggleAccept.setText("❌ Declined");
        } else {
            holder.tvStatusBadge.setTextColor(Color.parseColor("#E5E5EA"));
            holder.btnToggleAccept.setText("✅ Accept");
        }

        AvatarHelper.loadAvatar(context, req.getApplicantPhotoUrl(), req.getApplicantAvatar(), req.getApplicantName(), holder.ivApplicantAvatar);

        holder.btnStartCall.setOnClickListener(v -> {
            if (listener != null) listener.onStartCallClicked(req);
        });

        holder.btnOpenChat.setOnClickListener(v -> {
            if (listener != null) listener.onOpenChatClicked(req);
        });

        holder.btnToggleAccept.setOnClickListener(v -> {
            if (listener != null) listener.onToggleAcceptClicked(req);
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
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivApplicantAvatar;
        TextView tvApplicantNameAge, tvApplicantCity, tvStatusBadge, tvPostContext, tvRequestMessage, tvContactPreference, btnStartCall, btnOpenChat, btnToggleAccept;

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
        }
    }
}
