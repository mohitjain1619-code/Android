package com.mohitt.camverz;

public class RealMeetRequest {
    private String id;
    private String postId;
    private String postTitle;
    private String posterUserId;
    private String applicantUserId;
    private String applicantName;
    private String applicantAvatar;
    private String applicantPhotoUrl;
    private int applicantAge;
    private String applicantCity;
    private String message;
    private String contactPreference; // "Private Video Call", "Direct Chat", "Meet at Spot"
    private String status; // "PENDING", "ACCEPTED", "DECLINED"
    private String applicantGender;
    private boolean applicantVerified;
    private long createdAt;

    public RealMeetRequest() {}

    public RealMeetRequest(String id, String postId, String postTitle, String posterUserId, String applicantUserId, String applicantName, String applicantAvatar, String applicantPhotoUrl, int applicantAge, String applicantCity, String message, String contactPreference, String status, long createdAt) {
        this.id = id;
        this.postId = postId;
        this.postTitle = postTitle;
        this.posterUserId = posterUserId;
        this.applicantUserId = applicantUserId;
        this.applicantName = applicantName;
        this.applicantAvatar = applicantAvatar;
        this.applicantPhotoUrl = applicantPhotoUrl;
        this.applicantAge = applicantAge;
        this.applicantCity = applicantCity;
        this.message = message;
        this.contactPreference = contactPreference;
        this.status = status;
        this.createdAt = createdAt;
    }

    public RealMeetRequest(String id, String postId, String postTitle, String posterUserId, String applicantUserId, String applicantName, String applicantAvatar, String applicantPhotoUrl, int applicantAge, String applicantCity, String message, String contactPreference, String status, String applicantGender, boolean applicantVerified, long createdAt) {
        this.id = id;
        this.postId = postId;
        this.postTitle = postTitle;
        this.posterUserId = posterUserId;
        this.applicantUserId = applicantUserId;
        this.applicantName = applicantName;
        this.applicantAvatar = applicantAvatar;
        this.applicantPhotoUrl = applicantPhotoUrl;
        this.applicantAge = applicantAge;
        this.applicantCity = applicantCity;
        this.message = message;
        this.contactPreference = contactPreference;
        this.status = status;
        this.applicantGender = applicantGender;
        this.applicantVerified = applicantVerified;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public String getPosterUserId() { return posterUserId; }
    public void setPosterUserId(String posterUserId) { this.posterUserId = posterUserId; }

    public String getApplicantUserId() { return applicantUserId; }
    public void setApplicantUserId(String applicantUserId) { this.applicantUserId = applicantUserId; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantAvatar() { return applicantAvatar; }
    public void setApplicantAvatar(String applicantAvatar) { this.applicantAvatar = applicantAvatar; }

    public String getApplicantPhotoUrl() { return applicantPhotoUrl; }
    public void setApplicantPhotoUrl(String applicantPhotoUrl) { this.applicantPhotoUrl = applicantPhotoUrl; }

    public int getApplicantAge() { return applicantAge; }
    public void setApplicantAge(int applicantAge) { this.applicantAge = applicantAge; }

    public String getApplicantCity() { return applicantCity; }
    public void setApplicantCity(String applicantCity) { this.applicantCity = applicantCity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getContactPreference() { return contactPreference; }
    public void setContactPreference(String contactPreference) { this.contactPreference = contactPreference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApplicantGender() { return applicantGender; }
    public void setApplicantGender(String applicantGender) { this.applicantGender = applicantGender; }

    public boolean isApplicantVerified() { return applicantVerified; }
    public void setApplicantVerified(boolean applicantVerified) { this.applicantVerified = applicantVerified; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
