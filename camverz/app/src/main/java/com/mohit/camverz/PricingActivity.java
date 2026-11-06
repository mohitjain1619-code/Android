package com.mohit.camverz;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.ProductDetails;
import com.mohit.camverz.api.TokenManager;

import java.util.Map;

/**
 * Production-Grade Luxury Glossy Pricing & VIP Passes Activity.
 * Features 2 Luxury Tabs:
 * 1. Random Video Calling Passes (1 Day, 7 Days, 1 Month)
 * 2. RealMeet Community Passes (7 Days, 1 Month)
 * Structured with side-by-side 2-column cards: With Ads vs Without Ads (Zero Ads VIP).
 */
public class PricingActivity extends BaseActivity {

    private static final String TAG = "PricingActivity";

    // Tabs
    private LinearLayout tabVideoCall, tabRealMeet;
    private TextView tvTabVideo, tvTabRealMeet;
    private LinearLayout layoutTabVideo, layoutTabRealMeet;

    // 7 Days Weekly Video Views
    private TextView tvPrice7DaysAds, tvPrice7DaysVip;
    private Button btnBuy7DaysAds, btnBuy7DaysVip;

    // 1 Month Video Views
    private TextView tvPrice1MonthAds, tvPrice1MonthVip;
    private Button btnBuy1MonthAds, btnBuy1MonthVip;

    // 1 Day Video Views
    private TextView tvPrice1DayAds, tvPrice1DayVip;
    private Button btnBuy1DayAds, btnBuy1DayVip;

    // Community 7 Days Views
    private TextView tvPriceCommunity7DaysAds, tvPriceCommunity7DaysVip;
    private Button btnBuyCommunity7DaysAds, btnBuyCommunity7DaysVip;

    // Community 1 Month Views
    private TextView tvPriceCommunity1MonthAds, tvPriceCommunity1MonthVip;
    private Button btnBuyCommunity1MonthAds, btnBuyCommunity1MonthVip;

    private BillingManager billingManager;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pricing);

        billingManager = BillingManager.getInstance(this);
        tokenManager = TokenManager.getInstance(this);

        initViews();
        setupTabs();
        setupClickListeners();
        loadDynamicPrices();

        Intent intent = getIntent();
        if (intent != null && "realmeet".equalsIgnoreCase(intent.getStringExtra("default_tab"))) {
            selectTab(false);
        } else {
            selectTab(true);
        }
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Tabs
        tabVideoCall = findViewById(R.id.tab_video_call);
        tabRealMeet = findViewById(R.id.tab_realmeet);
        tvTabVideo = findViewById(R.id.tv_tab_video);
        tvTabRealMeet = findViewById(R.id.tv_tab_realmeet);
        layoutTabVideo = findViewById(R.id.layout_tab_video);
        layoutTabRealMeet = findViewById(R.id.layout_tab_realmeet);

        // 1 Day Video
        tvPrice1DayAds = findViewById(R.id.tv_price_1day_ads);
        tvPrice1DayVip = findViewById(R.id.tv_price_1day_vip);
        btnBuy1DayAds = findViewById(R.id.btn_buy_1day_ads);
        btnBuy1DayVip = findViewById(R.id.btn_buy_1day_vip);

        // 7 Days Video
        tvPrice7DaysAds = findViewById(R.id.tv_price_7days_ads);
        tvPrice7DaysVip = findViewById(R.id.tv_price_7days_vip);
        btnBuy7DaysAds = findViewById(R.id.btn_buy_7days_ads);
        btnBuy7DaysVip = findViewById(R.id.btn_buy_7days_vip);

        // 1 Month Video
        tvPrice1MonthAds = findViewById(R.id.tv_price_1month_ads);
        tvPrice1MonthVip = findViewById(R.id.tv_price_1month_vip);
        btnBuy1MonthAds = findViewById(R.id.btn_buy_1month_ads);
        btnBuy1MonthVip = findViewById(R.id.btn_buy_1month_vip);

        // Community 7 Days
        tvPriceCommunity7DaysAds = findViewById(R.id.tv_price_community_7days_ads);
        tvPriceCommunity7DaysVip = findViewById(R.id.tv_price_community_7days_vip);
        btnBuyCommunity7DaysAds = findViewById(R.id.btn_buy_community_7days_ads);
        btnBuyCommunity7DaysVip = findViewById(R.id.btn_buy_community_7days_vip);

        // Community 1 Month
        tvPriceCommunity1MonthAds = findViewById(R.id.tv_price_community_1month_ads);
        tvPriceCommunity1MonthVip = findViewById(R.id.tv_price_community_1month_vip);
        btnBuyCommunity1MonthAds = findViewById(R.id.btn_buy_community_1month_ads);
        btnBuyCommunity1MonthVip = findViewById(R.id.btn_buy_community_1month_vip);

        // Terms link
        View termsLink = findViewById(R.id.tv_terms_link);
        if (termsLink != null) {
            termsLink.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://camverz.com/legal/terms-and-conditions"));
                startActivity(browserIntent);
            });
        }
    }

    private void setupTabs() {
        if (tabVideoCall != null) {
            tabVideoCall.setOnClickListener(v -> selectTab(true));
        }
        if (tabRealMeet != null) {
            tabRealMeet.setOnClickListener(v -> selectTab(false));
        }
    }

    private void selectTab(boolean isVideoCall) {
        if (isVideoCall) {
            layoutTabVideo.setVisibility(View.VISIBLE);
            layoutTabRealMeet.setVisibility(View.GONE);

            tabVideoCall.setBackgroundResource(R.drawable.bg_tab_active_gradient);
            tvTabVideo.setTextColor(ContextCompat.getColor(this, R.color.text_white));

            tabRealMeet.setBackgroundResource(R.drawable.bg_tab_inactive);
            tvTabRealMeet.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        } else {
            layoutTabVideo.setVisibility(View.GONE);
            layoutTabRealMeet.setVisibility(View.VISIBLE);

            tabRealMeet.setBackgroundResource(R.drawable.bg_tab_active_gradient);
            tvTabRealMeet.setTextColor(ContextCompat.getColor(this, R.color.text_white));

            tabVideoCall.setBackgroundResource(R.drawable.bg_tab_inactive);
            tvTabVideo.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }
    }

    private void setupClickListeners() {
        // 1 Day Video Pass
        if (btnBuy1DayAds != null) {
            btnBuy1DayAds.setOnClickListener(v -> purchasePass(BillingManager.PASS_1DAY_ADS, null));
        }
        if (btnBuy1DayVip != null) {
            btnBuy1DayVip.setOnClickListener(v -> purchasePass(BillingManager.PASS_1DAY_VIP, null));
        }

        // 7 Days Weekly Video Pass
        if (btnBuy7DaysAds != null) {
            btnBuy7DaysAds.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_WEEKLY_VIP,
                    BillingManager.BASE_PLAN_7DAYS_WITH_ADS
            ));
        }
        if (btnBuy7DaysVip != null) {
            btnBuy7DaysVip.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_WEEKLY_VIP,
                    BillingManager.BASE_PLAN_7DAYS_NO_ADS
            ));
        }

        // 1 Month Video Pass
        if (btnBuy1MonthAds != null) {
            btnBuy1MonthAds.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_MONTHLY_PACKAGE,
                    BillingManager.BASE_PLAN_1MONTH_WITH_ADS
            ));
        }
        if (btnBuy1MonthVip != null) {
            btnBuy1MonthVip.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_MONTHLY_PACKAGE,
                    BillingManager.BASE_PLAN_1MONTH_NO_ADS
            ));
        }

        // Community 7 Days Pass
        if (btnBuyCommunity7DaysAds != null) {
            btnBuyCommunity7DaysAds.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_COMMUNITY_7DAYS,
                    BillingManager.BASE_PLAN_COMMUNITY_7DAYS_WITH_ADS
            ));
        }
        if (btnBuyCommunity7DaysVip != null) {
            btnBuyCommunity7DaysVip.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_COMMUNITY_7DAYS,
                    BillingManager.BASE_PLAN_COMMUNITY_7DAYS_NO_ADS
            ));
        }

        // Community 1 Month Pass
        if (btnBuyCommunity1MonthAds != null) {
            btnBuyCommunity1MonthAds.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_COMMUNITY_1MONTH,
                    BillingManager.BASE_PLAN_COMMUNITY_1MONTH_WITH_ADS
            ));
        }
        if (btnBuyCommunity1MonthVip != null) {
            btnBuyCommunity1MonthVip.setOnClickListener(v -> purchasePass(
                    BillingManager.SUB_COMMUNITY_1MONTH,
                    BillingManager.BASE_PLAN_COMMUNITY_1MONTH_NO_ADS
            ));
        }
    }

    private void loadDynamicPrices() {
        billingManager.queryProductDetails(new BillingManager.ProductDetailsCallback() {
            @Override
            public void onLoaded(Map<String, ProductDetails> detailsMap) {
                runOnUiThread(() -> {
                    // 1 Day Video
                    updatePriceLabel(tvPrice1DayAds, BillingManager.PASS_1DAY_ADS, null, "₹130");
                    updatePriceLabel(tvPrice1DayVip, BillingManager.PASS_1DAY_VIP, null, "₹170");

                    // 7 Days Video
                    updatePriceLabel(tvPrice7DaysAds, BillingManager.SUB_WEEKLY_VIP, BillingManager.BASE_PLAN_7DAYS_WITH_ADS, "₹249/wk");
                    updatePriceLabel(tvPrice7DaysVip, BillingManager.SUB_WEEKLY_VIP, BillingManager.BASE_PLAN_7DAYS_NO_ADS, "₹379/wk");

                    // 1 Month Video
                    updatePriceLabel(tvPrice1MonthAds, BillingManager.SUB_MONTHLY_PACKAGE, BillingManager.BASE_PLAN_1MONTH_WITH_ADS, "₹449/mo");
                    updatePriceLabel(tvPrice1MonthVip, BillingManager.SUB_MONTHLY_PACKAGE, BillingManager.BASE_PLAN_1MONTH_NO_ADS, "₹499/mo");

                    // Community 7 Days
                    updatePriceLabel(tvPriceCommunity7DaysAds, BillingManager.SUB_COMMUNITY_7DAYS, BillingManager.BASE_PLAN_COMMUNITY_7DAYS_WITH_ADS, "₹699/wk");
                    updatePriceLabel(tvPriceCommunity7DaysVip, BillingManager.SUB_COMMUNITY_7DAYS, BillingManager.BASE_PLAN_COMMUNITY_7DAYS_NO_ADS, "₹850/wk");

                    // Community 1 Month
                    updatePriceLabel(tvPriceCommunity1MonthAds, BillingManager.SUB_COMMUNITY_1MONTH, BillingManager.BASE_PLAN_COMMUNITY_1MONTH_WITH_ADS, "₹1,199/mo");
                    updatePriceLabel(tvPriceCommunity1MonthVip, BillingManager.SUB_COMMUNITY_1MONTH, BillingManager.BASE_PLAN_COMMUNITY_1MONTH_NO_ADS, "₹1,399/mo");
                });
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "ProductDetails error: " + message);
            }
        });
    }

    private void updatePriceLabel(TextView textView, String productId, String basePlanId, String fallback) {
        if (textView == null) return;
        String formatted = billingManager.getFormattedPrice(productId, basePlanId, fallback);
        textView.setText(formatted);
    }

    private void purchasePass(String productId, String basePlanId) {
        billingManager.launchPurchaseFlow(this, productId, basePlanId, new BillingManager.BillingPurchaseCallback() {
            @Override
            public void onSuccess(String prodId, String planName, boolean isAdFree) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(PricingActivity.this)
                            .setTitle("🎉 Pass Activated!")
                            .setMessage("Congratulations! Your " + planName + " is now active.\n\nEnjoy premium features on Camverz!")
                            .setPositiveButton("Start Enjoying", (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                });
            }

            @Override
            public void onUserCancelled() {
                Log.d(TAG, "User dismissed billing dialog");
            }

            @Override
            public void onError(int responseCode, String message) {
                runOnUiThread(() -> {
                    if (responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED) {
                        Toast.makeText(PricingActivity.this, "Purchase failed: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
