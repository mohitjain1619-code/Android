package com.mohitt.camverz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import de.hdodenhof.circleimageview.CircleImageView;

public class AvatarSelectionAdapter extends RecyclerView.Adapter<AvatarSelectionAdapter.AvatarViewHolder> {

    private final Context context;
    private final String[] avatars;
    private final OnAvatarSelectedListener listener;

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(String avatarName);
    }

    public AvatarSelectionAdapter(Context context, String[] avatars, OnAvatarSelectedListener listener) {
        this.context = context;
        this.avatars = avatars;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_avatar_selection, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        String avatarName = avatars[position];
        int avatarResId = context.getResources().getIdentifier(avatarName, "drawable", context.getPackageName());
        
        if (avatarResId != 0) {
            Glide.with(context).load(avatarResId).into(holder.avatarImage);
        }

        holder.itemView.setOnClickListener(v -> listener.onAvatarSelected(avatarName));
    }

    @Override
    public int getItemCount() {
        return avatars.length;
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        CircleImageView avatarImage;

        AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatar_image);
        }
    }
}
