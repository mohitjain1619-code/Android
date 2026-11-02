package com.mohit.camverz;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnboardingActivity extends BaseActivity {

    private static final String TAG = "OnboardingActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;
    private static final int REQUEST_CHECK_SETTINGS = 2002;

    // UI Elements
    private View rootContainer;
    private EditText etName, etCity;
    private LinearLayout cardMale, cardFemale;
    private ImageView ivMaleIcon, ivFemaleIcon;
    private TextView tvMaleText, tvFemaleText;
    private LinearLayout btnAutoDetectCity;
    private TextView tvAutoDetectText, tvLocationStatus;
    private LinearLayout cardBirthday;
    private TextView tvBirthday, tvAgeBadge;
    private Button btnStartCamverz;
    private ProgressBar progressBarSaving;

    // State Variables
    private String selectedGender = "";
    private String selectedDob = "";
    private String detectedCountry = "";
    private boolean isDetectingLocation = false;

    // Services
    private ApiService api;
    private TokenManager tokenManager;
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupPrePopulatedData();
        setupListeners();
    }

    private void initViews() {
        rootContainer = findViewById(R.id.rootContainer);
        etName = findViewById(R.id.etName);
        etCity = findViewById(R.id.etCity);

        cardMale = findViewById(R.id.cardMale);
        cardFemale = findViewById(R.id.cardFemale);
        ivMaleIcon = findViewById(R.id.ivMaleIcon);
        ivFemaleIcon = findViewById(R.id.ivFemaleIcon);
        tvMaleText = findViewById(R.id.tvMaleText);
        tvFemaleText = findViewById(R.id.tvFemaleText);

        btnAutoDetectCity = findViewById(R.id.btnAutoDetectCity);
        tvAutoDetectText = findViewById(R.id.tvAutoDetectText);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);

        cardBirthday = findViewById(R.id.cardBirthday);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvAgeBadge = findViewById(R.id.tvAgeBadge);

        btnStartCamverz = findViewById(R.id.btnStartCamverz);
        progressBarSaving = findViewById(R.id.progressBarSaving);
    }

    private void setupPrePopulatedData() {
        // Pre-fill Name from Intent or TokenManager if available
        String preName = getIntent().getStringExtra("userName");
        if (TextUtils.isEmpty(preName)) {
            preName = tokenManager.getUserName();
        }
        if (!TextUtils.isEmpty(preName)) {
            etName.setText(preName);
        }

        // Auto trigger location detection quietly on start if permission already granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkGpsSettingsAndFetchLocation();
        }
    }

    private void setupListeners() {
        // Gender Card Selection
        cardMale.setOnClickListener(v -> selectGender("male"));
        cardFemale.setOnClickListener(v -> selectGender("female"));

        // City Auto Detect Button
        btnAutoDetectCity.setOnClickListener(v -> checkGpsSettingsAndFetchLocation());

        // Birthday Picker Click
        cardBirthday.setOnClickListener(v -> openDatePicker());

        // Start Camverz Submit Button
        setupTouchScale(btnStartCamverz);
        btnStartCamverz.setOnClickListener(v -> submitProfile());

        // Touch feedback on interactive cards
        setupTouchScale(cardMale);
        setupTouchScale(cardFemale);
        setupTouchScale(btnAutoDetectCity);
        setupTouchScale(cardBirthday);

        // Hide keyboard when touching outside input fields
        if (rootContainer != null) {
            rootContainer.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideKeyboard();
                }
                return false;
            });
        }
    }

    private void selectGender(String gender) {
        selectedGender = gender;
        if ("male".equalsIgnoreCase(gender)) {
            cardMale.setBackgroundResource(R.drawable.bg_gender_card_selected_male);
            tvMaleText.setTextColor(0xFF00E5FF);
            ivMaleIcon.setColorFilter(0xFF00E5FF);

            cardFemale.setBackgroundResource(R.drawable.bg_gender_card_unselected);
            tvFemaleText.setTextColor(0xFFFFFFFF);
            ivFemaleIcon.setColorFilter(0x80FFFFFF);
        } else {
            cardFemale.setBackgroundResource(R.drawable.bg_gender_card_selected_female);
            tvFemaleText.setTextColor(0xFFFF0080);
            ivFemaleIcon.setColorFilter(0xFFFF0080);

            cardMale.setBackgroundResource(R.drawable.bg_gender_card_unselected);
            tvMaleText.setTextColor(0xFFFFFFFF);
            ivMaleIcon.setColorFilter(0x80FFFFFF);
        }
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerTheme,
                (view, year, month, day) -> {
                    int age = calculateAge(year, month, day);
                    if (age >= 18) {
                        selectedDob = day + "/" + (month + 1) + "/" + year;
                        tvBirthday.setText(selectedDob);
                        tvBirthday.setTextColor(0xFFFFFFFF);

                        tvAgeBadge.setVisibility(View.VISIBLE);
                        tvAgeBadge.setText(String.format(getString(R.string.onboarding_age_years), age));
                    } else {
                        showAgeErrorDialog();
                        selectedDob = "";
                        tvBirthday.setText(R.string.onboarding_birthday_hint);
                        tvBirthday.setTextColor(0xFF64748B);
                        tvAgeBadge.setVisibility(View.GONE);
                    }
                },
                cal.get(Calendar.YEAR) - 18,
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private int calculateAge(int year, int month, int day) {
        Calendar today = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();
        dob.set(year, month, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private void showAgeErrorDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_age_error, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            View btnOk = dialogView.findViewById(R.id.btnUnderstood);
            if (btnOk != null) {
                btnOk.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.onboarding_age_warning, Toast.LENGTH_LONG).show();
        }
    }

    // --- GPS / Location Flow ---
    private void checkGpsSettingsAndFetchLocation() {
        if (isDetectingLocation) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(1)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> fetchLocation());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(OnboardingActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e(TAG, "Error starting GPS resolution: " + sendEx.getMessage());
                }
            } else {
                fetchLocation();
            }
        });
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        setLocationDetectingState(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                processLocation(location);
            } else {
                requestFreshLocation();
            }
        }).addOnFailureListener(this, e -> requestFreshLocation());
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            setLocationDetectingState(false);
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        processLocation(location);
                    } else {
                        setLocationDetectingState(false);
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Fresh location fetch failed: " + e.getMessage());
                    setLocationDetectingState(false);
                });
    }

    private void processLocation(Location location) {
        backgroundExecutor.execute(() -> {
            Geocoder geocoder = new Geocoder(OnboardingActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String city = address.getLocality();
                    if (city == null || city.isEmpty()) city = address.getSubAdminArea();
                    if (city == null || city.isEmpty()) city = address.getAdminArea();

                    String country = address.getCountryName();
                    final String finalCity = city;
                    final String finalCountry = country;

                    runOnUiThread(() -> {
                        setLocationDetectingState(false);
                        if (finalCity != null && !finalCity.isEmpty()) {
                            etCity.setText(finalCity);
                            detectedCountry = finalCountry != null ? finalCountry : "";
                            tvLocationStatus.setVisibility(View.VISIBLE);
                            tvLocationStatus.setText("📍 " + finalCity + (!detectedCountry.isEmpty() ? ", " + detectedCountry : ""));
                        }
                    });
                    return;
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error: " + e.getMessage());
            }

            runOnUiThread(() -> setLocationDetectingState(false));
        });
    }

    private void setLocationDetectingState(boolean detecting) {
        isDetectingLocation = detecting;
        if (detecting) {
            tvAutoDetectText.setText(R.string.onboarding_detecting_location);
            btnAutoDetectCity.setAlpha(0.7f);
        } else {
            tvAutoDetectText.setText(R.string.onboarding_auto_detect);
            btnAutoDetectCity.setAlpha(1.0f);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Permission denied. You can manually enter your city.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                fetchLocation();
            }
        }
    }

    // --- Profile Submission ---
    private void submitProfile() {
        String name = etName.getText().toString().trim();
        String city = etCity.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError(getString(R.string.onboarding_error_name));
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(selectedGender)) {
            Toast.makeText(this, R.string.onboarding_error_gender, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(city)) {
            etCity.setError(getString(R.string.onboarding_error_city));
            etCity.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(selectedDob)) {
            Toast.makeText(this, R.string.onboarding_error_dob, Toast.LENGTH_SHORT).show();
            openDatePicker();
            return;
        }

        // Show saving state
        setSavingState(true);

        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("name", name);
        profileUpdate.put("gender", selectedGender);
        profileUpdate.put("city", city);
        profileUpdate.put("dob", selectedDob);
        profileUpdate.put("avatar", "av1");

        api.updateMe(profileUpdate).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Log.d(TAG, "✅ Profile saved to backend successfully");

                        // Update local preferences
                        tokenManager.setOnboardingComplete(true);
                        tokenManager.saveUser(
                                tokenManager.getUserId(),
                                name,
                                tokenManager.getUserEmail(),
                                selectedGender,
                                "av1",
                                tokenManager.isVerified()
                        );

                        // Route to MainScreenActivity
                        Intent intent = new Intent(OnboardingActivity.this, MainScreenActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }

                Log.e(TAG, "❌ Error response from server: " + response.code());
                setSavingState(false);
                Toast.makeText(OnboardingActivity.this, "Error saving profile. Please try again.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "❌ Network failure saving profile: " + t.getMessage());
                setSavingState(false);
                Toast.makeText(OnboardingActivity.this, "Network error. Please check connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSavingState(boolean saving) {
        if (saving) {
            btnStartCamverz.setEnabled(false);
            btnStartCamverz.setText("");
            progressBarSaving.setVisibility(View.VISIBLE);
        } else {
            btnStartCamverz.setEnabled(true);
            btnStartCamverz.setText(R.string.onboarding_btn_start);
            progressBarSaving.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            currentFocus.clearFocus();
        }
    }

    private void setupTouchScale(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }
}
