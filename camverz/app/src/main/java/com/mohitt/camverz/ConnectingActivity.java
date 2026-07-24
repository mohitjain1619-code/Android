package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import io.socket.client.Socket;

public class ConnectingActivity extends AppCompatActivity {

    private static final String TAG = "ConnectingActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Socket socket;

    private String category = "";
    private String userGender = "";
    private String myUid = "";

    private boolean isWaiting = false;
    // This flag is crucial to prevent a race condition where we leave the queue
    // after accepting a match.
    private boolean matchAccepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        socket = SocketManager.getInstance();

        myUid = mAuth.getCurrentUser().getUid();
        category = getIntent().getStringExtra("category");
        if (category == null) category = "straight";

        setupSocketListeners();
        loadUserDataAndJoinQueue();

        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            leaveQueue();
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        leaveQueue();
    }

    private void loadUserDataAndJoinQueue() {
        db.collection("users").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userGender = doc.getString("gender");
                        if (userGender == null) userGender = "unknown";
                        joinQueue();
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user: " + e.getMessage()));
    }

    private void joinQueue() {
        if (isWaiting) return;
        isWaiting = true;

        try {
            JSONObject obj = new JSONObject();
            obj.put("uid", myUid);
            obj.put("gender", userGender);
            obj.put("category", category);

            Log.d(TAG, "📤 join-queue");
            socket.emit("join-queue", obj);
        } catch (Exception e) {
            Log.e(TAG, "join-queue failed: " + e.getMessage());
        }
    }

    private void leaveQueue() {
        isWaiting = false;
        try {
            JSONObject obj = new JSONObject();
            obj.put("uid", myUid);

            Log.d(TAG, "📤 leave-queue");
            socket.emit("leave-queue", obj);
        } catch (Exception e) {
            Log.e(TAG, "leave-queue failed: " + e.getMessage());
        }
    }

    private void setupSocketListeners() {
        // Clear old listeners to be safe
        socket.off("match-found");

        socket.on("match-found", args -> {
            if (!isWaiting) return; // Prevent processing if we already have a match dialog

            try {
                JSONObject data = (JSONObject) args[0];
                String peerId = data.getString("peerId");

                if (peerId.equals(myUid)) return;

                runOnUiThread(() -> {
                    isWaiting = false;
                    socket.off("match-found"); // Unregister immediately to avoid multiple dialogs

                    new android.app.AlertDialog.Builder(ConnectingActivity.this)
                            .setTitle("Match Found!")
                            .setMessage("Do you want to connect?")
                            .setPositiveButton("Connect", (d, w) -> {
                                matchAccepted = true; // CRITICAL: Set flag to prevent leaving queue
                                Intent i = new Intent(ConnectingActivity.this, CallActivity.class);
                                i.putExtra("peer", peerId);
                                i.putExtra("category", category);
                                startActivity(i);
                                finish();
                            })
                            .setNegativeButton("Skip", (d, w) -> {
                                joinQueue(); // Re-join queue if user skips
                                d.dismiss();
                            })
                            .setCancelable(false)
                            .show();
                });

            } catch (Exception e) {
                Log.e(TAG, "match-found error: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // This is the key fix: Only leave the queue if the user did NOT accept a match.
        if (!matchAccepted) {
            leaveQueue();
        }
        // Clean up listener
        socket.off("match-found");
    }
}
