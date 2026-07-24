package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class GenderSelectionActivity extends BaseActivity {

    private LinearLayout cardMale, cardFemale;
    private Button btnContinue;
    private String selectedGender = "";
    private String userName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gender);

        userName = getIntent().getStringExtra("userName");

        cardMale = findViewById(R.id.cardMale);
        cardFemale = findViewById(R.id.cardFemale);
        btnContinue = findViewById(R.id.btnContinue);

        cardMale.setOnClickListener(v -> selectGender("male"));
        cardFemale.setOnClickListener(v -> selectGender("female"));

        btnContinue.setOnClickListener(v -> {
            if (selectedGender.isEmpty()) {
                Toast.makeText(GenderSelectionActivity.this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(GenderSelectionActivity.this, CityActivity.class);
            i.putExtra("userName", userName);
            i.putExtra("gender", selectedGender);
            startActivity(i);
        });
    }

    private void selectGender(String gender) {
        selectedGender = gender;
        btnContinue.setEnabled(true);
        btnContinue.setAlpha(1.0f);

        if (gender.equals("male")) {
            cardMale.setBackgroundResource(R.drawable.bg_gender_selection); 
            cardFemale.setBackgroundResource(R.drawable.bg_glass_item);
            cardMale.setAlpha(1.0f);
            cardFemale.setAlpha(0.6f);
        } else {
            cardFemale.setBackgroundResource(R.drawable.bg_gender_selection);
            cardMale.setBackgroundResource(R.drawable.bg_glass_item);
            cardFemale.setAlpha(1.0f);
            cardMale.setAlpha(0.6f);
        }
    }
}
