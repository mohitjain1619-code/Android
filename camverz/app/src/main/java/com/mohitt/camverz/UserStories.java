package com.mohitt.camverz;

import java.io.Serializable;
import java.util.List;

public class UserStories implements Serializable {
    private String userId;
    private String userName;
    private String userAvatar;
    private String userPhotoUrl;
    private String userEmail;
    private List<StoryItem> stories;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getUserPhotoUrl() {
        return userPhotoUrl;
    }

    public void setUserPhotoUrl(String userPhotoUrl) {
        this.userPhotoUrl = userPhotoUrl;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public List<StoryItem> getStories() {
        return stories;
    }

    public void setStories(List<StoryItem> stories) {
        this.stories = stories;
    }
}
