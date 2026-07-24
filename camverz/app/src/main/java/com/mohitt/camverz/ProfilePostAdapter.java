package com.mohitt.camverz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfilePostAdapter extends RecyclerView.Adapter<ProfilePostAdapter.ViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final ApiService api;
    private final TokenManager tokenManager;
    private final String currentUserId;
    private static final int DELETE_ID = 1;

    public ProfilePostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        this.api = ApiClient.getInstance(context).getApi();
        this.tokenManager = TokenManager.getInstance(context);
        this.currentUserId = tokenManager.isLoggedIn() ? tokenManager.getUserId() : null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.postText.setText(post.getText());
        holder.timestamp.setText(getFormattedTimestamp(Long.parseLong(post.getCreatedAt())));
        holder.likeCountTextView.setText(String.valueOf(post.getLikeCount()));
        holder.commentCountTextView.setText(String.valueOf(post.getCommentCount()));

        if (currentUserId != null) {
            boolean isLiked = post.isLikedByMe();
            holder.likeButton.setImageResource(isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);

            holder.likeButton.setOnClickListener(v -> {
                String postId = post.getId();
                if (postId == null) return;

                boolean isCurrentlyLiked = post.isLikedByMe();

                if (isCurrentlyLiked) {
                    api.toggleLike(postId).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().get("ok").getAsBoolean()) {
                                post.setLikeCount(post.getLikeCount() - 1); post.setLikedByMe(false);
                                notifyItemChanged(position);
                            }
                        }
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
                } else {
                    api.toggleLike(postId).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().get("ok").getAsBoolean()) {
                                post.setLikeCount(post.getLikeCount() + 1); post.setLikedByMe(true);
                                notifyItemChanged(position);
                            }
                        }
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
                }
            });
        } else {
            holder.likeButton.setImageResource(R.drawable.ic_like_outline);
            holder.likeButton.setOnClickListener(v -> Toast.makeText(context, "You need to be logged in to like posts", Toast.LENGTH_SHORT).show());
        }

        holder.commentButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getId());
            context.startActivity(intent);
        });

        if (currentUserId != null && currentUserId.equals(post.getUserId())) {
            holder.postMenuButton.setVisibility(View.VISIBLE);
            holder.postMenuButton.setOnClickListener(v -> showPopupMenu(holder.postMenuButton, post, position));
        } else {
            holder.postMenuButton.setVisibility(View.GONE);
        }
    }

    private void showPopupMenu(View view, Post post, int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenu().add(0, DELETE_ID, 0, "Delete");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == DELETE_ID) {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Post")
                        .setMessage("Are you sure you want to delete this post?")
                        .setPositiveButton("Delete", (dialog, which) -> deletePost(post, position))
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void deletePost(Post post, int position) {
        if (post.getId() == null) return;
        api.deletePost(post.getId()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().get("ok").getAsBoolean()) {
                    if (position >= 0 && position < postList.size()) {
                        postList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, postList.size());
                    }
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
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private String getFormattedTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView postText;
        TextView timestamp;
        ImageView postMenuButton;
        ImageView likeButton;
        TextView likeCountTextView;
        ImageView commentButton;
        TextView commentCountTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            postText = itemView.findViewById(R.id.post_text);
            timestamp = itemView.findViewById(R.id.timestamp);
            postMenuButton = itemView.findViewById(R.id.post_menu_button);
            likeButton = itemView.findViewById(R.id.like_button);
            likeCountTextView = itemView.findViewById(R.id.like_count_text_view);
            commentButton = itemView.findViewById(R.id.comment_button);
            commentCountTextView = itemView.findViewById(R.id.comment_count_text_view);
        }
    }
}
