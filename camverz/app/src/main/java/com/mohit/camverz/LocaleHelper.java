package com.mohit.camverz;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;

import java.util.Locale;

/**
 * Production-grade Locale Helper for dynamic runtime language switching.
 * Compatible with Android 7.0 (API 24) through Android 15/16.
 */
public class LocaleHelper {
    private static final String TAG = "LocaleHelper";
    private static final String PREFS_NAME = "camverz_language_prefs";
    private static final String KEY_SELECTED_LANGUAGE = "selected_language";
    private static final String KEY_LANGUAGE_SELECTED = "is_language_selected";
    public static final String DEFAULT_LANGUAGE = "en";

    public static boolean isLanguageSelected(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LANGUAGE_SELECTED, false);
    }

    public static String getLanguage(Context context) {
        if (context == null) return DEFAULT_LANGUAGE;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_LANGUAGE, DEFAULT_LANGUAGE);
    }

    public static Context setLocale(Context context, String languageCode) {
        if (context == null) return null;
        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = DEFAULT_LANGUAGE;
        }

        persistLanguage(context, languageCode);
        return applyLocale(context, languageCode);
    }

    private static void persistLanguage(Context context, String languageCode) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_SELECTED_LANGUAGE, languageCode)
                    .putBoolean(KEY_LANGUAGE_SELECTED, true)
                    .apply();
            Log.d(TAG, "Language saved to preferences: " + languageCode);
        } catch (Exception e) {
            Log.e(TAG, "Error persisting language", e);
        }
    }

    public static Context applyLocale(Context context) {
        if (context == null) return null;
        String language = getLanguage(context);
        return applyLocale(context, language);
    }

    public static Context applyLocale(Context context, String languageCode) {
        if (context == null) return null;
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
            configuration.setLayoutDirection(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            configuration.setLayoutDirection(locale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}
