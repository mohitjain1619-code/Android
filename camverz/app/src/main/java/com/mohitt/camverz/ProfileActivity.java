package com.mohitt.camverz;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    private CircleImageView profileImageView;
    private EditText profileName, bio;
    private TextView profileDetails, userId, followersCount, followingCount, changeGenderText;
    private ImageView editNameIcon, editBioIcon, genderIcon, menuIcon;
    private RecyclerView postsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button followButton, messageButton;
    private String friendshipStatus = "none";
    private String friendshipRequestId = null;
    private LinearLayout otherUserActionsLayout;
    private View mainContentLayout;

    private String visitedUserId;
    private String currentUserId;
    private User visitedUser;

    private final String[] avatars = {"av1", "av2", "av3", "av4", "av5", "av6", "av7", "av8", "av9", "av10", "av11", "av12", "av13", "av14", "av15"};
    private List<Post> postList;
    private PostAdapter postAdapter;
    private boolean isUpdating = false;
    private boolean isBlocked = false;
    private boolean isBlockedByOther = false;
    private boolean hasLoadedData = false;
    private com.facebook.ads.InterstitialAd metaAvatarInterstitialAd;
    private com.google.android.gms.ads.interstitial.InterstitialAd admobAvatarInterstitialAd;
    
    private ApiService api;
    private TokenManager tokenManager;
    private com.ironsource.mediationsdk.ads.nativead.NativeAdLayout nativeAdLayout;
    private android.widget.FrameLayout nativeAdContainer;
    private com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd levelPlayNativeAd;
    private com.facebook.ads.NativeAd metaNativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Apply edge-to-edge window insets to prevent status bar / navigation bar overlap
        applyWindowInsets(findViewById(R.id.profile_header_layout), findViewById(R.id.floating_menu_container));
        
        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> finish());

        // Floating Menu Buttons
        findViewById(R.id.menu_home_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, MainScreenActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_explore_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, FeedActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_messages_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, InboxActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.menu_profile_btn).setOnClickListener(v -> {});

        if (savedInstanceState != null) {
            isUpdating = savedInstanceState.getBoolean("isUpdating", false);
        }

        profileImageView = findViewById(R.id.profile_image);
        profileName = findViewById(R.id.profile_name);
        profileDetails = findViewById(R.id.profile_details);
        bio = findViewById(R.id.bio);
        userId = findViewById(R.id.user_id);
        editNameIcon = findViewById(R.id.edit_name_icon);
        editBioIcon = findViewById(R.id.edit_bio_icon);
        genderIcon = findViewById(R.id.gender_icon);
        changeGenderText = findViewById(R.id.change_gender_text);
        menuIcon = findViewById(R.id.menu_icon);
        postsRecyclerView = findViewById(R.id.posts_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        followersCount = findViewById(R.id.followers_count);
        followingCount = findViewById(R.id.following_count);
        followButton = findViewById(R.id.follow_button);
        messageButton = findViewById(R.id.message_button);
        otherUserActionsLayout = findViewById(R.id.other_user_actions_layout);
        
        Button verifyIdentityButton = findViewById(R.id.verify_identity_button);
        ImageView verificationBadge = findViewById(R.id.verification_badge);
        
        verifyIdentityButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, VerificationInfoActivity.class);
            startActivityForResult(intent, 102);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        profileName.setText("");
        bio.setText("");
        userId.setText("");
        profileDetails.setText("");
        followersCount.setText("-");
        followingCount.setText("-");

        if (swipeRefreshLayout.getChildCount() > 0) {
            mainContentLayout = swipeRefreshLayout.getChildAt(0);
            mainContentLayout.setVisibility(View.INVISIBLE);
        }

        if (tokenManager.isLoggedIn()) {
            currentUserId = tokenManager.getUserId();
            visitedUserId = getIntent().getStringExtra("userId");
            if (visitedUserId == null || visitedUserId.isEmpty()) {
                visitedUserId = currentUserId;
            }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkBlockStatusAndLoad();
        setupUI();

        // Initialize ironSource native ad layouts and load once LevelPlay init is complete
        nativeAdContainer = findViewById(R.id.ironsource_native_container);
        nativeAdLayout = findViewById(R.id.ironsource_native_ad_layout);
        BaseActivity.runOnLevelPlayInit(this::loadLevelPlayNativeAd);
    }

    private void checkBlockStatusAndLoad() {
        if (currentUserId.equals(visitedUserId)) {
            if (!hasLoadedData) {
                hasLoadedData = true;
                loadData();
            }
            return;
        }

        api.getFriendStatus(visitedUserId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        isBlocked = data.has("isBlocked") && data.get("isBlocked").getAsBoolean();
                        isBlockedByOther = data.has("isBlockedByOther") && data.get("isBlockedByOther").getAsBoolean();
                        
                        if (isBlocked) {
                            if (mainContentLayout != null) mainContentLayout.setVisibility(View.VISIBLE);
                            updateUIForBlockedUser();
                        } else if (isBlockedByOther) {
                            showBlockedByOtherDialog();
                        } else {
                            if (!hasLoadedData) {
                                hasLoadedData = true;
                                loadData();
                            }
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBlockedByOtherDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_user_blocked, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        Button btnOk = dialogView.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private void updateUIForBlockedUser() {
        profileName.setText("Blocked User");
        bio.setText("");
        profileDetails.setText("");
        userId.setText("");
        followersCount.setText("-");
        followingCount.setText("-");

        followButton.setVisibility(View.GONE);
        messageButton.setVisibility(View.GONE);
        otherUserActionsLayout.setVisibility(View.GONE);

        if (postList != null) {
            postList.clear();
            if (postAdapter != null) postAdapter.notifyDataSetChanged();
        }
        hasLoadedData = true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isUpdating", isUpdating);
    }

    private void setupUI() {
        menuIcon.setOnClickListener(v -> showPopupMenu(v));

        if (!currentUserId.equals(visitedUserId)) {
            editNameIcon.setVisibility(View.GONE);
            editBioIcon.setVisibility(View.GONE);
            profileImageView.setClickable(false);
            otherUserActionsLayout.setVisibility(View.VISIBLE);

            followButton.setOnClickListener(v -> followUser());
            messageButton.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
                intent.putExtra("userId", visitedUserId);
                if (visitedUser != null) {
                    intent.putExtra("userName", visitedUser.getName());
                    intent.putExtra("userAvatar", visitedUser.getAvatar());
                    intent.putExtra("userPhotoUrl", visitedUser.getPhotoUrl());
                }
                startActivity(intent);
            });

        } else {
            otherUserActionsLayout.setVisibility(View.GONE);
            profileName.setEnabled(false);
            bio.setEnabled(false);
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isBlocked || isBlockedByOther) {
                swipeRefreshLayout.setRefreshing(false);
            } else {
                loadData();
            }
        });

        editNameIcon.setOnClickListener(v -> {
            if (isUpdating) return;
            if (profileName.isEnabled()) {
                updateProfile("name", profileName.getText().toString());
            } else {
                profileName.setEnabled(true);
                profileName.requestFocus();
                editNameIcon.setImageResource(R.drawable.ic_save);
            }
        });

        editBioIcon.setOnClickListener(v -> {
            if (isUpdating) return;
            if (bio.isEnabled()) {
                updateProfile("bio", bio.getText().toString());
            } else {
                bio.setEnabled(true);
                bio.requestFocus();
                editBioIcon.setImageResource(R.drawable.ic_save);
            }
        });

        profileImageView.setOnClickListener(v -> {
            if (currentUserId.equals(visitedUserId)) {
                showAvatarDialog();
            }
        });

        genderIcon.setOnClickListener(v -> {
            if (currentUserId.equals(visitedUserId)) {
                showGenderChangeWarningDialog();
            }
        });

        changeGenderText.setOnClickListener(v -> {
            if (currentUserId.equals(visitedUserId)) {
                showGenderChangeWarningDialog();
            }
        });
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);

        if (currentUserId.equals(visitedUserId)) {
            popup.getMenu().add("Blocked Users");
            popup.getMenu().add("Privacy & Legal Terms");
            popup.getMenu().add("Delete Account");
        } else {
            if (isBlocked) {
                popup.getMenu().add("Unblock User");
            } else {
                popup.getMenu().add("Block User");
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Blocked Users")) {
                startActivity(new Intent(this, BlockedUsersActivity.class));
            } else if (title.equals("Privacy & Legal Terms")) {
                startActivity(new Intent(this, LegalActivity.class));
            } else if (title.equals("Block User")) {
                showBlockConfirmationDialog();
            } else if (title.equals("Unblock User")) {
                showUnblockConfirmationDialog();
            } else if (title.equals("Delete Account")) {
                showDeleteAccountDialog();
            }
            return true;
        });

        popup.show();
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etConfirmDelete = dialogView.findViewById(R.id.et_confirm_delete);
        Button btnConfirmDelete = dialogView.findViewById(R.id.btn_confirm_delete);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        etConfirmDelete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equalsIgnoreCase("delete")) {
                    btnConfirmDelete.setEnabled(true);
                    btnConfirmDelete.setAlpha(1.0f);
                } else {
                    btnConfirmDelete.setEnabled(false);
                    btnConfirmDelete.setAlpha(0.5f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirmDelete.setOnClickListener(v -> {
            dialog.dismiss();
            deleteAccount();
        });

        dialog.show();
    }

    private void deleteAccount() {
        setUpdatingState(true);
        api.deleteAccount().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Toast.makeText(ProfileActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                        
                        // Sign out of Google to ensure account picker shows next time
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build();
                        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(ProfileActivity.this, gso);
                        googleSignInClient.signOut();
                        
                        tokenManager.clearToken();
                        
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
                setUpdatingState(false);
                Toast.makeText(ProfileActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setUpdatingState(false);
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBlockConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Block User")
                .setMessage("Are you sure you want to block this user? They won't be able to message you or see your posts.")
                .setPositiveButton("Block", (dialog, which) -> blockUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUnblockConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unblock User")
                .setMessage("Do you want to unblock this user?")
                .setPositiveButton("Unblock", (dialog, which) -> unblockUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void blockUser() {
        setUpdatingState(true);
        api.blockUser(visitedUserId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setUpdatingState(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "User blocked", Toast.LENGTH_SHORT).show();
                    isBlocked = true;
                    updateUIForBlockedUser();
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to block user", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setUpdatingState(false);
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unblockUser() {
        setUpdatingState(true);
        api.unblockUser(visitedUserId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setUpdatingState(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "User unblocked", Toast.LENGTH_SHORT).show();
                    isBlocked = false;
                    loadData(); 
                    followButton.setVisibility(View.VISIBLE);
                    messageButton.setVisibility(View.VISIBLE);
                    otherUserActionsLayout.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to unblock user", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setUpdatingState(false);
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void followUser() {
        if (visitedUserId == null) return;
        setUpdatingState(true);

        if ("none".equals(friendshipStatus)) {
            // Send request
            Map<String, String> body = new HashMap<>();
            body.put("targetUserId", visitedUserId);
            api.sendFriendRequest(body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    setUpdatingState(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Request Sent", Toast.LENGTH_SHORT).show();
                        loadData();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    setUpdatingState(false);
                    Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } else if ("received".equals(friendshipStatus)) {
            // Accept request
            if (friendshipRequestId == null) {
                setUpdatingState(false);
                return;
            }
            api.acceptFriendRequest(friendshipRequestId).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    setUpdatingState(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Request Accepted", Toast.LENGTH_SHORT).show();
                        loadData();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to accept request", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    setUpdatingState(false);
                    Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } else if ("sent".equals(friendshipStatus) || "friends".equals(friendshipStatus)) {
            // Cancel request or Unfriend
            new AlertDialog.Builder(this)
                .setTitle("friends".equals(friendshipStatus) ? "Unfriend User" : "Cancel Request")
                .setMessage("friends".equals(friendshipStatus) ? "Are you sure you want to remove this user from your friends?" : "Are you sure you want to cancel your friend request?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    api.deleteFriendRequest(visitedUserId).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            setUpdatingState(false);
                            if (response.isSuccessful()) {
                                Toast.makeText(ProfileActivity.this, "Action completed successfully", Toast.LENGTH_SHORT).show();
                                loadData();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Failed to complete action", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            setUpdatingState(false);
                            Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", (dialog, which) -> setUpdatingState(false))
                .show();
        }
    }

    private void updateProfileUI() {
        if (isBlocked || isBlockedByOther) return;

        profileName.setText(visitedUser.getName());
        bio.setText(visitedUser.getBio());
        profileDetails.setText(String.format("%d, %s", getAge(visitedUser.getDob()), visitedUser.getCity()));
        
        followersCount.setText(String.valueOf(visitedUser.getFollowersCount()));
        followingCount.setText(String.valueOf(visitedUser.getFollowingCount()));

        String customId = visitedUser.getCustomId();
        String idText = "ID: " + ((customId == null || customId.isEmpty()) ? (currentUserId.equals(visitedUserId) ? currentUserId.substring(0, 8) : "N/A") : customId);
        if (currentUserId.equals(visitedUserId) && visitedUser.getEmail() != null && !visitedUser.getEmail().isEmpty()) {
            userId.setText(idText + "\n" + visitedUser.getEmail());
        } else {
            userId.setText(idText);
        }

        if (!currentUserId.equals(visitedUserId)) {
            if ("friends".equals(friendshipStatus)) {
                followButton.setText("Friends");
                followButton.setBackgroundResource(R.drawable.bg_following_button);
            } else if ("sent".equals(friendshipStatus)) {
                followButton.setText("Requested");
                followButton.setBackgroundResource(R.drawable.bg_following_button);
            } else if ("received".equals(friendshipStatus)) {
                followButton.setText("Accept Request");
                followButton.setBackgroundResource(R.drawable.bg_btn_primary_gradient);
            } else {
                followButton.setText("Add Friend");
                followButton.setBackgroundResource(R.drawable.bg_btn_primary_gradient);
            }
        }

        if ("male".equalsIgnoreCase(visitedUser.getGender())) {
            genderIcon.setImageResource(R.drawable.ic_male_blue);
            genderIcon.setVisibility(View.VISIBLE);
        } else if ("female".equalsIgnoreCase(visitedUser.getGender())) {
            genderIcon.setImageResource(R.drawable.ic_female_symbol_pink);
            genderIcon.setVisibility(View.VISIBLE);
        } else {
            genderIcon.setVisibility(View.GONE);
        }

        if (currentUserId.equals(visitedUserId)) {
            changeGenderText.setVisibility(View.VISIBLE);
        } else {
            changeGenderText.setVisibility(View.GONE);
        }

        ImageView verificationBadge = findViewById(R.id.verification_badge);
        Button verifyIdentityButton = findViewById(R.id.verify_identity_button);
        
        if (visitedUser.isVerified() || "male".equalsIgnoreCase(visitedUser.getGender())) {
            verificationBadge.setVisibility(View.VISIBLE);
            verifyIdentityButton.setVisibility(View.GONE);
        } else {
            verificationBadge.setVisibility(View.GONE);
            if (currentUserId.equals(visitedUserId)) {
                verifyIdentityButton.setVisibility(View.VISIBLE);
            } else {
                verifyIdentityButton.setVisibility(View.GONE);
            }
        }

        String avatarName = visitedUser.getAvatar();
        if (avatarName != null && !avatarName.isEmpty()) {
            int avatarResId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
            if (avatarResId != 0) {
                Glide.with(this).load(avatarResId).placeholder(R.drawable.av1).into(profileImageView);
            } else {
                Glide.with(this).load(R.drawable.av1).into(profileImageView);
            }
        } else {
            Glide.with(this).load(R.drawable.av1).into(profileImageView);
        }
    }

    private void loadUserPosts() {
        if (isBlocked || isBlockedByOther) return;

        if (postAdapter == null) {
            postList = new ArrayList<>();
            postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            postAdapter = new PostAdapter(this, postList);
            postsRecyclerView.setAdapter(postAdapter);
        }

        setLoadingState(true);
        api.getPosts(null, visitedUserId, 50, 0).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoadingState(false);
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
                                
                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoadingState(false);
            }
        });
    }

    private int getAge(String dob) {
        if (dob == null || dob.isEmpty()) return 0;
        String[] parts = dob.split("/");
        if (parts.length != 3) return 0;
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);
        Calendar dobCal = Calendar.getInstance();
        dobCal.set(year, month, day);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private void updateProfile(String field, String value) {
        setUpdatingState(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);

        api.updateMe(updates).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                        if ("avatar".equals(field)) {
                            int avatarResId = getResources().getIdentifier(value, "drawable", getPackageName());
                            Glide.with(ProfileActivity.this).load(avatarResId).into(profileImageView);
                            
                            // Save updated avatar in TokenManager
                            String currentGender = visitedUser != null ? visitedUser.getGender() : tokenManager.getUserGender();
                            tokenManager.saveUser(
                                tokenManager.getUserId(),
                                tokenManager.getUserName(),
                                tokenManager.getUserEmail(),
                                currentGender,
                                value,
                                "male".equalsIgnoreCase(currentGender)
                            );
                            loadAndShowAdMobInterstitialForAvatar();
                        } else if ("name".equals(field)) {
                            // Save updated name in TokenManager
                            String currentGender = visitedUser != null ? visitedUser.getGender() : tokenManager.getUserGender();
                            String currentAvatar = visitedUser != null ? visitedUser.getAvatar() : tokenManager.getUserAvatar();
                            tokenManager.saveUser(
                                tokenManager.getUserId(),
                                value,
                                tokenManager.getUserEmail(),
                                currentGender,
                                currentAvatar,
                                "male".equalsIgnoreCase(currentGender)
                            );
                        }
                        setUpdatingState(false);
                        return;
                    }
                }
                Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                setUpdatingState(false);
                loadData();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                setUpdatingState(false);
                loadData(); 
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(isLoading);
        }
    }

    private void setUpdatingState(boolean updating) {
        isUpdating = updating;
        setLoadingState(updating);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(!updating);
        }

        if (editNameIcon != null) editNameIcon.setEnabled(!updating);
        if (editBioIcon != null) editBioIcon.setEnabled(!updating);
        if (profileImageView != null) profileImageView.setClickable(!updating);

        if (profileName != null) profileName.setEnabled(false);
        if (bio != null) bio.setEnabled(false);

        if (!updating) {
            if (editNameIcon != null) editNameIcon.setImageResource(R.drawable.ic_edit);
            if (editBioIcon != null) editBioIcon.setImageResource(R.drawable.ic_edit);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isBlocked && !isBlockedByOther) {
            loadUserPosts();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 102 && resultCode == RESULT_OK) {
            if (visitedUser != null) {
                loadData(); 
            }
        }
    }

    private void loadData() {
        if (isBlocked || isBlockedByOther) return;

        setLoadingState(true);
        
        Call<JsonObject> userCall = currentUserId.equals(visitedUserId) ? api.getMe() : api.getUser(visitedUserId);
        
        userCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("user")) {
                        JsonObject userObj = data.getAsJsonObject("user");
                        
                        visitedUser = new User();
                        visitedUser.setUid(userObj.has("id") ? userObj.get("id").getAsString() : "");
                        visitedUser.setName(userObj.has("name") ? userObj.get("name").getAsString() : "");
                        visitedUser.setBio(userObj.has("bio") && !userObj.get("bio").isJsonNull() ? userObj.get("bio").getAsString() : "");
                        visitedUser.setAvatar(userObj.has("avatar") ? userObj.get("avatar").getAsString() : "");
                        visitedUser.setGender(userObj.has("gender") ? userObj.get("gender").getAsString() : "");
                        visitedUser.setCity(userObj.has("city") && !userObj.get("city").isJsonNull() ? userObj.get("city").getAsString() : "");
                        visitedUser.setDob(userObj.has("dob") && !userObj.get("dob").isJsonNull() ? userObj.get("dob").getAsString() : "");
                        visitedUser.setCustomId(userObj.has("customId") && !userObj.get("customId").isJsonNull() ? userObj.get("customId").getAsString() : "");
                        visitedUser.setVerified(userObj.has("verified") && userObj.get("verified").getAsBoolean());
                        
                        // Set follower/following stats logic provided by backend
                        visitedUser.setFollowersCount(userObj.has("followersCount") ? userObj.get("followersCount").getAsInt() : 0);
                        visitedUser.setFollowingCount(userObj.has("followingCount") ? userObj.get("followingCount").getAsInt() : 0);
                        visitedUser.setFollowedByMe(userObj.has("isFollowedByMe") && userObj.get("isFollowedByMe").getAsBoolean());
                        
                        friendshipStatus = userObj.has("friendshipStatus") ? userObj.get("friendshipStatus").getAsString() : "none";
                        friendshipRequestId = userObj.has("friendshipRequestId") && !userObj.get("friendshipRequestId").isJsonNull() ? userObj.get("friendshipRequestId").getAsString() : null;
                        
                        if (mainContentLayout != null) mainContentLayout.setVisibility(View.VISIBLE);
                        updateProfileUI();
                        return;
                    }
                }
                showUserDeletedDialog();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoadingState(false);
                Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        });
        
        loadUserPosts();
    }

    private void showAvatarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_avatar_selection, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RecyclerView recyclerView = view.findViewById(R.id.avatar_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        AvatarSelectionAdapter adapter = new AvatarSelectionAdapter(this, avatars, avatarName -> {
            updateProfile("avatar", avatarName);
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showGenderChangeWarningDialog() {
        if (visitedUser == null) return;
        
        final String currentGender = visitedUser.getGender();
        final String newGender = "male".equalsIgnoreCase(currentGender) ? "female" : "male";
        
        String warningMsg = "male".equalsIgnoreCase(currentGender)
            ? "🔒 Female profiles require live face verification to start matching."
            : "🔓 Male profiles are auto-verified on change.";
            
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Gender to " + ("male".equalsIgnoreCase(currentGender) ? "Female" : "Male") + "?")
                .setMessage("Are you sure you want to change your gender?\n\n⚠️ WARNING: Once changed, your profile verification settings will update accordingly.\n\n" + warningMsg)
                .setPositiveButton("Yes, Change", (dialog, which) -> {
                    dialog.dismiss();
                    performGenderChange(newGender);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performGenderChange(String newGender) {
        setUpdatingState(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("gender", newGender);
        updates.put("verified", "male".equalsIgnoreCase(newGender)); // Auto-verify male, unverify female
        updates.put("verificationStatus", "male".equalsIgnoreCase(newGender) ? "approved" : "none");

        api.updateMe(updates).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().get("ok").getAsBoolean()) {
                    Toast.makeText(ProfileActivity.this, "Gender updated to " + newGender, Toast.LENGTH_SHORT).show();
                    
                    // Update TokenManager local cache
                    String userId = tokenManager.getUserId();
                    String name = tokenManager.getUserName();
                    String email = tokenManager.getUserEmail();
                    String avatar = tokenManager.getUserAvatar();
                    tokenManager.saveUser(userId, name, email, newGender, avatar, "male".equalsIgnoreCase(newGender));
                    
                    setUpdatingState(false);
                    loadData(); // Reload profile details
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to change gender", Toast.LENGTH_SHORT).show();
                    setUpdatingState(false);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setUpdatingState(false);
            }
        });
    }

    private void showUserDeletedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("User Not Found")
                .setMessage("This user profile no longer exists.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                });
        builder.show();
    }

    private void loadLevelPlayNativeAd() {
        // Ads disabled in main app
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void loadMetaNativeAd() {
        // Ads disabled in main app
    }

    private void inflateMetaNativeAd(com.facebook.ads.NativeAd ad) {
        android.widget.FrameLayout adContainer = findViewById(R.id.metaNativeAdContainer);
        if (adContainer == null) return;
        adContainer.removeAllViews();
        adContainer.setVisibility(android.view.View.VISIBLE);

        com.facebook.ads.NativeAdLayout adLayout = new com.facebook.ads.NativeAdLayout(this);
        adLayout.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));

        android.widget.LinearLayout innerContainer = new android.widget.LinearLayout(this);
        innerContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        innerContainer.setBackgroundResource(R.drawable.bg_glass_card_premium);
        innerContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        android.widget.TextView adHeadline = new android.widget.TextView(this);
        adHeadline.setText(ad.getAdHeadline());
        adHeadline.setTextColor(getResources().getColor(R.color.text_primary));
        adHeadline.setTextSize(16);
        adHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(adHeadline);

        android.widget.TextView adBody = new android.widget.TextView(this);
        adBody.setText(ad.getAdBodyText());
        adBody.setTextColor(getResources().getColor(R.color.text_secondary));
        adBody.setTextSize(12);
        adBody.setPadding(0, dpToPx(4), 0, dpToPx(8));
        innerContainer.addView(adBody);

        android.widget.Button callToAction = new android.widget.Button(this);
        callToAction.setText(ad.getAdCallToAction());
        callToAction.setBackgroundResource(R.drawable.bg_glass_card_premium);
        callToAction.setTextColor(getResources().getColor(R.color.accent_primary));
        callToAction.setTextSize(14);
        callToAction.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        innerContainer.addView(callToAction);

        adLayout.addView(innerContainer);

        java.util.List<android.view.View> clickableViews = new java.util.ArrayList<>();
        clickableViews.add(adHeadline);
        clickableViews.add(callToAction);
        
        com.facebook.ads.MediaView mediaView = new com.facebook.ads.MediaView(this);
        ad.registerViewForInteraction(adLayout, mediaView, clickableViews);

        adContainer.addView(adLayout);
    }

    @Override
    protected void onDestroy() {
        if (levelPlayNativeAd != null) {
            levelPlayNativeAd.destroyAd();
            levelPlayNativeAd = null;
        }
        if (metaAvatarInterstitialAd != null) {
            metaAvatarInterstitialAd.destroy();
            metaAvatarInterstitialAd = null;
        }
        if (admobAvatarInterstitialAd != null) {
            admobAvatarInterstitialAd = null;
        }
        super.onDestroy();
    }

    private void loadAndShowAdMobInterstitialForAvatar() {
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            com.google.android.gms.ads.interstitial.InterstitialAd.load(this, getString(R.string.admob_interstitial_ad_unit_id), adRequest,
                new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd ad) {
                        android.util.Log.d("ProfileActivity", "AdMob Interstitial loaded on avatar update. Showing now.");
                        admobAvatarInterstitialAd = ad;
                        admobAvatarInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                android.util.Log.d("ProfileActivity", "AdMob Interstitial dismissed");
                                admobAvatarInterstitialAd = null;
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                android.util.Log.e("ProfileActivity", "AdMob Interstitial failed to show: " + adError.getMessage());
                                admobAvatarInterstitialAd = null;
                            }
                            @Override
                            public void onAdShowedFullScreenContent() {
                                android.util.Log.d("ProfileActivity", "AdMob Interstitial displayed");
                            }
                        });
                        admobAvatarInterstitialAd.show(ProfileActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        android.util.Log.e("ProfileActivity", "AdMob Interstitial failed to load on avatar update: " + loadAdError.getMessage());
                        admobAvatarInterstitialAd = null;
                    }
                });
        } catch (Exception e) {
            android.util.Log.e("ProfileActivity", "Error loading AdMob Interstitial for avatar: " + e.getMessage());
        }
    }
}
