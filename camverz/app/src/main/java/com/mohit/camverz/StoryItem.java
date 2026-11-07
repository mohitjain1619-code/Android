package com.mohit.camverz;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Keep
public class StoryItem implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("type")
    private String type; // TEXT, IMAGE, VIDEO

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("textContent")
    private String textContent;

    @SerializedName("textColor")
    private String textColor;

    @SerializedName("bgGradient")
    private String bgGradient;

    @SerializedName("createdAt")
    private long createdAt;

    @SerializedName("expiresAt")
    private long expiresAt;

    @SerializedName("viewsCount")
    private int viewsCount;

    @SerializedName("hasViewed")
    private boolean hasViewed;

    public StoryItem() {
        // Required empty no-arg constructor for Gson reflection
    }

    public int getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public boolean isHasViewed() {
        return hasViewed;
    }

    public void setHasViewed(boolean hasViewed) {
        this.hasViewed = hasViewed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getBgGradient() {
        return bgGradient;
    }

    public void setBgGradient(String bgGradient) {
        this.bgGradient = bgGradient;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
