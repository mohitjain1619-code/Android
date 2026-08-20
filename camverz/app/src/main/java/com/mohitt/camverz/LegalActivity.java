package com.mohitt.camverz;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LegalActivity extends AppCompatActivity {

    private RadioGroup rgPolicySelector;
    private TextView tvPolicyContent;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        rgPolicySelector = findViewById(R.id.rg_policy_selector);
        tvPolicyContent = findViewById(R.id.tv_policy_content);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // Setup check change listener for RadioGroup
        rgPolicySelector.setOnCheckedChangeListener((group, checkedId) -> {
            updatePolicyContent(checkedId);
            updateTabStyles();
        });

        // Load Child Safety by default (pre-selected in XML)
        updatePolicyContent(R.id.rb_child_safety);
        updateTabStyles();
    }

    private void updatePolicyContent(int checkedId) {
        String htmlContent = "";
        if (checkedId == R.id.rb_child_safety) {
            htmlContent = LegalTexts.CHILD_SAFETY;
        } else if (checkedId == R.id.rb_privacy_policy) {
            htmlContent = LegalTexts.PRIVACY_POLICY;
        } else if (checkedId == R.id.rb_terms_conditions) {
            htmlContent = LegalTexts.TERMS_CONDITIONS;
        } else if (checkedId == R.id.rb_refund_policy) {
            htmlContent = LegalTexts.REFUND_POLICY;
        } else if (checkedId == R.id.rb_cookie_policy) {
            htmlContent = LegalTexts.COOKIE_POLICY;
        } else if (checkedId == R.id.rb_community_guidelines) {
            htmlContent = LegalTexts.COMMUNITY_GUIDELINES;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvPolicyContent.setText(Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvPolicyContent.setText(Html.fromHtml(htmlContent));
        }
    }

    private void updateTabStyles() {
        for (int i = 0; i < rgPolicySelector.getChildCount(); i++) {
            RadioButton rb = (RadioButton) rgPolicySelector.getChildAt(i);
            if (rb.isChecked()) {
                rb.setBackgroundResource(R.drawable.bg_community_hot_gradient);
                rb.setTextColor(Color.WHITE);
            } else {
                rb.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
                rb.setTextColor(Color.parseColor("#94A3B8"));
            }
        }
    }
}
