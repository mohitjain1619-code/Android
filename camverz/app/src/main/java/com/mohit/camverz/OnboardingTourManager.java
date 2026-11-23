package com.mohit.camverz;

import android.content.Context;
import android.content.SharedPreferences;
import com.mohit.camverz.api.TokenManager;

/**
 * Production-Grade Manager for Mandatory New User Interactive Onboarding Tour.
 * Manages stages:
 * STAGE_FEED_POST (1) -> Guided to Feed & create first post (without ads)
 * STAGE_RETURN_HOME (2) -> Guided to tap Home button to return
 * STAGE_RANDOM_CALL (3) -> Guided to choose preference (Straight / Gay / Lesbian) for video call
 * STAGE_COMPLETED (4) -> Tour completed, regular app usage
 */
public class OnboardingTourManager {
    private static final String PREFS_NAME = "camverz_onboarding_tour_prefs";
    private static final String KEY_STAGE_PREFIX = "tour_stage_";
    private static final String KEY_COMPLETED_PREFIX = "tour_completed_";
    private static final String KEY_TEST_ALWAYS_ON = "tour_test_always_on";

    public static final int STAGE_FEED_POST = 1;
    public static final int STAGE_RETURN_HOME = 2;
    public static final int STAGE_RANDOM_CALL = 3;
    public static final int STAGE_COMPLETED = 4;

    private static volatile OnboardingTourManager instance;
    private final SharedPreferences prefs;

    private OnboardingTourManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static OnboardingTourManager getInstance(Context context) {
        if (instance == null) {
            synchronized (OnboardingTourManager.class) {
                if (instance == null) {
                    instance = new OnboardingTourManager(context);
                }
            }
        }
        return instance;
    }

    private String getUserId(Context context) {
        try {
            String uid = TokenManager.getInstance(context).getUserId();
            return (uid != null && !uid.trim().isEmpty()) ? uid.trim() : "default_user";
        } catch (Exception e) {
            return "default_user";
        }
    }

    public boolean isMohitTester(Context context) {
        try {
            String email = TokenManager.getInstance(context).getUserEmail();
            return email != null && (
                    "mohitjain1619@gmail.com".equalsIgnoreCase(email.trim()) ||
                    "mohitjain8120@gmail.com".equalsIgnoreCase(email.trim()) ||
                    "mohitj8120@gmail.com".equalsIgnoreCase(email.trim()) ||
                    "jainmohit8120@gmail.com".equalsIgnoreCase(email.trim())
            );
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTestModeAlwaysOn(Context context) {
        return prefs.getBoolean(KEY_TEST_ALWAYS_ON, false);
    }

    public void setTestModeAlwaysOn(Context context, boolean enabled) {
        prefs.edit().putBoolean(KEY_TEST_ALWAYS_ON, enabled).apply();
    }

    /**
     * Checks if the onboarding tour is currently pending for this user.
     */
    public boolean isTourPending(Context context) {
        String uid = getUserId(context);
        if (isTestModeAlwaysOn(context)) {
            return true;
        }
        return !prefs.getBoolean(KEY_COMPLETED_PREFIX + uid, false);
    }

    /**
     * Gets the current stage for this user.
     * Default is STAGE_FEED_POST (1) for new users.
     */
    public int getTourStage(Context context) {
        String uid = getUserId(context);
        if (prefs.getBoolean(KEY_COMPLETED_PREFIX + uid, false)) {
            if (isTestModeAlwaysOn(context)) {
                // If always-on test mode is explicitly turned ON, restart from beginning
                prefs.edit().remove(KEY_COMPLETED_PREFIX + uid).putInt(KEY_STAGE_PREFIX + uid, STAGE_FEED_POST).apply();
                return STAGE_FEED_POST;
            }
            return STAGE_COMPLETED;
        }
        return prefs.getInt(KEY_STAGE_PREFIX + uid, STAGE_FEED_POST);
    }

    /**
     * Sets the tour stage.
     */
    public void setTourStage(Context context, int stage) {
        String uid = getUserId(context);
        prefs.edit().putInt(KEY_STAGE_PREFIX + uid, stage).apply();
    }

    /**
     * Marks the tour fully completed.
     */
    public void markTourCompleted(Context context) {
        String uid = getUserId(context);
        prefs.edit()
                .putBoolean(KEY_COMPLETED_PREFIX + uid, true)
                .putInt(KEY_STAGE_PREFIX + uid, STAGE_COMPLETED)
                .apply();
    }

    /**
     * Helper to reset for testing if required.
     */
    public void resetTour(Context context) {
        String uid = getUserId(context);
        prefs.edit()
                .remove(KEY_COMPLETED_PREFIX + uid)
                .putInt(KEY_STAGE_PREFIX + uid, STAGE_FEED_POST)
                .apply();
    }

    /**
     * Checks whether ads should be bypassed for post upload.
     */
    public boolean shouldBypassAdsForPost(Context context) {
        return isTourPending(context) && getTourStage(context) <= STAGE_RETURN_HOME;
    }
}
