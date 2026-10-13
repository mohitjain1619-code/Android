package com.mohit.camverz;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Create Post — TEXT ONLY.
 * Requires watching a full LevelPlay Bidding Rewarded Video ad before posting.
 * If user stops/closes early or kills the app, the post is NOT uploaded.
 */
public class CreatePostActivity extends BaseActivity {

    private static final String TAG = "CreatePostActivity";
    private static final String PLACEMENT = "create_post";

    private EditText postText;
    private Button postButton;
    private RadioGroup categoryGroup;
    private ApiService api;
    private TokenManager tokenManager;

    private boolean isPosting = false;
    private boolean isAdLoading = false;
    private boolean isWaitingForAd = false;
    private boolean isRewardEarned = false;

    private String pendingPostText = "";
    private String pendingCategory = "";

    private LevelPlayRewardedAd levelPlayRewardedAd;
    private ProgressDialog adProgressDialog;
    private final Handler adHandler = new Handler(Looper.getMainLooper());
    private Runnable adTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.toolbar), findViewById(R.id.post_button));

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> {
            if (isPosting || BaseActivity.isAdShowing || isWaitingForAd) {
                Toast.makeText(CreatePostActivity.this, "Please wait...", Toast.LENGTH_SHORT).show();
                return;
            }
            finish();
        });

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        postText = findViewById(R.id.post_text);
        postButton = findViewById(R.id.post_button);
        categoryGroup = findViewById(R.id.category_group);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isPosting || BaseActivity.isAdShowing || isWaitingForAd) {
                    Toast.makeText(CreatePostActivity.this, "Please wait...", Toast.LENGTH_SHORT).show();
                    return;
                }
                finish();
            }
        });

        postButton.setOnClickListener(v -> {
            if (!isPosting && !isWaitingForAd) {
                uploadPost();
            }
        });

        // Preload LevelPlay Mediation Bidding Rewarded Video ad immediately while user types
        loadLevelPlayRewardedAd();
    }

    private void loadLevelPlayRewardedAd() {
        if (isAdLoading) return;
        if (levelPlayRewardedAd != null && levelPlayRewardedAd.isAdReady()) {
            Log.d(TAG, "LevelPlay Rewarded Ad already loaded and ready.");
            return;
        }

        AdManager.getInstance().runWhenLevelPlayReady(this, () -> {
            loadLevelPlayRewardedAdInternal();
        });
    }

    private void loadLevelPlayRewardedAdInternal() {
        if (isFinishing() || isDestroyed()) return;
        if (isAdLoading) return;
        if (levelPlayRewardedAd != null && levelPlayRewardedAd.isAdReady()) {
            Log.d(TAG, "LevelPlay Rewarded Ad already loaded and ready.");
            if (isWaitingForAd) {
                dismissAdLoadingDialog();
                isWaitingForAd = false;
                showLoadedRewardedAd();
            }
            return;
        }

        isAdLoading = true;
        try {
            if (levelPlayRewardedAd == null) {
                levelPlayRewardedAd = new LevelPlayRewardedAd(AdManager.LEVELPLAY_REWARDED_ID);
                levelPlayRewardedAd.setListener(new LevelPlayRewardedAdListener() {
                    @Override
                    public void onAdLoaded(LevelPlayAdInfo adInfo) {
                        runOnUiThread(() -> {
                            isAdLoading = false;
                            String winner = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "unknown";
                            Log.d(TAG, "🟢 [LEVELPLAY REWARDED LOADED] Winner Network: " + winner);
                            if (isWaitingForAd) {
                                dismissAdLoadingDialog();
                                isWaitingForAd = false;
                                showLoadedRewardedAd();
                            }
                        });
                    }

                    @Override
                    public void onAdLoadFailed(LevelPlayAdError error) {
                        runOnUiThread(() -> {
                            isAdLoading = false;
                            String errorMsg = (error != null) ? error.getErrorMessage() : "Unknown Error";
                            int code = (error != null) ? error.getErrorCode() : -1;
                            Log.e(TAG, "🔴 [LEVELPLAY REWARDED LOAD FAILED] (" + code + ") " + errorMsg);

                            if (code == 625) { // Load must be called after init success callback
                                Log.w(TAG, "⚠️ LevelPlay SDK init still finishing (625), waiting for init callback to retry...");
                                adHandler.postDelayed(() -> loadLevelPlayRewardedAd(), 1500);
                                return;
                            }

                            if (isWaitingForAd) {
                                dismissAdLoadingDialog();
                                isWaitingForAd = false;
                                isPosting = false;
                                postButton.setEnabled(true);
                                Toast.makeText(CreatePostActivity.this, "Unable to load video ad: " + errorMsg + ". Please try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onAdDisplayed(LevelPlayAdInfo adInfo) {
                        runOnUiThread(() -> {
                            BaseActivity.isAdShowing = true;
                            WindowBackBlocker.blockBack(CreatePostActivity.this);
                            String winner = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "ironsource";
                            Log.d(TAG, "📺 [LEVELPLAY REWARDED DISPLAYED] Fullscreen video playing from: " + winner);
                            AdAnalyticsTracker.trackEvent(CreatePostActivity.this, "IMPRESSION", "rewarded", winner, "DELIVERED", "", "", PLACEMENT);
                        });
                    }

                    @Override
                    public void onAdDisplayFailed(LevelPlayAdError error, LevelPlayAdInfo adInfo) {
                        runOnUiThread(() -> {
                            BaseActivity.isAdShowing = false;
                            isPosting = false;
                            postButton.setEnabled(true);
                            String winner = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "ironsource";
                            String errorMsg = (error != null) ? error.getErrorMessage() : "Display failed";
                            int code = (error != null) ? error.getErrorCode() : -1;
                            Log.e(TAG, "❌ [LEVELPLAY REWARDED DISPLAY FAILED] (" + code + ") " + errorMsg);
                            AdAnalyticsTracker.trackEvent(CreatePostActivity.this, "FAILED", "rewarded", winner, "FAILED", String.valueOf(code), errorMsg, PLACEMENT);
                            Toast.makeText(CreatePostActivity.this, "Ad display failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                            loadLevelPlayRewardedAd();
                        });
                    }

                    @Override
                    public void onAdClicked(LevelPlayAdInfo adInfo) {
                        runOnUiThread(() -> {
                            String winner = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "ironsource";
                            AdAnalyticsTracker.trackEvent(CreatePostActivity.this, "CLICKED", "rewarded", winner, "DELIVERED", "", "", PLACEMENT);
                        });
                    }

                    @Override
                    public void onAdRewarded(LevelPlayReward reward, LevelPlayAdInfo adInfo) {
                        runOnUiThread(() -> {
                            isRewardEarned = true;
                            String winner = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "ironsource";
                            Log.d(TAG, "🎁 [LEVELPLAY REWARD EARNED] Video completed! Winner: " + winner);
                            AdAnalyticsTracker.trackEvent(CreatePostActivity.this, "COMPLETED", "rewarded", winner, "DELIVERED", "", "", PLACEMENT);
                        });
                    }

                    @Override
                    public void onAdClosed(LevelPlayAdInfo adInfo) {
                        runOnUiThread(() -> {
                            BaseActivity.isAdShowing = false;
                            String winner = (adInfo != null && adInfo.getAdNetwork() != null) ? adInfo.getAdNetwork() : "ironsource";
                            Log.d(TAG, "⏹️ [LEVELPLAY REWARDED CLOSED] isRewardEarned=" + isRewardEarned);
                            AdAnalyticsTracker.trackEvent(CreatePostActivity.this, "CLOSED", "rewarded", winner, "DELIVERED", "", "", PLACEMENT);

                            if (isRewardEarned) {
                                // Full video was watched and reward was earned
                                Toast.makeText(CreatePostActivity.this, "Ad completed! Publishing post...", Toast.LENGTH_SHORT).show();
                                executePostUpload(pendingPostText, pendingCategory);
                            } else {
                                // User closed or stopped the ad before completion
                                isPosting = false;
                                postButton.setEnabled(true);
                                Toast.makeText(CreatePostActivity.this, "You must watch the full ad to publish your post.", Toast.LENGTH_LONG).show();
                                loadLevelPlayRewardedAd();
                            }
                        });
                    }

                    @Override
                    public void onAdInfoChanged(LevelPlayAdInfo adInfo) {}
                });
            }

            Log.d(TAG, "🚀 [LEVELPLAY REWARDED LOAD] Dispatching mediation auction request...");
            AdAnalyticsTracker.trackEvent(this, "REQUEST", "rewarded", "levelplay", "REQUESTED", "", "", PLACEMENT);
            levelPlayRewardedAd.loadAd();
        } catch (Exception e) {
            isAdLoading = false;
            Log.e(TAG, "Exception loading LevelPlay rewarded ad: " + e.getMessage(), e);
        }
    }

    private void uploadPost() {
        String text = postText.getText().toString().trim();
        int selectedCategoryId = categoryGroup.getCheckedRadioButtonId();

        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryId == -1) {
            new AlertDialog.Builder(this)
                .setTitle("Category Required")
                .setMessage("Please select a category for your post.")
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
            return;
        }

        RadioButton selectedCategory = findViewById(selectedCategoryId);
        String category = selectedCategory.getText().toString().toLowerCase();

        pendingPostText = text;
        pendingCategory = category;

        proceedWithRewardedAd();
    }

    private void proceedWithRewardedAd() {
        if (levelPlayRewardedAd != null && levelPlayRewardedAd.isAdReady()) {
            showLoadedRewardedAd();
        } else {
            // Ad not ready yet: buffer with spinner while auction completes
            isWaitingForAd = true;
            postButton.setEnabled(false);

            showAdLoadingDialog();

            if (!isAdLoading) {
                loadLevelPlayRewardedAd();
            }

            // 15-second safety timeout
            if (adTimeoutRunnable != null) {
                adHandler.removeCallbacks(adTimeoutRunnable);
            }
            adTimeoutRunnable = () -> {
                if (isWaitingForAd) {
                    isWaitingForAd = false;
                    dismissAdLoadingDialog();
                    isPosting = false;
                    postButton.setEnabled(true);
                    Toast.makeText(CreatePostActivity.this, "Ad is taking longer to load. Please check your internet and tap Post again.", Toast.LENGTH_LONG).show();
                }
            };
            adHandler.postDelayed(adTimeoutRunnable, 15000);
        }
    }

    private void showLoadedRewardedAd() {
        isRewardEarned = false;
        isPosting = true;
        postButton.setEnabled(false);
        Log.d(TAG, "🚀 [LEVELPLAY SHOW REWARDED] Showing rewarded video ad...");
        levelPlayRewardedAd.showAd(this);
    }

    private void showAdLoadingDialog() {
        if (isFinishing() || isDestroyed()) return;
        try {
            if (adProgressDialog == null) {
                adProgressDialog = new ProgressDialog(this);
                adProgressDialog.setMessage("Loading video ad to publish post... Please wait");
                adProgressDialog.setCancelable(false);
            }
            if (!adProgressDialog.isShowing()) {
                adProgressDialog.show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing ad progress dialog", e);
        }
    }

    private void dismissAdLoadingDialog() {
        if (adTimeoutRunnable != null) {
            adHandler.removeCallbacks(adTimeoutRunnable);
        }
        try {
            if (adProgressDialog != null && adProgressDialog.isShowing()) {
                adProgressDialog.dismiss();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if ((isPosting || BaseActivity.isAdShowing || isWaitingForAd) && event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if ((isPosting || BaseActivity.isAdShowing || isWaitingForAd) && keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void executePostUpload(String text, String category) {
        isPosting = true;
        postButton.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("text", text);
        body.put("category", category);

        api.createPost(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Toast.makeText(CreatePostActivity.this, "Post uploaded successfully", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                }
                String errorMsg = "Failed to upload post";
                if (response.code() == 429) {
                    try {
                        String errStr = response.errorBody().string();
                        com.google.gson.JsonObject errObj = com.google.gson.JsonParser.parseString(errStr).getAsJsonObject();
                        if (errObj.has("error")) {
                            errorMsg = errObj.get("error").getAsString();
                        }
                    } catch (Exception ignored) {}
                }
                Toast.makeText(CreatePostActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                isPosting = false;
                postButton.setEnabled(true);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Post creation failed", t);
                Toast.makeText(CreatePostActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                isPosting = false;
                postButton.setEnabled(true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        dismissAdLoadingDialog();
        if (levelPlayRewardedAd != null) {
            levelPlayRewardedAd = null;
        }
        super.onDestroy();
    }
}
