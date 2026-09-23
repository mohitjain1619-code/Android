package com.mohit.camverz.api;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton API client using Retrofit + OkHttp.
 * Auto-attaches JWT token to every request.
 * 
 * Usage:
 *   ApiService api = ApiClient.getInstance(context).getApi();
 *   api.getMe().enqueue(...);
 */
public class ApiClient {

    private static final String TAG = "ApiClient";

    // Change this URL for production in build.gradle (app-level)
    // Local Docker: http://10.0.2.2:3000/api/  (Android emulator → host machine)
    // Local Device: http://YOUR_PC_IP:3000/api/
    // Production:   https://api.camverz.com/api/
    private static final String BASE_URL = com.mohit.camverz.BuildConfig.BASE_URL + "/api/";

    private static ApiClient instance;
    private final ApiService api;
    private final TokenManager tokenManager;

    private ApiClient(Context context) {
        tokenManager = TokenManager.getInstance(context);

        // Logging interceptor (enabled only in debug builds)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(com.mohit.camverz.BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        // Auth interceptor — adds JWT to every request
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String token = tokenManager.getToken();

            Request.Builder requestBuilder = original.newBuilder()
                    .header("Bypass-Tunnel-Reminder", "true")
                    .header("localtunnel-bypass", "true");

            if (token != null) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }

            return chain.proceed(requestBuilder.build());
        };

        // 401 handler — clear token & redirect on auth failure or account deletion
        Interceptor unauthorizedInterceptor = chain -> {
            Response response = chain.proceed(chain.request());
            if (response.code() == 401) {
                Log.w(TAG, "401 Unauthorized — account deleted or session expired");
                boolean hadToken = tokenManager.hasToken();
                tokenManager.clearAll();
                if (hadToken) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        android.widget.Toast.makeText(context, "⚠️ Account deleted or session reset. Please sign in again.", android.widget.Toast.LENGTH_LONG).show();
                        android.content.Intent intent = new android.content.Intent(context, com.mohit.camverz.LoginActivity.class);
                        intent.putExtra("account_deleted", true);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                    });
                }
            }
            return response;
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .protocols(java.util.Collections.singletonList(okhttp3.Protocol.HTTP_1_1))
                .addInterceptor(authInterceptor)
                .addInterceptor(unauthorizedInterceptor)
                .addInterceptor(logging)
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(ApiService.class);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }

    public ApiService getApi() {
        return api;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }
}
