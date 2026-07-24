package com.mohitt.camverz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private Context context;
    private List<String> conversations;

    public MessagesAdapter(Context context, List<String> conversations) {
        this.context = context;
        this.conversations = conversations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // TODO: Replace with actual data from a Conversation model
        holder.userName.setText("User " + position);
        holder.lastMessage.setText("Last message goes here...");

        // TODO: Load actual user avatar
        Glide.with(context)
                .load(R.drawable.ic_launcher_background)
                .into(holder.userAvatar);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView userAvatar;
        public TextView userName;
        public TextView lastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.conversation_user_avatar);
            userName = itemView.findViewById(R.id.conversation_user_name);
            lastMessage = itemView.findViewById(R.id.conversation_last_message);
        }
    }
}
