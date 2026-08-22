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

    private static final String PREF_NAME = "real_meet_store_prefs";
    private static final String KEY_REAL_MEET_POSTS = "key_real_meet_posts_v3";
    private static final String KEY_PARTY_POSTS = "key_party_posts_v3";
    private static final String KEY_FANTASY_POSTS = "key_fantasy_posts_v3";
    private static final String KEY_MEET_REQUESTS = "key_meet_requests_v3";

    private static RealMeetStore instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private RealMeetStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        if (!prefs.getBoolean("cleared_mock_v4", false)) {
            prefs.edit().clear().putBoolean("cleared_mock_v4", true).apply();
        }
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

    public synchronized void setRealMeetPosts(List<RealMeetPost> newPosts) {
        if (newPosts == null) return;
        List<RealMeetPost> deduplicated = new ArrayList<>();
        for (RealMeetPost p : newPosts) {
            if (p != null && p.getId() != null) {
                boolean exists = false;
                for (RealMeetPost existing : deduplicated) {
                    if (existing.getId().equalsIgnoreCase(p.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) deduplicated.add(p);
            }
        }
        saveRealMeetPosts(deduplicated);
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
        if (post == null || post.getId() == null) return;
        List<RealMeetPost> posts = getRealMeetPosts();
        posts.removeIf(p -> p.getId().equalsIgnoreCase(post.getId()));
        posts.add(0, post);
        saveRealMeetPosts(posts);
    }

    public synchronized void deleteRealMeetPost(String postId) {
        if (postId == null) return;
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

    public synchronized void setPartyPosts(List<PartyPost> newPosts) {
        if (newPosts == null) return;
        List<PartyPost> deduplicated = new ArrayList<>();
        for (PartyPost p : newPosts) {
            if (p != null && p.getId() != null) {
                boolean exists = false;
                for (PartyPost existing : deduplicated) {
                    if (existing.getId().equalsIgnoreCase(p.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) deduplicated.add(p);
            }
        }
        savePartyPosts(deduplicated);
    }

    public synchronized void addPartyPost(PartyPost post) {
        if (post == null || post.getId() == null) return;
        List<PartyPost> posts = getPartyPosts();
        posts.removeIf(p -> p.getId().equalsIgnoreCase(post.getId()));
        posts.add(0, post);
        savePartyPosts(posts);
    }

    public synchronized void deletePartyPost(String postId) {
        if (postId == null) return;
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

    public synchronized void setFantasyPosts(List<FantasyPost> newPosts) {
        if (newPosts == null) return;
        List<FantasyPost> deduplicated = new ArrayList<>();
        for (FantasyPost p : newPosts) {
            if (p != null && p.getId() != null) {
                boolean exists = false;
                for (FantasyPost existing : deduplicated) {
                    if (existing.getId().equalsIgnoreCase(p.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) deduplicated.add(p);
            }
        }
        saveFantasyPosts(deduplicated);
    }

    public synchronized void addFantasyPost(FantasyPost post) {
        if (post == null || post.getId() == null) return;
        List<FantasyPost> posts = getFantasyPosts();
        posts.removeIf(p -> p.getId().equalsIgnoreCase(post.getId()));
        posts.add(0, post);
        saveFantasyPosts(posts);
    }

    public synchronized void deleteFantasyPost(String postId) {
        if (postId == null) return;
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

    public synchronized void addMeetRequest(RealMeetRequest request) {
        if (request == null || request.getId() == null) return;
        List<RealMeetRequest> requests = getMeetRequests();
        requests.removeIf(r -> r.getId().equalsIgnoreCase(request.getId()));
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

    public synchronized boolean hasUserRequestedPost(String userId, String postId) {
        if (userId == null || postId == null) return false;
        List<RealMeetRequest> requests = getMeetRequests();
        for (RealMeetRequest req : requests) {
            if (userId.equalsIgnoreCase(req.getApplicantUserId()) && postId.equalsIgnoreCase(req.getPostId())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void deleteMeetRequest(String requestId) {
        List<RealMeetRequest> requests = getMeetRequests();
        requests.removeIf(r -> r.getId().equalsIgnoreCase(requestId));
        saveMeetRequests(requests);
    }

    // ==========================================
    // SAVED PARTIES LOCAL CACHE
    // ==========================================
    private static final String KEY_SAVED_PARTIES = "key_saved_parties_v3";

    public synchronized List<String> getSavedParties() {
        String json = prefs.getString(KEY_SAVED_PARTIES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public synchronized void saveSavedParties(List<String> list) {
        String json = gson.toJson(list);
        prefs.edit().putString(KEY_SAVED_PARTIES, json).apply();
    }

    public synchronized boolean isPartySaved(String partyId) {
        if (partyId == null) return false;
        return getSavedParties().contains(partyId);
    }

    public synchronized void togglePartySaved(String partyId) {
        if (partyId == null) return;
        List<String> list = getSavedParties();
        if (list.contains(partyId)) {
            list.remove(partyId);
        } else {
            list.add(partyId);
        }
        saveSavedParties(list);
    }
}
