package com.mohitt.camverz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.InboxViewHolder> {

    private Context context;
    private List<Conversation> conversationList;
    private ApiService api;

    public InboxAdapter(Context context, List<Conversation> conversationList) {
        this.context = context;
        this.conversationList = conversationList;
        this.api = ApiClient.getInstance(context).getApi();
    }

    @NonNull
    @Override
    public InboxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inbox, parent, false);
        return new InboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InboxViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.nameTextView.setText(conversation.getName());
        holder.lastMessageTextView.setText(conversation.getLastMessage());
        holder.timestampTextView.setText(getFormattedTimestamp(conversation.getLastActivity()));

        String avatarName = conversation.getProfileImageUrl();
        if (avatarName != null && !avatarName.isEmpty()) {
            int avatarResId = context.getResources().getIdentifier(avatarName, "drawable", context.getPackageName());
            if (avatarResId != 0) {
                Glide.with(context).load(avatarResId).placeholder(R.drawable.av1).into(holder.profileImageView);
            } else {
                Glide.with(context).load(R.drawable.av1).into(holder.profileImageView);
            }
        } else {
            Glide.with(context).load(R.drawable.av1).into(holder.profileImageView);
        }

        if (conversation.isUnread()) {
            holder.lastMessageTextView.setTypeface(null, Typeface.BOLD);
            holder.lastMessageTextView.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.unreadIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.lastMessageTextView.setTypeface(null, Typeface.NORMAL);
            holder.lastMessageTextView.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
            holder.unreadIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("userId", conversation.getUserId());
            intent.putExtra("userName", conversation.getName());
            intent.putExtra("userAvatar", conversation.getProfileImageUrl());
            context.startActivity(intent);
        });

        // Open profile on avatar click
        holder.profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", conversation.getUserId());
            context.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Chat")
                    .setMessage("Are you sure you want to delete this chat?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteConversation(conversation.getUserId(), holder.getAdapterPosition());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    private String getFormattedTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void deleteConversation(String userId, int position) {
        api.deleteChat(userId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        if (position >= 0 && position < conversationList.size()) {
                            conversationList.remove(position);
                            notifyItemRemoved(position);
                        }
                        Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Toast.makeText(context, "Failed to delete chat", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class InboxViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView nameTextView;
        TextView lastMessageTextView;
        TextView timestampTextView;
        View unreadIndicator;

        public InboxViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }
}
