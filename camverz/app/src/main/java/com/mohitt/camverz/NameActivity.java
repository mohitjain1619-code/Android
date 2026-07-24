package com.mohitt.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class NameActivity extends BaseActivity {

    EditText nameInput;
    Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        nameInput = findViewById(R.id.nameInput);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Enter your name");
                return;
            }

            Intent i = new Intent(NameActivity.this, GenderSelectionActivity.class);
            i.putExtra("userName", name);
            startActivity(i);
        });
    }
}
