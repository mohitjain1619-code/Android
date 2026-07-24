package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        statusText = findViewById(R.id.statusText);

        // Check if already signed in
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User already signed in, going to MainScreen");
            goToMainScreen();
            return;
        }

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button googleSignInButton = findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(v -> {
            Log.d(TAG, "🔐 Google Sign In button clicked");
            signInWithGoogle();
        });

        // Initialize socket connection
        initializeSocket();
    }

    private void initializeSocket() {
        try {
            SocketManager.getInstance();
            Log.d(TAG, "✅ Socket initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing socket: " + e.getMessage(), e);
        }
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
                if (account != null) {
                    Log.d(TAG, "✅ Google Sign In successful");
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "❌ Google sign in failed", e);
                Toast.makeText(LoginActivity.this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Firebase authentication successful");
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid);
                        
                        runOnUiThread(() -> {
                            statusText.setText("✅ Google Sign In Done");
                            Toast.makeText(LoginActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                        });
                        
                        // Navigate after 1.5 seconds
                        new Handler(getMainLooper()).postDelayed(() -> {
                            goToGenderSelection();
                        }, 1500);
                    } else {
                        Log.w(TAG, "❌ Firebase auth failed", task.getException());
                        runOnUiThread(() -> {
                            statusText.setText("❌ Sign in failed");
                            Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void saveUserToFirestore(String uid) {
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    // New user - create document
                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("name", mAuth.getCurrentUser().getDisplayName());
                    user.put("email", mAuth.getCurrentUser().getEmail());
                    user.put("photoUrl", mAuth.getCurrentUser().getPhotoUrl() != null ? 
                            mAuth.getCurrentUser().getPhotoUrl().toString() : "");
                    user.put("gender", "");
                    user.put("verified", false);
                    user.put("createdAt", System.currentTimeMillis());

                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "✓ User saved to Firestore"))
                            .addOnFailureListener(e -> Log.w(TAG, "Error saving user", e));
                } else {
                    Log.d(TAG, "✓ User already exists in Firestore");
                }
            }
        });
    }

    private void goToGenderSelection() {
        Intent intent = new Intent(LoginActivity.this, GenderSelectionActivity.class);
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