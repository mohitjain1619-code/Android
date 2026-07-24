package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONException;
import org.json.JSONObject;
import io.socket.client.Socket;

public class MainScreenActivity extends AppCompatActivity {

    private static final String TAG = "MainScreen";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Socket socket;
    private TextView userNameTextView, statusTextView;
    private Button startChatButton, verificationButton, logoutButton;
    private LinearLayout cardGay, cardLesbian, cardStraight;
    private String userGender = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        socket = SocketManager.getInstance();

        userNameTextView = findViewById(R.id.user_name);
        statusTextView = findViewById(R.id.status_text);
        startChatButton = findViewById(R.id.start_chat_button);
        verificationButton = findViewById(R.id.verification_button);
        logoutButton = findViewById(R.id.logout_button);
        cardGay = findViewById(R.id.cardGay);
        cardLesbian = findViewById(R.id.cardLesbian);
        cardStraight = findViewById(R.id.cardStraight);

        loadUserProfile();
        setupSocketListeners();
        loadUserGender();

        verificationButton.setOnClickListener(v -> goToVerification());
        logoutButton.setOnClickListener(v -> logout());
        cardGay.setOnClickListener(v -> {
            if (!userGender.equals("male")) {
                Toast.makeText(MainScreenActivity.this, "Only males can join Gay section", Toast.LENGTH_SHORT).show();
                return;
            }
            goToConnecting("gay");
        });
        cardLesbian.setOnClickListener(v -> {
            if (!userGender.equals("female")) {
                Toast.makeText(MainScreenActivity.this, "Only females can join Lesbian section", Toast.LENGTH_SHORT).show();
                return;
            }
            goToConnecting("lesbian");
        });
        cardStraight.setOnClickListener(v -> goToConnecting("straight"));
    }

    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String name = (String) task.getResult().get("name");
                runOnUiThread(() -> {
                    userNameTextView.setText("Welcome, " + (name != null ? name : "User"));
                    Boolean verified = (Boolean) task.getResult().get("verified");
                    if (verified != null && verified) {
                        statusTextView.setText("✓ Verified");
                    } else {
                        statusTextView.setText("⚠ Not Verified");
                    }
                });
            }
        });
    }

    private void setupSocketListeners() {
        socket.on("match-found", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");
                Log.d(TAG, "Matched with: " + peerId);
                runOnUiThread(() -> {
                    statusTextView.setText("Matched! Starting call...");
                    Toast.makeText(MainScreenActivity.this, "Matched with: " + peerId, Toast.LENGTH_SHORT).show();
                });
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing match-found", e);
            }
        });
    }

    private void goToVerification() {
        Intent intent = new Intent(MainScreenActivity.this, VerificationActivity.class);
        startActivity(intent);
    }

    private void logout() {
        mAuth.signOut();
        socket.disconnect();
        Intent intent = new Intent(MainScreenActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadUserGender() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userGender = doc.getString("gender");
                    }
                });
    }

    private void goToConnecting(String category) {
        Intent intent = new Intent(MainScreenActivity.this, ConnectingActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }
}
