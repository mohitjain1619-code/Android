package com.mohit.camverz;

import android.content.Intent;
import android.os.Bundle;

public class GenderSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent forward = new Intent(this, OnboardingActivity.class);
        if (getIntent() != null && getIntent().getExtras() != null) {
            forward.putExtras(getIntent().getExtras());
        }
        startActivity(forward);
        finish();
    }
}

