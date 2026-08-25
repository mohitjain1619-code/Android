package com.mohitt.camverz;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryViewerActivity extends BaseActivity {

    private FrameLayout storyViewerCanvas;
    private ImageView ivStoryImage;
    private VideoView vvStoryVideo;
    private TextView tvStoryTextOnly;
    private TextView tvStoryOverlayText;

    private View navLeftClick, navRightClick;
    private LinearLayout progressBarsLayout;
    private de.hdodenhof.circleimageview.CircleImageView ivViewerAvatar;
    private TextView tvViewerName;
    private TextView btnStoryClose;
    private ImageView btnDeleteStory;

    private UserStories userStories;
    private List<StoryItem> storiesList;
    private List<ProgressBar> progressBars = new ArrayList<>();

    private int currentStoryIndex = 0;
    private int currentProgress = 0;
    private long storyDurationMs = 5000;
    private boolean isPaused = false;
    private boolean isVideoPrepared = false;

    private final Handler progressHandler = new Handler();
    private static final int PROGRESS_INTERVAL = 50;

    private ApiService api;
    private TokenManager tokenManager;

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (isPaused) {
                progressHandler.postDelayed(this, PROGRESS_INTERVAL);
                return;
            }
            
            // For video story, only start counting down once video is fully loaded and playing
            if ("VIDEO".equalsIgnoreCase(storiesList.get(currentStoryIndex).getType()) && !isVideoPrepared) {
                progressHandler.postDelayed(this, PROGRESS_INTERVAL);
                return;
            }

            currentProgress += (int) (100 * PROGRESS_INTERVAL / storyDurationMs);
            if (currentProgress >= 100) {
                currentProgress = 100;
                progressBars.get(currentStoryIndex).setProgress(100);
                showNextStory();
            } else {
                progressBars.get(currentStoryIndex).setProgress(currentProgress);
                progressHandler.postDelayed(this, PROGRESS_INTERVAL);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        userStories = (UserStories) getIntent().getSerializableExtra("userStories");
        if (userStories == null || userStories.getStories() == null || userStories.getStories().isEmpty()) {
            Toast.makeText(this, "No active stories", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        storiesList = userStories.getStories();

        storyViewerCanvas = findViewById(R.id.storyViewerCanvas);
        ivStoryImage = findViewById(R.id.ivStoryImage);
        vvStoryVideo = findViewById(R.id.vvStoryVideo);
        tvStoryTextOnly = findViewById(R.id.tvStoryTextOnly);
        tvStoryOverlayText = findViewById(R.id.tvStoryOverlayText);

        navLeftClick = findViewById(R.id.navLeftClick);
        navRightClick = findViewById(R.id.navRightClick);
        progressBarsLayout = findViewById(R.id.progressBarsLayout);
        ivViewerAvatar = findViewById(R.id.ivViewerAvatar);
        tvViewerName = findViewById(R.id.tvViewerName);
        btnStoryClose = findViewById(R.id.btnStoryClose);
        btnDeleteStory = findViewById(R.id.btnDeleteStory);

        tvViewerName.setText(userStories.getUserName());
        AvatarHelper.loadAvatar(this, null, userStories.getUserAvatar(), userStories.getUserName(), ivViewerAvatar);

        btnStoryClose.setOnClickListener(v -> finish());

        // Admin delete visibility check
        String currentEmail = tokenManager.getUserEmail();
        if (currentEmail != null && currentEmail.equalsIgnoreCase("mohitjain1619@gmail.com")) {
            btnDeleteStory.setVisibility(View.VISIBLE);
            btnDeleteStory.setOnClickListener(v -> confirmDeleteStory());
        }

        // Tap navigations
        navLeftClick.setOnClickListener(v -> showPreviousStory());
        navRightClick.setOnClickListener(v -> showNextStory());

        // Setup progress bars
        setupSegmentedProgressBars();

        // Load the first story
        loadStory(0);
    }

    private void setupSegmentedProgressBars() {
        progressBarsLayout.removeAllViews();
        progressBars.clear();

        for (int i = 0; i < storiesList.size(); i++) {
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            lp.setMargins(4, 0, 4, 0);
            pb.setLayoutParams(lp);
            pb.setMax(100);
            pb.setProgress(0);

            // Style progress indicators white with semi-transparent background
            pb.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
            pb.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#44FFFFFF")));

            progressBarsLayout.addView(pb);
            progressBars.add(pb);
        }
    }

    private void loadStory(int index) {
        if (index < 0 || index >= storiesList.size()) return;

        // Reset state
        progressHandler.removeCallbacks(progressRunnable);
        currentStoryIndex = index;
        currentProgress = 0;
        isVideoPrepared = false;
        isPaused = false;

        StoryItem story = storiesList.get(index);

        // Update progress bar views state
        for (int i = 0; i < progressBars.size(); i++) {
            if (i < index) progressBars.get(i).setProgress(100);
            else progressBars.get(i).setProgress(0);
        }

        // Hide all views first
        ivStoryImage.setVisibility(View.GONE);
        vvStoryVideo.setVisibility(View.GONE);
        tvStoryTextOnly.setVisibility(View.GONE);
        tvStoryOverlayText.setVisibility(View.GONE);

        String baseUrl = com.mohitt.camverz.BuildConfig.BASE_URL;

        if ("TEXT".equalsIgnoreCase(story.getType())) {
            tvStoryTextOnly.setVisibility(View.VISIBLE);
            tvStoryTextOnly.setText(story.getTextContent());
            applyTextStoryTheme(story.getBgGradient(), story.getTextColor());
            storyDurationMs = 5000;
            startProgressCountdown();
        } else if ("IMAGE".equalsIgnoreCase(story.getType())) {
            ivStoryImage.setVisibility(View.VISIBLE);
            storyViewerCanvas.setBackgroundColor(Color.BLACK);

            String fullUrl = baseUrl + story.getMediaUrl();
            
            // Caching enabled for image load optimization
            Glide.with(this)
                    .load(fullUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivStoryImage);

            if (story.getTextContent() != null && !story.getTextContent().isEmpty()) {
                tvStoryOverlayText.setVisibility(View.VISIBLE);
                tvStoryOverlayText.setText(story.getTextContent());
            }

            storyDurationMs = 5000;
            startProgressCountdown();
        } else if ("VIDEO".equalsIgnoreCase(story.getType())) {
            vvStoryVideo.setVisibility(View.VISIBLE);
            storyViewerCanvas.setBackgroundColor(Color.BLACK);

            String fullUrl = baseUrl + story.getMediaUrl();
            vvStoryVideo.setVideoURI(Uri.parse(fullUrl));

            if (story.getTextContent() != null && !story.getTextContent().isEmpty()) {
                tvStoryOverlayText.setVisibility(View.VISIBLE);
                tvStoryOverlayText.setText(story.getTextContent());
            }

            vvStoryVideo.setOnPreparedListener(mp -> {
                isVideoPrepared = true;
                storyDurationMs = mp.getDuration() > 0 ? mp.getDuration() : 5000;
                vvStoryVideo.start();
            });

            vvStoryVideo.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show();
                showNextStory();
                return true;
            });

            startProgressCountdown();
        }
    }

    private void applyTextStoryTheme(String theme, String textColor) {
        int color = Color.WHITE;
        try {
            if (textColor != null && !textColor.isEmpty()) {
                color = Color.parseColor(textColor);
            }
        } catch(Exception e){}
        tvStoryTextOnly.setTextColor(color);

        if (theme == null || theme.isEmpty()) {
            tvStoryTextOnly.setBackgroundResource(R.drawable.bg_community_hot_gradient);
            return;
        }

        if (theme.equals("bg_community_hot_gradient")) {
            tvStoryTextOnly.setBackgroundResource(R.drawable.bg_community_hot_gradient);
        } else if (theme.equals("bg_neon_amber_button")) {
            tvStoryTextOnly.setBackgroundResource(R.drawable.bg_neon_amber_button);
        } else if (theme.equals("cyan")) {
            tvStoryTextOnly.setBackgroundColor(Color.parseColor("#00E5FF"));
        } else if (theme.equals("dark")) {
            tvStoryTextOnly.setBackgroundColor(Color.parseColor("#1A1A1A"));
        } else {
            tvStoryTextOnly.setBackgroundResource(R.drawable.bg_community_hot_gradient);
        }
    }

    private void startProgressCountdown() {
        progressHandler.postDelayed(progressRunnable, PROGRESS_INTERVAL);
    }

    private void showNextStory() {
        if (currentStoryIndex + 1 < storiesList.size()) {
            loadStory(currentStoryIndex + 1);
        } else {
            // No more stories for this user, close viewer
            finish();
        }
    }

    private void showPreviousStory() {
        if (currentStoryIndex - 1 >= 0) {
            loadStory(currentStoryIndex - 1);
        } else {
            // Loop or keep on first
            loadStory(0);
        }
    }

    private void confirmDeleteStory() {
        isPaused = true;
        new AlertDialog.Builder(this)
                .setTitle("Delete Story")
                .setMessage("Are you sure you want to delete this story?")
                .setPositiveButton("Delete", (dialog, which) -> deleteStoryFromServer())
                .setNegativeButton("Cancel", (dialog, which) -> isPaused = false)
                .setCancelable(false)
                .show();
    }

    private void deleteStoryFromServer() {
        String storyId = storiesList.get(currentStoryIndex).getId();
        api.deleteStory(storyId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isPaused = false;
                if (response.isSuccessful()) {
                    Toast.makeText(StoryViewerActivity.this, "Story deleted", Toast.LENGTH_SHORT).show();
                    // Re-route stories list locally or exit if last story
                    storiesList.remove(currentStoryIndex);
                    if (storiesList.isEmpty()) {
                        finish();
                    } else {
                        // Re-initialize progress indicators mapping
                        setupSegmentedProgressBars();
                        if (currentStoryIndex >= storiesList.size()) {
                            loadStory(storiesList.size() - 1);
                        } else {
                            loadStory(currentStoryIndex);
                        }
                    }
                } else {
                    Toast.makeText(StoryViewerActivity.this, "Failed to delete story", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isPaused = false;
                Toast.makeText(StoryViewerActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        if (vvStoryVideo.isPlaying()) {
            vvStoryVideo.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        if (isVideoPrepared && !vvStoryVideo.isPlaying()) {
            vvStoryVideo.start();
        }
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(progressRunnable);
        super.onDestroy();
    }
}
