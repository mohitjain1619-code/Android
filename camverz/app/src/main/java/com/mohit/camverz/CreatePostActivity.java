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
    private boolean isWaitingForAd = false;
    private boolean isRewardEarned = false;

    private String pendingPostText = "";
    private String pendingCategory = "";

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

        // Preload Dual Bidding (Interstitial & Rewarded) auctions immediately while user types
        AdManager.getInstance().preloadDualBiddingAds(this);
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
        isPosting = true;
        postButton.setEnabled(false);

        if (AdManager.getInstance().isDualBiddingReady()) {
            showDualBiddingAd();
        } else {
            isWaitingForAd = true;
            showAdLoadingDialog();
            AdManager.getInstance().preloadDualBiddingAds(this);

            if (adTimeoutRunnable != null) {
                adHandler.removeCallbacks(adTimeoutRunnable);
            }
            adTimeoutRunnable = () -> {
                if (isWaitingForAd) {
                    isWaitingForAd = false;
                    dismissAdLoadingDialog();
                    showDualBiddingAd();
                }
            };
            adHandler.postDelayed(adTimeoutRunnable, 3500);
        }
    }

    private void showDualBiddingAd() {
        dismissAdLoadingDialog();
        isWaitingForAd = false;
        isRewardEarned = false;

        AdManager.getInstance().showHighestBidRewardAd(this, PLACEMENT, new AdManager.RewardedAdCallback() {
            @Override
            public void onRewardEarned() {
                isRewardEarned = true;
            }

            @Override
            public void onAdDismissed(boolean rewarded) {
                runOnUiThread(() -> {
                    dismissAdLoadingDialog();
                    if (rewarded || isRewardEarned) {
                        Toast.makeText(CreatePostActivity.this, "Ad completed! Publishing post...", Toast.LENGTH_SHORT).show();
                        executePostUpload(pendingPostText, pendingCategory);
                    } else {
                        isPosting = false;
                        postButton.setEnabled(true);
                        Toast.makeText(CreatePostActivity.this, "You must watch the full ad to publish your post.", Toast.LENGTH_LONG).show();
                    }
                    AdManager.getInstance().preloadDualBiddingAds(CreatePostActivity.this);
                });
            }

            @Override
            public void onAdFailed(String reason) {
                runOnUiThread(() -> {
                    dismissAdLoadingDialog();
                    Log.w(TAG, "Ad display failed (" + reason + "), proceeding with post publication.");
                    Toast.makeText(CreatePostActivity.this, "Publishing post...", Toast.LENGTH_SHORT).show();
                    executePostUpload(pendingPostText, pendingCategory);
                    AdManager.getInstance().preloadDualBiddingAds(CreatePostActivity.this);
                });
            }
        });
    }

    private void showAdLoadingDialog() {
        if (isFinishing() || isDestroyed()) return;
        try {
            if (adProgressDialog == null) {
                adProgressDialog = new ProgressDialog(this);
                adProgressDialog.setMessage("Loading highest bid ad... Please wait");
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
            adTimeoutRunnable = null;
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
        super.onDestroy();
    }
}
