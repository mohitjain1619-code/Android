package com.mohit.camverz;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class StoryViewersAdapter extends RecyclerView.Adapter<StoryViewersAdapter.ViewHolder> {

    public interface OnViewerClickListener {
        void onViewerClick(String userId);
    }

    private final Context context;
    private final JsonArray viewers;
    private final OnViewerClickListener listener;

    public StoryViewersAdapter(Context context, JsonArray viewers, OnViewerClickListener listener) {
        this.context = context;
        this.viewers = viewers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_viewer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= viewers.size()) return;
        JsonObject item = viewers.get(position).getAsJsonObject();

        String userId = item.has("id") ? item.get("id").getAsString() : "";
        String name = item.has("name") ? item.get("name").getAsString() : "User";
        String avatar = item.has("avatar") ? item.get("avatar").getAsString() : "";
        String photoUrl = item.has("photo_url") && !item.get("photo_url").isJsonNull() ? item.get("photo_url").getAsString() : "";
        String viewedAtStr = item.has("viewed_at") && !item.get("viewed_at").isJsonNull() ? item.get("viewed_at").getAsString() : "";

        holder.tvViewerName.setText(name);
        AvatarHelper.loadAvatar(context, photoUrl, avatar, name, holder.ivViewerAvatar);

        if (!viewedAtStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdf.parse(viewedAtStr);
                if (date != null) {
                    CharSequence ago = DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                    holder.tvViewedTime.setText("Viewed " + ago);
                } else {
                    holder.tvViewedTime.setText("Viewed recently");
                }
            } catch (Exception e) {
                holder.tvViewedTime.setText("Viewed recently");
            }
        } else {
            holder.tvViewedTime.setText("Viewed recently");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && !userId.isEmpty()) {
                listener.onViewerClick(userId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return viewers != null ? viewers.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivViewerAvatar;
        TextView tvViewerName;
        TextView tvViewedTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivViewerAvatar = itemView.findViewById(R.id.ivViewerAvatar);
            tvViewerName = itemView.findViewById(R.id.tvViewerName);
            tvViewedTime = itemView.findViewById(R.id.tvViewedTime);
        }
    }
}
