package com.mohit.camverz;

import android.os.Bundle;
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
    private TokenManager tokenManager;
    private boolean isPosting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.toolbar), findViewById(R.id.post_button));

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> finish());

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        postText = findViewById(R.id.post_text);
        postButton = findViewById(R.id.post_button);
        categoryGroup = findViewById(R.id.category_group);

        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.category_female) {
                boolean isVerifiedFemale = "female".equalsIgnoreCase(tokenManager.getUserGender()) && tokenManager.isVerified();
                if (!isVerifiedFemale) {
                    new AlertDialog.Builder(CreatePostActivity.this)
                        .setTitle("Premium Audience 👑")
                        .setMessage("Targeting the Female audience is a premium feature. Would you like to watch a video ad to unlock this selection?")
                        .setPositiveButton("Watch Ad", (dialog, which) -> {
                            Toast.makeText(CreatePostActivity.this, "Loading ad...", Toast.LENGTH_SHORT).show();
                            loadAndShowRewardedAd(() -> {
                                Toast.makeText(CreatePostActivity.this, "Unlocked Female Audience!", Toast.LENGTH_SHORT).show();
                            }, () -> {
                                categoryGroup.check(R.id.category_all);
                                Toast.makeText(CreatePostActivity.this, "Ad skipped. Resetting selection.", Toast.LENGTH_SHORT).show();
                            }, () -> {
                                Toast.makeText(CreatePostActivity.this, "Unlocked Female Audience!", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            categoryGroup.check(R.id.category_all);
                        })
                        .setCancelable(false)
                        .show();
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isPosting || BaseActivity.isAdShowing) {
                    Toast.makeText(CreatePostActivity.this, "Please wait, post is uploading...", Toast.LENGTH_SHORT).show();
                    return;
                }
                finish();
            }
        });

        postButton.setOnClickListener(v -> {
            if (!isPosting) {
                uploadPost();
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if ((isPosting || BaseActivity.isAdShowing) && event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if ((isPosting || BaseActivity.isAdShowing) && keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

        proceedToUpload(text, category);
    }

    private void proceedToUpload(final String text, final String category) {
        isPosting = true;
        postButton.setEnabled(false);

        if (tokenManager.isCommunityAdFree()) {
            executePostUpload(text, category);
        } else {
            Toast.makeText(this, "Preparing ad to upload post...", Toast.LENGTH_SHORT).show();
            loadAndShowRewardedAd(() -> {
                // Success: Ad watched -> upload post
                executePostUpload(text, category);
            }, () -> {
                // User cancelled / skipped ad -> block post
                isPosting = false;
                postButton.setEnabled(true);
                Toast.makeText(CreatePostActivity.this, "Ad skipped or incomplete. Post was not published.", Toast.LENGTH_SHORT).show();
            }, () -> {
                // No Fill / Ad Load Error -> ALLOW post creation directly without blocking
                Toast.makeText(CreatePostActivity.this, "Ad unavailable. Publishing post...", Toast.LENGTH_SHORT).show();
                executePostUpload(text, category);
            });
        }
    }

    private void executePostUpload(String text, String category) {
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
                    } catch (Exception e) {}
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
    protected void onResume() {
        super.onResume();
        AdManager.getInstance().preloadInterstitial(this);
    }

    private void loadAndShowRewardedAd(Runnable onSuccess, Runnable onUserCancelled, Runnable onNoFill) {
        AdManager.getInstance().showRewardedAd(this, "create_post_reward", new AdManager.RewardedAdCallback() {
            @Override
            public void onRewardEarned() {}

            @Override
            public void onAdDismissed(boolean rewarded) {
                if (rewarded) {
                    if (onSuccess != null) runOnUiThread(onSuccess);
                } else {
                    if (onUserCancelled != null) runOnUiThread(onUserCancelled);
                }
            }

            @Override
            public void onAdFailed(String reason) {
                if (onNoFill != null) runOnUiThread(onNoFill);
            }
        });
    }
}
