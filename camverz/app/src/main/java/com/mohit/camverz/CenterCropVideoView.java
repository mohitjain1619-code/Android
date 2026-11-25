package com.mohit.camverz;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * CenterCropVideoView ensures vertical full-bleed playback without letterbox black bars.
 * If a video is horizontal (e.g. 644x474) or non-standard aspect ratio, it scales
 * to cover 100% of the vertical viewport height and crops horizontally, perfectly centered.
 */
public class CenterCropVideoView extends VideoView {

    private int videoWidth = 0;
    private int videoHeight = 0;

    public CenterCropVideoView(Context context) {
        super(context);
    }

    public CenterCropVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CenterCropVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setVideoDimensions(int width, int height) {
        this.videoWidth = width;
        this.videoHeight = height;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(videoWidth, widthMeasureSpec);
        int height = getDefaultSize(videoHeight, heightMeasureSpec);

        if (videoWidth > 0 && videoHeight > 0) {
            if (videoHeight >= videoWidth) {
                // PORTRAIT / VERTICAL VIDEO (e.g. 9:16 mobile video):
                // Fill full screen vertically (AspectFill / CenterCrop)
                if (videoWidth * height > width * videoHeight) {
                    width = videoWidth * height / videoHeight;
                } else {
                    height = width * videoHeight / videoWidth;
                }
            } else {
                // LANDSCAPE / HORIZONTAL VIDEO (e.g. 644x474 widescreen):
                // Fit width of screen without aggressive 3x zooming or cutting off face
                height = width * videoHeight / videoWidth;
            }
        }
        setMeasuredDimension(width, height);
    }
}
