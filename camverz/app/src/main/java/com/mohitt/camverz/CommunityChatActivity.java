package com.mohitt.camverz;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import com.mohitt.camverz.api.TokenManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;

public class CommunityChatActivity extends BaseActivity {

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
    private LinearLayout btnHeaderVideoCall;
    private RecyclerView chatRecyclerView;
    private EditText etChatMessage;
    private FrameLayout btnSendMessage;

    private String targetUserId;
    private String targetUserName;
    private String targetUserAvatar;
    private String contactPreference;

    private String currentUserId;
    private TokenManager tokenManager;
    private Socket socket;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat);

        applyWindowInsets(findViewById(R.id.topChatHeader), findViewById(R.id.bottomInputBar));

        tokenManager = TokenManager.getInstance(this);
        currentUserId = tokenManager.getUserId();
        socket = SocketManager.getInstance();

        Intent intent = getIntent();
        targetUserId = intent.getStringExtra("targetUserId");
        if (targetUserId == null) targetUserId = intent.getStringExtra("userId");
        targetUserName = intent.getStringExtra("targetUserName");
        if (targetUserName == null) targetUserName = intent.getStringExtra("userName");
        targetUserAvatar = intent.getStringExtra("targetUserAvatar");
        if (targetUserAvatar == null) targetUserAvatar = intent.getStringExtra("userAvatar");
        contactPreference = intent.getStringExtra("contactPreference");

        btnBack = findViewById(R.id.btnBack);
        ivRecipientAvatar = findViewById(R.id.ivRecipientAvatar);
        tvRecipientName = findViewById(R.id.tvRecipientName);
        tvChatSubtitle = findViewById(R.id.tvChatSubtitle);
        btnHeaderVideoCall = findViewById(R.id.btnHeaderVideoCall);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        tvRecipientName.setText(targetUserName != null ? targetUserName : "Community Member");
        AvatarHelper.loadAvatar(this, null, targetUserAvatar, targetUserName, ivRecipientAvatar);

        // Strict preference check: Show Video Call button ONLY if preference included Video Call
        boolean allowVideo = contactPreference != null && contactPreference.toLowerCase().contains("video");
        btnHeaderVideoCall.setVisibility(allowVideo ? View.VISIBLE : View.GONE);

        btnBack.setOnClickListener(v -> finish());

        btnHeaderVideoCall.setOnClickListener(v -> {
            Intent callIntent = new Intent(this, CallActivity.class);
            callIntent.putExtra("targetUserId", targetUserId);
            callIntent.putExtra("isCaller", true);
            callIntent.putExtra("isPrivateCall", true);
            callIntent.putExtra("isVideoCall", true);
            startActivity(callIntent);
        });

        adapter = new ChatAdapter(messageList, currentUserId);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(lm);
        chatRecyclerView.setAdapter(adapter);

        btnSendMessage.setOnClickListener(v -> sendMessage());

        setupSocket();
    }

    private void setupSocket() {
        if (socket != null) {
            socket.on("chat-message-received", args -> {
                if (args != null && args.length > 0) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        String sender = obj.optString("senderId");
                        String msg = obj.optString("message");
                        if (targetUserId != null && targetUserId.equalsIgnoreCase(sender)) {
                            runOnUiThread(() -> {
                                messageList.add(new ChatMessage(sender, msg, System.currentTimeMillis()));
                                adapter.notifyItemInserted(messageList.size() - 1);
                                chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                            });
                        }
                    } catch (Exception e) {}
                }
            });
        }
    }

    private void sendMessage() {
        String text = etChatMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        etChatMessage.setText("");
        ChatMessage chatMsg = new ChatMessage(currentUserId, text, System.currentTimeMillis());
        messageList.add(chatMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);

        if (socket != null) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("senderId", currentUserId);
                payload.put("receiverId", targetUserId);
                payload.put("message", text);
                socket.emit("send-chat-message", payload);
            } catch (Exception e) {}
        }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(14);
            tv.setPadding(24, 18, 24, 18);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 10, 0, 10);
            tv.setLayoutParams(lp);

            return new RecyclerView.ViewHolder(tv) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage msg = list.get(position);
            TextView tv = (TextView) holder.itemView;
            tv.setText(msg.message);

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
            if (getItemViewType(position) == TYPE_SENT) {
                lp.gravity = android.view.Gravity.END;
                tv.setBackgroundResource(R.drawable.bg_luxury_tab_selected);
                tv.setTextColor(Color.BLACK);
            } else {
                lp.gravity = android.view.Gravity.START;
                tv.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
                tv.setTextColor(Color.WHITE);
            }
            tv.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
