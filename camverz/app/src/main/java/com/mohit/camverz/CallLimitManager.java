package com.mohit.camverz;

import android.content.Context;
import android.content.SharedPreferences;

public class CallLimitManager {
    private static final String PREF_NAME = "camverz_call_limits";
    private static final String KEY_FREE_SECONDS_LEFT = "free_seconds_left";
    private static final String KEY_REWARDED_ADS_WATCHED = "rewarded_ads_watched";
    private static final String KEY_REWARDED_TIER = "rewarded_tier";
    private static final String KEY_LIMIT_BLOCKED_TIME = "limit_blocked_time";
    private static final String KEY_BLOCK_LEVEL = "block_level";

    private static final long INITIAL_FREE_SECONDS = 300; // 5 minutes

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

    public static int getBlockLevel(Context context) {
        return getPrefs(context).getInt(KEY_BLOCK_LEVEL, 1);
    }

    public static void setBlockLevel(Context context, int level) {
        getPrefs(context).edit().putInt(KEY_BLOCK_LEVEL, level).apply();
    }

    public static long getLimitBlockedTime(Context context) {
        return getPrefs(context).getLong(KEY_LIMIT_BLOCKED_TIME, 0);
    }

    public static void setLimitBlockedTime(Context context, long timestamp) {
        getPrefs(context).edit().putLong(KEY_LIMIT_BLOCKED_TIME, timestamp).apply();
    }

    public static long getBlockDurationMs(Context context) {
        int level = getBlockLevel(context);
        switch (level) {
            case 1:
                return 30L * 60L * 1000L; // 30 minutes
            case 2:
            case 3:
                return 3L * 60L * 60L * 1000L; // 3 hours
            case 4:
            default:
                return 10L * 60L * 60L * 1000L; // 10 hours
        }
    }

    public static boolean isBlocked(Context context) {
        long blockedTime = getLimitBlockedTime(context);
        if (blockedTime == 0) return false;

        long elapsed = System.currentTimeMillis() - blockedTime;
        long duration = getBlockDurationMs(context);
        if (elapsed >= duration) {
            // Cooldown duration passed, advance block level for next cycle and auto reset limits!
            int currentLevel = getBlockLevel(context);
            int nextLevel = (currentLevel >= 4) ? 1 : currentLevel + 1;
            resetLimitsForNextSession(context, nextLevel);
            return false;
        }
        return true;
    }

    public static long getRemainingBlockTimeMs(Context context) {
        long blockedTime = getLimitBlockedTime(context);
        if (blockedTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - blockedTime;
        long duration = getBlockDurationMs(context);
        return Math.max(0, duration - elapsed);
    }

    public static String getFormattedRemainingBlockTime(Context context) {
        long ms = getRemainingBlockTimeMs(context);
        long totalSec = ms / 1000;
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;

        if (hours > 0) {
            return String.format(java.util.Locale.US, "%d hour(s) %d minute(s)", hours, minutes);
        } else if (minutes > 0) {
            return String.format(java.util.Locale.US, "%d minute(s)", minutes);
        } else {
            return String.format(java.util.Locale.US, "%d second(s)", seconds);
        }
    }

    public static void resetLimitsForNextSession(Context context, int nextBlockLevel) {
        getPrefs(context).edit()
                .putLong(KEY_FREE_SECONDS_LEFT, INITIAL_FREE_SECONDS)
                .putInt(KEY_REWARDED_ADS_WATCHED, 0)
                .putInt(KEY_REWARDED_TIER, 1)
                .putInt(KEY_BLOCK_LEVEL, nextBlockLevel)
                .putLong(KEY_LIMIT_BLOCKED_TIME, 0)
                .apply();
    }

    public static void resetLimits(Context context) {
        resetLimitsForNextSession(context, 1);
    }
}
