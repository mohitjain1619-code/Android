package com.mohit.camverz;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.ProductDetails;
import com.mohit.camverz.api.TokenManager;

import java.util.Map;

/**
 * Dedicated In-App Pricing & VIP Passes Activity.
 * Displays Subscriptions & Passes matching camverz-web pricing plans.
 * Prices are dynamically fetched from Google Play Store in the user's local currency.
 */
public class PricingActivity extends BaseActivity {

    private static final String TAG = "PricingActivity";

    private TextView tvPriceWeeklyVip, tvPriceMonthlyVip, tvPrice1DayVip;
    private Button btnBuyWeeklyVip, btnBuyMonthlyVip, btnBuy1DayVip;

    private BillingManager billingManager;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pricing);

        billingManager = BillingManager.getInstance(this);
        tokenManager = TokenManager.getInstance(this);

        initViews();
        setupClickListeners();
        loadDynamicPrices();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvPriceWeeklyVip = findViewById(R.id.tv_price_weekly_vip);
        tvPriceMonthlyVip = findViewById(R.id.tv_price_monthly_vip);
        tvPrice1DayVip = findViewById(R.id.tv_price_1day_vip);

        btnBuyWeeklyVip = findViewById(R.id.btn_buy_weekly_vip);
        btnBuyMonthlyVip = findViewById(R.id.btn_buy_monthly_vip);
        btnBuy1DayVip = findViewById(R.id.btn_buy_1day_vip);
    }

    private void setupClickListeners() {
        if (btnBuyWeeklyVip != null) {
            btnBuyWeeklyVip.setOnClickListener(v -> purchasePass(BillingManager.SUB_WEEKLY_VIP));
        }
        if (btnBuyMonthlyVip != null) {
            btnBuyMonthlyVip.setOnClickListener(v -> purchasePass(BillingManager.SUB_MONTHLY_VIP));
        }
        if (btnBuy1DayVip != null) {
            btnBuy1DayVip.setOnClickListener(v -> purchasePass(BillingManager.PASS_1DAY_VIP));
        }
    }

    private void loadDynamicPrices() {
        billingManager.queryProductDetails(new BillingManager.ProductDetailsCallback() {
            @Override
            public void onLoaded(Map<String, ProductDetails> detailsMap) {
                runOnUiThread(() -> {
                    updatePriceLabel(tvPriceWeeklyVip, BillingManager.SUB_WEEKLY_VIP, "₹299/wk");
                    updatePriceLabel(tvPriceMonthlyVip, BillingManager.SUB_MONTHLY_VIP, "₹499/mo");
                    updatePriceLabel(tvPrice1DayVip, BillingManager.PASS_1DAY_VIP, "₹170");
                });
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "ProductDetails error: " + message);
            }
        });
    }

    private void updatePriceLabel(TextView textView, String productId, String fallback) {
        if (textView == null) return;
        String formatted = billingManager.getFormattedPrice(productId, fallback);
        textView.setText(formatted);
    }

    private void purchasePass(String productId) {
        billingManager.launchPurchaseFlow(this, productId, new BillingManager.BillingPurchaseCallback() {
            @Override
            public void onSuccess(String prodId, String planName, boolean isAdFree) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(PricingActivity.this)
                            .setTitle("🎉 Pass Activated!")
                            .setMessage("Congratulations! Your " + planName + " is now active.\n\nEnjoy unlimited video calling with zero restrictions!")
                            .setCancelable(false)
                            .setPositiveButton("Awesome!", (dialog, which) -> {
                                setResult(RESULT_OK);
                                finish();
                            })
                            .show();
                });
            }

            @Override
            public void onUserCancelled() {
                Log.d(TAG, "Purchase flow cancelled by user");
            }

            @Override
            public void onError(int responseCode, String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(PricingActivity.this, "Purchase failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
