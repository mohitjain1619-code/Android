package com.mohit.camverz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mohit.camverz.api.TokenManager;

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

    public interface OnMessageReplyListener {
        void onReplyMessage(Message message, int position);
    }

    private OnMessageReplyListener onMessageReplyListener;

    public void setOnMessageReplyListener(OnMessageReplyListener listener) {
        this.onMessageReplyListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.messageText.setText(message.getMessage());

        // Bind Quoted Reply if present
        if (message.hasReply()) {
            if (holder.layoutQuotedReply != null) {
                holder.layoutQuotedReply.setVisibility(View.VISIBLE);
                if (holder.tvQuotedSender != null) {
                    holder.tvQuotedSender.setText(message.getReplyToSenderName() != null ? message.getReplyToSenderName() : "Quoted");
                }
                if (holder.tvQuotedText != null) {
                    holder.tvQuotedText.setText(message.getReplyToText());
                }
            }
        } else {
            if (holder.layoutQuotedReply != null) {
                holder.layoutQuotedReply.setVisibility(View.GONE);
            }
        }

        if (message.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(new Date(message.getTimestamp()));
            holder.timestamp.setText(formattedTime);
        }

        if (getItemViewType(position) == MSG_TYPE_RIGHT) {
            if (message.isSeen()) {
                holder.seenStatus.setVisibility(View.VISIBLE);
                holder.seenStatus.setText(context.getString(R.string.seen));
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

        // Tap on bubble to also show reply/actions
        holder.itemView.setOnClickListener(v -> {
            showDeleteDialog(message, position);
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
        public View layoutQuotedReply;
        public TextView tvQuotedSender;
        public TextView tvQuotedText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timestamp = itemView.findViewById(R.id.text_message_time);
            seenStatus = itemView.findViewById(R.id.text_message_seen);
            profileImage = itemView.findViewById(R.id.image_message_profile);
            layoutQuotedReply = itemView.findViewById(R.id.layout_quoted_reply);
            tvQuotedSender = itemView.findViewById(R.id.tv_quoted_sender);
            tvQuotedText = itemView.findViewById(R.id.tv_quoted_text);
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

    public interface OnMessageDeleteListener {
        void onDeleteMessage(Message message, int position, boolean forEveryone);
    }

    private OnMessageDeleteListener onMessageDeleteListener;

    public void setOnMessageDeleteListener(OnMessageDeleteListener listener) {
        this.onMessageDeleteListener = listener;
    }

    private void showDeleteDialog(final Message message, final int position) {
        if (context == null || message == null) return;

        try {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(context);
            View sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_message_bottomsheet, null);
            bottomSheetDialog.setContentView(sheetView);

            // Make background transparent so custom top rounded corners show properly
            if (bottomSheetDialog.getWindow() != null) {
                View designBottomSheet = bottomSheetDialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (designBottomSheet != null) {
                    designBottomSheet.setBackgroundResource(android.R.color.transparent);
                }
            }

            View btnReplyMessage = sheetView.findViewById(R.id.btn_reply_message);
            View btnDeleteForEveryone = sheetView.findViewById(R.id.btn_delete_for_everyone);
            View btnDeleteForMe = sheetView.findViewById(R.id.btn_delete_for_me);
            TextView tvDeleteForMeTitle = sheetView.findViewById(R.id.tv_delete_for_me_title);
            ImageView ivDeleteForMeIcon = sheetView.findViewById(R.id.iv_delete_for_me_icon);
            View btnCancelDelete = sheetView.findViewById(R.id.btn_cancel_delete);

            if (btnReplyMessage != null) {
                btnReplyMessage.setOnClickListener(v -> {
                    bottomSheetDialog.dismiss();
                    if (onMessageReplyListener != null) {
                        onMessageReplyListener.onReplyMessage(message, position);
                    }
                });
            }

            String currentUserId = tokenManager.getUserId();
            boolean isMe = currentUserId != null && currentUserId.equals(message.getSenderId());

            if (isMe) {
                btnDeleteForEveryone.setVisibility(View.VISIBLE);
                if (tvDeleteForMeTitle != null) {
                    tvDeleteForMeTitle.setTextColor(android.graphics.Color.parseColor("#F1F5F9"));
                }
                if (ivDeleteForMeIcon != null) {
                    ivDeleteForMeIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#CBD5E1")));
                }
            } else {
                // For received message, only Delete for Me is available
                btnDeleteForEveryone.setVisibility(View.GONE);
                if (tvDeleteForMeTitle != null) {
                    tvDeleteForMeTitle.setTextColor(android.graphics.Color.parseColor("#FF4B72"));
                }
                if (ivDeleteForMeIcon != null) {
                    ivDeleteForMeIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF4B72")));
                }
            }

            btnDeleteForEveryone.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                if (onMessageDeleteListener != null) {
                    onMessageDeleteListener.onDeleteMessage(message, position, true);
                }
            });

            btnDeleteForMe.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                if (onMessageDeleteListener != null) {
                    onMessageDeleteListener.onDeleteMessage(message, position, false);
                }
            });

            btnCancelDelete.setOnClickListener(v -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
