package com.mohit.camverz;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityNotificationAdapter extends RecyclerView.Adapter<CommunityNotificationAdapter.ViewHolder> {

    private final Context context;
    private final List<CommunityNotification> list;

    public CommunityNotificationAdapter(Context context, List<CommunityNotification> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_community_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityNotification notification = list.get(position);

        // Bind triggering user details
        CommunityNotification.TriggeringUser user = notification.getTriggeringUser();
        String someoneStr = context.getString(R.string.someone);
        String userName = (user != null && user.getName() != null && !user.getName().trim().isEmpty())
                ? user.getName().trim()
                : someoneStr;
        String avatar = user != null ? user.getAvatar() : "";
        String photoUrl = user != null ? user.getPhotoUrl() : "";

        AvatarHelper.loadAvatar(context, photoUrl, avatar, userName, holder.ivUserAvatar);

        // Format message with fail-safe fallbacks
        String text = "";
        String title = (notification.getPostTitle() != null && !notification.getPostTitle().trim().isEmpty())
                ? notification.getPostTitle().trim()
                : "";

        String type = notification.getType() != null ? notification.getType() : "";
        if ("realmeet_request".equalsIgnoreCase(type)) {
            text = context.getString(R.string.notif_realmeet_request, userName, title);
        } else if ("realmeet_accepted".equalsIgnoreCase(type)) {
            text = context.getString(R.string.notif_realmeet_accepted, userName, title);
        } else if ("party_announcement".equalsIgnoreCase(type) || "party".equalsIgnoreCase(type)) {
            text = context.getString(R.string.notif_party_announcement, userName, title);
        } else {
            text = context.getString(R.string.notif_community_generic, userName, title);
        }

        SpannableString ss = new SpannableString(text);
        if (text.startsWith(userName)) {
            ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, userName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFFFFF")), 0, userName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.tvNotificationMessage.setText(ss);

        // Format timestamp
        holder.tvNotificationTimestamp.setText(getFormattedTimestamp(notification.getCreatedAt()));

        // Request connection actions
        if ("realmeet_request".equals(notification.getType()) && "PENDING".equals(notification.getFriendshipStatus()) && notification.getFriendRequestId() != null) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnAccept.setEnabled(true);
            holder.btnReject.setEnabled(true);

            holder.btnAccept.setOnClickListener(v -> {
                holder.btnAccept.setEnabled(false);
                holder.btnReject.setEnabled(false);

                Map<String, Object> body = new HashMap<>();
                body.put("requestId", notification.getFriendRequestId());
                body.put("status", "ACCEPTED");

                ApiClient.getInstance(context).getApi().updateRealMeetServerRequestStatus(body).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            notification.setFriendshipStatus("ACCEPTED");
                            notifyItemChanged(holder.getAdapterPosition());
                            Toast.makeText(context, context.getString(R.string.request_accepted_toast), Toast.LENGTH_SHORT).show();
                        } else {
                            holder.btnAccept.setEnabled(true);
                            holder.btnReject.setEnabled(true);
                            Toast.makeText(context, context.getString(R.string.failed_update_status), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        holder.btnAccept.setEnabled(true);
                        holder.btnReject.setEnabled(true);
                        Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
            });

            holder.btnReject.setOnClickListener(v -> {
                holder.btnAccept.setEnabled(false);
                holder.btnReject.setEnabled(false);

                Map<String, Object> body = new HashMap<>();
                body.put("requestId", notification.getFriendRequestId());
                body.put("status", "REJECTED");

                ApiClient.getInstance(context).getApi().updateRealMeetServerRequestStatus(body).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            notification.setFriendshipStatus("REJECTED");
                            notifyItemChanged(holder.getAdapterPosition());
                            Toast.makeText(context, context.getString(R.string.request_declined_toast), Toast.LENGTH_SHORT).show();
                        } else {
                            holder.btnAccept.setEnabled(true);
                            holder.btnReject.setEnabled(true);
                            Toast.makeText(context, context.getString(R.string.failed_to_decline), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        holder.btnAccept.setEnabled(true);
                        holder.btnReject.setEnabled(true);
                        Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String getFormattedTimestamp(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60000) return context.getString(R.string.time_just_now);
        long mins = diff / 60000;
        if (mins < 60) return mins + " " + context.getString(R.string.time_mins_ago);
        long hours = mins / 60;
        if (hours < 24) return hours + " " + context.getString(R.string.time_hours_ago);
        return (hours / 24) + " " + context.getString(R.string.time_days_ago);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvNotificationMessage, tvNotificationTimestamp;
        LinearLayout layoutActions;
        TextView btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationTimestamp = itemView.findViewById(R.id.tvNotificationTimestamp);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
