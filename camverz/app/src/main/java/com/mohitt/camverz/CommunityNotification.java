package com.mohitt.camverz;

public class CommunityNotification {
    private String id;
    private String type;
    private boolean read;
    private TriggeringUser triggeringUser;
    private String postId;
    private String postTitle;
    private String postType;
    private String friendRequestId;
    private String friendshipStatus;
    private long createdAt;

    public static class TriggeringUser {
        private String id;
        private String name;
        private String avatar;
        private String photoUrl;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getAvatar() { return avatar; }
        public String getPhotoUrl() { return photoUrl; }
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public boolean isRead() { return read; }
    public TriggeringUser getTriggeringUser() { return triggeringUser; }
    public String getPostId() { return postId; }
    public String getPostTitle() { return postTitle; }
    public String getPostType() { return postType; }
    public String getFriendRequestId() { return friendRequestId; }
    public String getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }
    public long getCreatedAt() { return createdAt; }
}
