package com.mohitt.camverz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mohitt.camverz.api.TokenManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private final Context context;
    private final List<Message> messages;
    private String receiverAvatar;
    private String receiverPhotoUrl;
    private TokenManager tokenManager;

    public ChatAdapter(Context context, List<Message> messages, String receiverAvatar, String receiverPhotoUrl) {
        this.context = context;
        this.messages = messages;
        this.receiverAvatar = receiverAvatar;
        this.receiverPhotoUrl = receiverPhotoUrl;
        this.tokenManager = TokenManager.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == MSG_TYPE_RIGHT) ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.messageText.setText(message.getMessage());

        if (message.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(new Date(message.getTimestamp()));
            holder.timestamp.setText(formattedTime);
        }

        if (getItemViewType(position) == MSG_TYPE_RIGHT) {
            if (message.isSeen()) {
                holder.seenStatus.setVisibility(View.VISIBLE);
                holder.seenStatus.setText("Seen");
            } else {
                holder.seenStatus.setVisibility(View.GONE);
            }
        } else {
            // For received messages, show avatar
            if (holder.profileImage != null) {
                AvatarHelper.loadAvatar(context, receiverPhotoUrl, receiverAvatar, null, holder.profileImage);
                
                holder.profileImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("userId", message.getSenderId());
                    context.startActivity(intent);
                });
            }
        }

        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDialog(message, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public TextView timestamp;
        public TextView seenStatus;
        public CircleImageView profileImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timestamp = itemView.findViewById(R.id.text_message_time);
            seenStatus = itemView.findViewById(R.id.text_message_seen);
            profileImage = itemView.findViewById(R.id.image_message_profile);
        }
    }

    @Override
    public int getItemViewType(int position) {
        String currentUserId = tokenManager.getUserId();
        if (currentUserId != null && messages.get(position).getSenderId().equals(currentUserId)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    private void showDeleteDialog(final Message message, final int position) {
        // Individual message deletion is currently not supported by the backend API.
        // Alert the user.
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Message");
        builder.setMessage("Individual message deletion is not supported yet.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
