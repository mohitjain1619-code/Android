package com.mohitt.camverz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<Notification> notificationList;
    private final OnNotificationInteractionListener listener;

    public interface OnNotificationInteractionListener {
        void onNotificationDelete(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notificationList, OnNotificationInteractionListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        setNotificationText(holder, notification);
        holder.timestamp.setText(getFormattedTimestamp(notification.getTimestamp()));

        String avatarUrl = notification.getTriggeringUserAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            int avatarResId = context.getResources().getIdentifier(avatarUrl, "drawable", context.getPackageName());
            if (avatarResId != 0) {
                Glide.with(context).load(avatarResId).placeholder(R.drawable.ic_user_placeholder).into(holder.userAvatar);
            } else {
                Glide.with(context).load(avatarUrl).placeholder(R.drawable.ic_user_placeholder).into(holder.userAvatar);
            }
        } else {
            holder.userAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }

        holder.userAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", notification.getTriggeringUserId());
            context.startActivity(intent);
        });

        if (notification.getPostText() != null && !notification.getPostText().isEmpty()) {
            holder.postPreview.setVisibility(View.VISIBLE);
            holder.postPreview.setText(notification.getPostText());
        } else {
            holder.postPreview.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> handleNotificationClick(notification));

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Notification")
                    .setMessage("Are you sure you want to delete this notification?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (listener != null) {
                            listener.onNotificationDelete(notification);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void handleNotificationClick(Notification notification) {
        if ("profile_visit".equals(notification.getType()) || "follow".equals(notification.getType())) {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", notification.getTriggeringUserId());
            context.startActivity(intent);
        } else if ("comment".equals(notification.getType()) || "like".equals(notification.getType()) || "reply".equals(notification.getType())) {
            if (notification.getPostId() != null && !notification.getPostId().isEmpty()) {
                Intent intent = new Intent(context, CommentsActivity.class);
                intent.putExtra("postId", notification.getPostId());
                context.startActivity(intent);
            }
        }
    }

    private void setNotificationText(NotificationViewHolder holder, Notification notification) {
        String userName = notification.getTriggeringUserName() != null ? notification.getTriggeringUserName() : "Someone";
        String messageSuffix;

        switch (notification.getType()) {
            case "like":
                messageSuffix = " liked your post.";
                break;
            case "comment":
                messageSuffix = " commented on your post.";
                break;
            case "follow":
                messageSuffix = " started following you.";
                break;
            case "profile_visit":
                messageSuffix = " visited your profile.";
                break;
            case "reply":
                messageSuffix = " replied to your comment.";
                break;
            default:
                messageSuffix = " sent a notification.";
                break;
        }

        SpannableString spannableString = new SpannableString(userName + messageSuffix);

        ClickableSpan nameClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", notification.getTriggeringUserId());
                context.startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.WHITE);
                ds.setFakeBoldText(true);
            }
        };

        spannableString.setSpan(nameClickableSpan, 0, userName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ClickableSpan restClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                handleNotificationClick(notification);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.WHITE);
            }
        };

        if (spannableString.length() > userName.length()) {
            spannableString.setSpan(restClickableSpan, userName.length(), spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.notificationText.setText(spannableString);
        holder.notificationText.setMovementMethod(LinkMovementMethod.getInstance());
        holder.notificationText.setHighlightColor(Color.TRANSPARENT);
    }

    private String getFormattedTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "just now";
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userAvatar;
        TextView notificationText;
        TextView timestamp;
        TextView postPreview;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            notificationText = itemView.findViewById(R.id.notification_text);
            timestamp = itemView.findViewById(R.id.timestamp);
            postPreview = itemView.findViewById(R.id.post_preview);
        }
    }
}
