package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

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

        btnContinue.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    if (btnContinue.isEnabled()) v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (btnContinue.isEnabled()) v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });

        btnContinue.setOnClickListener(v -> {
            if (selectedGender.isEmpty()) {
                Toast.makeText(GenderSelectionActivity.this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(GenderSelectionActivity.this, CityActivity.class);
            i.putExtra("userName", userName);
            i.putExtra("gender", selectedGender);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void selectGender(String gender) {
        selectedGender = gender;
        btnContinue.setEnabled(true);
        btnContinue.animate().alpha(1.0f).setDuration(250).start();

        if ("male".equals(gender)) {
            cardMale.setBackgroundResource(R.drawable.bg_gender_card_selected);
            cardFemale.setBackgroundResource(R.drawable.bg_gender_card_unselected);
            cardMale.animate().scaleX(1.04f).scaleY(1.04f).translationY(-8f).alpha(1.0f).setDuration(200).start();
            cardFemale.animate().scaleX(0.96f).scaleY(0.96f).translationY(0f).alpha(0.6f).setDuration(200).start();
        } else {
            cardFemale.setBackgroundResource(R.drawable.bg_gender_card_selected);
            cardMale.setBackgroundResource(R.drawable.bg_gender_card_unselected);
            cardFemale.animate().scaleX(1.04f).scaleY(1.04f).translationY(-8f).alpha(1.0f).setDuration(200).start();
            cardMale.animate().scaleX(0.96f).scaleY(0.96f).translationY(0f).alpha(0.6f).setDuration(200).start();
        }
    }
}
