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
}
