package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FinishActivity extends BaseActivity {

    private static final String TAG = "FinishActivity";
    EditText finalName;
    TextView finalGender, finalCity, finalDob;
    Button btnStart;
    android.view.View rootLayout;

    private String userName, gender, city, dob;
    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        rootLayout = findViewById(R.id.rootLayout);
        finalName = findViewById(R.id.finalName);
        finalGender = findViewById(R.id.finalGender);
        finalCity = findViewById(R.id.finalCity);
        finalDob = findViewById(R.id.finalDob);
        btnStart = findViewById(R.id.btnStartUsing);

        // Receive data from previous screens
        userName = getIntent().getStringExtra("userName");
        gender = getIntent().getStringExtra("gender");
        city = getIntent().getStringExtra("city");
        dob = getIntent().getStringExtra("dob");

        // Display on screen
        finalName.setText(userName);
        finalGender.setText("Gender: " + (gender != null ? capitalizeFirst(gender) : "N/A"));
        finalCity.setText("City: " + (city != null ? city : "N/A"));
        finalDob.setText("Birthday: " + (dob != null ? dob : "N/A"));

        btnStart.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (btnStart.isEnabled()) v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (btnStart.isEnabled()) v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });

        btnStart.setOnClickListener(v -> saveProfileAndGoToHome());

        // Close keyboard when clicking outside EditText
        rootLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (finalName.isFocused()) {
                    hideKeyboard();
                    finalName.clearFocus();
                }
            }
            return false;
        });

        // Auto-hide keyboard when done editing
        finalName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && finalName != null) {
            imm.hideSoftInputFromWindow(finalName.getWindowToken(), 0);
        }
    }

    private void saveProfileAndGoToHome() {
        String name = finalName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            finalName.setError("Please enter your name");
            return;
        }

        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("name", name);
        profileUpdate.put("gender", gender);
        profileUpdate.put("city", city);
        profileUpdate.put("dob", dob);
        profileUpdate.put("avatar", "av1");

        btnStart.setEnabled(false);

        api.updateMe(profileUpdate).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Log.d(TAG, "✅ Profile saved to backend");
                        Toast.makeText(FinishActivity.this, "Profile complete!", Toast.LENGTH_SHORT).show();
                        
                        // Update cached user data
                        tokenManager.saveUser(
                                tokenManager.getUserId(),
                                name,
                                tokenManager.getUserEmail(),
                                gender,
                                "av1",
                                tokenManager.isVerified()
                        );
                        
                        goToMainScreen();
                        return;
                    }
                }
                
                Log.e(TAG, "❌ Error saving profile: " + response.code());
                Toast.makeText(FinishActivity.this, "Error saving profile", Toast.LENGTH_SHORT).show();
                btnStart.setEnabled(true);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "❌ Error saving profile: " + t.getMessage());
                Toast.makeText(FinishActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                btnStart.setEnabled(true);
            }
        });
    }

    private void goToMainScreen() {
        Intent i = new Intent(FinishActivity.this, MainScreenActivity.class);
        startActivity(i);
        finish();
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
