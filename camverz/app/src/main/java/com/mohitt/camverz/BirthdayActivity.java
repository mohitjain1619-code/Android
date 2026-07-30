package com.mohitt.camverz;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Calendar;

public class BirthdayActivity extends BaseActivity {

    private EditText birthdayInput;
    private Button btnContinue;
    private TextView backBtn;
    private String userName, gender, city, dob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday);

        birthdayInput = findViewById(R.id.birthdayInput);
        btnContinue = findViewById(R.id.btnContinueBirthday);
        backBtn = findViewById(R.id.backBtnBirthday);

        userName = getIntent().getStringExtra("userName");
        gender = getIntent().getStringExtra("gender");
        city = getIntent().getStringExtra("city");

        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        birthdayInput.setOnClickListener(v -> openDatePicker());

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
            if (dob == null || dob.isEmpty()) {
                birthdayInput.setError("Select your birth date");
                return;
            }
            Intent i = new Intent(BirthdayActivity.this, FinishActivity.class);
            i.putExtra("userName", userName);
            i.putExtra("gender", gender);
            i.putExtra("city", city);
            i.putExtra("dob", dob);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    if (isUserAdult(year, month, day)) {
                        dob = day + "/" + (month + 1) + "/" + year;
                        birthdayInput.setText(dob);
                        birthdayInput.setError(null);
                    } else {
                        showAgeErrorDialog();
                        birthdayInput.setText("");
                        dob = "";
                    }
                },
                cal.get(Calendar.YEAR) - 18,
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private boolean isUserAdult(int year, int month, int day) {
        Calendar today = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();
        dob.set(year, month, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age >= 18;
    }

    private void showAgeErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_age_error, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnUnderstood = dialogView.findViewById(R.id.btnUnderstood);
        if (btnUnderstood != null) {
            btnUnderstood.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
