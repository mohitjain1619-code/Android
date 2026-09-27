package com.mohit.camverz;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdAnalyticsTracker {
    private static final String TAG = "AdAnalyticsTracker";

    public static void trackEvent(Context context, String eventType, String adType, String adNetwork, String status, String errorCode, String errorMessage) {
        if (context == null) return;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("eventType", eventType != null ? eventType : "REQUEST");
            body.put("adType", adType != null ? adType : "rewarded");
            body.put("adNetwork", adNetwork != null ? adNetwork : "admob");
            body.put("status", status != null ? status : "DELIVERED");
            body.put("errorCode", errorCode != null ? errorCode : "");
            body.put("errorMessage", errorMessage != null ? errorMessage : "");
            body.put("platform", "android");

            ApiClient.getInstance(context).getApi().trackAdEvent(body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Ad event tracked: " + eventType + " (" + status + ")");
                    } else {
                        Log.w(TAG, "Failed to track ad event: HTTP " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Error sending ad tracking event", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in trackEvent", e);
        }
    }
}
