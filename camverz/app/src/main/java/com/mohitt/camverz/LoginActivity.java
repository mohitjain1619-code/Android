package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
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
        getStartedButton.setOnClickListener(v -> {
            Log.d(TAG, "🔐 Get Started button clicked");
            signInWithGoogle();
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

        api.authWithGoogle(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();

                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        // Save JWT token
                        String token = data.get("token").getAsString();
                        tokenManager.saveToken(token);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
