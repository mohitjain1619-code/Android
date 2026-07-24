package com.mohitt.camverz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mohitt.camverz.api.TokenManager;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentList;
    private OnCommentInteractionListener listener;
    private TokenManager tokenManager;

    public interface OnCommentInteractionListener {
        void onLikeClicked(String commentId);
        void onReplyClicked(String commentId);
        void onDeleteClicked(String commentId);
        void onUserClicked(String userId);
    }

    public CommentAdapter(Context context, List<Comment> commentList, OnCommentInteractionListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.listener = listener;
        this.tokenManager = TokenManager.getInstance(context);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.username.setText(comment.getUserName());
        if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
            int avatarResId = context.getResources().getIdentifier(comment.getUserAvatar(), "drawable", context.getPackageName());
            if (avatarResId != 0) {
                Glide.with(context)
                        .load(avatarResId)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(holder.profileImage);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_user_placeholder)
                        .into(holder.profileImage);
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_user_placeholder);
        }

        holder.commentText.setText(comment.getText());
        holder.timestamp.setText(getTimestampString(comment.getTimestamp()));

        // Handle indentation for replies
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.root.getLayoutParams();
        if (comment.getParentId() != null) {
            params.setMarginStart(60); // Indent replies
        } else {
            params.setMarginStart(0); // Reset for non-replies
        }
        holder.root.setLayoutParams(params);

        // Liking comments is not supported in the API yet, hide or handle gracefully
        holder.likeButton.setText("Like");
        holder.likeIcon.setVisibility(View.GONE);
        holder.likeButton.setOnClickListener(v -> listener.onLikeClicked(comment.getCommentId()));

        // Reply button
        holder.replyButton.setOnClickListener(v -> listener.onReplyClicked(comment.getCommentId()));

        // Delete button visibility
        if (tokenManager.isLoggedIn() && comment.getUserId().equals(tokenManager.getUserId())) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> listener.onDeleteClicked(comment.getCommentId()));
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }

        // Open profile on click
        View.OnClickListener profileClickListener = v -> listener.onUserClicked(comment.getUserId());
        holder.profileImage.setOnClickListener(profileClickListener);
        holder.username.setOnClickListener(profileClickListener);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout root;
        CircleImageView profileImage;
        TextView username;
        TextView commentText;
        TextView timestamp;
        TextView likeButton;
        TextView replyButton;
        TextView deleteButton;
        ImageView likeIcon;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.comment_root);
            profileImage = itemView.findViewById(R.id.comment_profile_image);
            username = itemView.findViewById(R.id.comment_username);
            commentText = itemView.findViewById(R.id.comment_text);
            timestamp = itemView.findViewById(R.id.comment_timestamp);
            likeButton = itemView.findViewById(R.id.comment_like_button);
            replyButton = itemView.findViewById(R.id.comment_reply_button);
            deleteButton = itemView.findViewById(R.id.comment_delete_button);
            likeIcon = itemView.findViewById(R.id.comment_like_icon);
        }
    }

    private String getTimestampString(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
}
