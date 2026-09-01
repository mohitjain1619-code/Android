package com.mohitt.camverz;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

public class AvatarHelper {

    public static BitmapDrawable getInitialAvatar(Context context, String name) {
        int size = 96; // 96px width/height
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Consistent background colors based on user's name
        int[] colors = {
            Color.parseColor("#EF4444"), Color.parseColor("#F97316"), Color.parseColor("#F59E0B"),
            Color.parseColor("#10B981"), Color.parseColor("#06B6D4"), Color.parseColor("#3B82F6"),
            Color.parseColor("#6366F1"), Color.parseColor("#8B5CF6"), Color.parseColor("#EC4899")
        };
        int colorIndex = Math.abs(name != null ? name.hashCode() : 0) % colors.length;
        int bgColor = colors[colorIndex];
        
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(bgColor);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        
        // Text Paint settings
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        String initial = "U";
        if (name != null && !name.trim().isEmpty()) {
            initial = name.trim().substring(0, 1).toUpperCase();
        }
        
        // Center text vertically
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float y = (size / 2f) - ((fontMetrics.ascent + fontMetrics.descent) / 2f);
        canvas.drawText(initial, size / 2f, y, textPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static void loadAvatar(Context context, String photoUrl, String avatar, String userName, ImageView imageView) {
        if (context == null || imageView == null) return;

        BitmapDrawable initials = getInitialAvatar(context, userName);
        imageView.setImageDrawable(initials);

        // 1. Try photoUrl first if it is a valid HTTP URL or relative path
        if (photoUrl != null && !photoUrl.isEmpty() && !"null".equalsIgnoreCase(photoUrl)) {
            if (photoUrl.startsWith("http") || photoUrl.contains("/")) {
                Glide.with(context)
                        .load(photoUrl)
                        .placeholder(initials)
                        .error(initials)
                        .circleCrop()
                        .into(imageView);
                return;
            }
        }

        // 2. Try avatar field (could be HTTP URL or drawable name like av1, av2, etc.)
        if (avatar != null && !avatar.isEmpty() && !"null".equalsIgnoreCase(avatar)) {
            if (avatar.startsWith("http") || avatar.contains("/")) {
                Glide.with(context)
                        .load(avatar)
                        .placeholder(initials)
                        .error(initials)
                        .circleCrop()
                        .into(imageView);
                return;
            }

            // Strip extension if present (e.g. av1.png -> av1)
            String cleanAvatar = avatar.replaceAll("(?i)\\.(png|jpg|jpeg|webp)$", "").trim();
            int avatarResId = context.getResources().getIdentifier(cleanAvatar, "drawable", context.getPackageName());
            
            // Fallback for lowercasing avatar resource names
            if (avatarResId == 0 && cleanAvatar.toLowerCase().startsWith("av")) {
                avatarResId = context.getResources().getIdentifier(cleanAvatar.toLowerCase(), "drawable", context.getPackageName());
            }

            if (avatarResId != 0) {
                Glide.with(context)
                        .load(avatarResId)
                        .placeholder(initials)
                        .error(initials)
                        .circleCrop()
                        .into(imageView);
            }
        }
    }
}
