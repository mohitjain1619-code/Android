package com.mohitt.camverz;

import android.content.Context;
import android.content.SharedPreferences;

public class CallLimitManager {
    private static final String PREF_NAME = "camverz_call_limits";
    private static final String KEY_FREE_SECONDS_LEFT = "free_seconds_left";
    private static final String KEY_REWARDED_ADS_WATCHED = "rewarded_ads_watched";
    private static final String KEY_REWARDED_TIER = "rewarded_tier";
    private static final String KEY_LIMIT_BLOCKED_TIME = "limit_blocked_time";

    private static final long INITIAL_FREE_SECONDS = 300; // 5 minutes
    private static final long BLOCK_DURATION_MS = 10L * 60L * 60L * 1000L; // 10 hours

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static long getFreeSecondsLeft(Context context) {
        return getPrefs(context).getLong(KEY_FREE_SECONDS_LEFT, INITIAL_FREE_SECONDS);
    }

    public static void decrementFreeSeconds(Context context) {
        long seconds = getFreeSecondsLeft(context);
        if (seconds > 0) {
            getPrefs(context).edit().putLong(KEY_FREE_SECONDS_LEFT, seconds - 1).apply();
        }
    }

    public static void addFreeSeconds(Context context, long seconds) {
        long current = getFreeSecondsLeft(context);
        getPrefs(context).edit().putLong(KEY_FREE_SECONDS_LEFT, current + seconds).apply();
    }

    public static int getRewardedAdsWatched(Context context) {
        return getPrefs(context).getInt(KEY_REWARDED_ADS_WATCHED, 0);
    }

    public static void incrementRewardedAdsWatched(Context context) {
        int watched = getRewardedAdsWatched(context);
        getPrefs(context).edit().putInt(KEY_REWARDED_ADS_WATCHED, watched + 1).apply();
    }

    public static void resetRewardedAdsWatched(Context context) {
        getPrefs(context).edit().putInt(KEY_REWARDED_ADS_WATCHED, 0).apply();
    }

    public static int getRewardedTier(Context context) {
        return getPrefs(context).getInt(KEY_REWARDED_TIER, 1);
    }

    public static void setRewardedTier(Context context, int tier) {
        getPrefs(context).edit().putInt(KEY_REWARDED_TIER, tier).apply();
    }

    public static long getLimitBlockedTime(Context context) {
        return getPrefs(context).getLong(KEY_LIMIT_BLOCKED_TIME, 0);
    }

    public static void setLimitBlockedTime(Context context, long timestamp) {
        getPrefs(context).edit().putLong(KEY_LIMIT_BLOCKED_TIME, timestamp).apply();
    }

    public static boolean isBlocked(Context context) {
        long blockedTime = getLimitBlockedTime(context);
        if (blockedTime == 0) return false;

        long elapsed = System.currentTimeMillis() - blockedTime;
        if (elapsed >= BLOCK_DURATION_MS) {
            // 10 hours passed, auto reset limits!
            resetLimits(context);
            return false;
        }
        return true;
    }

    public static long getRemainingBlockTimeMs(Context context) {
        long blockedTime = getLimitBlockedTime(context);
        if (blockedTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - blockedTime;
        return Math.max(0, BLOCK_DURATION_MS - elapsed);
    }

    public static void resetLimits(Context context) {
        getPrefs(context).edit()
                .putLong(KEY_FREE_SECONDS_LEFT, INITIAL_FREE_SECONDS)
                .putInt(KEY_REWARDED_ADS_WATCHED, 0)
                .putInt(KEY_REWARDED_TIER, 1)
                .putLong(KEY_LIMIT_BLOCKED_TIME, 0)
                .apply();
    }
}
