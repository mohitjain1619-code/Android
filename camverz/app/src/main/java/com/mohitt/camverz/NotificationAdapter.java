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
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        AvatarHelper.loadAvatar(context, notification.getTriggeringUserPhotoUrl(), notification.getTriggeringUserAvatar(), notification.getTriggeringUserName(), holder.userAvatar);

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

        if ("friend_request".equals(notification.getType()) && notification.getFriendRequestId() != null && "pending".equals(notification.getFriendshipStatus())) {
            holder.friendRequestActionsLayout.setVisibility(View.VISIBLE);
            holder.acceptButton.setEnabled(true);
            holder.rejectButton.setEnabled(true);

            holder.acceptButton.setOnClickListener(v -> {
                holder.acceptButton.setEnabled(false);
                holder.rejectButton.setEnabled(false);
                ApiClient.getInstance(context).getApi().acceptFriendRequest(notification.getFriendRequestId()).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            notification.setFriendshipStatus("accepted");
                            notifyItemChanged(holder.getAdapterPosition());
                        } else {
                            holder.acceptButton.setEnabled(true);
                            holder.rejectButton.setEnabled(true);
                            Toast.makeText(context, "Failed to accept request", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        holder.acceptButton.setEnabled(true);
                        holder.rejectButton.setEnabled(true);
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            holder.rejectButton.setOnClickListener(v -> {
                holder.acceptButton.setEnabled(false);
                holder.rejectButton.setEnabled(false);
                ApiClient.getInstance(context).getApi().rejectFriendRequest(notification.getFriendRequestId()).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            notification.setFriendshipStatus("rejected");
                            notifyItemChanged(holder.getAdapterPosition());
                        } else {
                            holder.acceptButton.setEnabled(true);
                            holder.rejectButton.setEnabled(true);
                            Toast.makeText(context, "Failed to reject request", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        holder.acceptButton.setEnabled(true);
                        holder.rejectButton.setEnabled(true);
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            holder.friendRequestActionsLayout.setVisibility(View.GONE);
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
            case "friend_request":
                if ("accepted".equals(notification.getFriendshipStatus())) {
                    messageSuffix = " sent you a request (Accepted).";
                } else if ("rejected".equals(notification.getFriendshipStatus())) {
                    messageSuffix = " sent you a request (Rejected).";
                } else {
                    messageSuffix = " sent you a request.";
                }
                break;
            case "friend_accepted":
                messageSuffix = " accepted your friend request.";
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
                ds.setColor(Color.parseColor("#54D6D2")); // Bright Teal/Cyan for username
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
                ds.setColor(Color.parseColor("#F4FAF9")); // Crisp Light Text for notification body
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
        LinearLayout friendRequestActionsLayout;
        androidx.appcompat.widget.AppCompatButton acceptButton;
        androidx.appcompat.widget.AppCompatButton rejectButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            notificationText = itemView.findViewById(R.id.notification_text);
            timestamp = itemView.findViewById(R.id.timestamp);
            postPreview = itemView.findViewById(R.id.post_preview);
            friendRequestActionsLayout = itemView.findViewById(R.id.friend_request_actions_layout);
            acceptButton = itemView.findViewById(R.id.accept_button);
            rejectButton = itemView.findViewById(R.id.reject_button);
        }
    }
}
