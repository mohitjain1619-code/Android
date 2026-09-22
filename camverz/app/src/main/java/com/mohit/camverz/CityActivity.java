package com.mohit.camverz;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CityActivity extends BaseActivity {

    private static final String TAG = "CityActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 1002;

    private FusedLocationProviderClient fusedLocationClient;
    private Button btnContinue;
    private TextView backBtn, tvCity, tvCountry;
    private ImageView locationIcon;

    private String userName, gender, city, country;
    private boolean isDetectingLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        btnContinue = findViewById(R.id.btnContinueCity);
        backBtn = findViewById(R.id.backBtnCity);
        tvCity = findViewById(R.id.tvCity);
        tvCountry = findViewById(R.id.tvCountry);
        locationIcon = findViewById(R.id.location_icon);

        userName = getIntent().getStringExtra("userName");
        gender = getIntent().getStringExtra("gender");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        // Location Icon Pulse Animation
        if (locationIcon != null) {
            startPulseAnimation(locationIcon);
        }

        // Tap on location display to retry location detection
        View.OnClickListener retryClickListener = v -> checkGpsSettingsAndFetchLocation();
        if (tvCity != null) tvCity.setOnClickListener(retryClickListener);
        if (tvCountry != null) tvCountry.setOnClickListener(retryClickListener);
        if (locationIcon != null) locationIcon.setOnClickListener(retryClickListener);

        btnContinue.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });

        btnContinue.setOnClickListener(v -> {
            if (city == null || city.trim().isEmpty() || country == null || country.trim().isEmpty()) {
                if (isDetectingLocation) {
                    Toast.makeText(CityActivity.this, "Detecting your location... Please wait a moment.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CityActivity.this, "Location detection failed. Tap on the location icon to retry turning on GPS.", Toast.LENGTH_LONG).show();
                    checkGpsSettingsAndFetchLocation();
                }
                return;
            }

            Intent i = new Intent(CityActivity.this, BirthdayActivity.class);
            i.putExtra("userName", userName);
            i.putExtra("gender", gender);
            i.putExtra("city", city);
            i.putExtra("country", country);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        requestLocationPermission();
    }

    private void startPulseAnimation(View v) {
        v.animate().scaleX(1.15f).scaleY(1.15f).alpha(0.8f).setDuration(800).withEndAction(() -> {
            v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(800).withEndAction(() -> {
                if (!isFinishing()) startPulseAnimation(v);
            }).start();
        }).start();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkGpsSettingsAndFetchLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                checkGpsSettingsAndFetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required to detect your city and country.", Toast.LENGTH_LONG).show();
                if (tvCity != null) tvCity.setText("Permission Required");
                if (tvCountry != null) tvCountry.setText("Tap to grant permission & detect");
            }
        }
    }

    /**
     * Checks if device GPS hardware is enabled via Google Location Settings API.
     * If GPS is OFF, automatically prompts system dialog: "To continue, turn on device location..."
     */
    private void checkGpsSettingsAndFetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        isDetectingLocation = true;
        if (tvCity != null) tvCity.setText("Detecting location...");
        if (tvCountry != null) tvCountry.setText("Please wait...");

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings satisfied — GPS is ON. Fetch current location!
            getLastKnownLocation();
        });

        task.addOnFailureListener(this, e -> {
            isDetectingLocation = false;
            if (e instanceof ResolvableApiException) {
                try {
                    // Show system popup dialog asking user to turn ON GPS automatically
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(CityActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e(TAG, "Error starting location settings resolution", sendEx);
                }
            } else {
                Toast.makeText(CityActivity.this, "GPS is required to detect your city.", Toast.LENGTH_LONG).show();
                if (tvCity != null) tvCity.setText("GPS Required");
                if (tvCountry != null) tvCountry.setText("Tap to enable GPS");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // User clicked "OK" on the system GPS dialog! Automatically fetch location.
                Log.d(TAG, "User enabled GPS via system dialog. Fetching location...");
                getLastKnownLocation();
            } else {
                isDetectingLocation = false;
                Toast.makeText(this, "Location/GPS is required to detect your city.", Toast.LENGTH_LONG).show();
                if (tvCity != null) tvCity.setText("GPS Turned Off");
                if (tvCountry != null) tvCountry.setText("Tap to enable GPS & retry");
            }
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isDetectingLocation = false;
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getCityAndCountry(location);
                    } else {
                        isDetectingLocation = false;
                        Toast.makeText(this, "Could not fetch GPS coordinates. Please tap to retry.", Toast.LENGTH_LONG).show();
                        if (tvCity != null) tvCity.setText("Location Not Found");
                        if (tvCountry != null) tvCountry.setText("Tap to retry detection");
                    }
                })
                .addOnFailureListener(this, e -> {
                    isDetectingLocation = false;
                    Log.e(TAG, "Failed to get location", e);
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (tvCity != null) tvCity.setText("Detection Error");
                    if (tvCountry != null) tvCountry.setText("Tap to retry");
                });
    }

    private void getCityAndCountry(Location location) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(CityActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                runOnUiThread(() -> {
                    isDetectingLocation = false;
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        city = address.getLocality();
                        if (city == null || city.trim().isEmpty()) {
                            city = address.getSubAdminArea();
                        }
                        if (city == null || city.trim().isEmpty()) {
                            city = address.getAdminArea();
                        }

                        country = address.getCountryName();

                        if (city != null && country != null) {
                            tvCity.setText(city);
                            tvCountry.setText(country);
                            Log.d(TAG, "✅ Location detected: " + city + ", " + country);
                        } else {
                            Toast.makeText(CityActivity.this, "Could not determine city name. Tap to retry.", Toast.LENGTH_SHORT).show();
                            tvCity.setText("Unknown Location");
                            tvCountry.setText("Tap to retry");
                        }
                    } else {
                        Toast.makeText(CityActivity.this, "Could not detect location. Tap to retry.", Toast.LENGTH_SHORT).show();
                        tvCity.setText("Unknown Location");
                        tvCountry.setText("Tap to retry");
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Geocoder Exception", e);
                runOnUiThread(() -> {
                    isDetectingLocation = false;
                    Toast.makeText(CityActivity.this, "Network error detecting city. Tap to retry.", Toast.LENGTH_SHORT).show();
                    tvCity.setText("Network Error");
                    tvCountry.setText("Tap to retry");
                });
            }
        }).start();
    }
}
