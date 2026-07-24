package com.mohitt.camverz;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int followersCount;
    private int followingCount;
    private boolean followedByMe;

    public int getFollowersCount() { return followersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public boolean isFollowedByMe() { return followedByMe; }
    public void setFollowedByMe(boolean followedByMe) { this.followedByMe = followedByMe; }

    private String uid;
    private String name;
    private String avatar;
    private String bio;
    private String dob;
    private String city;
    private String customId;
    private String gender;
    private String userId; // Field to hold document ID if needed explicitly, distinct from uid if needed.
    private List<String> followers = new ArrayList<>();
    private List<String> following = new ArrayList<>();
    private boolean verified;
    private String photoUrl;
    private long createdAt;
    private String email;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    
    // Alias for getUid/setUid or explicit userId if distinct
    public String getUserId() {
        return userId != null ? userId : uid;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        // Also set uid if it's null?
        if (this.uid == null) {
            this.uid = userId;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<String> getFollowers() {
        return followers;
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }

    public List<String> getFollowing() {
        return following;
    }

    public void setFollowing(List<String> following) {
        this.following = following;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
