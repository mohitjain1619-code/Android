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
import org.json.JSONException;
import org.json.JSONObject;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GoogleSignIn";
    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Socket socket;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        statusText = findViewById(R.id.statusText);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button googleSignInButton = findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // DEBUG: Test socket connection
        socket = SocketManager.getInstance();
        boolean isConnected = socket.connected();
        Log.d(TAG, "🔍 Socket connected on launch: " + isConnected);

        // Test emit after 2 seconds
        new Handler(getMainLooper()).postDelayed(() -> {
            if (socket.connected()) {
                Log.d(TAG, "✅ Socket connection successful!");
            } else {
                Log.e(TAG, "❌ Socket still not connected after 2 seconds");
            }
        }, 2000);
    }

    private void initializeSocket() {
        socket = SocketManager.getInstance();
        
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "✅ Connected to server");
                runOnUiThread(() -> {
                    statusText.setText("✅ Connected to server");
                });
            }
        });

        socket.on("match-found", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String peerId = data.getString("peerId");
                    Log.d(TAG, "Matched with: " + peerId);
                    runOnUiThread(() -> 
                        Toast.makeText(MainActivity.this, "Matched with user!", Toast.LENGTH_SHORT).show()
                    );
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing match-found", e);
                }
            }
        });

        socket.on("error-msg", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "Socket error: " + args[0]);
                runOnUiThread(() -> 
                    Toast.makeText(MainActivity.this, "Error: " + args[0], Toast.LENGTH_SHORT).show()
                );
            }
        });

        socket.connect();
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
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(MainActivity.this, "Sign in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Google Sign In Done");
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid);
                        
                        // ✅ SHOW STATUS MESSAGE
                        runOnUiThread(() -> {
                            statusText.setText("✅ Google Sign In Done");
                        });
                        
                        Toast.makeText(MainActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate after 1.5 seconds
                        new Handler(getMainLooper()).postDelayed(() -> {
                            goToMainScreen();
                        }, 1500);
                    } else {
                        Log.w(TAG, "❌ Sign in failed");
                        statusText.setText("❌ Sign in failed");
                    }
                });
    }

    private void goToMainScreen() {
        Intent intent = new Intent(MainActivity.this, MainScreenActivity.class);
        startActivity(intent);
        finish();  // Prevent back press from returning to login
    }

    private void saveUserToFirestore(String uid) {
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                Map<String, Object> user = new HashMap<>();
                user.put("uid", uid);
                user.put("name", mAuth.getCurrentUser().getDisplayName());
                user.put("email", mAuth.getCurrentUser().getEmail());
                user.put("photoUrl", mAuth.getCurrentUser().getPhotoUrl() != null ? 
                        mAuth.getCurrentUser().getPhotoUrl().toString() : "");
                user.put("gender", "");
                user.put("city", "");
                user.put("age", 0);
                user.put("verified", false);
                user.put("verificationStatus", "pending");
                user.put("createdAt", System.currentTimeMillis());
                user.put("banned", false);

                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firestore"))
                        .addOnFailureListener(e -> Log.w(TAG, "Error saving user", e));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect here - keep connection alive for the app
    }
}
