package com.mohitt.camverz;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Create Post — TEXT ONLY.
 * Image and voice functionality has been removed.
 */
public class CreatePostActivity extends BaseActivity {

    private static final String TAG = "CreatePostActivity";
    private EditText postText;
    private Button postButton;
    private RadioGroup categoryGroup;
    private ApiService api;
    private boolean isPosting = false;
    private com.unity3d.mediation.rewarded.LevelPlayRewardedAd levelPlayRewardedAd = null;
    private boolean isRewardedVideoAvailable = false;
    private android.app.ProgressDialog progressDialog;
    private final android.os.Handler adWaitHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable adWaitRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.toolbar), findViewById(R.id.post_button));

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> finish());

        api = ApiClient.getInstance(this).getApi();

        postText = findViewById(R.id.post_text);
        postButton = findViewById(R.id.post_button);
        categoryGroup = findViewById(R.id.category_group);

        postButton.setOnClickListener(v -> {
            if (!isPosting) {
                uploadPost();
            }
        });

        // Preload ironSource LevelPlay Rewarded Ad
        BaseActivity.runOnLevelPlayInit(this::loadLevelPlayRewardedVideoAd);
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

        isPosting = true;
        postButton.setEnabled(false);

        RadioButton selectedCategory = findViewById(selectedCategoryId);
        String category = selectedCategory.getText().toString().toLowerCase();

        Map<String, String> body = new HashMap<>();
        body.put("text", text);
        body.put("category", category);

        api.createPost(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Toast.makeText(CreatePostActivity.this, "Post uploaded", Toast.LENGTH_SHORT).show();
                        showLevelPlayRewardedAdAndFinish();
                        return;
                    }
                }
                Toast.makeText(CreatePostActivity.this, "Failed to upload post", Toast.LENGTH_SHORT).show();
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

    private void loadLevelPlayRewardedVideoAd() {
        try {
            levelPlayRewardedAd = new com.unity3d.mediation.rewarded.LevelPlayRewardedAd("onddt1lewzexkb5q");
            levelPlayRewardedAd.setListener(new com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener() {
                @Override
                public void onAdLoaded(com.unity3d.mediation.LevelPlayAdInfo adInfo) {
                    isRewardedVideoAvailable = true;
                    Log.d(TAG, "ironSource Rewarded Ad loaded successfully for CreatePostActivity");
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            adWaitHandler.removeCallbacks(adWaitRunnable);
                            progressDialog.dismiss();
                            if (levelPlayRewardedAd != null) {
                                levelPlayRewardedAd.showAd(CreatePostActivity.this);
                            } else {
                                finish();
                            }
                        }
                    });
                }

                @Override
                public void onAdLoadFailed(com.unity3d.mediation.LevelPlayAdError error) {
                    isRewardedVideoAvailable = false;
                    Log.w(TAG, "ironSource Rewarded Ad failed to load in CreatePostActivity: " + error.getErrorMessage());
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            adWaitHandler.removeCallbacks(adWaitRunnable);
                            cleanupAndFinish();
                        }
                    });
                }

                @Override
                public void onAdDisplayed(com.unity3d.mediation.LevelPlayAdInfo adInfo) {
                    Log.d(TAG, "ironSource Rewarded Ad displayed");
                }

                @Override
                public void onAdDisplayFailed(com.unity3d.mediation.LevelPlayAdError error, com.unity3d.mediation.LevelPlayAdInfo adInfo) {
                    Log.e(TAG, "ironSource Rewarded Ad display failed: " + error.getErrorMessage());
                    finish();
                }

                @Override
                public void onAdClicked(com.unity3d.mediation.LevelPlayAdInfo adInfo) {}

                @Override
                public void onAdRewarded(com.unity3d.mediation.rewarded.LevelPlayReward reward, com.unity3d.mediation.LevelPlayAdInfo adInfo) {
                    Log.d(TAG, "ironSource Rewarded Ad successfully rewarded user");
                }

                @Override
                public void onAdClosed(com.unity3d.mediation.LevelPlayAdInfo adInfo) {
                    finish();
                }
            });
            levelPlayRewardedAd.loadAd();
        } catch (Exception e) {
            Log.e(TAG, "Error loading ironSource Rewarded Ad: " + e.getMessage());
        }
    }

    private void showLevelPlayRewardedAdAndFinish() {
        if (levelPlayRewardedAd != null && (isRewardedVideoAvailable || levelPlayRewardedAd.isAdReady())) {
            levelPlayRewardedAd.showAd(this);
        } else {
            // Show loading dialog and wait up to 2.5 seconds
            progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setMessage("Loading ad...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            adWaitRunnable = new Runnable() {
                @Override
                public void run() {
                    cleanupAndFinish();
                }
            };
            adWaitHandler.postDelayed(adWaitRunnable, 2500);
        }
    }

    private void cleanupAndFinish() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        finish();
    }
}
