package com.mohit.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.mohit.camverz.OnboardingTourManager;
import com.mohit.camverz.SpotlightOverlayView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

// import com.applovin.mediation.ads.MaxAdView;
// import com.applovin.sdk.AppLovinSdkUtils;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedActivity extends BaseActivity {

    private static final String TAG = "FeedActivity";
    private RecyclerView postsRecyclerView;
    private TextView filterAll, filterMale, filterFemale;
    private SwipeRefreshLayout swipeRefreshLayout;

    private PostAdapter adapter;
    private List<Post> postList;
    private String currentCategory = "all";

    private ApiService api;
    private TokenManager tokenManager;

    private View emptyView;
    private TextView tvEmptyMessage;
    private View btnRetryFetch;
    private ProfileUpdateManager.OnProfileUpdatedListener profileUpdateListener;
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.top_bar), findViewById(R.id.floating_menu_container));

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> {
            int stage = OnboardingTourManager.getInstance(this).getTourStage(this);
            if (stage == OnboardingTourManager.STAGE_FEED_POST) {
                Toast.makeText(this, "Please create your first post to continue 👉", Toast.LENGTH_SHORT).show();
                return;
            }
            if (stage == OnboardingTourManager.STAGE_RETURN_HOME) {
                OnboardingTourManager.getInstance(this).setTourStage(this, OnboardingTourManager.STAGE_RANDOM_CALL);
                SpotlightOverlayView.detach(this);
                finish();
                return;
            }
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int stage = OnboardingTourManager.getInstance(FeedActivity.this).getTourStage(FeedActivity.this);
                if (stage == OnboardingTourManager.STAGE_FEED_POST) {
                    Toast.makeText(FeedActivity.this, "Please create your first post to continue 👉", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (stage == OnboardingTourManager.STAGE_RETURN_HOME) {
                    OnboardingTourManager.getInstance(FeedActivity.this).setTourStage(FeedActivity.this, OnboardingTourManager.STAGE_RANDOM_CALL);
                    SpotlightOverlayView.detach(FeedActivity.this);
                    finish();
                    return;
                }
                finish();
            }
        });

        // Create Post Button (+ Icon)
        findViewById(R.id.fab_create_post).setOnClickListener(v -> {
            handleCreatePostClick(false);
        });

        // Floating Menu Buttons
        findViewById(R.id.menu_home_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, MainScreenActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_explore_btn).setOnClickListener(v -> {
            // Already on Explore
        });
        findViewById(R.id.menu_messages_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, InboxActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_profile_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("userId", tokenManager.getUserId());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        postsRecyclerView = findViewById(R.id.posts_recycler_view);
        filterAll = findViewById(R.id.filter_all);
        filterMale = findViewById(R.id.filter_male);
        filterFemale = findViewById(R.id.filter_female);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        emptyView = findViewById(R.id.empty_view);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        btnRetryFetch = findViewById(R.id.btn_retry_fetch);
        if (btnRetryFetch != null) {
            btnRetryFetch.setOnClickListener(v -> fetchPosts());
        }

        // Hide opposite gender filter tab dynamically based on current user gender
        String userGender = tokenManager.getUserGender();
        if ("male".equalsIgnoreCase(userGender)) {
            filterFemale.setVisibility(View.GONE);
        } else if ("female".equalsIgnoreCase(userGender)) {
            filterMale.setVisibility(View.GONE);
        }

        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        adapter = new PostAdapter(this, postList);
        postsRecyclerView.setAdapter(adapter);

        filterAll.setOnClickListener(v -> setCategory("all"));
        filterMale.setOnClickListener(v -> setCategory("male"));
        filterFemale.setOnClickListener(v -> setCategory("female"));

        swipeRefreshLayout.setOnRefreshListener(this::fetchPosts);

        updateFilterButtons();
        // fetchPosts is triggered automatically by onResume()

        profileUpdateListener = (userId, newName, newAvatar) -> {
            runOnUiThread(() -> {
                if (postList != null && adapter != null) {
                    boolean updated = false;
                    for (Post post : postList) {
                        if (userId != null && userId.equals(post.getUserId())) {
                            if (newName != null && !newName.isEmpty()) post.setUsername(newName);
                            if (newAvatar != null && !newAvatar.isEmpty()) post.setUserAvatar(newAvatar);
                            updated = true;
                        }
                    }
                    if (updated) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        };
        ProfileUpdateManager.getInstance(this).registerListener(profileUpdateListener);

        FrameLayout adContainer = findViewById(R.id.banner_ad_container);
        if (adContainer != null) {
            adContainer.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AdManager.getInstance().preloadDualBiddingAds(this);
        fetchPosts();
        checkAndShowTourSpotlight();
    }

    private void checkAndShowTourSpotlight() {
        int stage = OnboardingTourManager.getInstance(this).getTourStage(this);
        if (stage == OnboardingTourManager.STAGE_FEED_POST) {
            View fabCreate = findViewById(R.id.fab_create_post);
            if (fabCreate != null) {
                fabCreate.post(() -> {
                    SpotlightOverlayView.attach(this, fabCreate,
                            "Tap to create a post",
                            true, android.graphics.Color.parseColor("#00FFFF"), () -> {
                                SpotlightOverlayView.detach(this);
                                handleCreatePostClick(true);
                            });
                });
            }
        } else if (stage == OnboardingTourManager.STAGE_RETURN_HOME) {
            View backBtn = findViewById(R.id.back_button_container);
            if (backBtn != null) {
                backBtn.post(() -> {
                    SpotlightOverlayView.attach(this, backBtn,
                            "Tap to return to Home",
                            true, android.graphics.Color.parseColor("#10B981"), () -> {
                                OnboardingTourManager.getInstance(this).setTourStage(this, OnboardingTourManager.STAGE_RANDOM_CALL);
                                SpotlightOverlayView.detach(this);
                                finish();
                            });
                });
            }
        } else {
            SpotlightOverlayView.detach(this);
        }
    }

    private void handleCreatePostClick(boolean isOnboardingPost) {
        TokenManager tokenManager = TokenManager.getInstance(this);
        String gender = tokenManager.getUserGender();
        boolean isVerified = tokenManager.isVerified();
        boolean femaleConfirmed = tokenManager.isFemalePostPrefConfirmed();

        // If female user has chosen female gender, is unverified, and has not confirmed preference yet:
        // Ask ONLY ONE TIME when she clicks the + icon to create a post!
        if ("female".equalsIgnoreCase(gender) && !isVerified && !femaleConfirmed) {
            PreferenceSelectionDialog dialog = new PreferenceSelectionDialog(this, selectedPref -> {
                tokenManager.setSexPreference(selectedPref);
                tokenManager.setFemalePostPrefConfirmed(true);
                proceedToCreatePost(isOnboardingPost);
            });
            dialog.show();
            return;
        }

        String preference = tokenManager.getSexPreference();
        if (preference == null || preference.trim().isEmpty()) {
            PreferenceSelectionDialog dialog = new PreferenceSelectionDialog(this, selectedPref -> {
                tokenManager.setSexPreference(selectedPref);
                proceedToCreatePost(isOnboardingPost);
            });
            dialog.show();
            return;
        }

        proceedToCreatePost(isOnboardingPost);
    }

    private void proceedToCreatePost(boolean isOnboardingPost) {
        AdManager.getInstance().preloadDualBiddingAds(this);
        Intent createIntent = new Intent(this, CreatePostActivity.class);
        if (isOnboardingPost) {
            createIntent.putExtra("is_onboarding_post", true);
        }
        startActivity(createIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setCategory(String category) {
        currentCategory = category;
        updateFilterButtons();
        fetchPosts();
    }

    private void updateFilterButtons() {
        filterAll.setSelected("all".equals(currentCategory));
        filterMale.setSelected("male".equals(currentCategory));
        filterFemale.setSelected("female".equals(currentCategory));
    }

    private String optString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private int optInt(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean optBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }

    private void fetchPosts() {
        if (isFetching) {
            return;
        }
        isFetching = true;

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        // Use API to fetch posts
        String queryCategory = currentCategory.equals("all") ? null : currentCategory;

        api.getPosts(queryCategory, null, 50, 0).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isFetching = false;
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        postList.clear();
                        if (data.has("posts") && !data.get("posts").isJsonNull()) {
                            JsonArray postsArray = data.getAsJsonArray("posts");
                            for (JsonElement element : postsArray) {
                                try {
                                    if (element == null || !element.isJsonObject()) continue;
                                    JsonObject postObj = element.getAsJsonObject();
                                    Post post = new Post();
                                    post.setId(optString(postObj, "id", ""));
                                    post.setText(optString(postObj, "text", ""));
                                    post.setCategory(optString(postObj, "category", "all"));
                                    post.setUserId(optString(postObj, "userId", ""));
                                    post.setUsername(optString(postObj, "username", "Anonymous"));
                                    post.setUserAvatar(optString(postObj, "userAvatar", "av1"));
                                    post.setLikeCount(optInt(postObj, "likeCount", 0));
                                    post.setCommentCount(optInt(postObj, "commentCount", 0));
                                    post.setLikedByMe(optBoolean(postObj, "likedByMe", false));
                                    post.setCreatedAt(optString(postObj, "createdAt", ""));
                                    post.setVerified(optBoolean(postObj, "verified", false));
                                    post.setGender(optString(postObj, "gender", "male"));
                                    post.setSexPreference(optString(postObj, "sexPreference", "Straight"));

                                    // Enforce post privacy based on current user's gender
                                    String postCat = post.getCategory();
                                    String myGender = tokenManager.getUserGender();
                                    if ("male".equalsIgnoreCase(myGender) && "female".equalsIgnoreCase(postCat)) {
                                        continue;
                                    }
                                    if ("female".equalsIgnoreCase(myGender) && "male".equalsIgnoreCase(postCat)) {
                                        continue;
                                    }

                                    postList.add(post);
                                } catch (Throwable itemEx) {
                                    Log.e(TAG, "Error parsing post item, skipping", itemEx);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (postsRecyclerView != null) {
                            postsRecyclerView.scrollToPosition(0);
                        }
                        updateEmptyState(false, null);
                        return;
                    }
                }
                updateEmptyState(true, getString(R.string.failed_to_load_posts));
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isFetching = false;
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Log.e(TAG, "Failed to load posts", t);
                updateEmptyState(true, getString(R.string.network_error_retry));
            }
        });
    }

    private void updateEmptyState(boolean isError, String errorMessage) {
        if (postList.isEmpty()) {
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
            if (tvEmptyMessage != null) {
                if (errorMessage != null) {
                    tvEmptyMessage.setText(errorMessage);
                } else {
                    tvEmptyMessage.setText(R.string.no_posts_available);
                }
            }
        } else {
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
        }
        if (postsRecyclerView != null) {
            postsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileUpdateListener != null) {
            ProfileUpdateManager.getInstance(this).unregisterListener(profileUpdateListener);
        }
    }
}
