package com.mohit.camverz;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;
import com.mohit.camverz.api.TokenManager;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityChatActivity extends BaseActivity {
    private static final String TAG = "CommunityChatActivity";

    public static class ChatMessage {
        public String senderId;
        public String message;
        public long timestamp;

        public ChatMessage(String senderId, String message, long timestamp) {
            this.senderId = senderId;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    private ImageView btnBack;
    private ImageView ivRecipientAvatar;
    private TextView tvRecipientName, tvChatSubtitle;
    private LinearLayout btnHeaderCallRequest;
    private TextView tvHeaderActionText, btnDeleteChat;
    private RecyclerView chatRecyclerView;
    private EditText etChatMessage;
    private FrameLayout btnSendMessage;

    private String targetUserId;
    private String targetUserName;
    private String targetUserAvatar;
    private String contactPreference;
    private String postTitle;

    private String currentUserId;
    private TokenManager tokenManager;
    private ApiService api;
    private Socket socket;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private boolean isCallUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat);

        applyWindowInsets(findViewById(R.id.topChatHeader), findViewById(R.id.bottomInputBar));

        tokenManager = TokenManager.getInstance(this);
        currentUserId = tokenManager.getUserId();
        api = ApiClient.getInstance(this).getApi();
        socket = SocketManager.getInstance();

        Intent intent = getIntent();
        targetUserId = intent.getStringExtra("targetUserId");
        if (targetUserId == null) targetUserId = intent.getStringExtra("userId");
        targetUserName = intent.getStringExtra("targetUserName");
        if (targetUserName == null) targetUserName = intent.getStringExtra("userName");
        targetUserAvatar = intent.getStringExtra("targetUserAvatar");
        if (targetUserAvatar == null) targetUserAvatar = intent.getStringExtra("userAvatar");
        contactPreference = intent.getStringExtra("contactPreference");
        postTitle = intent.getStringExtra("postTitle");

        btnBack = findViewById(R.id.btnBack);
        ivRecipientAvatar = findViewById(R.id.ivRecipientAvatar);
        tvRecipientName = findViewById(R.id.tvRecipientName);
        tvChatSubtitle = findViewById(R.id.tvChatSubtitle);
        btnHeaderCallRequest = findViewById(R.id.btnHeaderCallRequest);
        tvHeaderActionText = findViewById(R.id.tvHeaderActionText);
        btnDeleteChat = findViewById(R.id.btnDeleteChat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        String titleName = (targetUserName != null && !targetUserName.trim().isEmpty()) ? targetUserName : "Community Member";
        tvRecipientName.setText(titleName);

        if (postTitle != null && !postTitle.trim().isEmpty()) {
            tvChatSubtitle.setText("💬 " + postTitle);
        } else {
            tvChatSubtitle.setText("💬 RealMeet Connection");
        }

        AvatarHelper.loadAvatar(this, null, targetUserAvatar, titleName, ivRecipientAvatar);

        boolean isVideoPref = contactPreference != null && contactPreference.toLowerCase().contains("video");
        if (isVideoPref) {
            isCallUnlocked = true;
            tvHeaderActionText.setText("🎥 Video Call");
        } else {
            tvHeaderActionText.setText("🎥 Request Call");
        }

        btnBack.setOnClickListener(v -> finish());

        btnHeaderCallRequest.setOnClickListener(v -> {
            if (isCallUnlocked) {
                new AlertDialog.Builder(this)
                        .setTitle("Start Video Call")
                        .setMessage("Start private video call with " + titleName + "?")
                        .setPositiveButton("Start Call", (dialog, which) -> {
                            Intent callIntent = new Intent(this, CallActivity.class);
                            callIntent.putExtra("targetUserId", targetUserId);
                            callIntent.putExtra("callerId", currentUserId);
                            callIntent.putExtra("peer", targetUserId);
                            callIntent.putExtra("targetUserName", targetUserName);
                            callIntent.putExtra("peerName", targetUserName);
                            callIntent.putExtra("targetUserAvatar", targetUserAvatar);
                            callIntent.putExtra("isCaller", true);
                            callIntent.putExtra("isPrivateCall", true);
                            callIntent.putExtra("isVideoCall", true);
                            startActivity(callIntent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                sendInChatCallRequest();
            }
        });

        btnDeleteChat.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Chat")
                    .setMessage("Are you sure you want to clear this conversation view?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        messageList.clear();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Conversation cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        adapter = new ChatAdapter(messageList, currentUserId);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(lm);
        chatRecyclerView.setAdapter(adapter);

        btnSendMessage.setOnClickListener(v -> sendMessage());

        loadMessages();
        setupSocket();
    }

    private void loadMessages() {
        if (targetUserId == null || targetUserId.isEmpty() || api == null) return;

        api.getMessages(targetUserId, 50, null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("ok") && body.get("ok").getAsBoolean() && body.has("messages")) {
                        JsonArray arr = body.getAsJsonArray("messages");
                        messageList.clear();
                        for (JsonElement el : arr) {
                            if (!el.isJsonObject()) continue;
                            JsonObject m = el.getAsJsonObject();
                            String sId = m.has("senderId") && !m.get("senderId").isJsonNull() ? m.get("senderId").getAsString() : "";
                            String text = m.has("text") && !m.get("text").isJsonNull() ? m.get("text").getAsString() : "";
                            long ts = System.currentTimeMillis();
                            if (m.has("createdAt") && !m.get("createdAt").isJsonNull()) {
                                try {
                                    String crStr = m.get("createdAt").getAsString();
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    ts = sdf.parse(crStr).getTime();
                                } catch (Exception ignored) {}
                            }

                            if (text.contains("Requested a Private Video Call")) {
                                isCallUnlocked = true;
                                tvHeaderActionText.setText("🎥 Video Call");
                            }

                            messageList.add(new ChatMessage(sId, text, ts));
                        }
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            chatRecyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.w(TAG, "Failed loading community chat messages", t);
            }
        });

        // Mark read
        api.markChatRead(targetUserId).enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void sendInChatCallRequest() {
        String msg = "🎥 Requested a Private Video Call. Tap to Accept & Start Call.";
        sendMessageText(msg);
        Toast.makeText(this, "🎥 Video call request sent!", Toast.LENGTH_SHORT).show();
    }

    private void setupSocket() {
        if (socket == null) return;

        socket.on("new_message", args -> {
            if (args != null && args.length > 0) {
                try {
                    JSONObject obj = (JSONObject) args[0];
                    String sender = obj.optString("senderId");
                    String msg = obj.optString("text");
                    if (targetUserId != null && targetUserId.equalsIgnoreCase(sender)) {
                        runOnUiThread(() -> {
                            if (msg.contains("Video Call")) {
                                isCallUnlocked = true;
                                tvHeaderActionText.setText("🎥 Video Call");
                            }
                            messageList.add(new ChatMessage(sender, msg, System.currentTimeMillis()));
                            adapter.notifyItemInserted(messageList.size() - 1);
                            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                        });
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error handling socket new_message", e);
                }
            }
        });

        socket.on("chat-message-received", args -> {
            if (args != null && args.length > 0) {
                try {
                    JSONObject obj = (JSONObject) args[0];
                    String sender = obj.optString("senderId");
                    String msg = obj.optString("message");
                    if (targetUserId != null && targetUserId.equalsIgnoreCase(sender)) {
                        runOnUiThread(() -> {
                            if (msg.contains("Video Call")) {
                                isCallUnlocked = true;
                                tvHeaderActionText.setText("🎥 Video Call");
                            }
                            messageList.add(new ChatMessage(sender, msg, System.currentTimeMillis()));
                            adapter.notifyItemInserted(messageList.size() - 1);
                            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                        });
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error handling socket chat-message-received", e);
                }
            }
        });
    }

    private void sendMessage() {
        String text = etChatMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etChatMessage.setText("");
        sendMessageText(text);
    }

    private void sendMessageText(String text) {
        if (targetUserId == null || targetUserId.isEmpty()) return;

        ChatMessage chatMsg = new ChatMessage(currentUserId, text, System.currentTimeMillis());
        messageList.add(chatMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);

        // 1. Post to Server API
        Map<String, String> body = new HashMap<>();
        body.put("text", text);
        api.sendMessage(targetUserId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Server returned error for sendMessage: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to send message over HTTP", t);
            }
        });

        // 2. Real-time Socket Emit
        if (socket != null) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("senderId", currentUserId);
                payload.put("receiverId", targetUserId);
                payload.put("message", text);
                socket.emit("send-chat-message", payload);
            } catch (Exception e) {
                Log.w(TAG, "Socket emit error", e);
            }
        }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
        private static final int TYPE_SENT = 1;
        private static final int TYPE_RECEIVED = 2;

        private final List<ChatMessage> list;
        private final String currentUserId;

        public ChatAdapter(List<ChatMessage> list, String currentUserId) {
            this.list = list;
            this.currentUserId = currentUserId;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage msg = list.get(position);
            return (currentUserId != null && currentUserId.equalsIgnoreCase(msg.senderId)) ? TYPE_SENT : TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = (viewType == TYPE_SENT) ? R.layout.item_chat_right : R.layout.item_chat_left;
            View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            ChatMessage msg = list.get(position);
            holder.showMessage.setText(msg.message);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView showMessage;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                showMessage = itemView.findViewById(R.id.show_message);
            }
        }
    }
}
