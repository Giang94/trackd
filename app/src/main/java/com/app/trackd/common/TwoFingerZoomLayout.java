package com.app.trackd.common;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class TwoFingerZoomLayout extends FrameLayout {

    private static final float ZOOM_OUT_THRESHOLD = 0.9f; // pinch-out threshold
    private final android.view.ScaleGestureDetector scaleDetector;
    private OnZoomOutListener listener;
    private View originalContent;
    private ViewGroup originalParent;

    public TwoFingerZoomLayout(Context context) {
        this(context, null);
    }

    public TwoFingerZoomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        scaleDetector = new android.view.ScaleGestureDetector(context,
                new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(android.view.ScaleGestureDetector detector) {
                        // Trigger only for pinch-out (zooming out)
                        if (detector.getScaleFactor() < ZOOM_OUT_THRESHOLD) {
                            if (listener != null) listener.onZoomOut();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean onScaleBegin(android.view.ScaleGestureDetector detector) {
                        // Allow scale gesture to start
                        return true;
                    }
                });
    }

    public void setOnZoomOutListener(OnZoomOutListener listener) {
        this.listener = listener;
    }

    public void attachToActivity(Activity activity) {
        originalParent = activity.findViewById(android.R.id.content);
        originalContent = originalParent.getChildAt(0);

        // Avoid double wrapping
        if (originalContent.getParent() == this) return;

        originalParent.removeView(originalContent);
        this.addView(originalContent);

        originalParent.addView(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Intercept if two fingers are down
        if (ev.getPointerCount() == 2) {
            scaleDetector.onTouchEvent(ev);
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() == 2) {
            scaleDetector.onTouchEvent(ev);
            return true;
        }
        return super.onTouchEvent(ev);
    }

    public interface OnZoomOutListener {
        void onZoomOut();
    }
}
