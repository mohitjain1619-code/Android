package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationInfoActivity extends BaseActivity {

    private static final String TAG = "VerificationInfo";
    private static final int VERIFICATION_REQUEST = 101;
    private TextView genderDisplay;
    private Button startVerificationButton;
    private ApiService api;
    private String userGender = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_info);

        genderDisplay = findViewById(R.id.gender_display);
        startVerificationButton = findViewById(R.id.start_verification_button);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        api = ApiClient.getInstance(this).getApi();

        fetchUserGender();

        startVerificationButton.setOnClickListener(v -> {
            if ("male".equalsIgnoreCase(userGender)) {
                autoVerifyMaleUser();
            } else {
                Intent intent = new Intent(this, VerificationActivity.class);
                startActivityForResult(intent, VERIFICATION_REQUEST);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void autoVerifyMaleUser() {
        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("verificationStatus", "pending");
        verificationData.put("verificationDate", System.currentTimeMillis());
        verificationData.put("livenessVerified", true);
        verificationData.put("genderVerified", true);
        verificationData.put("verified", true);

        api.updateMe(verificationData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().get("ok").getAsBoolean()) {
                    Toast.makeText(VerificationInfoActivity.this, "✅ Male profile auto-verified successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(VerificationInfoActivity.this, "Auto-verification failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error auto-verifying male user", t);
                Toast.makeText(VerificationInfoActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VERIFICATION_REQUEST && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void fetchUserGender() {
        api.getMe().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("user")) {
                        JsonObject user = data.getAsJsonObject("user");
                        if (user.has("gender") && !user.get("gender").isJsonNull()) {
                            userGender = user.get("gender").getAsString();
                            if (!userGender.isEmpty()) {
                                String displayGender = userGender.substring(0, 1).toUpperCase() + userGender.substring(1).toLowerCase();
                                genderDisplay.setText("Selected Gender: " + displayGender);
                            } else {
                                genderDisplay.setText("Gender not set");
                            }
                        } else {
                            genderDisplay.setText("Gender not set");
                        }
                    }
                } else {
                    Toast.makeText(VerificationInfoActivity.this, "Failed to load your information", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to fetch user gender", t);
                Toast.makeText(VerificationInfoActivity.this, "Failed to load your information", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
