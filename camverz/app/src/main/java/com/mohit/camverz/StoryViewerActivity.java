package com.mohit.camverz;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;

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
    private ProgressBar pbStoryLoading;

    private View navLeftClick, navRightClick;
    private LinearLayout progressBarsLayout;
    private de.hdodenhof.circleimageview.CircleImageView ivViewerAvatar;
    private TextView tvViewerName;
    private TextView btnStoryClose;
    private ImageView btnDeleteStory;
    private TextView btnStoryViews;
    private TextView btnSendMessage;

    private UserStories userStories;
    private List<StoryItem> storiesList;
    private List<ProgressBar> progressBars = new ArrayList<>();

    private int currentStoryIndex = 0;
    private int currentProgress = 0;
    private float currentProgressFloat = 0f;
    private long storyDurationMs = 6000;
    private boolean isPaused = false;
    private boolean isVideoPrepared = false;
    private int videoCurrentLoop = 0;
    private int videoTargetLoops = 1;
    private int videoSingleLoopDuration = 0;
    private boolean isNavigatingStory = false;

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

            if (currentStoryIndex < 0 || currentStoryIndex >= storiesList.size()) return;
            StoryItem story = storiesList.get(currentStoryIndex);

            if ("VIDEO".equalsIgnoreCase(story.getType())) {
                if (!isVideoPrepared || vvStoryVideo == null) {
                    progressHandler.postDelayed(this, PROGRESS_INTERVAL);
                    return;
                }
                // Accurate video progress tracking based on player position and loop count
                int curPos = vvStoryVideo.getCurrentPosition();
                if (videoSingleLoopDuration > 0) {
                    if (curPos > videoSingleLoopDuration) curPos = videoSingleLoopDuration;
                    long totalPlayed = ((long) videoCurrentLoop * videoSingleLoopDuration) + curPos;
                    if (storyDurationMs > 0) {
                        currentProgress = (int) Math.min(100f, ((float) totalPlayed * 100f / (float) storyDurationMs));
                    }
                }
            } else {
                // Smooth float progress for images and text
                currentProgressFloat += (100f * PROGRESS_INTERVAL / (float) storyDurationMs);
                currentProgress = (int) currentProgressFloat;
            }

            if (currentProgress >= 100) {
                currentProgress = 100;
                if (currentStoryIndex < progressBars.size()) {
                    progressBars.get(currentStoryIndex).setProgress(100);
                }
                showNextStory();
            } else {
                if (currentStoryIndex < progressBars.size()) {
                    progressBars.get(currentStoryIndex).setProgress(currentProgress);
                }
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
        pbStoryLoading = findViewById(R.id.pbStoryLoading);

        navLeftClick = findViewById(R.id.navLeftClick);
        navRightClick = findViewById(R.id.navRightClick);
        progressBarsLayout = findViewById(R.id.progressBarsLayout);
        ivViewerAvatar = findViewById(R.id.ivViewerAvatar);
        tvViewerName = findViewById(R.id.tvViewerName);
        btnStoryClose = findViewById(R.id.btnStoryClose);
        btnDeleteStory = findViewById(R.id.btnDeleteStory);
        btnStoryViews = findViewById(R.id.btnStoryViews);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        tvViewerName.setText(userStories.getUserName());
        AvatarHelper.loadAvatar(this, null, userStories.getUserAvatar(), userStories.getUserName(), ivViewerAvatar);

        applyWindowInsets(findViewById(R.id.topHeaderLayout), findViewById(R.id.storyBottomLayout));

        View.OnClickListener openProfileListener = v -> {
            if (userStories != null && userStories.getUserId() != null) {
                isPaused = true;
                Intent intent = new Intent(StoryViewerActivity.this, ProfileActivity.class);
                intent.putExtra("userId", userStories.getUserId());
                startActivity(intent);
            }
        };
        ivViewerAvatar.setOnClickListener(openProfileListener);
        tvViewerName.setOnClickListener(openProfileListener);

        btnStoryClose.setOnClickListener(v -> finish());

        // Admin delete visibility check
        String currentEmail = tokenManager.getUserEmail();
        if (currentEmail != null && currentEmail.equalsIgnoreCase("mohitjain1619@gmail.com")) {
            btnDeleteStory.setVisibility(View.VISIBLE);
            btnDeleteStory.setOnClickListener(v -> confirmDeleteStory());
        }

        // Setup touch navigation (tap to switch, press and hold to pause)
        setupTouchNavigation();

        // Setup progress bars
        setupSegmentedProgressBars();

        // Load the first story
        loadStory(0);
    }

    private void setupTouchNavigation() {
        View.OnTouchListener leftTouchListener = (v, event) -> handleTouch(event, true);
        View.OnTouchListener rightTouchListener = (v, event) -> handleTouch(event, false);

        navLeftClick.setOnTouchListener(leftTouchListener);
        navRightClick.setOnTouchListener(rightTouchListener);
    }

    private long touchDownTime = 0;
    private float touchDownX = 0;
    private float touchDownY = 0;
    private static final long TAP_MAX_DURATION_MS = 250;
    private static final float TAP_MAX_DISTANCE_PX = 30f;

    private boolean handleTouch(android.view.MotionEvent event, boolean isLeft) {
        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                touchDownTime = System.currentTimeMillis();
                touchDownX = event.getRawX();
                touchDownY = event.getRawY();
                pauseStory();
                return true;

            case android.view.MotionEvent.ACTION_UP:
                long duration = System.currentTimeMillis() - touchDownTime;
                float deltaX = Math.abs(event.getRawX() - touchDownX);
                float deltaY = Math.abs(event.getRawY() - touchDownY);

                if (duration <= TAP_MAX_DURATION_MS && deltaX <= TAP_MAX_DISTANCE_PX && deltaY <= TAP_MAX_DISTANCE_PX) {
                    resumeStory();
                    if (isLeft) {
                        showPreviousStory();
                    } else {
                        showNextStory();
                    }
                } else {
                    // Was held down to pause, resume now
                    resumeStory();
                }
                return true;

            case android.view.MotionEvent.ACTION_CANCEL:
                resumeStory();
                return true;
        }
        return false;
    }

    private void pauseStory() {
        isPaused = true;
        try {
            if (vvStoryVideo != null && vvStoryVideo.isPlaying()) {
                vvStoryVideo.pause();
            }
        } catch (Exception ignored) {}
    }

    private void resumeStory() {
        isPaused = false;
        try {
            if (isVideoPrepared && vvStoryVideo != null && !vvStoryVideo.isPlaying()) {
                vvStoryVideo.start();
            }
        } catch (Exception ignored) {}
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
        isNavigatingStory = false;
        progressHandler.removeCallbacks(progressRunnable);
        try {
            if (vvStoryVideo != null) {
                vvStoryVideo.stopPlayback();
            }
        } catch (Exception ignored) {}

        currentStoryIndex = index;
        currentProgress = 0;
        currentProgressFloat = 0f;
        videoCurrentLoop = 0;
        videoTargetLoops = 1;
        videoSingleLoopDuration = 0;
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

        String currentUserId = tokenManager.getUserId();
        String authorUserId = userStories.getUserId();
        boolean isStoryOwner = (currentUserId != null && currentUserId.equalsIgnoreCase(authorUserId))
                || ("mohitjain1619@gmail.com".equalsIgnoreCase(tokenManager.getUserEmail()));

        if (isStoryOwner) {
            btnSendMessage.setVisibility(View.GONE);
            btnStoryViews.setVisibility(View.VISIBLE);
            btnStoryViews.setText("👁️ " + story.getViewsCount() + " views");
            btnStoryViews.setOnClickListener(v -> showStoryViewersBottomSheet(story.getId()));
        } else {
            btnStoryViews.setVisibility(View.GONE);
            btnSendMessage.setVisibility(View.VISIBLE);
            btnSendMessage.setOnClickListener(v -> {
                isPaused = true;
                Intent intent = new Intent(StoryViewerActivity.this, ChatActivity.class);
                intent.putExtra("userId", userStories.getUserId());
                intent.putExtra("userName", userStories.getUserName());
                intent.putExtra("userAvatar", userStories.getUserAvatar());
                intent.putExtra("userPhotoUrl", userStories.getUserPhotoUrl());
                startActivity(intent);
            });
        }

        // Record story view on backend (only if viewer is not the author)
        if (!isStoryOwner) {
            api.recordStoryView(story.getId()).enqueue(new Callback<JsonObject>() {
                @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
            });
        }

        String baseUrl = com.mohit.camverz.BuildConfig.BASE_URL;
        String mediaUrl = story.getMediaUrl();
        String fullUrl;
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
            fullUrl = "";
        } else if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) {
            fullUrl = mediaUrl;
        } else {
            String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String cleanPath = mediaUrl.startsWith("/") ? mediaUrl : "/" + mediaUrl;
            fullUrl = cleanBase + cleanPath;
        }

        if ("TEXT".equalsIgnoreCase(story.getType())) {
            if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.GONE);
            tvStoryTextOnly.setVisibility(View.VISIBLE);
            tvStoryTextOnly.setText(story.getTextContent());
            applyTextStoryTheme(story.getBgGradient(), story.getTextColor());
            storyDurationMs = 6000;
            currentProgressFloat = 0f;
            startProgressCountdown();
        } else if ("IMAGE".equalsIgnoreCase(story.getType())) {
            ivStoryImage.setVisibility(View.VISIBLE);
            storyViewerCanvas.setBackgroundColor(Color.BLACK);
            if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.VISIBLE);

            if (story.getTextContent() != null && !story.getTextContent().isEmpty()) {
                tvStoryOverlayText.setVisibility(View.VISIBLE);
                tvStoryOverlayText.setText(story.getTextContent());
            }

            Glide.with(this)
                    .load(fullUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            runOnUiThread(() -> {
                                if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.GONE);
                                Toast.makeText(StoryViewerActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                                showNextStory();
                            });
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            runOnUiThread(() -> {
                                if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.GONE);
                                storyDurationMs = 6000;
                                currentProgressFloat = 0f;
                                startProgressCountdown();
                            });
                            return false;
                        }
                    })
                    .into(ivStoryImage);
        } else if ("VIDEO".equalsIgnoreCase(story.getType())) {
            vvStoryVideo.setVisibility(View.VISIBLE);
            storyViewerCanvas.setBackgroundColor(Color.BLACK);
            if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.VISIBLE);

            if (story.getTextContent() != null && !story.getTextContent().isEmpty()) {
                tvStoryOverlayText.setVisibility(View.VISIBLE);
                tvStoryOverlayText.setText(story.getTextContent());
            }

            if (fullUrl == null || fullUrl.trim().isEmpty()) {
                if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.GONE);
                Toast.makeText(this, "Story media not found", Toast.LENGTH_SHORT).show();
                showNextStory();
                return;
            }

            // CRITICAL: Set listeners BEFORE setVideoURI to catch all player lifecycle events
            vvStoryVideo.setOnPreparedListener(mp -> {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.GONE);
                    isVideoPrepared = true;
                    int dur = mp.getDuration();
                    videoSingleLoopDuration = dur > 0 ? dur : 5000;
                    videoCurrentLoop = 0;

                    // If video is short (under ~6.5 seconds), loop 3 times before advancing like Instagram
                    if (videoSingleLoopDuration <= 6500) {
                        videoTargetLoops = 3;
                        storyDurationMs = (long) videoSingleLoopDuration * videoTargetLoops;
                    } else {
                        videoTargetLoops = 1;
                        storyDurationMs = videoSingleLoopDuration;
                    }

                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    if (videoWidth > 0 && videoHeight > 0) {
                        float videoAspect = (float) videoWidth / videoHeight;
                        int containerWidth = storyViewerCanvas.getWidth();
                        int containerHeight = storyViewerCanvas.getHeight();
                        if (containerWidth > 0 && containerHeight > 0) {
                            float containerAspect = (float) containerWidth / containerHeight;
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) vvStoryVideo.getLayoutParams();
                            if (videoAspect > containerAspect) {
                                lp.width = containerWidth;
                                lp.height = (int) (containerWidth / videoAspect);
                            } else {
                                lp.height = containerHeight;
                                lp.width = (int) (containerHeight * videoAspect);
                            }
                            lp.gravity = android.view.Gravity.CENTER;
                            vvStoryVideo.setLayoutParams(lp);
                        }
                    }
                    try {
                        vvStoryVideo.start();
                    } catch (Exception e) {
                        android.util.Log.e("StoryViewerActivity", "Error starting video: " + e.getMessage());
                    }
                    currentProgressFloat = 0f;
                    startProgressCountdown();
                });
            });

            vvStoryVideo.setOnCompletionListener(mp -> {
                videoCurrentLoop++;
                if (videoCurrentLoop < videoTargetLoops) {
                    try {
                        vvStoryVideo.seekTo(0);
                        vvStoryVideo.start();
                    } catch (Exception e) {
                        android.util.Log.e("StoryViewerActivity", "Error re-looping video: " + e.getMessage());
                        showNextStory();
                    }
                } else {
                    showNextStory();
                }
            });

            vvStoryVideo.setOnErrorListener((mp, what, extra) -> {
                runOnUiThread(() -> {
                    try {
                        if (vvStoryVideo != null) {
                            vvStoryVideo.stopPlayback();
                        }
                    } catch (Exception ignored) {}
                    if (pbStoryLoading != null) pbStoryLoading.setVisibility(View.GONE);
                    android.util.Log.e("StoryViewerActivity", "Video load error: what=" + what + ", extra=" + extra + " url=" + fullUrl);
                    Toast.makeText(this, "Story video unavailable", Toast.LENGTH_SHORT).show();
                    showNextStory();
                });
                return true;
            });

            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Bypass-Tunnel-Reminder", "true");
            headers.put("localtunnel-bypass", "true");
            vvStoryVideo.setVideoURI(Uri.parse(fullUrl), headers);
        }
    }

    private void showStoryViewersBottomSheet(String storyId) {
        isPaused = true;
        if (vvStoryVideo.isPlaying()) vvStoryVideo.pause();

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_story_viewers, null);
        dialog.setContentView(dialogView);

        TextView tvViewerTitle = dialogView.findViewById(R.id.tvViewerTitle);
        RecyclerView rvStoryViewers = dialogView.findViewById(R.id.rvStoryViewers);
        ProgressBar pbLoading = dialogView.findViewById(R.id.pbLoading);

        rvStoryViewers.setLayoutManager(new LinearLayoutManager(this));

        dialog.setOnDismissListener(d -> {
            isPaused = false;
            if (isVideoPrepared && !vvStoryVideo.isPlaying()) {
                vvStoryVideo.start();
            }
        });

        api.getStoryViewers(storyId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("viewers")) {
                        JsonArray viewers = data.getAsJsonArray("viewers");
                        int count = data.has("viewCount") ? data.get("viewCount").getAsInt() : viewers.size();
                        tvViewerTitle.setText("Story Views (" + count + ")");

                        StoryViewersAdapter adapter = new StoryViewersAdapter(StoryViewerActivity.this, viewers, viewerUserId -> {
                            dialog.dismiss();
                            Intent intent = new Intent(StoryViewerActivity.this, ProfileActivity.class);
                            intent.putExtra("userId", viewerUserId);
                            startActivity(intent);
                        });
                        rvStoryViewers.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(StoryViewerActivity.this, "Failed to load viewers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
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
        if (isNavigatingStory || isFinishing() || isDestroyed()) return;
        isNavigatingStory = true;
        try {
            if (vvStoryVideo != null) {
                vvStoryVideo.stopPlayback();
            }
        } catch (Exception ignored) {}
        if (currentStoryIndex + 1 < storiesList.size()) {
            loadStory(currentStoryIndex + 1);
        } else {
            // No more stories for this user, close viewer
            finish();
        }
    }

    private void showPreviousStory() {
        if (isNavigatingStory || isFinishing() || isDestroyed()) return;
        isNavigatingStory = true;
        try {
            if (vvStoryVideo != null) {
                vvStoryVideo.stopPlayback();
            }
        } catch (Exception ignored) {}
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
        pauseStory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeStory();
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(progressRunnable);
        try {
            if (vvStoryVideo != null) {
                vvStoryVideo.stopPlayback();
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
