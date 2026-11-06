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
 * Exact Product IDs & Base Plan IDs matched to Google Play Console:
 * 1. Video 7 Days: "camverz_sub_weekly_vip" -> "weekly-vip-base" & "camverz-7-days-vip"
 * 2. Video 1 Month: "camverz_sub_monthly_package" -> "1-month-with-ads" & "1-month-without-ads"
 * 3. Community 1 Month: "camverz_community_1_month_pass" -> "realmeet-1-month-with-ads" & "realmeet-1-month-without-ads"
 * 4. 1 Day: "camverz_pass_1day_ads" & "camverz_pass_1day_vip"
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";

    // =========================================================================
    // EXACT PRODUCT IDS (Matched to Google Play Console Screenshots)
    // =========================================================================
    // 1. 7 Days Weekly Video Call Subscription
    public static final String SUB_WEEKLY_VIP = "camverz_sub_weekly_vip";
    public static final String BASE_PLAN_7DAYS_WITH_ADS = "weekly-vip-base";
    public static final String BASE_PLAN_7DAYS_NO_ADS = "camverz-7-days-vip";

    // 2. 1 Month Video Call Subscription
    public static final String SUB_MONTHLY_PACKAGE = "camverz_sub_monthly_package";
    public static final String BASE_PLAN_1MONTH_WITH_ADS = "1-month-with-ads";
    public static final String BASE_PLAN_1MONTH_NO_ADS = "1-month-without-ads";

    // 3. Community RealMeet 1 Month Subscription
    public static final String SUB_COMMUNITY_1MONTH = "camverz_community_1_month_pass";
    public static final String BASE_PLAN_COMMUNITY_1MONTH_WITH_ADS = "realmeet-1-month-with-ads";
    public static final String BASE_PLAN_COMMUNITY_1MONTH_NO_ADS = "realmeet-1-month-without-ads";

    // 4. Community RealMeet 7 Days Subscription (fallback / optional)
    public static final String SUB_COMMUNITY_7DAYS = "camverz_community_7days";
    public static final String BASE_PLAN_COMMUNITY_7DAYS_WITH_ADS = "7-days-realmeethub-with-ads";
    public static final String BASE_PLAN_COMMUNITY_7DAYS_NO_ADS = "7-days-realmeethub-without-ads";

    // 5. 1 Day One-Time Passes
    public static final String PASS_1DAY_ADS = "camverz_pass_1day_ads";
    public static final String PASS_1DAY_VIP = "camverz_pass_1day_vip";

    public static final List<String> SUBSCRIPTION_PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            SUB_WEEKLY_VIP,
            SUB_MONTHLY_PACKAGE,
            SUB_COMMUNITY_1MONTH,
            SUB_COMMUNITY_7DAYS
    ));

    public static final List<String> INAPP_PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            PASS_1DAY_ADS,
            PASS_1DAY_VIP
    ));

    private static volatile BillingManager instance;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, ProductDetails> productDetailsMap = new ConcurrentHashMap<>();

    private BillingClient billingClient;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private int reconnectRetryCount = 0;

    private BillingPurchaseCallback currentPurchaseCallback;
    private String pendingBasePlanId = null;

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

    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        startConnection(null);
    }

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

    public String getFormattedPrice(String productId, @Nullable String basePlanId, String fallback) {
        ProductDetails details = productDetailsMap.get(productId);
        if (details != null) {
            if (BillingClient.ProductType.SUBS.equals(details.getProductType())) {
                List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
                if (offers != null && !offers.isEmpty()) {
                    ProductDetails.SubscriptionOfferDetails matchedOffer = null;
                    if (basePlanId != null) {
                        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                            if (basePlanId.equals(offer.getBasePlanId())) {
                                matchedOffer = offer;
                                break;
                            }
                        }
                    }
                    if (matchedOffer == null) {
                        matchedOffer = offers.get(0);
                    }
                    if (matchedOffer.getPricingPhases() != null && !matchedOffer.getPricingPhases().getPricingPhaseList().isEmpty()) {
                        return matchedOffer.getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
                    }
                }
            } else if (details.getOneTimePurchaseOfferDetails() != null) {
                return details.getOneTimePurchaseOfferDetails().getFormattedPrice();
            }
        }
        return fallback;
    }

    public void launchPurchaseFlow(@NonNull Activity activity, @NonNull String productId, @Nullable String targetBasePlanId, @Nullable BillingPurchaseCallback callback) {
        this.currentPurchaseCallback = callback;
        this.pendingBasePlanId = targetBasePlanId;

        if (!isConnected) {
            startConnection(() -> launchPurchaseFlow(activity, productId, targetBasePlanId, callback));
            return;
        }

        ProductDetails details = productDetailsMap.get(productId);
        if (details == null) {
            queryProductDetails(new ProductDetailsCallback() {
                @Override
                public void onLoaded(Map<String, ProductDetails> detailsMap) {
                    ProductDetails fetched = detailsMap.get(productId);
                    if (fetched != null) {
                        executeBillingFlow(activity, fetched, targetBasePlanId);
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

        executeBillingFlow(activity, details, targetBasePlanId);
    }

    private void executeBillingFlow(Activity activity, ProductDetails details, @Nullable String targetBasePlanId) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        BillingFlowParams.ProductDetailsParams.Builder paramBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details);

        if (BillingClient.ProductType.SUBS.equals(details.getProductType())) {
            List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                String selectedOfferToken = null;
                if (targetBasePlanId != null) {
                    for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                        if (targetBasePlanId.equals(offer.getBasePlanId())) {
                            selectedOfferToken = offer.getOfferToken();
                            break;
                        }
                    }
                }
                if (selectedOfferToken == null) {
                    selectedOfferToken = offers.get(0).getOfferToken();
                }
                paramBuilder.setOfferToken(selectedOfferToken);
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

    private void handlePurchaseFulfillment(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            Log.w(TAG, "Purchase state is not PURCHASED: " + purchase.getPurchaseState());
            return;
        }

        List<String> products = purchase.getProducts();
        if (products.isEmpty()) return;

        String productId = products.get(0);
        Log.d(TAG, "Processing fulfilled purchase for Product ID: " + productId + " (Order ID: " + purchase.getOrderId() + ")");

        PlanDetails plan = calculatePlanDetails(productId, pendingBasePlanId);

        syncPurchaseToBackend(purchase, plan, () -> {
            TokenManager tokenManager = TokenManager.getInstance(appContext);
            tokenManager.savePlanName(plan.planName);
            tokenManager.savePlanIsAdFree(plan.isAdFree);
            tokenManager.saveHasActivePlan(true);

            boolean isSubscription = SUBSCRIPTION_PRODUCTS.contains(productId) || productId.contains("sub") || productId.contains("community");
            if (isSubscription) {
                acknowledgeSubscription(purchase);
            } else {
                consumePurchase(purchase);
            }

            if (currentPurchaseCallback != null) {
                currentPurchaseCallback.onSuccess(productId, plan.planName, plan.isAdFree);
                currentPurchaseCallback = null;
            }
            pendingBasePlanId = null;
        });
    }

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

    public void restoreActivePurchases() {
        if (!isConnected) return;

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

    private PlanDetails calculatePlanDetails(String productId, @Nullable String basePlanId) {
        long nowMs = System.currentTimeMillis();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String startedAt = isoFormat.format(new Date(nowMs));
        long hoursToAdd = 168;
        String planName = "Camverz Pass";
        boolean isAdFree = false;

        if (SUB_WEEKLY_VIP.equals(productId)) {
            hoursToAdd = 168;
            if (BASE_PLAN_7DAYS_NO_ADS.equals(basePlanId)) {
                planName = "7 Days Weekly VIP Pass (Zero Ads)";
                isAdFree = true;
            } else {
                planName = "7 Days Weekly Pass (With Ads)";
                isAdFree = false;
            }
        } else if (SUB_MONTHLY_PACKAGE.equals(productId)) {
            hoursToAdd = 720;
            if (BASE_PLAN_1MONTH_NO_ADS.equals(basePlanId)) {
                planName = "1 Month VIP Package (Zero Ads)";
                isAdFree = true;
            } else {
                planName = "1 Month Pass (With Ads)";
                isAdFree = false;
            }
        } else if (SUB_COMMUNITY_1MONTH.equals(productId)) {
            hoursToAdd = 720;
            if (BASE_PLAN_COMMUNITY_1MONTH_NO_ADS.equals(basePlanId)) {
                planName = "Community 1 Month VIP (Zero Ads)";
                isAdFree = true;
            } else {
                planName = "Community 1 Month Pass (With Ads)";
                isAdFree = false;
            }
        } else if (SUB_COMMUNITY_7DAYS.equals(productId)) {
            hoursToAdd = 168;
            if (BASE_PLAN_COMMUNITY_7DAYS_NO_ADS.equals(basePlanId)) {
                planName = "Community 7 Days VIP (Zero Ads)";
                isAdFree = true;
            } else {
                planName = "Community 7 Days Pass (With Ads)";
                isAdFree = false;
            }
        } else if (PASS_1DAY_VIP.equals(productId)) {
            hoursToAdd = 24;
            planName = "1 Day VIP Pass (Zero Ads)";
            isAdFree = true;
        } else if (PASS_1DAY_ADS.equals(productId)) {
            hoursToAdd = 24;
            planName = "1 Day Pass (With Ads)";
            isAdFree = false;
        }

        long expiresMs = nowMs + (hoursToAdd * 3600 * 1000);
        String expiresAt = isoFormat.format(new Date(expiresMs));

        return new PlanDetails(productId, planName, isAdFree, startedAt, expiresAt);
    }
}
