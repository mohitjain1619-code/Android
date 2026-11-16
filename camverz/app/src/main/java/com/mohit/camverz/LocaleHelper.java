package com.mohit.camverz;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Production-grade Locale Helper for dynamic runtime language switching.
 * Compatible with Android 7.0 (API 24) through Android 15/16.
 * Seamlessly integrates AndroidX AppCompat per-app language APIs,
 * Android 13+ LocaleManager, and configuration context resolution.
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

    /**
     * Called in CamverzApp.onCreate() on app startup to sync the saved language
     * with AppCompatDelegate and the global Application resources.
     */
    public static void initAppLocale(Context context) {
        if (context == null) return;
        try {
            String languageCode = getLanguage(context);
            if (languageCode == null || languageCode.trim().isEmpty()) {
                languageCode = DEFAULT_LANGUAGE;
            }

            // Sync with AppCompatDelegate if empty
            LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
            if (appLocales.isEmpty()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
            }

            applyLocale(context, languageCode);
            Log.d(TAG, "🌐 App locale initialized to: " + languageCode);
        } catch (Throwable t) {
            Log.e(TAG, "Error initializing app locale in initAppLocale", t);
        }
    }

    /**
     * Changes and persists the active language across the entire application.
     * Updates SharedPreferences, AppCompatDelegate, and Resources configuration.
     */
    public static Context setLocale(Context context, String languageCode) {
        if (context == null) return null;
        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = DEFAULT_LANGUAGE;
        }

        // 1. Persist selection to SharedPreferences
        persistLanguage(context, languageCode);

        // 2. Official AndroidX AppCompat dynamic locale switch (syncs LocaleManager on Android 13+)
        try {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
        } catch (Throwable t) {
            Log.e(TAG, "Error calling AppCompatDelegate.setApplicationLocales", t);
        }

        // 3. Update resources & return wrapped configuration context
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
        try {
            String language = getLanguage(context);
            return applyLocale(context, language);
        } catch (Throwable t) {
            Log.e(TAG, "Error resolving locale in applyLocale(context)", t);
            return context;
        }
    }

    public static Context applyLocale(Context context, String languageCode) {
        if (context == null) return null;
        try {
            if (languageCode == null || languageCode.trim().isEmpty()) {
                languageCode = DEFAULT_LANGUAGE;
            }
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            Resources resources = context.getResources();
            if (resources == null) return context;

            Configuration configuration = new Configuration(resources.getConfiguration());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocale(locale);
                LocaleList localeList = new LocaleList(locale);
                LocaleList.setDefault(localeList);
                configuration.setLocales(localeList);
            } else {
                configuration.locale = locale;
            }
            configuration.setLayoutDirection(locale);

            // Directly update resources display metrics (vital for OEM ROMs & direct getResources() calls)
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());

            // Also update ApplicationContext resources if distinct
            if (context.getApplicationContext() != null && context.getApplicationContext() != context) {
                Resources appResources = context.getApplicationContext().getResources();
                if (appResources != null) {
                    appResources.updateConfiguration(configuration, appResources.getDisplayMetrics());
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Context configContext = context.createConfigurationContext(configuration);
                return configContext != null ? configContext : context;
            } else {
                return context;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error applying locale to configuration", t);
            return context;
        }
    }
}
