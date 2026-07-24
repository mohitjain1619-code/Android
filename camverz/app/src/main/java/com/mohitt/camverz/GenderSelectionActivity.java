package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GenderSelectionActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Button btnMale, btnFemale, btnNext;
    private String selectedGender = null;

    private static final String TAG = "GenderSelection";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gender);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnMale = findViewById(R.id.btnMale);
        btnFemale = findViewById(R.id.btnFemale);
        btnNext = findViewById(R.id.btnNext);

        btnMale.setOnClickListener(v -> selectGender("male"));
        btnFemale.setOnClickListener(v -> selectGender("female"));

        btnNext.setOnClickListener(v -> saveGenderToFirestore());
    }

    private void selectGender(String gender) {
        selectedGender = gender;

        Log.d(TAG, "Gender selected: " + selectedGender);

        btnMale.setAlpha(gender.equals("male") ? 1.0f : 0.5f);
        btnFemale.setAlpha(gender.equals("female") ? 1.0f : 0.5f);
    }

    private void saveGenderToFirestore() {
        if (selectedGender == null) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> update = new HashMap<>();
        update.put("gender", selectedGender);
        update.put("genderUpdatedAt", System.currentTimeMillis());

        Log.d(TAG, "Saving gender for UID: " + uid);

        db.collection("users")
                .document(uid)
                .update(update)
                .addOnSuccessListener(a -> {
                    Log.d(TAG, "Gender saved successfully");
                    goToMainScreen();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving gender: " + e.getMessage());
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToMainScreen() {
        Intent intent = new Intent(GenderSelectionActivity.this, MainScreenActivity.class);
        startActivity(intent);
        finish();
    }
}
