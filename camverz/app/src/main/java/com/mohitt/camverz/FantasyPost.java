package com.mohitt.camverz;

public class FantasyPost {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String photoUrl;
    private int age;
    private String relationshipStatus; // Married, Divorced, Single, Widow
    private String description; // Max 200 chars
    private String interests;
    private String gender;
    private boolean verified;
    private long createdAt;

    public FantasyPost() {}

    public FantasyPost(String id, String userId, String userName, String userAvatar, String photoUrl, int age, String relationshipStatus, String description, String interests, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.photoUrl = photoUrl;
        this.age = age;
        this.relationshipStatus = relationshipStatus;
        this.description = description;
        this.interests = interests;
        this.createdAt = createdAt;
    }

    public FantasyPost(String id, String userId, String userName, String userAvatar, String photoUrl, int age, String relationshipStatus, String description, String interests, String gender, boolean verified, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.photoUrl = photoUrl;
        this.age = age;
        this.relationshipStatus = relationshipStatus;
        this.description = description;
        this.interests = interests;
        this.gender = gender;
        this.verified = verified;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getRelationshipStatus() { return relationshipStatus; }
    public void setRelationshipStatus(String relationshipStatus) { this.relationshipStatus = relationshipStatus; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
