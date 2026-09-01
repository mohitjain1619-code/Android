package com.mohitt.camverz;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferenceSelectionDialog extends Dialog {

    private String selectedPreference = "";
    private final OnPreferenceSelectedListener listener;
    private final TokenManager tokenManager;
    private final ApiService apiService;

    public interface OnPreferenceSelectedListener {
        void onSelected(String preference);
    }

    public PreferenceSelectionDialog(@NonNull Context context, OnPreferenceSelectedListener listener) {
        super(context, R.style.CustomDialogTheme);
        this.listener = listener;
        this.tokenManager = TokenManager.getInstance(context);
        this.apiService = ApiClient.getInstance(context).getApi();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Transparent overlay window configurations
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_preference_selection, null);
        setContentView(view);
        setCancelable(false); // Force them to choose preference to complete onboarding

        MaterialCardView cardStraight = view.findViewById(R.id.card_straight);
        MaterialCardView cardGay = view.findViewById(R.id.card_gay);
        MaterialCardView cardLesbian = view.findViewById(R.id.card_lesbian);
        MaterialCardView cardTransgender = view.findViewById(R.id.card_transgender);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);

        // Restrict options based on gender
        String gender = tokenManager.getUserGender();
        if ("male".equalsIgnoreCase(gender)) {
            cardStraight.setVisibility(View.VISIBLE);
            cardGay.setVisibility(View.VISIBLE);
            cardLesbian.setVisibility(View.GONE);
            cardTransgender.setVisibility(View.GONE);
        } else if ("female".equalsIgnoreCase(gender)) {
            cardStraight.setVisibility(View.VISIBLE);
            cardGay.setVisibility(View.GONE);
            cardLesbian.setVisibility(View.VISIBLE);
            cardTransgender.setVisibility(View.GONE);
        } else {
            cardStraight.setVisibility(View.VISIBLE);
            cardGay.setVisibility(View.VISIBLE);
            cardLesbian.setVisibility(View.VISIBLE);
            cardTransgender.setVisibility(View.VISIBLE);
        }

        // Pre-select current preference if already configured
        String currentPreference = tokenManager.getSexPreference();
        if (!currentPreference.isEmpty()) {
            selectedPreference = currentPreference;
            btnSave.setEnabled(true);
            highlightSelected(cardStraight, cardGay, cardLesbian, cardTransgender);
        }

        cardStraight.setOnClickListener(v -> {
            selectedPreference = "Straight";
            btnSave.setEnabled(true);
            highlightSelected(cardStraight, cardGay, cardLesbian, cardTransgender);
        });

        cardGay.setOnClickListener(v -> {
            selectedPreference = "Gay";
            btnSave.setEnabled(true);
            highlightSelected(cardStraight, cardGay, cardLesbian, cardTransgender);
        });

        cardLesbian.setOnClickListener(v -> {
            selectedPreference = "Lesbian";
            btnSave.setEnabled(true);
            highlightSelected(cardStraight, cardGay, cardLesbian, cardTransgender);
        });

        cardTransgender.setOnClickListener(v -> {
            selectedPreference = "Transgender";
            btnSave.setEnabled(true);
            highlightSelected(cardStraight, cardGay, cardLesbian, cardTransgender);
        });

        btnSave.setOnClickListener(v -> {
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("sex_preference", selectedPreference);

            apiService.updateMe(updates).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Confirm Preference");
                    if (response.isSuccessful() && response.body() != null) {
                        tokenManager.setSexPreference(selectedPreference);
                        if (listener != null) {
                            listener.onSelected(selectedPreference);
                        }
                        dismiss();
                    } else {
                        Toast.makeText(getContext(), "Failed to save preference. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Confirm Preference");
                    Toast.makeText(getContext(), "Network error. Please check connection.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void highlightSelected(MaterialCardView straight, MaterialCardView gay, MaterialCardView lesbian, MaterialCardView transgender) {
        int selectedStrokeColor = Color.parseColor("#54D6D2");
        int defaultStrokeColor = Color.parseColor("#3A555A");
        int selectedBgColor = Color.parseColor("#1C3C3F");
        int defaultBgColor = Color.parseColor("#1A2B31");

        straight.setStrokeColor(selectedPreference.equals("Straight") ? selectedStrokeColor : defaultStrokeColor);
        straight.setCardBackgroundColor(selectedPreference.equals("Straight") ? selectedBgColor : defaultBgColor);

        gay.setStrokeColor(selectedPreference.equals("Gay") ? selectedStrokeColor : defaultStrokeColor);
        gay.setCardBackgroundColor(selectedPreference.equals("Gay") ? selectedBgColor : defaultBgColor);

        lesbian.setStrokeColor(selectedPreference.equals("Lesbian") ? selectedStrokeColor : defaultStrokeColor);
        lesbian.setCardBackgroundColor(selectedPreference.equals("Lesbian") ? selectedBgColor : defaultBgColor);

        transgender.setStrokeColor(selectedPreference.equals("Transgender") ? selectedStrokeColor : defaultStrokeColor);
        transgender.setCardBackgroundColor(selectedPreference.equals("Transgender") ? selectedBgColor : defaultBgColor);
    }
}
