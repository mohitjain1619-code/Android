package com.mohitt.camverz;

public class RealMeetPost {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String photoUrl;
    private int age;
    private String city;
    private String purpose;
    private String location;
    private String time;
    private String description;
    private String gender;
    private boolean verified;
    private boolean premium;
    private String sexPreference;
    private long createdAt;

    public RealMeetPost() {}

    public RealMeetPost(String id, String userId, String userName, String userAvatar, String photoUrl, int age, String city, String purpose, String location, String time, String description, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.photoUrl = photoUrl;
        this.age = age;
        this.city = city;
        this.purpose = purpose;
        this.location = location;
        this.time = time;
        this.description = description;
        this.createdAt = createdAt;
    }

    public RealMeetPost(String id, String userId, String userName, String userAvatar, String photoUrl, int age, String city, String purpose, String location, String time, String description, String gender, boolean verified, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.photoUrl = photoUrl;
        this.age = age;
        this.city = city;
        this.purpose = purpose;
        this.location = location;
        this.time = time;
        this.description = description;
        this.gender = gender;
        this.verified = verified;
        this.createdAt = createdAt;
    }

    public RealMeetPost(String id, String userId, String userName, String userAvatar, String photoUrl, int age, String city, String purpose, String location, String time, String description, String gender, boolean verified, boolean premium, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.photoUrl = photoUrl;
        this.age = age;
        this.city = city;
        this.purpose = purpose;
        this.location = location;
        this.time = time;
        this.description = description;
        this.gender = gender;
        this.verified = verified;
        this.premium = premium;
        this.createdAt = createdAt;
    }

    public RealMeetPost(String id, String userId, String userName, String userAvatar, String photoUrl, int age, String city, String purpose, String location, String time, String description, String gender, boolean verified, boolean premium, String sexPreference, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.photoUrl = photoUrl;
        this.age = age;
        this.city = city;
        this.purpose = purpose;
        this.location = location;
        this.time = time;
        this.description = description;
        this.gender = gender;
        this.verified = verified;
        this.premium = premium;
        this.sexPreference = sexPreference;
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

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }

    public String getSexPreference() { return sexPreference != null ? sexPreference : "Straight"; }
    public void setSexPreference(String sexPreference) { this.sexPreference = sexPreference; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
