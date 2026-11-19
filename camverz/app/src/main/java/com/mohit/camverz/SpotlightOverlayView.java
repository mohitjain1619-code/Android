package com.mohit.camverz;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Production-Grade Interactive Spotlight Coachmark Overlay.
 * Features:
 * - Single or Multi-target transparent cutouts with pulsing neon border
 * - Animated tapping finger icon (with physical tap dip & scale bounce)
 * - Sleek, single-line instruction banner in English
 * - Outside touches completely blocked with English hint
 */
public class SpotlightOverlayView extends FrameLayout {

    public interface OnTargetClickListener {
        void onTargetClicked();
    }

    public interface OnActionListener {
        void onAction();
    }

    public static class TargetItem {
        public View view;
        public final RectF rect = new RectF();
        public boolean isCircle;
        public int highlightColor;
        public OnTargetClickListener listener;

        public TargetItem(View view, boolean isCircle, int highlightColor, OnTargetClickListener listener) {
            this.view = view;
            this.isCircle = isCircle;
            this.highlightColor = highlightColor;
            this.listener = listener;
        }
    }

    private final List<TargetItem> targets = new ArrayList<>();
    private float cornerRadius = 24f;
    private float padding = 16f;

    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator pulseAnimator;
    private ValueAnimator fingerAnimator;
    private float pulseFraction = 0f;

    private LinearLayout coachmarkCard;
    private TextView tvMessage;
    private TextView tvFinger;
    private TextView btnAction;
    private TextView btnExitDemo;
    private boolean isFingerAbove = false;

    public SpotlightOverlayView(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        clearPaint.setColor(Color.TRANSPARENT);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(6f);

        // Neon border pulsing glow animation
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1100);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation -> {
            pulseFraction = (float) animation.getAnimatedValue();
            invalidate();
        });

        setupCoachmarkCard();
        setupExitDemoButton();
    }

    private void setupExitDemoButton() {
        btnExitDemo = new TextView(getContext());
        btnExitDemo.setText("✕ Exit Demo");
        btnExitDemo.setTextColor(Color.parseColor("#E2E8F0"));
        btnExitDemo.setTextSize(13f);
        btnExitDemo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnExitDemo.setBackgroundResource(R.drawable.bg_luxury_pill_dark);
        btnExitDemo.setPadding(36, 18, 36, 18);
        btnExitDemo.setElevation(30f);
        btnExitDemo.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams exitParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        exitParams.gravity = Gravity.TOP | Gravity.END;
        exitParams.topMargin = 50;
        exitParams.rightMargin = 36;
        addView(btnExitDemo, exitParams);

        btnExitDemo.setOnClickListener(v -> {
            OnboardingTourManager.getInstance(getContext()).setTestModeAlwaysOn(getContext(), false);
            OnboardingTourManager.getInstance(getContext()).markTourCompleted(getContext());
            if (getContext() instanceof Activity) {
                detach((Activity) getContext());
            }
            Toast.makeText(getContext(), "Demo mode turned OFF", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCoachmarkCard() {
        coachmarkCard = new LinearLayout(getContext());
        coachmarkCard.setOrientation(LinearLayout.VERTICAL);
        coachmarkCard.setGravity(Gravity.CENTER);
        coachmarkCard.setBackgroundResource(R.drawable.bg_glass_card_luxury);
        coachmarkCard.setPadding(36, 20, 36, 20);
        coachmarkCard.setElevation(24f);

        // Animated tapping finger TextView
        tvFinger = new TextView(getContext());
        tvFinger.setText("👆");
        tvFinger.setTextSize(34f);
        tvFinger.setGravity(Gravity.CENTER);
        coachmarkCard.addView(tvFinger);

        // 1-line clean instruction message in English
        tvMessage = new TextView(getContext());
        tvMessage.setTextColor(Color.WHITE);
        tvMessage.setTextSize(14f);
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgParams.topMargin = 8;
        tvMessage.setLayoutParams(msgParams);
        coachmarkCard.addView(tvMessage);

        btnAction = new TextView(getContext());
        btnAction.setTextColor(Color.WHITE);
        btnAction.setTextSize(13f);
        btnAction.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnAction.setBackgroundResource(R.drawable.bg_btn_primary_gradient);
        btnAction.setPadding(36, 14, 36, 14);
        btnAction.setGravity(Gravity.CENTER);
        btnAction.setVisibility(View.GONE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = 12;
        btnParams.gravity = Gravity.CENTER_HORIZONTAL;
        btnAction.setLayoutParams(btnParams);
        coachmarkCard.addView(btnAction);

        coachmarkCard.setClipChildren(false);
        coachmarkCard.setClipToPadding(false);
        setClipChildren(false);
        setClipToPadding(false);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.TOP | Gravity.START;
        addView(coachmarkCard, cardParams);

        // Realistic Tapping Finger Animation
        fingerAnimator = ValueAnimator.ofFloat(0f, 1f);
        fingerAnimator.setDuration(650);
        fingerAnimator.setRepeatMode(ValueAnimator.REVERSE);
        fingerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fingerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        fingerAnimator.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            if (tvFinger != null) {
                float dip = 18f * f;
                tvFinger.setTranslationY(isFingerAbove ? dip : -dip);
                float scale = 1.0f - (0.16f * f);
                tvFinger.setScaleX(scale);
                tvFinger.setScaleY(scale);
            }
        });
    }

    /**
     * Attach spotlight with an action button inside the coachmark card (e.g. "Okay, Typed ✓").
     */
    public static SpotlightOverlayView attachWithAction(Activity activity, View target, String singleLineMessage,
                                                        String actionButtonText, boolean circle, int highlightColor,
                                                        OnActionListener actionListener, OnTargetClickListener listener) {
        if (activity == null || activity.isFinishing() || target == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) return null;

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return null;

        detach(activity);

        SpotlightOverlayView overlay = new SpotlightOverlayView(activity);
        List<TargetItem> list = new ArrayList<>();
        list.add(new TargetItem(target, circle, highlightColor, listener));
        overlay.targets.addAll(list);
        overlay.tvMessage.setText(singleLineMessage);

        if (actionButtonText != null && !actionButtonText.trim().isEmpty()) {
            overlay.btnAction.setText(actionButtonText.trim());
            overlay.btnAction.setVisibility(View.VISIBLE);
            overlay.btnAction.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAction();
                }
            });
        } else {
            overlay.btnAction.setVisibility(View.GONE);
        }

        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        target.post(() -> {
            overlay.calculateAllTargetRects();
            overlay.positionCoachmarkCard();
            overlay.pulseAnimator.start();
            overlay.fingerAnimator.start();
        });

        return overlay;
    }

    /**
     * Attach spotlight for a single view.
     */
    public static SpotlightOverlayView attach(Activity activity, View target, String singleLineMessage,
                                              boolean circle, int highlightColor, OnTargetClickListener listener) {
        if (activity == null || activity.isFinishing() || target == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) return null;

        List<TargetItem> list = new ArrayList<>();
        list.add(new TargetItem(target, circle, highlightColor, listener));
        return attachMulti(activity, list, singleLineMessage);
    }

    /**
     * Attach spotlight for multiple views (e.g. 3 interest cards).
     */
    public static SpotlightOverlayView attachMulti(Activity activity, List<TargetItem> targetItems, String singleLineMessage) {
        if (activity == null || activity.isFinishing() || targetItems == null || targetItems.isEmpty()) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) return null;

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return null;

        detach(activity);

        SpotlightOverlayView overlay = new SpotlightOverlayView(activity);
        overlay.targets.addAll(targetItems);
        overlay.tvMessage.setText(singleLineMessage);

        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Wait for views to layout
        View firstView = targetItems.get(0).view;
        firstView.post(() -> {
            overlay.calculateAllTargetRects();
            overlay.positionCoachmarkCard();
            overlay.pulseAnimator.start();
            overlay.fingerAnimator.start();
        });

        return overlay;
    }

    public static void detach(Activity activity) {
        if (activity == null) return;
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            View child = root.getChildAt(i);
            if (child instanceof SpotlightOverlayView) {
                ((SpotlightOverlayView) child).cleanUp();
                root.removeViewAt(i);
            }
        }
    }

    public void cleanUp() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator.removeAllUpdateListeners();
        }
        if (fingerAnimator != null) {
            fingerAnimator.cancel();
            fingerAnimator.removeAllUpdateListeners();
        }
    }

    private void calculateAllTargetRects() {
        int[] overlayLocation = new int[2];
        getLocationOnScreen(overlayLocation);

        for (TargetItem item : targets) {
            if (item.view == null) continue;
            int[] viewLoc = new int[2];
            item.view.getLocationOnScreen(viewLoc);

            float left = viewLoc[0] - overlayLocation[0] - padding;
            float top = viewLoc[1] - overlayLocation[1] - padding;
            float right = left + item.view.getWidth() + (padding * 2);
            float bottom = top + item.view.getHeight() + (padding * 2);

            item.rect.set(left, top, right, bottom);
        }
        invalidate();
    }

    private void positionCoachmarkCard() {
        if (coachmarkCard == null || targets.isEmpty()) return;

        // Bounding box of all targets
        float minTop = Float.MAX_VALUE;
        float maxBottom = Float.MIN_VALUE;
        float minLeft = Float.MAX_VALUE;
        float maxRight = Float.MIN_VALUE;
        for (TargetItem item : targets) {
            if (item.rect.top < minTop) minTop = item.rect.top;
            if (item.rect.bottom > maxBottom) maxBottom = item.rect.bottom;
            if (item.rect.left < minLeft) minLeft = item.rect.left;
            if (item.rect.right > maxRight) maxRight = item.rect.right;
        }

        float targetCenterX = (minLeft + maxRight) / 2f;
        float targetCenterY = (minTop + maxBottom) / 2f;

        int screenWidth = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;

        coachmarkCard.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth - 64, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int cardWidth = coachmarkCard.getMeasuredWidth();
        int cardHeight = coachmarkCard.getMeasuredHeight();

        // Calculate card Left (clamped within screen boundaries)
        float idealLeft = targetCenterX - (cardWidth / 2f);
        float clampLeft = Math.max(32f, Math.min(screenWidth - cardWidth - 32f, idealLeft));

        // Horizontal offset of finger inside card to point directly at targetCenterX
        float fingerOffset = targetCenterX - (clampLeft + (cardWidth / 2f));
        float maxFingerOffset = (cardWidth / 2f) - 40f;
        if (maxFingerOffset > 0) {
            fingerOffset = Math.max(-maxFingerOffset, Math.min(maxFingerOffset, fingerOffset));
        } else {
            fingerOffset = 0f;
        }
        tvFinger.setTranslationX(fingerOffset);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) coachmarkCard.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = (int) clampLeft;

        // If target area is in the lower half of screen, place card ABOVE target area pointing DOWN 👇
        if (targetCenterY > screenHeight / 2f) {
            isFingerAbove = true;
            lp.topMargin = (int) Math.max(32f, minTop - cardHeight - 24);
            tvFinger.setText("👇");

            coachmarkCard.removeAllViews();
            coachmarkCard.addView(tvMessage);
            if (btnAction != null && btnAction.getVisibility() == View.VISIBLE) {
                coachmarkCard.addView(btnAction);
            }
            coachmarkCard.addView(tvFinger);
        } else {
            // Target area in upper half, place card BELOW target area pointing UP 👆
            isFingerAbove = false;
            lp.topMargin = (int) Math.min(screenHeight - cardHeight - 32f, maxBottom + 24);
            tvFinger.setText("👆");

            coachmarkCard.removeAllViews();
            coachmarkCard.addView(tvFinger);
            coachmarkCard.addView(tvMessage);
            if (btnAction != null && btnAction.getVisibility() == View.VISIBLE) {
                coachmarkCard.addView(btnAction);
            }
        }

        coachmarkCard.setLayoutParams(lp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean needsCalc = false;
        for (TargetItem item : targets) {
            if (item.rect.isEmpty()) {
                needsCalc = true;
                break;
            }
        }
        if (needsCalc) {
            calculateAllTargetRects();
        }

        // 1. Draw 85% Dark Screen Overlay
        canvas.drawColor(Color.parseColor("#E605050A"));

        // 2. Punch transparent cutout holes and draw pulsing neon border for each target
        for (TargetItem item : targets) {
            pulsePaint.setColor(item.highlightColor);
            pulsePaint.setAlpha((int) (140 + (115 * pulseFraction)));
            pulsePaint.setStrokeWidth(5f + (4f * pulseFraction));

            if (item.isCircle) {
                float cx = item.rect.centerX();
                float cy = item.rect.centerY();
                float radius = Math.max(item.rect.width(), item.rect.height()) / 2f;
                canvas.drawCircle(cx, cy, radius, clearPaint);
                canvas.drawCircle(cx, cy, radius + (6f * pulseFraction), pulsePaint);
            } else {
                canvas.drawRoundRect(item.rect, cornerRadius, cornerRadius, clearPaint);
                RectF pulseRect = new RectF(
                        item.rect.left - (4f * pulseFraction),
                        item.rect.top - (4f * pulseFraction),
                        item.rect.right + (4f * pulseFraction),
                        item.rect.bottom + (4f * pulseFraction)
                );
                canvas.drawRoundRect(pulseRect, cornerRadius + 2f, cornerRadius + 2f, pulsePaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        // 1. Check if touch falls on Exit Demo button
        if (btnExitDemo != null && btnExitDemo.getVisibility() == View.VISIBLE) {
            android.graphics.Rect exitRect = new android.graphics.Rect();
            btnExitDemo.getHitRect(exitRect);
            if (exitRect.contains((int) x, (int) y)) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    btnExitDemo.performClick();
                }
                return true;
            }
        }

        // 2. Check if touch falls on Action Button (e.g. "Okay, Typed ✓")
        if (btnAction != null && btnAction.getVisibility() == View.VISIBLE) {
            int[] loc = new int[2];
            btnAction.getLocationOnScreen(loc);
            int[] overlayLoc = new int[2];
            getLocationOnScreen(overlayLoc);
            float btnLeft = loc[0] - overlayLoc[0];
            float btnTop = loc[1] - overlayLoc[1];
            android.graphics.RectF r = new android.graphics.RectF(
                    btnLeft, btnTop, btnLeft + btnAction.getWidth(), btnTop + btnAction.getHeight()
            );
            if (r.contains(x, y)) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    btnAction.performClick();
                }
                return true;
            }
        }

        // 3. Check if touch falls within any target cutout
        for (TargetItem item : targets) {
            if (item.rect.contains(x, y)) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (item.listener != null) {
                        item.listener.onTargetClicked();
                    } else if (item.view != null) {
                        item.view.performClick();
                    }
                }
                return true;
            }
        }

        // Touches outside all targets are blocked with concise English toast
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Toast.makeText(getContext(), "Please tap on the highlighted option 👉", Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
