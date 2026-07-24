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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

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
                            for (JsonElement element : commentsArray) {
                                JsonObject obj = element.getAsJsonObject();
                                Comment comment = new Comment();
                                comment.setCommentId(obj.has("id") ? obj.get("id").getAsString() : "");
                                comment.setText(obj.has("text") ? obj.get("text").getAsString() : "");
                                comment.setUserId(obj.has("userId") ? obj.get("userId").getAsString() : "");
                                comment.setTimestamp(obj.has("timestamp") ? obj.get("timestamp").getAsLong() : 0);
                                comment.setParentId(obj.has("parentId") && !obj.get("parentId").isJsonNull() ? obj.get("parentId").getAsString() : null);
                                // Optional user info mapped from backend if available
                                comment.setUserName(obj.has("username") ? obj.get("username").getAsString() : "User");
                                comment.setUserAvatar(obj.has("userAvatar") ? obj.get("userAvatar").getAsString() : "");
                                
                                commentList.add(comment);
                            }
                        }
                        // For replies, organize them (simple linear layout for now)
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
                        replyingToCommentId = null;
                        commentEditText.setHint("Write a comment...");
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
        // Comment liking is not supported in the new backend yet.
        Toast.makeText(this, "Liking comments is disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReplyClicked(String commentId) {
        replyingToCommentId = commentId;
        commentEditText.setHint("Replying to a comment...");
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
