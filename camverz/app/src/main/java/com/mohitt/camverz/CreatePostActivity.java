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
    private boolean isRewardedAdLoaded = false;

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

        // Preload Unity Rewarded Ad
        loadUnityRewardedAd();
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
                        showUnityRewardedAdAndFinish();
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

    private void loadUnityRewardedAd() {
        if (!com.unity3d.ads.UnityAds.isInitialized()) {
            com.unity3d.ads.UnityAds.initialize(getApplicationContext(), "800356158", false, new com.unity3d.ads.IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    Log.d(TAG, "Unity Ads initialized in CreatePostActivity");
                    preloadRewardedAdInternal();
                }

                @Override
                public void onInitializationFailed(com.unity3d.ads.UnityAds.UnityAdsInitializationError error, String message) {
                    Log.e(TAG, "Unity Ads initialization failed in CreatePostActivity: " + message);
                }
            });
        } else {
            preloadRewardedAdInternal();
        }
    }

    private void preloadRewardedAdInternal() {
        com.unity3d.ads.UnityAds.load("Rewarded_Android", new com.unity3d.ads.IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                isRewardedAdLoaded = true;
                Log.d(TAG, "Unity Rewarded Ad loaded successfully");
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, com.unity3d.ads.UnityAds.UnityAdsLoadError error, String message) {
                isRewardedAdLoaded = false;
                Log.e(TAG, "Unity Rewarded Ad failed to load: " + message);
            }
        });
    }

    private void showUnityRewardedAdAndFinish() {
        if (isRewardedAdLoaded) {
            com.unity3d.ads.UnityAds.show(this, "Rewarded_Android", new com.unity3d.ads.IUnityAdsShowListener() {
                @Override
                public void onUnityAdsShowStart(String placementId) {
                    Log.d(TAG, "Unity Rewarded Ad started displaying");
                }

                @Override
                public void onUnityAdsShowClick(String placementId) {}

                @Override
                public void onUnityAdsShowComplete(String placementId, com.unity3d.ads.UnityAds.UnityAdsShowCompletionState state) {
                    Log.d(TAG, "Unity Rewarded Ad completed displaying with state: " + state);
                    finish();
                }

                @Override
                public void onUnityAdsShowFailure(String placementId, com.unity3d.ads.UnityAds.UnityAdsShowError error, String message) {
                    Log.e(TAG, "Unity Rewarded Ad failed to display: " + message);
                    finish();
                }
            });
        } else {
            finish();
        }
    }
}
