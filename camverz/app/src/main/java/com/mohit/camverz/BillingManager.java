package com.mohit.camverz;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Production-Grade BillingManager for Camverz utilizing Google Play Billing Library 8.x.
 * 
 * Supports:
 * 1. Auto-Renewing Subscriptions (SUBS): Weekly VIP (7 Days) and Monthly VIP (30 Days).
 * 2. One-Time Passes (INAPP): 1 Day VIP Pass and repurchasable passes.
 * 3. Acknowledges recurring subscriptions (prevents automatic Google refunds).
 * 4. Consumes one-time passes (allows repeated purchases).
 * 5. Instant Backend Sync (/api/users/me) and local TokenManager VIP activation.
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";

    // =========================================================================
    // PRODUCT IDS (Must match Google Play Console Subscriptions & In-App Products)
    // =========================================================================
    // Subscriptions (Auto-Pay)
    public static final String SUB_WEEKLY_VIP = "camverz_sub_weekly_vip";    // 7 Days Weekly Auto-Renewing
    public static final String SUB_MONTHLY_VIP = "camverz_sub_monthly_vip";  // 1 Month Auto-Renewing

    // One-Time Passes (Consumable)
    public static final String PASS_1DAY_VIP = "camverz_pass_1day_vip";       // 24 Hours Pass
    public static final String PASS_7DAYS_VIP = "camverz_pass_7days_vip";     // One-time 7 Days
    public static final String PASS_1MONTH_VIP = "camverz_pass_1month_vip";   // One-time 1 Month

    public static final List<String> SUBSCRIPTION_PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            SUB_WEEKLY_VIP,
            SUB_MONTHLY_VIP
    ));

    public static final List<String> INAPP_PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            PASS_1DAY_VIP,
            PASS_7DAYS_VIP,
            PASS_1MONTH_VIP
    ));

    private static volatile BillingManager instance;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, ProductDetails> productDetailsMap = new ConcurrentHashMap<>();

    private BillingClient billingClient;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private int reconnectRetryCount = 0;

    // Active purchase callback for the caller activity
    private BillingPurchaseCallback currentPurchaseCallback;

    public interface BillingPurchaseCallback {
        void onSuccess(String productId, String planName, boolean isAdFree);
        void onUserCancelled();
        void onError(int responseCode, String errorMessage);
    }

    public interface ProductDetailsCallback {
        void onLoaded(Map<String, ProductDetails> detailsMap);
        void onError(String message);
    }

    private BillingManager(Context context) {
        this.appContext = context.getApplicationContext();
        initializeBillingClient();
    }

    public static synchronized BillingManager getInstance(Context context) {
        if (instance == null) {
            instance = new BillingManager(context);
        }
        return instance;
    }

    /**
     * Build and configure the Google Play BillingClient (PBL 8.x compliant)
     */
    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        startConnection(null);
    }

    /**
     * Start connection to Google Play with exponential backoff on failure
     */
    public synchronized void startConnection(@Nullable Runnable onConnectedAction) {
        if (billingClient == null || isConnected || isConnecting) {
            if (isConnected && onConnectedAction != null) {
                onConnectedAction.run();
            }
            return;
        }

        isConnecting = true;
        Log.d(TAG, "Connecting to Google Play Billing Service...");

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                isConnecting = false;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    isConnected = true;
                    reconnectRetryCount = 0;
                    Log.d(TAG, "Google Play Billing Service Connected Successfully!");

                    // Query product details and active subscriptions automatically
                    queryProductDetails(null);
                    restoreActivePurchases();

                    if (onConnectedAction != null) {
                        mainHandler.post(onConnectedAction);
                    }
                } else {
                    isConnected = false;
                    Log.e(TAG, "Billing Setup Failed: " + billingResult.getDebugMessage() + " (Code: " + billingResult.getResponseCode() + ")");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isConnecting = false;
                isConnected = false;
                Log.w(TAG, "Google Play Billing Service Disconnected. Attempting reconnect...");

                int delayMs = Math.min(30000, (int) Math.pow(2, reconnectRetryCount) * 2000);
                reconnectRetryCount++;
                mainHandler.postDelayed(() -> startConnection(null), delayMs);
            }
        });
    }

    /**
     * Query both Subscriptions (SUBS) and One-time products (INAPP) from Google Play (PBL 8.x)
     */
    public void queryProductDetails(@Nullable ProductDetailsCallback callback) {
        if (!isConnected) {
            startConnection(() -> queryProductDetails(callback));
            return;
        }

        List<QueryProductDetailsParams.Product> subProducts = new ArrayList<>();
        for (String id : SUBSCRIPTION_PRODUCTS) {
            subProducts.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build());
        }

        List<QueryProductDetailsParams.Product> inappProducts = new ArrayList<>();
        for (String id : INAPP_PRODUCTS) {
            inappProducts.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }

        // 1. Query Subscriptions
        QueryProductDetailsParams subParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subProducts)
                .build();

        billingClient.queryProductDetailsAsync(subParams, (billingResult, result) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && result != null) {
                List<ProductDetails> subDetailsList = result.getProductDetailsList();
                if (subDetailsList != null) {
                    for (ProductDetails details : subDetailsList) {
                        productDetailsMap.put(details.getProductId(), details);
                        Log.d(TAG, "Loaded Subscription: " + details.getProductId() + " -> " + details.getTitle());
                    }
                }
            }

            // 2. Query In-App Passes
            QueryProductDetailsParams inappParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(inappProducts)
                    .build();

            billingClient.queryProductDetailsAsync(inappParams, (inappResult, inappResultObj) -> {
                if (inappResult.getResponseCode() == BillingClient.BillingResponseCode.OK && inappResultObj != null) {
                    List<ProductDetails> inappDetailsList = inappResultObj.getProductDetailsList();
                    if (inappDetailsList != null) {
                        for (ProductDetails details : inappDetailsList) {
                            productDetailsMap.put(details.getProductId(), details);
                            Log.d(TAG, "Loaded In-App Product: " + details.getProductId() + " -> " + details.getTitle());
                        }
                    }
                }

                if (callback != null) {
                    mainHandler.post(() -> callback.onLoaded(productDetailsMap));
                }
            });
        });
    }

    /**
     * Returns formatted price string for a product ID (e.g. "₹299.00" or "$2.99")
     */
    public String getFormattedPrice(String productId, String fallback) {
        ProductDetails details = productDetailsMap.get(productId);
        if (details != null) {
            if (BillingClient.ProductType.SUBS.equals(details.getProductType())) {
                List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
                if (offers != null && !offers.isEmpty()) {
                    ProductDetails.SubscriptionOfferDetails offer = offers.get(0);
                    if (offer.getPricingPhases() != null && !offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
                        return offer.getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
                    }
                }
            } else if (details.getOneTimePurchaseOfferDetails() != null) {
                return details.getOneTimePurchaseOfferDetails().getFormattedPrice();
            }
        }
        return fallback;
    }

    /**
     * Launches the Google Play Purchase / Subscription Flow
     */
    public void launchPurchaseFlow(@NonNull Activity activity, @NonNull String productId, @Nullable BillingPurchaseCallback callback) {
        this.currentPurchaseCallback = callback;

        if (!isConnected) {
            startConnection(() -> launchPurchaseFlow(activity, productId, callback));
            return;
        }

        ProductDetails details = productDetailsMap.get(productId);
        if (details == null) {
            queryProductDetails(new ProductDetailsCallback() {
                @Override
                public void onLoaded(Map<String, ProductDetails> detailsMap) {
                    ProductDetails fetched = detailsMap.get(productId);
                    if (fetched != null) {
                        executeBillingFlow(activity, fetched);
                    } else if (callback != null) {
                        callback.onError(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, "Product currently unavailable in Google Play Store.");
                    }
                }

                @Override
                public void onError(String message) {
                    if (callback != null) callback.onError(BillingClient.BillingResponseCode.ERROR, message);
                }
            });
            return;
        }

        executeBillingFlow(activity, details);
    }

    private void executeBillingFlow(Activity activity, ProductDetails details) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        BillingFlowParams.ProductDetailsParams.Builder paramBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details);

        // Required for Google Play Billing Subscriptions: offerToken
        if (BillingClient.ProductType.SUBS.equals(details.getProductType())) {
            List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                paramBuilder.setOfferToken(offers.get(0).getOfferToken());
            } else {
                Log.e(TAG, "No subscription offers found for: " + details.getProductId());
                if (currentPurchaseCallback != null) {
                    currentPurchaseCallback.onError(BillingClient.BillingResponseCode.ERROR, "Subscription offer not found.");
                    currentPurchaseCallback = null;
                }
                return;
            }
        }

        productDetailsParamsList.add(paramBuilder.build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        Log.d(TAG, "launchBillingFlow executed for: " + details.getProductId() + " (Code: " + result.getResponseCode() + ")");
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        Log.d(TAG, "onPurchasesUpdated responseCode: " + responseCode);

        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchaseFulfillment(purchase);
            }
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled purchase flow.");
            if (currentPurchaseCallback != null) {
                currentPurchaseCallback.onUserCancelled();
                currentPurchaseCallback = null;
            }
        } else {
            Log.e(TAG, "Purchase failed: " + billingResult.getDebugMessage() + " (Code: " + responseCode + ")");
            if (currentPurchaseCallback != null) {
                currentPurchaseCallback.onError(responseCode, billingResult.getDebugMessage());
                currentPurchaseCallback = null;
            }
        }
    }

    /**
     * Processes verified purchase:
     * - Subscriptions: Acknowledges purchase (prevents cancellation)
     * - One-time passes: Consumes purchase (allows repurchase)
     * - Syncs to backend and unlocks local TokenManager VIP privileges
     */
    private void handlePurchaseFulfillment(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            Log.w(TAG, "Purchase state is not PURCHASED: " + purchase.getPurchaseState());
            return;
        }

        List<String> products = purchase.getProducts();
        if (products.isEmpty()) return;

        String productId = products.get(0);
        Log.d(TAG, "Processing fulfilled purchase for Product ID: " + productId + " (Order ID: " + purchase.getOrderId() + ")");

        // 1. Calculate Plan Details (Name, Ad-Free status, Expiration timestamp)
        PlanDetails plan = calculatePlanDetails(productId);

        // 2. Sync to Backend via REST API
        syncPurchaseToBackend(purchase, plan, () -> {
            // 3. Update TokenManager for immediate UI unlocking
            TokenManager tokenManager = TokenManager.getInstance(appContext);
            tokenManager.savePlanName(plan.planName);
            tokenManager.savePlanIsAdFree(plan.isAdFree);
            tokenManager.saveHasActivePlan(true);

            // 4. Acknowledge (SUBS) or Consume (INAPP)
            boolean isSubscription = SUBSCRIPTION_PRODUCTS.contains(productId) || productId.contains("sub");
            if (isSubscription) {
                acknowledgeSubscription(purchase);
            } else {
                consumePurchase(purchase);
            }

            // 5. Notify UI Callback
            if (currentPurchaseCallback != null) {
                currentPurchaseCallback.onSuccess(productId, plan.planName, plan.isAdFree);
                currentPurchaseCallback = null;
            }
        });
    }

    /**
     * Acknowledges a subscription so Google Play does not refund/cancel it after 3 days.
     */
    private void acknowledgeSubscription(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();

            billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Subscription successfully acknowledged with Google Play!");
                } else {
                    Log.w(TAG, "Failed to acknowledge subscription: " + billingResult.getDebugMessage());
                }
            });
        }
    }

    /**
     * Consumes one-time purchase so it can be bought repeatedly
     */
    private void consumePurchase(Purchase purchase) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase successfully consumed! Product is repurchasable.");
            } else {
                Log.w(TAG, "Failed to consume purchase: " + billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Syncs purchase information to the Camverz Ultra Backend (/api/users/me)
     */
    private void syncPurchaseToBackend(Purchase purchase, PlanDetails plan, Runnable onSynced) {
        try {
            ApiService api = ApiClient.getInstance(appContext).getApi();

            Map<String, Object> updates = new HashMap<>();
            updates.put("plan_name", plan.planName);
            updates.put("plan_is_ad_free", plan.isAdFree);
            updates.put("plan_started_at", plan.startedAt);
            updates.put("plan_expires_at", plan.expiresAt);
            updates.put("google_purchase_token", purchase.getPurchaseToken());
            updates.put("google_order_id", purchase.getOrderId());
            updates.put("google_product_id", plan.productId);

            api.updateMe(updates).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Purchase successfully synced to Backend for " + plan.planName);
                    } else {
                        Log.w(TAG, "Backend sync returned non-200 code: " + response.code());
                    }
                    if (onSynced != null) mainHandler.post(onSynced);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Error syncing purchase to backend: " + t.getMessage());
                    if (onSynced != null) mainHandler.post(onSynced);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during syncPurchaseToBackend", e);
            if (onSynced != null) mainHandler.post(onSynced);
        }
    }

    /**
     * Checks active subscriptions & unconsumed purchases on app startup
     */
    public void restoreActivePurchases() {
        if (!isConnected) return;

        // 1. Check Subscriptions
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                Log.d(TAG, "Restoring active subscription: " + purchase.getOrderId());
                                handlePurchaseFulfillment(purchase);
                            }
                        }
                    }
                }
        );

        // 2. Check In-App Passes
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                Log.d(TAG, "Restoring unconsumed in-app pass: " + purchase.getOrderId());
                                handlePurchaseFulfillment(purchase);
                            }
                        }
                    }
                }
        );
    }

    private static class PlanDetails {
        final String productId;
        final String planName;
        final boolean isAdFree;
        final String startedAt;
        final String expiresAt;

        PlanDetails(String productId, String planName, boolean isAdFree, String startedAt, String expiresAt) {
            this.productId = productId;
            this.planName = planName;
            this.isAdFree = isAdFree;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
        }
    }

    private PlanDetails calculatePlanDetails(String productId) {
        long nowMs = System.currentTimeMillis();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String startedAt = isoFormat.format(new Date(nowMs));
        long hoursToAdd = 168; // Default to 7 days
        String planName = "7 Days Weekly VIP Pass";
        boolean isAdFree = true;

        switch (productId) {
            case SUB_WEEKLY_VIP:
            case PASS_7DAYS_VIP:
                hoursToAdd = 168; // 7 days (7 * 24h)
                planName = "7 Days Weekly VIP Pass";
                isAdFree = true;
                break;
            case SUB_MONTHLY_VIP:
            case PASS_1MONTH_VIP:
                hoursToAdd = 720; // 30 days
                planName = "1 Month VIP Package";
                isAdFree = true;
                break;
            case PASS_1DAY_VIP:
                hoursToAdd = 24;
                planName = "1 Day VIP Pass";
                isAdFree = true;
                break;
            default:
                if (productId.contains("month")) {
                    hoursToAdd = 720;
                    planName = "1 Month VIP Package";
                } else if (productId.contains("week") || productId.contains("7day")) {
                    hoursToAdd = 168;
                    planName = "7 Days Weekly VIP Pass";
                } else {
                    hoursToAdd = 24;
                    planName = "1 Day VIP Pass";
                }
                isAdFree = true;
                break;
        }

        long expiresMs = nowMs + (hoursToAdd * 3600 * 1000);
        String expiresAt = isoFormat.format(new Date(expiresMs));

        return new PlanDetails(productId, planName, isAdFree, startedAt, expiresAt);
    }
}
