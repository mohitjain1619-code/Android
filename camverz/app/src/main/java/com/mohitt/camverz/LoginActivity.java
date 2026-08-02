package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;
    private TokenManager tokenManager;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenManager = TokenManager.getInstance(this);
        api = ApiClient.getInstance(this).getApi();

        // Check Play Install Referrer for referrals
        checkInstallReferrer();

        // Check if already signed in (has JWT token)
        if (tokenManager.isLoggedIn()) {
            Log.d(TAG, "User already signed in with JWT, going to MainScreen");
            goToMainScreen();
            return;
        }

        // Configure Google Sign-In (same as before — still using Google OAuth)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button getStartedButton = findViewById(R.id.get_started_button);
        android.view.View logoHero = findViewById(R.id.logo_hero_container);
        android.view.View appTitle = findViewById(R.id.app_title);
        android.view.View appSubtitle = findViewById(R.id.app_subtitle);

        // Entrance Animations
        if (logoHero != null) {
            logoHero.setAlpha(0f);
            logoHero.setTranslationY(-30f);
            logoHero.animate().alpha(1f).translationY(0f).setDuration(600).start();
        }
        if (appTitle != null) {
            appTitle.setAlpha(0f);
            appTitle.animate().alpha(1f).setDuration(800).setStartDelay(200).start();
        }
        if (appSubtitle != null) {
            appSubtitle.setAlpha(0f);
            appSubtitle.animate().alpha(1f).setDuration(800).setStartDelay(350).start();
        }

        // Button Micro-interaction
        getStartedButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });

        getStartedButton.setOnClickListener(v -> {
            Log.d(TAG, "🔐 Get Started button clicked");
            signInWithGoogle();
        });

        TextView testerLoginLink = findViewById(R.id.tester_login_link);
        testerLoginLink.setOnClickListener(v -> {
            Log.d(TAG, "🔓 Tester login link clicked");
            showTesterLoginDialog();
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    Log.d(TAG, "✅ Google Sign In successful");
                    authenticateWithBackend(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "❌ Google sign in failed", e);
                Toast.makeText(LoginActivity.this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Send Google ID token to OUR backend (not Firebase).
     * Backend verifies with Google, creates/finds user in PostgreSQL, returns JWT.
     */
    private void authenticateWithBackend(String idToken) {
        Map<String, String> body = new HashMap<>();
        body.put("idToken", idToken);

        // Fetch saved referrer code
        String affiliateRef = getSharedPreferences("camverz_prefs", MODE_PRIVATE)
                .getString("affiliate_ref", null);
        if (affiliateRef != null && !affiliateRef.trim().isEmpty()) {
            body.put("affiliateRef", affiliateRef);
            Log.d(TAG, "Sending affiliateRef to backend: " + affiliateRef);
        }

        api.authWithGoogle(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();

                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        // Save JWT token
                        String token = data.get("token").getAsString();
                        tokenManager.saveToken(token);

                        // Clear saved referrer code once successfully registered
                        getSharedPreferences("camverz_prefs", MODE_PRIVATE)
                                .edit()
                                .remove("affiliate_ref")
                                .apply();

                        // Save user data
                        JsonObject user = data.getAsJsonObject("user");
                        tokenManager.saveUser(
                                user.get("id").getAsString(),
                                user.has("name") ? user.get("name").getAsString() : "",
                                user.has("email") ? user.get("email").getAsString() : "",
                                user.has("gender") ? user.get("gender").getAsString() : "",
                                user.has("avatar") ? user.get("avatar").getAsString() : "",
                                user.has("verified") && user.get("verified").getAsBoolean()
                        );

                        boolean isNewUser = data.has("isNewUser") && data.get("isNewUser").getAsBoolean();

                        Log.d(TAG, "✅ Backend authentication successful. New user: " + isNewUser);
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show()
                        );

                        // Navigate based on profile completeness
                        String gender = user.has("gender") ? user.get("gender").getAsString() : "";
                        if (isNewUser || gender == null || gender.trim().isEmpty()) {
                            goToGenderSelection();
                        } else {
                            goToMainScreen();
                        }
                    } else {
                        Log.w(TAG, "❌ Backend auth response not ok");
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    Log.w(TAG, "❌ Backend auth failed: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    );
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "❌ Network error during auth", t);
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void goToGenderSelection() {
        Intent intent = new Intent(LoginActivity.this, GenderSelectionActivity.class);
        intent.putExtra("userName", tokenManager.getUserName());
        startActivity(intent);
        finish();
    }

    private void goToMainScreen() {
        Intent intent = new Intent(LoginActivity.this, MainScreenActivity.class);
        startActivity(intent);
        finish();
    }

    private void showTesterLoginDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Tester Login");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        final android.widget.EditText emailInput = new android.widget.EditText(this);
        emailInput.setHint("Tester Email");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        final android.widget.EditText passwordInput = new android.widget.EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.equalsIgnoreCase("tester@camverz.com") && password.equals("camverz_tester_2026")) {
                Log.d(TAG, "🔓 Tester bypass credentials matched. Authenticating with backend...");
                authenticateWithBackend("google-play-reviewer-bypass-key-2026");
            } else {
                Toast.makeText(LoginActivity.this, "Invalid Tester Credentials", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void checkInstallReferrer() {
        if (getSharedPreferences("camverz_prefs", MODE_PRIVATE).getBoolean("referrer_checked", false)) {
            return;
        }

        try {
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(this).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        try {
                            ReferrerDetails response = referrerClient.getInstallReferrer();
                            String referrerUrl = response.getInstallReferrer();
                            if (referrerUrl != null && !referrerUrl.isEmpty()) {
                                Log.d(TAG, "Install Referrer: " + referrerUrl);
                                String refCode = null;
                                if (referrerUrl.contains("utm_campaign=")) {
                                    int startIndex = referrerUrl.indexOf("utm_campaign=") + "utm_campaign=".length();
                                    int endIndex = referrerUrl.indexOf("&", startIndex);
                                    if (endIndex == -1) {
                                        refCode = referrerUrl.substring(startIndex);
                                    } else {
                                        refCode = referrerUrl.substring(startIndex, endIndex);
                                    }
                                } else if (!referrerUrl.contains("=") && !referrerUrl.contains("&")) {
                                    refCode = referrerUrl;
                                } else {
                                    if (referrerUrl.contains("ref=")) {
                                        int startIndex = referrerUrl.indexOf("ref=") + 4;
                                        int endIndex = referrerUrl.indexOf("&", startIndex);
                                        refCode = (endIndex == -1) ? referrerUrl.substring(startIndex) : referrerUrl.substring(startIndex, endIndex);
                                    } else if (referrerUrl.contains("referrer=")) {
                                        int startIndex = referrerUrl.indexOf("referrer=") + 9;
                                        int endIndex = referrerUrl.indexOf("&", startIndex);
                                        refCode = (endIndex == -1) ? referrerUrl.substring(startIndex) : referrerUrl.substring(startIndex, endIndex);
                                    }
                                }

                                if (refCode != null && !refCode.trim().isEmpty()) {
                                    String finalCode = refCode.trim().toUpperCase();
                                    getSharedPreferences("camverz_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("affiliate_ref", finalCode)
                                            .apply();
                                    Log.d(TAG, "Referral Code captured & saved: " + finalCode);
                                }
                            }
                            getSharedPreferences("camverz_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("referrer_checked", true)
                                    .apply();
                            referrerClient.endConnection();
                        } catch (Exception e) {
                            Log.w(TAG, "Error getting referrer details", e);
                        }
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    // Disconnected
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Error initializing InstallReferrerClient", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
