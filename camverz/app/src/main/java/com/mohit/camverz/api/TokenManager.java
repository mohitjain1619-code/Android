package com.mohit.camverz.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

/**
 * Manages JWT token storage using EncryptedSharedPreferences with bulletproof
 * fallback to standard SharedPreferences if Keystore or encryption fails.
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
    private static final String KEY_FEMALE_POST_PREF_CONFIRMED = "female_post_pref_confirmed";
    private static final String KEY_PLAN_IS_AD_FREE = "plan_is_ad_free";
    private static final String KEY_HAS_ACTIVE_PLAN = "has_active_plan";
    private static final String KEY_PLAN_NAME = "plan_name";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private static TokenManager instance;
    private SharedPreferences prefs;
    private final Context appContext;

    private TokenManager(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        initPreferences();
    }

    private synchronized void initPreferences() {
        if (appContext == null) return;
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKey,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            // Quick test read to verify KeyStore crypto is working
            prefs.getString(KEY_TOKEN, null);
        } catch (Throwable t) {
            Log.w("TokenManager", "EncryptedSharedPreferences failed, falling back to standard SharedPreferences", t);
            try {
                prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            } catch (Throwable t2) {
                prefs = appContext.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
            }
        }
    }

    private synchronized void recoverPreferences() {
        if (appContext == null) return;
        try {
            prefs = appContext.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        } catch (Throwable ignored) {}
    }

    private String safeGetString(String key, String defValue) {
        try {
            return prefs != null ? prefs.getString(key, defValue) : defValue;
        } catch (Throwable t) {
            Log.e("TokenManager", "Error reading pref " + key + ", falling back", t);
            recoverPreferences();
            try {
                return prefs != null ? prefs.getString(key, defValue) : defValue;
            } catch (Throwable ignored) {
                return defValue;
            }
        }
    }

    private boolean safeGetBoolean(String key, boolean defValue) {
        try {
            return prefs != null ? prefs.getBoolean(key, defValue) : defValue;
        } catch (Throwable t) {
            Log.e("TokenManager", "Error reading pref " + key + ", falling back", t);
            recoverPreferences();
            try {
                return prefs != null ? prefs.getBoolean(key, defValue) : defValue;
            } catch (Throwable ignored) {
                return defValue;
            }
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
        try {
            if (prefs != null) prefs.edit().putString(KEY_TOKEN, token).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putString(KEY_TOKEN, token).apply();
        }
    }

    public String getToken() {
        return safeGetString(KEY_TOKEN, null);
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    public void clearToken() {
        try {
            if (prefs != null) prefs.edit().remove(KEY_TOKEN).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().remove(KEY_TOKEN).apply();
        }
    }

    // ============================================
    // USER DATA (cached locally)
    // ============================================
    public void saveUser(String userId, String name, String email, String gender, String avatar, boolean verified) {
        try {
            if (prefs != null) {
                prefs.edit()
                        .putString(KEY_USER_ID, userId)
                        .putString(KEY_USER_NAME, name)
                        .putString(KEY_USER_EMAIL, email)
                        .putString(KEY_USER_GENDER, gender)
                        .putString(KEY_USER_AVATAR, avatar)
                        .putBoolean(KEY_USER_VERIFIED, verified)
                        .apply();
            }
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) {
                prefs.edit()
                        .putString(KEY_USER_ID, userId)
                        .putString(KEY_USER_NAME, name)
                        .putString(KEY_USER_EMAIL, email)
                        .putString(KEY_USER_GENDER, gender)
                        .putString(KEY_USER_AVATAR, avatar)
                        .putBoolean(KEY_USER_VERIFIED, verified)
                        .apply();
            }
        }
    }

    public String getUserId() {
        return safeGetString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return safeGetString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return safeGetString(KEY_USER_EMAIL, "");
    }

    public String getUserGender() {
        return safeGetString(KEY_USER_GENDER, "");
    }

    public String getGender() {
        return getUserGender();
    }

    public String getUserAvatar() {
        return safeGetString(KEY_USER_AVATAR, "");
    }

    public void saveUserCity(String city) {
        try {
            if (prefs != null) prefs.edit().putString("user_city", city).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putString("user_city", city).apply();
        }
    }

    public String getUserCity() {
        return safeGetString("user_city", "");
    }

    public boolean isVerified() {
        return safeGetBoolean(KEY_USER_VERIFIED, false);
    }

    public void setVerified(boolean verified) {
        try {
            if (prefs != null) prefs.edit().putBoolean(KEY_USER_VERIFIED, verified).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putBoolean(KEY_USER_VERIFIED, verified).apply();
        }
    }

    public void setSexPreference(String preference) {
        try {
            if (prefs != null) prefs.edit().putString(KEY_USER_PREFERENCE, preference).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putString(KEY_USER_PREFERENCE, preference).apply();
        }
    }

    public String getSexPreference() {
        return safeGetString(KEY_USER_PREFERENCE, "");
    }

    public boolean isFemalePostPrefConfirmed() {
        return safeGetBoolean(KEY_FEMALE_POST_PREF_CONFIRMED, false);
    }

    public void setFemalePostPrefConfirmed(boolean confirmed) {
        try {
            if (prefs != null) prefs.edit().putBoolean(KEY_FEMALE_POST_PREF_CONFIRMED, confirmed).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putBoolean(KEY_FEMALE_POST_PREF_CONFIRMED, confirmed).apply();
        }
    }

    public void savePlanIsAdFree(boolean isAdFree) {
        try {
            if (prefs != null) prefs.edit().putBoolean(KEY_PLAN_IS_AD_FREE, isAdFree).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putBoolean(KEY_PLAN_IS_AD_FREE, isAdFree).apply();
        }
    }

    public boolean isPlanAdFree() {
        return safeGetBoolean(KEY_PLAN_IS_AD_FREE, false);
    }

    public void saveHasActivePlan(boolean hasActivePlan) {
        try {
            if (prefs != null) prefs.edit().putBoolean(KEY_HAS_ACTIVE_PLAN, hasActivePlan).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putBoolean(KEY_HAS_ACTIVE_PLAN, hasActivePlan).apply();
        }
    }

    public boolean hasActivePlan() {
        return safeGetBoolean(KEY_HAS_ACTIVE_PLAN, false);
    }

    public void savePlanName(String planName) {
        try {
            if (prefs != null) prefs.edit().putString(KEY_PLAN_NAME, planName).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putString(KEY_PLAN_NAME, planName).apply();
        }
    }

    public String getPlanName() {
        return safeGetString(KEY_PLAN_NAME, "");
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
        try {
            if (prefs != null) prefs.edit().putBoolean("is_onboarding_complete", complete).apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().putBoolean("is_onboarding_complete", complete).apply();
        }
    }

    public boolean isOnboardingComplete() {
        try {
            if (prefs != null && prefs.contains("is_onboarding_complete")) {
                return safeGetBoolean("is_onboarding_complete", false);
            }
        } catch (Throwable ignored) {}
        String gender = getUserGender();
        String name = getUserName();
        return gender != null && !gender.trim().isEmpty() && name != null && !name.trim().isEmpty();
    }

    // ============================================
    // CLEAR ALL
    // ============================================
    public void clearAll() {
        try {
            if (prefs != null) prefs.edit().clear().apply();
        } catch (Throwable t) {
            recoverPreferences();
            if (prefs != null) prefs.edit().clear().apply();
        }
    }

    public boolean isLoggedIn() {
        return hasToken() && getUserId() != null;
    }

    // ============================================
    // FCM PUSH TOKEN MANAGEMENT
    // ============================================
    public void saveFcmToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            try {
                if (prefs != null) prefs.edit().putString(KEY_FCM_TOKEN, token.trim()).apply();
            } catch (Throwable t) {
                recoverPreferences();
                if (prefs != null) prefs.edit().putString(KEY_FCM_TOKEN, token.trim()).apply();
            }
        }
    }

    public String getFcmToken() {
        return safeGetString(KEY_FCM_TOKEN, null);
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
        } catch (Throwable e) {
            android.util.Log.e("TokenManager", "Exception in syncFcmTokenToServer", e);
        }
    }
}
