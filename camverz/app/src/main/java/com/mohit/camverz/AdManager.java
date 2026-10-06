package com.mohit.camverz;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;

/**
 * Centralized, production-grade AdManager for Camverz.
 * 
 * Key Features & Solved Problems:
 * 1. Single source of truth for LevelPlay listeners (eliminates listener overwrite bugs).
 * 2. Intelligent debounced preloading with failure backoff (stops request spamming that killed fill rate).
 * 3. Seamless cross-format fallback: Interstitial <-> Rewarded Video with smart 2.5s buffering.
 * 4. Automatic Google Mobile Ads (AdMob) SDK initialization for LevelPlay bidding mediation.
 * 5. 100% reliable tracking via AdAnalyticsTracker and persistent ad-watch-pending enforcement.
 */
public class AdManager {
    private static final String TAG = "AdManager";
    public static final String IRONSOURCE_APP_KEY = "28890f0b5";
    public static final String LEVELPLAY_INTERSTITIAL_ID = "bspxbvo7aw4r8ybv";
    public static final String LEVELPLAY_REWARDED_ID = "inn2ugrw04ep2tzc";
    public static final String LEVELPLAY_NATIVE_ID = "6bpplx6inl1ahz01";
    public static final String PREFS_AD_TIMER = "camverz_ad_timer";
    public static final String KEY_LAST_INTERSTITIAL_TIME = "last_interstitial_time";
    public static final String KEY_AD_WATCH_PENDING = "ad_watch_pending";

    private static volatile AdManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isInitialized = false;
    private boolean isInterstitialLoading = false;
    private long lastInterstitialRequestTime = 0;
    private long lastInterstitialFailTime = 0;
    private Context appContext;

    // Callbacks
    public interface AdCallback {
        void onAdDismissed(boolean success);
    }

    public interface RewardedAdCallback {
        void onRewardEarned();
        void onAdDismissed(boolean rewarded);
        void onAdFailed(String reason);
    }

    private AdCallback currentInterstitialCallback;
    private RewardedAdCallback currentRewardedCallback;
    private boolean isRewardedFromInterstitial = false;
    private boolean rewardGrantedInCurrentSession = false;
    private String currentPlacement = "default";

    private AdManager() {}

    public static AdManager getInstance() {
        if (instance == null) {
            synchronized (AdManager.class) {
                if (instance == null) {
                    instance = new AdManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes Google Mobile Ads (for bidding signals) and ironSource LevelPlay SDK.
     * Registers singleton static listeners once for the entire application lifetime.
     */
    public synchronized void init(Activity activity) {
        if (activity == null) return;
        this.appContext = activity.getApplicationContext();

        if (isInitialized) return;

        // 1. Initialize Google Mobile Ads SDK so AdMob bidding adapter can participate in auctions
        try {
            com.google.android.gms.ads.MobileAds.initialize(appContext, initializationStatus -> {
                Log.d(TAG, "✅ Google Mobile Ads (AdMob) Initialized for LevelPlay Mediation");
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MobileAds for LevelPlay", e);
        }

        // 2. Initialize ironSource LevelPlay SDK
        try {
            IronSource.init(activity, IRONSOURCE_APP_KEY);
            IronSource.setLevelPlayInterstitialListener(interstitialListener);
            IronSource.setLevelPlayRewardedVideoListener(rewardedVideoListener);
            isInitialized = true;
            Log.d(TAG, "✅ ironSource LevelPlay Mediation SDK & Listeners Initialized Globally");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing IronSource SDK", e);
        }
    }

    public void onResume(Activity activity) {
        if (activity != null) {
            try {
                IronSource.onResume(activity);
            } catch (Exception e) {
                Log.e(TAG, "Error in IronSource.onResume", e);
            }
        }
    }

    public void onPause(Activity activity) {
        if (activity != null) {
            try {
                IronSource.onPause(activity);
            } catch (Exception e) {
                Log.e(TAG, "Error in IronSource.onPause", e);
            }
        }
    }

    /**
     * Preloads an interstitial ad with debouncing, in-flight checks, and cooldown backoff.
     * Prevents network flooding and mediation provider penalties.
     */
    public void preloadInterstitial(Context context) {
        if (context == null) context = appContext;
        if (context == null) return;
        this.appContext = context.getApplicationContext();

        long now = System.currentTimeMillis();

        // 1. If ad is already loaded and ready in memory, do not send duplicate request!
        if (IronSource.isInterstitialReady()) {
            Log.d(TAG, "Interstitial already cached and ready. Skipping duplicate load request.");
            return;
        }

        // 2. If an ad request is currently pending/in-flight, do not spam!
        if (isInterstitialLoading) {
            Log.d(TAG, "Interstitial request already in-flight. Skipping duplicate load request.");
            return;
        }

        // 3. Backoff if last load failed recently (within 15 seconds)
        if (now - lastInterstitialFailTime < 15000) {
            Log.d(TAG, "Interstitial failed recently. Cooldown active (skipping request).");
            return;
        }

        // 4. Rate-limit requests: minimum 8 seconds between consecutive calls
        if (now - lastInterstitialRequestTime < 8000) {
            Log.d(TAG, "Interstitial request throttled (<8s interval).");
            return;
        }

        isInterstitialLoading = true;
        lastInterstitialRequestTime = now;
        Log.d(TAG, "🚀 Dispatching LevelPlay Interstitial load request...");
        AdAnalyticsTracker.trackEvent(appContext, "REQUEST", "interstitial", "ironsource", "REQUESTED", "", "");
        IronSource.loadInterstitial();
    }

    /**
     * Shows Interstitial with intelligent fallback to Rewarded Video and 2.5s buffering.
     * Guarantees that callback will ALWAYS fire so the user is never blocked.
     */
    public void showInterstitialWithFallback(Activity activity, String placement, boolean isAdFree, AdCallback callback) {
        if (isAdFree) {
            Log.d(TAG, "User has ad-free pass for " + placement + ". Skipping ad.");
            if (callback != null) callback.onAdDismissed(true);
            return;
        }

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) callback.onAdDismissed(false);
            return;
        }

        final String finalPlacement = (placement != null && !placement.isEmpty()) ? placement : LEVELPLAY_INTERSTITIAL_ID;
        AdAnalyticsTracker.trackEvent(activity, "REQUEST", "interstitial", "ironsource", "REQUESTED", "", "");

        // PRIORITY 1: Interstitial Ad is ready right now
        if (IronSource.isInterstitialReady()) {
            Log.d(TAG, "Showing LevelPlay Interstitial (" + finalPlacement + ")");
            this.currentInterstitialCallback = callback;
            this.isRewardedFromInterstitial = false;
            this.currentPlacement = finalPlacement;
            BaseActivity.isAdShowing = true;
            WindowBackBlocker.blockBack(activity);
            setAdWatchPending(true);
            AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "interstitial", "ironsource", "DELIVERED", "", "");
            IronSource.showInterstitial(finalPlacement);
            return;
        }

        // PRIORITY 2: Fallback to Rewarded Video if available
        if (IronSource.isRewardedVideoAvailable()) {
            Log.d(TAG, "Interstitial not ready, falling back to Rewarded Video (" + finalPlacement + ")");
            this.currentInterstitialCallback = callback;
            this.isRewardedFromInterstitial = false;
            this.currentPlacement = finalPlacement;
            BaseActivity.isAdShowing = true;
            WindowBackBlocker.blockBack(activity);
            setAdWatchPending(true);
            AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "rewarded", "ironsource", "DELIVERED", "", "");
            IronSource.showRewardedVideo(finalPlacement);
            return;
        }

        // PRIORITY 3: Buffer for up to 2.5 seconds with ProgressDialog so we don't drop the impression
        Log.d(TAG, "Neither ad ready immediately. Starting 2.5s buffering for: " + finalPlacement);
        preloadInterstitial(activity);
        BaseActivity.isAdShowing = true;
        WindowBackBlocker.blockBack(activity);

        ProgressDialog progressDialog = null;
        try {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("Loading ad...");
            progressDialog.setCancelable(false);
            if (progressDialog.getWindow() != null) {
                WindowBackBlocker.blockBack(progressDialog.getWindow());
            }
            progressDialog.show();
        } catch (Exception ignored) {}

        final ProgressDialog finalDialog = progressDialog;
        final long startTime = System.currentTimeMillis();
        final Runnable[] bufferRunnable = new Runnable[1];

        bufferRunnable[0] = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    safeDismiss(finalDialog);
                    BaseActivity.isAdShowing = false;
                    if (callback != null) callback.onAdDismissed(false);
                    return;
                }

                if (IronSource.isInterstitialReady()) {
                    safeDismiss(finalDialog);
                    Log.d(TAG, "Interstitial loaded during buffer! Showing ad.");
                    currentInterstitialCallback = callback;
                    isRewardedFromInterstitial = false;
                    currentPlacement = finalPlacement;
                    BaseActivity.isAdShowing = true;
                    WindowBackBlocker.blockBack(activity);
                    setAdWatchPending(true);
                    AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "interstitial", "ironsource", "DELIVERED", "", "");
                    IronSource.showInterstitial(finalPlacement);
                } else if (IronSource.isRewardedVideoAvailable()) {
                    safeDismiss(finalDialog);
                    Log.d(TAG, "Rewarded ad loaded during buffer! Showing fallback ad.");
                    currentInterstitialCallback = callback;
                    isRewardedFromInterstitial = false;
                    currentPlacement = finalPlacement;
                    BaseActivity.isAdShowing = true;
                    WindowBackBlocker.blockBack(activity);
                    setAdWatchPending(true);
                    AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "rewarded", "ironsource", "DELIVERED", "", "");
                    IronSource.showRewardedVideo(finalPlacement);
                } else if (System.currentTimeMillis() - startTime < 2500) {
                    mainHandler.postDelayed(bufferRunnable[0], 300);
                } else {
                    safeDismiss(finalDialog);
                    BaseActivity.isAdShowing = false;
                    Log.w(TAG, "Ad buffer timeout reached for: " + finalPlacement + " (Proceeding without ad)");
                    setAdWatchPending(false);
                    AdAnalyticsTracker.trackEvent(activity, "FAILED", "interstitial", "ironsource", "FAILED", "NO_FILL", "Ad unavailable after 2.5s buffer");
                    if (callback != null) callback.onAdDismissed(false);
                    preloadInterstitial(activity);
                }
            }
        };

        mainHandler.postDelayed(bufferRunnable[0], 300);
    }

    /**
     * Shows a Rewarded Video ad with fallback to Interstitial and 3.0s buffering.
     */
    public void showRewardedAd(Activity activity, String placement, RewardedAdCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) callback.onAdFailed("ACTIVITY_DESTROYED");
            return;
        }

        final String finalPlacement = (placement != null && !placement.isEmpty()) ? placement : LEVELPLAY_REWARDED_ID;
        AdAnalyticsTracker.trackEvent(activity, "REQUEST", "rewarded", "ironsource", "REQUESTED", "", "");

        // PRIORITY 1: Rewarded Video is ready
        if (IronSource.isRewardedVideoAvailable()) {
            Log.d(TAG, "Showing LevelPlay Rewarded Video (" + finalPlacement + ")");
            this.currentRewardedCallback = callback;
            this.isRewardedFromInterstitial = false;
            this.rewardGrantedInCurrentSession = false;
            this.currentPlacement = finalPlacement;
            BaseActivity.isAdShowing = true;
            WindowBackBlocker.blockBack(activity);
            setAdWatchPending(true);
            AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "rewarded", "ironsource", "DELIVERED", "", "");
            IronSource.showRewardedVideo(finalPlacement);
            return;
        }

        // PRIORITY 2: Fallback to Interstitial if ready (grant reward on completion)
        if (IronSource.isInterstitialReady()) {
            Log.d(TAG, "Rewarded video not ready, fallback to Interstitial for reward (" + finalPlacement + ")");
            this.currentRewardedCallback = callback;
            this.isRewardedFromInterstitial = true;
            this.rewardGrantedInCurrentSession = false;
            this.currentPlacement = finalPlacement;
            BaseActivity.isAdShowing = true;
            WindowBackBlocker.blockBack(activity);
            setAdWatchPending(true);
            AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "interstitial", "ironsource", "DELIVERED", "", "");
            IronSource.showInterstitial(finalPlacement);
            return;
        }

        // PRIORITY 3: Buffer for up to 3 seconds with ProgressDialog
        Log.d(TAG, "Neither ad ready for rewarded action. Buffering for up to 3s (" + finalPlacement + ")");
        preloadInterstitial(activity);
        BaseActivity.isAdShowing = true;
        WindowBackBlocker.blockBack(activity);

        ProgressDialog progressDialog = null;
        try {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("Loading ad...");
            progressDialog.setCancelable(false);
            if (progressDialog.getWindow() != null) {
                WindowBackBlocker.blockBack(progressDialog.getWindow());
            }
            progressDialog.show();
        } catch (Exception ignored) {}

        final ProgressDialog finalDialog = progressDialog;
        final long startTime = System.currentTimeMillis();
        final Runnable[] bufferRunnable = new Runnable[1];

        bufferRunnable[0] = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    safeDismiss(finalDialog);
                    BaseActivity.isAdShowing = false;
                    if (callback != null) callback.onAdFailed("ACTIVITY_DESTROYED");
                    return;
                }

                if (IronSource.isRewardedVideoAvailable()) {
                    safeDismiss(finalDialog);
                    currentRewardedCallback = callback;
                    isRewardedFromInterstitial = false;
                    rewardGrantedInCurrentSession = false;
                    currentPlacement = finalPlacement;
                    BaseActivity.isAdShowing = true;
                    WindowBackBlocker.blockBack(activity);
                    setAdWatchPending(true);
                    AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "rewarded", "ironsource", "DELIVERED", "", "");
                    IronSource.showRewardedVideo(finalPlacement);
                } else if (IronSource.isInterstitialReady()) {
                    safeDismiss(finalDialog);
                    currentRewardedCallback = callback;
                    isRewardedFromInterstitial = true;
                    rewardGrantedInCurrentSession = false;
                    currentPlacement = finalPlacement;
                    BaseActivity.isAdShowing = true;
                    WindowBackBlocker.blockBack(activity);
                    setAdWatchPending(true);
                    AdAnalyticsTracker.trackEvent(activity, "IMPRESSION", "interstitial", "ironsource", "DELIVERED", "", "");
                    IronSource.showInterstitial(finalPlacement);
                } else if (System.currentTimeMillis() - startTime < 3000) {
                    mainHandler.postDelayed(bufferRunnable[0], 350);
                } else {
                    safeDismiss(finalDialog);
                    BaseActivity.isAdShowing = false;
                    setAdWatchPending(false);
                    AdAnalyticsTracker.trackEvent(activity, "FAILED", "rewarded", "ironsource", "FAILED", "NO_FILL", "Ad unavailable after 3s buffering timeout");
                    if (callback != null) callback.onAdFailed("NO_FILL");
                    preloadInterstitial(activity);
                }
            }
        };

        mainHandler.postDelayed(bufferRunnable[0], 350);
    }

    public void setAdWatchPending(boolean pending) {
        if (appContext == null) return;
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_AD_TIMER, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_AD_WATCH_PENDING, pending).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting ad_watch_pending", e);
        }
    }

    public boolean isAdWatchPending(Context context) {
        if (context == null) context = appContext;
        if (context == null) return false;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_AD_TIMER, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_AD_WATCH_PENDING, false);
        } catch (Exception e) {
            return false;
        }
    }

    public void setLastInterstitialTime(long timeMs) {
        if (appContext == null) return;
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_AD_TIMER, Context.MODE_PRIVATE);
            prefs.edit().putLong(KEY_LAST_INTERSTITIAL_TIME, timeMs).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting last_interstitial_time", e);
        }
    }

    public long getLastInterstitialTime(Context context) {
        if (context == null) context = appContext;
        if (context == null) return 0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_AD_TIMER, Context.MODE_PRIVATE);
            return prefs.getLong(KEY_LAST_INTERSTITIAL_TIME, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private void safeDismiss(ProgressDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // PERMANENT LEVELPLAY LISTENERS
    // ==========================================

    private final LevelPlayInterstitialListener interstitialListener = new LevelPlayInterstitialListener() {
        @Override
        public void onAdReady(AdInfo adInfo) {
            isInterstitialLoading = false;
            Log.d(TAG, "✅ LevelPlay Interstitial ready (Network: " + (adInfo != null ? adInfo.getAdNetwork() : "unknown") + ")");
        }

        @Override
        public void onAdLoadFailed(IronSourceError error) {
            isInterstitialLoading = false;
            lastInterstitialFailTime = System.currentTimeMillis();
            String errMsg = error != null ? error.getErrorMessage() : "Load failed";
            int errCode = error != null ? error.getErrorCode() : -1;
            Log.w(TAG, "❌ LevelPlay Interstitial load failed (" + errCode + "): " + errMsg);
            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "FAILED", "interstitial", "ironsource", "FAILED", String.valueOf(errCode), errMsg);
            }
        }

        @Override
        public void onAdOpened(AdInfo adInfo) {
            BaseActivity.isAdShowing = true;
            setAdWatchPending(true);
            Log.d(TAG, "▶️ LevelPlay Interstitial opened");
        }

        @Override
        public void onAdShowSucceeded(AdInfo adInfo) {
            Log.d(TAG, "LevelPlay Interstitial show succeeded");
        }

        @Override
        public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
            BaseActivity.isAdShowing = false;
            setAdWatchPending(false);
            String errMsg = error != null ? error.getErrorMessage() : "Show failed";
            int errCode = error != null ? error.getErrorCode() : -1;
            Log.e(TAG, "❌ LevelPlay Interstitial show failed (" + errCode + "): " + errMsg);

            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "FAILED", "interstitial", "ironsource", "FAILED", String.valueOf(errCode), errMsg);
            }

            if (isRewardedFromInterstitial) {
                RewardedAdCallback rCb = currentRewardedCallback;
                currentRewardedCallback = null;
                isRewardedFromInterstitial = false;
                if (rCb != null) rCb.onAdFailed(errMsg);
            } else {
                AdCallback cb = currentInterstitialCallback;
                currentInterstitialCallback = null;
                if (cb != null) cb.onAdDismissed(false);
            }

            mainHandler.postDelayed(() -> preloadInterstitial(appContext), 2000);
        }

        @Override
        public void onAdClicked(AdInfo adInfo) {
            Log.d(TAG, "LevelPlay Interstitial clicked");
            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "CLICKED", "interstitial", "ironsource", "DELIVERED", "", "");
            }
        }

        @Override
        public void onAdClosed(AdInfo adInfo) {
            BaseActivity.isAdShowing = false;
            setAdWatchPending(false);
            setLastInterstitialTime(System.currentTimeMillis());
            Log.d(TAG, "⏹️ LevelPlay Interstitial closed");

            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "CLOSED", "interstitial", "ironsource", "DELIVERED", "", "");
            }

            if (isRewardedFromInterstitial) {
                RewardedAdCallback rCb = currentRewardedCallback;
                currentRewardedCallback = null;
                isRewardedFromInterstitial = false;
                if (rCb != null) {
                    rCb.onRewardEarned();
                    rCb.onAdDismissed(true);
                }
            } else {
                AdCallback cb = currentInterstitialCallback;
                currentInterstitialCallback = null;
                if (cb != null) cb.onAdDismissed(true);
            }

            mainHandler.postDelayed(() -> preloadInterstitial(appContext), 2000);
        }
    };

    private final LevelPlayRewardedVideoListener rewardedVideoListener = new LevelPlayRewardedVideoListener() {
        @Override
        public void onAdAvailable(AdInfo adInfo) {
            Log.d(TAG, "✅ LevelPlay Rewarded Video available (Network: " + (adInfo != null ? adInfo.getAdNetwork() : "unknown") + ")");
        }

        @Override
        public void onAdUnavailable() {
            Log.d(TAG, "LevelPlay Rewarded Video unavailable");
        }

        @Override
        public void onAdOpened(AdInfo adInfo) {
            BaseActivity.isAdShowing = true;
            rewardGrantedInCurrentSession = false;
            setAdWatchPending(true);
            Log.d(TAG, "▶️ LevelPlay Rewarded Video opened");
        }

        @Override
        public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
            BaseActivity.isAdShowing = false;
            setAdWatchPending(false);
            String errMsg = error != null ? error.getErrorMessage() : "Show failed";
            int errCode = error != null ? error.getErrorCode() : -1;
            Log.e(TAG, "❌ LevelPlay Rewarded Video show failed (" + errCode + "): " + errMsg);

            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "FAILED", "rewarded", "ironsource", "FAILED", String.valueOf(errCode), errMsg);
            }

            if (currentInterstitialCallback != null) {
                AdCallback cb = currentInterstitialCallback;
                currentInterstitialCallback = null;
                cb.onAdDismissed(false);
            }

            if (currentRewardedCallback != null) {
                RewardedAdCallback rCb = currentRewardedCallback;
                currentRewardedCallback = null;
                rCb.onAdFailed(errMsg);
            }

            mainHandler.postDelayed(() -> preloadInterstitial(appContext), 2000);
        }

        @Override
        public void onAdClicked(Placement placement, AdInfo adInfo) {
            Log.d(TAG, "LevelPlay Rewarded Video clicked");
            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "CLICKED", "rewarded", "ironsource", "DELIVERED", "", "");
            }
        }

        @Override
        public void onAdRewarded(Placement placement, AdInfo adInfo) {
            rewardGrantedInCurrentSession = true;
            Log.d(TAG, "🎁 LevelPlay Rewarded Video completed (reward granted)");
            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "COMPLETED", "rewarded", "ironsource", "DELIVERED", "", "");
            }
            if (currentRewardedCallback != null) {
                currentRewardedCallback.onRewardEarned();
            }
        }

        @Override
        public void onAdClosed(AdInfo adInfo) {
            BaseActivity.isAdShowing = false;
            setAdWatchPending(false);
            setLastInterstitialTime(System.currentTimeMillis());
            Log.d(TAG, "⏹️ LevelPlay Rewarded Video closed");

            if (appContext != null) {
                AdAnalyticsTracker.trackEvent(appContext, "CLOSED", "rewarded", "ironsource", "DELIVERED", "", "");
            }

            if (currentInterstitialCallback != null) {
                AdCallback cb = currentInterstitialCallback;
                currentInterstitialCallback = null;
                cb.onAdDismissed(true);
            }

            if (currentRewardedCallback != null) {
                RewardedAdCallback rCb = currentRewardedCallback;
                currentRewardedCallback = null;
                rCb.onAdDismissed(rewardGrantedInCurrentSession);
            }

            mainHandler.postDelayed(() -> preloadInterstitial(appContext), 2000);
        }
    };

    public boolean isAdShowing() {
        return BaseActivity.isAdShowing;
    }
}
