package com.mohitt.camverz;

public class Notification {
    private String triggeringUserId;
    private String triggeringUserName;
    private String triggeringUserAvatar;
    private String triggeringUserPhotoUrl;
    private String targetUserId;
    private String type; // "like", "comment", "follow", "profile_visit"
    private String postId;
    private String postText;
    private long timestamp;
    private boolean isRead;
    private String notificationId;

    public Notification() {
        // Default constructor required for calls to DataSnapshot.getValue(Notification.class)
    }

    // Getters and Setters
    public String getTriggeringUserPhotoUrl() {
        return triggeringUserPhotoUrl;
    }

    public void setTriggeringUserPhotoUrl(String triggeringUserPhotoUrl) {
        this.triggeringUserPhotoUrl = triggeringUserPhotoUrl;
    }

    public String getTriggeringUserId() {
        return triggeringUserId;
    }

    public void setTriggeringUserId(String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
    }

    public String getTriggeringUserName() {
        return triggeringUserName;
    }

    public void setTriggeringUserName(String triggeringUserName) {
        this.triggeringUserName = triggeringUserName;
    }

    public String getTriggeringUserAvatar() {
        return triggeringUserAvatar;
    }

    public void setTriggeringUserAvatar(String triggeringUserAvatar) {
        this.triggeringUserAvatar = triggeringUserAvatar;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostText() {
        return postText;
    }

    public void setPostText(String postText) {
        this.postText = postText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }
}
