package com.mohit.camverz;

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

    private static boolean isValidContext(Context context) {
        if (context == null) return false;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) return false;
        }
        return true;
    }

    public static int resolveAvatarResId(Context context, String avatarName) {
        if (avatarName == null || avatarName.trim().isEmpty() || "null".equalsIgnoreCase(avatarName)) {
            return 0;
        }

        String cleanAvatar = avatarName.replaceAll("(?i)\\.(png|jpg|jpeg|webp|xml)$", "").trim().toLowerCase();

        switch (cleanAvatar) {
            case "av1": case "1": case "avatar1": case "avatar_1": return R.drawable.av1;
            case "av2": case "2": case "avatar2": case "avatar_2": return R.drawable.av2;
            case "av3": case "3": case "avatar3": case "avatar_3": return R.drawable.av3;
            case "av4": case "4": case "avatar4": case "avatar_4": return R.drawable.av4;
            case "av5": case "5": case "avatar5": case "avatar_5": return R.drawable.av5;
            case "av6": case "6": case "avatar6": case "avatar_6": return R.drawable.av6;
            case "av7": case "7": case "avatar7": case "avatar_7": return R.drawable.av7;
            case "av8": case "8": case "avatar8": case "avatar_8": return R.drawable.av8;
            case "av9": case "9": case "avatar9": case "avatar_9": return R.drawable.av9;
            case "av10": case "10": case "avatar10": case "avatar_10": return R.drawable.av10;
            case "av11": case "11": case "avatar11": case "avatar_11": return R.drawable.av11;
            case "av12": case "12": case "avatar12": case "avatar_12": return R.drawable.av12;
            case "av13": case "13": case "avatar13": case "avatar_13": return R.drawable.av13;
            case "av14": case "14": case "avatar14": case "avatar_14": return R.drawable.av14;
            case "av15": case "15": case "avatar15": case "avatar_15": return R.drawable.av15;
            case "ic_avatar_1": return R.drawable.ic_avatar_1;
            case "ic_avatar_2": return R.drawable.ic_avatar_2;
            case "ic_avatar_3": return R.drawable.ic_avatar_3;
            case "ic_avatar_4": return R.drawable.ic_avatar_4;
            case "ic_avatar_5": return R.drawable.ic_avatar_5;
        }

        if (context == null) return 0;

        try {
            int avatarResId = context.getResources().getIdentifier(cleanAvatar, "drawable", context.getPackageName());
            if (avatarResId != 0) return avatarResId;

            String digitsOnly = cleanAvatar.replaceAll("[^0-9]", "");
            if (!digitsOnly.isEmpty()) {
                return resolveAvatarResId(context, "av" + digitsOnly);
            }
        } catch (Exception ignored) {}

        return 0;
    }

    public static void loadAvatar(Context context, String photoUrl, String avatar, String userName, ImageView imageView) {
        if (!isValidContext(context) || imageView == null) return;

        // Calculate a deterministic fallback drawable resource ID (av1..av15) based on userName
        int fallbackResId = R.drawable.av1;
        if (userName != null && !userName.trim().isEmpty()) {
            int avatarNum = (Math.abs(userName.hashCode()) % 15) + 1;
            int resolved = resolveAvatarResId(context, "av" + avatarNum);
            if (resolved != 0) {
                fallbackResId = resolved;
            }
        }

        // 1. Try photoUrl first if valid HTTP URL or relative path
        if (photoUrl != null && !photoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(photoUrl)) {
            if (photoUrl.startsWith("http") || photoUrl.contains("/")) {
                Glide.with(context)
                        .load(photoUrl)
                        .placeholder(fallbackResId)
                        .error(fallbackResId)
                        .circleCrop()
                        .into(imageView);
                return;
            }
            int photoResId = resolveAvatarResId(context, photoUrl);
            if (photoResId != 0) {
                Glide.with(context)
                        .load(photoResId)
                        .placeholder(fallbackResId)
                        .error(fallbackResId)
                        .circleCrop()
                        .into(imageView);
                return;
            }
        }

        // 2. Try avatar field (could be HTTP URL or drawable name/number like av1, 1, avatar1, etc.)
        if (avatar != null && !avatar.trim().isEmpty() && !"null".equalsIgnoreCase(avatar)) {
            if (avatar.startsWith("http") || avatar.contains("/")) {
                Glide.with(context)
                        .load(avatar)
                        .placeholder(fallbackResId)
                        .error(fallbackResId)
                        .circleCrop()
                        .into(imageView);
                return;
            }

            int avatarResId = resolveAvatarResId(context, avatar);
            if (avatarResId != 0) {
                Glide.with(context)
                        .load(avatarResId)
                        .placeholder(fallbackResId)
                        .error(fallbackResId)
                        .circleCrop()
                        .into(imageView);
                return;
            }
        }

        // 3. Fallback: Load the assigned avatar graphic (av1..av15)
        Glide.with(context)
                .load(fallbackResId)
                .circleCrop()
                .into(imageView);
    }

    public static void loadAvatar(Context context, String avatar, String userName, ImageView imageView) {
        loadAvatar(context, null, avatar, userName, imageView);
    }
}

