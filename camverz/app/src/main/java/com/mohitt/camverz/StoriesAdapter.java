package com.mohitt.camverz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.ViewHolder> {

    public interface OnStoryActionListener {
        void onStorySelected(UserStories userStories);
        void onAddStorySelected();
    }

    private final Context context;
    private final List<UserStories> userStoriesList;
    private final boolean showAddStory;
    private final String currentAdminEmail = "mohitjain1619@gmail.com";
    private final String currentUserEmail;
    private final OnStoryActionListener listener;

    public StoriesAdapter(Context context, List<UserStories> userStoriesList, String currentUserEmail, OnStoryActionListener listener) {
        this.context = context;
        this.userStoriesList = userStoriesList;
        this.currentUserEmail = currentUserEmail;
        this.showAddStory = currentAdminEmail.equalsIgnoreCase(currentUserEmail);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (showAddStory && position == 0) {
            return 0; // Add Story item
        }
        return 1; // Standard Story item
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_circle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            // Admin Add Story Cell
            holder.tvStoryName.setText("Your Story");
            holder.ivAddStoryIcon.setVisibility(View.VISIBLE);
            
            // Check if admin already has active stories in the list
            UserStories adminStories = findAdminStories();
            if (adminStories != null && adminStories.getStories() != null && !adminStories.getStories().isEmpty()) {
                holder.storyRing.setVisibility(View.VISIBLE);
                AvatarHelper.loadAvatar(context, null, adminStories.getUserAvatar(), adminStories.getUserName(), holder.ivStoryAvatar);
                
                holder.ivStoryAvatar.setOnClickListener(v -> {
                    if (listener != null) listener.onStorySelected(adminStories);
                });
            } else {
                holder.storyRing.setVisibility(View.GONE);
                holder.ivStoryAvatar.setImageResource(R.drawable.av1); // Default placeholder
                holder.ivStoryAvatar.setOnClickListener(v -> {
                    if (listener != null) listener.onAddStorySelected();
                });
            }
            
            holder.ivAddStoryIcon.setOnClickListener(v -> {
                if (listener != null) listener.onAddStorySelected();
            });
        } else {
            // Standard Story Cell
            int listPos = position - (showAddStory ? 1 : 0);
            UserStories userStories = userStoriesList.get(listPos);

            holder.tvStoryName.setText(userStories.getUserName());
            holder.ivAddStoryIcon.setVisibility(View.GONE);
            holder.storyRing.setVisibility(View.VISIBLE);

            AvatarHelper.loadAvatar(context, null, userStories.getUserAvatar(), userStories.getUserName(), holder.ivStoryAvatar);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onStorySelected(userStories);
            });
        }
    }

    private UserStories findAdminStories() {
        for (UserStories us : userStoriesList) {
            if (currentAdminEmail.equalsIgnoreCase(us.getUserEmail())) {
                return us;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return userStoriesList.size() + (showAddStory ? 1 : 0);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View storyRing;
        ImageView ivStoryAvatar;
        ImageView ivAddStoryIcon;
        TextView tvStoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            storyRing = itemView.findViewById(R.id.storyRing);
            ivStoryAvatar = itemView.findViewById(R.id.ivStoryAvatar);
            ivAddStoryIcon = itemView.findViewById(R.id.ivAddStoryIcon);
            tvStoryName = itemView.findViewById(R.id.tvStoryName);
        }
    }
}
