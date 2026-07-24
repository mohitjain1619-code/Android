package com.mohitt.camverz.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

/**
 * Manages JWT token storage using EncryptedSharedPreferences.
 * Secure storage on device — token encrypted at rest.
 */
public class TokenManager {

    private static final String PREF_NAME = "camverz_auth";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_GENDER = "user_gender";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_VERIFIED = "user_verified";

    private static TokenManager instance;
    private SharedPreferences prefs;

    private TokenManager(Context context) {
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKey,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to regular SharedPreferences if encryption fails
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
        return instance;
    }

    // ============================================
    // TOKEN
    // ============================================
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    // ============================================
    // USER DATA (cached locally)
    // ============================================
    public void saveUser(String userId, String name, String email, String gender, String avatar, boolean verified) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_GENDER, gender)
                .putString(KEY_USER_AVATAR, avatar)
                .putBoolean(KEY_USER_VERIFIED, verified)
                .apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserGender() {
        return prefs.getString(KEY_USER_GENDER, "");
    }

    public String getUserAvatar() {
        return prefs.getString(KEY_USER_AVATAR, "");
    }

    public boolean isVerified() {
        return prefs.getBoolean(KEY_USER_VERIFIED, false);
    }

    // ============================================
    // CLEAR ALL
    // ============================================
    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return hasToken() && getUserId() != null;
    }
}
