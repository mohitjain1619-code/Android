package com.mohit.camverz;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Keep
public class UserStories implements Serializable {

    @SerializedName("userId")
    private String userId;

    @SerializedName("userName")
    private String userName;

    @SerializedName("userAvatar")
    private String userAvatar;

    @SerializedName("userPhotoUrl")
    private String userPhotoUrl;

    @SerializedName("userEmail")
    private String userEmail;

    @SerializedName("stories")
    private List<StoryItem> stories = new ArrayList<>();

    public UserStories() {
        // Required empty no-arg constructor for Gson reflection
    }

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
        if (stories == null) {
            stories = new ArrayList<>();
        }
        return stories;
    }

    public void setStories(List<StoryItem> stories) {
        this.stories = stories;
    }
}
