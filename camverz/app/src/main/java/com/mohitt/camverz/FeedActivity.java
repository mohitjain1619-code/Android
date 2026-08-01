package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.graphics.Color;

import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedActivity extends BaseActivity {

    private static final String TAG = "FeedActivity";
    private RecyclerView postsRecyclerView;
    private FloatingActionButton fabCreatePost;
    private TextView filterAll, filterMale, filterFemale;
    private SwipeRefreshLayout swipeRefreshLayout;

    private PostAdapter adapter;
    private List<Post> postList;
    private String currentCategory = "all";
    
    private ApiService api;
    private TokenManager tokenManager;
    private MaxAdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.top_bar), findViewById(R.id.floating_menu_container));

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> finish());

        // Create Post Button
        findViewById(R.id.fab_create_post).setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
        fetchPosts();
        loadBannerAd();
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

    private void fetchPosts() {
        swipeRefreshLayout.setRefreshing(true);
        
        // Use API to fetch posts
        String queryCategory = currentCategory.equals("all") ? null : currentCategory;
        
        api.getPosts(queryCategory, null, 50, 0).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        postList.clear();
                        if (data.has("posts")) {
                            JsonArray postsArray = data.getAsJsonArray("posts");
                            for (JsonElement element : postsArray) {
                                JsonObject postObj = element.getAsJsonObject();
                                Post post = new Post();
                                post.setId(postObj.has("id") ? postObj.get("id").getAsString() : "");
                                post.setText(postObj.has("text") ? postObj.get("text").getAsString() : "");
                                post.setCategory(postObj.has("category") ? postObj.get("category").getAsString() : "");
                                post.setUserId(postObj.has("userId") ? postObj.get("userId").getAsString() : "");
                                post.setUsername(postObj.has("username") ? postObj.get("username").getAsString() : "");
                                post.setUserAvatar(postObj.has("userAvatar") ? postObj.get("userAvatar").getAsString() : "");
                                post.setLikeCount(postObj.has("likeCount") ? postObj.get("likeCount").getAsInt() : 0);
                                post.setCommentCount(postObj.has("commentCount") ? postObj.get("commentCount").getAsInt() : 0);
                                post.setLikedByMe(postObj.has("likedByMe") && postObj.get("likedByMe").getAsBoolean());
                                post.setCreatedAt(postObj.has("createdAt") ? postObj.get("createdAt").getAsString() : "");
                                
                                // Enforce post privacy based on current user's gender
                                String postCat = post.getCategory();
                                String myGender = tokenManager.getUserGender();
                                if ("male".equalsIgnoreCase(myGender) && "female".equalsIgnoreCase(postCat)) {
                                    // Skip female-only post for male user
                                    continue;
                                }
                                if ("female".equalsIgnoreCase(myGender) && "male".equalsIgnoreCase(postCat)) {
                                    // Skip male-only post for female user
                                    continue;
                                }

                                postList.add(post);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        return;
                    }
                }
                Toast.makeText(FeedActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Failed to load posts", t);
                Toast.makeText(FeedActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPosts();
    }

    private void loadBannerAd() {
        adView = new MaxAdView(getString(R.string.applovin_banner_ad_unit_id), this);

        // Set size (Match parent width, 50dp height for phones)
        int heightPx = AppLovinSdkUtils.dpToPx(this, 50);
        adView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heightPx
        ));

        // Background color is required for banners to function properly
        adView.setBackgroundColor(Color.TRANSPARENT);

        // Add to your layout
        FrameLayout adContainer = findViewById(R.id.banner_ad_container);
        if (adContainer != null) {
            adContainer.removeAllViews();
            adContainer.addView(adView);
            // Load the ad
            adView.loadAd();
            Log.d(TAG, "✅ AppLovin Banner Ad requested for FeedActivity");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }
}
