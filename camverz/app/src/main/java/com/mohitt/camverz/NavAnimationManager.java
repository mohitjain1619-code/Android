package com.mohitt.camverz;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AnimationUtils;

public class NavAnimationManager {

    public static void animateNavItemActive(View indicator, View icon) {
        // Indicator fade in + scale
        AnimatorSet indicatorSet = new AnimatorSet();
        indicatorSet.playTogether(
            ObjectAnimator.ofFloat(indicator, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(indicator, "scaleX", 0.6f, 1f),
            ObjectAnimator.ofFloat(indicator, "scaleY", 0.6f, 1f)
        );
        indicatorSet.setDuration(500);
        indicatorSet.start();

        // Icon scale up
        AnimatorSet iconSet = new AnimatorSet();
        iconSet.playTogether(
            ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.15f),
            ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.15f)
        );
        iconSet.setDuration(400);
        iconSet.start();
    }

    public static void animateNavItemInactive(View indicator, View icon) {
        // Indicator fade out
        indicator.animate()
            .alpha(0f)
            .scaleX(0.6f)
            .scaleY(0.6f)
            .setDuration(300)
            .start();

        // Icon scale down
        AnimatorSet iconSet = new AnimatorSet();
        iconSet.playTogether(
            ObjectAnimator.ofFloat(icon, "scaleX", 1.15f, 1f),
            ObjectAnimator.ofFloat(icon, "scaleY", 1.15f, 1f)
        );
        iconSet.setDuration(300);
        iconSet.start();
    }

    public static void animatePulse(View view) {
        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playSequentially(
            createPulse(view, 1f, 1.1f, 200),
            createPulse(view, 1.1f, 1f, 200)
        );
        pulseSet.start();
    }

    private static AnimatorSet createPulse(View view, float from, float to, long duration) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(view, "scaleX", from, to),
            ObjectAnimator.ofFloat(view, "scaleY", from, to)
        );
        set.setDuration(duration);
        return set;
    }
}
