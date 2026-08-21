package com.mohitt.camverz;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;

import androidx.core.content.ContextCompat;

public class TextHelper {

    /**
     * Formats user headers dynamically to display Name, Gender, Age, Crown, and a Blue Vector Verified checkmark.
     * Uses ImageSpan for rendering the blue verified checkmark drawable inline inside the TextView.
     */
    public static SpannableStringBuilder getFormattedHeader(Context context, String name, String gender, int age, boolean isPremium, boolean isVerified) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // 1. Name
        builder.append(name != null ? name : "User");

        // 2. Gender Symbol
        if (gender != null) {
            String genLower = gender.toLowerCase();
            if (genLower.startsWith("f")) {
                builder.append(" ♀️");
            } else if (genLower.startsWith("m")) {
                builder.append(" ♂️");
            }
        }

        // 3. Age
        builder.append(" • ").append(String.valueOf(age)).append(" Yrs");

        // 4. Premium Crown
        if (isPremium) {
            builder.append(" 👑");
        }

        // 5. Blue Verified Badge checkmark (ImageSpan)
        if (isVerified) {
            builder.append("  "); // Spacer characters where the image span will reside
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_verified_badge);
            if (drawable != null) {
                // Scale drawable dimensions to matches text height (approx 15dp)
                float density = context.getResources().getDisplayMetrics().density;
                int size = (int) (14 * density);
                drawable.setBounds(0, 0, size, size);
                
                // Align image span inline
                ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                builder.setSpan(span, builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return builder;
    }

    /**
     * Formats post headers dynamically to display Name, Gender symbol (vector), a Blue Verified checkmark, and primary preference (small cyan text).
     */
    public static SpannableStringBuilder getPostHeader(Context context, String name, String gender, boolean isVerified, String sexPreference) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // 1. Name
        builder.append(name != null ? name : "User");

        // 2. Gender Vector Icon (ImageSpan)
        if (gender != null) {
            String genLower = gender.toLowerCase();
            int iconRes = 0;
            if (genLower.startsWith("f")) {
                iconRes = R.drawable.ic_female_symbol_pink;
            } else if (genLower.startsWith("m")) {
                iconRes = R.drawable.ic_male_blue;
            }

            if (iconRes != 0) {
                builder.append("  "); // Spacer character
                Drawable drawable = ContextCompat.getDrawable(context, iconRes);
                if (drawable != null) {
                    float density = context.getResources().getDisplayMetrics().density;
                    int size = (int) (14 * density);
                    drawable.setBounds(0, 0, size, size);
                    ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                    builder.setSpan(span, builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        // 3. Blue Verified Badge checkmark (ImageSpan)
        if (isVerified) {
            builder.append("  "); // Spacer character
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_verified_badge);
            if (drawable != null) {
                float density = context.getResources().getDisplayMetrics().density;
                int size = (int) (14 * density);
                drawable.setBounds(0, 0, size, size);
                ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                builder.setSpan(span, builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // 4. Sex Preference tag (Small neon cyan label)
        if (sexPreference != null && !sexPreference.isEmpty()) {
            int start = builder.length();
            builder.append("  ").append(sexPreference.toUpperCase());
            int end = builder.length();
            
            // Set size span: make it 35% smaller
            builder.setSpan(new RelativeSizeSpan(0.65f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // Set color span: use neon cyan color
            builder.setSpan(new ForegroundColorSpan(Color.parseColor("#00E5FF")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }
}
