package com.mohitt.camverz;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlockedUsersActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private BlockedUserAdapter adapter;
    private List<User> blockedUsers;
    private ImageView backBtn;
    
    private ApiService api;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        recyclerView = findViewById(R.id.blocked_users_recycler_view);
        backBtn = findViewById(R.id.back_btn);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        blockedUsers = new ArrayList<>();
        adapter = new BlockedUserAdapter(this, blockedUsers, this::unblockUser);
        recyclerView.setAdapter(adapter);

        backBtn.setOnClickListener(v -> finish());

        loadBlockedUsers();
    }

    private void loadBlockedUsers() {
        if (!tokenManager.isLoggedIn()) return;
        
        api.getBlockedUsers().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        blockedUsers.clear();
                        if (data.has("users")) {
                            JsonArray usersArray = data.getAsJsonArray("users");
                            for (JsonElement element : usersArray) {
                                JsonObject userObj = element.getAsJsonObject();
                                User user = new User();
                                user.setUid(userObj.has("id") ? userObj.get("id").getAsString() : "");
                                user.setName(userObj.has("name") ? userObj.get("name").getAsString() : "");
                                user.setAvatar(userObj.has("avatar") ? userObj.get("avatar").getAsString() : "");
                                user.setUserId(user.getUid()); // for adapter compat
                                blockedUsers.add(user);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        return;
                    }
                }
                Toast.makeText(BlockedUsersActivity.this, "Failed to load blocked users", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(BlockedUsersActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unblockUser(User user, int position) {
        if (user.getUid() == null || user.getUid().isEmpty()) return;

        api.unblockUser(user.getUid()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        Toast.makeText(BlockedUsersActivity.this, "Unblocked " + user.getName(), Toast.LENGTH_SHORT).show();
                        if (position >= 0 && position < blockedUsers.size()) {
                            blockedUsers.remove(position);
                            adapter.notifyItemRemoved(position);
                        }
                        return;
                    }
                }
                Toast.makeText(BlockedUsersActivity.this, "Failed to unblock user", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(BlockedUsersActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
