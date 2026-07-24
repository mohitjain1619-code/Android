package com.mohitt.camverz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlockedUserAdapter extends RecyclerView.Adapter<BlockedUserAdapter.ViewHolder> {

    private Context context;
    private List<User> blockedUsers;
    private OnUnblockListener listener;

    public interface OnUnblockListener {
        void onUnblock(User user, int position);
    }

    public BlockedUserAdapter(Context context, List<User> blockedUsers, OnUnblockListener listener) {
        this.context = context;
        this.blockedUsers = blockedUsers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = blockedUsers.get(position);
        holder.userName.setText(user.getName());
        
        String avatarName = user.getAvatar();
        if (avatarName != null && !avatarName.isEmpty()) {
            int avatarResId = context.getResources().getIdentifier(avatarName, "drawable", context.getPackageName());
            if (avatarResId != 0) {
                Glide.with(context).load(avatarResId).into(holder.avatar);
            } else {
                Glide.with(context).load(R.drawable.av1).into(holder.avatar);
            }
        } else {
            Glide.with(context).load(R.drawable.av1).into(holder.avatar);
        }

        holder.btnUnblock.setOnClickListener(v -> listener.onUnblock(user, position));
    }

    @Override
    public int getItemCount() {
        return blockedUsers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView avatar;
        TextView userName;
        Button btnUnblock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            userName = itemView.findViewById(R.id.user_name);
            btnUnblock = itemView.findViewById(R.id.btn_unblock);
        }
    }
}
