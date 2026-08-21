package com.mohitt.camverz;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

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
}
