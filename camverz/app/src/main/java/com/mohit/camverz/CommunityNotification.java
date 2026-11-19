package com.mohit.camverz;

import android.util.Log;
import com.google.gson.JsonElement;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class CommunityNotification {
    private static final String TAG = "CommunityNotification";

    private String id;
    private String type;
    private boolean read;
    private TriggeringUser triggeringUser;
    private String postId;
    private String postTitle;
    private String postType;
    private String friendRequestId;
    private String friendshipStatus;
    private JsonElement createdAt;

    public static class TriggeringUser {
        private String id;
        private String name;
        private String avatar;
        private String photoUrl;

        public String getId() { return id != null ? id : ""; }
        public String getName() { return name != null ? name : ""; }
        public String getAvatar() { return avatar != null ? avatar : ""; }
        public String getPhotoUrl() { return photoUrl != null ? photoUrl : ""; }
    }

    public String getId() { return id != null ? id : ""; }
    public String getType() { return type != null ? type : ""; }
    public boolean isRead() { return read; }
    public TriggeringUser getTriggeringUser() { return triggeringUser; }
    public String getPostId() { return postId != null ? postId : ""; }
    public String getPostTitle() { return postTitle != null ? postTitle : ""; }
    public String getPostType() { return postType != null ? postType : ""; }
    public String getFriendRequestId() { return friendRequestId != null ? friendRequestId : ""; }
    public String getFriendshipStatus() { return friendshipStatus != null ? friendshipStatus : ""; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }

    public long getCreatedAt() {
        if (createdAt == null || createdAt.isJsonNull()) {
            return 0L;
        }
        try {
            if (createdAt.isJsonPrimitive()) {
                if (createdAt.getAsJsonPrimitive().isNumber()) {
                    return createdAt.getAsLong();
                } else if (createdAt.getAsJsonPrimitive().isString()) {
                    String str = createdAt.getAsString().trim();
                    try {
                        return Long.parseLong(str);
                    } catch (NumberFormatException nfe) {
                        // Attempt ISO-8601 parsing
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        return sdf.parse(str).getTime();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed parsing notification createdAt: " + createdAt);
        }
        return 0L;
    }
}
