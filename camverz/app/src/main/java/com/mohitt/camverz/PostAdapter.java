package com.mohitt.camverz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter";
    private Context context;
    private List<Post> postList;
    private ApiService api;
    private TokenManager tokenManager;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        this.api = ApiClient.getInstance(context).getApi();
        this.tokenManager = TokenManager.getInstance(context);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        if (post == null) {
            Log.e(TAG, "Post object is null at position: " + position);
            return;
        }

        // Set user info directly from post data (no need to fetch user separately)
        holder.usernameTextView.setText(post.getUsername());
        
        // Hide verification badge (unless we add it to the backend post response)
        holder.verificationBadge.setVisibility(View.GONE);

        AvatarHelper.loadAvatar(context, post.getUserPhotoUrl(), post.getUserAvatar(), post.getUsername(), holder.profileImageView);

        holder.postTextView.setText(post.getText());
        holder.timestampTextView.setText(getFormattedTimestamp(post.getCreatedAt()));

        // --- Like Button Logic ---
        holder.likeCountTextView.setText(String.valueOf(post.getLikeCount()));

        if (tokenManager.isLoggedIn()) {
            holder.likeButton.setImageResource(post.isLikedByMe() ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);

            holder.likeButton.setOnClickListener(v -> {
                String postId = post.getId();
                if (postId == null || postId.isEmpty()) return;

                boolean wasLiked = post.isLikedByMe();
                
                // Optimistic UI update (direct view modification)
                post.setLikedByMe(!wasLiked);
                post.setLikeCount(wasLiked ? post.getLikeCount() - 1 : post.getLikeCount() + 1);
                holder.likeButton.setImageResource(post.isLikedByMe() ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
                holder.likeCountTextView.setText(String.valueOf(post.getLikeCount()));

                // Bounce micro-animation
                holder.likeButton.setScaleX(0.7f);
                holder.likeButton.setScaleY(0.7f);
                holder.likeButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();

                api.toggleLike(postId).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful()) {
                            // Revert on failure
                            post.setLikedByMe(wasLiked);
                            post.setLikeCount(wasLiked ? post.getLikeCount() + 1 : post.getLikeCount() - 1);
                            holder.likeButton.setImageResource(post.isLikedByMe() ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
                            holder.likeCountTextView.setText(String.valueOf(post.getLikeCount()));
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        // Revert on failure
                        post.setLikedByMe(wasLiked);
                        post.setLikeCount(wasLiked ? post.getLikeCount() + 1 : post.getLikeCount() - 1);
                        holder.likeButton.setImageResource(post.isLikedByMe() ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
                        holder.likeCountTextView.setText(String.valueOf(post.getLikeCount()));
                    }
                });
            });
        } else {
            holder.likeButton.setImageResource(R.drawable.ic_like_outline);
            holder.likeButton.setOnClickListener(v -> Toast.makeText(context, "You need to be logged in to like posts", Toast.LENGTH_SHORT).show());
        }

        // --- Click Post Box to Open Comments ---
        View.OnClickListener commentClickListener = v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getId());
            context.startActivity(intent);
        };
        holder.itemView.setOnClickListener(commentClickListener);
        holder.postTextView.setOnClickListener(commentClickListener);

        // --- Comment Button Logic ---
        holder.commentCountTextView.setText(String.valueOf(post.getCommentCount()));
        holder.commentButton.setOnClickListener(commentClickListener);

        // --- Post Menu Logic ---
        if (tokenManager.isLoggedIn() && tokenManager.getUserId().equals(post.getUserId())) {
            holder.postMenuButton.setVisibility(View.VISIBLE);
            holder.postMenuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, holder.postMenuButton);
                popup.getMenuInflater().inflate(R.menu.post_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete_post) {
                        new AlertDialog.Builder(context)
                                .setTitle("Delete Post")
                                .setMessage("Are you sure you want to delete this post?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    api.deletePost(post.getId()).enqueue(new Callback<JsonObject>() {
                                        @Override
                                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                            if (response.isSuccessful()) {
                                                postList.remove(position);
                                                notifyItemRemoved(position);
                                                notifyItemRangeChanged(position, postList.size());
                                                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(context, "Error deleting post", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        @Override
                                        public void onFailure(Call<JsonObject> call, Throwable t) {
                                            Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            holder.postMenuButton.setVisibility(View.GONE);
        }

        // --- Profile Click Logic ---
        View.OnClickListener profileClickListener = v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", post.getUserId());
            context.startActivity(intent);
        };

        holder.profileImageView.setOnClickListener(profileClickListener);
        holder.usernameTextView.setOnClickListener(profileClickListener);
    }

    private String getFormattedTimestamp(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "just now";
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date date = sdf.parse(isoDateString);
            if (date == null) return "just now";
            
            long timestamp = date.getTime();
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
        } catch (ParseException e) {
            return "just now";
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView;
        ImageView verificationBadge;
        TextView timestampTextView;
        TextView postTextView;
        ImageView postMenuButton;
        ImageView likeButton;
        TextView likeCountTextView;
        ImageView commentButton;
        TextView commentCountTextView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profile_image);
            usernameTextView = itemView.findViewById(R.id.username_text_view);
            verificationBadge = itemView.findViewById(R.id.post_verification_badge);
            timestampTextView = itemView.findViewById(R.id.timestamp_text_view);
            postTextView = itemView.findViewById(R.id.post_text_view);
            postMenuButton = itemView.findViewById(R.id.post_menu_button);
            likeButton = itemView.findViewById(R.id.like_button);
            likeCountTextView = itemView.findViewById(R.id.like_count_text_view);
            commentButton = itemView.findViewById(R.id.comment_button);
            commentCountTextView = itemView.findViewById(R.id.comment_count_text_view);
        }
    }
}
