package com.mohitt.camverz;

public class PartyPost {
    private String id;
    private String hostUserId;
    private String hostName;
    private String hostAvatar;
    private String hostPhotoUrl;
    private int hostAge;
    private String venue;
    private String purpose;
    private int capacity;
    private String targetGender; // "Everyone", "Female Only", "Couples", "Male Only"
    private String partyTime;
    private String gender;
    private boolean verified;
    private long createdAt;

    public PartyPost() {}

    public PartyPost(String id, String hostUserId, String hostName, String hostAvatar, String hostPhotoUrl, int hostAge, String venue, String purpose, int capacity, String targetGender, String partyTime, long createdAt) {
        this.id = id;
        this.hostUserId = hostUserId;
        this.hostName = hostName;
        this.hostAvatar = hostAvatar;
        this.hostPhotoUrl = hostPhotoUrl;
        this.hostAge = hostAge;
        this.venue = venue;
        this.purpose = purpose;
        this.capacity = capacity;
        this.targetGender = targetGender;
        this.partyTime = partyTime;
        this.createdAt = createdAt;
    }

    public PartyPost(String id, String hostUserId, String hostName, String hostAvatar, String hostPhotoUrl, int hostAge, String venue, String purpose, int capacity, String targetGender, String partyTime, String gender, boolean verified, long createdAt) {
        this.id = id;
        this.hostUserId = hostUserId;
        this.hostName = hostName;
        this.hostAvatar = hostAvatar;
        this.hostPhotoUrl = hostPhotoUrl;
        this.hostAge = hostAge;
        this.venue = venue;
        this.purpose = purpose;
        this.capacity = capacity;
        this.targetGender = targetGender;
        this.partyTime = partyTime;
        this.gender = gender;
        this.verified = verified;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHostUserId() { return hostUserId; }
    public void setHostUserId(String hostUserId) { this.hostUserId = hostUserId; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostAvatar() { return hostAvatar; }
    public void setHostAvatar(String hostAvatar) { this.hostAvatar = hostAvatar; }

    public String getHostPhotoUrl() { return hostPhotoUrl; }
    public void setHostPhotoUrl(String hostPhotoUrl) { this.hostPhotoUrl = hostPhotoUrl; }

    public int getHostAge() { return hostAge; }
    public void setHostAge(int hostAge) { this.hostAge = hostAge; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getTargetGender() { return targetGender; }
    public void setTargetGender(String targetGender) { this.targetGender = targetGender; }

    public String getPartyTime() { return partyTime; }
    public void setPartyTime(String partyTime) { this.partyTime = partyTime; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
