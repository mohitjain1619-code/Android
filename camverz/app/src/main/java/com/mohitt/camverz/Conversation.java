package com.mohitt.camverz;

public class Conversation {
    private String userId;
    private String name;
    private String lastMessage;
    private String profileImageUrl;
    private long lastActivity;
    private boolean isUnread;

    public Conversation() {
        // Default constructor required for calls to DataSnapshot.getValue(Conversation.class)
    }

    public Conversation(String userId, String name, String lastMessage, String profileImageUrl, long lastActivity) {
        this.userId = userId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.profileImageUrl = profileImageUrl;
        this.lastActivity = lastActivity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public boolean isUnread() {
        return isUnread;
    }

    public void setUnread(boolean unread) {
        isUnread = unread;
    }
}
