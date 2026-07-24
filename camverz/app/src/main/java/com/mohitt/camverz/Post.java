package com.mohitt.camverz;

import java.util.HashMap;
import java.util.Map;

/**
 * Post model — TEXT ONLY.
 * Image and voice functionality has been removed.
 */
public class Post {
    private String id;
    private String text;
    private String category;
    private String userId;
    private String username;
    private String userAvatar;
    private String userPhotoUrl;
    private int likeCount = 0;
    private int commentCount = 0;
    private boolean likedByMe = false;
    private String expiryAt;
    private String createdAt;

    public Post() {
        // Default constructor
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getUserPhotoUrl() { return userPhotoUrl; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }

    public String getExpiryAt() { return expiryAt; }
    public void setExpiryAt(String expiryAt) { this.expiryAt = expiryAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
