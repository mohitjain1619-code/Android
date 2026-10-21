package com.mohit.camverz.api;

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
    private static final String KEY_USER_PREFERENCE = "sex_preference";
    private static final String KEY_PLAN_IS_AD_FREE = "plan_is_ad_free";
    private static final String KEY_HAS_ACTIVE_PLAN = "has_active_plan";
    private static final String KEY_PLAN_NAME = "plan_name";
    private static final String KEY_FCM_TOKEN = "fcm_token";

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

    public void saveUserCity(String city) {
        prefs.edit().putString("user_city", city).apply();
    }

    public String getUserCity() {
        return prefs.getString("user_city", "");
    }

    public boolean isVerified() {
        return prefs.getBoolean(KEY_USER_VERIFIED, false);
    }

    public void setSexPreference(String preference) {
        prefs.edit().putString(KEY_USER_PREFERENCE, preference).apply();
    }

    public String getSexPreference() {
        return prefs.getString(KEY_USER_PREFERENCE, "");
    }

    public void savePlanIsAdFree(boolean isAdFree) {
        prefs.edit().putBoolean(KEY_PLAN_IS_AD_FREE, isAdFree).apply();
    }

    public boolean isPlanAdFree() {
        return prefs.getBoolean(KEY_PLAN_IS_AD_FREE, false);
    }

    public void saveHasActivePlan(boolean hasActivePlan) {
        prefs.edit().putBoolean(KEY_HAS_ACTIVE_PLAN, hasActivePlan).apply();
    }

    public boolean hasActivePlan() {
        return prefs.getBoolean(KEY_HAS_ACTIVE_PLAN, false);
    }

    public void savePlanName(String planName) {
        prefs.edit().putString(KEY_PLAN_NAME, planName).apply();
    }

    public String getPlanName() {
        return prefs.getString(KEY_PLAN_NAME, "");
    }

    public boolean hasVideoCallPlan() {
        return hasActivePlan() && !getPlanName().toLowerCase().contains("community");
    }

    public boolean isVideoCallAdFree() {
        return isPlanAdFree() && !getPlanName().toLowerCase().contains("community");
    }

    public boolean hasCommunityPlan() {
        return hasActivePlan() && getPlanName().toLowerCase().contains("community");
    }

    public boolean isCommunityAdFree() {
        return isPlanAdFree() && getPlanName().toLowerCase().contains("community");
    }

    public void setOnboardingComplete(boolean complete) {
        prefs.edit().putBoolean("is_onboarding_complete", complete).apply();
    }

    public boolean isOnboardingComplete() {
        if (prefs.getBoolean("is_onboarding_complete", false)) {
            return true;
        }
        String gender = getUserGender();
        String name = getUserName();
        return gender != null && !gender.trim().isEmpty() && name != null && !name.trim().isEmpty();
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

    // ============================================
    // FCM PUSH TOKEN MANAGEMENT
    // ============================================
    public void saveFcmToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            prefs.edit().putString(KEY_FCM_TOKEN, token.trim()).apply();
        }
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    public void syncFcmTokenToServer(Context context, String token) {
        if (token != null && !token.trim().isEmpty()) {
            saveFcmToken(token);
        }
        final String fcm = getFcmToken();
        if (fcm == null || fcm.isEmpty() || !isLoggedIn()) {
            return;
        }

        try {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("fcmToken", fcm);
            ApiClient.getInstance(context).getApi().updateFcmToken(body).enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                @Override
                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                    if (response.isSuccessful()) {
                        android.util.Log.d("TokenManager", "✅ FCM Token successfully synced to server");
                    } else {
                        android.util.Log.w("TokenManager", "Failed to sync FCM Token: HTTP " + response.code());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                    android.util.Log.e("TokenManager", "Error syncing FCM Token to server", t);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("TokenManager", "Exception in syncFcmTokenToServer", e);
        }
    }
}
