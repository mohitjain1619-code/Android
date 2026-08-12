package com.mohitt.camverz;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messages;
    private EditText messageEditText;
    private Button sendButton;
    private String currentUserId;
    private String receiverId;
    private String receiverName;
    private String receiverAvatar;
    
    private ApiService api;
    private TokenManager tokenManager;
    private Socket socket;
    
    private boolean isBlocked = false;
    private boolean isBlockedByOther = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);
        socket = SocketManager.getInstance();
        currentUserId = tokenManager.getUserId();
        
        receiverId = getIntent().getStringExtra("userId");
        receiverName = getIntent().getStringExtra("userName");
        receiverAvatar = getIntent().getStringExtra("userAvatar");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CircleImageView toolbarAvatar = findViewById(R.id.toolbar_avatar);
        TextView toolbarUsername = findViewById(R.id.toolbar_username);
        
        ImageView videoCallBtn = findViewById(R.id.video_call_button);
        ImageView voiceCallBtn = findViewById(R.id.voice_call_button);

        toolbarUsername.setText(receiverName);
        if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
            int avatarResId = getResources().getIdentifier(receiverAvatar, "drawable", getPackageName());
            if (avatarResId != 0) {
                Glide.with(this).load(avatarResId).placeholder(R.drawable.ic_user_placeholder).into(toolbarAvatar);
            } else {
                Glide.with(this).load(R.drawable.ic_user_placeholder).into(toolbarAvatar);
            }
        } else {
            toolbarAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }

        View.OnClickListener profileListener = v -> {
            Intent intent = new Intent(ChatActivity.this, ProfileActivity.class);
            intent.putExtra("userId", receiverId);
            startActivity(intent);
        };
        toolbarAvatar.setOnClickListener(profileListener);
        toolbarUsername.setOnClickListener(profileListener);
        
        videoCallBtn.setOnClickListener(v -> initiateCall(true));
        voiceCallBtn.setOnClickListener(v -> initiateCall(false));

        recyclerView = findViewById(R.id.chat_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> {
            if (isBlocked) {
                showBlockedDialog("You have blocked this user. Unblock them to send messages.");
                return;
            }
            if (isBlockedByOther) {
                showBlockedDialog("You cannot send messages because this user has blocked you.");
                return;
            }
            
            String msg = messageEditText.getText().toString();
            if (!msg.equals("")) {
                sendMessage(msg);
            }
            messageEditText.setText("");
        });

        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messages);
        chatAdapter.setReceiverAvatar(receiverAvatar);
        recyclerView.setAdapter(chatAdapter);

        setupSocket();
        checkBlockStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
        markAsRead();
    }

    private void setupSocket() {
        socket.on("new_message", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String senderId = data.getString("senderId");
                
                // If message is from the user we are chatting with, or from ourselves (sync)
                if (senderId.equals(receiverId) || senderId.equals(currentUserId)) {
                    runOnUiThread(() -> {
                        loadMessages(); // reload messages to get the latest
                        markAsRead();
                    });
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error handling new_message event", e);
            }
        });
    }

    private void checkBlockStatus() {
        if (!tokenManager.isLoggedIn()) return;
        
        api.getFriendStatus(receiverId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        isBlocked = data.has("isBlocked") && data.get("isBlocked").getAsBoolean();
                        isBlockedByOther = data.has("isBlockedByOther") && data.get("isBlockedByOther").getAsBoolean();
                    }
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error checking block status", t);
            }
        });
    }

    private void showBlockedDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_user_blocked, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        TextView msgView = dialogView.findViewById(R.id.messageBlocked);
        if (msgView != null) {
            msgView.setText(message);
        }
        
        Button btnOk = dialogView.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void initiateCall(boolean isVideo) {
        if (isBlocked) {
            showBlockedDialog("You have blocked this user. Unblock them to make calls.");
            return;
        }
        if (isBlockedByOther) {
            showBlockedDialog("You cannot call this user because they have blocked you.");
            return;
        }
    
        Intent intent = new Intent(ChatActivity.this, CallActivity.class);
        intent.putExtra("targetUserId", receiverId);
        intent.putExtra("targetUserName", receiverName);
        intent.putExtra("targetUserAvatar", receiverAvatar);
        intent.putExtra("isVideoCall", isVideo);
        intent.putExtra("isCaller", true);
        intent.putExtra("isPrivateCall", true);
        startActivity(intent);
    }

    private void sendMessage(String msg) {
        Map<String, String> body = new HashMap<>();
        body.put("text", msg);
        
        api.sendMessage(receiverId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        loadMessages(); // reload to show sent message
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        api.getMessages(receiverId, 50, null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        messages.clear();
                        if (data.has("messages")) {
                            JsonArray msgArray = data.getAsJsonArray("messages");
                            for (int i = msgArray.size() - 1; i >= 0; i--) { // Reverse order because we fetch newest first
                                JsonElement element = msgArray.get(i);
                                JsonObject msgObj = element.getAsJsonObject();
                                Message m = new Message();
                                m.setMessageId(msgObj.has("id") ? msgObj.get("id").getAsString() : "");
                                m.setSenderId(msgObj.has("senderId") ? msgObj.get("senderId").getAsString() : "");
                                m.setReceiverId(msgObj.has("receiverId") ? msgObj.get("receiverId").getAsString() : "");
                                m.setMessage(msgObj.has("text") ? msgObj.get("text").getAsString() : "");
                                m.setTimestamp(msgObj.has("timestamp") ? msgObj.get("timestamp").getAsLong() : 0);
                                m.setSeen(msgObj.has("seen") && msgObj.get("seen").getAsBoolean());
                                messages.add(m);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (messages.size() > 0) {
                            recyclerView.scrollToPosition(messages.size() - 1);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error loading messages", t);
            }
        });
    }

    private void markAsRead() {
        if (currentUserId != null && receiverId != null) {
            api.markChatRead(receiverId).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {}
            });
        }
    }
    
    // deleteMessage methods omitted for brevity, can be added later if needed via API

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.off("new_message");
    }
}
