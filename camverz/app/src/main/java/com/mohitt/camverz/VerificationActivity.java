package com.mohitt.camverz;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class VerificationActivity extends AppCompatActivity {

    private static final String TAG = "Verification";
    private static final int CAMERA_REQUEST = 1;
    private static final int GALLERY_REQUEST = 2;

    private ImageView selfieImageView, idImageView;
    private Button takeSelfieButton, uploadIdButton, submitButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri selfieUri, idUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        selfieImageView = findViewById(R.id.selfie_image);
        idImageView = findViewById(R.id.id_image);
        takeSelfieButton = findViewById(R.id.take_selfie_button);
        uploadIdButton = findViewById(R.id.upload_id_button);
        submitButton = findViewById(R.id.submit_verification_button);

        takeSelfieButton.setOnClickListener(v -> openCamera());
        uploadIdButton.setOnClickListener(v -> openGallery());
        submitButton.setOnClickListener(v -> submitVerification());

        checkVerificationStatus();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST && data != null) {
                selfieUri = data.getData();
                selfieImageView.setImageURI(selfieUri);
                uploadSelfieToStorage();
            } else if (requestCode == GALLERY_REQUEST && data != null) {
                idUri = data.getData();
                idImageView.setImageURI(idUri);
                uploadIdToStorage();
            }
        }
    }

    private void uploadSelfieToStorage() {
        String uid = mAuth.getCurrentUser().getUid();
        StorageReference selfieRef = storageRef.child("selfies/" + uid + ".jpg");

        if (selfieUri != null) {
            selfieRef.putFile(selfieUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Selfie uploaded successfully");
                        Toast.makeText(VerificationActivity.this, "Selfie uploaded", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Selfie upload failed", e);
                        Toast.makeText(VerificationActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void uploadIdToStorage() {
        String uid = mAuth.getCurrentUser().getUid();
        StorageReference idRef = storageRef.child("ids/" + uid + ".jpg");

        if (idUri != null) {
            idRef.putFile(idUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "ID uploaded successfully");
                        Toast.makeText(VerificationActivity.this, "ID uploaded", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "ID upload failed", e);
                        Toast.makeText(VerificationActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void submitVerification() {
        if (selfieUri == null || idUri == null) {
            Toast.makeText(this, "Please upload both selfie and ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).update("verificationStatus", "pending")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Verification submitted");
                    Toast.makeText(VerificationActivity.this, "Verification submitted for review", Toast.LENGTH_SHORT).show();
                    // Navigate to home after submission
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error submitting verification", e));
    }

    private void checkVerificationStatus() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Boolean verified = (Boolean) task.getResult().get("verified");
                String status = (String) task.getResult().get("verificationStatus");

                if (verified != null && verified) {
                    // User already verified
                    Toast.makeText(VerificationActivity.this, "Already verified!", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (status != null && status.equals("pending")) {
                    // Verification is pending
                    Toast.makeText(VerificationActivity.this, "Verification pending review", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
