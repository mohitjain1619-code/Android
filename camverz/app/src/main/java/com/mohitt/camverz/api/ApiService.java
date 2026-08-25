package com.mohitt.camverz.api;

import com.google.gson.JsonObject;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit API interface — all endpoints for the Camverz backend.
 * Replaces all Firebase Firestore/Auth/Storage calls.
 */
public interface ApiService {

    // ============================================
    // REAL MEET COMMUNITY SERVER SYNC
    // ============================================
    @GET("realmeet/feed")
    Call<JsonObject> getRealMeetFeed();

    @POST("realmeet/post")
    Call<JsonObject> createRealMeetServerPost(@Body Map<String, Object> body);

    @DELETE("realmeet/post/{id}")
    Call<JsonObject> deleteRealMeetServerPost(@Path("id") String postId);

    @GET("realmeet/requests")
    Call<JsonObject> getRealMeetServerRequests();

    @POST("realmeet/request")
    Call<JsonObject> createRealMeetServerRequest(@Body Map<String, Object> body);

    @PUT("realmeet/request/status")
    Call<JsonObject> updateRealMeetServerRequestStatus(@Body Map<String, Object> body);

    @GET("realmeet/saved-parties")
    Call<JsonObject> getSavedParties();

    @POST("realmeet/post/{id}/save")
    Call<JsonObject> saveParty(@Path("id") String postId);

    @DELETE("realmeet/post/{id}/save")
    Call<JsonObject> unsaveParty(@Path("id") String postId);

    @GET("realmeet/post/{id}/members")
    Call<JsonObject> getPartyMembers(@Path("id") String partyId);

    @PUT("realmeet/post/{id}/visibility")
    Call<JsonObject> setPartyVisibility(@Path("id") String partyId, @Body Map<String, Object> body);

    @POST("realmeet/post/{id}/announcements")
    Call<JsonObject> postAnnouncement(@Path("id") String partyId, @Body Map<String, Object> body);

    @GET("realmeet/post/{id}/announcements")
    Call<JsonObject> getAnnouncements(@Path("id") String partyId);

    @GET("realmeet/notifications")
    Call<JsonObject> getCommunityNotifications();

    // ============================================
    // AUTH
    // ============================================
    @POST("auth/google")
    Call<JsonObject> authWithGoogle(@Body Map<String, Object> body);

    @POST("auth/refresh")
    Call<JsonObject> refreshToken();

    // ============================================
    // USERS
    // ============================================
    @GET("users/me")
    Call<JsonObject> getMe();

    @GET("users/{id}")
    Call<JsonObject> getUser(@Path("id") String userId);

    @PUT("users/me")
    Call<JsonObject> updateMe(@Body Map<String, Object> updates);

    @DELETE("users/me")
    Call<JsonObject> deleteAccount();

    @POST("users/{id}/follow")
    Call<JsonObject> followUser(@Path("id") String userId);

    @DELETE("users/{id}/follow")
    Call<JsonObject> unfollowUser(@Path("id") String userId);

    @GET("users/{id}/followers")
    Call<JsonObject> getFollowers(@Path("id") String userId);

    @GET("users/{id}/following")
    Call<JsonObject> getFollowing(@Path("id") String userId);

    @POST("users/{id}/block")
    Call<JsonObject> blockUser(@Path("id") String userId);

    @DELETE("users/{id}/block")
    Call<JsonObject> unblockUser(@Path("id") String userId);

    @GET("users/me/blocked")
    Call<JsonObject> getBlockedUsers();

    @POST("users/{id}/report")
    Call<JsonObject> reportUser(@Path("id") String userId, @Body Map<String, String> body);

    // ============================================
    // POSTS
    // ============================================
    @GET("posts")
    Call<JsonObject> getPosts(
            @Query("category") String category,
            @Query("userId") String userId,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    @POST("posts")
    Call<JsonObject> createPost(@Body Map<String, String> body);

    @GET("posts/{id}")
    Call<JsonObject> getPost(@Path("id") String postId);

    @DELETE("posts/{id}")
    Call<JsonObject> deletePost(@Path("id") String postId);

    @POST("posts/{id}/like")
    Call<JsonObject> toggleLike(@Path("id") String postId);

    @GET("posts/{id}/comments")
    Call<JsonObject> getComments(@Path("id") String postId);

    @POST("posts/{id}/comments")
    Call<JsonObject> addComment(@Path("id") String postId, @Body Map<String, String> body);

    @DELETE("posts/{postId}/comments/{commentId}")
    Call<JsonObject> deleteComment(@Path("postId") String postId, @Path("commentId") String commentId);

    @POST("posts/comments/{id}/like")
    Call<JsonObject> toggleCommentLike(@Path("id") String commentId);

    // ============================================
    // CHATS
    // ============================================
    @GET("chats")
    Call<JsonObject> getChats();

    @GET("chats/{id}/messages")
    Call<JsonObject> getMessages(
            @Path("id") String chatId,
            @Query("limit") int limit,
            @Query("before") String before
    );

    @POST("chats/{targetUserId}/messages")
    Call<JsonObject> sendMessage(@Path("targetUserId") String targetUserId, @Body Map<String, String> body);

    @PUT("chats/{id}/read")
    Call<JsonObject> markChatRead(@Path("id") String chatId);

    @DELETE("chats/{id}")
    Call<JsonObject> deleteChat(@Path("id") String chatId);

    @GET("chats/unread-count")
    Call<JsonObject> getUnreadCount();

    // ============================================
    // NOTIFICATIONS
    // ============================================
    @GET("notifications")
    Call<JsonObject> getNotifications(@Query("limit") int limit, @Query("offset") int offset);

    @PUT("notifications/{id}/read")
    Call<JsonObject> markNotificationRead(@Path("id") String notificationId);

    @PUT("notifications/read-all")
    Call<JsonObject> markAllNotificationsRead();

    @GET("notifications/unread-count")
    Call<JsonObject> getNotificationUnreadCount();

    // ============================================
    // FRIENDS
    // ============================================
    @POST("friends/request")
    Call<JsonObject> sendFriendRequest(@Body Map<String, String> body);

    @PUT("friends/request/{id}/accept")
    Call<JsonObject> acceptFriendRequest(@Path("id") String requestId);

    @PUT("friends/request/{id}/reject")
    Call<JsonObject> rejectFriendRequest(@Path("id") String requestId);

    @GET("friends/requests")
    Call<JsonObject> getFriendRequests(@Query("type") String type);

    @GET("friends/status/{userId}")
    Call<JsonObject> getFriendStatus(@Path("userId") String userId);

    @DELETE("friends/request/{userId}")
    Call<JsonObject> deleteFriendRequest(@Path("userId") String userId);

    @GET("friends/online")
    Call<JsonObject> getOnlineFriends();

    // ============================================
    // VERIFICATION
    // ============================================
    @Multipart
    @POST("verify/gender")
    Call<JsonObject> verifyGender(@Part MultipartBody.Part image);

    // ============================================
    // WEBRTC
    // ============================================
    @GET("webrtc/ice")
    Call<JsonObject> getIceServers(@Query("useTurn") boolean useTurn);

    // ============================================
    // STORIES
    // ============================================
    @GET("stories/active")
    Call<JsonObject> getActiveStories();

    @Multipart
    @POST("stories/upload")
    Call<JsonObject> uploadMediaStory(
            @Part MultipartBody.Part file,
            @Part("type") RequestBody type,
            @Part("textContent") RequestBody textContent,
            @Part("textColor") RequestBody textColor,
            @Part("bgGradient") RequestBody bgGradient
    );

    @POST("stories/upload")
    Call<JsonObject> uploadTextStory(@Body Map<String, Object> body);

    @DELETE("stories/{id}")
    Call<JsonObject> deleteStory(@Path("id") String storyId);
}
