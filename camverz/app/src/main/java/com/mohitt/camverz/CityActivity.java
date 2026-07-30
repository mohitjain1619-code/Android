package com.mohitt.camverz;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CityActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private Button btnContinue;
    private TextView backBtn, tvCity, tvCountry;
    private ImageView locationIcon;

    private String userName, gender, city, country;

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
            if (city == null || city.isEmpty() || country == null || country.isEmpty()) {
                city = "New York";
                country = "United States";
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
            getLastKnownLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                getLastKnownLocation();
            } else {
                Toast.makeText(this, "Location permission is required to detect your city and country.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getCityAndCountry(location);
                    } else {
                        Toast.makeText(this, "Location not found. Please turn on GPS and try again.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        }
    }

    private void getCityAndCountry(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                city = address.getLocality();
                country = address.getCountryName();

                tvCity.setText(city);
                tvCountry.setText(country);
            } else {
                Toast.makeText(this, "Could not detect location. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not detect location. Please check your network connection.", Toast.LENGTH_SHORT).show();
        }
    }
}
