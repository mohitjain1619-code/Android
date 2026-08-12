package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentsActivity extends BaseActivity implements CommentAdapter.OnCommentInteractionListener {

    private static final String TAG = "CommentsActivity";
    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private List<Comment> commentList;
    private EditText commentEditText;
    private ImageView sendButton;

    private ApiService api;
    private TokenManager tokenManager;

    private String postId;
    private String replyingToCommentId; // To keep track of which comment is being replied to
    private android.view.View replyIndicatorLayout;
    private android.widget.TextView replyIndicatorText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.toolbar), findViewById(R.id.bottom_container));

        replyIndicatorLayout = findViewById(R.id.reply_indicator_layout);
        replyIndicatorText = findViewById(R.id.reply_indicator_text);
        findViewById(R.id.cancel_reply_button).setOnClickListener(v -> cancelReplyMode());

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> finish());

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        postId = getIntent().getStringExtra("postId");

        recyclerView = findViewById(R.id.comments_recycler_view);
        commentEditText = findViewById(R.id.comment_edit_text);
        sendButton = findViewById(R.id.send_comment_button);

        commentList = new ArrayList<>();
        adapter = new CommentAdapter(this, commentList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> postComment());

        loadComments();
    }

    private void loadComments() {
        if (!tokenManager.isLoggedIn() || postId == null) return;

        api.getComments(postId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        commentList.clear();
                        if (data.has("comments")) {
                            JsonArray commentsArray = data.getAsJsonArray("comments");
                            List<Comment> parsedList = new ArrayList<>();
                            for (JsonElement element : commentsArray) {
                                JsonObject obj = element.getAsJsonObject();
                                Comment comment = new Comment();
                                comment.setCommentId(obj.has("id") ? obj.get("id").getAsString() : "");
                                comment.setText(obj.has("text") ? obj.get("text").getAsString() : "");
                                comment.setUserId(obj.has("userId") ? obj.get("userId").getAsString() : "");
                                comment.setParentId(obj.has("parentId") && !obj.get("parentId").isJsonNull() ? obj.get("parentId").getAsString() : null);
                                comment.setUserName(obj.has("username") ? obj.get("username").getAsString() : "User");
                                comment.setUserAvatar(obj.has("userAvatar") ? obj.get("userAvatar").getAsString() : "");
                                comment.setUserPhotoUrl(obj.has("userPhotoUrl") && !obj.get("userPhotoUrl").isJsonNull() ? obj.get("userPhotoUrl").getAsString() : "");
                                comment.setLikeCount(obj.has("likeCount") ? obj.get("likeCount").getAsInt() : 0);
                                comment.setLikedByMe(obj.has("likedByMe") && obj.get("likedByMe").getAsBoolean());
                                
                                // Parse createdAt timestamp from ISO string
                                if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
                                    try {
                                        String dateStr = obj.get("createdAt").getAsString();
                                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                        java.util.Date parsedDate = sdf.parse(dateStr);
                                        if (parsedDate != null) {
                                            comment.setTimestamp(parsedDate.getTime());
                                        }
                                    } catch (Exception e) {
                                        try {
                                            String dateStr = obj.get("createdAt").getAsString();
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                            java.util.Date parsedDate = sdf.parse(dateStr);
                                            if (parsedDate != null) {
                                                comment.setTimestamp(parsedDate.getTime());
                                            }
                                        } catch (Exception ex) {
                                            comment.setTimestamp(System.currentTimeMillis());
                                        }
                                    }
                                } else {
                                    comment.setTimestamp(System.currentTimeMillis());
                                }
                                
                                parsedList.add(comment);
                            }

                            // Group replies directly below their parent comments
                            List<Comment> parentComments = new ArrayList<>();
                            List<Comment> replies = new ArrayList<>();
                            for (Comment c : parsedList) {
                                if (c.getParentId() == null || c.getParentId().isEmpty()) {
                                    parentComments.add(c);
                                } else {
                                    replies.add(c);
                                }
                            }

                            for (Comment parent : parentComments) {
                                commentList.add(parent);
                                for (Comment reply : replies) {
                                    if (parent.getCommentId().equals(reply.getParentId())) {
                                        commentList.add(reply);
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error loading comments", t);
                Toast.makeText(CommentsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postComment() {
        String commentText = commentEditText.getText().toString().trim();
        if (commentText.isEmpty() || postId == null) {
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("text", commentText);
        if (replyingToCommentId != null) {
            body.put("parentId", replyingToCommentId);
        }

        api.addComment(postId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        commentEditText.setText("");
                        cancelReplyMode();
                        loadComments(); // Refresh comments list
                        Toast.makeText(CommentsActivity.this, "Comment posted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Toast.makeText(CommentsActivity.this, "Failed to post comment", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CommentsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLikeClicked(String commentId) {
        if (commentId == null) return;
        api.toggleCommentLike(commentId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    loadComments();
                } else {
                    Toast.makeText(CommentsActivity.this, "Error toggling comment like", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CommentsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelReplyMode() {
        replyingToCommentId = null;
        if (replyIndicatorLayout != null) {
            replyIndicatorLayout.setVisibility(android.view.View.GONE);
        }
        commentEditText.setHint("Write a comment...");
    }

    @Override
    public void onReplyClicked(String commentId) {
        replyingToCommentId = commentId;
        
        String username = "User";
        if (commentList != null) {
            for (Comment c : commentList) {
                if (c.getId() != null && c.getId().equals(commentId)) {
                    username = c.getUserName();
                    break;
                }
            }
        }
        
        if (replyIndicatorText != null) {
            replyIndicatorText.setText("Replying to " + username);
        }
        if (replyIndicatorLayout != null) {
            replyIndicatorLayout.setVisibility(android.view.View.VISIBLE);
        }
        
        commentEditText.setHint("Write a reply...");
        commentEditText.requestFocus();
    }

    @Override
    public void onDeleteClicked(String commentId) {
        if (postId == null || commentId == null) return;
        
        api.deleteComment(postId, commentId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        loadComments();
                        Toast.makeText(CommentsActivity.this, "Comment deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Toast.makeText(CommentsActivity.this, "Failed to delete comment", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CommentsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUserClicked(String userId) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}
