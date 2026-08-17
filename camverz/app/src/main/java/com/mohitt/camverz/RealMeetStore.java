package com.mohitt.camverz;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RealMeetStore {

    private static final String PREF_NAME = "real_meet_store";
    private static final String KEY_REAL_MEET_POSTS = "real_meet_posts";
    private static final String KEY_PARTY_POSTS = "party_posts";
    private static final String KEY_FANTASY_POSTS = "fantasy_posts";
    private static final String KEY_MEET_REQUESTS = "meet_requests";

    private static RealMeetStore instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private RealMeetStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized RealMeetStore getInstance(Context context) {
        if (instance == null) {
            instance = new RealMeetStore(context);
        }
        return instance;
    }

    // ==========================================
    // REAL MEET POSTS
    // ==========================================
    public synchronized List<RealMeetPost> getRealMeetPosts() {
        String json = prefs.getString(KEY_REAL_MEET_POSTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<RealMeetPost>>() {}.getType();
        List<RealMeetPost> posts = gson.fromJson(json, type);
        return posts != null ? posts : new ArrayList<>();
    }

    public synchronized void saveRealMeetPosts(List<RealMeetPost> posts) {
        String json = gson.toJson(posts);
        prefs.edit().putString(KEY_REAL_MEET_POSTS, json).apply();
    }

    public synchronized RealMeetPost getUserPostToday(String userId) {
        if (userId == null || userId.isEmpty()) return null;
        List<RealMeetPost> posts = getRealMeetPosts();
        long now = System.currentTimeMillis();

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTimeInMillis(now);
        int todayYear = todayCal.get(Calendar.YEAR);
        int todayDay = todayCal.get(Calendar.DAY_OF_YEAR);

        for (RealMeetPost post : posts) {
            if (userId.equalsIgnoreCase(post.getUserId())) {
                Calendar postCal = Calendar.getInstance();
                postCal.setTimeInMillis(post.getCreatedAt());
                if (postCal.get(Calendar.YEAR) == todayYear && postCal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                    return post;
                }
            }
        }
        return null;
    }

    public synchronized void addRealMeetPost(RealMeetPost post) {
        List<RealMeetPost> posts = getRealMeetPosts();
        posts.removeIf(p -> p.getUserId().equalsIgnoreCase(post.getUserId()));
        posts.add(0, post);
        saveRealMeetPosts(posts);
    }

    public synchronized void deleteRealMeetPost(String postId) {
        List<RealMeetPost> posts = getRealMeetPosts();
        posts.removeIf(p -> p.getId().equalsIgnoreCase(postId));
        saveRealMeetPosts(posts);
    }

    // ==========================================
    // PARTY POSTS
    // ==========================================
    public synchronized List<PartyPost> getPartyPosts() {
        String json = prefs.getString(KEY_PARTY_POSTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<PartyPost>>() {}.getType();
        List<PartyPost> posts = gson.fromJson(json, type);
        return posts != null ? posts : new ArrayList<>();
    }

    public synchronized void savePartyPosts(List<PartyPost> posts) {
        String json = gson.toJson(posts);
        prefs.edit().putString(KEY_PARTY_POSTS, json).apply();
    }

    public synchronized void addPartyPost(PartyPost post) {
        List<PartyPost> posts = getPartyPosts();
        posts.add(0, post);
        savePartyPosts(posts);
    }

    public synchronized void deletePartyPost(String postId) {
        List<PartyPost> posts = getPartyPosts();
        posts.removeIf(p -> p.getId().equalsIgnoreCase(postId));
        savePartyPosts(posts);
    }

    // ==========================================
    // FANTASY POSTS
    // ==========================================
    public synchronized List<FantasyPost> getFantasyPosts() {
        String json = prefs.getString(KEY_FANTASY_POSTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<FantasyPost>>() {}.getType();
        List<FantasyPost> posts = gson.fromJson(json, type);
        return posts != null ? posts : new ArrayList<>();
    }

    public synchronized void saveFantasyPosts(List<FantasyPost> posts) {
        String json = gson.toJson(posts);
        prefs.edit().putString(KEY_FANTASY_POSTS, json).apply();
    }

    public synchronized void addFantasyPost(FantasyPost post) {
        List<FantasyPost> posts = getFantasyPosts();
        posts.add(0, post);
        saveFantasyPosts(posts);
    }

    public synchronized void deleteFantasyPost(String postId) {
        List<FantasyPost> posts = getFantasyPosts();
        posts.removeIf(p -> p.getId().equalsIgnoreCase(postId));
        saveFantasyPosts(posts);
    }

    // ==========================================
    // MEET REQUESTS & RESPONSES
    // ==========================================
    public synchronized List<RealMeetRequest> getMeetRequests() {
        String json = prefs.getString(KEY_MEET_REQUESTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<RealMeetRequest>>() {}.getType();
        List<RealMeetRequest> requests = gson.fromJson(json, type);
        return requests != null ? requests : new ArrayList<>();
    }

    public synchronized void saveMeetRequests(List<RealMeetRequest> requests) {
        String json = gson.toJson(requests);
        prefs.edit().putString(KEY_MEET_REQUESTS, json).apply();
    }

    public synchronized List<RealMeetRequest> getRequestsForPoster(String posterUserId) {
        List<RealMeetRequest> all = getMeetRequests();
        List<RealMeetRequest> result = new ArrayList<>();
        if (posterUserId == null) return result;
        for (RealMeetRequest req : all) {
            if (posterUserId.equalsIgnoreCase(req.getPosterUserId())) {
                result.add(req);
            }
        }
        return result;
    }

    public synchronized void addMeetRequest(RealMeetRequest request) {
        List<RealMeetRequest> requests = getMeetRequests();
        requests.add(0, request);
        saveMeetRequests(requests);
    }

    public synchronized void updateRequestStatus(String requestId, String status) {
        List<RealMeetRequest> requests = getMeetRequests();
        for (RealMeetRequest r : requests) {
            if (r.getId().equalsIgnoreCase(requestId)) {
                r.setStatus(status);
                break;
            }
        }
        saveMeetRequests(requests);
    }

    public synchronized void deleteMeetRequest(String requestId) {
        List<RealMeetRequest> requests = getMeetRequests();
        requests.removeIf(r -> r.getId().equalsIgnoreCase(requestId));
        saveMeetRequests(requests);
    }
}
