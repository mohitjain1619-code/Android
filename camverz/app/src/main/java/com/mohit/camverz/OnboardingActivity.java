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
import android.os.Handler;
import android.os.Looper;
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
    private static final String DEFAULT_DOB = "15/06/2002";
    private static final int DEFAULT_AGE = 24;

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
    private String selectedGender = "male";
    private String selectedDob = DEFAULT_DOB;
    private String detectedCountry = "";
    private boolean isDetectingLocation = false;

    // Services
    private ApiService api;
    private TokenManager tokenManager;
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

    /**
     * Pre-populates all 4 required fields with 100% valid, sensible defaults
     * so that the user is NEVER blocked and can proceed with 1 single tap.
     */
    private void setupPrePopulatedData() {
        // 1. Name Pre-population
        String preName = getIntent().getStringExtra("userName");
        if (TextUtils.isEmpty(preName)) {
            preName = tokenManager.getUserName();
        }
        if (TextUtils.isEmpty(preName)) {
            String email = tokenManager.getUserEmail();
            if (!TextUtils.isEmpty(email) && email.contains("@")) {
                preName = email.split("@")[0];
            }
        }
        if (TextUtils.isEmpty(preName)) {
            preName = "Camverz User";
        }
        etName.setText(preName);

        // 2. Gender Pre-selection (Default: male, but respect passed intent or tokenManager)
        String preGender = getIntent().getStringExtra("gender");
        if (TextUtils.isEmpty(preGender)) {
            preGender = tokenManager.getUserGender();
        }
        if (!TextUtils.isEmpty(preGender) && "female".equalsIgnoreCase(preGender)) {
            selectGender("female");
        } else {
            selectGender("male");
        }

        // 3. City / Location Pre-population (Locale display country or intent, never blank!)
        String preCity = getIntent().getStringExtra("city");
        if (TextUtils.isEmpty(preCity)) {
            try {
                Locale currentLocale = Locale.getDefault();
                preCity = currentLocale.getDisplayCountry();
                if (TextUtils.isEmpty(preCity)) {
                    preCity = currentLocale.getCountry();
                }
            } catch (Exception e) {
                Log.w(TAG, "Locale country resolution error: " + e.getMessage());
            }
        }
        if (TextUtils.isEmpty(preCity)) {
            preCity = "Global";
        }
        etCity.setText(preCity);
        detectedCountry = preCity;
        tvLocationStatus.setVisibility(View.VISIBLE);
        tvLocationStatus.setText("📍 " + preCity);

        // 4. Date of Birth Pre-population (Default: valid 18+ adult birthdate, 24 years old)
        String preDob = getIntent().getStringExtra("dob");
        if (!TextUtils.isEmpty(preDob) && preDob.contains("/")) {
            selectedDob = preDob;
        } else {
            selectedDob = DEFAULT_DOB;
        }
        tvBirthday.setText(selectedDob);
        tvBirthday.setTextColor(0xFFFFFFFF);
        tvAgeBadge.setVisibility(View.VISIBLE);
        tvAgeBadge.setText(String.format(getString(R.string.onboarding_age_years), DEFAULT_AGE));

        // 5. Quiet Background Location Refinement (if permission was already granted)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        selectedGender = "female".equalsIgnoreCase(gender) ? "female" : "male";
        if ("male".equalsIgnoreCase(selectedGender)) {
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

        // Parse existing DOB to initialize picker near user's current choice
        int initialYear = cal.get(Calendar.YEAR) - 22;
        int initialMonth = 5; // June
        int initialDay = 15;
        if (!TextUtils.isEmpty(selectedDob) && selectedDob.contains("/")) {
            try {
                String[] parts = selectedDob.split("/");
                if (parts.length == 3) {
                    initialDay = Integer.parseInt(parts[0]);
                    initialMonth = Integer.parseInt(parts[1]) - 1;
                    initialYear = Integer.parseInt(parts[2]);
                }
            } catch (Exception ignored) {}
        }

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
                        // Keep previous valid DOB so user is never left in an unsubmitable state
                        if (TextUtils.isEmpty(selectedDob)) {
                            selectedDob = DEFAULT_DOB;
                        }
                        tvBirthday.setText(selectedDob);
                        tvBirthday.setTextColor(0xFFFFFFFF);
                        tvAgeBadge.setVisibility(View.VISIBLE);
                        tvAgeBadge.setText(String.format(getString(R.string.onboarding_age_years), DEFAULT_AGE));
                    }
                },
                initialYear,
                initialMonth,
                initialDay
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

        try {
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
                        fetchLocation();
                    }
                } else {
                    fetchLocation();
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "GPS check exception: " + t.getMessage());
            fetchLocation();
        }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            setLocationDetectingState(false);
            return;
        }

        try {
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
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
        } catch (Throwable t) {
            Log.e(TAG, "Fresh location error: " + t.getMessage());
            setLocationDetectingState(false);
        }
    }

    private void processLocation(Location location) {
        backgroundExecutor.execute(() -> {
            String resolvedCity = null;
            String resolvedCountry = null;

            // Try Geocoder with Default Locale first
            try {
                Geocoder geocoder = new Geocoder(OnboardingActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    resolvedCity = address.getLocality();
                    if (TextUtils.isEmpty(resolvedCity)) resolvedCity = address.getSubAdminArea();
                    if (TextUtils.isEmpty(resolvedCity)) resolvedCity = address.getAdminArea();
                    resolvedCountry = address.getCountryName();
                }
            } catch (Throwable t) {
                Log.w(TAG, "Primary Geocoder failed: " + t.getMessage());
            }

            // Fallback Geocoder with US Locale if default failed
            if (TextUtils.isEmpty(resolvedCity)) {
                try {
                    Geocoder fallbackGeocoder = new Geocoder(OnboardingActivity.this, Locale.US);
                    List<Address> addresses = fallbackGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        resolvedCity = address.getLocality();
                        if (TextUtils.isEmpty(resolvedCity)) resolvedCity = address.getSubAdminArea();
                        if (TextUtils.isEmpty(resolvedCity)) resolvedCity = address.getAdminArea();
                        if (TextUtils.isEmpty(resolvedCountry)) resolvedCountry = address.getCountryName();
                    }
                } catch (Throwable ignored) {}
            }

            final String finalCity = resolvedCity;
            final String finalCountry = resolvedCountry;

            runOnUiThread(() -> {
                setLocationDetectingState(false);
                if (!TextUtils.isEmpty(finalCity)) {
                    etCity.setText(finalCity);
                    if (!TextUtils.isEmpty(finalCountry)) {
                        detectedCountry = finalCountry;
                    }
                    tvLocationStatus.setVisibility(View.VISIBLE);
                    tvLocationStatus.setText("📍 " + finalCity + (!TextUtils.isEmpty(detectedCountry) ? ", " + detectedCountry : ""));
                }
            });
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
                // Non-blocking: user already has their country pre-filled from Locale!
                Log.d(TAG, "Location permission declined, using pre-filled country.");
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

    // --- Profile Submission with Auto-Retries & Zero-Dropoff Fallbacks ---
    private void submitProfile() {
        String name = etName.getText().toString().trim();
        String city = etCity.getText().toString().trim();

        // 1. Name Resilience
        if (TextUtils.isEmpty(name)) {
            name = tokenManager.getUserName();
            if (TextUtils.isEmpty(name)) {
                String email = tokenManager.getUserEmail();
                if (!TextUtils.isEmpty(email) && email.contains("@")) {
                    name = email.split("@")[0];
                }
            }
            if (TextUtils.isEmpty(name)) {
                name = "Camverz User";
            }
            etName.setText(name);
        }

        // 2. Gender Resilience
        if (TextUtils.isEmpty(selectedGender)) {
            selectedGender = "male";
            selectGender("male");
        }

        // 3. City Resilience (Never block! Use locale country if city is blank)
        if (TextUtils.isEmpty(city)) {
            try {
                city = Locale.getDefault().getDisplayCountry();
            } catch (Exception ignored) {}
            if (TextUtils.isEmpty(city)) {
                city = !TextUtils.isEmpty(detectedCountry) ? detectedCountry : "Global";
            }
            etCity.setText(city);
        }

        // 4. Date of Birth Resilience (Default to 18+ valid birthdate)
        if (TextUtils.isEmpty(selectedDob)) {
            selectedDob = DEFAULT_DOB;
            tvBirthday.setText(selectedDob);
        }

        // Show saving state
        setSavingState(true);

        final String finalName = name;
        final String finalCity = city;
        final String finalGender = selectedGender;
        final String finalDob = selectedDob;

        executeProfileUpdateWithRetry(finalName, finalGender, finalCity, finalDob, 2);
    }

    private void executeProfileUpdateWithRetry(
            final String name,
            final String gender,
            final String city,
            final String dob,
            final int retriesLeft
    ) {
        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("name", name);
        profileUpdate.put("gender", gender);
        profileUpdate.put("city", city);
        profileUpdate.put("dob", dob);
        profileUpdate.put("avatar", "av1");

        api.updateMe(profileUpdate).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Log.d(TAG, "✅ Profile saved to backend successfully");
                        completeOnboardingAndProceed(name, gender);
                        return;
                    }
                }

                if (retriesLeft > 0) {
                    Log.w(TAG, "⚠️ Server update returned error " + response.code() + ", retrying... Retries left: " + retriesLeft);
                    mainHandler.postDelayed(() -> executeProfileUpdateWithRetry(name, gender, city, dob, retriesLeft - 1), 1000);
                    return;
                }

                // If backend still throws, do NOT lock the user out!
                // Cache locally and proceed to main app so user is not blocked.
                Log.w(TAG, "⚠️ Proceeding with local profile completion after server error");
                completeOnboardingAndProceed(name, gender);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "❌ Network failure saving profile: " + t.getMessage());
                if (retriesLeft > 0) {
                    Log.w(TAG, "⚠️ Network error, auto-retrying... Retries left: " + retriesLeft);
                    mainHandler.postDelayed(() -> executeProfileUpdateWithRetry(name, gender, city, dob, retriesLeft - 1), 1000);
                    return;
                }

                // Zero-dropoff policy: save locally and allow entry into app
                Log.w(TAG, "⚠️ Network offline, completing onboarding locally");
                completeOnboardingAndProceed(name, gender);
            }
        });
    }

    private void completeOnboardingAndProceed(String name, String gender) {
        tokenManager.setOnboardingComplete(true);
        tokenManager.saveUser(
                tokenManager.getUserId(),
                name,
                tokenManager.getUserEmail(),
                gender,
                "av1",
                tokenManager.isVerified()
        );

        Intent intent = new Intent(OnboardingActivity.this, MainScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        mainHandler.removeCallbacksAndMessages(null);
    }
}
