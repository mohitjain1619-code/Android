package com.mohitt.camverz;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class OvalFrameView extends View {

    private Paint strokePaint;
    private Paint cornerPaint;
    private float ovalRadiusX;
    private float ovalRadiusY;
    private int strokeColor = 0xFF00D9FF; // Default cyan
    private float strokeWidth = 6f;
    private float cornerRadius = 20f;

    public OvalFrameView(Context context) {
        super(context);
        init();
    }

    public OvalFrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OvalFrameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(strokeColor);

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(strokeWidth);
        cornerPaint.setColor(strokeColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        ovalRadiusX = width * 0.35f;
        ovalRadiusY = height * 0.45f;

        float centerX = width / 2f;
        float centerY = height / 2f;

        // Draw main oval
        RectF ovalRect = new RectF(
                centerX - ovalRadiusX,
                centerY - ovalRadiusY,
                centerX + ovalRadiusX,
                centerY + ovalRadiusY
        );

        canvas.drawOval(ovalRect, strokePaint);

        // Draw corner accents (Instagram style)
        float cornerLength = 30f;
        float[] corners = {
                // Top-left
                centerX - ovalRadiusX, centerY - ovalRadiusY,
                // Top-right
                centerX + ovalRadiusX, centerY - ovalRadiusY,
                // Bottom-left
                centerX - ovalRadiusX, centerY + ovalRadiusY,
                // Bottom-right
                centerX + ovalRadiusX, centerY + ovalRadiusY
        };

        for (int i = 0; i < corners.length; i += 2) {
            float x = corners[i];
            float y = corners[i + 1];

            // Only draw corners, not connecting them further
        }
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
        if (strokePaint != null) {
            strokePaint.setColor(color);
        }
        if (cornerPaint != null) {
            cornerPaint.setColor(color);
        }
        invalidate();
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        if (strokePaint != null) {
            strokePaint.setStrokeWidth(width);
        }
        if (cornerPaint != null) {
            cornerPaint.setStrokeWidth(width);
        }
        invalidate();
    }

    public float getOvalRadiusX() {
        return ovalRadiusX;
    }

    public float getOvalRadiusY() {
        return ovalRadiusY;
    }

    public float getOvalCenterX() {
        return getWidth() / 2f;
    }

    public float getOvalCenterY() {
        return getHeight() / 2f;
    }
}
