package com.mohit.camverz;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LanguageSelectionActivity extends BaseActivity {

    private RecyclerView rvLanguages;
    private LanguageAdapter adapter;
    private Button btnContinue;
    private EditText etSearch;
    private boolean isFromSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        isFromSettings = getIntent().getBooleanExtra("is_from_settings", false);

        rvLanguages = findViewById(R.id.rv_languages);
        btnContinue = findViewById(R.id.btn_continue_language);
        etSearch = findViewById(R.id.et_search_language);

        rvLanguages.setLayoutManager(new LinearLayoutManager(this));

        List<LanguageModel> languages = getSupportedLanguages();
        String currentLang = LocaleHelper.getLanguage(this);

        adapter = new LanguageAdapter(languages, currentLang, model -> {
            // Updated item selection
        });
        rvLanguages.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                adapter.filter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnContinue.setOnClickListener(v -> {
            String selectedCode = adapter.getSelectedLanguageCode();
            if (selectedCode == null || selectedCode.isEmpty()) {
                selectedCode = LocaleHelper.DEFAULT_LANGUAGE;
            }

            // Save and apply new language preference
            LocaleHelper.setLocale(this, selectedCode);

            if (isFromSettings) {
                // Restart main screen with new locale applied everywhere
                Intent intent = new Intent(this, MainScreenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                // First launch: proceed to Google Sign In
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            finish();
        });
    }

    private List<LanguageModel> getSupportedLanguages() {
        List<LanguageModel> list = new ArrayList<>();
        list.add(new LanguageModel("en", "English", "English", "🇬🇧"));
        list.add(new LanguageModel("hi", "हिन्दी", "Hindi", "🇮🇳"));
        list.add(new LanguageModel("es", "Español", "Spanish", "🇪🇸"));
        list.add(new LanguageModel("fr", "Français", "French", "🇫🇷"));
        list.add(new LanguageModel("ar", "العربية", "Arabic", "🇸🇦"));
        list.add(new LanguageModel("de", "Deutsch", "German", "🇩🇪"));
        list.add(new LanguageModel("ru", "Русский", "Russian", "🇷🇺"));
        list.add(new LanguageModel("pt", "Português", "Portuguese", "🇧🇷"));
        list.add(new LanguageModel("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"));
        list.add(new LanguageModel("tr", "Türkçe", "Turkish", "🇹🇷"));
        return list;
    }
}
